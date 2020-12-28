/*
 * ShortestGeographicalPathRouting.java
 *
 * Created on April 15, 2008, 11:43 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package sidnet.stack.users.csgp_adaptivepath.routing;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import jist.runtime.JistAPI;
import jist.swans.Constants;
import sidnet.core.interfaces.AppInterface;
import jist.swans.mac.MacAddress;
import jist.swans.misc.Message;
import jist.swans.net.NetAddress;
import jist.swans.net.NetInterface;
import jist.swans.net.NetMessage;
import jist.swans.route.RouteInterface;
import sidnet.core.gui.TopologyGUI;
import sidnet.core.interfaces.ColorProfile;
import sidnet.stack.users.csgp_adaptivepath.CSGPAPColorProfile.CSGPAPColorProfile;
import sidnet.core.misc.Location2D;
import sidnet.core.misc.NCS_Location2D;
import sidnet.core.misc.Node;
import sidnet.core.misc.NodeEntry;
import sidnet.core.misc.Reason;
import sidnet.core.misc.Region;
import sidnet.stack.users.csgp_adaptivepath.app.CSGPAP_App;
import static sidnet.stack.users.csgp_adaptivepath.app.CSGPAP_App.stats;

import sidnet.stack.users.csgp_adaptivepath.app.MessageDataValue;
import sidnet.stack.users.csgp_adaptivepath.app.MessageQuery;
import sidnet.stack.users.csgp_adaptivepath.driver.CSGPAP_Driver;
import sidnet.stack.users.csgp_adaptivepath.driver.SequenceGenerator;
import sidnet.utilityviews.statscollector.StatsCollector;

/**
 *
 * @author Oliviu C. Ghica, Northwestern University
 */
public class csgpaproutingmodif2 implements RouteInterface {
//    public final Node sinkNode;

    public static final byte ERROR = -1;
    public static final byte SUCCESS = 0;
    public static int drop = 0;
    public static int terkirim = 0;
    private final Node myNode; // The SIDnet handle to the node representation 
    public static TopologyGUI topologyGUI = null;
    public Node[] NodeList;
    public LinkedList<NetAddress> BlaklistNode = new LinkedList<NetAddress>();
    public List<Double> receivedMessage2 = new ArrayList();
    public List<Double> receivedMessage3 = new ArrayList();
    public static StatsCollector stats = null;
    SequenceGenerator sequenceNumberGlobal;
    private int sequenceCount = 0, sequenceAggregate = 0;

    // entity hook-up (network stack)
    /**
     * Network entity.
     */
    private NetInterface netEntity;

    /**
     * Self-referencing proxy entity.
     */
    private RouteInterface self;

    /**
     * The proxy-entity for this application interface
     */
    private AppInterface appInterface;

    // DO NOT MAKE THIS STATIC
    private CSGPAPColorProfile colorProfileGeneric = new CSGPAPColorProfile();

    //fixing routing variable
    // private String up, leaves1, leaves2;
    private java.util.HashMap listMessage = new java.util.HashMap();

    /**
     * Creates a new instance of ShortestGeographicalPathRouting
     *
     * @param Node the SIDnet node handle to access its GUI-primitives and
     * shared environment
     */
    public csgpaproutingmodif2(Node myNode) {
        this.myNode = myNode;
//cc
        /**
         * Create a proxy for the application layer of this node
         */
        self = (RouteInterface) JistAPI.proxy(this, RouteInterface.class);
    }

    public csgpaproutingmodif2(Node myNode, Node[] NodeList) {
        this.myNode = myNode;
        this.NodeList = NodeList;
        /**
         * Create a proxy for the application layer of this node
         */
        self = (RouteInterface) JistAPI.proxy(this, RouteInterface.class);
    }

    public csgpaproutingmodif2(Node myNode, Node[] NodeList, StatsCollector stats,
            SequenceGenerator SequenceNumberGlobal) {
        this.myNode = myNode;
        this.NodeList = NodeList;
        this.stats = stats;
        this.sequenceNumberGlobal = SequenceNumberGlobal;
        /**
         * Create a proxy for the application layer of this node
         */
        self = (RouteInterface) JistAPI.proxy(this, RouteInterface.class);
    }

    /**
     * SWANS legacy. We no longer enable this
     */
    public void peek(NetMessage msg, MacAddress lastHopMac) {
        // no peeking
    }

    /**
     * Receive a message from the network layer This method is typically called
     * when this node is the ultimate destination of an incoming data-message
     * (the sink)
     *
     * @param Message the incomming message
     * @param NetAddress the original source of the message
     * @param MacAddress the MAC address 1-hop neighbor from which this nodes
     * received this message
     * @param macId the macId interface through which this message was received
     * @param NetAddress the IP address of the ultimate node destination (this)
     * @param priority the priority of the incoming message
     * @param ttl Time To Leave
     */
    public void receive(Message msg, NetAddress src, MacAddress lastHop,
            byte macId, NetAddress dst, byte priority, byte ttl) {

        if (myNode.getEnergyManagement().getBattery().getPercentageEnergyLevel() < 2) {
            return;
        }

        // Provide a basic visual feedback on the fact that this node has received a message */
        myNode.getNodeGUI().colorCode.mark(colorProfileGeneric, CSGPAPColorProfile.RECEIVE, 20);

        // A message may come in a format that you define based on your implementation needs
        // You must extract that format and act upon
        if (msg instanceof CSGPWrapperMessage) {
            CSGPWrapperMessage msgNZ = (CSGPWrapperMessage) msg;
            sendToAppLayer(msgNZ.getPayload(), null);
            terkirim++;
        } // otherwise, it is a format not recognized by SGP, so we ignore it.
    }

