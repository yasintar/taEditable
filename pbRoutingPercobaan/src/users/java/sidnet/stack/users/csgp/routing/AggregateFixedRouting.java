/*
 * ShortestGeographicalPathRouting.java
 *
 * Created on April 15, 2008, 11:43 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package sidnet.stack.users.csgp.routing;

import java.util.LinkedList;
import jist.runtime.JistAPI;
import jist.swans.Constants;
import sidnet.core.interfaces.AppInterface;
import jist.swans.mac.MacAddress;
import jist.swans.misc.Message;
import jist.swans.net.NetAddress;
import jist.swans.net.NetInterface;
import jist.swans.net.NetMessage;
import jist.swans.route.RouteInterface;
import sidnet.stack.users.csgp.colorprofile.CSGPColorProfile;
import sidnet.core.misc.Location2D;
import sidnet.core.misc.Node;
import sidnet.core.misc.NodeEntry;
import sidnet.core.misc.Reason;
import sidnet.core.misc.Region;

import sidnet.stack.users.csgp.app.MessageQuery;
import sidnet.stack.users.csgp.app.MessageDataValue;

/**
 *
 * @author Oliviu C. Ghica, Northwestern University
 */
public class AggregateFixedRouting implements RouteInterface {
    public static final byte ERROR = -1;
    public static final byte SUCCESS = 0;

    private final Node myNode; // The SIDnet handle to the node representation 
    
    // entity hook-up (network stack)
    /** Network entity. */
    private NetInterface netEntity;
   
    /** Self-referencing proxy entity. */
    private RouteInterface self; 
    
    /** The proxy-entity for this application interface */
    private AppInterface appInterface;
    
    // DO NOT MAKE THIS STATIC
    private CSGPColorProfile colorProfileGeneric = new CSGPColorProfile();

    //fixing routing variable
    private String up, leaves1, leaves2;
    
