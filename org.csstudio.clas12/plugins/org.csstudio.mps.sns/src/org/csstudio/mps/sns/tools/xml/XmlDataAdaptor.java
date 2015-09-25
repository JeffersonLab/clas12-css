/*
 * XmlDataAdaptor.java
 *
 * Created on February 14, 2002, 11:04 AM
 */

package org.csstudio.mps.sns.tools.xml;

import java.util.*;
import java.io.*;
import java.net.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

import org.csstudio.mps.sns.tools.data.*;

/**
 * XmlDataAdaptor is a DataAdaptor that specifically supports (reading/writing)
 * (from/to) XML.  While the DataAdaptor provides methods for getting and 
 * setting properties of nodes and getting and setting nodes in a tree of 
 * data, XmlDataAdaptor uses an XML backing store for the data.  In particular, 
 * you can use the methods of DataAdaptor for all data manipulation.  You need to 
 * specifically use XmlDataAdaptor when creating a new document, writing an XML 
 * document to a file or loading an XML document from a file.
 *
 * To create a new, empty XML document simply invoke:
 * <code> document_adaptor = XmlDataAdaptor.newEmptyDocumentAdaptor(); </code>
 *
 * You can populate the document by adding child nodes.  You can only add a single node to the 
 * top document node, but otherwise you can add and nest as many nodes as needed. Each 
 * such node is returned as a DataAdaptor.  For example, to add a node to the top 
 * document node, invoke:
 * <code> childAdaptor = document_adaptor.createChild("nodeName") </code>
 *
 * You can set attributes of nodes with some basic types such as boolean, integer, double
 * and string where you supply the name of the attribute and the value.  If you add an Object
 * as a value, then toString() is invoked to fetch the value as a string.  Some examples:
 * <code> adaptor.setValue("attribute", "some string"); </code>
 * <code> adaptor.setValue("attr2", 3.14); </code>
 *
 * You can write an XML document to a URL, a file or generally to a java.io.Writer.
 * For example, to write an XML document to a file invoke:
 * <code> document_adaptor.writeTo( new File("file_path") ); </code>
 *
 * You can read an XML document from a string of XML text, a URL or a file.  You may 
 * specify whether or not to use DTD validation.
 * For example, to read an XML document from a file invoke:
 * <code> document_adaptor = XmlDataAdaptor.adaptorForFile( new File("file_path"), false ); </code>
 *
 * You can fetch named child nodes from a parent node.  Some examples are:
 * <code> List xAdaptors = parentAdaptor.childAdaptors("X") </code>
 * <code> List allAdaptors = parentAdaptor.childAdaptors() </code>
 * <code> DataAdaptor yAdaptor = parentAdaptor.childAdaptor("Y") </code>
 * 
 * You can test if a node defines an attribute:
 * <code> boolean status = adaptor.hasAttribute("attribute"); </code>
 *
 * You can read the value of an attribute:
 * <code> double value = adaptor.doubleValue("attr2"); </code>
 * 
 * There are several more methods available for DataAdaptor and XmlDataAdaptor, but 
 * the examples listed above should provide an overview and foundation.
 *
 * @author  tap
 */
