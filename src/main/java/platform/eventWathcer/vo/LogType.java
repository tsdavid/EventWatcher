package platform.eventWathcer.vo;

public enum LogType {

    RECV("receive"),
    SEND("send"),
    ACK("acknowledge");
    
    private String type = null;
    
    LogType(String type){
	this.type = type;
    }
    
    public String getType() {
	return this.type;
    }
}
