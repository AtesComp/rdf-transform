/*
 *  Class RDFExporterMenuBar
 *
 *  Set up the Extension Menubar for all RDF exports file types managed by the
 *  Export Manager.
 *  NOTE: Executes the menu for display with the end code.
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
 *
 */

class RDFExporterMenuBar
{
    static async init(mgrExport, barExtension) {
        await RDFExporterMenuBar.initLanguage();

        mgrExport.MenuItems.push( {} ); // ...add separator
        mgrExport.MenuItems.push( RDFExporterMenuBar.constructExportRDF() );

        RDFExporterMenuBar.addExtensionMenu(barExtension);
    }

    /*
     *  Language Management: Initialize
     *
     *    NOTE: We must retrieve and load language translation file syncronously so that
     *    the following $.i18n() language callouts process correctly.  An asynchronous
     *    load does not present the language elements for many of the display items that
     *    directly follow.
     */
    static async initLanguage() {
        if (typeof I18NUtil !== 'undefined') {
            // eslint-disable-next-line no-undef
            await I18NUtil.init("rdf-transform");
        }
        // TODO: This code may be removed sometime after the 3.7 release has been circulated.
        else {
            let lang = (navigator.language).split("-")[0];
            let dictionary = "";

            /*
              Initialize i18n and load message translation file from the server.

              Note that the language is set by the 'userLang' user preference setting.  You can change that by
              clicking on 'Language Settings' on the landing page.
            */
           // @ts-ignore
            await $.ajax({
                url: "command/core/load-language?",
                type: "POST",
                async: false,
                data: {
                    module: "rdf-transform"
                },
                success: function (data) {
                    dictionary = data['dictionary'];
                    var langFromServer = data['lang'];
                    if (lang !== langFromServer) {
                        console.warn('Language \'' + lang + '\' missing translation. Defaulting to \'' + langFromServer + '\'.');
                        lang = langFromServer;
                    }
                }
            }).fail(function( jqXhr, textStatus, errorThrown ) {
                // @ts-ignore
                var errorMessage = $.i18n('core-index/prefs-loading-failed');
                if (errorMessage != "" && errorMessage != 'core-index/prefs-loading-failed') {
                    alert(errorMessage);
                }
                else {
                    alert( textStatus + ':' + errorThrown );
                }
            });
            // @ts-ignore
            $.i18n().load(dictionary, lang);
            // @ts-ignore
            $.i18n( { locale: lang } );
        }
    }
    // ...end Language Management


