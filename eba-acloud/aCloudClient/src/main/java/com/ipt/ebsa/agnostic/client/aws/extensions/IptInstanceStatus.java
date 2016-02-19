package com.ipt.ebsa.agnostic.client.aws.extensions;

import com.amazonaws.services.ec2.model.InstanceStateName;

public enum IptInstanceStatus {

	Pending("pending", 0), Running("running", 1), Stopping("stopping", 2), Stopped("stopped", 3), ShuttingDown("shutting-down", 4), Terminated(
			"terminated", 5);

	private String value;
	private int code;

	private IptInstanceStatus(String value, int code) {
		this.value = value;
		this.code = code;
	}

	@Override
	public String toString() {
		return this.value;
	}

	public String getValue() {
		return value;
	}

	public int getCode() {
		return code;
	}

	/**
	 * Use this in place of valueOf.
	 *
	 * @param value
	 *            real value
	 * @return InstanceState corresponding to the value
	 */
	public static IptInstanceStatus fromValue(String value) {
		if (value == null || "".equals(value)) {
			throw new IllegalArgumentException("Value cannot be null or empty!");

		} else if ("pending".equals(value)) {
			return IptInstanceStatus.Pending;
		} else if ("running".equals(value)) {
			return IptInstanceStatus.Running;
		} else if ("shutting-down".equals(value)) {
			return IptInstanceStatus.ShuttingDown;
		} else if ("terminated".equals(value)) {
			return IptInstanceStatus.Terminated;
		} else if ("stopping".equals(value)) {
			return IptInstanceStatus.Stopping;
		} else if ("stopped".equals(value)) {
			return IptInstanceStatus.Stopped;
		} else {
			throw new IllegalArgumentException("Cannot create enum from " + value + " value!");
		}
	}

	public static IptInstanceStatus fromValue(InstanceStateName desiredState) {
		if (desiredState == null) {
			throw new IllegalArgumentException("Value cannot be null or empty!");
		}
		switch (desiredState) {
		case Pending:
			return IptInstanceStatus.Pending;
		case Running:
			return IptInstanceStatus.Running;
		case ShuttingDown:
			return IptInstanceStatus.ShuttingDown;
		case Stopped:
			return IptInstanceStatus.Stopped;
		case Stopping:
			return IptInstanceStatus.Stopping;
		case Terminated:
			return IptInstanceStatus.Terminated;
		default:
			throw new IllegalArgumentException("Unknown value " + desiredState.toString() + " no case defined for InstanceStateName conversion");

		}
	}

}
