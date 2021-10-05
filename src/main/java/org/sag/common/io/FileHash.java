package org.sag.common.io;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.Security;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import org.sag.common.xstream.XStreamInOut;
import org.sag.common.xstream.XStreamInOut.XStreamInOutInterface;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("FileHash")
public class FileHash implements XStreamInOutInterface {

	@XStreamAsAttribute
	@XStreamAlias("Type")
	private String hashName;
	@XStreamAsAttribute
	@XStreamAlias("Hash")
	private String hash;
	@XStreamAsAttribute
	@XStreamAlias("Path")
	private String path;
	@XStreamOmitField
	private byte[] hashBytes;
	
	@XStreamOmitField
	private static Map<Path,FileHash> fullPathToFileHash = new HashMap<>();
	
	private FileHash(){
		this(null,null,null,null);
	}
	
	private FileHash(String hashName, String hash, byte[] hashBytes, String path) {
		this.hashName = hashName;
		this.hash = hash;
		this.path = path;
		this.hashBytes = hashBytes;
	}
	
	private Object readResolve(){
		hashBytes = Hex.decode(hash);
		return this;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Objects.hashCode(hash);
		result = prime * result + Objects.hashCode(hashName);
		result = prime * result + Objects.hashCode(path);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || !(obj instanceof FileHash))
			return false;
		
		FileHash other = (FileHash) obj;
		return Objects.equals(hashName, other.hashName) 
				&& Objects.equals(hash, other.hash) 
				&& Objects.equals(path, other.path);
	}

	@Override
	public String toString() {
		return "Path=" + path + " --> Type=" + hashName + " Hash=" + hash;
	}
	
	/**
	 * @return the hashName
	 */
	public String getType() {
		return hashName;
	}

	/**
	 * @return the hash
	 */
	public String getHash() {
		return hash;
	}

	/**
	 * @return the path
	 */
	public String getPathString() {
		return path;
	}
	
	public Path getPath() {
		return Paths.get(path);
	}
	
	public Path getFullPath(Path rootPath) {
		return FileHelpers.getPath(rootPath, getPath());
	}

	/**
	 * @return the hashBytes
	 */
	public byte[] getHashBytes() {
		return hashBytes;
	}
	
	public boolean compareHash(FileHash other){
		return hashName != null && other.hashName != null && hash != null && 
				other.hash != null && Objects.equals(hashName,other.hashName) && Objects.equals(hash,other.hash);
	}
	
	public static void resetFileHashRecord(){
		synchronized(fullPathToFileHash) {
			fullPathToFileHash.clear();
		}
	}
	
	public static void removeFileHashRecord(Path fullFilePath) {
		synchronized (fullPathToFileHash) {
			fullPathToFileHash.remove(fullFilePath);
		}
	}
	
	public static FileHash genFileHash(String hashName, Path fullFilePath, Path realtiveFilePath) throws Exception {
		if(realtiveFilePath == null){
			realtiveFilePath = fullFilePath;
		}
		
		synchronized (fullPathToFileHash) {
			FileHash ret = fullPathToFileHash.get(fullFilePath);
			if(ret != null) {
				if(ret.getPathString().equals(realtiveFilePath.toString())) {
					return ret;
				} else {
					return new FileHash(ret.getType(),ret.getHash(),ret.getHashBytes(),realtiveFilePath.toString());
				}
			}
			
			if(Files.exists(fullFilePath) && Files.isRegularFile(fullFilePath) && Files.isReadable(fullFilePath)) {
				
				if(Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null){
					Security.addProvider(new BouncyCastleProvider());
				}
				
				MessageDigest messageDigest = MessageDigest.getInstance(hashName);
				
				try{
					messageDigest.update(Files.readAllBytes(fullFilePath));
				} catch (OutOfMemoryError e){
					byte[] dataBytes = new byte[1048576];
					int nread = 0; 
					InputStream in = Files.newInputStream(fullFilePath);
					while ((nread = in.read(dataBytes)) != -1) {
						messageDigest.update(dataBytes, 0, nread);
					}
				}
				
				byte[] hash = messageDigest.digest();
				
				ret = new FileHash(hashName,Hex.toHexString(hash),hash,realtiveFilePath.toString());
				fullPathToFileHash.put(fullFilePath, ret);
				return ret;
			}else {
				throw new Exception("Error: The file '" + fullFilePath.toString() + 
						"' does not exist, is not a file, or is not readable.");
			}
		}
	}
	
	public static FileHash getExistingFileHash(Path path) throws Exception {
		return new FileHash().readXML(null, path);
	}

	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
		
	}

	@Override
	public FileHash readXML(String filePath, Path path) throws Exception {
		return XStreamInOut.readXML(this, filePath, path);
	}

	@XStreamOmitField
	private static AbstractXStreamSetup xstreamSetup = null;

	public static AbstractXStreamSetup getXStreamSetupStatic(){
		if(xstreamSetup == null)
			xstreamSetup = new XStreamSetup();
		return xstreamSetup;
	}
	
	@Override
	public AbstractXStreamSetup getXStreamSetup() {
		return getXStreamSetupStatic();
	}

	public static class XStreamSetup extends AbstractXStreamSetup {
		
		@Override
		public void getOutputGraph(LinkedHashSet<AbstractXStreamSetup> in) {
			if(!in.contains(this)) {
				in.add(this);
			}
		}

		@Override
		public Set<Class<?>> getAnnotatedClasses() {
			return Collections.singleton(FileHash.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}

}
