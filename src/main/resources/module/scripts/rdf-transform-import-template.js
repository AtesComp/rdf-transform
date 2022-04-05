/*
 *  Import Template
 */
class RDFImportTemplate
{
    static async importTemplate() {
        // NOTE: No Server-Side processing required.  The current RDF Template
        //      always resides on the Client-Side.  Prior processing should
        //      save the prior template since we are importing over the current one.
        /** @type {string} */
        var strTemplate = null;
        //var waitOnOpenFile =
        //    async () => {
        //        await RDFTransformCommon.openFile(
        //            "json",
        //            "application/json",
        //            "RDF Transform (.json)"
        //        );
        //    };
        try {
            //strTemplate = await waitOnOpenFile();
            strTemplate =
                await RDFTransformCommon.readFile(
                    "json",
                    "application/json",
                    "RDF Transform (.json)"
                );
        }
        catch (evt) {
            return null;
        }
        //    .catch( (error) => {
        //        theTransform = null;
        //        // ...ignore...
        //    });
        return JSON.parse(strTemplate);
    }
}
