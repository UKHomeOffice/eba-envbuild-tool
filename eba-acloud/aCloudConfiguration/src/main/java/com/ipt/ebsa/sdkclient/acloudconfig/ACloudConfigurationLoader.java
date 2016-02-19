package com.ipt.ebsa.sdkclient.acloudconfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.io.IOUtils;
import org.xml.sax.SAXException;

import com.ipt.ebsa.agnostic.cloud.command.v1.CmdExecute;
import com.ipt.ebsa.agnostic.cloud.command.v1.Execute;
import com.ipt.ebsa.agnostic.cloud.config.v1.XMLGeographicContainerType;
import com.sun.xml.bind.marshaller.NamespacePrefixMapper;

/**
 * This class loads VCloud specifications into memory
 *
 */
public class ACloudConfigurationLoader {
		
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
			
			
			Schema schema = getNamedSchema( "/aCloudCommand-1.1.xsd");
			JAXBContext context = JAXBContext.newInstance(CmdExecute.class);
			javax.xml.transform.stream.StreamSource streamSource = new javax.xml.transform.stream.StreamSource(file);
			Unmarshaller unmarshaller = context.createUnmarshaller();
			unmarshaller.setSchema(schema);
			unmarshaller.setEventHandler(new ValidationEventHandler(){
				public boolean handleEvent(ValidationEvent event) {
					return false;
				}
			});
			com.ipt.ebsa.agnostic.cloud.command.v1.CmdExecute job = (CmdExecute) unmarshaller.unmarshal(streamSource, CmdExecute.class).getValue();
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
	public XMLGeographicContainerType loadVC(File file) throws SAXException, IOException {

		try {
			if (!file.exists()) {
				throw new FileNotFoundException("Unable to load file '" + file.getAbsolutePath() + "' because it does not exist cannot be read.");
			}
			Schema schema = getNamedSchema( "/AgnosticCloudConfig-1.0.xsd");
			JAXBContext context = JAXBContext.newInstance(XMLGeographicContainerType.class);
			javax.xml.transform.stream.StreamSource streamSource = new javax.xml.transform.stream.StreamSource(file);
			Unmarshaller unmarshaller = context.createUnmarshaller();
			unmarshaller.setSchema(schema);
			XMLGeographicContainerType config = (XMLGeographicContainerType) unmarshaller.unmarshal(streamSource, XMLGeographicContainerType.class).getValue();
			return config;
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
	public File write(XMLGeographicContainerType geographic , String filename) throws SAXException, IOException {
		com.ipt.ebsa.agnostic.cloud.config.v1.ObjectFactory obj = new com.ipt.ebsa.agnostic.cloud.config.v1.ObjectFactory();
		File returnFile = null;
		try {
			returnFile = File.createTempFile(filename, "xml");
			
			JAXBElement<XMLGeographicContainerType> geographicRootElement = obj.createGeographicContainer(geographic);
			Schema schema = getNamedSchema( "/AgnosticCloudConfig-1.0.xsd");
			JAXBContext context = JAXBContext.newInstance(XMLGeographicContainerType.class);
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new NamespacePrefixMapper() {
	             @Override
	            public String getPreferredPrefix(String arg0, String arg1, boolean arg2) {
	                return "ac";
	            }
	        });
			marshaller.setSchema(schema);
			marshaller.marshal(geographicRootElement, returnFile);
			
			return returnFile;
		} catch (JAXBException e) {
			e.printStackTrace();
			throw new RuntimeException("Unable to write XML file", e);
		}
	}
	
	/**
	 * Load the configuration from a file
	 * @param file
	 * @return
	 * @throws IOException 
	 * @throws SAXException 
	 */
	public File write(CmdExecute instruction , String filename) throws SAXException, IOException {
		com.ipt.ebsa.agnostic.cloud.command.v1.ObjectFactory obj = new com.ipt.ebsa.agnostic.cloud.command.v1.ObjectFactory();
		File returnFile = null;
		try {
			returnFile = File.createTempFile(filename, "xml");
			Schema schema = getNamedSchema( "/aCloudCommand-1.1.xsd");
			JAXBContext context = JAXBContext.newInstance(Execute.class);
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new NamespacePrefixMapper() {
	             @Override
	            public String getPreferredPrefix(String arg0, String arg1, boolean arg2) {
	                return "ac";
	            }
	        });
			marshaller.setSchema(schema);
			marshaller.marshal(instruction, returnFile);
			
			return returnFile;
		} catch (JAXBException e) {
			throw new RuntimeException("Unable to write XML file", e);
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
