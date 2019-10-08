#!/usr/bin/perl

#Generate information about the apks, jars, odex, oat, dex, etc. files in a system image

$| = 1; # Turn on print flushing.

use strict;
use warnings;
use File::Find;
use File::Slurp qw( read_dir );
use File::Find::Rule;
use File::Basename;
use Archive::Zip qw( :ERROR_CODES :CONSTANTS );
use File::Which;
use Cwd 'abs_path';
use Data::Dumper;

sub eval_apps($$);
sub eval_framework($$);

my($wca, $script_dir, $wcb) = fileparse(abs_path(__FILE__));
my $cygpath = which('cygpath');
my $baksmali = abs_path(File::Spec->catfile($script_dir,'../lib/baksmali-2.2b4-fat.jar'));
my $in_path = '';
if(@ARGV) {
	$in_path = $ARGV[0];
}

if(!defined $in_path || $in_path eq '' || ! -e $in_path || ! -r $in_path || ! -d $in_path){
	die("Please provide a valid file containing the system image.");
}

if(!defined $baksmali || $baksmali eq '' || ! -e $baksmali || ! -r $baksmali || ! -f $baksmali){
	die("Unable to find baksmali at location $baksmali");
}

if(defined $cygpath) {
	$baksmali = trim(`'$cygpath' --windows '$baksmali'`);
}

my $app_dir = File::Spec->catdir($in_path,'app');
my $privapp_dir = File::Spec->catdir($in_path,'priv-app');
my $framework_dir = File::Spec->catdir($in_path,'framework');

if(! -e $app_dir || ! -r $app_dir || ! -d $app_dir) {
	die("Unable to find app dir $app_dir");
}
if(! -e $privapp_dir || ! -r $privapp_dir || ! -d $privapp_dir) {
	die("Unable to find app dir $privapp_dir");
}
if(! -e $framework_dir || ! -r $framework_dir || ! -d $framework_dir) {
	die("Unable to find app dir $framework_dir");
}

my $outfile = File::Spec->catfile($in_path,"stats.txt");
open(my $out, ">", $outfile) or die "Unable to open $outfile - $!\n";

eval_apps($app_dir,$out);
print $out "\n";
eval_apps($privapp_dir,$out);
print $out "\n";
eval_framework($framework_dir,$out);

close $out;

