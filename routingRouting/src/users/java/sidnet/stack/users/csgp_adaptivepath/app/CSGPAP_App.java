/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 * Created on April 15, 2008, 11:14 AM
 * 
 * @author  Oliviu Ghica
 */
package sidnet.stack.users.csgp_adaptivepath.app;

import sidnet.stack.users.csgp_adaptivepath.CSGPAPColorProfile.CSGPAPColorProfile;
import java.util.LinkedList;
import java.util.ArrayList;
import sidnet.stack.users.csgp_adaptivepath.driver.CSGPAP_Driver;
import java.util.List;
import jist.swans.misc.Message; 
import jist.swans.net.NetInterface; 
import jist.swans.net.NetAddress; 
import jist.swans.mac.MacAddress;
import jist.swans.Constants; 
import jist.runtime.JistAPI; 
//import sidnet.stack.users.csgp.colorprofile.CSGPColorProfile;
import sidnet.core.gui.TopologyGUI;
import sidnet.core.interfaces.AppInterface;
import sidnet.core.interfaces.CallbackInterface;
import sidnet.core.interfaces.ColorProfile;
import sidnet.core.misc.Location2D;
import sidnet.stack.std.routing.heartbeat.MessageHeartbeat;
import sidnet.stack.users.csgp_adaptivepath.routing.CSGPWrapperMessage;
import sidnet.core.misc.Node;
import sidnet.core.query.Query;
import sidnet.utilityviews.statscollector.StatsCollector;
import sidnet.core.simcontrol.SimManager;
import sidnet.stack.users.csgp_adaptivepath.driver.SequenceGenerator;
import java.util.Random;
import sidnet.core.misc.NodeEntry;


public class CSGPAP_App implements AppInterface, CallbackInterface {
    private final Node myNode; // The SIDnet handle to the node representation
    
    public static TopologyGUI topologyGUI = null;
    
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
    
    List exp = new ArrayList();
    
    public static String statistik;
    
    private int sequenceCount=0, sequenceSensing=0;
    
    SequenceGenerator sequenceNumberGlobal;
    
    
    // do not make this static
    private CSGPAPColorProfile colorProfileGeneric = new CSGPAPColorProfile();
        double p = 1;
           double q = CSGPAP_Driver.jmlCluster-1;
           Random rNode = new Random();
           double randomNode1 = p + (q - p) * rNode.nextDouble();
           double randomNode2 = p + (q - p) * rNode.nextDouble();
           double randomNode3 = p + (q - p) * rNode.nextDouble();
           double randomNode4 = p + (q - p) * rNode.nextDouble();
           double randomNode5 = p + (q - p) * rNode.nextDouble();
           
