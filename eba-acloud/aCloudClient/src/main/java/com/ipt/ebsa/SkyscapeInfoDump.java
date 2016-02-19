package com.ipt.ebsa;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ipt.ebsa.agnostic.client.config.Config;
import com.ipt.ebsa.agnostic.client.skyscape.connection.SkyscapeCloudValues;
import com.vmware.vcloud.api.rest.schema.NetworkConnectionType;
import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.api.rest.schema.ovf.OperatingSystemSectionType;
import com.vmware.vcloud.sdk.Organization;
import com.vmware.vcloud.sdk.VM;
import com.vmware.vcloud.sdk.Vapp;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.Vdc;
import com.vmware.vcloud.sdk.VirtualDisk;

/**
 * This is the class that dumps out info on an organisation.
 * 
 *
 */
public class SkyscapeInfoDump  {

	private Logger logger = LogManager.getLogger(SkyscapeInfoDump.class);

	@Inject
	private SkyscapeCloudValues cloudValues;

	@Inject
	@Config
	private String infodumpfullfilepath;
	
	private static final String DEFAULT_FILE_NAME="/tmp/infoDump";
	private static final String FILE_NAME_TXT=DEFAULT_FILE_NAME+".txt";
	private static final String FILE_NAME_CSV=DEFAULT_FILE_NAME+".csv";
	
	public enum DumpType {
		CSV, TEXT;
	};

	private String theFileName;
	private PrintWriter pw;
	
	/**
	 * @param dumpType
	 * @throws Exception
	 */
	public void dumpOutput(DumpType dumpType)
			throws Exception {

		VcloudClient vcloudClient = cloudValues.getClient();
		String organisation = cloudValues.getOrganisationAsStr();
		
		if (infodumpfullfilepath != null) {
			logger.info("Getting the full output file from a config parameter passed in...");
			this.theFileName = infodumpfullfilepath;
		} else {
			logger.info("No infoDumpOutputFullFilePath config parameter passed in. So will use the default value...");
			if (dumpType.equals(DumpType.CSV)) {
				this.theFileName=FILE_NAME_CSV;
			} else {
				this.theFileName=FILE_NAME_TXT;
			}			
		}
		// create the file Stream
		File file = new File(theFileName);
		FileOutputStream foStream = new FileOutputStream(file,false);
		pw = new PrintWriter(foStream);
		
		
		if (dumpType.equals(DumpType.CSV)) {
			dumpCsvOutput(vcloudClient, organisation);
		} else {
			dumpTextOutput(vcloudClient, organisation);
		}
		
		if (pw!=null) {
			pw.flush();
			pw.close();
		}
	}

	/**
	 * @param vcloudClient
	 * @param organisation
	 * @throws Exception
	 */
	private void dumpCsvOutput(VcloudClient vcloudClient, String organisation)
			throws Exception {

		CSVPrinter printer = new CSVPrinter(pw, CSVFormat.DEFAULT);

		HashMap<String, ReferenceType> orgsList = vcloudClient
				.getOrgRefsByName();

		for (ReferenceType orgRef : orgsList.values()) {
			logger.info("Searching for organisation " + organisation);
			logger.info("This orgRef name is" + orgRef.getName());
			if (organisation.equals(orgRef.getName())) {
				logger.info("FOUND IT"+ ". DETAILS OF THIS ORGANISATION ARE AS FOLLOWS.....Writing the file to "+theFileName);

				printer.printRecord("VDC", "VAPP", "VM", "Status", "OperatingSystemDetails","NetworkCardDetails","NumOfCPUs",
						"Memory", "Disk1-Size", "Disk2-Size", "Disk3-Size", "Disk4-Size");

				for (ReferenceType vdcRef : Organization
						.getOrganizationByReference(vcloudClient, orgRef)
						.getVdcRefs()) {

					Vdc vdc = Vdc.getVdcByReference(vcloudClient, vdcRef);

					for (ReferenceType vAppRef : Vdc.getVdcByReference(
							vcloudClient, vdcRef).getVappRefs()) {

						String vdcName = vdcRef.getName();
						String vAppName = vAppRef.getName();

						Vapp vapp = Vapp.getVappByReference(vcloudClient,
								vAppRef);
						List<VM> vms = vapp.getChildrenVms();
						for (VM vm : vms) {

							List<String> rowData = new ArrayList<String>();

							rowData.add(vdcName);
							rowData.add(vAppName);

							rowData.add(vm.getResource().getName());
							rowData.add(vm.getVMStatus().toString());
							
							rowData.add(getOperatingSystemDetails(vm));
							
							rowData.add(getNICDetails(vm));

							rowData.add("" + vm.getCpu().getNoOfCpus());
							rowData.add(vm.getMemory().getMemorySize() + " Mb");

							int diskNum=1;
							for (VirtualDisk disk : vm.getDisks()) {
								if (disk.isHardDisk()) {
									rowData.add(disk.getHardDiskSize() + " Mb");
									diskNum++;
								}									
							}
							
							while (diskNum < 4) {
								rowData.add("none");
								diskNum++;
							}
								
							printer.printRecord(rowData);
						}

					}

				}

			}
		}
		// close the printer
		printer.close();
		
		

	}

