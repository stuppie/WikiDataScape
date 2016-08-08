package org.cytoscape.WikiDataScape.internal;

import org.cytoscape.WikiDataScape.internal.model.Property;
import org.cytoscape.WikiDataScape.internal.tasks.NodeLookupTask;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JMenu;
import org.cytoscape.WikiDataScape.internal.model.Counter;
import org.cytoscape.WikiDataScape.internal.model.Item;
import org.cytoscape.WikiDataScape.internal.model.Triples;
import org.cytoscape.WikiDataScape.internal.tasks.TransformTask_1;
import org.cytoscape.application.CyApplicationManager;

import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.application.swing.CyNodeViewContextMenuFactory;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.TaskManager;

/**
 * This is the right click menu when clicking a node, gets populated with properties of that node from wikidata when
 * clicked. It constructs a sparql query using the prop ID for the menu item clicked and the node selected, then creates
 * a PropQueryTask to do the sparql query and make the network
 *
 * @author gstupp
 */
public class NodeInversePropQueryContextMenuFactory implements CyNodeViewContextMenuFactory, ActionListener {

    private CyNetworkView netView;
    private final TaskManager taskManager;
    HashSet<Item> objects;

    public NodeInversePropQueryContextMenuFactory(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    @Override
    public CyMenuItem createMenuItem(CyNetworkView netView, View<CyNode> nodeView) {
        System.out.println("createMenuItem nodeinverse");

        // If a node has already done the "what links here" query, it should display the submenu
        // otherwise, should display the single "what links here" menu
        // If multiple nodes are selected. It should be a lookup if any one of the selected nodes hasn't been looked up
        this.netView = netView;
        CyNetwork myNet = this.netView.getModel();
        List<CyNode> nodes = CyTableUtil.getNodesInState(myNet, "selected", true);
        JMenuItem root;
        System.out.println("CyActivator.nodeDoneWhatLinks: " + CyActivator.nodeDoneWhatLinks);
        for (CyNode node : nodes) {
            if (!CyActivator.nodeDoneWhatLinks.getOrDefault(node, Boolean.FALSE)) {
                root = new JMenuItem("Perform \"What links here?\" query");
                root.addActionListener(this);
                CyMenuItem cyMenuItem = new CyMenuItem(root, 0);
                return cyMenuItem;
            }
        }

        root = new JMenu("What links here?");
        // populate the submenu with known properties for these nodes
        JMenuItem menuItem;
        objects = new HashSet<>();
        for (CyNode node : nodes) {
            String nodeName = myNet.getDefaultNodeTable().getRow(node.getSUID()).get("name", String.class);
            String nodeWdid = myNet.getDefaultNodeTable().getRow(node.getSUID()).get("wdid", String.class);
            objects.add(new Item(nodeName, nodeWdid));
        }
        Counter objectsProperties = CyActivator.triples.getObjectProperties(objects);
        System.out.println("objectsProperties: " + objectsProperties);

        for (Object propObj : objectsProperties) {
            Property prop = (Property) propObj;
            int count = objectsProperties.count(prop);
            menuItem = new JMenuItem(prop.getName(count));
            menuItem.addActionListener((ActionEvent e) -> {
                clickedProp(prop);
            });
            root.add(menuItem);
        }
        CyMenuItem cyMenuItem = new CyMenuItem(root, 0);
        return cyMenuItem;
    }

    private void clickedProp(Property prop) {
        System.out.println("----------clickedProp------------");

        // We can do they query using our stored triples! Don't need to do another sparql query!
        Triples objectTriples = CyActivator.triples.getObjectTriples(objects);
        Triples triplesWithProperty = objectTriples.getTriplesWithProperty(prop);
        TransformTask_1 transformTask = new TransformTask_1(triplesWithProperty);
        taskManager.execute(transformTask.createTaskIterator());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("------- Perform \"What links here?\" query --------");
        CyNetwork myNet = this.netView.getModel();
        List<CyNode> nodes = CyTableUtil.getNodesInState(myNet, "selected", true);
        NodeLookupTask nodeLookupTask = new NodeLookupTask(nodes, true);
        taskManager.execute(nodeLookupTask.createTaskIterator());

    }
}
