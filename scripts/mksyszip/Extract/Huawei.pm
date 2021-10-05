#!/usr/bin/perl

package Extract::Huawei;

use strict;
use warnings;

BEGIN {
	use Exporter;
	our $VERSION = 1.00; # set the version for version checking
	our @ISA = qw(Exporter); # Inherit from Exporter to export functions and variables
	our @EXPORT_OK = qw(run_huawei_extract calc_crc_threaded); # Could be exported
	our @EXPORT = qw(run_huawei_extract); # Exported by default
	$| = 1; # Turn on print flushing.
}

use Thread::Pool;
use System::Info;
use File::Path qw(make_path remove_tree);
use File::Spec;
use Cwd 'abs_path';
use File::Basename;
use File::Find;
use File::Remove 'remove';

# {
# 	my $file_path = '';
# 	if(@ARGV) {
# 		$file_path = $ARGV[0];
# 	}
# 	run_huawei_extract($file_path);
# }

sub run_huawei_extract {

	my ($file_path) = @_;
	my $out_dir = undef;
	my $infile = undef;
	my $pos = 0;
	my($wca, $script_dir, $wcb) = fileparse(__FILE__);
	my $crc_exe = File::Spec->catfile($script_dir, "crc_huawei");
	my $use_c_crc = system("$crc_exe $crc_exe >/dev/null 2>&1") == 0;
	# TODO - The crc_huawei was compiled for 32 bit systems and cannot open files larger than 2^31 - 1. This
	# is a problem now a days because the system images are generally larger than this. As I don't have the
	# source code for this executable this program needs to be rewritten.
	
	if(!defined $file_path || $file_path eq '' || ! -e $file_path || ! -r $file_path || ! -f $file_path){
		die("Please provide a valid file containing the system image.\n");
	}
	
	my($wc1, $working_dir, $wc2) = fileparse(abs_path($file_path));
	$out_dir = File::Spec->catdir($working_dir,'out_huawei');
	
	if(-e $out_dir){
		if(-r $out_dir && -w $out_dir && -d $out_dir) {
			remove_tree($out_dir,{safe => 0});
		} else {
			die("Please provide a valid output directory.\n");
		}
	}
	make_path($out_dir);
	
	open($infile, $file_path) or die "Unable to open $file_path for reading - $!\n";
	binmode $infile;
	
	while (!eof($infile)) {
		$pos = find_next_file($pos,$infile);
		seek($infile, $pos, 0);
		$pos = dump_file($infile,$out_dir,$use_c_crc,$crc_exe);
	}

	close $infile or die "Unable to close $file_path - $!\n";
}

# Find the next file block in the main file
sub find_next_file {
	my ($pos, $infile) = @_;
	my $buffer = undef;
	my $skipped = 0;

	#Reads in first 4 bytes but does not increment counter
	read($infile, $buffer, 4);
	#Keeps reading until the 4 byte seperator is found
	while ($buffer ne "\x55\xAA\x5A\xA5" && !eof($infile)) {
		read($infile, $buffer, 4);
		$skipped += 4;
	}

	#Pointer ends up being at the beginning of the 4 byte seperator before the next image
	#If it ends up anywhere else (i.e. 4 byte before then end of file) it will cause an error
	#Should reach end of file here because dumping the last image should put it at eof so this will not be called again
	return $pos + $skipped;
}
 