public class XmlDataAdaptor implements DataAdaptor {
    private Document document;  // root document
    private Node mainNode;      // could be a document or an element
    private NodeList nodeList;  // child nodes
    private List<Node> childNodes;    // child nodes as a List

    
    /** Creates a new XmlDataAdaptor from an XML Node */
    public XmlDataAdaptor(Node newNode) {
        mainNode = newNode;
        document = mainNode.getOwnerDocument();
        nodeList = mainNode.getChildNodes();
        createChildren();        
    }
    
    
    /** Creates a new XmlDataAdaptor from an XML Document */
    public XmlDataAdaptor(Document newDocument) {
        document = newDocument;
        mainNode = document;
        nodeList = mainNode.getChildNodes();
        createChildren();
    }
    
    
    /** fetch and store non-null child nodes as a list */
    private void createChildren() {
        int numNodes = rawNodeCount();
        childNodes = new ArrayList<Node>();
        
        for ( int index = 0 ; index < numNodes ; index++ ) {
            Node node = nodeList.item(index);
            //String nodeName = node.getLocalName();
            String nodeName = node.getNodeName();
            if ( nodeName != null && node.getNodeType() == Node.ELEMENT_NODE ) {
                childNodes.add( node );
            }            
        }
    }
        
    
    /** cast the mainNode as an XML Element */
    private Element asElement() {
        return (Element)mainNode;
    }
    
    
    /** cast the mainNode as an XML Document */
    private Document asDocument() {
        return (Document)mainNode;
    }
    
    
    /** get the tag name for the specified XML node */
    static private String nameForNode(Node node) {
        return node.getNodeName();
        //return node.getLocalName();
    }
                
    
    /** get the tag name for the main node */
    public String name() {
        return nameForNode(mainNode);
    }
    
    
    /** get the document associated with this XML adaptor */
    public Document document() {
        return document;
    }
    
    
    /** check whether the main node has the specified attribute */
    public boolean hasAttribute(String attribute) {
        return asElement().hasAttribute(attribute);
    }
    
    
    /** 
	 * Get the string value associated with the specified attribute allowing DOM to recover escaped characters as necessary.
	 * @param attribute The node attribute.
	 * @return the raw string value associated with the attribute or null if the attribute does not exist
	 */
    final protected String rawValue(final String attribute) {
        //return (String)asElement().getAttribute(attribute);
		Attr attributeNode = ((Attr)mainNode.getAttributes().getNamedItem(attribute));
		return (attributeNode != null) ? attributeNode.getValue() : null;
    }
    
    
    /** 
	 * Get the string value associated with the specified attribute.
	 * @param attribute The node attribute.
	 * @return the raw string value associated with the attribute 
	 */
    public String stringValue(final String attribute) {
		return rawValue(attribute);
    }
    
    
    /** return the double value associated with the attribute */
    public double doubleValue(final String attribute) throws NumberFormatException {
        String strValue = rawValue(attribute);
        
        if ( strValue.length() != 0 ) {
            try {
                return Double.parseDouble(strValue);
            }
            catch(java.lang.NumberFormatException excpt) {
                String message;
                message = "Error parsing as double attribute: " + attribute + 
                ", from string: " + strValue + ", for XML node: " + name();
                throw new NumberFormatException(message);
            }
        }
        else {
            return Double.NaN;
        }
    }
    
    
    /** return the long value associated with the attribute */
    public long longValue(final String attribute) throws NumberFormatException {
        String strValue = rawValue(attribute);
        
        if ( strValue.length() != 0 ) {
            try {
                return Long.parseLong(strValue);
            }
            catch(java.lang.NumberFormatException excpt) {
                String message;
                message = "Error parsing as long attribute: " + attribute + 
                ", from string: " + strValue + ", for XML node: " + name();
                throw new NumberFormatException(message);
            }            
        }
        else {
            return 0;
        }
    }
    
    
    /** return the integer value associated with the attribute */
    public int intValue(final String attribute) throws NumberFormatException {
        String strValue = rawValue(attribute);
        
        if ( strValue.length() != 0 ) {
            try {
                return Integer.parseInt(strValue);
            }
            catch(java.lang.NumberFormatException excpt) {
                String message;
                message = "Error parsing as integer attribute: " + attribute + 
                ", from string: " + strValue + ", for XML node: " + name();
                throw new NumberFormatException(message);
            }            
        }
        else {
            return 0;
        }
    }
    
    
    /** return the boolean value associated with the attribute */
    public boolean booleanValue(final String attribute) throws NumberFormatException {
        String strValue = rawValue(attribute);
        
        try {
            return Boolean.valueOf(strValue).booleanValue();
        }
        catch(java.lang.NumberFormatException excpt) {
            String message;
            message = "Error parsing as boolean attribute: " + attribute + 
            ", from string: " + strValue + ", for XML node: " + name();
            throw new NumberFormatException(message);
        }            
    }
	
	
	/**
	 * Set the string value to associate with the attribute and allow DOM to escape special characters as necessary.
	 * @param attribute The node attribute.
	 * @param value The string value to associate with the attribute.
	 */
	final protected void setRawValue(String attribute, String value) {
        //asElement().setAttribute(attribute, value);
		Attr attributeNode = document.createAttribute(attribute);
		attributeNode.setValue(value);
		asElement().setAttributeNode(attributeNode);
	}
    
    
    /**
	 * Set the string value to be associated with the attribute and replace illegal XML attribute characters
	 * (less than and ampersand) with their legal XML attribute substitutions.
	 * @param attribute The node attribute.
	 * @param value The string value to associate with the attribute.
	 */
    public void setValue(final String attribute, final String value) {
		setRawValue(attribute, value);
    }
    
    
    /** set the double value to be associated with the attribute */
    public void setValue(final String attribute, final double doubleValue) {
        String strValue = String.valueOf(doubleValue);
        setRawValue(attribute, strValue);
    }
    
    
    /** set the long value to be associated with the attribute */
    public void setValue(final String attribute, final long longValue) {
        String strValue = String.valueOf(longValue);
        setRawValue(attribute, strValue);
    }
    
    
    /** set the integer value to be associated with the attribute */
    public void setValue(final String attribute, final int intValue) {
        String strValue = String.valueOf(intValue);
        setRawValue(attribute, strValue);
    }
    
    
    /** set the boolean value to be associated with the attribute */
    public void setValue(final String attribute, final boolean boolValue) {
        String strValue = String.valueOf(boolValue);
        setRawValue(attribute, strValue);
    }
    
    
    /** set the value of the specified attribute to the specified value */
    public void setValue(final String attribute, final Object value) {
        String strValue = value.toString();
        setValue(attribute, strValue);
    }
    

