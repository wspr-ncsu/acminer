#!/usr/bin/perl -w
use strict;
use warnings;

use Getopt::Long;
use File::Path qw(make_path remove_tree);
use File::Spec;
use Cwd 'abs_path';
use File::Basename;

$SIG{INT} = \&interrupt;

my $defNumRun = 3;

my $numrun = $defNumRun;
my $cmdsfile = '';

my $sptpath = abs_path($0);
my($wc1, $scriptdir, $wc2) = fileparse(abs_path($0));
my $helperpath = File::Spec->catfile($scriptdir,'cmdhelper.pl');

my $tmpdir = File::Spec->catfile($scriptdir,'pids');

GetOptions ("n=i" => \$numrun,
			"c=s"   => \$cmdsfile
);

if(!defined $cmdsfile || $cmdsfile eq '' || ! -e $cmdsfile || ! -r $cmdsfile){
	die("Please provide a valid file containing the commands to run.");
}

if(!defined $numrun || $numrun <= 0){
	$numrun = $defNumRun;
}

remove_tree($tmpdir,{safe => 0});
make_path($tmpdir);

open(my $fh, '<', $cmdsfile);
my @cmds = <$fh>;
close $fh;

#spawn processes max $numrun active at a time
my $count = 0;
my $completed = 0;
my %activePids = ();
foreach my $cmd (@cmds){
	$cmd =~ s/^\s+|\s+$//g;
	if($cmd eq '' || $cmd =~ /^#/){
		next;
	}
	if($count == $numrun){
		checkFinishedPids();
	}
	
	if($completed != 0){
		last;
	}
	
	my $pid = fork();
	if(!defined $pid || $pid < 0){
		die "Failed to fork a new process.";
	}elsif($pid == 0){
		exec("x-terminal-emulator -T '$cmd' -e \"$helperpath '$cmd'\"") or die "Failed to start cmd: $cmd";
	}else{
		wait();
		$count++;
		my $cldpid = -1;
		foreach (1..6){
			sleep 5;
			$cldpid = checkNewProcesses();
			if($cldpid != -1){
				last;
			}
		}
		if($cldpid != -1){
			$activePids{$cldpid} = $cmd;
			print "Started[$cldpid]: $cmd\n";
		}else{
			$count--;
		}
	}
}

#wait until all finished before exiting
while(checkFinishedPids() != -1){}

if($completed == 0){
	$completed = 1;
	remove_tree($tmpdir,{safe => 0});
	print "All jobs finished!\n";
}

sub checkNewProcesses{
	my $ret = -1;
	opendir(DIR, $tmpdir) or die $!;
	my @pids = readdir DIR;
	closedir(DIR);
	foreach my $pid (@pids){
		if($pid eq '.' || $pid eq '..' || $pid !~ /^[0-9]+$/){
			next;
		}
		if(!exists $activePids{$pid} || !defined $activePids{$pid}){
			$ret = $pid;
			last;
		}
	}
	return $ret;
}

sub checkFinishedPids{
	my $allDone = -2;
	while($completed == 0 && $allDone == -2){
		$allDone = -1;
		my %cp = %activePids;
		my $found = 0;
		while(my ($key, $value) = each %cp){
			if($value ne ''){
				open(my $fd, '<', File::Spec->catfile($tmpdir,$key)) or die $!;
				my @filelines = <$fd>;
				close($fd);
				if(@filelines){
					$filelines[0] =~ s/^\s+|\s+$//g;
					if($filelines[0] =~ /^(.*)\sexit$/){
						if($1 == 0){
							print "Success[$key]: " . $activePids{$key} . "\n";
						}else{
							print "Terminated[$key] $1: " . $activePids{$key} . "\n";
						}
						$activePids{$key} = '';
						$count--;
						$found = 1;
					}
				}
				$allDone = -2;
			}
		}
		if($found){
			return 0;
		}
		if($completed == 0 && $allDone == -2){
			sleep 30;
		}
	}
	return -1;
}

sub interrupt{
	print "\nPlease enter a command:\n";
	my $userinput =  <STDIN>;
	$userinput =~ s/^\s+|\s+$//g;
	if($userinput eq 'all'){
		while(my ($key, $value) = each %activePids){
			if($value ne ''){
				killpid($key);
			}
		}
		remove_tree($tmpdir,{safe => 0});
		print "Killed before all jobs were finished!\n";
		exit(1);
	}elsif($userinput =~ /^\d+$/){
		if(exists $activePids{$userinput} && defined $activePids{$userinput} && $activePids{$userinput} ne ''){
			killpid($userinput);
		}
	}else{
		print "Unrecongized command!\n";
	}
}

sub killpid{
	my $pid = shift;
	do{
		kill(-9,$pid);
	}while(`pgrep -c -g $pid` != 0);
	print "Killed[$pid]: " . $activePids{$pid} . "\n";
	$activePids{$pid} = '';
	$count--;
}

