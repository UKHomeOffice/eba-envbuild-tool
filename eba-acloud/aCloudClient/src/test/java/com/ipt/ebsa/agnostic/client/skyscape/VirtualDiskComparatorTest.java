package com.ipt.ebsa.agnostic.client.skyscape;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.ipt.ebsa.agnostic.client.skyscape.comparator.VirtualDiskComparator;
import com.jcabi.aspects.Loggable;
import com.vmware.vcloud.api.rest.schema.ovf.CimString;
import com.vmware.vcloud.api.rest.schema.ovf.RASDType;
import com.vmware.vcloud.sdk.VirtualDisk;

@Loggable(prepend = true)
public class VirtualDiskComparatorTest {

	private Logger logger = LogManager.getLogger(VirtualDiskComparatorTest.class);
	
	// Set up the Mock VirtualDisks
	VirtualDisk mockDiskOne;
	VirtualDisk mockDiskTwo;
	VirtualDisk mockDiskThree;
	VirtualDisk mockDiskTen;
	VirtualDisk mockDiskHundred;
	VirtualDisk mockDiskMinusOne;
	VirtualDisk mockDiskZero;
	
	//Set up the Mock RASDItems
	RASDType mockRasdTypeOne;
	RASDType mockRasdTypeTwo;
	RASDType mockRasdTypeThree;
	RASDType mockRasdTypeFour;
	RASDType mockRasdTypeFive;
	RASDType mockRasdTypeMinusOne;
	RASDType mockRasdTypeZero;
	
	// Set up Mock CIMStrings
	CimString mockCimStringOne;
	CimString mockCimStringTwo;
	CimString mockCimStringThree;
	CimString mockCimStringFour;
	CimString mockCimStringFive;
	CimString mockCimStringMinusOne;
	CimString mockCimStringZero;
	
	private static final String HDD_ELEMENT_NAME_ONE = "Hard Disk 1";
	private static final String HDD_ELEMENT_NAME_TWO = "Hard Disk 2";
	private static final String HDD_ELEMENT_NAME_THREE = "Hard Disk 3";
	private static final String HDD_ELEMENT_NAME_TEN = "Hard Disk 10";
	private static final String HDD_ELEMENT_NAME_HUNDRED = "Hard Disk 100";
	private static final String HDD_ELEMENT_NAME_MINUS_ONE = "Hard Disk -1";
	private static final String HDD_ELEMENT_NAME_ZERO = "Hard Disk 0";
	
	@Before
	public void setUp(){
		mockDiskOne = mock(VirtualDisk.class);
		mockDiskTwo = mock(VirtualDisk.class);
		mockDiskThree = mock(VirtualDisk.class);
		mockDiskTen = mock(VirtualDisk.class);
		mockDiskHundred = mock(VirtualDisk.class);
		mockDiskMinusOne = mock(VirtualDisk.class);
		mockDiskZero = mock(VirtualDisk.class);
		mockRasdTypeOne = mock(RASDType.class);
		mockRasdTypeTwo = mock(RASDType.class);
		mockRasdTypeThree = mock(RASDType.class);
		mockRasdTypeFour = mock(RASDType.class);
		mockRasdTypeFive = mock(RASDType.class);
		mockRasdTypeMinusOne = mock(RASDType.class);
		mockRasdTypeZero = mock(RASDType.class);
		mockCimStringOne = mock(CimString.class);
		mockCimStringTwo = mock(CimString.class);	
		mockCimStringThree = mock(CimString.class);
		mockCimStringFour = mock(CimString.class);
		mockCimStringFive = mock(CimString.class);
		mockCimStringMinusOne = mock(CimString.class);
		mockCimStringZero = mock(CimString.class);
		
		// Set up our expected return from the mock CIMStrings when called by the invoked method
		doReturn(HDD_ELEMENT_NAME_ONE).
		when(mockCimStringOne).getValue();
		doReturn(HDD_ELEMENT_NAME_TWO).
		when(mockCimStringTwo).getValue();
		doReturn(HDD_ELEMENT_NAME_THREE).
		when(mockCimStringThree).getValue();
		doReturn(HDD_ELEMENT_NAME_TEN).
		when(mockCimStringFour).getValue();
		doReturn(HDD_ELEMENT_NAME_HUNDRED).
		when(mockCimStringFive).getValue();
		doReturn(HDD_ELEMENT_NAME_MINUS_ONE).
		when(mockCimStringMinusOne).getValue();
		doReturn(HDD_ELEMENT_NAME_ZERO).
		when(mockCimStringZero).getValue();
				
		// Set up our expected return from the mock RASDType when called by the invoked method
		doReturn(mockCimStringOne).
		when(mockRasdTypeOne).getElementName();
		doReturn(mockCimStringTwo).
		when(mockRasdTypeTwo).getElementName();
		doReturn(mockCimStringThree).
		when(mockRasdTypeThree).getElementName();
		doReturn(mockCimStringFour).
		when(mockRasdTypeFour).getElementName();
		doReturn(mockCimStringFive).
		when(mockRasdTypeFive).getElementName();
		doReturn(mockCimStringMinusOne).
		when(mockRasdTypeMinusOne).getElementName();
		doReturn(mockCimStringZero).
		when(mockRasdTypeZero).getElementName();
		
		// Set up our expected return from the mock Disk when called by the invoked method
		doReturn(mockRasdTypeOne).
		when(mockDiskOne).getItemResource();
		doReturn(mockRasdTypeTwo).
		when(mockDiskTwo).getItemResource();
		doReturn(mockRasdTypeThree).
		when(mockDiskThree).getItemResource();
		doReturn(mockRasdTypeFour).
		when(mockDiskTen).getItemResource();
		doReturn(mockRasdTypeFive).
		when(mockDiskHundred).getItemResource();
		doReturn(mockRasdTypeMinusOne).
		when(mockDiskMinusOne).getItemResource();
		doReturn(mockRasdTypeZero).
		when(mockDiskZero).getItemResource();
	}
	