# Unpack a file block and output the payload to a file.
sub dump_file {
	my ($infile,$out_dir,$use_c_crc,$crc_exe) = @_;
	my $buffer = undef;
	my $buffer_size = undef;
	my $header_size = undef;
	my $hw_id = undef;
	my $file_seq = undef;
	my $img_size = undef;
	my $img_date = undef;
	my $img_time = undef;
	my $img_name = undef;
	my $out_file = undef;
	my $source_crc = undef;
	my $calc_crc = undef;
 
	# Packet Identifier - Verify the identifier matches
	read($infile, $buffer, 4); 
	unless ($buffer eq "\x55\xAA\x5A\xA5") { die "Unrecognised file format. Wrong identifier.\n"; }
	
	# Packet Length - Reads in a (little-endian) order unsigned 32 bit value as an integer
	read($infile, $buffer, 4); 
	$header_size = unpack("V", $buffer);
	
	# Always 1
	read($infile, $buffer, 4); 
	
	# Hardware ID
	$buffer_size = read($infile, $buffer, 8);
	$hw_id = printable(unpack("A$buffer_size", $buffer));
	
	# File Sequence
	read($infile, $buffer, 4); 
	$file_seq = unpack("V",$buffer);
	
	# Data file length - Reads in a (little-endian) order unsigned 32 bit value as an integer
	read($infile, $buffer, 4); 
	$img_size = unpack("V", $buffer);
	
	# Image date
	$buffer_size = read($infile, $buffer, 16);
	$img_date = printable(unpack("A$buffer_size", $buffer));
	
	# Image time
	$buffer_size = read($infile, $buffer, 16); 
	$img_time = printable(unpack("A$buffer_size", $buffer));
	
	# Image name
	$buffer_size = read($infile, $buffer, 16); 
	$img_name = printable(unpack("A$buffer_size", $buffer));
	
	read($infile, $buffer, 16); # Blank
	read($infile, $buffer, 2); # Checksum of the header maybe?
	read($infile, $buffer, 2); # Always 0x1000?
	read($infile, $buffer, 2); # Blank
	
	# Up to this point it has read in 98 bytes including the packet identifier
	# Grab the checksum of the file
	$buffer_size = read($infile, $buffer, $header_size - 98);  
	$source_crc = get_hex_str($buffer, $buffer_size);
	
	if($img_name eq "") {
		$img_name = "unknown";
	} else {
		$img_name = lc $img_name;
	}
	
	printf("Name: %s - Size: %d - Seq: %d - Date: %s_%s - HW: %s\n",$img_name,$img_size,$file_seq,$img_date,$img_time,$hw_id);
	
	$out_file = File::Spec->catfile($out_dir, $img_name . ".img");
	my $count = 2;
	while(-e $out_file) {
		$out_file = File::Spec->catfile($out_dir, $img_name . "_" . $count . ".img");
		$count++;
	}
	
	# Dump the payload.
	read($infile, $buffer, $img_size);
	open(OUTFILE, ">", $out_file) or die "Unable to create $out_file - $!\n";
	binmode OUTFILE;
	print OUTFILE $buffer;
	close OUTFILE;
	
	# Ensure we finish on a 4 byte boundary alignment.
	my $remainder = 4 - (tell($infile) % 4);
	if ($remainder < 4) {
		# We can ignore the remaining padding.
		read($infile, $buffer, $remainder);
	}
	
	printf("    - Written to '%s'\n",$out_file);
	
	if($use_c_crc) {
		$calc_crc = `$crc_exe $out_file`;
		$calc_crc = trim($calc_crc);
	} else {
		$calc_crc = calc_crc_threaded($out_file);
	}

	if ($calc_crc eq $source_crc) {
		print "    - CRC Okay\n";
	} else {
		print "    - ERROR: CRC did not match\n";
	}
	
	return (tell($infile));
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
		
sub get_hex_str {
	my $i;
	my ($buf, $buf_size) = @_;
	my $ret = "";

	for(my $i = 0; $i < $buf_size; $i++) {
		my $byte = unpack("x$i C1", $buf);
		$ret .= sprintf("%02X",$byte);
	}

	return $ret;
}
		
# Function for calculating the CRC as it is stored in .app files. Note the CRC stored in .app files is of variable
# length because of the dumb way in which the CRC is computed for a file. This works as follows:
#	 1. Start at the file head
#	 2. Read into buffer 4096 bytes and pass buffer to CRC16 calculator
#	 3. CRC16 function spits out 2 bytes which are appended to the end of the overall CRC string.
#	 4. Repeat steps 2-3 until the end of the file is reached.
# The CRC16 function appears to be some variant where the Polynomial=0x8408 and the Initial Value=0xFFFF. This 
# variant likely has a name but it is unclear exactly which this variant is because the code for this was reverse 
# engineered from the executable with no additional information.
sub calc_crc {

	my $buffer = undef;
	my ($file_path) = @_;
	my $ret = "";

	open(FILE, '<', $file_path) or die "Unable to open file $file_path for reading - $!\n";
	binmode(FILE);

	while (!eof(FILE)) {
		my $size = read(FILE, $buffer, 4096);
		$ret .= sprintf("%04X",crc16($buffer, $size));
	}

	close(FILE) or die "Unable to close file $file_path - $!\n";

	return $ret;

}

sub calc_crc_threaded {

	my ($file_path) = @_;
	my $ret = "";
	my @jobids = ();
	my $cpu_count = cpu_count();
	my $pool = Thread::Pool->new(
		{
			optimize => 'memory',
			do => \&crc16,
			workers => $cpu_count
		}
	);

	open(FILE, '<', $file_path) or die "Unable to open file $file_path for reading - $!\n";
	binmode(FILE);
	
	while (!eof(FILE)) {
		my $buffer = undef;
		my $size = read(FILE, $buffer, 4096);
		my $jobid = $pool->job($buffer, $size);
		push @jobids, $jobid;
	}
	
	close(FILE) or die "Unable to close file $file_path - $!\n";
	
	foreach my $id (@jobids) {
		$ret .= sprintf("%04X",$pool->result($id));
	}

	$pool->shutdown;
	
	return $ret;
	
}

sub crc16 {

	my ($buffer, $size) = @_;
	my $res = 0xFFFF;
	
	for(my $i = 0; $i < $size; $i++) {
		my $byte = unpack("x$i C1",$buffer);
		for(my $j = 0; $j < 8; $j++) {
			if((($res ^ $byte) & 1) != 0) {
				$res = ($res >> 1) ^ 0x8408;
			} else {
				$res >>= 1;
			}
			$byte >>= 1;
		}
	}
	$res = ~$res & 0xFFFF;
	$res = (($res << 8) & 0xFF00) | (($res >> 8) & 0xFF);
	
	return $res;
	
}

sub cpu_count {
	my $si = System::Info->new;
	my $cpu_str = $si->ncpu;
	my $ret = undef;
	if($cpu_str =~ m/^\s*(\d+)\s*(?:$|\[(\d+)\s+cores\]\s*$)/) {
		if(defined($2)) {
			$ret = $1 * $2;
		} else {
			$ret = $1;
		}
	} else {
		die "Failed to parse cpu count from $cpu_str\n";
	}
	return $ret;
}

1;