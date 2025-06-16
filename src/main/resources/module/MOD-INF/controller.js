/*
 *  ***** CONTROLLER *****
 *
 *  The Server-Side JavaScript processor may be a limited funtionality
 *  processor, so many functions taken for granted in modern JavaScript
 *  processors may not be present in this implementation.
 *
 *  NOTE: This is a Server-Side JavaScript
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

var logger = Packages.org.slf4j.LoggerFactory.getLogger("RDFT:Controller");

//var RefineBase = Packages.com.google.refine;
var RDFTCmd = Packages.org.openrefine.rdf.command;

/*
 * Initialization Function for RDF Transform Extension.
 *
 *      The init() function is called by OpenRefine's Simile Butterfly Server for an extension.
 */
function init() {
    //
    // Get and test Java VM for an RDF Transform compliant version
    //
    var strJVMVersion = Packages.java.lang.System.getProperty("java.version");
    logger.info('Current Java VM Version: ' + strJVMVersion);
    if (strJVMVersion < "11.0") {
        logger.error("ERROR: Java VM Version must be at least 11.0 to load and run RDF Transform!");
        logger.error("       Install a Java JDK from version 11 to 21.  Use it for OpenRefine by");
        logger.error("       setting your JAVA_HOME environment variable to point to its Java");
        logger.error("       directory OR set it as your system's default Java language.");
        logger.error("       Ending RDF Transform load...");
        return;
    }

    /*
     * Fool Butterfly:
     *      Make the extension's Initializer do all the heavy lifting instead of the
     *      limited server side JavaScript processor for this "controller.js".
     *      NOTE TO SELF: Doh, I should have seen this a long, long, LONG time ago.
     *
     *  Server-side Initialization Command...
     *    The InitializationCommand constructor calls its initialize() method...where all the magic happens.
     */

    new RDFTCmd.InitializationCommand(module); // ...self register
}

/*
 * Process Function for external command requests.
 *
 *      The process() function is called by OpenRefine's Simile Butterfly Server when the web
 *      URL address uses the extension.  Example:
 *          http://127.0.0.1:3333/extension/rdf-transform/...
 *      However, this function is only used to reshape the URL.  RDF Transform does not need
 *      to reshape the URL, so is only here for completeness.
 */
function process(path, request, response) {
    /********************************************************************************
    var RDFTransformPrefs = RDFTCmd.InitializationCommand.Preferences;
    var method = request.getMethod();

    if ( RDFTransformPrefs.get("DebugMode") === "true" ) {
        logger.info('DEBUG: Receiving request by ' + method + ' for "' + path + '"\n' +
                    '       Request: ' + request);
    }

    //
    // Analyze path and handle this request...
    //

    var context = {};
    if (path == "" || path == "/") {
        // Here's how to pass things into the .vt templates:
        //   context.someList = ["Superior","Michigan","Huron","Erie","Ontario"];
        //   context.someString = "foo";
        //   context.someInt = RefineBase.sampleExtension.SampleUtil.stringArrayLength(context.someList);

        var paramsReq = {};
        paramsReq.uri    = request.getRequestURI();
        paramsReq.path   = request.getPathInfo();
        paramsReq.host   = request.getServerName();
        paramsReq.port   = request.getServerPort();
        paramsReq.prot   = request.getProtocol();
        paramsReq.scheme = request.getScheme();
        paramsReq.method = request.getMethod();
        context.RDFTRequest = paramsReq;

        send(request, response, "website/", context);
    }
    ********************************************************************************/
}

/***********************************************************************************
function send(request, response, template, context) {
    var encoding = "UTF-8";
    var html = "text/html";

    butterfly.sendTextFromTemplate(request, response, context, template, encoding, html);
}
************************************************************************************/