    /** Creates a new instance of ShortestGeographicalPathRouting
     *
     * @param Node    the SIDnet node handle to access 
     * 				  its GUI-primitives and shared environment
     */
    public AggregateFixedRouting(Node myNode) {
        this.myNode = myNode;
        
        /** Create a proxy for the application layer of this node */
        self = (RouteInterface)JistAPI.proxy(this, RouteInterface.class);

        //creating fixing route
        if (myNode.getID() == 0) {this.up = "NULL"; this.leaves1 = "1"; this.leaves2 = "2";}
        else if (myNode.getID() == 1) {this.up = "0"; this.leaves1 = "3"; this.leaves2 = "4";}
        else if (myNode.getID() == 2) {this.up = "0"; this.leaves1 = "5"; this.leaves2 = "6";}
        else if (myNode.getID() == 3) {this.up = "1"; this.leaves1 = "7"; this.leaves2 = "8";}
        else if (myNode.getID() == 4) {this.up = "1"; this.leaves1 = "9"; this.leaves2 = "10";}
        else if (myNode.getID() == 5) {this.up = "2"; this.leaves1 = "11"; this.leaves2 = "12";}
        else if (myNode.getID() == 6) {this.up = "2"; this.leaves1 = "13"; this.leaves2 = "14";}
        else if (myNode.getID() == 7) {this.up = "3"; this.leaves1 = "15"; this.leaves2 = "16";}
        else if (myNode.getID() == 8) {this.up = "3"; this.leaves1 = "17"; this.leaves2 = "18";}
        else if (myNode.getID() == 9) {this.up = "4"; this.leaves1 = "19"; this.leaves2 = "20";}
        else if (myNode.getID() == 10) {this.up = "4"; this.leaves1 = "21"; this.leaves2 = "22";}
        else if (myNode.getID() == 11) {this.up = "5"; this.leaves1 = "23"; this.leaves2 = "24";}
        else if (myNode.getID() == 12) {this.up = "5"; this.leaves1 = "25"; this.leaves2 = "26";}
        else if (myNode.getID() == 13) {this.up = "6"; this.leaves1 = "27"; this.leaves2 = "28";}
        else if (myNode.getID() == 14) {this.up = "6"; this.leaves1 = "29"; this.leaves2 = "30";}
        else if (myNode.getID() == 15) {this.up = "7"; this.leaves1 = "31"; this.leaves2 = "32";}
        else if (myNode.getID() == 16) {this.up = "7"; this.leaves1 = "33"; this.leaves2 = "34";}
        else if (myNode.getID() == 17) {this.up = "8"; this.leaves1 = "35"; this.leaves2 = "36";}
        else if (myNode.getID() == 18) {this.up = "8"; this.leaves1 = "37"; this.leaves2 = "38";}
        else if (myNode.getID() == 19) {this.up = "9"; this.leaves1 = "39"; this.leaves2 = "40";}
        else if (myNode.getID() == 20) {this.up = "9"; this.leaves1 = "41"; this.leaves2 = "42";}
        else if (myNode.getID() == 21) {this.up = "10"; this.leaves1 = "43"; this.leaves2 = "44";}
        else if (myNode.getID() == 22) {this.up = "10"; this.leaves1 = "45"; this.leaves2 = "46";}
        else if (myNode.getID() == 23) {this.up = "11"; this.leaves1 = "47"; this.leaves2 = "48";}
        else if (myNode.getID() == 24) {this.up = "11"; this.leaves1 = "49"; this.leaves2 = "50";}
        else if (myNode.getID() == 25) {this.up = "12"; this.leaves1 = "51"; this.leaves2 = "52";}
        else if (myNode.getID() == 26) {this.up = "12"; this.leaves1 = "53"; this.leaves2 = "54";}
        else if (myNode.getID() == 27) {this.up = "13"; this.leaves1 = "55"; this.leaves2 = "56";}
        else if (myNode.getID() == 28) {this.up = "13"; this.leaves1 = "57"; this.leaves2 = "58";}
        else if (myNode.getID() == 29) {this.up = "14"; this.leaves1 = "59"; this.leaves2 = "60";}
        else if (myNode.getID() == 30) {this.up = "14"; this.leaves1 = "61"; this.leaves2 = "62";}
        else if (myNode.getID() == 31) {this.up = "15"; this.leaves1 = "NULL"; this.leaves2 = "NULL";}
        else if (myNode.getID() == 32) {this.up = "15"; this.leaves1 = "NULL"; this.leaves2 = "NULL";}
        else if (myNode.getID() == 33) {this.up = "16"; this.leaves1 = "NULL"; this.leaves2 = "NULL";}
        else if (myNode.getID() == 34) {this.up = "16"; this.leaves1 = "NULL"; this.leaves2 = "NULL";}
        else if (myNode.getID() == 35) {this.up = "17"; this.leaves1 = "NULL"; this.leaves2 = "NULL";}
        else if (myNode.getID() == 36) {this.up = "17"; this.leaves1 = "NULL"; this.leaves2 = "NULL";}
        else if (myNode.getID() == 37) {this.up = "18"; this.leaves1 = "NULL"; this.leaves2 = "NULL";}
        else if (myNode.getID() == 38) {this.up = "18"; this.leaves1 = "NULL"; this.leaves2 = "NULL";}
        else if (myNode.getID() == 39) {this.up = "19"; this.leaves1 = "NULL"; this.leaves2 = "NULL";}
        else if (myNode.getID() == 40) {this.up = "19"; this.leaves1 = "NULL"; this.leaves2 = "NULL";}
        else if (myNode.getID() == 41) {this.up = "20"; this.leaves1 = "NULL"; this.leaves2 = "NULL";}
        else if (myNode.getID() == 42) {this.up = "20"; this.leaves1 = "NULL"; this.leaves2 = "NULL";}
        else if (myNode.getID() == 43) {this.up = "21"; this.leaves1 = "NULL"; this.leaves2 = "NULL";}
        else if (myNode.getID() == 44) {this.up = "21"; this.leaves1 = "NULL"; this.leaves2 = "NULL";}
        else if (myNode.getID() == 45) {this.up = "22"; this.leaves1 = "NULL"; this.leaves2 = "NULL";}
        else if (myNode.getID() == 46) {this.up = "22"; this.leaves1 = "NULL"; this.leaves2 = "NULL";}
        else if (myNode.getID() == 47) {this.up = "23"; this.leaves1 = "NULL"; this.leaves2 = "NULL";}
        else if (myNode.getID() == 48) {this.up = "23"; this.leaves1 = "NULL"; this.leaves2 = "NULL";}
        else if (myNode.getID() == 49) {this.up = "24"; this.leaves1 = "NULL"; this.leaves2 = "NULL";}
        else if (myNode.getID() == 50) {this.up = "24"; this.leaves1 = "NULL"; this.leaves2 = "NULL";}
        else if (myNode.getID() == 51) {this.up = "25"; this.leaves1 = "NULL"; this.leaves2 = "NULL";}
        else if (myNode.getID() == 52) {this.up = "25"; this.leaves1 = "NULL"; this.leaves2 = "NULL";}
        else if (myNode.getID() == 53) {this.up = "26"; this.leaves1 = "NULL"; this.leaves2 = "NULL";}
        else if (myNode.getID() == 54) {this.up = "26"; this.leaves1 = "NULL"; this.leaves2 = "NULL";}
        else if (myNode.getID() == 55) {this.up = "27"; this.leaves1 = "NULL"; this.leaves2 = "NULL";}
        else if (myNode.getID() == 56) {this.up = "27"; this.leaves1 = "NULL"; this.leaves2 = "NULL";}
        else if (myNode.getID() == 57) {this.up = "28"; this.leaves1 = "NULL"; this.leaves2 = "NULL";}
        else if (myNode.getID() == 58) {this.up = "28"; this.leaves1 = "NULL"; this.leaves2 = "NULL";}
        else if (myNode.getID() == 59) {this.up = "29"; this.leaves1 = "NULL"; this.leaves2 = "NULL";}
        else if (myNode.getID() == 60) {this.up = "29"; this.leaves1 = "NULL"; this.leaves2 = "NULL";}
        else if (myNode.getID() == 61) {this.up = "30"; this.leaves1 = "NULL"; this.leaves2 = "NULL";}
        else if (myNode.getID() == 62) {this.up = "30"; this.leaves1 = "NULL"; this.leaves2 = "NULL";}
    }
    
