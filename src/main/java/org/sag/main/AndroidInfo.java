package org.sag.main;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import org.sag.common.xstream.NamedCollectionConverterWithSize;
import org.sag.common.xstream.XStreamInOut;
import org.sag.common.xstream.XStreamInOut.XStreamInOutInterface;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("AndroidInfo")
public final class AndroidInfo implements XStreamInOutInterface {
	
	@XStreamAlias("Name")
	private volatile String name;
	@XStreamAlias("Codename")
	private volatile String codename;
	@XStreamAlias("Model")
	private volatile String model;
	@XStreamAlias("Country")
	private volatile String country;
	@XStreamAlias("Carrier")
	private volatile String carrier;
	@XStreamAlias("Version")
	private volatile String version;
	@XStreamAlias("Revision")
	private volatile String revision;
	@XStreamAlias("Build")
	private volatile String build;
	@XStreamAlias("BuildDate")
	private volatile String buildDate;
	@XStreamAlias("SecurityPatchDate")
	private volatile String securityPatchDate;
	@XStreamAlias("Api")
	private volatile int api;
	@XStreamAlias("Java")
	private volatile int javaVersion;
	@XStreamAlias("ImageURLs")
	@XStreamConverter(value=NamedCollectionConverterWithSize.class,strings={"ImageURL"},types={String.class})
	private volatile ArrayList<String> imgUrls;
	@XStreamAlias("Notes")
	private volatile String notes;
	
	@XStreamOmitField
	private volatile String toString;
	
	private AndroidInfo(){}
	
	public AndroidInfo(String name, String codename, String model, String country, String carrier, String version, 
			String revision, String build, String buildDate, String securityPatchDate, ArrayList<String> imgUrls, 
			String notes) throws Exception {
		this(name,codename,model,country,carrier,version,revision,build,buildDate,securityPatchDate,imgUrls,notes,-1);
	}
	
	public AndroidInfo(String name, String codename, String model, String country, String carrier, String version, 
			String revision, String build, String buildDate, String securityPatchDate, ArrayList<String> imgUrls, 
			String notes, int api) throws Exception {
		this.name = name;
		this.codename = codename;
		this.model = model;
		this.country = country;
		this.carrier = carrier;
		this.version = version;
		this.revision = revision;
		this.build = build;
		this.buildDate = buildDate;
		this.securityPatchDate = securityPatchDate;
		this.imgUrls = imgUrls;
		this.notes = notes;
		this.api = api > 0 ? api : getApiFromVersion(version);
		this.javaVersion = getJavaFromApi(api);
		this.toString = null;
	}

	public String getName() {
		return name;
	}

	public String getCodename() {
		return codename;
	}

	public String getModel() {
		return model;
	}
	
	public String getCountry() {
		return country;
	}
	
	public String getCarrier() {
		return carrier;
	}
	
	public String getVersion() {
		return version;
	}

	public String getRevision() {
		return revision;
	}
	
	public String getBuild() {
		return build;
	}
	
	public String getBuildDate() {
		return buildDate;
	}
	
	public String getSecurityPatchDate() {
		return securityPatchDate;
	}
	
	public String getNotes() {
		return notes;
	}
	
	public ArrayList<String> getImageUrls() {
		return imgUrls;
	}

	public int getApi() {
		return api;
	}
	
	public int getJavaVersion() {
		return javaVersion;
	}
	
	@Override
	public boolean equals(Object other) {
		if(this == other)
			return true;
		if(other == null || !(other instanceof AndroidInfo))
			return false;
		AndroidInfo o = (AndroidInfo)other;
		return Objects.equals(name, o.name) &&
				Objects.equals(codename, o.codename) &&
				Objects.equals(model, o.model) &&
				Objects.equals(country, o.country) &&
				Objects.equals(carrier, o.carrier) &&
				Objects.equals(version, o.version) && 
				Objects.equals(revision, o.revision) &&
				Objects.equals(build, o.build) &&
				Objects.equals(buildDate, o.buildDate) &&
				Objects.equals(securityPatchDate, o.securityPatchDate) &&
				Objects.equals(notes, o.notes) &&
				Objects.equals(imgUrls, o.imgUrls) &&
				Objects.equals(api, o.api) &&
				Objects.equals(javaVersion, o.javaVersion);
	}
	
