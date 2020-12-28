/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sidnet.stack.users.csgp_adaptivepath.routing;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import jist.swans.misc.Message;
import jist.swans.net.NetAddress;
import sidnet.core.query.Query;
import sidnet.core.misc.Location2D;
import sidnet.core.misc.Region;
/**
 *
 * @author Maira_Fakhri
 */
public class CSGPWrapperMessage implements Message{
    private Message payload;
    
//if ip source is now known, you can provide target location or target area (surrounded by a polygon/region)
    private Location2D targetLocation;
    private Region targetRegion;
    
//for tracking
    private long s_seq; //sequence number to filter duplicates
    private long timeSent;
    public String messageID;
    public int jumlahHop; //zhy
    public LinkedList<NetAddress> BlaklistNode = new LinkedList<NetAddress>();
    public List<Double> idNode=new ArrayList();
    public boolean pasCH=false;
    
    public void setMessageId(String newId){
        this.messageID=newId;
    }

    public boolean setStatus(){
        this.pasCH=true;
        return this.pasCH;
    }
    
    public boolean getStatus(){
        return this.pasCH;
    }
    
    public CSGPWrapperMessage(){
        payload=null;
        targetLocation=null;
        s_seq=-1;
        timeSent=0;
    }
    public CSGPWrapperMessage(Message payload, Location2D targetLocation, long s_seq, long timeSent){
        this.payload=payload;
        this.targetLocation=targetLocation;
        this.s_seq=s_seq;
        this.timeSent=timeSent;
    }
    
    public CSGPWrapperMessage(Message payload, Region targetRegion,
    						 long s_seq, long timeSent) {
    	this.payload  	    = payload;
    	this.targetLocation = null;
    	this.targetRegion   = targetRegion;
    	this.s_seq     		= s_seq;
    	this.timeSent  		= timeSent;
        }
        
    public CSGPWrapperMessage(Message payload, Region targetRegion, long s_seq, long timeSent, String messageId){
        this.payload=payload;
        this.targetLocation=null;
        this.targetRegion=targetRegion;
        this.s_seq=s_seq;
        this.timeSent=timeSent;
        this.messageID=messageId;
    }
        
    public Message getPayload(){
        return payload;
    }
    public Location2D getTargetLocation(){
        return targetLocation;
    }
    public Region getTargetRegion(){
        return targetRegion;
    }
    public void setTargetLocation(Location2D targetLocation){
        this.targetLocation=targetLocation;
    }
    
 //@inheritdoc 
    public int getSize(){
        int size=0;
        if(payload!=null)
            size+=payload.getSize();
        if(targetRegion!=null)
            size+=targetRegion.getAsMessageSize();
        if(targetLocation!=null)
            size+=4;
        size+=4;  //long s_seq
        size+=4;  //long timeSent
        return size;
    }
  //@inheritdoc 
    public void getBytes(byte[]b, int offset){
        throw new RuntimeException("not implemented");
    }
    
    public CSGPWrapperMessage copy(){
        CSGPWrapperMessage newMsg=new CSGPWrapperMessage();
        newMsg.payload=payload;
        newMsg.targetLocation=targetLocation;
        newMsg.targetRegion=targetRegion;
        newMsg.s_seq=s_seq;
        newMsg.timeSent=timeSent;
        return newMsg;
    }
}
