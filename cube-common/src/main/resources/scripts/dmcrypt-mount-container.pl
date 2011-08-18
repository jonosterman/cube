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

use strict;
use File::Find;
use File::Listing;
use Getopt::Long;

## Trim string
sub trim($)
{
	my $string = shift;
	$string =~ s/^\s+//;
	$string =~ s/\s+$//;
	return $string;
}

#############################################################################
#############################################################################
#############################################################################
my ( $encFile, $size, $keyfile, $mountpoint, $sizeKo, $loopDev, $tempDev );
## Parse arguments
GetOptions(
	'f|file=s'  => \$encFile,
	'm|mountpoint=s'    => \$mountpoint,
	'k|keyfile=s' => \$keyfile
  )
  or die;
if ($encFile eq '' || $mountpoint eq '' || $keyfile eq '') { die "missing arguments: file[${encFile}] mountpoint[${mountpoint}] keyfile[${keyfile}]"; }
# foreach ( sort keys %ENV ) { print "$_ $ENV{$_}\n"; }
  
## ensure that script is run as root
if ("root" ne $ENV{'USER'}) {
	die "Root priviledges needed.";
}
## check volume file
if (! -e $encFile) {
	die "file [$encFile] doe not exist.";
}
## check volume file
if (! -e $keyfile) {
	die "key file [$keyfile] doe not exist.";
}

## check no lock is set for this file
my $lockfile = trim(`pwd`)."/../var/".trim(`echo "$encFile" | md5sum | awk '{print \$1}'`);
if ( -e $lockfile ) {
	print("Lockfile ------------ begin");
	print `cat $lockfile`;
	print("Lockfile ------------ end");
	print("Failed to mount volume, due to lock file ($lockfile).");
	## exit with a special code to be trapped in java
	exit 65;		
}

# create loop device 
$loopDev = trim(`losetup --show -f ${encFile}`);
if ($loopDev !~/\/dev\/loop[0-9]/) {
	print "[ERROR] device '$loopDev' not valid.";
	exit 0;
}
print "[DEBUG] Loop device setup on '$loopDev'\n";

## define encryption volume name 
$tempDev = "temp-volume-0x".int(rand(100000));

## create lock file for this file to avoid remounting it
`mkdir -p ../var/`;
`echo "
file=$encFile
loop=$loopDev
mapping=$tempDev
mountpoint=$mountpoint
" > $lockfile;`;
print("Lock file [$lockfile] created\n");

## create encryption device (dm-crypt + LUKS)
system("cryptsetup luksOpen -q --key-file $keyfile $loopDev $tempDev") == 0 or die "Failed to open LUKS volume. $?";

## mount and fix permissions
system("mkdir -p $mountpoint") == 0 or die "Failed to mount formatted volume";
system("mount /dev/mapper/$tempDev $mountpoint");
system("chown $ENV{'SUDO_UID'}:vboxusers $mountpoint");

print "Volume mounted successfully [$encFile]";	


