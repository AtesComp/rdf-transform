/*
 *  Class InitializationCommand
 *
 *  Initializes the RDF Transform extension.
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
import com.google.refine.grel.Function;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.operations.OperationRegistry;

import org.openrefine.rdf.ApplicationContext;
import org.openrefine.rdf.RDFTransform;
import org.openrefine.rdf.RDFTGlobals;
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

        String strRDFTransformSuccess = "...RDF Transform successfully initialized.";
        String strRDFTransformFailed = "...RDF Transform Extension failed to initialize!";

        //
        // Get and test Java VM for an RDF Transform compliant version...
        //
        String strJVMVersion = java.lang.System.getProperty("java.version");
        float fJVMVersion = 0.0F;
        int iPosFrstDecimal = strJVMVersion.indexOf('.');
        int iPosLastDecimal = strJVMVersion.lastIndexOf('.');
        if (iPosFrstDecimal == iPosLastDecimal) iPosLastDecimal = -1;
        try {
            if (iPosLastDecimal > 0) fJVMVersion = Float.parseFloat( strJVMVersion.substring(0, iPosLastDecimal) );
            else                     fJVMVersion = Float.parseFloat( strJVMVersion );
        }
        catch (NumberFormatException ex) {}
        InitializationCommand.logger.info("Current Java VM Version: " + strJVMVersion);
        if (fJVMVersion < 11.0F) {
            InitializationCommand.logger.error("ERROR: Java VM Version must be at least 11.0 to load and run RDF Transform!");
            InitializationCommand.logger.error("       Install a Java JDK from version 11 to 21.  Use it for OpenRefine by");
            InitializationCommand.logger.error("       setting your JAVA_HOME environment variable to point to its Java");
            InitializationCommand.logger.error("       directory OR set it as your system's default Java language.");
            InitializationCommand.logger.error(strRDFTransformFailed);
            return;
        }

        this.theModule = theModule;

        // Set the RDF Transform preferences via the OpenRefine Preference Store...
        Util.setPreferencesByPreferenceStore();

        try {
            this.initialize();
        }
        catch (Throwable ex) { // ...try to catch all Exceptions and Errors...
            InitializationCommand.logger.error("ERROR: initialize: " + ex.getMessage(), ex);
            if ( Util.isVerbose() ) ex.printStackTrace();
            InitializationCommand.logger.error(strRDFTransformFailed);
            return;
        }

        InitializationCommand.logger.info(strRDFTransformSuccess);
    }

    private void initialize() {
        InitializationCommand.logger.info("Initializing RDF Transform Extension " + RDFTransform.VERSION + "...");
        InitializationCommand.logger.info("  Ext Mount Point: " + this.theModule.getMountPoint() );

        InitializationCommand.logger.info("  Client Side...");
        this.registerClientSide();

        InitializationCommand.logger.info("  Server Side...");
        this.registerServerSide();

        InitializationCommand.logger.info( "  Preferences...\n" + Util.preferencesToString() );
    }

    private void registerClientSide() {
        //
        //  Client-side Resources...
        // ------------------------------------------------------------

        //
        // Client-side Javascript...
        //
        if ( Util.isDebugMode() ) InitializationCommand.logger.info("DEBUG: registerClientSide(): Add script paths...");

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
        if ( Util.isDebugMode() ) InitializationCommand.logger.info("DEBUG: registerClientSide(): Add style paths...");

        String[] astrStyles = new String[] {
            "styles/flyout.css",
            "styles/rdf-transform-dialog.css",
        };
        // Inject style files into /project page...
        ClientSideResourceManager.addPaths("project/styles", this.theModule, astrStyles);

        if ( Util.isDebugMode() ) InitializationCommand.logger.info("DEBUG: registerClientSide(): ...finished");
    }

    private void registerServerSide() {
        //
        // Server-side Resources...
        // ------------------------------------------------------------

        //
        // Server-side Context Initialization...
        //  The One and Only RDF Transform Application Context.
        //
        if ( Util.isDebugMode() ) InitializationCommand.logger.info("DEBUG: registerServerSide(): Set the extension's application context...");
        this.theContext = new ApplicationContext();
        RDFTransform.setGlobalContext(this.theContext);

        //
        // Server-side Ajax Commands...
        //  Each call to the RefineServlet registerCommand() calls the given command's init() method.
        //  Those commands generally DO NOT contain overridden init() methods.  However, this InitializationCommand
        //  registers itself and overrides the init() method to set additional extension wide settings.
        //
        if ( Util.isDebugMode() ) InitializationCommand.logger.info("DEBUG: registerServerSide(): Add the extension's commands...");

        class RDFTCommandItem
        {
            public String strCommand;
            public Command command;
            RDFTCommandItem(String strCmd, Command cmd) {
                strCommand = strCmd;
                command = cmd;
            }
        };

        List<RDFTCommandItem> aCommands = new ArrayList<RDFTCommandItem>();
        // Commands
        aCommands.add(new RDFTCommandItem( RDFTGlobals.strInitialize,           this ));
        aCommands.add(new RDFTCommandItem( RDFTGlobals.strGetPreferences,       new PreferencesCommand() ));
        aCommands.add(new RDFTCommandItem( RDFTGlobals.strPreviewRDF,           new PreviewRDFCommand() ));
        aCommands.add(new RDFTCommandItem( RDFTGlobals.strPreviewRDFExpression, new PreviewRDFTExpressionCommand() ));
        aCommands.add(new RDFTCommandItem( RDFTGlobals.strSaveRDFTransform,     new SaveRDFTransformCommand() ));
        aCommands.add(new RDFTCommandItem( RDFTGlobals.strSaveBaseIRI,          new SaveBaseIRICommand() ));
        aCommands.add(new RDFTCommandItem( RDFTGlobals.strValidateIRI,          new ValidateIRICommand() ));
        aCommands.add(new RDFTCommandItem( RDFTGlobals.strConvertToIRI,         new ToIRICommand() ));
        // Vocabs Commands
        aCommands.add(new RDFTCommandItem( RDFTGlobals.strGetDefaultNamespaces, new NamespacesGetDefaultCommand() ));
        aCommands.add(new RDFTCommandItem( RDFTGlobals.strSaveNamespaces,       new NamespacesSaveCommand() ));
        aCommands.add(new RDFTCommandItem( RDFTGlobals.strAddNamespaceFromURL,  new NamespaceAddFromURLCommand() ));
        aCommands.add(new RDFTCommandItem( RDFTGlobals.strAddNamespaceFromFile, new NamespaceAddFromFileCommand() ));
        aCommands.add(new RDFTCommandItem( RDFTGlobals.strRemoveNamespace,      new NamespaceRemoveCommand() ));
        aCommands.add(new RDFTCommandItem( RDFTGlobals.strSuggestNamespace,     new SuggestNamespaceCommand() ));
        aCommands.add(new RDFTCommandItem( RDFTGlobals.strSuggestTerm,          new SuggestTermCommand() ));
        aCommands.add(new RDFTCommandItem( RDFTGlobals.strAddSuggestTerm,       new SuggestTermAddCommand() ));
        // Others:
        //   CodeResponse - Standard Response Class for Commands
        //   RDFTransformCommand - Abstract RDF Command Class

        for (RDFTCommandItem itemCmd : aCommands) {
            if (itemCmd.command != null) {
                RefineServlet.registerCommand(this.theModule, itemCmd.strCommand, itemCmd.command);
            }
        };

        //
        // Server-side Custom Change Class...
        //
        if ( Util.isDebugMode() ) InitializationCommand.logger.info("DEBUG: registerServerSide(): Register the extension's Change class...");

        RefineServlet.registerClassMapping(
            // Non-existent name--we are adding, not renaming, so this is a dummy parameter...
            "org.openrefine.rdf.model.operation.DataExtensionChange",
            // Added Change Class name...
            "org.openrefine.rdf.model.operation.RDFTransformChange"
        );
        RefineServlet.cacheClass(RDFTransformChange.class);

        //
        // Server-side Operations...
        //
        if ( Util.isDebugMode() ) InitializationCommand.logger.info("DEBUG: registerServerSide(): Register the extension's Operation classes...");

        class RDFTOperationItem
        {
            public String strOperation;
            public Class<? extends AbstractOperation> classOperation;
            RDFTOperationItem(String strCmd, Class<? extends AbstractOperation> classOp) {
                strOperation = strCmd;
                classOperation = classOp;
            }
        };

        List<RDFTOperationItem> aOperations = new ArrayList<RDFTOperationItem>();
        // Operations
        aOperations.add(new RDFTOperationItem( RDFTGlobals.strSaveRDFTransform, SaveRDFTransformOperation.class ));

        for (RDFTOperationItem itemOp : aOperations) {
            if (itemOp.classOperation != null) {
                OperationRegistry.registerOperation(this.theModule, itemOp.strOperation, itemOp.classOperation);
            }
        };

        //
        // Server-side GREL Functions and Binder...
        //
        if ( Util.isDebugMode() ) InitializationCommand.logger.info("DEBUG: registerServerSide(): Register the extension's Functions and Binder...");

        class RDFTFunctionItem
        {
            public String strFunction;
            public Function function;
            RDFTFunctionItem(String strCmd, Function func) {
                strFunction = strCmd;
                function = func;
            }
        };

        List<RDFTFunctionItem> aFunction = new ArrayList<RDFTFunctionItem>();
        // Functionns
        aFunction.add(new RDFTFunctionItem( "toIRIString",        new ToIRIString() ));
        aFunction.add(new RDFTFunctionItem( "toStrippedLiteral",  new ToStrippedLiteral() ));

        for (RDFTFunctionItem itemFunc : aFunction) {
            if (itemFunc.function != null) {
                ControlFunctionRegistry.registerFunction(itemFunc.strFunction, itemFunc.function);
            }
        };

        ExpressionUtils.registerBinder( new RDFTransformBinder() );

        //
        // Server-side Exporters...
        //
        if ( Util.isDebugMode() ) InitializationCommand.logger.info("DEBUG: registerServerSide(): Register the extension's Exporters...");

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
        aPretty.add(new RDFTExportPrinter(RDFFormat.RDFXML_PRETTY,  "RDFXML_PRETTY"));
        aPretty.add(new RDFTExportPrinter(RDFFormat.TURTLE_PRETTY,  "TURTLE_PRETTY"));
        aPretty.add(new RDFTExportPrinter(RDFFormat.TRIG_PRETTY,    "TRIG_PRETTY"));
        aPretty.add(new RDFTExportPrinter(RDFFormat.JSONLD_PRETTY,  "JSONLD_PRETTY")); // default version is JSON-LD 1.1
        //aPretty.add(new RDFTExportPrinter(null,                 "NDJSONLD_PRETTY")); // NDJSONLD_PRETTY
        aPretty.add(new RDFTExportPrinter(RDFFormat.RDFJSON,        "RDFJSON"));

        for (RDFTExportPrinter itemPtr : aPretty) {
            if (itemPtr.rdfFormat != null) {
                ExporterRegistry.registerExporter( itemPtr.strFormat, new RDFPrettyExporter(itemPtr.rdfFormat, itemPtr.strFormat) );
            }
        }

        //
        // STREAM PRINTERS:
        //
        List<RDFTExportPrinter> aStream = new ArrayList<RDFTExportPrinter>();
        // BLOCKS PRINTERS: per Subject (Stream)
        aStream.add(new RDFTExportPrinter(RDFFormat.TURTLE_BLOCKS,  "TURTLE_BLOCKS"));
        aStream.add(new RDFTExportPrinter(RDFFormat.TRIG_BLOCKS,    "TRIG_BLOCKS"));
        // LINE PRINTERS: triple, quad (Stream)
        aStream.add(new RDFTExportPrinter(RDFFormat.NTRIPLES_UTF8,  "NTRIPLES"));
        aStream.add(new RDFTExportPrinter(RDFFormat.NQUADS_UTF8,    "NQUADS"));
        aStream.add(new RDFTExportPrinter(RDFFormat.TRIX,           "TRIX"));
        // DUMMY PRINTERS: (Stream)
        aStream.add(new RDFTExportPrinter(RDFFormat.RDFNULL,        "RDFNULL"));
        // BINARY PRINTERS: (Stream)
        aStream.add(new RDFTExportPrinter(RDFFormat.RDF_PROTO,      "RDF_PROTO"));
        aStream.add(new RDFTExportPrinter(RDFFormat.RDF_THRIFT,     "RDF_THRIFT"));
        //aStream.add(new RDFTExportPrinter(null,                 "BinaryRDF")); // BINARY_RDF
        //aStream.add(new RDFTExportPrinter(null,                 "HDT")); // HDT

        for (RDFTExportPrinter itemPtr : aStream) {
            if (itemPtr.rdfFormat != null) {
                ExporterRegistry.registerExporter( itemPtr.strFormat, new RDFStreamExporter(itemPtr.rdfFormat, itemPtr.strFormat) );
            }
        }

        /*================================================================================
        //
        // SPECIAL PRINTERS:
        //
        List<RDFTExportPrinter> aSpecial = new ArrayList<RDFTExportPrinter>();
        // TODO: Are these even doable???
        aSpecial.add(new RDFTExportPrinter(RDFFormat.RDFA, "RDFa"));
        aSpecial.add(new RDFTExportPrinter(RDFFormat.SHACLC, "SHACLC"));

        for (RDFTExportPrinter ptr : aSpecial) {
            if (ptr.rdfFormat != null) {
                ExporterRegistry.registerExporter( ptr.strFormat, new RDFSpecialExporter(ptr.rdfFormat, ptr.strFormat) );
            }
        }
        ================================================================================*/

        //
        // Server-side Overlay Models - Attach an RDFTransform object to the project...
        //
        if ( Util.isDebugMode() ) InitializationCommand.logger.info("DEBUG: registerServerSide(): Register the extension's Overlay Model...");

        Project.registerOverlayModel("RDFTransform", RDFTransform.class);

        // Test Exporter Registry...
        Exporter exporter = ExporterRegistry.getExporter("RDF_PROTO");
        if (exporter == null) {
            InitializationCommand.logger.error("ERROR: ExporterRegistry test failed!");
        }
        else {
            InitializationCommand.logger.info("SUCCESS: ExporterRegistry test succeeded!");
        }

        if ( Util.isDebugMode() ) InitializationCommand.logger.info("DEBUG: registerServerSide(): ...finished");
    }

    @Override
    public void init(RefineServlet servlet) {
        InitializationCommand.logger.info("Initializing with servlet...");
        super.init(servlet);

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
        File fileRDFTCacheDir = servlet.getCacheDir(RDFTransform.KEY);


        try {
            this.theContext.init(strHost, strIFace, strPort, fileRDFTCacheDir);
        }
        catch (IOException ex) {
            InitializationCommand.logger.error("ERROR: App Context Init: " + ex.getMessage(), ex);
            if ( Util.isVerbose() ) ex.printStackTrace();
        }

        if ( Util.isDebugMode() ) JenaSystem.DEBUG_INIT = true;
        JenaSystem.init();
        InitializationCommand.logger.info("...Apache Jena initialized.");

        InitializationCommand.logger.info("...initialized with servlet.");
    }

    //
    // DUMMY OVERRIDES ================
    //      The InitializationCommand is a dummy Command class that does not actually process
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
