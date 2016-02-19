package com.ipt.ebsa.agnostic.client.skyscape.module;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ipt.ebsa.agnostic.client.config.Config;
import com.ipt.ebsa.agnostic.client.skyscape.connection.SkyscapeConnector;
import com.vmware.vcloud.api.rest.schema.CatalogType;
import com.vmware.vcloud.api.rest.schema.IpScopeType;
import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.api.rest.schema.VAppNetworkConfigurationType;
import com.vmware.vcloud.api.rest.schema.VdcType;
import com.vmware.vcloud.sdk.Catalog;
import com.vmware.vcloud.sdk.Organization;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VM;
import com.vmware.vcloud.sdk.Vapp;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.Vdc;
import com.vmware.vcloud.sdk.VirtualDisk;
import com.vmware.vcloud.sdk.VirtualMedia;
import com.vmware.vcloud.sdk.VirtualMemory;
import com.vmware.vcloud.sdk.VirtualNetworkCard;

/**
 * Contains logic for navigating an environment and writing out what is in it
 * 
 *
 */
public class IntrospectionModule {

	private Logger logger = LogManager.getLogger(IntrospectionModule.class);

	@Inject
	private SkyscapeConnector connector;

	@Inject
	@Config
	private String organisation;

	private BufferedWriter writer;

