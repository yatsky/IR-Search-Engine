package com.infs7410;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;


public class App {
    private JButton btnSearch;
    private JPanel panelMain;
    private JTextField query;
    private JRadioButton allFieldsRadioButton;
    private JRadioButton URLRadioButton;
    private JRadioButton titleRadioButton;
    private JRadioButton textRadioButton;
    private JTextPane fuzzySearchTextPane;
    private JTextPane normalSearchTextPane;
    private Set<String> fields = new HashSet<>();

    public App() {
        // Only return the maximum number of results
        SearchEngine se = new SearchEngine(20);

        btnSearch.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (fields.isEmpty()) JOptionPane.showMessageDialog(null, "Please select one of " +
                        "the fields!");
                else if (query.getText().isEmpty()) JOptionPane.showMessageDialog(null, "Please " +
                        "add a query!");
                else {
                    try {
                        StringBuilder normalOutput = new StringBuilder();
                        StringBuilder fuzzyOutput = new StringBuilder();
                        normalOutput.append("Current limit of results is capped to 20.\n");
                        fuzzyOutput.append("Current limit of results is capped to 20.\n");
                        for (String field : fields) {
                            // Start normal search
                            normalOutput.append("Normal Search:\n");
                            TopDocs normalTopDocs = se.normalSearch(field, query.getText());
                            normalOutput.append("\tTotal Results by " + field + ": " + normalTopDocs.totalHits + "\n");
                            buildOutput(normalOutput, se, field, normalTopDocs, false);
                            normalSearchTextPane.setText(normalOutput.toString());
                            // Start fuzzy search
                            TopDocs fuzzyTopDocs = se.fuzzySearch(field, query.getText());
                            fuzzyOutput.append("Fuzzy Search\n");
                            fuzzyOutput.append("\tTotal Results by " + field + ": " + fuzzyTopDocs.totalHits + "\n");
                            buildOutput(fuzzyOutput, se, field, fuzzyTopDocs, true);
                            fuzzySearchTextPane.setText(fuzzyOutput.toString());
                        }

//                        JOptionPane.showMessageDialog(null, output.toString());
                    } catch (Exception exc) {
                        System.out.println(e.toString());
                    }
                }

            }
        });
        allFieldsRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (fields.size() == 3) fields = new HashSet<>();
                else {
                    fields.add("text");
                    fields.add("title");
                    fields.add("baseUrl");
                }
            }
        });
        titleRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (fields.contains("title")) {
                    fields.remove("title");
                } else {
                    fields.add("title");
                }
            }
        });
        URLRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (fields.contains("baseUrl")) fields.remove("baseUrl");
                else fields.add("baseUrl");
            }
        });
        textRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (fields.contains("text")) fields.remove("text");
                else fields.add("text");
            }
        });
    }

    /**
     * Build the output of the search
     * @param output Output to be built
     * @param se The search engine
     * @param field Field to be searched against
     * @param topDocs Top docs returned in the search
     * @throws IOException
     */
    private static void buildOutput(StringBuilder output, SearchEngine se, String field,
                                    TopDocs topDocs, Boolean isFuzzy)
            throws Exception {
        // Do not try to show all text
        String field_to_show = field.equals("text") ? "title" : field;
        int counter = 0;
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document doc = se.indexSearcher.doc(scoreDoc.doc);
            // Highlight the keywords
            Query query = isFuzzy? se.fuzzyQuery.rewrite(se.indexSearcher.getIndexReader()):se.normalQuery;
            String[] frags = se.highlightQuery(field_to_show,
                    query, se.analyzer, scoreDoc);
            output.append("\t");
            output.append(++counter);
            output.append(". ");

            // Take care of show titles for text matches.
            // if the title cannot be highlighted, then simply show title
            if (frags.length == 0){
                output.append(doc.get(field_to_show));
            }else {
                for (String frag:frags){
                    output.append(frag);
                }
            }

            output.append("\n");
        }
        output.append("\n");
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Lucene Search Engine");
        frame.setContentPane(new App().panelMain);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}
