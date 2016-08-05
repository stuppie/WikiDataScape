package org.cytoscape.WikiDataScape.internal;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JMenuItem;
import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.application.swing.CyNodeViewContextMenuFactory;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.TaskManager;

/**
 *
 * @author gstupp
 */
public class BrowseContextMenu implements CyNodeViewContextMenuFactory, ActionListener {

    private CyNetworkView netView;
    private final TaskManager taskManager;

    public BrowseContextMenu() {
        CyAppAdapter adapter = CyActivator.getCyAppAdapter();
        this.taskManager = adapter.getTaskManager();
    }

    @Override
    public CyMenuItem createMenuItem(CyNetworkView netView, View<CyNode> nodeView) {
        this.netView = netView;
        JMenuItem root = new JMenuItem("Open in browser");
        root.addActionListener(this);
        /*CyNetwork myNet = this.netView.getModel();
        List<CyNode> nodes = CyTableUtil.getNodesInState(myNet, "selected", true);
        if (! (nodes.size() == 1)){
            root.setEnabled(false);
        }*/
        CyMenuItem cyMenuItem = new CyMenuItem(root, 0);
        return cyMenuItem;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        CyNetwork myNet = this.netView.getModel();
        List<CyNode> nodes = CyTableUtil.getNodesInState(myNet, "selected", true);
        for (CyNode node : nodes) {
            String wdid = myNet.getDefaultNodeTable().getRow(node.getSUID()).get("wdid", String.class);
            try {
                openWebpage(new URL("https://www.wikidata.org/wiki/" + wdid));
            } catch (MalformedURLException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void openWebpage(URI uri) {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(uri);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void openWebpage(URL url) {
        // http://stackoverflow.com/questions/10967451/open-a-link-in-browser-with-java-button
        try {
            openWebpage(url.toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
