package org.openrefine.rdf.command;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.ClientSideResourceManager;
import com.google.refine.RefineServlet;
import com.google.refine.commands.Command;
import com.google.refine.exporters.Exporter;
import com.google.refine.exporters.ExporterRegistry;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.model.Project;
import com.google.refine.operations.OperationRegistry;

import org.openrefine.rdf.ApplicationContext;
import org.openrefine.rdf.RDFTransform;
import org.openrefine.rdf.model.exporter.RDFPrettyExporter;
import org.openrefine.rdf.model.exporter.RDFStreamExporter;
import org.openrefine.rdf.model.expr.RDFTransformBinder;
import org.openrefine.rdf.model.expr.functions.ToIRIString;
import org.openrefine.rdf.model.expr.functions.ToStrippedLiteral;
import org.openrefine.rdf.model.Util;
import org.openrefine.rdf.model.operation.RDFTransformChange;
import org.openrefine.rdf.model.operation.SaveRDFTransformOperation;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.sys.JenaSystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.simile.butterfly.ButterflyModule;

public class InitializationCommand extends Command {
    static private final Logger logger = LoggerFactory.getLogger("RDFT:InitCmd");

    private ButterflyModule theModule;
    private ApplicationContext theContext;

    public InitializationCommand(ButterflyModule theModule) {
        super();
        this.theModule = theModule;
        //String strJVMVersion = System.getProperty("java.version");
        //InitializationCommand.logger.info("Current Java VM Version: " + strJVMVersion);
        this.initialize();
    }

    private void initialize() {
        InitializationCommand.logger.info("Initializing RDF Transform Extension " + RDFTransform.VERSION + "...");
        InitializationCommand.logger.info("  Ext Mount Point: " + this.theModule.getMountPoint() );

        InitializationCommand.logger.info("  Client Side...");
        this.registerClientSide();

        InitializationCommand.logger.info("  Server Side...");
        this.registerServerSide();

        InitializationCommand.logger.info("  Preferences...");
        InitializationCommand.logger.info( Util.preferencesToString() );

        InitializationCommand.logger.info("...RDF Transform Extension initialized.");
    }

    private void registerClientSide() {
        //
        //  Client-side Resources...
        // ------------------------------------------------------------

        //
        // Client-side Javascript...
        //
         String[] astrScripts = new String[] {
            "scripts/rdf-transform-menubar-extensions.js",    // 1. must be first: language and menu load
            "scripts/rdf-transform-common.js",
            "scripts/rdf-transform.js",
            "scripts/rdf-transform-resource.js",
            "scripts/rdf-transform-ui-node.js",
            "scripts/rdf-transform-ui-node-config.js",
            "scripts/rdf-transform-ui-property.js",
            "scripts/rdf-transform-vocab-manager.js",
            "scripts/rdf-transform-namespaces-manager.js",
            "scripts/rdf-transform-namespace-adder.js",
            "scripts/rdf-transform-suggest-term.js",
            "scripts/rdf-transform-import-template.js",
            "scripts/rdf-transform-export-template.js",
            "scripts/rdf-data-table-view.js",
            // JQuery is required and provided by OpenRefine.
            // Add the JQuery Form plugin...
            "scripts/externals/jquery.form.min.js"
        };
        // Inject script files into /project page...
        ClientSideResourceManager.addPaths("project/scripts", this.theModule, astrScripts);

        //
        // Client-side CSS...
        //
        String[] astrStyles = new String[] {
            "styles/flyout.css",
            "styles/rdf-transform-dialog.css",
        };
        // Inject style files into /project page...
        ClientSideResourceManager.addPaths("project/styles", this.theModule, astrStyles);
    }