	@After
    public void tearDown() {
		mockDiskOne = null;
		mockDiskTwo = null;
		mockDiskThree = null;
		mockDiskTen = null;
		mockDiskHundred = null;
		mockDiskMinusOne = null;
		mockDiskZero = null;
		mockRasdTypeOne = null;
		mockRasdTypeTwo = null;
		mockRasdTypeThree = null;
		mockRasdTypeFour = null;
		mockRasdTypeFive = null;
		mockRasdTypeMinusOne = null;
		mockRasdTypeZero = null;
		mockCimStringOne = null;
		mockCimStringTwo = null;
		mockCimStringThree = null;
		mockCimStringFour = null;
		mockCimStringFive = null;
		mockCimStringMinusOne = null;
		mockCimStringZero = null;
	}
	
	
	
	/**
	 * Method to ensure a list of Virtual=Disks is sorted into the correct order by the comparator.
	 */
	@Test
	public void testVirtualDiskComparatorLowHighSeq(){
		
		List<VirtualDisk> disks = new ArrayList<VirtualDisk>();
		
		disks.add(mockDiskOne);
		disks.add(mockDiskTwo);
		disks.add(mockDiskThree);
		
		Assert.assertEquals(mockDiskOne, disks.get(0));
		Assert.assertEquals(mockDiskTwo, disks.get(1));
		Assert.assertEquals(mockDiskThree, disks.get(2));	
		
		//  - change to use ElementName as identifier and key for list sort order, instead of AddressOnParent
		Collections.sort(disks, new VirtualDiskComparator());
		
		Assert.assertEquals(mockDiskOne, disks.get(0));
		Assert.assertEquals(mockDiskTwo, disks.get(1));
		Assert.assertEquals(mockDiskThree, disks.get(2));		
	}
	
	/**
	 * Method to ensure a list of Virtual=Disks is sorted into the correct order by the comparator.
	 */
	@Test
	public void testVirtualDiskComparatorHighLowSeq(){
		
		List<VirtualDisk> disks = new ArrayList<VirtualDisk>();
		
		disks.add(mockDiskThree);
		disks.add(mockDiskTwo);
		disks.add(mockDiskOne);
		
		Assert.assertEquals(mockDiskThree, disks.get(0));
		Assert.assertEquals(mockDiskTwo, disks.get(1));
		Assert.assertEquals(mockDiskOne, disks.get(2));	
		
		//  - change to use ElementName as identifier and key for list sort order, instead of AddressOnParent
		Collections.sort(disks, new VirtualDiskComparator());
		
		Assert.assertEquals(mockDiskOne, disks.get(0));
		Assert.assertEquals(mockDiskTwo, disks.get(1));
		Assert.assertEquals(mockDiskThree, disks.get(2));		
	}
	
