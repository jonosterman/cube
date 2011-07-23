#!/usr/bin/env perl
#
# Copyright (C) 2011 / cube-team <https://cube.forge.osor.eu>
#
# Licensed under the Apache License, Version 2.0 (the "License").
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

##
## This script is helpful to cleanup all rest resource while developing an
## debugging container scripts.
##

use strict;
use File::Find;
use File::Listing;
use Getopt::Long;

## Trim string
sub trim($) {
	my $string = shift;
	$string =~ s/^\s+//;
	$string =~ s/\s+$//;
	return $string;
}

#############################################################################
#############################################################################
#############################################################################
## Parse arguments
my ( $encFile, $mapDev, $loopDev, $mp );

## ensure that script is run as root
if ( "root" ne $ENV{'USER'} ) {
	die "Root priviledges needed.";
}

## list mounted file
my @mounts = `mount | grep "temp-volume-0x"`;
foreach my $mount (@mounts) {
	$mount = trim($mount);
	if ($mount =~ /\/dev\/mapper\/(.*?) on (.*?) type ext.*/) {
		my $map = $1;
		my $mp = $2;
		## just because "losetup -a" truncate mountpoint names
		my $mpShort =  substr $mp, 0, 60;
		my $loopDev = trim(`losetup -a | grep "$mpShort" | sed 's/:.*//'`);
		print "mount found [$map] [$mp] [$loopDev]\n";		
		## unmount
		system("umount $mp");
		system("rmdir $mp");
		## close crypto
		system("cryptsetup luksClose $map");
		## free loop device
		system("losetup -d $loopDev");
	}
}


my @mapped = `ls /dev/mapper/temp*`;
foreach my $mapp (@mapped) {
 print "[INFO ] unmap $mapp\n";
 `cryptsetup luksClose $mapp`;	
}

`losetup -d /dev/loop0`;
`losetup -d /dev/loop1`;
`losetup -d /dev/loop2`;
`losetup -d /dev/loop3`;
`losetup -d /dev/loop4`;
`losetup -d /dev/loop5`;
`losetup -d /dev/loop6`;
`losetup -d /dev/loop7`;
