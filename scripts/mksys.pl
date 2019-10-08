#!/usr/bin/perl
use strict;
use warnings;
use File::Path qw(make_path remove_tree);
use File::Find;
use File::Remove 'remove';

# Creates a file "android.jar" which is a combination of all the class files found in all jar and apk
# files included in the standard eng rom built for the emulator. The names of the jars and apks 
# included in the rom are grabed from the "installed-files.txt" which is created during the build process.

# Input: The root source directory of whatever version of android this is to be run on. For example 
# "~/android-4.4.2".

# Output: All files are output in the "androidJar" folder located in the root source directory that is 
# given as input. It outputs three files: the "android.jar", "packages_emu_eng.txt" which is a list of 
# the names of all the jars and apks included in the "android.jar", and "classes_emu_eng.txt" which is a 
# list of all the classes included in the "android.jar".

my $mydroid_dir = $ARGV[0];
my $installFile = "/out/target/product/generic/installed-files.txt";
my $sysapp_dir = "/out/target/common/obj/APPS/";
my $fw_dir = "/out/target/common/obj/JAVA_LIBRARIES/";
my $outdir = $mydroid_dir . '/androidJar';
my $outjardir = $outdir . '/jar';
my $outextdir = $outdir . '/ext';

my @installed;
my @files;

sub findfiles{
	if(-f $_){
		if($_ =~ /\.class$/){
			$File::Find::name =~ /^\Q$outextdir\E\/(.*)/;
			$files[@files] = $1;
		}else{
			remove($_);
		}
	}
}

if($#ARGV != 0){
	die "Not enough args\n";
}

remove_tree($outdir);
make_path($outdir, $outjardir, $outextdir) or die $!;

open(my $FILE, "<", "$mydroid_dir$installFile") or die $!;
while (<$FILE>) {
	chomp($_);
	if ($_ =~ m/ \/system\/app\/(.*)\/\1.apk/ || $_ =~ m/ \/system\/priv-app\/(.*)\/\1.apk/ || $_ =~ m/  \/system\/app\/(.*).apk/) {
		my $sysapp = $1;
		if (-e "$mydroid_dir$sysapp_dir$sysapp" . "_intermediates/classes-full-debug.jar") {
			system("cp $mydroid_dir$sysapp_dir$sysapp" . "_intermediates/classes-full-debug.jar $outjardir/$sysapp.jar");
			$installed[@installed] = $sysapp . ".apk";
		} else {
			print "???Cannot find compiled classes for $sysapp???\n";
		}
	} elsif ($_ =~ m/  \/system\/framework\/(.*).jar/) {
		my $fw = $1;
		if (-e "$mydroid_dir$fw_dir$fw" . "_intermediates/classes-full-debug.jar") {
			system("cp $mydroid_dir$fw_dir$fw" . "_intermediates/classes-full-debug.jar $outjardir/$fw.jar");
			$installed[@installed] = $fw . ".jar";
		} elsif (-e "$mydroid_dir$fw_dir$fw" . "_intermediates/classes.jar") {
			system("cp $mydroid_dir$fw_dir$fw" . "_intermediates/classes.jar $outjardir/$fw.jar");
			$installed[@installed] = $fw . ".jar";
		} else {
			print "???Cannot find compiled classes for $fw???\n";
		}
	}
}
close($FILE);

# NFC is not installed to generic target, thus not found in the installed-files.txt
if (-e "$mydroid_dir$sysapp_dir" . "Nfc_intermediates/classes-full-debug.jar") {
	system("cp $mydroid_dir$sysapp_dir" . "Nfc_intermediates/classes-full-debug.jar $outjardir/Nfc.jar");
	$installed[@installed] = "Nfc.jar";
}

system('unzip -oq \'' . $outjardir . '/*.jar\' -x \'META-INF/*\' -d ' . $outextdir);

find(\&findfiles, $outextdir);

system('cd ' . $outextdir . '&& zip -rq ../System.jar *');

remove_tree($outjardir);
remove_tree($outextdir);

@installed = sort {lc($a) cmp lc($b)} @installed;
@files = sort {lc($a) cmp lc($b)} @files;

open(my $out4, ">", "$outdir/packages_emu_eng.txt") or die "Error opening $outdir/packages_emu_eng.txt";
for my $i (@installed){
	print $out4 "$i\n";
}
close $out4;

open(my $out, ">", "$outdir/classes_emu_eng.txt") or die "Error opening $outdir/classes_emu_eng.txt";
for my $i (@files){
	print $out "$i\n";
}
close $out;
