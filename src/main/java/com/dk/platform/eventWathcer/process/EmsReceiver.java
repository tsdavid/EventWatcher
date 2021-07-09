package com.dk.platform.eventWathcer.process;

import com.dk.platform.eventWathcer.process.conf.EmsMsgPropertyConf;
import com.dk.platform.eventWathcer.util.InfluxDbUtil;
import com.dk.platform.eventWathcer.util.MapCentral;
import com.dk.platform.eventWathcer.util.influx.InfluxDbConf;
import com.dk.platform.eventWathcer.util.influx.schema.query.Means;
import com.dk.platform.eventWathcer.vo.EventVO;
import com.dk.platform.eventWathcer.process.conf.LogType;
import com.tibco.tibjms.TibjmsConnectionFactory;
import com.tibco.tibjms.TibjmsMapMessage;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.time.LocalTime;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class EmsReceiver {
    
	private static String CLASS_NAME = EmsReceiver.class.getSimpleName();
	private static Logger logger = LoggerFactory.getLogger(CLASS_NAME);
	
	/**
	 * MapCentral Instance
	 * Resources from MapCentral 
	 */
	private final MapCentral mapCentral = MapCentral.getInstance();
	private final ConcurrentHashMap<String, EventVO> Memoery_Store = this.mapCentral.getMemoryStore();
	private final ConcurrentHashMap<String, Integer> KeyCount_Store = this.mapCentral.getKeyCountMap();
	private final Queue<String> TaskQueue = this.mapCentral.getTaskQueue();
	private final Queue<String> WatchQueue = this.mapCentral.getWatchQueue();		// ==> Surveilance Queue
	
	
	private InfluxDbUtil influxDbUtil = null;
	
	private TibjmsConnectionFactory factory = null;
	
	private Connection connection = null;
	
	private Session session = null;
	
	private MessageConsumer msgConsumer = null;
	
	private int ackMode = Session.AUTO_ACKNOWLEDGE;
	
	private Double standard_SR = null;
	
	private Double standard_AS = null;
	
	private int DELAY_PERCENT = 0;
	
	private Destination Destination = null;
	
	private EmsMsgPropertyConf proceeConf  = null;
	
	final String RECV_TYPE = LogType.RECV.getType();
	
	final String SEND_TYPE = LogType.SEND.getType();
	
	final String ACK_TYPE = LogType.ACK.getType();
		
	boolean stopRun = false;
	
	
	/**
	 * 
	 * @param serverUrl
	 * @param userName
	 * @param passWord
	 * @param destiNation
	 * @param isQueue
	 */
	public EmsReceiver(String serverUrl, String userName, String passWord, String destiNation, boolean isQueue, int delay_percent) {
	    
	    String METHOD_NAME = Thread.currentThread().getStackTrace()[1].getMethodName();
	    logger.info("[{}][{}] ", CLASS_NAME, METHOD_NAME);
	    
	    
	    /**
	     * Initialize MapCentral Resources
	     */
	    try {
		
		// Initialize KeyCount Store.
		this.KeyCount_Store.put("sq", 0);
		this.KeyCount_Store.put("dq", 0);
		
	    }catch (Exception e) {
		
		logger.error("[{}][{}] Error.,  {}\n{}", CLASS_NAME, METHOD_NAME, e.getMessage(), e.getStackTrace());
		throw new RuntimeException();
	    }
	    
	    
	    /**
	     * Initialize InfluxDB
	     */
	    try {
		this.influxDbUtil = new InfluxDbUtil();
		
	    } catch (Exception e) {
		logger.error("[{}][{}] Error.,  {}\n{}", CLASS_NAME, METHOD_NAME, e.getMessage(), e.getStackTrace());
		throw new RuntimeException();
	    }
	    
	    
	    /** Initialize EMS Connection
	     */
	    try {
		this.proceeConf = EmsMsgPropertyConf.DEFAULT;
		this.prepareEmsConnection(serverUrl, userName, passWord);
		
	    }catch (Exception e) {
		
		logger.error("[{}][{}] Error.,  {}\n{}", CLASS_NAME, METHOD_NAME, e.getMessage(), e.getStackTrace());
		throw new RuntimeException();
	    }
	    
	    
	    /**
	     * Initialize Delay-Decision.
	     */
	    try {
		this.DELAY_PERCENT =  delay_percent;
		logger.info("[{}][{}]  Initialize Delay-Decision Complete.", CLASS_NAME, "EmsReceiver");
		
	    }catch (Exception e) {
		
		logger.error("[{}][{}]  Error Occur While Initialize Delay-Decision.  Error : {}\n{}." ,CLASS_NAME, METHOD_NAME, e.getMessage(), e.getStackTrace());
		throw new RuntimeException("Error Occur While Initialize Delay-Decision.");
	    }
	    
	    /**
	     * Invoke Query Thread.
	     */
	    try {
		
		this.invokeQueryStandardTimeThread();
		logger.info("[{}][{}]  Start invokeQueryStandardTimeThread Complete.", CLASS_NAME, "EmsReceiver");
		
	    }catch (Exception e) {
		
		logger.error("[{}][{}]  Error Occur While Start invokeQueryStandardTimeThread.  Error : {}\n{}." ,CLASS_NAME, METHOD_NAME, e.getMessage(), e.getStackTrace());
		throw new RuntimeException("Error Occur While Start invokeQueryStandardTimeThread.");
	    }
	    
	    
	    /**
	     * Start EmsReceiverRun
	     */
	    try {
		
		this.startRunning(destiNation, false);
		logger.info("[{}][{}]  Start EmsReceiverRun Complete.", CLASS_NAME, "EmsReceiver");
		
	    }catch (Exception e) {
		
		logger.error("[{}][{}]  Error Occur While Start EmsReceiverRun.  Error : {}\n{}." ,CLASS_NAME, METHOD_NAME, e.getMessage(), e.getStackTrace());
		throw new RuntimeException("Error Occur While Start EmsReceiverRun.");
	    }
	    
	}
	
	
	/**
	 * 
	 * @param serverUrl
	 * @param userName
	 * @param passWord
	 * @param destiNationName
	 * @throws Exception
	 */
	private void prepareEmsConnection(String serverUrl, String userName, String passWord) throws Exception {
	    
	    String METHOD_NAME = Thread.currentThread().getStackTrace()[1].getMethodName();
	    logger.info("[{}][{}]   Server Url : {.}, User Name : {}., PassWord : {}.Destination : {}.", CLASS_NAME, METHOD_NAME, serverUrl, userName, passWord);
	    
	    this.factory = new com.tibco.tibjms.TibjmsConnectionFactory(serverUrl);
	    
	    try {
		this.connection = this.factory.createConnection(userName, passWord);
		this.connection.start();
		
		
	    }catch (Exception e) {
		
		logger.error("[{}][{}] Error.,  {}\n{}", CLASS_NAME, METHOD_NAME, e.getMessage(), e.getStackTrace());
	    }
	}
	
	
	/**
	 * 
	 * @param destinationName
	 * @param isQueue
	 */
	public void startRunning(String destinationName, boolean isQueue) {
	    
	    String METHOD_NAME = Thread.currentThread().getStackTrace()[1].getMethodName();
	    logger.info("[{}][{}] Start Receving Message from {}. isQueue : {} ", CLASS_NAME, METHOD_NAME, destinationName, isQueue);
	    
	    try {
		this.session = this.connection.createSession(false , this.ackMode);
		
		this.Destination = (isQueue) ? this.session.createQueue(destinationName) : this.session.createTopic(destinationName);
		this.msgConsumer = this.session.createConsumer(this.Destination);
			
	    }catch (Exception e) {
		
		logger.error("[{}][{}] Error.,  {}\n{}", CLASS_NAME, METHOD_NAME, e.getMessage(), e.getStackTrace());
	    }
	    
	    
	    while(!this.stopRun) {
		
		try {
		    TibjmsMapMessage msg = (TibjmsMapMessage) msgConsumer.receive();
		    
		    msg.acknowledge();
		    
		    
		    
		    try {
			
			String eventType = msg.getStringProperty(proceeConf.getEventType_Pro());
			
			long timeStamp = msg.getJMSTimestamp();
			
			String messageID = msg.getStringProperty(proceeConf.getMessageID_Pro());
			
			String destName = msg.getStringProperty(proceeConf.getDestination_Pro());
			
			String queueType = (destName.endsWith("ALL")) ? "dq" : "sq";
			
			String hotName = msg.getStringProperty(proceeConf.getHostName_Pro());
			
			// TODO Store pay-load in MapVO in case of Missing Message Case.
			String payload = new String(msg.getBytes(proceeConf.getPayload_Pro()), "UTF-8");
			
			String etxn = getETXN(payload);
			
			String cmd = getCMD(payload);
			
			/****************************************************************************************/
			/****************************************************************************************/
			/********************************  Sort Message By Event Type.  *******************************/
			/****************************************************************************************/
			/****************************************************************************************/
			
			/**
			 * Case :  Receive Event Type.
			 * Condition : 1. The Key(Message ID) is not exist in Map. 		2.  The Event Type is RECV.
			 * Store Data into MapCentral.
			 * Task.
			 * 1. Generate New MapVO.  and Initialize it.
			 * 2. Count Current Message Count.
			 * 
			 */
			if(eventType.equals(this.RECV_TYPE) && Memoery_Store.containsKey(messageID)) {
			    
			    
			    // Task 1. Count Current Message Count.
			    try {
				
				logger.info("[{}][{}]  CurrentCountMsg  SQ : {},  DQ : {}.,", CLASS_NAME, METHOD_NAME, this.KeyCount_Store.get("sq"), this.KeyCount_Store.get("dq"));
				this.updateMsgCount(true, queueType);
				
			    }catch (Exception e) {
				
				logger.error("[{}][{}] Error While Task1.  Generate New MapVO.  and Initialize It.  Event Type : {}.,  Error : {}\n{}."
					, CLASS_NAME, METHOD_NAME, eventType, e.getMessage(), e.getStackTrace());
			    }
			    
			    
			    // Task 2.  Generate New MapVO., and Initialize it.
			    try {
				
				this.Memoery_Store.put(messageID, new EventVO(eventType, getETXN(payload), getCMD(payload), queueType, destinationName, hotName, timeStamp));
				logger.info("[{}][{}] Message ID : {}.,  Insert R logType.  Initialize MapVO.", CLASS_NAME, METHOD_NAME, messageID);
				
			    }catch (Exception e) {
				
				logger.error("[{}][{}]  Error While Task 2. Generate New MapVO.  Error : {}\n{}.", CLASS_NAME, METHOD_NAME, e.getMessage(), e.getStackTrace());
			    }
			    
			    
			}else {
			    
			    /**
			     * CASE : Send and ACK Event Type.
			     * Condition : 1. The Event Type is Not RECV Type.
			     * Update Data into MapCentral.
			     * Task.
			     * 1.  Get MapVO.
			     * 
			     */
			    EventVO mapVO = this.Memoery_Store.get(messageID);
			    
			    
			    /**
			     * CASE : Send Event Type.
			     * Condition :  1. The Event Type is SEND.
			     * Update Data info MapCentral.
			     * Task.
			     * 1.  Update MapVO.
			     * 2.  Check Delay-Decision.
			     */
			    if(eventType.equals(LogType.SEND.getType())) {

				//  Task 1. Update MapVO.  & Task 2. Delay-Decision.
				try {
				    
				    if(this.delayDecision(this.standard_SR, mapVO.updateS_data(timeStamp))) {
					mapVO.setSSR(this.standard_SR);
					
					// Task 2. Send Delay Message ID to watch Carefully.
					this.WatchQueue.offer(messageID);
					logger.warn("[{}][{}] Type : {}.  Message ID : {}., is DelayCase Message.  ::  SSR : {} -  SR : {}  = {}.  Watch Carefully. Sent to Watch Queue.",
						CLASS_NAME, METHOD_NAME, eventType,messageID, mapVO.getSSR(), mapVO.getSR(), mapVO.getSSR() - mapVO.getSR());
				    }
				    
				    logger.info("[{}][{}] Message ID : {}.,  Complete Insert  {} type. Update MapVO."
					    , CLASS_NAME, METHOD_NAME, eventType, messageID);
				    
				}catch (Exception e) {
				    
				    logger.error("[{}][{}] Error While Task  1. Update MapVO.  &  Task  2.  Delay-Decision.  Event Type : {}.,  Error : {}\n{}.",
					    CLASS_NAME, METHOD_NAME, eventType, e.getMessage(), e.getStackTrace());
				}
			
    			/**
    			 * CASE : ACK Event Type.
    			 * Condition : 1.  Event Type is ACK.
    			 * Update Data into MapCentral.
    			 * Task
    			 * 1.  Update MapVO.
    			 * 2.  Check Delay - Decision.
    			 * 3.  Decrease Current Message Count.
    			 * 4.  Send Key to Queue for notifying Remove.
    			 * 
    			 */
			    } else if(eventType.equals(LogType.ACK.getType())) {
				
				// Task 1. Update MapVO.  & Task 2. Delay-Decision.
				try {

				    if(this.delayDecision(this.standard_AS, mapVO.updateA_data(timeStamp))) {
					mapVO.setSAS(this.standard_AS);
					
					// Task 2. Send Delay Message ID to watch Carefully.
					this.WatchQueue.offer(messageID);
					logger.warn("[{}][{}] Type : {}.  Message ID : {}., is DelayCase Message.  ::  SAS : {} -  AS : {}  = {}.  Watch Carefully. Sent to Watch Queue.",
						CLASS_NAME, METHOD_NAME, eventType,messageID, mapVO.getSAS(), mapVO.getAS(), mapVO.getSAS() - mapVO.getAS());
				    }
				    
				    logger.info("[{}][{}] Message ID : {}., Complete Insert {} Type.  Update MapVO.", CLASS_NAME, METHOD_NAME, eventType, messageID);
				    
				}catch (Exception e) {
				    
				    logger.error("[{}][{}] Error While Task 1. Update MapVO.  &  Task 2 Delay-Decision.  Event type : {}., ERROR : {}\n{}.",
					    CLASS_NAME, METHOD_NAME, eventType, e.getMessage(),e.getStackTrace());
				}
				
				
				// Task 3. Decrease Current Message Count.
				try {
				    
				    this.TaskQueue.offer(messageID);
				    logger.info("[{}][{}] Message ID : {}.,  Send Task to Task Queue.,  Task Queue Size : {}.",
					    CLASS_NAME, METHOD_NAME, messageID, this.TaskQueue.size());
				    logger.debug("[{}][{}] Message ID : {}.,  Send Task to Task Queue : {}.",
					    CLASS_NAME, METHOD_NAME, messageID, this.TaskQueue.toString());
				    
				}catch (Exception e) {
				    
				    logger.error("[{}][{}]  Error While Task 4. Send Key to Queue for notifying Remove.  Event Type : {}., Error : {}\n{}.",
					    CLASS_NAME, METHOD_NAME, eventType, e.getMessage(), e.getStackTrace());
				}
			    }
			}
			
			logger.debug("[{}][{}]   MemoryStoreSize : {}.", CLASS_NAME, METHOD_NAME, this.Memoery_Store.size());
		    
		    
		}catch (JMSException e) {
		    if(this.stopRun == false) {
			logger.error("[{}][{}]  JMSException : {}\n{}", CLASS_NAME, METHOD_NAME, e.getMessage(), e.getStackTrace());
		    }
		    
		    try {
			Thread.sleep(1 * 1000);
		    }catch (InterruptedException e1) {
			logger.error("[{}][{}]  InterruptedException : {}\n{}", CLASS_NAME, METHOD_NAME, e1.getMessage(), e1.getStackTrace());
		    }

		}
		
		
	    }catch (Exception e) {
		if(this.stopRun == false) {
		    logger.error("[{}][{}]  Exception : {}\n{}", CLASS_NAME, METHOD_NAME, e.getMessage(), e.getStackTrace());
		}
		
		try {
		    Thread.sleep(1 * 1000);
		} catch (InterruptedException e1) {
		    logger.error("[{}][{}]  InterruptedException : {}\n{}", CLASS_NAME, METHOD_NAME, e1.getMessage(), e1.getStackTrace());
		}
	    }
	}
	}
	
	
	private String getETXN(String msg) {
	    
	
	    String val = null;
	    
	    int etxn_start = msg.indexOf("ETXN") + 4;   // + 4 for ETXN strings
	    
	    if(msg.indexOf("rvTC") == -1) {
		// if "rvTC" is not in bytes
		val = msg.substring(etxn_start, etxn_start + 27).trim();
	    }else {
		
		val = msg.substring(etxn_start, msg.indexOf("rvTC")).trim();
	    }
	    
	    return val;
	}
	
	
	private String getCMD(String msg) {
	    
	    String val = null;
	    int cmdStart = msg.indexOf("CMD") + 3;
	    
	    try {
		val = msg.substring(cmdStart, msg.indexOf("{")).trim();
	    }catch (Exception e) {
		
		val = msg.substring(cmdStart, msg.indexOf("Envelop") -3 ).trim();
	    }
	    
//	    if(val.lastIndexOf("{") != -1) {
//		val = val.substring(0, val.indexOf("{"));
//	    }
	    
	    // Ư������ ����
	    String match = "[^3xfe0-9a-zA-Z\\_\\s]";	   // ����, ���� , _ ����
	    val = val.replaceAll(match, "");
	    
	    return val;
	}
	
	private void updateMsgCount(boolean isIncrease, String type) {
	    
	    try {
		if(isIncrease) {
		    this.KeyCount_Store.put(type, this.KeyCount_Store.get(type) + 1);
		    logger.info("[{}][{}]  Increase : {}  Value in Map Key : {}.", CLASS_NAME, "updateMsgCount", isIncrease, type);
		    
		}else {
		    this.KeyCount_Store.put(type, this.KeyCount_Store.get(type) -1);
		    logger.info("[{}][{}]  Decrease : {}  Value in Map Key : {}.", CLASS_NAME, "updateMsgCount", !isIncrease, type);
		}
		
	    }catch (Exception e) {
		
		logger.error("[{}][{}] Error While Update Message Count in Map.  Error : {}\n{}", CLASS_NAME, "updateMsgCount", e.getMessage(), e.getStackTrace());
	    }
	}
	
	private void invokeQueryStandardTimeThread() {
	    
	    String METHOD_NAME = Thread.currentThread().getStackTrace()[1].getMethodName();
	    
	    // Get Standard Time with schedule.
	    ScheduledExecutorService SR_Executor = Executors.newScheduledThreadPool(2);
	    ScheduledExecutorService AS_Executor = Executors.newScheduledThreadPool(2);
	    
	    int delay = 3;
	    
	    Runnable SR_Runnalbe = () -> this.getStandardTime("sr");
	    Runnable AS_Runnable = () -> this.getStandardTime("as");
	    
	    logger.info("[{}][{}] Scheduled Task.  Query for Delay Decision from InfluxDB.,  Time now = {}", CLASS_NAME, METHOD_NAME, LocalTime.now());
	    
	    SR_Executor.scheduleWithFixedDelay(SR_Runnalbe, 0, delay, TimeUnit.SECONDS);
	    AS_Executor.scheduleWithFixedDelay(AS_Runnable, 0, delay, TimeUnit.SECONDS);
	    
	}
	
	
	private void getStandardTime(String type) {
	    
	    String METHOD_NAME = "getStandardTime";
	    logger.info("[{}][{}]  Runnable Task : {}.", CLASS_NAME, METHOD_NAME, LocalTime.now());
	    
	    double QueryReturn = 0.0;
	    
	    if(type.equals("as")) {
		
		String QueryAS = "SELECT mean(\"AminusS\")  FROM \"test\".\"message\" WHERE time >= now() - 5m fill(null)";
		QueryReturn = this.influxDbUtil.getQueryResult(QueryAS, InfluxDbConf.LOCAL.getDB_Name(), Means.class);
		this.standard_AS = QueryReturn;
		logger.info("[{}][{}] Standard AS : {}.,  now : {}.", CLASS_NAME, METHOD_NAME, this.standard_AS, LocalTime.now());
		
	    }else if(type.equals("sr")) {
		
		String QuerySR = "SELECT mean(\"SminusR\")  FROM \"test\".\"message\" WHERE time >= now() - 5m fill(null)";
		QueryReturn = this.influxDbUtil.getQueryResult(QuerySR, InfluxDbConf.LOCAL.getDB_Name(), Means.class);
		this.standard_SR = QueryReturn;
		logger.info("[{}][{}] Standard SR : {}.,  now : {}.", CLASS_NAME, METHOD_NAME, this.standard_SR, LocalTime.now());
		
	    }else {
		
		logger.error("[{}][{}]  No Match.,  Need to Check Type : {}.", CLASS_NAME, METHOD_NAME, type);
	    }
	}
	
	/**
	 * �񱳴���� target�� �������� standard ���� ���ڰ� �� ũ�� true => delay.
	 * @param Standard
	 * @param target
	 * @return
	 */
	private boolean delayDecision(Double Standard, Double target) {
	    
	    logger.debug("[{}][{}]  Standard : {}.,  Target : {}.",CLASS_NAME, "delayDecision", Standard, target);
	    return Standard * ((100 + this.DELAY_PERCENT) / 100) < target;
	}
	
	public void setStopRun(boolean stopRun) {
	    
	    logger.info("[{}][{}]  EmsReceiver Set to Stop.  setStopRun : {}", CLASS_NAME, "setStopRun");
	    this.stopRun = stopRun;
	}
	
	public void closeAllResource() {
	    
	    if(this.msgConsumer != null) {
		try {
		    this.msgConsumer.close();
		}catch (Exception e) {
		    // TODO: handle exception
		}
		
	    }
	    
	    if(this.session != null) {
		try {
		    this.session.close();
		}catch (Exception e) {
		    // TODO: handle exception
		}
	    }
	
	    if(this.connection != null) {
		try {
		    this.connection.close();
		}catch (Exception e) {
		    // TODO: handle exception
		}
	    }

}
}