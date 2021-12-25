/*
 *  Export Template
 */
class RDFExportTemplate
{
    static exportTemplate() {
        // NOTE: No Server-Side processing required.  The current RDF Template
        //      always resides on the Client-Side.  Prior processing should
        //      save the template since we are exporting the current one.
        const theTransform = theProject.overlayModels.RDFTransform;
        if (! theTransform) {
            alert( $.i18n("rdft-menu/alert-no-transform") );
            return;
        }

        const strTemplate = JSON.stringify( theTransform );
        const strFilename =
            theProject.metadata.name
            .replace(/^\p{White_Space}+/u, '') // ...trim from beginning
            .replace(/\p{White_Space}+$/u, '') // ...trim from end
            .replace(/[^\p{L}\p{N}_]/gu, '_') // ...convert non-char to "_"
            .replace(/\p{White_Space}+/gu, '-'); // ...convert sp to '-'
        //alert("Project Name: " + theProject.metadata.name +
        //    "\nCalc'ed Name: " + strFilename);

        RDFTransformCommon.saveFile(
            strTemplate, strFilename, "json",
            "application/json",
            "RDF Template (.json)"
        );
    }
}
