/*
 * ShortestGeographicalPathRouting.java
 *
 * Created on April 15, 2008, 11:43 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package sidnet.stack.users.csgp.routing;

import java.util.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
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
import sidnet.core.interfaces.ColorProfile;
import sidnet.stack.users.csgp.colorprofile.CSGPColorProfile;
import sidnet.core.misc.Location2D;
import sidnet.core.misc.Node;
import sidnet.core.misc.NodeEntry;
import sidnet.core.misc.Reason;
import sidnet.core.misc.Region;
import sidnet.stack.users.csgp.app.ChApp;
import sidnet.stack.users.csgp.app.Konstanta;


import sidnet.stack.users.csgp.app.MessageQuery;
import sidnet.stack.users.csgp.app.MessageDataValue;
import sidnet.stack.users.csgp.driver.SwingRoutingDriver;
import sidnet.stack.users.csgp.driver.SequenceGenerator;
import sidnet.utilityviews.statscollector.StatsCollector;



import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Collections;

/**
 *
 * @author Oliviu C. Ghica, Northwestern University
 */
public class SwingRouting2 implements RouteInterface {
//    public final Node sinkNode;
    public static final byte ERROR = -1;
    public static final byte SUCCESS = 0;
    public static int drop=0;
    public static int terkirim=0;
    public Node[] NodeList;
    public static StatsCollector stats = null;
    SequenceGenerator sequenceNumberGlobal;
    private int sequenceCount=0, sequenceAggregate=0;


    private final Node myNode; // The SIDnet handle to the node representation
    private int numHop=10000;
    private String travelPathtoSink="";
    private String idSink="199";
    private String travelList;
    private boolean cekTetangga=false;
   private int urutan =0;
   private String availablePathList="";
   public String[] jalurLagi;
   public boolean pesanBackhole=false;
    List<NetAddress> forwardingNodes = null; // variabel  forwarding node
    LinkedList<NetAddress> BlacklistNode = new LinkedList<NetAddress>();
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
   // private String up, leaves1, leaves2;

    private String availablePath="";

     private java.util.HashMap listMessage = new java.util.HashMap();


    /** Creates a new instance of ShortestGeographicalPathRouting
     *
     * @param Node    the SIDnet node handle to access
     * 				  its GUI-primitives and shared environment
     */
    public SwingRouting2(Node myNode) {
        this.myNode = myNode;

        /** Create a proxy for the application layer of this node */
        self = (RouteInterface)JistAPI.proxy(this, RouteInterface.class);

    }

    public SwingRouting2(Node myNode,Node[] NodeList)  {
        this.myNode = myNode;
        this.NodeList = NodeList;


        /** Create a proxy for the application layer of this node */
        self = (RouteInterface)JistAPI.proxy(this, RouteInterface.class);

    }

    public SwingRouting2(Node myNode,Node[] NodeList, StatsCollector stats,
                                    SequenceGenerator SequenceNumberGlobal) {
        this.myNode = myNode;
        this.NodeList = NodeList;
        this.stats = stats;
        this.sequenceNumberGlobal = SequenceNumberGlobal;


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
        								   CSGPColorProfile.RECEIVE, 20);

