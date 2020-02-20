/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package sidnet.stack.users.csgp.driver;

/**
 *
 * @author admin
 */
/*
 * Driver_CH.java
 * @version 1.0
 *
 * Created on April 15, 2008, 1:00 PM
 *
 * @author Oliviu C. Ghica, Northwestern University
 */
import sidnet.stack.users.csgp.routing.SwingRouting;
import sidnet.stack.users.csgp.routing.CSGProuting;
import sidnet.stack.users.csgp.app.ChApp;
import java.util.Arrays;
import jist.swans.misc.Mapper;
import jist.swans.misc.Location;
import jist.swans.misc.Util;
import jist.swans.net.PacketLoss;
import jist.swans.net.NetIp;
import jist.swans.net.NetAddress;

import sidnet.core.gui.SimGUI;
import sidnet.colorprofiles.ColorProfileGeneric;
import sidnet.core.gui.PanelContext;
import jist.runtime.JistAPI;
import jist.swans.Constants;

import sidnet.core.simcontrol.SimManager;
import sidnet.core.interfaces.GPS;
import jist.swans.mac.MacAddress;

import jist.swans.radio.RadioInfo;
import jist.swans.field.Field;
import jist.swans.field.Mobility;
import jist.swans.field.Placement;
import jist.swans.field.Spatial;
import jist.swans.field.Fading;
import jist.swans.field.PathLoss;

import sidnet.models.energy.batteries.Battery;
import sidnet.models.energy.batteries.IdealBattery;
import sidnet.models.energy.energyconsumptionmodels.EnergyManagementImpl;
import sidnet.models.energy.energyconsumptionmodels.EnergyConsumptionModel;
import sidnet.models.energy.energyconsumptionmodels.EnergyConsumptionModelImpl;
import sidnet.core.gui.GroupSelectionTool;
import sidnet.core.gui.TopologyGUI;
import sidnet.stack.std.mac.ieee802_15_4.Mac802_15_4Impl;
import sidnet.stack.std.mac.ieee802_15_4.Phy802_15_4Impl;

import sidnet.core.misc.GPSimpl;
import sidnet.core.misc.Location2D;
import sidnet.core.misc.LocationContext;
import sidnet.core.misc.Node;
import sidnet.models.energy.batteries.BatteryUtils;
import sidnet.models.energy.energyconsumptionmodels.EnergyManagement;
import sidnet.models.energy.energyconsumptionparameters.ElectricParameters;
import sidnet.models.energy.energyconsumptionparameters.EnergyConsumptionParameters;
import sidnet.models.senseable.phenomena.GenericDynamicPhenomenon;
import sidnet.models.senseable.phenomena.PhenomenaLayerInterface;
import sidnet.utilityviews.statscollector.StatsCollector;
import sidnet.stack.std.routing.heartbeat.HeartbeatProtocol;
import sidnet.stack.std.routing.shortestgeopath.ShortestGeoPathRouting;
import sidnet.utilityviews.energymap.EnergyMap;
import sidnet.utilityviews.statscollector.StatEntry_PacketDeliveryLatency;
import sidnet.utilityviews.statscollector.StatEntry_PacketReceivedContor;
import sidnet.utilityviews.statscollector.StatEntry_PacketReceivedPercentage;
import sidnet.utilityviews.statscollector.StatEntry_PacketSentContor;
import sidnet.utilityviews.statscollector.StatEntry_Time;
import sidnet.utilityviews.statscollector.*;

import sidnet.stack.users.csgp.colorprofile.CSGPColorProfile;
import sidnet.stack.users.csgp.routing.ShortestGeoPath;


public class SwingRoutingDriver{


    //xczx
    public static double[] tempLocX;
    public static double[] tempLocY;
    public static TopologyGUI topologyGUI = new TopologyGUI();
    public static int nodes, fieldLength, time;
    public static SequenceGenerator sequenceNumberGlobal = new SequenceGenerator();
    public static    Location2D sinkLocation;
    public static String sgp, csgp, adaptive, randomCH;
    public static int jmlCluster;


   

    /** Define the battery-type for the nodes 75mAh should give enough juice for 24-48h */
    public static Battery battery = new IdealBattery(BatteryUtils.mAhToMJ(40, 3), 3);
    public static Battery batterySink = new IdealBattery(BatteryUtils.mAhToMJ(80, 3),3);

    /** Define the power-consumption characteristics of the nodes, based on Mica Mote MPR500CA */
    public static EnergyConsumptionParameters eCostParam = new EnergyConsumptionParameters(
                                                        new ElectricParameters(     8,   // ProcessorCurrentDrawn_ActiveMode [mA],
                                                                                0.015,   // ProcessorCurrentDrawn_SleepMode [mA],
                                                                                   27,   // RadioCurrentDrawn_TransmitMode [mA],
                                                                                   10,   // RadioCurrentDrawn_ReceiveMode [mA],
                                                                                    3,   // RadioCurrentDrawn_ListenMode [mA],
                                                                                  0.5,   // RadioCurrentDrawn_SleepMode [mA],
                                                                                   10,   // SensorCurrentDrawn_ActiveMode [mA]
                                                                                  0.01   // SensorCurrentDrawn_PassiveMode [mA]
                                                                           ),
                                                        battery.getVoltage());

    /** This is the entry point in the program */
    public static void main(String[] args)
    {

        /** Command line arguments is the best way to configure run-time parameters, for now */
        if(args.length < 3)
        {
           System.out.println("syntax: swans driver.SwingRoutingDriver <nodes> <field-length [m]> <max-simulation time> <using csgp?> <using sgp?> <using adaptive sending interval?>");
           System.out.println("    eg: swans driver.SwingRoutingDriver    5          100                  50000            true            false            true");
           return;
        }

        System.out.println("Driver initialization started ... ");

        /* Parse command line arguments */
        nodes  = Integer.parseInt(args[0]);
        fieldLength = Integer.parseInt(args[1]);
        time   = Integer.parseInt(args[2]);
     
        /** Computing some statistics basic */
        float density = nodes / (float)(fieldLength/1000.0 * fieldLength/1000.0);
        System.out.println("nodes   = "+nodes);
        System.out.println("size    = "+fieldLength+" x "+fieldLength);
        System.out.println("time    = "+time+" seconds");
        System.out.print("Creating simulation nodes ... ");
        
        /** Create the simulation */
        Field f = createSim(nodes, fieldLength);

        System.out.println("Average density = "+f.computeDensity()*1000*1000+"/km^2");
        System.out.println("Average sensing = "+f.computeAvgConnectivity(true));
        System.out.println("Average receive = "+f.computeAvgConnectivity(false));


                /** Indicates WHEN the JiST simulation should self-terminate (automatically) */
        JistAPI.endAt(time * Constants.SECOND); /* so it will self-terminate after "time" seconds. Not the way we specify the unit of time */

        System.out.println("Driver initialization complete!");

    }


