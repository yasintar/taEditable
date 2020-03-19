/*
 * AppSampleP2P.java
 *
 * Created on April 15, 2008, 11:14 AM
 * 
 * @author  Oliviu Ghica
 */
package sidnet.stack.users.csgp.app;

import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;
import sidnet.stack.users.csgp.driver.SwingRoutingDriver;
import java.util.List;
import jist.swans.misc.Message; 
import jist.swans.net.NetInterface; 
import jist.swans.net.NetAddress; 
import jist.swans.mac.MacAddress;
import jist.swans.Constants; 
import jist.runtime.JistAPI; 
import sidnet.stack.users.csgp.colorprofile.CSGPColorProfile;
import sidnet.core.gui.TopologyGUI;
import sidnet.core.interfaces.AppInterface;
import sidnet.core.interfaces.CallbackInterface;
import sidnet.core.interfaces.ColorProfile;
import sidnet.core.misc.Location2D;
import sidnet.stack.std.routing.heartbeat.MessageHeartbeat;
import sidnet.stack.users.csgp.routing.CSGPWrapperMessage;
import sidnet.stack.std.routing.shortestgeopath.SGPWrapperMessage;
import sidnet.core.misc.Node;
import sidnet.core.query.Query;
import sidnet.utilityviews.statscollector.StatsCollector;
import sidnet.core.simcontrol.SimManager;
import sidnet.stack.users.csgp.driver.SequenceGenerator;
import sidnet.utilityviews.statscollector.StatEntry_EnergyLeftPercentage;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import sidnet.core.gui.TransmitReceiveFX;
import sidnet.stack.users.csgp.app.WriteCSV;
import sidnet.stack.users.csgp.routing.SwingRouting;
import sidnet.stack.users.csgp.driver.SequenceGenerator;

public class ChApp implements AppInterface, CallbackInterface {
//   xzcxz
   List exp = new ArrayList();
   //public int[] msgSpace = new int[10];
 //String energyFile = "D:\\Dropbox\\Third_Priority KULIAH\\thesis\\SIDnet-SWANS-v1.5.6\\SIDnet-SWANS\\SIDnet-SWANS\\src\\users\\java\\sidnet\\stack\\users\\aggregate_cluster\\experiment\\csgp.csv";
     public static String statistik;
    private final Node myNode; // The SIDnet handle to the node representation 

    private boolean pertama=true;
    
    public static TopologyGUI topologyGUI = null;
    //public static int sense;
    
    /** network entity. */ 
    private NetInterface netEntity;
    
    /** self-referencing proxy entity. */
    private Object self;
    
    /** flag to mark if a heartbeat protocol has been initialized */
    private boolean heartbeatInitiated = false;
    
    private static boolean flag = false;
    
    private boolean signaledUserRequest = false;
    
    private final short routingProtocolIndex;
    
    public static StatsCollector stats = null;
    
    private boolean startedSensing = false;
    private int sequenceCount=0, sequenceSensing=0;
    SequenceGenerator sequenceNumberGlobal;
    
    // do not make this static
    private CSGPColorProfile colorProfileGeneric = new CSGPColorProfile();
     double p = 1;
           double q = SwingRoutingDriver.jmlCluster-1;
           Random rNode = new Random();
           double randomNode1 = p + (q - p) * rNode.nextDouble();
           double randomNode2 = p + (q - p) * rNode.nextDouble();
           double randomNode3 = p + (q - p) * rNode.nextDouble();
           double randomNode4 = p + (q - p) * rNode.nextDouble();
           double randomNode5 = p + (q - p) * rNode.nextDouble();
    /** Creates a new instance of the AppP2P */
    
    public ChApp(Node myNode, short routingProtocolIndex, StatsCollector stats,SequenceGenerator SequenceNumberGlobal)
    {
       
        this.self = JistAPI.proxyMany(this, new Class[] { AppInterface.class });
        this.myNode = myNode;
        
        // To allow the upper layer (user's terminal) 
        //to signal any updates to this node */
        this.myNode.setAppCallback(this);
  
        this.routingProtocolIndex = routingProtocolIndex;

        this.stats = stats;
       this.sequenceNumberGlobal = SequenceNumberGlobal;
       
       
    }
    
    
    
