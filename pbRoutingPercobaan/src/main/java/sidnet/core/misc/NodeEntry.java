/*
 * NodeEntry.java
 *
 * Created on November 3, 2005, 6:15 PM
 */

package sidnet.core.misc;

import jist.swans.net.NetAddress;
import jist.swans.mac.MacAddress;

/**
 *
 * @author  Oliviu Ghica
 */
    /**
    * Neighbour entry information.
    */
    public class NodeEntry 
    {
        /** mac address of neighbour. */
        public MacAddress mac;
        /** IP address of neighbour   */
        public NetAddress ip;
        /** Physical Location of neighbour */
        private NCS_Location2D loc;
        public double distToCluster;
        public int clusterId;
        public boolean status1, status2;
        public double battery;
       
        
        //override
        public NodeEntry(MacAddress mac, NetAddress ip, NCS_Location2D loc,double distToCluster,int clusterId)
        {
            this.distToCluster = distToCluster;
            this.clusterId = clusterId;
            this.mac = mac;
            this.ip  = ip;
            this.loc = loc;
        }
         public NodeEntry(MacAddress mac, NetAddress ip, NCS_Location2D loc)
        {
            this.distToCluster = distToCluster;
            this.clusterId = clusterId;
            this.mac = mac;
            this.ip  = ip;
            this.loc = loc;
        }
        public boolean getStatus1(){
            return this.status1;
        }
        public void setStatus1(){
            this.status1=true;
        }
         public boolean getStatus2(){
            return this.status2;
        }
        public void setStatus2(){
            this.status2=true;
        }
        public void setNCS_Location2D(NCS_Location2D loc)
        {
            this.loc = loc;
        }
        
        public NCS_Location2D getNCS_Location2D()
        {
            return loc;
        }
    }
