package com.google.refine.rdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.refine.rdf.model.Node;
import com.google.refine.rdf.model.ResourceNode;
import com.google.refine.rdf.model.Util;
import com.google.refine.rdf.model.vocab.Vocabulary;
import com.google.refine.rdf.model.vocab.VocabularyList;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.model.OverlayModel;
import com.google.refine.model.Project;

import org.eclipse.rdf4j.common.net.ParsedIRI;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RDFTransform implements OverlayModel {
    /****************************************************************************************************
     ****************************************************************************************************
     *
     *  Class Classes
     * 
     ****************************************************************************************************
     ****************************************************************************************************/

    static public final class Reconstructor {
        private Reconstructor() {};
    }

    /****************************************************************************************************
     ****************************************************************************************************
     *
     *  Class Variables
     * 
     ****************************************************************************************************
     ****************************************************************************************************/

	static private final Logger logger = LoggerFactory.getLogger("RDFT:RDFTransform");

    static public final String EXTENSION = "RDFTransform";
    static public final String VERSION_MAJOR = "2";
    static public final String VERSION_MINOR = "0";
    static public final String VERSION_MICRO = "0";
    static public final String VERSION =
        RDFTransform.VERSION_MAJOR + "." +
        RDFTransform.VERSION_MINOR + "." +
        RDFTransform.VERSION_MICRO;
    // This Server-side KEY matches Client-side RDFTransform.KEY
    static public final String KEY = "rdf_transform";

    // Reconstruction Validator
    static private final Reconstructor theReconstructor = new Reconstructor();

    /****************************************************************************************************
     ****************************************************************************************************
     *
     *  Class Methods
     * 
     ****************************************************************************************************
     ****************************************************************************************************/

    static public RDFTransform getRDFTransform(ApplicationContext theContext, Project theProject)
			throws IOException {
		synchronized(theProject) {
			RDFTransform theTransform = (RDFTransform) theProject.overlayModels.get(RDFTransform.EXTENSION);
			if (theTransform == null) {
				theTransform = new RDFTransform(theContext, theProject);

				theProject.overlayModels.put(RDFTransform.EXTENSION, theTransform);
				theProject.getMetadata().updateModified();
			}
            return theTransform;
		}
	}

    static public RDFTransform load(Project theProject, JsonNode jnodeElement) {
        RDFTransform theTransform = RDFTransform.reconstruct(jnodeElement);
        return theTransform;
    }

    /*
     * Method reconstruct()
     *
     *      Reconstruct an RDF Transform, it's Subject, Property, and Type Mappings.
     * 
     *      RDF Transform Nodes are use to construct (Subject, Property, Object) tuples for a
     *      data transform to RDF.
     *      An RDFTransform contains the following:
     *        Base IRI - a default namespace descriptor
     *        Namespaces - a mapping of short prefixes to namespace descriptors
     *        Subject Mappings - a list of Transform Nodes that serve as Subjects and contain
     *            Property Mappings and Type Mappings
     *        
     *      Transform Nodes are column or constant descriptors that represent Subjects,
     *          Properties, and Objects.
     *      Subjects and Objects are column or constant descriptors that represent graph nodes.
     *      Properties are column or constant descriptors that connect Subjects to
     *          Objects, i.e. graph edges, and provide context.
     *      Types use the "rdf:type" property to describe Objects that define a Subject's
     *          type.
     * 
     *      Nodes and Edges:
     *      --------------------
     *      Generally, there are two Transform Node categories - Resources and Literals.
     *          Subjects and Properties are always Resources.
     *          Properties are linking (edge) connections between Subjects and
     *              Objects.
     *          Objects can be either Resources or Literals.
     *      Then, Resources are the nodes and edges for an RDF graph and may
     *          be Subjects, Properties, and Objects.
     *      Then, Literals are "leaf (terminal)" nodes in an RDF graph and are
     *          always Objects.
     *      A given Transform Node can server as all three simultaniously, but generally not
     *          in the same tuple.
     *      Then, a tuple (Subject, Property, Object) represents and node-edge-node
     *          subgraph that connects the Subject node to the Object node through the
     *          Property edge (a directed edge).
     *      Resources and Literals can be subcategorized as Constants and Columns.
     *      Constants are constant Resources or Literals that can be defined and used anywhere
     *          in the entire resulting RDF graph.
     *      Columns are column defined Resources or Literals that are based on data rows or
     *          records that intersect with a specific column name to harvest data cell to
     *          include in the transform.  Column names are generally used to specify Property
     *          Resources while the cell data are used to specify Subject Resources and Object
     *          Resources or Literals.
     *      Additionally, Blank Nodes are special unnamed, i.e. blank, Resources used as
     *          connective tissue to generally group several related Objects for a Subject.
     *          Each Blank node is local graph unique and can be system defined or value based.
     *          Value based Blank Nodes can be Constant or Column defined.  Blank node have a
     *          specialized, graph local, naming scheme of the form "_:somename" where
     *          "somename" may be user specified as a placeholder for queries or inserts and
     *          system specified for query results.
     * 
     *      Subject Mappings:
     *      --------------------
     *      A Subject Mapping is a list of subject elements.  Each subject element is a
     *          composite of a single subject, a Property Mapping, and a Type Mapping.  The
     *          subjects of the Subject Mapping list are the root nodes of the RDF Transform.
     * 
     *      Property Mapping:
     *      --------------------
     *      A Property Mapping is a a list of property elements.  Each property element is a
     *          composite of a single property and an Object Mapping.  A Property Mapping is
     *          held by each subject to describe its general tuples.
     * 
     *      Type Mappings:
     *      --------------------
     *      A Type Mapping is a list of resource elements that describe the RDF types that
     *          define a subject.  They are like Property Mappings, but the property is always
     *          "rdf:type" and the objects are always resources.  Therefore, a Type Mapping only
     *          holds resources that provide type information for a subject.  A Type Mapping is
     *          held by a subject to describe its type tuples.  This completes a type
     *          description (Subject, Property, Object) tuple.
     * 
     *      Object Mappings:
     *      --------------------
     *      An Object Mapping is a list of object elements.  Objects may also serve as subjects
     *      for other tuples.  Therefore, individual objects may iterate as per subjects from
     *      the above Subject Mappings.  An Object Mapping is held by each property to describe
     *      the general tuples for the property's subject.  This completes a general statement
     *      description (Subject, Property, Object) tuple.
     */
    static public RDFTransform reconstruct(JsonNode jnodeRoot) {
        if ( Util.isVerbose(2) ) RDFTransform.logger.info("Reconstructing overlay...");

        if (jnodeRoot == null) {
            if ( Util.isVerbose(3) ) RDFTransform.logger.info("  Nothing to reconstruct!");
            return null;
        }

        RDFTransform theTransform = new RDFTransform();

        //
        // JSON Header...
        //
        String strExtension = null;
        JsonNode jnodeExtension = jnodeRoot.get(Util.gstrExtension);
        if (jnodeExtension != null) {
            strExtension = jnodeExtension.asText();
            if (strExtension == null) {
                strExtension = "";
            }
        }
        String strVersion = null;
        JsonNode jnodeVersion = jnodeRoot.get(Util.gstrVersion);
        if (jnodeVersion != null) {
            strVersion = jnodeVersion.asText();
            if (strVersion == null) {
                strVersion = "";
            }
        }
        if ( Util.isVerbose(3) ) {
            RDFTransform.logger.info("  Found Extension: [" + strExtension + "]  Version: [" + strVersion + "]");
            if ( ! strVersion.equals(VERSION) ) {
                RDFTransform.logger.info("    Current Version: [" + VERSION + "] will update template on save.");
            }
        }

        //
        // Construct Base IRI from "baseIRI"...
        //
        JsonNode jnodeBaseIRI = null;
        if ( jnodeRoot.has(Util.gstrBaseIRI) ) {
            jnodeBaseIRI = jnodeRoot.get(Util.gstrBaseIRI);
            theTransform.setBaseIRI(jnodeBaseIRI);
        }
        else {
            if ( Util.isVerbose(2) ) RDFTransform.logger.warn("  No Base IRI!");
        }

        //
        // Construct prefixesMap from "namespaces"...
        //
        //  NOTE: The map "thePrefixes" is just a convenience map to store and look up vocabularies
        //      based on the prefix as a key.
        VocabularyList thePrefixes = new VocabularyList();
        JsonNode jnodePrefixes = null;
        if ( jnodeRoot.has(Util.gstrNamespaces) ) {
            jnodePrefixes = jnodeRoot.get(Util.gstrNamespaces);
            if ( jnodePrefixes.isObject() ) {
                Iterator<Entry<String, JsonNode>> iterNodes = jnodePrefixes.fields();
                Map.Entry<String, JsonNode> mePrefix;
                while ( iterNodes.hasNext() ) {
                    mePrefix = (Map.Entry<String, JsonNode>) iterNodes.next();
                    String strPrefix = mePrefix.getKey();
                    String strIRI    = mePrefix.getValue().asText();
                    thePrefixes.add( new Vocabulary(strPrefix, strIRI) );
                }
                theTransform.setPrefixesMap(thePrefixes);
            }
        }
        else {
            if ( Util.isVerbose(2) ) RDFTransform.logger.warn("  No Namespaces!");
        }

        //
        // Construct listRootNodes from "subjectMappings"...
        //
        JsonNode jnodeSubjectMappings = null;
        if ( jnodeRoot.has(Util.gstrSubjectMappings) ) {
            jnodeSubjectMappings = jnodeRoot.get(Util.gstrSubjectMappings);
            List<ResourceNode> theRootNodes = new ArrayList<ResourceNode>();
            if ( jnodeSubjectMappings.isArray() ) {
                for (JsonNode jnodeSubject : jnodeSubjectMappings) {
                    Node nodeRoot =
                        Node.reconstructNode(
                            RDFTransform.theReconstructor, jnodeSubject,
                            theTransform.getBaseIRI(), thePrefixes);
                    if (nodeRoot != null && nodeRoot instanceof ResourceNode) {
                        theRootNodes.add( (ResourceNode) nodeRoot );
                    }
                    // Otherwise, non-resource nodes (literals, generic nodes) cannot be root nodes.
                    // So, skip them.  They should never have been in the root node list anyway.
                }
            }
            theTransform.setRoots(theRootNodes);
        }
        else {
            if ( Util.isVerbose(2) ) RDFTransform.logger.warn("  No Subjects!");
        }

        if ( Util.isVerbose(2) ) RDFTransform.logger.info("...reconstructed overlay");
        return theTransform;
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
    private VocabularyList thePrefixes;

    /*
     * Root Nodes for Document
     *
     *  A root node is any subject element  designated in the transform.
     */
    //@JsonProperty("rootNodes")
    @JsonIgnore
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
        if ( Util.isVerbose(2) ) RDFTransform.logger.info("Created empty overlay");
    }

    public RDFTransform(ApplicationContext theContext, Project theProject)
            throws IOException {
        if ( Util.isVerbose(2) ) RDFTransform.logger.info("Creating base overlay for project from context...");

        this.theBaseIRI = Util.buildIRI( theContext.getDefaultBaseIRI() );
       	this.thePrefixes = theContext.getPredefinedVocabularyManager().getPredefinedVocabularies().clone();
       	// Copy the index of predefined vocabularies...
       	//   Each project will have its own copy of these predefined vocabs to enable, delete, update...
       	theContext.getVocabularySearcher().addPredefinedVocabulariesToProject(theProject.id);
        this.theRootNodes = new ArrayList<ResourceNode>();

        if ( Util.isVerbose(2) ) RDFTransform.logger.info("Created overlay");
    }

    /*
        Methods
    */
    @JsonProperty(Util.gstrExtension)
    public String getExtension() {
        return RDFTransform.EXTENSION;
    }

    @JsonProperty(Util.gstrExtension)
    public void setExtension(JsonNode jnodeExtension) {
        // Ignore...
        return;
    }

    @JsonProperty(Util.gstrVersion)
    public String getVersion() {
        return RDFTransform.VERSION;
    }

    @JsonProperty(Util.gstrVersion)
    public void setVersion(JsonNode jnodeVersion) {
        // Ignore...
        return;
    }

    @JsonIgnore // ...see getBaseIRIAsString()
    public ParsedIRI getBaseIRI() {
        return this.theBaseIRI;
    }

    @JsonProperty(Util.gstrBaseIRI)
    public String getBaseIRIAsString() {
        return this.theBaseIRI.toString();
    }

    @JsonIgnore // ...see setBaseIRI(JsonNode)
    public void setBaseIRI(ParsedIRI iriBase)  {
        if ( Util.isVerbose(2) ) RDFTransform.logger.info("Setting Base IRI...");
        this.theBaseIRI = iriBase;
    }

    @JsonProperty(Util.gstrBaseIRI)
    public void setBaseIRI(JsonNode jnodeBaseIRI)  {
        if ( Util.isVerbose(2) ) RDFTransform.logger.info("Setting Base IRI from JSON Node...");

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
                        if ( Util.isVerbose() ) RDFTransform.logger.warn("Mismatch reconstructing Base IRI: Absolute Value");
                    }
                    if (iriBase.isOpaque() != bOpaque) {
                        if ( Util.isVerbose() ) RDFTransform.logger.warn("Mismatch reconstructing Base IRI: Opaque Value");
                    }
                }
            }
        }
        if (iriBase != null) {
            this.theBaseIRI = iriBase;
        }
    }

    @JsonIgnore
    public VocabularyList getPrefixesMap() {
		return this.thePrefixes;
	}

    @JsonIgnore
    public void setPrefixesMap(VocabularyList mapPrefixes) {
        this.thePrefixes = mapPrefixes;
	}

    @JsonIgnore
    public void addPrefix(String strPrefix, String strNamespace) {
        if (this.thePrefixes == null) {
            this.thePrefixes = new VocabularyList();
        }
        this.thePrefixes.add( new Vocabulary(strPrefix, strNamespace) );
    }

    @JsonIgnore
    public void removePrefix(String strPrefix) {
        this.thePrefixes.removeByPrefix(strPrefix);
    }

    @JsonIgnore
    public VocabularyList getPrefixes() {
        if ( Util.isVerbose(2) ) RDFTransform.logger.info("Getting prefixes...");
        return thePrefixes;
    }

    @JsonProperty(Util.gstrNamespaces)
    public String getPrefixesAsString() {
        if ( Util.isVerbose(2) ) RDFTransform.logger.info("Getting prefixes as JSON string...");
        ByteArrayOutputStream baostream = new ByteArrayOutputStream();
        try {
            JsonGenerator jsonWriter = ParsingUtilities.mapper.getFactory().createGenerator(baostream);
            if ( this.thePrefixes == null || this.thePrefixes.isEmpty() ) {
                jsonWriter.writeNull();
                return baostream.toString("UTF-8");
            }
            jsonWriter.writeStartObject();
            for (Vocabulary thePrefix : this.thePrefixes) {
                jsonWriter.writeObjectField( thePrefix.getPrefix(), thePrefix.getNamespace() );
            }
            jsonWriter.writeEndObject();
            return baostream.toString("UTF-8");
        }
        catch (Exception ex) {
            RDFTransform.logger.error("Error getting namespaces!", ex);
            return "null";
        }
    }

    @JsonProperty(Util.gstrNamespaces)
    public void setPrefixes(JsonNode jnodePrefixes) {
        if ( Util.isVerbose(2) ) RDFTransform.logger.info("Setting prefixes...");
        if (this.thePrefixes == null) {
            this.thePrefixes = new VocabularyList();
        }
        synchronized(this.thePrefixes) {
            if (jnodePrefixes == null) {
                return;
            }
            this.thePrefixes.clear();
            if ( jnodePrefixes.isObject() ) {
                Iterator<Entry<String, JsonNode>> iterNodes = jnodePrefixes.fields();
                Map.Entry<String, JsonNode> mePrefix;
                while ( iterNodes.hasNext() ) {
                    mePrefix = (Map.Entry<String, JsonNode>) iterNodes.next();
                    String strPrefix = mePrefix.getKey();
                    String strIRI    = mePrefix.getValue().asText();
                    this.thePrefixes.add( new Vocabulary(strPrefix, strIRI) );
                }
            }
        }
    }

    @JsonIgnore
	public List<ResourceNode> getRoots() {
        if ( Util.isVerbose(2) ) RDFTransform.logger.info("Getting root nodes: size = " + this.theRootNodes.size());
        return this.theRootNodes;
    }

    @JsonProperty(Util.gstrSubjectMappings)
    public String getRootsAsString() {
        if ( Util.isVerbose(2) ) RDFTransform.logger.info("Getting roots as JSON string...");
        ByteArrayOutputStream baostream = new ByteArrayOutputStream();
        try {
            JsonGenerator jsonWriter = ParsingUtilities.mapper.getFactory().createGenerator(baostream);
            if ( this.theRootNodes == null || this.theRootNodes.isEmpty() ) {
                jsonWriter.writeNull();
                return baostream.toString("UTF-8");
            }
            jsonWriter.writeStartArray();
            for (Node nodeRoot : this.theRootNodes) {
                nodeRoot.write(jsonWriter);
            }
            jsonWriter.writeEndArray();
            return baostream.toString("UTF-8");
        }
        catch (Exception ex) {
            RDFTransform.logger.error("Error getting root nodes!", ex);
            return "null";
        }
    }

    @JsonIgnore
	public void setRoots(List<ResourceNode> listRootNodes) {
        if ( Util.isVerbose(2) ) RDFTransform.logger.info("Setting root nodes...");
        this.theRootNodes = listRootNodes;
    }

    @JsonProperty(Util.gstrSubjectMappings)
    public void setRoots(JsonNode jnodeSubjectMappings) {
        if ( Util.isVerbose(2) ) RDFTransform.logger.info("Setting root nodes...");
        if (this.theRootNodes == null) {
            this.theRootNodes = new ArrayList<ResourceNode>();
        }
        synchronized(this.theRootNodes) {
            if (jnodeSubjectMappings == null) {
                return;
            }
            List<ResourceNode> listRootNodes = new ArrayList<ResourceNode>();
            if ( jnodeSubjectMappings.isArray() ) {
                for (JsonNode jnodeSubject : jnodeSubjectMappings) {
                    Node nodeRoot =
                        Node.reconstructNode(
                            RDFTransform.theReconstructor, jnodeSubject,
                            this.getBaseIRI(), this.thePrefixes);
                    if (nodeRoot != null && nodeRoot instanceof ResourceNode) {
                        listRootNodes.add( (ResourceNode) nodeRoot );
                    }
                    // Otherwise, non-resource nodes (literals, generic nodes) cannot be root nodes.
                    // So, skip them.  They should never have been in the root node list anyway.
                }
            }
            this.theRootNodes = listRootNodes;
        }
    }

    @Override
    @JsonIgnore
    public void onBeforeSave(Project theProject) {
        if ( Util.isVerbose(2) ) RDFTransform.logger.info("Saving...");
    }

    @Override
    @JsonIgnore
    public void onAfterSave(Project theProject) {
        if ( Util.isVerbose(2) ) RDFTransform.logger.info("...saved.");
    }

    @Override
    @JsonIgnore
    public void dispose(Project theProject) {
	   /*try {
			ApplicationContext.instance().getVocabularySearcher().deleteProjectVocabularies(String.valueOf(theProject.id));
		}
        catch (ParseException ex) {
			RDFTransform.logger.error("ERROR: Unable to delete index for project " + theProject.id, ex);
		}
        catch (IOException ex) {
			RDFTransform.logger.error("ERROR: Unable to delete index for project " + theProject.id, ex);
		}*/
        if ( Util.isVerbose(2) ) RDFTransform.logger.info("Disposed overlay");
    }

    /*
     * Method: write(JsonGenerator theWriter)
     * 
     * This write() method produces the JSON object containing the entire RDF Transform template.
     * The template's format is:
     * 
     * The Base IRI
     * The Namespaces ( "namespaces" ) containing each namespace keyed to its prefix
     * The Subject Mappings array of Subjects ( "subjectMappings" ) consisting of
     *      the Subject's Prefix ( "prefix" )
     *      the Subject's Source ( "valueSource" )
     *      the Subject's Expression ( "expression" )
     *      the Subject's Type Mappings ( "typeMappings" ) array of Types [via rdf:type ("a") property, an IRI] consisting of
     *          the Type's Prefix (prefix)
     *          the Type's Source (valueSource)
     *          the Type's Expression (expression)
     *      the Subject's Property Mappings ( "propertyMappings" ) array of Properties consisting of
     *          the Property's Prefix (prefix)
     *          the Property's Source (valueSource)
     *          the Property's Expression (expression)
     *          the Property's Objects (values) array of Objects [resource IRI or literal Data] consisting of
     *              the Object's Prefix (prefix) [for resources]
     *              the Object's Source (valueSource)
     *              the Object's Type (valueType) [Datatype for literals]
     *              the Object's Expression (expression)
     * 
     * "baseIRI" : an IRI
     * "namespaces" : {
     *      a prefix : a namespace IRI,
     *      ...
     * }
     * "subjectMappings" : [
     *      {   "prefix" : a prefix,
     *          "valueSource" : { },
     *          "expression": { }
     *          "typeMappings" : [ { }, ... ],
     *          "propertyMappings" : [ { }, ... ]
     *      },
     *      ...
     * ]
     * 
     * "typeMappings" : [
     *      {   "prefix" : a prefix,
     *          "valueSource" : { },
     *          "expression": { }
     *      },
     *      ...
     * ]
     * "propertyMappings" : [
     *      {   "prefix" : a prefix,
     *          "valueSource" : { },
     *          "expression": { },
     *          "objectMappings": [ { }, ... ]
     *      },
     *      ...
     * ]
     * "objectMappings": [
     *      {   "prefix" : a prefix,
     *          "valueSource" : { },
     *          "valueType" : { },
     *          "expression": { }
     *      },
     *      ...
     * ]
     * 
     * "prefix" : a prefix from "namespaces"
     * 
     * "valueSource" : {
     *      "source": a source ["row_index", "record_id", "constant", "column", "expression"],
     *    for "constant":
     *      "constant": a constant value string
     *    for "column":
     *      "columnName": a column name from the data store
     * }
     * 
     * "valueType" : {
     *      "type": a type ["iri", "literal", "language_literal", "datatype_literal", "value_bnode", "bnode"],
     *    for "iri":
     *      "typeMappings": []
     *      "propertyMappings": [],
     *    for "language_literal":
     *      "language": a language code ("en", ...)
     *    for "datatype_literal":
     *      "datatype": {
     *          "prefix" : a prefix,
     *          "valueSource": { }
     *          "expression": { },
     *      }
     * }
     *      
     * "expression": {
     *      "language": a code language such as "grel",
     *      "code": the code language expression
     * }
     */
    @JsonIgnore
    public void write(JsonGenerator theWriter)
            throws IOException {
        theWriter.writeStartObject();

        //
        // JSON Header...
        //
        theWriter.writeStringField(Util.gstrExtension, RDFTransform.EXTENSION);
        theWriter.writeStringField(Util.gstrVersion, RDFTransform.VERSION);

        //
        // The Base IRI (baseIRI)...
        //
        theWriter.writeStringField(Util.gstrBaseIRI, this.theBaseIRI.toString());

        //
        // The Namespaces (namespaces)...
        //
        theWriter.writeObjectFieldStart(Util.gstrNamespaces);
        for ( Vocabulary vocab : this.thePrefixes ) {
            vocab.write(theWriter);
        }
        theWriter.writeEndObject();

        //
        // The Subject Mappings (subjectMappings) array...
        //
        theWriter.writeArrayFieldStart(Util.gstrSubjectMappings);
        for (Node nodeRoot : this.theRootNodes) {
            nodeRoot.write(theWriter);
        }
        theWriter.writeEndArray();

        theWriter.writeEndObject();

        theWriter.flush();
    }
}
