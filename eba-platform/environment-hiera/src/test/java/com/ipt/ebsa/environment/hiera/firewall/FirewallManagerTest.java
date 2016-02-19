package com.ipt.ebsa.environment.hiera.firewall;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

public class FirewallManagerTest {
	
	@Test
	public void testForward() {
		FirewallManager fwm = new FirewallManager(new File("src/test/resources/xl/IPTFirewallRulesNP.xlsx"));
		String rules = fwm.getForwardFirewallRules("II_PJT3_DEV1").toString();
		String withoutHash = rules.replaceAll("com\\.ipt\\.ebsa\\.environment\\.hiera\\.firewall\\.ForwardFirewallRule[^\\[]*", "");
		assertEquals("[[desc=logstash,dest=10.16.16.15,host=mfcam01,port=20514,protocol=tcp,rule name=<null>,rule number=<null>,source=10.43.128.0/17,zone=np-co-dat1-mabc.ipt.ho.local], [desc=logstash,dest=10.16.16.15,host=mfcam02,port=20514,protocol=tcp,rule name=<null>,rule number=<null>,source=10.43.128.0/17,zone=np-co-dat1-mabc.ipt.ho.local], [desc=syslog,dest=10.16.16.14,host=mfcam01,port=10514,protocol=tcp,rule name=<null>,rule number=<null>,source=10.43.128.0/17,zone=np-co-dat1-mabc.ipt.ho.local], [desc=syslog,dest=10.16.16.14,host=mfcam02,port=10514,protocol=tcp,rule name=<null>,rule number=<null>,source=10.43.128.0/17,zone=np-co-dat1-mabc.ipt.ho.local], [desc=zabbix server,dest=10.16.17.6,host=mfcam01,port=10051,protocol=tcp,rule name=<null>,rule number=<null>,source=10.43.128.0/17,zone=np-co-dat1-mabc.ipt.ho.local], [desc=zabbix server,dest=10.16.17.6,host=mfcam02,port=10051,protocol=tcp,rule name=<null>,rule number=<null>,source=10.43.128.0/17,zone=np-co-dat1-mabc.ipt.ho.local], [desc=dns,dest=10.16.16.7,host=mfcam01,port=53,protocol=udp,rule name=<null>,rule number=<null>,source=10.43.128.0/17,zone=np-co-dat1-mabc.ipt.ho.local], [desc=dns,dest=10.16.16.8,host=mfcam01,port=53,protocol=udp,rule name=<null>,rule number=<null>,source=10.43.128.0/17,zone=np-co-dat1-mabc.ipt.ho.local], [desc=dns,dest=10.16.16.9,host=mfcam01,port=53,protocol=udp,rule name=<null>,rule number=<null>,source=10.43.128.0/17,zone=np-co-dat1-mabc.ipt.ho.local], [desc=dns,dest=10.16.16.7,host=mfcam02,port=53,protocol=udp,rule name=<null>,rule number=<null>,source=10.43.128.0/17,zone=np-co-dat1-mabc.ipt.ho.local], [desc=dns,dest=10.16.16.8,host=mfcam02,port=53,protocol=udp,rule name=<null>,rule number=<null>,source=10.43.128.0/17,zone=np-co-dat1-mabc.ipt.ho.local], [desc=dns,dest=10.16.16.9,host=mfcam02,port=53,protocol=udp,rule name=<null>,rule number=<null>,source=10.43.128.0/17,zone=np-co-dat1-mabc.ipt.ho.local], [desc=ntp,dest=10.16.16.7,host=mfcam01,port=123,protocol=udp,rule name=<null>,rule number=<null>,source=10.43.128.0/17,zone=np-co-dat1-mabc.ipt.ho.local], [desc=ntp,dest=10.16.16.7,host=mfcam02,port=123,protocol=udp,rule name=<null>,rule number=<null>,source=10.43.128.0/17,zone=np-co-dat1-mabc.ipt.ho.local], [desc=yum,dest=10.16.16.16,host=mfcam01,port=80,protocol=tcp,rule name=<null>,rule number=<null>,source=10.43.128.0/17,zone=np-co-dat1-mabc.ipt.ho.local], [desc=yum,dest=10.16.16.16,host=mfcam02,port=80,protocol=tcp,rule name=<null>,rule number=<null>,source=10.43.128.0/17,zone=np-co-dat1-mabc.ipt.ho.local], [desc=ldaps,dest=10.16.16.4,host=mfcam01,port=636,protocol=tcp,rule name=<null>,rule number=<null>,source=10.43.128.0/17,zone=np-co-dat1-mabc.ipt.ho.local], [desc=ldaps,dest=10.16.16.4,host=mfcam02,port=636,protocol=tcp,rule name=<null>,rule number=<null>,source=10.43.128.0/17,zone=np-co-dat1-mabc.ipt.ho.local], [desc=mcollective,dest=10.16.16.11,host=mfcam01,port=61613,protocol=tcp,rule name=<null>,rule number=<null>,source=10.43.128.0/17,zone=np-co-dat1-mabc.ipt.ho.local], [desc=mcollective,dest=10.16.16.11,host=mfcam02,port=61613,protocol=tcp,rule name=<null>,rule number=<null>,source=10.43.128.0/17,zone=np-co-dat1-mabc.ipt.ho.local], [desc=puppet,dest=10.16.16.11,host=mfcam01,port=8140,protocol=tcp,rule name=<null>,rule number=<null>,source=10.43.128.0/17,zone=np-co-dat1-mabc.ipt.ho.local], [desc=puppet,dest=10.16.16.11,host=mfcam02,port=8140,protocol=tcp,rule name=<null>,rule number=<null>,source=10.43.128.0/17,zone=np-co-dat1-mabc.ipt.ho.local], [desc=zabbix agent,dest=10.16.17.6,host=mfcam01,port=10050,protocol=tcp,rule name=<null>,rule number=<null>,source=10.43.128.0/17,zone=np-co-dat1-mabc.ipt.ho.local], [desc=zabbix agent,dest=10.16.17.6,host=mfcam02,port=10050,protocol=tcp,rule name=<null>,rule number=<null>,source=10.43.128.0/17,zone=np-co-dat1-mabc.ipt.ho.local], [desc=clam,dest=10.16.16.7,host=mfcam01,port=80,protocol=tcp,rule name=<null>,rule number=<null>,source=10.43.128.0/17,zone=np-co-dat1-mabc.ipt.ho.local], [desc=clam,dest=10.16.16.7,host=mfcam02,port=80,protocol=tcp,rule name=<null>,rule number=<null>,source=10.43.128.0/17,zone=np-co-dat1-mabc.ipt.ho.local], [desc=mcollective,dest=10.43.128.0/17,host=mfcam01,port=61613,protocol=tcp,rule name=<null>,rule number=<null>,source=10.16.16.11,zone=np-co-dat1-mabc.ipt.ho.local], [desc=mcollective,dest=10.43.128.0/17,host=mfcam01,port=61613,protocol=tcp,rule name=<null>,rule number=<null>,source=10.16.16.12,zone=np-co-dat1-mabc.ipt.ho.local], [desc=mcollective,dest=10.43.128.0/17,host=mfcam01,port=61613,protocol=tcp,rule name=<null>,rule number=<null>,source=10.16.16.13,zone=np-co-dat1-mabc.ipt.ho.local], [desc=mcollective,dest=10.43.128.0/17,host=mfcam02,port=61613,protocol=tcp,rule name=<null>,rule number=<null>,source=10.16.16.11,zone=np-co-dat1-mabc.ipt.ho.local], [desc=mcollective,dest=10.43.128.0/17,host=mfcam02,port=61613,protocol=tcp,rule name=<null>,rule number=<null>,source=10.16.16.12,zone=np-co-dat1-mabc.ipt.ho.local], [desc=mcollective,dest=10.43.128.0/17,host=mfcam02,port=61613,protocol=tcp,rule name=<null>,rule number=<null>,source=10.16.16.13,zone=np-co-dat1-mabc.ipt.ho.local], [desc=puppet,dest=10.43.128.0/17,host=mfcam01,port=8140,protocol=tcp,rule name=<null>,rule number=<null>,source=10.16.16.11,zone=np-co-dat1-mabc.ipt.ho.local], [desc=puppet,dest=10.43.128.0/17,host=mfcam01,port=8140,protocol=tcp,rule name=<null>,rule number=<null>,source=10.16.16.12,zone=np-co-dat1-mabc.ipt.ho.local], [desc=puppet,dest=10.43.128.0/17,host=mfcam01,port=8140,protocol=tcp,rule name=<null>,rule number=<null>,source=10.16.16.13,zone=np-co-dat1-mabc.ipt.ho.local], [desc=puppet,dest=10.43.128.0/17,host=mfcam02,port=8140,protocol=tcp,rule name=<null>,rule number=<null>,source=10.16.16.11,zone=np-co-dat1-mabc.ipt.ho.local], [desc=puppet,dest=10.43.128.0/17,host=mfcam02,port=8140,protocol=tcp,rule name=<null>,rule number=<null>,source=10.16.16.12,zone=np-co-dat1-mabc.ipt.ho.local], [desc=puppet,dest=10.43.128.0/17,host=mfcam02,port=8140,protocol=tcp,rule name=<null>,rule number=<null>,source=10.16.16.13,zone=np-co-dat1-mabc.ipt.ho.local], [desc=ssh,dest=10.43.128.0/17,host=mfcam01,port=22,protocol=tcp,rule name=<null>,rule number=<null>,source=10.16.1.5,zone=np-co-dat1-mabc.ipt.ho.local], [desc=ssh,dest=10.43.128.0/17,host=mfcam02,port=22,protocol=tcp,rule name=<null>,rule number=<null>,source=10.16.1.5,zone=np-co-dat1-mabc.ipt.ho.local], [desc=ssh,dest=10.43.128.0/17,host=mfcam01,port=22,protocol=tcp,rule name=<null>,rule number=<null>,source=10.16.32.0/24,zone=np-co-dat1-mabc.ipt.ho.local], [desc=ssh,dest=10.43.128.0/17,host=mfcam02,port=22,protocol=tcp,rule name=<null>,rule number=<null>,source=10.16.32.0/24,zone=np-co-dat1-mabc.ipt.ho.local], [desc=zabbix agent,dest=10.43.128.0/17,host=mfcam01,port=10050,protocol=tcp,rule name=<null>,rule number=<null>,source=10.16.17.6,zone=np-co-dat1-mabc.ipt.ho.local], [desc=zabbix agent,dest=10.43.128.0/17,host=mfcam02,port=10050,protocol=tcp,rule name=<null>,rule number=<null>,source=10.16.17.6,zone=np-co-dat1-mabc.ipt.ho.local], [desc=ssb (II_PJT3_DEV1),dest=10.43.0.3,host=afcbm01,port=9090,protocol=tcp,rule name=<null>,rule number=<null>,source=10.16.33.0/24,zone=np-co-dat1-inbc.ho.ipt.local], [desc=ssb (II_PJT3_DEV1),dest=10.43.0.3,host=afcbm02,port=9090,protocol=tcp,rule name=<null>,rule number=<null>,source=10.16.33.0/24,zone=np-co-dat1-inbc.ho.ipt.local], [desc=app (II_PJT3_DEV1),dest=10.43.0.12,host=afcbm01,port=8080,protocol=tcp,rule name=<null>,rule number=<null>,source=10.16.33.0/24,zone=np-co-dat1-inbc.ho.ipt.local], [desc=app (II_PJT3_DEV1),dest=10.43.0.12,host=afcbm02,port=8080,protocol=tcp,rule name=<null>,rule number=<null>,source=10.16.33.0/24,zone=np-co-dat1-inbc.ho.ipt.local], [desc=cdp (II_PJT3_DEV1),dest=10.43.0.18,host=afcbm01,port=8180,protocol=tcp,rule name=<null>,rule number=<null>,source=10.16.33.0/24,zone=np-co-dat1-inbc.ho.ipt.local], [desc=cdp (II_PJT3_DEV1),dest=10.43.0.18,host=afcbm02,port=8180,protocol=tcp,rule name=<null>,rule number=<null>,source=10.16.33.0/24,zone=np-co-dat1-inbc.ho.ipt.local], [desc=To Test Tooling,dest=10.16.33.0/24,host=afcbm01,port=8080,protocol=tcp,rule name=<null>,rule number=<null>,source=10.43.0.3,zone=np-co-dat1-inbc.ho.ipt.local], [desc=To Test Tooling,dest=10.16.33.0/24,host=afcbm01,port=8080,protocol=tcp,rule name=<null>,rule number=<null>,source=10.43.0.12,zone=np-co-dat1-inbc.ho.ipt.local], [desc=To Test Tooling,dest=10.16.33.0/24,host=afcbm01,port=8080,protocol=tcp,rule name=<null>,rule number=<null>,source=10.43.0.18,zone=np-co-dat1-inbc.ho.ipt.local], [desc=To Test Tooling,dest=10.16.33.0/24,host=afcbm02,port=8080,protocol=tcp,rule name=<null>,rule number=<null>,source=10.43.0.3,zone=np-co-dat1-inbc.ho.ipt.local], [desc=To Test Tooling,dest=10.16.33.0/24,host=afcbm02,port=8080,protocol=tcp,rule name=<null>,rule number=<null>,source=10.43.0.12,zone=np-co-dat1-inbc.ho.ipt.local], [desc=To Test Tooling,dest=10.16.33.0/24,host=afcbm02,port=8080,protocol=tcp,rule name=<null>,rule number=<null>,source=10.43.0.18,zone=np-co-dat1-inbc.ho.ipt.local], [desc=app (II_PJT3_DEV1),dest=10.43.16.12,host=afwbm01,port=8080,protocol=tcp,rule name=<null>,rule number=1,source=10.166.33.0/24,zone=np-co-dat1-inbc.ho.ipt.local], [desc=app (II_PJT3_DEV1),dest=10.43.16.12,host=afwbm02,port=8080,protocol=tcp,rule name=<null>,rule number=1,source=10.166.33.0/24,zone=np-co-dat1-inbc.ho.ipt.local], [desc=app (II_PJT3_DEV1),dest=10.43.16.12,host=afwbm01,port=8080%8443,protocol=tcp,rule name=<null>,rule number=2,source=10.166.32.0/24,zone=np-co-dat1-inbc.ho.ipt.local], [desc=app (II_PJT3_DEV1),dest=10.43.16.12,host=afwbm02,port=8080%8443,protocol=tcp,rule name=<null>,rule number=2,source=10.166.32.0/24,zone=np-co-dat1-inbc.ho.ipt.local]]", withoutHash);
	}
	
