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
sub vpnopen() {
	my (
		$tap,$hostname,$port,$key,$cert,$ca,$vm
	);
	## Parse arguments
	GetOptions(
		'tap=s'          => \$tap,
		'hostname=s'          => \$hostname,
		'port=s'          => \$port,
		'key=s'          => \$key,
		'cert=s'          => \$cert,
		'vm=s'          => \$vm,
		'ca=s'          => \$ca
	) or die "parameters error. $?";
	## parameters validation // unquoting
	if ( ! ($tap =~ m/^tap[-_\w]+$/) ) { die "wrong --tap format [$tap]"; }
	if ( ! ($hostname =~ m/^[\d\w][-\._\w\d]+$/) ) { die "wrong --hostname format [$hostname]"; }
	if ( ! ($port =~ m/^[\d]+$/) ) { die "wrong --port format [$port]"; }
	if ( $key eq '' || ! ($key =~ m/^(\/[-=\._a-zA-z0-9]+)+\/?$/) ) { die "invalid value for option --key [$key]"; }
	if ( $cert eq '' || ! ($cert =~ m/^(\/[-=\._a-zA-z0-9]+)+\/?$/) ) { die "invalid value for option --cert [$cert]"; }
	if ( $ca eq '' || ! ($ca =~ m/^(\/[-=\._a-zA-z0-9]+)+\/?$/) ) { die "invalid value for option --ca [$ca]"; }
	
	if ( ! -e $key ) { die "file [$key] does not exists"; }
	if ( ! -e $cert ) { die "file [$cert] does not exists."; }
	if ( ! -e $ca ) { die "file [$ca] does not exists."; }


	## vbox_hack : OpenVPN and VirtualBox seems to have some conflicts: If we start VirtualBox 
	## before OpenVPN, it locks the TAP and OpenVPN fails to open its tunnel. Therefore we
	## add an intermediate bridge
	## @see: vpn-open.pl, vpn-close.pl, vbox-tuncreate.pl, vbox-tundelete.pl
	my $tapvbox = $tap;
	my $bridge = $tap;
	$tap =~ s/^tap/tapX/;
	$bridge =~ s/^tap/br/;
	## end of vbox_hack (see below for the rest of the hack)
	
	## check if vpn is still running
	my $isRunning = int(`ps -ef | grep "$tap" | grep -v "grep" | grep -v "vpn-open" | wc -l`);
	if ($isRunning != 0) {
		# kill already running vpn
		my @pids = `ps -ef | grep "$tap" | grep -v "grep" | grep -v "vpn-open" | awk '{ print \$2 }'`;
		for my $pid (@pids) {
			$pid = int($pid);
			print "[DEBUG] Kill openvpn process [$pid]\n";
			runCmd("kill -9 $pid");
		}
	}
	## disconnect vbox NIC (it will force guest to query a new dhcp address as soon it will reconnect)
	my $nicId = `su $ENV{SUDO_USER} -c "VBoxManage showvminfo $vm | grep "NIC" | grep "$tapvbox" | awk '{print 1}'"`;
	if ($nicId =~ /(\d+):/) {
		$nicId = int($1);
		print "[DEBUG] disconnect vbox NIC [$nicId]";
			  runCmd("su $ENV{SUDO_USER} -c \"VBoxManage modifyvm $vm --cableconnected$nicId off\"");				
	} else {
		print "[ERROR] invalid NIC : [$nicId]";
		$nicId = -1;
		exit 1; # exit with error
	}
	
	## open VPN
	print "[DEBUG] Start new openvpn process [setsid openvpn --client --remote $hostname $port --dev-type tap --dev $tap --proto udp --resolv-retry infinite --nobind --ca $ca --cert $cert --key $key --ns-cert-type server --comp-lzo --verb 3 --log /tmp/openvpn-${tap}.log]\n";
	runCmd("setsid openvpn --client --remote $hostname $port --dev-type tap --dev $tap --proto udp --resolv-retry infinite --nobind --ca $ca --cert $cert --key $key --ns-cert-type server --comp-lzo --verb 3 --log /tmp/openvpn-${tap}.log &");
	## wait tap to be defined
	my $timeout = 15; 
	while (int(`ifconfig -a | grep "$tap"| wc -l`) == 0 && $timeout-- > 0) {
		print "[DEBUG] wait tap to be configured ($timeout)..\n";
		sleep(1);
	}
	`touch /tmp/openvpn-${tap}.log`;
	## wait "Error/Exiting/exiting" or "Initialization Sequence Completed" messages)
	while (int(`cat /tmp/openvpn-${tap}.log | grep -i "error"| wc -l`) == 0 && int(`cat /tmp/openvpn-${tap}.log | grep -i "exiting"| wc -l`) == 0 && int(`cat /tmp/openvpn-${tap}.log | grep -i "Initialization Sequence Completed"| wc -l`) == 0 && $timeout-- > 0) {
		print "[DEBUG] wait vpn success or error message ($timeout)..\n";
		sleep(1);
	}
	## if timeout has been reached, kill buggy openvpn process
	if ($timeout < 0) {
		print "[ERROR] failed to configure interface $tap (timeout)\n";
		## dump logs
		print `cat /tmp/openvpn-${tap}.log`;
		
		## kill process	
		my @pids = `ps -ef | grep "$tap" | grep -v "grep" | grep -v "vpn-open" | awk '{ print \$2 }'`;
		for my $pid (@pids) {
			$pid = int($pid);
			print "[DEBUG] Kill openvpn process [$pid]\n";
			runCmd("kill -9 $pid");
		}		
	 } else {
		if (int(`cat /tmp/openvpn-${tap}.log | grep -i "Initialization Sequence Completed"| wc -l`) != 0) {
			## VPN is open
			print "[DEBUG] set interface $tap UP\n";
			## bring tap up
			runCmd("ifconfig $tap 0.0.0.0 up");
			## connect VM nic
			if ($nicId >= 0) {
			  print "[DEBUG] connect VM's NIC [$nicId]\n";
			  runCmd("su $ENV{SUDO_USER} -c \"VBoxManage modifyvm $vm --cableconnected$nicId on\"");				
			}
		} else {
			## VPN opening failed
			print "[ERROR] Failed to setup VPN\n";
			print `cat /tmp/openvpn-${tap}.log`;
		}
	}
	
	## vbox_hack (continue): bridge to vpn
	`brctl addbr $bridge`;
	`brctl stp $bridge off`;
	`brctl addif $bridge $tap`;
	`brctl addif $bridge $tapvbox`;
	`ifconfig $tap 0.0.0.0 up`;
	`ifconfig $tapvbox 0.0.0.0 up`;
	`ifconfig $bridge 0.0.0.0 up`;
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
vpnopen();


