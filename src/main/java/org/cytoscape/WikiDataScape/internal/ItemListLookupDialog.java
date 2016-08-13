package org.cytoscape.WikiDataScape.internal;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
public class ItemListLookupDialog extends javax.swing.JFrame {

    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JList<String> jList1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextField jTextField1;

    List<SearchResult> results = new ArrayList<>();

    public ItemListLookupDialog() {

        initComponents();

        CyAppAdapter adapter = CyActivator.getCyAppAdapter();
        TaskManager taskManager = adapter.getTaskManager();

        Action searchAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = jTextField1.getText();
                try {
                    results.clear();
                    doSearch(text);
                    String[] jListValues = results.stream().map(x -> x.toString()).collect(Collectors.toList()).toArray(new String[0]);
                    jList1.setListData(jListValues);
                    if (!results.isEmpty()) {
                        jButton1.setEnabled(true);
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
                if (jList1.getSelectedValue() == null) {
                    selectedIndex = 0;
                } else {
                    selectedIndex = jList1.getSelectedIndex();
                }
                SearchResult selectedProperty = results.get(selectedIndex);

                IdLookupTask idLookupTask = new IdLookupTask(jTextArea1.getText().split("\n"), selectedProperty.id);
                taskManager.execute(idLookupTask.createTaskIterator());
            }
        };

        jButton1.addActionListener(search);
        jTextField1.addActionListener(searchAction);

        // set up the jframe, then display it
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    private void initComponents() {

        jTextField1 = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jLabel2 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        jList1 = new javax.swing.JList<>();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("Search for External ID Property (ex: UniProt ID)");

        jScrollPane1.setEnabled(false);

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane1.setViewportView(jTextArea1);

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("Enter a list of IDs to lookup");

        jButton1.setText("Go!");

        jList1.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = {};

            public int getSize() {
                return strings.length;
            }

            public String getElementAt(int i) {
                return strings[i];
            }
        });
        jScrollPane3.setViewportView(jList1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(jScrollPane3)
                                .addGroup(layout.createSequentialGroup()
                                        .addGap(0, 0, Short.MAX_VALUE)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 342, Short.MAX_VALUE)
                                                .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jButton1))
                                .addComponent(jTextField1, javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 484, Short.MAX_VALUE))
                        .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                        .addGap(19, 19, 19)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 10, Short.MAX_VALUE)
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(layout.createSequentialGroup()
                                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 262, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addContainerGap())
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                        .addComponent(jButton1)
                                        .addGap(128, 128, 128))))
        );

        pack();
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
            if (datatype.equals("external-id")) {
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
