package com.dk.platform.eventWathcer.process;

import com.dk.platform.common.EppConf;
import com.dk.platform.common.EventPlatformProcess;
import com.dk.platform.common.util.CommonUtil;
import com.dk.platform.common.util.EmsUtil;
import com.dk.platform.eventWathcer.util.MemoryStorage;
import com.dk.platform.eventWathcer.util.WatcherUtil;
import com.tibco.tibjms.admin.TibjmsAdminException;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.C;

@Slf4j
public class InitializeProcess implements EventPlatformProcess {

    private EmsUtil emsUtil;

    private String emsServerUrl;

    private String emsUserName;

    private String emsPassword;

    private EppConf eppConf;

    /*
     ***********************************  Constructor ****************************************
     ****************************************************************************************/

    public InitializeProcess(String filePath){

        eppConf = new EppConf(filePath);

        emsServerUrl = eppConf.ems.getUrlVal();

        emsUserName = eppConf.ems.getUsrVal();

        emsPassword = eppConf.ems.getPwdVal();

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
    public void setUpInstance() {

        /*
         ***********************************  Variables ******************************************
         ****************************************************************************************/
        MemoryStorage memoryStorage = MemoryStorage.getInstance();

        try {
            this.emsUtil = new EmsUtil(this.emsServerUrl, this.emsUserName, this.emsPassword);
            memoryStorage.setEmsUtil(this.emsUtil);

            memoryStorage.setEppConf(eppConf);

            CommonUtil commonUtil = new CommonUtil();
            memoryStorage.setCommonUtil(commonUtil);

        } catch (TibjmsAdminException e) {

            log.error("[{}] Cannot Get EmsUtil Instance. Exit System. Error : {}/{}.","TaskerInitialize", e.getMessage(), e.toString());
            e.printStackTrace();
            System.exit(1);
        }

        /**
         * Watcher Util.
         */
        WatcherUtil watcherUtil = new WatcherUtil();
        memoryStorage.setWatcherUtil(watcherUtil);


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
        log.info("InitProcess Execute");

    }
}
