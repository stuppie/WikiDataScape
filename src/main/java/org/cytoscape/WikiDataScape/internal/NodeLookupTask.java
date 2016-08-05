package org.cytoscape.WikiDataScape.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.jena.rdf.model.Resource;
import org.cytoscape.model.CyRow;

/**
 *
 * @author gstupp
 */
public class NodeLookupTask extends AbstractTask {

    private String queryString;
    private TaskManager taskManager;
    private final CyNetworkManager netMgr;
    private final CyApplicationManager applicationManager;
    private final CyNetworkView myView;
    private final CyNetwork myNet;
    private final List<CyNode> nodes;
    private BiMap<CyNode, String> nodeWdid;
    private boolean includeReverse;

    String prefix = "PREFIX wd: <http://www.wikidata.org/entity/>\n"
            + "PREFIX wdt: <http://www.wikidata.org/prop/direct/>\n"
            + "PREFIX wikibase: <http://wikiba.se/ontology#>\n"
            + "PREFIX p: <http://www.wikidata.org/prop/>\n"
            + "PREFIX ps: <http://www.wikidata.org/prop/statement/>\n"
            + "PREFIX pq: <http://www.wikidata.org/prop/qualifier/>\n"
            + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
            + "PREFIX bd: <http://www.bigdata.com/rdf#>\n"
            + "PREFIX hint: <http://www.bigdata.com/queryHints#>\n";
    

    public NodeLookupTask(List<CyNode> nodes, boolean includeReverse) {
        this.includeReverse = includeReverse;
        CyAppAdapter adapter = CyActivator.getCyAppAdapter();
        this.netMgr = adapter.getCyNetworkManager();
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
        this.taskManager = adapter.getTaskManager();

        nodeWdid = HashBiMap.create();
        CyTable nodeTable = myNet.getDefaultNodeTable();

        Set<String> wdids = nodes.stream().map(node -> nodeTable.getRow(node.getSUID()).get("wdid", String.class)).collect(Collectors.toSet());

        for (CyNode node : nodes) {
            String wdid = nodeTable.getRow(node.getSUID()).get("wdid", String.class);
            nodeWdid.put(node, wdid); // map ID to node
            CyActivator.nodeDoneWhatLinks.put(node, this.includeReverse);
        }
        System.out.println("nodeWdid" + nodeWdid);

        doQuery(wdids, includeReverse);

    }

