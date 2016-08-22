package org.cytoscape.WikiDataScape.internal.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.util.Iterator;
import java.util.stream.Collectors;
import org.apache.http.client.methods.HttpPost;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.engine.http.HttpQuery;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.cytoscape.WikiDataScape.internal.CyActivator;
import org.cytoscape.WikiDataScape.internal.model.Item;
import org.cytoscape.WikiDataScape.internal.model.Property;
import org.cytoscape.WikiDataScape.internal.model.Triple;
import org.cytoscape.WikiDataScape.internal.model.Triples;
import org.cytoscape.model.CyRow;

/**
 *
 * @author gstupp
 */
public class NodeLookupTask extends AbstractTask {

    private final CyApplicationManager applicationManager;
    private final CyNetworkView myView;
    private final CyNetwork myNet;
    private final List<CyNode> nodes;
    private BiMap<CyNode, String> nodeWdid;
    private boolean doReverse;

    String prefix = "PREFIX wd: <http://www.wikidata.org/entity/>\n"
            + "PREFIX wdt: <http://www.wikidata.org/prop/direct/>\n"
            + "PREFIX wikibase: <http://wikiba.se/ontology#>\n"
            + "PREFIX p: <http://www.wikidata.org/prop/>\n"
            + "PREFIX ps: <http://www.wikidata.org/prop/statement/>\n"
            + "PREFIX pq: <http://www.wikidata.org/prop/qualifier/>\n"
            + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
            + "PREFIX bd: <http://www.bigdata.com/rdf#>\n"
            + "PREFIX hint: <http://www.bigdata.com/queryHints#>\n";
    

    public NodeLookupTask(List<CyNode> nodes, boolean doReverse) {
        this.doReverse = doReverse;
        CyAppAdapter adapter = CyActivator.getCyAppAdapter();
        this.applicationManager = adapter.getCyApplicationManager();
        this.myView = this.applicationManager.getCurrentNetworkView();
        this.myNet = this.myView.getModel();
        this.nodes = nodes;
    }
    
    public NodeLookupTask(List<CyNode> nodes) {
        this(nodes, false);
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        CyAppAdapter adapter = CyActivator.getCyAppAdapter();
        
        // make the type column
        CyTable nodeTable = myNet.getDefaultNodeTable();
        if (nodeTable.getColumn("subclass of") == null) 
            nodeTable.createListColumn("subclass of", String.class, false);
        if (nodeTable.getColumn("instance of") == null) 
            nodeTable.createListColumn("instance of", String.class, false);
        

        nodeWdid = HashBiMap.create();
        
        Set<String> wdids = nodes.stream().map(node -> nodeTable.getRow(node.getSUID()).get("wdid", String.class)).collect(Collectors.toSet());

        for (CyNode node : nodes) {
            String wdid = nodeTable.getRow(node.getSUID()).get("wdid", String.class);
            nodeWdid.put(node, wdid); // map ID to node
            CyActivator.nodeDoneWhatLinks.put(node, this.doReverse);
        }
        System.out.println("nodeWdid" + nodeWdid);

        doQuery(wdids, doReverse);
        
        TaskManager taskManager = adapter.getTaskManager();
        SetVisualStyleTask setVisualStyleTask = new SetVisualStyleTask(myView);
        taskManager.execute(setVisualStyleTask.createTaskIterator());

    }

