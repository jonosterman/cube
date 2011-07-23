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

sub trim($)
{
	my $string = shift;
	$string =~ s/^\s+//;
	$string =~ s/\s+$//;
	return $string;
}

sub info($) {
	my $str = shift;
	print "[INFO ] $str\n";
}

sub debug($) {
	my $str = shift;
	print "[DEBUG] $str\n";
}

sub error($) {
	my $str = shift;
	print "[ERROR] $str\n";
}


sub usage {
  info "";
  info "Usage:";
  info "sudo $0";
  info "";
}

sub parameterCheck {
 if ($ENV{'USER'} ne "root") {
 	usage();
 	exit(1);
 }
 if ($ENV{'SUDO_USER'} eq "root") {
 	error "Root is not allowed to install, use sudo -u <user> instead!";
 	usage();
 	exit(1);
 }
}

sub execCmd($) {
	my $cmd = shift;
	debug "(shell) '$cmd'.";
	my $result = `$cmd`;
	my $ret = "${^CHILD_ERROR_NATIVE}";
	print "$result";
	return $ret;
}
sub readPropertiesFile($) {
	my $file = shift;
	my %hash;
	open my $in, $file or die $!;
	while(<$in>) {
   		$hash{$1}=$2 while m/(\S+)=(\S+)/g;
	}
	close $in;
	return %hash;
}

#############################################################################
#############################################################################
#############################################################################
sub unmountContainer {	
	## Parse arguments
	my ( $encFile, $mapDev, $loopDev, $mountpoint );
	## Parse arguments
	GetOptions(
		'f|file=s'  => \$encFile,
		'm|mountpoint=s'    => \$mountpoint
	  )
	  or die;
	if ( $encFile eq '' ) { die "missing arguments: file[${encFile}] "; }
	if ( $mountpoint eq '' ) { die "missing arguments: mountpoint[${mountpoint}] "; }
	
	## check if container exists
	if ( !-e $encFile ) {
		debug "file [$encFile] does not exists.";
		exit 0;
	}
	
	## check if lockfile exists
	my $lockfile = trim(`pwd`)."/../var/".trim(`echo "$encFile" | md5sum | awk '{print \$1}'`);
	if ( -e $lockfile ) {
		## Load lock file
		my %lock = readPropertiesFile($lockfile);
		my $errors = 0;
		print "[$lock{'file'}][$lock{'mountpoint'}][$lock{'loop'}][$lock{'mapping'}]\n";
		## Unmount 		
		if  (-e $lock{'mountpoint'}) {
			my $isMounted = `mount | grep "$lock{'mountpoint'}" | wc -l`;
			if ($isMounted != 0) {
				## try to unmount
			  if (execCmd("umount $lock{'mountpoint'}") !=0) {print "[WARN ] could not unmount directory $lock{'mountpoint'}\n";$errors++;}	
			}
			## try to delete mount point
			if (execCmd("rmdir $lock{'mountpoint'}") != 0) {print "[WARN ] could not remove mountpoint directory $lock{'mountpoint'}\n";$errors++;}
		} else {
			print "Skip unmount (mountpoint '$lock{'mountpoint'}' does not exist).\n";
		}		
		## close crypto device
		if  (-e "/dev/mapper/$lock{'mapping'}") {
			if (execCmd("cryptsetup luksClose $lock{'mapping'}") !=0) {print "[WARN ] Failed unmap encrypted device [$lock{'mapping'}].\n";$errors++;}
		} else {
			print "Skip luksClose (mapping device '/dev/mapper/$lock{'mapping'}' does not exist).\n";
		}
		## cleanup losetup
		if  (trim(`losetup -a | grep "$lock{'loop'}:" | wc -l`) ne "0") {
			if (execCmd("losetup -d $lock{'loop'}") !=0) {print "[WARN ] Failed to remove loop device [$lock{'loop'}].\n";$errors++;}
		} else {
			print "Skip losetup cleanup (loop device 'lock{'loop'}' not in use).\n";
			print "losetup -a | grep \"$lock{'loop'}:\" | wc -l\n";
		}
		## evtl remove lock file
		if ($errors == 0) {
			print "Remove Lock File";
			`mv $lockfile $lockfile.last`; 
		}
	} else {
		print "No lock file found. Skip.";
	}
	
	
}

########################################
########################################
########################################
parameterCheck
unmountContainer()

