package com.ipt.ebsa.manage.deploy;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ipt.ebsa.buildtools.release.entities.ApplicationVersion;
import com.ipt.ebsa.deployment.descriptor.XMLChainBehaviourType;
import com.ipt.ebsa.manage.deploy.data.CVD;
import com.ipt.ebsa.manage.deploy.data.DDD;
import com.ipt.ebsa.manage.deploy.data.Data;
import com.ipt.ebsa.manage.deploy.data.ETD;
import com.ipt.ebsa.manage.deploy.data.YD;
import com.ipt.ebsa.manage.deploy.database.DBTest;

/**
 * This tests different combinations of deploying chains
 * @author scowx
 *
 */
public class ChainTestDeployerTest extends DBTest {
	
	private Deployer d = new Deployer();
	private Data data;
	
	@Before
	public void setUp() throws Exception {
	    data = new Data(getEntityManager());
	}
	
	@BeforeClass
	public static void setUpDirs() throws Exception {
		File directory = new File("target/chaindeployertest");
		FileUtils.deleteDirectory(directory);
		directory.mkdirs();
		File directory2 = new File("target/chaindeployertest_reports");
		FileUtils.deleteDirectory(directory2);
		directory2.mkdirs();
	}
	
	@Test
	public void testStraightForwardInstall() throws Exception {
		ApplicationVersion appVersion = data.setUpDataForTest("testStraightForwardInstall", getCVD("1.0.0"), 
				                                              new YD[]{new YD("one","absent","uni"),new YD("two","absent","uni"),new YD("three","absent","uni"),new YD("four","absent","uni")},
				                                              null,
				                                              "src/test/resources/ChainTest_DeploymentDescriptor.xml");
		ApplicationDeployment dep = d.deploy(appVersion, Data.ENV_UNITTEST, null);
		ETD[] s = new ETD[]{ new ETD(true,"1.0.0-1","system::packages/one/ensure","absent","/st/st-unittest/uni.yaml"),
				 new ETD(true,"1.0.0-1","system::packages/two/ensure","absent","/st/st-unittest/uni.yaml"),
				 new ETD(true,"1.0.0-1","system::packages/three/ensure","absent","/st/st-unittest/uni.yaml"),
				 new ETD(true,"1.0.0-1","system::packages/four/ensure","absent","/st/st-unittest/uni.yaml")};
		String csv = "four,1.0.0-1,com.group.a,four,1.0.0,war,\n"
				+ "one,1.0.0-1,com.group.a,one,1.0.0,war,\n"
				+ "three,1.0.0-1,com.group.a,three,1.0.0,war,\n"
				+ "two,1.0.0-1,com.group.a,two,1.0.0,war,\n";
		ChainDeploymentVerification.verify(dep, csv, 1, new int[]{4}, s);
	}
	
	@Test
	public void testBrandNewInstall() throws Exception {
		ApplicationVersion appVersion = data.setUpDataForTest("testBrandNewInstall",getCV_a("1.0.0"), 
															  null,
											                  null,
											                  "src/test/resources/ChainTestTwoMachine_DeploymentDescriptor.xml");
		ApplicationDeployment dep = d.deploy(appVersion, Data.ENV_BRANDNEW, null);
		ETD[] s = new ETD[]{ new ETD(true,"{ensure=1.0.0-1}","system::packages/one_a",null,"/st/st-brandnew/mc1.yaml"),
				 new ETD(true,"{ensure=1.0.0-1, require=Package[one_a]}","system::packages/two_a",null,"/st/st-brandnew/mc1.yaml")};
		String csv = "four_a,1.0.0-1,com.group.a,four_a,1.0.0,war,\n"
				+ "one_a,1.0.0-1,com.group.a,one_a,1.0.0,war,\n"
				+ "three_a,1.0.0-1,com.group.a,three_a,1.0.0,war,\n"
				+ "two_a,1.0.0-1,com.group.a,two_a,1.0.0,war,\n";
		ChainDeploymentVerification.verify(dep, csv, 2, new int[]{2,2}, s);
	}
	
