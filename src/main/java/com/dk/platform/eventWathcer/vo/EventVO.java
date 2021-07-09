package com.dk.platform.eventWathcer.vo;

import com.dk.platform.eventWathcer.util.MemoryStorage;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@NoArgsConstructor
public class EventVO {

	private static String CLASS_NAME = EventVO.class.getSimpleName();
	private static Logger logger = LoggerFactory.getLogger(CLASS_NAME);


	/**
	 *  =====> Schema <=====
	 *  MessageID				:			String			:			Event Key. Event ID,  can find VO in Map with ID.
	 *  Destination				:			String			:			EMS Destination name.
	 *  HostName				:			String			:			Client Server Name
	 *  RecvTime				:			long			:			Receive Event Type Time Stamp
	 *  SendTime				:			long			:			Send Event Type Time Stamp
	 *  AckTime					:			long			:			Acknowledge Event Type Time Stamp
	 *  payload					:			byte[]			:			Message Contents
	 * (7)
	 *
	 * differenceSR				:			int				:			Difference between Receive Event Time and Send Event Time. (Send - Receive)
	 * differenceAS				:			int				:			Difference between Send Event Time and Ack Event Time. ( Ack - Send)
	 * (2)
	 *
	 * thresholdSR				:			int				:			Threshold for delay case calculating by windows queue.
	 * thresholdAS				:			int				:			Same Above
	 * (2)
	 *
	 *  DelayForSR				:			boolean			:			Flag on When Delay when SR Case.
	 *  DelayForAS				:			boolean			:			Flag on When Delay when AS Case.
	 * (2)
	 *
	 *  WatchedCount			:			int				:			for Watcher,  Watch Count.
	 * (1)
	 */

	/*
	 * Default Variables
	 */
	private String messageID; private String destName; private String hostName;
	private Long receiveTime; private Long sendTime; private Long ackTime;
	private byte[] paylod;

	private int differenceSR; private int differenceAS;
	private int thresholdSR; private int thresholdAS;

	private boolean DelayForSR; private boolean DelayForAS;

	private AtomicInteger WatchedCount = new AtomicInteger();


	private final Timer timer = new Timer("DelayTimer", true); private TimerTask timerTask;
	private int delayMargin = MemoryStorage.getInstance().getDELAY_PRECENT();


	/**
	 *
	 * @param messageID
	 * @param destname
	 * @param hostName
	 * @param timeStamp
	 * @param payload
	 * @param thresholdSR
	 */
	@Builder
	public EventVO(String messageID, String destname, String hostName, long timeStamp, byte[] payload, int thresholdSR) {

		log.debug(" Print ALl Parameter");

		this.messageID = messageID; this.destName = destname; this.hostName = hostName;
		this.receiveTime = timeStamp; this.paylod = payload;

		log.info("Initialize MapVO., By Inserting Receive Log Type. ");

		// SR Timer Start.
		this.thresholdSR = thresholdSR;
		timerTask = new TimerTask() {
			@Override
			public void run() {
				if(sendTime == null){
					DelayForSR = true;
					MemoryStorage.getInstance().getSurveillanceTargets().add(messageID);
					log.warn(" SR Delay Case Flag on. Send to Surveillance Queue.Current EventVO : {}", this.toString());

				}
				log.info("Not SR Delay Case. Message ID  : {}", messageID);
			}
		};
		long delay = this.getDelayTime(thresholdSR);
		timer.schedule(timerTask, delay);
		log.info("Set-Up RS Delay Timer. after {}ms", delay);

	}

	/**
	 *
	 * @param threshold
	 * @return
	 */
	private long getDelayTime(int threshold){
		return Math.round(threshold * (1 + delayMargin * 0.01));
	}


