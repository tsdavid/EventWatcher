package platform.eventWathcer.util;

import com.dk.emslog.db.mapCentral.mapValue.MapVO;
import com.dk.platform.eventWathcer.vo.EventVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.RuntimeErrorException;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * Store Log into Map using specific Value Object, Defined by MapVO.
 * @author tspsc
 *
 */
public class MapCentral {
	
	private static String CLASS_NAME = MapCentral.class.getSimpleName();
	private static Logger logger = LoggerFactory.getLogger(CLASS_NAME);
	
	/**
	 * DQ or SQ.
	 * Map for Key Count
	 */
	private ConcurrentHashMap<String, Integer> Key_Count_Map = null;

	/**
	 *
	 */
	private ConcurrentHashMap<String, EventVO> Memory_Store = null;
	
	
	/**
	 * Number of Reporting Thread.
	 */
	private Integer Numof_ReportManager = 0;	 // Have to Over than Reporting Content.
	private Integer ReportingInterval = 3;		// Interval with Seconds.
	
	/**
	 * Queue will be Stored Remove Key
	 */
	private Queue<String> Task_Queue = null;
	// TODO Set-up external Conf File
	private Integer Numof_MapManager = 0;
	
	/**
	 * Queue will be Stored Servileness MessageID.
	 */
	private Queue<String> Watch_Queue = null;
	// TODO Set-up external Conf File
	private Integer Numof_WatchManager = 0;
	private Integer WatcherCountLimit = 3;		// Count Limit
	private Integer WatcherWaitTime_sec = 3;	//
	
	/**
	 * UTILL for InfluxDB.
	 */
	private InfluxDbUtil influxDbUtil = null;
	
	
	/**
	 * Private Constructor. Singleton Design Pattern
	 */
	private MapCentral() {
		
		String METHOD_NAME = Thread.currentThread().getStackTrace()[1].getMethodName();
		logger.info("[{}][{}] ", CLASS_NAME, METHOD_NAME);
		
		// Initialize Internal Data Structures.
		try {
			
			// Initialize Maps
			if(this.Key_Count_Map == null) this.Key_Count_Map = new ConcurrentHashMap<String, Integer>();
			
			if(this.Memory_Store == null) this.Memory_Store = new ConcurrentHashMap<String, MapVO>();
			
			this.Numof_ReportManager = 2;
			
			this.ReportingInterval = 3;
			
			if(this.Task_Queue == null) this.Task_Queue = new ConcurrentLinkedDeque<>();
			
			this.Numof_MapManager = 200;
			
			this.Numof_WatchManager = 1;
			
			if(this.Watch_Queue == null) this.Watch_Queue = new ConcurrentLinkedDeque<>();
			
			
		} catch (Exception e) {
			
			logger.error("[{}][{}] Error.,  {}\n{}", CLASS_NAME, METHOD_NAME, e.getMessage(), e.getStackTrace());
			throw new RuntimeErrorException(null, "[{}][{}] Runtime Error  While Initialize MapCentral.");
		}
		
		
		// Initialize Utility for InfluxDB.
		try {
		    
		    this.influxDbUtil = new InfluxDbUtil();
		    
		}catch (Exception e) {
		    
		    logger.error("[{}][{}] Error.,  {}\n{}", CLASS_NAME, METHOD_NAME, e.getMessage(), e.getStackTrace());
		    throw new RuntimeErrorException(null, "[{}][{}] Runtime Error  While Initialize InfluxDB.");
		}
		
		
		// Invoke Internal Threads.
		try {
		    
		    // Invoke Reporting Thread.
		    this.invokeReportingThread(this.Numof_ReportManager, this.ReportingInterval);
		    
		    //Invoke Manager Thread.
		    this.invokeMapManagingThread();
		    
		    // Invoke Watcher Thread.
		    this.invokeWatcherThread(this.Numof_WatchManager, this.WatcherWaitTime_sec, this.WatcherCountLimit);
		    
		    
		}catch (Exception e) {
		    
		    logger.error("[{}][{}] ERROR While Invoke Internal Thread.,  Error : {}\n{}.",CLASS_NAME, METHOD_NAME, e.getMessage(), e.getStackTrace());
		}
		
		
	}
	
	
	/**
	 * Singleton Design Pattern. Using Helper
	 * @author tspsc
	 *
	 */
	private static class SingletonHelper{
		
		private static final MapCentral INSTANCE = new MapCentral();
	}
	
	
	/**
	 * Get MapCentral Instance
	 * @return
	 */
	public static MapCentral getInstance() {
		
		String METHOD_NAME = Thread.currentThread().getStackTrace()[1].getMethodName();
		logger.info("[{}][{}] ", CLASS_NAME, METHOD_NAME);
		
		return SingletonHelper.INSTANCE;
		
		
	}
	
	
	/**
	 * Method Return Memory Store
	 * @return
	 */
	public ConcurrentHashMap<String, EventVO> getMemoryStore(){
	    return this.Memory_Store;
	}
	
	
	/**
	 * Key Count DQ or SQ.
	 * Method Return KeyCount Map
	 * @return
	 */
	public ConcurrentHashMap<String, Integer> getKeyCountMap(){
	    return this.Key_Count_Map;
	}
	
	
	/**
	 * Method Return Task Queue
	 * @return
	 */
	public Queue<String> getTaskQueue(){
	    return this.Task_Queue;
	}
	
