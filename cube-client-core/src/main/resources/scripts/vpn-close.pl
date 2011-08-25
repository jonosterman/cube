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

## 
sub vpnclose() {
	my (
		$tap
	);
	## Parse arguments
	GetOptions(
		'tap=s'          => \$tap
	) or die "parameters error. $?";
	
	
	## parameters validation // unquoting
	if ( ! ($tap =~ m/^tap[-_\w]+$/) ) { die "wrong --tap format [$tap]"; }
	
	## vbox_hack : OpenVPN and VirtualBox seems to have some problems: If we start VirtualBox 
	## before OpenVPN, it lock the TAP and OpenVPN fails to open its tunnel. Therefore we
	## add an intermediate bridge
	## @see: vpn-open.pl, vpn-close.pl, vbox-tuncreate.pl, vbox-tundelete.pl
	my $tapvbox = $tap;
	my $bridge = $tap;
	$tap =~ s/^tap/tapX/;
	$bridge =~ s/^tap/br/;
	## end of vbox_hack
	
	## check if running
	my $isRunning = int(`ps -ef | grep "$tap" | grep -v "grep" | grep -v "vpn-close" | wc -l`);
	if ($isRunning != 0) {
		# kill already running vpn
		my @pids = `ps -ef | grep "$tap" | grep -v "grep" | grep -v "vpn-close" | awk '{ print \$2 }'`;
		for my $pid (@pids) {
			$pid = int($pid);
			print "[DEBUG] Kill openvpn process [$pid]\n";
			runCmd("kill -9 $pid");
		}
	}
	## remove tap (sometime the command hang and never ends: uninterruptible sleep state. Therefore we fork this command with '&')
	`tunctl -d $tap &`;
	
	
	## vbox_hack (continue): bridge to vpn
	`ifconfig $bridge 0.0.0.0 down`;
	`brctl delbr $bridge`;
	## end of vbox_hack
	
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
vpnclose();


