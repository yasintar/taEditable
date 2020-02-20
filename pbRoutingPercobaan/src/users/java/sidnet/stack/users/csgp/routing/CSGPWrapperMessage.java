/*
 * NZMessage.java
 *
 * Created on November 24, 2007, 1:16 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package sidnet.stack.users.csgp.routing;

import jist.swans.misc.Message;

import sidnet.core.query.Query;
import sidnet.core.misc.Location2D;
import sidnet.core.misc.Region;


/**
 *
 * @author nikolay
 */
public class CSGPWrapperMessage implements Message {
    private Message payload;
    
    // if IP of the source is now known, you can provide target location
    // or a target area (surrounded by a polygon/region)
    private Location2D targetLocation;
    private Region 	   targetRegion;
    
    // for tracking
    private long s_seq;      // sequence number to filter duplicates
    private long timeSent;
    public String messageID;
    public boolean passCH=false;
    public boolean passHole=false;
    
    public boolean setStatus(){
        this.passCH=true;
        return this.passCH;
    }
    public boolean getStatus(){
        return this.passCH;
    }

    public void setPassHole( boolean status){
        this.passHole=status;
        //return this.passCH;
    }
    public boolean getPassHole(){
        return this.passHole;
    }

    public CSGPWrapperMessage() {
    	payload 	   = null;
    	targetLocation = null;
    	s_seq          = -1;
    	timeSent       = 0;
    }
    
    public CSGPWrapperMessage(Message payload, Location2D targetLocation,
    						 long s_seq, long timeSent) {
    	this.payload  	    = payload;
    	this.targetLocation = targetLocation;        
    	this.s_seq     		= s_seq;
        this.timeSent  		= timeSent;
    }
    
    public CSGPWrapperMessage(Message payload, Region targetRegion,
    						 long s_seq, long timeSent) {
    	this.payload  	    = payload;
    	this.targetLocation = null;
    	this.targetRegion   = targetRegion;
    	this.s_seq     		= s_seq;
    	this.timeSent  		= timeSent;
}
     public CSGPWrapperMessage(Message payload, Region targetRegion,
    						 long s_seq, long timeSent,String messageId) {
    	this.payload  	    = payload;
    	this.targetLocation = null;
    	this.targetRegion   = targetRegion;
    	this.s_seq     		= s_seq;
    	this.timeSent  		= timeSent;
        this.messageID= messageId;
    }
    
    public Message    getPayload()		 { return payload;       }
    public Location2D getTargetLocation(){ return targetLocation;}
    public Region     getTargetRegion()  { return targetRegion;  } 
    public void setTargetLocation(Location2D targetLocation) { 
    	this.targetLocation = targetLocation;
    };
         
    /** {@inheritDoc} */
    public int getSize() { 
        int size = 0;
        if (payload != null)
            size += payload.getSize();
        if (targetRegion != null)
            size += targetRegion.getAsMessageSize();
        if (targetLocation != null)
            size += 4;
        size += 4;  // long s_seq;
        size += 4;  // long timeSent

        return size;
    }
    
    /** {@inheritDoc} */
    public void getBytes(byte[] b, int offset) {
        throw new RuntimeException("not implemented");
    }
   
    public CSGPWrapperMessage copy () {
    	CSGPWrapperMessage newMsg = new CSGPWrapperMessage();
        newMsg.payload = payload;        
        newMsg.targetLocation = targetLocation;
        newMsg.targetRegion = targetRegion;
        newMsg.s_seq = s_seq;      
        newMsg.timeSent = timeSent;
        
        return newMsg;
    }
}
