package com.ipt.ebsa.manage.deploy.component;

import org.junit.Assert;
import org.junit.Test;

import com.ipt.ebsa.manage.util.VersionCompare;

/**
 * Unit test for the version comparison functionality
 * @author scowx
 *
 */
public class VersionCompareTest {

	@Test
	public void test() {
		VersionCompare v = new VersionCompare();
		
		Assert.assertEquals(-1, v.compare("1.0.81", "1.0.145-1"));
		
		Assert.assertEquals(0, v.compare("1.0.0-1", "1.0.0-1"));
		Assert.assertEquals(1, v.compare("1.0.1-1", "1.0.0-1"));
		Assert.assertEquals(-1, v.compare("1.0.0-1", "1.0.1-1"));
		Assert.assertEquals(1, v.compare("1.0.0.0", "1.0.0"));
		Assert.assertEquals(-1, v.compare("1.0.0", "1.0.0.0"));
		
		Assert.assertEquals(0, v.compare("1.0.0-1", "1.0.0-1"));
		Assert.assertEquals(1, v.compare("1.0.0-2", "1.0.0-1"));
		Assert.assertEquals(-1, v.compare("1.0.0-1", "1.0.0-2"));
		Assert.assertEquals(-1, v.compare("1.0.0", "1.0.0-1"));
		Assert.assertEquals(1, v.compare("1.0.0-1", "1.0.0"));
		
		Assert.assertEquals(-1, v.compare("1.0.0-1.nn.r", "1.0.0-1.nn.t"));
		Assert.assertEquals(1, v.compare("1.0.0-1.nn.t", "1.0.0-1.nn.r"));
		Assert.assertEquals(-1, v.compare("1.0.0-1.thisisaletter", "1.0.0-2.thisisaletter"));
		Assert.assertEquals(1, v.compare("1.0.0-2.thisisaletter", "1.0.0-1.thisisaletter"));
		Assert.assertEquals(-1, v.compare("a.b.c", "a.b.d"));
		Assert.assertEquals(1, v.compare("a.b.d", "a.b.c"));
		Assert.assertEquals(-1, v.compare("a.1.9", "a.1.z"));
		Assert.assertEquals(1, v.compare("a.1.z", "a.1.9"));
		Assert.assertEquals(0, v.compare("a.1.z", "a.1.z"));
		
		
		Assert.assertEquals(0, v.compare("1.0.0", "1.0.0"));
		Assert.assertEquals(1, v.compare("1.0.1", "1.0.0"));
		Assert.assertEquals(-1, v.compare("1.0.0", "1.0.1"));
		
		Assert.assertEquals(0, v.compare(null, null));
		Assert.assertEquals(1, v.compare("1.0.0", null));
		Assert.assertEquals(-1, v.compare(null, "1.0.0"));
		
		Assert.assertEquals(-1, v.compare("1.0.0", "1.n.n"));
		Assert.assertEquals(1, v.compare("1.n.n","1.0.0"));
		
		try { v.compare("1.n.n"," "); Assert.fail("Expected an exception because of a blank value"); } catch (Exception e) {}
		try { v.compare("1.n.n","1.n.a-"); Assert.fail("Expected an exception because of an invalid release"); } catch (Exception e) {}
		try { v.compare("1.n.n","-2"); Assert.fail("Expected an exception because of an invalid version"); } catch (Exception e) {}		
		
	}

}