    private void doQuery(Set<String> wdids, boolean includeReverse) {
        if (!includeReverse) {
            doQuery(wdids);
            return;
        }

        String IDs = String.join(" ", wdids.stream().map(wdid -> "wd:" + wdid).collect(Collectors.toSet()));

        String queryString = prefix
                + "SELECT * WHERE {\n"
                + "  {\n"
                + "    SELECT distinct ?item ?itemLabel ?prop ?propLabel ?vals (datatype (?vals) AS ?type) WHERE {\n"
                + "      hint:Query hint:optimizer \"None\" .\n"
                + String.format("values ?item {%s}\n", IDs)
                + "      ?item ?p ?vals .\n"
                + "      ?prop wikibase:directClaim ?p .\n"
                + "      SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\" }\n"
                + "    }\n"
                + "  } UNION {\n"
                + "      hint:Query hint:optimizer \"None\" .\n"
                + String.format("values ?vals {%s}\n", IDs)
                + "      ?item ?p ?vals .\n"
                + "      ?prop wikibase:directClaim ?p .\n"
                + "      SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\" }\n"
                + "  }\n"
                + "}";

        System.out.println(queryString);
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.sparqlService("https://query.wikidata.org/sparql", queryString);
        ResultSet results = qexec.execSelect();
        //ResultSetFormatter.out(System.out, results, query);

        Map<CyNode, Map<Property, Property>> nodeProps = new HashMap<>();
        Map<CyNode, Map<Property, Property>> inverseNodeProps = new HashMap<>();

        while (results.hasNext()) {
            QuerySolution statement = results.next();
            String propLabel = statement.getLiteral("propLabel").getString();
            String itemLabel = statement.getLiteral("itemLabel").getString();
            String prop = statement.getResource("prop").getLocalName();
            String valueWdid = statement.getResource("item").getLocalName();
            Resource typeResource = statement.getResource("type");

            String propType = null;

            if (wdids.contains(valueWdid)) {
                CyNode thisNode = this.nodeWdid.inverse().get(valueWdid);

                if (typeResource != null) {
                    if ("string".equals(typeResource.getLocalName())) {
                        // This is an ID property. Add it to the table, don't add it to the right-click menu
                        // System.out.println("string statement: " + statement);
                        String value = statement.getLiteral("vals").getString();
                        updateNodeTable(thisNode, propLabel, value);
                        propType = "id";
                    } // else ignore it (example: http://www.wikidata.org/entity/P1121)
                } else {
                    // The property may still not be wikidata item
                    // example http://www.wikidata.org/entity/P117
                    String valNameSpace = statement.getResource("vals").getNameSpace();
                    if ("http://www.wikidata.org/entity/".equals(valNameSpace)) {
                        // This property links to a wikidata item. We'll add it to the right-click menu
                        propType = "item";
                    }
                }
                nodeProps.putIfAbsent(thisNode, new HashMap<>());
                Property p = new Property(propLabel, prop, propType);
                if (nodeProps.get(thisNode).containsKey(p)) {
                    nodeProps.get(thisNode).get(p).incrementCount();
                } else {
                    nodeProps.get(thisNode).put(p, p);
                }

                // TODO: This could be optimized
                myNet.getDefaultNodeTable().getRow(thisNode.getSUID()).set("name", itemLabel);
            } else {
                // reverse prop
                String thisNodeWdid = statement.getResource("vals").getLocalName();
                CyNode thisNode = this.nodeWdid.inverse().get(thisNodeWdid);
                inverseNodeProps.putIfAbsent(thisNode, new HashMap<>());
                Property p = new Property(propLabel, prop, propType);
                if (inverseNodeProps.get(thisNode).containsKey(p)) {
                    inverseNodeProps.get(thisNode).get(p).incrementCount();
                } else {
                    inverseNodeProps.get(thisNode).put(p, p);
                }
            }
        }
        System.out.println("nodeProps: " + nodeProps);
        System.out.println("inverseNodeProps: " + inverseNodeProps);
        
        // make the type column
        CyTable nodeTable = myNet.getDefaultNodeTable();
        if (nodeTable.getColumn("type") == null) {
            nodeTable.createColumn("type", String.class, true);
        }

        // Set the node properties for each node and lookup the node's type based on them
        for (CyNode thisNode : nodeProps.keySet()) {
            Collection<Property> props = nodeProps.get(thisNode).values();

            Set<Property> itemProps = props.stream().filter(p -> "item".equals(p.getType())).collect(Collectors.toSet());
            CyActivator.setNodeProps(thisNode, itemProps);

            Set<Property> idProps = props.stream().filter(p -> "id".equals(p.getType())).collect(Collectors.toSet());
            String type = propertiesToType(idProps);
            myNet.getDefaultNodeTable().getRow(thisNode.getSUID()).set("type", type);
        }
        
        for (CyNode thisNode : inverseNodeProps.keySet()) {
            Collection<Property> props = inverseNodeProps.get(thisNode).values();
            CyActivator.setInverseNodeProps(thisNode, new HashSet<>(props));
        }
        

    }

    private void doQuery(Set<String> wdids) {

        String IDs = String.join(" ", wdids.stream().map(wdid -> "wd:" + wdid).collect(Collectors.toSet()));

        // Get all properties for all selected nodes
        // http://tinyurl.com/h2ovsqj
        String queryString = prefix
                + "SELECT distinct ?item ?itemLabel ?prop ?propLabel ?vals (datatype (?vals) AS ?type) WHERE {\n"
                + "  hint:Query hint:optimizer \"None\" .\n"
                + String.format("  values ?item {%s}\n", IDs)
                + "  ?item ?p ?vals .\n"
                + "  ?prop wikibase:directClaim ?p .\n"
                + "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\" }\n"
                + "}";

        System.out.println(queryString);
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.sparqlService("https://query.wikidata.org/sparql", queryString);
        ResultSet results = qexec.execSelect();
        //ResultSetFormatter.out(System.out, results, query);

        // this is so I can get an element from a set (to update the property's count)
        // http://stackoverflow.com/questions/7283338/getting-an-element-from-a-set
        Map<CyNode, Map<Property, Property>> nodeProps = new HashMap<>();

        while (results.hasNext()) {
            QuerySolution statement = results.next();
            String propLabel = statement.getLiteral("propLabel").getString();
            String itemLabel = statement.getLiteral("itemLabel").getString();
            String prop = statement.getResource("prop").getLocalName();
            String valueWdid = statement.getResource("item").getLocalName();
            Resource typeResource = statement.getResource("type");

            CyNode thisNode = this.nodeWdid.inverse().get(valueWdid);
            String propType = null;

            if (typeResource != null) {
                if ("string".equals(typeResource.getLocalName())) {
                    // This is an ID property. Add it to the table, don't add it to the right-click menu
                    // System.out.println("string statement: " + statement);
                    String value = statement.getLiteral("vals").getString();
                    updateNodeTable(thisNode, propLabel, value);
                    propType = "id";
                } // else ignore it (example: http://www.wikidata.org/entity/P1121)
            } else {
                // The property may still not be wikidata item
                // example http://www.wikidata.org/entity/P117
                String valNameSpace = statement.getResource("vals").getNameSpace();
                if ("http://www.wikidata.org/entity/".equals(valNameSpace)) {
                    // This property links to a wikidata item. We'll add it to the right-click menu
                    propType = "item";
                }
            }
            nodeProps.putIfAbsent(thisNode, new HashMap<>());
            Property p = new Property(propLabel, prop, propType);
            if (nodeProps.get(thisNode).containsKey(p)) {
                nodeProps.get(thisNode).get(p).incrementCount();
            } else {
                nodeProps.get(thisNode).put(p, p);
            }

            // TODO: This could be optimized
            myNet.getDefaultNodeTable().getRow(thisNode.getSUID()).set("name", itemLabel);
        }
        System.out.println("nodeProps: " + nodeProps);

        // make the type column
        CyTable nodeTable = myNet.getDefaultNodeTable();
        if (nodeTable.getColumn("type") == null) {
            nodeTable.createColumn("type", String.class, true);
        }

        // Set the node properties for each node and lookup the node's type based on them
        for (CyNode thisNode : nodeProps.keySet()) {
            Collection<Property> props = nodeProps.get(thisNode).values();

            Set<Property> itemProps = props.stream().filter(p -> "item".equals(p.getType())).collect(Collectors.toSet());
            CyActivator.setNodeProps(thisNode, itemProps);

            Set<Property> idProps = props.stream().filter(p -> "id".equals(p.getType())).collect(Collectors.toSet());
            String type = propertiesToType(idProps);
            myNet.getDefaultNodeTable().getRow(thisNode.getSUID()).set("type", type);
        }
    }

