package org.cytoscape.WikiDataScape.internal.model;

/**
 * Represents a wikidata property
 *
 * @author gstupp
 */
public class Property implements Comparable {

    private String name = null; // ex: "KEGG ID"
    private String id = null; // ex: "P665"
    private String type = null;

    public Property(String name, String id, String type) {
        this.name = name;
        this.id = id;
        this.type = type;
    }

    public String getID() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getName(Integer count) {
        return this.name + " (" + count + ")";
    }

    public String getType() {
        return this.type;
    }

    @Override
    public String toString() {
        return "<" + this.name + " (" + this.id + ")" + ">";
    }

    /*    
    @Override
    public String toString() {
        return "<" + this.name + " (" + this.id + "): " + this.count + ">";
    }
     */
    @Override
    public boolean equals(Object otherObject) {
        // check for reference equality.
        if (this == otherObject) {
            return true;
        }
        if (otherObject instanceof Property) {
            Property that = (Property) otherObject;
            // Check for name equality.
            return (id == null && that.id == null) || id.equals(that.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = this.id.hashCode();
        // hash = hash * 31 + this.id.hashCode();
        return hash;
    }

    @Override
    public int compareTo(Object o) {
        return (this.getName().compareToIgnoreCase(((Property) o).getName()));
    }

}
