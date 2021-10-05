#!/usr/bin/perl

# Turn on print flushing.
BEGIN { $| = 1 }
 
use strict;
use warnings;

use FindBin;
use lib $FindBin::Bin;
use Extract::Huawei;

# This is a temporary sample script that shows how to call the 
# run_huawei_extract method from another perl script. Once this
# call is integrated into the main script it can be removed.

run_huawei_extract($ARGV[0]);