    public Object getAppProxy;
    
    
    /** Creates a new instance of the AppP2P */
    public CSGPAP_App(Node myNode,
                                    short routingProtocolIndex,
                                    StatsCollector stats,
                                    SequenceGenerator SequenceNumberGlobal)
    {
        this.self = JistAPI.proxyMany(this, new Class[] { AppInterface.class });
        this.myNode=myNode;
        
        // To allow the upper layer (user's terminal) 
        //to signal any updates to this node */
        this.myNode.setAppCallback(this);
  
        this.routingProtocolIndex = routingProtocolIndex;

        this.stats = stats;
        
        this.sequenceNumberGlobal = SequenceNumberGlobal; 
    }
/* 
     * This is your main execution loop at the Application Level.
     * Here you design the application functionality. It is simulation-time driven
     * The first call to this function is made automatically upon starting the simulation, from the Driver
     */
    public void run(String[] args){

            if(myNode.clusterId==1){
                myNode.getNodeGUI().colorCode.mark(colorProfileGeneric, CSGPAPColorProfile.SATU, ColorProfile.FOREVER);
            }else if(myNode.clusterId==2){
                myNode.getNodeGUI().colorCode.mark(colorProfileGeneric, CSGPAPColorProfile.DUA, ColorProfile.FOREVER);
            }else if(myNode.clusterId==3){
                myNode.getNodeGUI().colorCode.mark(colorProfileGeneric, CSGPAPColorProfile.TIGA, ColorProfile.FOREVER);
            }else if(myNode.clusterId==4){
                myNode.getNodeGUI().colorCode.mark(colorProfileGeneric, CSGPAPColorProfile.EMPAT, ColorProfile.FOREVER);
            }else if(myNode.clusterId==5){
                myNode.getNodeGUI().colorCode.mark(colorProfileGeneric, CSGPAPColorProfile.LIMA, ColorProfile.FOREVER);
            }else if(myNode.clusterId==6){
                myNode.getNodeGUI().colorCode.mark(colorProfileGeneric, CSGPAPColorProfile.ENAM, ColorProfile.FOREVER);
            }else if(myNode.clusterId==7){
                myNode.getNodeGUI().colorCode.mark(colorProfileGeneric, CSGPAPColorProfile.TUJUH, ColorProfile.FOREVER);
            }else if(myNode.clusterId==8){
                myNode.getNodeGUI().colorCode.mark(colorProfileGeneric, CSGPAPColorProfile.DELAPAN, ColorProfile.FOREVER);    
            }else if(myNode.clusterId==9){
                myNode.getNodeGUI().colorCode.mark(colorProfileGeneric, CSGPAPColorProfile.SEMBILAN, ColorProfile.FOREVER);
            }else if(myNode.clusterId==10){
                myNode.getNodeGUI().colorCode.mark(colorProfileGeneric, CSGPAPColorProfile.SEPULUH, ColorProfile.FOREVER);    
            }else if(myNode.clusterId%10==1){
                myNode.getNodeGUI().colorCode.mark(colorProfileGeneric, CSGPAPColorProfile.SATU, ColorProfile.FOREVER);
            }else if(myNode.clusterId%10==2){
                myNode.getNodeGUI().colorCode.mark(colorProfileGeneric, CSGPAPColorProfile.DUA, ColorProfile.FOREVER);
            }else if(myNode.clusterId%10==3){
                myNode.getNodeGUI().colorCode.mark(colorProfileGeneric, CSGPAPColorProfile.TIGA, ColorProfile.FOREVER);
            }else if(myNode.clusterId%10==4){
                myNode.getNodeGUI().colorCode.mark(colorProfileGeneric, CSGPAPColorProfile.EMPAT, ColorProfile.FOREVER);
            }else if(myNode.clusterId%10==5){
                myNode.getNodeGUI().colorCode.mark(colorProfileGeneric, CSGPAPColorProfile.LIMA, ColorProfile.FOREVER);
            }else if(myNode.clusterId%10==6){
                myNode.getNodeGUI().colorCode.mark(colorProfileGeneric, CSGPAPColorProfile.ENAM, ColorProfile.FOREVER);
            }else if(myNode.clusterId%10==7){
                myNode.getNodeGUI().colorCode.mark(colorProfileGeneric, CSGPAPColorProfile.TUJUH, ColorProfile.FOREVER);
            }else if(myNode.clusterId%10==8){
                myNode.getNodeGUI().colorCode.mark(colorProfileGeneric, CSGPAPColorProfile.DELAPAN, ColorProfile.FOREVER);    
            }else if(myNode.clusterId%10==9){
                myNode.getNodeGUI().colorCode.mark(colorProfileGeneric, CSGPAPColorProfile.SEMBILAN, ColorProfile.FOREVER);
            }else if(myNode.clusterId%10==10){
                myNode.getNodeGUI().colorCode.mark(colorProfileGeneric, CSGPAPColorProfile.SEPULUH, ColorProfile.FOREVER);  
            }
      

    /* At time 0, set the simulation speed to x1000 to get over the heartbeat node identification phase fast */
        if (JistAPI.getTime() == 0)  // this is how to get the simulation time, by the way
            myNode.getSimControl().setSpeed(SimManager.X1000);

    /* This is a one-time phase. We'll allow a one-hour warm-up in which each node identifies its neighbors (The Heartbeat Protocol) */
        if (JistAPI.getTime() > 0 && !heartbeatInitiated){
            myNode.getNodeGUI().colorCode.mark(colorProfileGeneric, CSGPAPColorProfile.TRANSMIT, 500);

    /* To avoid all nodes to transmit in the same time */
            JistAPI.sleepBlock(myNode.getID() * 5 * Constants.SECOND); 


            MessageHeartbeat msg = new MessageHeartbeat();
            msg.setNCS_Location(myNode.getNCS_Location2D());
            msg.setClusterID(myNode.clusterId); //ww tambahi clusterID
            msg.setDistToCluster(myNode.distToClusterCenter); //ww tambahin informasi jarak tiap node tetangga e cluster center
            msg.setBattery(myNode.getEnergyManagement().getBattery().getPercentageEnergyLevel()); //zhy, menambahkan informasi persentase energi
            msg.setJumlahTetangga(myNode.neighboursList.size()); //zhy, menambahkan informasi jumlah tetangga

            myNode.getNodeGUI().colorCode.mark(colorProfileGeneric, CSGPAPColorProfile.TRANSMIT, 500);

            /* Send the heartbeat message. The heartbeat protocol will handle these messages and continue according to the protocol*/
            netEntity.send(msg, NetAddress.ANY, Constants.NET_PROTOCOL_HEARTBEAT, Constants.NET_PRIORITY_NORMAL, (byte)100);  // TTL 100
            heartbeatInitiated = true;
        }


    /* Wait 1 hour for the heartbeat-bootstrap to finish, then slow down to allow users to interact in real-time*/
        if (JistAPI.getTime()/Constants.HOUR>=1 && !flag) {
            myNode.getSimControl().setSpeed(SimManager.X1);
            flag = true;
        }

        if (JistAPI.getTime()/Constants.MINUTE < 60) {
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
    public void sensing(List params) {
        stats.updateCommonStats();
        statistik = JistAPI.getTime()/Constants.MINUTE+" ,"
                     +stats.get(1).getValueAsString()+" ,"
                     +stats.get(2).getValueAsString()+" ,"
                     +stats.get(3).getValueAsString()+" ,"
                     +stats.get(4).getValueAsString()+" ,"
                     +stats.get(5).getValueAsString()+" ,"
                     +stats.get(6).getValueAsString()+" ,"
                     +stats.get(7).getValueAsString()+" ,"
                     +stats.get(8).getValueAsString()+" ,"
                     +stats.get(9).getValueAsString()+" ,"
                     +stats.get(10).getValueAsString()+" ,"
                     +stats.get(11).getValueAsString()+" ,"
                     +stats.get(12).getValueAsString()+" ,"
                     +stats.get(13).getValueAsString()+" ,"
                     +stats.get(14).getValueAsString();
              
        
    if(myNode.getID()==1){
        System.out.println(statistik);
    }

//     if (myNode.clusterId > 11) {
//
//         System.out.println(myNode.getID()+":"+myNode.clusterId);
//     }
    
        long samplingInterval  = (Long)params.get(0);
        long endTime           = (Long)params.get(1);
        int  queryId           = (Integer)params.get(2);
        long sequenceNumber    = sequenceNumberGlobal.getandincrement();
        NetAddress sinkAddress = (NetAddress)params.get(4);
        Location2D sinkLocation= (Location2D)params.get(5);
                     
    JistAPI.sleepBlock(samplingInterval);

        //awalnya semua create normal
        double sensedValue;
        double rangeMinN = 18;
        double rangeMaxN = 32;
        Random rn = new Random();
        double normalTemp = rangeMinN + (rangeMaxN - rangeMinN) * rn.nextDouble();
        sensedValue = normalTemp;
        
       //pada waktu tertentu
           //naik /abnormal
           if(JistAPI.getTime()/Constants.MINUTE>100 && JistAPI.getTime()/Constants.MINUTE<=150){
                double rangeMin3 = 33;
                double rangeMax3 = 43;
                Random r3 = new Random();
                double abnormalTemp = rangeMin3 + (rangeMax3 - rangeMin3) * r3.nextDouble();
                sensedValue = abnormalTemp;
                }

           //kebakaran /fire
           if(JistAPI.getTime()/Constants.MINUTE>150 && JistAPI.getTime()/Constants.MINUTE<=200){
                double rangeMin2 = 44;
                double rangeMax2 = 110;
                Random r2 = new Random();
                double fireTemp = rangeMin2 + (rangeMax2 - rangeMin2) * r2.nextDouble();
                sensedValue = fireTemp;
                }
                
           //turun /abnormal
           if(JistAPI.getTime()/Constants.MINUTE> 200 && JistAPI.getTime()/Constants.MINUTE < 240){
                double rangeMin = 33;
                double rangeMax = 43;
                Random r = new Random();
                double abnormalTemp = rangeMin + (rangeMax - rangeMin) * r.nextDouble();
                sensedValue = abnormalTemp;
                }
           
//        double avgValue = 0;
//        double temp =0;

        if(sensedValue>=44 && sensedValue<=110){
            myNode.msgSpace.add(sensedValue);

            if(myNode.msgSpace.size()>0){
//                for(int i=0; i<myNode.msgSpace.size(); i++){
//                    temp = temp + myNode.msgSpace.get(i);
//                }
//                avgValue=temp/myNode.msgSpace.size();

            MessageDataValue msgDataValue = new MessageDataValue(sensedValue,queryId,sequenceNumber,myNode.getID());
            msgDataValue.setFire();
            CSGPWrapperMessage msgAgwSensing
                                = new CSGPWrapperMessage(msgDataValue, sinkLocation,0, JistAPI.getTime());
            msgAgwSensing.messageID = "F:"+this.myNode.getIP().toString()+":"+this.sequenceSensing++;
            long sleepMin = 0;
            long sleepMax = myNode.clusterNeighbourList.size()/2;
            Random s = new Random();
            long sleeptime = (long) (sleepMin + (sleepMax - sleepMin) * s.nextDouble());


            JistAPI.sleepBlock((long) (sleeptime*0.1*Constants.SECOND));

            netEntity.send(msgAgwSensing,
                                      sinkAddress,
                                      routingProtocolIndex,
                                      Constants.NET_PRIORITY_NORMAL, (byte)40);             
            myNode.msgSpace.clear();

            }
        }

        if(sensedValue>=33 && sensedValue<=43){ 
            myNode.msgSpace2.add(sensedValue);
//
            if(myNode.msgSpace2.size()>0){
//               for(int i=0; i<myNode.msgSpace2.size(); i++){
//                   temp = temp + myNode.msgSpace2.get(i);
//               }
//                avgValue=temp/myNode.msgSpace2.size();

                MessageDataValue msgDataValue = new MessageDataValue(sensedValue,queryId,sequenceNumber,myNode.getID());
                msgDataValue.setAbnormal();
                CSGPWrapperMessage msgAgwSensing
                = new CSGPWrapperMessage(msgDataValue, sinkLocation,0, JistAPI.getTime());
                msgAgwSensing.messageID = "A:"+this.myNode.getIP().toString()+":"+this.sequenceSensing++;
                long sleepMin = 0;
                long sleepMax = myNode.clusterNeighbourList.size()/2;
                Random s = new Random();
                long sleeptime = (long) (sleepMin + (sleepMax - sleepMin) * s.nextDouble());


                JistAPI.sleepBlock((long) (sleeptime*0.3*Constants.SECOND));

                netEntity.send(msgAgwSensing,
                                          sinkAddress,
                                          routingProtocolIndex,
                                          Constants.NET_PRIORITY_NORMAL, (byte)40); 
//                 stats.markPacketSent("Second_Priority", sequenceNumber);
//                 myNode.getNodeGUI().colorCode.mark(colorProfileGeneric,ColorProfileAggregate.TRANSMIT5, 5);

                myNode.msgSpace2.clear();
           }
      }

        if(sensedValue>=18 && sensedValue<32){ 
          myNode.msgSpace3.add(sensedValue);
//
            if(myNode.msgSpace3.size()>0){
//                for(int i=0; i<myNode.msgSpace3.size(); i++){
//                    temp = temp + myNode.msgSpace3.get(i);
//                }
//                avgValue=temp/myNode.msgSpace3.size();
//

                MessageDataValue msgDataValue = new MessageDataValue(sensedValue,queryId,sequenceNumber,myNode.getID());
                msgDataValue.setNormal();
                CSGPWrapperMessage msgAgwSensing
                            = new CSGPWrapperMessage(msgDataValue, sinkLocation,0, JistAPI.getTime());
                msgAgwSensing.messageID = "S:"+this.myNode.getIP().toString()+":"+this.sequenceSensing++;

                long sleepMin = 0;
                long sleepMax = myNode.clusterNeighbourList.size()/2;
                Random s = new Random();
                long sleeptime = (long) (sleepMin + (sleepMax - sleepMin) * s.nextDouble());


                JistAPI.sleepBlock((long) (sleeptime*0.5*Constants.SECOND));

                netEntity.send(msgAgwSensing,
                                          sinkAddress,
                                          routingProtocolIndex,
                                          Constants.NET_PRIORITY_NORMAL, (byte)40); 
//                 stats.markPacketSent("Third_Priority", sequenceNumber);
           //      myNode.getNodeGUI().colorCode.mark(colorProfileGeneric,ColorProfileAggregate.TRANSMIT, 5);

                myNode.msgSpace3.clear();
       }
      }
       
    if (JistAPI.getTime() < endTime){
        params.set(0, samplingInterval);
        params.set(1, endTime);
        params.set(2, queryId);
        params.set(3, sequenceNumberGlobal);
        params.set(4, sinkAddress);
        params.set(5, sinkLocation);
                
/** this is to schedule the next run(args). this
 * DO NOT use WHILE loops to do this, 
 * not call the function directly. Let JiST handle it 
 * */
    ((AppInterface)self).sensing(params);
    }
}
      

/** Callback registered with the terminal,
     * The terminal will call this function whenever the user posts a new query or just closes the terminal window
     * <p>
     * You should inspect the myNode.localTerminalDataSet.getQueryList() to check for new posted queries that your node must act upon
     * Have a look at the TerminalDataSet.java for the available data that is exchanged between this node and the terminal
*/
    
public void signalUserRequest() {
    /* We'll assume that the node through which the user has posted a query becomes a sink node */
    if (myNode.getQueryList().size() > 0 ){     
        Query query = ((LinkedList<Query>)myNode.getQueryList()).getLast();
          if (!query.isDispatched()) {        
             int[] rootIDArray = new int[1];
             rootIDArray[0] = myNode.getID();
             MessageQuery msgQuery = new MessageQuery(query);
        // wrap the MessageQuery as a SGGW message  --> WW Tambahi messageId (ip +sequence count)
            String messageId= "Q:"+this.myNode.getIP().toString()+":"+this.sequenceCount++;
            //String messageId= "Q_0:"+this.myNode.getID()+":"+"P_"+this.sequenceCount++;
            CSGPWrapperMessage msgCH= new CSGPWrapperMessage(msgQuery, query.getRegion(),0, JistAPI.getTime());
            msgCH.messageID=messageId;
            netEntity.send(msgCH,
                            null/*unknown Dest IP, only its approx location*/,
                            routingProtocolIndex /* (see Driver) */,
                            Constants.NET_PRIORITY_NORMAL, (byte)100);                  
                            
            query.dispatched(true);
            }
        }
    }
            

    
/** Message has been received. 
 * This node must be the either the sink or the source nodes 
 */
    public void receive(Message msg, NetAddress src, MacAddress lastHop, byte macId, NetAddress dst, byte priority, byte ttl) {   
        if (myNode.getEnergyManagement().getBattery().getPercentageEnergyLevel() < 5)
            return;

        if(msg instanceof MessageQuery) { /* This is a source node. It receives the query request, and not it prepares to do the periodic sensing/sampling */
            MessageQuery msgQuery = (MessageQuery)msg;

                if(msgQuery.getQuery() != null) { /* a query init message */
                    if (!startedSensing) { /* To avoid creating duplicated sensing tasks due to duplicated requests, which may happen */
                       startedSensing = true;

                       LinkedList params = new LinkedList();
                       params.add(msgQuery.getQuery().getSamplingInterval());   /* sampling interval */
                       params.add(JistAPI.getTime()/Constants.MILLI_SECOND + msgQuery.getQuery().getEndTime()); /* endTime */
                       params.add(msgQuery.getQuery().getID());
                       params.add(sequenceNumberGlobal);
                       params.add(msgQuery.getQuery().getSinkIP());
                       params.add(msgQuery.getQuery().getSinkNCSLocation2D().fromNCS(myNode.getLocationContext()));
                       //params.add(msgQuery.getQuery().getsrcIP()); //zhy, untuk menyimpan id tetangga yang mengirim query
                    JistAPI.sleepBlock(msgQuery.getQuery().getSamplingInterval());//ini adalh interval untuk szmpling
                    sensing(params);
                    }
                 }
            }

    // it is a data message, which means this node is the sink (consumer node)
        else if (msg instanceof MessageDataValue) { 
            MessageDataValue msgData = (MessageDataValue) msg;

                if(msgData.isFire()==true){
                    stats.markPacketReceived("First_Priority", msgData.sequenceNumber);
                    myNode.getNodeGUI().setUserDefinedData1((int)msgData.dataValue);
                    myNode.getNodeGUI().setUserDefinedData2((int)msgData.sequenceNumber);

                 //   System.out.println("First priority");
                 // Connecting a terminal to this node, at run time,
                 // allows the user to visualize the result of the posted query 
                    myNode.getNodeGUI().getTerminal().appendConsoleText(myNode.getNodeGUI().localTerminalDataSet,
                        "First_Priority WARNING AT Sample node " + msgData.producerNodeId + " #" +
                        msgData.sequenceNumber + " | val: " + msgData.dataValue);   

                }else if(msgData.isAbnormal()==true){
                    stats.markPacketReceived("Second_Priority", msgData.sequenceNumber);
                    myNode.getNodeGUI().setUserDefinedData1((int)msgData.dataValue);
                    myNode.getNodeGUI().setUserDefinedData2((int)msgData.sequenceNumber);

                   // System.out.println("Second priority");
                 // Connecting a terminal to this node, at run time,
                 // allows the user to visualize the result of the posted query 
                    myNode.getNodeGUI().getTerminal().appendConsoleText(myNode.getNodeGUI().localTerminalDataSet,
                        "Second_Priority TEMP AT Sample node " + msgData.producerNodeId + " #" +
                        msgData.sequenceNumber + " | val: " + msgData.dataValue);     

                }else if(msgData.isNormal()==true){  
                    stats.markPacketReceived("Third_Priority", msgData.sequenceNumber);
                    myNode.getNodeGUI().setUserDefinedData1((int)msgData.dataValue);
                    myNode.getNodeGUI().setUserDefinedData2((int)msgData.sequenceNumber);

                   // System.out.println("Third priority");
                 // Connecting a terminal to this node, at run time,
                 // allows the user to visualize the result of the posted query 
                    myNode.getNodeGUI().getTerminal() .appendConsoleText(myNode.getNodeGUI().localTerminalDataSet,
                        "Third Priority Temp at Sample node " + msgData.producerNodeId + " #" +
                        msgData.sequenceNumber + " | val: " + msgData.dataValue);

                }else if(msgData.fire==false||msgData.abnormal==false){  
                    stats.markPacketReceived("Third_Priority", msgData.sequenceNumber);
                    myNode.getNodeGUI().setUserDefinedData1((int)msgData.dataValue);
                    myNode.getNodeGUI().setUserDefinedData2((int)msgData.sequenceNumber);

              //      System.out.println("Third,else priority");
                 // Connecting a terminal to this node, at run time,
                 // allows the user to visualize the result of the posted query 
                    myNode.getNodeGUI().getTerminal() .appendConsoleText(myNode.getNodeGUI().localTerminalDataSet,
                        "Third Priority Temp at Sample node " + msgData.producerNodeId + " #" +
                        msgData.sequenceNumber + " | val: " + msgData.dataValue);             
                }
        }
    }


    
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
