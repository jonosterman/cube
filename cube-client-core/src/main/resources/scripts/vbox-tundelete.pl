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

## registervbox --tun <tun name>
sub tundelete() {
	my ( $tap );
	## Parse arguments
	GetOptions( 'tun=s' => \$tap ) or die "parameters error. $?";
	runCmd("pkill -f $tap");
	runCmd("ifconfig $tap down");
	#runCmd("tunctl -d $tun");
	runCmd("openvpn --rmtun $tap");
	
	## vbox_hack : OpenVPN and VirtualBox seems to have some problems: If we start VirtualBox 
	## before OpenVPN, it lock the TAP and OpenVPN fails to open its tunnel. Therefore we
	## add an intermediate bridge
	## @see: vpn-open.pl, vpn-close.pl, vbox-tuncreate.pl, vbox-tundelete.pl
	my $bridge = $tap;
	$bridge =~ s/^tap/br/;
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
tundelete();