	/**
	 * Method to ensure a list of Virtual=Disks is sorted into the correct order by the comparator.
	 */
	@Test
	public void testVirtualDiskComparatorMixedSeq(){
		
		List<VirtualDisk> disks = new ArrayList<VirtualDisk>();
		
		disks.add(mockDiskTwo);
		disks.add(mockDiskOne);
		disks.add(mockDiskThree);
		
		Assert.assertEquals(mockDiskTwo, disks.get(0));
		Assert.assertEquals(mockDiskOne, disks.get(1));
		Assert.assertEquals(mockDiskThree, disks.get(2));	
		
		//  - change to use ElementName as identifier and key for list sort order, instead of AddressOnParent
		Collections.sort(disks, new VirtualDiskComparator());
		
		Assert.assertEquals(mockDiskOne, disks.get(0));
		Assert.assertEquals(mockDiskTwo, disks.get(1));
		Assert.assertEquals(mockDiskThree, disks.get(2));		
	}
	
	/**
	 * Method to ensure a list of Virtual=Disks is sorted into the correct order by the comparator.
	 */
	@Test
	public void testVirtualDiskComparatorLowHighSeqTenHundred(){
		
		List<VirtualDisk> disks = new ArrayList<VirtualDisk>();
		
		disks.add(mockDiskOne);
		disks.add(mockDiskTwo);
		disks.add(mockDiskThree);
		disks.add(mockDiskTen);
		disks.add(mockDiskHundred);		
				
		Assert.assertEquals(mockDiskOne, disks.get(0));
		Assert.assertEquals(mockDiskTwo, disks.get(1));
		Assert.assertEquals(mockDiskThree, disks.get(2));
		Assert.assertEquals(mockDiskTen, disks.get(3));
		Assert.assertEquals(mockDiskHundred, disks.get(4));	
		
		//  - change to use ElementName as identifier and key for list sort order, instead of AddressOnParent
		Collections.sort(disks, new VirtualDiskComparator());
		
		Assert.assertEquals(mockDiskOne, disks.get(0));
		Assert.assertEquals(mockDiskTwo, disks.get(1));
		Assert.assertEquals(mockDiskThree, disks.get(2));	
		Assert.assertEquals(mockDiskTen, disks.get(3));
		Assert.assertEquals(mockDiskHundred, disks.get(4));
	}
	
	/**
	 * Method to ensure a list of Virtual=Disks is sorted into the correct order by the comparator.
	 */
	@Test
	public void testVirtualDiskComparatorHighLowSeqTenHundred(){
		
		List<VirtualDisk> disks = new ArrayList<VirtualDisk>();
			
		disks.add(mockDiskHundred);		
		disks.add(mockDiskTen);
		disks.add(mockDiskThree);
		disks.add(mockDiskTwo);
		disks.add(mockDiskOne);
		
		
		Assert.assertEquals(mockDiskHundred, disks.get(0));
		Assert.assertEquals(mockDiskTen, disks.get(1));
		Assert.assertEquals(mockDiskThree, disks.get(2));
		Assert.assertEquals(mockDiskTwo, disks.get(3));
		Assert.assertEquals(mockDiskOne, disks.get(4));	
		
		//  - change to use ElementName as identifier and key for list sort order, instead of AddressOnParent
		Collections.sort(disks, new VirtualDiskComparator());
		
		Assert.assertEquals(mockDiskOne, disks.get(0));
		Assert.assertEquals(mockDiskTwo, disks.get(1));
		Assert.assertEquals(mockDiskThree, disks.get(2));	
		Assert.assertEquals(mockDiskTen, disks.get(3));
		Assert.assertEquals(mockDiskHundred, disks.get(4));
	}
	
