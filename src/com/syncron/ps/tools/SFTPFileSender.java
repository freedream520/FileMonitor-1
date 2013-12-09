package com.syncron.ps.tools;

import java.nio.file.Path;

import org.apache.log4j.Logger;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

/**
 * 
 * An implementation of FileSender that uses SFTP
 * to transfer local files to a remote location.
 * @author micsie
 *
 */
public class SFTPFileSender implements FileSender {
	
	public static final Logger logger = Logger.getLogger(SFTPFileSender.class);
	
	private String user;
	private String password;
	private String host;
	private Integer port;
	
	public SFTPFileSender(String user, String password, String host) {
		initialize(user, password, host, 22);
	}
	
	public SFTPFileSender(String user, String password, String host, Integer port) {
		initialize(user, password, host, port);
	}

	private void initialize(String user, String password, String host, Integer port) {
		this.user = user;
		this.password = password;
		this.host = host;
		this.port = port;
	}
	
	@Override
	public void transferFile(Path path, String remoteDir) throws TransferFailedException {
		JSch jsch = new JSch();
        Session session = null;
        try {
            session = jsch.getSession(user, host, port);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setPassword(password);
            session.connect();

            Channel channel = session.openChannel("sftp");
            channel.connect();
            ChannelSftp sftpChannel = (ChannelSftp) channel;
            sftpChannel.cd(remoteDir);
            sftpChannel.put(path.toString(), path.getFileName().toString());
            sftpChannel.exit();
            session.disconnect();
        } catch (SftpException | JSchException e) {
            logger.error(e.getMessage(), e);
            throw new TransferFailedException(path.getFileName().toString());
        }
	}
	
}