    /** SWANS legacy. We no longer enable this */
    public void peek(NetMessage msg, MacAddress lastHopMac) {
       // no peeking
    }
    
    /** 
     *  Receive a message from the network layer
     *  This method is typically called when this node is the ultimate 
     *  	destination of an incoming data-message (the sink) 
     * 
     * @param Message   the incomming message
     * @param NetAddress the original source of the message
     * @param MacAddress the MAC address 1-hop neighbor from which this 
     * 					 nodes received this message
     * @param macId     the macId interface through which 
     * 					this message was received
     * @param NetAddress the IP address of the ultimate node destination (this)
     * @param priority  the priority of the incoming message
     * @param ttl    Time To Leave
     */
    public void receive(Message msg, 
    					NetAddress src, 
    					MacAddress lastHop, 
    					byte macId, NetAddress dst, byte priority, byte ttl) 
    {  
        if (myNode.getEnergyManagement()
        		  .getBattery()
        		  .getPercentageEnergyLevel()< 2)
            return;
        
        // Provide a basic visual feedback on the fact that 
        //this node has received a message */
        myNode.getNodeGUI().colorCode.mark(colorProfileGeneric,
        								   CSGPColorProfile.RECEIVE, 20);
        
        // A message may come in a format that you define based
        // on your implementation needs
        // You must extract that format and act upon
        if (msg instanceof CSGPWrapperMessage) {
        	CSGPWrapperMessage msgNZ = (CSGPWrapperMessage)msg;
            sendToAppLayer(msgNZ.getPayload(), null);
        }
         
        // otherwise, it is a format not recognized by SGP, so we ignore it.
    }
    
