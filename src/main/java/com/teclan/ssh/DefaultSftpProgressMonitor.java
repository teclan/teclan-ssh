package com.teclan.ssh;

import com.jcraft.jsch.SftpProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.DecimalFormat;

public class DefaultSftpProgressMonitor implements SftpProgressMonitor {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSftpProgressMonitor.class);
    private static final DecimalFormat DF = new DecimalFormat("#.00");
    private String fileSize = "0";
    private long count = 0L;
    private long length = 0;
    private double percent = 0;

    public DefaultSftpProgressMonitor(long length) {
        this.length = length;
        this.fileSize = DF.format(length * 1.0 / 1024 / 1024);
    }

    public DefaultSftpProgressMonitor(File file) {
        this.length = file.length();
        this.fileSize = DF.format(length * 1.0 / 1024 / 1024);
    }

    public void init(int i, String s, String s1, long l) {

    }

    public boolean count(long count) {
        this.count += count;
        percent = this.count * 100.0 / length;
        LOGGER.info("已完成{}%,文件总大小:{}M", DF.format(percent), fileSize);
        return true;
    }

    public void end() {
    }
}
