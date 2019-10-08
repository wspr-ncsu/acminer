#!/usr/bin/perl -w
use strict;
use warnings;

use Getopt::Long;
use File::Path qw(make_path remove_tree);
use File::Spec;
use Cwd 'abs_path';
use File::Basename;
use Data::Dumper;
use Clone 'clone';
use List::MoreUtils qw(uniq);
use Storable;

my $indir = '';
my $outdir = '';
my $const = '';

GetOptions ("i=s" => \$indir,
			"c=s" => \$const,
			"o=s"   => \$outdir
);

if(!defined $outdir || $outdir eq '' || !-e $outdir){
	$outdir = "out";
}

if(!defined $const || $indir eq '' || !-f $const){
	die "Please provide the constraints.txt file!";
}

if(!defined $indir || $indir eq '' || !-e $indir){
	die "Please provide an input directory!";
}

open my $fhconst, "<", $const or die "Could not open $const: $!";
my @constLines = <$fhconst>;
close $fhconst;

my $curstub = '';
my @skipTmp = ();
my %epToNmInfo = ();

my %stubToSkipped = (); #because no native methods
my %stubToNmInfo = ();
my @orderStub = ();

foreach my $line (@constLines){
	$line =~ s/^\s+|\s+$//g; #trim both ends
	if($line =~ /^<Start Group (.+)>$/){
		$curstub = $1;
		push(@orderStub,$curstub);
	}elsif($line =~ /^<End Group \Q$curstub\E>$/){
		if(@skipTmp > 0){
			$stubToSkipped{$curstub} = clone(\@skipTmp);
		}
		if(keys %epToNmInfo > 0){
			$stubToNmInfo{$curstub} = clone(\%epToNmInfo);
		}
		$curstub = '';
		@skipTmp = ();
		%epToNmInfo = ();
	}elsif($line =~ /^(<.+>)\[\] true_value: skipped$/){
		push(@skipTmp,$1);
	}elsif($line =~ /^(<.+>)\[.+(<.+: .+>).+\] true_value: error$/){
		my $ep = $1;
		my $nm = $2;
		if(!exists $epToNmInfo{$ep}){
			$epToNmInfo{$ep} = {
				$nm => [1,0,0]
			};
		}else{
			my $nmhash = $epToNmInfo{$ep};
			if(!exists $nmhash->{$nm}){
				$nmhash->{$nm} = [1,0,0];
			}else{
				$nmhash->{$nm}[0] += 1;
			}
		}
	}elsif($line =~ /^(<.+>)\[.+(<.+: .+>).+\] true_value: skipped$/){
		my $ep = $1;
		my $nm = $2;
		if(!exists $epToNmInfo{$ep}){
			$epToNmInfo{$ep} = {
				$nm => [0,0,1]
			};
		}else{
			my $nmhash = $epToNmInfo{$ep};
			if(!exists $nmhash->{$nm}){
				$nmhash->{$nm} = [0,0,1]; #error, success, skipped
			}else{
				$nmhash->{$nm}[2] += 1;
			}
		}
	}else{
		my @arr = split(' true_value: ',$line,2);
		$line = $arr[0] . ' true_value:';
		if($line =~ /^(<.+>)\[.+(<.+: .+>).+\] true_value:$/){
			my $ep = $1;
			my $nm = $2;
			if(!exists $epToNmInfo{$ep}){
				$epToNmInfo{$ep} = {
					$nm => [0,1,0]
				};
			}else{
				my $nmhash = $epToNmInfo{$ep};
				if(!exists $nmhash->{$nm}){
					$nmhash->{$nm} = [0,1,0];
				}else{
					$nmhash->{$nm}[1] += 1;
				}
			}
		}else{
			print "Could not parse $line\n";
		}
	}
}

remove_tree($outdir);
make_path($outdir);

dumpNmInfo();

