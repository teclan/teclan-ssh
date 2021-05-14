package com.teclan.ssh;

import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Vector;

public class SSHClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(SSHClient.class);

    private String host;
    private int port;
    private String user;
    private String password;
   private boolean openJschLog = false;
   private long timeout = 5000;// 超时默认结束等待，单位：ms

    private JSch jsch = null;
    private Session session = null;
    private ChannelExec channelExec=null;
    private ChannelSftp channelSftp = null;
    private Linehandler linehandler = null;

    public SSHClient(){
        jsch = new JSch();
    }

    public SSHClient(String host,String user,String password){
      this(host,22,user,password);
    }

    public SSHClient(String host, int port, String user, String password){
        this.host=host;
        this.port=port;
        this.user=user;
        this.password=password;

        jsch = new JSch();
    }

    /**
     * 设置服务器返回内容处理类
     * @param linehandler
     */
    public void setLinehandler(Linehandler linehandler){
        this.linehandler=linehandler;
    }

    /**
     * 设置每个命令最多等待的时间
     * @param timeout 单位：毫秒
     */
    public void setTimeout(long timeout){
        this.timeout=timeout;
    }

    /**
     * 是否开启 Jsch 的日志，默认不开启
     * @param open
     */
    public void setJschLogOpen(boolean open){
        this.openJschLog=open;
        JSch.setLogger(new com.jcraft.jsch.Logger(){
            public boolean isEnabled(int i) {
                return openJschLog;
            }
            public void log(int i, String message) {
                LOGGER.info("{}",message);
            }
        } );
    }

    public String exec(String...cmds) throws Exception {
        StringBuffer sb = new StringBuffer();
        for(String cmd:cmds){
            sb.append(cmd).append(" \n ");
        }
      return exec(sb.toString());
    }

    public String exec(String cmd) throws Exception {
        BufferedReader reader = null;
        WatchDog watchDog = new WatchDog(timeout);
        watchDog.watch();

        if(linehandler==null){
            linehandler = new DefaultLinehandler();
        }

        try {
            LOGGER.info("执行命令: {}",cmd);

            if(channelExec==null){
                channelExec = (ChannelExec)session.openChannel("exec");
            }
            channelExec.setCommand(cmd);
            channelExec.setErrStream(System.err);
            channelExec.connect();
            InputStream in = channelExec.getInputStream();
            reader = new BufferedReader(new InputStreamReader(in,
                    Charset.forName("UTF-8")));

            StringBuffer sb = new StringBuffer();
            String line = "";
            int index = 0;
            while ((line = reader.readLine()) != null) {
                if(watchDog.isNeed2Stop()){
                   LOGGER.warn("程序等待超过预设时间:{}ms，已被终止...",timeout);
                   break;
                }else {
                   sb.append(line).append("\n");
                    linehandler.handle(++index,line);
                }
            }
            LOGGER.info("\n=========================================================\n{}=========================================================",sb);
            watchDog.destroy();
            return sb.toString();
        }catch (Exception e) {
            LOGGER.error(e.getMessage(),e);
            throw e;
        }finally {
            try {
                if(reader!=null){
                    reader.close();
                }
            } catch (IOException e) {
                LOGGER.error(e.getMessage(),e);
            }
        }
    }


    public void upload(String src, String dst,SftpProgressMonitor  monitor) throws Exception {
        LOGGER.info("执行命令: scp {} {}@{}:{}",src,user,host,dst);
        try {
            if(channelSftp==null){
                channelSftp  = (ChannelSftp) session.openChannel("sftp");
            }
            if(!channelSftp.isConnected()){
                channelSftp.connect();
            }

            File file = new File(src);
            if(!file.exists()){
                throw new Exception(String.format("源文件不存在,%s",src));
            }

            if(dst.endsWith("/")){
                try{
                    channelSftp.cd(dst);
                }catch (Exception e){
                    channelSftp.mkdir(dst);
                }
                dst += new File(src).getName();
            }
            channelSftp.put(src, dst,monitor,channelSftp.OVERWRITE);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(),e);
            throw e;
        }
    }

    public void upload(String src, String dst) throws Exception {
        upload(src,dst,null);
    }


    public void download(String src, String dst) throws Exception {
        download(src,dst,null);
    }

    public void download(String src, String dst,SftpProgressMonitor  monitor) throws Exception {
        try {
            if(channelSftp==null){
                channelSftp  = (ChannelSftp) session.openChannel("sftp");
            }
            if(!channelSftp.isConnected()){
                channelSftp.connect();
            }

            String sFileName = new File(src).getName();
            if(src.endsWith("/")){
                try{
                    channelSftp.cd(src);
                }catch (Exception e){
                    LOGGER.error(e.getMessage(),e);
                    throw new Exception(String.format("[src]期望一个有效的目录，但目标机器不存在指定路径:%s",src));
                }
                new File(dst).mkdirs();

                Vector filesName = channelSftp.ls(src);
                Iterator it = filesName.iterator();
                while (it.hasNext()) {
                    String nameString = ((ChannelSftp.LsEntry) it.next()).getFilename();
                    if (".".equals(nameString) || "..".equals(nameString)) {
                        continue;
                    }

                    String s = src+"/"+nameString;
                    String d = dst+"/"+nameString;

                    try{
                        channelSftp.cd(s);
                        s += "/";
                        download(s,d,monitor);
                    }catch (Exception e){
                        downloanSingleFile(s,d,monitor);
                    }
                }
            }else if(sFileName.contains("*")){
                String tmp="";
                try{
                    tmp = new File(src).getParent()+"/".replace("\\","/");
                    tmp = tmp.replace("\\","/");
                    channelSftp.cd(tmp);
                }catch (Exception e){
                    LOGGER.error(e.getMessage(),e);
                    throw new Exception(String.format("目标机器不存在指定路径:%s",tmp));
                }
                new File(dst).mkdirs();
                Vector filesName = channelSftp.ls(src);
                Iterator it = filesName.iterator();

                if(!it.hasNext()){
                    throw new Exception(String.format("目标机器不存在指定路径:%s",tmp));
                }


                while (it.hasNext()) {
                    String nameString = ((ChannelSftp.LsEntry) it.next()).getFilename();
                    if (".".equals(nameString) || "..".equals(nameString)) {
                        continue;
                    }

                    String fileNamePrefix = sFileName.substring(0,sFileName.indexOf("*"));
                    if(!nameString.contains(fileNamePrefix)){
                        continue;
                    }

                    String s = tmp+"/"+nameString;
                    String d = dst+"/"+nameString;

                    try{
                        channelSftp.cd(s);
                        s += "/";
                        download(s,d,monitor);
                    }catch (Exception e){
                        downloanSingleFile(s,d,monitor);
                    }
                }
            }else{
                downloanSingleFile(src,dst,monitor);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(),e);
            throw e;
        }
    }

    public void downloanSingleFile(String src, String dst,SftpProgressMonitor  monitor) throws Exception {
        File file = new File(dst);
        if(file.isDirectory()){
            dst = dst+new File(src).getName();
            file = new File(dst);
        }

        try{

        }catch (Exception e){
            return;
        }

        LOGGER.info("执行命令: scp {}@{}:{}  {}",user,host,src,dst);
        file.getParentFile().mkdirs();
        file.createNewFile();
        channelSftp.get(src, dst,monitor,channelSftp.OVERWRITE);
    }


    /***
     * 检查文件是否存在
     * @param directory 文件在服务器的路径
     * @param fileName  文件名称
     */
    private boolean findFile(String directory, String fileName) throws Exception {
        try {
            // 得到所有文件
            Vector filesName = channelSftp.ls(directory);
            Iterator it = filesName.iterator();
            while (it.hasNext()) {
                String nameString = ((ChannelSftp.LsEntry) it.next()).getFilename();
                if (".".equals(nameString) || "..".equals(nameString)) {
                    continue;
                }
                if (fileName.equals(nameString)) {
                    return true;
                }
            }
        } catch (Exception e) {
            throw new Exception(e.getMessage(), e);
        }
        return false;
    }

    public void login(){
        try {
            //给出连接需要的用户名，ip地址以及端口号
            session=jsch.getSession(user, host, port);
            //第一次登陆时候，是否需要提示信息，value可以填写 yes，no或者是ask
            session.setConfig("StrictHostKeyChecking", "no");
            //设置是否超时
            session.setTimeout(10000);
            //设置密码
            session.setPassword(password);
            //创建连接
            session.connect();
            LOGGER.info("已登录...");
        } catch (Exception e) {
            LOGGER.error(e.getMessage(),e);
        }
    }

    public void logout(){
        if(session!=null){
            session.disconnect();
        }
        if(channelExec!=null && channelExec.isConnected()){
            channelExec.disconnect();
        }
        if(channelSftp!=null && channelSftp.isConnected()){
            channelSftp.disconnect();
        }
       jsch = null;
       linehandler = null;
       session = null;
       LOGGER.info("已退出...");
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
