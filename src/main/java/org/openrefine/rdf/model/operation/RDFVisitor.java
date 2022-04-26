package org.openrefine.rdf.model.operation;

import java.util.Collection;

import com.google.refine.model.Project;

import org.openrefine.rdf.RDFTransform;
import org.openrefine.rdf.model.Util;
import org.openrefine.rdf.model.vocab.Vocabulary;
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

    private final RDFTransform theTransform;
    private final RDFWriter theWriter;
    private final SailRepository theModel;
    private final SailRepositoryConnection theConnection;

    public RDFVisitor(RDFTransform theTransform, RDFWriter theWriter) {
        this.theTransform = theTransform;
        this.theWriter = theWriter;

        // Initializing repository...
        this.theModel = new SailRepository( new MemoryStore() );
        this.theModel.init();
        this.theConnection = this.theModel.getConnection();

        //
        // Populate the namespaces in the repository...
        //

        // Prepare Namespaces...
        String strBaseIRI = this.theTransform.getBaseIRIAsString();
        Collection<Vocabulary> collVocab = this.theTransform.getNamespaces();

        // Check for the BaseIRI (default namespace) in the Prefixed Namespaces...
        boolean bUseBaseIRI = true; // ...default: use the BaseIRI
        for (Vocabulary vocab : collVocab) {
            // If the BaseIRI is in the Prefixed Namespace...
            if ( vocab.getNamespace().equals(strBaseIRI) ) {
                bUseBaseIRI = false; // ...don't use the BaseIRI!
                break;
            }
        }

        // Set Default Namespace for repository...
        if ( bUseBaseIRI && ! strBaseIRI.isEmpty() ) {
            if ( Util.isDebugMode() ) RDFVisitor.logger.info("DEBUG: Using BaseIRI");
            this.theConnection.setNamespace("", strBaseIRI);
        }
        else {
            if ( Util.isDebugMode() ) RDFVisitor.logger.info("DEBUG: Not using BaseIRI");
        }

        // Set Prefix Namespaces for repository...
        for (Vocabulary vocab : collVocab) {
            this.theConnection.setNamespace( vocab.getPrefix(), vocab.getNamespace() );
        }
    }

    public RDFTransform getRDFTransform() {
        return this.theTransform;
    }

    public Repository getModel() {
        return this.theModel;
    }

    abstract public void buildModel(Project theProject, Engine theEngine);

    public void start(Project theProject) {
        if ( Util.isVerbose(3) ) RDFVisitor.logger.info("Starting Visitation...");

        try {
            // Export namespace information previously populated in the repository...
            RepositoryResult<Namespace> nsIter = this.theConnection.getNamespaces();
            try {
                while ( nsIter.hasNext() ) {
                    Namespace ns = nsIter.next();
                    this.theWriter.handleNamespace( ns.getPrefix(), ns.getName() );
                    if ( Util.isDebugMode() ) RDFVisitor.logger.info("DEBUG: Prefix: " + ns.getPrefix() + " : " + ns.getName());
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
            RDFVisitor.logger.error("ERROR: Visiting rows: ", ex);
            if ( Util.isVerbose() || Util.isDebugMode() ) ex.printStackTrace();
        }
    }

    public void end(Project theProject) {
        if ( Util.isVerbose(3) ) RDFVisitor.logger.info("...Ending Visitation");

        try {
            if (this.theConnection.isOpen()) {
                this.theConnection.close();
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
        //List<Resource> resourceList = Iterations.asList( this.theConnection.getContextIDs() );
        //Resource[] resources = resourceList.toArray( new Resource[ resourceList.size() ] );

        // Export statements...
        RepositoryResult<Statement> stmtIter =
            //this.theConnection.getStatements(null, null, null, false, resources);
            this.theConnection.getStatements(null, null, null, false);

        try {
            while ( stmtIter.hasNext() ) {
                this.theWriter.handleStatement( stmtIter.next() );
            }
        }
        finally {
            stmtIter.close();
        }

        // Empty the repository...
        this.theConnection.clear();
    }
}
