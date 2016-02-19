package com.ipt.ebsa.environment.build.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * The persistent class for the hardwareProfile database table.
 *
 */
@Entity
@NamedQueries({
	@NamedQuery(name="HardwareProfile.find", query="SELECT h FROM HardwareProfile h where provider = :provider and cpuCount = :cpuCount and memory >= :memory and interfaceCount >= :interfaceCount and vmRole = :vmRole and enabled = true order by h.interfaceCount, h.memory")
})
public class HardwareProfile implements DBEntity {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Integer id;

	private String provider;

	private int cpuCount;

	private int memory;

	private int interfaceCount;

	private String vmRole;
	
	private String profile;

	private boolean enabled = true;
	
	public HardwareProfile() {
	}

	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public int getCpuCount() {
		return cpuCount;
	}

	public void setCpuCount(int cpuCount) {
		this.cpuCount = cpuCount;
	}

	public int getMemory() {
		return memory;
	}

	public void setMemory(int memory) {
		this.memory = memory;
	}

	public int getInterfaceCount() {
		return interfaceCount;
	}

	public void setInterfaceCount(int interfaceCount) {
		this.interfaceCount = interfaceCount;
	}

	public String getVmRole() {
		return vmRole;
	}

	public void setVmRole(String vmRole) {
		this.vmRole = vmRole;
	}

	public String getProfile() {
		return profile;
	}

	public void setProfile(String profile) {
		this.profile = profile;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String toString() {
		return new ToStringBuilder(this)
			.append("id", getId())
			.append("provider", getProvider())
			.append("cpuCount", getCpuCount())
			.append("memory", getMemory())
			.append("interfaceCount", getInterfaceCount())
			.append("vmRole", getVmRole())
			.append("profile", getProfile())
			.append("enabled", isEnabled())
			.toString();
	}

}
