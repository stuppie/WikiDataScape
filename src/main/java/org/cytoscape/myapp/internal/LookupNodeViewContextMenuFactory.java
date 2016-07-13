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

public class LookupNodeViewContextMenuFactory implements CyNodeViewContextMenuFactory, ActionListener {

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

    public LookupNodeViewContextMenuFactory(CyNetworkManager cyNetworkManager, CyNetworkFactory cyNetworkFactory,
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
            return;
        }
        String IDs = "";
        CyTable nodeTable = myNet.getDefaultNodeTable();
        for (CyNode node : nodes) {
            String gene = nodeTable.getRow(node.getSUID()).get("wdid", String.class);
            IDs = IDs.concat("wd:" + gene + " ");
        }
        System.out.println(IDs);
        String queryString = prefix
                + "SELECT ?item ?value ?valueLabel ?itemLabel WHERE {\n"
                + "  values ?item {%s}\n"
                + "  {?item wdt:P279* ?value .}\n"
                + "  union{?item wdt:P31* ?value .}\n"
                + "  filter (?item != ?value)\n"
                + "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\" }\n"
                + "}";

        queryString = String.format(queryString, IDs);
        System.out.println(queryString);
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.sparqlService("https://query.wikidata.org/sparql", queryString);
        ResultSet results = qexec.execSelect();
        //ResultSetFormatter.out(System.out, results, query);

        Set<String> types = new HashSet<>();
        String itemLabel = "";
        while (results.hasNext()) {
            QuerySolution statement = results.next();
            itemLabel = statement.getLiteral("itemLabel").getString();
            String valueWdid = statement.getResource("value").toString().replaceFirst("http://www.wikidata.org/entity/", "");
            types.add(valueWdid);
        }
        System.out.println("types: " + types);
        myNet.getDefaultNodeTable().getRow(nodes.get(0).getSUID()).set("name", itemLabel);
        if (myNet.getDefaultNodeTable().getColumn("type") == null) {
            myNet.getDefaultNodeTable().createColumn("type", String.class, true);
        }
        if (types.contains("Q8054")) // protein
        {
            myNet.getDefaultNodeTable().getRow(nodes.get(0).getSUID()).set("type", "protein");
        } else if (types.contains("Q2996394")) // BIO PROCESS
        {
            myNet.getDefaultNodeTable().getRow(nodes.get(0).getSUID()).set("type", "GO");
        } else if (types.contains("Q14860489")) // mol func
        {
            myNet.getDefaultNodeTable().getRow(nodes.get(0).getSUID()).set("type", "GO");
        } else if (types.contains("Q5058355")) // cell comp
        {
            myNet.getDefaultNodeTable().getRow(nodes.get(0).getSUID()).set("type", "GO");

        } else if (types.contains("Q898273")) // domain
        {
            myNet.getDefaultNodeTable().getRow(nodes.get(0).getSUID()).set("type", "domain");
        } else if (types.contains("Q7187")) // gene
        {
            myNet.getDefaultNodeTable().getRow(nodes.get(0).getSUID()).set("type", "gene");
        } else if (types.contains("Q11173")) // chemical compound
        {
            myNet.getDefaultNodeTable().getRow(nodes.get(0).getSUID()).set("type", "compound");
        }
        
        
        // testing
        String queryStringProps = prefix
                + "SELECT distinct ?prop ?propLabel  WHERE {\n" +
                "  %s ?prop ?val .\n" +
                "  ?property ?ref ?prop .\n" +
                "  ?property rdfs:label ?propLabel\n" +
                "  FILTER (LANG(?propLabel) = 'en') .\n" +
                "}";
        queryString = String.format(queryStringProps, IDs);
        System.out.println(queryString);
        query = QueryFactory.create(queryString);
        qexec = QueryExecutionFactory.sparqlService("https://query.wikidata.org/sparql", queryString);
        results = qexec.execSelect();
        //ResultSetFormatter.out(System.out, results, query);

        HashMap<String, String> props = new HashMap<>();
        while (results.hasNext()) {
            QuerySolution statement = results.next();
            itemLabel = statement.getLiteral("propLabel").getString();
            String valueWdid = statement.getResource("prop").toString();
            props.put(itemLabel, valueWdid);
        }
        System.out.println(props);
        CyNode node = nodes.get(0);
        CyActivator.setNodeProps(node, props);
        
    }
}
