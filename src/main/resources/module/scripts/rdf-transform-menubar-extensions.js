/*
 *  Export Management: Setup Menu
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
                var errorMessage = $.i18n('core-index/prefs-loading-failed');
                if (errorMessage != "" && errorMessage != 'core-index/prefs-loading-failed') {
                    alert(errorMessage);
                }
                else {
                    alert( textStatus + ':' + errorThrown );
                }
            });
            $.i18n().load(dictionary, lang);
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
        /** @type {string} */
        var strType;
        /** @type {{ id:string, label:string, click:function }} */
        var objTypeSubSubMenuItem;
        /** @type {string} */
        var strExp;

        objExports = {
            id : "rdf-transform",
            label : $.i18n('rdft'),
            submenu : []
        };

        //
        // PRETTY PRINTERS: (Graph) *** Not suggested for large graphs ***
        //
        objTypeSubMenuItem = {
            id : "rdf-transform/pretty",
            label : $.i18n('rdft-menu/export-pretty'),
            submenu : []
        };

        strType = "_PRETTY";

        strExp = "RDFXML" + strType;
        objTypeSubSubMenuItem = {
            id : "rdf-transform/pretty/exportRDFXML",
            label : $.i18n("rdft-menu/rdf-xml-pretty"),
            click : () => RDFExporterMenuBar.#exportRDF(strExp, "rdf")
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        strExp = "TURTLE" + strType;
        objTypeSubSubMenuItem = {
            id : "rdf-transform/pretty/exportRDFTurtle",
            label : $.i18n("rdft-menu/rdf-turtle-pretty"),
            click : () => RDFExporterMenuBar.#exportRDF(strExp, "ttl")
         } ;
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        strExp = "TRIG" + strType;
        objTypeSubSubMenuItem = {
            id : "rdf-transform/pretty/exportRDFTriG",
            label : $.i18n("rdft-menu/rdf-trig-pretty"),
            click : () => RDFExporterMenuBar.#exportRDF(strExp, "trig")
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        strExp = "JSONLD" + strType;
        objTypeSubSubMenuItem = {
            id : "rdf-transform/pretty/exportRDFJSONLD",
            label : $.i18n("rdft-menu/rdf-jsonld-pretty"),
            click : () => RDFExporterMenuBar.#exportRDF(strExp, "jsonld")
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        //strExp = "NDJSONLD" + strType;
        //objTypeSubSubMenuItem = {
        //    id : "rdf-transform/pretty/exportRDFNDJSONLD",
        //    label : $.i18n("rdft-menu/rdf-ndjsonld-pretty"),
        //    click : () => RDFExporterMenuBar.#exportRDF(strExp, "ndjsonld")
        //};
        //objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        strExp = "RDFJSON";
        objTypeSubSubMenuItem = {
            id : "rdf-transform/pretty/exportRDFJSON",
            label : $.i18n("rdft-menu/rdf-json-pretty"),
            click : () => RDFExporterMenuBar.#exportRDF(strExp, "rj")
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        objExports.submenu.push(objTypeSubMenuItem);

        //
        // STREAM PRINTERS: per Subject (Stream)
        //
        objTypeSubMenuItem = {
            id : "rdf-transform/stream",
            label : $.i18n('rdft-menu/export-stream'),
            submenu : []
        };

        // BLOCKS PRINTERS: per Subject (Stream)
        strType = "_BLOCKS";

        strExp = "TURTLE" + strType;
        objTypeSubSubMenuItem = {
            id : "rdf-transform/stream/exportRDFTurtle",
            label : $.i18n("rdft-menu/rdf-turtle-stream"),
            click : () => RDFExporterMenuBar.#exportRDF(strExp, "ttl")
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        strExp = "TRIG" + strType;
        objTypeSubSubMenuItem = {
            id : "rdf-transform/stream/exportRDFTriG",
            label : $.i18n("rdft-menu/rdf-trig-stream"),
            click : () => RDFExporterMenuBar.#exportRDF(strExp, "trig")
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        // LINE PRINTERS: triple, quad (Stream)

        strExp = "NTRIPLES";
        objTypeSubSubMenuItem = {
            id : "rdf-transform/stream/exportRDFNTriples",
            label : $.i18n("rdft-menu/rdf-n-triples-stream"),
            click : () => RDFExporterMenuBar.#exportRDF(strExp, "nt")
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        strExp = "NQUADS";
        objTypeSubSubMenuItem = {
            id : "rdf-transform/stream/exportRDFNQuads",
            label : $.i18n("rdft-menu/rdf-nquads-stream"),
            click : () => RDFExporterMenuBar.#exportRDF(strExp, "nq")
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        strExp = "TRIX";
        objTypeSubSubMenuItem = {
            id : "rdf-transform/stream/exportRDFTriX",
            label : $.i18n("rdft-menu/rdf-trix-stream"),
            click : () => RDFExporterMenuBar.#exportRDF(strExp, "xml")
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        // DUMMY PRINTERS: (Stream)

        strExp = "RDFNULL";
        objTypeSubSubMenuItem = {
            id : "rdf-transform/stream/exportRDFNull",
            label : $.i18n("rdft-menu/rdf-null-stream"),
            click : () => RDFExporterMenuBar.#exportRDF(strExp, "rn")
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        // BINARY PRINTERS: (Stream)

        strExp = "RDF_PROTO";
        objTypeSubSubMenuItem = {
            id : "rdf-transform/stream/exportRDFProto",
            label : $.i18n("rdft-menu/rdf-proto-stream"),
            click : () => RDFExporterMenuBar.#exportRDF(strExp, "rp")
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        strExp = "RDF_THRIFT";
        objTypeSubSubMenuItem = {
            id : "rdf-transform/stream/exportRDFThrift",
            label : $.i18n("rdft-menu/rdf-thrift-stream"),
            click : () => RDFExporterMenuBar.#exportRDF(strExp, "rt")
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        //strExp = "BinaryRDF";
        //objTypeSubSubMenuItem = {
        //    id : "rdf-transform/stream/exportRDFBinaryRDF",
        //    label : $.i18n("rdft-menu/rdf-binary"),
        //    click : () => RDFExporterMenuBar.#exportRDF(strExp, "brf")
        //};
        //objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        //strExp = "HDT";
        //objTypeSubSubMenuItem = {
        //    id : "rdf-transform/stream/exportRDFHDT",
        //    label : $.i18n("rdft-menu/rdf-hdt"),
        //    click : () => RDFExporterMenuBar.#exportRDF(strExp, "hdt")
        //};
        //objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        objExports.submenu.push(objTypeSubMenuItem);

        //
        // TODO: SPECIAL PRINTERS - Are these even doable???
        //
        // objTypeSubMenuItem = {
        //     id : "rdf-transform/special",
        //     label : $.i18n('rdft-menu/export-special'),
        //     submenu : []
        // };

        //strExp = "RDFa";
        //objTypeSubSubMenuItem = {
        //    id : "rdf-transform/special/exportRDFa",
        //    label : $.i18n("rdft-menu/rdf-a-special"),
        //    click : () => RDFExporterMenuBar.#exportRDF(strExp, "xhtml")
        //};
        //objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        //strExp = "SHACLC";
        //objTypeSubSubMenuItem = {
        //    id : "rdf-transform/special/exportRDFSHACLC",
        //    label : $.i18n("rdft-menu/rdf-shaclc-special"),
        //    click : () => RDFExporterMenuBar.#exportRDF(strExp, "sc")
        //};
        //objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        //objExports.submenu.push(objTypeSubMenuItem);

        return objExports;
    }

    static #exportRDF(format, ext) {
        console.log('Exporting with Format: ' + format + ' Extension: ' + ext);

        if (! theProject.overlayModels.RDFTransform) {
            alert( $.i18n("rdft-menu/alert-no-transform") );
            return;
        }

        var strProjectName =
            theProject.metadata.name
            .replace(/^\p{White_Space}+/u, '') // Leading Whitespace to none
            .replace(/\p{White_Space}+$/u, '') // Trailing Whitespace to none
            .replace(/[^\p{L}\p{N}_]/gu, '_') // Unprintable to _
            .replace(/\p{White_Space}+/gu, '-');  // Other Whitespace to -

        var form = document.createElement("form");

        $(form)
        .attr("method", "post")
        .attr("action", "command/core/export-rows/" +  strProjectName + "." + ext)
        .attr("target", "gridworks-export")
        .hide();

        $('<input />')
        .attr("name", "engine")
        .val( JSON.stringify( ui.browsingEngine.getJSON() ) )
        .appendTo(form);

        $('<input />')
        .attr("name", "project")
        .val(theProject.id)
        .appendTo(form);

        $('<input />')
        .attr("name", "format")
        .val(format)
        .appendTo(form);

        document.body.appendChild(form);

        window.open("Export " + format, "gridworks-export");
        form.submit();

        document.body.removeChild(form);
    }

    /*
    *  Extension Management: Setup Menu
    */
    static addExtensionMenu(barExtension) { // ...load all DOM elements before executing the following...
        barExtension.addExtensionMenu(
            {
                "id"        : "rdf-transform",
                "label"     : $.i18n('rdft'),
                "submenu"   : [
                    {
                        "id"    : "rdft/edit-rdf-transform",
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
