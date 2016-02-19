package com.ipt.ebsa.agnostic.client.aws.module.handler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.amazonaws.util.Base64;
import com.ipt.ebsa.agnostic.client.aws.extensions.IptNetworkInterface;
import com.ipt.ebsa.agnostic.client.skyscape.exception.UnresolvedDependencyException;
import com.ipt.ebsa.agnostic.client.util.CustomisationHelper;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLMetaDataType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineContainerType;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLVirtualMachineType;
import com.jcabi.aspects.Loggable;

/**
 * 
 *
 */
@Loggable(prepend = true)
public class UserDataHandler {

	private Logger logger = LogManager.getLogger(UserDataHandler.class);

	public static final String HOSTNAME = "<HOSTNAME>";
	public static final String DOMAIN = "<DOMAIN>";
	public static final String PRIMARY_NIC = "<PRIMARY_NIC>";
	public static final String PRIMARY_IP = "<PRIMARY_IP>";
	public static final String NAMESERVER = "<NAMESERVER>";
	public static final String TOOLING_DOMAIN = "<TOOLING_DOMAIN>";
	public static final String USER_DATA_PRIMARY_NIC = "<USER_DATA_PRIMARY_NIC>";
	public static final String USER_DATA_NIC_INFO = "<USER_DATA_NIC_INFO>";
	public static final String EXTERNAL_IP_ADDRESS = "<EXTERNAL_IP_ADDRESS>";

	private String guestCustScriptDir;
	private String toolingDomain;
	private String externalIpAddress;

	public UserDataHandler() {

	}

	public UserDataHandler(String guestCustScriptDir, String toolingDomain, String externalIpAddress) {
		this.guestCustScriptDir = guestCustScriptDir;
		this.toolingDomain = toolingDomain;
		this.externalIpAddress = externalIpAddress;
	}

	public String getGuestCustScriptDir() {
		return guestCustScriptDir;
	}

	public void setGuestCustScriptDir(String guestCustScriptDir) {
		this.guestCustScriptDir = guestCustScriptDir;
	}

	public String getToolingDomain() {
		return toolingDomain;
	}

	public void setToolingDomain(String toolingDomain) {
		this.toolingDomain = toolingDomain;
	}

	public String getExternalIpAddress() {
		return externalIpAddress;
	}

	public void setExternalIpAddress(String externalIpAddress) {
		this.externalIpAddress = externalIpAddress;
	}

	private String cleanser(String input) {
		if (StringUtils.isBlank(input)) {
			return StringUtils.EMPTY;
		} else {
			return input;
		}
	}

	public String getUserDataScript(boolean encode, XMLVirtualMachineContainerType vmc, XMLVirtualMachineType virtualMachine,
			Collection<IptNetworkInterface> nics) throws UnresolvedDependencyException, IOException {
		String userData = StringUtils.EMPTY;
		IptNetworkInterface primaryNic = new IptNetworkInterface();
		String primaryNicEth = "";

		StringBuffer nicInfo = new StringBuffer();
		for (IptNetworkInterface nic : nics) {
			if (nic.isPrimary()) {
				primaryNic = nic;
				primaryNicEth = "eth" + nic.getDeviceIndex();
			}
			nicInfo.append("eth");
			nicInfo.append(nic.getDeviceIndex());
			nicInfo.append("_ip");
			nicInfo.append("=");
			nicInfo.append(nic.getPrivateIpAddress());
			nicInfo.append("\n");
			nicInfo.append("eth");
			nicInfo.append(nic.getDeviceIndex());
			nicInfo.append("_mac");
			nicInfo.append("=");
			nicInfo.append(nic.getMacAddress());
			nicInfo.append("\n");
		}

		if (StringUtils.isBlank(virtualMachine.getCustomisationScript())) {
			logger.debug("No customisation script set");
		} else {
			userData = CustomisationHelper.readCustomisationScript(virtualMachine.getCustomisationScript(), guestCustScriptDir);
			userData = userData.replace(EXTERNAL_IP_ADDRESS, cleanser(externalIpAddress));
			userData = userData.replace(HOSTNAME, cleanser(virtualMachine.getComputerName()));
			userData = userData.replace(DOMAIN, cleanser(vmc.getDomain()));
			userData = userData.replace(PRIMARY_NIC, cleanser(primaryNicEth));
			userData = userData.replace(PRIMARY_IP, cleanser(primaryNic.getPrivateIpAddress()));
			userData = userData.replace(NAMESERVER, cleanser(vmc.getNetwork().get(0).getPrimaryDns()));
			userData = userData.replace(TOOLING_DOMAIN, cleanser(toolingDomain));
			userData = userData.replace(USER_DATA_PRIMARY_NIC, cleanser("eth" + primaryNic.getDeviceIndex()));
			userData = userData.replace(USER_DATA_NIC_INFO, cleanser(nicInfo.toString()));
		}

		logger.debug("Unencoded userdata [" + userData + "]");

		if (encode) {
			return encodeBase64(userData);
		} else {
			return userData;
		}
	}

