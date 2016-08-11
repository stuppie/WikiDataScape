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
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.*;
import org.cytoscape.WikiDataScape.internal.tasks.IdLookupTask;
import org.cytoscape.WikiDataScape.internal.tasks.NodeLookupTask;
import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.work.TaskManager;

/**
 * Uses the wikidata search api to allow searching for and adding a list of items By specifying a property and a list of
 * values for that property. Properties are limited to type external ID
 *
 * @author gstupp
 */
//B7NR10
//Q2S1V4
// https://www.wikidata.org/w/api.php?action=wbsearchentities&search=uniprot&language=en&format=json&type=property
// https://www.wikidata.org/w/api.php?action=wbgetentities&ids=P22|P25|P352&props=datatype
public class ItemListLookupDialog {

    List<SearchResult> results = new ArrayList<>();

    public ItemListLookupDialog() {
        CyAppAdapter adapter = CyActivator.getCyAppAdapter();
        TaskManager taskManager = adapter.getTaskManager();

        JFrame frame = new JFrame("Lookup Property");
        JPanel myPanel = new JPanel();
        JTextField textField = new JTextField(20);
        textField.setMaximumSize(new Dimension(400, 200));
        JList jList = new JList();
        jList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jList.setSize(100, 100);
        JButton searchButton = new JButton("Create Nodes");
        searchButton.setEnabled(false);

        JTextArea textArea = new JTextArea(10, 20);
        JScrollPane scrollPane = new JScrollPane(textArea);

        Action searchAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = textField.getText();
                try {
                    results.clear();
                    doSearch(text);
                    String[] jListValues = results.stream().map(x -> x.toString()).collect(Collectors.toList()).toArray(new String[0]);
                    jList.setListData(jListValues);
                    if (!results.isEmpty()) {
                        searchButton.setEnabled(true);
                    }
                } catch (IOException ex) {
                    Logger.getLogger(ItemListLookupDialog.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };

        Action search = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedIndex;
                if (jList.getSelectedValue() == null) {
                    selectedIndex = 0;
                } else {
                    selectedIndex = jList.getSelectedIndex();
                }
                SearchResult selectedProperty = results.get(selectedIndex);

                IdLookupTask idLookupTask = new IdLookupTask(textArea.getText().split("\n"), selectedProperty.id);
                taskManager.execute(idLookupTask.createTaskIterator());
            }
        };

        searchButton.addActionListener(search);

        textField.addActionListener(searchAction);

        myPanel.add(new JLabel("Input Item to Lookup"));
        myPanel.add(textField);
        myPanel.add(jList);
        myPanel.add(searchButton);
        myPanel.add(scrollPane);

        frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.X_AXIS));
        myPanel.setLayout(new BoxLayout(myPanel, BoxLayout.Y_AXIS));
        frame.add(myPanel);
        // set up the jframe, then display it
        frame.setPreferredSize(new Dimension(400, 600));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void doSearch(String text) throws MalformedURLException, IOException {
        Map<String, SearchResult> searchResults = new HashMap<>();

        String sURL = "https://www.wikidata.org/w/api.php?action=wbsearchentities&search=%s&language=en&format=json&type=property";
        sURL = String.format(sURL, text);
        System.out.println("sURL: " + sURL);

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
            String description = null;
            try {
                description = s.getAsJsonObject().get("description").getAsString();
            } catch (NullPointerException e) {
            }
            searchResults.put(id, new SearchResult(name, id, description));
        }
        lookupType(searchResults);
    }
    
    private void lookupType(Map<String, SearchResult> searchResults) throws MalformedURLException, IOException {
        
        // Lookup types of each property
        String join = String.join("|", searchResults.keySet());
        String sURL = "https://www.wikidata.org/w/api.php?action=wbgetentities&ids=%s&props=datatype&format=json";
        sURL = String.format(sURL, join);
        System.out.println("sURL: " + sURL);
        URL url = new URL(sURL);
        HttpURLConnection request = (HttpURLConnection) url.openConnection();
        request.connect();

        JsonParser jp = new JsonParser(); //from gson
        JsonElement root = jp.parse(new InputStreamReader((InputStream) request.getContent())); //Convert the input stream to a json element
        JsonObject rootobj = root.getAsJsonObject(); //May be an array, may be an object. 
        Set<Map.Entry<String, JsonElement>> search = rootobj.get("entities").getAsJsonObject().entrySet();
        System.out.println(rootobj.get("entities").toString());
        
        for (Map.Entry<String, JsonElement> s : search) {
            String propId = s.getKey();
            String datatype = s.getValue().getAsJsonObject().get("datatype").getAsString();
            if (datatype.equals("external-id")){
                results.add(searchResults.get(propId));
            }
        }

    }

    public static void main(String[] args) {
        ItemListLookupDialog x = new ItemListLookupDialog();
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

        @Override
        public String toString() {
            if (description == null) {
                return name;
            } else {
                return name + " (" + description + ")";
            }
        }
    }

}
