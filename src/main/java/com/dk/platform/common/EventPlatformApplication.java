package com.dk.platform.common;

/**
 * Step to Run Application
 * 1. run initialize process, init EppConf
 * 2. Set-Up MemoryStorage
 * 3. Create Instance of Process.
 * 4. do process.setUpInstance
 * 5. setActive(optional)
 */
public interface EventPlatformApplication {

    void initialize(String filePath);
}
