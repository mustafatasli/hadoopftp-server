package net.mustafatasli.hadoop.ftp.hadoopftp;

import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

/**
 * This class implements all actions to HDFS
 */
public class HdfsFtpFile implements FtpFile {

	private final Logger log = Logger.getLogger(HdfsFtpFile.class);

	private final Path path;
	private final User user;
	private final FileSystem dfs;
	private FileStatus status;

	/**
	 * Constructs HdfsFileObject from path
	 * 
	 * @param path
	 *            path to represent object
	 * @param user
	 *            accessor of the object
	 */
	public HdfsFtpFile(String path, User user, FileSystem dfs) {
		this.path = new Path(path);
		this.user = user;
		this.dfs = dfs;
		try {
			if (dfs.exists(this.path)) {
				log.info("list status for:" + path);
				this.status = dfs.getFileStatus(this.path);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getAbsolutePath() {
		String p= path.toString();
		if (p.startsWith(user.getHomeDirectory()))
			return p.substring(user.getHomeDirectory().length());
		return p;
	}

	public String getName() {
		String full = getAbsolutePath();
		int pos = full.lastIndexOf("/");
		if (full.length() == 1) {
			return "/";
		}
		return full.substring(pos + 1);
	}

	/**
	 * HDFS has no hidden objects
	 * 
	 * @return always false
	 */
	public boolean isHidden() {
		return false;
	}

	/**
	 * Checks if the object is a directory
	 * 
	 * @return true if the object is a directory
	 */
	public boolean isDirectory() {
		return status.isDir();
	}

	/**
	 * Get HDFS permissions
	 * 
	 * @return HDFS permissions as a FsPermission instance
	 * @throws IOException
	 *             if path doesn't exist so we get permissions of parent object
	 *             in that case
	 */
	private FsPermission getPermissions() throws IOException {
		// return status.getPermission();
		return dfs.getFileStatus(path).getPermission();
	}

	/**
	 * Checks if the object is a file
	 * 
	 * @return true if the object is a file
	 */
	public boolean isFile() {
		return !status.isDir();
	}

	/**
	 * Checks if the object does exist
	 * 
	 * @return true if the object does exist
	 */
	public boolean doesExist() {
		return status != null;
	}

	/**
	 * Checks if the user has a read permission on the object
	 * 
	 * @return true if the user can read the object
	 */
	public boolean isReadable() {
		try {
			FsPermission permissions = getPermissions();
			if (user.getName().equals(getOwnerName())) {
				if (permissions.toString().substring(0, 1).equals("r")) {
					log.debug("PERMISSIONS: " + path + " - "
							+ " read allowed for user");
					return true;
				}
			/*} else if (user.isGroupMember(getGroupName())) {
				if (permissions.toString().substring(3, 4).equals("r")) {
					log.debug("PERMISSIONS: " + path + " - "
							+ " read allowed for group");
					return true;
				}*/
			} else {
				if (permissions.toString().substring(6, 7).equals("r")) {
					log.debug("PERMISSIONS: " + path + " - "
							+ " read allowed for others");
					return true;
				}
			}
			log.debug("PERMISSIONS: " + path + " - " + " read denied");
			return false;
		} catch (IOException e) {
			e.printStackTrace(); // To change body of catch statement use File |
			// Settings | File Templates.
			return false;
		}
	}

	private HdfsFtpFile getParent() {
		String pathS = path.toString();
		String parentS = "/";
		int pos = pathS.lastIndexOf("/");
		if (pos > 0) {
			parentS = pathS.substring(0, pos);
		}
		return new HdfsFtpFile(parentS, user, dfs);
	}

	/**
	 * Checks if the user has a write permission on the object
	 * 
	 * @return true if the user has write permission on the object
	 */
	public boolean isWritable() {
		try {
			FsPermission permissions = getPermissions();
			log.info("Path:"+path);
			log.info("Permission:"+permissions);
			log.info("User:"+user.getName());
			log.info("Owner:"+getOwnerName());
			log.info("Group:"+getGroupName());
			if (user.getName().equals(getOwnerName())) {
				if (permissions.toString().substring(1, 2).equals("w")) {
					log.debug("PERMISSIONS: " + path + " - "
							+ " write allowed for user");
					return true;
				}
			/*} else if (user.isGroupMember(getGroupName())) {
				if (permissions.toString().substring(4, 5).equals("w")) {
					log.debug("PERMISSIONS: " + path + " - "
							+ " write allowed for group");
					return true;
				}*/
			} else {
				if (permissions.toString().substring(7, 8).equals("w")) {
					log.debug("PERMISSIONS: " + path + " - "
							+ " write allowed for others");
					return true;
				}
			}
			log.debug("PERMISSIONS: " + path + " - " + " write denied");
			return false;
		} catch (IOException e) {
			return getParent().isWritable();
		}
	}

	/**
	 * Checks if the user has a delete permission on the object
	 * 
	 * @return true if the user has delete permission on the object
	 */
	public boolean isRemovable() {
		return isWritable();
	}

	/**
	 * Get owner of the object
	 * 
	 * @return owner of the object
	 */
	public String getOwnerName() {
		if (status != null)
			return status.getOwner();
		try {
			FileStatus fs = dfs.getFileStatus(path);
			return fs.getOwner();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Get group of the object
	 * 
	 * @return group of the object
	 */
	public String getGroupName() {
		if (status!=null)
			return status.getGroup();
		try {
			FileStatus fs = dfs.getFileStatus(path);
			return fs.getGroup();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Get link count
	 * 
	 * @return 3 is for a directory and 1 is for a file
	 */
	public int getLinkCount() {
		return isDirectory() ? 3 : 1;
	}

	/**
	 * Get last modification date
	 * 
	 * @return last modification date as a long
	 */
	public long getLastModified() {
		return status.getModificationTime();
		// try {
		// FileStatus fs = dfs.getFileStatus(path);
		// return fs.getModificationTime();
		// } catch (IOException e) {
		// e.printStackTrace();
		// return 0;
		// }
	}

	public boolean setLastModified(long l) {
		return false;
	}

	/**
	 * Get a size of the object
	 * 
	 * @return size of the object in bytes
	 */
	public long getSize() {
		return status.getLen();
		// try {
		// FileStatus fs = dfs.getFileStatus(path);
		// log.info("getSize(): " + path + " : " + fs.getLen());
		// return fs.getLen();
		// } catch (IOException e) {
		// e.printStackTrace();
		// return 0;
		// }
	}

	/**
	 * Create a new dir from the object
	 * 
	 * @return true if dir is created
	 */
	public boolean mkdir() {

		if (!isWritable()) {
			log.debug("No write permission : " + path);
			return false;
		}

		try {
			
			dfs.mkdirs(path);
			
			
			dfs.setOwner(path, user.getName(), "hadoop");
			return true;
		} catch (IOException e) {
			log.error("mkdir error", e);
			return false;
		}
	}

	/**
	 * Delete object from the HDFS filesystem
	 * 
	 * @return true if the object is deleted
	 */
	public boolean delete() {
		if (!isWritable()) {
			log.debug("No write permission : " + path);
			return false;
		}
		try {
			dfs.delete(path, true);
			return true;
		} catch (IOException e) {
			log.error("delete error", e);
			return false;
		}
	}

	/**
	 * Move the object to another location
	 * 
	 * @param fileObject
	 *            location to move the object
	 * @return true if the object is moved successfully
	 */
	public boolean move(FtpFile fileObject) {
		if (!isWritable() && fileObject.isWritable()) {
			log.debug("No write permission : " + path);
			return false;
		}
		try {
			dfs.rename(path, new Path(fileObject.getAbsolutePath()));
			return true;
		} catch (IOException e) {
			log.error("move error", e);
			return false;
		}
	}

	/**
	 * List files of the directory
	 * 
	 * @return List of files in the directory
	 */
	public List<FtpFile> listFiles() {

		// checkPathPermission();
		if (!isReadable()) {
			log.debug("No read permission : " + path);
			return null;
		}

		try {
			FileStatus fileStats[] = dfs.listStatus(path);

			FtpFile fileObjects[] = new FtpFile[fileStats.length];
			for (int i = 0; i < fileStats.length; i++) {
				fileObjects[i] = new HdfsFtpFile(fileStats[i].getPath()
						.toString(), user, dfs);
			}
			return Arrays.asList(fileObjects);
		} catch (IOException e) {
			log.debug("", e);
			return null;
		}
	}

	/**
	 * Creates output stream to write to the object
	 * 
	 * @param l
	 *            is not used here
	 * @return OutputStream
	 * @throws IOException
	 */
	public OutputStream createOutputStream(long l) throws IOException {
		// if (checkPathPermission()) {
		// throw new IOException("No Permission for path:" + path);
		// }
		// permission check
		if (!isWritable()) {
			throw new IOException("No write permission : " + path);
		}

		try {
			FSDataOutputStream out = dfs.create(path);
			dfs.setOwner(path, user.getName(), "hadoop");
			return out;
		} catch (IOException e) {
			log.error("createOutputStream error", e);
			return null;
		}
	}

	/**
	 * Creates input stream to read from the object
	 * 
	 * @param l
	 *            is not used here
	 * @return OutputStream
	 * @throws IOException
	 */
	public InputStream createInputStream(long l) throws IOException {
		// if (checkPathPermission()) {
		// throw new IOException("No Permission for path:" + path);
		// }
		// permission check
		if (!isReadable()) {
			throw new IOException("No read permission : " + path);
		}
		try {
			FSDataInputStream in = dfs.open(path);
			this.status = dfs.getFileStatus(path);
			return in;
		} catch (IOException e) {
			log.error("createInputStream error", e);
			return null;
		}
	}

	// public boolean checkPathPermission() {
	// return path.toString().startsWith(user.getHomeDirectory());
	// }
}

