#!/usr/bin/perl -w
use strict;
use warnings;

use File::Spec;

my $cmd = $ARGV[0];

open(my $fd, ">", File::Spec->catfile("pids",$$));
close $fd;

setpgrp(0,$$);
system($cmd);

open(my $fd2, ">", File::Spec->catfile("pids",$$));
print $fd2 "$? exit\n";
close $fd2;

exit($?);
