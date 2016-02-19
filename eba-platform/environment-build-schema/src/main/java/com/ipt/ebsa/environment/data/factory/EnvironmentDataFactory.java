package com.ipt.ebsa.environment.data.factory;

import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;

import com.ipt.ebsa.environment.data.model.EnvironmentData;
import com.ipt.ebsa.environment.data.model.EnvironmentDataImpl;
import com.ipt.ebsa.environment.v1.build.XMLActionType;
import com.ipt.ebsa.environment.v1.build.XMLBuildType;
import com.ipt.ebsa.environment.v1.build.XMLBuildsType;
import com.ipt.ebsa.environment.v1.build.XMLEnvironmentContainerType;
import com.ipt.ebsa.environment.v1.build.XMLEnvironmentType;
import com.ipt.ebsa.environment.v1.build.XMLParamType;
import com.ipt.ebsa.environment.v1.build.XMLSequenceType;


/**
 * Builds up the store of environment sequences and action.
 */
public class EnvironmentDataFactory extends XMLBase {
	
	private static final Logger LOG = Logger.getLogger(EnvironmentDataFactory.class);

	/**
	 * Builds and {@link EnvironmentDataImpl} from xml files in the provided directory.
	 * All other files are ignored.
	 * 
	 * @param directory containing xml files.
	 * @return the fully populate {@link EnvironmentDataImpl}.
	 */
	public EnvironmentData getEnvironmentDataInstance(File directory) {
		EnvironmentDataImpl environmentData = new EnvironmentDataImpl();
		if (directory == null) {
			throw new IllegalArgumentException("File parameter cannot be null.");
		}
		if (!directory.exists()) {
			throw new IllegalStateException("Unable to load file '" + directory.getAbsolutePath() + "' as it does not exist.");
		}

		LOG.info("Loading from directory [" + directory.getAbsolutePath() + "]");
		
		recurseDirectories(directory, environmentData);
		return environmentData;
	}
	
	private void recurseDirectories(File file, EnvironmentDataImpl data) {
		for (File fileOrSubdirectory : file.listFiles()) {
			if (fileOrSubdirectory.isDirectory()) {
				recurseDirectories(fileOrSubdirectory, data);
			}
			readFile(data, fileOrSubdirectory);
		}
	}
	
	private void readFile(EnvironmentDataImpl environmentData, File fileOrSubdirectory) {
		if (!fileOrSubdirectory.getAbsolutePath().endsWith("xml")) {
			return;
		}
		LOG.info("Loading from File [" + fileOrSubdirectory.getAbsolutePath() + "]");
		
		try {
			JAXBContext context = JAXBContext.newInstance(XMLBuildsType.class);
			StreamSource streamSource = new StreamSource(fileOrSubdirectory);
			Unmarshaller unmarshaller = getUnmarshaller("/EnvironmentBuildSchema-1.0.xsd", context);
		
			XMLBuildsType root = (XMLBuildsType) unmarshaller.unmarshal(streamSource, XMLBuildsType.class).getValue();
			
			readContentIntoStructure(environmentData, root);
		} catch (JAXBException e) {
			throw new IllegalStateException("Unable to unmarshall file + [" + fileOrSubdirectory.getAbsolutePath() + "]", e);
		}
	}

	private void readContentIntoStructure(EnvironmentDataImpl environmentData, XMLBuildsType root) {
		// Read the builds
		for (XMLBuildType build : root.getBuild()) {
			environmentData.addBuild(build);
		}
		
		// Read the sequences
		for (XMLSequenceType sequence : root.getSequence()) {
			environmentData.addSequence(sequence);
		}
		
		// And global config
		if (root.getGlobalparams() != null) {
			for (XMLParamType xmlParamType : root.getGlobalparams().getParam()) {
				environmentData.addGlobalConfig(xmlParamType);
			}
		}
		
		for (XMLEnvironmentType xmlEnvironmentType : root.getEnvironment()) {
			environmentData.addEnvironment(xmlEnvironmentType);
		}
		
		for (XMLEnvironmentContainerType xmlEnvironmentType : root.getEnvironmentcontainer()) {
			environmentData.addEnvironmentContainer(xmlEnvironmentType);
		}
		
		for (XMLActionType xmlActionType : root.getAction()) {
			environmentData.addAction(xmlActionType);
		}
	}
}
