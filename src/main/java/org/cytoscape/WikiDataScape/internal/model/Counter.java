package org.cytoscape.WikiDataScape.internal.model;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;



public class Counter<T> implements Iterable<T>{
    // http://stackoverflow.com/questions/28254447/is-there-a-scala-java-equivalent-of-python-3s-collections-counter
    final Map<T, Integer> counts = new HashMap<>();

    public void add(T t) {
        counts.merge(t, 1, Integer::sum);
    }

    public int count(T t) {
        return counts.getOrDefault(t, 0);
    }

    @Override
    public String toString() {
        return counts.toString();
    }

    @Override
    public Iterator<T> iterator() {
        return counts.keySet().iterator();
    }

}