	@Test
	public void testSingleComponentUpgrade() throws Exception {
		ApplicationVersion appVersion = data.setUpDataForTest("testSingleComponentUpgrade", getCVD("1.0.0","1.0.1","1.0.0","1.0.0"), 
				                                              new YD[]{new YD("one","1.0.0-1","uni"),new YD("two","1.0.0-1","uni"),new YD("three","1.0.0-1","uni"),new YD("four","1.0.0-1","uni")},
				                                              null,
				                                              "src/test/resources/ChainTest_DeploymentDescriptor.xml");
		ApplicationDeployment dep = d.deploy(appVersion, Data.ENV_UNITTEST, null);
		ETD[] s = new ETD[]{ new ETD(true,"1.0.1-1","system::packages/two/ensure","1.0.0-1","/st/st-unittest/uni.yaml"),};
		ChainDeploymentVerification.verify(dep, "two,1.0.1-1,com.group.a,two,1.0.1,war,\n", 1, new int[]{1}, s);
	}
	
	@Test
	public void testMultiComponentUpgrade() throws Exception {
		ApplicationVersion appVersion = data.setUpDataForTest("testSingleComponentUpgrade", getCVD("1.0.0","1.0.1","1.0.1","1.0.1"), 
				                                              new YD[]{new YD("one","1.0.0-1","uni"),new YD("two","1.0.0-1","uni"),new YD("three","1.0.0-1","uni"),new YD("four","1.0.0-1","uni")},
				                                              null,
				                                              "src/test/resources/ChainTest_DeploymentDescriptor.xml");
		ApplicationDeployment dep = d.deploy(appVersion, Data.ENV_UNITTEST, null);
		ETD[] s = new ETD[]{ new ETD(true,"1.0.1-1","system::packages/two/ensure","1.0.0-1","/st/st-unittest/uni.yaml"),
				             new ETD(true,"1.0.1-1","system::packages/three/ensure","1.0.0-1","/st/st-unittest/uni.yaml"),
				             new ETD(true,"1.0.1-1","system::packages/four/ensure","1.0.0-1","/st/st-unittest/uni.yaml"),};
		String csv = "four,1.0.1-1,com.group.a,four,1.0.1,war,\n"
				+ "three,1.0.1-1,com.group.a,three,1.0.1,war,\n"
				+ "two,1.0.1-1,com.group.a,two,1.0.1,war,\n";
		ChainDeploymentVerification.verify(dep, csv, 1, new int[]{3}, s);
	}
	
	@Test
	public void testMultiComponentMixture() throws Exception {
		ApplicationVersion appVersion = data.setUpDataForTest("testMultiComponentMixture", getCVD("1.0.0","0.0.9","absent","1.0.1"), 
				                                              new YD[]{new YD("one","1.0.0-1","uni"),new YD("two","1.0.0-1","uni"),new YD("three","1.0.0-1","uni"),new YD("four","1.0.0-1","uni")},
				                                              null,
				                                              "src/test/resources/ChainTest_DeploymentDescriptor.xml");
		ApplicationDeployment dep = d.deploy(appVersion, Data.ENV_UNITTEST, null);
		ETD[] t1 = new ETD[]{ new ETD(true,"absent","system::packages/three/ensure","1.0.0-1","/st/st-unittest/uni.yaml")};
		ETD[] t2 = new ETD[]{ new ETD(true,"0.0.9-1","system::packages/two/ensure","1.0.0-1","/st/st-unittest/uni.yaml"),
				             new ETD(true,"1.0.1-1","system::packages/four/ensure","1.0.0-1","/st/st-unittest/uni.yaml"),};
		ChainDeploymentVerification.verify(dep, "four,1.0.1-1,com.group.a,four,1.0.1,war,\ntwo,0.0.9-1,com.group.a,two,0.0.9,war,\n", 2, new int[]{1,2}, new ETD[][]{t1,t2});
	}
	
