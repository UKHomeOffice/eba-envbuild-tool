package com.ipt.ebsa.agnostic.client.aws.extensions;

public enum VpcPeeringConnectionStatus {
	INITIALISING("initiating-request", 1), PENDING_ACCEPTANCE("pending-acceptance", 2), PROVISIONING("provisioning", 3), ACTIVE("active", 4), FAILED(
			"failed", 5), EXPIRED("expired", 6), REJECTED("rejected", 7), DELETED("deleted", 8), REMOTE_SUBNET_UNAVAILABLE(
			VpcPeeringConnectionStatus.REMOTE_SUBNET_UNAVAILABLE_STRING, 9), LOCAL_SUBNET_UNAVAILABLE(
			VpcPeeringConnectionStatus.LOCAL_SUBNET_UNAVAILABLE_STRING, 10);

	private String value;
	private int code;
	public static final String REMOTE_SUBNET_UNAVAILABLE_STRING = "remote_subnet_unavailable";
	public static final String LOCAL_SUBNET_UNAVAILABLE_STRING = "local_subnet_unavailable";

	private VpcPeeringConnectionStatus(String value, int code) {
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
	public static VpcPeeringConnectionStatus fromValue(String value) {
		if (value == null || "".equals(value)) {
			throw new IllegalArgumentException("Value cannot be null or empty!");

		} else if ("initiating-request".equals(value)) {
			return VpcPeeringConnectionStatus.INITIALISING;
		} else if ("pending-acceptance".equals(value)) {
			return VpcPeeringConnectionStatus.PENDING_ACCEPTANCE;
		} else if ("failed".equals(value)) {
			return VpcPeeringConnectionStatus.FAILED;
		} else if ("expired".equals(value)) {
			return VpcPeeringConnectionStatus.EXPIRED;
		} else if ("provisioning".equals(value)) {
			return VpcPeeringConnectionStatus.PROVISIONING;
		} else if ("active".equals(value)) {
			return VpcPeeringConnectionStatus.ACTIVE;
		} else if ("deleted".equals(value)) {
			return VpcPeeringConnectionStatus.DELETED;
		} else if ("rejected".equals(value)) {
			return VpcPeeringConnectionStatus.REJECTED;
		} else if (VpcPeeringConnectionStatus.REMOTE_SUBNET_UNAVAILABLE_STRING.equals(value)) {
			return VpcPeeringConnectionStatus.REMOTE_SUBNET_UNAVAILABLE;
		} else if (VpcPeeringConnectionStatus.LOCAL_SUBNET_UNAVAILABLE_STRING.equals(value)) {
			return VpcPeeringConnectionStatus.LOCAL_SUBNET_UNAVAILABLE;
		} else {
			throw new IllegalArgumentException("Cannot create enum from " + value + " value!");
		}
	}
}
