/*
 *  Language Management: Initialize
 *
 *    NOTE: We must retrieve and load language translation file syncronously so that
 *    the following $.i18n() language callouts process correctly.  An asynchronous
 *    load does not present the language elements for many of the display items that
 *    directly follow.
 */

/** @type {string} */
var strDict = "";
/** @type {string} */
var strLang = navigator.languages[1] || navigator.language.split("-")[0];
$.ajax(
    {   url: "command/core/load-language?",
        type: 'POST',
        async: false, // ...wait on results...
        data: { module: "rdf-transform" },
        success: (data) => {
            strDict = data['dictionary'];
            strLang = data['lang'];
        }
    }
)
.always(
    () => {
        $.i18n().load(strDict, strLang);
    }
);

// ...end Language Management

/*
 *  Export Management: Setup Menu
 */
class RDFExporterMenuBar
{
    static constructExportRDF(format, ext) {
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

        strType = " (Pretty)";

        strExp = "RDF/XML" + strType;
        objTypeSubSubMenuItem = {
            id : "rdf-transform/pretty/exportRDFXML",
            label : $.i18n("rdft-menu/rdf-xml-pretty"),
            click : () => RDFExporterMenuBar.#exportRDF(strExp, "rdf")
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        strExp = "Turtle" + strType;
        objTypeSubSubMenuItem = {
            id : "rdf-transform/pretty/exportRDFTurtle",
            label : $.i18n("rdft-menu/rdf-turtle-pretty"),
            click : () => RDFExporterMenuBar.#exportRDF(strExp, "ttl")
         } ;
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        strExp = "Turtle*" + strType;
        objTypeSubSubMenuItem = {
            id : "rdf-transform/pretty/exportRDFTurtleStar",
            label : $.i18n("rdft-menu/rdf-turtle-star-pretty"),
            click : () => RDFExporterMenuBar.#exportRDF(strExp, "ttls")
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        strExp = "N3" + strType;
        objTypeSubSubMenuItem = {
            id : "rdf-transform/pretty/exportRDFNotation3",
            label : $.i18n("rdft-menu/rdf-n3-pretty"),
            click : () => RDFExporterMenuBar.#exportRDF(strExp, "n3")
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        strExp = "N3*" + strType;
        objTypeSubSubMenuItem = {
            id : "rdf-transform/pretty/exportRDFNotation3Star",
            label : $.i18n("rdft-menu/rdf-n3-star-pretty"),
            click : () => RDFExporterMenuBar.#exportRDF(strExp, "n3s")
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        strExp = "TriG" + strType;
        objTypeSubSubMenuItem = {
            id : "rdf-transform/pretty/exportRDFTriG",
            label : $.i18n("rdft-menu/rdf-trig-pretty"),
            click : () => RDFExporterMenuBar.#exportRDF(strExp, "trig")
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        strExp = "TriG*" + strType;
        objTypeSubSubMenuItem = {
            id : "rdf-transform/pretty/exportRDFTriGStar",
            label : $.i18n("rdft-menu/rdf-trig-star-pretty"),
            click : () => RDFExporterMenuBar.#exportRDF(strExp, "trigs")
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

        strExp = "RDF/JSON" + strType;
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

        //
        // BLOCKS PRINTERS: per Subject (Stream)
        //
        strType = " (Blocks)";

        strExp = "Turtle" + strType;
        objTypeSubSubMenuItem = {
            id : "rdf-transform/stream/exportRDFTurtle",
            label : $.i18n("rdft-menu/rdf-turtle-stream"),
            click : () => RDFExporterMenuBar.#exportRDF(strExp, "ttl")
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        strExp = "Turtle*" + strType;
        objTypeSubSubMenuItem = {
            id : "rdf-transform/stream/exportRDFTurtleStar",
            label : $.i18n("rdft-menu/rdf-turtle-star-stream"),
            click : () => RDFExporterMenuBar.#exportRDF(strExp, "ttls")
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        strExp = "N3" + strType;
        objTypeSubSubMenuItem = {
            id : "rdf-transform/stream/exportRDFNotation3",
            label : $.i18n("rdft-menu/rdf-n3-stream"),
            click : () => RDFExporterMenuBar.#exportRDF(strExp, "n3")
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        strExp = "N3*" + strType;
        objTypeSubSubMenuItem = {
            id : "rdf-transform/stream/exportRDFNotation3Star",
            label : $.i18n("rdft-menu/rdf-n3-star-stream"),
            click : () => RDFExporterMenuBar.#exportRDF(strExp, "n3s")
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        strExp = "TriG" + strType;
        objTypeSubSubMenuItem = {
            id : "rdf-transform/stream/exportRDFTriG",
            label : $.i18n("rdft-menu/rdf-trig-stream"),
            click : () => RDFExporterMenuBar.#exportRDF(strExp, "trig")
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        strExp = "TriG*" + strType;
        objTypeSubSubMenuItem = {
            id : "rdf-transform/stream/exportRDFTriGStar",
            label : $.i18n("rdft-menu/rdf-trig-star-stream"),
            click : () => RDFExporterMenuBar.#exportRDF(strExp, "trigs")
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        //
        // LINE PRINTERS: triple, quad (Stream)
        //
        strType = " (Flat)";

        strExp = "NTriples" + strType;
        objTypeSubSubMenuItem = {
            id : "rdf-transform/stream/exportRDFNTriples",
            label : $.i18n("rdft-menu/rdf-n-triples-stream"),
            click : () => RDFExporterMenuBar.#exportRDF(strExp, "nt")
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        strExp = "NTriples*" + strType;
        objTypeSubSubMenuItem = {
            id : "rdf-transform/stream/exportRDFNTriplesStar",
            label : $.i18n("rdft-menu/rdf-n-triples-star-stream"),
            click : () => RDFExporterMenuBar.#exportRDF(strExp, "nts")
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        strExp = "NQuads" + strType;
        objTypeSubSubMenuItem = {
            id : "rdf-transform/stream/exportRDFNQuads",
            label : $.i18n("rdft-menu/rdf-nquads-stream"),
            click : () => RDFExporterMenuBar.#exportRDF(strExp, "nq")
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        strExp = "NQuads*" + strType;
        objTypeSubSubMenuItem = {
            id : "rdf-transform/stream/exportRDFNQuadsStar",
            label : $.i18n("rdft-menu/rdf-nquads-star-stream"),
            click : () => RDFExporterMenuBar.#exportRDF(strExp, "nqs")
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        strExp = "TriX" + strType;
        objTypeSubSubMenuItem = {
            id : "rdf-transform/stream/exportRDFTriX",
            label : $.i18n("rdft-menu/rdf-trix-stream"),
            click : () => RDFExporterMenuBar.#exportRDF(strExp, "xml")
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        //
        // DUMMY PRINTERS: (Stream)
        //
        strType = " (Test)";

        strExp = "RDFNull" + strType;
        objTypeSubSubMenuItem = {
            id : "rdf-transform/stream/exportRDFNull",
            label : $.i18n("rdft-menu/rdf-null-stream"),
            click : () => RDFExporterMenuBar.#exportRDF(strExp, "rn")
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        //
        // BINARY PRINTERS: (Stream)
        //
        strType = " (Binary)";

        // TODO: Uncomment the "RDFProtoBuf" export when OpenRefine is up-to-date on Jena
        //strExp = "RDFProtoBuf" + strType;
        //objTypeSubSubMenuItem = {
        //    id : "rdf-transform/stream/exportRDFProto",
        //    label : $.i18n("rdft-menu/rdf-proto-stream"),
        //    click : () => RDFExporterMenuBar.#exportRDF(strExp, "rp")
        //};
        //objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        strExp = "RDFThrift" + strType;
        objTypeSubSubMenuItem = {
            id : "rdf-transform/stream/exportRDFThrift",
            label : $.i18n("rdft-menu/rdf-thrift-stream"),
            click : () => RDFExporterMenuBar.#exportRDF(strExp, "rt")
        };
        objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        //strExp = "BinaryRDF" + strType;
        //objTypeSubSubMenuItem = {
        //    id : "rdf-transform/stream/exportRDFBinaryRDF",
        //    label : $.i18n("rdft-menu/rdf-binary"),
        //    click : () => RDFExporterMenuBar.#exportRDF(strExp, "brf")
        //};
        //objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        //strExp = "HDT" + strType;
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
        objTypeSubMenuItem = {
            id : "rdf-transform/special",
            label : $.i18n('rdft-menu/export-special'),
            submenu : []
        };

        //strType = " (Special)";

        //strExp = "RDFa" + strType;
        //objTypeSubSubMenuItem = {
        //    id : "rdf-transform/special/exportRDFa",
        //    label : $.i18n("rdft-menu/rdf-a-special"),
        //    click : () => RDFExporterMenuBar.#exportRDF(strExp, "xhtml")
        //};
        //objTypeSubMenuItem.submenu.push(objTypeSubSubMenuItem);

        //strExp = "SHACLC" + strType;
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
}

ExporterManager.MenuItems.push( {} ); // ...add separator
ExporterManager.MenuItems.push( RDFExporterMenuBar.constructExportRDF() );

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