    private void registerServerSide() {
        //
        //  Server-side Resources...
        // ------------------------------------------------------------

        /*
         *  Server-side Context Initialization...
         *    The One and Only RDF Transform Application Context.
         */
        this.theContext = new ApplicationContext();

        RDFTransform.setGlobalContext(this.theContext);

        /*
         *  Server-side Ajax Commands...
         *    Each registration calls the class' init() method.
         */
        String strSaveRDFTransform = "save-rdf-transform";
        RefineServlet.registerCommand( this.theModule, "initialize", this);
        RefineServlet.registerCommand( this.theModule, "get-preferences",         new PreferencesCommand() );
        RefineServlet.registerCommand( this.theModule, "preview-rdf",             new PreviewRDFCommand() );
        RefineServlet.registerCommand( this.theModule, "preview-rdf-expression",  new PreviewRDFTExpressionCommand() );
        RefineServlet.registerCommand( this.theModule, strSaveRDFTransform,       new SaveRDFTransformCommand() );
        RefineServlet.registerCommand( this.theModule, "save-baseIRI",            new SaveBaseIRICommand() );
        RefineServlet.registerCommand( this.theModule, "validate-iri",            new ValidateIRICommand() );
        RefineServlet.registerCommand( this.theModule, "convert-to-iri",          new ToIRICommand() );
        // Vocabs commands
        RefineServlet.registerCommand( this.theModule, "get-default-namespaces",  new NamespacesGetDefaultCommand() );
        RefineServlet.registerCommand( this.theModule, "save-namespaces",         new NamespacesSaveCommand() );
        RefineServlet.registerCommand( this.theModule, "add-namespace",           new NamespaceAddCommand() );
        RefineServlet.registerCommand( this.theModule, "add-namespace-from-file", new NamespaceAddFromFileCommand() );
        RefineServlet.registerCommand( this.theModule, "refresh-prefix",          new NamespaceRefreshCommand() );
        RefineServlet.registerCommand( this.theModule, "remove-prefix",           new NamespaceRemoveCommand() );
        RefineServlet.registerCommand( this.theModule, "suggest-namespace",       new SuggestNamespaceCommand() );
        RefineServlet.registerCommand( this.theModule, "suggest-term",            new SuggestTermCommand() );
        RefineServlet.registerCommand( this.theModule, "add-suggest-term",        new SuggestTermAddCommand() );
        // Others:
        //   CodeResponse - Standard Response Class for Commands
        //   RDFTransformCommand - Abstract RDF Command Class

        /*
         *  Server-side Custom Change Class...
         */
        RefineServlet.registerClassMapping(
            // Non-existent name--we are adding, not renaming, so this can be a dummy...
            "com.google.refine.model.changes.DataExtensionChange",
            // Added Change Class name...
            "org.openrefine.rdf.model.operation.RDFTransformChange"
        );
        RefineServlet.cacheClass(RDFTransformChange.class);

        /*
         *  Server-side Operations...
         */
        OperationRegistry.registerOperation(
            this.theModule, strSaveRDFTransform, SaveRDFTransformOperation.class
        );

        /*
         *  Server-side GREL Functions and Binders...
         */
        ControlFunctionRegistry.registerFunction( "toIRIString", new ToIRIString() );
        ControlFunctionRegistry.registerFunction( "toStrippedLiteral", new ToStrippedLiteral() );

        ExpressionUtils.registerBinder( new RDFTransformBinder() );

        /*
         *  Server-side Exporters...
         */

        //
        // PRETTY PRINTERS: (Graph) *** Not suggested for large graphs ***
        //
        ExporterRegistry.registerExporter( "RDFXML_PRETTY", new RDFPrettyExporter(RDFFormat.RDFXML_PRETTY, "RDFXML_PRETTY") );
        ExporterRegistry.registerExporter( "TURTLE_PRETTY", new RDFPrettyExporter(RDFFormat.TURTLE_PRETTY, "TURTLE_PRETTY") );
        ExporterRegistry.registerExporter( "TRIG_PRETTY", new RDFPrettyExporter(RDFFormat.TRIG_PRETTY, "TRIG_PRETTY") );
        ExporterRegistry.registerExporter( "JSONLD_PRETTY", new RDFPrettyExporter(RDFFormat.JSONLD_PRETTY, "JSONLD_PRETTY") );
        //ExporterRegistry.registerExporter( "NDJSONLD_PRETTY", new RDFPrettyExporter(NDJSONLD, "NDJSONLD_PRETTY") ); // RDF4J NewLine Delimited JSONLD
        ExporterRegistry.registerExporter( "RDFJSON", new RDFPrettyExporter(RDFFormat.RDFJSON, "RDFJSON") );

        //
        // STREAM PRINTERS:
        //
        // BLOCKS PRINTERS: per Subject (Stream)
        ExporterRegistry.registerExporter( "TURTLE_BLOCKS", new RDFStreamExporter(RDFFormat.TURTLE_BLOCKS, "TURTLE_BLOCKS") );
        ExporterRegistry.registerExporter( "TRIG_BLOCKS", new RDFStreamExporter(RDFFormat.TRIG_BLOCKS, "TRIG_BLOCKS") );
        // LINE PRINTERS: triple, quad (Stream)
        ExporterRegistry.registerExporter( "NTRIPLES", new RDFStreamExporter(RDFFormat.NTRIPLES_UTF8, "NTRIPLES") );
        ExporterRegistry.registerExporter( "NQUADS", new RDFStreamExporter(RDFFormat.NQUADS_UTF8, "NQUADS") );
        ExporterRegistry.registerExporter( "TRIX", new RDFStreamExporter(RDFFormat.TRIX, "TRIX") );
        // DUMMY PRINTERS: (Stream)
        ExporterRegistry.registerExporter( "RDFNULL", new RDFStreamExporter(RDFFormat.RDFNULL, "RDFNULL") );
        // BINARY PRINTERS: (Stream)
        ExporterRegistry.registerExporter( "RDF_PROTO", new RDFStreamExporter(RDFFormat.RDF_PROTO, "RDF_PROTO") );
        ExporterRegistry.registerExporter( "RDF_THRIFT", new RDFStreamExporter(RDFFormat.RDF_THRIFT, "RDF_THRIFT") );
        //ExporterRegistry.registerExporter( "BinaryRDF", new RDFExporter(BINARY, "BinaryRDF") ); // RDF4J
        //ExporterRegistry.registerExporter( "HDT", new RDFExporter(HDT, "HDT") );
        // TODO: Special RDFExporters - Are these even doable???
        //ExporterRegistry.registerExporter( "RDFa", new RDFExporter(RDFA, "RDFa") );
        //ExporterRegistry.registerExporter( "SHACLC", new RDFExporter(RDFFormat.SHACLC, "SHACLC") );

        /*
         *  Server-side Overlay Models - Attach an RDFTransform object to the project...
         */
        Project.registerOverlayModel("RDFTransform", RDFTransform.class);

        // Test Exporter Registry...
        Exporter exporter = ExporterRegistry.getExporter("RDF_PROTO");
        if (exporter == null) {
            InitializationCommand.logger.error("ERROR: ExporterRegistry test failed!");
        }
        else {
            InitializationCommand.logger.error("SUCCESS: ExporterRegistry test succeeded!");
        }
    }

