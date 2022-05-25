/*
 *  Language Management: Initialize
 *
 *    NOTE: We must retrieve and load language translation file syncronously so that
 *    the following $.i18n() language callouts process correctly.  An asynchronous
 *    load does not present the language elements for many of the display items that
 *    directly follow.
 */

/** @type {string} */
var dict = "";
/** @type {string} */
var lang = navigator.language.split("-")[0];
$.ajax(
    {   url: "command/core/load-language",
        type: 'POST',
        async: false, // ...wait on results...
        data: { module: "rdf-transform" },
        success: (data) => {
            dict = data['dictionary'];
            lang = data['lang'];
        }
    }
)
.always(
    () => { $.i18n().load(dict, lang); }
);

// ...end Language Management

/*
 *  Export Management: Setup Menu
 */
class RDFExporterMenuBar
{
    static exportRDF(format, ext) {
        if (! theProject.overlayModels.RDFTransform) {
            alert( $.i18n("rdft-menu/alert-no-transform") );
        }
        else {
            RDFExporterMenuBar.#exportRDFData(format, ext);
        }
    }

    static #exportRDFData(format, ext) {
        var name =
            theProject.metadata.name
            .replace(/^\p{White_Space}+/u, '')
            .replace(/\p{White_Space}+$/u, '')
            .replace(/[^\p{L}\p{N}_]/gu, '_')
            .replace(/\p{White_Space}+/gu, '-');
        /* DEBUG:
        alert("Project Name: " + theProject.metadata.name +
            "\nCalc'ed Name: " + name +
            "\nFormat: " + format +
            "\nExt: " + ext);
        */

        var form = document.createElement("form");

        $(form)
        .attr("method", "post")
        .attr("action", "command/core/export-rows/" + name + "." + ext)
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

        //window.open("about:blank", "gridworks-export");
        window.open("Export " + format, "gridworks-export");
        form.submit();

        document.body.removeChild(form);
    }
}

ExporterManager.MenuItems.push({}); // ...add separator

