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

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import org.sag.common.io.walker.MTDeleteDirectoryTask;
import org.sag.common.io.walker.MTDeleteEmptyDirectoryTask;
import org.sag.common.io.walker.MTFileTreeWalker;
import org.sag.common.io.walker.MTFindTask;

public class FileHelpers {
	
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
	
	public static final List<Path> getDirectoryEntries(String first, String... more) throws IOException {
		return getDirectoryEntries(getPath(first, more));
	}
	
	public static final List<Path> getDirectoryEntries(Path path) throws IOException {
		verifyRWDirectoryExists(path);
		DirectoryStream<Path> ds = Files.newDirectoryStream(path);
		ArrayList<Path> entries = new ArrayList<>();
		try{
			for(Path entry : ds){
				entries.add(getNormAndAbsPath(entry));
			}
		} catch (DirectoryIteratorException ex) {
			// I/O error encounted during the iteration, the cause is an IOException
			throw ex.getCause();   
		} finally {
			ds.close();
		}
		return entries;
	}
	
	public static final List<Path> getAllDirectoryEntries(String first, String... more) throws IOException {
		return getAllDirectoryEntries(getPath(first, more));
	}
	
	public static final List<Path> getAllDirectoryEntries(Path path) throws IOException {
		verifyRWDirectoryExists(path);
		List<Path> ret = new ArrayList<>();
		Queue<Path> tovisit = new ArrayDeque<>();
		tovisit.add(path);
		while(!tovisit.isEmpty()) {
			Path cur = tovisit.poll();
			try(DirectoryStream<Path> ds = Files.newDirectoryStream(cur)) {
				for(Path entry : ds) {
					if(Files.isDirectory(entry)) {
						tovisit.add(entry);
					}
					ret.add(getNormAndAbsPath(entry));
				}
			} catch(DirectoryIteratorException ex) {
				// I/O error encounted during the iteration, the cause is an IOException
				throw ex.getCause();
			}
		}
		return ret;
	}
	
	public static final void cleanDirectory(String first, String... more) throws IOException {
		cleanDirectory(getPath(first,more));
	}
	
	public static final void cleanDirectory(Path path) throws IOException {
		verifyRWDirectoryExists(path);
		try{
			List<Path> entries = getDirectoryEntries(path);
			for(Path entry : entries){
				if(Files.isDirectory(entry)){
					removeDirectory(entry);
				}else{
					verifyRWFileExists(entry);
					Files.delete(entry);
				}
			}
		} catch (Throwable t){
			throw new IOException("Error: Failed to clean directory '" + path.toString() + "'.",t);
		}
	}
	
	public static final void removeDirectory(String first, String... more) throws Exception {
		removeDirectory(getPath(first,more));
	}
	
	public static final void removeDirectory(Path path) throws Exception {
		verifyRWDirectoryExists(path);
		MTDeleteDirectoryTask startTask = new MTDeleteDirectoryTask(path);
		MTFileTreeWalker walker = new MTFileTreeWalker(startTask);
		walker.walkFileTree();
	}
	
	public static final void removeEmptyDirectories(String first, String... more) throws Exception {
		removeEmptyDirectories(getPath(first,more));
	}
	
	public static final void removeEmptyDirectories(Path path) throws Exception {
		verifyRWDirectoryExists(path);
		MTDeleteEmptyDirectoryTask startTask = new MTDeleteEmptyDirectoryTask(path);
		MTFileTreeWalker walker = new MTFileTreeWalker(startTask);
		walker.walkFileTree();
	}
	
	/**
	 * Same as {@link #processDirectory(Path, boolean, boolean)} except the strings first and more
	 * are fed to {@link #getPath(String, String...)} first to produce a path.
	 * 
	 * @param create - create directory if it does not exist
	 * @param clean - clean an existing directory of all files/sub-directories
	 * @param first - the first part of a path to a directory
	 * @param more - the other parts of a path to a directory
	 * @throws IOException If any exception occurs it is throw ultimately as an IOException.
	 */
	public static void processDirectory(boolean create, boolean clean, String first, String... more) throws IOException {
		processDirectory(getPath(first,more),create,clean);
	}
	
