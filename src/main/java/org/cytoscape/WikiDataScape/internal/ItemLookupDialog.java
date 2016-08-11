package org.cytoscape.WikiDataScape.internal;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.*;

/**
 * Uses the wikidata search api to allow searching for and adding an item
 * @author gstupp
 */
//B7NR10
//Q2S1V4
public class ItemLookupDialog {

    List<SearchResult> results = new ArrayList<>();

    public ItemLookupDialog() {

        JFrame frame = new JFrame("Lookup item");
        JPanel myPanel = new JPanel();
        JTextField textField = new JTextField(20);
        JList jList = new JList();
        jList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JButton selectButton = new JButton("Use this item");

        Action searchAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = textField.getText();
                try {
                    results.clear();
                    doSearch(text);
                    String[] jListValues = results.stream().map(x -> x.name + " (" + x.description + ")").collect(Collectors.toList()).toArray(new String[0]);
                    jList.setListData(jListValues);
                    if (!results.isEmpty()) {
                        selectButton.setEnabled(true);
                    }
                } catch (IOException ex) {
                    Logger.getLogger(ItemLookupDialog.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };

        Action addSelection = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedIndex;
                if (jList.getSelectedValue() == null) {
                    selectedIndex = 0;
                } else {
                    selectedIndex = jList.getSelectedIndex();
                }
                SearchResult result = results.get(selectedIndex);
                String[] wdids = {result.id};
                CyActivator.makeNewNodes(wdids);
            }
        };

        selectButton.addActionListener(addSelection);
        selectButton.setEnabled(false);

        textField.setMaximumSize(new Dimension(Integer.MAX_VALUE, textField.getPreferredSize().height));
        textField.addActionListener(searchAction);

        myPanel.add(new JLabel("Input Item to Lookup"));
        myPanel.add(textField);
        myPanel.add(jList);
        myPanel.add(selectButton);

        frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
        myPanel.setLayout(new BoxLayout(myPanel, BoxLayout.Y_AXIS));
        frame.add(myPanel);
        // set up the jframe, then display it
        frame.setPreferredSize(new Dimension(300, 400));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // https://www.wikidata.org/w/api.php?action=wbsearchentities&search=indole&language=en
    // props
    // https://www.wikidata.org/w/api.php?action=wbsearchentities&search=uniprot&language=en&format=json&type=property
    // https://www.wikidata.org/w/api.php?action=wbgetentities&ids=P22|P25|P352&props=datatype
    
    
    private void doSearch(String text) throws MalformedURLException, IOException {
        String sURL = "https://www.wikidata.org/w/api.php?action=wbsearchentities&search=%s&language=en&format=json";
        sURL = String.format(sURL, text);

        // Connect to the URL using java's native library
        URL url = new URL(sURL);
        HttpURLConnection request = (HttpURLConnection) url.openConnection();
        request.connect();

        // Convert to a JSON object to print data
        JsonParser jp = new JsonParser(); //from gson
        JsonElement root = jp.parse(new InputStreamReader((InputStream) request.getContent())); //Convert the input stream to a json element
        JsonObject rootobj = root.getAsJsonObject(); //May be an array, may be an object. 
        JsonArray search = rootobj.get("search").getAsJsonArray();

        for (JsonElement s : search) {
            String id = s.getAsJsonObject().get("id").getAsString();
            String name = s.getAsJsonObject().get("label").getAsString();
            String description = s.getAsJsonObject().get("description").getAsString();
            results.add(new SearchResult(name, id, description));
        }

    }

    public static void main(String[] args) {
        ItemLookupDialog x = new ItemLookupDialog();
    }
    
    private class SearchResult {

        public String name = null;
        public String id = null;
        public String description = null;

        public SearchResult(String name, String id, String description) {
            this.name = name;
            this.id = id;
            this.description = description;
        }

        public SearchResult() {

        }
    }

}
