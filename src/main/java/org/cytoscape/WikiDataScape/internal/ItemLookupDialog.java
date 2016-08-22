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
import java.net.URLEncoder;
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

public class ItemLookupDialog extends javax.swing.JFrame {
    
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JList<String> jList1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTextField jTextField1;
    
    List<SearchResult> results = new ArrayList<>();

    public ItemLookupDialog() {
        
        initComponents();

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
                    Logger.getLogger(ItemLookupDialog.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };

        Action addSelection = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedIndex;
                if (jList1.getSelectedValue() == null) {
                    selectedIndex = 0;
                } else {
                    selectedIndex = jList1.getSelectedIndex();
                }
                SearchResult result = results.get(selectedIndex);
                String[] wdids = {result.id};
                CyActivator.makeNewNodes(wdids);
            }
        };

        jButton1.addActionListener(addSelection);
        jButton1.setEnabled(false);

        jTextField1.addActionListener(searchAction);
        this.setVisible(true);
    }
    
    private void initComponents() {

        jScrollPane3 = new javax.swing.JScrollPane();
        jList1 = new javax.swing.JList<>();
        jTextField1 = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jList1.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane3.setViewportView(jList1);

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("Search for an item");

        jButton1.setText("Use this item!");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 312, Short.MAX_VALUE)
                    .addComponent(jTextField1, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
            .addGroup(layout.createSequentialGroup()
                .addGap(103, 103, 103)
                .addComponent(jButton1)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 274, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jButton1)
                .addContainerGap(13, Short.MAX_VALUE))
        );

        pack();
        setLocationRelativeTo(null);
    }
    
    
    // https://www.wikidata.org/w/api.php?action=wbsearchentities&search=indole&language=en
    private void doSearch(String text) throws MalformedURLException, IOException {
        String sURL = "https://www.wikidata.org/w/api.php?action=wbsearchentities&search=%s&language=en&format=json";
        sURL = String.format(sURL, URLEncoder.encode(text, "UTF-8"));

        URL url = new URL(sURL);
        System.out.println("url: " + url);
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
            } catch (NullPointerException e){}
            
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
