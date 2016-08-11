package org.cytoscape.WikiDataScape.internal;

import java.util.HashMap;
import java.util.Map;
import javax.swing.*;

/**
 *
 * @author gstupp
 */
//B7NR10
//Q2S1V4
public class IdLookupDialog {

    JTextArea textArea;
    JComboBox petList;
    private final int result;
    String[] dbs;
    Map<String, String> prop;

    public IdLookupDialog() {
        // dbs = new String[] {"UniProt ID", "Entrez Gene ID"};
        prop = new HashMap<>();
        prop.put("UniProt ID", "P352");
        prop.put("Entrez Gene ID", "P351");
        dbs = prop.keySet().toArray(new String[prop.size()]);

        textArea = new JTextArea(10, 20);
        JScrollPane scrollPane = new JScrollPane(textArea);
        petList = new JComboBox(dbs);
        JPanel myPanel = new JPanel();
        myPanel.add(new JLabel("Input IDs to lookup"));
        myPanel.add(petList);
        myPanel.add(scrollPane);

        result = JOptionPane.showConfirmDialog(null, myPanel, "ID Lookup", JOptionPane.OK_CANCEL_OPTION);
    }

    public String getIds() {
        return textArea.getText();
    }

    public String getDb() {
        return prop.get((String) petList.getSelectedItem());
    }

    public int getResult() {
        return result;
    }

    public static void main(String[] args) {
        IdLookupDialog x = new IdLookupDialog();
        if (x.getResult() == 0) {
            System.out.println(x.getIds());
            System.out.println(x.getDb());
        }
    }

}