  /**
   * Initialize simulation environment and field
   *
   * @param nodes number of nodes
   * @param length length of field
   * @return simulation field
   */
  public static Field createSim(int nodes, int length)
  {
    System.out.println("createSim()");

    /** Launch the SIDnet main graphical interface and set-up the title */
    SimGUI simGUI = new SimGUI();
 
    /** Internal stuff: configure and start the simulation manager. Hook up control for GUI panels*/
    SimManager simManager = new SimManager(simGUI, null, SimManager.EXPERIMENT);

    /** Configure the SWANS: */

    /** Nodes deployment: random (but it can be XML-based, grid, manual place, air-dropped, etc */
    Location.Location2D bounds = new Location.Location2D(length, length);
    Location.Location2D sink = new Location.Location2D(200, 200);
    Placement place = new Placement.Random(bounds);
    //Placement place = new Placement.Grid(bounds,200,10);
  //  Placement placement = new Placement.Grid(bounds, 30, 34);
    Placement sinkPlace = new Placement.Grid(bounds, 1, 1);
  

    /** Nodes mobility: static (but nodes can move if you need to */
    Mobility mobility   = new Mobility.Static();

    /** Some other internals: Spatial configuration */
    Spatial spatial = new Spatial.HierGrid(bounds, 5);
    Fading fading = new Fading.None();
    PathLoss pathloss = new PathLoss.FreeSpace();
    Field field = new Field(spatial, fading, pathloss, mobility, Constants.PROPAGATION_LIMIT_DEFAULT);

    /** Configure the radio environment properties */
    RadioInfo.RadioInfoShared radioInfoShared = RadioInfo.createShared(
        Constants.FREQUENCY_DEFAULT, 40000 /* BANDWIDTH bps - it will be overloaded when using 802_15_4  */,
        -12 /* dBm for Mica Z */, Constants.GAIN_DEFAULT,
        Util.fromDB(Constants.SENSITIVITY_DEFAULT), Util.fromDB(Constants.THRESHOLD_DEFAULT),
        Constants.TEMPERATURE_DEFAULT, Constants.TEMPERATURE_FACTOR_DEFAULT, Constants.AMBIENT_NOISE_DEFAULT);

    /** Build up the networking stack: APP, NETWORK, MAC
     *  Technically, at the Network Layer you may have several "protocols".
     *  We keep a mapping of these protocols (indexed) so that a packet may be forwarded to the proper protocol to be handled */
    Mapper protMap = new Mapper(Constants.NET_PROTOCOL_MAX);
    protMap.mapToNext(Constants.NET_PROTOCOL_HEARTBEAT); // Constants.NET_PROTOCOL_HEARTBEAT is just a numerical value to uniquely identify (index) one of the protocols (the node discovery one)
    protMap.mapToNext(Constants.NET_PROTOCOL_INDEX_1); // and this will be the other protocol, which is, in this case, a shortest-path routing protocol.

    /** We'll assume no packet loss due to "random" conditions. Packets may still be lost due to collisions though
     *  This should be the case when developing the first-time implementation, then you can remove this constraint if you want to test your rezilience
     */
    PacketLoss pl = new PacketLoss.Zero();

    /* ******************************************
     * Create the SIDnet-specific simulation environment  *
     * ******************************************/

    /* Creating the SIDnet nodes */
    Node[] myNode = new Node[nodes];
    LocationContext fieldContext = new LocationContext(length, length);

    // [ww] estimating cluster center
    Cluster[] formedCluster= estimateClusterArea(myNode);
    



    /** StatsCollector Hook-up - to allow you to see a quick-stat including elapsed time, number of packet lost, and so on. Also used to perform run-time logging */
    
    StatsCollector statistics = new StatsCollector(myNode, length, (int) battery.getCapacity_mJ(), 30 * Constants.SECOND);
    statistics.monitor(new StatEntry_Time());
    statistics.monitor(new StatEntry_PacketSentContor("data_sensing"));
    statistics.monitor(new StatEntry_PacketReceivedContor("data_sensing"));
    statistics.monitor(new StatEntry_PacketReceivedPercentage("data_sensing"));
    statistics.monitor(new StatEntry_PacketDeliveryLatency("data_sensing", StatEntry_PacketDeliveryLatency.MODE.AVG));
    statistics.monitor(new StatEntry_DeadNodesCount("ALL", 2));
    statistics.monitor(new StatEntry_EnergyLeftPercentage("ALL", StatEntry_EnergyLeftPercentage.MODE.AVG));
    
 
    

    /** Create the sensor nodes (each at a time). Initialize each node's data and network stack */
    
    for(int i=0; i<nodes-1; i++) {
       myNode[i] = createNodeWW1(i, field, place, protMap, radioInfoShared, pl, pl, simGUI.getSensorsPanelContext(), fieldContext, simManager, statistics, topologyGUI,formedCluster,sequenceNumberGlobal);
     //  System.out.println("Node "+ myNode[i].getID() +" terbentuk di lokasi X: "+myNode[i].getLocation2D().getX()+" Y: "+myNode[i].getLocation2D().getY());
     }
    
    
        myNode[nodes-1] = createSinkNode(nodes-1, field, sinkPlace, protMap, radioInfoShared, pl, pl, simGUI.getSensorsPanelContext(), fieldContext, simManager, statistics, topologyGUI,formedCluster,sequenceNumberGlobal);
        System.out.println("Sink node created at node "+myNode[nodes-1].getID()+" x loc = "+myNode[nodes-1].getLocation2D().getX()+" y loc = "+ myNode[nodes-1].getLocation2D().getY());
    
    
    
    
    simManager.registerAndRun(statistics, simGUI.getUtilityPanelContext2()); // Indicate where do you want this to show up on the GUI
    simManager.registerAndRun(topologyGUI, simGUI.getSensorsPanelContext());
    topologyGUI.setNodeList(myNode);

    /** Configuring the sensorial layer - give the node something to sense, measure */
    PhenomenaLayerInterface phenomenaLayer = new GenericDynamicPhenomenon(); // but it can be something else, such as a moving-objects field
    simManager.registerAndRun(phenomenaLayer,simGUI.getSensorsPanelContext());     // needs to be done ... internals

    /** All the nodes will measure the same environment in this case, but this is not a limitation. You can have them heterogeneous */
    for (int i = 0; i < nodes; i++)
         myNode[i].addSensor(phenomenaLayer);


    /** Allow simManager to handle nodes' GUI (internals)*/
    simManager.register(myNode);

    /** EnergyMap hookup - give an overall view of the energy levels in the networks */
    EnergyMap energyMap = new EnergyMap(myNode);
    simManager.registerAndRun(energyMap, simGUI.getUtilityPanelContext1()); // Indicate where do you want this to show up on the GUI

    /** Add GroupInteraction capability - if you may want to be able to select a group of nodes */
    GroupSelectionTool gst = new GroupSelectionTool(myNode);
    simManager.registerAndRun(gst, simGUI.getSensorsPanelContext());
    myNode[0].getNodeGUI().setGroupSelectionTool(gst); // internals
 
    

    /** Starts the core (GUI) engine */
    simManager.getProxy().run();


    

    System.out.println("Simulation Started");



    return field;
  }

