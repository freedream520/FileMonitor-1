package com.syncron.ps.tools;
import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;


public class FileMonStarter {
	
	public static final Logger logger = Logger.getLogger(FileMonStarter.class);
	
	public static final String USER = "user";
	public static final String PASSWORD = "password";
	public static final String HOST = "host";
	public static final String PORT = "port";
	
	public static final String DEFAULT_PROPERTY_FILE_PATH = "conf/properties.xml";
	
	public static final String XPATH_LOCAL_DIRECTORY = "@local";
	public static final String XPATH_REMOTE_DIRECTORY = "@remote";
	public static final String XPATH_RECURSIVE = "@recursive";
	public static final String XPATH_CONNECTION_DETAILS = "/properties/connection";
	public static final String XPATH_LOG_FILE_PATH = "/properties/log/@configFile";
	public static final String XPATH_DIR = "/properties/dir";
	public static final String XPATH_MASK = "mask";
	
	private Map<Path, MonitoredDirectory> monitoredDirectories;
	
	public static void main(String[] args) {
		String propertyFilePath;
		if (args.length > 0) {
			propertyFilePath = args[0];
		}
		else {
			propertyFilePath = DEFAULT_PROPERTY_FILE_PATH;
		}
		FileMonStarter starter = new FileMonStarter();
		starter.start(propertyFilePath);
	}
	
	public void start(String propertyFilePath) {
		try {
			SAXReader reader = new SAXReader();
			Document document = reader.read(propertyFilePath);
			
			// Initialize logs
			initializeLog(document);
			
			// Initialize the file sender
			FileSender sender = getSFTPFileSender(document);
			
			// Get monitored directories
			initMonitoredDirectories(document);
			
			// Create FileMonitor
			FileMonitor fileMonitor = new FileMonitor(monitoredDirectories, sender);
			
			// Run the monitor
			fileMonitor.processEvents();
			
		} catch (DocumentException | ConnectionDetailsException | IOException e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	/**
	 * Gets an SFTP file sender based on properties in a container.
	 * @param properties	XML container describing connection details
	 * @return				SFTP file sender
	 * @throws ConnectionDetailsException
	 */
	private SFTPFileSender getSFTPFileSender(Document document) throws ConnectionDetailsException {
		
		Node properties = document.selectSingleNode(XPATH_CONNECTION_DETAILS);
		Node node;
		
		node = properties.selectSingleNode(USER);
		if (node == null) throw new ConnectionDetailsException(USER);
		String user = node.getText();
		
		node = properties.selectSingleNode(PASSWORD);
		if (node == null) throw new ConnectionDetailsException(PASSWORD);
		String password = node.getText();
		
		node = properties.selectSingleNode(HOST);
		if (node == null) throw new ConnectionDetailsException(HOST);
		String host = node.getText();
		
		Integer port = 22;
		node = properties.selectSingleNode(PORT);
		if (node != null) {
			try {
				Integer.parseInt(node.getText());
			} catch (NumberFormatException e) {
				logger.warn("Couldn't parse the port number. Using default: " + port.toString());
			}
		}
		
		return new SFTPFileSender(user, password, host, port);
	}
	
	private void initMonitoredDirectories(Document document) throws IOException {
		
		monitoredDirectories = new HashMap<Path, MonitoredDirectory>();
		
		@SuppressWarnings("unchecked")
		List<Node> dirNodes = document.selectNodes(XPATH_DIR);
		
		for (Node dirNode : dirNodes) {
			
			String localDirectory = dirNode.valueOf(XPATH_LOCAL_DIRECTORY);
			String remoteDirectory = dirNode.valueOf(XPATH_REMOTE_DIRECTORY);
			boolean recursive =	Boolean.parseBoolean(dirNode.valueOf(XPATH_RECURSIVE));
			
			@SuppressWarnings("unchecked")
			List<Node> maskNodes = dirNode.selectNodes(XPATH_MASK);
			
			registerDirectory(Paths.get(localDirectory), remoteDirectory,
					maskNodes, recursive);
		}
		
	}
	
	/*
	 * TODO Recursive registration needs to be improved.
	 */
	private void registerDirectory(Path path, String remoteDirectory,
			List<Node> maskNodes, boolean recursive) {
		
		logger.debug("Registering mapping from " + path.toString() +
				" to " + remoteDirectory);
		
		MonitoredDirectory mapping = new MonitoredDirectory(path.toString(), remoteDirectory);
		monitoredDirectories.put(path, mapping);
		for (Node maskNode : maskNodes) {
			String mask = maskNode.getText();
			mapping.registerMask(mask);
		}
		
		if (recursive) {
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
			    for (Path file: stream) {
			    	if (Files.isDirectory(file, LinkOption.NOFOLLOW_LINKS))
				        registerDirectory(file,
				        		remoteDirectory + "/" + file.getFileName(),
				        		maskNodes,
				        		recursive);
			    }
			} catch (IOException | DirectoryIteratorException x) {
				logger.error("Error while registering directories recursively", x);
			}
		}
	}
	
	private void initializeLog(Document document) {
		String configFile = document.valueOf(XPATH_LOG_FILE_PATH);
		PropertyConfigurator.configure(configFile);
		logger.info("Logger initialized successfully.");
	}
}