    private void doQuery(Set<String> wdids) {

        String IDs = String.join(" ", wdids.stream().map(wdid -> "wd:" + wdid).collect(Collectors.toSet()));

        // Get all properties for all selected nodes
        // http://tinyurl.com/h2ovsqj
        String queryString = prefix
                + "SELECT distinct ?item ?itemLabel ?prop ?propLabel ?vals ?valsLabel (datatype (?vals) AS ?type) WHERE {\n"
                + "  hint:Query hint:optimizer \"None\" .\n"
                + String.format("  values ?item {%s}\n", IDs)
                + "  ?item ?p ?vals .\n"
                + "  ?prop wikibase:directClaim ?p .\n"
                + "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\" }\n"
                + "}";

        System.out.println(queryString);
        HttpQuery.urlLimit = 4000;
        Query query = QueryFactory.create(queryString, Syntax.syntaxSPARQL);
        QueryEngineHTTP qeh = new QueryEngineHTTP("https://query.wikidata.org/sparql", query);
        ResultSet results = qeh.execSelect();
        
        //ResultSetFormatter.out(System.out, results, query);
        
        Triples triples = new Triples();

        while (results.hasNext()) {
            QuerySolution statement = results.next();
            String propLabel = statement.getLiteral("propLabel").getString();
            String itemLabel = statement.getLiteral("itemLabel").getString();
            String valueLabel = statement.getLiteral("valsLabel").getString();
            String propId = statement.getResource("prop").getLocalName();
            String itemWdid = statement.getResource("item").getLocalName();
            Resource typeResource = statement.getResource("type");
            CyNode thisNode = this.nodeWdid.inverse().get(itemWdid);
            
            if (typeResource != null) {
                if ("string".equals(typeResource.getLocalName())) {
                    // This is an ID property. Add it to the table, don't add it to the right-click menu
                    // System.out.println("string statement: " + statement);
                    String value = statement.getLiteral("vals").getString();
                    updateNodeTable(thisNode, propLabel, value);
                } // else ignore it (example: http://www.wikidata.org/entity/P1121)
            } else {
                // The property may still not be wikidata item
                // example http://www.wikidata.org/entity/P117
                String valNameSpace = statement.getResource("vals").getNameSpace();
                if ("http://www.wikidata.org/entity/".equals(valNameSpace)) {
                    // This property links to a wikidata item. We'll add it to the right-click menu
                    Item subject = new Item(itemLabel, itemWdid);
                    Item object = new Item(valueLabel, statement.getResource("vals").getLocalName());
                    Property prop = new Property(propLabel, propId, "item");
                    Triple triple = new Triple(subject, prop, object);
                    //System.out.println("triple: " +triple);
                    triples.addTriple(triple);
                }
            }
            
            myNet.getDefaultNodeTable().getRow(thisNode.getSUID()).set("name", itemLabel);
            
        }
        CyActivator.triples.addTriples(triples);
        
        for (Item subj : triples.getSubjects()){
            CyNode thisNode = this.nodeWdid.inverse().get(subj.getWdid());
            Triples subjectTriples = triples.getSubjectTriples(subj);
            Triples subclassTriples = subjectTriples.getTriplesWithProperty(new Property("subclass of", "P279", null));
            Triples instanceTriples = subjectTriples.getTriplesWithProperty(new Property("instance of", "P31", null));
            List<Item> subclasses = subclassTriples.getTriplesFlat().stream().map(x -> x.getObject()).collect(Collectors.toList());
            List<Item> instances = instanceTriples.getTriplesFlat().stream().map(x -> x.getObject()).collect(Collectors.toList());
            for (Item item: subclasses){
                updateNodeTable(thisNode, "subclass of", item.name);
            }
            for (Item item: instances){
                updateNodeTable(thisNode, "instance of", item.name);
            }
        }
        
    }
    
    private void doReverseQuery(Set<String> wdids){
        String IDs = String.join(" ", wdids.stream().map(wdid -> "wd:" + wdid).collect(Collectors.toSet()));

        String queryString = prefix
                + "    SELECT distinct ?item ?itemLabel ?prop ?propLabel ?vals ?valsLabel WHERE {\n"
                + "      hint:Query hint:optimizer \"None\" .\n"
                + String.format("values ?vals {%s}\n", IDs)
                + "      ?item ?p ?vals .\n"
                + "      ?prop wikibase:directClaim ?p .\n"
                + "      SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\" }\n"
                + "  }";
        System.out.println(queryString);
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.sparqlService("https://query.wikidata.org/sparql", queryString);
        ResultSet results = qexec.execSelect();
        //ResultSetFormatter.out(System.out, results, query);
        
        Triples triples = new Triples();

        while (results.hasNext()) {
            QuerySolution statement = results.next();
            String propLabel = statement.getLiteral("propLabel").getString();
            String itemLabel = statement.getLiteral("itemLabel").getString();
            String valueLabel = statement.getLiteral("valsLabel").getString();
            String propId = statement.getResource("prop").getLocalName();
            String itemWdid = statement.getResource("item").getLocalName();
            String valsWdid = statement.getResource("vals").getLocalName();
            
            Item subject = new Item(itemLabel, itemWdid);
            Item object = new Item(valueLabel, valsWdid);
            Property prop = new Property(propLabel, propId, "item");
            Triple triple = new Triple(subject, prop, object);
            System.out.println("triple: " +triple);
            triples.addTriple(triple);
            
        }
        CyActivator.triples.addTriples(triples);

    }
    
    
    
    private void doQuery(Set<String> wdids, boolean doReverse) {
        if (!doReverse) {
            doQuery(wdids);
        } else
            doReverseQuery(wdids);
    }

    private void updateNodeTable(CyNode node, String propLabel, String value) {
        // Every "ID" column is a list column
        CyTable nodeTable = myNet.getDefaultNodeTable();
        if (nodeTable.getColumn(propLabel) == null) {
            nodeTable.createListColumn(propLabel, String.class, false);
        }
        //Check if the new value is already in the list.
        CyRow thisRow = nodeTable.getRow(node.getSUID());
        if (thisRow.getList(propLabel, String.class) == null) {
            thisRow.set(propLabel, new ArrayList<String>());
        }
        List<String> currList = thisRow.getList(propLabel, String.class);
        if (!currList.contains(value)) {
            thisRow.getList(propLabel, String.class).add(value);
        }
    }
    

    public TaskIterator createTaskIterator() {
        return new TaskIterator(new NodeLookupTask(nodes, doReverse));
    }
}
