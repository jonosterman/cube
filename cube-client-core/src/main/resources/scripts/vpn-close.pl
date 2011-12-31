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
		$tap,$noBridge
	);
	$noBridge = 0;
	## Parse arguments
	GetOptions(
		'tap=s'          => \$tap,
		'no-bridge+'        => \$noBridge
	) or die "parameters error. $?";
	
	
	## parameters validation // unquoting
	if ( ! ($tap =~ m/^tap[-_\w]+$/) ) { die "wrong --tap format [$tap]"; }
	
	my $tapvbox = "";
	my $bridge = "";
	if ($noBridge == 0) {
		## vbox_hack : OpenVPN and VirtualBox seems to have some problems: If we start VirtualBox 
		## before OpenVPN, it lock the TAP and OpenVPN fails to open its tunnel. Therefore we
		## add an intermediate bridge
		## @see: vpn-open.pl, vpn-close.pl, vbox-tuncreate.pl, vbox-tundelete.pl
		$tapvbox = $tap;
		$bridge = $tap;
		$tap =~ s/^tap/tapX/;
		$bridge =~ s/^tap/br/;
		`ifconfig $bridge down`;
		`brctl delbr $bridge`;
		## end of vbox_hack
	} else {
		`ifconfig $tap down`;
	}
	
	
	## check if running
	my $pidFile = "/tmp/openvpn-${tap}.pid";
    if ( -e $pidFile && system("ps -p `cat $pidFile` > /dev/null") == 0) {
    	## OpenVPN is running. Kill it
    	my $pid = `cat ${pidFile}`;
        print "[DEBUG] Kill openvpn process [$pid]\n";
    	runCmd("kill -9 $pid");
    }
    ## remove tap (sometime the command hang and never ends: uninterruptible sleep state. Therefore we fork this command with '&')
    `tunctl -d $tap &`;
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



###################################################
# Main
###################################################
vpnclose();


