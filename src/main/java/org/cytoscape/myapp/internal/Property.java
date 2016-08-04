
package org.cytoscape.myapp.internal;

/**
 * Represents a wikidata property
 * @author gstupp
 */
public class Property {

    private String name = null; // ex: "KEGG ID"
    private String id = null; // ex: "P665"
    private Integer count = null; // the number of items this property links to

    public Property(String name, String id) {
        this.name = name;
        this.id = id;
        this.count = 1;
    }
    
    public String getID(){
        return this.id;
    }
    public String getName() {
        return this.name;
    }
    public String getName(boolean withCount) {
        if (withCount)
            return this.name + " (" + this.count + ")";
        else
            return this.name;
    }
    public Integer getCount() {
        return this.count;
    }
    public void incrementCount(){
        this.count += 1;
    }
    
    
    @Override
    public String toString() {
        return "<" + this.name + " (" + this.id + "): " + this.count + ">";
    }

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

}