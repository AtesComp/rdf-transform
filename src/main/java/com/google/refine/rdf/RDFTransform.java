package com.google.refine.rdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.refine.rdf.app.ApplicationContext;
import com.google.refine.rdf.vocab.PrefixExistException;
import com.google.refine.rdf.vocab.Vocabulary;
import com.google.refine.rdf.vocab.VocabularyIndexException;

import com.google.refine.model.OverlayModel;
import com.google.refine.model.Project;

import org.eclipse.rdf4j.common.net.ParsedIRI;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RDFTransform implements OverlayModel {
    /****************************************************************************************************
     ****************************************************************************************************
     *
     *  Class Variables
     * 
     ****************************************************************************************************
     ****************************************************************************************************/

    static public final String EXTENSION = "RDFTransform";
    // This Server-side KEY matches Client-side RDFTransform.KEY
    static public final String KEY = "rdf_transform";

	static private final Logger logger = LoggerFactory.getLogger("RDFT:RDFTransform");

    /****************************************************************************************************
     ****************************************************************************************************
     *
     *  Class Methods
     * 
     ****************************************************************************************************
     ****************************************************************************************************/

    static public RDFTransform getRDFTransform(ApplicationContext theContext, Project theProject)
			throws VocabularyIndexException, IOException {
		synchronized (theProject) {
			RDFTransform theTransform = (RDFTransform) theProject.overlayModels.get(RDFTransform.EXTENSION);
			if (theTransform == null) {
				theTransform = new RDFTransform(theContext, theProject);

				theProject.overlayModels.put(RDFTransform.EXTENSION, theTransform);
				theProject.getMetadata().updateModified();
			}
            return theTransform;
		}
	}

    static public RDFTransform load(Project theProject, JsonNode jnodeElement)
            throws Exception {
        RDFTransform theTransform = RDFTransform.reconstruct(jnodeElement);
        return theTransform;
    }

    /*
     * Method reconstruct()
     *
     *      Reconstruct an RDFTransform, it's Nodes, Links, and Types
     *      Nodes are use to construct (Subject, Property, Object) tuples for a
     *      data transform to RDF.
     *      An RDFTransform contains the following:
     *        Base IRI - a default namespace descriptor
     *        Prefixes - a mapping of namespace descriptors to shortened prefixes
     *        Root Nodes - a list of nodes that serve as Subject elements and contain
     *            Links and Types
     *        
     *      Nodes are generic data cell or constant descriptors.
     *      Links are generic column or constant connection descriptors.
     *      Types are specified node descriptors like links, but describe type.
     * 
     *      Nodes:
     *      Generally there are two type of nodes - Resource and Literal.
     *          Subject and Property elements are always Resource nodes.
     *          Property elements are linking (edge) connections between Subject and
     *              Object elements.
     *          Object elements can be either Resource or Literal nodes.
     *      Literal nodes are leaf nodes for an RDF graph.
     *      A given Node can server as all three elements simultaniously.
     * 
     *      Links:
     *      A Link is composite of a Property element and an Object list.
     *      A Link list is held by a Subject element to describe its tuples.
     * 
     *      Types:
     *      Types are RDF types assigned to a Subject element.  They are like
     *      Links, but the Property elements is always "rdf:type".  Therefore,
     *      Types only hold Object elements that provide type information for
     *      the Subject element.
     *      A Type list is held by a Subject element to describe its type tuples.
     */
    static public RDFTransform reconstruct(JsonNode jnodeElement)
            throws JsonGenerationException {
        if ( Util.isVerbose(2) ) logger.info("Reconstructing overlay...");

        if (jnodeElement == null) {
            if ( Util.isVerbose(3) ) logger.info("  Nothing to reconstruct!");
            return null;
        }

        RDFTransform theTransform = new RDFTransform();

        //
        // Construct baseIRI...
        //
        // If it is not a direct string representation, then these keys are stored:
        // { "scheme"   : null or scheme in "scheme:" from beginning of string,
        //   "userInfo" : null or user:password in "://user:password@",
        //   "host"     : null or host in "@host:",
        //   "port"     : -1 or a port as integer (0 - 65535) in ":port/",
        //   "path"     : null or path in "path?" including starting /,
        //   "query"    : null or query in "?query#",
        //   "fragment" : null or fragment in "#fragment" to end of string,
        //   "absolute" : true if there is enough required for a "complete" IRI,
        //   "opaque"   : false if there is no "//", just :string
        // }
        JsonNode jnodeBaseIRI = jnodeElement.get("baseIRI");
        if (jnodeBaseIRI == null) {
            if ( Util.isVerbose(2) ) logger.warn("  No Base IRI!");
        }
        else {
            theTransform.setBaseIRI(jnodeBaseIRI);
        }

        //
        // Construct prefixesMap...
        //
        JsonNode jnodePrefixes = jnodeElement.get("prefixes");
        if (jnodePrefixes == null) {
            if ( Util.isVerbose(2) ) logger.warn("  No Prefixes!");
        }
        else {
            Map<String, Vocabulary> thePrefixes = new HashMap<String, Vocabulary>();
            if ( jnodePrefixes == null || ! jnodePrefixes.isArray() ) {
            	jnodePrefixes = JsonNodeFactory.instance.arrayNode();
            }
            for (JsonNode jnodePrefix : jnodePrefixes) {
            	String strPrefix = jnodePrefix.get("name").asText();
                String strIRI    = jnodePrefix.get("iri").asText();
        	    thePrefixes.put( strPrefix, new Vocabulary(strPrefix, strIRI) );
            }
            theTransform.setPrefixesMap(thePrefixes);
        }

        //
        // Construct listRootNodes...
        //
        JsonNode jnodeRoots = jnodeElement.get("rootNodes");
        if (jnodeRoots == null) {
            if ( Util.isVerbose(2) ) logger.warn("  No Root Nodes!");
        }
        else {
            List<ResourceNode> theRootNodes = new ArrayList<ResourceNode>();
            if ( jnodeRoots != null && ! jnodeRoots.isNull() ) {
                for (JsonNode jNodeRoot : jnodeRoots) {
                    Node nodeRoot = reconstructNode( jNodeRoot, theTransform.getBaseIRI() );
                    if (nodeRoot != null && nodeRoot instanceof ResourceNode) {
                        theRootNodes.add( (ResourceNode) nodeRoot );
                    }
                    // Otherwise, non-resource nodes (literals, generic nodes) cannot be root nodes.
                    // So, skip them.  They should never have been in the root node list anyway.
                }
            }
            theTransform.setRoots(theRootNodes);
        }

        if ( Util.isVerbose(2) ) logger.info("...reconstructed overlay");
        return theTransform;
    }

    /*
     * Method reconstructNode()
     *
     *      Helper function for reconstruct()
     *
     *      Reconstruct any ol' node and their links.  Nodes are generic descriptors for a related
     *      transformation.  They are transformed into RDF Resource and Literal nodes to construct
     *      (Subject, Property, Object) tuples for an RDF graph.
     */
    static private Node reconstructNode(JsonNode jnodeElement, final ParsedIRI baseIRI) {
        Node nodeElement = null;

        String strNodeType = jnodeElement.get("nodeType").asText();

        // Node contains Cell is a...
        if ( strNodeType.startsWith("cell-as-") ) {
            JsonNode jNodeRowNumberCell = jnodeElement.get("isRowNumberCell");
        	boolean bIsRowNumberCell = (jNodeRowNumberCell == null) ? false : jNodeRowNumberCell.asBoolean(false);

            // Get cell's ColumnName: null (for row number) or column...
            String strColumnName = null;
            if ( !bIsRowNumberCell ) {
            	strColumnName = jnodeElement.get("columnName").asText();
            }

            // Get cell's Expression: "value" or expression string...
            JsonNode jnodeExp = jnodeElement.get("expression");
            String strExp = (jnodeExp == null) ? "value" : jnodeExp.asText().strip();

            // Resource Node...
            if ( "cell-as-resource".equals(strNodeType) ) {
                CellResourceNode nodeCellResource = new CellResourceNode(strColumnName, strExp, bIsRowNumberCell);
                reconstructTypes(nodeCellResource, jnodeElement);
                nodeElement = nodeCellResource;
            }
            // Literal...
            else if ("cell-as-literal".equals(strNodeType)) {
                JsonNode jnodeValueType = jnodeElement.get("valueType");
                String strValueType =
                    (jnodeValueType == null) ?
                        null : Util.getDataType( baseIRI, jnodeValueType.asText() );

                JsonNode jnodeLang = jnodeElement.get("lang");
                String strLang = (jnodeLang == null) ? null : stripLanguageMarker( jnodeLang.asText() );

                nodeElement = new CellLiteralNode(strColumnName, strExp, strValueType, strLang, bIsRowNumberCell);
            }
            // Blank Node...
            else if ( "cell-as-blank".equals(strNodeType) ) {
                CellBlankNode nodeCellBlank = new CellBlankNode(strColumnName, strExp, bIsRowNumberCell);
                reconstructTypes(nodeCellBlank, jnodeElement );
                nodeElement = nodeCellBlank;
            }
        }
        // Node contains Constant Resource...
        else if ( "resource".equals(strNodeType) ) {
            ConstantResourceNode nodeConstResource = new ConstantResourceNode( jnodeElement.get("value").asText() );
            reconstructTypes(nodeConstResource, jnodeElement);
            nodeElement = nodeConstResource;
        }
        // Node contains Constant Literal...
        else if ( "literal".equals(strNodeType) ) {
            JsonNode jnodeValueType = jnodeElement.get("valueType");
            String strValueType =
                (jnodeValueType == null) ?
                    null : Util.getDataType( baseIRI, jnodeValueType.asText() );
            JsonNode jnodeLang = jnodeElement.get("lang");
            String strLang =
                jnodeLang == null ? null : stripLanguageMarker( jnodeLang.asText() );
            JsonNode jnodeValue = jnodeElement.get("value");
            nodeElement = new ConstantLiteralNode(jnodeValue.asText(), strValueType, strLang);
        }
        // Node contains Constant Blank Node Resource...
        else if ( "blank".equals(strNodeType) ) {
            ConstantBlankNode nodeConstBlank = new ConstantBlankNode();
            reconstructTypes(nodeConstBlank, jnodeElement);
            nodeElement = nodeConstBlank;
        }

        // For Resource Nodes, process any links...
        if ( nodeElement != null && nodeElement instanceof ResourceNode ) {
            JsonNode jnodeLinks = jnodeElement.get("links");
            if ( jnodeLinks != null && ! jnodeLinks.isNull() && jnodeLinks.isArray() ) {
                for (JsonNode jnodeLink : jnodeLinks) {
                    JsonNode jnodeLinkTarget = jnodeLink.get("target");
                    Node nodeLink = null;
                    if ( jnodeLinkTarget != null && ! jnodeLinkTarget.isNull() )
                        nodeLink = reconstructNode(jnodeLinkTarget, baseIRI);
                    ((ResourceNode) nodeElement).addLink(
                        new Link(
                            jnodeLink.get("iri").asText(),
                            jnodeLink.get("cirie").asText(),
                            nodeLink)
                    );
                }
            }
        }

        return nodeElement;
    }

    /*
     * Method reconstructTypes()
     *
     *      Helper function for reconstruct()
     *
     *      Reconstruct the Node Types.  Nodes are generic descriptors for a related
     *      transformation.  They are transformed into RDF Resource and Literal nodes to construct
     *      (Subject, Property, Object) tuples for an RDF graph.
     */
    static private void reconstructTypes(ResourceNode rnodeResource, JsonNode jnodeElement) {
    	if (jnodeElement.has("rdfTypes")) {
    		JsonNode jnodeTypes = jnodeElement.get("rdfTypes");

            for (JsonNode jnodeType : jnodeTypes) {
                rnodeResource.addType(
                    new RDFType(
                        jnodeType.get("iri").asText(),
                        jnodeType.get("cirie").asText()
                    )
                );
            }
        }
    }

    /*
     * Method stripLanguageMarker()
     *
     *      Helper function for reconstructNode()
     *
     *      Strip the starting '@' character, the language marker, from the string.  This function
     *      assumes it is given a language type string from a literal node.
     */
    static private String stripLanguageMarker(String strLanguage) {
    	if (strLanguage != null && strLanguage.startsWith("@")) {
    		return strLanguage.substring(1);
    	}
    	return strLanguage;
    }

    /****************************************************************************************************
     ****************************************************************************************************
     *
     *  Instance Variables
     * 
     ****************************************************************************************************
     ****************************************************************************************************/

    /*
     * Base IRI for Document
     *
     *  A base IRI is the default namespace used for resources lacking a formal namespace.
     *
     *    Given a base IRI:
     *      http://example.com/
     *    and a raw column name used for a property:
     *      first name
     *    the resulting IRI for the property would be:
     *      http://example.com/first_name
     */
    @JsonIgnore
    private ParsedIRI theBaseIRI;

    /*
     * Prefix Mapping to Namespace
     * 
     *   A defined Prefix mapping consists of a:
     *       1) Prefix - a short name for a Namespace
     *       2) Namespace - a full IRI
     *
     *   For this mapping:
     *       Keys:   the Namespace Prefix
     *       Values: the full IRI Namespace
     * 
     *   Example:
     *       Prefix  Namespace
     *       (Key)   (Value)
     *       ------  ----------------------------------------
     *       foaf    http://xmlns.com/foaf/0.1/
     *
     *   Prefixes are use to construct Condensed IRI Expressions (CIRIE).
     *   For an IRI:
     *       http://xmlns.com/foaf/0.1/knows
     *   a CIRIE using the above prefix is:
     *       foaf:knows
     */ 
    @JsonIgnore
    private Map<String, Vocabulary> thePrefixes;

    /*
     * Root Nodes for Document
     *
     *  A root node is any subject element  designated in the transform.
     */
    @JsonProperty("rootNodes")
    private List<ResourceNode> theRootNodes;

    /****************************************************************************************************
     ****************************************************************************************************
     *
     *  Instance Methods
     * 
     ****************************************************************************************************
     ****************************************************************************************************/

    /*
        Constructors
    */
    @JsonCreator
    public RDFTransform() {
        if ( Util.isVerbose(2) ) logger.info("Created empty overlay");
    }

    public RDFTransform(ApplicationContext theContext, Project theProject) throws VocabularyIndexException, IOException {
        if ( Util.isVerbose(2) ) logger.info("Creating base overlay for project from context...");

        this.theBaseIRI = Util.buildIRI( theContext.getDefaultBaseIRI() );
       	this.thePrefixes = clonePrefixes( theContext.getPredefinedVocabularyManager().getPredefinedVocabulariesMap() );
       	// Copy the index of predefined vocabularies...
       	//   Each project will have its own copy of these predefined vocabs to enable, delete, update...
       	theContext.getVocabularySearcher().addPredefinedVocabulariesToProject(theProject.id);
        this.theRootNodes = new ArrayList<ResourceNode>();

        if ( Util.isVerbose(2) ) logger.info("Created overlay");
    }

    /*
        Methods
    */
    @JsonIgnore // ...see getBaseIRIAsString()
    public ParsedIRI getBaseIRI() {
        return this.theBaseIRI;
    }

    @JsonProperty("baseIRI")
    public String getBaseIRIAsString() {
        return this.theBaseIRI.toString();
    }

    @JsonIgnore // ...see setBaseIRI(JsonNode)
    public void setBaseIRI(ParsedIRI iriBase)  {
        if ( Util.isVerbose(2) ) logger.info("Setting Base IRI...");
        this.theBaseIRI = iriBase;
    }

    @JsonProperty("baseIRI")
    public void setBaseIRI(JsonNode jnodeBaseIRI)  {
        if ( Util.isVerbose(2) ) logger.info("Setting Base IRI from JSON Node...");

        ParsedIRI iriBase = null;
        if ( jnodeBaseIRI != null && ! jnodeBaseIRI.isNull() ) {
            String strBaseIRI;
            if ( jnodeBaseIRI.isValueNode() ) {
                // Get the Base IRI string...
                strBaseIRI = jnodeBaseIRI.asText();
                iriBase = Util.buildIRI( strBaseIRI );
            }
            else {
                // Get the Base IRI components...
                String  strScheme   = jnodeBaseIRI.get("scheme"  ).asText();
                String  strUserInfo = jnodeBaseIRI.get("userInfo").asText();
                String  strHost     = jnodeBaseIRI.get("host"    ).asText();
                int     iPort       = jnodeBaseIRI.get("port"    ).asInt(-1);
                String  strPath     = jnodeBaseIRI.get("path"    ).asText();
                String  strQuery    = jnodeBaseIRI.get("query"   ).asText();
                String  strFragment = jnodeBaseIRI.get("fragment").asText();
                boolean bAbsolute   = jnodeBaseIRI.get("absolute").asBoolean();
                boolean bOpaque     = jnodeBaseIRI.get("opaque"  ).asBoolean();

                // Construct the Base IRI string from the components...
                strBaseIRI = "";
                if ( strScheme != null && ! strScheme.isEmpty() )
                    strBaseIRI += strScheme + ":";
                if ( strHost != null && ! strHost.isEmpty() ) {
                    strBaseIRI += "//";
                    if ( strUserInfo != null && ! strUserInfo.isEmpty() )
                        strBaseIRI += strUserInfo + "@";
                    strBaseIRI += strHost;
                    if ( iPort > -1 )
                        strBaseIRI += ":" + Integer.toString(iPort);
                }
                if ( strPath != null && ! strPath.isEmpty() )
                    strBaseIRI += strPath;
                if ( strQuery != null && ! strQuery.isEmpty() )
                    strBaseIRI += "?" + strQuery;
                if ( strFragment != null && ! strFragment.isEmpty() )
                    strBaseIRI += "#" + strFragment;

                // Realize the Base IRI...
                iriBase = Util.buildIRI( strBaseIRI );

                // Sanity check the Base IRI against remaining components...
                if (iriBase != null) {
                    if (iriBase.isAbsolute() != bAbsolute) {
                        if ( Util.isVerbose() ) logger.warn("Mismatch reconstructing Base IRI: Absolute Value");
                    }
                    if (iriBase.isOpaque() != bOpaque) {
                        if ( Util.isVerbose() ) logger.warn("Mismatch reconstructing Base IRI: Opaque Value");
                    }
                }
            }
        }
        this.theBaseIRI = iriBase;
    }

    public Map<String, Vocabulary> getPrefixesMap() {
		return this.thePrefixes;
	}

    public void setPrefixesMap(Map<String, Vocabulary> mapPrefixes) {
        this.thePrefixes = mapPrefixes;
	}

    public void addPrefix(String strName, String strIRI) throws PrefixExistException {
        if (this.thePrefixes == null) {
            this.thePrefixes = new HashMap<String, Vocabulary>();
        }
        synchronized(this.thePrefixes) {
    		if ( this.thePrefixes.containsKey(strName) ) {
    		    throw new PrefixExistException(strName + " already defined");
    		}
    		this.thePrefixes.put( strName, new Vocabulary(strName, strIRI) );
    	}
    }

    public void removePrefix(String strName) {
        synchronized(this.thePrefixes) {
            this.thePrefixes.remove(strName);
        }
    }

    @JsonProperty("prefixes")
    public Collection<Vocabulary> getPrefixes() {
        if ( Util.isVerbose(2) ) logger.info("Getting prefixes...");
        if (thePrefixes != null) {
    	    return this.thePrefixes.values();
        }
        return null;
    }

    @JsonProperty("prefixes")
    public void setPrefixes(Vocabulary[] aVocabularies) {
        if ( Util.isVerbose(2) ) logger.info("Setting prefixes...");
        if (this.thePrefixes == null) {
            this.thePrefixes = new HashMap<String, Vocabulary>();
        }
        synchronized(this.thePrefixes) {
            this.thePrefixes.clear();
            for (Vocabulary vocab : aVocabularies) {
                this.thePrefixes.put( vocab.getPrefix(), vocab );
            }
        }
    }

    @JsonProperty("rootNodes")
	public List<ResourceNode> getRoots() {
        if ( Util.isVerbose(2) ) logger.info("Getting root nodes: size = " + this.theRootNodes.size());
        return this.theRootNodes;
    }

    @JsonProperty("rootNodes")
	public void setRoots(List<ResourceNode> listRootNodes) {
        if ( Util.isVerbose(2) ) logger.info("Setting root nodes...");
        this.theRootNodes = listRootNodes;
    }

    @Override
    public void onBeforeSave(Project theProject) {
        if ( Util.isVerbose(2) ) logger.info("Saving...");
    }

    @Override
    public void onAfterSave(Project theProject) {
        if ( Util.isVerbose(2) ) logger.info("...saved.");
    }

    @Override
    public void dispose(Project theProject) {
	   /*try {
			ApplicationContext.instance().getVocabularySearcher().deleteProjectVocabularies(String.valueOf(theProject.id));
		}
        catch (ParseException ex) {
			logger.error("ERROR: Unable to delete index for project " + theProject.id, ex);
		}
        catch (IOException ex) {
			logger.error("ERROR: Unable to delete index for project " + theProject.id, ex);
		}*/
        if ( Util.isVerbose(2) ) logger.info("Disposed overlay");
    }

    public void write(JsonGenerator theWriter)
            // JsonGenerationException is subclass of IOException
            throws IOException {
        theWriter.writeStartObject();

        theWriter.writeStringField("baseIRI", this.theBaseIRI.toString());

        theWriter.writeFieldName("prefixes");
        theWriter.writeStartArray();
        for ( Vocabulary vocab : this.thePrefixes.values() ) {
            theWriter.writeStartObject();
            theWriter.writeStringField( "name", vocab.getPrefix() );
            theWriter.writeStringField( "iri", vocab.getNamespace() );
            theWriter.writeEndObject();
        }
        theWriter.writeEndArray();

        theWriter.writeFieldName("rootNodes");
        theWriter.writeStartArray();
        for (Node nodeRoot : this.theRootNodes) {
            nodeRoot.write(theWriter);
        }
        theWriter.writeEndArray();

        theWriter.writeEndObject();

        theWriter.flush();
    }

    private Map<String, Vocabulary> clonePrefixes(Map<String, Vocabulary> mapPrefixes) {
    	Map<String, Vocabulary> mapPrefixesCopy = new HashMap<String, Vocabulary>();

        for ( Entry<String, Vocabulary> entryPrefix : mapPrefixes.entrySet() ) {
    		mapPrefixesCopy.put(
                entryPrefix.getKey(),
                new Vocabulary(
                    entryPrefix.getValue().getPrefix(),
                    entryPrefix.getValue().getNamespace() )
            );
    	}

    	return mapPrefixesCopy;
    }
}