    /** return an array of attribute names */
    public String[] attributes() {
        NamedNodeMap attributeMap = mainNode.getAttributes();
        int numAttributes = attributeMap.getLength();
        String[] attributeArray = new String[numAttributes];
        
        for ( int index = 0 ; index < numAttributes ; index++ ) {
            Attr attribute = (Attr)attributeMap.item(index);
            attributeArray[index] = attribute.getName();
        }
        
        return attributeArray;
    }
        
    
    /** return the count of non-null child nodes */
    public int nodeCount() {
        return childNodes.size();
    }
    
    
    /** return the count of all child nodes (including null child nodes) */
    public int rawNodeCount() {
        return nodeList.getLength();
    }
    
    
    /** create a new adaptor for the specified node */
    static private XmlDataAdaptor newAdaptor(Node node) {
        XmlDataAdaptor adaptor = new XmlDataAdaptor(node);
        return adaptor;
    }
    
    
    /** 
     * Create a list of child adaptors (one adaptor for each non-null child node).
	 * @return a list of child adaptors
     */
    public List<DataAdaptor> childAdaptors() {
        List<DataAdaptor> childAdaptors = new ArrayList<DataAdaptor>();
        
		for ( Node node : childNodes ) {
            XmlDataAdaptor adaptor = newAdaptor( node );
            childAdaptors.add( adaptor );
		}
        
        return childAdaptors;
    }
    