	public void introspect() {

		File outputFile = new File("report.html");
		try {
			writer = new BufferedWriter(new FileWriter(outputFile));
			startDocument();

			VcloudClient vcloudClient = connector.connect();
			Map<String, ReferenceType> organizationsMap = vcloudClient.getOrgRefsByName();

			/* List the catalogs within the VDC's */
			listItemsForOrganisation(vcloudClient, organizationsMap);

			/* List the data stores */
			// VcloudAdminExtension extension =
			// vcloudClient.getVcloudAdminExtension();
			// listAllDatastores(vcloudClient, extension);
			// listAllResourcePools(vcloudClient, extension);
			// listAllVMs(vcloudClient, extension);
			endDocument();

		} catch (Exception e) {
			logger.error("Exception", e);
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					// this is a secondary error, just going to spit it out and
					// move on
					e.printStackTrace();
				}
			}
		}

	}

	
	public void listItemsForOrganisation(VcloudClient vcloudClient, Map<String, ReferenceType> organizationsMap) throws VCloudException {

		if (!organizationsMap.isEmpty()) {
			for (String organizationName : organizationsMap.keySet()) {
				ReferenceType organizationReference = organizationsMap.get(organizationName);

				startTable();
				writeRow(1, organizationName + " - " + organizationReference.getHref());

				startRowAndCell();
				listVApps(vcloudClient, organizationReference, organizationName);
				endCellAndRow();

				startRowAndCell();
				listCatalogsForOrganisation(vcloudClient, organizationReference, organizationName);
				endCellAndRow();

				startRowAndCell();
				listDetailForAllVapps(vcloudClient, organizationReference, organizationName);
				endCellAndRow();

				endTable();

			}
		} else {
			text("No Organizations");
		}
	}

	/**
	 * List the catalogs for a particular organisation
	 * 
	 * @param vcloudClient
	 * @param organizationsMap
	 * @param organizationName
	 * @throws VCloudException
	 */
	private void listCatalogsForOrganisation(VcloudClient vcloudClient, ReferenceType organizationReference, String organizationName) throws VCloudException {

		startTable("Catalog", "Catalog Item");
		Organization organization = Organization.getOrganizationByReference(vcloudClient, organizationReference);
		Collection<ReferenceType> catalogLinks = organization.getCatalogRefs();
		if (!catalogLinks.isEmpty()) {
			for (ReferenceType catalogLink : catalogLinks) {
				Catalog catalog = Catalog.getCatalogByReference(vcloudClient, catalogLink);
				CatalogType catalogParams = catalog.getResource();
				Collection<ReferenceType> catalogItemReferences = catalog.getCatalogItemReferences();

				startRow();
				writeCell(catalogParams.getName() + "<br />" + catalogLink.getHref());
				listReferencesInCell(null, catalogItemReferences);
				endRow();
			}
		} else {
			writeRow(1, "No Catalogs Found");
		}
		endTable();

	}

	/**
	 * List the vapps in this organisation
	 * 
	 * @param maxItemsPerRow
	 * @param vcloudClient
	 * @param organization
	 * @throws VCloudException
	 */
	private void listVApps(VcloudClient vcloudClient, ReferenceType organizationReference, String organizationName) throws VCloudException {

		startTable();
		Organization organization = Organization.getOrganizationByReference(vcloudClient, organizationReference);
		Collection<ReferenceType> vdcLinks = organization.getVdcRefs();
		if (!vdcLinks.isEmpty()) {
			for (ReferenceType vdcLink : vdcLinks) {
				Vdc vdc = Vdc.getVdcByReference(vcloudClient, vdcLink);
				VdcType vdcParams = vdc.getResource();

				startRow();
				startCell();
				text(vdcParams.getName() + " <br />(" + vdcLink.getHref() + ")");
				endCell();

				startCell();
				startTable();

				listReferenceItems(vdc.getAvailableNetworkRefs(), "Network Refs");
				listReferenceItems(vdc.getDiskRefs(), "Disk Refs");
				listReferenceItems(vdc.getMediaRefs(), "Media Refs");
				listReferenceItems(vdc.getVappRefs(), "VApps");
				listReferenceItems(vdc.getVappTemplateRefs(), "VApp Templates");
				listReferenceItems(vdc.getVdcStorageProfileRefs(), "Storage");

				endTable();
				endCell();
				endRow();

			}

		}
		endTable();
	}

	
	public void listDetailForAllVapps(VcloudClient vcloudClient, ReferenceType organizationReference, String organizationName) throws VCloudException {
		startTable();
		Organization organization = Organization.getOrganizationByReference(vcloudClient, organizationReference);
		Collection<ReferenceType> vdcLinks = organization.getVdcRefs();
		if (!vdcLinks.isEmpty()) {
			for (ReferenceType vdcLink : vdcLinks) {
				Vdc vdc = Vdc.getVdcByReference(vcloudClient, vdcLink);

				Collection<ReferenceType> vappReferences = vdc.getVappRefs();
				for (ReferenceType vappRef : vappReferences) {
					startDiv();
					listVAppDetails(vcloudClient, vdc, vappRef);
					endDiv();
				}

			}

		}
		endTable();
	}

	/**
	 * Lists the details of a partifcular VM
	 * 
	 * @param vcloudClient
	 * @param vdc
	 * @param vappRef
	 * @throws VCloudException
	 */
	private void listVAppDetails(VcloudClient vcloudClient, Vdc vdc, ReferenceType vappRef) throws VCloudException {

		Vapp vapp = Vapp.getVappByReference(vcloudClient, vappRef);

		// stuffThatsNull(vapp);
		startRowAndCell();
		boldText(vapp.getReference().getName());
		// vapp.getReference().getHref()
		endCell();
		startCell();
		startTable();

		writeRow(2, "VApp Size", vapp.getVappSize());
		writeRow(2, "Status", vapp.getVappStatus());

		writeRow(2, "Owner", vapp.getOwner().getId() + " - " + vapp.getOwner().getHref());
		writeRow(2, "Resource", vapp.getResource().getName() + " - " + vapp.getResource().getHref());

		// //Collection<NetworkSectionNetwork> networks = vapp.getNetworks();
		// List<ProductSectionType> productSections = vapp.getProductSections();

		/* Network names */
		Set<String> networkNames = vapp.getNetworkNames();
		startRow();
		writeCell("Networks");
		startCell();
		for (String n : networkNames) {
			startTable();
			writeRow(2, "Name", n);
			endTable();
		}
		endCellAndRow();

		/* Children VApps */
		List<Vapp> children = vapp.getChildrenVapps();
		startRow();
		writeCell("Children VApps");
		startCell();
		for (Vapp vappA : children) {
			startTable();
			writeRow(2, "ID", vappA.getReference().getName() + " - " + vappA.getReference().getHref());
			endTable();
		}
		endCellAndRow();

		/* Contained VMs */
		startRow();
		writeCell("VMs");
		startCell();
		List<VM> vms = vapp.getChildrenVms();
		for (VM vm : vms) {
			startTable();
			startRowAndCell();
			boldText(vm.getReference().getName());
			endCellAndStartAnother();
			startTable();
			writeRow(2, "Size", vm.getVmSize());
			writeRow(2, "Status", vm.getVMStatus());
			writeRow(2, "Number of CPUs", vm.getCpu().getNoOfCpus());
			writeRow(2, "CPU", vm.getCpu());

			/* Disks */
			startRowAndCell();
			text("Disks");
			endCellAndStartAnother();
			List<VirtualDisk> disks = vm.getDisks();
			for (VirtualDisk virtualDisk : disks) {
				startTable();
				try {
					writeRow(2, "Bus Type", virtualDisk.getHardDiskBusType());
				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					writeRow(2, "Size", virtualDisk.getHardDiskSize());
				} catch (Exception e) {
					e.printStackTrace();
				}
				// com.vmware.vcloud.sdk.VCloudException: Not a Hard Disk - SCSI
				// Controller 0
				endTable();
			}
			endCellAndRow();

			/* Media */
			startRowAndCell();
			text("Media");
			endCellAndStartAnother();
			List<VirtualMedia> medias = vm.getMedias();
			for (VirtualMedia media : medias) {
				startTable();
				writeRow(2, "ItemResource", media.getItemResource());
				writeRow(2, "Media", media);
				endTable();
			}
			endCellAndRow();

			/* Memory */
			VirtualMemory memory = vm.getMemory();
			writeRow(2, "Memory", memory.getMemorySize());

			/* Nics */
			startRowAndCell();
			text("Nics");
			endCellAndStartAnother();
			List<VirtualNetworkCard> nics = vm.getNetworkCards();
			for (VirtualNetworkCard nic : nics) {
				startTable();
				try {
					writeRow(2, "IPAddress", nic.getIpAddress());
				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					writeRow(2, "IPAddressMode", nic.getIpAddressingMode());
				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					writeRow(2, "Mac address", nic.getMacAddress());
				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					writeRow(2, "Network", nic.getNetwork());
				} catch (Exception e) {
					e.printStackTrace();
				}
				endTable();
			}
			endCellAndRow();

			endTable();
			endCellAndRow();
			endTable();
		}
		endCellAndRow();

		try {
			Collection<VAppNetworkConfigurationType> vappNetworkConfigurations = vapp.getVappNetworkConfigurations();
			startRow();
			writeCell("Network Configurations");
			startCell();
			// ########

			startTable();
			for (VAppNetworkConfigurationType type : vappNetworkConfigurations) {

				writeRow(2, "Network name", type.getNetworkName());
				writeRow(2, "Description", type.getDescription());
				writeRow(2, "HRef", type.getHref());
				writeRow(2, "Type", type.getType());
				writeRow(2, "FenceMode", type.getConfiguration().getFenceMode());
				startRowAndCell();
				text("IPs");
				endCell();
				startCell();
				IpScopeType ipScope = type.getConfiguration().getIpScope();
				try {
					if (ipScope != null) {
						List<String> t = ipScope.getAllocatedIpAddresses().getIpAddress();
						startTable();
						for (String string : t) {
							writeRow(2, "IP", string);
						}

						writeRow(2, "DNS1", ipScope.getDns1());
						writeRow(2, "DNS2", ipScope.getDns2());
						writeRow(2, "DNS Suffix", ipScope.getDnsSuffix());
						writeRow(2, "Gateway", ipScope.getGateway());
						writeRow(2, "Netmask", ipScope.getNetmask());
						writeRow(2, "VCloudExtension", ipScope.getVCloudExtension());
						writeRow(2, "Parent Network", type.getConfiguration().getParentNetwork().getId() + " - " + type.getConfiguration().getParentNetwork().getHref());

						endTable();
					}
				} catch (Exception e) {
					logger.error(e.getClass().getName() + " - " + e.getMessage());
				}
				endCellAndRow();
			}
			endTable();
			// ########
			endCellAndRow();
		} catch (com.vmware.vcloud.sdk.VCloudException e) {
			logger.error(e.getClass().getName() + " - " + e.getMessage());
		}

		endTable();
		endCellAndRow();

	}

	//
	// private void stuffThatsNull(Vapp vapp) throws VCloudException {
	// ControlAccessParamsType controlAccess = vapp.getControlAccess();
	// writeRow(2, "Everyone Access Level",controlAccess != controlAccess ?
	// controlAccess.getEveryoneAccessLevel() : "");
	// AccessSettingsType accessSettings = controlAccess.getAccessSettings();
	// if (accessSettings != null) {
	// accessSettings.getAccessSetting();
	// accessSettings.getOtherAttributes();
	// }
	//
	// try {
	// LeaseSettingsSectionType leaseSettingsSection =
	// vapp.getLeaseSettingsSection();
	// } catch (com.vmware.vcloud.sdk.VCloudException e) {
	// logger.error(e.getMessage());
	// }
	//
	// try {
	// NetworkConfigSectionType networkConfigSection =
	// vapp.getNetworkConfigSection();
	// } catch (com.vmware.vcloud.sdk.VCloudException e) {
	// logger.error(e.getMessage());
	// }
	//
	// try {
	// NetworkSectionType networkSection = vapp.getNetworkSection();
	// } catch (com.vmware.vcloud.sdk.VCloudException e) {
	// logger.error(e.getMessage());
	// }
	//
	// try {
	// EnvelopeType ovf = vapp.getOvf();
	// } catch (com.vmware.vcloud.sdk.VCloudException e) {
	// logger.error(e.getMessage());
	// }
	//
	// try {
	// SnapshotSectionType snapshotSection = vapp.getSnapshotSection();
	// } catch (com.vmware.vcloud.sdk.VCloudException e) {
	// logger.error(e.getMessage());
	// }
	//
	// try {
	// StartupSectionType startUpSection = vapp.getStartUpSection();
	// } catch (com.vmware.vcloud.sdk.VCloudException e) {
	// logger.error(e.getMessage());
	// }
	//
	// List<Task> tasks = vapp.getTasks();
	// try {
	// Set<String> vappNetworkConfigurationNames =
	// vapp.getVappNetworkConfigurationNames();
	// } catch (com.vmware.vcloud.sdk.VCloudException e) {
	// logger.error(e.getMessage());
	// }
	// }

	private void listReferenceItems(Collection<ReferenceType> storageProfiles, String s) {
		startRowAndCell();
		listReferencesInCell(s, storageProfiles);
		endCellAndRow();
	}

	/**
	 * List the references in tabular form with a table title
	 * 
	 * @param title
	 * @param referencesByname
	 */
	private void listReferencesInCell(String title, Collection<ReferenceType> references) {
		startCell();
		if (title != null) {
			boldText(title);
		}
		startTable();
		if (!references.isEmpty()) {
			for (ReferenceType ref : references) {
				writeRow(2, ref.getName(), ref.getHref());
			}
		} else {
			writeRow(2, "None Found");
		}
		endTable();
	}

	/**
	 * HTML BELOW THIS LINE
	 * 
	 */

	private void startDocument() throws IOException {
		write("<html><body>");
	}

	private void endDocument() throws IOException {
		write("</body></html>");
	}

	private void startTable(String... header) {
		write("<table border=\"1\">");
		if (header != null && header.length > 0) {
			write("<tr>");
			for (int i = 0; i < header.length; i++) {
				write("<td>" + header[i] + "</td>");
			}
			write("</tr>");
		}
	}

	private void endTable() {
		write("</table>");
	}

	private void startDiv() {
		write("<div>");
	}

	private void endDiv() {
		write("</div>");
	}

	private void startRow() {
		write("<tr>");
	}

	private void endRow() {
		write("</tr>");
	}

	private void startRowAndCell() {
		write("<tr><td>");
	}

	private void endCellAndRow() {
		write("</td></tr>");
	}

	private void startCell() {
		write("<td>");
	}

	private void endCell() {
		write("</td>");
	}

	private void endCellAndStartAnother() {
		write("</td><td>");
	}

	protected void heading(int level, String text) {
		write("<h" + level + ">" + text + "</h" + level + ">");
	}

	private void text(String text) {
		write("<span>" + text + "</span>");
	}

	private void boldText(String text) {
		write("<span style=\"font-weight:bold;\">" + text + "</span>");
	}

	private void writeCell(String text) {
		startCell();
		text(text);
		endCell();
	}

	/**
	 * Write a row to HTMl
	 * 
	 * @param writer
	 * @param values
	 * @param max
	 * @throws IOException
	 */
	private void writeRow(int max, Object... values) {
		write("<tr>");
		for (int i = 0; i < max; i++) {
			write("<td>");
			if (i < values.length) {
				write(values[i]);
			}
			write("</td>");
		}
		write("</tr>");
	}

	/**
	 * Generic write method which handles IOExceptions and throws an unchecked
	 * RuntimeException
	 * 
	 * @param text
	 */
	private void write(Object text) {
		try {
			if (text != null) {
				writer.write(text.toString());
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not write to stream");
		}
	}

}