/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sidnet.stack.users.csgp_adaptivepath.driver;
/**
 *
 * @author Maira_Fakhri
 */
//dadas
import sidnet.stack.users.csgp_adaptivepath.app.CSGPAP_App;
import sidnet.stack.users.csgp_adaptivepath.driver.CSGPAP_Driver;
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
import sidnet.stack.users.csgp_adaptivepath.CSGPAPColorProfile.CSGPAPColorProfile;
import sidnet.stack.users.csgp_adaptivepath.routing.Csgp;
import sidnet.stack.users.csgp_adaptivepath.routing.csgpaproutingmodif;
import sidnet.stack.users.csgp_adaptivepath.routing.Sgp;
import sidnet.utilityviews.energymap.EnergyMap;
import sidnet.utilityviews.statscollector.*;

public class CSGPAP_Driver {
    public static double[]tempLocX;
    public static double[]tempLocY;
    public static TopologyGUI topologyGUI=new TopologyGUI();
    public static int nodes,fieldLength,time,jmlCluster;
    public static SequenceGenerator sequenceNumberGlobal=new SequenceGenerator();
    public static Location2D sinkLocation;
    public static String sgp,csgp,randomCH,adaptivePath;
 
//**Define the battery-type for the nodes 75mAh should give enough juice for 24-48h*/
    public static Battery battery=new IdealBattery(BatteryUtils.mAhToMJ(40,3),3);
    public static Battery batterysink=new IdealBattery(BatteryUtils.mAhToMJ(80,3),3);
    
/**Define the power consumption characteristics of nodes, based on Mica Mote MPR500CA*/
    public static  EnergyConsumptionParameters eCostParam=new EnergyConsumptionParameters(
                                     new ElectricParameters(8,//ProcessCurrentDrawn_ActiveMode[mA],
                                                            0.015,//ProcessCurrentDrawn_SleepMode[mA]
                                                            27,//RadioCurrentDrawn_TransmitMode[mA]
                                                            10,//RadioCurrentDrawn_ReceiveMode[mA]
                                                            3,//RadioCurrentDrawn_ListenMode[mA]
                                                            0.5,//RadioCurrentDrawn_SleepMode[mA]
                                                            10,//SensorCurrentDrawn_ActiveMode[mA]
                                                            0.01//SensorCurrentDrawn_PassiveMode[mA]
                                     ),
                                    battery.getVoltage());
    
/**this is the entry point in the program */
    public static void main(String[] args)
    {
        /*command line arguments is the best way to configure run-time parameters, for now*/
        if(args.length<3)
        {
            System.out.println("Syntax: swans driver.CSGP_adaptivePath_driver<nodes><field-lenght[m]><max-simulation time><using csgpadaptivepath?><using csgp?><using sgp?><using randomCH?>");
            System.out.println("    eg: swans driver.CSGP_AdaptivePath_driver   5       100              5000                         true            false         true           false");
            return;
        }
    System.out.println("Driver Initialization started..........");
    /*Parse command line arguments*/
        nodes = Integer.parseInt(args[0]);
        fieldLength = Integer.parseInt(args[1]);
        time = Integer.parseInt(args[2]);
      
    //Computing some statistics basic//
        float density=nodes/(float)(fieldLength/1000.0 * fieldLength/1000.0);
        System.out.println("Nodes = "+nodes);
        System.out.println("Size = "+fieldLength+"x"+fieldLength);
        System.out.println("time = "+time+" Seconds");
        System.out.println("Creating Simulation nodes........");

        //Creating the simulation
        Field f=createSim(nodes, fieldLength);

        System.out.println("Average Density = "+f.computeDensity()*1000*1000+"km^2");
        System.out.println("Average Sensing = "+f.computeAvgConnectivity(true));
        System.out.println("Average Receive = "+f.computeAvgConnectivity(false));

            //indicates WHEN the jiST simulation should self-terminate(automaticallly)
        JistAPI.endAt(time*Constants.SECOND);/*so it will self-terminate after "time" seconds. not the way we specify the unit of time*/
        System.out.println(" Initialization Complete!");
    }
    
/** Initialize Simulation Environment and Field
 * @param nodes number of node
 * @param length length of field
 * @return simulation field **/
 
