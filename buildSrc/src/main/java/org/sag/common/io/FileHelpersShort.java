package org.sag.common.io;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.Security;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Stream;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.GlobalMemory;

public class FileHelpersShort {
	
	public static final Path getPath(String first, String... more){
		return getNormAndAbsPath(Paths.get(first, more));
	}
	
	public static final Path getPath(Path first, String... more){
		if(first == null)
			throw new IllegalArgumentException("A first path is required.");
		if(more == null || more.length == 0){
			return getNormAndAbsPath(first);
		}else{
			return getNormAndAbsPath(Paths.get(first.toString(), more));
		}
	}
	
	public static final Path getPath(Path first, Path... more){
		if(first == null)
			throw new IllegalArgumentException("A first path is required.");
		if(more == null || more.length == 0){
			return getNormAndAbsPath(first);
		}else{
			String[] temp = new String[more.length];
			for(int i = 0; i < more.length; i++){
				temp[i] = more[i].toString();
			}
			return getNormAndAbsPath(Paths.get(first.toString(), temp));
		}
	}
	
	public static final Path getNormAndAbsPath(Path path){
		if(path == null)
			throw new IllegalArgumentException("A path is required.");
		return path.toAbsolutePath().normalize().toAbsolutePath();
	}
	
	/** Returns 1 is the path is a directory, 0 if the path is a file, and throws a
	 * IOException if the path does not exist, is not read and writable, or is not a file
	 * or directory.
	 */
	public static final int verifyRWExists(String first, String... more) throws IOException {
		return verifyRWExists(getPath(first,more));
	}
	
	/** Returns 1 is the path is a directory, 0 if the path is a file, and throws a
	 * IOException if the path does not exist, is not read and writable, or is not a file
	 * or directory.
	 */
	public static final int verifyRWExists(Path path) throws IOException {
		int temp = checkRWExists(path);
		if(temp == -1)
			throw new IOException("Error: '" + path.toString() + 
					"' either does not exist, is not R/W able, or is not a directory/file.");
		return temp;
	}
	
	/** Returns 1 is the path is a directory, 0 if the path is a file, and -1
	 * if the path does not exist, is not read and writable, or is not a file
	 * or directory.
	 */
	public static final int checkRWExists(String first, String... more) {
		return checkRWExists(getPath(first,more));
	}
	
	/** Returns 1 is the path is a directory, 0 if the path is a file, and -1
	 * if the path does not exist, is not read and writable, or is not a file
	 * or directory.
	 */
	public static int checkRWExists(Path path) {
		if(Files.exists(path) && Files.isReadable(path) && Files.isWritable(path)) {
			if(Files.isDirectory(path))
				return 1;
			else if(Files.isRegularFile(path))
				return 0;
			else
				return -1;
		}
		return -1;
	}
	
	public static final void verifyRWDirectoryExists(String first, String... more) throws IOException {
		verifyRWDirectoryExists(getPath(first,more));
	}
	
	public static final boolean checkRWDirectoryExists(String first, String... more){
		return checkRWDirectoryExists(getPath(first,more));
	}
	
	public static final void verifyRWDirectoryExists(Path path) throws IOException {
		if(!checkRWDirectoryExists(path)){
			throw new IOException("Error: Directory '" + path.toString() + 
					"' either does not exist, is not a directory, or is not R/W able.");
		}
	}
	
	public static final boolean checkRWDirectoryExists(Path path) {
		return Files.exists(path) && Files.isDirectory(path) && Files.isReadable(path) && Files.isWritable(path);
	}
	
	public static final void verifyRWFileExists(String first, String... more) throws IOException {
		verifyRWFileExists(getPath(first,more));
	}
	
	public static final boolean checkRWFileExists(String first, String... more){
		return checkRWFileExists(getPath(first,more));
	}
	
	public static final void verifyRWFileExists(Path filePath) throws IOException {
		if(!checkRWFileExists(filePath)){
			throw new IOException("Error: File '" + filePath.toString() + 
					"' either does not exist, is not a file, or is not R/W able.");
		}
	}
	
	public static final boolean checkRWFileExists(Path path){
		return Files.exists(path) && Files.isRegularFile(path) && Files.isReadable(path) && Files.isWritable(path);
	}
	
	public static final long testMemory() {
		com.sun.management.OperatingSystemMXBean os = (com.sun.management.OperatingSystemMXBean)java.lang.management.ManagementFactory.getOperatingSystemMXBean();
		return os.getTotalPhysicalMemorySize();
	}
	
	public static final long getTotalSystemMemory() {
		SystemInfo si = new SystemInfo();
		HardwareAbstractionLayer hal = si.getHardware();
		GlobalMemory gmem = hal.getMemory();
		return gmem.getTotal();
	}
	
	public static final long getAvailableSystemMemory() {
		SystemInfo si = new SystemInfo();
		HardwareAbstractionLayer hal = si.getHardware();
		GlobalMemory gmem = hal.getMemory();
		return gmem.getAvailable();
	}
	
}