	/**
	 * If create and clean are both false, this will simply verify that the directory represented
	 * by path exists, is a directory, and is R/W able. If create is true then this will check to 
	 * make sure the path does not exist and if not will attempt to create all directories in the
	 * path that are missing before performing the first set of verifications mentioned. If clean
	 * is true then this will remove all files and sub-directories contained within the directory
	 * represented by path but will not remove the directory represented by path itself. If clean
	 * is enabled, it will occur after both create and the first set of verifications mentioned.
	 * 
	 * @param path - the path to the directory
	 * @param create - create directory if it does not exist
	 * @param clean - clean an existing directory of all files/sub-directories
	 * @throws IOException If any exception occurs it is throw ultimately as an IOException.
	 */
	public static void processDirectory(Path path, boolean create, boolean clean) throws IOException {
		if(create && !Files.exists(path)) {
			Files.createDirectories(path);
		}
		
		verifyRWDirectoryExists(path);
		
		if(clean){
			cleanDirectory(path);
		}
	}
	
	public static Path findFirstFile(Path startDir, String findName, String findPath) throws Exception{
		Set<Path> ret = find(startDir,findName,findPath,"f",null,null);
		if(ret == null || ret.isEmpty())
			return null;
		return ret.iterator().next();
	}
	
	public static Path findFirstDir(Path startDir, String findName, String findPath) throws Exception{
		Set<Path> ret = find(startDir,findName,findPath,"d",null,null);
		if(ret == null || ret.isEmpty())
			return null;
		return ret.iterator().next();
	}
	
	/** This method returns a set of all the paths that match the given name and/or path starting at the startDir. Note
	 * the matching is performed on the absolute normalized path and not the relative path so all patterns should take
	 * this into account. The pattern matcher uses glob to perform all matches. More info on this can be found at
	 * {@link PathMatcher}. This method also allows for the pruning of results by a pattern as well as by type (either
	 * file or directory).
	 */
	public static Set<Path> find(Path startDir, String findName, String findPath, String type, String pruneName, String prunePath)
			throws Exception{
		verifyRWDirectoryExists(startDir);
		if(findName == null && findPath == null)
			throw new IllegalArgumentException("Error: A name pattern, path pattern, or both must be provided.");
		
		MTFindTask findTask = new MTFindTask(startDir, findName, findPath, type, pruneName, prunePath);
		MTFileTreeWalker walker = new MTFileTreeWalker(findTask);
		walker.walkFileTree();
		return findTask.getResults();
	}
	
	public static FileHashList getExistingFileHashList(String first, String... more) throws Exception {
		return FileHashList.readXMLStatic(null, getPath(first, more));
	}
	
	public static FileHashList getExistingFileHashList(Path path) throws Exception {
		return FileHashList.readXMLStatic(null, path);
	}
	
	public static FileHash getExistingFileHash(String first, String... more) throws Exception{
		return FileHash.getExistingFileHash(getPath(first, more));
	}
	
	public static FileHash getExistingFileHash(Path path) throws Exception {
		return FileHash.getExistingFileHash(path);
	}
	
	public static FileHashList genFileHashList(List<Path> fullPaths, Path rootPath) throws Exception {
		List<Path> realtivePaths = new ArrayList<>(fullPaths.size());
		for(Path p : fullPaths) {
			realtivePaths.add(rootPath.relativize(p));
		}
		return genFileHashList(fullPaths, realtivePaths);
	}
	
	public static FileHashList genFileHashList(List<Path> fullPaths, List<Path> realtivePaths) throws Exception {
		if(fullPaths == null || realtivePaths == null || fullPaths.size() != realtivePaths.size())
			throw new IllegalArgumentException("Error: The lists cannot be null and must be the same length.");
		List<FileHash> list = new ArrayList<>();
		for(int i = 0; i < fullPaths.size(); i++) {
			list.add(genFileHash(fullPaths.get(i),realtivePaths.get(i)));
		}
		return new FileHashList(list);
	}
	
	public static FileHash genFileHash(Path fullFilePath, Path realtiveFilePath) throws Exception {
		return FileHash.genFileHash("SHA-256", fullFilePath, realtiveFilePath);
	}
	
	public static void removeCachedFileHash(Path fullFilePath) {
		FileHash.removeFileHashRecord(fullFilePath);
	}
	
	//Start code that needs to be transitioned to Paths using java.nio

