package net.mustafatasli.hadoop.ftp.hadoopftp;

import java.io.File;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.filesystem.nativefs.NativeFileSystemFactory;
import org.apache.ftpserver.ftplet.FileSystemFactory;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.ClearTextPasswordEncryptor;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.impl.PropertiesUserManager;


public class App 
{
    public static void main( String[] args )
    {
    	FtpServerFactory serverFactory = new FtpServerFactory();
    	ListenerFactory factory = new ListenerFactory();
    	factory.setPort(2221);
    	serverFactory.addListener("default", factory.createListener());
    	
    	PropertiesUserManagerFactory um = new PropertiesUserManagerFactory();
    	um.setFile(new File("user.properties"));
    	um.setPasswordEncryptor(new ClearTextPasswordEncryptor());
    	
    	serverFactory.setUserManager(um.createUserManager());
    	//serverFactory.setFileSystem(new NativeFileSystemFactory());
    	serverFactory.setFileSystem(new HdfsFileSystemFactory());
  
    	// start the server
    	FtpServer server = serverFactory.createServer(); 
    	try {
			server.start();
		} catch (FtpException e) {
			e.printStackTrace();
		}
    }
}