	/**
	 * Update VO When SEND Data Inserted.
	 * @param timeStamp
	 * @return
	 */
	public void updateSendEvent(long timeStamp, int thresholdAS) {

		this.sendTime = timeStamp;
		this.differenceSR = (int) (sendTime - receiveTime);

		// AS Timmer Start.
		timerTask.cancel();
		log.info("Cancel TimerTask for Create AS Timer. SR is Not Delay Case.");

		this.thresholdAS = thresholdAS;
		timerTask = new TimerTask() {
			@Override
			public void run() {
				if(ackTime == null){
					DelayForAS = true;
					MemoryStorage.getInstance().getSurveillanceTargets().add(messageID);
					log.warn(" AS Delay Case Flag on. Send to Surveillance Queue.Current EventVO : {}", this.toString());

				}
				log.info("Not AS Delay Case. Message ID  : {}", messageID);
			}
		};
		long delay = this.getDelayTime(thresholdAS);
		timer.schedule(timerTask, delay);
		log.info("Set-Up AS Delay Timer. after {}ms", delay);

	}
	
	
	/**
	 * Update VO when ACK Data Inserted.
	 * 1. Update Ack Time Stamp
	 * 2. Cancel Timer and Timer Task.
	 *
	 * @param timStamp
	 * @return
	 */
	public void updateAckEvent(long timStamp) {
	    
	    this.ackTime = timStamp;
	    this.differenceAS = (int) (ackTime - sendTime);

	    // Remove and Purge Timer.
		timerTask.cancel();
		timer.cancel();
		log.info("Cancel TimerTask and Timer. It is Not AS Delay Case.");


	}

	/**
	 * 지연처리 판단 기준을 위한 Window Queue로 시간별 차이 값 보고 여부 확인 메소드
	 * 지연인 경우(AS or SR)에는 보고하지 않고 Event-Tracker로 보내서 처리해야한다.
	 *
	 * Report difference to Windows Queue for calculating Threshold.
	 * if either AS and SR are Not Delay -> Report data
	 * if not report to tracker.
	 *
	 * @return
	 */
	public boolean isReportable(){
		return !this.isDelayForSR() && !this.isDelayForAS();
	}

	/**
	 *
	 * @return			:		Watched Count.
	 */
	public int increaseWatchCount(){
		return this.WatchedCount.incrementAndGet();
	}


	/*
	 * Getter
	 */

	public int getDifferenceSR() {
		return differenceSR;
	}

	public int getDifferenceAS() {
		return differenceAS;
	}

	public String getMessageID() {
		return messageID;
	}

	public Long getReceiveTime() {
		return receiveTime;
	}

	public Long getSendTime() {
		return sendTime;
	}

	public int getThresholdSR() {
		return thresholdSR;
	}

	public int getThresholdAS() {
		return thresholdAS;
	}

	public boolean isDelayForSR() {
		return DelayForSR;
	}

	public boolean isDelayForAS() {
		return DelayForAS;
	}

	/*
	 * Setter
	 */

	public void setThresholdSR(int thresholdSR) {
		this.thresholdSR = thresholdSR;
	}

	public void setThresholdAS(int thresholdAS) {
		this.thresholdAS = thresholdAS;
	}

	@Override
	public String toString() {
		return "EventVO{" +
				"messageID='" + messageID + '\'' +
				", destName='" + destName + '\'' +
				", hostName='" + hostName + '\'' +
				", receiveTime=" + receiveTime +
				", sendTime=" + sendTime +
				", ackTime=" + ackTime +
				", differenceSR=" + differenceSR +
				", differenceAS=" + differenceAS +
				", thresholdSR=" + thresholdSR +
				", thresholdAS=" + thresholdAS +
				", DelayForSR=" + DelayForSR +
				", DelayForAS=" + DelayForAS +
				", WatchedCount=" + WatchedCount +
				'}';
	}

//	/**
//	 */
//	public void Backup_method() {
//	    /**
//		 * Update Map VO.,  (Send Type,  Acknowledge Type)
//		 * @param logType
//		 * @param extn
//		 * @param timeStamp
//		 * @return
//		 * @throws Exception
//		 */
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

//	}

	public static void main(String[] args) {

		int num = 50;
		int percent = 20;
		long delay = Math.round(50 * (20 * 0.01));
		System.out.println(delay);
	}
	
}
