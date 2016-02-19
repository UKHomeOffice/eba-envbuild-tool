#!/bin/bash
LOG=/root/post_customize.log
hostname=USER_DATA_HOSTNAME
domain=USER_DATA_DOMAIN
cat > /etc/hosts << EOF
127.0.0.1 localhost.localdomain localhost
$(/usr/local/bin/facter -p ipaddress_eth0) ${hostname}.${domain} ${hostname}
EOF
sed -i -e '/GATEWAY/d' /etc/sysconfig/network-scripts/ifcfg-eth*
service network restart