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

## registervbox --uuid
sub startvbox() {
	my ( $uuid, $snapshotFolder );
	## Parse arguments
	GetOptions(
		'uuid=s'     => \$uuid,
		'snapshot:s' => \$snapshotFolder
	  )
	  or die "parameters error. $?";
	## parameters validation // unquoting
	if (
		!(
			$uuid =~
m/^[-0-9a-f]{8}\-[-0-9a-f]{4}\-[-0-9a-f]{4}\-[-0-9a-f]{4}\-[-0-9a-f]{12}$/
		)
	  )
	{
		die "wrong uuid format [$uuid]";
	}
	if ( !( $snapshotFolder =~ m/^(\/[-_a-zA-z0-9]+)+\/?$/ ) ) {
		die "invalid value for option --snapshot [$snapshotFolder]";
	}

	## check if snapshot exists
	if ( -e "$snapshotFolder/state.sav" ) {
		print "[DEBUG] restore snapshot\n";
		runCmd("VBoxManage -nologo adoptstate $uuid $snapshotFolder/state.sav");
	}

	## start VM
	#runCmd("setsid nice -1 VBoxSDL --startvm $uuid --evdevkeymap &");
	#runCmd("setsid nice -1 VBoxSDL --startvm $uuid &");
	runCmd("setsid VBoxSDL --startvm $uuid &");

	#runCmd("VBoxManage -nologo startvm $uuid --type sdl");
	#my @processes =
	#  `ps -ef | grep VBoxSDL | grep -v "grep" | awk '{ print \$2 }'`;
	#for my $line (@processes) {
#		my $pid = int($line);
#		`sudo renice -1 $pid`;
#	}
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
sub trim($) {
	my $string = shift;
	$string =~ s/^\s+//;
	$string =~ s/\s+$//;
	print "[DEBUG] trim [$string]\n";
	return $string;
}

###################################################
# Main
###################################################
startvbox();