	/**
	 * Method to ensure a list of Virtual=Disks is sorted into the correct order by the comparator.
	 */
	@Test
	public void testVirtualDiskComparatorMixedSeqTenHundred(){
		
		List<VirtualDisk> disks = new ArrayList<VirtualDisk>();
		
		disks.add(mockDiskTen);
		disks.add(mockDiskTwo);
		disks.add(mockDiskHundred);		
		disks.add(mockDiskThree);
		disks.add(mockDiskOne);
		
		Assert.assertEquals(mockDiskTen, disks.get(0));
		Assert.assertEquals(mockDiskTwo, disks.get(1));
		Assert.assertEquals(mockDiskHundred, disks.get(2));
		Assert.assertEquals(mockDiskThree, disks.get(3));
		Assert.assertEquals(mockDiskOne, disks.get(4));	
		
		//  - change to use ElementName as identifier and key for list sort order, instead of AddressOnParent
		Collections.sort(disks, new VirtualDiskComparator());
		
		Assert.assertEquals(mockDiskOne, disks.get(0));
		Assert.assertEquals(mockDiskTwo, disks.get(1));
		Assert.assertEquals(mockDiskThree, disks.get(2));	
		Assert.assertEquals(mockDiskTen, disks.get(3));
		Assert.assertEquals(mockDiskHundred, disks.get(4));
	}
	
	/**
	 * Method to ensure a list of Virtual=Disks is sorted into the correct order by the comparator.
	 */
	@Test
	public void testVirtualDiskComparatorLowHighSeqZeroMinus(){
		
		List<VirtualDisk> disks = new ArrayList<VirtualDisk>();
				
		disks.add(mockDiskMinusOne);
		disks.add(mockDiskZero);
		disks.add(mockDiskOne);
		disks.add(mockDiskTwo);
		disks.add(mockDiskThree);
		
		Assert.assertEquals(mockDiskMinusOne, disks.get(0));
		Assert.assertEquals(mockDiskZero, disks.get(1));
		Assert.assertEquals(mockDiskOne, disks.get(2));
		Assert.assertEquals(mockDiskTwo, disks.get(3));
		Assert.assertEquals(mockDiskThree, disks.get(4));
		
		//  - change to use ElementName as identifier and key for list sort order, instead of AddressOnParent
		Collections.sort(disks, new VirtualDiskComparator());
		
		Assert.assertEquals(mockDiskMinusOne, disks.get(0));
		Assert.assertEquals(mockDiskZero, disks.get(1));
		Assert.assertEquals(mockDiskOne, disks.get(2));
		Assert.assertEquals(mockDiskTwo, disks.get(3));
		Assert.assertEquals(mockDiskThree, disks.get(4));		
	}
	
	/**
	 * Method to ensure a list of Virtual=Disks is sorted into the correct order by the comparator.
	 */
	@Test
	public void testVirtualDiskComparatorHighLowSeqZeroMinus(){
		
		List<VirtualDisk> disks = new ArrayList<VirtualDisk>();
				
		disks.add(mockDiskThree);
		disks.add(mockDiskTwo);
		disks.add(mockDiskOne);
		disks.add(mockDiskZero);
		disks.add(mockDiskMinusOne);
		
		Assert.assertEquals(mockDiskThree, disks.get(0));
		Assert.assertEquals(mockDiskTwo, disks.get(1));
		Assert.assertEquals(mockDiskOne, disks.get(2));
		Assert.assertEquals(mockDiskZero, disks.get(3));
		Assert.assertEquals(mockDiskMinusOne, disks.get(4));
		
		//  - change to use ElementName as identifier and key for list sort order, instead of AddressOnParent
		Collections.sort(disks, new VirtualDiskComparator());
		
		Assert.assertEquals(mockDiskMinusOne, disks.get(0));
		Assert.assertEquals(mockDiskZero, disks.get(1));
		Assert.assertEquals(mockDiskOne, disks.get(2));
		Assert.assertEquals(mockDiskTwo, disks.get(3));
		Assert.assertEquals(mockDiskThree, disks.get(4));		
	}
	
	/**
	 * Method to ensure a list of Virtual=Disks is sorted into the correct order by the comparator.
	 */
	@Test
	public void testVirtualDiskComparatorMixedSeqZeroMinus(){
		
		List<VirtualDisk> disks = new ArrayList<VirtualDisk>();
				
		disks.add(mockDiskThree);
		disks.add(mockDiskMinusOne);
		disks.add(mockDiskOne);
		disks.add(mockDiskZero);		
		disks.add(mockDiskTwo);
		
		
		Assert.assertEquals(mockDiskThree, disks.get(0));
		Assert.assertEquals(mockDiskMinusOne, disks.get(1));
		Assert.assertEquals(mockDiskOne, disks.get(2));
		Assert.assertEquals(mockDiskZero, disks.get(3));
		Assert.assertEquals(mockDiskTwo, disks.get(4));
		
		//  - change to use ElementName as identifier and key for list sort order, instead of AddressOnParent
		Collections.sort(disks, new VirtualDiskComparator());
		
		Assert.assertEquals(mockDiskMinusOne, disks.get(0));
		Assert.assertEquals(mockDiskZero, disks.get(1));
		Assert.assertEquals(mockDiskOne, disks.get(2));
		Assert.assertEquals(mockDiskTwo, disks.get(3));
		Assert.assertEquals(mockDiskThree, disks.get(4));		
	}	
	
