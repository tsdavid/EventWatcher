package com.dk.platform.eventWathcer.process.conf;

public enum EmsMsgPropertyConf {

    // TODO External Config
    DEFAULT("message_bytes", "msg_id", "target_dest_name", "event_action", "conn_hostname");
    
    private String Payload_Pro;
    
    private String MessageID_Pro;
    
    private String Destination_Pro;
    
    private String EventType_Pro;
    
    private String HostName_Pro;
    
    private EmsMsgPropertyConf(String payload_pro, String messageID_Pro, String destination_Pro, String eventType_pro, String hostName_Pro) {
	
	this.Payload_Pro = payload_pro;
	
	this.MessageID_Pro = messageID_Pro;
	
	this.Destination_Pro = destination_Pro;
	
	this.EventType_Pro = eventType_pro;
	
	this.HostName_Pro = hostName_Pro;
    }
    
    
    public String getPayload_Pro() {
	return this.Payload_Pro;
    }
    
    public String getMessageID_Pro() {
	return this.MessageID_Pro;
    }
    
    public String getDestination_Pro() {
	return this.Destination_Pro;
    }
    
    public String getEventType_Pro() {
	return this.EventType_Pro;
    }
    
    public String getHostName_Pro() {
	return this.HostName_Pro;
    }
    
    

}
