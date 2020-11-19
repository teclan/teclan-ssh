package com.teclan.ssh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WatchDog {
    private static final Logger LOGGER = LoggerFactory.getLogger(WatchDog.class);

    private long timeOut = 5000;
    private boolean need2Stop=false;
    private Thread thread;

    public WatchDog(final long timeOut){
       this.timeOut=timeOut;
    }

    public boolean isNeed2Stop(){
        return need2Stop;
    }

    public void watch(){
        thread = new Thread(new Runnable() {
            public void run() {

                try {
                    Thread.sleep(timeOut);
                } catch (InterruptedException e) {
                    LOGGER.error(e.getMessage(),e);
                }
                need2Stop = true;
                LOGGER.debug("等待时间已经超过 {}ms，即将停止 ...",timeOut);
            }
        });
        thread.start();
    }

    public void destroy(){
        thread.interrupt();
    }
}
