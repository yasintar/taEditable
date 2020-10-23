/*
 * NodeEntry.java
 *
 * Created on November 3, 2005, 6:15 PM
 */

package sidnet.core.misc;

import java.util.ArrayList;
import java.util.List;
import jist.swans.net.NetAddress;
import jist.swans.mac.MacAddress;
import sidnet.core.misc.Node;
import sidnet.models.energy.energyconsumptionmodels.EnergyManagement;

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
        private EnergyManagement energyManagement;
        /** Physical Location of neighbour */
        private NCS_Location2D loc;
        public double distToCluster;
        public double clusterId;
        public double battery;
        public int jumlahTetangga;
        public boolean status1, status2, status3;
        public boolean flagCH;
        
    public NodeEntry(MacAddress mac, NetAddress ip, NCS_Location2D loc)
    {
        this.mac = mac;
        this.ip  = ip;
        this.loc = loc;
    }
        
    public NodeEntry(MacAddress mac, NetAddress ip, NCS_Location2D loc, double distToCluster, double clusterId)
    {
        this.distToCluster= distToCluster;
        this.clusterId= clusterId;
        this.mac = mac;
        this.ip  = ip;
        this.loc = loc;
    }
        
        //modif by zhy
    public NodeEntry(MacAddress mac, NetAddress ip, NCS_Location2D loc, double distToCluster, double clusterId, double battery)
    {
        this.mac = mac;
        this.ip  = ip;
        this.loc = loc;
        this.distToCluster= distToCluster;
        this.clusterId= clusterId;
        this.battery= battery;
    }
    
    public NodeEntry(MacAddress mac, NetAddress ip, NCS_Location2D loc, double distToCluster, double clusterId, double battery, int jumlahTetangga)
    {
        this.mac = mac;
        this.ip  = ip;
        this.loc = loc;
        this.distToCluster= distToCluster;
        this.clusterId= clusterId;
        this.battery=battery;
        this.jumlahTetangga=jumlahTetangga;
    }
       
    
//    public NodeEntry(double sisaBattery)
//    {
//        this.sisaBattery= sisaBattery;
//    }
      
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
    
        public boolean getStatus3(){
        return this.status3;
    }
      
    public void setStatus3(){
        this.status3=true;
    }
    
        
    public void setNCS_Location2D(NCS_Location2D loc)
    {
        this.loc = loc;
    }
        
    public NCS_Location2D getNCS_Location2D()
    {
        return loc;
    }
        
    public EnergyManagement getEnergyManagement()
    {
        return energyManagement;
    }
}