    @Override
    public void init(RefineServlet servlet) {
        InitializationCommand.logger.info("Initializing...");
        super.init(servlet);

        // Set the RDF Transform preferences via the OpenRefine Preference Store...
        Util.setPreferencesByPreferenceStore();

        // From refine.ini (or defaults)...
        String strHost =  System.getProperty("refine.host");
        if (strHost == null)
            strHost = "localhost"; // Default
        String strIFace = System.getProperty("refine.iface");
        if (strIFace == null)
            strIFace = ""; // Default
        String strPort =  System.getProperty("refine.port");
        if (strPort == null)
            strPort = "3333"; // Default
        File fileWorkingDir = servlet.getCacheDir(RDFTransform.KEY);

        try {
            this.theContext.init(strHost, strIFace, strPort, fileWorkingDir);
        }
        catch (IOException ex) {
            InitializationCommand.logger.error("ERROR: App Context Init: " + ex.getMessage(), ex);
            if ( Util.isVerbose() || Util.isDebugMode() ) ex.printStackTrace();
        }

        if ( Util.isDebugMode() ) JenaSystem.DEBUG_INIT = true;
        JenaSystem.init();
        InitializationCommand.logger.info("...Apache Jena initialized.");

        InitializationCommand.logger.info("...initialized.");
    }

    //
    // DUMMY OVERRIDES ================
    //      The InitializationCommand is s dummy Command class that does not actually process
    //      POST or GET requests.  Instead, it absorbs all the "controller.js" server-side
    //      JavaScript functionality in a more performant code processor...Java
    //
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        this.doGet(request, response);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        throw new UnsupportedOperationException("This command is not meant to be called. It is just necessary for initialization.");
    }
}
