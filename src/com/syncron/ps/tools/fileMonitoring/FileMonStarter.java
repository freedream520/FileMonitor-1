package com.syncron.ps.tools.fileMonitoring;
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


/**
 * 
 * This class initializes the logger, sender and monitored directories
 * based on the property file passed as an argument to the method
 * <code>main</code>. If not provided, <code>DEFAULT_PROPERTY_FILE_PATH</code>
 * is used.
 * @author micsie
 *
 */
public class FileMonStarter {
	
	public static final Logger logger = Logger.getLogger(FileMonStarter.class);
	
	/**
	 * The name of the property containing a user name.
	 */
	private static final String USER = "user";
	/**
	 * The name of the property containing a password.
	 */
	private static final String PASSWORD = "password";
	/**
	 * The name of the property containing a host name or an IP address.
	 */
	private static final String HOST = "host";
	/**
	 * The name of the property containing a port number.
	 */
	private static final String PORT = "port";
	
	/**
	 * The default path to the properties file.
	 */
	private static final String DEFAULT_PROPERTY_FILE_PATH = "conf/properties.xml";
	
	private static final String XPATH_LOCAL_DIRECTORY = "@local";
	private static final String XPATH_REMOTE_DIRECTORY = "@remote";
	private static final String XPATH_RECURSIVE = "@recursive";
	private static final String XPATH_CONNECTION_DETAILS = "/properties/connection";
	private static final String XPATH_LOG_FILE_PATH = "/properties/log/@configFile";
	private static final String XPATH_DIR = "/properties/dir";
	private static final String XPATH_MASK = "mask";
	
	/**
	 * The directories monitored by the application.
	 */
	private Map<Path, MonitoredDirectory> monitoredDirectories;
	
	/**
	 * This static method creates a new object of the class and
	 * executes <code>start</code> method with a property file path
	 * based on the application arguments. If no argument passed,
	 * {@link FileMonStarter#DEFAULT_PROPERTY_FILE_PATH} used as default.
	 * @param args	The applications arguments.
	 */
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
	
	/**
	 * This method reads the property file located at {@link propertyFilePath}.
	 * Based on the properties, it initializes logs, sender and monitored
	 * directories. Finally the file monitor is created and the processing
	 * of events is initiated.
	 * @param propertyFilePath	A path to the properties file.
	 */
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
	 * Gets an SFTP file sender based on properties in a document.
	 * If property {@value #PORT} not provided, by default the port number
	 * is set to 22.
	 * @param document		An XML document describing connection details
	 * @return				SFTP file sender.
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
	
	/**
	 * Initializes monitored directories based on properties in a document.
	 * @param document		An XML document containing monitored directories.
	 * @throws IOException
	 */
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
	
	/**
	 * This method registers a monitored directory based on the directory
	 * path, remote directory path string, and list of masks.
	 * If {@code recursive} is {@code true}, then all local sub-directories
	 * are registered with corresponding sub-directories on the remote side.
	 * E.g., if {@code /path} that maps to {@code /remote} is registered
	 * and it contains {@code /path/sub} folder, then {@code /path/sub} is
	 * registered and maps to {@code /remote/sub} directory.
	 * <br />
	 * TODO: inferring remote sub-directories are implemented by appending
	 * string literals {@literal /} and sub-folders names. It probably should
	 * be changed.
	 * @param path				The path to the registered directory.
	 * @param remoteDirectory	The corresponding remote directory.
	 * @param maskNodes			XML nodes describing filter masks.
	 * @param recursive			If {@code true}, sub-folders will be registered
	 * 							as well.
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
	
	/**
	 * Initialize the logger configuration.
	 * @param document	The XML document containing the path to the log4j
	 * 					configuration file.
	 */
	private void initializeLog(Document document) {
		String configFile = document.valueOf(XPATH_LOG_FILE_PATH);
		PropertyConfigurator.configure(configFile);
		logger.info("Logger initialized successfully.");
	}
}
