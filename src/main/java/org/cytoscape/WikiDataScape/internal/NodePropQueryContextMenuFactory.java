package org.cytoscape.WikiDataScape.internal;

import org.cytoscape.WikiDataScape.internal.model.Property;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JMenu;
import org.cytoscape.WikiDataScape.internal.model.Counter;
import org.cytoscape.WikiDataScape.internal.model.Item;
import org.cytoscape.WikiDataScape.internal.model.Triples;
import org.cytoscape.WikiDataScape.internal.tasks.TransformTask;

import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.application.swing.CyNodeViewContextMenuFactory;
import org.cytoscape.model.CyNetwork;
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
public class NodePropQueryContextMenuFactory implements CyNodeViewContextMenuFactory {

    private CyNetworkView netView;
    private final TaskManager taskManager;
    HashSet<Item> subjects;

    public NodePropQueryContextMenuFactory(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    @Override
    public CyMenuItem createMenuItem(CyNetworkView netView, View<CyNode> nodeView) {
        System.out.println("createMenuItem nodePropQuery");
        this.netView = netView;

        CyNetwork myNet = this.netView.getModel();
        JMenu root = new JMenu("Properties");
        List<CyNode> nodes = CyTableUtil.getNodesInState(myNet, "selected", true);
        
        // populate the submenu with known properties for these nodes
        JMenuItem menuItem;
        subjects = new HashSet<>();
        for (CyNode node : nodes){
            String nodeName = myNet.getDefaultNodeTable().getRow(node.getSUID()).get("name", String.class);
            String nodeWdid = myNet.getDefaultNodeTable().getRow(node.getSUID()).get("wdid", String.class);
            subjects.add(new Item(nodeName, nodeWdid));
        }
        Counter subjectProperties = CyActivator.triples.getSubjectProperties(subjects);
        System.out.println("subjectProperties: " + subjectProperties);
        
        for (Object propObj : subjectProperties) {
            Property prop = (Property)propObj;
            int count = subjectProperties.count(prop);
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
        System.out.println("-----------clickedProp-----------");
        CyNetwork myNet = this.netView.getModel();
        List<CyNode> nodes = CyTableUtil.getNodesInState(myNet, "selected", true);
        
        //PropQueryTask propQueryTask = new PropQueryTask(nodes, prop);
        //taskManager.execute(propQueryTask.createTaskIterator());
        
        // We can do they query using our stored triples! Don't need to do another sparql query!
        Triples subjectTriples = CyActivator.triples.getSubjectTriples(subjects);
        Triples triplesWithProperty = subjectTriples.getTriplesWithProperty(prop);
        TransformTask transformTask = new TransformTask(triplesWithProperty);
        taskManager.execute(transformTask.createTaskIterator());
    }
}
