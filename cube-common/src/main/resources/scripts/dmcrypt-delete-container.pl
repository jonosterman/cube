#!/usr/bin/env perl
#
# Copyright (C) 2011 / manhattan <https://cube.forge.osor.eu>
#
# Licensed under the Apache License, Version 2.0 (the "License");
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
	debug "(system) '$cmd'.";
	my $result = `$cmd 2>&1`;
	my $ret = "${^CHILD_ERROR_NATIVE}";
	print "$result";
	return $ret;
}

sub deleteContainer {
	my ( $encFile, $mountpoint );
	## Parse arguments
	GetOptions(
		'f|file=s'  => \$encFile,
		'm|mountpoint=s'    => \$mountpoint
	  )
	  or die;
	if ($encFile eq '' ) { die "missing arguments: file[${encFile}]"; }
	  	
	## check if container file realy exists 
	if (! -e $encFile) {
		## exit with an error
		die "[ERROR] file [$encFile] does not exists.";
	}
	
	## check if lock file exists
	my $lockfile = trim(`pwd`)."/../var/".trim(`echo "$encFile" | md5sum | awk '{print \$1}'`);
	if ( -e $lockfile ) {
		die "Container [$encFile] has a lock file [$lockfile] and should NOT be deleted.";
	}	
	
	## check if mounted
	my $result = trim(`mount | grep $mountpoint | awk '{ print \$1 }'`);
	if ( $result ne "" ) {
		## container is still mounted and should NOT be deleted.
		## if the file is deleted, "losetup -j" will bug!!!!
		die "Container is mounted ($result) and should NOT be deleted.";
	}
	## check if looped
	my $result = trim(`losetup -j $encFile | awk '{ print \$1 }'`);
	if ( $result ne "" ) {
		## container is still looped and should NOT be deleted.
		die "Container is looped ($result) and should NOT be deleted.";
	}
	## delete container
	if (execCmd("rm -f $encFile") != 0) { die "Failed to delete encrypted container."}; 
	## delete mountpoint (actually it should have been deleted during unmount. But who know?)
	if ( -e $mountpoint ) {
		if (execCmd("rmdir $mountpoint") != 0) { die "Failed to delete container's mountpoint."}; 
	}
	print "Volume successfully deleted [$encFile]";	
}

##########################################
##########################################
##########################################

parameterCheck();
deleteContainer();