  public static Cluster[] estimateClusterArea( Node[] myNode){
      //menentukan jarak node ke cluster head
        double nodeX, nodeY;
        double jarak[];
        jarak = new double[myNode.length];
        tempLocX = new double[myNode.length];
        tempLocY = new double[myNode.length];
    
      
        float R = 60;//harusnya 85 atau ?? tergantung dari konfigurasi simulator
        double Rcx = R;
        double Rcy = 0.857*R;

        int r,c;
        r = (int)Math.round(((fieldLength/(2*Rcy))));
        c =(int)Math.round(((fieldLength/(2*0.75*Rcx)))+1);
        jmlCluster = r*c;

      
  //      System.out.println("jumlah baris = "+r);
    //    System.out.println("jumlah kolom = "+c);
        System.out.println("jumlah cluster = "+jmlCluster);


        //System.out.println("jangkauan transmisi = R = "+R);
  //      System.out.println("separuh lebar cluster = Rcx = "+Rcx);
  //      System.out.println("Separuh tinggi cluster = Rcy = "+Rcy);


        //menentukan titik tengah cluster
        double[][][] M;
        M = new double[r][c][2];
        double x=-0.5;
        double y=0;
        double setX, setY;
        double otherY=1;
        for(int i=0; i<c; i++){
            for(int j=0; j<r; j++){
                if(i%2==0){
                    setY=y*Rcy;
                    setX=x*Rcx;
                    M[j][i][0] = setX;
                    M[j][i][1]=setY;
                    y+=2;
                }else{
                    setX=x*Rcx;
                    M[j][i][0] = setX;
                    M[j][i][1] = otherY*Rcx;
                    otherY+=2;
                }
             }
            y=0;
            otherY=1;
            x+=1.5;
        }

        // preparing variabel of Cluster;

       Cluster[] cluster = new Cluster[jmlCluster];
        
        int idCluster=0;
        for(int i=0; i<c; i++){
            for(int j=0; j<r; j++){
                 cluster[idCluster]= new Cluster();
                 cluster[idCluster].clusterID=idCluster;
                 cluster[idCluster].x=M[j][i][0];
                 cluster[idCluster].y=M[j][i][1];
           //      System.out.println("koordinat titik tengah cluster ke "+j+","+i+" = "+ M[j][i][0]+" "+M[j][i][1]);
             //    System.out.println("koordinat titik tengah cluster ke ("+idCluster+")"+j+","+i+" = "+ cluster[idCluster].x +" : "+cluster[idCluster].y);
                 idCluster++;
            }
        }
        return cluster;
  }   //estimate cluster

   /**
   * Configures each node representation and network stack
   *
   * @param int id      a numerical value to represent the id of a node. Will correspond to the IP address representation
   * @param Field       the field properties
   * @param Placement   information regarding positions length of field
   * @param Mapper      network stack mapper
    *@param RadioInfo.RadioInfoShared   configuration of the radio
    *@param plIn        property of the PacketLoss for incoming data packet
    *@param plOut       property of the PacketLoss for outgoing data packet
    *@param hostPanelContext    the context of the panel this node will be drawn
    *@param fieldContext        the context of the actual field this node is in (for GPS)
    *@param simControl          handle to the simulation manager
    *@param Battery     indicate the battery that will power this particular node
    *@param StatsCollector the statistical collector tool
   */
    public static Node createNode(int id,
                                  Field field,
                                  Placement placement,
                                  Mapper protMap,
                                  RadioInfo.RadioInfoShared radioInfoShared,
                                  PacketLoss plIn,
                                  PacketLoss plOut,
                                  PanelContext hostPanelContext,
                                  LocationContext fieldContext,
                                  SimManager simControl,
                                  StatsCollector stats,
                                  TopologyGUI topologyGUI,SequenceGenerator sequenceNumber)