	@Test
	public void testStraightForwardUnInstall() throws Exception {
		ApplicationVersion appVersion = data.setUpDataForTest("testStraightForwardUnInstall",null, 
				                                              new YD[]{new YD("one","1.0.0","uni"),new YD("two","1.0.0","uni"),new YD("three","1.0.0","uni"),new YD("four","1.0.0","uni")},
				                                              null,
				                                              "src/test/resources/ChainTest_DeploymentDescriptor.xml");
		ApplicationDeployment dep = d.deploy(appVersion, Data.ENV_UNITTEST, null);
		ETD[] s = new ETD[]{ new ETD(true,"absent","system::packages/four/ensure","1.0.0","/st/st-unittest/uni.yaml"),
						   new ETD(true,"absent","system::packages/three/ensure","1.0.0","/st/st-unittest/uni.yaml"),
						   new ETD(true,"absent","system::packages/two/ensure","1.0.0","/st/st-unittest/uni.yaml"),
						   new ETD(true,"absent","system::packages/one/ensure","1.0.0","/st/st-unittest/uni.yaml")};
		ChainDeploymentVerification.verify(dep, null, 1, new int[]{4}, s);
	}
	
	@Test
	public void testStraightForwardUpgrade() throws Exception {
		ApplicationVersion appVersion = data.setUpDataForTest("testStraightForwardUpgrade", getCVD("1.0.1"), 
                                                              new YD[]{new YD("one","1.0.0","uni"),new YD("two","1.0.0","uni"),new YD("three","1.0.0","uni"),new YD("four","1.0.0","uni")}, 
				                                              null,
                                                              "src/test/resources/ChainTest_DeploymentDescriptor.xml");
		ApplicationDeployment dep = d.deploy(appVersion, Data.ENV_UNITTEST, null);
		ETD[] s = new ETD[]{ new ETD(true,"1.0.1-1","system::packages/one/ensure","1.0.0","/st/st-unittest/uni.yaml"),
				new ETD(true,"1.0.1-1","system::packages/two/ensure","1.0.0","/st/st-unittest/uni.yaml"),
				new ETD(true,"1.0.1-1","system::packages/three/ensure","1.0.0","/st/st-unittest/uni.yaml"),
				new ETD(true,"1.0.1-1","system::packages/four/ensure","1.0.0","/st/st-unittest/uni.yaml")};
		String csv = "four,1.0.1-1,com.group.a,four,1.0.1,war,\n"
				+ "one,1.0.1-1,com.group.a,one,1.0.1,war,\n"
				+ "three,1.0.1-1,com.group.a,three,1.0.1,war,\n"
				+ "two,1.0.1-1,com.group.a,two,1.0.1,war,\n";
		ChainDeploymentVerification.verify(dep, csv, 1, new int[]{4}, s);
	}
	
	@Test
	public void testStraightForwardDowngrade() throws Exception {
		ApplicationVersion appVersion = data.setUpDataForTest("testStraightForwardDowngrade",getCVD("1.0.0"), 
                                                              new YD[]{new YD("one","1.0.1","uni"),new YD("two","1.0.1","uni"),new YD("three","1.0.1","uni"),new YD("four","1.0.1","uni")}, 
										                      null,
                                                              "src/test/resources/ChainTest_DeploymentDescriptor.xml");
		ApplicationDeployment dep = d.deploy(appVersion, Data.ENV_UNITTEST, null);
		ETD[] s = new ETD[]{ new ETD(true,"1.0.0-1","system::packages/one/ensure","1.0.1","/st/st-unittest/uni.yaml"),
				 new ETD(true,"1.0.0-1","system::packages/two/ensure","1.0.1","/st/st-unittest/uni.yaml"),
				 new ETD(true,"1.0.0-1","system::packages/three/ensure","1.0.1","/st/st-unittest/uni.yaml"),
				 new ETD(true,"1.0.0-1","system::packages/four/ensure","1.0.1","/st/st-unittest/uni.yaml")};
		String csv = "four,1.0.0-1,com.group.a,four,1.0.0,war,\n"
				+ "one,1.0.0-1,com.group.a,one,1.0.0,war,\n"
				+ "three,1.0.0-1,com.group.a,three,1.0.0,war,\n"
				+ "two,1.0.0-1,com.group.a,two,1.0.0,war,\n";
		ChainDeploymentVerification.verify(dep, csv, 1, new int[]{4}, s);
	}
	

	
	@Test
	public void testMultiTransitionInstall() throws Exception {
		System.out.println("STARTING TEST testMultiTransitionInstall()");
		ApplicationVersion appVersion = data.setUpDataForTest("testMultiTransitionInstall",getCVD("1.0.1"), 
										                      new YD[]{
															    new YD("one","absent","uni"),
															    new YD("two","absent","uni"),
															    new YD("three","absent","uni"),
															    new YD("four","absent","uni")},
										                      new DDD(null, null, null, null, XMLChainBehaviourType.whole_chain_multi_transition,"0,1,2,3", null,null),
                                                              "src/test/resources/ChainTest_DeploymentDescriptor.xml");
		ApplicationDeployment dep = d.deploy(appVersion, Data.ENV_UNITTEST, null);
		ETD[] s = new ETD[]{ new ETD(true,"1.0.1-1","system::packages/one/ensure","absent","/st/st-unittest/uni.yaml")};
		String csv = "four,1.0.1-1,com.group.a,four,1.0.1,war,\n"
				+ "one,1.0.1-1,com.group.a,one,1.0.1,war,\n"
				+ "three,1.0.1-1,com.group.a,three,1.0.1,war,\n"
				+ "two,1.0.1-1,com.group.a,two,1.0.1,war,\n";
		ChainDeploymentVerification.verify(dep, csv, 4, new int[]{1,1,1,1}, s);
	}
	
