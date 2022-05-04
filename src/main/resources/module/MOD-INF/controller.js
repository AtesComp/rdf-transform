//
// ***** CONTROLLER *****
//
// NOTE: This is a Server-Side JavaScript
//
//   The Server-Side JavaScript processor may be a limited funtionality
//   processor, so many functions taken for granted in modern JavaScript
//   processors may not be present in this implementation.
//
importPackage(org.openrefine.rdf.commands);

// Client side preferences mirror Server side...
var RDFTransformPrefs = {
	"Verbosity"   : 0,
	"ExportLimit" : 10737418,
	"DebugMode"   : false
}

var logger = Packages.org.slf4j.LoggerFactory.getLogger("RDFT:Controller");

var RefineBase = Packages.com.google.refine;
var RDFTBase = Packages.org.openrefine.rdf;

function registerClientSide() {
	var ClientSideResourceManager = RefineBase.ClientSideResourceManager;

	//
    //  Client-side Resources...
    // ------------------------------------------------------------

    /*
	 *  Client-side Javascript...
	 */
	ClientSideResourceManager.addPaths(
		"project/scripts",
		module,
		// Script files to inject into /project page...
		[	"scripts/rdf-transform-menubar-extensions.js",	// 1. must be first: language and menu load
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
			// JQuery is required and provided by OpenRefine...
			// "scripts/externals/jquery.form.min.js"
		]
	);

    /*
	 *  Client-side CSS...
	 */
	ClientSideResourceManager.addPaths(
		"project/styles",
		module,
		// Style files to inject into /project page...
		[	"styles/rdf-transform-dialog.css",
			"styles/flyout.css",
		]
	);
}

