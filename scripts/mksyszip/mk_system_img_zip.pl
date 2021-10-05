#!/usr/bin/perl
use strict;
use warnings;

use Getopt::Long;
use File::Path qw(make_path remove_tree);
use File::Spec;
use Cwd 'abs_path';
use File::Basename;
use Archive::Zip qw( :ERROR_CODES :CONSTANTS );

use File::Find;
use File::Remove 'remove';

my $inputFile = '';
my $outDir = '';

# Note run simg2img before this script if the image is sparse
# simg2img /path/to/Android/images/system.img /output/path/system.raw.img
GetOptions ("o=s" => \$outDir,
			"i=s"   => \$inputFile
);

if(!defined $inputFile || $inputFile eq '' || ! -e $inputFile || ! -r $inputFile || ! -f $inputFile){
	die("Please provide a valid file containing the system image.");
}

if(!defined $outDir || $outDir eq ''){
	my($wc1, $scriptdir, $wc2) = fileparse(abs_path($0));
	$outDir = $scriptdir;
}

if(-e $outDir){
	if(! -r $outDir || ! -w $outDir || ! -d $outDir){
		die("Please provide a valid output directory");
	}
}else{
	make_path($outDir);
}

my($inFileName, $inFileDirs, $inFileExt) = fileparse($inputFile,qr/\.[^.]*/);

my $outFile = File::Spec->catfile($outDir,$inFileName . '.zip');
my $workingdir = File::Spec->catfile($outDir,'working');
my $systemdir = File::Spec->catfile($workingdir,'system');
my $tmpdir = File::Spec->catfile($workingdir,'tmp');

if(-e $outFile){
	if(-f $outFile){
		if(-r $outFile && -w $outFile){
			unlink $outFile or die $!;
		}else{
			die("'$outFile' exists but is not a r/w able file.");
		}
	}else{
		die("'$outFile' exists but is not a file.");
	}
}

remove_tree($workingdir,{safe => 0});
make_path($workingdir);
make_path($systemdir);
make_path($tmpdir);

system("sudo mount -o ro,loop $inputFile $systemdir") == 0 or die "Error: Failed to mount the system image '$inputFile' to '$systemdir'";
system("sudo cp -r $systemdir/* $tmpdir") == 0 or die "Error: Failed to copy the files from the mounted system image at '$systemdir' to the directory '$tmpdir'.";
system("sudo chown -R \$USER:\$USER $tmpdir") == 0 or die "Error: Failed to change ownership of the copied files in '$tmpdir' to the current user.";
system("sudo umount $systemdir") == 0 or die "Error: Failed to unmount the system image from '$systemdir'";

my $zip = Archive::Zip->new();
$zip->addTree($tmpdir);
unless ( $zip->writeToFileNamed($outFile) == AZ_OK ) {
	die "Error: Failed to create the system image zip at '$outFile'.";
}

remove_tree($workingdir,{safe => 0});