	public Queue<String> getWatchQueue(){
	    return this.Watch_Queue;
	}
	
	
	/**
	 * 
	 * @param NumofManager
	 * @param reportInterval
	 */
	private void invokeReportingThread(int NumofManager, int reportInterval) {
	    
	    String METHOD_NAME = Thread.currentThread().getStackTrace()[1].getMethodName();
	    logger.info("[{}][{}] ", CLASS_NAME, METHOD_NAME);
	    
	    // TODO Hard Coding 10, Report Contents.	    
	    /**
	     * Create Scheduled Thread Pool. 
	     */
	    ScheduledExecutorService SQ_Executor = Executors.newScheduledThreadPool(NumofManager);
	    ScheduledExecutorService DQ_Executor = Executors.newScheduledThreadPool(NumofManager);
	    
	    int delay = reportInterval;
	    
	    
	    // TODO Hard Coding 10, DB information.
	    /**
	     * Generate Job for Reporting Manager.
	     */
	    Runnable SQ_Runnable = () -> this.reportToInflux("queuetype", "sq", this.Key_Count_Map.get("sq"));
	    Runnable DQ_Runnable = () -> this.reportToInflux("queuetype", "dq", this.Key_Count_Map.get("dq"));
	    
	    logger.debug("[{}][{}]  ScheduledTask.  Report Message Count to influxDB.  Current Time : {}. ", CLASS_NAME, METHOD_NAME, java.time.LocalTime.now());
	    
	    // TODO Hard Coding 10, initialize delay Time.
	    SQ_Executor.scheduleWithFixedDelay(SQ_Runnable, 10, delay, TimeUnit.SECONDS);
	    DQ_Executor.scheduleWithFixedDelay(DQ_Runnable, 10, delay, TimeUnit.SECONDS);
	}
	
	
	/**
	 * 
	 * @param measurements
	 * @param tagKey
	 * @param tagValue
	 * @param size
	 */
	private void reportToInflux(String tagKey, String tagValue, int size) {
	    
	    String METHOD_NAME = Thread.currentThread().getStackTrace()[1].getMethodName();
	    logger.info("[{}][{}] ", CLASS_NAME, METHOD_NAME);
	    
	    this.influxDbUtil.InsertMsgCountPoint(tagKey, tagValue, size);
	    
	}
	
	
	/**
	 * 
	 */
	private void invokeMapManagingThread() {
	    
	    String METHOD_NAME = Thread.currentThread().getStackTrace()[1].getMethodName();
	    logger.info("[{}][{}] ", CLASS_NAME, METHOD_NAME);
	    
	    ExecutorService workerPool = Executors.newFixedThreadPool(this.Numof_MapManager);
	    
	    Runnable task = new Runnable() {
	       
		int id = 0;
		
	        @Override
	        public void run() {
	            	String METHOD_NAME = "MapManager";
	            	
	            	id = id + 1;
	            	Thread.currentThread().setName(METHOD_NAME + id);
	            	String managerName = Thread.currentThread().getName();
	            	try {
	            	    
	            	    while(true) {
	            		
	            		logger.debug("[{}][{}]  {} Invoke Map manger Thread.  Current Task Size : {}", CLASS_NAME, METHOD_NAME, managerName, Task_Queue.size());
	            		
	            		// Get Task
	            		String TargetKey = Task_Queue.poll();
	            		if(TargetKey != null) {
	            		    
	            		    try {
	            			
	            			MapVO vo = Memory_Store.get(TargetKey);
	            			Memory_Store.remove(TargetKey);
	            			logger.debug("[{}][{}] Manager : {}.,  get the Task : {}.,  its's MapVO :  {}.", CLASS_NAME, METHOD_NAME, managerName, vo.toString());
	            			
	            			influxDbUtil.InsertMessagePoint("message", TargetKey, vo.getEXTN(), vo.getCMD(), vo.getDestination(), vo.getQueueType(), vo.getHostName(), vo.getSR(), vo.getAS());
	            			logger.debug("[{}][{}] InsertMessageComplete  {} is Complete Inert Message    Key  : {}.", CLASS_NAME, METHOD_NAME, managerName, TargetKey);
	            			logger.info("[{}][{}]  {} is COmpleteTask  {} is Remove from JobList :{}.,  Remain Task Szie : {}. ",CLASS_NAME, METHOD_NAME, managerName, TargetKey,Task_Queue.size());
	            			
	            			// TODO While Exit Logic.
	            			
	            		    }catch (Exception e) {
	            			
	            			logger.error("[{}][{}] ERROR While Managing Task {} is Remove Job : {}., Remian Task Size : {}.  ERROR : {}\n{}.",CLASS_NAME, METHOD_NAME, managerName, TargetKey, Task_Queue.size(), e.getMessage(), e.getStackTrace());
	            			
				    }
	            		    
	            		}else {
	            		    logger.debug("[{}][{}]  {} is Waiting for a job.", CLASS_NAME, METHOD_NAME, managerName);
	            		    Thread.sleep(5000);
	            		}
	            		
	            	    }
	            	}catch (Exception e) {
	            	    
	            	    try {
				Thread.sleep(1000);
			    } catch (InterruptedException e1) {
				logger.error("[{}][{}]  Error While Thread Sleep.  ERROR : {}\n{}.", CLASS_NAME, METHOD_NAME, e.getMessage(), e.getStackTrace());
			    }
	            	    
	            	    logger.debug("[{}][{}]  {} is Get Out of While-Loop,  Re Assign Task.", CLASS_NAME, METHOD_NAME, managerName);
	            	    this.run();
			}
	    	
	        }
	    };
	    
	    // Invoke Workers.
	    for(int i=0; i < this.Numof_MapManager; i++) {
		logger.debug("[{}][{}]  Woker{} is Start Working now.", CLASS_NAME, METHOD_NAME, i);
		workerPool.execute(task);
	    }
	}
	
	
	