  {
    /** create entities (gives a physical location) */
    Location nextLocation = placement.getNextLocation();

    /** Create an individual battery, since no two nodes can be powered by the same battery. The specs of the battery are the same though */
    Battery individualBattery = new IdealBattery(battery.getCapacity_mJ(), battery.getVoltage());

    /** Set the battery and the energy consumption profile */
    EnergyConsumptionModel energyConsumptionModel = new EnergyConsumptionModelImpl(eCostParam, individualBattery);
    energyConsumptionModel.setID(id);

    /** Create the energy management unit */
    EnergyManagement energyManagementUnit = new EnergyManagementImpl(energyConsumptionModel, individualBattery);

    /** Create the node and nodeGUI interface for this node */
    Node node = new Node(id, energyManagementUnit, hostPanelContext, fieldContext, new ColorProfileGeneric(), simControl);
    node.enableRelocation(field); // if you want to be able to relocate, by mouse, the node in the field at run time.
    //RadioNoiseIndep radio = new RadioNoiseIndep(id, radioInfoShared); // uncomment this if you want noisy environments

    /** Put a 'GPS' (must to) to obtain the location information (for this assignment, for gaphical purposes only
     *  Now, really, this is not a GPS per-se, just a 'logical' way of obtaining location information from the simulator
     */
    GPS gps = new GPSimpl(new Location2D((int)nextLocation.getX(), (int)nextLocation.getY()));
    gps.configure(new LocationContext(fieldContext));
    node.setGPS(gps);

     /* *** Configuring the ISO layers - more or less self-explanatory *** */
                /* APP layer configuration */

    //   public ChApp(Node myNode,
 //   					short routingProtocolIndex,
   // 					StatsCollector stats, SequenceGenerator SequenceNumberGlobal)

              //     AgApp app = new AgApp(node, Constants.NET_PROTOCOL_INDEX_1, stats, sequenceNumber);

                ChApp app = new ChApp(node, Constants.NET_PROTOCOL_INDEX_1, stats, sequenceNumber);

                if (app.topologyGUI == null)
                    app.topologyGUI = topologyGUI;

                /* NET layer configuration - this is where the node gets its "ip" address */
                NetIp net = new NetIp(new NetAddress(id), protMap, plIn, plOut);

                /* ROUTING protocols configuration */
                HeartbeatProtocol heartbeatProtocol
                	= new HeartbeatProtocol(net.getAddress(),
                							node,
                							hostPanelContext,
                							30 * Constants.MINUTE);



             //    AggregateFixedRouting aggregateFixedRouting
               // 	= new AggregateFixedRouting(node);

             //   ChRouting chRouting
               // 	= new ChRouting(node);
                
                CSGProuting chRouting = new CSGProuting(node);

                node.setIP(net.getAddress());

                /* MAC layer configuration */
                Mac802_15_4Impl mac = new Mac802_15_4Impl(new MacAddress(id), radioInfoShared, node.getEnergyManagement(), node);

                /* PHY layer configuration */
                Phy802_15_4Impl phy = new Phy802_15_4Impl(id, radioInfoShared, energyManagementUnit, node, 0 * Constants.SECOND);

                /* RADIO "layer configuration */
                field.addRadio(phy.getRadioInfo(), phy.getProxy(), nextLocation);
                field.startMobility(phy.getRadioInfo().getUnique().getID());

                /* *** Hooking up the ISO layers *** */
                /* APP <- Routing hookup */
             //   shortestGeographicalPathRouting.setAppInterface(app.getAppProxy());
                chRouting.setAppInterface(app.getAppProxy());

                /* APP -> NET hookup */
                app.setNetEntity(net.getProxy());

                /* NET<->Routing hookup */
                heartbeatProtocol.setNetEntity(net.getProxy());
              //  shortestGeographicalPathRouting.setNetEntity(net.getProxy());
                chRouting.setNetEntity(net.getProxy());

            //    net.setProtocolHandler(Constants.NET_PROTOCOL_INDEX_1, shortestGeographicalPathRouting.getProxy());

                net.setProtocolHandler(Constants.NET_PROTOCOL_INDEX_1, chRouting.getProxy());

                net.setProtocolHandler(Constants.NET_PROTOCOL_HEARTBEAT, heartbeatProtocol.getProxy());
                //net.setMacEntity(mac);

                /* net-MAC-phy hookup */
                byte intId = net.addInterface(mac.getProxy());
                mac.setNetEntity(net.getProxy(), intId);
                mac.setPhyEntity(phy.getProxy());

                /* PHY-RADIO hookup */
                phy.setFieldEntity(field.getProxy());
                phy.setMacEntity(mac.getProxy());

    /* Here we actually start this node's application layer execution. It is important to observe
       that we don't actually call the app's run() method directly, but through its proxy, which allows JiST engine to actually decide when this call will
       be actually made (based on the simulation time)*/


    app.getAppProxy().run(null);

    return node;
  }


