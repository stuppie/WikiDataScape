package org.cytoscape.myapp.internal;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JMenuItem;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
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
 * Single lookup menu item. Queries wikidata for all queryable properties for an item
 *
 * @author gstupp
 */
public class NodeLookupContextMenuFactory implements CyNodeViewContextMenuFactory, ActionListener {

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

    public NodeLookupContextMenuFactory(CyNetworkManager cyNetworkManager, CyNetworkFactory cyNetworkFactory,
            TaskManager taskManager, CyApplicationManager applicationManager, CyEventHelper eventHelper) {
        this.cyNetworkManager = cyNetworkManager;
        this.cyNetworkFactory = cyNetworkFactory;
        this.taskManager = taskManager;
        this.applicationManager = applicationManager;
        this.eventHelper = eventHelper;
    }

    @Override
    public CyMenuItem createMenuItem(CyNetworkView netView, View<CyNode> nodeView) {
        this.netView = netView;
        JMenuItem root = new JMenuItem("Lookup");
        root.addActionListener(this);
        CyMenuItem cyMenuItem = new CyMenuItem(root, 0);
        return cyMenuItem;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("-------lookup--------");
        WikibaseDataFetcher wbdf = WikibaseDataFetcher.getWikidataDataFetcher();
        CyNetwork myNet = this.netView.getModel();
        List<CyNode> nodes = CyTableUtil.getNodesInState(myNet, "selected", true);
        if (nodes.size() > 1) {
            System.out.println("Only works on one selected node");
            // TODO: Make it work on more tha one node at a time
            return;
        }
        String IDs = "";
        CyTable nodeTable = myNet.getDefaultNodeTable();
        for (CyNode node : nodes) {
            String gene = nodeTable.getRow(node.getSUID()).get("wdid", String.class);
            IDs = IDs.concat("wd:" + gene + " ");
        }
        System.out.println(IDs);

        // Get all subclass and instance relationships for this item
        String queryString = prefix
                + "SELECT ?item ?value ?valueLabel ?itemLabel WHERE {\n"
                + "  values ?item {%s}\n"
                + "  {?item wdt:P279* ?value .}\n"
                + "  union{?item wdt:P31* ?value .}\n"
                + "  filter (?item != ?value)\n"
                + "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\" }\n"
                + "}";

        queryString = String.format(queryString, IDs);
        CyNode node = nodes.get(0);
        NodeLookupTypeTask nodeLookupTask = new NodeLookupTypeTask(queryString, node);
        taskManager.execute(nodeLookupTask.createTaskIterator());

        // Get all properties for an item
        String queryStringProps = prefix
                + "SELECT distinct ?prop ?propLabel  WHERE {\n"
                + "  %s ?prop ?val .\n"
                + "  ?property ?ref ?prop .\n"
                + "  ?property rdfs:label ?propLabel\n"
                + "  FILTER (LANG(?propLabel) = 'en') .\n"
                + "}";
        queryString = String.format(queryStringProps, IDs);
        
        NodeLookupPropsTask nodeLookupPropsTask = new NodeLookupPropsTask(queryString, node);
        taskManager.execute(nodeLookupPropsTask.createTaskIterator());

    }
}