	/**
	 * Method to ensure a list of Virtual=Disks is sorted into the correct order by the comparator.
	 */
	@Test
	public void testVirtualDiskComparatorLowHighSeqZeroMinusTenHundred(){
		
		List<VirtualDisk> disks = new ArrayList<VirtualDisk>();
				
		disks.add(mockDiskMinusOne);		
		disks.add(mockDiskZero);
		disks.add(mockDiskOne);
		disks.add(mockDiskTwo);
		disks.add(mockDiskThree);
		disks.add(mockDiskTen);
		disks.add(mockDiskHundred);
				
		Assert.assertEquals(mockDiskMinusOne, disks.get(0));
		Assert.assertEquals(mockDiskZero, disks.get(1));
		Assert.assertEquals(mockDiskOne, disks.get(2));
		Assert.assertEquals(mockDiskTwo, disks.get(3));
		Assert.assertEquals(mockDiskThree, disks.get(4));
		Assert.assertEquals(mockDiskTen, disks.get(5));
		Assert.assertEquals(mockDiskHundred, disks.get(6));
		
		//  - change to use ElementName as identifier and key for list sort order, instead of AddressOnParent
		Collections.sort(disks, new VirtualDiskComparator());
		
		Assert.assertEquals(mockDiskMinusOne, disks.get(0));
		Assert.assertEquals(mockDiskZero, disks.get(1));
		Assert.assertEquals(mockDiskOne, disks.get(2));
		Assert.assertEquals(mockDiskTwo, disks.get(3));
		Assert.assertEquals(mockDiskThree, disks.get(4));	
		Assert.assertEquals(mockDiskTen, disks.get(5));
		Assert.assertEquals(mockDiskHundred, disks.get(6));
	}
	
	/**
	 * Method to ensure a list of Virtual=Disks is sorted into the correct order by the comparator.
	 */
	@Test
	public void testVirtualDiskComparatorHighLowSeqZeroMinusTenHundred(){
		
		List<VirtualDisk> disks = new ArrayList<VirtualDisk>();
				
		disks.add(mockDiskHundred);		
		disks.add(mockDiskTen);
		disks.add(mockDiskThree);
		disks.add(mockDiskTwo);
		disks.add(mockDiskOne);
		disks.add(mockDiskZero);
		disks.add(mockDiskMinusOne);
				
		Assert.assertEquals(mockDiskHundred, disks.get(0));
		Assert.assertEquals(mockDiskTen, disks.get(1));
		Assert.assertEquals(mockDiskThree, disks.get(2));
		Assert.assertEquals(mockDiskTwo, disks.get(3));
		Assert.assertEquals(mockDiskOne, disks.get(4));
		Assert.assertEquals(mockDiskZero, disks.get(5));
		Assert.assertEquals(mockDiskMinusOne, disks.get(6));
		
		//  - change to use ElementName as identifier and key for list sort order, instead of AddressOnParent
		Collections.sort(disks, new VirtualDiskComparator());
		
		Assert.assertEquals(mockDiskMinusOne, disks.get(0));
		Assert.assertEquals(mockDiskZero, disks.get(1));
		Assert.assertEquals(mockDiskOne, disks.get(2));
		Assert.assertEquals(mockDiskTwo, disks.get(3));
		Assert.assertEquals(mockDiskThree, disks.get(4));	
		Assert.assertEquals(mockDiskTen, disks.get(5));
		Assert.assertEquals(mockDiskHundred, disks.get(6));
	}
	
