package com.teclan.ssh;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

public class SSHClientSftpTest {
    SSHClient sshClient = null;


    @Test
    public void downloanTest() throws Exception {
        sshClient.download("/home/teclan/Chrome_72.0.3626.109.rar","E:\\");
    }

    @Test
    public void downloanTest1() throws Exception {
        sshClient.download("/home/teclan/test1/","E:\\remote\\");
    }

    @Test
    public void uploadTest() throws Exception {
        sshClient.upload("E:\\Apps\\apache-maven-3.6.3-bin.zip","/home/teclan/123.zip");
    }

    @Test
    public void uploadTest1() throws Exception {
        String src = "E:\\Apps\\Chrome_72.0.3626.109.rar";
        sshClient.upload(src,"/home/teclan/",new DefaultSftpProgressMonitor(new File(src)));// 指定文件上传监视器
    }

    @Before
    public void setUp(){
        sshClient = new SSHClient("localhost","root","123456");
        sshClient.setJschLogOpen(true); // 启用Jsch日志，打印 ssh 连接信息等
        sshClient.setLinehandler(new DefaultLinehandler()); // 设置服务返回的每一行的处理类
        sshClient.setTimeout(3000);// 设置命令最大的等待时间，例如； top -b 命令会持续输出
        sshClient.login();
    }

    @After
    public void setDown() {
        sshClient.logout();
    }
}
