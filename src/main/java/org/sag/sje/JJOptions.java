package org.sag.sje;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.sag.common.io.FileHelpers;
import org.sag.common.logging.ILogger;
import org.sag.common.logging.LoggerWrapperSLF4J;



public class JJOptions {

	private static JJOptions singleton = null;
	
	public static JJOptions v(){
		if(singleton == null)
			singleton = new JJOptions();
		return singleton;
	}
	
	public static void reset(){
		singleton = null;
	}
	
	private JJOptions(){}
	
	private final ILogger defaultMainLogger = new LoggerWrapperSLF4J(JJExtractor.class);
	private ILogger mainLogger = defaultMainLogger;
	
	public void setMainLogger(ILogger logger) {
		this.mainLogger = logger;
	}
	
	public ILogger getMainLogger(){
		return mainLogger;
	}
	
	public ILogger getDefaultMainLogger() {
		return defaultMainLogger;
	}
	
	private boolean isDextraEnabled = false;
	private boolean isSmaliEnabled = true;
	
	public void enableDextra(){
		this.isDextraEnabled = true;
		this.isSmaliEnabled = false;
	}
	
	public void enableSmali(){
		this.isSmaliEnabled = true;
		this.isDextraEnabled = false;
	}
	
	public boolean isDextraEnabled(){
		return isDextraEnabled;
	}
	
	public boolean isSmaliEnabled(){
		return isSmaliEnabled;
	}
	
	private static final String defaultInputAndroidInfoName = "android_info.xml";
	private static final String defaultInputSystemImgZipName = "system_img.zip";
	private static final String defaultInputDir = "input";
	private Path inputDir = FileHelpers.getPath(defaultInputDir);
	private Path input_SystemImgZip = FileHelpers.getPath(defaultInputDir,defaultInputSystemImgZipName);
	private Path input_AndroidInfo = FileHelpers.getPath(defaultInputDir, defaultInputAndroidInfoName);
	
	public void setInput(String input) {
		setInput(FileHelpers.getPath(input));
	}
	
	public void setInput(Path input) {
		if(Files.isDirectory(input)) {
			setInputDir(input);
			setInput_SystemImgZip(FileHelpers.getPath(input,defaultInputSystemImgZipName));
			setInput_AndroidInfo(FileHelpers.getPath(input,defaultInputAndroidInfoName));
		} else if(Files.isRegularFile(input)) {
			setInputDir(input.getParent());
			setInput_SystemImgZip(input);
			setInput_AndroidInfo(FileHelpers.getPath(getInputDir(),defaultInputAndroidInfoName));
		} else {
			throw new RuntimeException("Error: Input path '" + input.toString() + "' is not a file or directory.");
		}
	}
	
	public void setInputDir(String inputDir) {
		setInputDir(FileHelpers.getPath(inputDir));
	}
	
	public void setInputDir(Path inputDir) {
		this.inputDir = inputDir;
	}
	
	public Path getInputDir() {
		return inputDir;
	}
	
	public void setInput_SystemImgZip(Path inputPath){
		this.input_SystemImgZip = inputPath;
	}
	
	public void setInput_SystemImgZip(String inputPath) {
		setInput_SystemImgZip(FileHelpers.getPath(inputPath));
	}
	
	public Path getInput_SystemImgZip() {
		return input_SystemImgZip;
	}
	
	public void setInput_AndroidInfo(Path inputPath){
		this.input_AndroidInfo = inputPath;
	}
	
	public void setInput_AndroidInfo(String inputPath) {
		setInput_AndroidInfo(FileHelpers.getPath(inputPath));
	}
	
	public Path getInput_AndroidInfo() {
		return input_AndroidInfo;
	}
	
	private Path outputDir = FileHelpers.getPath("output");
	private Path output_WorkingDir = null;
	private Path output_SystemJimpleJarFile = null;
	private Path output_SystemClassJarFile = null;
	private Path output_SystemJimpleFrameworkOnlyJarFile = null;
	private Path output_SystemJimpleClassConflictsZipFile = null;
	private Path output_SystemArchivesZipFile = null;
	private Path output_FrameworkPkgsFile = null;
	private String[] locations = {"framework","app","priv-app"};
	private String[] archs = {"arm64", "arm", "x86_64", "x86"};
	private String[] apexLocations = {"apex"};
	
	public void setOutputDir(String outputDir) {
		this.outputDir = FileHelpers.getPath(outputDir);
		this.output_WorkingDir = null;
		this.output_SystemJimpleJarFile = null;
		this.output_SystemClassJarFile = null;
		this.output_SystemJimpleFrameworkOnlyJarFile = null;
		this.output_SystemJimpleClassConflictsZipFile = null;
		this.output_SystemArchivesZipFile = null;
		this.output_FrameworkPkgsFile = null;
	}
	