    public static Field createSim(int nodes, int length){
        System.out.println("Create Sim()");
        
    /**Launch the SIDNet Main Graphical Interface and set-up the title*/
        SimGUI simGUI=new SimGUI();
     
    /**Internal stuff: Configure and start the simualtor manager. Hook up control for GUI panels**/
        SimManager simManager=new SimManager(simGUI, null, SimManager.EXPERIMENT);
        
    /**Cofigure the SWANS: */
    /**Nodes Deployment: random(but it can be XML-based, grid, manual place, air-dropped,etc*/
        Location.Location2D bounds=new Location.Location2D(length, length);
        Location.Location2D sink = new Location.Location2D(200, 200);
        Placement place=new Placement.Random(bounds);
        Placement sinkPlace= new Placement.Grid(bounds, 1, 1);
    
    /**Nodes Mobility: Static (but node can move if yoou need to*/
        Mobility mobility=new Mobility.Static();
    
    /**Some other internals:Spatial Configuration*/
        Spatial spatial=new Spatial.HierGrid(bounds, 5);
        Fading fading=new Fading.None();
        PathLoss pathloss=new PathLoss.FreeSpace();
        Field field=new Field(spatial, fading, pathloss, mobility, Constants.PROPAGATION_LIMIT_DEFAULT);
    
    /**Configure the radio environment Properties*/
        RadioInfo.RadioInfoShared radioInfoShared=RadioInfo.createShared(
            Constants.FREQUENCY_DEFAULT, 40000 /**BANDWIDTH bps-it will be overloaded when using 802_15_4*/,
            -11 /*dBm for Miza Z*/, Constants.GAIN_DEFAULT, Util.fromDB(Constants.SENSITIVITY_DEFAULT),
            Util.fromDB(Constants.THRESHOLD_DEFAULT), Constants.TEMPERATURE_DEFAULT,
            Constants.TEMPERATURE_FACTOR_DEFAULT, Constants.AMBIENT_NOISE_DEFAULT);
    
    /**Build up the networking stack
     * Technically, at the network layer you may have several "protocol".
     * we keep a maping of these protocols(indexed) so that a packet may be forwarded to the proper protocol to be handled
     */
        Mapper protMap=new Mapper(Constants.NET_PROTOCOL_MAX);
        protMap.mapToNext(Constants.NET_PROTOCOL_HEARTBEAT);//Constants.NET_PROTOCOL_HEARTBEAT is just a numerical value to uniquely identify*/
        protMap.mapToNext(Constants.NET_PROTOCOL_INDEX_1);/*and this will be the other protocol, which is in this case a shortest path routing protocol*/
    
    /**We'll assume no packet loss due to "random" condition. Packets may still be lost due to collisions through
     * this is should be the case when developing the first-time implementation, then you can remove this constraint if you want to test your rezilience
     */
        PacketLoss pl=new PacketLoss.Zero();
 
    
    /****************************************************
     * Create the Sidnet specific simulation environment*
     ****************************************************/
     /*Creating the SIDNet Nodes*/
        Node[] myNode=new Node[nodes];
        LocationContext fieldContext = new LocationContext(length, length);
        Cluster[] formedCluster= estimateClusterArea(myNode); //[WW] Estimating Cluster Center
    
    /** StatsCollector Hook-up - to allow you to see a quick-stat including elapsed time, number of packet lost, and so on. Also used to perform run-time logging */
        StatsCollector statistics = new StatsCollector(myNode, length, (int) battery.getCapacity_mJ(), 30 * Constants.SECOND);
        statistics.monitor(new StatEntry_Time());
        statistics.monitor(new StatEntry_PacketSentContor("First_Priority"));
        statistics.monitor(new StatEntry_PacketReceivedContor("First_Priority"));
        statistics.monitor(new StatEntry_PacketReceivedPercentage("First_Priority"));
        statistics.monitor(new StatEntry_PacketDeliveryLatency("First_Priority", StatEntry_PacketDeliveryLatency.MODE.AVG));
        statistics.monitor(new StatEntry_PacketSentContor("Second_Priority"));
        statistics.monitor(new StatEntry_PacketReceivedContor("Second_Priority"));
        statistics.monitor(new StatEntry_PacketReceivedPercentage("Second_Priority"));
        statistics.monitor(new StatEntry_PacketDeliveryLatency("Second_Priority", StatEntry_PacketDeliveryLatency.MODE.AVG));
        statistics.monitor(new StatEntry_PacketSentContor("Third_Priority"));
        statistics.monitor(new StatEntry_PacketReceivedContor("Third_Priority"));
        statistics.monitor(new StatEntry_PacketReceivedPercentage("Third_Priority"));
        statistics.monitor(new StatEntry_PacketDeliveryLatency("Third_Priority", StatEntry_PacketDeliveryLatency.MODE.AVG));
        statistics.monitor(new StatEntry_DeadNodesCount("ALL", 2));
        statistics.monitor(new StatEntry_EnergyLeftPercentage("ALL-NODES", StatEntry_EnergyLeftPercentage.MODE.AVG));
        //statistics.monitor(new StatEntry_AliveCount("NCA", 5));
    
    
 
    //Create the sensor nodes (each at a time).
            for(int i=0; i<nodes-1; i++){
                myNode[i] = createNodeCSGP(i, field, place, protMap, radioInfoShared, pl, pl, simGUI.getSensorsPanelContext(), fieldContext, simManager, statistics, topologyGUI,formedCluster,sequenceNumberGlobal,myNode);
           //System.out.println("Node "+myNode[i].getID()+"Terbentuk dilokasi X:"+myNode[i].getLocation2D().getX()+"Y: "+myNode[i].getLocation2D().getY());
            }
                myNode[nodes-1]=createNodeCSGP(nodes-1, field, sinkPlace, protMap, radioInfoShared, pl, pl, simGUI.getSensorsPanelContext(), fieldContext, simManager, statistics, topologyGUI,formedCluster,sequenceNumberGlobal,myNode);
                    System.out.println("Sink node created at node "+myNode[nodes-1].getID()+"x loc:"+myNode[nodes-1].getLocation2D().getX()+"y loc: "+myNode[nodes-1].getLocation2D().getY());
        

        simManager.registerAndRun(statistics, simGUI.getUtilityPanelContext2());//indicate where do you want this to show up on the GUI
        simManager.registerAndRun(topologyGUI, simGUI.getSensorsPanelContext());
        topologyGUI.setNodeList(myNode);

    //configuring the sensorial layer give the node something to sense, measure
        PhenomenaLayerInterface phenomenaLayer = new GenericDynamicPhenomenon();//but it can something else, such as a moving objects field
        simManager.registerAndRun(phenomenaLayer, simGUI.getSensorsPanelContext());//need tobe done.... internals

    //All the nodes will measure the same environment in this case, but this is nor a limitation. you can have them heterogeneous
        for(int i=0; i<nodes; i++)
            myNode[i].addSensor(phenomenaLayer);

    //allow simmanager to handle nodes GUI(internals)
        simManager.register(myNode);

    //EnergyMap hookup - give and overall view of the energy levels in the networks.
        EnergyMap energyMap=new EnergyMap(myNode);
        simManager.registerAndRun(energyMap, simGUI.getUtilityPanelContext1());//indicate where do you want this to show u[ on the GUI

    //add GroupInteraction capability- if you may want to be able to select a group of nodes
        GroupSelectionTool gst=new GroupSelectionTool(myNode);
        simManager.registerAndRun(gst, simGUI.getSensorsPanelContext());
        myNode[0].getNodeGUI().setGroupSelectionTool(gst);//internals

    //starts the core(GUI) engine
        simManager.getProxy().run();
            System.out.println("Simulation Started..");

        return field;
    }
    
    
    public static Cluster[] estimateClusterArea(Node[] myNode) {
    //menentukan jarak node ke cluster head
        double jarak[];
        jarak = new double[myNode.length];
        tempLocX=new double[myNode.length];
        tempLocY=new double[myNode.length];
        double R=89; //nilai coverage area
        double Rcx = R;
        double Rcy = 0.87*R;
        int r,c;
        r=(int)Math.round(fieldLength/(2*Rcy)+1); 
        c=(int)Math.round(fieldLength/(2*Rcx)+1);
        jmlCluster=r*c;
//            System.out.println("Jumlah cluster = "+jmlCluster);
    //Menentukan Titik Tengah cluster
        double[][][]M;
        M=new double[r][c][2];
        double x=0.5;
        double y=0;
        double setX, setY;
        double otherY=1;
            for(int i=0; i<c; i++){
                for(int j=0; j<r; j++){
                    if(i%2==0){
                        setX=x*Rcx;
                        setY=y*Rcy;
                        M[j][i][0]=setX;
                        M[j][i][1]=setY;
                        y+=2;
                    }else{
                        setX=x*Rcx;
                        M[j][i][0]=setX;
                        M[j][i][1]=otherY*Rcy;
                        otherY+=2;
                    }
                  }
                y=0;
                otherY=1;
                x+=1.5;
                }
        //prepring variabel of cluster
            Cluster[] cluster=new Cluster[jmlCluster];
            int idCluster=0;
            for(int i=0; i<c; i++){
                for(int j=0; j<r; j++){
                cluster[idCluster]=new Cluster();
                cluster[idCluster].clusterID=idCluster;
                cluster[idCluster].x=M[j][i][0];
                cluster[idCluster].y=M[j][i][1];
                idCluster++;
                }
            }
        return cluster;
    }//estimate cluster
    
/**Configures each Node representation and network Stack
 * @param int id --> a numerical value to represent the id of a node. will correspond to the ip address representation
 * @param Field --> the field properties
 * @param Placement --> information regarding positions length of field
 * @param Mapper --> network stack mapper
 * @param RadioInfo.RadioInfoShared --> configuration of radio
 * @param plIn --> Property of the PacketLoss for incoming data packet
 * @param plOut --> Property of the PacketLoss for outgoing data packet
 * @param hostPanelContext --> the context of the panel this node will be drawn
 * @param fieldContext --> teh context of the actual field this node is in)for GPS)
 * @param simControl --> handle the simulator manager
 * @param Battery --> indicate the battery that will power this particular node
 * @param StatsCollector --> the statistical collector tool
 */
   
//modif - ww
    public static Node createNodeCSGP(int id,
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
                                  SequenceGenerator sequenceNumber,Node[] NodeList)
    {
    //create entities (gives a physical location)
        Location nextLocation=place.getNextLocation();
    
    //create an individual battery, since no two node can be powered by the same battery. the spacs of battery are the same though
        Battery individualBattery=new IdealBattery(battery.getCapacity_mJ(), battery.getVoltage());
        
    //set the battery and energy consumption profile
        EnergyConsumptionModel energyConsumptionModel = new EnergyConsumptionModelImpl(eCostParam, individualBattery);
        energyConsumptionModel.setID(id);
        
    //create the energy management unit
        EnergyManagement energyManagementUnit= new EnergyManagementImpl(energyConsumptionModel, individualBattery);
    
    //create  the node and nodeGUI interface for this node
        Node node=new Node(id, energyManagementUnit,hostPanelContext, fieldContext, new ColorProfileGeneric(), simControl);
        node.enableRelocation(field); //if you want to be able to relocate by mouse the node in the field at run time
        
    /**put a GPS(must to) to obtain the location information(for this assignment, for gaphical purposes only
     * Now, really, this is not a GPS per-se, just a "logical" way of obtaining location information from the simulator
     */
        GPS gps= new GPSimpl(new Location2D((int)nextLocation.getX(),(int)nextLocation.getY()));
        gps.configure(new LocationContext(fieldContext));
        node.setGPS(gps);
    
    /**configuring the ISO layer-more or less self-explanatory
    * APP Layer Configuration
    */
        CSGPAP_App app=new CSGPAP_App(node, Constants.NET_PROTOCOL_INDEX_1, stats, sequenceNumber);
        if(app.topologyGUI==null)
           app.topologyGUI=topologyGUI;
    //NET layer configuration - this is where the node get its "ip" address
        NetIp net = new NetIp(new NetAddress(id), protMap, plIn, plOut);
    
    //Measuring cluster ID
        double x=node.getLocation2D().getX();
        double y=node.getLocation2D().getY();
        int clusterId=0;
        double distToCluster;
        double minToCluster=10000; //set initial maximum value
        for(int a=0; a<formedCluster.length; a++){
            distToCluster=Math.sqrt((x-formedCluster[a].x)*(x-formedCluster[a].x)+(y-formedCluster[a].y)*(y-formedCluster[a].y));
            if(distToCluster<minToCluster){
                clusterId=a;
                minToCluster=distToCluster;
                node.clusterId=clusterId;
                node.distToClusterCenter=distToCluster;
            }
        }
    //Routing Protocol Configuration
        HeartbeatProtocol heartbeatProtocol = new HeartbeatProtocol(net.getAddress(), node, hostPanelContext, 30*Constants.MINUTE);
       // CSGPAP_Routing chRouting=new CSGPAP_Routing(node);

        //    CSGPAP_Routing csgpAP=new CSGPAP_Routing(node);// jika csgpap maka jalankan ini
            
            for (int i = 0; i < id; i++) {
                if(node.neighboursList.contains(NodeList[i].getIP()))
                node.neighboursList.get(NodeList[i].getIP()).battery = NodeList[i].getEnergyManagement().getBattery().getPercentageEnergyLevel();
            }
            csgpaproutingmodif csgpAP=new csgpaproutingmodif(node,NodeList, stats, sequenceNumber);
            
            if(csgpaproutingmodif.topologyGUI == null)
                    csgpaproutingmodif.topologyGUI = topologyGUI;
            
            node.setIP(net.getAddress());
        
            //MAC layer configuration 
            Mac802_15_4Impl mac=new Mac802_15_4Impl(new MacAddress(id),radioInfoShared, node.getEnergyManagement(), node);
        
            //PHY Layer Configuration
            Phy802_15_4Impl phy=new Phy802_15_4Impl(id, radioInfoShared, energyManagementUnit, node, 0*Constants.SECOND);
        
            //Radio Layer Configuration
            field.addRadio(phy.getRadioInfo(), phy.getProxy(), nextLocation);
            field.startMobility(phy.getRadioInfo().getUnique().getID());
        
            //hooking up the ISO Layers, APP <--routing hookup
            csgpAP.setAppInterface(app.getAppProxy());
        
            //APP--> NET hookup
            app.setNetEntity(net.getProxy());
        
            //net <-> routing hookup
            heartbeatProtocol.setNetEntity(net.getProxy());
            csgpAP.setNetEntity(net.getProxy());
        
            net.setProtocolHandler(Constants.NET_PROTOCOL_INDEX_1, csgpAP.getProxy());
            net.setProtocolHandler(Constants.NET_PROTOCOL_HEARTBEAT, heartbeatProtocol.getProxy());
        
            //net-MAC-phy hookup
            byte intID=net.addInterface(mac.getProxy());
            mac.setNetEntity(net.getProxy(), intID);
            mac.setPhyEntity(phy.getProxy());
        
            //PHY-Radio hookup
            phy.setFieldEntity(field.getProxy());
            phy.setMacEntity(mac.getProxy());
            
        
/* Here we actually start this nodes aplication layer execution. it is important to observe
        that we dont actually call the apps run() method directly, but through its proxy, which alloes JiST engine to actually decide when this call will
        be actually made(based on the simulation time)*/
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
    //create entities (gives a physical location)
        Location nextLocation=place.getNextLocation();
    
    //create an individual battery, since no two node can be powered by the same battery. the spacs of battery are the same though
        Battery individualBattery=new IdealBattery(battery.getCapacity_mJ(), battery.getVoltage());
        
    //set the battery and energy consumption profile
        EnergyConsumptionModel energyConsumptionModel = new EnergyConsumptionModelImpl(eCostParam, individualBattery);
        energyConsumptionModel.setID(id);
        
    //create the energy management unit
        EnergyManagement energyManagementUnit= new EnergyManagementImpl(energyConsumptionModel, individualBattery);
    
    //create  the node and nodeGUI interface for this node
        Node node=new Node(id, energyManagementUnit,hostPanelContext, fieldContext, new ColorProfileGeneric(), simControl);
        node.enableRelocation(field); //if you want to be able to relocate by mouse the node in the field at run time
        
    /**put a GPS(must to) to obtain the location information(for this assignment, for gaphical purposes only
     * Now, really, this is not a GPS per-se, just a "logical" way of obtaining location information from the simulator
     */
        GPS gps= new GPSimpl(new Location2D((int)nextLocation.getX(),(int)nextLocation.getY()));
        gps.configure(new LocationContext(fieldContext));
        node.setGPS(gps);
    
    /**configuring the ISO layer-more or less self-explanatory
    * APP Layer Configuration
    */
        CSGPAP_App app=new CSGPAP_App(node, Constants.NET_PROTOCOL_INDEX_1, stats, sequenceNumber);
        if(app.topologyGUI==null)
           app.topologyGUI=topologyGUI;
    //NET layer configuration - this is where the node get its "ip" address
        NetIp net = new NetIp(new NetAddress(id), protMap, plIn, plOut);
    
//    Measuring cluster ID
//        double x=node.getLocation2D().getX();
//        double y=node.getLocation2D().getY();
//        int clusterId=0;
//        double distToCluster;
//        double minToCluster=10000; //set initial maximum value
//        for(int a=0; a<formedCluster.length; a++){
//            distToCluster=Math.sqrt((x-formedCluster[a].x)*(x-formedCluster[a].x)+(y-formedCluster[a].y)*(y-formedCluster[a].y));
//            if(distToCluster<minToCluster){
//                clusterId=a;
//                minToCluster=distToCluster;
//                node.clusterId=clusterId;
//                node.distToClusterCenter=distToCluster;
//            }
//        }
    //Routing Protocol Configuration
        HeartbeatProtocol heartbeatProtocol = new HeartbeatProtocol(net.getAddress(), node, hostPanelContext, 30*Constants.MINUTE);
        Sgp sgp=new Sgp(node);

        node.setIP(net.getAddress());
        
    //MAC layer configuration 
        Mac802_15_4Impl mac=new Mac802_15_4Impl(new MacAddress(id),radioInfoShared, node.getEnergyManagement(), node);
        
    //PHY Layer Configuration
        Phy802_15_4Impl phy=new Phy802_15_4Impl(id, radioInfoShared, energyManagementUnit, node, 0*Constants.SECOND);
        
    //Radio Layer Configuration
        field.addRadio(phy.getRadioInfo(), phy.getProxy(), nextLocation);
        field.startMobility(phy.getRadioInfo().getUnique().getID());
        
    //hooking up the ISO Layers, APP <--routing hookup
        sgp.setAppInterface(app.getAppProxy());
        
    //APP--> NET hookup
        app.setNetEntity(net.getProxy());
        
    //net <-> routing hookup
        heartbeatProtocol.setNetEntity(net.getProxy());
        sgp.setNetEntity(net.getProxy());
        
        net.setProtocolHandler(Constants.NET_PROTOCOL_INDEX_1, sgp.getProxy());
        net.setProtocolHandler(Constants.NET_PROTOCOL_HEARTBEAT, heartbeatProtocol.getProxy());
        
    //net-MAC-phy hookup
        byte intID=net.addInterface(mac.getProxy());
        mac.setNetEntity(net.getProxy(), intID);
        mac.setPhyEntity(phy.getProxy());
        
    //PHY-Radio hookup
        phy.setFieldEntity(field.getProxy());
        phy.setMacEntity(mac.getProxy());
        
/* Here we actually start this nodes aplication layer execution. it is important to observe
        that we dont actually call the apps run() method directly, but through its proxy, which alloes JiST engine to actually decide when this call will
        be actually made(based on the simulation time)*/
     app.getAppProxy().run(null);
     return node;
    }     
}