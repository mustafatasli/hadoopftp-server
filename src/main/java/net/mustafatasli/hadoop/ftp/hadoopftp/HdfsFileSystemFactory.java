package net.mustafatasli.hadoop.ftp.hadoopftp;

import org.apache.ftpserver.ftplet.FileSystemFactory;
import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;

public class HdfsFileSystemFactory implements FileSystemFactory {

	public FileSystemView createFileSystemView(User user) throws FtpException {
		return new HdfsFileSytemView(user);
	}

}