    /** 
     * Create an iterator for a list of child adaptors (one adaptor for each non-null child node).
	 * @return an iterator of child adaptors
     */
    public Iterator<DataAdaptor> childAdaptorIterator() {
        return childAdaptors().iterator();
    }
    
    
    /** 
     * Create a list of child adaptors (one adaptor for each non-null child node whose tag name is equal to the specified label).
	 * @param label the label for which to match the node's tag
	 * @return a list of child adaptors
     */
    public List<DataAdaptor> childAdaptors( final String label ) {
        List<DataAdaptor> childAdaptors = new ArrayList<DataAdaptor>();
		
		for ( Node node : childNodes ) {
            if ( nameForNode( node ).equals( label ) ) {
                childAdaptors.add( newAdaptor( node ) );
            }
		}

        return childAdaptors;
    }
    
    
    /** 
     * Create an iterator for a list of child adaptors (one adaptor for each non-null child node whose tag equals the specified label).
	 * @param label the label which identifies the tag of the nodes to fetch
	 * @return an iterator of data adaptors corresponding to the specified child nodes
     */
    public Iterator<DataAdaptor> childAdaptorIterator( final String label ) {
        return childAdaptors( label ).iterator();
    }

    
    /*
     * Convenience method to get a single child adaptor when only one is expected
	 * @param label the label which identifies the tag of the nodes to fetch
	 * @return a data adaptor for the specified child node
     */
    public DataAdaptor childAdaptor( final String label ) {
        final List<DataAdaptor> adaptors = childAdaptors( label );
        return adaptors.isEmpty() ? null : adaptors.get( 0 );
    }
    
    
    /** Create a new offspring DataAdaptor given the tagName */
    public DataAdaptor createChild(String tagName) {
        Node node = document.createElement(tagName);
        XmlDataAdaptor childAdaptor = newAdaptor(node);

        /* NOTE: added 11/21/03 CKA */
        this.childNodes.add(childAdaptor.mainNode);
        
        mainNode.appendChild(node);
        
        return childAdaptor;
    }
    
    
    /** append a node associated with the listener */
    public void writeNode(DataListener listener) {
        String tagName = listener.dataLabel();
        
        DataAdaptor adaptor = createChild(tagName);
        listener.write(adaptor);
    }
    
    
    /** append a node for each listener in the listener list */
    public void writeNodes( final Collection<? extends DataListener> nodes ) {
		for ( DataListener node : nodes ) {
			writeNode( node );
		}
    }
    
    
    /** Write XML to the specified url */
    public void writeToUrlSpec(String urlSpec) throws WriteException {
        try {
            XmlWriter.writeToUrlSpec(document, urlSpec);
        }
        catch(Exception excpt) {
            throw new WriteException(excpt);
        }
    }
    
    
    /** Write XML to the specified url */
    public void writeToUrl(java.net.URL url) throws WriteException {
        try {
            XmlWriter.writeToUrl(document, url);
        }
        catch(Exception excpt) {
            throw new WriteException(excpt);
        }
    }
    
    
    /** 
     *  Wrapper for exceptions that may be thrown while writing:
     *    java.io.IOException
     *    java.net.MalformedURLException
     */
    static public class WriteException extends org.csstudio.mps.sns.tools.ExceptionWrapper {
        public WriteException(Exception excpt) {
            super(excpt);
        }
    }
    
    
    /** Write XML to the specified url */
    public void writeTo(java.io.Writer writer) {
        XmlWriter.writeToWriter(document, writer);
    }
    
    
    /** Convenience method for writing an XML file */
    public void writeTo(File file) throws IOException { 
        writeTo( new FileWriter(file) );
    }
    
    
    /**
     * Generate an XmlDataAdaptor from a urlPath and given dtd validating option
     */
    static public XmlDataAdaptor adaptorForUrl(String urlPath, boolean isValidating) 
    throws ParseException, ResourceNotFoundException {
        try {
            DocumentBuilder builder = newDocumentBuilder(isValidating);
            Document document = builder.parse(urlPath);

            return new XmlDataAdaptor(document);
        }
        catch(java.io.FileNotFoundException excpt) {
            throw new ResourceNotFoundException(excpt);
        }
        catch(Exception excpt) {
            throw new ParseException(excpt);
        }
    }
    
    
    /**
     * Generate an XmlDataAdaptor from a urlPath and given dtd validating option
     */
    static public XmlDataAdaptor adaptorForUrl(URL url, boolean isValidating) throws ParseException, ResourceNotFoundException {
        return XmlDataAdaptor.adaptorForUrl(url.toString(), isValidating);
    }
    
    
    /**
     * Generate an XmlDataAdaptor from a urlPath and given dtd validating option
     */
    static public XmlDataAdaptor adaptorForFile(File file, boolean isValidating) throws MalformedURLException, ParseException, ResourceNotFoundException {
        return XmlDataAdaptor.adaptorForUrl(file.toURL(), isValidating);
    }
    
    
    /**
     * Generate an XmlDataAdaptor from an XML string and given dtd validating option
     */
    static public XmlDataAdaptor adaptorForString(String source, boolean isValidating) 
    throws ParseException, ResourceNotFoundException {
        try {
            DocumentBuilder builder = newDocumentBuilder(isValidating);
            Document document = builder.parse(new ByteArrayInputStream( source.getBytes() ));
			
            return new XmlDataAdaptor(document);
        }
        catch(java.io.FileNotFoundException excpt) {
            throw new ResourceNotFoundException(excpt);
        }
        catch(Exception excpt) {
            throw new ParseException(excpt);
        }
    }
    
    
    /** Create a new document builder with the given DTD validation */
    static protected DocumentBuilder newDocumentBuilder(boolean isValidating) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(isValidating);
		
