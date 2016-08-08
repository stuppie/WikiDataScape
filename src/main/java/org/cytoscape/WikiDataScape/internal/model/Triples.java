package org.cytoscape.WikiDataScape.internal.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.cytoscape.model.CyNode;

/**
 * Stores a collection of triples.
 *
 * @author gstupp
 */
public class Triples {

    private Map<Item, HashSet<Triple>> triples = new HashMap<>();

    public Triples() {

    }

    public Triples(HashSet<Triple> newTriples) {
        this.addTriples(newTriples);
    }

    public HashSet<Triple> getTriplesFlat() {
        HashSet<Triple> tripleSet = new HashSet<>();
        triples.values().forEach(tripleSet::addAll);
        return tripleSet;
    }

    public void addTriples(Triples newTriples) {
        this.addTriples(newTriples.getTriplesFlat());
    }

    public void addTriples(HashSet<Triple> newTriples) {
        for (Triple triple : newTriples) {
            this.addTriple(triple);
        }
    }

    public void addTriple(Triple triple) {
        Item subject = triple.getSubject();
        triples.putIfAbsent(subject, new HashSet<>());
        triples.get(subject).add(triple);
    }

    public Triples getSubjectTriples(Collection<Item> subjects) {
        Triples newTriples = new Triples();
        for (Item subj : subjects) {
            newTriples.addTriples(triples.getOrDefault(subj, new HashSet<>()));
        }
        return newTriples;
    }

    public Triples getSubjectTriples(Item subject) {
        HashSet<Triple> orDefault = triples.getOrDefault(subject, new HashSet<>());
        return new Triples(orDefault);
    }

    public HashSet<Item> getSubjects() {
        return new HashSet<Item>(triples.keySet());
    }

    public Triples getTriplesWithProperty(Property prop) {
        // Get all triples with the associated propert prop
        Triples newTriples = new Triples();
        for (Triple triple : this.getTriplesFlat()) {
            if (triple.getPredicate().equals(prop)) {
                newTriples.addTriple(triple);
            }
        }

        return newTriples;
    }

    public Counter getSubjectProperties(Item subject) {
        // For a subject, get the properties that are associated with it
        HashSet<Triple> subjTriples = triples.get(subject);
        Counter counter = new Counter();
        for (Triple triple : subjTriples) {
            counter.add(triple.getPredicate());
        }
        return counter;
    }

    public Counter getSubjectProperties(Collection<Item> subject) {
        Triples subjectTriples = getSubjectTriples(subject);
        Counter counter = new Counter();
        for (Triple triple : subjectTriples.getTriplesFlat()) {
            counter.add(triple.getPredicate());
        }
        return counter;
    }
    
    
    // For reverse queries
    public Triples getObjectTriples(Item object) {
        Triples newTriples = new Triples();
        for (Triple triple : this.getTriplesFlat())
            if (triple.getObject().equals(object))
                newTriples.addTriple(triple);
        return newTriples;
    }
    
    public Triples getObjectTriples(Collection<Item> objects) {
        Triples newTriples = new Triples();
        for (Item obj : objects) {
            newTriples.addTriples(this.getObjectTriples(obj));
        }
        return newTriples;
    }
    
    public Counter getObjectProperties(Collection<Item> objects) {
        Triples objectTriples = getObjectTriples(objects);
        Counter counter = new Counter();
        for (Triple triple : objectTriples.getTriplesFlat()) {
            counter.add(triple.getPredicate());
        }
        return counter;
    }

    @Override
    public String toString() {
        return triples.toString();
    }

}
