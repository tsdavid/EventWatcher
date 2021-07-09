package com.dk.platform.eventWathcer.process;

import com.dk.platform.common.EventPlatformProcess;
import com.dk.platform.common.util.CommonUtil;
import com.dk.platform.common.util.EmsUtil;
import com.dk.platform.eventWathcer.util.MemoryStorage;
import com.dk.platform.eventWathcer.util.WatcherUtil;
import com.dk.platform.eventWathcer.vo.EventVO;
import com.tibco.tibjms.admin.TibjmsAdminException;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.C;

import javax.jms.JMSException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;


/**
 * 1. window queue를 순회하면서 Interval 마다 Threshold 구해서 Update
 * 2. surveillance queue를 순회하면서 Interval 마다 감시하기.
 */
@Slf4j
public class PollingProcess implements EventPlatformProcess, Runnable {

    private MemoryStorage memoryStorage;
    private CommonUtil commonUtil;
    private WatcherUtil watcherUtil;
    private EmsUtil emsUtil;

    private int Interval_Window = 5; // TODO External Config

    private Timer timer;

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

        memoryStorage = MemoryStorage.getInstance();

        if(memoryStorage.getCommonUtil() == null) memoryStorage.setCommonUtil(new CommonUtil());
        commonUtil = memoryStorage.getCommonUtil();

        if(memoryStorage.getWatcherUtil() == null) memoryStorage.setWatcherUtil(new WatcherUtil());
        watcherUtil = memoryStorage.getWatcherUtil();

        if(memoryStorage.getEmsUtil() == null) memoryStorage.setEmsUtil(new EmsUtil());
        emsUtil = memoryStorage.getEmsUtil();

    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {

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

        // Timer : get Threshold Task.
        this.doThresholdTask(Interval_Window);

        // Invoke Thread, Watch Surveillance Queue
        this.doSurveillanceWatch(invokeSurveillanceWorker());



    }


    /*
     ***********************************  Process Logic **************************************
     *****************************************************************************************/

    private void doSurveillanceWatch(Thread thread){

        thread.setName("SurveillanceWorker");       // TODO Hard-Coding
        thread.setDaemon(true);
        thread.run();
    }

    private Thread invokeSurveillanceWorker() {

        return new Thread(new Runnable() {
            @Override
            public void run() {
                 while (true){

                     if(!memoryStorage.getSurveillanceTargets().isEmpty()){

                        String watchTarget = memoryStorage.getSurveillanceTargets().poll();
                         CompletableFuture.runAsync(() -> {
                             int Interval_Watching = 3; // TODO External Conf , Hard-Coding
                             int Max_Watch_Count = 3;  // TODO External Conf , Hard-Coding
                             String DangerQueueName = "DangerQueueName"; // TODO External Conf, Hard-Coding.

                             try {
                                 Thread.sleep(Interval_Watching * 1000);
                             } catch (InterruptedException e) {
                                 e.printStackTrace();
                             }

                             // get EventVO.
                             EventVO eventVO = memoryStorage.getEventVoMap().get(watchTarget);
                             // SR Delay Case and Still Send Time is Null.
                             if(eventVO.isDelayForSR() && eventVO.getSendTime() == null){

                                 // 1. Increase Watch Count.
                                 // TODO Check Max Watch Count. if count is over Max. need to Something.
                                 if(eventVO.increaseWatchCount() == Max_Watch_Count + 1) {
                                     // TODO Over Max Count.
                                     // Send EMS Message.
                                     try {

                                         memoryStorage.getEmsUtil().sendSyncQueueMessage(DangerQueueName, eventVO.toString(), null);
                                         log.warn("Occur Danger Delay Case.  Possible to be hanged. Event VO : {}", eventVO.toString());

                                     } catch (JMSException e) {
                                         e.printStackTrace();
                                         log.error(" ERROR : {}\n{}.", e.getStackTrace(), e.getMessage());
                                     }
                                     // Remove EventVO in Map.
                                     memoryStorage.getEventVoMap().remove(watchTarget);
                                     log.warn("a EventVO is Removed from Map. Because of Hang Danger Case. Event VO : {}", eventVO.toString());
                                     return;
                                 }

                                 // 2. Send Back to Surveillance Queue.
                                 memoryStorage.getSurveillanceTargets().add(watchTarget);
                                 log.info("Watch Target : {} is still delay Case. Not exceed Maximum yet. re-add to Surveillance Queue.", watchTarget);
                                 log.debug("Current Surveillance Queue Status : {}.", memoryStorage.getSurveillanceTargets().toString());

                             }


                         });
                     }

                 }
            }
        });
    }

    /**
     * Timer : get Threshold Task.
     */
    private void doThresholdTask(long delay) {

        timer = new Timer("ThresholdTimer", true);      // TODO Hard-Coding
        timer.schedule(this.setThresholdTimerTask(), delay);
        log.info("Timer Task is running now. Interval : {}s", delay);

    }

    /**
     * Threshold Timer-Task
     * 1. get Windows-Queue Map(SR and AS)
     * 2. get Average from each Window-Queue
     * 3. Update Threshold to memory-Storage.
     *
     * @return
     */
    private TimerTask setThresholdTimerTask() {

        return new TimerTask() {
            @Override
            public void run() {

                // get Ex Value.
                int exThresholdSR = memoryStorage.getThresholdSR(); int exThresholdAS = memoryStorage.getThresholdAS();

                long start = System.currentTimeMillis();
                String[] arr = {"SR", "AS"};        // TODO External Config, Hard-Coding
                for(String str : arr){

                    int threshold = memoryStorage.getWindowManageMap().get(str).stream().mapToInt(i -> i).sum();
                    if(str.equals("SR")) memoryStorage.setThresholdSR(threshold);
                    if(str.equals("AS")) memoryStorage.setThresholdAS(threshold);
                    log.info(" Calculate {} Threshold.. Value : {}", arr, threshold);
                }
                log.info("New Threshold is Updated. Before Threshold : {} and {} (SR, AS)   and New : {} and {}.  Elapsed Time : {}ms.",
                        exThresholdSR, exThresholdAS, memoryStorage.getThresholdSR(), memoryStorage.getThresholdAS(), System.currentTimeMillis() - start);

            }
        };
    }
}
