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

sub createContainer {
	my ( $encFile, $size, $keyfile, $mountpoint, $sizeKo, $loopDev, $tempDev );
	## Parse arguments
	GetOptions(
		'f|file=s'  => \$encFile,
		's|size=s'    => \$size,
		'k|keyfile=s' => \$keyfile
	  )
	  or die;
	if ($encFile eq '' || $size eq '' || $keyfile eq '') { die "missing arguments: file[${encFile}] size[${size}] keyfile[${keyfile}]"; }
	  	
	## check if container file already exists 
	if (-e $encFile) {
		## exit with an error
		die "[ERROR] file [$encFile] already exists.";
	}
	## validate parameter 'size'
	if ($size =~ /^(\d+)M$/) {
		$size = $1*1024*1024;
	} elsif ($size =~ /^(\d+)G$/){
		$size = $1*1024*1024*1000;
	} elsif ($size =~ /^(\d+)$/){
		$size = $1;
	} else { 
		die "[ERROR] Invalid size: [$size]"
	}
	
	
	$sizeKo = int($size/1024);
	my $sizeKo_minus1 = $sizeKo-1; 
	info "Create dmcrypt volume file[${encFile}] size[${sizeKo} Kb] keyfile[${keyfile}]..\n";
	# for paranoid (slower) :"dd if=/dev/urandom of=${encFile}"
	# not paranoid :"dd if=/dev/zero of=${encFile} count=<size>"
	# not paranoid :"dd if=/dev/zero of=${encFile} seek=<size-1> count=<1>"
	if (execCmd("dd bs=1024 count=1 seek=${sizeKo_minus1}  if=/dev/zero of=${encFile}")) {
		die "[ERROR] Creation failed.";	
	}
	execCmd("chown $ENV{'SUDO_USER'}:vboxusers ${encFile}");
	info "Empty file initilized successfully\n";
	
	# create loop device on file
	my $shellRet = `losetup --show -f ${encFile}`;
	if ("${^CHILD_ERROR_NATIVE}" ne "0") { die "Unable to found free loop device"; }
	$loopDev = trim($shellRet);
	if ($loopDev !~/\/dev\/loop[0-9]/) {
		die "[ERROR] device '$loopDev' not valid.";
	}
	debug "Use loop device '$loopDev'\n";
	# encryption (dm-crypt + LUKS)
        # use xt and plain64 and key size 512 for extra security
	$tempDev = "temp-volume-0x".int(rand(100000));
	execCmd("cryptsetup luksFormat -q $loopDev -c aes-xts-plain64 -s 512 $keyfile") == 0 or die "Failed to create LUKS volume].  $?";
	execCmd("cryptsetup luksOpen -q --key-file $keyfile $loopDev $tempDev") == 0 or die "Failed to open LUKS volume. $?";
        # fill device with zero to initialize the random file (may take long but add extra security)
  # sloww...  execCmd("dd if=/dev/zero of=/dev/mapper/$tempDev bs=1024 count=${sizeKo}");
	# format device
	execCmd("mkfs.ext3 -q /dev/mapper/$tempDev") == 0 or die "Failed to format";
	# mount and fix permissions
	execCmd("mkdir -p /tmp/$tempDev") == 0 or die "Failed to mount formatted volume";
	execCmd("mount /dev/mapper/$tempDev /tmp/$tempDev");
	execCmd("chown $ENV{'SUDO_UID'}:vboxusers /tmp/$tempDev");
	# cleanup
	#execCmd("sync") == 0 or die "Sync failed. $?";
	execCmd("umount /tmp/$tempDev") == 0 or die "Failed to unmount volume. $?";
	execCmd("rmdir /tmp/$tempDev") == 0 or die "Failed to remove temporary mount point. $?";
	execCmd("cryptsetup luksClose $tempDev") == 0 or die "Failed unmap encrypted device. $?";
	execCmd("losetup -d $loopDev") == 0 or die "Failed free loop device. $?";
	
	print "Volume created successfully [$encFile]";	

}

##########################################
##########################################
##########################################

parameterCheck();
createContainer();
