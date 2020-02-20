/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sidnet.stack.users.csgp.driver;
//import sidnet.stack.users.sample_p2p.driver.*;
//import sidnet.stack.users.sample_p2p.app.*;
import java.util.List;
import jist.swans.misc.Location;
import sidnet.core.misc.NCS_Location2D;
import sidnet.core.misc.Location2D;
import sidnet.core.misc.LocationContext;
import sidnet.core.misc.Node;

/**
 *
 * @author Dewi
 */
public class Cluster {
   public int clusterID, clusterSize;
   public double x;
   public double y;
   
   public List memberList;
   
   Location2D alamat = new Location2D(x,y);
   
   public int setID(int id){
       this.clusterID = id;
       //System.out.println(this.clusterID);
       return clusterID;
   }
   
   public int getID(){
       return clusterID;
   }
   
   public double setLocX(double x){
       this.x =x;
       return this.x;
       
   }
   
   public double setLocY(double y){
       this.y=y;
       return this.y;
   }
   
   public int setClusterSize(int size){
       this.clusterSize = size;
       return this.clusterSize;
   }
   
   public int getClusterSize(){
       return clusterSize;
   }
   
}
