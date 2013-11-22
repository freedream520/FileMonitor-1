import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;


public class FileMonStarter {
	public static void main(String[] args) {
		FileMonitor fM = null;
		Properties props = new Properties();
		try {
			props.load(new FileInputStream(System.getProperty("user.dir") + "/../conf/FileMon.properties"));
			String log4jProp = props.getProperty("log4j_properties");
			PropertyConfigurator.configure(log4jProp);
			String watchDir = props.getProperty("watch_dir");
			fM = new FileMonitor(watchDir, true);
		} catch (FileNotFoundException e) {
			System.err.println("Could not load properties file \"FileMon.properties\" from the ../conf folder");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		fM.processEvents();
	}
}
