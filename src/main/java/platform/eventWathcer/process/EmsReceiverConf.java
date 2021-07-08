package platform.eventWathcer.process;


// TODO Make it Json Conf FIle.
public enum EmsReceiverConf {
    
    LOCAL("tcp://localhost:7222", "admin", "", "$sys.monitor.Q.*.M16.MES.EP.*.>", false),

    private String serverUrl = null;
    
    private String userName = null;
    
    private String passWord = null;
    
    private String destiNation = null;
    
    private boolean isQueue = false;
    
    EmsReceiverConf(String serverUrl, String userName, String passWord, String destiNantion, boolean isQueue) {
	
	this.serverUrl = serverUrl;
	this.passWord = passWord;
	this.destiNation = destiNantion;
	this.isQueue = isQueue;
	
    }
    
    public String getServerUrl() {
	return this.serverUrl;
    }
    
    public String getuserName() { 
	return this.userName;
    }
    
    public String getpassWord() {
	return this.passWord;
    }
    
    public String getdestination() {
	return this.destiNation;
    }
    
    public boolean getisQueue() {
	return this.isQueue;
    }
}
