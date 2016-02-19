package com.ipt.ebsa.manage.deploy.data;

import com.ipt.ebsa.deployment.descriptor.XMLChainBehaviourType;
import com.ipt.ebsa.deployment.descriptor.XMLChangeMethodType;

/**
 * DDDetails stands for Deployment Descriptor Details
 * @author scowx
 *
 */
public class DDD {
	XMLChangeMethodType upgrade;
	String upgradeInt = "";
	XMLChangeMethodType downgrade;
	String downgradeInt = "";
	XMLChainBehaviourType deployBehaviour;
	String deployBehaviourInt = "";
	XMLChainBehaviourType undeployBehaviour;
	String undeployBehaviourInt = "";
	public DDD(XMLChangeMethodType upgrade, XMLChangeMethodType downgrade, XMLChainBehaviourType undeployBehaviour) {
		super();
		this.upgrade = upgrade;
		this.downgrade = downgrade;
		this.undeployBehaviour = undeployBehaviour;
	}
	public DDD(XMLChangeMethodType upgrade, XMLChangeMethodType downgrade, XMLChainBehaviourType deployBehaviour, XMLChainBehaviourType undeployBehaviour) {
		super();
		this.upgrade = upgrade;
		this.downgrade = downgrade;
		this.undeployBehaviour = undeployBehaviour;
	}
	public DDD(XMLChangeMethodType upgrade,String upgradeInt, XMLChangeMethodType downgrade,String downgradeInt, XMLChainBehaviourType deployBehaviour,String deployBehaviourInt, XMLChainBehaviourType undeployBehaviour,String undeployBehaviourInt) {
		super();
		this.upgrade = upgrade;
		this.downgrade = downgrade;
		this.undeployBehaviour = undeployBehaviour;
		this.deployBehaviour = deployBehaviour;
		this.upgradeInt = upgradeInt;
		this.downgradeInt = downgradeInt;
		this.undeployBehaviourInt = undeployBehaviourInt;
		this.deployBehaviourInt = deployBehaviourInt;
	}
}
