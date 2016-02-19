package com.ipt.ebsa.manage.deploy.data;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import com.ipt.ebsa.buildtools.release.entities.ApplicationVersion;
import com.ipt.ebsa.config.ConfigurationFactory;
import com.ipt.ebsa.config.Organisation;
import com.ipt.ebsa.deployment.descriptor.DDConfigurationLoader;
import com.ipt.ebsa.deployment.descriptor.DDConfigurationWriter;
import com.ipt.ebsa.deployment.descriptor.ObjectFactory;
import com.ipt.ebsa.deployment.descriptor.XMLComponentType;
import com.ipt.ebsa.deployment.descriptor.XMLDeploymentDescriptorType;
import com.ipt.ebsa.deployment.descriptor.XMLHintsType.XMLDeploy;
import com.ipt.ebsa.deployment.descriptor.XMLHintsType.XMLDowngrade;
import com.ipt.ebsa.deployment.descriptor.XMLHintsType.XMLUndeploy;
import com.ipt.ebsa.deployment.descriptor.XMLHintsType.XMLUpgrade;
import com.ipt.ebsa.environment.MachineState;
import com.ipt.ebsa.hiera.HieraData;
import com.ipt.ebsa.hiera.NodeMissingBehaviour;
import com.ipt.ebsa.manage.Configuration;
import com.ipt.ebsa.manage.deploy.database.DBTestUtil;
import com.ipt.ebsa.manage.hiera.HieraEnvironmentStateManager;
import com.ipt.ebsa.yaml.YamlInjector;
import com.ipt.ebsa.yaml.YamlUtil;

/**
 * Test data utility for the Deployer tests 
 * @author scowx
 *
 */
public class Data {
	
	private final Logger log = LogManager.getLogger(Data.class);
	
	private EntityManager entityManager;
	private HieraEnvironmentStateManager envStateManager;
	public static final String ENV_UNITTEST = "IPT_ST_UNITTEST";
	public static final String ENV_BRANDNEW = "IPT_ST_BRANDNEW";
	private static final String APP = "APP";
	private static final String ZONE = "IPT_ST_SIT1_COR1";
	
	public Data(EntityManager entityManager) throws Exception {
		this.entityManager = entityManager;
	}
	
	public EntityManager getEntityManager() {
		return entityManager;
	}