	@Test
	public void testMultiTransitionUnInstall() throws Exception {
		ApplicationVersion appVersion = data.setUpDataForTest("testMultiTransitionUnInstall",null, 
															  new YD[]{new YD("one","1.0.0","uni"),new YD("two","1.0.0","uni"),new YD("three","1.0.0","uni"),new YD("four","1.0.0","uni")}, 
															  new DDD(null, null, null, null,null,null, XMLChainBehaviourType.whole_chain_multi_transition,"0,1,2,3"),
                                                              "src/test/resources/ChainTest_DeploymentDescriptor.xml");
		ApplicationDeployment dep = d.deploy(appVersion, Data.ENV_UNITTEST, null);
		ETD[] s = new ETD[]{ new ETD( true,"absent","system::packages/four/ensure","1.0.0","/st/st-unittest/uni.yaml")};
		ChainDeploymentVerification.verify(dep, null, 4, new int[]{1,1,1,1}, s);
	}
	
	@Test
	public void testMultiMachineMultiTransitonInstall() throws Exception {
		ApplicationVersion appVersion = data.setUpDataForTest("testMultiMachineMultiTransitonInstall",getCV_a("1.0.1"), 
				                                              new YD[]{new YD("one_a","absent","mc1"),new YD("two_a","absent","mc1"),new YD("three_a","absent","mc2"),new YD("four_a","absent","mc2")},
				                                              new DDD(null, null, null, null, XMLChainBehaviourType.whole_chain_multi_transition,"0,1,2,3", null,null),
                                                              "src/test/resources/ChainTestTwoMachine_DeploymentDescriptor.xml");
		ApplicationDeployment dep = d.deploy(appVersion, Data.ENV_UNITTEST, null);
		ETD[] s = new ETD[]{ new ETD(true,"1.0.1-1","system::packages/one_a/ensure","absent","/st/st-unittest/mc1.yaml")};
		String csv = "four_a,1.0.1-1,com.group.a,four_a,1.0.1,war,\n"
				+ "one_a,1.0.1-1,com.group.a,one_a,1.0.1,war,\n"
				+ "three_a,1.0.1-1,com.group.a,three_a,1.0.1,war,\n"
				+ "two_a,1.0.1-1,com.group.a,two_a,1.0.1,war,\n";
		ChainDeploymentVerification.verify(dep, csv, 4, new int[]{1,1,1,1}, s);
	}
	
