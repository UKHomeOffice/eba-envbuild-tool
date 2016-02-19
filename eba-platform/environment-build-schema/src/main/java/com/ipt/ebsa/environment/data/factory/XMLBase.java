package com.ipt.ebsa.environment.data.factory;

import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

public class XMLBase {

	private static final Logger LOG = Logger.getLogger(XMLBase.class);
	
	protected Schema getSchema(String schemaFileName) {
		try {
			URL schemaURL = getClass().getResource(schemaFileName);
			SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			Schema schema = sf.newSchema(schemaURL);
			return schema;
		} catch (SAXException e) {
			throw new IllegalStateException("Unable to load schema from jar: [" + schemaFileName + "]");
		}
	}

	public Unmarshaller getUnmarshaller(String schemaFileName, JAXBContext context) throws JAXBException {
		Schema schema = getSchema(schemaFileName);
		Unmarshaller unmarshaller = context.createUnmarshaller();
		unmarshaller.setSchema(schema);
		unmarshaller.setEventHandler(new ValidationEventHandler(){
			public boolean handleEvent(ValidationEvent event) {
				return handleError(event);
			}
		});
		return unmarshaller;
	}

	/**
	 * Handle errors from unmarshalling the xml
	 */
	protected boolean handleError(ValidationEvent event) {
		if (event.getSeverity() == ValidationEvent.ERROR || event.getSeverity() == ValidationEvent.FATAL_ERROR) {
			LOG.error(event.getMessage() + ", at location: l" + event.getLocator().getLineNumber() + ", offset: " + event.getLocator().getOffset());
			return false;
		} else {
			// TODO assuming we can recover from a warn? Need to validate this assumption
			LOG.warn(event.getMessage() + ", at location: l" + event.getLocator().getLineNumber() + ", offset: " + event.getLocator().getOffset());
			return true;
		}
	}

}