   // modif-ww
    public static Node createNodeWW1(int id,
                                  Field field,
                                  Placement place,
                                  Mapper protMap,
                                  RadioInfo.RadioInfoShared radioInfoShared,
                                  PacketLoss plIn,
                                  PacketLoss plOut,
                                  PanelContext hostPanelContext,
                                  LocationContext fieldContext,
                                  SimManager simControl,
                                  StatsCollector stats,
                                  TopologyGUI topologyGUI, 
                                  Cluster[] formedCluster,
                                  SequenceGenerator sequenceNumber)
  {
    /** create entities (gives a physical location) */
    Location nextLocation = place.getNextLocation();

    /** Create an individual battery, since no two nodes can be powered by the same battery. The specs of the battery are the same though */
    Battery individualBattery = new IdealBattery(battery.getCapacity_mJ(), battery.getVoltage());

    /** Set the battery and the energy consumption profile */
    EnergyConsumptionModel energyConsumptionModel = new EnergyConsumptionModelImpl(eCostParam, individualBattery);
    energyConsumptionModel.setID(id);

    /** Create the energy management unit */
    EnergyManagement energyManagementUnit = new EnergyManagementImpl(energyConsumptionModel, individualBattery);

    /** Create the node and nodeGUI interface for this node */
    Node node = new Node(id, energyManagementUnit, hostPanelContext, fieldContext, new CSGPColorProfile(), simControl);
    node.enableRelocation(field); // if you want to be able to relocate, by mouse, the node in the field at run time.
    //RadioNoiseIndep radio = new RadioNoiseIndep(id, radioInfoShared); // uncomment this if you want noisy environments

    /** Put a 'GPS' (must to) to obtain the location information (for this assignment, for gaphical purposes only
     *  Now, really, this is not a GPS per-se, just a 'logical' way of obtaining location information from the simulator
     */
    GPS gps = new GPSimpl(new Location2D((int)nextLocation.getX(), (int)nextLocation.getY()));
    gps.configure(new LocationContext(fieldContext));
    node.setGPS(gps);

     /* *** Configuring the ISO layers - more or less self-explanatory *** */
                /* APP layer configuration */
                ChApp app = new ChApp(node, Constants.NET_PROTOCOL_INDEX_1, stats, sequenceNumber);

                if (app.topologyGUI == null)
                    app.topologyGUI = topologyGUI;

                /* NET layer configuration - this is where the node gets its "ip" address */
                NetIp net = new NetIp(new NetAddress(id), protMap, plIn, plOut);

                // Measuring ClusterID;
                double x = node.getLocation2D().getX();
                double y = node.getLocation2D().getY();

                int clusterId=0;
                double distToCluster;
                double minToCluster=1000000; // set initial maximum values;
                for (int a=0;a< formedCluster.length;a++){
                    distToCluster = Math.sqrt((x-formedCluster[a].x)*(x-formedCluster[a].x) + (y-formedCluster[a].y)*(y-formedCluster[a].y));
                    if (distToCluster < minToCluster){
                        clusterId=a;
                        minToCluster = distToCluster;
                        node.clusterId=clusterId;
                        node.distToClusterCenter=distToCluster;

                    }
                }
             //   System.out.println("Node " + node.getID() + " , join cluster " + node.getClusterID() +"("+ formedCluster[node.getClusterID()].x+","+ formedCluster[node.getClusterID()].y+ ")" +" , dist to cluter : "+ node.distToClusterCenter );
              //  return Math.sqrt((x-toLocation.getX())*(x-toLocation.getX()) + (y-toLocation.getY())*(y-toLocation.getY()));
                
                
               

              //sampai disini

                /* ROUTING protocols configuration */
                
                
                // test
                
                HeartbeatProtocol heartbeatProtocol
                	= new HeartbeatProtocol(net.getAddress(),
                							node,
                							hostPanelContext,
                							30 * Constants.MINUTE);

              // ganti dari SGP
             //   ShortestGeoPathRouting shortestGeographicalPathRouting   = new ShortestGeoPathRouting(node);

                // ini draft routing yang dibuat
       //         SwingRouting mySwingRouting  = new SwingRouting(node);

                  SwingRouting mySwingRouting  	= new SwingRouting(node ,sequenceNumber );
                   if(mySwingRouting.topologyGUI == null)
                    mySwingRouting.topologyGUI = topologyGUI;

               //Sgp sgp
               //= new Sgp(node); 
                
             /*   ShortestGeoPathRouting ClusterHeadRouting
                	= new ShortestGeoPathRouting(node);
*/
                node.setIP(net.getAddress());

                /* MAC layer configuration */
                Mac802_15_4Impl mac = new Mac802_15_4Impl(new MacAddress(id), radioInfoShared, node.getEnergyManagement(), node);

                /* PHY layer configuration */
                Phy802_15_4Impl phy = new Phy802_15_4Impl(id, radioInfoShared, energyManagementUnit, node, 0 * Constants.SECOND);

                /* RADIO "layer configuration */
                field.addRadio(phy.getRadioInfo(), phy.getProxy(), nextLocation);
                field.startMobility(phy.getRadioInfo().getUnique().getID());

                /* *** Hooking up the ISO layers *** */
                /* APP <- Routing hookup */
              //  shortestGeographicalPathRouting.setAppInterface(app.getAppProxy());
                
                    mySwingRouting.setAppInterface(app.getAppProxy());
                    //sgp.setAppInterface(app.getAppProxy());
                    
                /* APP -> NET hookup */
                app.setNetEntity(net.getProxy());

                /* NET<->Routing hookup */
                heartbeatProtocol.setNetEntity(net.getProxy());
                //sgp.setNetEntity(net.getProxy());
               // ganti baru
                    mySwingRouting.setNetEntity(net.getProxy());
              
                //net.setProtocolHandler(Constants.NET_PROTOCOL_INDEX_1, sgp.getProxy());

                    net.setProtocolHandler(Constants.NET_PROTOCOL_INDEX_1, mySwingRouting.getProxy());
              
                

                net.setProtocolHandler(Constants.NET_PROTOCOL_HEARTBEAT, heartbeatProtocol.getProxy());
                //net.setMacEntity(mac);

                /* net-MAC-phy hookup */
                byte intId = net.addInterface(mac.getProxy());
                mac.setNetEntity(net.getProxy(), intId);
                mac.setPhyEntity(phy.getProxy());

                /* PHY-RADIO hookup */
                phy.setFieldEntity(field.getProxy());
                phy.setMacEntity(mac.getProxy());



    /* Here we actually start this node's application layer execution. It is important to observe
       that we don't actually call the app's run() method directly, but through its proxy, which allows JiST engine to actually decide when this call will
       be actually made (based on the simulation time)*/


    app.getAppProxy().run(null);
    

    return node;
  }
public static Node createNodeSGP(int id,
                                  Field field,
                                  Placement place,
                                  Mapper protMap,
                                  RadioInfo.RadioInfoShared radioInfoShared,
                                  PacketLoss plIn,
                                  PacketLoss plOut,
                                  PanelContext hostPanelContext,
                                  LocationContext fieldContext,
                                  SimManager simControl,
                                  StatsCollector stats,
                                  TopologyGUI topologyGUI, 
                                  Cluster[] formedCluster,
                                  SequenceGenerator sequenceNumber)
  {
    /** create entities (gives a physical location) */
    Location nextLocation = place.getNextLocation();

    /** Create an individual battery, since no two nodes can be powered by the same battery. The specs of the battery are the same though */
    Battery individualBattery = new IdealBattery(battery.getCapacity_mJ(), battery.getVoltage());

    /** Set the battery and the energy consumption profile */
    EnergyConsumptionModel energyConsumptionModel = new EnergyConsumptionModelImpl(eCostParam, individualBattery);
    energyConsumptionModel.setID(id);

    /** Create the energy management unit */
    EnergyManagement energyManagementUnit = new EnergyManagementImpl(energyConsumptionModel, individualBattery);

    /** Create the node and nodeGUI interface for this node */
    Node node = new Node(id, energyManagementUnit, hostPanelContext, fieldContext, new CSGPColorProfile(), simControl);
    node.enableRelocation(field); // if you want to be able to relocate, by mouse, the node in the field at run time.
    //RadioNoiseIndep radio = new RadioNoiseIndep(id, radioInfoShared); // uncomment this if you want noisy environments

    /** Put a 'GPS' (must to) to obtain the location information (for this assignment, for gaphical purposes only
     *  Now, really, this is not a GPS per-se, just a 'logical' way of obtaining location information from the simulator
     */
    GPS gps = new GPSimpl(new Location2D((int)nextLocation.getX(), (int)nextLocation.getY()));
    gps.configure(new LocationContext(fieldContext));
    node.setGPS(gps);

     /* *** Configuring the ISO layers - more or less self-explanatory *** */
                /* APP layer configuration */
                ChApp app = new ChApp(node, Constants.NET_PROTOCOL_INDEX_1, stats, sequenceNumber);

                if (app.topologyGUI == null)
                    app.topologyGUI = topologyGUI;

                /* NET layer configuration - this is where the node gets its "ip" address */
                NetIp net = new NetIp(new NetAddress(id), protMap, plIn, plOut);
                
                // Measuring ClusterID;
                double x = node.getLocation2D().getX();
                double y = node.getLocation2D().getY();

                int clusterId=0;
                double distToCluster;
                double minToCluster=1000000; // set initial maximum values;
                for (int a=0;a< formedCluster.length;a++){
                    distToCluster = Math.sqrt((x-formedCluster[a].x)*(x-formedCluster[a].x) + (y-formedCluster[a].y)*(y-formedCluster[a].y));
                    if (distToCluster < minToCluster){
                        clusterId=a;
                        minToCluster = distToCluster;
                        node.clusterId=clusterId;
                        node.distToClusterCenter=distToCluster;

                    }
                }
        //        System.out.println("Node " + node.getID() + " , join cluster " + node.getClusterID() +"("+ formedCluster[node.getClusterID()].x+","+ formedCluster[node.getClusterID()].y+ ")" +" , dist to cluter : "+ node.distToClusterCenter );
              //  return Math.sqrt((x-toLocation.getX())*(x-toLocation.getX()) + (y-toLocation.getY())*(y-toLocation.getY()));
                
                
               

              //sampai disini

                /* ROUTING protocols configuration */
                HeartbeatProtocol heartbeatProtocol
                	= new HeartbeatProtocol(net.getAddress(),
                							node,
                							hostPanelContext,
                							30 * Constants.MINUTE);

              // ganti dari SGP
             //   ShortestGeoPathRouting shortestGeographicalPathRouting   = new ShortestGeoPathRouting(node);

                // ini draft routing yang dibuat
          //      Csgp mySwingRouting
            //   	= new Csgp(node);
            
               SwingRouting sgp
               = new SwingRouting(node); 
                
             /*   ShortestGeoPathRouting ClusterHeadRouting
                	= new ShortestGeoPathRouting(node);
*/
                node.setIP(net.getAddress());

                /* MAC layer configuration */
                Mac802_15_4Impl mac = new Mac802_15_4Impl(new MacAddress(id), radioInfoShared, node.getEnergyManagement(), node);

                /* PHY layer configuration */
                Phy802_15_4Impl phy = new Phy802_15_4Impl(id, radioInfoShared, energyManagementUnit, node, 0 * Constants.SECOND);

                /* RADIO "layer configuration */
                field.addRadio(phy.getRadioInfo(), phy.getProxy(), nextLocation);
                field.startMobility(phy.getRadioInfo().getUnique().getID());

                /* *** Hooking up the ISO layers *** */
                /* APP <- Routing hookup */
              //  shortestGeographicalPathRouting.setAppInterface(app.getAppProxy());
                
                    sgp.setAppInterface(app.getAppProxy());
              
                /* APP -> NET hookup */
                app.setNetEntity(net.getProxy());

                /* NET<->Routing hookup */
                heartbeatProtocol.setNetEntity(net.getProxy());
              //  shortestGeographicalPathRouting.setNetEntity(net.getProxy());
               // ganti baru
                    sgp.setNetEntity(net.getProxy());
              
              //  net.setProtocolHandler(Constants.NET_PROTOCOL_INDEX_1, shortestGeographicalPathRouting.getProxy());

                    net.setProtocolHandler(Constants.NET_PROTOCOL_INDEX_1, sgp.getProxy());
              
                

                net.setProtocolHandler(Constants.NET_PROTOCOL_HEARTBEAT, heartbeatProtocol.getProxy());
                //net.setMacEntity(mac);

                /* net-MAC-phy hookup */
                byte intId = net.addInterface(mac.getProxy());
                mac.setNetEntity(net.getProxy(), intId);
                mac.setPhyEntity(phy.getProxy());

                /* PHY-RADIO hookup */
                phy.setFieldEntity(field.getProxy());
                phy.setMacEntity(mac.getProxy());



    /* Here we actually start this node's application layer execution. It is important to observe
       that we don't actually call the app's run() method directly, but through its proxy, which allows JiST engine to actually decide when this call will
       be actually made (based on the simulation time)*/


    app.getAppProxy().run(null);
    

    return node;
  }
    /**
     *
     * @param id
     * @param field
     * @param placement
     * @param protMap
     * @param radioInfoShared
     * @param plIn
     * @param plOut
     * @param hostPanelContext
     * @param fieldContext
     * @param simControl
     * @param stats
     * @param topologyGUI
     * @param formedCluster
     * @param sequenceNumber
     * @return
     */
    public static Node createSinkNode(int id,
                                  Field field,
                                  Placement sinkPlace,
                                  Mapper protMap,
                                  RadioInfo.RadioInfoShared radioInfoShared,
                                  PacketLoss plIn,
                                  PacketLoss plOut,
                                  PanelContext hostPanelContext,
                                  LocationContext fieldContext,
                                  SimManager simControl,
                                  StatsCollector stats,
                                  TopologyGUI topologyGUI, 
                                  Cluster[] formedCluster,
                                  SequenceGenerator sequenceNumber)
  {
    /** create entities (gives a physical location) */
    Location nextLocation = sinkPlace.getNextLocation();

    /** Create an individual battery, since no two nodes can be powered by the same battery. The specs of the battery are the same though */
    Battery individualBattery = new IdealBattery(batterySink.getCapacity_mJ(), batterySink.getVoltage());

    /** Set the battery and the energy consumption profile */
    EnergyConsumptionModel energyConsumptionModel = new EnergyConsumptionModelImpl(eCostParam, individualBattery);
    energyConsumptionModel.setID(id);

    /** Create the energy management unit */
    EnergyManagement energyManagementUnit = new EnergyManagementImpl(energyConsumptionModel, individualBattery);

    /** Create the node and nodeGUI interface for this node */
    Node node = new Node(id, energyManagementUnit, hostPanelContext, fieldContext, new CSGPColorProfile(), simControl);
    node.enableRelocation(field); // if you want to be able to relocate, by mouse, the node in the field at run time.
    //RadioNoiseIndep radio = new RadioNoiseIndep(id, radioInfoShared); // uncomment this if you want noisy environments

    /** Put a 'GPS' (must to) to obtain the location information (for this assignment, for gaphical purposes only
     *  Now, really, this is not a GPS per-se, just a 'logical' way of obtaining location information from the simulator
     */
    GPS gps = new GPSimpl(new Location2D((int)nextLocation.getX(), (int)nextLocation.getY()));
    gps.configure(new LocationContext(fieldContext));
    node.setGPS(gps);

     /* *** Configuring the ISO layers - more or less self-explanatory *** */
                /* APP layer configuration */
                ChApp app = new ChApp(node, Constants.NET_PROTOCOL_INDEX_1, stats, sequenceNumber);

                if (app.topologyGUI == null)
                    app.topologyGUI = topologyGUI;

                /* NET layer configuration - this is where the node gets its "ip" address */
                NetIp net = new NetIp(new NetAddress(id), protMap, plIn, plOut);

                // Measuring ClusterID;
                double x = node.getLocation2D().getX();
                double y = node.getLocation2D().getY();

                int clusterId=0;
                double distToCluster;
                double minToCluster=1000000; // set initial maximum values;
                for (int a=0;a< formedCluster.length;a++){
                    distToCluster = Math.sqrt((x-formedCluster[a].x)*(x-formedCluster[a].x) + (y-formedCluster[a].y)*(y-formedCluster[a].y));
                    if (distToCluster < minToCluster){
                        clusterId=a;
                        minToCluster = distToCluster;
                        node.clusterId=clusterId;
                        node.distToClusterCenter=distToCluster;

                    }
                }
          //      System.out.println("Node " + node.getID() + " , join cluster " + node.getClusterID() +"("+ formedCluster[node.getClusterID()].x+","+ formedCluster[node.getClusterID()].y+ ")" +" , dist to cluter : "+ node.distToClusterCenter );
              //  return Math.sqrt((x-toLocation.getX())*(x-toLocation.getX()) + (y-toLocation.getY())*(y-toLocation.getY()));
                
                
               

              //sampai disini

                /* ROUTING protocols configuration */
                HeartbeatProtocol heartbeatProtocol
                	= new HeartbeatProtocol(net.getAddress(),
                							node,
                							hostPanelContext,
                							30 * Constants.MINUTE);

              // ganti dari SGP
             //   ShortestGeoPathRouting shortestGeographicalPathRouting   = new ShortestGeoPathRouting(node);

                // ini draft routing yang dibuat
                
               SwingRouting ClusterHeadRouting
              	= new SwingRouting(node);
                
             //   Sgp ClusterHeadRouting
               // 	= new Sgp(node); 
                
             /*   ShortestGeoPathRouting ClusterHeadRouting
                	= new ShortestGeoPathRouting(node);
*/
                node.setIP(net.getAddress());

                /* MAC layer configuration */
                Mac802_15_4Impl mac = new Mac802_15_4Impl(new MacAddress(id), radioInfoShared, node.getEnergyManagement(), node);

                /* PHY layer configuration */
                Phy802_15_4Impl phy = new Phy802_15_4Impl(id, radioInfoShared, energyManagementUnit, node, 0 * Constants.SECOND);

                /* RADIO "layer configuration */
                field.addRadio(phy.getRadioInfo(), phy.getProxy(), nextLocation);
                field.startMobility(phy.getRadioInfo().getUnique().getID());

                /* *** Hooking up the ISO layers *** */
                /* APP <- Routing hookup */
              //  shortestGeographicalPathRouting.setAppInterface(app.getAppProxy());
                ClusterHeadRouting.setAppInterface(app.getAppProxy());

                /* APP -> NET hookup */
                app.setNetEntity(net.getProxy());

                /* NET<->Routing hookup */
                heartbeatProtocol.setNetEntity(net.getProxy());
              //  shortestGeographicalPathRouting.setNetEntity(net.getProxy());
               // ganti baru
                ClusterHeadRouting.setNetEntity(net.getProxy());

              //  net.setProtocolHandler(Constants.NET_PROTOCOL_INDEX_1, shortestGeographicalPathRouting.getProxy());

                net.setProtocolHandler(Constants.NET_PROTOCOL_INDEX_1, ClusterHeadRouting.getProxy());

                net.setProtocolHandler(Constants.NET_PROTOCOL_HEARTBEAT, heartbeatProtocol.getProxy());
                //net.setMacEntity(mac);

                /* net-MAC-phy hookup */
                byte intId = net.addInterface(mac.getProxy());
                mac.setNetEntity(net.getProxy(), intId);
                mac.setPhyEntity(phy.getProxy());

                /* PHY-RADIO hookup */
                phy.setFieldEntity(field.getProxy());
                phy.setMacEntity(mac.getProxy());



    /* Here we actually start this node's application layer execution. It is important to observe
       that we don't actually call the app's run() method directly, but through its proxy, which allows JiST engine to actually decide when this call will
       be actually made (based on the simulation time)*/


    app.getAppProxy().run(null);
    

    return node;
  }
public static Node createSinkSGP(int id,
                                  Field field,
                                  Placement sinkPlace,
                                  Mapper protMap,
                                  RadioInfo.RadioInfoShared radioInfoShared,
                                  PacketLoss plIn,
                                  PacketLoss plOut,
                                  PanelContext hostPanelContext,
                                  LocationContext fieldContext,
                                  SimManager simControl,
                                  StatsCollector stats,
                                  TopologyGUI topologyGUI, 
                                  Cluster[] formedCluster,
                                  SequenceGenerator sequenceNumber)
  {
    /** create entities (gives a physical location) */
    Location nextLocation = sinkPlace.getNextLocation();

    /** Create an individual battery, since no two nodes can be powered by the same battery. The specs of the battery are the same though */
    Battery individualBattery = new IdealBattery(batterySink.getCapacity_mJ(), batterySink.getVoltage());

    /** Set the battery and the energy consumption profile */
    EnergyConsumptionModel energyConsumptionModel = new EnergyConsumptionModelImpl(eCostParam, individualBattery);
    energyConsumptionModel.setID(id);

    /** Create the energy management unit */
    EnergyManagement energyManagementUnit = new EnergyManagementImpl(energyConsumptionModel, individualBattery);

    /** Create the node and nodeGUI interface for this node */
    Node node = new Node(id, energyManagementUnit, hostPanelContext, fieldContext, new CSGPColorProfile(), simControl);
    node.enableRelocation(field); // if you want to be able to relocate, by mouse, the node in the field at run time.
    //RadioNoiseIndep radio = new RadioNoiseIndep(id, radioInfoShared); // uncomment this if you want noisy environments

    /** Put a 'GPS' (must to) to obtain the location information (for this assignment, for gaphical purposes only
     *  Now, really, this is not a GPS per-se, just a 'logical' way of obtaining location information from the simulator
     */
    GPS gps = new GPSimpl(new Location2D((int)nextLocation.getX(), (int)nextLocation.getY()));
    gps.configure(new LocationContext(fieldContext));
    node.setGPS(gps);

     /* *** Configuring the ISO layers - more or less self-explanatory *** */
                /* APP layer configuration */
                ChApp app = new ChApp(node, Constants.NET_PROTOCOL_INDEX_1, stats, sequenceNumber);

                if (app.topologyGUI == null)
                    app.topologyGUI = topologyGUI;

                /* NET layer configuration - this is where the node gets its "ip" address */
                NetIp net = new NetIp(new NetAddress(id), protMap, plIn, plOut);

                // Measuring ClusterID;
                double x = node.getLocation2D().getX();
                double y = node.getLocation2D().getY();

                int clusterId=0;
                double distToCluster;
                double minToCluster=1000000; // set initial maximum values;
                for (int a=0;a< formedCluster.length;a++){
                    distToCluster = Math.sqrt((x-formedCluster[a].x)*(x-formedCluster[a].x) + (y-formedCluster[a].y)*(y-formedCluster[a].y));
                    if (distToCluster < minToCluster){
                        clusterId=a;
                        minToCluster = distToCluster;
                        node.clusterId=clusterId;
                        node.distToClusterCenter=distToCluster;

                    }
                }
      //          System.out.println("Node " + node.getID() + " , join cluster " + node.getClusterID() +"("+ formedCluster[node.getClusterID()].x+","+ formedCluster[node.getClusterID()].y+ ")" +" , dist to cluter : "+ node.distToClusterCenter );
              //  return Math.sqrt((x-toLocation.getX())*(x-toLocation.getX()) + (y-toLocation.getY())*(y-toLocation.getY()));
                
                
               

              //sampai disini

                /* ROUTING protocols configuration */
                HeartbeatProtocol heartbeatProtocol
                	= new HeartbeatProtocol(net.getAddress(),
                							node,
                							hostPanelContext,
                							30 * Constants.MINUTE);

              // ganti dari SGP
             //   ShortestGeoPathRouting shortestGeographicalPathRouting   = new ShortestGeoPathRouting(node);

                // ini draft routing yang dibuat
                
               //Csgp ClusterHeadRouting
              	//= new Csgp(node);
                
                SwingRouting ClusterHeadRouting
              	= new SwingRouting(node); 
                
             /*   ShortestGeoPathRouting ClusterHeadRouting
                	= new ShortestGeoPathRouting(node);
*/
                node.setIP(net.getAddress());

                /* MAC layer configuration */
                Mac802_15_4Impl mac = new Mac802_15_4Impl(new MacAddress(id), radioInfoShared, node.getEnergyManagement(), node);

                /* PHY layer configuration */
                Phy802_15_4Impl phy = new Phy802_15_4Impl(id, radioInfoShared, energyManagementUnit, node, 0 * Constants.SECOND);

                /* RADIO "layer configuration */
                field.addRadio(phy.getRadioInfo(), phy.getProxy(), nextLocation);
                field.startMobility(phy.getRadioInfo().getUnique().getID());

                /* *** Hooking up the ISO layers *** */
                /* APP <- Routing hookup */
              //  shortestGeographicalPathRouting.setAppInterface(app.getAppProxy());
                ClusterHeadRouting.setAppInterface(app.getAppProxy());

                /* APP -> NET hookup */
                app.setNetEntity(net.getProxy());

                /* NET<->Routing hookup */
                heartbeatProtocol.setNetEntity(net.getProxy());
              //  shortestGeographicalPathRouting.setNetEntity(net.getProxy());
               // ganti baru
                ClusterHeadRouting.setNetEntity(net.getProxy());

              //  net.setProtocolHandler(Constants.NET_PROTOCOL_INDEX_1, shortestGeographicalPathRouting.getProxy());

                net.setProtocolHandler(Constants.NET_PROTOCOL_INDEX_1, ClusterHeadRouting.getProxy());

                net.setProtocolHandler(Constants.NET_PROTOCOL_HEARTBEAT, heartbeatProtocol.getProxy());
                //net.setMacEntity(mac);

                /* net-MAC-phy hookup */
                byte intId = net.addInterface(mac.getProxy());
                mac.setNetEntity(net.getProxy(), intId);
                mac.setPhyEntity(phy.getProxy());

                /* PHY-RADIO hookup */
                phy.setFieldEntity(field.getProxy());
                phy.setMacEntity(mac.getProxy());



    /* Here we actually start this node's application layer execution. It is important to observe
       that we don't actually call the app's run() method directly, but through its proxy, which allows JiST engine to actually decide when this call will
       be actually made (based on the simulation time)*/


    app.getAppProxy().run(null);
    

    return node;
  }

}
