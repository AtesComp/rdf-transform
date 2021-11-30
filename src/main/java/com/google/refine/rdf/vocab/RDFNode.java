package com.google.refine.rdf.vocab;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerationException;

import java.util.Properties;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RDFNode {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:RDFNode");

    public static final int
        IRI       = 0,
        Label     = 1,
        Desc      = 2,
        Prefix    = 3,
        Namespace = 4,
        LocalPart = 5;

    private String strIRI;
    private String strLabel;
    private String strDescription;
    private String strPrefix;
    private String strNamespace;
    private String strLocalPart;

    public RDFNode() {}

    public RDFNode(String strIRI) {
        this.strIRI         = strIRI;
        this.strLabel       = strIRI;
        this.strDescription = null;
        this.strPrefix      = null;
        this.strNamespace   = null;
        this.strLocalPart   = null;
        this.extractLocalPart();
    }

    public RDFNode(String[] astrLoader) {
        this.load(astrLoader);
    }

    public void load(String[] astrLoader) {
        this.strIRI         = astrLoader[IRI];
        this.strLabel       = astrLoader[Label];
        this.strDescription = astrLoader[Desc];
        this.strPrefix      = astrLoader[Prefix];
        this.strNamespace   = astrLoader[Namespace];
        this.strLocalPart   = astrLoader[LocalPart];
        if ( this.strLabel.isEmpty() ) {
            this.strLabel = this.strIRI;
        }
        if ( this.strLocalPart.isEmpty() ) {
            this.extractLocalPart();
        }
        if ( this.strNamespace.isEmpty() ) {
            // ...when LocalPart was not empty, but Namespace is...
            //
            //  Otherwise, if still empty after extractLocalPart(),
            //      then the IRI must be empty or
            //           the split was at the 0 index
            //           and there is nothing to do!
            if ( ! this.strLocalPart.isEmpty() ) {
                splitNamespaceAndLocalPart(false);
            }
        }
    }

    private void extractLocalPart() {
        if ( ! this.strIRI.isEmpty() ) {
            if ( ! this.strNamespace.isEmpty() ) {
                int iIndex = this.strNamespace.length();
                if (this.strIRI.substring(0, iIndex) == this.strNamespace) {
                    this.strLocalPart = strIRI.substring(iIndex);
                }
                else {
                    // Big Problem: Namespace is not a starting substring of IRI!
                    // Since Namespace is set by others, we can't change it.
                    // Also, the Prefix may be related to the Namespace.
                    //
                    // Report an error and continue...
                    logger.error(
                        "ERROR: extractLocalPart(): Namespace is not in IRI!\n" +
                        "             IRI: " + this.strIRI + "\n" +
                        "       Namespace: " + this.strNamespace
                    );
                }
            }
            else { // ...this.strNamespace is empty, so set it too...
                splitNamespaceAndLocalPart(true);
            }
        }
    }

    private void splitNamespaceAndLocalPart(boolean bSetLocalPart) {
        // From RDF4J Interface IRI documentation:
        // --------------------------------------------------------------------------------
        // An IRI can be split into a namespace part and a local name part, which are
        // derived from an IRI string by splitting it in two using the following algorithm:
        //
        // * Split after the first occurrence of the '#' character,
        // * If this fails, split after the last occurrence of the '/' character,
        // * If this fails, split after the last occurrence of the ':' character.
        //
        // The last step should never fail as every legal (full) IRI contains at least one
        // ':' character to separate the scheme from the rest of the IRI. The
        // implementation should check this upon object creation.
        if ( ! this.strIRI.isEmpty() ) {
            int iIndex = this.strIRI.indexOf('#');
            if (iIndex == -1) {
                iIndex = this.strIRI.lastIndexOf('/');
                if (iIndex == -1) {
                    iIndex = this.strIRI.lastIndexOf(':');
                }
            }
            if (iIndex != -1) { // ...found a split
                ++iIndex; // ...split after
                // Namespace includes the character...
                this.strNamespace = this.strIRI.substring(0, iIndex);
                if (bSetLocalPart) {
                    this.strLocalPart = this.strIRI.substring(iIndex);
                }
            }
        }
    }

    public String getIRI() {
        return this.strIRI;
    }
    // public void setIRI(String strIRI) {
    //     this.strIRI = strIRI;
    // }

    public String getLabel() {
        return this.strLabel;
    }
    // public void setLabel(String strLabel) {
    //     this.strLabel = strLabel;
    // }

    public String getDescription() {
        return this.strDescription;
    }
    // public void setDescription(String strDescription) {
    //     this.strDescription = strDescription;
    // }

    public String getPrefix() {
        return this.strPrefix;
    }
    // public void setPrefix(String strPrefix) {
    //     this.strPrefix = strPrefix;
    // }

    public String getNamespace() {
        return this.strNamespace;
    }
    // public void setNamespace(String strNamespace) {
    //     this.strNamespace = strNamespace;
    // }

    public String getLocalPart() {
        return this.strLocalPart;
    }
    // public void setLocalPart(String strLocalPart) {
    //     this.strLocalPart = strLocalPart;
    // }

    public String getType() {
    	//throw new UnsupportedOperationException();
        return this.getClass().getSimpleName();
    }

    public void write(JsonGenerator writer, Properties options)
            throws JsonGenerationException, IOException {
        writer.writeStartObject();
        writer.writeStringField("type",        this.getType());
        writer.writeStringField("iri",         this.strIRI);
        writer.writeStringField("label",       this.strLabel);
        writer.writeStringField("description", this.strDescription);
        writer.writeStringField("prefix",      this.strPrefix);
        writer.writeStringField("namespace",   this.strNamespace);
        writer.writeStringField("localPart",   this.strLocalPart);
        writer.writeEndObject();
    }

    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof RDFNode) ) return false;
        RDFNode node = (RDFNode) obj;
        if (node.getIRI() == null || this.strIRI == null) {
            return false;
        }
        return this.strIRI.equals( node.getIRI() );
    }

    @Override
    public int hashCode() {
        return this.strIRI.hashCode();
    }

}