    /* 
     * This is your main execution loop at the Application Level. Here you design the application functionality. It is simulation-time driven
     * The first call to this function is made automatically upon starting the simulation, from the Driver
     */
    public void run(String[] args) 
    {   
        //try{
             
       //   } catch(Exception e){
          //   e.printStackTrace();
          //}   
    
         /* At time 0, set the simulation speed to x1000 to get over the heartbeat node identification phase fast */
          if (JistAPI.getTime() == 0)  // this is how to get the simulation time, by the way
               myNode.getSimControl().setSpeed(SimManager.X1000);
     
          //if (myNode.getID() != 2) return;  // ???
          /* This is a one-time phase. We'll allow a one-hour warm-up in which each node identifies its neighbors (The Heartbeat Protocol) */
          if (JistAPI.getTime() > 0 && !heartbeatInitiated)
          {
             //   System.out.println("["+(myNode.getID() * 5 * Constants.MINUTE) +"] Node " + myNode.getID() + " broadcasts a heartbeat message");
               
                myNode.getNodeGUI().colorCode.mark(colorProfileGeneric, CSGPColorProfile.TRANSMIT, 500);
               
                /* To avoid all nodes to transmit in the same time */
                JistAPI.sleepBlock(myNode.getID() * 5 * Constants.SECOND); 
                
                MessageHeartbeat msg = new MessageHeartbeat();
                msg.setNCS_Location(myNode.getNCS_Location2D());

                //ww tambahi clusterID
                msg.setClusterID(myNode.clusterId);
                msg.setDistToCluster(myNode.distToClusterCenter);
                               
                myNode.getNodeGUI().colorCode.mark(colorProfileGeneric, CSGPColorProfile.TRANSMIT, 500);
                
                /* Send the heartbeat message. The heartbeat protocol will handle these messages and continue according to the protocol*/
                netEntity.send(msg, NetAddress.ANY, Constants.NET_PROTOCOL_HEARTBEAT, Constants.NET_PRIORITY_NORMAL, (byte)100);  // TTL 100
                
                heartbeatInitiated = true;
          }
         
         /* Wait 1 hour for the heartbeat-bootstrap to finish, then slow down to allow users to interact in real-time*/
         if (JistAPI.getTime()/Constants.HOUR >= 1 && !flag) {
              myNode.getSimControl().setSpeed(SimManager.X1);
              flag = true;
         }
         
 //        if (JistAPI.getTime()/Constants.MINUTE>120){
              
    //      }
          
          if (JistAPI.getTime()/Constants.MINUTE < 60) {
              /*if (myNode.getID() == 0)
              {
                  topologyGUI.addLink(new NCS_Location2D(0,0), new NCS_Location2D(1,1), 0 , Color.blue);
                  topologyGUI.addLink(3, 5, 0, Color.green);
              }*/
              
               JistAPI.sleep(5000*Constants.MILLI_SECOND);  // 5000 milliseconds
              
              /* this is to schedule the next run(args) */
              ((AppInterface)self).run(null);  /* !!! Pay attention to the way we re-run the app-layer code. We don't use a while loop, but rather let JiST call this again and again */
         
              return;
          }
          
         
      }
    
    
    public void run() {
        //Location currentLoc = field.getRadioData(new Integer(nodenum)).getLocation();
        JistAPI.sleep(2 + (long)((1000-2)*Constants.random.nextFloat())); 
        
        run(null);
    }
    
    
    
    
    /* Sensing the phenomena is most likely a periodic process. We wrote a procedure to do so.
     * Since the sensing() takes place at various simulation-time, this function should be called through a proxy reference, rather than directly to avoid
     * an infinite starvation loop */
      public void sensing(List params){
        stats.updateCommonStats();
         statistik = JistAPI.getTime()/Constants.MINUTE+","
                     +stats.get(1).getValueAsString()+","
                     +stats.get(2).getValueAsString()+","
                     +stats.get(3).getValueAsString()+","
                     +stats.get(4).getValueAsString()+","
                     +stats.get(5).getValueAsString()+","
                     +stats.get(6).getValueAsString();
                   //  +sense+","
                    // +ChRouting.terkirim+"\n";
             /*exp.add(statistik);
              WriteCSV tulis = new WriteCSV();
             if(JistAPI.getTime()/Constants.HOUR>30){
             
              tulis.writeCsv(energyFile, String.valueOf(exp));
             }*/
         if(myNode.getID()==1){
                  System.out.println(statistik);
            
         }
           long samplingInterval  = (Long)params.get(0);
           long endTime           = (Long)params.get(1);
           int  queryId           = (Integer)params.get(2);
           //long sequenceNumber    = (Long)params.get(3);
           long sequenceNumber    = sequenceNumberGlobal.getandincrement();
      //     String seqNumber = Integer.toString(this.myNode.getID())+":"+ Integer.toString(this.sequenceCount);
           NetAddress sinkAddress = (NetAddress)params.get(4);
           Location2D sinkLocation= (Location2D)params.get(5);
                     
           JistAPI.sleepBlock(samplingInterval);
        
           //default sensed value
         // 
          // double sensedValue = myNode.readAnalogSensorData(0);
           double sensedValue;
           double rangeMinN = 18;
           double rangeMaxN = 39;
           Random rn = new Random();
            double normalTemp = rangeMinN + (rangeMaxN - rangeMinN) * rn.nextDouble();
           sensedValue = normalTemp;
        //   myNode.getNodeGUI().colorCode.mark(colorProfileGeneric, ColorProfileAggregate.SENSE, 5);
           
           
           if (this.isSensing(myNode.getID())) { //penentuan node yang melakukan sensing

               if (pertama){
                   System.out.println("[node]" + myNode.getID() + " start sensing ");
                   pertama=false;
                     myNode.getNodeGUI().colorCode.mark(colorProfileGeneric,colorProfileGeneric.SENSE,ColorProfile.FOREVER);
               }

                MessageDataValue msgDataValue = new MessageDataValue(sensedValue,queryId,sequenceNumber,myNode.getID());
                msgDataValue.setFire();
                CSGPWrapperMessage msgAgwSensing = new CSGPWrapperMessage(msgDataValue, sinkLocation,0, JistAPI.getTime());
                msgAgwSensing.messageID = "A:"+this.myNode.getIP().toString()+":"+this.sequenceSensing++;
                long sleepMin = 0;
                long sleepMax = myNode.clusterNeighbourList.size()/2;
                Random s = new Random();
                long sleeptime = (long) (sleepMin + (sleepMax - sleepMin) * s.nextDouble());
               
                JistAPI.sleepBlock((long) (sleeptime*0.1*Constants.SECOND));
     
                netEntity.send(msgAgwSensing,
        		   		  sinkAddress,
        		   		  routingProtocolIndex,
        		   		  Constants.NET_PRIORITY_NORMAL, (byte)60);
                stats.markPacketSent("data_sensing", sequenceNumber);
           
           }
                
            if (JistAPI.getTime() < endTime){
           //     sequenceNumber++;
                
                params.set(0, samplingInterval);
                params.set(1, endTime);
                params.set(2, queryId);
             //   params.set(3, sequenceNumber);
                params.set(3, sequenceNumberGlobal);

                params.set(4, sinkAddress);
                params.set(5, sinkLocation);
                
                // this is to schedule the next run(args). thi
                //DO NOT use WHILE loops to do this, 
                // nor call the function directly. Let JiST handle it 
                ((AppInterface)self).sensing(params);
                 
             
             //mengumpulkan statistik
            // ChRouting r = new ChRouting();
                       
        }
          
    }
      
      
     
         
      
      
    /** Callback registered with the terminal,
     * The terminal will call this function whenever the user posts a new query or just closes the terminal window
     * <p>
     * You should inspect the myNode.localTerminalDataSet.getQueryList() to check for new posted queries that your node must act upon
     * Have a look at the TerminalDataSet.java for the available data that is exchanged between this node and the terminal
     */
    public void signalUserRequest()
    {
        /* We'll assume that the node through which the user has posted a query becomes a sink node */
        if (myNode.getQueryList().size() > 0 )
        {     
            Query query = ((LinkedList<Query>)myNode.getQueryList()).getLast();
            //myNode.getNodeGUI().colorCode.mark(colorProfileGeneric, ColorProfileAggregate.SINK, ColorProfile.FOREVER); // to make easier to you to see the node you've posted the query through (the sink node)
          
            if (!query.isDispatched()) {        
             //   myNode.getNodeGUI().colorCode.mark(colorProfileGeneric, ColorProfileAggregate.SINK, ColorProfileAggregate.FOREVER);
                
                int[] rootIDArray = new int[1];
                rootIDArray[0] = myNode.getID();
                
                MessageQuery msgQuery = new MessageQuery(query);

                  msgQuery.setTravelListSink(Integer.toString(myNode.getID()));
               // wrap the MessageQuery as a SGGW message  --> WW Tambahi messageeId (ip +sequence count)
               String messageId= "Q:"+this.myNode.getIP().toString()+":"+this.sequenceCount++;
                CSGPWrapperMessage msgCH
                	= new CSGPWrapperMessage(msgQuery, query.getRegion(),
                							0, JistAPI.getTime());

                msgCH.messageID=messageId;
                netEntity.send(msgCH,
                		       null/*unknown Dest IP, only its approx location*/,
                		       routingProtocolIndex /* (see Driver) */,
                		       Constants.NET_PRIORITY_NORMAL, (byte)100);                  
                            
                query.dispatched(true);
            }
        }
    }
    

