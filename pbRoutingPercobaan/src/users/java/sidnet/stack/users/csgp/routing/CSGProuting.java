/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
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
import sidnet.colorprofiles.ColorProfileGeneric;
import sidnet.core.misc.Location2D;
import sidnet.core.misc.Node;
import sidnet.core.misc.NodeEntry;
import sidnet.core.misc.Reason;
import sidnet.core.misc.Region;
import sidnet.stack.std.routing.shortestgeopath.SGPWrapperMessage;
import sidnet.stack.users.csgp.app.MessageDataValue;
import sidnet.stack.users.csgp.app.MessageQuery;
/**
 *
 * @author MMC
 */
public class CSGProuting implements RouteInterface{

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
    private ColorProfileGeneric colorProfileGeneric = new ColorProfileGeneric(); 
    
    /** Creates a new instance of ShortestGeographicalPathRouting
     *
     * @param Node    the SIDnet node handle to access 
     * 				  its GUI-primitives and shared environment
     */
    public CSGProuting(Node myNode) {
        this.myNode = myNode;
        
        /** Create a proxy for the application layer of this node */
        self = (RouteInterface)JistAPI.proxy(this, RouteInterface.class);
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
        								   ColorProfileGeneric.RECEIVE, 20);  
        
        // A message may come in a format that you define based
        // on your implementation needs
        // You must extract that format and act upon
        if (msg instanceof CSGPWrapperMessage) {
        	CSGPWrapperMessage msgNZ = (CSGPWrapperMessage)msg;
            sendToAppLayer(msgNZ.getPayload(), null);
            System.out.println("Node "+myNode.getIP()+" has received a message ");
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
    									   ColorProfileGeneric.RECEIVE, 2000);
      
        // If this message comes from App Layer
        if (!(((NetMessage.Ip)msg).getPayload() instanceof CSGPWrapperMessage))
        	return; // ignore non-specific messages
        
        // extract message
        Location2D targetLocation = null;
        CSGPWrapperMessage msgSGP 
        	= (CSGPWrapperMessage)((NetMessage.Ip)msg).getPayload();

        // Seek destination information (Location or Region based)
        if (msgSGP.getTargetRegion() != null)
        	targetLocation = extractClosestVertex(msgSGP.getTargetRegion());
                
        if (msgSGP.getTargetLocation() != null)
        	targetLocation = msgSGP.getTargetLocation();
        
        //if (targetLocation != null)	// TODO FIXME occasionally this turns to be null
        //	handleWithTargetLocationToCH(targetLocation, msg);
        
