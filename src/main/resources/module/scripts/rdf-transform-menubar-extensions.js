/*
 *  Language Management: Initialize
 *
 *    Must retrieve and load language translation file syncronously so that
 *    the following $.i18n() language callouts process correctly.  An asynchronous
 *    load does not present the language elements for many of the display items.
 */
var dict = "";
var lang =
        navigator.language.split("-")[0] || 
        navigator.userLanguage.split("-")[0];
$.post(
    {   url: "command/core/load-language",
        async: false,
        data: { module: "rdf-transform" },
        success: (data) => {
		    dict = data['dictionary'];
            lang = data['lang'];
        }
    }
)
.always(
    () => {
        $.i18n().load(dict, lang);
    }
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
            RDFExporterMenuBar.#exportRDFRows(format, ext);
        }
    }

    static #exportRDFRows(format, ext) {
        var name =
            theProject.metadata.name
            .replace(/^\p{White_Space}+/u, '')
            .replace(/\p{White_Space}+$/u, '')
            .replace(/[^\p{L}\p{N}_]/gu, '_')
            .replace(/\p{White_Space}+/gu, '-');
        //alert("Project Name: " + theProject.metadata.name +
        //    "\nCalc'ed Name: " + name +
        //    "\nFormat: " + format +
        //    "\nExt: " + ext);

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
	{   "id"    : "exportRDFXML",
        "label" : $.i18n("rdft-menu/rdf-xml"),
        "click" : () => RDFExporterMenuBar.exportRDF("RDF", "rdf")
	}
);

ExporterManager.MenuItems.push(
	{   "id"    : "exportRDFNTriples",
        "label" : $.i18n("rdft-menu/rdf-n-triples"),
        "click" : () => RDFExporterMenuBar.exportRDF("N-Triples", "nt")
	}
);

ExporterManager.MenuItems.push(
    {   "id"    : "exportRDFTurtle",
        "label" : $.i18n("rdft-menu/rdf-turtle"),
        "click" : () => RDFExporterMenuBar.exportRDF("Turtle", "ttl")
    }
);

ExporterManager.MenuItems.push(
    {   "id"    : "exportRDFTurtleStar",
        "label" : $.i18n("rdft-menu/rdf-turtle-star"),
        "click" : () => RDFExporterMenuBar.exportRDF("Turtle-star", "ttls")
    }
);

ExporterManager.MenuItems.push(
    {   "id"    : "exportRDFNotation3",
        "label" : $.i18n("rdft-menu/rdf-n3"),
        "click" : () => RDFExporterMenuBar.exportRDF("N3", "n3")
    }
);

ExporterManager.MenuItems.push(
    {   "id"    : "exportRDFTriX",
        "label" : $.i18n("rdft-menu/rdf-trix"),
        "click" : () => RDFExporterMenuBar.exportRDF("TriX", "xml")
    }
);

ExporterManager.MenuItems.push(
    {   "id"    : "exportRDFTriG",
        "label" : $.i18n("rdft-menu/rdf-trig"),
        "click" : () => RDFExporterMenuBar.exportRDF("TriG", "trig")
    }
);

ExporterManager.MenuItems.push(
    {   "id"    : "exportRDFTriGStar",
        "label" : $.i18n("rdft-menu/rdf-trig-star"),
        "click" : () => RDFExporterMenuBar.exportRDF("TriG-star", "trigs")
    }
);

ExporterManager.MenuItems.push(
    {   "id"    : "exportRDFBinaryRDF",
        "label" : $.i18n("rdft-menu/rdf-binary"),
        "click" : () => RDFExporterMenuBar.exportRDF("BinaryRDF", "brf")
    }
);

ExporterManager.MenuItems.push(
    {   "id"    : "exportRDFNQuads",
        "label" : $.i18n("rdft-menu/rdf-nquads"),
        "click" : () => RDFExporterMenuBar.exportRDF("N-Quads", "nq")
    }
);

ExporterManager.MenuItems.push(
    {   "id"    : "exportRDFJSONLD",
        "label" : $.i18n("rdft-menu/rdf-jsonld"),
        "click" : () => RDFExporterMenuBar.exportRDF("JSON-LD", "jsonld")
    }
);

ExporterManager.MenuItems.push(
    {   "id"    : "exportRDFNDJSONLD",
        "label" : $.i18n("rdft-menu/rdf-ndjsonld"),
        "click" : () => RDFExporterMenuBar.exportRDF("NDJSON-LD", "ndjsonld")
    }
);

ExporterManager.MenuItems.push(
    {   "id"    : "exportRDFJSON",
        "label" : $.i18n("rdft-menu/rdf-json"),
        "click" : () => RDFExporterMenuBar.exportRDF("RDF/JSON", "rj")
    }
);

ExporterManager.MenuItems.push(
    {   "id"    : "exportRDFa",
        "label" : $.i18n("rdft-menu/rdf-a"),
        "click" : () => RDFExporterMenuBar.exportRDF("RDFa", "xhtml")
    }
);

ExporterManager.MenuItems.push(
    {   "id"    : "exportRDFHDT",
        "label" : $.i18n("rdft-menu/rdf-hdt"),
        "click" : () => RDFExporterMenuBar.exportRDF("HDT", "hdt")
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
                        const theDialog = new RDFTransformDialog(theProject.overlayModels.RDFTransform);
                        theDialog.initTransform();
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
