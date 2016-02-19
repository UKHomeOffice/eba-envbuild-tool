package com.ipt.ebsa.manage.transitions;

import org.junit.Assert;
import org.junit.Test;


public class TransitionTest {

	@Test
	public void test() {
		Transition transition = new Transition();
		transition.setSequenceNumber(2);
		
		Assert.assertEquals(2, transition.getSequenceNumber());
	}

}
