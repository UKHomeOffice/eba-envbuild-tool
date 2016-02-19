package com.ipt.ebsa.agnostic.client.skyscape.comparator;

import java.util.Comparator;

import org.apache.commons.lang.StringUtils;

import com.vmware.vcloud.sdk.VirtualDisk;

/**
 * Class to provide a sort comparator for VirtualDisks, based on the ElementName, which is non-transient once allocated.
 *
 */
public class VirtualDiskComparator implements Comparator<VirtualDisk> {

	private static final String SPACE = " ";

	@Override
	public int compare(VirtualDisk d1, VirtualDisk d2) {
		
		String diskOneElement = d1.getItemResource().getElementName().getValue();
		String diskTwoElement = d2.getItemResource().getElementName().getValue();
		
		String diskOneNum = StringUtils.substringAfterLast(diskOneElement, SPACE);
		String diskTwoNum = StringUtils.substringAfterLast(diskTwoElement, SPACE);
		
		Integer dOne = Integer.valueOf(diskOneNum);
		Integer dTwo = Integer.valueOf(diskTwoNum);
		
		return dOne.compareTo(dTwo);
	}
}