	/**
	 * Sets up data for a standard install
	 * @return
	 * @throws Exception
	 * @throws SAXException
	 * @throws IOException
	 */
	public ApplicationVersion setUpDataForTest(String name, CVD[] cvVersions, YD[] yamldata, DDD deploymentDescriptorData, String ddFileLocation) throws Exception, SAXException, IOException {
		
		System.out.println("STARTING TEST '"+name+"'");
		
		
		/* Configure the code to pick up our deployment descriptor and the YAML in a known location where we have edited it */
		String targetHeiraLocation = "target/chaindeployertest/heira";
		String ddOutput = "target/chaindeployertest/deploymentdescriptor.xml";
		String reportFolder = "target/chaindeployertest_reports";
		
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_PREPARATION_SUMMARY_REPORT_USE_UNIQUE_NAME, "false");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_PREPARE_ONLY, "true");
		ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_DEPLOYMENT_DESCRIPTOR, ddOutput);
	    ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_HIERA_FOLDER, targetHeiraLocation);
	    ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_PREPARATION_SUMMARY_REPORT_FOLDER, reportFolder);
	    ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_PREPARATION_SUMMARY_REPORT_FILENAME, name + ".html");
	    ConfigurationFactory.getProperties().put(Configuration.DEPLOYMENT_CONFIG_USE_EXISTING_DD_DATA, "true");

		//ensure that CSV's are always created regardless of if ST or NP environment
		ConfigurationFactory.getProperties().put("np.yum.repo.update.enabled", "true");
		ConfigurationFactory.getProperties().put("st.yum.repo.update.enabled", "true");
	    
	    /* Clear the output directory where we will write our test input data to */
		FileUtils.deleteDirectory(new File("target/chaindeployertest"));
		
		/* Set up database data */
		String group = "com.group.a";
		String[][] data = null;
		if (cvVersions != null) {
			data = new String[cvVersions.length][4];
			for (int i = 0; i < cvVersions.length; i++) {
				data[i] = new String[]{
						cvVersions[i].componentName.toUpperCase(), group, 
						cvVersions[i].componentName, cvVersions[i].version,
						cvVersions[i].componentName.toLowerCase(), cvVersions[i].version + "-1" //make up some rpm package / versions
					};
			}	
		}
		
		ApplicationVersion appVersion = DBTestUtil.createApplicationVersion(getEntityManager(), "APP", "Application Layer", data);
		
		/* Set up Hiera data */
		FileUtils.copyDirectory(new File("src/test/resources/hiera"), new File(targetHeiraLocation));
		envStateManager = new HieraEnvironmentStateManager();
		File hieraFolder = new File(Configuration.getHieraFolder());
		envStateManager.load(hieraFolder, new Organisation("st"), null, null);
		createHiera(targetHeiraLocation + "/st/st-unittest", ENV_UNITTEST, yamldata);
		
		/* Set up a deployment descriptor */
		log.debug("Loading deployment descriptor from '"+ddFileLocation+"'");
		XMLDeploymentDescriptorType dd = new DDConfigurationLoader().loadDD(new File(ddFileLocation), "APP").getXMLType();
		if (deploymentDescriptorData != null) {
			ObjectFactory o = new ObjectFactory();
			XMLUndeploy undeploy = null;
			XMLDowngrade downgrade = null;
			XMLUpgrade upgrade = null;
			XMLDeploy deploy = null;
			
			if (deploymentDescriptorData.undeployBehaviour != null) {
				undeploy = o.createXMLHintsTypeXMLUndeploy();
				undeploy.setChainBehaviour(deploymentDescriptorData.undeployBehaviour);
			}
			
			if (deploymentDescriptorData.deployBehaviour != null) {
				deploy = o.createXMLHintsTypeXMLDeploy();
				deploy.setChainBehaviour(deploymentDescriptorData.deployBehaviour);
			}
			
			if (deploymentDescriptorData.downgrade != null) {
				downgrade = o.createXMLHintsTypeXMLDowngrade();
				downgrade.setMethod(deploymentDescriptorData.downgrade);
			}
			
			if (deploymentDescriptorData.upgrade != null) {
				upgrade = o.createXMLHintsTypeXMLUpgrade();
				upgrade.setMethod(deploymentDescriptorData.upgrade);
			}
			
			if (upgrade != null || downgrade != null || undeploy != null || deploy != null) {
				if (!StringUtils.isBlank(deploymentDescriptorData.deployBehaviourInt) ||  
					!StringUtils.isBlank(deploymentDescriptorData.undeployBehaviourInt) ||
						   !StringUtils.isBlank(deploymentDescriptorData.downgradeInt)||
						   !StringUtils.isBlank(deploymentDescriptorData.upgradeInt)) {
					   List<XMLComponentType> components = dd.getComponents().getComponent();
					   int i=0;
					   for (XMLComponentType c : components) {
						    c.setHints(o.createXMLHintsType());
							if (deploymentDescriptorData.downgradeInt != null && deploymentDescriptorData.downgradeInt.contains(""+i)) {
								 c.getHints().setDowngrade(downgrade);
								 log.debug("Adding downgrade hint '"+downgrade.getMethod()+"'");
							}
							if (deploymentDescriptorData.undeployBehaviourInt != null &&  deploymentDescriptorData.undeployBehaviourInt.contains(""+i)) {
								c.getHints().setUndeploy(undeploy);
								log.debug("Adding undeploy behaviour hint '"+undeploy.getChainBehaviour()+"'");
							}
							if (deploymentDescriptorData.deployBehaviourInt != null && deploymentDescriptorData.deployBehaviourInt.contains(""+i)) {
								c.getHints().setDeploy(deploy);
								log.debug("Adding deploy behaviour hint '"+deploy.getChainBehaviour()+"'");
							}
							if (deploymentDescriptorData.upgradeInt != null && deploymentDescriptorData.upgradeInt.contains(""+i)) {
								c.getHints().setUpgrade(upgrade);
								log.debug("Adding downgrade hint '"+upgrade.getMethod()+"'");
							}  
						    i++;
					   }
				}
			   
			}
		}
		log.debug("Writing deployment descriptor to '"+ddOutput+"'");
		new DDConfigurationWriter().writeTo(dd, new FileWriter(ddOutput));
		
		return appVersion;
	}


	
	
	/**
	 * Creates a hiera file off
	 * @param yamlData
	 * @param environment
	 * @param role
	 * @throws Exception
	 */
	public void createHiera(String outputPath, String environment, YD[] yamlData) throws Exception {
		if (yamlData == null) {
			return;
		}
		List<String> onesWeHaveAlreadyCleared = new ArrayList<String>();
		for (int i = 0; i < yamlData.length; i++) {
			MachineState state = envStateManager.getEnvironmentState(environment, yamlData[i].role);
			log.debug("Writing ROLE:"+ yamlData[i].role + " COMPONENT" + yamlData[i].componentName+ " VERSION" + yamlData[i].version);
			YamlInjector inj = new YamlInjector();
			try {
			    inj.inject(state.getState(), HieraData.getEnsurePath(yamlData[i].componentName), yamlData[i].version,NodeMissingBehaviour.FAIL, APP, ZONE);
			
				File outFile = new File(outputPath);
				if (outFile.exists() && !onesWeHaveAlreadyCleared.contains(outFile.getAbsolutePath())) {
					/* Delete the folder the first time through during this data setup, we don't want old data lying around */
					//FileUtils.deleteDirectory(outFile);
					//onesWeHaveAlreadyCleared.add(outFile.getAbsolutePath());
				}
				outFile.mkdirs();

				File file2 = new File(outputPath, state.getSourceName());
				log.debug("Writing out to '"+file2+"'");
				FileWriter writer = new FileWriter(file2);
				YamlUtil.write(state.getState(), writer);
				writer.close();
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
		
	}
	
}