     /**
      * Send a message
      * This method is being called when a message, 
      * coming from either the application layer or the mac layer,
      * needs to be forwarded
      *
      * @param  NetMessage  the 'NetMessage' wrapped Message
      */
     public void send(NetMessage msg) 
     {        
    	// process only if there are energy reserves
        if (myNode.getEnergyManagement()
        		  .getBattery()
        		  .getPercentageEnergyLevel()< 2)
            return;
        
        // update visuals        
    	myNode.getNodeGUI().colorCode.mark(colorProfileGeneric,
    									   CSGPColorProfile.RECEIVE, 2000);
      
        // If this message comes from App Layer
        if (!(((NetMessage.Ip)msg).getPayload() instanceof CSGPWrapperMessage))
        	return; // ignore non-specific messages
        
        // extract message
        Location2D targetLocation = null;
        CSGPWrapperMessage msgSGP
        	= (CSGPWrapperMessage)((NetMessage.Ip)msg).getPayload();

        if (msgSGP.getPayload() instanceof MessageQuery) {
            System.out.println("Node " + myNode.getID() + " get query message");
            handleQueryMessage(msg);
        } else if (msgSGP.getPayload() instanceof MessageDataValue) {
            System.out.println("Node " + myNode.getID() + " get data value message");
            handleMessageDataValue(msg);
        } else {
            System.out.println("Node " + myNode.getID() + " get unknown message");
        }

     }

     /*
      * Jika mendapatkan pesan query, lakukan broadcasting ke leave
      * (unicast pesan ke masing-masing leaves) asumsi 1 nodes 2 leaves
      * kemudian lempar ke app layer
      */
     private void handleQueryMessage(NetMessage msg) {
         CSGPWrapperMessage msgSGP
    	 	= (CSGPWrapperMessage)((NetMessage.Ip)msg).getPayload();

         //jika ini bukan node terakhir lanjut sebarkan
         if (this.leaves1 != "NULL") {

            NetAddress nextHopIP1 = getIPAddressFromMac(leaves1);
            NetAddress nextHopIP2 = getIPAddressFromMac(leaves2);

            NetMessage.Ip copyOfMsg1
              	= new NetMessage.Ip(msgSGP,
		       ((NetMessage.Ip)msg).getSrc(),
                       ((NetMessage.Ip)msg).getDst(),
                       ((NetMessage.Ip)msg).getProtocol(),
                       ((NetMessage.Ip)msg).getPriority(),
                       ((NetMessage.Ip)msg).getTTL(),
                       ((NetMessage.Ip)msg).getId(),
                       ((NetMessage.Ip)msg).getFragOffset());

            NetMessage.Ip copyOfMsg2
              	= new NetMessage.Ip(msgSGP,
		       ((NetMessage.Ip)msg).getSrc(),
                       ((NetMessage.Ip)msg).getDst(),
                       ((NetMessage.Ip)msg).getProtocol(),
                       ((NetMessage.Ip)msg).getPriority(),
                       ((NetMessage.Ip)msg).getTTL(),
                       ((NetMessage.Ip)msg).getId(),
                       ((NetMessage.Ip)msg).getFragOffset());

             System.out.println("Node " + myNode.getID() + " forwarding query msg to node " + leaves1 + ", " + leaves2);
             sendToLinkLayer(copyOfMsg1, nextHopIP1);
             sendToLinkLayer(copyOfMsg2, nextHopIP2);
         }

         //lempar ke layer APP untuk sensing lokasi node ini juga
         //jika ini bukan node sink (asumsi sink tidak sensing)
         if (this.myNode.getID() != 0)
            sendToAppLayer(msgSGP.getPayload(), null);
     }
     
     private NetAddress getIPAddressFromMac(String MacAddress) {
        NetAddress nextNode = myNode.getIP();
        LinkedList<NodeEntry> neighboursLinkedList 
        	= myNode.neighboursList.getAsLinkedList();
        
        for(NodeEntry nodeEntry: neighboursLinkedList) {
            if (nodeEntry.mac.toString().equals(MacAddress)) {
                nextNode = nodeEntry.ip;
            }
        }
        
        return nextNode;
     }

