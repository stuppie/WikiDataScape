package org.cytoscape.myapp.internal;

import java.awt.Color;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JMenuItem;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Literal;
import org.cytoscape.app.CyAppAdapter;
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
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.view.model.ContinuousRange;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.DiscreteRange;
import org.cytoscape.view.model.Range;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.model.Visualizable;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.DefaultVisualizableVisualProperty;
import org.cytoscape.view.presentation.property.PaintVisualProperty;
import org.cytoscape.view.vizmap.VisualMappingFunction;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;
import sun.security.util.Length;

public class PropNodeViewContextMenuFactory implements CyNodeViewContextMenuFactory {

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

    public PropNodeViewContextMenuFactory(CyNetworkManager cyNetworkManager, CyNetworkFactory cyNetworkFactory,
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
        JMenuItem menuItem;
        CyNode node = nodes.get(0);
        Set<String> nodeProps = CyActivator.getNodeProps(node);
        for (String nodeProp : nodeProps) {
            menuItem = new JMenuItem(nodeProp);
            menuItem.addActionListener((ActionEvent e) -> {
                clickedProp(e, nodeProp);
            });
            root.add(menuItem);
        }
        CyMenuItem cyMenuItem = new CyMenuItem(root, 0);
        return cyMenuItem;
    }

    private void clickedProp(ActionEvent e, String nodeProp) {
        System.out.println("----------------------");
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

        System.out.println(queryString);
        Map<Item, List<String>> valueProt = new HashMap<>();
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.sparqlService("https://query.wikidata.org/sparql", queryString);
        ResultSet results = qexec.execSelect();
        //ResultSetFormatter.out(System.out, results, query);

        while (results.hasNext()) {
            QuerySolution statement = results.next();
            System.out.println("statement: " + statement.toString());
            String item = statement.getResource("item").toString().replaceFirst("http://www.wikidata.org/entity/", "");
            String valueWdid = "";
            try {
                valueWdid = statement.getResource("value").toString().replaceFirst("http://www.wikidata.org/entity/", "");
            } catch (java.lang.ClassCastException classCastException) {
                try {
                    valueWdid = statement.getLiteral("value").getString();
                } catch (java.lang.ClassCastException classCastException2) {
                    System.out.println("error");
                    valueWdid = "error";
                }
            }
            String valueLabel = statement.getLiteral("valueLabel").getString();

            Item gt = new Item(valueLabel, "", "unknown", valueWdid);
            if (!valueProt.containsKey(gt)) {
                valueProt.put(gt, new ArrayList<String>());
            }
            valueProt.get(gt).add(item);
        }
        //System.out.println("goProt: " + valueProt);

        TransformTaskFactory ttf = new TransformTaskFactory(cyNetworkManager, cyNetworkFactory, applicationManager, valueProt);
        taskManager.execute(ttf.createTaskIterator());

    }

}
