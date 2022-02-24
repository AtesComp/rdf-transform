import { RDFTransformCommon } from "./rdf-transform-common";

/*
 *  Import Template
 */
class RDFImportTemplate
{
    static importTemplate() {
        // NOTE: No Server-Side processing required.  The current RDF Template
        //      always resides on the Client-Side.  Prior processing should
        //      save the prior template since we are importing over the current one.
        /** @type {string} */
        var strTemplate = null;
        var waitOnOpenFile =
            async () => {
                await RDFTransformCommon.openFile(
                    "json",
                    "application/json",
                    "RDF Template (.json)"
                );
            };
        try {
            // @ts-ignore
            strTemplate = waitOnOpenFile();
            return JSON.parse( strTemplate );
        }
        catch (evt) {
            return null;
        }
        //    .catch( (error) => {
        //        theTransform = null;
        //        // ...ignore...
        //    });
    }
}

export { RDFImportTemplate }