	/**
	 * Method to ensure a list of Virtual=Disks is sorted into the correct order by the comparator.
	 */
	@Test
	public void testVirtualDiskComparatorMixedSeqZeroMinusTenHundred(){
		
		List<VirtualDisk> disks = new ArrayList<VirtualDisk>();
				
		disks.add(mockDiskThree);
		disks.add(mockDiskTen);
		disks.add(mockDiskMinusOne);
		disks.add(mockDiskOne);
		disks.add(mockDiskHundred);
		disks.add(mockDiskZero);		
		disks.add(mockDiskTwo);
		
		
		Assert.assertEquals(mockDiskThree, disks.get(0));
		Assert.assertEquals(mockDiskTen, disks.get(1));
		Assert.assertEquals(mockDiskMinusOne, disks.get(2));
		Assert.assertEquals(mockDiskOne, disks.get(3));
		Assert.assertEquals(mockDiskHundred, disks.get(4));
		Assert.assertEquals(mockDiskZero, disks.get(5));
		Assert.assertEquals(mockDiskTwo, disks.get(6));
		
		//  - change to use ElementName as identifier and key for list sort order, instead of AddressOnParent
		Collections.sort(disks, new VirtualDiskComparator());
		
		Assert.assertEquals(mockDiskMinusOne, disks.get(0));
		Assert.assertEquals(mockDiskZero, disks.get(1));
		Assert.assertEquals(mockDiskOne, disks.get(2));
		Assert.assertEquals(mockDiskTwo, disks.get(3));
		Assert.assertEquals(mockDiskThree, disks.get(4));	
		Assert.assertEquals(mockDiskTen, disks.get(5));
		Assert.assertEquals(mockDiskHundred, disks.get(6));
	}
	
	/**
	 * Non-test methods. Just for convenience.
	 */
	
	public void testStringComparator()
	{
		logger.debug("testStringComparator");
		
		String one = "1";
		String two = "2";
		
		String seven = "7";
		
		String ten = "10";
		String hundred = "100";
		
		logger.debug("one compared to two is :: " + one.compareTo(two));
		logger.debug("two compared to one is :: " + two.compareTo(one));
		logger.debug("one compared to one is :: " + one.compareTo(one));
		
		logger.debug("one compared to seven is :: " + one.compareTo(seven));
		logger.debug("seven compared to one is :: " + seven.compareTo(one));
		logger.debug("seven compared to seven is :: " + seven.compareTo(seven));
		
		logger.debug("seven compared to ten is :: " + one.compareTo(ten));
		logger.debug("ten compared to seven is :: " + ten.compareTo(one));
		
		logger.debug("one compared to ten is :: " + one.compareTo(ten));
		logger.debug("ten compared to one is :: " + ten.compareTo(one));
		logger.debug("ten compared to ten is :: " + ten.compareTo(ten));
		
		logger.debug("one compared to hundred is :: " + one.compareTo(hundred));
		logger.debug("hundred compared to one is :: " + hundred.compareTo(one));
		logger.debug("hundred compared to hundred is :: " + hundred.compareTo(hundred));
		
		logger.debug("ten compared to hundred is :: " + ten.compareTo(hundred));
		logger.debug("hundred compared to ten is :: " + hundred.compareTo(ten));
		
	}
	
	public void testIntegerComparator()
	{
		logger.debug("testIntegerComparator");
		
		Integer one = 1;
		Integer two = 2;
		
		Integer seven = 7;
		
		Integer ten = 10;
		Integer hundred = 100;
		
		logger.debug("one compared to two is :: " + one.compareTo(two));
		logger.debug("two compared to one is :: " + two.compareTo(one));
		logger.debug("one compared to one is :: " + one.compareTo(one));
		
		logger.debug("one compared to seven is :: " + one.compareTo(seven));
		logger.debug("seven compared to one is :: " + seven.compareTo(one));
		logger.debug("seven compared to seven is :: " + seven.compareTo(seven));
		
		logger.debug("seven compared to ten is :: " + one.compareTo(ten));
		logger.debug("ten compared to seven is :: " + ten.compareTo(one));
		
		logger.debug("one compared to ten is :: " + one.compareTo(ten));
		logger.debug("ten compared to one is :: " + ten.compareTo(one));
		logger.debug("ten compared to ten is :: " + ten.compareTo(ten));
		
		logger.debug("one compared to hundred is :: " + one.compareTo(hundred));
		logger.debug("hundred compared to one is :: " + hundred.compareTo(one));
		logger.debug("hundred compared to hundred is :: " + hundred.compareTo(hundred));
		
		logger.debug("ten compared to hundred is :: " + ten.compareTo(hundred));
		logger.debug("hundred compared to ten is :: " + hundred.compareTo(ten));
		
	}
}
