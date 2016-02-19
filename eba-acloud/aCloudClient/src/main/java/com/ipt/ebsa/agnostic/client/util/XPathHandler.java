package com.ipt.ebsa.agnostic.client.util;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import com.ipt.ebsa.agnostic.client.skyscape.exception.EnvironmentOverrideException;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdEnvironmentType.CmdOverrides.CmdOverride;

public class XPathHandler {

	private Logger logger = LogManager.getLogger(XPathHandler.class);
	
	public static final String	DEFN_OVERRIDE_FILENAME = "overriddenDefinition.xml";
	private static final String SEPARATOR = "/";
	private static final String VIRTUAL_HARDWARE_NODE = "VirtualHardware";
	private static final String VMORDER_NODE = "VMOrder";
	private static final String VMSTART_DELAY_NODE = "VMStartDelay";
	private static final String VMSTOP_DELAY_NODE = "VMStopDelay";
	private static final String VMSTART_ACTION_NODE = "VMStartAction";
	private static final String VMSTOP_ACTION_NODE = "VMStopAction";
	
	private static final String PARENT_NODE_NAME_VM = "VirtualMachine";
	
	private String[] supportedNodes = new String[]{ VMORDER_NODE, VMSTART_DELAY_NODE, VMSTOP_DELAY_NODE, VMSTART_ACTION_NODE,VMSTOP_ACTION_NODE};
	
			
	/**
	 *  - Method to apply XPath override values to the Controller's injected environment file.
	 *  
	 * @param overrides
	 * @throws EnvironmentOverrideException 
	 * @throws IOException 
	 * @throws ParserConfigurationException 
	 * @throws SAXException 
	 * @throws XPathExpressionException 
	 * @throws TransformerException 
	 */
	public File applyEnvOverrides(String definition, List<CmdOverride> overrides, File processedDefinitionFile) throws EnvironmentOverrideException {
		logger.debug("IN >> applyEnvOverrides");
		
		logger.debug("Creating Document object and applying XPath values in file '"+definition+"'");
		
		try{
		
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(definition);
			
			for(CmdOverride override : overrides)
			{
				logger.debug(String.format("Attempting the apply value %s to XPath substitution %s", override.getValue(), override.getXpath()));
				replaceXpathValue(doc, override);
			}
			
			logger.debug(String.format("Outputting the Document as an xml file :: %s", DEFN_OVERRIDE_FILENAME));
			
			// Create the temp file we will be writing out to
			processedDefinitionFile = new File(DEFN_OVERRIDE_FILENAME);
	
			// Create the file using a Transformer
		    Transformer transformer =  TransformerFactory.newInstance().newTransformer();	
		    DOMSource source = new DOMSource(doc);
		    StreamResult result = new StreamResult(processedDefinitionFile);
		    transformer.transform(source, result);
	    
		} catch(IOException ioex) {
			throw new EnvironmentOverrideException("Error reading or writing to a file during Overrides processing.", ioex);
		} catch (ParserConfigurationException pce) {
			throw new EnvironmentOverrideException("Error parsing a file during Overrides processing.", pce);
		} catch (SAXException saxe) {
			throw new EnvironmentOverrideException("Error parsing a file during Overrides processing.", saxe);
		} catch (XPathExpressionException xpe) {
			throw new EnvironmentOverrideException("Error applying an XPath during Overrides processing.", xpe);
		} catch (TransformerConfigurationException tce) {
			throw new EnvironmentOverrideException("Error transforming an XML file during Overrides processing.", tce);
		} catch (TransformerFactoryConfigurationError tfce) {
			throw new EnvironmentOverrideException("Error in transformer donfiguration when transforming an XML file during Overrides processing.", tfce);
		} catch (TransformerException te) {
			throw new EnvironmentOverrideException("Error transforming an XML file during Overrides processing.", te);
		} catch(Exception ex) {
			throw new EnvironmentOverrideException("Exception thrown during Overrides processing.", ex);
		}	    

	    	logger.debug(String.format("processedDefinitionFile is :: %s", (processedDefinitionFile != null && processedDefinitionFile.getAbsolutePath() != null) ? processedDefinitionFile.getAbsolutePath() : "not created" ));
	    	
	    logger.debug("applyEnvOverrides >> OUT");
	    
	    return processedDefinitionFile;
	}
	
	/**
	 *  - Method to apply XPath override value to an individual Node in the Document.
	 * 
	 * @param doc
	 * @param override
	 * @throws XPathExpressionException
	 * @throws EnvironmentOverrideException 
	 */
	private void replaceXpathValue(Document doc, CmdOverride override) throws XPathExpressionException, EnvironmentOverrideException
	{
		logger.debug("IN > replaceXpathValue");
		
		// The XPath and value we will be working with
		String xPath = override.getXpath();
		String value = override.getValue();
		
		logger.debug(String.format("xPath is :: %s , value is :: %s", xPath, value));
		
		// Compile the XPath expression and get the Node in question
		XPath xpath = XPathFactory.newInstance().newXPath();
		XPathExpression expr = xpath.compile(xPath);
		NodeList nodes=(NodeList)expr.evaluate(doc, XPathConstants.NODESET);
		
		// We'll just set the Node value directly - this will still be referenced in the parent DOM structure
		if(nodes != null && !(nodes.getLength() > 1)) {
			Node node = nodes.item(0);
			// the firstChild is the actual value node
			if(node != null && node.getFirstChild() != null){
				logger.debug(String.format("Initial Node value is :: %s", node.getFirstChild().getNodeValue()));
				node.getFirstChild().setNodeValue(value);
				logger.debug(String.format("Node value has been set to :: %s", node.getFirstChild().getNodeValue()));
			}
			else
			{
				logger.debug("The Element defined by the XPath does not already exist in the Document. Attempting to add the new Element.");
				addNodeToEnvDefinition(doc, xPath, value);
			}
		}		
		
		logger.debug("replaceXpathValue >> OUT");		
	}
	
