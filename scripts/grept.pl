#grep -rsIPzo -m1 "^    From: (.*)_\n^    To: \1_ \+ 1$" * | sort | uniq

use strict;
use warnings;
use Cwd;

use File::Find;
use File::Basename;
use Getopt::Long;

my $cwd = cwd;
my $in_files = ".";
my $first_only;
my $count;
my $name_only;
my $inverse;

GetOptions ("i=s" => \$in_files,
				 "f" =>  \$first_only,
				 "c" => \$count,
				 "n" => \$name_only,
				 "i" => \$inverse
);

if($inverse){
	if($first_only || $count){
		print "Cannot have i with either c or f enabled at the same time!\n" and die;
	}
	if(!$name_only){
		$name_only = 1;
	}
}

my $arg_check = 0;
if($first_only){
	$arg_check += 1;
}
if($count){
	$arg_check += 1;
}
if($name_only){
	$arg_check += 1;
}
if($arg_check > 1){
	print "Cannot have f, c, or n enabled at the same time!\n" and die;
}

sub trim($) {
	my $string = shift;
	$string =~ s/[\r\n]+//g;
	$string =~ s/\s+$//;
	return $string;
}

find(\&findfiles, $cwd);

sub findfiles {
	my $file = $File::Find::name; 
	
	return unless -f $file;                                                               # process files (-f), not directories
	return unless $_ =~ m/$in_files/io;                                             # check if file matches input regex
	                                                                                             # /io = case-insensitive, compiled
                                                                                                 # $_ = just the file name, no path
	
	open my $fh, '<:encoding(UTF-8)', $file or die "\n* Couldn't open ${file}\n\n";   # Open file and search for matching contents
	
	my $found = 0;                                                                        # used to keep track of first match
    $file =~ s/^\Q$cwd\E//g;                                                              # remove current working directory

	if(!(-B $file)){
		my $num_matches = 0;
		while (my $line1 = <$fh>) {
			defined(my $line2 = <$fh>) or last;
			
			$line1 =~ s/\R//g;
			$line2 =~ s/\R//g;
			
			if(my ($part1) = $line1 =~ /^\s+From:\s+(.*)_$/){
				if($line2 =~ /^\s+To:\s+\Q$part1\E_\s+(?:\+|-)\s+(?:-|\+|)1$/){
					$found = 1;
					if(!$inverse){
						if($count){
							$num_matches += 1;
						}elsif($name_only){
							print $file."\n"
						}else{
							print $file.": ".$line1.$line2."\n";
						}
					}
					
					if($first_only || $name_only){
						last;
					}
				}
			}
		}
		if($inverse && !$found){
			print "$file\n";
		}elsif(!$inverse && $count){
			print "$file: $num_matches\n";
		}
	}
	close $fh;
}
