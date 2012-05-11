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
	print "[INFO ] $str";
}

sub debug($) {
	my $str = shift;
	print "[DEBUG] $str";
}

sub error($) {
	my $str = shift;
	print "[ERROR] $str";
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
	debug "(system) '$cmd'.\n";
	my $result = `$cmd`;
	my $ret = "${^CHILD_ERROR_NATIVE}";
	print "$result";
	return $ret;
}

sub changekey {
	my ( $device );
	## Parse arguments
	GetOptions(
		'd|device=s'  => \$device
		)
	  or die;
	if ($device eq '' ) { die "missing arguments: device[${device}]"; }
	  	
	## check if container file already exists 
	if (! -e $device) {
		## exit with an error
		die "[ERROR] device [$device] does not exist.";
	}

	#execCmd("cryptsetup luksChangeKey $device") == 0 or die "Failed change Key on device [$device]. $?";
	execCmd("cryptsetup luksAddKey $device") == 0 or die "Failed change Key on device [$device]. $?";
}

##########################################
##########################################
##########################################

parameterCheck();
changekey();