	/**
	 * Method to add a missing Node and value into the Environment Definition file, as specified in the Xpath.
	 * Current implementation allows the overriding of the VMOrder, VMStartDelay, VMStopDelay, VMStartAction, VMStopAction values only,
	 * due to xsd compilation issues if *any* Node is attemptedly added at *any* point.
	 * 
	 * NB - the XPath string must end with a forward-slash followed by the new Node name. We cannot do multiple-xpath matching!
	 * 
	 * @param doc
	 * @param xpath - XPath of the Node to be added. Must end with a backslash followed by the new Node name.
	 * @param value
	 * @throws EnvironmentOverrideException 
	 * @throws XPathExpressionException 
	 */
	private void addNodeToEnvDefinition(Document doc, String xpath, String value) throws EnvironmentOverrideException, XPathExpressionException
	{
		logger.debug("IN >> addNodeToEnvDefinition");
		
		// Get the parent node of the Node that is to be inserted - this is dependent n the format being passed in matching the expected format
		// Expected format will be a backslash followed by the new Node name.
		String newNodeParent = StringUtils.substringBeforeLast(xpath, SEPARATOR);
		String newNodeName = StringUtils.substringAfterLast(xpath, SEPARATOR);
		
		// Xpath out the Node of the parent 'VirtualMachine' Node.
		XPath xpathToParent = XPathFactory.newInstance().newXPath();
		XPathExpression expr = xpathToParent.compile(newNodeParent);
		NodeList nodes=(NodeList)expr.evaluate(doc, XPathConstants.NODESET);
		
		// We'll just get the parent Node directly and add the new Node to it - this will still be referenced in the parent Document structure
		if(nodes != null && !(nodes.getLength() > 1)) {
			Node node = nodes.item(0);
			
			int nindex = node.getNodeName().indexOf(":");
			String prefix = nindex > 0 ? node.getNodeName().substring(0,nindex) : null;
			String parentNodeName = node.getNodeName();
			//Remove the prefix if there is one
			if (prefix != null && prefix.length() > 0)
			{
				parentNodeName = parentNodeName.substring(prefix.length()+1);
			}
			boolean isSupportedNode = Arrays.asList(supportedNodes).contains(newNodeName);
					
			// the firstChild is the actual value node AND we are trying to override 
			if(node != null && parentNodeName.equals(PARENT_NODE_NAME_VM) && isSupportedNode ) {
				// We are in the named VirtualMachine Node
				logger.debug(String.format("Adding new child Node %s with value %s to existing Node :: %s", newNodeName, value, node.getNodeName()));
				
				// Create the new Element we are going to add, and add its Text value
				// We need to add the namespace into the actual document, not just to XPath node identifier
				Element newNode = doc.createElement(prefix + ":" + newNodeName);
				Text valueNode = doc.createTextNode(value);
				newNode.appendChild(valueNode);
				
				// Get the subsequent VirtualHardware sibling Node to the proposed new VMxxxx Node				
				XPath xpathToVirtualHardwareSibling = XPathFactory.newInstance().newXPath();
				XPathExpression virtHardwareExpr = xpathToVirtualHardwareSibling.compile(newNodeParent + SEPARATOR + VIRTUAL_HARDWARE_NODE);
				// This is the VirtualHardware Node before which we want to add the new VMxxxx Node.
				NodeList siblingNodeAfter=(NodeList)virtHardwareExpr.evaluate(doc, XPathConstants.NODESET);
					
				// We now add the new VMxxxx Node under the 'VirtualMachine' Node, directly before the subsequent sibling 'VirtualHardware' Node.
				siblingNodeAfter.item(0).getParentNode().insertBefore(newNode, siblingNodeAfter.item(0));
												
				logger.debug(String.format("New Node %s has been successfully set to value :: %s", newNodeName, value));
			}
			else
			{
				throw new EnvironmentOverrideException(String.format("Unable to add new Node :: %s to Parent xpath of :: %s. Only a new VMOrder, VMStartDelay, VMStopDelay, VMStartAction, VMStopAction Node is currently supported to be added through an override in this method.", newNodeName, newNodeParent));
			}
		}
		else
		{
			throw new EnvironmentOverrideException(String.format("Unable to add new Node :: %s to Parent xpath of :: %s. Only a new VMOrder, VMStartDelay, VMStopDelay, VMStartAction, VMStopAction Node is currently supported to be added through an override in this method.", newNodeName, newNodeParent));
		}
		
		logger.debug("addNodeToEnvDefinition >> OUT");
	}	
	
}