        // A message may come in a format that you define based
        // on your implementation needs
        // You must extract that format and act upon
        if (msg instanceof CSGPWrapperMessage) {
        	CSGPWrapperMessage msgNZ = (CSGPWrapperMessage)msg;
            sendToAppLayer(msgNZ.getPayload(), null);
            terkirim++;
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



        // If this message comes from App Layer
        if (!(((NetMessage.Ip)msg).getPayload() instanceof CSGPWrapperMessage))
        	return; // ignore non-specific messages

        CSGPWrapperMessage pesan;

        pesan= (CSGPWrapperMessage)  (((NetMessage.Ip)msg).getPayload());
         //NetAdrress[]

        // Message sudah pernah diterima
        if (this.listMessage.containsKey(pesan.messageID)){
           //
           //System.out.println("Node " + myNode.getID() + " drops double message " + pesan.messageID );
           // drop++;
            //pesan.messageID.concat("XX");
            //System.out.println("Message ID Baru " + pesan.messageID);

            return;

            //this.listMessage.put(pesan.messageID, null);
        }
        else{
            //masukkan
            this.listMessage.put(pesan.messageID, null);
        }


        // extract message
//        Location2D targetLocation = null;
//        CSGPWrapperMessage msgSW
//        	= (CSGPWrapperMessage)((NetMessage.Ip)msg).getPayload();
//
//        if (msgSW.getTargetRegion() != null)
//        	targetLocation = extractClosestVertex(msgSW.getTargetRegion());
//
//        if (msgSW.getTargetLocation() != null)
//        	targetLocation = msgSW.getTargetLocation();
//
//        if (msgSW.getPayload() instanceof MessageQuery) {
//           // System.out.println("Node " + myNode.getID() + " get query message");
//            handleQueryMessage(msg);
//        } else if (msgSW.getPayload() instanceof MessageDataValue) {
//            //System.out.println("Node " + myNode.getID() + " get data value message");
//
//            MessageDataValue isiData = (MessageDataValue) msgSW.getPayload();
//           // if pesan maju
//            handleMessageDataValue(msg, targetLocation);
//            //if pesan mundur karena back hole ?
//
//
//        } else {
//            //System.out.println("Node " + myNode.getID() + " get unknown message");
//        }
        // extract message
        Location2D targetLocation = null;
        Location2D targetLocationBack = null;
        CSGPWrapperMessage msgAGW = (CSGPWrapperMessage)((NetMessage.Ip)msg).getPayload();
        CSGPWrapperMessage msgBS = (CSGPWrapperMessage)((NetMessage.Ip)msg).getPayload();


        if (msgAGW.getTargetRegion() != null)
        	targetLocation = extractClosestVertex(msgAGW.getTargetRegion());

        if (msgAGW.getTargetLocation() != null)
        	targetLocation = msgAGW.getTargetLocation();

        if (msgAGW.getPayload() instanceof MessageQuery) {
            handleQueryMessage(msg);
        } else if (msgAGW.getPayload() instanceof MessageDataValue) {
            MessageDataValue isiData = (MessageDataValue) msgAGW.getPayload();
            MessageDataValue msgBSValue = (MessageDataValue) msgAGW.getPayload();
           // if pesan maju
            if (msgAGW.getTargetLocation() != null){
        	targetLocation = msgAGW.getTargetLocation();
                handleMessageDataValue(msg, targetLocation);
            }
//            //if pesan mundur karena back hole ?
//            if (msgBS.getTargetLocation() != null){
//        	targetLocationBack = msgBS.getTargetLocation();
//                handleMessageDataValue(msg, targetLocationBack);
//            }
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
//            int myIp = this.myNode.getID();
//            if (myIp == 199){
//                return;
//            }
//            else {

             //((MessageQuery)wrapperQuery.getPayload()).setTravelList(Integer.toString(myNode.getID()));
             CSGPWrapperMessage wrapperQuery
    	 	= (CSGPWrapperMessage)((NetMessage.Ip)msg).getPayload();



             this.availablePath = this.availablePath + ((MessageQuery) wrapperQuery.getPayload()).getTravelList()+"#";


        MessageQuery msgQuery = (MessageQuery) wrapperQuery.getPayload();
            ((MessageQuery)wrapperQuery.getPayload()).setTravelList(Integer.toString(myNode.getID()));

            CSGPWrapperMessage msgNewQuery
                	= new CSGPWrapperMessage(msgQuery, wrapperQuery.getTargetRegion(),
                							0, JistAPI.getTime());

            msgNewQuery.messageID= wrapperQuery.messageID;


            if (myNode.getID() == 77){
                 System.out.println(this.availablePath);
            }


             NetMessage.Ip copyOfMsg
              	= new NetMessage.Ip(msgNewQuery,
		       ((NetMessage.Ip)msg).getSrc(),
                       ((NetMessage.Ip)msg).getDst(),
                       ((NetMessage.Ip)msg).getProtocol(),
                       ((NetMessage.Ip)msg).getPriority(),
                       ((NetMessage.Ip)msg).getTTL(),
                       ((NetMessage.Ip)msg).getId(),
                       ((NetMessage.Ip)msg).getFragOffset());


             //kirim ke semua node tetangga

                sendToLinkLayer(copyOfMsg, NetAddress.ANY);

                if (this.myNode.getIP() != ((NetMessage.Ip)msg).getSrc()){
               sendToAppLayer(wrapperQuery.getPayload(),null);
            }
//        }

//        public handleQueryMessage {
//            private void NetMessage(String args[])
//            {
//                String str = "geekss@for@geekss";
//                String[] arrOfStr = str.split("@", -2);
//
//                for (String a : arrOfStr)
//                    System.out.println(a);
//            }
//        }


        //kirim ke semua node tetangga
            //disini berarti dihitung berapa lompatan, yang paling kecil dicatat


           // travelList.concat(msgQuery.getTravelList());


            //String listHop[] = travelList.split("#");
            //if (this.numHop > 9999) {
             //   this.numHop = listHop.length;
            //        this.travelPathtoSink = travelList;
           // }else
           // {
             //   if (this.numHop > listHop.length){
             //       this.numHop = listHop.length;
             //       this.travelPathtoSink = travelList;
            //    }
            //}

                //node yang menerima juga melakukan sensing maka kirim ke APP layer
            //if (this.myNode.getID() == 199)
              //  ((MessageQuery)wrapperQuery.getPayload()).setTravelListSink(Integer.toString(myNode.getID()));



        //System.out.println("Node " + myNode.getID() + " forwarding query msg to neigbours ");


     }


     private void handleQueryMessage2(NetMessage msg) {
         CSGPWrapperMessage msgSGP
    	 	= (CSGPWrapperMessage)((NetMessage.Ip)msg).getPayload();

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
              //dia sendiri adalah Cluster Head
        }


         //lempar ke layer APP untuk sensing lokasi node ini juga
         //jika ini bukan node sink (asumsi sink tidak sensing)
         if (this.myNode.getID() != 0)
            sendToAppLayer(msgSGP.getPayload(), null);
     }

//     private void handleQueryMessage3(NetMessage msg) {
//        sidnet.stack.users.csgp.routing.CSGPWrapperMessage msgAGW
//    	 	= (CSGPWrapperMessage)((NetMessage.Ip)msg).getPayload();
//
//          NetMessage.Ip copyOfMsg
//              	= new NetMessage.Ip(msgAGW,
//		       ((NetMessage.Ip)msg).getSrc(),
//                       ((NetMessage.Ip)msg).getDst(),
//                       ((NetMessage.Ip)msg).getProtocol(),
//                       ((NetMessage.Ip)msg).getPriority(),
//                       ((NetMessage.Ip)msg).getTTL(),
//                       ((NetMessage.Ip)msg).getId(),
//                       ((NetMessage.Ip)msg).getFragOffset(),
//                       ((NetMessage.Ip)msg).gettravelList());
//
//         //kirim ke semua node tetangga
//
//                sendToLinkLayer(copyOfMsg, NetAddress.ANY);
//
//        //System.out.println("Node " + myNode.getID() + " forwarding query msg to neigbours ");
//
//        // node yang menerima juga melakukan sensing maka kirim ke APP layer
//        if (this.myNode.getIP() != ((NetMessage.Ip)msg).getSrc())
//            sendToAppLayer(msgAGW.getPayload(), null);
//     }


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

          }
        }
        if (distToCluster < myNode.distToClusterCenter) {
           this.myNode.ChAddress= nextNode;
          return nextNode;
        }
        else
          return ipNode;
        // return null;
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


     private void handleMessageDataValueNormal(NetMessage msg, Location2D targetLocation) {

         //listJalur.add(myNode.getID());
         //jalur.add(myNode.getID());

        //System.out.println("size : " + jalur.size());
        //System.out.println("size lisJalur : " + listJalur.size());
         double[] listHole = new double[30];
         //LinkedList listJalur = new LinkedList();
         List<NetAddress> list = new ArrayList<NetAddress>();


         NetAddress forwardHop  = myNode.getIP();
         NetAddress BackHop  = myNode.getIP(); //pegiriman untuk hole

         CSGPWrapperMessage msgAGW
    	 	= (CSGPWrapperMessage)((NetMessage.Ip)msg).getPayload();

        MessageDataValue msgDataValue  = (MessageDataValue) msgAGW.getPayload();

        CSGPWrapperMessage msgBack
    	 	= (CSGPWrapperMessage)((NetMessage.Ip)msg).getPayload();

        MessageDataValue msgDataValueBack  = (MessageDataValue) msgBack.getPayload();

        //CSGPWrapperMessage msgAGW = (CSGPWrapperMessage)((NetMessage.Ip)msg).getPayload();
     //     System.out.println(targetLocation.getX()+" "+targetLocation.getY());

       //int queryId=msgDataValue.queryId;

       //long sequenceNumber    = sequenceNumberGlobal.getandincrement();
       Location2D sinkLocation=targetLocation; //masih perlu dideklarasikan dimana sinklocation tersebut


        NetAddress sinkIp= ((NetMessage.Ip)msg).getDst(); // tujuannya adalah sink node
        //NetAddress NodeSebelumnya = ((NetMessage.Ip)msg).getSrc(); //mengetahui node Sumber
        NetAddress backNode=null;


       // java.net.InetAddress = new
//NetAddress NodeSebelumnya = ((NetMessage.Ip)msg).getPathList(); //mengetahui path sebelumnya(type String harus dijadikan NetAdrress

      //pengecekan apakah belum dilakukan cek tetangga?
      //-----------------------------
      this.availablePathList = this.availablePathList + ((MessageDataValue) msgAGW.getPayload()).getPathList();
      jalurLagi = availablePathList.split(",", 100);
      //System.out.println("Node [" + myNode.getIP().toString() + "] have Path List "+availablePathList);

      if (Konstanta.modeRouting ==  1) {   //swing routing
          if ((!this.cekTetangga)){

              forwardingNodes=  this.getForwardNodeSwingWW(targetLocation); // ini untuk mengisi forwarding Node

               this.cekTetangga = true;
               if ( forwardingNodes.isEmpty()){
                   System.out.println("Ini Terdapat Node : " + myNode.getID() + " : hole ");
                   return;
                   //System.out.println(targetHop.getIP());
               }
              //targetHop = backNode;
              forwardHop = forwardingNodes.get(this.urutan);
              urutan= urutan+1;
              //jika urutan= panjang list
              if (urutan == forwardingNodes.size()){         // jika i = 0 maka next hop ada diurutan list yang ke nol
                  urutan =0;                                // maka urutan jadi 0 kembali
              }
                  // nexthop
               //langsung loooping
          } else {
              if ( forwardingNodes.isEmpty()){
                   System.out.println("Ini Terdapat Node : " + myNode.getID() + " : hole ");
                   return;
                   //System.out.println(targetHop.getIP());
               }

              else{
             // kerjakan looping untuk kirim
                forwardHop = forwardingNodes.get(this.urutan); // forwarding node sudah diisi
                 urutan= urutan+1;
                 //jika urutan= panjang list
                 if (urutan == forwardingNodes.size()){
                     urutan =0;
                 }
             }
          }
        } else if (Konstanta.modeRouting == 2) {
             //SGP

             forwardHop = this.ShortestGeoPathRouting(targetLocation);
             if (forwardHop == myNode.getIP()){
                //String tes  = ((MessageDataValue) wrapperDataValue.getPayload()).getPathList();
                //this.availablePathList = this.availablePathList + ((MessageDataValue) wrapperDataValue.getPayload()).getPathList()+"#";

                //LinkedList listJalur = new LinkedList();
                //listJalur.add(this.availablePathList);
                //System.out.println("data : " + tes);



                //System.out.println("Node [" + myNode.getID() + "] have Path List "+availablePathList);
                //System.out.println("Node Forward Terakhir: " + listJalur.toString());

                //targetHop = NodeSebelumnya;

                System.out.println("Terjadi Hole di Node : " + myNode.getID() + " : hole ");



              //   NetIp net = new NetIp(new NetAddress(id), this.p protMap, plIn, plOut);
//              backNode = netAddress
                //System.out.println("Node Forward Terakhir: " + jalur.get(jalur.size() - 1));
             }

         }

      //------------------------

        if((this.myNode.getIP() == sinkIp)) { //sink node
             sendToAppLayer(msgAGW.getPayload(), null);

        }
         if(myNode.neighboursList.contains(sinkIp)){            //jika tetangga terdapat Sink maka langsung kirim ke Sink
                 NetMessage.Ip copyOfMsg
              	= new NetMessage.Ip(msgAGW,
		       ((NetMessage.Ip)msg).getSrc(),
                       ((NetMessage.Ip)msg).getDst(),
                       ((NetMessage.Ip)msg).getProtocol(),
                       ((NetMessage.Ip)msg).getPriority(),
                       ((NetMessage.Ip)msg).getTTL(),
                       ((NetMessage.Ip)msg).getId(),
                       ((NetMessage.Ip)msg).getFragOffset());

                sendToLinkLayer(copyOfMsg, sinkIp);
                //-----------
             //      System.out.println("Node [" + myNode.getID() + "] forwarding Sensing Message to Sink Node ");
                //-----------

               //     terkirim++;
                return;
            }


              NetMessage.Ip copyOfMsg
              	= new NetMessage.Ip(msgAGW,
		       ((NetMessage.Ip)msg).getSrc(),
                       ((NetMessage.Ip)msg).getDst(),
                       ((NetMessage.Ip)msg).getProtocol(),
                       ((NetMessage.Ip)msg).getPriority(),
                       ((NetMessage.Ip)msg).getTTL(),
                       ((NetMessage.Ip)msg).getId(),
                       ((NetMessage.Ip)msg).getFragOffset());

                         sendToLinkLayer(copyOfMsg, forwardHop);

              //this.availablePathList = this.availablePathList + ((MessageDataValue) wrapperDataValue.getPayload()).getPathList()+"#";

              //LinkedList listJalur = new LinkedList();
              //listJalur.add(this.availablePathList);
              //listJalur.add(myNode.getID());

              //System.out.println("LinkedList:" + listJalur);


              MessageDataValue msgQuery = (MessageDataValue) msgAGW.getPayload();
            ((MessageDataValue)msgAGW.getPayload()).setPathList(myNode.getIP().toString());




              //------------
                 //System.out.println("Node [" + myNode.getID() + "] forwarding Sensing Message to neighbor "+targetHop.getIP());
                 //System.out.println("Node [" + myNode.getID() + "] have Path List "+availablePathList);
                 //System.out.println("Node Forward Terakhir: " + listJalur.getLast());
                 //listJalur.add(myNode.getID());
                // System.out.println("Node Forward Terakhir: " + availablePathList.length());
              //------------


     }

     private void handleMessageDataValueTanpaGantiMessageID(NetMessage msg, Location2D targetLocation) {
         double[] listHole = new double[30];
         List<NetAddress> list = new ArrayList<NetAddress>();

         NetAddress sinkIp= ((NetMessage.Ip)msg).getDst(); // tujuannya adalah sink node

         NetAddress targetHop  = myNode.getIP();
         CSGPWrapperMessage msgAGW = (CSGPWrapperMessage)((NetMessage.Ip)msg).getPayload();
         boolean pesanBack=false;

         MessageDataValue msgDataValue  = (MessageDataValue) msgAGW.getPayload();

         MessageDataValue msgBSValue = (MessageDataValue) msgAGW.getPayload();

         CSGPWrapperMessage msgBS = new CSGPWrapperMessage(msgBSValue, msgAGW.getTargetLocation(),0, JistAPI.getTime());

        if (msgAGW.getStatus() == false){ // jika bukan pesan hole


              Location2D sinkLocation=targetLocation; //masih perlu dideklarasikan dimana sinklocation tersebut
             //     System.out.println(targetLocation.getX()+" "+targetLocation.getY());

                //NetAddress sinkIp= ((NetMessage.Ip)msg).getDst(); // tujuannya adalah sink node
                NetAddress backNode=null;

              //pengecekan apakah belum dilakukan cek tetangga?
              //-----------------------------
                this.availablePathList = this.availablePathList + ((MessageDataValue) msgAGW.getPayload()).getPathList();
                jalurLagi = availablePathList.split(",", 100);
                //System.out.println("Node [" + myNode.getIP().toString() + "] have Path List "+availablePathList);


              if (Konstanta.modeRouting ==  1) {   //swing routing
                     if ((!this.cekTetangga)){
                         msgAGW.setStatus();
                     //  forwardingNodes= getForwardNode(targetLocation); // ini untuk mengisi forwarding Node

                      forwardingNodes=  this.getForwardNodeSwingWW(targetLocation); // ini untuk mengisi forwarding Node


                       this.cekTetangga = true;
                       if ( forwardingNodes.isEmpty()){
                           System.out.println("Node : " + myNode.getID() + " : hole ");

                           //System.out.println("Node Forward Terakhir: " + jalurLagi[jalurLagi.length - 1]);
                           String tempBackHole = jalurLagi[jalurLagi.length - 1];
                           //backNode = this.getBackNode(tempBackHole);
                           targetHop = this.GetForwardAfter2Hole(targetLocation);
                           //targetHop = this.getBackPath(targetLocation);
                           //msgDataValue.setPesanBackHole();
                           //msgDataValue.setPesanBackHole();
                           //System.out.println("Node berikutnya " + targetHop );
                           msgAGW.setStatus();

                           //msgAGW.messageID.replace('F','L');
                           //msgAGW.messageID.concat(jalurLagi[jalurLagi.length - 1]);

                           //long sequenceNumberBS    = sequenceNumberGlobal.getandincrement();

                           //MessageDataValue msgBSValue = (MessageDataValue) msgAGW.getPayload();
                    //msgDSValue.sequenceNumber=sequenceNumberDS;
                    //msgDSValue.setDirect();

                            //CSGPWrapperMessage msgBS = new CSGPWrapperMessage(msgBSValue, msgAGW.getTargetLocation(),0, JistAPI.getTime());

                            //msgBS.messageID = "BS# "+this.myNode.getIP().toString()+"#"+JistAPI.getTime();
                             NetMessage.Ip copyOfMsg
                              = new NetMessage.Ip(msgAGW,
                               ((NetMessage.Ip)msg).getSrc(),
                               ((NetMessage.Ip)msg).getDst(),
                               ((NetMessage.Ip)msg).getProtocol(),
                               ((NetMessage.Ip)msg).getPriority(),
                               ((NetMessage.Ip)msg).getTTL(),
                               ((NetMessage.Ip)msg).getId(),
                               ((NetMessage.Ip)msg).getFragOffset());

                        sendToLinkLayer(copyOfMsg, targetHop);
                        System.out.println("Node ID" + myNode.getID() + "-> " + targetHop.toString());

                           } else {
                                targetHop = forwardingNodes.get(this.urutan);
                                urutan= urutan+1;
                                //jika urutan= panjang list
                                if (urutan == forwardingNodes.size()){         // jika i = 0 maka next hop ada diurutan list yang ke nol
                                urutan =0;                                // maka urutan jadi 0 kembali
                                }
                            }

                          // nexthop
                       //langsung loooping
                  } else {
                     // kerjakan looping untuk kirim
                     //if jika targetHop = 1 maka jalankan 1 saja
                      //if targetHop = forwardingNodes

                      if ( forwardingNodes.isEmpty()){
                           System.out.println("Node : " + myNode.getID() + " : hole ");

                           //System.out.println("Node Forward Terakhir: " + jalurLagi[jalurLagi.length - 1]);
                           String tempBackHole = jalurLagi[jalurLagi.length - 1];
                           //backNode = this.getBackNode(tempBackHole);
                           targetHop = this.GetForwardAfter2Hole(targetLocation);
                           //targetHop = this.getBackPath(targetLocation);
                           //msgDataValue.setPesanBackHole();
                           //msgDataValue.setPesanBackHole();
                           msgAGW.setStatus();
                           //System.out.println("Status " + msgAGW.getStatus());
                           //System.out.println("Message ID lama " + msgAGW.messageID);
                           //msgAGW.messageID.replace('F','L');
                           //msgAGW.messageID.concat(jalurLagi[jalurLagi.length - 1]);

                           //long sequenceNumberBS    = sequenceNumberGlobal.getandincrement();

                           //MessageDataValue msgBSValue = (MessageDataValue) msgAGW.getPayload();
                    //msgDSValue.sequenceNumber=sequenceNumberDS;
                    //msgDSValue.setDirect();

                            //CSGPWrapperMessage msgBS = new CSGPWrapperMessage(msgBSValue, msgAGW.getTargetLocation(),0, JistAPI.getTime());

                            //msgBS.messageID = "BS# "+this.myNode.getIP().toString()+"#"+JistAPI.getTime();
                             NetMessage.Ip copyOfMsg
                              = new NetMessage.Ip(msgAGW,
                               ((NetMessage.Ip)msg).getSrc(),
                               ((NetMessage.Ip)msg).getDst(),
                               ((NetMessage.Ip)msg).getProtocol(),
                               ((NetMessage.Ip)msg).getPriority(),
                               ((NetMessage.Ip)msg).getTTL(),
                               ((NetMessage.Ip)msg).getId(),
                               ((NetMessage.Ip)msg).getFragOffset());

                        sendToLinkLayer(copyOfMsg, targetHop);
                        System.out.println("Node ID" + myNode.getID() + "-> " + targetHop.toString());
                        //System.out.println("Message ID lama " + msgBS.messageID);

                          } else{
                                targetHop = forwardingNodes.get(this.urutan); // forwarding node sudah diisi
                                urutan= urutan+1;
                                //jika urutan= panjang list
                                if (urutan == forwardingNodes.size()){
                                urutan =0;
                                }
                          }

                  }

                 } else if (Konstanta.modeRouting == 2) {
                     //SGP
                     //msgAGW.setStatus();

                     targetHop = this.ShortestGeoPathRouting(targetLocation);
                     if (targetHop == myNode.getIP()){

                         System.out.println("Node : " + myNode.getID() + " : hole ");
                         System.out.println("Node Forward Terakhir: " + jalurLagi[jalurLagi.length - 1]);
                         //BlacklistNode.add(this.myNode.getIP());
                         //String tempBackHole = jalurLagi[jalurLagi.length - 1];
                         //backNode = this.getBackNode(tempBackHole);
                         //targetHop = backNode;
                        targetHop = this.GetForwardAfter2Hole(targetLocation);
                         msgAGW.setStatus();


                            NetMessage.Ip copyOfMsg
                              = new NetMessage.Ip(msgAGW,
                               ((NetMessage.Ip)msg).getSrc(),
                               ((NetMessage.Ip)msg).getDst(),
                               ((NetMessage.Ip)msg).getProtocol(),
                               ((NetMessage.Ip)msg).getPriority(),
                               ((NetMessage.Ip)msg).getTTL(),
                               ((NetMessage.Ip)msg).getId(),
                               ((NetMessage.Ip)msg).getFragOffset());

                        sendToLinkLayer(copyOfMsg, targetHop);
                        System.out.println("Node ID" + myNode.getID() + "-> " + targetHop.toString());
                        //System.out.println("Message ID baru " + msgBS.messageID);

                     }

                 }

              //------------------------

         }
         else
         {

            //mencatat node yg kirim backNode ke dalam list blacklistNode
             //BlacklistNode.add(this.myNode.getIP());
            this.myNode.getIP();
             NetAddress backNode=null;
            this.availablePathList = this.availablePathList + ((MessageDataValue) msgAGW.getPayload()).getPathList();
            jalurLagi = availablePathList.split(",", 100);

            String tempBackHole = jalurLagi[jalurLagi.length - 1];
            backNode = this.getBackNode(tempBackHole);
            BlacklistNode.add(backNode);

            targetHop = this.ShortestGeoPathRouting(targetLocation);
            System.out.println("Node berikutnya " + targetHop );
            if (targetHop == myNode.getIP()){

                         System.out.println("Node : " + myNode.getID() + " : hole ");
                         //System.out.println("Node Forward Terakhir: " + jalurLagi[jalurLagi.length - 1]);
                         //BlacklistNode.add(this.myNode.getIP());
                         //String tempBackHole = jalurLagi[jalurLagi.length - 1];
                         //backNode = this.getBackNode(tempBackHole);
                         //targetHop = backNode;
                        targetHop = this.GetForwardAfter2Hole(targetLocation);
                         msgAGW.setStatus();

                         if((this.myNode.getIP() == sinkIp)) { //sink node
                     sendToAppLayer(msgAGW.getPayload(), null);

                }
                 if(myNode.neighboursList.contains(sinkIp)){            //jika tetangga terdapat Sink maka langsung kirim ke Sink
                         NetMessage.Ip copyOfMsg
                        = new NetMessage.Ip(msgAGW,
                               ((NetMessage.Ip)msg).getSrc(),
                               ((NetMessage.Ip)msg).getDst(),
                               ((NetMessage.Ip)msg).getProtocol(),
                               ((NetMessage.Ip)msg).getPriority(),
                               ((NetMessage.Ip)msg).getTTL(),
                               ((NetMessage.Ip)msg).getId(),
                               ((NetMessage.Ip)msg).getFragOffset());

                        sendToLinkLayer(copyOfMsg, sinkIp);
                        //System.out.println("Node ID" + myNode.getID() + "-> " + targetHop.toString());
                        //-----------
                           System.out.println("Node [" + myNode.getID() + "] forwarding Sensing Message to Sink Node ");
                        //-----------

                       //     terkirim++;
                        return;
                    }

                            NetMessage.Ip copyOfMsg
                              = new NetMessage.Ip(msgAGW,
                               ((NetMessage.Ip)msg).getSrc(),
                               ((NetMessage.Ip)msg).getDst(),
                               ((NetMessage.Ip)msg).getProtocol(),
                               ((NetMessage.Ip)msg).getPriority(),
                               ((NetMessage.Ip)msg).getTTL(),
                               ((NetMessage.Ip)msg).getId(),
                               ((NetMessage.Ip)msg).getFragOffset());

                        sendToLinkLayer(copyOfMsg, targetHop);
                        //System.out.println("Message ID baru " + msgBS.messageID);
                        System.out.println("Node ID" + myNode.getID() + "-> " + targetHop.toString());
                     }
             if  (BlacklistNode.contains(targetHop)){

                 targetHop = this.GetForwardAfterHole(targetLocation);


                 NetMessage.Ip copyOfMsg
                        = new NetMessage.Ip(msgAGW,
                               ((NetMessage.Ip)msg).getSrc(),
                               ((NetMessage.Ip)msg).getDst(),
                               ((NetMessage.Ip)msg).getProtocol(),
                               ((NetMessage.Ip)msg).getPriority(),
                               ((NetMessage.Ip)msg).getTTL(),
                               ((NetMessage.Ip)msg).getId(),
                               ((NetMessage.Ip)msg).getFragOffset());

                                 sendToLinkLayer(copyOfMsg, targetHop);
                                 System.out.println("Node ID" + myNode.getID() + "-> " + targetHop.toString());
                                 return;
             }

             //NetAddress sumber = ((NetMessage.Ip)msg).getSrc();
             //System.out.println("Node Sumber "+sumber);

              //NetAddress BlackListHole= ((NetMessage.Ip)msg).getSrc();
              //buat sorting untuk forward agar tidak mengirim ke list blacklist
              //kirim ke forwarding lain
              //System.out.println("Masuk Pesan Balik");



              //System.out.println("Node yg di Blacklist: " + BlacklistNode);

               //targetHop = this.getBackPath(targetLocation);

               //System.out.println("Status " + msgAGW.getStatus());

               //System.out.println("Target Node berikutnya: " + targetHop);

//               NetMessage.Ip copyOfMsg
//                        = new NetMessage.Ip(msgBS,
//                               ((NetMessage.Ip)msg).getSrc(),
//                               ((NetMessage.Ip)msg).getDst(),
//                               ((NetMessage.Ip)msg).getProtocol(),
//                               ((NetMessage.Ip)msg).getPriority(),
//                               ((NetMessage.Ip)msg).getTTL(),
//                               ((NetMessage.Ip)msg).getId(),
//                               ((NetMessage.Ip)msg).getFragOffset());
//
//                                 sendToLinkLayer(copyOfMsg, targetHop);
         }


        if((this.myNode.getIP() == sinkIp)) { //sink node
                     sendToAppLayer(msgAGW.getPayload(), null);

                }
                 if(myNode.neighboursList.contains(sinkIp)){            //jika tetangga terdapat Sink maka langsung kirim ke Sink
                         NetMessage.Ip copyOfMsg
                        = new NetMessage.Ip(msgAGW,
                               ((NetMessage.Ip)msg).getSrc(),
                               ((NetMessage.Ip)msg).getDst(),
                               ((NetMessage.Ip)msg).getProtocol(),
                               ((NetMessage.Ip)msg).getPriority(),
                               ((NetMessage.Ip)msg).getTTL(),
                               ((NetMessage.Ip)msg).getId(),
                               ((NetMessage.Ip)msg).getFragOffset());

                        sendToLinkLayer(copyOfMsg, sinkIp);
                        //-----------
                           System.out.println("Node [" + myNode.getID() + "] forwarding Sensing Message to Sink Node ");
                        //-----------

                       //     terkirim++;
                        return;
                    }

                      NetMessage.Ip copyOfMsg
                        = new NetMessage.Ip(msgAGW,
                               ((NetMessage.Ip)msg).getSrc(),
                               ((NetMessage.Ip)msg).getDst(),
                               ((NetMessage.Ip)msg).getProtocol(),
                               ((NetMessage.Ip)msg).getPriority(),
                               ((NetMessage.Ip)msg).getTTL(),
                               ((NetMessage.Ip)msg).getId(),
                               ((NetMessage.Ip)msg).getFragOffset());

                                 sendToLinkLayer(copyOfMsg, targetHop);
                                 System.out.println("Node ID" + myNode.getID() + "-> " + targetHop.toString());
                      //------------
                         //System.out.println("Node [" + myNode.getID() + "] forwarding Sensing Message to neighbor "+targetHop.getIP());
                      //------------

                    MessageDataValue msgQuery = (MessageDataValue) msgAGW.getPayload();
                    ((MessageDataValue)msgAGW.getPayload()).setPathList(myNode.getIP().toString());

    }

     private void handleMessageDataValueFINAL(NetMessage msg, Location2D targetLocation) {
         double[] listHole = new double[30];
         List<NetAddress> list = new ArrayList<NetAddress>();

         NetAddress sinkIp= ((NetMessage.Ip)msg).getDst(); // tujuannya adalah sink node

         NetAddress targetHop  = myNode.getIP();
         CSGPWrapperMessage msgAGW = (CSGPWrapperMessage)((NetMessage.Ip)msg).getPayload();
         boolean pesanBack=false;

         MessageDataValue msgDataValue  = (MessageDataValue) msgAGW.getPayload();

         MessageDataValue msgBSValue = (MessageDataValue) msgAGW.getPayload();

         CSGPWrapperMessage msgBS = new CSGPWrapperMessage(msgBSValue, msgAGW.getTargetLocation(),0, JistAPI.getTime());

        if (msgAGW.getStatus() == false){ // jika bukan pesan hole


              Location2D sinkLocation=targetLocation; //masih perlu dideklarasikan dimana sinklocation tersebut
             //     System.out.println(targetLocation.getX()+" "+targetLocation.getY());

                //NetAddress sinkIp= ((NetMessage.Ip)msg).getDst(); // tujuannya adalah sink node
                NetAddress backNode=null;

              //pengecekan apakah belum dilakukan cek tetangga?
              //-----------------------------
                this.availablePathList = this.availablePathList + ((MessageDataValue) msgAGW.getPayload()).getPathList();
                jalurLagi = availablePathList.split(",", 100);
                //System.out.println("Node [" + myNode.getIP().toString() + "] have Path List "+availablePathList);


              if (Konstanta.modeRouting ==  1) {   //swing routing
                     if ((!this.cekTetangga)){
                         msgAGW.setStatus();
                     //  forwardingNodes= getForwardNode(targetLocation); // ini untuk mengisi forwarding Node

                      forwardingNodes=  this.getForwardNodeSwingWW(targetLocation); // ini untuk mengisi forwarding Node


                       this.cekTetangga = true;
                       if ( forwardingNodes.isEmpty()){
                           System.out.println("Node : " + myNode.getID() + " : hole ");

                           System.out.println("Node Forward Terakhir: " + jalurLagi[jalurLagi.length - 1]);
                           String tempBackHole = jalurLagi[jalurLagi.length - 1];
                           backNode = this.getBackNode(tempBackHole);
                           targetHop = backNode;
                           //targetHop = this.getBackPath(targetLocation);
                           //msgDataValue.setPesanBackHole();
                           //msgDataValue.setPesanBackHole();
                           msgAGW.setStatus();
                           System.out.println("Status " + msgAGW.getStatus());
                           //System.out.println("Message ID lama " + msgAGW.messageID);
                           //msgAGW.messageID.replace('F','L');
                           //msgAGW.messageID.concat(jalurLagi[jalurLagi.length - 1]);

                           //long sequenceNumberBS    = sequenceNumberGlobal.getandincrement();

                           //MessageDataValue msgBSValue = (MessageDataValue) msgAGW.getPayload();
                    //msgDSValue.sequenceNumber=sequenceNumberDS;
                    //msgDSValue.setDirect();

                            //CSGPWrapperMessage msgBS = new CSGPWrapperMessage(msgBSValue, msgAGW.getTargetLocation(),0, JistAPI.getTime());

                            msgBS.messageID = "BS# "+this.myNode.getIP().toString()+"#"+JistAPI.getTime();
                             NetMessage.Ip copyOfMsg
                              = new NetMessage.Ip(msgBS,
                               ((NetMessage.Ip)msg).getSrc(),
                               ((NetMessage.Ip)msg).getDst(),
                               ((NetMessage.Ip)msg).getProtocol(),
                               ((NetMessage.Ip)msg).getPriority(),
                               ((NetMessage.Ip)msg).getTTL(),
                               ((NetMessage.Ip)msg).getId(),
                               ((NetMessage.Ip)msg).getFragOffset());

                        sendToLinkLayer(copyOfMsg, targetHop);
                        //System.out.println("Message ID lama " + msgBS.messageID);

                           } else {
                                targetHop = forwardingNodes.get(this.urutan);
                                urutan= urutan+1;
                                //jika urutan= panjang list
                                if (urutan == forwardingNodes.size()){         // jika i = 0 maka next hop ada diurutan list yang ke nol
                                urutan =0;                                // maka urutan jadi 0 kembali
                                }
                            }

                          // nexthop
                       //langsung loooping
                  } else {
                     // kerjakan looping untuk kirim
                     //if jika targetHop = 1 maka jalankan 1 saja
                      //if targetHop = forwardingNodes

                      if ( forwardingNodes.isEmpty()){
                           System.out.println("Node : " + myNode.getID() + " : hole ");

                           System.out.println("Node Forward Terakhir: " + jalurLagi[jalurLagi.length - 1]);
                           String tempBackHole = jalurLagi[jalurLagi.length - 1];
                           backNode = this.getBackNode(tempBackHole);
                           targetHop = backNode;
                           //targetHop = this.getBackPath(targetLocation);
                           //msgDataValue.setPesanBackHole();
                           //msgDataValue.setPesanBackHole();
                           msgAGW.setStatus();
                           System.out.println("Status " + msgAGW.getStatus());
                           System.out.println("Message ID lama " + msgAGW.messageID);
                           //msgAGW.messageID.replace('F','L');
                           //msgAGW.messageID.concat(jalurLagi[jalurLagi.length - 1]);

                           //long sequenceNumberBS    = sequenceNumberGlobal.getandincrement();

                           //MessageDataValue msgBSValue = (MessageDataValue) msgAGW.getPayload();
                    //msgDSValue.sequenceNumber=sequenceNumberDS;
                    //msgDSValue.setDirect();

                            //CSGPWrapperMessage msgBS = new CSGPWrapperMessage(msgBSValue, msgAGW.getTargetLocation(),0, JistAPI.getTime());

                            msgBS.messageID = "BS# "+this.myNode.getIP().toString()+"#"+JistAPI.getTime();
                             NetMessage.Ip copyOfMsg
                              = new NetMessage.Ip(msgBS,
                               ((NetMessage.Ip)msg).getSrc(),
                               ((NetMessage.Ip)msg).getDst(),
                               ((NetMessage.Ip)msg).getProtocol(),
                               ((NetMessage.Ip)msg).getPriority(),
                               ((NetMessage.Ip)msg).getTTL(),
                               ((NetMessage.Ip)msg).getId(),
                               ((NetMessage.Ip)msg).getFragOffset());

                        sendToLinkLayer(copyOfMsg, targetHop);
                        System.out.println("Message ID lama " + msgBS.messageID);

                          } else{
                                targetHop = forwardingNodes.get(this.urutan); // forwarding node sudah diisi
                                urutan= urutan+1;
                                //jika urutan= panjang list
                                if (urutan == forwardingNodes.size()){
                                urutan =0;
                                }
                          }

                  }

                 } else if (Konstanta.modeRouting == 2) {
                     //SGP
                     //msgAGW.setStatus();

                     targetHop = this.ShortestGeoPathRouting(targetLocation);
                     if (targetHop == myNode.getIP()){

                         System.out.println("Node : " + myNode.getID() + " : hole ");
                         System.out.println("Node Forward Terakhir: " + jalurLagi[jalurLagi.length - 1]);
                         //BlacklistNode.add(this.myNode.getIP());
                         //String tempBackHole = jalurLagi[jalurLagi.length - 1];
                         //backNode = this.getBackNode(tempBackHole);
                         //targetHop = backNode;
                        targetHop = this.GetForwardAfter2Hole(targetLocation);
                        // msgAGW.setStatus();

                           msgBS.setStatus();
                           //MessageDataValue msgBSValue = (MessageDataValue) msgAGW.getPayload();

                            //CSGPWrapperMessage msgBS = new CSGPWrapperMessage(msgBSValue, msgAGW.getTargetLocation(),0, JistAPI.getTime());
                            //ubah pesan menjadi pesan Back Send
                            msgBS.messageID = "BS# "+this.myNode.getIP().toString()+"#"+JistAPI.getTime();


                            NetMessage.Ip copyOfMsg
                              = new NetMessage.Ip(msgBS,
                               ((NetMessage.Ip)msg).getSrc(),
                               ((NetMessage.Ip)msg).getDst(),
                               ((NetMessage.Ip)msg).getProtocol(),
                               ((NetMessage.Ip)msg).getPriority(),
                               ((NetMessage.Ip)msg).getTTL(),
                               ((NetMessage.Ip)msg).getId(),
                               ((NetMessage.Ip)msg).getFragOffset());

                        sendToLinkLayer(copyOfMsg, targetHop);
                        //System.out.println("Message ID baru " + msgBS.messageID);

                     }

                 }

              //------------------------

         }
         else
         {

            //mencatat node yg kirim backNode ke dalam list blacklistNode
             //BlacklistNode.add(this.myNode.getIP());
            this.myNode.getIP();
             NetAddress backNode=null;
            this.availablePathList = this.availablePathList + ((MessageDataValue) msgAGW.getPayload()).getPathList();
            jalurLagi = availablePathList.split(",", 100);

            String tempBackHole = jalurLagi[jalurLagi.length - 1];
            backNode = this.getBackNode(tempBackHole);
            BlacklistNode.add(backNode);

            targetHop = this.ShortestGeoPathRouting(targetLocation);

             if  (BlacklistNode.contains(targetHop)){

                 targetHop = this.GetForwardAfterHole(targetLocation);


                 NetMessage.Ip copyOfMsg
                        = new NetMessage.Ip(msgAGW,
                               ((NetMessage.Ip)msg).getSrc(),
                               ((NetMessage.Ip)msg).getDst(),
                               ((NetMessage.Ip)msg).getProtocol(),
                               ((NetMessage.Ip)msg).getPriority(),
                               ((NetMessage.Ip)msg).getTTL(),
                               ((NetMessage.Ip)msg).getId(),
                               ((NetMessage.Ip)msg).getFragOffset());

                                 sendToLinkLayer(copyOfMsg, targetHop);
                    return;
             }

             //NetAddress sumber = ((NetMessage.Ip)msg).getSrc();
             //System.out.println("Node Sumber "+sumber);

              //NetAddress BlackListHole= ((NetMessage.Ip)msg).getSrc();
              //buat sorting untuk forward agar tidak mengirim ke list blacklist
              //kirim ke forwarding lain
              System.out.println("Masuk Pesan Balik");



              System.out.println("Node yg di Blacklist: " + BlacklistNode);

               //targetHop = this.getBackPath(targetLocation);

               //System.out.println("Status " + msgAGW.getStatus());

               //System.out.println("Target Node berikutnya: " + targetHop);

//               NetMessage.Ip copyOfMsg
//                        = new NetMessage.Ip(msgBS,
//                               ((NetMessage.Ip)msg).getSrc(),
//                               ((NetMessage.Ip)msg).getDst(),
//                               ((NetMessage.Ip)msg).getProtocol(),
//                               ((NetMessage.Ip)msg).getPriority(),
//                               ((NetMessage.Ip)msg).getTTL(),
//                               ((NetMessage.Ip)msg).getId(),
//                               ((NetMessage.Ip)msg).getFragOffset());
//
//                                 sendToLinkLayer(copyOfMsg, targetHop);
         }


        if((this.myNode.getIP() == sinkIp)) { //sink node
                     sendToAppLayer(msgAGW.getPayload(), null);

                }
                 if(myNode.neighboursList.contains(sinkIp)){            //jika tetangga terdapat Sink maka langsung kirim ke Sink
                         NetMessage.Ip copyOfMsg
                        = new NetMessage.Ip(msgAGW,
                               ((NetMessage.Ip)msg).getSrc(),
                               ((NetMessage.Ip)msg).getDst(),
                               ((NetMessage.Ip)msg).getProtocol(),
                               ((NetMessage.Ip)msg).getPriority(),
                               ((NetMessage.Ip)msg).getTTL(),
                               ((NetMessage.Ip)msg).getId(),
                               ((NetMessage.Ip)msg).getFragOffset());

                        sendToLinkLayer(copyOfMsg, sinkIp);
                        //-----------
                           //System.out.println("Node [" + myNode.getID() + "] forwarding Sensing Message to Sink Node ");
                        //-----------

                       //     terkirim++;
                        return;
                    }

                      NetMessage.Ip copyOfMsg
                        = new NetMessage.Ip(msgAGW,
                               ((NetMessage.Ip)msg).getSrc(),
                               ((NetMessage.Ip)msg).getDst(),
                               ((NetMessage.Ip)msg).getProtocol(),
                               ((NetMessage.Ip)msg).getPriority(),
                               ((NetMessage.Ip)msg).getTTL(),
                               ((NetMessage.Ip)msg).getId(),
                               ((NetMessage.Ip)msg).getFragOffset());

                                 sendToLinkLayer(copyOfMsg, targetHop);

                      //------------
                         //System.out.println("Node [" + myNode.getID() + "] forwarding Sensing Message to neighbor "+targetHop.getIP());
                      //------------

                    MessageDataValue msgQuery = (MessageDataValue) msgAGW.getPayload();
                    ((MessageDataValue)msgAGW.getPayload()).setPathList(myNode.getIP().toString());

    }


     private void handleMessageDataValue(NetMessage msg, Location2D targetLocation) {
         double[] listHole = new double[30];
         List<NetAddress> list = new ArrayList<NetAddress>();

         NetAddress sinkIp= ((NetMessage.Ip)msg).getDst(); // tujuannya adalah sink node

         NetAddress targetHop  = myNode.getIP();
         CSGPWrapperMessage msgAGW = (CSGPWrapperMessage)((NetMessage.Ip)msg).getPayload();
         boolean pesanBack=false;

         MessageDataValue msgDataValue  = (MessageDataValue) msgAGW.getPayload();

         MessageDataValue msgBSValue = (MessageDataValue) msgAGW.getPayload();

         CSGPWrapperMessage msgBS = new CSGPWrapperMessage(msgBSValue, msgAGW.getTargetLocation(),0, JistAPI.getTime());

        if (msgAGW.getStatus() == false){ // jika bukan pesan hole


              Location2D sinkLocation=targetLocation; //masih perlu dideklarasikan dimana sinklocation tersebut
             //     System.out.println(targetLocation.getX()+" "+targetLocation.getY());

                //NetAddress sinkIp= ((NetMessage.Ip)msg).getDst(); // tujuannya adalah sink node
                NetAddress backNode=null;

              //pengecekan apakah belum dilakukan cek tetangga?
              //-----------------------------
                this.availablePathList = this.availablePathList + ((MessageDataValue) msgAGW.getPayload()).getPathList();
                jalurLagi = availablePathList.split(",", 100);
                //System.out.println("Node [" + myNode.getIP().toString() + "] have Path List "+availablePathList);


              if (Konstanta.modeRouting ==  1) {   //swing routing
                     if ((!this.cekTetangga)){
                         msgAGW.setStatus();
                     //  forwardingNodes= getForwardNode(targetLocation); // ini untuk mengisi forwarding Node

                      forwardingNodes=  this.getForwardNodeSwingWW(targetLocation); // ini untuk mengisi forwarding Node


                       this.cekTetangga = true;
                       if ( forwardingNodes.isEmpty()){
                           System.out.println("Node : " + myNode.getID() + " : hole ");

                           //System.out.println("Node Forward Terakhir: " + jalurLagi[jalurLagi.length - 1]);
                           String tempBackHole = jalurLagi[jalurLagi.length - 1];
                           //backNode = this.getBackNode(tempBackHole);
                           targetHop = this.GetForwardAfter2Hole(targetLocation);
                           //targetHop = this.getBackPath(targetLocation);
                           //msgDataValue.setPesanBackHole();
                           //msgDataValue.setPesanBackHole();
                           msgBS.setStatus();
                           //System.out.println("Status " + msgAGW.getStatus());
                           //System.out.println("Message ID lama " + msgAGW.messageID);
                           //msgAGW.messageID.replace('F','L');
                           //msgAGW.messageID.concat(jalurLagi[jalurLagi.length - 1]);

                           //long sequenceNumberBS    = sequenceNumberGlobal.getandincrement();

                           //MessageDataValue msgBSValue = (MessageDataValue) msgAGW.getPayload();
                    //msgDSValue.sequenceNumber=sequenceNumberDS;
                    //msgDSValue.setDirect();

                            //CSGPWrapperMessage msgBS = new CSGPWrapperMessage(msgBSValue, msgAGW.getTargetLocation(),0, JistAPI.getTime());

                            msgBS.messageID = "BS# "+this.myNode.getIP().toString()+"#"+JistAPI.getTime();
                             NetMessage.Ip copyOfMsg
                              = new NetMessage.Ip(msgBS,
                               ((NetMessage.Ip)msg).getSrc(),
                               ((NetMessage.Ip)msg).getDst(),
                               ((NetMessage.Ip)msg).getProtocol(),
                               ((NetMessage.Ip)msg).getPriority(),
                               ((NetMessage.Ip)msg).getTTL(),
                               ((NetMessage.Ip)msg).getId(),
                               ((NetMessage.Ip)msg).getFragOffset());

                        sendToLinkLayer(copyOfMsg, targetHop);
                        //System.out.println("Message ID lama " + msgBS.messageID);

                           } else {
                                targetHop = forwardingNodes.get(this.urutan);
                                urutan= urutan+1;
                                //jika urutan= panjang list
                                if (urutan == forwardingNodes.size()){         // jika i = 0 maka next hop ada diurutan list yang ke nol
                                urutan =0;                                // maka urutan jadi 0 kembali
                                }
                            }

                          // nexthop
                       //langsung loooping
                  } else {
                     // kerjakan looping untuk kirim
                     //if jika targetHop = 1 maka jalankan 1 saja
                      //if targetHop = forwardingNodes

                      if ( forwardingNodes.isEmpty()){
                           System.out.println("Node : " + myNode.getID() + " : hole ");

                           //System.out.println("Node Forward Terakhir: " + jalurLagi[jalurLagi.length - 1]);
                           String tempBackHole = jalurLagi[jalurLagi.length - 1];
                           //backNode = this.getBackNode(tempBackHole);
                           //targetHop = backNode;
                           targetHop = this.GetForwardAfter2Hole(targetLocation);
                            //targetHop = this.getBackPath(targetLocation);
                           //msgDataValue.setPesanBackHole();
                           //msgDataValue.setPesanBackHole();
                           msgBS.setStatus();
                           //System.out.println("Status " + msgAGW.getStatus());
                           //System.out.println("Message ID lama " + msgAGW.messageID);
                           //msgAGW.messageID.replace('F','L');
                           //msgAGW.messageID.concat(jalurLagi[jalurLagi.length - 1]);

                           //long sequenceNumberBS    = sequenceNumberGlobal.getandincrement();

                           //MessageDataValue msgBSValue = (MessageDataValue) msgAGW.getPayload();
                    //msgDSValue.sequenceNumber=sequenceNumberDS;
                    //msgDSValue.setDirect();

                            //CSGPWrapperMessage msgBS = new CSGPWrapperMessage(msgBSValue, msgAGW.getTargetLocation(),0, JistAPI.getTime());

                            msgBS.messageID = "BS# "+this.myNode.getIP().toString()+"#"+JistAPI.getTime();
                             NetMessage.Ip copyOfMsg
                              = new NetMessage.Ip(msgBS,
                               ((NetMessage.Ip)msg).getSrc(),
                               ((NetMessage.Ip)msg).getDst(),
                               ((NetMessage.Ip)msg).getProtocol(),
                               ((NetMessage.Ip)msg).getPriority(),
                               ((NetMessage.Ip)msg).getTTL(),
                               ((NetMessage.Ip)msg).getId(),
                               ((NetMessage.Ip)msg).getFragOffset());

                        sendToLinkLayer(copyOfMsg, targetHop);
                        //System.out.println("Message ID lama " + msgBS.messageID);

                          } else{
                                targetHop = forwardingNodes.get(this.urutan); // forwarding node sudah diisi
                                urutan= urutan+1;
                                //jika urutan= panjang list
                                if (urutan == forwardingNodes.size()){
                                urutan =0;
                                }
                          }

                  }

                 } else if (Konstanta.modeRouting == 2) {
                     //SGP
                     //msgAGW.setStatus();

                     targetHop = this.ShortestGeoPathRouting(targetLocation);
                     if (targetHop == myNode.getIP()){

                         System.out.println("Node : " + myNode.getID() + " : hole ");
                         //System.out.println("Node Forward Terakhir: " + jalurLagi[jalurLagi.length - 1]);
                         //BlacklistNode.add(this.myNode.getIP());
                         //String tempBackHole = jalurLagi[jalurLagi.length - 1];
                         //backNode = this.getBackNode(tempBackHole);
                         //targetHop = backNode;
                        targetHop = this.GetForwardAfter2Hole(targetLocation);
                        // msgAGW.setStatus();

                           msgBS.setStatus();
                           //MessageDataValue msgBSValue = (MessageDataValue) msgAGW.getPayload();

                            //CSGPWrapperMessage msgBS = new CSGPWrapperMessage(msgBSValue, msgAGW.getTargetLocation(),0, JistAPI.getTime());
                            //ubah pesan menjadi pesan Back Send
                            msgBS.messageID = "BS# "+this.myNode.getIP().toString()+"#"+JistAPI.getTime();


                            NetMessage.Ip copyOfMsg
                              = new NetMessage.Ip(msgBS,
                               ((NetMessage.Ip)msg).getSrc(),
                               ((NetMessage.Ip)msg).getDst(),
                               ((NetMessage.Ip)msg).getProtocol(),
                               ((NetMessage.Ip)msg).getPriority(),
                               ((NetMessage.Ip)msg).getTTL(),
                               ((NetMessage.Ip)msg).getId(),
                               ((NetMessage.Ip)msg).getFragOffset());

                        sendToLinkLayer(copyOfMsg, targetHop);
                        //System.out.println("Message ID baru " + msgBS.messageID);

                     }

                 }

              //------------------------

         }
         else
         {

            //mencatat node yg kirim backNode ke dalam list blacklistNode
             //BlacklistNode.add(this.myNode.getIP());
            this.myNode.getIP();
             NetAddress backNode=null;
            this.availablePathList = this.availablePathList + ((MessageDataValue) msgAGW.getPayload()).getPathList();
            jalurLagi = availablePathList.split(",", 100);

            String tempBackHole = jalurLagi[jalurLagi.length - 1];
            backNode = this.getBackNode(tempBackHole);
            BlacklistNode.add(backNode);

            targetHop = this.ShortestGeoPathRouting(targetLocation);

            if (targetHop == myNode.getIP()){

                         System.out.println("Node : " + myNode.getID() + " : hole ");
                         //System.out.println("Node Forward Terakhir: " + jalurLagi[jalurLagi.length - 1]);
                         //BlacklistNode.add(this.myNode.getIP());
                         //String tempBackHole = jalurLagi[jalurLagi.length - 1];
                         //backNode = this.getBackNode(tempBackHole);
                         //targetHop = backNode;
                        targetHop = this.GetForwardAfter2Hole(targetLocation);
                         msgBS.setStatus();

                         if((this.myNode.getIP() == sinkIp)) { //sink node
                     sendToAppLayer(msgAGW.getPayload(), null);

                }
                 if(myNode.neighboursList.contains(sinkIp)){            //jika tetangga terdapat Sink maka langsung kirim ke Sink
                         NetMessage.Ip copyOfMsg
                        = new NetMessage.Ip(msgBS,
                               ((NetMessage.Ip)msg).getSrc(),
                               ((NetMessage.Ip)msg).getDst(),
                               ((NetMessage.Ip)msg).getProtocol(),
                               ((NetMessage.Ip)msg).getPriority(),
                               ((NetMessage.Ip)msg).getTTL(),
                               ((NetMessage.Ip)msg).getId(),
                               ((NetMessage.Ip)msg).getFragOffset());

                        sendToLinkLayer(copyOfMsg, sinkIp);
                        //System.out.println("Node ID" + myNode.getID() + "-> " + targetHop.toString());
                        //-----------
                           System.out.println("Node [" + myNode.getID() + "] forwarding Sensing Message to Sink Node ");
                        //-----------

                       //     terkirim++;
                        return;
                    }

                            NetMessage.Ip copyOfMsg
                              = new NetMessage.Ip(msgBS,
                               ((NetMessage.Ip)msg).getSrc(),
                               ((NetMessage.Ip)msg).getDst(),
                               ((NetMessage.Ip)msg).getProtocol(),
                               ((NetMessage.Ip)msg).getPriority(),
                               ((NetMessage.Ip)msg).getTTL(),
                               ((NetMessage.Ip)msg).getId(),
                               ((NetMessage.Ip)msg).getFragOffset());

                        sendToLinkLayer(copyOfMsg, targetHop);
                        //System.out.println("Message ID baru " + msgBS.messageID);
                        System.out.println("Node ID" + myNode.getID() + "-> " + targetHop.toString());
                     }

             if  (BlacklistNode.contains(targetHop)){

                 targetHop = this.GetForwardAfterHole(targetLocation);


                 NetMessage.Ip copyOfMsg
                        = new NetMessage.Ip(msgBS,
                               ((NetMessage.Ip)msg).getSrc(),
                               ((NetMessage.Ip)msg).getDst(),
                               ((NetMessage.Ip)msg).getProtocol(),
                               ((NetMessage.Ip)msg).getPriority(),
                               ((NetMessage.Ip)msg).getTTL(),
                               ((NetMessage.Ip)msg).getId(),
                               ((NetMessage.Ip)msg).getFragOffset());

                                 sendToLinkLayer(copyOfMsg, targetHop);
                    return;
             }

             //NetAddress sumber = ((NetMessage.Ip)msg).getSrc();
             //System.out.println("Node Sumber "+sumber);

              //NetAddress BlackListHole= ((NetMessage.Ip)msg).getSrc();
              //buat sorting untuk forward agar tidak mengirim ke list blacklist
              //kirim ke forwarding lain
              //System.out.println("Masuk Pesan Balik");



              //System.out.println("Node yg di Blacklist: " + BlacklistNode);

               //targetHop = this.getBackPath(targetLocation);

               //System.out.println("Status " + msgAGW.getStatus());

               //System.out.println("Target Node berikutnya: " + targetHop);

//               NetMessage.Ip copyOfMsg
//                        = new NetMessage.Ip(msgBS,
//                               ((NetMessage.Ip)msg).getSrc(),
//                               ((NetMessage.Ip)msg).getDst(),
//                               ((NetMessage.Ip)msg).getProtocol(),
//                               ((NetMessage.Ip)msg).getPriority(),
//                               ((NetMessage.Ip)msg).getTTL(),
//                               ((NetMessage.Ip)msg).getId(),
//                               ((NetMessage.Ip)msg).getFragOffset());
//
//                                 sendToLinkLayer(copyOfMsg, targetHop);
         }


        if((this.myNode.getIP() == sinkIp)) { //sink node
                     sendToAppLayer(msgAGW.getPayload(), null);

                }
                 if(myNode.neighboursList.contains(sinkIp)){            //jika tetangga terdapat Sink maka langsung kirim ke Sink
                         NetMessage.Ip copyOfMsg
                        = new NetMessage.Ip(msgAGW,
                               ((NetMessage.Ip)msg).getSrc(),
                               ((NetMessage.Ip)msg).getDst(),
                               ((NetMessage.Ip)msg).getProtocol(),
                               ((NetMessage.Ip)msg).getPriority(),
                               ((NetMessage.Ip)msg).getTTL(),
                               ((NetMessage.Ip)msg).getId(),
                               ((NetMessage.Ip)msg).getFragOffset());

                        sendToLinkLayer(copyOfMsg, sinkIp);
                        //-----------
                           //System.out.println("Node [" + myNode.getID() + "] forwarding Sensing Message to Sink Node ");
                        //-----------

                       //     terkirim++;
                        return;
                    }

                      NetMessage.Ip copyOfMsg
                        = new NetMessage.Ip(msgAGW,
                               ((NetMessage.Ip)msg).getSrc(),
                               ((NetMessage.Ip)msg).getDst(),
                               ((NetMessage.Ip)msg).getProtocol(),
                               ((NetMessage.Ip)msg).getPriority(),
                               ((NetMessage.Ip)msg).getTTL(),
                               ((NetMessage.Ip)msg).getId(),
                               ((NetMessage.Ip)msg).getFragOffset());

                                 sendToLinkLayer(copyOfMsg, targetHop);

                      //------------
                         //System.out.println("Node [" + myNode.getID() + "] forwarding Sensing Message to neighbor "+targetHop.getIP());
                      //------------

                    MessageDataValue msgQuery = (MessageDataValue) msgAGW.getPayload();
                    ((MessageDataValue)msgAGW.getPayload()).setPathList(myNode.getIP().toString());

    }



     private void handleMessageDataValuelast(NetMessage msg, Location2D targetLocation) {
         double[] listHole = new double[30];
         List<NetAddress> list = new ArrayList<NetAddress>();


         NetAddress targetHop  = myNode.getIP();
         CSGPWrapperMessage msgAGW = (CSGPWrapperMessage)((NetMessage.Ip)msg).getPayload();
         MessageDataValue msgDataValue  = (MessageDataValue) msgAGW.getPayload();



         CSGPWrapperMessage msgBack  = (CSGPWrapperMessage)((NetMessage.Ip)msg).getPayload();

         MessageDataValue msgDataValueBack  = (MessageDataValue) msgBack.getPayload();



        Location2D sinkLocation=targetLocation; //masih perlu dideklarasikan dimana sinklocation tersebut
     //     System.out.println(targetLocation.getX()+" "+targetLocation.getY());

        NetAddress sinkIp= ((NetMessage.Ip)msg).getDst(); // tujuannya adalah sink node
        NetAddress backNode=null;

      //pengecekan apakah belum dilakukan cek tetangga?
      //-----------------------------
        this.availablePathList = this.availablePathList + ((MessageDataValue) msgAGW.getPayload()).getPathList();
        jalurLagi = availablePathList.split(",", 100);
        System.out.println("Node [" + myNode.getIP().toString() + "] have Path List "+availablePathList);


      if (Konstanta.modeRouting ==  1) {   //swing routing
          if ((!this.cekTetangga)){
             //  forwardingNodes= getForwardNode(targetLocation); // ini untuk mengisi forwarding Node

              forwardingNodes=  this.getForwardNodeSwingWW(targetLocation); // ini untuk mengisi forwarding Node


               this.cekTetangga = true;
               if ( forwardingNodes.isEmpty()){
                   System.out.println("Node : " + myNode.getID() + " : hole ");

                   //System.out.println("Node Forward Terakhir: " + jalurLagi[jalurLagi.length - 1]);
                   String tempBackHole = jalurLagi[jalurLagi.length - 1];
                   backNode = this.getBackNode(tempBackHole);
                   targetHop = this.getBackPath(targetLocation);

                   if((this.myNode.getIP() == sinkIp)) { //sink node

                   sendToAppLayer(msgBack.getPayload(), null);

                }
                 if(myNode.neighboursList.contains(sinkIp)){            //jika tetangga terdapat Sink maka langsung kirim ke Sink
                         NetMessage.Ip copyOfMsg
                        = new NetMessage.Ip(msgBack,
                               ((NetMessage.Ip)msg).getSrc(),
                               ((NetMessage.Ip)msg).getDst(),
                               ((NetMessage.Ip)msg).getProtocol(),
                               ((NetMessage.Ip)msg).getPriority(),
                               ((NetMessage.Ip)msg).getTTL(),
                               ((NetMessage.Ip)msg).getId(),
                               ((NetMessage.Ip)msg).getFragOffset());

                        sendToLinkLayer(copyOfMsg, sinkIp);
                        //-----------
                           //System.out.println("Node [" + myNode.getID() + "] forwarding Sensing Message to Sink Node ");
                        //-----------

                       //     terkirim++;
                        return;
                    }

                      NetMessage.Ip copyOfMsg
                        = new NetMessage.Ip(msgBack,
                               ((NetMessage.Ip)msg).getSrc(),
                               ((NetMessage.Ip)msg).getDst(),
                               ((NetMessage.Ip)msg).getProtocol(),
                               ((NetMessage.Ip)msg).getPriority(),
                               ((NetMessage.Ip)msg).getTTL(),
                               ((NetMessage.Ip)msg).getId(),
                               ((NetMessage.Ip)msg).getFragOffset());

                                 sendToLinkLayer(copyOfMsg, targetHop);
                       } else {
                              targetHop = forwardingNodes.get(this.urutan);
                                urutan= urutan+1;
              //jika urutan= panjang list
                                if (urutan == forwardingNodes.size()){         // jika i = 0 maka next hop ada diurutan list yang ke nol
                                 urutan =0;                                // maka urutan jadi 0 kembali
                        }
                      }


                  // nexthop
               //langsung loooping
          } else {
             // kerjakan looping untuk kirim
             //if jika targetHop = 1 maka jalankan 1 saja
              //if targetHop = forwardingNodes

              if ( forwardingNodes.isEmpty()){
                   System.out.println("Node : " + myNode.getID() + " : hole ");

                   //System.out.println("Node Forward Terakhir: " + jalurLagi[jalurLagi.length - 1]);
                   String tempBackHole = jalurLagi[jalurLagi.length - 1];
                   backNode = this.getBackNode(tempBackHole);
                   targetHop = this.getBackPath(targetLocation);

                   if((this.myNode.getIP() == sinkIp)) { //sink node

                   sendToAppLayer(msgBack.getPayload(), null);

                }
                 if(myNode.neighboursList.contains(sinkIp)){            //jika tetangga terdapat Sink maka langsung kirim ke Sink
                         NetMessage.Ip copyOfMsg
                        = new NetMessage.Ip(msgBack,
                               ((NetMessage.Ip)msg).getSrc(),
                               ((NetMessage.Ip)msg).getDst(),
                               ((NetMessage.Ip)msg).getProtocol(),
                               ((NetMessage.Ip)msg).getPriority(),
                               ((NetMessage.Ip)msg).getTTL(),
                               ((NetMessage.Ip)msg).getId(),
                               ((NetMessage.Ip)msg).getFragOffset());

                        sendToLinkLayer(copyOfMsg, sinkIp);
                        //-----------
                           //System.out.println("Node [" + myNode.getID() + "] forwarding Sensing Message to Sink Node ");
                        //-----------

                       //     terkirim++;
                        return;
                    }

                      NetMessage.Ip copyOfMsg
                        = new NetMessage.Ip(msgBack,
                               ((NetMessage.Ip)msg).getSrc(),
                               ((NetMessage.Ip)msg).getDst(),
                               ((NetMessage.Ip)msg).getProtocol(),
                               ((NetMessage.Ip)msg).getPriority(),
                               ((NetMessage.Ip)msg).getTTL(),
                               ((NetMessage.Ip)msg).getId(),
                               ((NetMessage.Ip)msg).getFragOffset());

                                 sendToLinkLayer(copyOfMsg, targetHop);
                       } else{
                            targetHop = forwardingNodes.get(this.urutan); // forwarding node sudah diisi
                            urutan= urutan+1;
                            //jika urutan= panjang list
                            if (urutan == forwardingNodes.size()){
                            urutan =0;
                           }
              }





          }
         } else if (Konstanta.modeRouting == 2) {
             //SGP

             targetHop = this.ShortestGeoPathRouting(targetLocation);
             if (targetHop == myNode.getIP()){
                 System.out.println("Node : " + myNode.getID() + " : hole ");
                 //BlacklistNode.add(this.myNode.getIP());
                 String tempBackHole = jalurLagi[jalurLagi.length - 1];
                 backNode = this.getBackNode(tempBackHole);
                 targetHop = this.getBackPath(targetLocation);

                 if((this.myNode.getIP() == sinkIp)) { //sink node

                   sendToAppLayer(msgBack.getPayload(), null);

                }
                 if(myNode.neighboursList.contains(sinkIp)){            //jika tetangga terdapat Sink maka langsung kirim ke Sink
                         NetMessage.Ip copyOfMsg
                        = new NetMessage.Ip(msgBack,
                               ((NetMessage.Ip)msg).getSrc(),
                               ((NetMessage.Ip)msg).getDst(),
                               ((NetMessage.Ip)msg).getProtocol(),
                               ((NetMessage.Ip)msg).getPriority(),
                               ((NetMessage.Ip)msg).getTTL(),
                               ((NetMessage.Ip)msg).getId(),
                               ((NetMessage.Ip)msg).getFragOffset());

                        sendToLinkLayer(copyOfMsg, sinkIp);
                        //-----------
                           //System.out.println("Node [" + myNode.getID() + "] forwarding Sensing Message to Sink Node ");
                        //-----------

                       //     terkirim++;
                        return;
                    }

                      NetMessage.Ip copyOfMsg
                        = new NetMessage.Ip(msgBack,
                               ((NetMessage.Ip)msg).getSrc(),
                               ((NetMessage.Ip)msg).getDst(),
                               ((NetMessage.Ip)msg).getProtocol(),
                               ((NetMessage.Ip)msg).getPriority(),
                               ((NetMessage.Ip)msg).getTTL(),
                               ((NetMessage.Ip)msg).getId(),
                               ((NetMessage.Ip)msg).getFragOffset());

                                 sendToLinkLayer(copyOfMsg, targetHop);


             }

         }

      //------------------------

        if((this.myNode.getIP() == sinkIp)) { //sink node
             sendToAppLayer(msgAGW.getPayload(), null);

        }
         if(myNode.neighboursList.contains(sinkIp)){            //jika tetangga terdapat Sink maka langsung kirim ke Sink
                 NetMessage.Ip copyOfMsg
              	= new NetMessage.Ip(msgAGW,
		       ((NetMessage.Ip)msg).getSrc(),
                       ((NetMessage.Ip)msg).getDst(),
                       ((NetMessage.Ip)msg).getProtocol(),
                       ((NetMessage.Ip)msg).getPriority(),
                       ((NetMessage.Ip)msg).getTTL(),
                       ((NetMessage.Ip)msg).getId(),
                       ((NetMessage.Ip)msg).getFragOffset());

                sendToLinkLayer(copyOfMsg, sinkIp);
                //-----------
                   //System.out.println("Node [" + myNode.getID() + "] forwarding Sensing Message to Sink Node ");
                //-----------

               //     terkirim++;
                return;
            }

              NetMessage.Ip copyOfMsg
              	= new NetMessage.Ip(msgAGW,
		       ((NetMessage.Ip)msg).getSrc(),
                       ((NetMessage.Ip)msg).getDst(),
                       ((NetMessage.Ip)msg).getProtocol(),
                       ((NetMessage.Ip)msg).getPriority(),
                       ((NetMessage.Ip)msg).getTTL(),
                       ((NetMessage.Ip)msg).getId(),
                       ((NetMessage.Ip)msg).getFragOffset());

                         sendToLinkLayer(copyOfMsg, targetHop);

              //------------
                 //System.out.println("Node [" + myNode.getID() + "] forwarding Sensing Message to neighbor "+targetHop.getIP());
              //------------

            MessageDataValue msgQuery = (MessageDataValue) msgAGW.getPayload();
            ((MessageDataValue)msgAGW.getPayload()).setPathList(myNode.getIP().toString());
     }

     private void handleMessageDataValue2(NetMessage msg, Location2D targetLocation) {

         NetAddress targetHop  = myNode.getIP();
         CSGPWrapperMessage msgAGW = (CSGPWrapperMessage)((NetMessage.Ip)msg).getPayload();
     //     System.out.println(targetLocation.getX()+" "+targetLocation.getY());

        NetAddress sinkIp= ((NetMessage.Ip)msg).getDst(); // tujuannya adalah sink node

      //pengecekan apakah belum dilakukan cek tetangga?
      //-----------------------------

      if (Konstanta.modeRouting ==  1) {   //swing routing
          if ((!this.cekTetangga)){
             //  forwardingNodes= getForwardNode(targetLocation); // ini untuk mengisi forwarding Node

              forwardingNodes=  this.getForwardNodeSwingWW(targetLocation); // ini untuk mengisi forwarding Node


               this.cekTetangga = true;
               if ( forwardingNodes.isEmpty()){
                   System.out.println("Node : " + myNode.getID() + " : hole ");
               }

              targetHop = forwardingNodes.get(this.urutan);
              urutan= urutan+1;
              //jika urutan= panjang list
              if (urutan == forwardingNodes.size()){         // jika i = 0 maka next hop ada diurutan list yang ke nol
                  urutan =0;                                // maka urutan jadi 0 kembali
              }
                  // nexthop
               //langsung loooping
          } else {
               // kerjakan looping untuk kirim
             targetHop = forwardingNodes.get(this.urutan); // forwarding node sudah diisi
              urutan= urutan+1;
              //jika urutan= panjang list
              if (urutan == forwardingNodes.size()){
                  urutan =0;
              }
          }
         } else if (Konstanta.modeRouting == 2) {
             //SGP

             targetHop = this.ShortestGeoPathRouting(targetLocation);
             if (targetHop == myNode.getIP()){
                 System.out.println("Node : " + myNode.getID() + " : hole ");
             }

         }

      //------------------------

        if((this.myNode.getIP() == sinkIp)) { //sink node
             sendToAppLayer(msgAGW.getPayload(), null);

        }
         if(myNode.neighboursList.contains(sinkIp)){            //jika tetangga terdapat Sink maka langsung kirim ke Sink
                 NetMessage.Ip copyOfMsg
              	= new NetMessage.Ip(msgAGW,
		       ((NetMessage.Ip)msg).getSrc(),
                       ((NetMessage.Ip)msg).getDst(),
                       ((NetMessage.Ip)msg).getProtocol(),
                       ((NetMessage.Ip)msg).getPriority(),
                       ((NetMessage.Ip)msg).getTTL(),
                       ((NetMessage.Ip)msg).getId(),
                       ((NetMessage.Ip)msg).getFragOffset());

                sendToLinkLayer(copyOfMsg, sinkIp);
                //-----------
                   //System.out.println("Node [" + myNode.getID() + "] forwarding Sensing Message to Sink Node ");
                //-----------

               //     terkirim++;
                return;
            }

              NetMessage.Ip copyOfMsg
              	= new NetMessage.Ip(msgAGW,
		       ((NetMessage.Ip)msg).getSrc(),
                       ((NetMessage.Ip)msg).getDst(),
                       ((NetMessage.Ip)msg).getProtocol(),
                       ((NetMessage.Ip)msg).getPriority(),
                       ((NetMessage.Ip)msg).getTTL(),
                       ((NetMessage.Ip)msg).getId(),
                       ((NetMessage.Ip)msg).getFragOffset());

                         sendToLinkLayer(copyOfMsg, targetHop);

              //------------
                 //System.out.println("Node [" + myNode.getID() + "] forwarding Sensing Message to neighbor "+targetHop.getIP());
              //------------


     }





     private void handleWithTargetLocation(Location2D targetLocation,NetMessage msg) {

          NetAddress targetHop;

         CSGPWrapperMessage msgSGP
    	 	= (CSGPWrapperMessage)((NetMessage.Ip)msg).getPayload();
     //     System.out.println(targetLocation.getX()+" "+targetLocation.getY());


      NetAddress sinkIp= ((NetMessage.Ip)msg).getDst(); // tujuannya adalah sink node
      List<NetAddress> forwardingNodes = null; // variabel  forwarding node

        targetHop = forwardingNodes.get(this.urutan);

    	 // If there is no node closer to the area of interest than this node,
    	 // then this node will get the message
         if (targetHop.hashCode() == myNode.getIP().hashCode())
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

             sendToLinkLayer(copyOfMsg, targetHop);
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
        //terkirim++;
    }



    public byte sendToLinkLayer(NetMessage.Ip ipMsg, NetAddress nextHopDestIP)
    {
        if (myNode.getEnergyManagement()
        		  .getBattery()
        		  .getPercentageEnergyLevel()< 2)
            return 0;



        if (myNode.getID() == 164)
           // System.out.println("route packet to " + nextHopDestIP);

        if (nextHopDestIP == null)
            System.err.println("NULL nextHopDestIP");
        if (nextHopDestIP == NetAddress.ANY)
            netEntity.send(ipMsg, Constants.NET_INTERFACE_DEFAULT, MacAddress.ANY);
        else
        {
            NodeEntry nodeEntry = myNode.neighboursList.get(nextHopDestIP);
            if (nodeEntry == null)
            {
            //     System.err.println("Node #" + myNode.getID() + ": Destination IP (" + nextHopDestIP + ") not in my neighborhood. Please re-route! Are you sending the packet to yourself?");
                // System.err.println("Node #" + myNode.getID() + "has + " + myNode.neighboursList.size() + " neighbors");
       //          new Exception().printStackTrace();
                 return ERROR;
            }
            MacAddress macAddress = nodeEntry.mac;
            if (macAddress == null)
            {
                 //System.err.println("Node #" + myNode.getID() + ": Destination IP (" + nextHopDestIP + ") not in my neighborhood. Please re-route! Are you sending the packet to yourself?");
                 //System.err.println("Node #" + myNode.getID() + "has + " + myNode.neighboursList.size() + " neighbors");
                 return ERROR;
            }
            myNode.getNodeGUI().colorCode.mark(colorProfileGeneric, CSGPColorProfile.TRANSMIT, 2);
            netEntity.send(ipMsg, Constants.NET_INTERFACE_DEFAULT, macAddress);
           // System.out.println("sent from "+myNode.getIP()+" to "+nextHopDestIP);
        }

        return SUCCESS;
    }