    public boolean isSensing( int nodeId){
        boolean status =false;

        for (int i=0;i < Konstanta.sourceNodes.length;i++){
            if (Konstanta.sourceNodes[i]== nodeId){
                
                return true;
                //kalo shortestpath dijalankan semua node
            }
        }

        return status;

    }


    /**
     * Message has been received. 
     * This node must be the either the sink or the source nodes 
     */
    public void receive(Message msg, NetAddress src, MacAddress lastHop, byte macId, NetAddress dst, byte priority, byte ttl) 
    {   
        if (myNode.getEnergyManagement().getBattery().getPercentageEnergyLevel() < 5)
            return;
        
        if (msg instanceof MessageQuery) { /* This is a source node. It receives the query request, and not it prepares to do the periodic sensing/sampling */
             MessageQuery msgQuery = (MessageQuery)msg;
         
             if (msgQuery.getQuery() != null) { /* a query init message */
                if (!startedSensing) { /* To avoid creating duplicated sensing tasks due to duplicated requests, which may happen */
                   // myNode.getNodeGUI().colorCode.mark(colorProfileGeneric, ColorProfileAggregate.SOURCE, ColorProfileAggregate.FOREVER);
                    
                    startedSensing = true;
                    
                    LinkedList params = new LinkedList();
                    params.add(msgQuery.getQuery().getSamplingInterval());   /* sampling interval */
                    params.add(JistAPI.getTime()/Constants.MILLI_SECOND + msgQuery.getQuery().getEndTime()); /* endTime */
                    params.add(msgQuery.getQuery().getID());
                    params.add(sequenceNumberGlobal);
               //     params.add(this.sequenceCount++);
                    params.add(msgQuery.getQuery().getSinkIP());
                    params.add(msgQuery.getQuery()
                    		           .getSinkNCSLocation2D()
                    		           .fromNCS(myNode.getLocationContext()));
                    
                    JistAPI.sleepBlock(msgQuery.getQuery().getSamplingInterval());
                   // int y=msgQuery.hitung();
                   // myNode.setJumlahHop(y);
                    
                    //String y=msgQuery.setTravelList(Integer.toString(myNode.getID()));
                    //System.out.println(y);
                    sensing(params);
                    
                }
             }
        }
        
        // it is a data message, 
        // which means this node is the sink (consumer node)
        if (msg instanceof MessageDataValue) {  
        	 MessageDataValue msgData = (MessageDataValue)msg;
           if(msgData.isFire()==true){
             //untuk menandai jenis node apakah ini(?)
             
             stats.markPacketReceived("data_sensing", msgData.sequenceNumber); //harusnya selain ini ada lagi
             myNode.getNodeGUI().setUserDefinedData1((int)msgData.dataValue);
             myNode.getNodeGUI().setUserDefinedData2((int)msgData.sequenceNumber);

             // Connecting a terminal to this node, at run time,
             // allows the user to visualize the result of the posted query 
             myNode.getNodeGUI()
                   .getTerminal()
                   .appendConsoleText(myNode.getNodeGUI().localTerminalDataSet,
            		                  "data_Sensing WARNING AT Sample node " + msgData.producerNodeId + " #" +
            		                  msgData.sequenceNumber +
            		                  " | val: " + msgData.dataValue);     
             }

        }      
               
        
    }

    /*
    private void handleWithTargetLocation(Location2D targetLocation,
    		 								NetMessage msg) {
    	 SGPWrapperMessage msgSGP
    	 	= (SGPWrapperMessage)((NetMessage.Ip)msg).getPayload();

    	 // Retrieve the IP address of the 1-hop neighbor
    	 // closest to the area of interest
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
 */
    
    
    /* **************************************** *
     * SWANS network's stack hook-up interfaces *
     * **************************************** */
    
    /**
     * Set network entity.
     *
     * @param netEntity network entity
     */
     public void setNetEntity(NetInterface netEntity) {
       this.netEntity = netEntity;
     } 
    
     /**
      * Return self-referencing APPLICATION proxy entity.
      *
      * @return self-referencing APPLICATION proxy entity
      */
     public AppInterface getAppProxy() {
        return (AppInterface)self;
     } 
     
}

