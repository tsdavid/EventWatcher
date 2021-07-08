package platform.eventWathcer.util;

import com.dk.emslog.db.influx.InfluxDbConn;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class InfluxDbUtil {

    private static String CLASS_NAME = MapCentral.class.getSimpleName();
    private static Logger logger = LoggerFactory.getLogger(CLASS_NAME);
    
    private InfluxDbConn influxDbConn = InfluxDbConn.getInstance();
    private InfluxDB influxDB = this.influxDbConn.getInfluxDB();
    
    /**
     * Connection Instance
     */
    private InfluxDbConn con_instance = null;
    
    public InfluxDbUtil() throws Exception {
	
	String METHOD_NAME = Thread.currentThread().getStackTrace()[1].getMethodName();
	logger.info("[{}][{}] ", CLASS_NAME, METHOD_NAME);
	
	try {
	    
	    this.con_instance = InfluxDbConn.getInstance();
	    
	}catch (Exception e) {
	    
	    logger.error("[{}][{}] Error.,  {}\n{}", CLASS_NAME, METHOD_NAME, e.getMessage(), e.getStackTrace());
	    throw new Exception("ERROR.  While get InfluxDB Connection Instance.");
	}
	
    }
    
    
    /**
     * Insert Point
     * @param tagName
     * @param tagValue
     * @param keyValue
     */
    public void InsertMsgCountPoint(String tagName, String tagValue, double keyValue) {
	
	String METHOD_NAME = Thread.currentThread().getStackTrace()[1].getMethodName();
	logger.info("[{}][{}] ", CLASS_NAME, METHOD_NAME);
	
	logger.info("[{}][{}] onGoingMsg Point Type : {}., TagValue : {}., keyValue : {}., ",
		CLASS_NAME, METHOD_NAME, tagValue, keyValue);
	
	// Generate Point
	Point point = Point.measurement("onGoingMsg")
                        		.tag(tagName, tagValue)
                        		.addField("value", keyValue)
                        		.time(Instant.now().toEpochMilli(), TimeUnit.MICROSECONDS)
                        		.build();
	this.influxDB.write(point);
    }
    
    
    public boolean InsertMessagePoint(String measurements, String messageID, String extn, 
	    							String cmd, String destination, String queueType, String hostName, 
	    							double sr, double as) {
	
	String METHOD_NAME = Thread.currentThread().getStackTrace()[1].getMethodName();
	
	Point point = Point.measurement(measurements)
					.tag("EXTN", extn).tag("CMD", cmd)
					.tag("Destination", destination)
					.tag("HostName", hostName)
					.addField("value", messageID)
					.addField("SminusR", sr)
					.addField("AminusS", as)
					.time(Instant.now().toEpochMilli(), TimeUnit.MILLISECONDS)
					.build();
	this.influxDB.write(point);
	
	return true;
	
    }
    
    public <T> Double getQueryResult(String queryCmd, String database, Class<T> clazz) {
	
	String METHOD_NAME = "getQueryResult";
	logger.info("[{}][{}]  Running Query.", CLASS_NAME, METHOD_NAME);
	
	QueryResult result = null;
	try {
	    result = this.influxDB.query(new Query(queryCmd, database));
	    logger.debug("[{}][{}] Result Size : {}.", CLASS_NAME, METHOD_NAME, result.getResults().size());
	    
	    if(result != null) {
		return (double) result.getResults().iterator().next().getSeries().iterator().next().getValues().get(0).get(1);
	    }
	    
	}catch (Exception e) {
	    
	    logger.error("[{}][{}]  Error.,  {}\n{}.", CLASS_NAME, METHOD_NAME, e.getMessage(), e.getStackTrace());
	}
	
	return 0.0;
    }
    
}
