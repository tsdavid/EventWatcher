package platform.eventWathcer.util.influx;

import com.dk.emslog.db.mapCentral.MapCentral;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDB.LogLevel;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Pong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for InfluxDb Connection, 
 * Apply Singleton Design Pattern
 * 
 * reference : https://www.baeldung.com/java-influxdb
 * 
 * @author tspsc
 *
 */
public class InfluxDbConn {
    
    private static String CLASS_NAME = MapCentral.class.getSimpleName();
    private static Logger logger = LoggerFactory.getLogger(CLASS_NAME);
    
    /**
     * InfluxDB Class
     * 
     * Features of this Class
     *  - Make Connection with Database., if No database, then create
     *   - Check Retention Policy,  Make Retention Policy
     *    - Log Level
     *   - Initialize Database
     */
    private InfluxDB influxDB = null;
    
    private InfluxDbConf conf = InfluxDbConf.LOCAL;
    
    private InfluxDbConn() {
	
	String METHOD_NAME = Thread.currentThread().getStackTrace()[1].getMethodName();
	logger.info("[{}][{}] ", CLASS_NAME, METHOD_NAME);
	
	// Make Connection With INFLUX Database
	try {
	    
	    this.connectInfluxDB(conf.getDB_Url(), conf.getDB_UserName(), conf.getDB_Password(),conf.getDB_Name(), conf.getDB_RetentionName(), conf.getDB_LogLevel());
	    
	} catch (Exception e) {
	    
	    logger.error("[{}][{}] Error., {}\n{}", CLASS_NAME, METHOD_NAME, e.getMessage(), e.getStackTrace());
	}
	
	
    }
    
    
    /**
     * Singleton Design Pattern. Using Helper
     * @author tspsc
     *
     */
    private static class SingletonHelper {
	
	private static final InfluxDbConn INSTANCE = new InfluxDbConn();
    }
    
    
    /**
     * Get InfluxDbConn Instance
     * @return
     */
    public static InfluxDbConn getInstance() {
	
	String METHOD_NAME = Thread.currentThread().getStackTrace()[1].getMethodName();
	logger.info("[{}][{}] ", CLASS_NAME, METHOD_NAME);
	
	return SingletonHelper.INSTANCE;
    }
    
    
    /**
     * 
     * @param databaseURL
     * @param userName
     * @param password
     * @return
     * @throws Exception 
     */
    private boolean connectInfluxDB(String databaseURL, String userName, String password, String databaseName, String retentionName, LogLevel loglevel) throws Exception {
	
	String METHOD_NAME = Thread.currentThread().getStackTrace()[1].getMethodName();
	logger.info("[{}][{}] ", CLASS_NAME, METHOD_NAME);
	
	try {
	 // Connection with DB URL  if user name is null
		this.influxDB = (userName == null) ? InfluxDBFactory.connect(conf.getDB_Url()) : InfluxDBFactory.connect(conf.getDB_Url(), conf.getDB_UserName(), conf.getDB_Password());
		
		
		// set database for using Insert Point
		this.influxDB.setDatabase(databaseName);
		
		// set Retention Policy  which is configured in configuration file.
		this.influxDB.setRetentionPolicy(retentionName);
		
		// Setting a Logging Level
		this.influxDB.setLogLevel(loglevel);
		
	}catch (Exception e) {
	    
	    logger.error("[{}][{}]  While Connecting and set-up INFLUX DB. error is ouccured.  "
	    	+ "Print All Parameters :  DataBase Url : {}.,  Database UserName : {}., Database Password : {}.,  Database Name : {}.,  Database Retention Name : {}.,  Database Log Levl : {}"
	    	+ "DB  ERROR : {}\n{}. ", 
		    CLASS_NAME, METHOD_NAME, e.getMessage(), e.getStackTrace());
	    return false;
	}
	
	
	// Verifying the Connection
	Pong resp = this.influxDB.ping();
	if(resp.getVersion().equalsIgnoreCase("unknown")) {
	    
	    logger.error("[{}][{}]  INFLUX DB Ping Response.  Error Pinging Server. ", CLASS_NAME, METHOD_NAME);
	    throw new Exception(" INFLUX DB  Ping Response.,  Get Error Pinging From Server.  Check DB Connection Status");
	}
	
	return false;
    }
    
    
    
    public InfluxDB getInfluxDB() {
	return this.influxDB;
    }
    
    
    
}
