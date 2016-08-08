package org.cytoscape.WikiDataScape.internal.model;

/**
 * Represents a wikidata triple subject predicate object
 *
 * @author gstupp
 */
public class Triple {

    private Item subject = null; // ex: "KEGG ID"
    private Property predicate = null;
    private Item object = null;

    public Triple(Item subject, Property predicate, Item object) {
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
    }

    public Item getSubject() {
        return this.subject;
    }

    public Property getPredicate() {
        return this.predicate;
    }

    public Item getObject() {
        return this.object;
    }

    @Override
    public String toString() {
        return "{" + this.subject + "<->" + this.predicate + "<->" + this.object + "}";
    }

    @Override
    public boolean equals(Object otherObject) {
        // check for reference equality.
        if (this == otherObject) {
            return true;
        }
        if (otherObject instanceof Triple) {
            Triple other = (Triple) otherObject;
            return (this.getSubject().equals(other.getSubject()) && this.getPredicate().equals(other.getPredicate())
                    && this.getObject().equals(other.getObject()));
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int hash = this.subject.hashCode();
        hash = hash * 31 + this.predicate.hashCode();
        hash = hash * 31 + this.object.hashCode();
        return hash;
    }
}