sub eval_framework($$) {
	my ($dir,$out) = @_;
	my($wkname, $wc1, $wc2) = fileparse($dir);
	my @jars = File::Find::Rule->file()->name('*.apk', '*.jar')->in($dir);
	my @oats = File::Find::Rule->file()->name('*.oat')->in($dir);
	my @odexs = File::Find::Rule->file()->name('*.odex')->in($dir);
	my $device_path = "/system/framework/";
	
	my @res = ();
	
	foreach my $jar (@jars) {
		$jar = trim($jar);
		my $jarname = $jar;
		$jarname =~ s/$dir\///g;
		$jarname = trim($jarname);
		my $contains_dex = 0;
		my $zip = Archive::Zip->new($jar) or die "Can't create jar $jar\n";
		if(grep("classes.dex" eq $_,$zip->memberNames())) {
			$contains_dex = 1;
		}
		my @source = ();
		my %hash = ('name' => $jarname, 'path' => $jar, 'source' => \@source, 'hasdex' => $contains_dex);
		push(@res, \%hash);
	}
	
	foreach my $oat (@oats) {
		my $path = $oat;
		if(defined $cygpath) {
			$path = trim(`'$cygpath' --windows '$path'`);
		}
		my @bkout = `java -jar '$baksmali' l d '$path'`;
		chomp(@bkout);
		@bkout = map {$_ =~ s/$device_path//g; trim($_)} @bkout;
		@bkout = grep($_ ne '', @bkout);
		if(@bkout > 1) {
			my @arr = ();
			my $r = shift(@bkout); #assume this is the one we care about and see if the rest are just other split dex entries
			foreach my $temp (@bkout) {
				if($temp !~ /^\Q$r\E/) {
					push(@arr,$temp);
				}
			}
			push(@arr,$r);
			foreach my $jarname (@arr) {
				my $found = 0;
				foreach my $hash_ref (@res) {
					my %hash = %{$hash_ref};
					if($hash{'name'} eq $jarname) {
						my $trim_oat = $oat;
						$trim_oat =~ s/$dir\///g;
						push(@{$hash{'source'}},$trim_oat);
						$found = 1;
						last;
					}
				}
				if($found == 0) {
					my $trim_oat = $oat;
					$trim_oat =~ s/$dir\///g;
					my @source = ($trim_oat);
					my %hash = ('name' => $jarname, 'path' => '__unknown__', 'source' => \@source, 'hasdex' => 0);
					push(@res, \%hash);
				}
			}
		} elsif(@bkout == 1) {
			my $jarname = pop(@bkout);
			my $found = 0;
			foreach my $hash_ref (@res) {
				my %hash = %{$hash_ref};
				if($hash{'name'} eq $jarname) {
					my $trim_oat = $oat;
					$trim_oat =~ s/$dir\///g;
					push(@{$hash{'source'}},$trim_oat);
					$found = 1;
					last;
				}
			}
			if($found == 0) {
				my $trim_oat = $oat;
				$trim_oat =~ s/$dir\///g;
				my @source = ($trim_oat);
				my %hash = ('name' => $jarname, 'path' => '__unknown__', 'source' => \@source, 'hasdex' => 0);
				push(@res, \%hash);
			}
		} else {
			my $trim_oat = $oat;
			$trim_oat =~ s/$dir\///g;
			my @source = ($trim_oat);
			my %hash = ('name' => '__unknown__', 'path' => '__unknown__', 'source' => \@source, 'hasdex' => 0);
			push(@res, \%hash);
		}
	}
	
	foreach my $odex (@odexs) {
		my($jarname, $wc1, $wc2) = fileparse($odex,qr/\.[^.]*/);
		$jarname = "$jarname.jar";
		my $found = 0;
		foreach my $hash_ref (@res) {
			my %hash = %{$hash_ref};
			if($hash{'name'} eq $jarname) {
				my $trim_odex = $odex;
				$trim_odex =~ s/$dir\///g;
				push(@{$hash{'source'}},$trim_odex);
				$found = 1;
				last;
			}
		}
		if($found == 0) {
			my $trim_odex = $odex;
			$trim_odex =~ s/$dir\///g;
			my @source = ($trim_odex);
			my %hash = ('name' => $jarname, 'path' => '__unknown__', 'source' => \@source, 'hasdex' => 0);
			push(@res, \%hash);
		}
	}
	
	my @unknown_jar = (); #i.e. we have an oat file but no jar and the oat file does not list one???
	my @no_jar = (); #i.e. we have and oat or odex file (so we know what the jar should be) but no matching physical jar
	my @hasdex = (); #i.e. we have physical jar with dex already inside it so we don't care about the source
	my @no_source = (); #i.e. we have a physical jar with no dex and no source

	# The only source files are oat and then we brake them up into arm, arm64, both, neither
	my @oat_arm = ();
	my @oat_arm64 = ();
	my @oat_both = ();
	my @oat_neither = ();

 	# The only source files are odex and then we brake them up into arm, arm64, both, neither
	my @odex_arm = ();
	my @odex_arm64 = ();
	my @odex_both = ();
	my @odex_neither = ();

	# There are a mix of both oat and odex files and then we brake them up into arm, arm64, both, neither
	my @both_arm = ();
	my @both_arm64 = ();
	my @both_both = ();
	my @both_neither = ();

	# Boot Path vs other
	# arm vs arm64 vs both vs unknown
	
	foreach my $hash_ref (@res) {
		my %hash = %{$hash_ref};
		if($hash{'name'} eq '__unknown__') {
			foreach (@{$hash{'source'}}) {
				push(@unknown_jar,$_);
			}
		} elsif($hash{'path'} eq '__unknown__') {
			push(@no_jar,$hash{'name'});
		} elsif($hash{'hasdex'} == 1) {
			push(@hasdex,$hash{'name'});
		} elsif(@{$hash{'source'}} == 0) {
			push(@no_source,$hash{'name'});
		} else {
			my $hasoat = 0;
			my $hasodex = 0;
			my $hasarm = 0;
			my $hasarm64 = 0;
			foreach my $source (@{$hash{'source'}}) {
				if($source =~ /\.oat$/) {
					$hasoat = 1;
				} elsif($source =~ /\.odex$/) {
					$hasodex = 1;
				}
				if($source =~ /(^|\/)arm64\//) {
					$hasarm64 = 1;
				} elsif($source =~ /(^|\/)arm\//) {
					$hasarm = 1;
				}
			}

			if($hasoat == 1 && $hasodex == 1) {
				if($hasarm == 1 && $hasarm64 == 1) {
					push(@both_both,$hash{'name'});
				} elsif($hasarm == 1) {
					push(@both_arm,$hash{'name'});
				} elsif($hasarm64 == 1) {
					push(@both_arm64,$hash{'name'});
				} else {
					push(@both_neither,$hash{'name'});
				}
			} elsif($hasoat == 1) {
				if($hasarm == 1 && $hasarm64 == 1) {
					push(@oat_both,$hash{'name'});
				} elsif($hasarm == 1) {
					push(@oat_arm,$hash{'name'});
				} elsif($hasarm64 == 1) {
					push(@oat_arm64,$hash{'name'});
				} else {
					push(@oat_neither,$hash{'name'});
				}
			} elsif($hasodex == 1) {
				if($hasarm == 1 && $hasarm64 == 1) {
					push(@odex_both,$hash{'name'});
				} elsif($hasarm == 1) {
					push(@odex_arm,$hash{'name'});
				} elsif($hasarm64 == 1) {
					push(@odex_arm64,$hash{'name'});
				} else {
					push(@odex_neither,$hash{'name'});
				}
			} # note neither oat or odex can't happen because those are the only ones we search for
		}
	}

	print $out "$wkname:\n"
		  ."  Total=" . @res . "\n"
		  ."    Oat With No Source Jar Listing=" . @unknown_jar . "\n" 
		  ."    Oat or Odex With No Physical Jar=" . @no_jar . "\n" 
		  ."    Jar Containing Dex=" . @hasdex . "\n"
		  ."    Jar With No Oat or Odex=" . @no_source . "\n"
		  ."    Jar With Oat or Odex=" . (@oat_arm + @oat_arm64 + @oat_both + @oat_neither + @odex_arm + @odex_arm64 + @odex_both + @odex_neither + @both_arm + @both_arm64 + @both_both + @both_neither) . "\n"
		  ."      Only Oat=" . (@oat_arm + @oat_arm64 + @oat_both + @oat_neither) . "\n"
		  ."        Arm=" . @oat_arm . "\n"
		  ."        Arm64=" . @oat_arm64 . "\n"
		  ."        Both=" . @oat_both . "\n"
		  ."        Unknown=" . @oat_neither . "\n"
		  ."      Only Odex=" . (@odex_arm + @odex_arm64 + @odex_both + @odex_neither) . "\n"
		  ."        Arm=" . @odex_arm . "\n"
		  ."        Arm64=" . @odex_arm64 . "\n"
		  ."        Both=" . @odex_both . "\n"
		  ."        Unknown=" . @odex_neither . "\n"
		  ."      Both Oat and Odex=" . (@both_arm + @both_arm64 + @both_both + @both_neither) . "\n"
		  ."        Arm=" . @both_arm . "\n"
		  ."        Arm64=" . @both_arm64 . "\n"
		  ."        Both=" . @both_both . "\n"
		  ."        Unknown=" . @both_neither . "\n";
	
	if(@unknown_jar > 0) {
		print $out "\n  Oat With No Source Jar Listing:\n";
		foreach (@unknown_jar) {
			print $out "    $_\n";
		}
	}

	if(@no_jar > 0) {
		print $out "\n  Oat or Odex With No Physical Jar:\n";
		foreach (@no_jar) {
			print $out "    $_\n";
		}
	}

	if(@hasdex > 0) {
		print $out "\n  Jar Containing Dex:\n";
		foreach (@hasdex) {
			print $out "    $_\n";
		}
	}

	if(@no_source > 0) {
		print $out "\n  Jar With No Oat or Odex:\n";
		foreach (@no_source) {
			print $out "    $_\n";
		}
	}

	if((@oat_arm + @oat_arm64 + @oat_both + @oat_neither + @odex_arm + @odex_arm64 + @odex_both + @odex_neither + @both_arm + @both_arm64 + @both_both + @both_neither) > 0) {
		print $out "\n  Jar With Oat or Odex:\n";

		if((@oat_arm + @oat_arm64 + @oat_both + @oat_neither) > 0) {
			print $out  "\n    Only Oat:\n";
			if(@oat_arm > 0) {
				print $out  "\n      Arm:\n";
				foreach (@oat_arm) {
					print $out  "        $_\n";
				}
			}
			if(@oat_arm64 > 0) {
				print $out  "\n      Arm64:\n";
				foreach (@oat_arm64) {
					print $out  "        $_\n";
				}
			}
			if(@oat_both > 0) {
				print $out  "\n      Both:\n";
				foreach (@oat_both) {
					print $out  "        $_\n";
				}
			}
			if(@oat_neither > 0) {
				print $out  "\n      Unknown:\n";
				foreach (@oat_neither) {
					print $out  "        $_\n";
				}
			}
		}

		if((@odex_arm + @odex_arm64 + @odex_both + @odex_neither) > 0) {
			print $out  "\n    Only Odex:\n";
			if(@odex_arm > 0) {
				print $out  "\n      Arm:\n";
				foreach (@odex_arm) {
					print $out  "        $_\n";
				}
			}
			if(@odex_arm64 > 0) {
				print $out  "\n      Arm64:\n";
				foreach (@odex_arm64) {
					print $out  "        $_\n";
				}
			}
			if(@odex_both > 0) {
				print $out  "\n      Both:\n";
				foreach (@odex_both) {
					print $out  "        $_\n";
				}
			}
			if(@odex_neither > 0) {
				print $out  "\n      Unknown:\n";
				foreach (@odex_neither) {
					print $out  "        $_\n";
				}
			}
		}

		if((@both_arm + @both_arm64 + @both_both + @both_neither) > 0) {
			print $out  "\n    Both Oat and Odex:\n";
			if(@both_arm > 0) {
				print $out  "\n      Arm:\n";
				foreach (@both_arm) {
					print $out  "        $_\n";
				}
			}
			if(@both_arm64 > 0) {
				print $out  "\n      Arm64:\n";
				foreach (@both_arm64) {
					print $out  "        $_\n";
				}
			}
			if(@both_both > 0) {
				print $out  "\n      Both:\n";
				foreach (@both_both) {
					print $out  "        $_\n";
				}
			}
			if(@both_neither > 0) {
				print $out  "\n      Unknown:\n";
				foreach (@both_neither) {
					print $out  "        $_\n";
				}
			}
		}
	}	
}

sub printable {
	my $s = shift;
	$s =~ s/[[:^print:]]/ /g;
	$s = trim($s);
	return $s;
}

sub trim {
	my $s = shift; 
	$s =~ s/^\s+|\s+$//g; 
	return $s 
}

sub eval_apps($$) {
	my ($dir,$out) = @_;
	my($wkname, $wc1, $wc2) = fileparse($dir);
	my @sub_dirs = grep(-d $_, read_dir($dir, prefix => 1));
	
	my @apps = ();
	my @noapks = ();
	my @multapks = ();
	my @contain_dex = ();
	my @apk_no_odex = ();
	my @apk_odex = ();
	my $armct = 0;
	my $arm64ct = 0;
	my $bothct = 0;
	my $unknownct = 0;
	
	foreach my $subdir (@sub_dirs) {
		my($appname, $wc1, $wc2) = fileparse($subdir);
		my @apks = File::Find::Rule->file()->name('*.apk')->in($subdir);
		push(@apps,$appname);
		if(@apks == 0) {
			push(@noapks,$appname);
		} elsif(@apks == 1) {
			my $apk = pop(@apks);
			my $zip = Archive::Zip->new($apk) or die "Can't create apk $apk\n";
			if(grep("classes.dex" eq $_,$zip->memberNames())) {
				push(@contain_dex,$appname);
			} else {
				my @odexs = File::Find::Rule->file()->name('*.oat', '*.odex')->in($subdir);
				if(@odexs == 0) {
					push(@apk_no_odex,$appname);
				} else {
					my $has64 = 0;
					my $has32 = 0;
					my @unknown = ();
					foreach my $odex (@odexs) {
						if($odex =~ /\/arm64\//) {
							$has64 = 1;
						} elsif($odex =~ /\/arm\//) {
							$has32 = 1;
						} else {
							$odex =~ s/$subdir\///g;
							push(@unknown,$odex);
						}
					}
					
					if($has32 == 1 && $has64 == 1) {
						$bothct += 1;
					} elsif($has32 == 1) {
						$armct += 1;
					} elsif($has64 == 1) {
						$arm64ct += 1;
					}
					
					if(@unknown > 0) {
						$unknownct += 1;
					}
					
					my %hash = ('name' => $appname, 'has32' => $has32, 'has64' => $has64, 'unknown' => \@unknown);
					push(@apk_odex, \%hash);
				}
			}
		} else {
			my @temp = ();
			foreach (@apks) {
				$_ =~ s/$subdir\///g;
				push(@temp,$_);
			}
			my %hash = ('name' => $appname, 'apks' => \@temp);
			push(@multapks, \%hash);
		}
	}
	
	print $out  "$wkname:\n"
		  ."  Total=" . @apps . "\n"
		  ."    No Apks=" . @noapks . "\n" 
		  ."    Multi-Apks=" . @multapks . "\n" 
		  ."    Single Apk=" . (@contain_dex + @apk_no_odex + @apk_odex) . "\n"
		  ."      Has Dex=" . @contain_dex . "\n"
		  ."      Has Odex=" . @apk_odex . "\n"
		  ."        Arm=" . $armct . "\n"
		  ."        Arm64=" . $arm64ct . "\n"
		  ."        Both=" . $bothct . "\n"
		  ."        Unknown=" . $unknownct . "\n"
		  ."      No Dex or Odex=" . @apk_no_odex . "\n";
		  
	if(@noapks > 0) {
		print $out  "\n  No Apks:\n";
		foreach (@noapks) {
			print $out  "    $_\n";
		}
	}
	
	if(@multapks > 0) {
		print $out  "\n  Multi-Apks:\n";
		foreach my $hash_ref (@multapks) {
			my %hash = %{$hash_ref};
			my $name = $hash{'name'};
			my @apks = @{$hash{'apks'}};
			print $out  "    $name:\n";
			foreach (@apks) {
				print $out  "      $_\n";
			}
		}
	}
	
	if((@contain_dex + @apk_no_odex + @apk_odex) > 0) {
		print $out  "\n  Single Apk:\n";
		
		if(@contain_dex > 0) {
			print $out  "\n    Has Dex:\n";
			foreach (@contain_dex) {
				print $out  "      $_\n";
			}
		}
		
		if(@apk_odex > 0) {
			print $out  "\n    Has Odex:\n";
			foreach my $hash_ref (@apk_odex) {
				my %hash = %{$hash_ref};
				my $name = $hash{'name'};
				my $has32 = $hash{'has32'};
				my $has64 = $hash{'has64'};
				my @unknown = @{$hash{'unknown'}};
				print $out  "      $name: arm=$has32 arm64=$has64\n";
				foreach (@unknown) {
					print $out  "        Unknown: $_\n";
				}
			}
		}
		
		if(@apk_no_odex > 0) {
			print $out  "\n    No Dex or Odex:\n";
			foreach (@apk_no_odex) {
				print $out  "      $_\n";
			}
		}
	}
	
	return (\@apps,\@noapks,\@multapks,\@contain_dex,\@apk_no_odex,\@apk_odex,$armct,$arm64ct,$bothct,$unknownct);
}