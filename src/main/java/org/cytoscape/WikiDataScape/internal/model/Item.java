
package org.cytoscape.WikiDataScape.internal.model;

/**
 * Generic class representing a wikidata item, which is displayed in cytoscape as a node
 * @author gstupp
 */
public class Item {

    public String name = null;
    public String type = null;
    public String wdid = null;

    public Item(String name, String wdid) {
        this.name = name;
        this.wdid = wdid;
    }
    public Item(){

    }

    public String getName() {
        return this.name;
    }
    public String getWdid() {
        return this.wdid;
    }
    
    public String getType() {
        return this.type;
    }

    @Override
    public String toString() {
        return "[" + this.name + " (" + this.wdid + ")]";
    }

    @Override
    public boolean equals(Object otherObject) {
        // check for reference equality.
        if (this == otherObject) {
            return true;
        }
        if (otherObject instanceof Item) {
            Item that = (Item) otherObject;
            // Check for name equality.
            return (wdid == null && that.wdid == null) || wdid.equals(that.wdid);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = this.wdid.hashCode();
        // hash = hash * 31 + this.id.hashCode();
        return hash;
    }

}