	public Path getOutputDir() {
		return outputDir;
	}
	
	public void setOutput_WorkingDir(Path in){
		this.output_WorkingDir = in;
	}

	public Path getOutput_WorkingDir() {
		if(output_WorkingDir == null)
			output_WorkingDir = FileHelpers.getPath(getOutputDir(), "working");
		return output_WorkingDir;
	}
	
	public String[] getLocations(){
		return Arrays.copyOf(locations, locations.length);
	}
	
	public String[] getApexLocations() {
		return Arrays.copyOf(apexLocations, apexLocations.length);
	}
	
	public String[] getArchs() {
		return Arrays.copyOf(archs, archs.length);
	}
	
	public void setOutput_SystemArchivesZipFile(Path in){
		this.output_SystemArchivesZipFile = in;
	}
	
	public Path getOutput_SystemArchivesZipFile(){
		if(output_SystemArchivesZipFile == null)
			output_SystemArchivesZipFile = FileHelpers.getPath(getOutputDir(), "system_archives.zip");
		return output_SystemArchivesZipFile;
	}
	
	public void setOutput_SystemJimpleJarFile(Path in){
		this.output_SystemJimpleJarFile = in;
	}
	
	public Path getOutput_SystemJimpleJarFile(){
		if(output_SystemJimpleJarFile == null)
			output_SystemJimpleJarFile = FileHelpers.getPath(getOutputDir(), "system_jimple.jar");
		return output_SystemJimpleJarFile;
	}
	
	public void setOutput_SystemJimpleFrameworkOnlyJarFile(Path in){
		this.output_SystemJimpleFrameworkOnlyJarFile = in;
	}
	
	public Path getOutput_SystemJimpleFrameworkOnlyJarFile(){
		if(output_SystemJimpleFrameworkOnlyJarFile == null)
			output_SystemJimpleFrameworkOnlyJarFile = FileHelpers.getPath(getOutputDir(), "system_jimple_framework_only.jar");
		return output_SystemJimpleFrameworkOnlyJarFile;
	}
	
	public void setOutput_SystemJimpleClassConflictsZipFile(Path in){
		this.output_SystemJimpleClassConflictsZipFile = in;
	}
	
	public Path getOutput_SystemJimpleClassConflictsZipFile(){
		if(output_SystemJimpleClassConflictsZipFile == null)
			output_SystemJimpleClassConflictsZipFile = FileHelpers.getPath(getOutputDir(), "system_jimple_class_conflicts.zip");
		return output_SystemJimpleClassConflictsZipFile;
	}
	
	public void setOutput_SystemClassJarFile(Path in){
		this.output_SystemClassJarFile = in;
	}
	
	public Path getOutput_SystemClassJarFile(){
		if(output_SystemClassJarFile == null)
			output_SystemClassJarFile = FileHelpers.getPath(getOutputDir(), "system_class.jar");
		return output_SystemClassJarFile;
	}
	
	public void setOutput_FrameworkPkgsFile(Path in) {
		this.output_FrameworkPkgsFile = in;
	}
	
	public Path getOutput_FrameworkPkgsFile() {
		if(output_FrameworkPkgsFile == null)
			output_FrameworkPkgsFile = FileHelpers.getPath(getOutputDir(), "framework_pkgs.txt");
		return output_FrameworkPkgsFile;
	}
	
	private boolean allAppsSameTime = true;

	public boolean getAllAppsSameTime() {
		return allAppsSameTime;
	}
	
	public void disableAllAppsSameTime() {
		allAppsSameTime = false;
	}
	
	private boolean includeApps = false;
	
	public boolean getIncludeApps() {
		return includeApps;
	}
	
	public void enableIncludeApps() {
		includeApps = true;
	}
	
	private boolean dumpClasses = true;
	
	public boolean getDumpClasses() {
		return dumpClasses;
	}
	
	public void disableDumpClasses() {
		dumpClasses = false;
	}
	
	private String bootClassPath = "boot.oat";
	
	public String getBootClassPath() {
		return bootClassPath;
	}
	
	public void setBootClassPath(String bootClassPath) {
		this.bootClassPath = bootClassPath;
	}
	
	private boolean classConflicts = false;
	
	public boolean getClassConflicts() {
		return classConflicts;
	}

	public void enableClassConflicts() {
		classConflicts = true;
	}
	
}