ExporterManager.MenuItems.push(
    {
        "id"        : "rdf-transform",
        "label"     : $.i18n('rdft'),
        "submenu"   : [
            //
            // PRETTY PRINTERS: (Graph) *** Not suggested for large graphs ***
            //
            {   "id"        : "rdf-transform/pretty",
                "label"     : $.i18n('rdft-menu/export-pretty'),
                "submenu"   : [
                    {   "id"    : "rdf-transform/pretty/exportRDFXML",
                        "label" : $.i18n("rdft-menu/rdf-xml-pretty"),
                        "click" : () => RDFExporterMenuBar.exportRDF("RDF/XML (Pretty)", "rdf")
                    },
                    {   "id"    : "rdf-transform/pretty/exportRDFTurtle",
                        "label" : $.i18n("rdft-menu/rdf-turtle-pretty"),
                        "click" : () => RDFExporterMenuBar.exportRDF("Turtle (Pretty)", "ttl")
                    },
                    {   "id"    : "rdf-transform/pretty/exportRDFTurtleStar",
                        "label" : $.i18n("rdft-menu/rdf-turtle-star-pretty"),
                        "click" : () => RDFExporterMenuBar.exportRDF("Turtle* (Pretty)", "ttls")
                    },
                    {   "id"    : "rdf-transform/pretty/exportRDFNotation3",
                        "label" : $.i18n("rdft-menu/rdf-n3-pretty"),
                        "click" : () => RDFExporterMenuBar.exportRDF("N3 (Pretty)", "n3")
                    },
                    {   "id"    : "rdf-transform/pretty/exportRDFNotation3Star",
                        "label" : $.i18n("rdft-menu/rdf-n3-star-pretty"),
                        "click" : () => RDFExporterMenuBar.exportRDF("N3* (Pretty)", "n3s")
                    },
                    {   "id"    : "rdf-transform/pretty/exportRDFTriG",
                        "label" : $.i18n("rdft-menu/rdf-trig-pretty"),
                        "click" : () => RDFExporterMenuBar.exportRDF("TriG (Pretty)", "trig")
                    },
                    {   "id"    : "rdf-transform/pretty/exportRDFTriGStar",
                        "label" : $.i18n("rdft-menu/rdf-trig-star-pretty"),
                        "click" : () => RDFExporterMenuBar.exportRDF("TriG* (Pretty)", "trigs")
                    },
                    {   "id"    : "rdf-transform/pretty/exportRDFJSONLD",
                        "label" : $.i18n("rdft-menu/rdf-jsonld-pretty"),
                        "click" : () => RDFExporterMenuBar.exportRDF("JSONLD (Pretty)", "jsonld")
                    },
                    //{   "id"    : "rdf-transform/pretty/exportRDFNDJSONLD",
                    //    "label" : $.i18n("rdft-menu/rdf-ndjsonld-pretty"),
                    //    "click" : () => RDFExporterMenuBar.exportRDF("NDJSONLD (Pretty)", "ndjsonld")
                    //},
                    {   "id"    : "rdf-transform/pretty/exportRDFJSON",
                        "label" : $.i18n("rdft-menu/rdf-json-pretty"),
                        "click" : () => RDFExporterMenuBar.exportRDF("RDF/JSON (Pretty)", "rj")
                    }
                ]
            },

            //
            // STREAM PRINTERS: per Subject (Stream)
            //
            {   "id"        : "rdf-transform/stream",
                "label"     : $.i18n('rdft-menu/export-stream'),
                "submenu"   : [
                    //
                    // BLOCKS PRINTERS: per Subject (Stream)
                    //
                    {   "id"    : "rdf-transform/stream/exportRDFTurtle",
                        "label" : $.i18n("rdft-menu/rdf-turtle-stream"),
                        "click" : () => RDFExporterMenuBar.exportRDF("Turtle (Blocks)", "ttl")
                    },
                    {   "id"    : "rdf-transform/stream/exportRDFTurtleStar",
                        "label" : $.i18n("rdft-menu/rdf-turtle-star-stream"),
                        "click" : () => RDFExporterMenuBar.exportRDF("Turtle* (Blocks)", "ttls")
                    },
                    {   "id"    : "rdf-transform/stream/exportRDFNotation3",
                        "label" : $.i18n("rdft-menu/rdf-n3-stream"),
                        "click" : () => RDFExporterMenuBar.exportRDF("N3 (Blocks)", "n3")
                    },
                    {   "id"    : "rdf-transform/stream/exportRDFNotation3Star",
                        "label" : $.i18n("rdft-menu/rdf-n3-star-stream"),
                        "click" : () => RDFExporterMenuBar.exportRDF("N3* (Blocks)", "n3s")
                    },
                    {   "id"    : "rdf-transform/stream/exportRDFTriG",
                        "label" : $.i18n("rdft-menu/rdf-trig-stream"),
                        "click" : () => RDFExporterMenuBar.exportRDF("TriG (Blocks)", "trig")
                    },
                    {   "id"    : "rdf-transform/stream/exportRDFTriGStar",
                        "label" : $.i18n("rdft-menu/rdf-trig-star-stream"),
                        "click" : () => RDFExporterMenuBar.exportRDF("TriG* (Blocks)", "trigs")
                    },

                    //
                    // LINE PRINTERS: triple, quad (Stream)
                    //
                    {   "id"    : "rdf-transform/stream/exportRDFNTriples",
                        "label" : $.i18n("rdft-menu/rdf-n-triples-stream"),
                        "click" : () => RDFExporterMenuBar.exportRDF("NTriples (Flat)", "nt")
                    },
                    {   "id"    : "rdf-transform/stream/exportRDFNTriplesStar",
                        "label" : $.i18n("rdft-menu/rdf-n-triples-star-stream"),
                        "click" : () => RDFExporterMenuBar.exportRDF("NTriples* (Flat)", "nts")
                    },
                    {   "id"    : "rdf-transform/stream/exportRDFNQuads",
                        "label" : $.i18n("rdft-menu/rdf-nquads-stream"),
                        "click" : () => RDFExporterMenuBar.exportRDF("NQuads (Flat)", "nq")
                    },
                    {   "id"    : "rdf-transform/stream/exportRDFNQuadsStar",
                        "label" : $.i18n("rdft-menu/rdf-nquads-star-stream"),
                        "click" : () => RDFExporterMenuBar.exportRDF("NQuads* (Flat)", "nqs")
                    },
                    {   "id"    : "rdf-transform/stream/exportRDFTriX",
                        "label" : $.i18n("rdft-menu/rdf-trix-stream"),
                        "click" : () => RDFExporterMenuBar.exportRDF("TriX", "xml")
                    },
                    {   "id"    : "rdf-transform/stream/exportRDFNull",
                        "label" : $.i18n("rdft-menu/rdf-null-stream"),
                        "click" : () => RDFExporterMenuBar.exportRDF("RDFNull (Test)", "rn")
                    },

                    //
                    // BINARY PRINTERS: (Stream)
                    //
                    // TODO: Uncomment the "RDFProtoBuf" export when OpenRefine is up-to-date on Jena
                    //{   "id"    : "rdf-transform/stream/exportRDFProto",
                    //    "label" : $.i18n("rdft-menu/rdf-proto-stream"),
                    //    "click" : () => RDFExporterMenuBar.exportRDF("RDFProtoBuf", "rp")
                    //},
                    {   "id"    : "rdf-transform/stream/exportRDFThrift",
                        "label" : $.i18n("rdft-menu/rdf-thrift-stream"),
                        "click" : () => RDFExporterMenuBar.exportRDF("RDFThrift", "rt")
                    },

                    //{   "id"    : "rdf-transform/stream/exportRDFBinaryRDF",
                    //    "label" : $.i18n("rdft-menu/rdf-binary"),
                    //    "click" : () => RDFExporterMenuBar.exportRDF("BinaryRDF", "brf")
                    //},
                    //{   "id"    : "rdf-transform/stream/exportRDFHDT",
                    //    "label" : $.i18n("rdft-menu/rdf-hdt"),
                    //    "click" : () => RDFExporterMenuBar.exportRDF("HDT", "hdt")
                    //},
                ]
            }

            //
            // TODO: Special RDFExporters - Are these even doable???
            //
            //{   "id"    : "rdf-transform/unknown/exportRDFa",
            //    "label" : $.i18n("rdft-menu/rdf-a"),
            //    "click" : () => RDFExporterMenuBar.exportRDF("RDFa", "xhtml")
            //},
            //{   "id"    : "rdf-transform/unknown/exportRDFSHACLC",
            //    "label" : $.i18n("rdft-menu/rdf-shaclc"),
            //    "click" : () => RDFExporterMenuBar.exportRDF("SHACLC", "sc")
            //},
        ]
    }
);

// ...end Export Management

/*
 *  Extension Management: Setup Menu
 */
$( function() { // ...load all DOM elements before executing the following...
    ExtensionBar.addExtensionMenu(
        {
            "id"        : "rdf-transform",
            "label"     : $.i18n('rdft'),
            "submenu"   : [
                {
                    "id"    : "rdf/edit-rdf-transform",
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
                    "id"    : "rdf/reset-rdf-transform",
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
});

// ...end Extension Management