    private String propertiesToType(Set<Property> properties) {
        Set<String> props = properties.stream().map(Property::getID).collect(Collectors.toSet());
        // Accepts a list of properties. Outputs the type
        if (props.contains("P715")) //Drugbank ID
        {
            return "compound";
        }
        if (props.contains("P592")) //ChEMBL ID
        {
            return "compound";
        }
        if (props.contains("P352")) //UniProt ID
        {
            return "protein";
        }
        if (props.contains("P637")) //RefSeq Protein ID
        {
            return "protein";
        }
        if (props.contains("P354")) //HGNC ID
        {
            return "gene";
        }
        if (props.contains("P353")) //HGNC gene symbol
        {
            return "gene";
        }
        if (props.contains("P351")) //Entrez Gene ID
        {
            return "gene";
        }
        if (props.contains("P685")) //NCBI Taxonomy ID
        {
            return "organism";
        }
        if (props.contains("P686")) //Gene Ontology ID
        {
            return "GO";
        }
        if (props.contains("P2926")) //InterPro ID
        {
            return "interpro";
        }
        return "unknown";
    }

    private void updateNodeTable(CyNode node, String propLabel, String value) {
        CyTable nodeTable = myNet.getDefaultNodeTable();
        if (nodeTable.getColumn(propLabel) == null) {
            nodeTable.createColumn(propLabel, String.class, false);
        }

        // If its not a list column, check if the column already has this value set.
        if (nodeTable.getColumn(propLabel).getListElementType() == null) {
            // column is not a list
            String currVal = nodeTable.getRow(node.getSUID()).get(propLabel, String.class);
            if (currVal == null) {
                // column is empty
                nodeTable.getRow(node.getSUID()).set(propLabel, value);
            } else if (currVal.equals(value)) {
                // trying to set same value. Do nothing
            } else {
                // change this to a list and append the new value
                List<String> currValues = nodeTable.getColumn(propLabel).getValues(String.class);
                System.out.println("currValues: " + currValues);
                nodeTable.deleteColumn(propLabel);
                nodeTable.createListColumn(propLabel, String.class, false);

                // re set the values from the other rows
                Iterator<String> iterator = currValues.iterator();
                for (CyRow row : nodeTable.getAllRows()) {
                    row.set(propLabel, new ArrayList<String>());
                    String next = iterator.next();
                    if (next != null) {
                        row.getList(propLabel, String.class).add(next);
                    }
                }

                CyRow thisRow = nodeTable.getRow(node.getSUID());
                //thisRow.set(propLabel, new ArrayList<String>());
                //thisRow.getList(propLabel, String.class).add(currVal);
                thisRow.getList(propLabel, String.class).add(value);
            }
        } else {
            // columns is already a list. Check if the new value is already in the list.
            CyRow thisRow = nodeTable.getRow(node.getSUID());
            if (thisRow.getList(propLabel, String.class) == null) {
                thisRow.set(propLabel, new ArrayList<String>());
            }
            List<String> currList = thisRow.getList(propLabel, String.class);
            if (!currList.contains(value)) {
                thisRow.getList(propLabel, String.class).add(value);
            }
        }
    }

    public TaskIterator createTaskIterator() {
        return new TaskIterator(new NodeLookupTask(nodes, includeReverse));
    }
}
