package com.ipt.ebsa.environment.data.factory;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;

import com.ipt.ebsa.agnostic.cloud.config.v1.XMLGeographicContainerType;
import com.ipt.ebsa.environment.v1.build.XMLBuildsType;
import com.ipt.ebsa.skyscape.command.v2.CmdExecute;
import com.ipt.ebsa.skyscape.config.v2.XMLOrganisationType;
import com.sun.xml.bind.marshaller.NamespacePrefixMapper;

public class XMLHelper extends XMLBase {

	private static final Logger LOG = Logger.getLogger(XMLHelper.class);
	
	private static final String ACLOUD_CMD_NAMESPACE = "http://ebsa.ipt.com/aCloudCommand-1.1";
	private static final String ACLOUD_CFG_NAMESPACE = "http://ebsa.ipt.com/AgnosticCloudConfig-1.0";
	private static final String VCLOUD_CMD_NAMESPACE = "http://ebsa.ipt.com/VCloudCommand-2.0";
	private static final String VCLOUD_CFG_NAMESPACE = "http://ebsa.ipt.com/VCloudConfig-2.0";
	
	private static final Map<String, String> NAMESPACES = new HashMap<String, String>();
	static {
		NAMESPACES.put(VCLOUD_CMD_NAMESPACE, "vc");
		NAMESPACES.put(ACLOUD_CMD_NAMESPACE, "ac");
		NAMESPACES.put(VCLOUD_CFG_NAMESPACE, "vc");
		NAMESPACES.put(ACLOUD_CFG_NAMESPACE, "ac");
	}

	/**
	 * Maps a namespaceUri to a namespace prefix for consistent marshalled XML output 
	 */
	private static final NamespacePrefixMapper NAMESPACE_PREFIX_MAPPER = new NamespacePrefixMapper() {
		@Override
		public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
			System.err.println("namespaceUri="+namespaceUri);
			System.err.println("suggestion="+suggestion);
			System.err.println("requirePrefix="+requirePrefix);
			String prefix =	NAMESPACES.get(namespaceUri);
			return prefix == null ? suggestion : prefix;
		}
	};
	
	public String marshallInstructionXML(CmdExecute action) {
		return marshallXML("{" + VCLOUD_CMD_NAMESPACE + "}execute", action);
	}
	
	public String marshallInstructionXML(com.ipt.ebsa.agnostic.cloud.command.v1.CmdExecute action) {
		return marshallXML("{" + ACLOUD_CMD_NAMESPACE + "}execute", action);
	}
	
	public String marshallConfigurationXML(XMLOrganisationType config) {
		return marshallXML("{" + VCLOUD_CFG_NAMESPACE + "}Organisation", config);
	}
	
	public String marshallConfigurationXML(XMLGeographicContainerType config) {
		return marshallXML("{" + ACLOUD_CFG_NAMESPACE + "}GeographicContainer", config);
	}
	
	public CmdExecute unmarshallVCloudInstructionXML(String xml) {
		try {
			JAXBContext context = JAXBContext.newInstance(CmdExecute.class);
			StreamSource streamSource = new StreamSource(new StringReader(xml));
			Unmarshaller unmarshaller = getUnmarshaller("/vCloudCommand-2.0.xsd", context);
			LOG.debug("Unmarshalling...");
			return (CmdExecute) unmarshaller.unmarshal(streamSource, CmdExecute.class).getValue();
		} catch (JAXBException e) {
			throw new RuntimeException("Failed to convert XML to JAXB object", e);
		}
	}
	
	public XMLOrganisationType unmarshallVCloudConfigXML(String xml) {
		try {
			JAXBContext context = JAXBContext.newInstance(XMLOrganisationType.class);
			StreamSource streamSource = new StreamSource(new StringReader(xml));
			Unmarshaller unmarshaller = getUnmarshaller("/vCloudConfig-2.0.xsd", context);
			LOG.debug("Unmarshalling...");
			return (XMLOrganisationType) unmarshaller.unmarshal(streamSource, XMLOrganisationType.class).getValue();
		} catch (JAXBException e) {
			throw new RuntimeException("Failed to convert XML to JAXB object", e);
		}
	}
	
	public com.ipt.ebsa.agnostic.cloud.command.v1.CmdExecute unmarshallACloudInstructionXML(String xml) {
		try {
			JAXBContext context = JAXBContext.newInstance(com.ipt.ebsa.agnostic.cloud.command.v1.CmdExecute.class);
			StreamSource streamSource = new StreamSource(new StringReader(xml));
			Unmarshaller unmarshaller = getUnmarshaller("/aCloudCommand-1.1.xsd", context);
			return (com.ipt.ebsa.agnostic.cloud.command.v1.CmdExecute) unmarshaller.unmarshal(streamSource, com.ipt.ebsa.agnostic.cloud.command.v1.CmdExecute.class).getValue();
		} catch (JAXBException e) {
			throw new RuntimeException("Failed to convert XML to JAXB object", e);
		}
	}
	
	public String marshallBuildPlanXML(XMLBuildsType builds) {
		return marshallXML("{http://ebsa.ipt.com/EnvironmentBuildSequence-1.0}builds", builds);
	}

	public <T> String marshallXML(String qname, T xmlObject) {
		try {
			StringWriter sw = new StringWriter();
			@SuppressWarnings("unchecked")
			Class<T> clazz = (Class<T>) xmlObject.getClass();
			JAXBContext context = JAXBContext.newInstance(clazz);
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
			marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", NAMESPACE_PREFIX_MAPPER);
			JAXBElement<T> element = new JAXBElement<T>(QName.valueOf(qname), clazz, xmlObject);
			marshaller.marshal(element, sw);
			String output = sw.toString();
			output = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + output;
			LOG.debug("Marshalling...");
			return output;
		} catch (JAXBException e) {
			throw new RuntimeException("Failed to convert JAXB object to XML", e);
		}
	}
}