	/**
	 * @param vcloudClient
	 * @param organisation
	 * @throws Exception
	 */
	private void dumpTextOutput(VcloudClient vcloudClient, String organisation)
			throws Exception {

		pw.println("********");
		
		HashMap<String, ReferenceType> orgsList = vcloudClient
				.getOrgRefsByName();

		for (ReferenceType orgRef : orgsList.values()) {
			logger.info("Searching for organisation " + organisation);
			logger.info("This orgRef name is" + orgRef.getName());
			
			if (organisation.equals(orgRef.getName())) {
				logger.info("FOUND IT. DETAILS OF THIS ORGANISATION ARE AS FOLLOWS.....Writing the file to "+theFileName);
				pw.println("All Details for Organisation "+organisation);
				for (ReferenceType vdcRef : Organization
						.getOrganizationByReference(vcloudClient, orgRef)
						.getVdcRefs()) {
					pw.println("Showing Details for vDC Ref Type....");
					pw.println("The vdc ref is..." + vdcRef.getName());
					Vdc vdc = Vdc.getVdcByReference(vcloudClient, vdcRef);

					pw.println("Vdc : " + vdcRef.getName() + " : "
							+ vdc.getResource().getAllocationModel());
					for (ReferenceType vAppRef : Vdc.getVdcByReference(
							vcloudClient, vdcRef).getVappRefs()) {

						pw.println("	Vapp : " + vAppRef.getName());

						Vapp vapp = Vapp.getVappByReference(vcloudClient,
								vAppRef);
						List<VM> vms = vapp.getChildrenVms();
						for (VM vm : vms) {

							List<String> rowData = new ArrayList<String>();

							pw.println("		Vm : "
									+ vm.getResource().getName());
							pw.println("			Status : "
									+ vm.getVMStatus());
							
							pw.println("			OperatingSystemDetails : "
									+ getOperatingSystemDetails(vm));
							
							pw.println("			NICDetails : "
									+ getNICDetails(vm));
							
							
							
							pw.println("			CPU : "
									+ vm.getCpu().getNoOfCpus());
							pw.println("			Memory : "
									+ vm.getMemory().getMemorySize() + " Mb");

							int i=0;
							for (VirtualDisk disk : vm.getDisks()) {
								if (disk.isHardDisk()) {
									i++;
									pw.println("			Disk"+i+" : "
											+ disk.getHardDiskSize() + " Mb");
								}
							}
								
							pw.flush();
						}

					}

				}

			}
		}

	}
	
	/**
	 * @param theVm
	 * @return
	 * @throws Exception
	 */
	private String getOperatingSystemDetails(VM theVm) throws Exception {
		
		OperatingSystemSectionType osSection = theVm.getOperatingSystemSection();
		
		return osSection.getDescription().getValue();
	}
	
	/**
	 * @param theVm
	 * @return
	 * @throws Exception
	 */
	private  String getNICDetails(VM theVm) throws Exception {
		
		HashMap<Integer, List<String>> results = new HashMap<Integer, List<String>>();
		List<String> ipAddrs;
		
		
//		List<VirtualNetworkCard> networkCards = theVm.getNetworkCards();
//		
//		int i=0;
//		for (VirtualNetworkCard netCard : networkCards) {
//			
//			ipAddrs = new ArrayList<String>();
//			ipAddrs.add(netCard.getNetwork());
//			ipAddrs.add(netCard.getIpAddress());
//			results.put(i++, ipAddrs);
//		}
		

		List<NetworkConnectionType> networkCards = theVm.getNetworkConnectionSection().getNetworkConnection();
		
		int i=0;
		for (NetworkConnectionType netCard : networkCards) {
			
			ipAddrs = new ArrayList<String>();
			ipAddrs.add(netCard.getNetwork());
			ipAddrs.add(netCard.getIpAddress());
			//ipAddrs.add("external="+netCard.getExternalIpAddress());
			results.put(i++, ipAddrs);
		}

		int nicIndex=0;
		StringBuffer sb = new StringBuffer();
		for (List<String> nicInfo : results.values()) {
			sb.append("NIC");
			sb.append(nicIndex++);
			sb.append("[");
			for (String val : nicInfo) {
				sb.append("|");
				sb.append(val);
				sb.append("|");	
			}
			sb.append("]");
		}
		return sb.toString();
	}
	
}