	@Override
	public int hashCode() {
		int i = 17;
		i = i * 31 + Objects.hashCode(name);
		i = i * 31 + Objects.hashCode(codename);
		i = i * 31 + Objects.hashCode(model);
		i = i * 31 + Objects.hashCode(country);
		i = i * 31 + Objects.hashCode(carrier);
		i = i * 31 + Objects.hashCode(version);
		i = i * 31 + Objects.hashCode(revision);
		i = i * 31 + Objects.hashCode(build);
		i = i * 31 + Objects.hashCode(buildDate);
		i = i * 31 + Objects.hashCode(securityPatchDate);
		i = i * 31 + Objects.hashCode(notes);
		i = i * 31 + Objects.hashCode(imgUrls);
		i = i * 31 + Objects.hashCode(api);
		i = i * 31 + Objects.hashCode(javaVersion);
		return i;
	}
	
	@Override
	public String toString() {
		if(toString == null) {
			StringBuilder sb = new StringBuilder();
			sb.append("Device '").append(name);
			if(codename != null && !codename.isEmpty())
				sb.append(" (").append(codename).append(")");
			sb.append("'");
			if(model != null && !model.isEmpty())
				sb.append(" Model '").append(model).append("'");
			if(carrier != null && !carrier.isEmpty())
				sb.append(" for Carrier '").append(carrier).append("'");
			if(country != null && !country.isEmpty())
				sb.append(" in Country '").append(country).append("'");
			sb.append(" running Android '").append(version);
			if(revision != null && !revision.isEmpty())
				sb.append("_").append(revision);
			sb.append(" (").append(api).append(")");
			sb.append(" (JDK 1.").append(javaVersion).append(")'");
			if(build != null && !build.isEmpty())
				sb.append(" Build '").append(build).append("'");
			if(buildDate != null && !buildDate.isEmpty())
				sb.append(" from Build Date '").append(buildDate).append("'");
			if(securityPatchDate != null && !securityPatchDate.isEmpty())
				sb.append(" with Security Patch Level '").append(securityPatchDate).append("'");
			toString = sb.toString();
		}
		return toString;
	}
	
	public static boolean isBetween(int x, int lower, int upper) { return lower <= x && x <= upper; }
	
	//This info came from https://source.android.com/source/requirements
	public static int getJavaFromApi(int api) throws Exception {
		if(isBetween(api,1,8)) {
			return 5;
		} else if(isBetween(api,9,19)) {
			return 6;
		} else if(isBetween(api,20,23)) {
			return 7;
		} else if(api >= 24) {
			return 8;
		} else {
			throw new Exception("Error: Unable to determine the java version from the android api '" + api + "'.");
		}
	}
	
	//https://source.android.com/source/build-numbers.html
	public static int getApiFromVersion(String version) throws Exception {
		String failureMsg = "Error: Failed to parse the android version string '" + version 
				+ "' and produce a valid android API level.";
		try{
			String[] parts = version.split("\\.");
			int[] iParts = new int[parts.length];
			for(int i = 0; i < parts.length; i++) {
				iParts[i] = Integer.parseInt(parts[i]);
			}
			if(iParts[0] == 1){
				if(iParts[1] == 0) {
					return 1; //1.0
				} else if(isBetween(iParts[1],1,4)) {
					return 2; //1.1
				} else if(iParts[1] == 5) {
					return 3; //1.5
				} else if(iParts[1] == 6) {
					return 4; //1.6
				}
			} else if(iParts[0] == 2) {
				if(iParts[1] == 0) {
					if(iParts.length == 2) {
						return 5; //2.0
					} else {
						return 6; //2.0.1
					}
				} else if(iParts[1] == 1) {
					return 7; //2.1
				} else if(iParts[1] == 2) {
					return 8; //2.2.x
				} else if(iParts[1] == 3) {
					if(iParts.length == 2 || isBetween(iParts[2],0,2)) {
						return 9; //2.3-2.3.2
					} else if(isBetween(iParts[2],3,7)) {
						return 10; //2.3.3-2.3.7
					}
				}
			} else if(iParts[0] == 3) {
				if(iParts[1] == 0) {
					return 11; //3.0
				} else if(iParts[1] == 1) {
					return 12; //3.1
				} else if(iParts[1] == 2) {
					return 13; //3.2.x
				}
			} else if(iParts[0] == 4) {
				if(iParts[1] == 0) {
					if(iParts.length == 2 || isBetween(iParts[2],0,2)) {
						return 14; //4.0.1-4.0.2
					} else if(isBetween(iParts[2],3,4)) {
						return 15; //4.0.3-4.0.4
					}
				} else if(iParts[1] == 1) {
					return 16; //4.1.x
				} else if(iParts[1] == 2) {
					return 17; //4.2.x
				} else if(iParts[1] == 3) {
					return 18; //4.3.x
				} else if(iParts[1] == 4) {
					return 19; //4.4-4.4.4
				}
			} else if(iParts[0] == 5) {
				if(iParts[1] == 0) {
					return 21; //5.0
				} else if(iParts[1] == 1) {
					return 22; //5.1
				}
			} else if(iParts[0] == 6) {
				if(iParts[1] == 0) {
					return 23; //6.0
				}
			} else if(iParts[0] == 7) {
				if(iParts[1] == 0) {
					return 24; //7.0
				} else if(iParts[1] == 1) {
					return 25; //7.1-7.1.2
				}
			} else if(iParts[0] == 8) {
				if(iParts[1] == 0) {
					return 26; //8.0.0
				}
			}
		} catch(Throwable t) {
			throw new Exception(failureMsg,t);
		}
		throw new Exception(failureMsg);
	}
	
