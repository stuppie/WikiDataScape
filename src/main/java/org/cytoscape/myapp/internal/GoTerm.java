
package org.cytoscape.myapp.internal;

/**
 *
 * @author gstupp
 */
public class GoTerm extends Item {
    public String x = "GO";
    
    
    GoTerm() {
      type = "GO";
    }
    
    GoTerm(String name, String id, String wdid) {
        this.name=name;
        this.id=id;
        this.wdid=wdid;
        type = "GO";
    }
    
    public static void main(String args[]) {
       GoTerm rect = new GoTerm("name", "id", "wdid");
        System.out.println(rect.toString());
   }
   
}

   
