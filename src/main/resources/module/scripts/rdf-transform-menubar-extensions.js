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
            {   "id"    : "exportRDFXML",
                "label" : $.i18n("rdft-menu/rdf-xml"),
                "click" : () => RDFExporterMenuBar.exportRDF("RDF/XML", "rdf")
            },
            {   "id"    : "exportRDFNTriples",
                "label" : $.i18n("rdft-menu/rdf-n-triples"),
                "click" : () => RDFExporterMenuBar.exportRDF("N-Triples", "nt")
            },
            {   "id"    : "exportRDFTurtle",
                "label" : $.i18n("rdft-menu/rdf-turtle"),
                "click" : () => RDFExporterMenuBar.exportRDF("Turtle", "ttl")
            },
            {   "id"    : "exportRDFTurtleStar",
                "label" : $.i18n("rdft-menu/rdf-turtle-star"),
                "click" : () => RDFExporterMenuBar.exportRDF("Turtle-star", "ttls")
            },
            {   "id"    : "exportRDFNotation3",
                "label" : $.i18n("rdft-menu/rdf-n3"),
                "click" : () => RDFExporterMenuBar.exportRDF("N3", "n3")
            },
            {   "id"    : "exportRDFTriX",
                "label" : $.i18n("rdft-menu/rdf-trix"),
                "click" : () => RDFExporterMenuBar.exportRDF("TriX", "xml")
            },
            {   "id"    : "exportRDFTriG",
                "label" : $.i18n("rdft-menu/rdf-trig"),
                "click" : () => RDFExporterMenuBar.exportRDF("TriG", "trig")
            },
            {   "id"    : "exportRDFTriGStar",
                "label" : $.i18n("rdft-menu/rdf-trig-star"),
                "click" : () => RDFExporterMenuBar.exportRDF("TriG-star", "trigs")
            },
            {   "id"    : "exportRDFBinaryRDF",
                "label" : $.i18n("rdft-menu/rdf-binary"),
                "click" : () => RDFExporterMenuBar.exportRDF("BinaryRDF", "brf")
            },
            {   "id"    : "exportRDFNQuads",
                "label" : $.i18n("rdft-menu/rdf-nquads"),
                "click" : () => RDFExporterMenuBar.exportRDF("N-Quads", "nq")
            },
            {   "id"    : "exportRDFJSONLD",
                "label" : $.i18n("rdft-menu/rdf-jsonld"),
                "click" : () => RDFExporterMenuBar.exportRDF("JSON-LD", "jsonld")
            },
            {   "id"    : "exportRDFNDJSONLD",
                "label" : $.i18n("rdft-menu/rdf-ndjsonld"),
                "click" : () => RDFExporterMenuBar.exportRDF("NDJSON-LD", "ndjsonld")
            },
            {   "id"    : "exportRDFJSON",
                "label" : $.i18n("rdft-menu/rdf-json"),
                "click" : () => RDFExporterMenuBar.exportRDF("RDF/JSON", "rj")
            },
            {   "id"    : "exportRDFa",
                "label" : $.i18n("rdft-menu/rdf-a"),
                "click" : () => RDFExporterMenuBar.exportRDF("RDFa", "xhtml")
            },
            {   "id"    : "exportRDFHDT",
                "label" : $.i18n("rdft-menu/rdf-hdt"),
                "click" : () => RDFExporterMenuBar.exportRDF("HDT", "hdt")
            }
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
                        const theDialog = new RDFTransformDialog();
                        theDialog.initTransform(theProject.overlayModels.RDFTransform);
                    }
				},
				{
					"id"    : "rdf/reset-rdf-transform",
					"label" : $.i18n('rdft-menu/reset') + "...",
					"click" : () => {
                        const theDialog = new RDFTransformDialog();
                        theDialog.initTransform();
                    }
				}
            ]
        }
    )
});

// ...end Extension Management
