package sidnet.stack.users.csgp.app;

import sidnet.core.query.Query;
import jist.swans.misc.Message;

public class MessageQuery implements Message {

	private final Query query;
	private String travelList="";
        private int i=0;
                
	public MessageQuery(Query query) {
		this.query = query;
	}
        
        public int hitung(){
            this.i+=1;
            return i;
        }
        
         public void setTravelListSink(String nodeId){
            travelList =nodeId+",";
        }
        
        public void setTravelList(String x){
            travelList = travelList+","+x;
        }
        
        public String getTravelList(){
            return this.travelList;
        }
	
	public Query getQuery() {
		return query;
	}
	
	public void getBytes(byte[] msg, int offset) {
		throw new RuntimeException("not implemented");
	}

	public int getSize() {
		return query.getAsMessageSize();
	}

}
