package com.dk.platform.eventWathcer.vo;

import com.dk.platform.eventWathcer.process.conf.LogType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventVO {
	
	private static String CLASS_NAME = EventVO.class.getSimpleName();
	private static Logger logger = LoggerFactory.getLogger(CLASS_NAME);
	
	
	/**
	 *  =====> Schema <=====
	 *  EXTN					:			String			:			EXTEN, Secondary Key
	 *  CMD					:			String			:			Command 
	 *  Destination				:			String			:
	 *  QueueType				:			String			:			"SQ" : Sequential Queue,  "DQ" : Distribute Queue
	 *  HostName				:			String			:			Client Server Name
	 *  RecvTime				:			Double			:			Receive Event Type Time Stamp
	 *  SendTime				:			Double			:			Send Event Type Time Stamp
	 *  DelayForSR			:			boolean			:			Flag on When Delay when SR Case.
	 *  AckTime				:			Double			:			Acknowledge Event Type Time Stamp
	 *  DelayForAS			:			boolean			:			Flag on When Delay when AS Case.
	 *  SminusR				:			Double			:			SendTime		-		RecvTime
	 *  AminusS				:			Double			:			AckTime			-		SendTime
	 *  CompleteFlag			:			boolean			:			Marking as Seem to be Danger
	 *  SSR					:			Double			: 			Stands for Standard SminusR 
	 *  SAS					:			Double			:			Stands for Standard	AminusS
	 *  WatchCount			:			Intger			:			for Watcher,  Watch Count.
	 */
	private String extn;
	
	private String cmd;
	
	private String destiNation;
	
	private String queueType;
	
	private String hostName;
	
	private Double recv_time = null;
	
	private Double send_time = null;
	
	private Double ack_time = null;
	
	// R- S TIme
	private Double SminusR = null;
	private Double SSR = null;
	
	private Double AminusS = null;
	private Double SAS = null;
	
	private int WatchCount = 0;
	
	private boolean CompleteFlag = false;
	
	
	
	/**
	 * Initialize Constructor MapVO
	 * Insert R data Type
	 * @param logType
	 * @param extn
	 * @param dest
	 * @param hostName
	 * @param timeStamp
	 * @throws Exception
	 */
	public EventVO(String logType, String extn, String cmd, String queueType, String dest, String hostName, Long timeStamp) throws Exception {
		
		String METHOD_NAME = Thread.currentThread().getStackTrace()[1].getMethodName();
		logger.info("[{}][{}] ", CLASS_NAME, METHOD_NAME);
		
		
		
		// Initialize Value Object
		// Case : Insert Receive Log Type
		if(logType.equals(LogType.RECV.getType())){
		    
		    if(insertR_data(extn, cmd, queueType, dest, hostName, timeStamp))
			
			logger.info("[{}][{}]  Initialize MapVO., By Inserting Receive Log Type. ", CLASS_NAME, METHOD_NAME);
		    
		}else {
		    logger.error("[{}][{}]  Error While Inserting Receive Log Type.  Print Paramerters.  LogType : {}., EXTN : {}., Command : {}., Destination : {}., QueueType : {}.,  HostName : {}., TimeStamp : {}.",
			    CLASS_NAME, METHOD_NAME, logType, extn, dest, cmd, queueType, hostName, timeStamp);
		    
		    throw new Exception(" Fail to Initialize MapVO.");
		    
		}
	}
	
	
	/**
	 * Insert Receive Data., Initialize MapVO
	 * @param extn
	 * @param dest
	 * @param hostName
	 * @param timestamp
	 * @return
	 */
	private boolean insertR_data (String extn, String cmd, String queueType, String dest, String hostName, double timestamp) {
	    
	    	String METHOD_NAME = Thread.currentThread().getStackTrace()[1].getMethodName();
		logger.info("[{}][{}] ", CLASS_NAME, METHOD_NAME);
		
		try {
		    if(this.destiNation == null) this.destiNation = dest;
		    
		    this.queueType = queueType;
		    
		    this.cmd = cmd;
		    
		    if(this.hostName == null) this.hostName = hostName;
		    
		    if(this.recv_time == null) this.recv_time = timestamp;
		    
		    if(this.extn == null) this.extn = extn;
		    
		}catch (Exception e) {
		    
		    logger.error("[{}][{}] Error.,  {}\n{}", CLASS_NAME, METHOD_NAME, e.getMessage(), e.getStackTrace());
		    return false;
		}
		
		return true;
	    
	}
	
	
	
	/**
	 * Update VO When SEND Data Inserted.
	 * @param timeStamp
	 * @return
	 */
	public double updateS_data(double timeStamp) {
	    
	    this.send_time = timeStamp;
	    this.SminusR = timeStamp - this.recv_time;
	    
	    return this.SminusR;
	}
	
	
	/**
	 * Update VO when ACK Data Inserted.
	 * @param timStamp
	 * @return
	 */
	public double updateA_data(double timStamp) {
	    
	    this.ack_time = timStamp;
	    this.AminusS = timStamp - this.send_time;
	    this.CompleteFlag = true;
	    return this.AminusS;
	}
	
	
	public String getEXTN() {
	    return this.extn;
	}
	
	public String getCMD() {
	    return this.cmd;
	}
	
	public String getDestination() {
	    return this.destiNation;
	}
	
	public String getQueueType() {
	    return this.queueType;
	}
	
	public String getHostName() {
	    return this.hostName;
	}
	
	public boolean getCompleteFlag() {
	    return this.CompleteFlag;
	}
	
	public Double getSR() {
	    return this.SminusR;
	}
	
	public Double getAS() {
	    return this.AminusS;
	}
	
	public Double getSSR() {
	    return this.SSR;
	}
	
	public Double getSAS() {
	    return this.SAS;
	}
	
	public int getWatchCount() {
	    return this.WatchCount;
	}
	public void setSSR(double ssr) {
	    this.SSR = ssr;
	}
	public void setSAS(double sas) {
	    this.SAS = sas;
	}
	
	
	@Override
	public String toString() {
	    
	    return "EXTN : " + this.extn + 
		    " Destination : " + this.destiNation + 
		    " Queue Type : " + this.queueType + 
		    " Host Name : " + this.hostName + 
		    " Recv Time : " + this.recv_time + 
		    " Send Time : " + this.send_time + 
		    " Ack Time : "  + this.ack_time  + 
		    " SR Time : " + this.SminusR + 
		    " AS Time : " + this.AminusS;
	}
	
	
	/**
	 */
	public void Backup_method() {
	    /**
		 * Update Map VO.,  (Send Type,  Acknowledge Type)
		 * @param logType
		 * @param extn
		 * @param timeStamp
		 * @return
		 * @throws Exception
		 */
//		public boolean updateVO(String logType, String extn, long timeStamp) throws Exception {
	//
//		    String METHOD_NAME = Thread.currentThread().getStackTrace()[1].getMethodName();
//		    logger.info("[{}][{}] ", CLASS_NAME, METHOD_NAME);
//		   
//		    // Check EXTN, the secondary key
//		    if(this.extn.equals(extn)) {
//			
//			/// Check Log Type 
//			if(logType.equals(LogType.SEND.getType())) {
//			    
//			    // SEND Case
//			    if(this.send_time == null) {
//				this.send_time = timeStamp;
//	  		       // Calculate S-R time -->	Send Event - Receive == Complete QMM Process.
//				this.SminusR = timeStamp - this.recv_time;
//				
//				return true;
	//
//			    }else {
//				
//				// Not Null
//				logger.error("[{}][{}] Error While Update MapVO.  Send Time is Not Null.  "
//					+ "Print Parameters.  LogType : {},  EXTN : {}, TimeStamp :  {}.",
//					CLASS_NAME, METHOD_NAME, extn, timeStamp);
//				throw new Exception("Fail to Update SEND MapVO.,  SendTime is Not Null");
//			    }
//			    
//			    
//			}else if(logType.equals(LogType.ACK.getType())) {
//			    
//			    // ACK Case
//			    if(this.ack_time == null) {
//				this.ack_time = timeStamp;
//				// Calculate A-S time -->	Acknowledge Event - Send == Complete SO Process.
//				this.AminusS = timeStamp - this.send_time;
//				
//				return true;
//				
//			    }else {
//				 // Not NULL
//				  logger.error("[{}][{}] Error While Update MapVO.  Ack Time is Not Null.  "
//				  	+ "Print Parameters.  LogType : {},  EXTN : {}, TimeStamp :  {}.",
//						CLASS_NAME, METHOD_NAME,  extn, timeStamp);
//				throw new Exception("Fail to Update ACK MapVO.,  Acknowledge Time is Not Null");
//			    }
//			    
//			}else {
//			    // Not Match Anything
//			    logger.error("[{}][{}] Error While Update MapVO.  LogType is Not Match Anything.  "
//			    	+ "Print Parameters.  LogType : {},  EXTN : {}, TimeStamp :  {}.",
//					CLASS_NAME, METHOD_NAME,  extn, timeStamp);
//				throw new Exception("Fail to Update MapVO");
//			}
//			
//		    }else {
//			
//			logger.error("[{}][{}] Error While Update MapVO.  EXTN is Not Equl. Check EXTN.  "
//				+ "Print Parameters.  LogType : {},  EXTN : {}, TimeStamp :  {}.",
//				CLASS_NAME, METHOD_NAME, extn, timeStamp);
//			throw new Exception("Fail to Update MapVO");
//			
//		    }
//		    
//		}

	}
	
}
