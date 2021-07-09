package com.dk.platform.eventWathcer.util;

import com.dk.platform.common.EppConf;
import com.dk.platform.common.util.CommonUtil;
import com.dk.platform.common.util.EmsUtil;
import com.dk.platform.eventWathcer.vo.EventVO;
import com.google.common.collect.EvictingQueue;
import lombok.extern.slf4j.Slf4j;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Main Job
 * 1. Singleton
 * 2. Data Structure
 */
@Slf4j
public class MemoryStorage {


    /*
     **************************************  Singleton ***************************************
     ****************************************************************************************/


    private static class SingletonHelper {
        private static final MemoryStorage INSTANCE = new MemoryStorage();
    }


    public static MemoryStorage getInstance() {
        return SingletonHelper.INSTANCE;
    }


    /*
     **************************************  Logger ******************************************
     ****************************************************************************************/



    /*
     ***********************************  Variables ******************************************
     ****************************************************************************************/

    /* Default Variables */

    private EppConf eppConf;

    private EmsUtil emsUtil;

    private CommonUtil commonUtil;

    /* Custom Variables */

    private WatcherUtil watcherUtil;

    private InfluxDbUtil influxDbUtil;

    /* Data Structures. */
    /**
     * Threshold for Checking Delay Case.
     * SR means Threshold between Receive Event and Send Event.
     */
    private Double ThresholdSR;

    /**
     * Threshold for Checking Delay Case.
     * AS means Threshold between Send Event and Acknowledge Event.
     */
    private Double ThresholdAS;

    /**
     * DELAY 여유공간
     */
    private int DELAY_PRECENT = 15; // TODO EXTERNAL CONF

    /**
     * HashMap Contain Event VO.
     */
    private ConcurrentHashMap<String, EventVO> EventVoMap;

    /**
     * Queue Contained Message ID which is likely to be delayed.
     */
    private Queue<String> SurveillanceTargets;


    /**
     *
     */
    private ConcurrentHashMap<String, EvictingQueue<String>> WindowManageMap;




    /*
     ***********************************  Constructor ****************************************
     ****************************************************************************************/


    private MemoryStorage(){}

    private void initWindowManageMap(){
        // TODO Make it Conf file.
        WindowManageMap = new ConcurrentHashMap<>();

        EvictingQueue<String> windowsForSR = EvictingQueue.create(50);
        EvictingQueue<String> windowsForAS = EvictingQueue.create(50);

        WindowManageMap.put("SR", windowsForSR);
        WindowManageMap.put("AS", windowsForAS);

    }


    /*
     ***********************************  Logic **********************************************
     *****************************************************************************************/




    /*
     ***********************************  Getter *********************************************
     ****************************************************************************************/

    public int getDELAY_PRECENT() {
        return DELAY_PRECENT;
    }

    public EppConf getEppConf() {
        return eppConf;
    }

    public EmsUtil getEmsUtil() {
        return emsUtil;
    }

    public CommonUtil getCommonUtil() { return  commonUtil;}

    public WatcherUtil getWatcherUtil() {
        return watcherUtil;
    }

    public InfluxDbUtil getInfluxDbUtil() {
        return influxDbUtil;
    }

    public ConcurrentHashMap<String, EventVO> getEventVoMap() {
        if(EventVoMap == null) EventVoMap = new ConcurrentHashMap<>();
        return EventVoMap;
    }

    public Queue<String> getSurveillanceTargets() {
        if(SurveillanceTargets == null) SurveillanceTargets = new ConcurrentLinkedDeque<>();
        return SurveillanceTargets;
    }

    public ConcurrentHashMap<String, EvictingQueue<String>> getWindowManageMap() {
        if(WindowManageMap == null) this.initWindowManageMap();
        return WindowManageMap;
    }

    public Double getThresholdSR() {
        return ThresholdSR;
    }

    public Double getThresholdAS() {
        return ThresholdAS;
    }

    /*
     ***********************************  Setter *********************************************
     ****************************************************************************************/

    public void setEppConf(EppConf eppConf) {
        this.eppConf = eppConf;
    }

    public void setEmsUtil(EmsUtil emsUtil) {
        this.emsUtil = emsUtil;
    }

    public void setCommonUtil(CommonUtil commonUtil) {
        this.commonUtil = commonUtil;
    }

    public void setWatcherUtil(WatcherUtil watcherUtil) {
        this.watcherUtil = watcherUtil;
    }

    public void setInfluxDbUtil(InfluxDbUtil influxDbUtil) {
        this.influxDbUtil = influxDbUtil;
    }

    public void setEventVoMap(ConcurrentHashMap<String, EventVO> eventVoMap) {
        EventVoMap = eventVoMap;
    }

    public void setSurveillanceTargets(Queue<String> surveillanceTargets) {
        SurveillanceTargets = surveillanceTargets;
    }

    public void setWindowManageMap(ConcurrentHashMap<String, EvictingQueue<String>> windowManageMap) {
        WindowManageMap = windowManageMap;
    }

    public void setThresholdSR(Double thresholdSR) {
        ThresholdSR = thresholdSR;
    }

    public void setThresholdAS(Double thresholdAS) {
        ThresholdAS = thresholdAS;
    }

    /*
     *************************************  Deprecated ***************************************
     ****************************************************************************************/



    /*
     *************************************  Main *********************************************
     ****************************************************************************************/


    public static void main(String[] args) {

    }

}