function registerServerSide() {
	var RefineServlet = RefineBase.RefineServlet;

	var RDFTCmd = RDFTBase.command;
	var RDFTModel = RDFTBase.model;

    //
    //  Server-side Resources...
    // ------------------------------------------------------------

	/*
	 *  Server-side Context Initialization...
	 *    The One and Only RDF Transform Application Context.
	 */
	var appContext = new RDFTBase.ApplicationContext();

	/*
     *  Server-side Ajax Commands...
	 *    Each registration calls the class' init() method.
     */
	var strSaveRDFTransform = "save-rdf-transform";
	RefineServlet.registerCommand( module, "initialize",              new RDFTCmd.InitializationCommand(appContext) );
	RefineServlet.registerCommand( module, strSaveRDFTransform,       new RDFTCmd.SaveRDFTransformCommand() );
    RefineServlet.registerCommand( module, "save-baseIRI",            new RDFTCmd.SaveBaseIRICommand() );
    RefineServlet.registerCommand( module, "preview-rdf",             new RDFTCmd.PreviewRDFCommand() );
    RefineServlet.registerCommand( module, "preview-rdf-expression",  new RDFTCmd.PreviewRDFExpressionCommand() );
    RefineServlet.registerCommand( module, "validate-iri",            new RDFTCmd.ValidateIRICommand() );
	// Vocabs commands
    RefineServlet.registerCommand( module, "get-default-namespaces",  new RDFTCmd.NamespacesGetDefaultCommand() );
    RefineServlet.registerCommand( module, "save-namespaces",         new RDFTCmd.NamespacesSaveCommand() );
    RefineServlet.registerCommand( module, "add-namespace",           new RDFTCmd.NamespaceAddCommand() );
    RefineServlet.registerCommand( module, "add-namespace-from-file", new RDFTCmd.NamespaceAddFromFileCommand() );
    RefineServlet.registerCommand( module, "refresh-prefix",          new RDFTCmd.NamespaceRefreshCommand() );
    RefineServlet.registerCommand( module, "remove-prefix",           new RDFTCmd.NamespaceRemoveCommand() );
    RefineServlet.registerCommand( module, "suggest-namespace",       new RDFTCmd.SuggestNamespaceCommand() );
    RefineServlet.registerCommand( module, "suggest-term",            new RDFTCmd.SuggestTermCommand() );
	// Others:
	//   CodeResponse - Standard Response Class for Commands
	//   RDFTransformCommand - Abstract RDF Command Class

    /*
     *  Server-side Custom Change Class...
     */
	var strRefineBase = "com.google.refine";
	var strRDFTransformBase = "org.openrefine.rdf";
	RefineServlet.registerClassMapping(
		// Non-existent name--we are adding, not renaming...
		strRefineBase + ".model.changes.DataExtensionChange",
		// Added Change Class name...
		strRDFTransformBase + ".model.operation.RDFTransformChange"
	);
	RefineServlet.cacheClass(RDFTModel.operation.RDFTransformChange);

    /*
     *  Server-side Operations...
     */
    RefineBase.operations.OperationRegistry
	.registerOperation( module, strSaveRDFTransform, RDFTModel.operation.SaveRDFTransformOperation );

    /*
     *  Server-side GREL Functions and Binders...
     */
	var RefineGrelFuncReg = RefineBase.grel.ControlFunctionRegistry;
    RefineGrelFuncReg.registerFunction( "toIRIString", new RDFTModel.expr.functions.ToIRIString() );
    RefineGrelFuncReg.registerFunction( "toStrippedLiteral", new RDFTModel.expr.functions.ToStrippedLiteral() );

	RefineBase.expr.ExpressionUtils
	.registerBinder( new RDFTModel.expr.RDFTransformBinder() );

    /*
     *  Server-side Exporters...
     */
    var RefineExpReg = RefineBase.exporters.ExporterRegistry;
    var RDFTExp = RDFTModel.exporter.RDFExporter;
	var RDFFormat = org.apache.jena.riot.RDFFormat;

    RefineExpReg.registerExporter( "RDF/XML",    new RDFTExp(RDFFormat.RDFXML) );
    RefineExpReg.registerExporter( "NTriples",   new RDFTExp(RDFFormat.NTRIPLES) );
    RefineExpReg.registerExporter( "NTriples*",  new RDFTExp(RDFFormat.NTRIPLES) );
    RefineExpReg.registerExporter( "Turtle",     new RDFTExp(RDFFormat.TURTLE) );
    RefineExpReg.registerExporter( "Turtle*",    new RDFTExp(RDFFormat.TURTLE) );
	//RefineExpReg.registerExporter( "N3",         new RDFTExp(RDFFormat.TURTLE) );
	RefineExpReg.registerExporter( "TriX",       new RDFTExp(RDFFormat.TRIX) );
	RefineExpReg.registerExporter( "TriG",       new RDFTExp(RDFFormat.TRIG) );
	RefineExpReg.registerExporter( "TriG*",      new RDFTExp(RDFFormat.TRIG) );
	//RefineExpReg.registerExporter( "BinaryRDF",   new RDFTExp(BINARY) ); // RDF4J Binary
	RefineExpReg.registerExporter( "NQuads",     new RDFTExp(RDFFormat.NQUADS) );
	RefineExpReg.registerExporter( "NQuads*",    new RDFTExp(RDFFormat.NQUADS) );
	RefineExpReg.registerExporter( "JSONLD-1.0", new RDFTExp(RDFFormat.JSONLD) );
	//RefineExpReg.registerExporter( "JSONLD-1.1", new RDFTExp(RDFFormat.JSONLD11) );
	//RefineExpReg.registerExporter( "NDJSONLD",   new RDFTExp(NDJSONLD) ); // RDF4J NewLine Delimited JSONLD
	RefineExpReg.registerExporter( "RDF/JSON",   new RDFTExp(RDFFormat.RDFJSON) );
	//RefineExpReg.registerExporter( "RDFa",       new RDFTExp(RDFA) );
	//RefineExpReg.registerExporter( "HDT",        new RDFTExp(HDT) );
	//RefineExpReg.registerExporter( "RDFNull",    new RDFTExp(RDFFormat.RDFNULL) );
	//RefineExpReg.registerExporter( "RDFProto",   new RDFTExp(RDFFormat.RDF_PROTO) );
	RefineExpReg.registerExporter( "RDFTrift",   new RDFTExp(RDFFormat.RDF_THRIFT) );
	//RefineExpReg.registerExporter( "SHACLC",     new RDFTExp(RDFFormat.SHACLC) );

    /*
     *  Server-side Overlay Models - Attach an RDFTransform object to the project...
     */
    RefineBase.model.Project
	.registerOverlayModel("RDFTransform", RDFTBase.RDFTransform);
}