        return factory.newDocumentBuilder();
    }
    
    
    
    /*
     * Exception to wrap any exceptions thrown by adaptorForUrl()
     */
    static public class ParseException extends org.csstudio.mps.sns.tools.ExceptionWrapper {
        public ParseException(Exception excpt) {
            super(excpt);
        }
    }
    
    
    /*
     * Exception when the source of the URL does not exist
     */
    static public class ResourceNotFoundException extends org.csstudio.mps.sns.tools.ExceptionWrapper {
        public ResourceNotFoundException(Exception excpt) {
            super(excpt);
        }
    }
    
    
    
    /** Create a new XmlDataAdaptor given a DataListener and a dtd URI */
    static public XmlDataAdaptor newDocumentAdaptor(DataListener dataHandler, String dtdUri) throws CreationException {
        XmlDataAdaptor adaptor = null;

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            DocumentBuilder builder = factory.newDocumentBuilder();
            DOMImplementation implementation = builder.getDOMImplementation();
            String docTag = dataHandler.dataLabel();
            DocumentType docType = 
                implementation.createDocumentType(docTag, null, dtdUri);
            Document document = builder.newDocument();
            document.appendChild(docType);

            adaptor = new XmlDataAdaptor(document);
            adaptor.writeNode(dataHandler);
        }
        catch(Exception excpt) {
            throw new CreationException(excpt);
        }
        
        return adaptor;
    }    
    
    
    /** Create an XML document with only the doc tag and DTD URI specified */
    static public XmlDataAdaptor newEmptyDocumentAdaptor(String docTag, String dtdURI) {
        XmlDataAdaptor adaptor;
        
        try {
            DocumentBuilderFactory factory = 
                DocumentBuilderFactory.newInstance();
        
            DocumentBuilder builder = factory.newDocumentBuilder();
            DOMImplementation implementation = builder.getDOMImplementation();
            Document document = builder.newDocument();
            
            if ( docTag != null && dtdURI != null ) {
                DocumentType docType = 
                    implementation.createDocumentType(docTag, null, dtdURI);
                document.appendChild(docType);
            }
            
            adaptor = new XmlDataAdaptor(document);
        }
        catch(Exception excpt) {
            throw new CreationException(excpt);
        }
        
        return adaptor;
    }
    
  
    /** Create an empty XML document */
    static public XmlDataAdaptor newEmptyDocumentAdaptor() {
        return newEmptyDocumentAdaptor(null, null);
    }

    
    /*
     * Exception to wrap any DOM exceptions
     */
    static public class CreationException extends org.csstudio.mps.sns.tools.ExceptionWrapper {
        public CreationException(Exception excpt) {
            super(excpt);
        }
    }
}




