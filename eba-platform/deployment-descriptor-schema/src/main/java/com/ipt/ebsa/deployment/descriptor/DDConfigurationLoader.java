package com.ipt.ebsa.deployment.descriptor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

import com.ipt.ebsa.deployment.descriptor.release.XMLReleaseDeploymentDescriptorType;


/**
 * This class loads DeploymentDescriptors into memory
 * @author Daniel Pettifor
 *
 */
public class DDConfigurationLoader {

	/**
	 * Load the configuration from a file, application short name provided for context.
	 * @param file
	 * @param applicationShortName
	 * @return
	 * @throws IOException 
	 * @throws SAXException 
	 */
	public DeploymentDescriptor loadDD(File file, String applicationShortName) throws SAXException, IOException {
		XMLDeploymentDescriptorType xmlDD = loadDeploymentDescriptor(file, "/ddConfig-1.0.xsd", XMLDeploymentDescriptorType.class);
		return new DeploymentDescriptor(xmlDD, applicationShortName);
	}
	
	/**
	 * Load the IPT Release Deployment Descriptor from a file
	 * @param file
	 * @return
	 * @throws IOException 
	 * @throws SAXException 
	 */
	public XMLReleaseDeploymentDescriptorType loadReleaseDD(File file) throws SAXException, IOException {
		return loadDeploymentDescriptor(file, "/releaseDDconfig-1.0.xsd", XMLReleaseDeploymentDescriptorType.class);
	}
	
	/**
	 * Load the deployment descriptor
	 * @param file
	 * @param schemaPath
	 * @param descriptorType
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 */
	private <T> T loadDeploymentDescriptor(File file, String schemaPath, Class<T> descriptorType) throws SAXException, IOException  {
		try {
			if (file == null) {
				throw new IllegalArgumentException("File parameter cannot be null.");
			}
			if (!file.exists()) {
				throw new FileNotFoundException(String.format("Unable to load file '%s' because it does not exist.", file.getAbsolutePath()));
			}
			
			Schema schema = getNamedSchema(schemaPath);
			JAXBContext context = JAXBContext.newInstance(descriptorType);
			StreamSource streamSource = new StreamSource(file);
			Unmarshaller unmarshaller = context.createUnmarshaller();
			unmarshaller.setSchema(schema);
			unmarshaller.setEventHandler(new ValidationEventHandler(){
				public boolean handleEvent(ValidationEvent event) {
					return false;
				}
			});
			return unmarshaller.unmarshal(streamSource, descriptorType).getValue();
		} catch (JAXBException e) {
			throw new RuntimeException("Unable to load XML file", e);
		}
	}
	
	/**
	 * Load the schema with the given filename
	 * @param schemaFileName
	 * @return
	 * @throws SAXException
	 */
	private Schema getNamedSchema(String schemaFileName) throws SAXException, IOException {
		URL schemaURL = getClass().getResource(schemaFileName);
		SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = sf.newSchema(schemaURL);
		return schema;
	}
}
