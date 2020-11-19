package com.teclan.ssh;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SSHClientShellTest {
    SSHClient sshClient = null;

    @Test
    public void testLs() throws Exception {
        sshClient.exec("ls /");
    }

    @Test
    public void testTop() throws Exception {
        sshClient.exec("top -b");
    }

    @Test
    public void testCmds() throws Exception {
        sshClient.exec("cd /home","ls -l");
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
