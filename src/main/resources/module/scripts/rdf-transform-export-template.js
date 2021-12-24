/*
 *  Export Template
 */
class RDFExportTemplate
{
    static exportTemplate() {
        if (! theProject.overlayModels.RDFTransform) {
            alert( $.i18n("rdft-menu/alert-no-transform") );
        }
        else {
            RDFExporterMenuBar.#exportTemplateData();
        }
    }

    static #exportTemplateData() {
        var name =
            theProject.metadata.name
            .replace(/^\p{White_Space}+/u, '')
            .replace(/\p{White_Space}+$/u, '')
            .replace(/[^\p{L}\p{N}_]/gu, '_')
            .replace(/\p{White_Space}+/gu, '-');
        //alert("Project Name: " + theProject.metadata.name +
        //    "\nCalc'ed Name: " + name);

        // TODO: BAD FORM
        var form = document.createElement("form");

        $(form)
        .attr("method", "post")
        .attr("action", "command/rdf-tranform/export-rdf-template/" + name + "." + ext)
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
        .val("json")
        .appendTo(form);

        document.body.appendChild(form);

        //window.open("about:blank", "gridworks-export");
        window.open("Export RDF Template", "gridworks-export");
        form.submit();

        document.body.removeChild(form);
    }
}