     private void handleMessageDataValue(NetMessage msg) {
         CSGPWrapperMessage msgSGP
    	 	= (CSGPWrapperMessage)((NetMessage.Ip)msg).getPayload();

         //jika bukan sink node, lanjutkan pesan
         if (this.up != "NULL") {
             NetAddress nextHopIP = getIPAddressFromMac(this.up);

             NetMessage.Ip copyOfMsg
                    = new NetMessage.Ip(msgSGP,
                           ((NetMessage.Ip)msg).getSrc(),
                           ((NetMessage.Ip)msg).getDst(),
                           ((NetMessage.Ip)msg).getProtocol(),
                           ((NetMessage.Ip)msg).getPriority(),
                           ((NetMessage.Ip)msg).getTTL(),
                           ((NetMessage.Ip)msg).getId(),
                           ((NetMessage.Ip)msg).getFragOffset());

             sendToLinkLayer(copyOfMsg, nextHopIP);
         } else { //sink node
             sendToAppLayer(msgSGP.getPayload(), null);
         }
     }

     private void handleWithTargetLocation(Location2D targetLocation,
    		 								NetMessage msg) {
    	 CSGPWrapperMessage msgSGP
    	 	= (CSGPWrapperMessage)((NetMessage.Ip)msg).getPayload();
    	 
    	 // Retrieve the IP address of the 1-hop neighbor
    	 // closest to the area of interest */
         NetAddress nextHopIP = getThroughShortestPath(targetLocation);
         
    	 // If there is no node closer to the area of interest than this node, 
    	 // then this node will get the message
         if (nextHopIP.hashCode() == myNode.getIP().hashCode())
        	 sendToAppLayer(msgSGP.getPayload(), null);
         else { // keep forwarding
        	  // first, make a copy of the message
              NetMessage.Ip copyOfMsg 
              	= new NetMessage.Ip(msgSGP,
					   ((NetMessage.Ip)msg).getSrc(),
                       ((NetMessage.Ip)msg).getDst(),
                       ((NetMessage.Ip)msg).getProtocol(),
                       ((NetMessage.Ip)msg).getPriority(), 
                       ((NetMessage.Ip)msg).getTTL(),
                       ((NetMessage.Ip)msg).getId(),
                       ((NetMessage.Ip)msg).getFragOffset());
              
             sendToLinkLayer(copyOfMsg, nextHopIP);
         }
     }
     
     /** Find the closest vertex. 
	  * A query may contain a region of interest.
	  * Since this is SGP routing, 
	  * we look for the closest vertex of that region 
	  */
     private Location2D extractClosestVertex(Region targetRegion) {    
         targetRegion.resetIterator();
             
         Location2D nextLoc 
         	= targetRegion.getNext()
         				  .convertTo(targetRegion.getLocationContext(),
         						     myNode.getLocationContext());;
         Location2D closestVertex = nextLoc;
            
         double distMin 
         	= nextLoc.distanceTo(myNode.getLocation2D());
             
         while (targetRegion.hasNext()) {
             nextLoc = targetRegion.getNext();
             Location2D actualLoc 
             	= nextLoc.convertTo(targetRegion.getLocationContext(),
             						myNode.getLocationContext());
             if (actualLoc.distanceTo(myNode.getLocation2D()) < distMin) {
                 distMin = actualLoc.distanceTo(myNode.getLocation2D());
                 closestVertex = actualLoc;
             }
         }         
         return closestVertex;
     }
    
     
    public void sendToAppLayer(Message msg, NetAddress src)
    {
    	// ignore if not enough energy
        if (myNode.getEnergyManagement()
        		  .getBattery()
        		  .getPercentageEnergyLevel()< 2)
            return;

        appInterface.receive(msg, src, null, (byte)-1,
        					 NetAddress.LOCAL, (byte)-1, (byte)-1);
    }
    