	public String encodeBase64(String userData) {
		String userDataEncodedBase64 = new String(Base64.encodeAsString(userData.getBytes()));
		return userDataEncodedBase64;
	}

	public String getUserDataKeyValuePairs(XMLVirtualMachineContainerType vmc, XMLVirtualMachineType virtualMachine,
			Collection<IptNetworkInterface> nics) {
		HashMap<String, String> kvp = new HashMap<String, String>();

		for (IptNetworkInterface nic : nics) {
			if (nic.isPrimary()) {
				kvp.put("primary_nic_index", String.valueOf(nic.getDeviceIndex()));
			}
			kvp.put("eth" + nic.getDeviceIndex() + "_mac", String.valueOf(nic.getMacAddress()));
		}

		kvp.put("domain", vmc.getDomain());
		kvp.put("hostname", virtualMachine.getComputerName());

		for (XMLMetaDataType metaData : virtualMachine.getMetaData()) {
			if (kvp.containsKey(metaData.getName())) {
				throw new UnsupportedOperationException();
			}
			kvp.put(metaData.getName(), metaData.getValue());
		}

		StringBuffer userData = new StringBuffer();
		for (Entry<String, String> pair : kvp.entrySet()) {
			userData.append(pair.getKey());
			userData.append("=");
			userData.append(pair.getValue());
			userData.append("\n");
		}
		return userData.toString();
	}

	public String getUserDataCloudConfig(XMLVirtualMachineContainerType vmc, XMLVirtualMachineType virtualMachine,
			Collection<IptNetworkInterface> nics) {
		StringBuffer userData = new StringBuffer();
		userData.append("#cloud-config");
		userData.append("\n");
		userData.append("hostname: " + virtualMachine.getComputerName());
		userData.append("\n");
		userData.append("fqdn: " + virtualMachine.getComputerName() + "." + vmc.getDomain());
		// userData.append("\n");
		// userData.append("ssh_keys:\n");
		// userData.append("  rsa_private: |\n");
		// userData.append("    -----BEGIN RSA PRIVATE KEY-----\n");
		// userData.append("    MIIEpAIBAAKCAQEAjBhXvUYqyqVtktvFJoiEbMtOumFbboHBcmNsOVAD1EXLfhCFuQ76G8qoEYEd\n");
		// userData.append("    yd7HlqDkVC2NBs4iDHUomeIhJqNzSO0PSpHbraS1Wa6fJuKeuxJBwRtjMuFGDbWIlPaWo9JYZkT4\n");
		// userData.append("    0uCw3MNwKm3kkfR/72BLgQ7vYyDu+2JA1QQe4wzajFOTtBSSGm/P4LSEnxA9B1exlTqwFmt/EtTW\n");
		// userData.append("    t8pgOTKr4lUiuJy7nbwDkhrAr9HpWQA8vZeHajXNvNZ7DhSZ1/9tyYk/Iu6z5W2alK6yF4kZjGyc\n");
		// userData.append("    wWLMZmAM2dIGHOnlSRzD2s5xYSY8mDOSklYxSt92GzSumz5DQnIl7QIDAQABAoIBAHKmkVeDDw4c\n");
		// userData.append("    6FmhpeztGIL4t4tslakQY8I7f5w/0YJtRCX9W5KhoSoRZg49PqWwvmKVdYc2aA3DsLXFQ9774ul+\n");
		// userData.append("    JqDt8O+0ArZO72NftzvZhtHOFSX2Re6j0fLyN77BN3MGd/XqSEA8RkHFaAwxNzgDgpfilKggEuVr\n");
		// userData.append("    VGlCdZBdwNtCcSIKNvad2tNrM47FjI33BclIV17kddqWKxZGglXpoLuIFj4NlIz7tBfJSG18syaA\n");
		// userData.append("    7alFy0WCNolVS0pGi5HQPxj9d5eNUlxXU81tryV591VicLxR1D8Q4cDOZt6SoF5YOdRK99jlpYi3\n");
		// userData.append("    CjpXNpykF+B+Sg8P9pkVmY6mjoECgYEA1NRT5LY2om3Hdnw0b0YGhLIYeqDp6Q8PVgPyyh4q7tpW\n");
		// userData.append("    DV75DJaG9TRDx30uTY/RBVPn7byGGu9sUsksJ77cOnk2k0Uco7C2L/Barab5QX552qsH9jOcLd0e\n");
		// userData.append("    khzINqqY22tgB8GePmB2hh/EBLno/wheuyGaB7hILavhzhuU+DECgYEAqIMc6C7bhIeyvT0KyFou\n");
		// userData.append("    LfWtCubkJyFkHpNJoGt1VPxHePvhimr+XTKN984Mgww4X7W+QrNhh2ig5AXzggFKdcVpFxK+qfWr\n");
		// userData.append("    yWqk+kxO9l+osxz+0CEUtGv6rhMaTk8ZtSWPXPVz8Y0HGVLU00EnfVNeYGIxvox/U1jTkmP81n0C\n");
		// userData.append("    gYEAnO/s5OvQIbodqSsxZxQV3meNUW+yzF6EioLi11OjiPmLFtT/hN7CqbFegY23RQ5VrT4P+ehY\n");
		// userData.append("    YyhFkk4xtyUwNBYJvlLqKXI3BeaQlxhXGIpwxGP1vQCVm+Q/lLKQmG73xGTLLS0CyTBMdd6apnmI\n");
		// userData.append("    a/b6E7dVY3fqEXN7UIZaNSECgYBVXZZXSOWLzfKOS04bERQz1DFFRJJwAEpn8tVpnwZhdyv97hvj\n");
		// userData.append("    szAIpoOk9Y0BFj+4vKkVwSm3HxniaeFiCqVd5BPIzIYqM3pDHvNUmTOnO+6KTjYH2bpTbCAh7yt0\n");
		// userData.append("    YD31toFZ5j5GL9tYyM1n96m5o3I8AYKRtTsLTgRgNKbxQQKBgQDIL6riv5ibNqezRXzLg/aBfKxR\n");
		// userData.append("    /WdVW656Wpi1Oo9nuVKBPmQ2wj3TroV6zFzFE4Zw11JG9IUnoBAtGgo+MIFrPlZV690zZ/etE9UN\n");
		// userData.append("    q+I6YJuMknbCIBOrEjzoyuPw5VPVs/WhQpqv1JHXwJMDAWhe3pNsM5VelqLa8D7/BlYc0A==\n");
		// userData.append("    -----END RSA PRIVATE KEY-----\n");

		return userData.toString();
	}