       if (msgSGP.getPayload() instanceof MessageQuery) {
         //   System.out.println("Node " + myNode.getID() + " get query message");
            handleQueryMessage(msg);
        } else if (msgSGP.getPayload() instanceof MessageDataValue) {
            System.out.println("Node " + myNode.getID() + " get data value message");
          //  handleMessageDataValue(targetLocation, msg);
            handleWithTargetLocationToCH(targetLocation, msg);
        } else {
            System.out.println("Node " + myNode.getID() + " get unknown message");
        }

        
       
     }
     private void handleQueryMessage(NetMessage msg) {
        CSGPWrapperMessage msgAGW
    	 	= (CSGPWrapperMessage)((NetMessage.Ip)msg).getPayload();

        NetMessage.Ip copyOfMsg
              	= new NetMessage.Ip(msgAGW,
		       ((NetMessage.Ip)msg).getSrc(),
                       ((NetMessage.Ip)msg).getDst(),
                       ((NetMessage.Ip)msg).getProtocol(),
                       ((NetMessage.Ip)msg).getPriority(),
                       ((NetMessage.Ip)msg).getTTL(),
                       ((NetMessage.Ip)msg).getId(),
                       ((NetMessage.Ip)msg).getFragOffset());

         //kirim ke semua node tetangga

                sendToLinkLayer(copyOfMsg, NetAddress.ANY);

        //System.out.println("Node " + myNode.getID() + " forwarding query msg to neigbours ");
        
        // node yang menerima juga melakukan sensing maka kirim ke APP layer
        if (this.myNode.getIP() != ((NetMessage.Ip)msg).getSrc())
            sendToAppLayer(msgAGW.getPayload(), null);
     }

     private void handleWithTargetLocation(Location2D targetLocation,
    		 								NetMessage msg) {
    	 SGPWrapperMessage msgSGP 
    	 	= (SGPWrapperMessage)((NetMessage.Ip)msg).getPayload();
    	 
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
     
     private void handleWithTargetLocationToCH(Location2D targetLocation,
    		 								NetMessage msg) {
    	 CSGPWrapperMessage msgSGP 
    	 	= (CSGPWrapperMessage)((NetMessage.Ip)msg).getPayload();
    	 
    	 // Retrieve the IP address of the 1-hop neighbor
    	 // closest to the area of interest */
           
         
         NetAddress nextHopIP;
         nextHopIP = getThroughShortestPath(targetLocation);
         if (msgSGP.getStatus()==false){
             nextHopIP = SearchChIPAddress();
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
             msgSGP.setStatus();
         }            
         else if ( msgSGP.getStatus()==true){
             //handleWithTargetLocation(targetLocation, msg);
             //nextHopIP = getThroughShortestPath(targetLocation);
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
         }else if (nextHopIP.hashCode() == myNode.getIP().hashCode()){
              sendToAppLayer(msgSGP.getPayload(), null);
              System.out.println("Message received at sink");
         }
        	 
         else {
             System.out.println("unknown destination");
       }
         
    	 // If there is no node closer to the area of interest than this node, 
    	 // then this node will get the message
        // if (nextHopIP.hashCode() == myNode.getIP().hashCode())
        	 //sendToAppLayer(msgSGP.getPayload(), null);
        // else { // keep forwarding
        	  // first, make a copy of the message
                       //}
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
               //  new Exception().printStackTrace();
                 return ERROR; 
            }
            MacAddress macAddress = nodeEntry.mac;
            if (macAddress == null)
            {
                 System.err.println("Node #" + myNode.getID() + ": Destination IP (" + nextHopDestIP + ") not in my neighborhood. Please re-route! Are you sending the packet to yourself?");
                 System.err.println("Node #" + myNode.getID() + "has + " + myNode.neighboursList.size() + " neighbors");
                 return ERROR;
            }
            myNode.getNodeGUI().colorCode.mark(colorProfileGeneric, ColorProfileGeneric.TRANSMIT, 2);
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
     
     int jmlDrop=0;
     
    // *** USER CODE FUNCTIONS *** //
    public void dropNotify(Message msg, MacAddress nextHopMac, Reason reason) {
        
    //	System.out.println("Packet dropped notify");
    	if (reason == Reason.PACKET_SIZE_TOO_LARGE) {
    		System.out.println("WARNING: Packet size too large - unable to transmit");
    		throw new RuntimeException("Packet size too large - unable to transmit");
    	}
        if (reason == Reason.NET_QUEUE_FULL) {
            jmlDrop++;
            
        //    System.out.println("WARNING: Net Queue full");
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
    
    public NetAddress SearchChIPAddress() {

        NetAddress ipNode = myNode.getIP();
        LinkedList<NodeEntry> neighboursLinkedList
        	= myNode.clusterNeighbourList.getAsLinkedList();

        NetAddress nextNode=null;
        double distToCluster =100000;
        for(NodeEntry nodeEntry: neighboursLinkedList) {
          if (nodeEntry.distToCluster < distToCluster){
              distToCluster= nodeEntry.distToCluster;
              nextNode = nodeEntry.ip;
              System.out.println("Next Node IP : "+ nextNode.getIP());
          }
        }
        if (distToCluster < myNode.distToClusterCenter) {
           this.myNode.ChAddress= nextNode;
           System.out.println("Next Node IP (Cluster Head IP Address): "+nextNode.getIP());
          return nextNode;
        }
        else
          return ipNode;
        
       
     }
    
    private NetAddress getChIP(){
         NetAddress nextHopAddress = null;
         LinkedList<NodeEntry> neighboursLinkedList =  myNode.neighboursList.getAsLinkedList();
         
         double jarakToCluster=1000000;
         for (NodeEntry nodeEntry : neighboursLinkedList){
             if (nodeEntry.clusterId == myNode.clusterId){
                 if (nodeEntry.distToCluster < jarakToCluster) {
                     jarakToCluster = nodeEntry.distToCluster;
                     nextHopAddress = nodeEntry.ip;
                 }
             }
             
         }
              
         System.out.println(" Cluster head terpilih dari node " + myNode.getIP().toString() + " : " + nextHopAddress.getIP().toString());
         return nextHopAddress;
     
     }
    private void handleQueryMessage2(Location2D targetLocation, NetMessage msg) {
         SGPWrapperMessage msgSGP
    	 	= (SGPWrapperMessage)((NetMessage.Ip)msg).getPayload();

         //jika ini bukan node terakhir lanjut sebarkan


        if (this.myNode.ChAddress == null ){
            this.myNode.ChAddress= this.SearchChIPAddress();
        }
        NetAddress CH_Address = this.myNode.ChAddress;

        if (CH_Address != this.myNode.getIP()){
            // sending to cH
            System.out.println (" Sending query message to cluster head : " +CH_Address.toString());
            NetMessage.Ip copyOfMsg
              	= new NetMessage.Ip(msgSGP,
		       ((NetMessage.Ip)msg).getSrc(),
                       ((NetMessage.Ip)msg).getDst(),
                       ((NetMessage.Ip)msg).getProtocol(),
                       ((NetMessage.Ip)msg).getPriority(),
                       ((NetMessage.Ip)msg).getTTL(),
                       ((NetMessage.Ip)msg).getId(),
                       ((NetMessage.Ip)msg).getFragOffset());
            sendToLinkLayer(copyOfMsg, CH_Address);

        }
        else {
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
    }
    private void handleMessageDataValue(Location2D targetLocation, NetMessage msg) {
         SGPWrapperMessage msgAGW
    	 	= (SGPWrapperMessage)((NetMessage.Ip)msg).getPayload();


      NetAddress ip= ((NetMessage.Ip)msg).getDst();
      /// jika dia bukan sink node (destination)
      if (!(myNode.getIP() == ip)){

            NetMessage.Ip copyOfMsg
              	= new NetMessage.Ip(msgAGW,
		       ((NetMessage.Ip)msg).getSrc(),
                       ((NetMessage.Ip)msg).getDst(),
                       ((NetMessage.Ip)msg).getProtocol(),
                       ((NetMessage.Ip)msg).getPriority(),
                       ((NetMessage.Ip)msg).getTTL(),
                       ((NetMessage.Ip)msg).getId(),
                       ((NetMessage.Ip)msg).getFragOffset());

         //kirim ke semua node tetangga FLOODING INI !!!

            
            // disini harus ngirim ke ch dengan tipe data NetAddress nextHop=  getCH(.....
            // disini tempat kita nyari NetAddress dari ClusterHEad
            //  sendToLinkLayer(copyOfMsg, nextHop);
                NetAddress nextHop= this.getChIP();
                sendToLinkLayer(copyOfMsg, nextHop); // ini ngirim ke CH
                //sendToLinkLayer(copyOfMsg, NetAddress.ANY);

            System.out.println("Node " + myNode.getID() + " forwarding Sensing Message to clusterhead  ");

         } else if(myNode.getIP()== this.getChIP()){
             //JIKA DIA SENDIRI ADALAH CLUSTER HEAD, MAKA GUNAKAN SGP
             NetMessage.Ip copyOfMsg
              	= new NetMessage.Ip(msgAGW,
		       ((NetMessage.Ip)msg).getSrc(),
                       ((NetMessage.Ip)msg).getDst(),
                       ((NetMessage.Ip)msg).getProtocol(),
                       ((NetMessage.Ip)msg).getPriority(),
                       ((NetMessage.Ip)msg).getTTL(),
                       ((NetMessage.Ip)msg).getId(),
                       ((NetMessage.Ip)msg).getFragOffset());

         //kirim ke semua node tetangga FLOODING INI !!!

            
            // disini harus ngirim ke ch dengan tipe data NetAddress nextHop=  getCH(.....
            // disini tempat kita nyari NetAddress dari ClusterHEad
            //  sendToLinkLayer(copyOfMsg, nextHop);
                NetAddress nextHop= this.getChIP();
                sendToLinkLayer(copyOfMsg, nextHop); // ini ngirim ke CH
                //sendToLinkLayer(copyOfMsg, NetAddress.ANY);

            System.out.println("Node " + myNode.getID() + " forwarding Sensing Message to clusterhead  ");

         }
            else { //sink node
             sendToAppLayer(msgAGW.getPayload(), null);
         }
    }
        
   public void start()
   {
        //nothing
   }
   
    
}
