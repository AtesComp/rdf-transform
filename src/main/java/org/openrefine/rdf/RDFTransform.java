/*
 *  Class RDFTransform
 *
 *  The RDF Transform class use to manage RDF Transform structures used by
 *  OpenRefine projects.
 *
 *  Copyright 2025 Keven L. Ates
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.openrefine.rdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.openrefine.rdf.model.Node;
import org.openrefine.rdf.model.ResourceNode;
import org.openrefine.rdf.model.Util;
import org.openrefine.rdf.model.vocab.Vocabulary;
import org.openrefine.rdf.model.vocab.VocabularyList;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.model.OverlayModel;
import com.google.refine.model.Project;

import org.apache.jena.iri.IRI;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
    // RDF Transform Version Control
    static public final String VERSION_MAJOR = "2";
    static public final String VERSION_MINOR = "3";
    static public final String VERSION_MICRO = "6";
    static public final String VERSION =
        RDFTransform.VERSION_MAJOR + "." +
        RDFTransform.VERSION_MINOR + "." +
        RDFTransform.VERSION_MICRO;
    // This Server-side RDFTransform.KEY matches Client-side RDFTransform.KEY
    static public final String KEY = "rdf-transform";

    // Reconstruction Validator
    static private final Reconstructor theReconstructor = new Reconstructor();

    static private ApplicationContext theGlobalContext = null;

    /****************************************************************************************************
     ****************************************************************************************************
     *
     *  Class Methods
     *
     ****************************************************************************************************
     ****************************************************************************************************/

    static public void setGlobalContext(ApplicationContext theContext) {
        RDFTransform.theGlobalContext = theContext;
    }

    static public ApplicationContext getGlobalContext() {
        return RDFTransform.theGlobalContext;
    }

    static public RDFTransform getRDFTransform(Project theProject) {
        synchronized(theProject) {
            // Get the project's existing RDFTransform, if it exists...
            RDFTransform theTransform = (RDFTransform) theProject.overlayModels.get(RDFTransform.EXTENSION);
            if (theTransform != null) {
                if ( Util.isDebugMode() ) RDFTransform.logger.info("DEBUG: getRDFTransform(): The transform exists.");
            }
            else {
                // Create a new RDFTransform for the project...
                theTransform = new RDFTransform(theProject);

                theProject.overlayModels.put(RDFTransform.EXTENSION, theTransform);
                theProject.getMetadata().updateModified();

                if ( Util.isDebugMode() ) RDFTransform.logger.info("DEBUG: getRDFTransform(): The transform is created.");
            }
            return theTransform;
        }
    }

    /**
     * Load a reconstructed RDFTransform...<br /><br />
     * NOTE: The RDFTransform JSON should be complete, but use the Project anyway.
     * @param theProject - The Project used to reconstruct the RDFTransform (not strictly required).
     * @param jnodeTransform - The complete RDFTransform as JSON.
     * @return RDFTransform
     */
    static public RDFTransform load(Project theProject, JsonNode jnodeTransform) {
        if ( Util.isDebugMode() ) RDFTransform.logger.info("DEBUG: load(): Reconstructing...");
        return RDFTransform.reconstruct(theProject, jnodeTransform);
    }

    static public Vocabulary getVocabFromPrefixNode(Entry<String, JsonNode> entryPrefix) {
        String strPrefix = entryPrefix.getKey();
        String[] astrValues = { null, null, null }; // 0 = Namespace, 1 = Location, 2 = Location Type
        Vocabulary.LocationType theLocType = Vocabulary.LocationType.NONE;
        Vocabulary vocab = null;
        JsonNode nodePrefix = entryPrefix.getValue();

        // If the prefix value is a simple JSON Node, it only has a namespace value (DEPRECATED Old Format)...
        if ( nodePrefix.isValueNode() ) {
            // Upgrade to New Format...
            astrValues[0] = nodePrefix.asText();
            vocab = new Vocabulary( strPrefix, astrValues[0] );
        }
        // Otherwise, process the prefix...
        else {
            nodePrefix.properties().forEach(
                entryPrefixVal -> {
                    String strKey = entryPrefixVal.getKey();
                    String strVal = entryPrefixVal.getValue().asText();
                    if      ( strKey.equals("namespace") )  astrValues[0] = strVal;
                    else if ( strKey.equals("location") )   astrValues[1] = strVal;
                    else if ( strKey.equals("loctype") )    astrValues[2] = strVal;
                }
            );
            if ( astrValues[2] != null && ! astrValues[2].isEmpty() ) theLocType = Vocabulary.fromLocTypeString( astrValues[2] ) ;

            if (astrValues[1] == null)
                vocab = new Vocabulary( strPrefix, astrValues[0], "", theLocType );
            else
                vocab = new Vocabulary( strPrefix, astrValues[0], astrValues[1], theLocType );
        }

        return vocab;
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
        return RDFTransform.reconstruct(null, jnodeRoot);
    }

    static public RDFTransform reconstruct(Project theProject, JsonNode jnodeTransform) {
        // Reset the RDF Transform preferences via the OpenRefine Preference Store
        // as it may have changed since last call...
        Util.setPreferencesByPreferenceStore();

        if ( Util.isVerbose(2) ) RDFTransform.logger.info("Start reconstruction...");
        if ( jnodeTransform == null || jnodeTransform.isNull() || jnodeTransform.isEmpty() ) {
            if ( Util.isVerbose(3) ) RDFTransform.logger.info("  Nothing to reconstruct!");
            return null;
        }
        if ( Util.isDebugJSON() ) RDFTransform.logger.info( "  JSON:\n" + jnodeTransform.toPrettyString() );

        RDFTransform theTransform = new RDFTransform(theProject);

        //
        // JSON Header...
        //
        String strExtension = null;
        if ( jnodeTransform.has(Util.gstrExtension) ) {
            strExtension = jnodeTransform.get(Util.gstrExtension).asText();
            if (strExtension == null) strExtension = "";
        }
        String strVersion = null;
        if ( jnodeTransform.has(Util.gstrVersion) ) {
            strVersion = jnodeTransform.get(Util.gstrVersion).asText();
            if (strVersion == null) strVersion = "";
        }
        if ( Util.isVerbose(2) ) {
            RDFTransform.logger.info("  Version: " + strExtension + " " + strVersion);
            if ( ! strVersion.equals(VERSION) ) {
                RDFTransform.logger.info("    Current Version: " + EXTENSION + " " + VERSION + " will update transform on save.");
            }
        }

        //
        // Construct Base IRI from "baseIRI"...
        //
        if ( jnodeTransform.has(Util.gstrBaseIRI) ) {
            theTransform.setBaseIRI( jnodeTransform.get(Util.gstrBaseIRI) );
        }
        else {
            if ( Util.isVerbose(2) ) RDFTransform.logger.warn("  No Base IRI!  Set to default.");
        }

        //
        // Construct theNamespaces from "namespaces"...
        //
        if ( jnodeTransform.has(Util.gstrNamespaces) ) {
            theTransform.setNamespaces( jnodeTransform.get(Util.gstrNamespaces) );
        }
        else {
            if ( Util.isVerbose(2) ) RDFTransform.logger.warn("  No Namespaces!  Set to default.");
        }

        //
        // Construct listRootNodes from "subjectMappings"...
        //
        if ( jnodeTransform.has(Util.gstrSubjectMappings) ) {
            theTransform.setRoots( jnodeTransform.get(Util.gstrSubjectMappings) );
        }
        else {
            if ( Util.isVerbose(2) ) RDFTransform.logger.warn("  No Subjects!");
        }

        if ( Util.isVerbose(2) ) RDFTransform.logger.info("...ending reconstruction");
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
    private IRI theBaseIRI = null;

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
     *   Namespaces are use to construct Condensed IRI Expressions (CIRIE).
     *   For an IRI:
     *       http://xmlns.com/foaf/0.1/knows
     *   a CIRIE using the above prefix is:
     *       foaf:knows
     */
    @JsonIgnore
    private VocabularyList theNamespaces = null;

    /*
     * Root Nodes for Document
     *
     *  A root node is any subject element designated in the transform.
     */
    @JsonIgnore
    private List<ResourceNode> theRootNodes = null;
    private JsonNode theSubjectMappingsJSON = null;

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

    /**
     * Constructor for deserialization via Jackson
     */
    @JsonCreator
    public RDFTransform()
    {
        if ( Util.isVerbose(2) ) RDFTransform.logger.info("  Reconstructing transform...");

        // Sanity Checks...
        if (RDFTransform.theGlobalContext == null) {
            RDFTransform.logger.error("ERROR: The Global Context is missing!");
            return;
        }

        if ( Util.isVerbose(2) ) RDFTransform.logger.info("  ...created.");
    }

    public RDFTransform(Project theProject) {
        // NOTE:
        //  When the Project is given, we attempt to initialize all the RDFTransform elements for a
        //      new Project.
        //  When the Project is NOT given, we assume a reconstruction is occurring which should have
        //      a complete portrait of a RDFTransform.

        String strSpace = "  ";
        String strBase = "Reconstructing";
        String strContext = "";
        if (theProject != null) {
            strSpace = "";
            strBase = "Creating";
            strContext = " for project " + theProject.id;
        }
        if ( Util.isVerbose(2) ) RDFTransform.logger.info("{}{} transform{}...", strSpace, strBase, strContext);

        // Sanity Checks...
        if (RDFTransform.theGlobalContext == null) {
            RDFTransform.logger.error("ERROR: The Global Context is missing!");
            return;
        }

        if (theProject != null) {
            this.theBaseIRI = Util.buildIRI( RDFTransform.theGlobalContext.getDefaultBaseIRI() );

            this.theNamespaces = RDFTransform.theGlobalContext.getPredefinedVocabularyManager().getPredefinedVocabularies().clone();

            // Copy the index of predefined vocabularies...
            //   Each project will have its own copy of these predefined vocabs to enable, delete, update...
            try {
                RDFTransform.theGlobalContext.getVocabularySearcher().addPredefinedVocabulariesToProject(theProject.id);
            }
            catch (Throwable ex) { // ...try to catch all Exceptions and Errors...
                RDFTransform.logger.error("ERROR: Cannot add predefined vocabularies to transform!", ex);
            }

            this.theRootNodes = new ArrayList<ResourceNode>();
        }

        if ( Util.isVerbose(2) ) RDFTransform.logger.info("{}...created{}.", strSpace, strContext);
    }

    /*
        Methods
    */
    @JsonGetter(Util.gstrExtension)
    public String getExtension() {
        // Return "String" is ok for JSON since it's a single value.
        if ( Util.isVerbose(3) ) RDFTransform.logger.info("Getting Extension...");

        // Ensure all @JsonProperty() getter methods properly handle a null context...

        return RDFTransform.EXTENSION;
    }

    @JsonSetter(Util.gstrExtension)
    public void setExtension(JsonNode jnodeExtension) {
        if ( Util.isVerbose(3) ) RDFTransform.logger.info("Setting Extension from JSON...");
        // Ignore since extension is constant...
        return;
    }

    @JsonGetter(Util.gstrVersion)
    public String getVersion() {
        // Return "String" is ok for JSON since it's a single value.
        if ( Util.isVerbose(3) ) RDFTransform.logger.info("Getting Version...");

        // Ensure all @JsonProperty() getter methods properly handle a null context...

        return RDFTransform.VERSION;
    }

    @JsonSetter(Util.gstrVersion)
    public void setVersion(JsonNode jnodeVersion) {
        if ( Util.isVerbose(3) ) RDFTransform.logger.info("Setting Version from JSON...");
        // Ignore since version is constant...
        return;
    }

    @JsonIgnore // ...see getBaseIRIAsString()
    public IRI getBaseIRI() {
        return this.theBaseIRI;
    }

    @JsonGetter(Util.gstrBaseIRI)
    public String getBaseIRIAsString() {
        // Return "String" is ok for JSON since it's a single value.
        if ( Util.isVerbose(3) ) RDFTransform.logger.info("Getting Base IRI as String...");

        // Ensure all @JsonProperty() getter methods properly handle a null context...

        String strBaseIRI = null;
        if (this.theBaseIRI != null) {
            strBaseIRI = this.theBaseIRI.toString();
        }
        else {
            strBaseIRI = RDFTransform.theGlobalContext.getDefaultBaseIRI(); // ...null context
        }
        if (Util.isDebugMode()) RDFTransform.logger.info("DEBUG: BaseIRI is: " + strBaseIRI);
        return strBaseIRI;
    }

    @JsonIgnore // ...see setBaseIRI(JsonNode)
    public void setBaseIRI(IRI iriBase)  {
        if ( Util.isVerbose(3) ) RDFTransform.logger.info("Setting Base IRI from Parsed IRI...");
        if (Util.isDebugMode()) RDFTransform.logger.info("DEBUG: BaseIRI set to:" + iriBase.toString());
        this.theBaseIRI = iriBase;
    }

    @JsonSetter(Util.gstrBaseIRI)
    public void setBaseIRI(JsonNode jnodeBaseIRI)  {
        if ( Util.isVerbose(3) ) RDFTransform.logger.info("Setting Base IRI from JSON...");

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

        IRI iriBase = null;
        String strBaseIRI = null;
        if ( jnodeBaseIRI == null || jnodeBaseIRI.isNull() ) {
            // Get Default BaseIRI...
            if (Util.isDebugMode()) RDFTransform.logger.info("DEBUG: setBaseIRI(): No BaseIRI.");
            iriBase = null;
        }
        else if ( jnodeBaseIRI.isTextual() ) {
            // Get the Base IRI string...
            strBaseIRI = jnodeBaseIRI.textValue();
            if (Util.isDebugMode()) RDFTransform.logger.info("DEBUG: setBaseIRI(): BaseIRI JSON Value Text: " + strBaseIRI);
            iriBase = Util.buildIRI( strBaseIRI );
        }
        else if ( jnodeBaseIRI.isObject() && ! jnodeBaseIRI.isEmpty() ) {
            // Get the Base IRI from components...
            if (Util.isDebugMode()) RDFTransform.logger.info("DEBUG: setBaseIRI(): BaseIRI JSON extracting components...");

            String  strScheme   = jnodeBaseIRI.get("scheme"  ).asText();
            String  strUserInfo = jnodeBaseIRI.get("userInfo").asText();
            String  strHost     = jnodeBaseIRI.get("host"    ).asText();
            int     iPort       = jnodeBaseIRI.get("port"    ).asInt(-1);
            String  strPath     = jnodeBaseIRI.get("path"    ).asText();
            String  strQuery    = jnodeBaseIRI.get("query"   ).asText();
            String  strFragment = jnodeBaseIRI.get("fragment").asText();

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
        }

        if (iriBase != null) {
            this.theBaseIRI = iriBase;
            if ( Util.isDebugMode() ) RDFTransform.logger.info("DEBUG: setBaseIRI(): BaseIRI set to: " + strBaseIRI);
        }
        else {
            this.theBaseIRI = Util.buildIRI( RDFTransform.theGlobalContext.getDefaultBaseIRI() );
            if ( Util.isDebugMode() ) RDFTransform.logger.info("DEBUG: setBaseIRI(): BaseIRI set to default.");
        }
    }

    @JsonIgnore
    public VocabularyList getNamespaces() {
        if ( Util.isVerbose(3) ) RDFTransform.logger.info("Getting Namespaces...");
        return this.theNamespaces;
    }

    @JsonGetter(Util.gstrNamespaces)
    public JsonNode getNamespacesAsJSON() {
        // Return must be "JsonNode" for JSON since it's an array!
        if ( Util.isVerbose(3) ) RDFTransform.logger.info("Getting Namespaces as JSON...");

        // Ensure all @JsonProperty() getter methods properly handle a null context...

        if ( this.theNamespaces == null || this.theNamespaces.isEmpty() ) {
            return NullNode.getInstance(); // ...null context
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode objnodeNamespaces = mapper.createObjectNode();
        for (Vocabulary thePrefix : this.theNamespaces) {
            objnodeNamespaces.put( thePrefix.getPrefix(), thePrefix.getNamespace() );
        }
        return objnodeNamespaces;
    }

    @JsonIgnore
    public void setNamespaces(VocabularyList listNamespaces) {
        this.theNamespaces = listNamespaces;
    }

    @JsonSetter(Util.gstrNamespaces)
    public void setNamespaces(JsonNode jnodeNamespaces) {
        if ( Util.isVerbose(3) ) RDFTransform.logger.info("Setting Namespaces from JSON...");
        if (this.theNamespaces == null) {
            this.theNamespaces = new VocabularyList();
        }
        synchronized(this.theNamespaces) {
            if ( jnodeNamespaces == null || jnodeNamespaces.isNull() || jnodeNamespaces.isEmpty() ) {
                if (Util.isDebugMode()) RDFTransform.logger.info("DEBUG: setNamespaces(): No Namespaces.");
                return;
            }
            this.theNamespaces.clear();

            if ( Util.isDebugJSON() ) RDFTransform.logger.info("  Namespaces:\n" + jnodeNamespaces.toPrettyString());

            if ( jnodeNamespaces.isObject() ) {
                if ( Util.isDebugMode() ) RDFTransform.logger.info("DEBUG: setNamespaces(): Namespaces set by JSON Object...");
                jnodeNamespaces.properties().forEach(
                    entryPrefix -> {
                        Vocabulary vocab = RDFTransform.getVocabFromPrefixNode(entryPrefix);
                        if (vocab != null) this.theNamespaces.add(vocab);
                    }
                );
            }
            if ( this.theNamespaces.isEmpty() ) {
                if ( Util.isDebugMode() ) RDFTransform.logger.info("DEBUG: setNamespaces(): Namespaces set by Predefined defaults...");
                this.theNamespaces =
                    RDFTransform.theGlobalContext.
                        getPredefinedVocabularyManager().
                            getPredefinedVocabularies().clone();
            }
            this.processRootNodes();
        }
    }

    @JsonIgnore
    public void addNamespace(String strPrefix, String strNamespace, String strLocation, Vocabulary.LocationType theLocType) {
        if (this.theNamespaces == null) {
            this.theNamespaces = new VocabularyList();
        }
        if ( strLocation == null || strLocation.isEmpty() ) {
            strLocation = "";
            theLocType = Vocabulary.LocationType.NONE;
        }
        this.theNamespaces.add( new Vocabulary(strPrefix, strNamespace, strLocation, theLocType) );
    }

    @JsonIgnore
    public boolean removeNamespace(String strPrefix) {
        if ( Util.isDebugMode() ) RDFTransform.logger.info("DEBUG: removeNamespace(): by prefix[" + strPrefix + "]");
        return this.theNamespaces.removeByPrefix(strPrefix);
    }

    @JsonIgnore
    public List<ResourceNode> getRoots() {
        if ( Util.isVerbose(3) ) RDFTransform.logger.info("Getting root nodes: size = " + this.theRootNodes.size());
        if ( this.theRootNodes.isEmpty() )
            RDFTransform.logger.warn("WARNING: No RDF Transform found! Select the \"RDT Transform\" extension and save.");
        return this.theRootNodes;
    }

    @JsonGetter(Util.gstrSubjectMappings)
    public JsonNode getRootsAsJSON() {
        if ( Util.isVerbose(3) ) RDFTransform.logger.info("Getting Subject Mappings as JSON...");

        // Ensure all @JsonProperty() getter methods properly handle a null context...

        if ( this.theRootNodes == null || this.theRootNodes.isEmpty() ) {
            return NullNode.getInstance(); // ...null context
        }

        ByteArrayOutputStream baostream = new ByteArrayOutputStream();
        try {
            JsonGenerator jsonWriter = ParsingUtilities.mapper.getFactory().createGenerator(baostream);
            jsonWriter.writeStartArray();
            for (Node nodeRoot : this.theRootNodes) {
                nodeRoot.write(jsonWriter, true);
            }
            jsonWriter.writeEndArray();
            jsonWriter.flush();
            jsonWriter.close();
            String strJSON = baostream.toString("UTF-8");
            JsonNode jnodeTransform = ParsingUtilities.evaluateJsonStringToArrayNode(strJSON);
            if ( Util.isDebugJSON() ) RDFTransform.logger.info("DEBUG JSON: getRootsAsJSON():\n" + jnodeTransform.toPrettyString());
            return jnodeTransform;
        }
        catch (Exception ex) {
            RDFTransform.logger.error("ERROR: Cannot get root nodes!", ex);
            if ( Util.isVerbose() ) ex.printStackTrace();
        }
        return NullNode.getInstance(); // ...null context
    }

    @JsonIgnore
    public void setRoots(List<ResourceNode> listRootNodes) {
        if ( Util.isVerbose(3) ) RDFTransform.logger.info("Setting root nodes...");
        this.theRootNodes = listRootNodes;
    }

    @JsonSetter(Util.gstrSubjectMappings)
    public void setRoots(JsonNode jnodeSubjectMappings) {
        if ( Util.isVerbose(3) ) RDFTransform.logger.info("Setting Subject Mappings from JSON...");
        if (this.theRootNodes == null) {
            if ( Util.isDebugMode() ) RDFTransform.logger.info("DEBUG: setRoots(): Root nodes initialized.");
            this.theRootNodes = new ArrayList<ResourceNode>();
        }
        synchronized(this.theRootNodes) {
            if ( jnodeSubjectMappings == null || jnodeSubjectMappings.isNull() || jnodeSubjectMappings.isEmpty() ) {
                if ( Util.isDebugMode() ) RDFTransform.logger.info("DEBUG: setRoots(): No Subject Mappings.");
                return;
            }
            this.theSubjectMappingsJSON = jnodeSubjectMappings;
            this.processRootNodes();
        }
    }

    @JsonIgnore
    private void processRootNodes() {
        if (this.theNamespaces == null || this.theSubjectMappingsJSON == null) return;

        List<ResourceNode> listRootNodes = new ArrayList<ResourceNode>();
        if ( this.theSubjectMappingsJSON.isArray() ) {
            if ( Util.isVerbose(3) ) RDFTransform.logger.info(" Processing Subject Mappings Array...");
            for (JsonNode jnodeSubject : this.theSubjectMappingsJSON) {
                Node nodeRoot =
                    Node.reconstructNode(
                        RDFTransform.theReconstructor, jnodeSubject,
                        this.theBaseIRI, this.theNamespaces);
                if ( Util.isDebugMode() ) RDFTransform.logger.info("DEBUG: processRootNodes(): Root Node reconstructed.");
                if (nodeRoot != null && nodeRoot instanceof ResourceNode) {
                    listRootNodes.add( (ResourceNode) nodeRoot );
                    if ( Util.isDebugMode() ) RDFTransform.logger.info("DEBUG: processRootNodes(): Root Node added.");
                }
                // Otherwise, non-resource nodes (literals, generic nodes) cannot be root nodes.
                // So, skip them.  They should never have been in the root node list anyway.
                else {
                    if ( Util.isDebugMode() ) RDFTransform.logger.info("DEBUG: processRootNodes(): Root Node not a Resource.");
                }
            }
        }
        this.theRootNodes = listRootNodes;
        this.theSubjectMappingsJSON = null;
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
        if ( Util.isVerbose(3) ) RDFTransform.logger.info("Writing JSON...");
        try {
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
            for ( Vocabulary vocab : this.theNamespaces ) {
                vocab.write(theWriter);
            }
            theWriter.writeEndObject();

            //
            // The Subject Mappings (subjectMappings) array...
            //
            theWriter.writeArrayFieldStart(Util.gstrSubjectMappings);
            for (Node nodeRoot : this.theRootNodes) {
                nodeRoot.write(theWriter, true);
            }
            theWriter.writeEndArray();

            theWriter.writeEndObject();

            theWriter.flush();
        }
        catch (Exception ex) {
            RDFTransform.logger.error("ERROR: Cannot write JSON!", ex);
        }
    }
}
