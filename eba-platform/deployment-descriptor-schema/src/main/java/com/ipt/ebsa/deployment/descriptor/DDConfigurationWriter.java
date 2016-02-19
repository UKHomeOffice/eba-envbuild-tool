package com.ipt.ebsa.deployment.descriptor;

import java.io.IOException;
import java.io.Writer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import org.xml.sax.SAXException;

public class DDConfigurationWriter {

	/**
	 * Writer to the writer passed in
	 * @param file
	 * @return
	 * @throws IOException 
	 * @throws SAXException 
	 */
	public void writeTo(XMLDeploymentDescriptorType descriptor, Writer writer) throws SAXException, IOException {

		try {
			if (writer == null) {
				throw new IllegalArgumentException("Writer cannot be null.");
			}
			if (descriptor == null) {
				throw new IllegalArgumentException("Descriptor cannot be null.");
			}
			
			JAXBContext context = JAXBContext.newInstance(XMLDeploymentDescriptorType.class);
			Marshaller marshaller = context.createMarshaller();
			try {
				marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
				QName qname = new QName("http://ebsa.ipt.com/ddConfig-1.0", "deploymentdescriptor");
				JAXBElement<XMLDeploymentDescriptorType> jaxbElement = new JAXBElement<XMLDeploymentDescriptorType>(qname, XMLDeploymentDescriptorType.class, descriptor);
				marshaller.marshal(jaxbElement, writer);
			} finally {
			    writer.close();
			}
			
		} catch (JAXBException e) {
			throw new RuntimeException("Unable to marshal XML to writer ", e);
		}
	}
}
