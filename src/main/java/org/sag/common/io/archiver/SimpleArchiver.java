package org.sag.common.io.archiver;

import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.sag.common.io.FileHashList;
import org.sag.common.io.FileHelpers;
import org.sag.common.tuple.Pair;

public class SimpleArchiver extends BaseArchiver {
	
	private SimpleArchiver(Path archivePath, boolean create) throws Exception {
		super(archivePath,create,create);
	}
	
	/** Searches through an archive to find files and directories that match the given patterns. 
	 * See {@link FileHelpers#find(Path, String, String, String, String, String)}.
	 * Note it returns strings instead of paths because the file system is closed at the
	 * end of this method and thus Path objects in it would not be valid.
	 */
	public static Set<String> findInArchive(Path archivePath, String startDir, String findName, String findPath, String type, 
			String pruneName, String prunePath) throws Exception {
		List<Throwable> errs = new ArrayList<>();
		SimpleArchiver arch = null;
		archivePath = FileHelpers.getNormAndAbsPath(archivePath);
		Set<Path> res = null;
		Set<String> ret = new LinkedHashSet<>();
		
		try {
			arch = new SimpleArchiver(archivePath, false);
			Path startPath = FileHelpers.getNormAndAbsPath(arch.zipfs.getPath(startDir));
			res = FileHelpers.find(startPath, findName, findPath, type, pruneName, prunePath);
			//Spawns no threads so once find returns we have the results
			for(Path p : res){
				ret.add(p.toString());
			}
		}catch(Throwable t){
			errs.add(t);
		}finally{
			try{
				if(arch != null){
					arch.close();
				}
			} catch(Throwable t){
				errs.add(t);
			}
			if(arch != null)
				errs.addAll(arch.getExceptions());
		}
		
		if(!errs.isEmpty()){
			throwJointError(errs, "Failed complete the finding procedure for archive'" + archivePath + "'.");
		}
		
		return ret;
	}
	
