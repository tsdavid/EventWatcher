package platform.eventWathcer.util.influx;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDB.LogLevel;

public enum InfluxDbConf {
    
    LOCAL("http://localhost:8086", "emslog", null, null, "one_hour", InfluxDB.LogLevel.BASIC),
    DEVREAL("http://10.25.205.239", "emslog", null, null , "test", InfluxDB.LogLevel.BASIC);
    
    private String DB_URL = null;
    
    private String DB_NAME = null;
    
    private String DB_UserName = null;
    
    private String DB_Password = null;
    
    private String DB_RetentionName = null;
    
    private LogLevel DB_LogLevel = null;
    
    
    InfluxDbConf(String db_url,  String db_name, String db_username, String db_password, String retentionPolicy, LogLevel logLevel){
	
	this.DB_URL = db_url;
		
	this.DB_NAME = db_name;
	
	this.DB_UserName = db_username;
	
	this.DB_Password = db_password;
	
	this.DB_RetentionName = retentionPolicy;
	
	this.DB_LogLevel = logLevel;
	
	
    }
    
    public String getDB_Url() {
	return this.DB_URL;
    }
    
    public String getDB_Name() {
	return this.DB_NAME;
    }
    
    
    public String getDB_UserName() {
	return this.DB_UserName;
    }
    
    public String getDB_Password() {
	return this.DB_Password;
    }
    
    public String getDB_RetentionName() {
	return this.DB_RetentionName;
    }

    public LogLevel getDB_LogLevel() {
	return this.DB_LogLevel;
    }
}