sub dumpNmInfo{
	my $file1 = File::Spec->catfile($outdir,"EpWithNoNativeMethodsByStub.txt");
	my $file2 = File::Spec->catfile($outdir,"EpWithNoNativeMethods.txt");
	open my $fh1, ">", $file1 or die "Could not open $file1: $!";
	open my $fh2, ">", $file2 or die "Could not open $file2: $!";
	foreach my $stub (@orderStub){
		if(exists $stubToSkipped{$stub}){
			print $fh1 "$stub\n";
			foreach my $ep (@{$stubToSkipped{$stub}}){
				print $fh2 "$ep\n";
				print $fh1 "\t$ep\n";
			}
		}
	}
	close $fh1;
	close $fh2;
	
	#calculate the number before printing
	my $totalCalls = 0;
	my $totalSkipped = 0;
	my $totalError = 0;
	my $totalSuccess = 0;
	my %epToCounts = ();
	my %stubToCounts = ();
	my %stubToNativeMethods = ();
	
	foreach my $stub (@orderStub){
		if(exists $stubToNmInfo{$stub}){
			my %nms = ();
			$stubToNativeMethods{$stub} = \%nms;
			foreach my $ep (sort {lc $a cmp lc $b} keys $stubToNmInfo{$stub}){
				foreach my $nm (sort {lc $a cmp lc $b} keys $stubToNmInfo{$stub}->{$ep}){
					my $error = $stubToNmInfo{$stub}->{$ep}->{$nm}->[0];
					my $success = $stubToNmInfo{$stub}->{$ep}->{$nm}->[1];
					my $skipped = $stubToNmInfo{$stub}->{$ep}->{$nm}->[2];
					$totalCalls += $error + $success + $skipped;
					$totalSkipped += $skipped;
					$totalError += $error;
					$totalSuccess += $success;
					if(!exists $epToCounts{$ep}){
						$epToCounts{$ep} = [0,0,0,0];
					}
					if(!exists $stubToCounts{$stub}){
						$stubToCounts{$stub} = [0,0,0,0];
					}
					$epToCounts{$ep}[0] += $error;
					$epToCounts{$ep}[1] += $success;
					$epToCounts{$ep}[2] += $skipped;
					$epToCounts{$ep}[3] += $error + $success + $skipped;
					$stubToCounts{$stub}[0] += $error;
					$stubToCounts{$stub}[1] += $success;
					$stubToCounts{$stub}[2] += $skipped;
					$stubToCounts{$stub}[3] += $error + $success + $skipped;
					
					if(!exists $nms{$nm}){
						$nms{$nm} = [$error,$success,$skipped];
					}else{
						$nms{$nm}->[0] += $error;
						$nms{$nm}->[1] += $success;
						$nms{$nm}->[2] += $skipped;
					}
				}
			}
		}
	}
	
	#by stub only this gives us the cupples
	#list of all the native methods that do not have anything to compare to
	#
	
	my %uniqNm = ();
	my $file3 = File::Spec->catfile($outdir,"NmInfoDump.txt");
	open my $fh3, ">", $file3 or die "Could not open $file3: $!";
	
	foreach my $stub (@orderStub){
		if(exists $stubToNmInfo{$stub}){
			print $fh3 "$stub: Total " . $stubToCounts{$stub}->[3] . ", Success " . $stubToCounts{$stub}->[1] . ", Skip " . $stubToCounts{$stub}->[2] . ", Timeout " . $stubToCounts{$stub}->[0] . "\n";
			foreach my $ep (sort {lc $a cmp lc $b} keys $stubToNmInfo{$stub}){
				print $fh3 "\t$ep: Total " . $epToCounts{$ep}->[3] . ", Success " . $epToCounts{$ep}->[1] . ", Skip " . $epToCounts{$ep}->[2] . ", Timeout " . $epToCounts{$ep}->[0] . "\n";
				foreach my $nm (sort {lc $a cmp lc $b} keys $stubToNmInfo{$stub}->{$ep}){
					my $error = $stubToNmInfo{$stub}->{$ep}->{$nm}->[0];
					my $success = $stubToNmInfo{$stub}->{$ep}->{$nm}->[1];
					my $skipped = $stubToNmInfo{$stub}->{$ep}->{$nm}->[2];
					$uniqNm{$nm} = 1;
					print $fh3 "\t\t$nm: Success $success, Skip $skipped, Timeout $error\n";
				}
			}
		}
	}
	close $fh3;
	
	
	my $numUniqNm = keys %uniqNm;
	my $file4 = File::Spec->catfile($outdir,"UniqueNativeMethods.txt");
	open my $fh4, ">", $file4 or die "Could not open $file4: $!";
	foreach my $nm (sort {lc $a cmp lc $b} keys %uniqNm){
		print $fh4 "$nm\n";
	}
	close $fh4;
	
	my $numUniqNmStubPairs = 0;
	my %singlePairs = ();
	my $file5 = File::Spec->catfile($outdir,"StubAndNativeMethodPairs.txt");
	open my $fh5, ">", $file5 or die "Could not open $file5: $!";
	foreach my $stub (@orderStub){
		if(exists $stubToNativeMethods{$stub}){
			my %nms = ();
			print $fh5 "$stub: Total " . $stubToCounts{$stub}->[3] . ", Success " . $stubToCounts{$stub}->[1] . ", Skip " . $stubToCounts{$stub}->[2] . ", Timeout " . $stubToCounts{$stub}->[0] . "\n";
			foreach my $nm (sort {lc $a cmp lc $b} keys $stubToNativeMethods{$stub}){
				my $error = $stubToNativeMethods{$stub}->{$nm}->[0];
				my $success = $stubToNativeMethods{$stub}->{$nm}->[1];
				my $skipped = $stubToNativeMethods{$stub}->{$nm}->[2];
				print $fh5 "\t$nm: Success $success, Skip $skipped, Timeout $error\n";
				if(($success == 1 && $skipped == 0) || ($success == 0 && $skipped == 1)){
					$nms{$nm} = [$error,$success,$skipped];
				}
			}
			$numUniqNmStubPairs += keys $stubToNativeMethods{$stub};
			if(keys %nms > 0){
				$singlePairs{$stub} = \%nms;
			}
		}
	}
	close $fh5;
	
	my $totalSinglePairs = 0;
	my $file6 = File::Spec->catfile($outdir,"SinglePathForNmInStub.txt");
	open my $fh6, ">", $file6 or die "Could not open $file6: $!";
	foreach my $stub (@orderStub){
		if(exists $singlePairs{$stub}){
			print $fh6 "$stub\n";
			foreach my $nm (sort {lc $a cmp lc $b} keys $singlePairs{$stub}){
				my $error = $singlePairs{$stub}->{$nm}->[0];
				my $success = $singlePairs{$stub}->{$nm}->[1];
				my $skipped = $singlePairs{$stub}->{$nm}->[2];
				print $fh6 "\t$nm: Success $success, Skip $skipped, Timeout $error\n";
			}
			$totalSinglePairs += keys $singlePairs{$stub};
		}
	}
	close $fh6;
	
	#call data
	print "Total Calls: $totalCalls\n";
	print "Total Calls Success: $totalSuccess\n";
	print "Total Calls Timeout: $totalError\n";
	print "Total Calls Skipped: $totalSkipped\n";
	print "Total Unique Native Methods: $numUniqNm\n";
	print "Total Unique Stub and Native Method Pairs: $numUniqNmStubPairs\n";
	print "Total Single Paths: $totalSinglePairs\n";
}