    /**
     * Send a message This method is being called when a message, coming from
     * either the application layer or the mac layer,needs to be forwarded
     *
     * @param NetMessage the 'NetMessage' wrapped Message
     */
    public void send(NetMessage msg) {
        // process only if there are energy reserves
        if (myNode.getEnergyManagement()
                .getBattery()
                .getPercentageEnergyLevel() < 2) {
            return;
        }

        // If this message comes from App Layer
        if (!(((NetMessage.Ip) msg).getPayload() instanceof CSGPWrapperMessage)) {
            return; // ignore non-specific messages
        }

        CSGPWrapperMessage Agw = (CSGPWrapperMessage) (((NetMessage.Ip) msg).getPayload());

        // Message sudah pernah diterima
        if (this.listMessage.containsKey(Agw.messageID)) {
            drop++;
            return;
        } else { //masukkan
            this.listMessage.put(Agw.messageID, null); //supaya tdk dobel
        }

        // extract message
        Location2D targetLocation = null;

        CSGPWrapperMessage msgSGP = (CSGPWrapperMessage) ((NetMessage.Ip) msg).getPayload();

        if (msgSGP.getTargetRegion() != null) {
            targetLocation = extractClosestVertex(msgSGP.getTargetRegion());
        }

        if (msgSGP.getTargetLocation() != null) {
            targetLocation = msgSGP.getTargetLocation();
        }

        if (msgSGP.getPayload() instanceof MessageQuery) {
            handleQueryMessage(msg);
        } else if (msgSGP.getPayload() instanceof MessageDataValue) {
            handleMessageDataValue(msg, targetLocation);
        } else {
            System.out.println("Node " + myNode.getID() + " get unknown message");
        }

    }

    private int getIdfromAddress(NetAddress na) {
        String tempIp = na.getIP().getHostAddress();
        int hc = na.getIP().hashCode();
        return hc;
    }

    private void handleQueryMessage(NetMessage msg) {
        sidnet.stack.users.csgp_adaptivepath.routing.CSGPWrapperMessage msgQuery = (CSGPWrapperMessage) ((NetMessage.Ip) msg).getPayload();

        //msgQuery.setMessageId(newMessageID);
        NetMessage.Ip copyOfMsg
                = new NetMessage.Ip(msgQuery,
                        ((NetMessage.Ip) msg).getSrc(),
                        ((NetMessage.Ip) msg).getDst(),
                        ((NetMessage.Ip) msg).getProtocol(),
                        ((NetMessage.Ip) msg).getPriority(),
                        ((NetMessage.Ip) msg).getTTL(),
                        ((NetMessage.Ip) msg).getId(),
                        ((NetMessage.Ip) msg).getFragOffset());
        sendToLinkLayer(copyOfMsg, NetAddress.ANY); //kirim ke semua node tetangga

        // node yang menerima juga melakukan sensing maka kirim ke APP layer
        if (this.myNode.getIP() != ((NetMessage.Ip) msg).getSrc()) {
            sendToAppLayer(msgQuery.getPayload(), null);
        }
    }


