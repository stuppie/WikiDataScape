package org.cytoscape.myapp.internal;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Set;

import javax.swing.JMenuItem;
import javax.swing.JMenu;
import org.cytoscape.application.CyApplicationManager;

import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.application.swing.CyNodeViewContextMenuFactory;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.TaskManager;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;

/**
 * This is the right click menu when clicking a node, gets populated with properties of that node from wikidata when
 * clicked. It constructs a sparql query using the prop ID for the menu item clicked and the node selected, then creates
 * a PropQueryTask to do the sparql query and make the network
 *
 * @author gstupp
 */
public class NodePropQueryContextMenuFactory implements CyNodeViewContextMenuFactory {

    private CyNetworkView netView;
    private final CyNetworkManager cyNetworkManager;
    private final CyNetworkFactory cyNetworkFactory;
    private final TaskManager taskManager;
    private final CyApplicationManager applicationManager;
    private final CyEventHelper eventHelper;
    String prefix = "PREFIX wd: <http://www.wikidata.org/entity/>\n"
            + "PREFIX wdt: <http://www.wikidata.org/prop/direct/>\n"
            + "PREFIX wikibase: <http://wikiba.se/ontology#>\n"
            + "PREFIX p: <http://www.wikidata.org/prop/>\n"
            + "PREFIX ps: <http://www.wikidata.org/prop/statement/>\n"
            + "PREFIX pq: <http://www.wikidata.org/prop/qualifier/>\n"
            + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
            + "PREFIX bd: <http://www.bigdata.com/rdf#>\n";

    public NodePropQueryContextMenuFactory(CyNetworkManager cyNetworkManager, CyNetworkFactory cyNetworkFactory,
            TaskManager taskManager, CyApplicationManager applicationManager, CyEventHelper eventHelper) {
        this.cyNetworkManager = cyNetworkManager;
        this.cyNetworkFactory = cyNetworkFactory;
        this.taskManager = taskManager;
        this.applicationManager = applicationManager;
        this.eventHelper = eventHelper;
    }

    @Override
    public CyMenuItem createMenuItem(CyNetworkView netView, View<CyNode> nodeView) {
        System.out.println("createMenuItem");
        this.netView = netView;

        CyNetwork myNet = this.netView.getModel();
        JMenu root = new JMenu("Properties");
        List<CyNode> nodes = CyTableUtil.getNodesInState(myNet, "selected", true);
        if (nodes.size() > 1) {
            System.out.println("Only works on one selected node");
        }
        // populate the submenu with known properties for that node
        JMenuItem menuItem;
        CyNode node = nodes.get(0);
        Set<String> nodeProps = CyActivator.getNodeProps(node);
        for (String nodeProp : nodeProps) {
            menuItem = new JMenuItem(nodeProp);
            menuItem.addActionListener((ActionEvent e) -> {
                clickedProp(nodeProp);
            });
            root.add(menuItem);
        }
        CyMenuItem cyMenuItem = new CyMenuItem(root, 0);
        return cyMenuItem;
    }

    private void clickedProp(String nodeProp) {
        System.out.println("----------------------");

        // set node visual styles
        SetVisualStyleTask setVisualStyleTask = new SetVisualStyleTask();
        taskManager.execute(setVisualStyleTask.createTaskIterator());

        WikibaseDataFetcher wbdf = WikibaseDataFetcher.getWikidataDataFetcher();

        CyNetwork myNet = this.netView.getModel();
        List<CyNode> nodes = CyTableUtil.getNodesInState(myNet, "selected", true);
        String IDs = "";
        CyTable nodeTable = myNet.getDefaultNodeTable();
        for (CyNode node : nodes) {
            String gene = nodeTable.getRow(node.getSUID()).get("wdid", String.class);
            IDs = IDs.concat("wd:" + gene + " ");
        }
        System.out.println(IDs);

        String queryString = prefix
                + "SELECT ?item ?value ?valueLabel WHERE {\n"
                + "  ?item wdt:%s ?value .\n"
                + "  values ?item {%s}\n"
                + "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\" }\n"
                + "}";
        queryString = String.format(queryString, CyActivator.getPropID(nodeProp), IDs);

        PropQueryTask propQueryTask = new PropQueryTask(queryString);
        taskManager.execute(propQueryTask.createTaskIterator());
    }
}