	public String getUserDataMultiPartMime(boolean encode, XMLVirtualMachineContainerType vmc, XMLVirtualMachineType virtualMachine,
			Collection<IptNetworkInterface> nics) throws UnresolvedDependencyException, IOException {
		MimeMessage userDataMime = new MimeMessage(Session.getDefaultInstance(new Properties()));
		MimeMultipart userDataMimeMulti = new MimeMultipart();
		try {
			userDataMime.setContent(userDataMimeMulti);
		} catch (MessagingException e) {
			logger.error("Failed making multipart mime file for setting userdata", e);
			throw new RuntimeException(e);
		}

		MimeBodyPart mimeBodyPartCloudInit = new MimeBodyPart();
		try {
			mimeBodyPartCloudInit.setText(getUserDataCloudConfig(vmc, virtualMachine, nics), Charsets.UTF_8.name(), "cloud-config");
			mimeBodyPartCloudInit.setFileName("cloud-init-config.txt");
			userDataMimeMulti.addBodyPart(mimeBodyPartCloudInit);
		} catch (MessagingException e) {
			logger.error("Error constructing the multipart mime with a shell script" + e);
		}

		MimeBodyPart mimeBodyPartShellscript = new MimeBodyPart();
		try {
			mimeBodyPartShellscript.setText(getUserDataScript(false, vmc, virtualMachine, nics), Charsets.UTF_8.name(), "x-shellscript");
			mimeBodyPartShellscript.setFileName("cloud-init-customise-script.txt");
			userDataMimeMulti.addBodyPart(mimeBodyPartShellscript);
		} catch (MessagingException e) {
			logger.error("Error constructing the multipart mime with a shell script" + e);
		}

		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			userDataMime.writeTo(outputStream);
			String mime = new String(outputStream.toByteArray(), Charsets.UTF_8.name());
			if (encode) {
				String encoded = encodeBase64(mime);
				return encoded;
			} else {
				return mime;
			}

		} catch (UnsupportedEncodingException e) {
			String error = "Bad encoding of userdata";
			logger.error(error, e);
			throw new RuntimeException(error, e);
		} catch (IOException e) {
			String error = "IO exception writing userdata";
			logger.error(error, e);
			throw new RuntimeException(error, e);
		} catch (MessagingException e) {
			String error = "Messaging exception when writing userdata";
			logger.error(error, e);
			throw new RuntimeException(error, e);
		}
	}

	private String readFile(String base, String file) throws IOException {
		if (StringUtils.isNotBlank(base)) {
			return FileUtils.readFileToString(new File(new File(base), file));
		} else {
			return FileUtils.readFileToString(new File(file));
		}
	}

}