	private void invokeWatcherThread(int NumOfwatchers, int WaitSec, int WaitCountLimit) {
	    
	    String METHOD_NAME = Thread.currentThread().getStackTrace()[1].getMethodName();
	    logger.info("[{}][{}] ", CLASS_NAME, METHOD_NAME);
	    
	    ExecutorService watcherPool = Executors.newFixedThreadPool(NumOfwatchers);
	    Runnable task = new Runnable() {
	       
		int id = 0;
	        @Override
	        public void run() {
	            
	            String METHOD_NAME = "Watcher";
	            id = id +1;
	            Thread.currentThread().setName(METHOD_NAME + id);
	            String watcherName = Thread.currentThread().getName();
	            
	            try {
	        	
	        	while(true) {
	        	    
	        	    logger.debug("[{}][{}] Name : {}  Start Serveillance Watcher.  Current Task Size  : {}.", CLASS_NAME, METHOD_NAME, Watch_Queue.size());
	        	    
	        	    // Task
	        	    String TargetKey = Watch_Queue.poll();
	        	    if(TargetKey != null && Memory_Store.contains(TargetKey)) {
	        		
	        		try {
	        		    	MapVO vo = Memory_Store.get(TargetKey);
		        		logger.debug("[{}][{}]  Name : {}  get Task : {}.,  MapVO : {}", CLASS_NAME, METHOD_NAME, watcherName, TargetKey, vo.toString());
		        		
		        		// Wait for 3s. Until ACK Log Inserted.
		        		Thread.sleep(WaitSec * 1000);
		        		
		        		// Check Complete.
		        		// !vo.getCompleteFlag => Not Get ACK Log Yet.  Still Not Insert ACK.
		        		if(!vo.getCompleteFlag()) {
		        		    
		        		    if(vo.getWatchCount() > WaitCountLimit) {
		        			System.out.println("This Message is Very Danger Status.  Let ADMIN know this Message.");
		        			// TODO Alert this message to ADMIN.
		        		    }else {
		        			// Have more Chance untill over count limit
		        			Watch_Queue.offer(TargetKey);
		        		    }
		        		}
	        		}catch (Exception e) {
	        		    
	        		    logger.error("[{}][{}]  ERROR While Watching Task.,  Worker Name ;{},  Task Name : {}. Current Task Status : {}.,  ERROR : {} \n {}",
	        			    CLASS_NAME, METHOD_NAME, watcherName, TargetKey, Watch_Queue.toString(), e.getMessage(), e.getStackTrace());
				}
	        	    }else {
	        		
	        		logger.debug("[{}][{}]  {} is Waiting for a job., Current Job List Size : {}.,  ", CLASS_NAME, METHOD_NAME, watcherName, Watch_Queue.size());
	        		
	        		Thread.sleep(5000);
	        		
	        		// Re Assign Task.
	        		logger.debug("[{}][{}]  {} will reassign to task., Current Job List Size : {}.,  ", CLASS_NAME, METHOD_NAME, watcherName, Watch_Queue.size());
	        		this.run();
	        	    }
	        	}
	        	
	            }catch (Exception e) {
			logger.error("[{}][{}] Worker : {}  Error While Running Watch Task.  Error : {}\n{}.", CLASS_NAME, METHOD_NAME, watcherName, e.getMessage(), e.getStackTrace());
			
		    }
	    	
	        }
	    };
	    
	    // Invoke Watcher.
	    for (int i = 0; i < NumOfwatchers; i++) {
		logger.debug("[{}][{}] Watcher {} is Start Working.",CLASS_NAME, METHOD_NAME, i);
		watcherPool.execute(task);
	    }
	}
}
