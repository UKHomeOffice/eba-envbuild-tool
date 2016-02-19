package com.ipt.ebsa.manage.deploy.impl;

/**
 * These are the types of changes that can be executed within an zone
 * @author scowx
 *
 */
public enum ChangeType {
    NO_CHANGE_OUT_OF_SCOPE, NO_CHANGE, UPGRADE, DOWNGRADE, DEPLOY, UNDEPLOY, FIX, FAIL;
}
