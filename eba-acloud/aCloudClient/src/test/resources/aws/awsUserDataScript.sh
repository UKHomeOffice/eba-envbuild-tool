#!/bin/bash
hostname=<HOSTNAME>
domain=<DOMAIN>
primaryNic=<PRIMARY_NIC>
primaryIp=<PRIMARY_IP>
nameServer=<NAMESERVER>
toolingDomain=<TOOLING_DOMAIN>
userdataPrimaryNic=<USER_DATA_PRIMARY_NIC>
elasticIpAddress=<EXTERNAL_IP_ADDRESS>

<USER_DATA_NIC_INFO>

sed -i -e '/GATEWAY/d' /etc/sysconfig/network-scripts/ifcfg-eth*
gateway=$(/usr/local/bin/facter network_$primaryNic | awk -F. -v OFS=. '{sub('0','1',$4); print}')
echo "GATEWAY=$gateway" >> /etc/sysconfig/network
cd /etc/sysconfig/network-scripts
#for nic in $(ip link | awk -F: '$2 ~ /^ eth/ { sub(/ /, "", $2); print $2 }'); do 
#    if [ $nic = "eth0" ]; then 
#	    continue; 
#    fi 
    #cp /etc/sysconfig/network-scripts/ifcfg-eth0 /etc/sysconfig/network-scripts/ifcfg-$nic
#done

for nic in $(ip link | awk -F: '$2 ~ /^ eth/ { sub(/ /, "", $2); print $2 }'); do
    #if [ $nic = "eth0" ]; then 
	#    continue; 
    #fi 
    echo "#Dynamically written by cloud tool" > /etc/sysconfig/network-scripts/ifcfg-$nic
    echo "DEVICE=$nic" >> /etc/sysconfig/network-scripts/ifcfg-$nic
    echo "BOOTPROTO=none" >> /etc/sysconfig/network-scripts/ifcfg-$nic
    echo "ONBOOT=yes" >> /etc/sysconfig/network-scripts/ifcfg-$nic
    echo "HOTPLUG=yes" >> /etc/sysconfig/network-scripts/ifcfg-$nic
    echo "TYPE=Ethernet" >> /etc/sysconfig/network-scripts/ifcfg-$nic
    appendip=_ip
    evalNic=$nic$appendip
    appendmac=_mac
    evalMac=$nic$appendmac
    eval "echo IPADDR=\$$evalNic >> /etc/sysconfig/network-scripts/ifcfg-$nic"
    eval "echo HWADDR=\$$evalMac >> /etc/sysconfig/network-scripts/ifcfg-$nic"
    echo "NETMASK=255.255.255.0" >> /etc/sysconfig/network-scripts/ifcfg-$nic
done
service network restart