package com.ipt.ebsa.agnostic.client.skyscape.comparator;

import java.util.Comparator;

import com.vmware.vcloud.sdk.VirtualNetworkCard;

/**
 * 
 *
 */
public class VirtualNetworkCardComparator implements
		Comparator<VirtualNetworkCard> {

	@Override
	public int compare(VirtualNetworkCard o1, VirtualNetworkCard o2) {
		return o1.getItemResource().getAddressOnParent().getValue().compareTo(o2.getItemResource().getAddressOnParent().getValue());
	}

}
