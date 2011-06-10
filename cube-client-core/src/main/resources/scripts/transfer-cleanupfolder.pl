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

## getstatus --uuid
sub cleanupfolder() {
	my (
		$folder
	);
	## Parse arguments
	GetOptions(
		'folder=s'          => \$folder
	) or die "parameters error. $?";
	## parameters validation // unquoting
	if ( $folder eq '' || ! ($folder =~ m/^(\/[-_a-zA-z0-9]+)+\/?$/) ) { die "invalid value for option --folder [$folder]"; }
	runCmd("rm -rf $folder/*");
}

###################################################
# Helper
###################################################

## Verbose system execution routine
sub runCmd {
	my ($cmd) = $_[0];
	if ( system($cmd) ) {
		die "Failed to execute [$cmd]";
	}
}
## Trim string
sub trim($)
{	
	my $string = shift;
	$string =~ s/^\s+//;
	$string =~ s/\s+$//;
	#print "[DEBUG] trim [$string]\n";
	return $string;
}


###################################################
# Main
###################################################
cleanupfolder();


