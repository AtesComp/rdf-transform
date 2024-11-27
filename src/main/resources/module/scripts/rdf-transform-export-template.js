/*
 *  Class RDFExportTemplate
 *
 *  Export the RDF Transform template
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

class RDFExportTemplate
{
    static exportTemplate( theTransform ) {
        // NOTE: No Server-Side processing required.  The current RDF Template
        //      always resides on the Client-Side.  Prior processing should
        //      save the template since we are exporting the current one.
        const strTemplate = JSON.stringify( theTransform );
        const strFilename =
            theProject.metadata.name
            .replace(/^\p{White_Space}+/u, '') // ...trim from beginning
            .replace(/\p{White_Space}+$/u, '') // ...trim from end
            .replace(/[^\p{L}\p{N}_]/gu, '_') // ...convert non-char to "_"
            .replace(/\p{White_Space}+/gu, '-'); // ...convert sp to '-'

        //var waitOnSaveFile =
        //    async () => {
        //        await RDFTransformCommon.saveFile(
        //            strTemplate, strFilename, "json",
        //            "application/json",
        //            "RDF Transform (.json)"
        //        );
        //    };
        try {
            //waitOnSaveFile();
            RDFTransformCommon.saveFile(
                strTemplate, strFilename, "json",
                "application/json",
                "RDF Transform (.json)"
            );
        }
        catch (evt) {
            // ...ignore...
        }
    }
}
