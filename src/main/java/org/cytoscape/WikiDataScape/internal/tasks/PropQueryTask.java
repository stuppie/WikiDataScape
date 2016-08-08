package org.cytoscape.WikiDataScape.internal.tasks;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.cytoscape.WikiDataScape.internal.CyActivator;
import org.cytoscape.WikiDataScape.internal.model.Item;
import org.cytoscape.WikiDataScape.internal.model.Property;
import org.cytoscape.WikiDataScape.internal.model.Triple;
import org.cytoscape.WikiDataScape.internal.model.Triples;
import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;

/**
 * Query the selected nodes using one property.
 * This can theoretically be replaced, because we should have all of this in triples
 * @author gstupp
 */
public class PropQueryTask extends AbstractTask {

    private Property prop;
    private TaskManager taskManager;
    private final Collection<CyNode> nodes;
    private final CyApplicationManager applicationManager;
    private final CyNetworkView myView;
    private final CyNetwork myNet;
    
    public PropQueryTask(Collection<CyNode> nodes, Property prop) {
        this.nodes = nodes;
        this.prop = prop;
        CyAppAdapter adapter = CyActivator.getCyAppAdapter();
        this.taskManager = adapter.getTaskManager();
        this.applicationManager = adapter.getCyApplicationManager();
        this.myView = this.applicationManager.getCurrentNetworkView();
        this.myNet = this.myView.getModel();
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        CyTable nodeTable = myNet.getDefaultNodeTable();
        Set<String> wdids = nodes.stream().map(node -> nodeTable.getRow(node.getSUID()).get("wdid", String.class)).collect(Collectors.toSet());
        String IDs = String.join(" ", wdids.stream().map(wdid -> "wd:" + wdid).collect(Collectors.toSet()));
        
        String prefix = "PREFIX wd: <http://www.wikidata.org/entity/>\n"
            + "PREFIX wdt: <http://www.wikidata.org/prop/direct/>\n"
            + "PREFIX wikibase: <http://wikiba.se/ontology#>\n"
            + "PREFIX p: <http://www.wikidata.org/prop/>\n"
            + "PREFIX ps: <http://www.wikidata.org/prop/statement/>\n"
            + "PREFIX pq: <http://www.wikidata.org/prop/qualifier/>\n"
            + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
            + "PREFIX bd: <http://www.bigdata.com/rdf#>\n";
        
        String nodePropId = prop.getID();
        String queryString = prefix
                + "SELECT ?item ?itemLabel ?value ?valueLabel WHERE {\n"
                + "  ?item wdt:%s ?value .\n"
                + "  values ?item {%s}\n"
                + "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\" }\n"
                + "}";

        queryString = String.format(queryString, nodePropId, IDs);
        
        doQuery(queryString, prop);
        
        /*// set node visual styles
        SetVisualStyleTask setVisualStyleTask = new SetVisualStyleTask();
        taskManager.execute(setVisualStyleTask.createTaskIterator());*/
    }

    private void doQuery(String queryString, Property prop) {
        System.out.println(queryString);
        Triples triples = new Triples();
        QueryExecution qexec = QueryExecutionFactory.sparqlService("https://query.wikidata.org/sparql", queryString);
        ResultSet results = qexec.execSelect();
        //ResultSetFormatter.out(System.out, results, query);

        while (results.hasNext()) {
            QuerySolution statement = results.next();
            System.out.println("statement: " + statement.toString());
            String wdid = statement.getResource("item").getLocalName();
            String name = statement.getLiteral("itemLabel").getString();
            Item subj = new Item(name, wdid);
            try {
                String valueWdid = statement.getResource("value").getLocalName();
                String valueLabel = statement.getLiteral("valueLabel").getString();
                Item obj = new Item(valueLabel, valueWdid);
                triples.addTriple(new Triple(subj, prop, obj));
            } catch (java.lang.ClassCastException classCastException) {
                System.out.println("fghjkl;");
            }
        }
        System.out.println("triples: " + triples);

        TransformTask_1 transformTask = new TransformTask_1(triples);
        taskManager.execute(transformTask.createTaskIterator());

    }

    public TaskIterator createTaskIterator() {
        System.out.println("createTaskIterator");
        return new TaskIterator(new PropQueryTask(nodes, prop));
    }
}
