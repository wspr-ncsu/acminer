#!/usr/bin/perl

$| = 1; # Turn on print flushing.

use strict;
use warnings;
use Android::ADB;
use File::Which;
use Text::SimpleTable::AutoWidth;
use IO::Prompt::Hooked;
use File::Path qw(make_path remove_tree);
use File::Spec;
use Cwd 'abs_path';

sub trim {
	my $s = shift; 
	$s =~ s/^\s+|\s+$//g; 
	return $s 
}

my $cygpath = which('cygpath');

my $outdir = undef;
if(@ARGV) {
	$outdir = $ARGV[0];
}
if(!defined $outdir || $outdir eq ''){
	die("Please provide a valid output directory.\n");
}
if(-e $outdir){
	if(-r $outdir && -w $outdir && -d $outdir) {
		remove_tree($outdir,{safe => 0});
	} else {
		die("Please provide a valid output directory.\n");
	}
}
make_path($outdir) or die $!;
$outdir = abs_path($outdir);

my $outDumpsysPackages = File::Spec->catdir($outdir,'dumpsys_packages');
make_path($outDumpsysPackages) or die $!;

my $adb = Android::ADB->new();
my @devices = $adb->devices;
if(! @devices) {
	die("Unable to find any devices.\n");
}

my $c = 0;
my $table = Text::SimpleTable::AutoWidth->new(captions => [qw/ Index Name State /]);
for my $device (@devices) {
	$table->row( $c, $device->serial, $device->state );
	$c = $c + 1;
}
print $table->draw;
print "\n";
my $id = trim(prompt(
    message  =>  "Select a device by entering its index:",
    error    =>  "Invalid device index, please try again\n",
    validate =>  sub {
        my $id = shift;
		$id = trim($id);
        return ( $id =~ /^\d\d*$/  &&  0 <= $id && $id < (scalar @devices) ); 
    },
));

$adb->set_device($devices[$id]);
my $temp = $adb->shell("pm list packages");
my @packages = split(/\n/,$temp);
foreach my $pkg (@packages) {
	$pkg = trim($pkg);
	$pkg =~ s/^package://g;
	
	my $data = $adb->shell("dumpsys package $pkg");
	my $outfile = File::Spec->catfile($outDumpsysPackages,$pkg . '.txt');
	open(OUTFILE, ">", $outfile) or die "Unable to create $outfile - $!\n";
	print OUTFILE $data;
	close OUTFILE;
}
