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

var logger = Packages.org.slf4j.LoggerFactory.getLogger("RDFT:Controller");

//var RefineBase = Packages.com.google.refine;
var RDFTCmd = Packages.org.openrefine.rdf.command;

/*
 * Initialization Function for RDF Transform Extension.
 */
function init() {
    /*
     * Fool Butterfly: Make the extension's Initializer do all the heavy lifting instead of the
     *      limited server side JavaScript processor for "controller.js".
     *      NOTE TO SELF: Doh, I should have seen this a long, long, LONG time ago.
     *
     *  Server-side Initialization Command...
     *    Registration calls the class' init() method...where all the magic happens.
     */

    new RDFTCmd.InitializationCommand(module); // ...self register
}

/*
 * Process Function for external command requests.
 */
function process(path, request, response) {

    //var RDFTransformPrefs = RDFTCmd.InitializationCommand.Preferences;
    //var method = request.getMethod();

    //if ( RDFTransformPrefs.get("DebugMode") === "true" ) {
    //    logger.info('DEBUG: Receiving request by ' + method + ' for "' + path + '"\n' +
    //                '       Request: ' + request);
    //}

    //
    // Analyze path and handle this request...
    //

    // var context = {};
    // if (path == "" || path == "/") {
    //     // Here's how to pass things into the .vt templates:
    //     //   context.someList = ["Superior","Michigan","Huron","Erie","Ontario"];
    //     //   context.someString = "foo";
    //     //   context.someInt = RefineBase.sampleExtension.SampleUtil.stringArrayLength(context.someList);

    //     var paramsReq = {};
    //     paramsReq.uri    = request.getRequestURI();
    //     paramsReq.path   = request.getPathInfo();
    //     paramsReq.host   = request.getServerName();
    //     paramsReq.port   = request.getServerPort();
    //     paramsReq.prot   = request.getProtocol();
    //     paramsReq.scheme = request.getScheme();
    //     paramsReq.method = request.getMethod();
    //     context.RDFTRequest = paramsReq;

    //     send(request, response, "website/", context);
    //}
}

// function send(request, response, template, context) {
//     var encoding = "UTF-8";
//     var html = "text/html";

//     butterfly.sendTextFromTemplate(request, response, context, template, encoding, html);
// }