	@Test
	public void testMultiMachineSingleTransitonInstall() throws Exception {
		ApplicationVersion appVersion = data.setUpDataForTest("testMultiMachineSingleTransitonInstall",getCV_a("1.0.1"), 
				                                              new YD[]{new YD("one_a","absent","mc1"),new YD("two_a","absent","mc1"),new YD("three_a","absent","mc2"),new YD("four_a","absent","mc2")},
				                                              null,
                                                              "src/test/resources/ChainTestTwoMachine_DeploymentDescriptor.xml");
		ApplicationDeployment dep = d.deploy(appVersion, Data.ENV_UNITTEST, null);
		ETD[] s = new ETD[] {
			new ETD(true, "1.0.1-1", "system::packages/one_a/ensure", "absent", "/st/st-unittest/mc1.yaml"),
			new ETD(true, "1.0.1-1", "system::packages/two_a/ensure", "absent", "/st/st-unittest/mc1.yaml"),
		};
		String csv = "four_a,1.0.1-1,com.group.a,four_a,1.0.1,war,\n"
				+ "one_a,1.0.1-1,com.group.a,one_a,1.0.1,war,\n"
				+ "three_a,1.0.1-1,com.group.a,three_a,1.0.1,war,\n"
				+ "two_a,1.0.1-1,com.group.a,two_a,1.0.1,war,\n";
		ChainDeploymentVerification.verify(dep, csv, 2, new int[]{2,2}, s);
	}
		
    @Test
	public void testMultiMachineMultiTransitionUnInstall() throws Exception {
		ApplicationVersion appVersion = data.setUpDataForTest("testMultiMachineMultiTransitonUnInstall",
															  null,
				                                              new YD[]{new YD("one_a","1.0.1","mc1"),new YD("two_a","1.0.1","mc1"),new YD("three_a","1.0.1","mc2"),new YD("four_a","1.0.1","mc2")},
				                                              new DDD(null, null, null, null,null,null, XMLChainBehaviourType.whole_chain_multi_transition,"0,1,2,3"),
                                                              "src/test/resources/ChainTestTwoMachine_DeploymentDescriptor.xml");
		ApplicationDeployment dep = d.deploy(appVersion, Data.ENV_UNITTEST, null);
		ETD[] s = new ETD[]{ new ETD(true,"absent","system::packages/four_a/ensure","1.0.1","/st/st-unittest/mc2.yaml")};
		ChainDeploymentVerification.verify(dep, null, 4, new int[]{1,1,1,1}, s);
	}	
    
    @Test
	public void testMultiMachineSingleTransitionUnInstall() throws Exception {
		ApplicationVersion appVersion = data.setUpDataForTest("testMultiMachineSingleTransitonUnInstall", 
															  null,
				                                              new YD[]{new YD("one_a","1.0.1","mc1"),new YD("two_a","1.0.1","mc1"),new YD("three_a","1.0.1","mc2"),new YD("four_a","1.0.1","mc2")},
				                                              null,
                                                              "src/test/resources/ChainTestTwoMachine_DeploymentDescriptor.xml");
		ApplicationDeployment dep = d.deploy(appVersion, Data.ENV_UNITTEST, null);
		ETD[] s = new ETD[]{
			new ETD(true,"absent","system::packages/four_a/ensure","1.0.1","/st/st-unittest/mc2.yaml"),
			new ETD(true,"absent","system::packages/three_a/ensure","1.0.1","/st/st-unittest/mc2.yaml")
		};
		ChainDeploymentVerification.verify(dep, null, 2, new int[]{2,2}, s);
	}	
	
	private CVD[] getCVD(String... version) {
		if (version.length == 1) {
		  return new CVD[]{new CVD("one",version[0]),new CVD("two",version[0]),new CVD("three",version[0]),new CVD("four",version[0])};
		}
		else if (version.length != 4) {
			throw new RuntimeException("Its 4 or 1 or nothing!!");
		}
		else {
			return new CVD[]{new CVD("one",version[0]),new CVD("two",version[1]),new CVD("three",version[2]),new CVD("four",version[3])};
		}
	}
	
	private CVD[] getCV_a(String version) {
		return new CVD[]{new CVD("one_a",version),new CVD("two_a",version),new CVD("three_a",version),new CVD("four_a",version)};
	}
}
