package org.openrefine.rdf.command;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
        class RDFTCommandItem
        {
            public String strCommand;
            public Command command;
            RDFTCommandItem(String strCmd, Command cmd) {
                strCommand = strCmd;
                command = cmd;
            }
        };
        String strSaveRDFTransform = "save-rdf-transform"; // ...also for operation registry later

        List<RDFTCommandItem> aCommands = new ArrayList<RDFTCommandItem>();
        aCommands.add(new RDFTCommandItem( "initialize", this ));
        aCommands.add(new RDFTCommandItem( "get-preferences", new PreferencesCommand() ));
        aCommands.add(new RDFTCommandItem( "preview-rdf", new PreviewRDFCommand() ));
        aCommands.add(new RDFTCommandItem( "preview-rdf-expression", new PreviewRDFTExpressionCommand() ));
        aCommands.add(new RDFTCommandItem( strSaveRDFTransform, new SaveRDFTransformCommand() ));
        aCommands.add(new RDFTCommandItem( "save-baseIRI", new SaveBaseIRICommand() ));
        aCommands.add(new RDFTCommandItem( "validate-iri", new ValidateIRICommand() ));
        aCommands.add(new RDFTCommandItem( "convert-to-iri", new ToIRICommand() ));
        // Vocabs commands
        aCommands.add(new RDFTCommandItem( "get-default-namespaces", new NamespacesGetDefaultCommand() ));
        aCommands.add(new RDFTCommandItem( "save-namespaces", new NamespacesSaveCommand() ));
        aCommands.add(new RDFTCommandItem( "add-namespace", new NamespaceAddCommand() ));
        aCommands.add(new RDFTCommandItem( "add-namespace-from-file", new NamespaceAddFromFileCommand() ));
        aCommands.add(new RDFTCommandItem( "refresh-prefix", new NamespaceRefreshCommand() ));
        aCommands.add(new RDFTCommandItem( "remove-prefix", new NamespaceRemoveCommand() ));
        aCommands.add(new RDFTCommandItem( "suggest-namespace", new SuggestNamespaceCommand() ));
        aCommands.add(new RDFTCommandItem( "suggest-term", new SuggestTermCommand() ));
        aCommands.add(new RDFTCommandItem( "add-suggest-term", new SuggestTermAddCommand() ));
        // Others:
        //   CodeResponse - Standard Response Class for Commands
        //   RDFTransformCommand - Abstract RDF Command Class

        for (RDFTCommandItem citem : aCommands) {
            if (citem.command != null) {
                RefineServlet.registerCommand( this.theModule, citem.strCommand, citem.command);
            }
        };

        /*
         *  Server-side Custom Change Class...
         */
        RefineServlet.registerClassMapping(
            // Non-existent name--we are adding, not renaming, so this can be a dummy...
            "org.openrefine.model.changes.DataExtensionChange",
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
        class RDFTExportPrinter
        {
            public RDFFormat rdfFormat;
            public String strFormat;
            RDFTExportPrinter(RDFFormat fmt, String strFmt) {
                rdfFormat = fmt;
                strFormat = strFmt;
            }
        };

        //
        // PRETTY PRINTERS: (Graph) *** Not suggested for large graphs ***
        //
        List<RDFTExportPrinter> aPretty = new ArrayList<RDFTExportPrinter>();
        aPretty.add(new RDFTExportPrinter(RDFFormat.RDFXML_PRETTY, "RDFXML_PRETTY"));
        aPretty.add(new RDFTExportPrinter(RDFFormat.TURTLE_PRETTY, "TURTLE_PRETTY"));
        aPretty.add(new RDFTExportPrinter(RDFFormat.TRIG_PRETTY, "TRIG_PRETTY"));
        aPretty.add(new RDFTExportPrinter(RDFFormat.JSONLD_PRETTY, "JSONLD_PRETTY"));
        aPretty.add(new RDFTExportPrinter(null /* NDJSONLD_PRETTY */, "NDJSONLD_PRETTY"));
        aPretty.add(new RDFTExportPrinter(RDFFormat.RDFJSON, "RDFJSON"));

        for (RDFTExportPrinter ptr : aPretty) {
            if (ptr.rdfFormat != null) {
                ExporterRegistry.registerExporter( ptr.strFormat, new RDFPrettyExporter(ptr.rdfFormat, ptr.strFormat) );
            }
        }

        //
        // STREAM PRINTERS:
        //
        List<RDFTExportPrinter> aStream = new ArrayList<RDFTExportPrinter>();
        // BLOCKS PRINTERS: per Subject (Stream)
        aStream.add(new RDFTExportPrinter(RDFFormat.TURTLE_BLOCKS, "TURTLE_BLOCKS"));
        aStream.add(new RDFTExportPrinter(RDFFormat.TRIG_BLOCKS, "TRIG_BLOCKS"));
        // LINE PRINTERS: triple, quad (Stream)
        aStream.add(new RDFTExportPrinter(RDFFormat.NTRIPLES_UTF8, "NTRIPLES"));
        aStream.add(new RDFTExportPrinter(RDFFormat.NQUADS_UTF8, "NQUADS"));
        aStream.add(new RDFTExportPrinter(RDFFormat.TRIX, "TRIX"));
        // DUMMY PRINTERS: (Stream)
        aStream.add(new RDFTExportPrinter(RDFFormat.RDFNULL, "RDFNULL"));
        // BINARY PRINTERS: (Stream)
        aStream.add(new RDFTExportPrinter(RDFFormat.RDF_PROTO, "RDF_PROTO"));
        aStream.add(new RDFTExportPrinter(RDFFormat.RDF_THRIFT, "RDF_THRIFT"));
        aStream.add(new RDFTExportPrinter(null /* BINARY_RDF */, "BinaryRDF"));
        aStream.add(new RDFTExportPrinter(null /* HDT */, "HDT"));

        for (RDFTExportPrinter ptr : aStream) {
            if (ptr.rdfFormat != null) {
                ExporterRegistry.registerExporter( ptr.strFormat, new RDFStreamExporter(ptr.rdfFormat, ptr.strFormat) );
            }
        }

        // //
        // // SPECIAL PRINTERS:
        // //
        // List<RDFTExportPrinter> aSpecial = new ArrayList<RDFTExportPrinter>();
        // // TODO: Are these even doable???
        // aSpecial.add(new RDFTExportPrinter(null /* RDFA */, "RDFa"));
        // aSpecial.add(new RDFTExportPrinter(null /* RDFFormat.SHACLC */, "SHACLC"));

        // for (RDFTExportPrinter ptr : aSpecial) {
        //     if (ptr.rdfFormat != null) {
        //         ExporterRegistry.registerExporter( ptr.strFormat, new RDFSpecialExporter(ptr.rdfFormat, ptr.strFormat) );
        //     }
        // }

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
