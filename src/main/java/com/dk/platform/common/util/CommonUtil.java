package com.dk.platform.common.util;

import com.dk.platform.common.EppConf;
import com.dk.platform.eventWathcer.util.MemoryStorage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommonUtil {

    final MemoryStorage memoryStorage = MemoryStorage.getInstance();

    private EppConf eppConf;

    public CommonUtil() {

        eppConf = memoryStorage.getEppConf();
    }

    /**
     * Set Up Custom Thread Name.
     * @param clazzName         :           This Class Name.
     * @return                  :           formatted Thread Name( Package Name - Class Name - Thread)
     */
    // TODO THINK BETTER ==> Make Common Util?
    public String setUpThreadName(String clazzName) {

        String fullPackName = this.getClass().getPackage().getName();
        String packageName = null;
        // Tasker Case.
//        if(fullPackName.contains(AppPro.PACKAGE_TSK.getValue())) packageName = AppPro.PROCESS_TSK.getValue();
        if(fullPackName.contains(eppConf.process.getPackageTSKVal())) packageName = eppConf.process.getProcessTSKVal();
        // Manager Case.
//        if(fullPackName.contains(AppPro.PACKAGE_MNG.getValue())) packageName = AppPro.PROCESS_MNG.getValue();
        if(fullPackName.contains(eppConf.process.getPackageMNGVal())) packageName = eppConf.process.getProcessMNGVal();
        // Handler Case.
//        if(fullPackName.contains(AppPro.PACKAGE_HND.getValue())) packageName = AppPro.PROCESS_HND.getValue();
        if(fullPackName.contains(eppConf.process.getPackageHNDVal())) packageName = eppConf.process.getProcessHNDVal();

        if(fullPackName.contains(eppConf.process.getPackageWTCVal())) packageName = eppConf.process.getProcessWTCVal();

        return packageName + "-" + clazzName+"or";
    }


}
