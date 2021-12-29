/*
 *  Import Template
 */
class RDFImportTemplate
{
    static async importTemplate() {
        // NOTE: No Server-Side processing required.  The current RDF Template
        //      always resides on the Client-Side.  Prior processing should
        //      save the prior template since we are importing over the current one.
        const strTemplate = 
            await RDFTransformCommon.openFile(
                "json",
                "application/json",
                "RDF Template (.json)"
            );

        const theTransform = JSON.parse( strTemplate );
        return theTransform;
    }
}