    static constructExportRDF() {
        /* DEBUG:
        alert("Project Name: " + theProject.metadata.name +
            "\nCalc'ed Name: " + name +
            "\nFormat: " + format +
            "\nExt: " + ext);
        */

        /** @type {{ id:string, label:string, submenu:any[] }} */
        var objExports;
        /** @type {{ id:string, label:string, submenu:any[] }} */
        var objTypeSubMenuItem;
        /** @type {{ id:string, label:string, click:function }} */
        var objTypeSubSubMenuItem;

        objExports = {
            id : "rdf-transform",
            // @ts-ignore
            label : $.i18n('rdft'),
            submenu : []
        };

        //
        // PRETTY PRINTERS: (Graph) *** Not suggested for large graphs ***
        //
        objTypeSubMenuItem = {
            id : "rdf-transform/pretty",
            // @ts-ignore
            label : $.i18n('rdft-menu/export-pretty'),
            submenu : []
        };

        objTypeSubSubMenuItem = {
            id : "rdf-transform/pretty/exportRDFXML",
            // @ts-ignore
            label : $.i18n("rdft-menu/rdf-xml-pretty"),
            click : () => RDFExporterMenuBar.#exportRDF("RDFXML_PRETTY", "rdf")
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        objTypeSubSubMenuItem = {
            id : "rdf-transform/pretty/exportRDFTurtle",
            // @ts-ignore
            label : $.i18n("rdft-menu/rdf-turtle-pretty"),
            click : () => RDFExporterMenuBar.#exportRDF("TURTLE_PRETTY", "ttl")
         } ;
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        objTypeSubSubMenuItem = {
            id : "rdf-transform/pretty/exportRDFTriG",
            // @ts-ignore
            label : $.i18n("rdft-menu/rdf-trig-pretty"),
            click : () => RDFExporterMenuBar.#exportRDF("TRIG_PRETTY", "trig")
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        objTypeSubSubMenuItem = {
            id : "rdf-transform/pretty/exportRDFJSONLD",
            // @ts-ignore
            label : $.i18n("rdft-menu/rdf-jsonld-pretty"),
            click : () => RDFExporterMenuBar.#exportRDF("JSONLD_PRETTY", "jsonld") // default version is JSON-LD 1.1
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        //objTypeSubSubMenuItem = {
        //    id : "rdf-transform/pretty/exportRDFNDJSONLD",
        //    label : $.i18n("rdft-menu/rdf-ndjsonld-pretty"),
        //    click : () => RDFExporterMenuBar.#exportRDF("NDJSONLD_PRETTY", "ndjsonld")
        //};
        //objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        objTypeSubSubMenuItem = {
            id : "rdf-transform/pretty/exportRDFJSON",
            // @ts-ignore
            label : $.i18n("rdft-menu/rdf-json-pretty"),
            click : () => RDFExporterMenuBar.#exportRDF("RDFJSON", "rj")
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        objExports.submenu.push(objTypeSubMenuItem);

        //
        // STREAM PRINTERS: per Subject (Stream)
        //
        objTypeSubMenuItem = {
            id : "rdf-transform/stream",
            // @ts-ignore
            label : $.i18n('rdft-menu/export-stream'),
            submenu : []
        };

        // BLOCKS PRINTERS: per Subject (Stream)

        objTypeSubSubMenuItem = {
            id : "rdf-transform/stream/exportRDFTurtle",
            // @ts-ignore
            label : $.i18n("rdft-menu/rdf-turtle-stream"),
            click : () => RDFExporterMenuBar.#exportRDF("TURTLE_BLOCKS", "ttl")
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        objTypeSubSubMenuItem = {
            id : "rdf-transform/stream/exportRDFTriG",
            // @ts-ignore
            label : $.i18n("rdft-menu/rdf-trig-stream"),
            click : () => RDFExporterMenuBar.#exportRDF("TRIG_BLOCKS", "trig")
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        // LINE PRINTERS: triple, quad (Stream)

        objTypeSubSubMenuItem = {
            id : "rdf-transform/stream/exportRDFNTriples",
            // @ts-ignore
            label : $.i18n("rdft-menu/rdf-n-triples-stream"),
            click : () => RDFExporterMenuBar.#exportRDF("NTRIPLES", "nt")
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        objTypeSubSubMenuItem = {
            id : "rdf-transform/stream/exportRDFNQuads",
            // @ts-ignore
            label : $.i18n("rdft-menu/rdf-nquads-stream"),
            click : () => RDFExporterMenuBar.#exportRDF("NQUADS", "nq")
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        objTypeSubSubMenuItem = {
            id : "rdf-transform/stream/exportRDFTriX",
            // @ts-ignore
            label : $.i18n("rdft-menu/rdf-trix-stream"),
            click : () => RDFExporterMenuBar.#exportRDF("TRIX", "xml")
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        // DUMMY PRINTERS: (Stream)

        objTypeSubSubMenuItem = {
            id : "rdf-transform/stream/exportRDFNull",
            // @ts-ignore
            label : $.i18n("rdft-menu/rdf-null-stream"),
            click : () => RDFExporterMenuBar.#exportRDF("RDFNULL", "rn")
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        // BINARY PRINTERS: (Stream)

        objTypeSubSubMenuItem = {
            id : "rdf-transform/stream/exportRDFProto",
            // @ts-ignore
            label : $.i18n("rdft-menu/rdf-proto-stream"),
            click : () => RDFExporterMenuBar.#exportRDF("RDF_PROTO", "rp")
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        objTypeSubSubMenuItem = {
            id : "rdf-transform/stream/exportRDFThrift",
            // @ts-ignore
            label : $.i18n("rdft-menu/rdf-thrift-stream"),
            click : () => RDFExporterMenuBar.#exportRDF("RDF_THRIFT", "rt")
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        //objTypeSubSubMenuItem = {
        //    id : "rdf-transform/stream/exportRDFBinaryRDF",
        //    // @ts-ignore
        //    label : $.i18n("rdft-menu/rdf-binary"),
        //    click : () => RDFExporterMenuBar.#exportRDF("BinaryRDF", "brf")
        //};
        //objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        //objTypeSubSubMenuItem = {
        //    id : "rdf-transform/stream/exportRDFHDT",
        //    // @ts-ignore
        //    label : $.i18n("rdft-menu/rdf-hdt"),
        //    click : () => RDFExporterMenuBar.#exportRDF("HDT", "hdt")
        //};
        //objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        objExports.submenu.push(objTypeSubMenuItem);

        //
        // TODO: SPECIAL PRINTERS - Are these even doable???
        //
        //objTypeSubMenuItem = {
        //    id : "rdf-transform/special",
        //    // @ts-ignore
        //    label : $.i18n('rdft-menu/export-special'),
        //    submenu : []
        // };

        //objTypeSubSubMenuItem = {
        //    id : "rdf-transform/special/exportRDFa",
        //    // @ts-ignore
        //    label : $.i18n("rdft-menu/rdf-a-special"),
        //    click : () => RDFExporterMenuBar.#exportRDF("RDFa", "xhtml")
        //};
        //objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        //objTypeSubSubMenuItem = {
        //    id : "rdf-transform/special/exportRDFSHACLC",
        //    // @ts-ignore
        //    label : $.i18n("rdft-menu/rdf-shaclc-special"),
        //    click : () => RDFExporterMenuBar.#exportRDF("SHACLC", "sc")
        //};
        //objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        //objExports.submenu.push(objTypeSubMenuItem);

        return objExports;
    }

    static #exportRDF(format, ext) {
        console.log('Exporting with Format: ' + format + ' Extension: ' + ext);

        if (! theProject.overlayModels.RDFTransform) {
            // @ts-ignore
            alert( $.i18n("rdft-menu/alert-no-transform") );
            return;
        }

        const winHtml =
            "<!DOCTYPE html>\n" +
            "<html>\n" +
            "  <head>\n" +
            "    <title>OpenRefine RDF Transform Export</title>\n" +
            "  </head>\n" +
            "  <body>\n" +
            "    <h1>Transforming data to RDF format " + format + "...</h1>\n" +
            "    <p>If there is a lot of data, it could take a while to assemble it. Please be patient.</p>"
            "  </body>\n" +
            "</html>\n";
        const winUrl = URL.createObjectURL( new Blob( [winHtml], { type: "text/html" } ) );
        const winName = "OpenRefine RDF Transform Export " + format;
        const iLeft = window.screenX + 100;
        const iTop = window.screenY + 100;
        const winFeatures = "popup,left=" + iLeft + ",top=" + iTop + ",width=600,height=300";

        Refine.wrapCSRF(
          (token) => {
            let form = RDFExporterMenuBar.#prepareExportRDFForm(format, ext, token);
            document.body.appendChild(form);
            form.submit();
            document.body.removeChild(form);

            const win = window.open(winUrl, winName, winFeatures);
            if (win) win.focus();
          }
        );

    }

    static #prepareExportRDFForm(format, ext, token) {
        var strProjectName =
            theProject.metadata.name
            .replace(/^\p{White_Space}+/u, '') // Leading Whitespace to none
            .replace(/\p{White_Space}+$/u, '') // Trailing Whitespace to none
            .replace(/[^\p{L}\p{N}_]/gu, '_') // Unprintable to _
            .replace(/\p{White_Space}+/gu, '-');  // Other Whitespace to -

        var form = document.createElement("form");

        // @ts-ignore
        $(form)
        .css("display", "none")
        .attr("method", "post")
        // @ts-ignore
        .attr("action", "command/core/export-rows/" +  strProjectName + "." + ext + "?" + $.param( {csrf_token: token} ))
        .attr("target", "gridworks-export");

        var appendField = (name, value) => {
            // @ts-ignore
            $('<input />')
                .attr("name", name)
                .val(value)
                .appendTo(form);
        };
    
        appendField("engine",   JSON.stringify( ui.browsingEngine.getJSON() ));
        appendField("project",  theProject.id);
        appendField("format",   format);

        return form;
    }

    /*
    *  Extension Management: Setup Menu
    */
    static addExtensionMenu(barExtension) { // ...load all DOM elements before executing the following...
        barExtension.addExtensionMenu(
            {
                "id"        : "rdf-transform",
                // @ts-ignore
                "label"     : $.i18n('rdft'),
                "submenu"   : [
                    {
                        "id"    : "rdft/edit-rdf-transform",
                        // @ts-ignore
                        "label" : $.i18n('rdft-menu/edit') + "...",
                        "click" : () => {
                            const theTransform = theProject.overlayModels.RDFTransform;
                            // Use setTimeout() to end menuitem and display dialog...
                            setTimeout(
                                () => {
                                    new RDFTransformDialog(theTransform);
                                }
                            );
                        }
                    },
                    {
                        "id"    : "rdft/reset-rdf-transform",
                        // @ts-ignore
                        "label" : $.i18n('rdft-menu/reset') + "...",
                        "click" : () => {
                            // Use setTimeout() to end menuitem and display dialog...
                            setTimeout(
                                () => {
                                    new RDFTransformDialog();
                                }
                            );
                        }
                    }
                ]
            }
        )
    }

    // ...end Extension Management
}

RDFExporterMenuBar.init(ExporterManager, ExtensionBar);

// ...end Export Management