	public static void extractFromArchive(Path archivePath, Set<Pair<Path,Path>> srcDestMap) throws Exception {
		List<Throwable> errs = new ArrayList<>();
		SimpleArchiver arch = null;
		archivePath = FileHelpers.getNormAndAbsPath(archivePath);
		try {
			arch = new SimpleArchiver(archivePath, false);
			final SimpleArchiver archt = arch;
			final Path root = arch.zipfs.getRootDirectories().iterator().next();
			for(Pair<Path, Path> e : srcDestMap){
				Path srcPath = e.getFirst();
				final Path destPath = FileHelpers.getNormAndAbsPath(e.getSecond());
				if(srcPath == null || srcPath.toString().isEmpty() || Objects.equals(srcPath,srcPath.getRoot()))
					srcPath = root;
				else
					srcPath = archt.zipfs.getPath(root.toString(),srcPath.toString());
				if(FileHelpers.checkRWFileExists(srcPath)){
					arch.addTask(arch.new CopyFileTask(srcPath,destPath));
				}else if(FileHelpers.checkRWDirectoryExists(srcPath)){
					final Path src = srcPath;
					Files.walkFileTree(srcPath, new SimpleFileVisitor<Path>(){
						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
							archt.addTask(archt.new CopyFileTask(file,FileHelpers.getPath(destPath,src.relativize(file))));
							return FileVisitResult.CONTINUE;
						}
						@Override
						public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
							Path realtivePath = src.relativize(dir);
							if(!realtivePath.toString().isEmpty()){
								archt.addTask(archt.new CreateDirTask(FileHelpers.getPath(destPath,realtivePath)));
							}
							return FileVisitResult.CONTINUE;
						}
					});
				}else{
					errs.add(new RuntimeException("Error: The path '" + srcPath + "' is either not a file or directory, "
							+ "is not accessable, or does not exist."));
				}
			}
		} catch(Throwable t) {
			errs.add(t);
		} finally {
			try{
				if(arch != null){
					arch.close();
				}
			} catch(Throwable t){
				errs.add(t);
			}
			if(arch != null)
				errs.addAll(arch.getExceptions());
		}
		
		if(!errs.isEmpty()){
			throwJointError(errs, "Failed to extract all requested files and directories from the archive '" + archivePath + "'.");
		}
	}
	
	public static void extractArchive(Path archivePath, Path outputDir) throws Exception {
		List<Throwable> errs = new ArrayList<>();
		SimpleArchiver arch = null;
		final Path out = FileHelpers.getNormAndAbsPath(outputDir);
		archivePath = FileHelpers.getNormAndAbsPath(archivePath);
		
		try {
			arch = new SimpleArchiver(archivePath, false);
			final SimpleArchiver archt = arch;
			final Path root = arch.zipfs.getRootDirectories().iterator().next();
			Files.walkFileTree(root, new SimpleFileVisitor<Path>(){
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
					archt.addTask(archt.new CopyFileTask(file,FileHelpers.getPath(out, file.toString())));
					return FileVisitResult.CONTINUE;
				}
	
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
					archt.addTask(archt.new CreateDirTask(FileHelpers.getPath(out, dir.toString())));
					return FileVisitResult.CONTINUE;
				}
			});
		}catch(Throwable t){
			errs.add(t);
		}finally{
			try{
				if(arch != null){
					arch.close();
				}
			} catch(Throwable t){
				errs.add(t);
			}
			if(arch != null)
				errs.addAll(arch.getExceptions());
		}
		
		if(!errs.isEmpty()){
			throwJointError(errs, "Failed to extract the archive '" + archivePath + "' to the output path '" + outputDir + "'.");
		}
	}
	
	public static void addToArchive(Path archivePath, Set<Pair<Path,Path>> srcDestMap, boolean create) throws Exception {
		List<Throwable> errs = new ArrayList<>();
		SimpleArchiver arch = null;
		archivePath = FileHelpers.getNormAndAbsPath(archivePath);
		try {
			arch = new SimpleArchiver(archivePath, create);
			final Path root = arch.zipfs.getRootDirectories().iterator().next();
			for(Pair<Path, Path> e : srcDestMap){
				final Path srcPath = FileHelpers.getNormAndAbsPath(e.getFirst());
				Path destPath = e.getSecond();
				if(FileHelpers.checkRWFileExists(srcPath)){
					arch.addTask(arch.new CopyFileTask(srcPath,arch.zipfs.getPath(root.toString(),destPath.toString())));
				}else if(FileHelpers.checkRWDirectoryExists(srcPath)){
					final SimpleArchiver archt = arch;
					if(destPath == null || destPath.toString().isEmpty() || Objects.equals(destPath,destPath.getRoot()))
						destPath = root;
					else
						destPath = archt.zipfs.getPath(root.toString(),destPath.toString());
					final String dest = destPath.toString();
					Files.walkFileTree(srcPath, new SimpleFileVisitor<Path>(){
						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
							archt.addTask(archt.new CopyFileTask(file,archt.zipfs.getPath(dest,
									srcPath.relativize(file).toString())));
							return FileVisitResult.CONTINUE;
						}
			
						@Override
						public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
							Path out = archt.zipfs.getPath(dest,srcPath.relativize(dir).toString());
							if(out != null && !out.equals(root) && !out.toString().isEmpty()){
								archt.addTask(archt.new CreateDirTask(out));
							}
							return FileVisitResult.CONTINUE;
						}
					});
				}else{
					errs.add(new RuntimeException("Error: The path '" + srcPath + "' is either not a file or directory, "
							+ "is not accessable, or does not exist."));
				}
			}
		}catch(Throwable t){
			errs.add(t);
		}finally{
			try{
				if(arch != null){
					arch.close();
				}
			} catch(Throwable t){
				errs.add(t);
			}
			if(arch != null)
				errs.addAll(arch.getExceptions());
		}
		
		if(!errs.isEmpty()){
			throwJointError(errs, "Failed to add all requested files and directories to the archive '" + archivePath + "'.");
		}
	}
	
	public static FileHashList addSourceChecksumToArchive(Path archivePath, List<Path> fullSourcePath, List<Path> realtiveSourcePath, 
			boolean create) throws Exception {
		if(archivePath == null || fullSourcePath == null || realtiveSourcePath == null || fullSourcePath.size() != realtiveSourcePath.size())
			throw new IllegalArgumentException("Error: All paths must be non null and both lists must be the same length");
		List<Throwable> errs = new ArrayList<>();
		SimpleArchiver arch = null;
		WriteSourceChecksumFileTask fhTask = null;
		FileHashList ret = null;
		archivePath = FileHelpers.getNormAndAbsPath(archivePath);
		try {
			arch = new SimpleArchiver(archivePath, create);
			ret = FileHelpers.genFileHashList(fullSourcePath, realtiveSourcePath);
			
			fhTask = arch.new WriteSourceChecksumFileTask(fullSourcePath,realtiveSourcePath);
			arch.addTask(fhTask);
		}catch(Throwable t){
			errs.add(t);
		}finally{
			try{
				if(arch != null){
					arch.close();
					if(fhTask != null)
						ret = fhTask.getFileHashList();
				}
			} catch(Throwable t){
				errs.add(t);
			}
			if(arch != null)
				errs.addAll(arch.getExceptions());
		}
		
		if(!errs.isEmpty()){
			throwJointError(errs, "Failed to add the source checksum of '" + fullSourcePath + "' to the archive at path '" + archivePath + "'.");
		}
		
		return ret;
	}
	
	public static void createArchive(Path archivePath, Path inputDir) throws Exception {
		createArchive(archivePath, inputDir, null, null);
	}
	
	public static FileHashList createArchive(Path archivePath, Path inputDir, List<Path> fullSourcePath, List<Path> realtiveSourcePath) 
			throws Exception {
		List<Throwable> errs = new ArrayList<>();
		SimpleArchiver arch = null;
		WriteSourceChecksumFileTask fhTask = null;
		FileHashList ret = null;
		final Path in = FileHelpers.getNormAndAbsPath(inputDir);
		archivePath = FileHelpers.getNormAndAbsPath(archivePath);
		try {
			arch = new SimpleArchiver(archivePath, true);
			final SimpleArchiver archt = arch;
			Files.walkFileTree(in, new SimpleFileVisitor<Path>(){
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
					archt.addTask(archt.new CopyFileTask(file,archt.zipfs.getPath(in.relativize(file).toString())));
					return FileVisitResult.CONTINUE;
				}
	
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
					String realtivePath = in.relativize(dir).toString();
					if(!realtivePath.isEmpty()){
						archt.addTask(archt.new CreateDirTask(archt.zipfs.getPath(realtivePath)));
					}
					return FileVisitResult.CONTINUE;
				}
			});
			if(fullSourcePath != null && realtiveSourcePath != null){
				fhTask = archt.new WriteSourceChecksumFileTask(fullSourcePath,realtiveSourcePath);
				archt.addTask(fhTask);
			}
		}catch(Throwable t){
			errs.add(t);
		}finally{
			try{
				if(arch != null){
					arch.close();
					if(fhTask != null)
						ret = fhTask.getFileHashList();
				}
			} catch(Throwable t){
				errs.add(t);
			}
			if(arch != null)
				errs.addAll(arch.getExceptions());
		}
		
		if(!errs.isEmpty()){
			throwJointError(errs, "Failed to create the archive '" + archivePath + "' from the input path '" + inputDir + "'.");
		}
		
		return ret;
	}
	
	private class CreateDirTask extends Task {
		private Path dirToCreate;
		public CreateDirTask(Path dirToCreate) {
			this.dirToCreate = dirToCreate;
		}
		@Override
		public void run() {
			try{
				Files.createDirectories(dirToCreate);
			} catch (Throwable t){
				throw new RuntimeException("Error: Something went wrong when creating dir '" + dirToCreate + "'.",t);
			}
		}
	}
	
	private class CopyFileTask extends Task {
		private Path srcFile;
		private Path destFile;
		public CopyFileTask(Path srcFile, Path destFile) {
			this.srcFile = srcFile;
			this.destFile = destFile;
		}
		@Override
		public void run() {
			try{
				Path dirPath = destFile.getParent();
				if(dirPath != null)
					Files.createDirectories(dirPath);
				Files.copy(srcFile, destFile, StandardCopyOption.REPLACE_EXISTING);
			} catch (Throwable t){
				throw new RuntimeException("Error: Something went wrong when copying '" + srcFile + "' to '" + destFile + "'.",t);
			}
		}
	}
	
	private class WriteSourceChecksumFileTask extends Task {
		private final static String checksumPath = "checksum.xml";
		private List<Path> fullSourcePath;
		private List<Path> realtiveSourcePath;
		private FileHashList fh;
		public WriteSourceChecksumFileTask(List<Path> fullSourcePath, List<Path> realtiveSourcePath) {
			this.fullSourcePath = fullSourcePath;
			this.realtiveSourcePath = realtiveSourcePath;
			this.fh = null;
		}
		@Override
		public void run() {
			try {
				fh = FileHelpers.genFileHashList(fullSourcePath, realtiveSourcePath);
				fh.writeXML(null, getZipFS().getPath(checksumPath));
			} catch (Throwable t) {
				throw new RuntimeException("Error: Something went wrong when writing the checksum of the source file at '" 
						+ fullSourcePath + "'.",t);
			}
		}
		public FileHashList getFileHashList(){
			return fh;
		}
	}
	
}