	/**
	 * Note no common rules
	 */
	@Test
	public void testInput() {
		FirewallManager fwm = new FirewallManager(new File("src/test/resources/xl/IPTFirewallRulesPR.xlsx"));
		String rules = fwm.getInputFirewallRules("dbsem41", "pr-prd1-dazo.ipt.ho.local").toString();
		String withoutHash = rules.replaceAll("com\\.ipt\\.ebsa\\.environment\\.hiera\\.firewall\\.InputFirewallRule[^\\[]*", "");
		assertEquals("[[host=dbsem41,domain=pr-prd1-dazo.ipt.ho.local,source=etlga01,port=1532,protocol=<null>,description=CID SQLNet,version=0.10,comments=], [host=dbsem41,domain=pr-prd1-dazo.ipt.ho.local,source=etlga02,port=1532,protocol=<null>,description=CID SQLNet,version=0.10,comments=], [host=dbsem41,domain=pr-prd1-dazo.ipt.ho.local,source=dbsea01,port=1532,protocol=<null>,description=CID SQLNet,version=0.10,comments=], [host=dbsem41,domain=pr-prd1-dazo.ipt.ho.local,source=dbsea02,port=1532,protocol=<null>,description=CID SQLNet,version=0.10,comments=], [host=dbsem41,domain=pr-prd1-dazo.ipt.ho.local,source=192.168.32.0/24,port=1532,protocol=<null>,description=CID SQLNet,version=0.10,comments=], [host=dbsem41,domain=pr-prd1-dazo.ipt.ho.local,source=192.168.3.41,port=1532,protocol=<null>,description=CID SQLNet,version=0.10,comments=], [host=dbsem41,domain=pr-prd1-dazo.ipt.ho.local,source=192.168.3.44,port=1532,protocol=<null>,description=CID SQLNet,version=0.10,comments=], [host=dbsem41,domain=pr-prd1-dazo.ipt.ho.local,source=192.168.3.47,port=1532,protocol=<null>,description=CID SQLNet,version=0.10,comments=], [host=dbsem41,domain=pr-prd1-dazo.ipt.ho.local,source=<null>,port=7912,protocol=<null>,description=CID GoldenGate,version=0,comments=], [host=dbsem41,domain=pr-prd1-dazo.ipt.ho.local,source=<null>,port=7913,protocol=<null>,description=CID GoldenGate,version=0,comments=], [host=dbsem41,domain=pr-prd1-dazo.ipt.ho.local,source=<null>,port=7914,protocol=<null>,description=CID GoldenGate,version=0,comments=], [host=dbsem41,domain=pr-prd1-dazo.ipt.ho.local,source=<null>,port=7915,protocol=<null>,description=CID GoldenGate,version=0,comments=], [host=dbsem41,domain=pr-prd1-dazo.ipt.ho.local,source=<null>,port=7916,protocol=<null>,description=CID GoldenGate,version=0,comments=], [host=dbsem41,domain=pr-prd1-dazo.ipt.ho.local,source=<null>,port=7917,protocol=<null>,description=CID GoldenGate,version=0,comments=], [host=dbsem41,domain=pr-prd1-dazo.ipt.ho.local,source=<null>,port=7918,protocol=<null>,description=CID GoldenGate,version=0,comments=], [host=dbsem41,domain=pr-prd1-dazo.ipt.ho.local,source=<null>,port=7919,protocol=<null>,description=CID GoldenGate,version=0,comments=], [host=dbsem41,domain=pr-prd1-dazo.ipt.ho.local,source=<null>,port=7920,protocol=<null>,description=CID GoldenGate,version=0,comments=]]", withoutHash);
	}
	