    // rubah ini HANYA SGP
    public NetAddress getThroughShortestPath(NetMessage msg, Location2D destLocation, int index) { //SGP
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


    public NetAddress getThroughShortestPath2(NetMessage msg, Location2D destLocation, int index) { //SGP
        CSGPWrapperMessage msgDV = (CSGPWrapperMessage) ((NetMessage.Ip) msg).getPayload();
        double closestDist = myNode.getLocation2D().distanceTo(destLocation);

        double jarakNodeIni = myNode.getLocation2D().distanceTo(destLocation);


        NetAddress SourceDist = ((NetMessage.Ip) msg).getSrc();


       //   ArrayList<kumpulNode> nodeDekat = new ArrayList<kumpulNode>;

        ArrayList<kumpulNode> ArrayNodeDekat;
        ArrayNodeDekat= new ArrayList <kumpulNode>();

        kumpulNode itemNodeDekat;

        ArrayList<NetAddress> listClosestNode;
        listClosestNode = new ArrayList<NetAddress>();
       
        //arraylist <kumpulNode>
        // array yang menyimpan jarak

         ArrayList<Double> kumpulanJarakNode;
        kumpulanJarakNode = new ArrayList<Double>();


        NetAddress closestNode = myNode.getIP();
      //  listClosestNode.add(closestNode);

        LinkedList<NodeEntry> neighboursLinkedList
                = myNode.neighboursList.getAsLinkedList();

        for (NodeEntry nodeEntry : neighboursLinkedList) {
            /* Get the location coordinates of the neighbour 'i' */

            double neighbourDistance = nodeEntry.getNCS_Location2D()
                    .fromNCS(myNode.getLocationContext())
                    .distanceTo(destLocation);

       //      System.out.println("distance " + neighbourDistance + " " + closestDist);

            // MASUKKAN semua node yang jarakanya lebih pendek dari myNode

            if (neighbourDistance < jarakNodeIni) {
               // closestNode = nodeEntry.ip;
               // listClosestNode.add(closestNode); //  ini kamu memasukkan ke linkedlist

                itemNodeDekat= new kumpulNode();
                itemNodeDekat.ipNode= nodeEntry.ip;
                itemNodeDekat.jaraknya=neighbourDistance;

                ArrayNodeDekat.add(itemNodeDekat);

            }
        }

       ///  kamu mendapat semua node yang lokasinya lebih dekat dari SAYA

        // kamu harus sorting berdasarkarak  semua node di listClosestNode (karena ini array of IP)


        int n = ArrayNodeDekat.size();
        kumpulNode temp;
         for(int i=0; i < n; i++){
                 for(int j=1; j < (n-i); j++){  
                          if(ArrayNodeDekat.get(j-1).jaraknya > ArrayNodeDekat.get(j).jaraknya){
                                 //swap elements
                                 temp = ArrayNodeDekat.get(j-1);
                                 ArrayNodeDekat.set((j-1),ArrayNodeDekat.get(j)) ;
                                 ArrayNodeDekat.set(j,temp);
                                 
                         }

                 }
         }


        int arrLen = listClosestNode.size();
        int selectedIndex = arrLen - 1; // if arrLen < index
        System.out.println(listClosestNode);
        if (arrLen - index > 0) {
            selectedIndex = arrLen - index;
        }

        //  direturn kalau  prioritas 2 maka dikirim index ke 2 / 3
   //     return (ArrayNodeDekat.get(index).ipNode);

        return listClosestNode.get(selectedIndex);
    }



    private void handleMessageDataValue(NetMessage msg, Location2D targetLocation) {
        CSGPWrapperMessage msgDV = (CSGPWrapperMessage) ((NetMessage.Ip) msg).getPayload();
        MessageDataValue msgDataValue = (MessageDataValue) msgDV.getPayload();
        double aggregatedValue = 0;
        double temp = 0;
        int queryId = msgDataValue.queryId;
        long sequenceNumber = sequenceNumberGlobal.getandincrement();
        Location2D sinkLocation = targetLocation;
        NetAddress sinkIP = ((NetMessage.Ip) msg).getDst(); //ini adalah sink
        NetAddress nextHop; // tujuan routing
//        NetAddress nextHop = getCHTetangga();

//        if(this.myNode.getEnergyManagement().getBattery().getPercentageEnergyLevel()>50&& this.myNode.getEnergyManagement().getBattery().getPercentageEnergyLevel()<75){
//            nextHop=getCHKombinasi2();
//        if(this.myNode.getEnergyManagement().getBattery().getPercentageEnergyLevel()<=50){
//            nextHop=getSecondCH(); // dimatikan dulu tanpa second CH
//        }
//  ketika sudah sampai sink kirim ke app layere
        if ((this.myNode.getIP() == sinkIP)) { //sink node
            sendToAppLayer(msgDV.getPayload(), null);
        }

//            if(nextHop!=this.myNode.getIP()){
        if (msgDataValue.isFire() == true) {
            aggregatedValue = msgDataValue.dataValue;

            MessageDataValue msgAggregateValue = new MessageDataValue(aggregatedValue, queryId, sequenceNumber, myNode.getID());
            msgAggregateValue.setFire();
            CSGPWrapperMessage msgAG
                    = new CSGPWrapperMessage(msgAggregateValue, sinkLocation, 0, JistAPI.getTime());

            msgAG.setStatus(); //set status menjadi = true 
            msgAG.messageID = "F:" + this.myNode.getIP().toString() + ":" + this.sequenceAggregate++;


            /// JIKA PESAN EMERGENCY langsung SGP
            nextHop = getThroughShortestPath(msg, targetLocation, 1); //kirim menggunakan sgp

//            if (nextHop == this.myNode.getIP()) { //jika menemui hole, maka dicek tetangga terdekat kesink
//                BlaklistNode.add(this.myNode.getIP());
//                nextHop = getAdaptivePath(targetLocation);//zhy
////                        System.out.println(" Node "+nextHop+" Menemui hole ");
//            }
            System.out.println("nexthop " + nextHop); //coba run

            if (myNode.neighboursList.contains(sinkIP)) { //jika ditabel tetangganya ada sink maka pesan akan dikirim kesink
                NetMessage.Ip copyOfMsg = new NetMessage.Ip(msgAG, myNode.getIP(),
                        ((NetMessage.Ip) msg).getDst(),
                        ((NetMessage.Ip) msg).getProtocol(),
                        ((NetMessage.Ip) msg).getPriority(),
                        ((NetMessage.Ip) msg).getTTL(),
                        ((NetMessage.Ip) msg).getId(),
                        ((NetMessage.Ip) msg).getFragOffset());
                sendToLinkLayer(copyOfMsg, sinkIP);

                stats.markPacketSent("First_Priority", sequenceNumber);
                System.out.println("Node [" + myNode.getID() + "]  forward  to sink node " + sinkIP);
                topologyGUI.addLink(myNode.getID(), this.getIdfromAddress(sinkIP), 1, Color.BLUE, TopologyGUI.HeadType.LEAD_ARROW);

            } else {
                NetMessage.Ip copyOfMsg = new NetMessage.Ip(msgAG,
                        ((NetMessage.Ip) msg).getSrc(),
                        ((NetMessage.Ip) msg).getDst(),
                        ((NetMessage.Ip) msg).getProtocol(),
                        ((NetMessage.Ip) msg).getPriority(),
                        ((NetMessage.Ip) msg).getTTL(),
                        ((NetMessage.Ip) msg).getId(),
                        ((NetMessage.Ip) msg).getFragOffset());
                sendToLinkLayer(copyOfMsg, nextHop);

                stats.markPacketSent("First_Priority", sequenceNumber);
                System.out.println("Node [" + myNode.getID() + "]  forward  to " + nextHop);
                topologyGUI.addLink(myNode.getID(), this.getIdfromAddress(nextHop), 1, Color.BLUE, TopologyGUI.HeadType.LEAD_ARROW);
            }

        } else if (msgDataValue.isAbnormal() == true) {
            receivedMessage2.add(msgDataValue.dataValue);

            if (receivedMessage2.size() == 10) {
                for (int i = 0; i < receivedMessage2.size(); i++) {
                    temp = temp + receivedMessage2.get(i);
                }
                aggregatedValue = temp / receivedMessage2.size();

                MessageDataValue msgAggregateValue = new MessageDataValue(aggregatedValue, queryId, sequenceNumber, myNode.getID());
                msgAggregateValue.setAbnormal();
                CSGPWrapperMessage msgAG
                        = new CSGPWrapperMessage(msgAggregateValue, sinkLocation, 0, JistAPI.getTime());

                msgAG.setStatus();
                msgAG.messageID = "A: " + this.myNode.getIP().toString() + ":" + this.sequenceAggregate++;

                nextHop = getThroughShortestPath2(msg, targetLocation, 2); //kirim menggunakan sgp

                if (nextHop == this.myNode.getIP()) { //jika menemui hole, maka dicek tetangga terdekat kesink
                    BlaklistNode.add(this.myNode.getIP());
                    nextHop = getAdaptivePath(targetLocation);//zhy
//////                            System.out.println(" Node "+nextHop+" Menemui hole ");
                }

                if (myNode.neighboursList.contains(sinkIP)) { //jika ditabel tetangganya ada sink maka pesan akan dikirim kesink
                    NetMessage.Ip copyOfMsg = new NetMessage.Ip(msgAG, //ini diubah dengan wrapper pesan terbaru
                            ((NetMessage.Ip) msg).getSrc(),
                            ((NetMessage.Ip) msg).getDst(),
                            ((NetMessage.Ip) msg).getProtocol(),
                            ((NetMessage.Ip) msg).getPriority(),
                            ((NetMessage.Ip) msg).getTTL(),
                            ((NetMessage.Ip) msg).getId(),
                            ((NetMessage.Ip) msg).getFragOffset());
                    sendToLinkLayer(copyOfMsg, sinkIP);

                    stats.markPacketSent("Second_Priority", sequenceNumber);
                    System.out.println("Node [" + myNode.getID() + "]  forward  to sink node " + sinkIP);
                    topologyGUI.addLink(myNode.getID(), this.getIdfromAddress(sinkIP), 1, Color.BLUE, TopologyGUI.HeadType.LEAD_ARROW);

                    receivedMessage2.clear();

                } else {
                    NetMessage.Ip copyOfMsg = new NetMessage.Ip(msgAG,
                            ((NetMessage.Ip) msg).getSrc(),
                            ((NetMessage.Ip) msg).getDst(),
                            ((NetMessage.Ip) msg).getProtocol(),
                            ((NetMessage.Ip) msg).getPriority(),
                            ((NetMessage.Ip) msg).getTTL(),
                            ((NetMessage.Ip) msg).getId(),
                            ((NetMessage.Ip) msg).getFragOffset());
                    sendToLinkLayer(copyOfMsg, nextHop);

                    stats.markPacketSent("Second_Priority", sequenceNumber);
                    System.out.println("Node [" + myNode.getID() + "]  forward  to " + nextHop);
                    topologyGUI.addLink(myNode.getID(), this.getIdfromAddress(nextHop), 1, Color.BLUE, TopologyGUI.HeadType.LEAD_ARROW);
                    receivedMessage2.clear();
                }
            }

        } else if (msgDataValue.isNormal() == true) {
            receivedMessage3.add(msgDataValue.dataValue);

            if (receivedMessage3.size() == 20) {
                for (int i = 0; i < receivedMessage3.size(); i++) {
                    temp = temp + receivedMessage3.get(i);
                }
                aggregatedValue = temp / receivedMessage3.size();

                MessageDataValue msgAggregateValue = new MessageDataValue(aggregatedValue, queryId, sequenceNumber, myNode.getID());
                msgAggregateValue.setNormal();
                CSGPWrapperMessage msgAG
                        = new CSGPWrapperMessage(msgAggregateValue, sinkLocation, 0, JistAPI.getTime());

                msgAG.setStatus(); //set status menjadi = true
                msgAG.messageID = "N:" + this.myNode.getIP().toString() + ":" + this.sequenceAggregate++;

                nextHop = getThroughShortestPath(msg, targetLocation, 3); //kirim menggunakan sgp

                if (nextHop == this.myNode.getIP()) { //jika menemui hole, maka dicek tetangga terdekat kesink
                    BlaklistNode.add(this.myNode.getIP());
                    nextHop = getAdaptivePath(targetLocation);//zhy
//                            System.out.println(" Node "+nextHop+" Menemui hole ");
                }
                if (myNode.neighboursList.contains(sinkIP)) { //jika ditabel tetangganya ada sink maka pesan akan dikirim kesink
                    NetMessage.Ip copyOfMsg = new NetMessage.Ip(msgAG, //ini diubah dengan wrapper pesan terbaru
                            ((NetMessage.Ip) msg).getSrc(),
                            ((NetMessage.Ip) msg).getDst(),
                            ((NetMessage.Ip) msg).getProtocol(),
                            ((NetMessage.Ip) msg).getPriority(),
                            ((NetMessage.Ip) msg).getTTL(),
                            ((NetMessage.Ip) msg).getId(),
                            ((NetMessage.Ip) msg).getFragOffset());
                    sendToLinkLayer(copyOfMsg, sinkIP);

                    stats.markPacketSent("Third_Priority", sequenceNumber);
                    System.out.println("Node [" + myNode.getID() + "]  forward  to sink node " + sinkIP);
                    topologyGUI.addLink(myNode.getID(), this.getIdfromAddress(sinkIP), 1, Color.BLUE, TopologyGUI.HeadType.LEAD_ARROW);

                    receivedMessage3.clear();
                } else {
                    NetMessage.Ip copyOfMsg = new NetMessage.Ip(msgAG,
                            ((NetMessage.Ip) msg).getSrc(),
                            ((NetMessage.Ip) msg).getDst(),
                            ((NetMessage.Ip) msg).getProtocol(),
                            ((NetMessage.Ip) msg).getPriority(),
                            ((NetMessage.Ip) msg).getTTL(),
                            ((NetMessage.Ip) msg).getId(),
                            ((NetMessage.Ip) msg).getFragOffset());
                    sendToLinkLayer(copyOfMsg, nextHop);

                    stats.markPacketSent("Third_Priority", sequenceNumber);
                    System.out.println("Node [" + myNode.getID() + "]  forward  to " + nextHop);
                    topologyGUI.addLink(myNode.getID(), this.getIdfromAddress(nextHop), 1, Color.BLUE, TopologyGUI.HeadType.LEAD_ARROW);

                    receivedMessage3.clear();
                }
            }
        }

//            }
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


    private void handleWithTargetLocation(Location2D targetLocation, NetMessage msg, int index) {
        CSGPWrapperMessage msgSGP = (CSGPWrapperMessage) ((NetMessage.Ip) msg).getPayload();

        // Retrieve the IP address of the 1-hop neighbor closest to the area of interest */
        NetAddress nextHopIP = getThroughShortestPath(msg, targetLocation, index);

        // If there is no node closer to the area of interest than this node, 
        // then this node will get the message
        if (nextHopIP.hashCode() == myNode.getIP().hashCode()) {
            sendToAppLayer(msgSGP.getPayload(), null);

        } else { // keep forwarding, first, make a copy of the message
            NetMessage.Ip copyOfMsg = new NetMessage.Ip(msgSGP,
                    ((NetMessage.Ip) msg).getSrc(),
                    ((NetMessage.Ip) msg).getDst(),
                    ((NetMessage.Ip) msg).getProtocol(),
                    ((NetMessage.Ip) msg).getPriority(),
                    ((NetMessage.Ip) msg).getTTL(),
                    ((NetMessage.Ip) msg).getId(),
                    ((NetMessage.Ip) msg).getFragOffset());
            sendToLinkLayer(copyOfMsg, nextHopIP);
        }
    }

    /**
     * Find the closest vertex. A query may contain a region of interest. Since
     * this is SGP routing, we look for the closest vertex of that region
     */
    private Location2D extractClosestVertex(Region targetRegion) {
        targetRegion.resetIterator();

        Location2D nextLoc = targetRegion.getNext()
                .convertTo(targetRegion.getLocationContext(),
                        myNode.getLocationContext());
        Location2D closestVertex = nextLoc;

        double distMin = nextLoc.distanceTo(myNode.getLocation2D());

        while (targetRegion.hasNext()) {
            nextLoc = targetRegion.getNext();
            Location2D actualLoc = nextLoc.convertTo(targetRegion.getLocationContext(),
                    myNode.getLocationContext());
            if (actualLoc.distanceTo(myNode.getLocation2D()) < distMin) {
                distMin = actualLoc.distanceTo(myNode.getLocation2D());
                closestVertex = actualLoc;
            }
        }
        return closestVertex;
    }

    public void sendToAppLayer(Message msg, NetAddress src) {
        // ignore if not enough energy
        if (myNode.getEnergyManagement()
                .getBattery()
                .getPercentageEnergyLevel() < 2) {
            return;
        }

        appInterface.receive(msg, src, null, (byte) -1, NetAddress.LOCAL, (byte) -1, (byte) -1);
        //terkirim++;
    }

    public byte sendToLinkLayer(NetMessage.Ip ipMsg, NetAddress nextHopDestIP) {
        if (myNode.getEnergyManagement()
                .getBattery()
                .getPercentageEnergyLevel() < 2) {
            return 0;
        }

        //if (myNode.getID() == 164)
        // System.out.println("route packet to " + nextHopDestIP);
        if (nextHopDestIP == null) {
            //    System.err.println("NULL nextHopDestIP");
        }

        if (nextHopDestIP == NetAddress.ANY) {
            netEntity.send(ipMsg, Constants.NET_INTERFACE_DEFAULT, MacAddress.ANY);
        } 
        else {
            NodeEntry nodeEntry = myNode.neighboursList.get(nextHopDestIP);
            if (nodeEntry == null) {
                // System.err.println("Node #" + myNode.getID() + ": Destination IP (" + nextHopDestIP + ") not in my neighborhood. Please re-route! Are you sending the packet to yourself?");
                // System.err.println("Node #" + myNode.getID() + "has + " + myNode.neighboursList.size() + " neighbors");
                new Exception().printStackTrace();
                return ERROR;
            }

            MacAddress macAddress = nodeEntry.mac;
            if (macAddress == null) {
                //System.err.println("Node #" + myNode.getID() + ": Destination IP (" + nextHopDestIP + ") not in my neighborhood. Please re-route! Are you sending the packet to yourself?");
                //System.err.println("Node #" + myNode.getID() + "has + " + myNode.neighboursList.size() + " neighbors");
                return ERROR;
            }

            myNode.getNodeGUI().colorCode.mark(colorProfileGeneric, CSGPAPColorProfile.TRANSMIT, 2);
            netEntity.send(ipMsg, Constants.NET_INTERFACE_DEFAULT, macAddress);
            //System.out.println("sent from "+myNode.getIP()+" to "+nextHopDestIP);
        }
        return SUCCESS;
    }

    /* private NetAddress getClusterHead() { //pemilihan CH berdasarkan sisa energi tertinggi
        
        NetAddress nextHopAddress = myNode.getIP();
        LinkedList<NodeEntry> neighboursLinkedList = myNode.neighboursList.getAsLinkedList();
        double bobotNode = 0;
        double bobotNodeSekarang = myNode.getEnergyManagement().getBattery().getPercentageEnergyLevel();

        for (NodeEntry nodeEntry : neighboursLinkedList) { 
            if (nodeEntry.clusterId == myNode.clusterId) {
                bobotNode = nodeEntry.battery;
                if (bobotNode > bobotNodeSekarang) {
                    bobotNodeSekarang = bobotNode;
                    nextHopAddress = nodeEntry.ip;

                    myNode.getSimManager().getSimGUI()
                          .getAnimationDrawingTool().animate("CrossHair", nodeEntry.getNCS_Location2D());
                }
            }
        }
        return nextHopAddress;
    } */
    private NetAddress getCHTetangga() {  //pemilihan Cluster head berdasarkan jumlah tetangga terbanyak dalam satu cluster
        NetAddress nextHopAddress = myNode.getIP();
        int jumlahTetanggaSaya = myNode.neighboursList.size();
        int jumlahTetangga = 0;

        LinkedList<NodeEntry> neighboursLinkedList = myNode.neighboursList.getAsLinkedList();

        for (NodeEntry nodeEntry : neighboursLinkedList) {
            if (nodeEntry.clusterId == myNode.clusterId) {
                jumlahTetangga = nodeEntry.jumlahTetangga;
                if (jumlahTetangga > jumlahTetanggaSaya) {
                    jumlahTetanggaSaya = jumlahTetangga;
                    nextHopAddress = nodeEntry.ip;
                }
            }
        }

        for (NodeEntry nodeEntry : neighboursLinkedList) {
            if (nodeEntry.ip == nextHopAddress) {
                nodeEntry.setStatus1();

                myNode.getSimManager().getSimGUI()
                        .getAnimationDrawingTool().animate("CrossHair", nodeEntry.getNCS_Location2D());
            }
        }
        return nextHopAddress;
    }

    private NetAddress getSecondCH() { //pemilihan Ch kedua berdasarkan jumlah tetangga terbanyak kedua
        NetAddress nextHopAddress = myNode.getIP();
        int jumlahTetanggaSaya = myNode.neighboursList.size();
        int jumlahTetangga = 0;

        LinkedList<NodeEntry> neighboursLinkedList = myNode.neighboursList.getAsLinkedList();

        for (NodeEntry nodeEntry : neighboursLinkedList) {
            if (nodeEntry.clusterId == myNode.clusterId) {
                if (nodeEntry.status1 == false) {
                    jumlahTetangga = nodeEntry.jumlahTetangga; //mencari rata-rata bobot node (ex: =30)
                    if (jumlahTetangga > jumlahTetanggaSaya) { //jika bobot node lebih besar dari bobot node sekarang (ex: 30>0)
                        jumlahTetanggaSaya = jumlahTetangga; // maka bobot node sekarang sama dengan bobot node (bobotnodesekarang= 30)
                        nextHopAddress = nodeEntry.ip;

                        myNode.getSimManager().getSimGUI()
                                .getAnimationDrawingTool().animate("CrossHair", nodeEntry.getNCS_Location2D());
                    }
                }
            }
        }
        return nextHopAddress;
    }

    private NetAddress getClusterHead3() {  //pemilihan Cluster head berdasarkan jumlah tetangga terbanyak

        NetAddress nextHopAddress = null;
        int jumlahTetanggaMin = 0;
        int jumlahTetangga = 0;

        LinkedList<NodeEntry> neighboursLinkedList = myNode.neighboursList.getAsLinkedList();

        for (NodeEntry nodeEntry : neighboursLinkedList) {

            jumlahTetangga = nodeEntry.jumlahTetangga;
            if (jumlahTetangga > jumlahTetanggaMin) {
                jumlahTetanggaMin = jumlahTetangga;
                nextHopAddress = nodeEntry.ip;
            }
        }

        for (NodeEntry nodeEntry : neighboursLinkedList) {
            if (nodeEntry.ip == nextHopAddress) {
                nodeEntry.setStatus1();

                myNode.getSimManager().getSimGUI()
                        .getAnimationDrawingTool().animate("CrossHair", nodeEntry.getNCS_Location2D());
            }
        }
        return nextHopAddress;
    }

    private NetAddress getThirdCH() { //pemilihan CH berdasarkan jmlah tetangga terbanyak ke tiga 
        NetAddress nextHopAddress = null;
        int jumlahTetanggaMin = 0;
        int jumlahTetangga = 0;

        LinkedList<NodeEntry> neighboursLinkedList = myNode.neighboursList.getAsLinkedList();

        for (NodeEntry nodeEntry : neighboursLinkedList) {
            if (nodeEntry.status1 == false && nodeEntry.status2 == false) {
                jumlahTetangga = nodeEntry.jumlahTetangga;
                if (jumlahTetangga > jumlahTetanggaMin) {
                    jumlahTetanggaMin = jumlahTetangga;
                    nextHopAddress = nodeEntry.ip;
                }
            }
        }

        for (NodeEntry nodeEntry : neighboursLinkedList) {
            if (nodeEntry.ip == nextHopAddress) {
                nodeEntry.setStatus3();
                myNode.getSimManager().getSimGUI()
                        .getAnimationDrawingTool().animate("CrossHair", nodeEntry.getNCS_Location2D());
            }
        }
        return nextHopAddress;
    }

    private NetAddress getForthCH() { //pemilihan CH berdasarkan jumlah tetangga terbanyak ke 4
        NetAddress nextHopAddress = null;
        int jumlahTetanggaMin = 0;
        int jumlahTetangga = 0;

        LinkedList<NodeEntry> neighboursLinkedList = myNode.neighboursList.getAsLinkedList();

        for (NodeEntry nodeEntry : neighboursLinkedList) {
            if (nodeEntry.status1 == false && nodeEntry.status2 == false && nodeEntry.status3 == false) {
                jumlahTetangga = nodeEntry.jumlahTetangga;
                if (jumlahTetangga > jumlahTetanggaMin) {
                    jumlahTetanggaMin = jumlahTetangga;
                    nextHopAddress = nodeEntry.ip;

                    myNode.getSimManager().getSimGUI()
                            .getAnimationDrawingTool().animate("CrossHair", nodeEntry.getNCS_Location2D());
                }
            }
        }
        return nextHopAddress;
    }

//    private NetAddress getCHKombinasi() { //pemilihan cluster head berdasarkan energi tertinggi dan jarak terdekat ke CC
//        NetAddress nextHopAddress = myNode.getIP();
//        double sisaEnergy=myNode.getEnergyManagement().getBattery().getPercentageEnergyLevel();
//        LinkedList<NodeEntry> neighboursLinkedList = myNode.neighboursList.getAsLinkedList();
//        double bobotNode = 0;
//        double bobotNodeSekarang = 0.2 *(89-myNode.distToClusterCenter)+ 0.8* sisaEnergy; //nilainya diubah sesuai kebutuhan
//
//            for (NodeEntry nodeEntry : neighboursLinkedList) {
//                // Update Batery Tetangga dengan acuan nilai dari NodeList
//                for (int i = 0; i < NodeList.length; i++) {
//                    if(NodeList[i] != null && NodeList[i].getIP() == nodeEntry.ip){
//                        nodeEntry.battery = NodeList[i].getEnergyManagement().getBattery().getPercentageEnergyLevel();                 
//                    }
//                }
//
//                if (nodeEntry.clusterId == myNode.clusterId) {
//                    bobotNode = 0.2 * (89 - nodeEntry.distToCluster) + 0.8 * nodeEntry.battery;
//
//                    if (bobotNode > bobotNodeSekarang) { 
//                        bobotNodeSekarang = bobotNode; 
//                        nextHopAddress = nodeEntry.ip;
//                    }
//                }
//            }
//
//            for (NodeEntry nodeEntry : neighboursLinkedList) {
//                if(nodeEntry.ip==nextHopAddress){
//                    nodeEntry.setStatus1();
//
//                    myNode.getSimManager().getSimGUI()
//                          .getAnimationDrawingTool().animate("CrossHair", nodeEntry.getNCS_Location2D());
//                }
//            }
//        return nextHopAddress;
//    }
//    
//    
//    private NetAddress getCHKombinasi2() { //pemilihan cluster head berdasarkan energi tertinggi dan jarak terdekat ke CC
//        
//        NetAddress nextHopAddress = myNode.getIP();
//        double sisaEnergy=myNode.getEnergyManagement().getBattery().getPercentageEnergyLevel();
//        LinkedList<NodeEntry> neighboursLinkedList = myNode.neighboursList.getAsLinkedList();
//        double bobotNode = 0;
//        double bobotNodeSekarang = 0.2 *(89-myNode.distToClusterCenter)+ 0.8 * sisaEnergy;
//       
//        for (NodeEntry nodeEntry : neighboursLinkedList) {
//            // Update Batery Tetangga dengan acuan nilai dari NodeList
//            for (int i = 0; i < NodeList.length; i++) {
//                if(NodeList[i] != null && NodeList[i].getIP() == nodeEntry.ip){
//                    nodeEntry.battery = NodeList[i].getEnergyManagement().getBattery().getPercentageEnergyLevel();                 
//                }
//            }
//            
//            if (nodeEntry.clusterId == myNode.clusterId) {
//                if(nodeEntry.status1==false){
//                    bobotNode = 0.2 * (89 - nodeEntry.distToCluster) + 0.8 * nodeEntry.battery;
//                    if (bobotNode > bobotNodeSekarang) { 
//                        bobotNodeSekarang = bobotNode; 
//                        nextHopAddress = nodeEntry.ip;
//                    } 
//                }
//            }
//        }
//            
//        for (NodeEntry nodeEntry : neighboursLinkedList) {
//            if(nodeEntry.ip==nextHopAddress){
//                nodeEntry.setStatus2();
//
//                myNode.getSimManager().getSimGUI()
//                      .getAnimationDrawingTool().animate("CrossHair", nodeEntry.getNCS_Location2D());
//            }
//        }
//        return nextHopAddress;
//    }
//    
//    private NetAddress getCHKombinasi3() { //pemilihan cluster head berdasarkan energi tertinggi dan jarak terdekat ke CC
//        
//        NetAddress nextHopAddress = myNode.getIP();
//        double sisaEnergy=myNode.getEnergyManagement().getBattery().getPercentageEnergyLevel();
//        LinkedList<NodeEntry> neighboursLinkedList = myNode.neighboursList.getAsLinkedList();
//        double bobotNode = 0;
//        double bobotNodeSekarang = 0.2 *(89-myNode.distToClusterCenter)+ 0.8 * sisaEnergy;
//       
//        for (NodeEntry nodeEntry : neighboursLinkedList) {
//            // Update Batery Tetangga dengan acuan nilai dari NodeList
//            for (int i = 0; i < NodeList.length; i++) {
//                if(NodeList[i] != null && NodeList[i].getIP() == nodeEntry.ip){
//                    nodeEntry.battery = NodeList[i].getEnergyManagement().getBattery().getPercentageEnergyLevel();                 
//                }
//            }
//            
//            if (nodeEntry.clusterId == myNode.clusterId) {
//                if(nodeEntry.status1==false && nodeEntry.status2==false){
//                bobotNode = 0.2 * (89 - nodeEntry.distToCluster) + 0.8 * nodeEntry.battery;
//                
//                if (bobotNode > bobotNodeSekarang) { 
//                    bobotNodeSekarang = bobotNode;
//                    nextHopAddress = nodeEntry.ip;
//                    
//                    myNode.getSimManager().getSimGUI()
//                          .getAnimationDrawingTool().animate("CrossHair", nodeEntry.getNCS_Location2D());
//                }
//                }
//            }
//            }
//        return nextHopAddress;
//    }
//        
//        
//        
//    private NetAddress getCHDistance() { //pemilihan cluster head berdasarkan jarak terdekat ke CC
//        
//        NetAddress nextHopAddress = myNode.getIP();
//        LinkedList<NodeEntry> neighboursLinkedList = myNode.neighboursList.getAsLinkedList();
//        double distanceSaya = myNode.distToClusterCenter;
//        double distanceTetangga=0;
//       
//        for (NodeEntry nodeEntry : neighboursLinkedList) {
//            if (nodeEntry.clusterId == myNode.clusterId) {
//                distanceTetangga=nodeEntry.distToCluster;
//                
//                if (distanceTetangga > distanceSaya) {
//                    distanceSaya = distanceTetangga;
//                    nextHopAddress = nodeEntry.ip;
//                }
//            }
//        }
//            
//        for (NodeEntry nodeEntry : neighboursLinkedList) {
//            if(nodeEntry.ip==nextHopAddress){
//                nodeEntry.setStatus1();
//
//                myNode.getSimManager().getSimGUI()
//                      .getAnimationDrawingTool().animate("CrossHair", nodeEntry.getNCS_Location2D());
//            }
//        }
//        return nextHopAddress;
//    }
    private NetAddress getCHDistance2() { //pemilihan cluster head berdasarkan jarak terdekat ke CC ke 2

        NetAddress nextHopAddress = myNode.getIP();
        LinkedList<NodeEntry> neighboursLinkedList = myNode.neighboursList.getAsLinkedList();
        double distanceSaya = myNode.distToClusterCenter;
        double distanceTetangga = 0;

        for (NodeEntry nodeEntry : neighboursLinkedList) {
            if (nodeEntry.clusterId == myNode.clusterId) {
                if (nodeEntry.status1 == false) {
                    distanceTetangga = nodeEntry.distToCluster;
                    if (distanceTetangga > distanceSaya) { //
                        distanceSaya = distanceTetangga; // 
                        nextHopAddress = nodeEntry.ip;

                        myNode.getSimManager().getSimGUI()
                                .getAnimationDrawingTool().animate("CrossHair", nodeEntry.getNCS_Location2D());
                    }
                }
            }
        }
        return nextHopAddress;
    }

    private NetAddress getAdaptivePath(Location2D destLocation) { //adaptive path
        double jarakSekarang = 1000000; //set initial maximum values
        NetAddress nodeTerdekat = myNode.getIP();
        LinkedList<NodeEntry> neighboursLinkedList = myNode.neighboursList.getAsLinkedList();

        for (NodeEntry nodeEntry : neighboursLinkedList) {
            if (BlaklistNode.contains(nodeEntry.ip)) {
                continue;
            }
            /* Get the location coordinates and distance of the neighbour 'i' */
            double jarakNodeTetangga = nodeEntry.getNCS_Location2D()
                    .fromNCS(myNode.getLocationContext())
                    .distanceTo(destLocation);
            if (jarakNodeTetangga < jarakSekarang) {
                jarakSekarang = jarakNodeTetangga;
                nodeTerdekat = nodeEntry.ip;
            }
        }
        return nodeTerdekat;
    }

    // *** USER CODE FUNCTIONS *** //
    public void dropNotify(Message msg, MacAddress nextHopMac, Reason reason) {
        //	System.out.println("Packet dropped notify");

        if (reason == Reason.PACKET_SIZE_TOO_LARGE) {
            //	System.out.println("WARNING: Packet size too large - unable to transmit");
            //throw new RuntimeException("Packet size too large - unable to transmit");
            //drop++;
        }
        if (reason == Reason.NET_QUEUE_FULL) {
            System.out.println("WARNING: Net Queue full : " + nextHopMac.toString());
            //  throw new RuntimeException("Net Queue Full");
            //drop++;
        }
        if (reason == Reason.UNDELIVERABLE || reason == Reason.MAC_BUSY) {
            System.out.println("WARNING: Cannot relay packet to the destination node : " + nextHopMac);
        }

        // wait 5 seconds and try again
        JistAPI.sleepBlock(500 * Constants.MILLI_SECOND);
        //netEntity.send((NetMessage.Ip)msg, Constants.NET_INTERFACE_DEFAULT, nextHopMac);
        this.send((NetMessage) msg);

    }

    /* **************************************** *
    * SWANS network's stack hook-up interfaces *
    * **************************************** */
    public RouteInterface getProxy() {
        return self;
    }

    public void setNetEntity(NetInterface netEntity) {
        if (!JistAPI.isEntity(netEntity)) {
            throw new IllegalArgumentException("expected entity");
        }
        if (this.netEntity != null) {
            throw new IllegalStateException("net entity already set");
        }

        this.netEntity = netEntity;
    }

    public void setAppInterface(AppInterface appInterface) {
        this.appInterface = appInterface;
    }

    public void start() {
        //nothing
    }

}