//       private List<NetAddress> getFirstCH(){
//         List<NetAddress> nextHopAddress = null;
//         LinkedList<NodeEntry> neighboursLinkedList =  myNode.neighboursList.getAsLinkedList();
//
//         double jarakToCluster=1000000;
//         for (NodeEntry nodeEntry : neighboursLinkedList){
//             if (nodeEntry.clusterId == myNode.clusterId){
//                 if (nodeEntry.distToCluster < jarakToCluster) {
//                     jarakToCluster = nodeEntry.distToCluster;
//                     nextHopAddress = nodeEntry.ip;
//                 }
//             }
//
//         }
//
//         for (NodeEntry nodeEntry : neighboursLinkedList){
//             if(nodeEntry.ip == nextHopAddress){
//                 nodeEntry.setStatus1();
//                 myNode.getSimManager().getSimGUI()
//		  .getAnimationDrawingTool().animate("CrossHair",nodeEntry.getNCS_Location2D());
//             }
//
//         }
//       //  System.out.println(" Cluster head terpilih dari node " + myNode.getIP().toString() + " : " + nextHopAddress.getIP().toString());
//         return nextHopAddress;
//
//     }
//       private List<NetAddress> getSecondCH(){
//         NetAddress nextHopAddress = null;
//         LinkedList<NodeEntry> neighboursLinkedList =  myNode.neighboursList.getAsLinkedList();
//
//         double jarakToCluster=1000000;
//         for (NodeEntry nodeEntry : neighboursLinkedList){
//             if(nodeEntry.status1 == false){
//             if (nodeEntry.clusterId == myNode.clusterId){
//                 if (nodeEntry.distToCluster < jarakToCluster) {
//                     jarakToCluster = nodeEntry.distToCluster;
//                     nextHopAddress = nodeEntry.ip;
//                 }
//             }
//             }
//
//         }
//        for (NodeEntry nodeEntry : neighboursLinkedList){
//             if(nodeEntry.ip == nextHopAddress){
//                 nodeEntry.setStatus2();
//                 myNode.getSimManager().getSimGUI()
//		  .getAnimationDrawingTool().animate("CrossHair",nodeEntry.getNCS_Location2D());
//             }
//
//         }
//       // System.out.println(" Cluster head terpilih dari node " + myNode.getIP().toString() + " : " + nextHopAddress.getIP().toString());
//         return nextHopAddress;
//       }
//
//       private List<NetAddress> getThirdCH(){
//         NetAddress nextHopAddress = null;
//         LinkedList<NodeEntry> neighboursLinkedList =  myNode.neighboursList.getAsLinkedList();
//
//         double jarakToCluster=1000000;
//         for (NodeEntry nodeEntry : neighboursLinkedList){
//             if(nodeEntry.status2 == false && nodeEntry.status1 == false){
//             if (nodeEntry.clusterId == myNode.clusterId){
//                 if (nodeEntry.distToCluster < jarakToCluster) {
//                     jarakToCluster = nodeEntry.distToCluster;
//                     nextHopAddress = nodeEntry.ip;
//                 }
//             }
//             }
//
//         }
//        for (NodeEntry nodeEntry : neighboursLinkedList){
//             if(nodeEntry.ip == nextHopAddress){
//
//                 myNode.getSimManager().getSimGUI()
//		  .getAnimationDrawingTool().animate("CrossHair",nodeEntry.getNCS_Location2D());
//             }
//
//         }
//       //  System.out.println(" Cluster head terpilih dari node " + myNode.getIP().toString() + " : " + nextHopAddress.getIP().toString());
//         return nextHopAddress;
//       }
//
//       private List<NetAddress> getRandomCH(){
//         List<NetAddress> nextHopAddress = null;
//           LinkedList<NodeEntry> neighboursLinkedList =  myNode.neighboursList.getAsLinkedList();
//            long min = 10;
//            long max = 70;
//            Random s = new Random();
//            long jarak = (long) (min + (max - min) * s.nextDouble());
//            for (NodeEntry nodeEntry : neighboursLinkedList){
//
//                  if (nodeEntry.distToCluster > jarak) {
//                     jarak = (long) nodeEntry.distToCluster;
//                     nextHopAddress = nodeEntry.ip;
//                     break;
//                 }
//                    }
//            for (NodeEntry nodeEntry : neighboursLinkedList){
//             if(nodeEntry.ip == nextHopAddress){
//
//                 myNode.getSimManager().getSimGUI()
//		  .getAnimationDrawingTool().animate("CrossHair",nodeEntry.getNCS_Location2D());
//             }
//
//         }
//       //    System.out.println(" Cluster head terpilih dari node " + myNode.getIP().toString() + " : " + nextHopAddress.getIP().toString());
//           return nextHopAddress;
//    }

     public List<NetAddress> getForwardNodeSwing(Location2D destLocation) {

        double jarakNodekeSink = myNode.getLocation2D().distanceTo(destLocation);
        double sisaEnergi = myNode.getEnergyManagement().getBattery().getPercentageEnergyLevel();
        List<NetAddress> list = new ArrayList<NetAddress>();
        NetAddress closestNode = myNode.getIP();
        LinkedList<NodeEntry> neighboursLinkedList
        	= myNode.neighboursList.getAsLinkedList();
        double bobotNode = 0;
        double bobotSekarang = 0.4 * jarakNodekeSink + 0.6* sisaEnergi;

        double[] listSortingBobot = new double[30];
        NetAddress[] listSortingIp = new NetAddress[30];
        double[] listSortingJarakX = new double[30];
        double[] listSortingJarakY = new double[30];

        int x = 0;

        for(NodeEntry nodeEntry: neighboursLinkedList) {
            // Update Batery Tetangga dengan acuan nilai dari NodeList
//            for (int k = 0; k < NodeList.length; k++) {
//                if(NodeList[k] != null && NodeList[k].getIP() == nodeEntry.ip){
//                    nodeEntry.battery = NodeList[k].getEnergyManagement().getBattery().getPercentageEnergyLevel();
//                }
//            }

            /* Get the location coordinates of the neighbour 'i' */
            double neighbourDistance
            	= nodeEntry.getNCS_Location2D()
            	  		   .fromNCS(myNode.getLocationContext())
            			   .distanceTo(destLocation);
            double neighbourCoordinateX
                = nodeEntry.getNCS_Location2D()
                                    .fromNCS(myNode.getLocationContext())
                                    .getX();

            double neighbourCoordinateY
                = nodeEntry.getNCS_Location2D()
                                    .fromNCS(myNode.getLocationContext())
                                    .getY();

            bobotNode = 0.4 * neighbourDistance + 0.6* sisaEnergi;

                if ( bobotNode < bobotSekarang ) {
                  listSortingBobot[x] = bobotNode;
                  listSortingIp[x] = nodeEntry.ip;
                  listSortingJarakX[x] = neighbourCoordinateX;
                  //System.out.println("jarak x : " + listSortingJarakX[x]);
                  listSortingJarakY[x] = neighbourCoordinateY;
                  x++;
                  //list.add(nodeEntry.ip);
//                List<double> k = new ArrayList<double>();
//                k.
//                LinkedList<NodeEntry> k
//                    = closestNode
                }
                //Collections.sort(list<NetAddress>);
        }

        int minimum_id, i, j;
        int kondisi = 1; // kondisi 1=jarak+x; kondisi 2=jarak+y; kondisi 3=jarak;
        double tempDis;
        double tempJarX;
        double tempJarY;
        NetAddress tempIp;


        for (i = 0; i < x; i++){
            minimum_id = i;
            for(j = i + 1; j < x; j++){
                if((listSortingBobot[j] < listSortingBobot[minimum_id]) && (listSortingJarakX[j] < listSortingJarakX[minimum_id]) && (urutan == 0)  ){
                    minimum_id = j;
                }

                if((listSortingBobot[j] < listSortingBobot[minimum_id]) && (listSortingJarakY[j] < listSortingJarakY[minimum_id]) && (urutan == 1) ){
                    minimum_id = j;
                }

//                if((listSortingBobot[j] < listSortingBobot[minimum_id]) && (urutan == 2) ){
//                    minimum_id = j;
//                }
            }
            tempDis = listSortingBobot[minimum_id];
            listSortingBobot[minimum_id] = listSortingBobot[i];
            listSortingBobot[i] = tempDis;

            tempJarX = listSortingJarakX[minimum_id];
            listSortingJarakX[minimum_id] = listSortingJarakX[i];
            listSortingJarakX[i] = tempJarX;

            tempJarY = listSortingJarakY[minimum_id];
            listSortingJarakY[minimum_id] = listSortingJarakY[i];
            listSortingJarakY[i] = tempJarY;

            tempIp = listSortingIp[minimum_id];
            listSortingIp[minimum_id] = listSortingIp[i];
            listSortingIp[i] = tempIp;
        }

//        for (int t = 0; t <= listSortingDistance.length; t++){
//            System.out.println(listSortingDistance[t]);
//        }
        for (int a = 0; a <= 1; a++){
            list.add(listSortingIp[a]);
        }



        return list;
    }


     /*
     {

          LinkedList<NodeEntry> neighboursLinkedList = myNode.neighboursList.getAsLinkedList();
                for(NodeEntry nodeEntry: neighboursLinkedList) { //Program untuk mencari node tetangga yang lebih dekat
                 if (nodeEntry.ip.getIP().toString().equalsIgnoreCase(tempBackHole)){
                     backNode = nodeEntry.ip;
                 }

                }
     }

     */

      public NetAddress getBackNode(String ip){
          NetAddress ipBack=null;
          LinkedList<NodeEntry> neighboursLinkedList
        	= myNode.neighboursList.getAsLinkedList();
          for(NodeEntry nodeEntry: neighboursLinkedList)
           //Program untuk mencari node tetangga yang lebih dekat
          {
              if (nodeEntry.ip.toString().equalsIgnoreCase(ip)){
                  return nodeEntry.ip;
              }
          }

          return ipBack;
      }

      public NetAddress getBackNode2(Location2D destLocation){
        double closestDist = myNode.getLocation2D().distanceTo(destLocation);
        NetAddress ipBackDest = myNode.getIP();
          LinkedList<NodeEntry> neighboursLinkedList
        	= myNode.neighboursList.getAsLinkedList();
          for(NodeEntry nodeEntry: neighboursLinkedList)
           //Program untuk mencari node tetangga yang lebih dekat
          {
              if (closestDist != 0  ){
                  return nodeEntry.ip;
              }
          }

          return ipBackDest;
      }

      private NetAddress getBackPath(Location2D destLocation) {
        double jarakSekarang = 1000000; //set initial maximum values
        double koordinatXNode = myNode.getLocation2D().getX();
        double koordinatYNode = myNode.getLocation2D().getY();
        NetAddress nodeTerdekat = myNode.getIP();
        LinkedList<NodeEntry> neighboursLinkedList = myNode.neighboursList.getAsLinkedList();

        for (NodeEntry nodeEntry : neighboursLinkedList) {
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


    public List<NetAddress> getForwardNodeSwingWW(Location2D destLocation) {

        //destLocation.getX()
        double koordinatXNode = myNode.getLocation2D().getX();
        double koordinatYNode = myNode.getLocation2D().getY();
        double jarakNodekeSink = myNode.getLocation2D().distanceTo(destLocation);
        //double sisaEnergi = myNode.getEnergyManagement().getBattery().getPercentageEnergyLevel();
        List<NetAddress> list = new ArrayList<NetAddress>();
        NetAddress closestNode = myNode.getIP();
        LinkedList<NodeEntry> neighboursLinkedList
        	= myNode.neighboursList.getAsLinkedList();
        double bobotNode = 0;
        double jarakSekarang = jarakNodekeSink;
        //nodeEntry.getNCS_Location2D()
            	  		   //.fromNCS(myNode.getLocationContext())
            			   //.distanceTo(destLocation);

        double[] listSortingJarak = new double[neighboursLinkedList.size()];
        NetAddress[] listSortingIp = new NetAddress[neighboursLinkedList.size()];
        double[] listSortingJarakX = new double[neighboursLinkedList.size()];
        double[] listSortingJarakY = new double[neighboursLinkedList.size()];
        String[] listSortingId = new String[neighboursLinkedList.size()];

        int x = 0;

        for(NodeEntry nodeEntry: neighboursLinkedList) { //Program untuk mencari node tetangga yang lebih dekat

            /* Get the location coordinates of the neighbour 'i' */
            double neighbourDistance
            	= nodeEntry.getNCS_Location2D()
            	  		   .fromNCS(myNode.getLocationContext())
            			   .distanceTo(destLocation);

            double neighbourCoordinateX
                = nodeEntry.getNCS_Location2D()
                                    .fromNCS(myNode.getLocationContext())
                                    .getX();

            double neighbourCoordinateY
                = nodeEntry.getNCS_Location2D()
                                    .fromNCS(myNode.getLocationContext())
                                    .getY();

            double sisaEnergi = nodeEntry.battery;

            //bobotNode = 0.4 * neighbourDistance + 0.6* sisaEnergi;
            bobotNode = neighbourDistance;

                if ( bobotNode < jarakNodekeSink ) {
                  listSortingJarak[x] = bobotNode;
                  listSortingIp[x] = nodeEntry.ip;
                  listSortingId[x] = nodeEntry.ip.toString();
                  listSortingJarakX[x] = neighbourCoordinateX;
                  //System.out.println("jarak x : " + listSortingJarakX[x]);
                  listSortingJarakY[x] = neighbourCoordinateY;
                  //bobotSekarang = bobotNode;
                  x++;

                }
                //Collections.sort(list<NetAddress>);
        }
        int indexJauhY=0;
        double bufferJauh=0;
        for (int y=0;y<x;y++){
            if ( listSortingJarakY[y]>bufferJauh){
                indexJauhY=y;
                bufferJauh=listSortingJarakY[y];
            }
        }

        int indexJauhX=0;
        double bufferJauhX=0;
        for (int y=0;y<x;y++){
            if ( listSortingJarakX[y]>bufferJauhX){
                indexJauhX=y;
                bufferJauhX=listSortingJarakX[y];
            }
        }

        int indexDekatY=0;
        double bufferDekatY=100000;
        for (int y=0;y<x;y++){
            if ( listSortingJarakY[y] <  bufferDekatY){
                indexDekatY=y;
                bufferDekatY=listSortingJarakY[y];
            }
        }

        int indexDekatX=0;
        double bufferDekatX=100000;
        for (int y=0;y<x;y++){
            if ( listSortingJarakX[y] <  bufferDekatX){
                indexDekatX=y;
                bufferDekatX=listSortingJarakX[y];
            }
        }

        int indexJauhJarak=0;
        double bufferJauhJarak=100000;
        for (int y=0;y<x;y++){
            if ( listSortingJarak[y]<bufferJauhJarak){
                indexJauhJarak=y;
                bufferJauhJarak=listSortingJarak[y];
            }
        }


//   NetAddress ipTerjauh =  listSortingIp[indexJauhY];

        //jika nilai koordinat X lebih besar dari Y maka dipilih swing Y
        if (koordinatYNode <= koordinatXNode ){
            if ( listSortingIp[indexJauhY] != null ){
            list.add(listSortingIp[indexJauhY]);
            }

            if ( listSortingIp[indexDekatY] != null ){
            list.add(listSortingIp[indexDekatY]);
            }

//            if ( listSortingIp[indexJauhJarak] != null ){
//            list.add(listSortingIp[indexJauhJarak]);
//        }


        }
        //jika nilai koordinat Y lebih besar dari X dipilih swing X
        else {
            if ( listSortingIp[indexDekatX] != null ){
            list.add(listSortingIp[indexDekatX]);
            }

            if ( listSortingIp[indexJauhX] != null ){
            list.add(listSortingIp[indexJauhX]);
            }

//            if ( listSortingIp[indexJauhJarak] != null ){
//            list.add(listSortingIp[indexJauhJarak]);
//        }

        }

        return list;
    }

     public List<NetAddress> getForwardNode(Location2D destLocation) {

        double jarakNodekeSink = myNode.getLocation2D().distanceTo(destLocation);
        double sisaEnergi = myNode.getEnergyManagement().getBattery().getPercentageEnergyLevel();
        List<NetAddress> list = new ArrayList<NetAddress>();
        NetAddress closestNode = myNode.getIP();
        LinkedList<NodeEntry> neighboursLinkedList
        	= myNode.neighboursList.getAsLinkedList();
        double bobotNode = 0;
        double bobotSekarang = 0.4 * jarakNodekeSink + 0.6* sisaEnergi;

        double[] listSortingBobot = new double[30];
        NetAddress[] listSortingIp = new NetAddress[30];

        int x = 0;

        for(NodeEntry nodeEntry: neighboursLinkedList) {
            // Update Batery Tetangga dengan acuan nilai dari NodeList
//            for (int k = 0; k < NodeList.length; k++) {
//                if(NodeList[k] != null && NodeList[k].getIP() == nodeEntry.ip){
//                    nodeEntry.battery = NodeList[k].getEnergyManagement().getBattery().getPercentageEnergyLevel();
//                }
//            }

            /* Get the location coordinates of the neighbour 'i' */
            double neighbourDistance
            	= nodeEntry.getNCS_Location2D()
            	  		   .fromNCS(myNode.getLocationContext())
            			   .distanceTo(destLocation);
            bobotNode = 0.4 * neighbourDistance + 0.6* sisaEnergi;

                if ( bobotNode < bobotSekarang ) {
                  listSortingBobot[x] = bobotNode;
                  listSortingIp[x] = nodeEntry.ip;
                  x++;
                  //list.add(nodeEntry.ip);
//                List<double> k = new ArrayList<double>();
//                k.
//                LinkedList<NodeEntry> k
//                    = closestNode
                }
                //Collections.sort(list<NetAddress>);
        }

        int minimum_id, i, j;
        double tempDis;
        NetAddress tempIp;

        for (i = 0; i < x; i++){
            minimum_id = i;
            for(j = i + 1; j < x; j++){
                if(listSortingBobot[j] < listSortingBobot[minimum_id]){
                    minimum_id = j;
                }
            }
            tempDis = listSortingBobot[minimum_id];
            listSortingBobot[minimum_id] = listSortingBobot[i];
            listSortingBobot[i] = tempDis;

            tempIp = listSortingIp[minimum_id];
            listSortingIp[minimum_id] = listSortingIp[i];
            listSortingIp[i] = tempIp;
        }

//        for (int t = 0; t <= listSortingDistance.length; t++){
//            System.out.println(listSortingDistance[t]);
//        }
        for (int a = 0; a <= 2; a++){
            list.add(listSortingIp[a]);
        }



        return list;
    }



     public List<NetAddress> getForwardNodeAsli(Location2D destLocation) {

        double jarakNodekeSink = myNode.getLocation2D().distanceTo(destLocation);
        List<NetAddress> list = new ArrayList<NetAddress>();
        NetAddress closestNode = myNode.getIP();
        LinkedList<NodeEntry> neighboursLinkedList
        	= myNode.neighboursList.getAsLinkedList();
//        double[] listSortingDistance = new double[30];
  //      NetAddress[] listSortingIp = new NetAddress[30];




        int x = 0;

        for(NodeEntry nodeEntry: neighboursLinkedList) {
             /* Get the location coordinates of the neighbour 'i' */
            double neighbourDistance
            	= nodeEntry.getNCS_Location2D()
            	  		   .fromNCS(myNode.getLocationContext())
            			   .distanceTo(destLocation);


                if ( neighbourDistance < jarakNodekeSink ) {
                  //listSortingDistance[x] = neighbourDistance;
                 // listSortingIp[x] = nodeEntry.ip;
                 //  x++;
                  list.add(nodeEntry.ip);
//                List<double> k = new ArrayList<double>();
//                k.
//                LinkedList<NodeEntry> k
//                    = closestNode
                }
                //Collections.sort(list<NetAddress>);
        }


       /*
        int minimum_id, i, j;
        double tempDis;
        NetAddress tempIp;

        for (i = 0; i < x; i++){
            minimum_id = i;
            for(j = i + 1; j < x; j++){
                if(listSortingDistance[j] < listSortingDistance[minimum_id]){
                    minimum_id = j;
                }
            }
            tempDis = listSortingDistance[minimum_id];
            listSortingDistance[minimum_id] = listSortingDistance[i];
            listSortingDistance[i] = tempDis;

            tempIp = listSortingIp[minimum_id];
            listSortingIp[minimum_id] = listSortingIp[i];
            listSortingIp[i] = tempIp;
        }

//        for (int t = 0; t <= listSortingDistance.length; t++){
//            System.out.println(listSortingDistance[t]);
//        }
        for (int a = 0; a <= 2; a++){
            list.add(listSortingIp[a]);
        }

        */

        return list;
    }



     public NetAddress ShortestGeoPathRouting2(Location2D destLocation) {
        double closestDist = myNode.getLocation2D().distanceTo(destLocation);
        double jarakAwal = 10000000;
        NetAddress closestNode = myNode.getIP();
        LinkedList<NodeEntry> neighboursLinkedList
        	= myNode.neighboursList.getAsLinkedList();
        //boolean daftarHole = BlacklistNode.contains(nodeEntry.ip);


        for(NodeEntry nodeEntry: neighboursLinkedList  ) {
             /* Get the location coordinates of the neighbour 'i' */


           //LinkedList<NodeEntry> daftarHole = BlacklistNode.contains(nodeEntry.ip);
           // double boolToDouble (Boolean daftarHole){
            //    return daftarHole.compareTo(false);
            //}


            if (! this.BlacklistNode.contains(nodeEntry.ip)){
                    double neighbourDistance
                        = nodeEntry.getNCS_Location2D()
                                           .fromNCS(myNode.getLocationContext())
                                           .distanceTo(destLocation);
                    if ( neighbourDistance < closestDist) {
                        closestDist = neighbourDistance;
                        if (closestDist < jarakAwal){
                        jarakAwal = closestDist;
                        closestNode = nodeEntry.ip;
                        }

                    }

            }
        }
        return closestNode;
    }

  public NetAddress ShortestGeoPathRouting(Location2D destLocation) {
        double closestDist = myNode.getLocation2D().distanceTo(destLocation);
        NetAddress closestNode = myNode.getIP();
        LinkedList<NodeEntry> neighboursLinkedList
        	= myNode.neighboursList.getAsLinkedList();
        //boolean daftarHole = BlacklistNode.contains(nodeEntry.ip);


        for(NodeEntry nodeEntry: neighboursLinkedList  ) {
             /* Get the location coordinates of the neighbour 'i' */


           //LinkedList<NodeEntry> daftarHole = BlacklistNode.contains(nodeEntry.ip);
           // double boolToDouble (Boolean daftarHole){
            //    return daftarHole.compareTo(false);
            //}


            if (! this.BlacklistNode.contains(nodeEntry.ip)){
                    double neighbourDistance
                        = nodeEntry.getNCS_Location2D()
                                           .fromNCS(myNode.getLocationContext())
                                           .distanceTo(destLocation);
                    if ( neighbourDistance < closestDist) {
                        closestDist = neighbourDistance;
                        closestNode = nodeEntry.ip;
                    }

            }
        }
        return closestNode;
    }


  public NetAddress GetForwardAfterHole(Location2D destLocation) {
        double jarakSekarang = 1000000; //set initial maximum values
        double closestDist = myNode.getLocation2D().distanceTo(destLocation);
        NetAddress closestNode = myNode.getIP();
        LinkedList<NodeEntry> neighboursLinkedList
        	= myNode.neighboursList.getAsLinkedList();
        //boolean daftarHole = BlacklistNode;
        LinkedList daftarHole = this.BlacklistNode;
        //neighboursLinkedList.remove(BlacklistNode);



        for(NodeEntry nodeEntry: neighboursLinkedList  ) {
             /* Get the location coordinates of the neighbour 'i' */

                if (! this.BlacklistNode.contains(nodeEntry.ip)){
               //LinkedList<NodeEntry> daftarHole = BlacklistNode.contains(nodeEntry.ip);
               // double boolToDouble (Boolean daftarHole){
                //    return daftarHole.compareTo(false);
                //}



                double neighbourDistance
                    = nodeEntry.getNCS_Location2D()
                                       .fromNCS(myNode.getLocationContext())
                                       .distanceTo(destLocation);
                if ( neighbourDistance < jarakSekarang) {
                    jarakSekarang = neighbourDistance;
                    closestNode = nodeEntry.ip;
                }
            }
        }

        return closestNode;
    }

  public NetAddress GetForwardAfter2Hole(Location2D destLocation) {
        double koordinatAwal = 0; //set initial maximum values
        double closestDist = myNode.getLocation2D().distanceTo(destLocation);
        NetAddress closestNode = myNode.getIP();
        LinkedList<NodeEntry> neighboursLinkedList
        	= myNode.neighboursList.getAsLinkedList();
        //boolean daftarHole = BlacklistNode;
        LinkedList daftarHole = this.BlacklistNode;
        //neighboursLinkedList.remove(BlacklistNode);
        double koordinatXNode = myNode.getLocation2D().getX();
        double koordinatYNode = myNode.getLocation2D().getY();


        for(NodeEntry nodeEntry: neighboursLinkedList  ) {
             /* Get the location coordinates of the neighbour 'i' */

                if (! this.BlacklistNode.contains(nodeEntry.ip)){
               //LinkedList<NodeEntry> daftarHole = BlacklistNode.contains(nodeEntry.ip);
               // double boolToDouble (Boolean daftarHole){
                //    return daftarHole.compareTo(false);
                //}

                int x =0;
                double neighbourCoordinateX
                = nodeEntry.getNCS_Location2D()
                                    .fromNCS(myNode.getLocationContext())
                                    .getX();

                double neighbourCoordinateY
                    = nodeEntry.getNCS_Location2D()
                                        .fromNCS(myNode.getLocationContext())
                                        .getY();

                double neighbourDistance
                    = nodeEntry.getNCS_Location2D()
                                       .fromNCS(myNode.getLocationContext())
                                       .distanceTo(destLocation);


                if (koordinatXNode > koordinatYNode){
                    if ( koordinatAwal < neighbourCoordinateY) {
                        koordinatAwal = neighbourCoordinateY;
                        closestNode = nodeEntry.ip;
                    }
                }

                if (koordinatYNode > koordinatXNode){
                    if ( koordinatAwal < neighbourCoordinateX) {
                        koordinatAwal = neighbourCoordinateX;
                        closestNode = nodeEntry.ip;
                    }
                }
            }
        }

        return closestNode;
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
            //System.out.println("WARNING: CONGESTION " +    nextHopMac.toString()+ ":" + JistAPI.getTime()/Constants.MINUTE); //informasi node yang mengalami kongesti
//            throw new RuntimeException("Net Queue Full");
            //drop++;
        }
        if (reason == Reason.UNDELIVERABLE || reason == Reason.MAC_BUSY)
      //      System.out.println("WARNING: Cannot relay packet to the destination node " + nextHopMac);

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
