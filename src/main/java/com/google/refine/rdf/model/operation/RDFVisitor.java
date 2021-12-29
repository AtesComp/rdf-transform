package com.google.refine.rdf.model.operation;

import java.util.Collection;

import com.google.refine.model.Project;

import com.google.refine.rdf.RDFTransform;
import com.google.refine.rdf.model.Util;
import com.google.refine.rdf.model.vocab.Vocabulary;
import com.google.refine.browsing.Engine;

import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RDFVisitor {
    private final static Logger logger = LoggerFactory.getLogger("RDFT:RDFVisitor");

    private final RDFTransform transform;
    private final RDFWriter writer;
    private final SailRepository model;
    private final SailRepositoryConnection connection;

    public RDFVisitor(RDFTransform theTransform, RDFWriter theWriter) {
        this.transform = theTransform;
        this.writer = theWriter;

        // Initializing repository...
        this.model = new SailRepository( new MemoryStore() );
        this.model.init();
        this.connection = this.model.getConnection();

        //
        // Populate the namespaces in the repository...
        //

        // Set Base Namespace...
        String strBaseIRI = this.transform.getBaseIRIAsString();
        if ( ! strBaseIRI.isEmpty() ) {
            this.connection.setNamespace("", strBaseIRI);
        }
        // Set Prefix Namespaces...
        Collection<Vocabulary> collVocab = this.transform.getPrefixes();
        for (Vocabulary vocab : collVocab) {
            this.connection.setNamespace( vocab.getPrefix(), vocab.getNamespace() );
        }
    }

    public RDFTransform getRDFTransform() {
        return this.transform;
    }

    public Repository getModel() {
        return this.model;
    }

    abstract public void buildModel(Project theProject, Engine theEngine);

    public void start(Project theProject) {
        if ( Util.isVerbose(3) ) logger.info("Starting Visitation...");

        try {
            // Export namespace information previously populated in the repository...
            RepositoryResult<Namespace> nsIter = this.connection.getNamespaces();
            try {
                while ( nsIter.hasNext() ) {
                    Namespace ns = nsIter.next();
                    this.writer.handleNamespace( ns.getPrefix(), ns.getName() );
                    if ( Util.isDebugMode() ) logger.info("DEBUG: Prefix: " + ns.getPrefix() + " : " + ns.getName());
                }
            }
            finally {
                nsIter.close();
            }
        }
        catch (RepositoryException ex) {
            throw new RuntimeException("", ex);
        }
        catch (RDFHandlerException ex) {
            ex.printStackTrace();
        }
    }

    public void end(Project theProject) {
        if ( Util.isVerbose(3) ) logger.info("...Ending Visitation");

        try {
            if (this.connection.isOpen()) {
                this.connection.close();
            }
        }
        catch (RepositoryException ex) {
            throw new RuntimeException("", ex);
        }
        catch (RDFHandlerException ex) {
            throw new RuntimeException("", ex);
        }
    }

    protected void flushStatements() throws RepositoryException, RDFHandlerException {
        // TODO: Reserve this commented code for future context upgrade (quads)
        //List<Resource> resourceList = Iterations.asList( this.connection.getContextIDs() );
        //Resource[] resources = resourceList.toArray( new Resource[ resourceList.size() ] );

        // Export statements...
        RepositoryResult<Statement> stmtIter =
            //this.connection.getStatements(null, null, null, false, resources);
            this.connection.getStatements(null, null, null, false);

        try {
            while ( stmtIter.hasNext() ) {
                this.writer.handleStatement( stmtIter.next() );
            }
        }
        finally {
            stmtIter.close();
        }

        // Empty the repository...
        this.connection.clear();
    }
}