	/**
	 * Note includes common rules
	 */
	@Test
	public void testInputVyatta() {
		FirewallManager fwm = new FirewallManager(new File("src/test/resources/xl/IPTFirewallRulesPR.xlsx"));
		String rules = fwm.getInputFirewallRules("afwem01", "pr-prd1-dazo.ipt.ho.local").toString();
		String withoutHash = rules.replaceAll("com\\.ipt\\.ebsa\\.environment\\.hiera\\.firewall\\.InputFirewallRule[^\\[]*", "");
		assertEquals("[[host=afwem01,domain=pr-prd1-dazo.ipt.ho.local,source=<null>,port=22,protocol=tcp,description=ssh,version=,comments=], [host=afwem01,domain=pr-prd1-dazo.ipt.ho.local,source=<null>,port=10050,protocol=tcp,description=zabbix agent,version=,comments=], [host=afwem01,domain=pr-prd1-dazo.ipt.ho.local,source=soaga01,port=1523,protocol=<null>,description=OER SQLNet,version=0.10,comments=], [host=afwem01,domain=pr-prd1-dazo.ipt.ho.local,source=soaga02,port=1523,protocol=<null>,description=OER SQLNet,version=0.10,comments=], [host=afwem01,domain=pr-prd1-dazo.ipt.ho.local,source=etlga01,port=1523,protocol=<null>,description=OER SQLNet,version=0.10,comments=], [host=afwem01,domain=pr-prd1-dazo.ipt.ho.local,source=etlga02,port=1523,protocol=<null>,description=OER SQLNet,version=0.10,comments=], [host=afwem01,domain=pr-prd1-dazo.ipt.ho.local,source=dbsea21,port=1523,protocol=<null>,description=OER SQLNet,version=0.10,comments=], [host=afwem01,domain=pr-prd1-dazo.ipt.ho.local,source=dbsea22,port=1523,protocol=<null>,description=OER SQLNet,version=0.10,comments=], [host=afwem01,domain=pr-prd1-dazo.ipt.ho.local,source=dbsea31,port=1523,protocol=<null>,description=OER SQLNet,version=0.10,comments=], [host=afwem01,domain=pr-prd1-dazo.ipt.ho.local,source=dbsea32,port=1523,protocol=<null>,description=OER SQLNet,version=0.10,comments=], [host=afwem01,domain=pr-prd1-dazo.ipt.ho.local,source=dbsea41,port=1523,protocol=<null>,description=OER SQLNet,version=0.10,comments=], [host=afwem01,domain=pr-prd1-dazo.ipt.ho.local,source=dbsea42,port=1523,protocol=<null>,description=OER SQLNet,version=0.10,comments=], [host=afwem01,domain=pr-prd1-dazo.ipt.ho.local,source=dbsea51,port=1523,protocol=<null>,description=OER SQLNet,version=0.10,comments=], [host=afwem01,domain=pr-prd1-dazo.ipt.ho.local,source=dbsea52,port=1523,protocol=<null>,description=OER SQLNet,version=0.10,comments=], [host=afwem01,domain=pr-prd1-dazo.ipt.ho.local,source=192.168.32.0/24,port=1523,protocol=<null>,description=OER SQLNet,version=0.10,comments=]]", withoutHash);
	}
}