import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.nio.file.*;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.commons.net.ftp.*;

import static java.nio.file.StandardWatchEventKinds.*;
import static java.nio.file.LinkOption.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
 
public class FileMonitor implements Runnable {
 
    private final WatchService watcher;
    private final Map<WatchKey,Path> keys;
    private boolean running = true;
    private boolean recursive = false;
    private String ftp_username;
    private String ftp_password;
    private String ftp_server;
    private String fileInputFolder;
    private LinkedList<Path> backlog;
 
	static Logger logger = Logger.getLogger(FileMonitor.class.getName());
    
    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>)event;
    }
 
    /**
     * Register the given directory with the WatchService
     */
    private void register(Path dir) throws IOException {
        WatchKey key = dir.register(watcher, ENTRY_CREATE);
        keys.put(key, dir);
    }
 
    /**
     * Register the given directory, and all its sub-directories, with the
     * WatchService.
     */
    private void registerAll(final Path start) throws IOException {
        // register directory and sub-directories
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                throws IOException
            {
                register(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
 
    /**
     * Creates a WatchService and registers the given directory
     */
    public FileMonitor(String strDir, boolean recursive) throws IOException {
    	Path dir = Paths.get(strDir);
        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<WatchKey,Path>();
        this.recursive = recursive;
        backlog = new LinkedList<Path>();
        
        Properties props = new Properties();
		try {
			props.load(new FileInputStream(System.getProperty("user.dir") + "/../conf/FileMon.properties"));
			String log4jProp = props.getProperty("log4j_properties");
			PropertyConfigurator.configure(log4jProp);
			ftp_server = props.getProperty("ftp_server");
			ftp_username = props.getProperty("ftp_user");
			ftp_password = props.getProperty("ftp_password");
			fileInputFolder = props.getProperty("remote_input_dir");
		} catch (FileNotFoundException e) {
			System.err.println("Could not load properties file \"FileMon.properties\" from the ../conf folder");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (recursive) {
			registerAll(dir);
		} else {
			register(dir);
		}
        
    }
 
    /**
     * Process all events for keys queued to the watcher
     */
    public void processEvents() {
        Thread t = new Thread(this);
        t.start();
    }
    
    public void stop () {
    	running = false;
    }

	@Override
	public void run() {
		while(running) {
			 
            // wait for key to be signalled
            WatchKey key;
            try {
            	if (backlog.size() > 0) {
            		key = watcher.poll(5000, TimeUnit.MILLISECONDS);
            	} else {
            		key = watcher.take();
            	}
            } catch (InterruptedException x) {
                return;
            }
 
            Path dir = keys.get(key);
            if (dir == null && backlog.size() == 0) {
                System.err.println("WatchKey not recognized!!");
                continue;
            } else if (dir == null) {
            	dir = backlog.pop();
            	try {
            		ftpStoreFile(dir);
            	} catch (Exception e) {
            		logger.error("General error: ", e);
            	}
            	continue;
            }
 
            for (WatchEvent<?> event: key.pollEvents()) {
                WatchEvent.Kind kind = event.kind();
 
                // TBD - provide example of how OVERFLOW event is handled
                if (kind == OVERFLOW) {
                    continue;
                }
 
                // Context for directory entry event is the file name of entry
                WatchEvent<Path> ev = cast(event);
                Path name = ev.context();
                
                Path child = dir.resolve(name);

                if (recursive && Files.isDirectory(child, NOFOLLOW_LINKS)) {
                    try {
                        if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                            registerAll(child);
                        }
                    } catch (IOException x) {
                        // ignore to keep sample readbale
                    }
                } else  if (kind == ENTRY_CREATE) {
                	try {
                		ftpStoreFile(child);
                	} catch (Exception e) {
                		logger.error("General error: ", e);
                	}
                }
            }
 
            // reset key and remove from set if directory no longer accessible
            boolean valid = key.reset();
            if (!valid) {
                keys.remove(key);
 
                // all directories are inaccessible
                if (keys.isEmpty()) {
                    break;
                }
            }
        }		
	}
	
	private void ftpStoreFile(Path file) throws Exception {
		//"C:/SCP/INPUT/LOCATION/BACKUP/file.fil"
		String location = "";
		String parentFolder = "";
		int parentSlash = file.toString().lastIndexOf(File.separatorChar);
		int endSlash = file.toString().lastIndexOf(File.separatorChar, parentSlash-1);
		int startSlash = file.toString().lastIndexOf(File.separatorChar, endSlash-1);
		if (parentSlash != endSlash) {
			parentFolder = file.toString().substring(endSlash+1, parentSlash);
		}
		if (endSlash != startSlash) {
			location = file.toString().substring(startSlash+1, endSlash) + "/";
		}
					
		if (parentFolder.trim().equalsIgnoreCase("BACKUP")) {
			FTPClient f = new FTPClient();
	    	FileInputStream fis = null;
	    	try {
	        	f.connect(ftp_server);
	        	String abc = f.getReplyString();
	            f.login(ftp_username, ftp_password);
	            abc = f.getReplyString();
	            logger.info("File: " + file.getFileName().toString() + ", status: " + abc);
	            fis = new FileInputStream(file.toString());
	            boolean success = f.storeFile("temp/" + file.getFileName().toString(), fis);
	            abc = f.getReplyString();
	            logger.info("File: " + file.getFileName().toString() + ", status: " + abc);
	            success = f.rename("temp/" + file.getFileName().toString(), location + file.getFileName().toString());
	            abc = f.getReplyString();
	            f.logout();
	            logger.info("File: " + file.getFileName().toString() + ", status: " + abc);
	    	} catch (IOException e) {
	    		if (e instanceof FileNotFoundException) {
	    			logger.warn("The file (" + location + file.getFileName().toString() + ") is locked by the filesystem, retrying later (5 sec intervals)");
	    			backlog.add(file);
	    			return;
	    		} else {
	    			logger.error("Could not upload to server: " + e.getMessage() + " \n File: " + file.toString() + "\n Remote file: temp/" + file.getFileName().toString() + "\n Renamed: " + location + file.getFileName().toString());
	    		}
	    	} finally {
	    		try {
	    			if (fis != null) {
	    				fis.close();
	    			}
	    			f.disconnect();
	    		} catch (IOException e) {
	    			logger.error("Could not close connection to ftp server: " + e.getMessage());
	    		}
	        }
		}
	}

	public void setFileInputFolder(String fileInputFolder) {
		this.fileInputFolder = fileInputFolder;
		if (fileInputFolder.endsWith("/")) {
			fileInputFolder = fileInputFolder + "/";
		}
	}

}
