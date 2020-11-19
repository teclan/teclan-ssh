package com.teclan.ssh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultLinehandler implements Linehandler{

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultLinehandler.class);

    public void handle(int index,String line) {
        LOGGER.info("服务器返回行：{}，内容：{}",index,line);
    }
}
