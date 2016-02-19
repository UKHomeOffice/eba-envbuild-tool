package com.ipt.ebsa.sdkclient.vcloudconfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.io.IOUtils;
import org.xml.sax.SAXException;

import com.ipt.ebsa.skyscape.command.v2.CmdExecute;
import com.ipt.ebsa.skyscape.config.v2.XMLOrganisationType;

/**
 * This class loads VCloud specifications into memory
 * @author Stephen Cowx
 *
 */
public class VCloudConfigurationLoader {
		
	/**
	 * Load the job definition from a file
	 * @param file
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 */
	public CmdExecute loadJob(File file) throws SAXException, IOException {

		try {
			if (!file.exists()) {
				throw new FileNotFoundException("Unable to load file '" + file.getAbsolutePath() + "' because it does not exist cannot be read.");
			}
			
			
			Schema schema = getNamedSchema( "/vCloudCommand-2.0.xsd");
			JAXBContext context = JAXBContext.newInstance(CmdExecute.class);
			javax.xml.transform.stream.StreamSource streamSource = new javax.xml.transform.stream.StreamSource(file);
			Unmarshaller unmarshaller = context.createUnmarshaller();
			unmarshaller.setSchema(schema);
			unmarshaller.setEventHandler(new ValidationEventHandler(){
				public boolean handleEvent(ValidationEvent event) {
					return false;
				}
			});
			CmdExecute job = (CmdExecute) unmarshaller.unmarshal(streamSource, CmdExecute.class).getValue();
			return job;
		} catch (JAXBException e) {
			throw new RuntimeException("Unable to load XML file", e);
		}
	}

	/**
	 * Load the configuration from a file
	 * @param file
	 * @return
	 * @throws IOException 
	 * @throws SAXException 
	 */
	public XMLOrganisationType loadVC(File file) throws SAXException, IOException {

		try {
			if (!file.exists()) {
				throw new FileNotFoundException("Unable to load file '" + file.getAbsolutePath() + "' because it does not exist cannot be read.");
			}
			Schema schema = getNamedSchema( "/vCloudConfig-2.0.xsd");
			JAXBContext context = JAXBContext.newInstance(XMLOrganisationType.class);
			javax.xml.transform.stream.StreamSource streamSource = new javax.xml.transform.stream.StreamSource(file);
			Unmarshaller unmarshaller = context.createUnmarshaller();
			unmarshaller.setSchema(schema);
			XMLOrganisationType config = (XMLOrganisationType) unmarshaller.unmarshal(streamSource, XMLOrganisationType.class).getValue();
			return config;
		} catch (JAXBException e) {
			throw new RuntimeException("Unable to load XML file", e);
		}
	}
		 

	private Schema getNamedSchema(String schemaFileName) throws SAXException, IOException {
		File schemaFile = new File(System.getProperty("java.io.tmpdir"), schemaFileName);
		FileOutputStream s = new FileOutputStream(schemaFile);
		InputStream is = this.getClass().getResourceAsStream(schemaFileName);
		IOUtils.copy(is,s);
		SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = sf.newSchema(schemaFile);
		return schema;
	}
}
