package com.ipt.ebsa.agnostic.client.skyscape.comparator;

import java.util.Comparator;

import com.ipt.ebsa.agnostic.client.skyscape.module.NicDetail;

/**
 * 
 *
 */
public class NicDetailComparator implements Comparator<NicDetail> {

	@Override
	public int compare(NicDetail o1, NicDetail o2) {
		return o1.getNicNumber().compareTo(o2.getNicNumber());
	}

}