function processPreferences() {
	/*
	 *  Process OpenRefine Preference Store...
	 *
	 *  NOTE: We have limited use for these preferences in this Server-Side
	 *  controller.js code.  We use this opportunity to simple check and report
	 *  on the preferences related to this extension.
	 */
	var prefStore = RefineBase.ProjectManager.singleton.getPreferenceStore();
	if (prefStore != null) {
		// Verbosity...
		var prefVerbosity = prefStore.get('RDFTransform.verbose');
		if (prefVerbosity == null) {
			prefVerbosity = prefStore.get('verbose');
		}
		if (prefVerbosity != null) {
			var iVerbosity = parseInt(prefVerbosity);
			if ( ! isNaN(iVerbosity) ) {
				RDFTransformPrefs["Verbosity"] = iVerbosity;
			}
		}
		// Export Limit...
		var prefExportLimit = prefStore.get('RDFTransform.exportLimit');
		if (prefExportLimit != null) {
			var iExportLimit = parseInt(prefExportLimit);
			if ( ! isNaN(iExportLimit) ) {
				RDFTransformPrefs["ExportLimit"] = iExportLimit;
			}
		}
		// Debug...
		var prefDebug = prefStore.get('RDFTransform.debug');
		if (prefDebug == null) {
			prefDebug = prefStore.get('debug');
		}
		if (prefDebug != null) {
			var bDebug = (prefDebug.toLowerCase() == 'true');
			logger.info("DebugMode Test: " + prefDebug + " " + bDebug);
			RDFTransformPrefs.DebugMode = bDebug;
		}

		//
		// Output RDFTranform Preferences...
		//
		// NOTE: This really sucks because this server-side JavaScript is extremely limited!!!
		//		1. Looping structure don't exist!
		//		2. JSON object does not exist, so no stringify()!
		// In other words, we can't automate the processing of the RDFTransformPrefs list, but
		// must call out each pref by key explicitly.
		var strPrefs = "Preferences: { ";
		var strPref;
		strPref = "Verbosity";
		strPrefs += strPref + " : " + RDFTransformPrefs[strPref].toString() + " , ";
		strPref = "ExportLimit";
		strPrefs += strPref + " : " + RDFTransformPrefs[strPref].toString() + " , ";
		strPref = "DebugMode";
		strPrefs += strPref + " : " + RDFTransformPrefs[strPref].toString() + " }";
		logger.info(strPrefs);
	}
	else {
		logger.info("Preferences not yet loaded!");
	}
}

/*
 * Initialization Function for RDF Transform Extension.
 */
function init() {
	logger.info("Initializing RDF Transform Extension " + RDFTBase.RDFTransform.VERSION + "...");
	logger.info("  Ext Mount Point: " + module.getMountPoint() );
	logger.info("  Client Side...");
	registerClientSide();
	logger.info("  Server Side...");
	registerServerSide();
	logger.info("  Preferences...");
	processPreferences();

	logger.info("...RDF Transform Extension initialized.");
}

/*
 * Process Function for external command requests.
 */
function process(path, request, response) {

    var method = request.getMethod();

	if ( RDFTransformPrefs.DebugMode ) {
		logger.info('DEBUG: Receiving request by ' + method + ' for "' + path + '"\n' +
					'       Request: ' + request);
	}

	//
	// Analyze path and handle this request...
	//

	if (path == "" || path == "/") {
		var context = {};
		//
		// Here's how to pass things into the .vt templates:
		//   context.someList = ["Superior","Michigan","Huron","Erie","Ontario"];
		//   context.someString = "foo";
		//   context.someInt = RefineBase.sampleExtension.SampleUtil.stringArrayLength(context.someList);
		//
		//context.RDFTransform_protocol = request.url.protocol;
		//context.RDFTransform_host = request.url.host;
		//context.RDFTransform_path = request.url.pathname;
		//context.RDFTransform_search = request.url.search;
		//context.RDFTransform_href = request.url.href;

		//var paramsReq = new Packages.java.util.Properties();
		//paramsReq.put( "uri",    request.getRequestURI() );
		//paramsReq.put( "path",   request.getPathInfo() );
		//paramsReq.put( "host",   request.getServerName() );
		//paramsReq.put( "port",   request.getServerPort() );
		//paramsReq.put( "prot",   request.getProtocol() );
		//paramsReq.put( "scheme", request.getScheme() );
		//paramsReq.put( "method", request.getMethod() );

		var paramsReq = {};
		paramsReq.uri    = request.getRequestURI();
		paramsReq.path   = request.getPathInfo();
		paramsReq.host   = request.getServerName();
		paramsReq.port   = request.getServerPort();
		paramsReq.prot   = request.getProtocol();
		paramsReq.scheme = request.getScheme();
		paramsReq.method = request.getMethod();
		context.RDFTRequest = paramsReq;

		send(request, response, "website/index.html", context);
    }
}

function send(request, response, template, context) {
	var encoding = "UTF-8";
	var html = "text/html";

	butterfly.sendTextFromTemplate(request, response, context, template, encoding, html);
}
