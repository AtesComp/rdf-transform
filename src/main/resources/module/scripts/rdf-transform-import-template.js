/*
 *  Class RDFImportTemplate
 *
 *  Import the RDF Transform template
 *
 *  Copyright 2024 Keven L. Ates
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
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
