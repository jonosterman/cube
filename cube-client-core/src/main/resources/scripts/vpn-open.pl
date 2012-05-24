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
		$tap,$hostname,$port,$key,$cert,$ca,$noBridge,$noLzo
	);
	$noBridge = 0;
        $noLzo = 0;
	## Parse arguments
	GetOptions(
		'tap=s'        => \$tap,
		'hostname=s'   => \$hostname,
		'port=s'       => \$port,
		'key=s'        => \$key,
		'cert=s'       => \$cert,
		'ca=s'         => \$ca,
		'no-bridge+'        => \$noBridge,
		'no-lzo+'        => \$noLzo
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

    my $tapvbox = "";
    my $bridge = "";
    if ($noBridge == 0) {
		## vbox_hack : OpenVPN and VirtualBox seems to have some conflicts: If we start VirtualBox 
		## before OpenVPN, it locks the TAP and OpenVPN fails to open its tunnel. Therefore we
		## add an intermediate bridge
		## @see: vpn-open.pl, vpn-close.pl, vbox-tuncreate.pl, vbox-tundelete.pl
		$tapvbox = $tap;
		$bridge = $tap;
		$tap =~ s/^tap/tapX/;
		$bridge =~ s/^tap/br/;
		## end of vbox_hack (see below for the rest of the hack)
    }
    
    
	## check if vpn is still running (pid file and matching process are present)
	my $pidFile = "/tmp/openvpn-${tap}.pid";
    if ( -e $pidFile && system("ps -p `cat $pidFile` > /dev/null") == 0) {
    	my $pid = int(`cat $pidFile`);
  	    ## kill the process
  	    print "[DEBUG] Kill old openvpn process [$pid]\n";
  	    system("kill -9 $pid");
    }
    
    ## DEBUG : check that openvpn is not running with another PID (if yes it is a bug).
    ##         Fix the situation by killing the process and write an error log 
    if ( system("pgrep $pidFile") == 0 ) {
      ## found one or more process
      my $pid = int(`cat $pidFile`);
      `echo "Running VPN process(es) found with wrong PID ($pid):" >> /tmp/openvpn.error`; 
      `ps -ef | grep openvpn >> /tmp/openvpn.error`;
      ## fix the problem
      `pkill $pidFile`;
      `echo "After fix report:" >> /tmp/openvpn.error`; 
      `ps -ef | grep openvpn >> /tmp/openvpn.error`;
    }
    
    
    ## 2nd check (if something get nasty with the pid file)
    #system("pkill -f -9 '$pidFile'");
    
	
    ## open VPN
    ## -> setsid: process will be started in background. We will monitor 
    ##    log file in order to detect success or failure and abort after a given timeout.
    ## -> --persist-key: key files are going to be shreded as soons as openvpn has been started. Therefore openvpn need to 
    ##    cache them.
    ## -> --resolv-retry infinite: ?
    ## -> --nobind: ?
    ## -> --writepid: keep a track of this process ID
    ## -> --fast-io: may boost throughput
    ## -> --log --verb 3: needed log config to monitor OpenVPN success or failure later in this script     
    ## openvpn command
    my $lzoOption = "";
    if ($noLzo == 0) {
	 $lzoOption = "--comp-lzo";
    }
	my $ocmd = "setsid openvpn --client --remote $hostname $port --dev-type tap --dev $tap --persist-key --proto udp --resolv-retry infinite --nobind --ca $ca --cert $cert --key $key --fast-io --ns-cert-type server $lzoOption --verb 3 --log /tmp/openvpn-${tap}.log --writepid ${pidFile} &";
	print "[DEBUG] Start new openvpn process [$ocmd]\n";
	runCmd($ocmd);
	## wait tap to be defined (openvpn create the tap device)
	my $timeout = 60;
	while (int(`ifconfig -a | grep "$tap"| wc -l`) == 0 && $timeout-- > 0) {
		print "[DEBUG] wait tap to be configured ($timeout)..\n";
		sleep(1);
	}
	## ensure that log file exists (openvpn will not create it if it failed in an early stage)
	`touch /tmp/openvpn-${tap}.log`;
	## wait "Error/Exiting/exiting" or "Initialization Sequence Completed" messages in log file
	while (int(`cat /tmp/openvpn-${tap}.log | grep -i "error"| wc -l`) == 0 && int(`cat /tmp/openvpn-${tap}.log | grep -i "exiting"| wc -l`) == 0 && int(`cat /tmp/openvpn-${tap}.log | grep -i "Initialization Sequence Completed"| wc -l`) == 0 && $timeout-- > 0) {
		print "[DEBUG] wait vpn success or error message ($timeout)..\n";
		sleep(1);
	}
	## if timeout has been reached, kill buggy openvpn process
	if ($timeout < 0) {
		print "[ERROR] failed to open VPN on $tap (timeout)\n";
		## dump logs
		print `cat /tmp/openvpn-${tap}.log`;
		
		## kill openvpn
		if ( -e $pidFile && system("ps -p `cat $pidFile` > /dev/null") == 0) {
     		my $pid = int(`cat $pidFile`);
  	    	## kill the process
  	    	print "[DEBUG] Kill old openvpn process [$pid]\n";
  	    	system("kill -9 $pid");
        }
		exit 11;
	 } else {
		if (int(`cat /tmp/openvpn-${tap}.log | grep -i "Initialization Sequence Completed" | wc -l`) != 0) {
			## VPN is open
			print "[DEBUG] set interface $tap UP\n";
		} else {
			## VPN opening failed
			print "[ERROR] Failed to setup VPN\n";
			print `cat /tmp/openvpn-${tap}.log`;
			exit 12;
		}
	}
	
	if ($noBridge == 0) {
		## vbox_hack (continue): bridge to vpn
		`brctl addbr $bridge`;
		`brctl stp $bridge off`;
		`brctl addif $bridge $tap`;
		`brctl addif $bridge $tapvbox`;
		`ifconfig $tap 0.0.0.0 up`;
		`ifconfig $tapvbox 0.0.0.0 up`;
		`ifconfig $bridge 0.0.0.0 up`;
	}

	## end of vbox_hack	
}

###################################################
# Helper
###################################################

## Verbose system execution routine
sub runCmd {
	my ($cmd) = $_[0];
	if ( system($cmd) ) {
	   print "[ERROR] Failed to execute [$cmd]";
           exit(7);
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


