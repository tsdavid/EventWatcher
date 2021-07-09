package com.dk.platform.eventWathcer.process;

import com.dk.platform.common.EppConf;
import com.dk.platform.common.EventPlatformConsumer;
import com.dk.platform.common.EventPlatformProcess;
import com.dk.platform.common.EventPlatformReceiver;
import com.dk.platform.common.util.CommonUtil;
import com.dk.platform.common.util.EmsUtil;
import com.dk.platform.eventWathcer.process.conf.EmsMsgPropertyConf;
import com.dk.platform.eventWathcer.process.conf.LogType;
import com.dk.platform.eventWathcer.util.InfluxDbUtil;
import com.dk.platform.eventWathcer.util.MemoryStorage;
import com.dk.platform.eventWathcer.util.WatcherUtil;
import com.dk.platform.eventWathcer.vo.EventVO;
import com.tibco.tibjms.Tibjms;
import com.tibco.tibjms.TibjmsMapMessage;
import com.tibco.tibjms.admin.TibjmsAdminException;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MonitoringReceiverProcess implements Runnable, EventPlatformConsumer, EventPlatformProcess, EventPlatformReceiver {

    /*
     **************************************  Logger ******************************************
     ****************************************************************************************/

    private static final Logger logger = LoggerFactory.getLogger(MonitoringReceiverProcess.class);



    /*
     ***********************************  Variables ******************************************
     ****************************************************************************************/

    /* Default Variables */
    private Connection connection;

    private Session session;

    private MessageConsumer msgConsumer;

    private Destination destination;

    private int ackMode;

    private String destName;

    private boolean isTopic;

    private boolean active = true;

    private MemoryStorage memoryStorage;

    private EppConf eppConf;

    private EmsUtil emsUtil;

    private CommonUtil commonUtil;


    /* Custom Variables */
    private WatcherUtil watcherUtil;

    private InfluxDbUtil influxDbUtil;

    final EmsMsgPropertyConf proceeConf  = EmsMsgPropertyConf.DEFAULT;

    private ConcurrentHashMap<String, EventVO> eventVoMap;



    /*
     ********************************  Message Properties ************************************
     ****************************************************************************************/

    // TODO External Conf
    final String RECV_TYPE = LogType.RECV.getType();

    final String SEND_TYPE = LogType.SEND.getType();

    final String ACK_TYPE = LogType.ACK.getType();


    /*
     ***********************************  Constructor ****************************************
     ****************************************************************************************/

    /**
     *
     * @param destName          :           EMS Destination. Receive Message From
     */
    public MonitoringReceiverProcess(String destName){
        this(destName, Session.AUTO_ACKNOWLEDGE);
    }

    /**
     *
     * * @param destName          :           EMS Destination. Receive Message From
     * @param ackMode       :       Ems Acknowledge, Default = Auto Acknowledge {@link Session}
     */
    public MonitoringReceiverProcess(String destName, int ackMode) {
        this(destName, ackMode, true);
    }




    /**
     *
     * @param destName      :       Ems destination Name. Receive Message From.
     * @param ackMode       :       Ems Acknowledge, Default = Auto Acknowledge {@link Session}
     * @param isTopic       :       Ems Destination Type. Default = false
     */
    public MonitoringReceiverProcess(String destName, int ackMode, boolean isTopic) {

        this.destName = destName;

        this.ackMode = ackMode;

        this.isTopic = isTopic;

    }


    /**
     * Use Instantiate Essential Class.
     * 0. Thread Name
     * 1. MemoryStorage
     * 2. EmsUtil
     * 3. Util
     * 4. (Optional) ProcessName.
     * 5. Create Ems Connection from Receiver Interface.
     */
    @Override
    public void setUpInstance() throws TibjmsAdminException {


        // Set-Up MemoryStorage.
        this.memoryStorage = MemoryStorage.getInstance();
        eventVoMap = memoryStorage.getEventVoMap();

        // Common Util
        if(memoryStorage.getCommonUtil() == null) memoryStorage.setCommonUtil(new CommonUtil());
        commonUtil = memoryStorage.getCommonUtil();

        // EMS Util
        if(this.memoryStorage.getEmsUtil() == null) {
            this.emsUtil = new EmsUtil();
            this.memoryStorage.setEmsUtil(this.emsUtil);
        }
        this.emsUtil = this.memoryStorage.getEmsUtil();

        // Watcher Util.
        if(this.memoryStorage.getWatcherUtil() == null) this.watcherUtil = new WatcherUtil();
        this.watcherUtil = this.memoryStorage.getWatcherUtil();


        // EppConf
        this.eppConf = memoryStorage.getEppConf();


        // Create EMS Connection.
        try {
            this.createEmsConnection();

        } catch (JMSException e) {
            logger.error(" Error : {}/{}.", e.getMessage(), e.toString());
            e.printStackTrace();
        }

        // SetUp ThreadName
        String threadName = commonUtil.setUpThreadName(MonitoringReceiverProcess.class.getSimpleName());
        // Set-Up Thread Name.
        Thread thread = Thread.currentThread();
        String orginName = thread.getName();
        thread.setName(threadName);
        String ChangedName = thread.getName();

        if(ChangedName.equals(threadName)){
            logger.info("Thread Name Has been changed.. Original : {}.  New : {}..",
                    orginName, ChangedName);
        }
        // TODO THINK BETTER ==> What if Error while Change Name?

    }



    /**
     * Create Connection with EMS.
     * Receiver has to make connection with EMS, to Receive Some staff from there.
     *
     * @throws TibjmsAdminException :       Exception when Initiate {@link EmsUtil}
     * @throws JMSException         :       Exception when Create Session {@link Session}
     */
    @Override
    public void createEmsConnection() throws TibjmsAdminException, JMSException {

        if(this.emsUtil == null){
            this.connection = new EmsUtil(eppConf.ems.getUrlVal(), eppConf.ems.getUsrVal(), eppConf.ems.getPwdVal()).getEmsConnection();
        }
        this.connection = this.emsUtil.getEmsConnection();
        this.session = this.connection.createSession(ackMode);

        destination = (this.isTopic) ? session.createTopic(this.destName) : session.createQueue(this.destName);
        msgConsumer = session.createConsumer(destination);

    }


    /*
     ***********************************  Process Logic **************************************
     *****************************************************************************************/

    @Override
    public void run() {

        logger.info(" Run Method, Run Execute Method");

        this.execute();
    }

    /**
     * run Logic
     * if runnable case, run() {
     * execute() {
     * Logic.
     * }
     * }
     */
    @Override
    public void execute() {

        logger.info(" execute Method, Run the While Loop, Check Active : {}", active);

        while(active){

            TextMessage message = null;
            try {

                message = (TextMessage) msgConsumer.receive();

                // Do Something
                this.handleMessage(message);

            } catch (JMSException e) {
                logger.error("[{}] Error : {}/{}.","ParsingMsg", e.getMessage(), e.toString());
                e.printStackTrace();
            }



            // Ack
            if (ackMode == Session.CLIENT_ACKNOWLEDGE ||
                    ackMode == Tibjms.EXPLICIT_CLIENT_ACKNOWLEDGE ||
                    ackMode == Tibjms.EXPLICIT_CLIENT_DUPS_OK_ACKNOWLEDGE)
            {
                try {
                    // TODO THINKK BETTER ==> Prevent NetWork Dis-Connection Case.
                    message.acknowledge();
                } catch (JMSException e) {
                    logger.error("[{}] Error : {}/{}.","AckMsg", e.getMessage(), e.toString());
                    e.printStackTrace();
                }
            }


        }
        logger.info(" execute Method, Break the While Loop");

    }

    /**
     * Message Handle Method.
     * When Receiver Receive Message, run handleMessage method.
     *
     * @param message :           Message.
     */
    @Override
    public void handleMessage(Message message) {

        TibjmsMapMessage msg = (TibjmsMapMessage) message;

        // Parsing Message
        try{
            String eventType = msg.getStringProperty(proceeConf.getEventType_Pro());
            long timeStamp = msg.getJMSTimestamp();
            String messageID = msg.getStringProperty(proceeConf.getMessageID_Pro());
            String destName = msg.getStringProperty(proceeConf.getDestination_Pro());
            String hostName = msg.getStringProperty(proceeConf.getHostName_Pro());
            // TODO Store pay-load in MapVO in case of Missing Message Case.
            byte[] payload = msg.getBytes(proceeConf.getPayload_Pro());
            logger.debug("Parsing Message Contents :::  EventType :  {}, TimeStamp : {}, MessageID : {}, DestName : {}, HostName : {}"
                    , eventType, timeStamp, messageID, destName, hostName);


            /*
            ***************************************************************************************
            ****************************  Sort Message By Event Type.  ****************************
            ***************************************************************************************
            **************************************************************************************/


            /*
             * Case :  Receive Event Type.
             * Condition : The Event Type is RECV.
             * Store Data into MapCentral.
             * Task.
             * 1. Generate New EventVO.  and Initialize it.
             */
            if(eventType.equals(RECV_TYPE)){

                try {
                    receiveEventHandle(timeStamp, messageID, destName, hostName, payload);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("");
                }
            }


            /*
             * CASE : Send Event Type.
             * Condition :  1. The Event Type is SEND.
             * Update Data info MapCentral.
             * Task.
             * 0. Get EventVO.
             * 1.  Update MapVO.
             * 2.  Check Delay-Decision.
             */
            if(eventType.equals(SEND_TYPE)){ sendEventHandle(timeStamp, messageID);}


            /*
             * CASE : ACK Event Type.
             * Condition : 1.  Event Type is ACK.
             * Update Data into MapCentral.
             * Task
             * 0. Get EventVO.
             * 1.  Update MapVO.
             * 2.  Check Delay - Decision.
             * 3.  Decrease Current Message Count.
             * 4.  Send Key to Queue for notifying Remove.
             *
             */
            if(eventType.equals(ACK_TYPE)){ ackEventHandle(timeStamp, messageID); }


        }catch (JMSException e){
            logger.error("[{}] Error : {}/{}.","HandleError", e.getMessage(), e.toString());
            e.printStackTrace();

        }

    }

    /*
     ***********************************  Case Logic *****************************************
     *****************************************************************************************/

    /**
     *
     * Store Data into MapCentral.
     * Task.
     * 1. Generate New EventVO.  and Initialize it.
     * @param timeStamp                 :               TimeStamp when Event Type received.
     * @param messageID                 :               Event Unique ID.
     * @param destName                  :               Event contained EMS Destination.
     * @param hostName                  :               Host Server handle thie Event.
     * @param payload                   :               Message Contents.
     */
    private void receiveEventHandle(long timeStamp, String messageID, String destName,
                                    String hostName, byte[] payload) throws Exception {
        log.debug("");

        if(eventVoMap.containsKey(messageID)) {
            // TODO Receive Type is Already in Map Case.
            log.error("Receive Type Event's Message ID is already in Map.");
            return;
        }

        // 1. Generate New EventVO. put in map.
        eventVoMap.put(messageID, EventVO.builder().messageID(messageID).timeStamp(timeStamp).destname(destName).
                                                    hostName(hostName).payload(payload).thresholdSR(memoryStorage.getThresholdSR()).
                                                    build());

    }


    /**
     * Task.
     * 0. Get EventVO.
     * 1.  Update MapVO.
     * @param timeStamp                 :               TimeStamp when Event Type received.
     * @param messageID                 :               Event Unique ID.
     */
    private void sendEventHandle(long timeStamp, String messageID){

        log.debug("");
        EventVO eventVO = eventVoMap.get(messageID);
        if(eventVO == null) {
            // TODO EventVo is Not in Map on Send Case.
            log.error("Send Type Event's Message ID is already in Map.");
            return;
        }

        // Update time-stamp in EventVO.
        eventVO.updateSendEvent(timeStamp, memoryStorage.getThresholdAS());

    }


    /**
     * 1. Update Time-Stamp
     * 2. Reporting
     * 3. Remove EventVO.
     *
     * @param timeStamp                 :               TimeStamp when Event Type received.
     * @param messageID                 :               Event Unique ID.
     */
    private void ackEventHandle(long timeStamp, String messageID){

        log.debug("");
        EventVO eventVO = eventVoMap.get(messageID);
        if(eventVO == null){
            // TODO EventVo is Not in Map on Send Case.
            log.error("Ack Type Event's Message ID is already in Map.");
            return;
        }

        // update time-stamp in EventVO.
        eventVO.updateAckEvent(timeStamp);

        // Report Window Queue Case.  Report Difference to Window Queues.
        if(eventVO.isReportable()) {
            memoryStorage.getWindowManageMap().get("SR").add(eventVO.getDifferenceSR());    // TODO Hard-Coding
            memoryStorage.getWindowManageMap().get("AS").add(eventVO.getDifferenceAS());
            log.info("ID : {}. is Not-Delay Case. Report to Windows Queue for calculating Threshold and " +
                    "DifferenceSR : {}, DifferenceAS : {}.  it's  going to be removed",
                    messageID, eventVO.getDifferenceSR(), eventVO.getDifferenceAS());
        }else{
            // TODO Delay Case. Send to Tracker to analyze reason of Delay.
        }

        // Remove
        eventVoMap.remove(messageID);
        log.debug("ID : {}. is Removed from Map. Current Map Status : {}", messageID, eventVoMap.keys().toString());

    }


    @Override
    public void setActive(){
        logger.info(" Set Active Method, This will Start in sec.");
        active = true;
    }


    @Override
    public void setDeActive() {
        logger.info(" Set DeActive Method, This will be Stopped in sec.");
        active = false;

    }

    @Override
    public void closeAllResources() {

        if(session != null){
            try {
                session.close();
            } catch (JMSException e) {
                logger.error("[{}] Error : {}/{}.","CloseSession", e.getMessage(), e.toString());
                e.printStackTrace();
            }
        }

        if(msgConsumer != null){
            try {
                msgConsumer.close();
            } catch (JMSException e) {
                logger.error("[{}] Error : {}/{}.","CloseConsumer", e.getMessage(), e.toString());
                e.printStackTrace();
            }
        }

    }






    /*
     *************************************  Main *********************************************
     ****************************************************************************************/

    public static void main(String[] args) throws JMSException {
//        new ReceiverProcess("h", Session.AUTO_ACKNOWLEDGE, false).run();

    }
}