    public byte sendToLinkLayer(NetMessage.Ip ipMsg, NetAddress nextHopDestIP)
    {
        if (myNode.getEnergyManagement()
        		  .getBattery()
        		  .getPercentageEnergyLevel()< 2)
            return 0;
     
        /*myNode.getSimManager().getSimGUI()
		  .getAnimationDrawingTool()
		 	  .animate("ExpandingFadingCircle",
				       myNode.getNCS_Location2D());*/         
        
        if (myNode.getID() == 164)
            System.out.println("route packet to " + nextHopDestIP);

        if (nextHopDestIP == null)
            System.err.println("NULL nextHopDestIP");
        if (nextHopDestIP == NetAddress.ANY)
            netEntity.send(ipMsg, Constants.NET_INTERFACE_DEFAULT, MacAddress.ANY);
        else
        {
            NodeEntry nodeEntry = myNode.neighboursList.get(nextHopDestIP);
            if (nodeEntry == null)
            {
                 System.err.println("Node #" + myNode.getID() + ": Destination IP (" + nextHopDestIP + ") not in my neighborhood. Please re-route! Are you sending the packet to yourself?");
                 System.err.println("Node #" + myNode.getID() + "has + " + myNode.neighboursList.size() + " neighbors");
                 new Exception().printStackTrace();
                 return ERROR; 
            }
            MacAddress macAddress = nodeEntry.mac;
            if (macAddress == null)
            {
                 System.err.println("Node #" + myNode.getID() + ": Destination IP (" + nextHopDestIP + ") not in my neighborhood. Please re-route! Are you sending the packet to yourself?");
                 System.err.println("Node #" + myNode.getID() + "has + " + myNode.neighboursList.size() + " neighbors");
                 return ERROR;
            }
            myNode.getNodeGUI().colorCode.mark(colorProfileGeneric, CSGPColorProfile.TRANSMIT, 2);
            netEntity.send(ipMsg, Constants.NET_INTERFACE_DEFAULT, macAddress);
        }
        
        return SUCCESS;
    }
   
       
     public NetAddress getThroughShortestPath(Location2D destLocation) {        
        double closestDist = myNode.getLocation2D().distanceTo(destLocation);
        NetAddress closestNode = myNode.getIP();
        LinkedList<NodeEntry> neighboursLinkedList 
        	= myNode.neighboursList.getAsLinkedList();
        
        for(NodeEntry nodeEntry: neighboursLinkedList) {
             /* Get the location coordinates of the neighbour 'i' */
            double neighbourDistance 
            	= nodeEntry.getNCS_Location2D()
            	  		   .fromNCS(myNode.getLocationContext())
            			   .distanceTo(destLocation);
            if ( neighbourDistance < closestDist ) {
                closestDist = neighbourDistance;
                closestNode = nodeEntry.ip;
            }
        }
        
        return closestNode;
    }   
       
     
    // *** USER CODE FUNCTIONS *** //
    public void dropNotify(Message msg, MacAddress nextHopMac, Reason reason) {
    	System.out.println("Packet dropped notify");
    	if (reason == Reason.PACKET_SIZE_TOO_LARGE) {
    		System.out.println("WARNING: Packet size too large - unable to transmit");
    		throw new RuntimeException("Packet size too large - unable to transmit");
    	}
        if (reason == Reason.NET_QUEUE_FULL) {
            System.out.println("WARNING: Net Queue full");
//            throw new RuntimeException("Net Queue Full");
        }
        if (reason == Reason.UNDELIVERABLE || reason == Reason.MAC_BUSY)
            System.out.println("WARNING: Cannot relay packet to the destination node " + nextHopMac);
        
        // wait 5 seconds and try again
        JistAPI.sleepBlock(500 * Constants.MILLI_SECOND);
        //netEntity.send((NetMessage.Ip)msg, Constants.NET_INTERFACE_DEFAULT, nextHopMac);
        this.send((NetMessage)msg);
    }

       
   /* **************************************** *
    * SWANS network's stack hook-up interfaces *
    * **************************************** */
    
    public RouteInterface getProxy()
    {
        return self;
    }
   
    
    public void setNetEntity(NetInterface netEntity)
    {
        if(!JistAPI.isEntity(netEntity)) throw new IllegalArgumentException("expected entity");
        if(this.netEntity!=null) throw new IllegalStateException("net entity already set");
        
        this.netEntity = netEntity;
    }

    
    public void setAppInterface(AppInterface appInterface)
    {
        this.appInterface = appInterface;
    }
        
   public void start()
   {
        //nothing
   }
    
}