	@Override
	public void writeXML(String filePath, Path path) throws Exception {
		XStreamInOut.writeXML(this, filePath, path);
	}
	
	public AndroidInfo readXML(String filePath, Path path) throws Exception {
		return (AndroidInfo)XStreamInOut.readXML(this,filePath, path);
	}
	
	public static AndroidInfo readXMLStatic(String filePath, Path path) throws Exception {
		return new AndroidInfo().readXML(filePath, path);
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
			return Collections.singleton(AndroidInfo.class);
		}
		
		@Override
		public void setXStreamOptions(XStream xstream) {
			defaultOptionsNoRef(xstream);
		}
		
	}
	
	public static void parseConfigsFromFile(Path path) throws Exception {
		try(BufferedReader in = Files.newBufferedReader(path)) {
			boolean first = true;
			boolean inNotes = false;
			String line;
			String deviceName = "";
			String deviceCodeName = "";
			String modelNumber = "";
			String country = "";
			String carrier = "";
			String version = "";
			String revision = "";
			String build = "";
			String buildDate = "";
			String securityPatchDate = "";
			ArrayList<String> imgUrl = new ArrayList<>();
			String notes = "";
			int api = -1;
			int count = 0;
			while((line = in.readLine()) != null) {
				if(line.startsWith("Name: ")) {
					if(!first) {
						if(inNotes) {
							notes = notes.replaceAll("\\s+$", "");
							if(!notes.isEmpty()) {
								notes += "\n\t";
							}
						}
						AndroidInfo ai = new AndroidInfo(deviceName,deviceCodeName,modelNumber,country,carrier,version, revision,
								build,buildDate,securityPatchDate,imgUrl,notes,api);
						//Avoid the use of FileHelpers to make this easy to export
						ai.writeXML(null,Paths.get(path.getParent().toString(),"android_info_"+ count++ + ".xml")
								.toAbsolutePath().normalize().toAbsolutePath());
						deviceName = deviceCodeName = modelNumber = country = carrier = version 
								= build = buildDate = securityPatchDate = notes = revision = "";
						api = -1;
						imgUrl = new ArrayList<>();
						inNotes = false;
					} else {
						first = false;
					}
					deviceName = line.replaceFirst("Name: ", "").trim();
				} else if(!inNotes && line.startsWith("Codename: ")) {
					deviceCodeName = line.replaceFirst("Codename: ", "").trim();
				} else if(!inNotes && line.startsWith("Model: ")) {
					modelNumber = line.replaceFirst("Model: ", "").trim();
				} else if(!inNotes && line.startsWith("Country: ")) {
					country = line.replaceFirst("Country: ", "").trim();
				} else if(!inNotes && line.startsWith("Carrier: ")) {
					carrier = line.replaceFirst("Carrier: ", "").trim();
				} else if(!inNotes && line.startsWith("Version: ")) {
					version = line.replaceFirst("Version: ", "").trim();
				} else if(!inNotes && line.startsWith("Revision: ")) {
					revision = line.replaceFirst("Revision: ", "").trim();
				} else if(!inNotes && line.startsWith("Build: ")) {
					build = line.replaceFirst("Build: ", "").trim();
				} else if(!inNotes && line.startsWith("Build Date: ")) {
					buildDate = line.replaceFirst("Build Date: ", "").trim();
				} else if(!inNotes && line.startsWith("Security Patch Date: ")) {
					securityPatchDate = line.replaceFirst("Security Patch Date: ", "").trim();
				} else if(!inNotes && line.startsWith("Image URL: ")) {
					imgUrl.add(line.replaceFirst("Image URL: ", "").trim());
				} else if(!inNotes && line.startsWith("Api: ")) {
					api = Integer.parseInt(line.replaceFirst("Api: ", "").trim());
				} else if(line.startsWith("Notes:")) {
					inNotes = true;
					notes += "\n";
				} else if(inNotes) {
					notes += "\t" + line + "\n";
				}
			}
			
			if(!deviceName.isEmpty()) {
				if(inNotes) {
					notes = notes.replaceAll("\\s+$", "");
					if(!notes.isEmpty()) {
						notes += "\n\t";
					}
				}
				AndroidInfo ai = new AndroidInfo(deviceName,deviceCodeName,modelNumber,country,carrier,version, revision,
						build,buildDate,securityPatchDate,imgUrl,notes,api);
				//Avoid the use of FileHelpers to make this easy to export
				ai.writeXML(null,Paths.get(path.getParent().toString(),"android_info_"+ count++ + ".xml")
						.toAbsolutePath().normalize().toAbsolutePath());
			}
		}
	}
	
	public static void main(String[] args) {
		String usage = "Usage: [-i <path/to/input/file>] | [-o <path/to/output/file>] [--name <name>]\n"
					 + "       [--codename <codename>] [--model <model>] [--country <country>]\n"
					 + "       [--carrier <carrier>] [--version <version>] [--revision <revision>]\n"
					 + "       [--build <build>] [--builddate <build date>] [--security <security\n"
					 + "       patch date>] [--api <api>] [--imgurl <url to image>] [--notes <notes>]";
		try{
			String in = "";
			String out = "";
			String deviceName = "";
			String deviceCodeName = "";
			String modelNumber = "";
			String country = "";
			String carrier = "";
			String version = "";
			String revision = "";
			String build = "";
			String buildDate = "";
			String securityPatchDate = "";
			ArrayList<String> imgUrl = new ArrayList<>();
			String notes = "";
			String api = "";
			for(int i = 0; i < args.length; i++) {
				String option = args[i];
				String temp = args[++i].trim();
				switch(option) {
					case "-i": in = temp; break;
					case "-o": out = temp; break;
					case "--name": deviceName = temp; break;
					case "--codename": deviceCodeName = temp; break;
					case "--model": modelNumber = temp; break;
					case "--country": country = temp; break;
					case "--carrier": carrier = temp; break;
					case "--version": version = temp; break;
					case "--revision": revision = temp; break;
					case "--build": build = temp; break;
					case "--builddate": buildDate = temp; break;
					case "--security": securityPatchDate = temp; break;
					case "--imgurl": imgUrl.add(temp); break;
					case "--notes": notes = temp; break;
					case "--api": api = temp; break;
					default:
						throw new RuntimeException("Error: Unrecongized argument " + option + " " + temp);
				}
			}
			
			if(!in.isEmpty()) {
				//Avoid the use of FileHelpers to make this easy to export
				Path inPath = Paths.get(in).toAbsolutePath().normalize().toAbsolutePath();
				parseConfigsFromFile(inPath);
			} else if(!out.isEmpty()) {
				AndroidInfo i;
				//Avoid the use of FileHelpers to make this easy to export
				Path outPath = Paths.get(out).toAbsolutePath().normalize().toAbsolutePath();
				if(api.isEmpty()) {
					i = new AndroidInfo(deviceName,deviceCodeName,modelNumber,country,carrier,version, revision,
							build,buildDate,securityPatchDate,imgUrl,notes);
				} else {
					Integer a = Integer.parseInt(api);
					i = new AndroidInfo(deviceName,deviceCodeName,modelNumber,country,carrier,version, revision,
							build,buildDate,securityPatchDate,imgUrl,notes,a);
				}
				i.writeXML(null, outPath);
			} else {
				throw new RuntimeException("Error: An input or output file must be provided.");
			}
		} catch(Throwable t) {
			System.out.println(usage);
			t.printStackTrace();
			System.exit(1);
		}
		System.exit(0);
	}
	
}
