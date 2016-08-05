package org.cytoscape.WikiDataScape.internal;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;

/**
 *
 * @author gstupp
 */
public class PropQueryTask extends AbstractTask {

    private String queryString;
    private String interaction;
    private TaskManager taskManager;

    public PropQueryTask(String queryString, String interaction) {
        this.queryString = queryString;
        this.interaction = interaction;
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        CyAppAdapter adapter = CyActivator.getCyAppAdapter();
        this.taskManager = adapter.getTaskManager();
        doQuery(queryString, interaction);
    }

    private void doQuery(String queryString, String interaction) {
        System.out.println(queryString);
        Map<Item, List<Item>> valueProt = new HashMap<>();
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.sparqlService("https://query.wikidata.org/sparql", queryString);
        ResultSet results = qexec.execSelect();
        //ResultSetFormatter.out(System.out, results, query);

        while (results.hasNext()) {
            QuerySolution statement = results.next();
            System.out.println("statement: " + statement.toString());
            String wdid = statement.getResource("item").getLocalName();
            String name = statement.getLiteral("itemLabel").getString();
            Item item = new Item(name, "", wdid);
            String valueWdid;
            String type;
            // We don't know if the item is a wikidata item or a string. So try both
            try {
                valueWdid = statement.getResource("value").getLocalName();
                type = "item";
            } catch (java.lang.ClassCastException classCastException) {
                try {
                    //valueWdid = statement.getLiteral("value").getString();
                    valueWdid = ""; // this is an ID property. no wikidata ID
                    type = "string";
                } catch (java.lang.ClassCastException classCastException2) {
                    System.out.println("error");
                    valueWdid = "error";
                    type = "unknown";
                }
            }
            String valueLabel = statement.getLiteral("valueLabel").getString();
            Item linkedItem = new Item(valueLabel, type, valueWdid);
            if (!valueProt.containsKey(linkedItem)) {
                valueProt.put(linkedItem, new ArrayList<Item>());
            }
            valueProt.get(linkedItem).add(item);
        }
        System.out.println("valueProt: " + valueProt);

        TransformTask transformTask = new TransformTask(valueProt, interaction);
        taskManager.execute(transformTask.createTaskIterator());

    }

    public TaskIterator createTaskIterator() {
        System.out.println("createTaskIterator");
        return new TaskIterator(new PropQueryTask(queryString, interaction));
    }
}
