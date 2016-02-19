package com.ipt.ebsa.environment.data.factory;

import org.junit.Assert;
import org.junit.Test;

import com.ipt.ebsa.skyscape.config.v2.XMLOrganisationType;

public class XMLHelperTest {
	
	
	private String genVcloudXmlString() {
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
				"<vc:Organisation xmlns:vc=\"http://ebsa.ipt.com/VCloudConfig-2.0\">\n" + 
				"    <vc:Environment>\n" + 
				"        <vc:Name>aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa</vc:Name>\n" + 
				"        <vc:VirtualApplication>\n" + 
				"            <vc:Name>aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa</vc:Name>\n" + 
				"            <vc:Description>aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa</vc:Description>\n" + 
				"            <vc:ServiceLevel>aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa</vc:ServiceLevel>\n" + 
				"            <vc:PowerOn>true</vc:PowerOn>\n" + 
				"            <vc:Deploy>true</vc:Deploy>\n" + 
				"        </vc:VirtualApplication>\n" + 
				"        <vc:VirtualApplication>\n" + 
				"            <vc:Name>aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa</vc:Name>\n" + 
				"            <vc:Description>aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa</vc:Description>\n" + 
				"            <vc:ServiceLevel>aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa</vc:ServiceLevel>\n" + 
				"            <vc:PowerOn>true</vc:PowerOn>\n" + 
				"            <vc:Deploy>true</vc:Deploy>\n" + 
				"        </vc:VirtualApplication>\n" + 
				"    </vc:Environment>\n" + 
				"</vc:Organisation>");
		
		return sb.toString();
	}

	@Test
	public void testNamespaceVcloud() {
		XMLHelper xmlHelper = new XMLHelper();
		XMLOrganisationType config = xmlHelper.unmarshallVCloudConfigXML(genVcloudXmlString());
		String output = xmlHelper.marshallConfigurationXML(config);
		Assert.assertEquals(genVcloudXmlString(), output);
	}

}