	private final static int[] illegalChars = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 34, 42, 47, 58, 59, 60, 62, 63, 92, 124};
	
	public static StringBuilder cleanFileName(StringBuilder fileName, char sub) {
		int len = fileName.codePointCount(0, fileName.length());
		StringBuilder ret = new StringBuilder();
		for (int i=0; i<len; i++) {
			int c = fileName.codePointAt(i);
			if (Arrays.binarySearch(FileHelpers.illegalChars, c) >= 0){
				ret.append(sub);
			}else{
				ret.appendCodePoint(c);
			}
		}
		return ret;
	}
	
	public static StringBuilder cleanFileName(StringBuilder fileName) {
		return cleanFileName(fileName,'-');
	}
	
	public static String cleanFileName(String fileName) {
		return cleanFileName(new StringBuilder(fileName),'-').toString();
	}
	
	public static StringBuilder replaceWhitespace(StringBuilder fileName, char sub) {
		int len = fileName.codePointCount(0, fileName.length());
		StringBuilder ret = new StringBuilder();
		for (int i = 0; i < len; i++) {
			int c = fileName.codePointAt(i);
			if(Character.isWhitespace(c)) {
				ret.append(sub);
			}else{
				ret.appendCodePoint(c);
			}
		}
		return ret;
	}
	
	public static StringBuilder replaceWhitespace(StringBuilder fileName) {
		return replaceWhitespace(fileName,'_');
	}
	
	public static String replaceWhitespace(String fileName) {
		return replaceWhitespace(new StringBuilder(fileName),'_').toString();
	}
	
	public static String trimFullFilePath(String filePath) throws Exception {
		if(com.google.common.io.Files.getFileExtension(filePath).length() > 0){
			return trimFullFilePath(filePath,true,0);
		}
		return trimFullFilePath(filePath,false,0);
	}
	
	public static String trimFullFilePath(String filePath, boolean keepExtension, int buffer) throws Exception {
		File file = new File(filePath);
		file = file.getCanonicalFile();
		String fullFilePath = file.getPath();
		int limit = 259;
		if(buffer >= limit){
			throw new Exception("Error: The size cannot be larger or equal to the max path size.");
		}
		limit = limit - buffer;
		if(fullFilePath.length() <= limit){
			return fullFilePath;
		}else{
			String fileex = com.google.common.io.Files.getFileExtension(fullFilePath);
			int remainder = fullFilePath.length() - limit;
			StringBuilder ret = new StringBuilder();
			StringBuilder name = new StringBuilder(com.google.common.io.Files.getNameWithoutExtension(fullFilePath));
			if(!keepExtension && fileex.length() > 0){
				name.append(".").append(fileex);
			}
			int len = name.codePointCount(0, name.length());
			for(int i = len - 1; i >= 0; i--){
				int c = name.codePointAt(i);
				if(remainder <= 0){
					ret.insert(0, Character.toChars(c));
				}else{
					remainder -= Character.charCount(c);
				}
			}
			if(ret.length() == 0){
				throw new Exception("Error: Not enough characters in the file name to reduce overall path size to a 259 char length.");
			}
			if(keepExtension && fileex.length() > 0){
				ret.append(".").append(fileex);
			}
			ret.insert(0, File.separator).insert(0,fullFilePath.substring(0,fullFilePath.lastIndexOf(File.separator)));
			return ret.toString();
		}
	}
	
	public static String getHashOfString(String hashName, String s) throws Exception {
		if(Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null){
			Security.addProvider(new BouncyCastleProvider());
		}
		MessageDigest messageDigest = MessageDigest.getInstance(hashName);
		try {
			messageDigest.update(s.getBytes());
		} catch(OutOfMemoryError e) {
			byte[] dataBytes = new byte[8192];
			int nread = 0; 
			InputStream in = new ByteArrayInputStream(s.getBytes());
			while ((nread = in.read(dataBytes)) != -1) {
				messageDigest.update(dataBytes, 0, nread);
			}
		}
		return Hex.toHexString(messageDigest.digest());
	}
	
	public static List<Path> getAllFilesInDirectory(Path dir) throws Exception {
		List<Path> ret = new ArrayList<>();
		try(Stream<Path> s = Files.list(dir).filter(Files::isRegularFile)) {
			for(Iterator<Path> it = s.iterator(); it.hasNext();) {
				Path p = it.next();
				ret.add(FileHelpers.getNormAndAbsPath(p));
			}
		}
		return ret;
	}
	
}
