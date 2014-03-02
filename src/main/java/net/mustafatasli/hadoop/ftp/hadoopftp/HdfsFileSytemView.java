package net.mustafatasli.hadoop.ftp.hadoopftp;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.conf.Configuration;

/**
 * Implemented FileSystemView to use HdfsFileObject
 */
public class HdfsFileSytemView implements FileSystemView {

	// the first and the last character will always be '/'
	// It is always with respect to the root directory.
	private String currDir = "/";

	private User user;
	private final DistributedFileSystem dfs;

	/**
	 * Constructor - set the user object.
	 */
	protected HdfsFileSytemView(User user) throws FtpException {
		if (user == null) {
			throw new IllegalArgumentException("user can not be null");
		}
		if (user.getHomeDirectory() == null) {
			throw new IllegalArgumentException(
					"User home directory can not be null");
		}
		
		this.dfs = new DistributedFileSystem();
		this.initHdfs();
		
		try {
			if (dfs.exists(new Path(user.getHomeDirectory()))) {
				dfs.mkdirs(new Path(user.getHomeDirectory()));
			}
		} catch (IOException e) {
			throw new FtpException(e);
		}

		this.currDir = user.getHomeDirectory();
		this.user = user;
	}

	private void initHdfs() {
		Configuration conf = new Configuration();
		try {
			dfs.initialize(new URI("http://192.168.3.11:9000"), conf);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Get the user home directory. It would be the file system root for the
	 * user.
	 */
	public FtpFile getHomeDirectory() {
		return new HdfsFtpFile(user.getHomeDirectory(), user, dfs);
	}

	/**
	 * Get the current directory.
	 */
	public FtpFile getWorkingDirectory() {
		return new HdfsFtpFile(currDir, user, dfs);
	}

	/**
	 * Get file object.
	 */
	public FtpFile getFile(String file) {
		String path;
		if (file.startsWith("/")) {
			if (file.startsWith(user.getHomeDirectory())) {
				path = file;
			} else {
				path = user.getHomeDirectory() + file;
			}
		} else if (currDir.length() > 1) {
			path = currDir + "/" + file;
		} else {
			path = "/" + file;
		}
		return new HdfsFtpFile(path, user, dfs, null);
	}

	/**
	 * Change directory.
	 */
	public boolean changeWorkingDirectory(String dir) {
		String path;
		if (dir.startsWith("/")) {
			if (dir.startsWith(user.getHomeDirectory()))
				path = dir;
			else
				path = user.getHomeDirectory() + dir;

		} else if (dir.equals("..")) {
			if (!new Path(currDir).toString().equals("/")) {
				path = currDir + "/..";
			} else {
				path = "/";
			}
		} else if (dir.startsWith("~")) {
			path = user.getHomeDirectory() + dir.substring(1);
		} else if (currDir.length() > 1) {
			path = currDir + "/" + dir;
		} else {
			path = "/" + dir;
		}
		HdfsFtpFile file = new HdfsFtpFile(path, user, dfs);
		if (file.isDirectory()
				&& file.isReadable()
				&& new Path(path).toString()
						.startsWith(user.getHomeDirectory())) {
			currDir = path;
		} else if (user.getHomeDirectory().startsWith(path.toString())) {
			currDir = user.getHomeDirectory();
		}
		return true;
	}

	/**
	 * Is the file content random accessible?
	 */
	public boolean isRandomAccessible() {
		return true;
	}

	/**
	 * Dispose file system view - does nothing.
	 */
	public void dispose() {
	}
}
