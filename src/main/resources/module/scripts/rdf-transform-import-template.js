/*
 *  Class RDFImportTemplate
 *
 *  Import the RDF Transform template
 *
 *  Copyright 2025 Keven L. Ates
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
    static async importTemplate(elemInputImpTemplate) {
        // NOTE: No Server-Side processing required.  The current RDF Template
        //      always resides on the Client-Side.  Prior processing should
        //      save the prior template since we are importing over the current one.

        var theTransform = null;
        /** @type {string} */
        var strTemplate = null;

        if ('showOpenFilePicker' in window && typeof window.showOpenFilePicker === 'function') {
            try {
                strTemplate =
                    await RDFTransformCommon.readFile(
                        "json",
                        "application/json",
                        "RDF Transform (.json)"
                    );
            }
            catch (evt) { return null; }
        }
        // Otherwise, get file by input element...
        else {
            try {
                var file = null;
                elemInputImpTemplate.empty();
                // 1. Hook up Import RDF Template input file event handlers...
                const promise = this.#fetchTransformFile(elemInputImpTemplate);
                // 2. Trigger the file selection UI...
                elemInputImpTemplate.trigger('click');
                file = await promise; // ...and wait for the user to finish
                // 3. Load the selected RDF Template input file...
                if (file) strTemplate = await this.#fetchTransformInput(file);
            }
            catch (error) { return null; }
        }

        theTransform = JSON.parse(strTemplate);
        return theTransform;
    }

    static #fetchTransformFile(elemInputImpTemplate) {
        return new Promise(
            (resolve, reject) => {
                // Set file input event handlers...
                elemInputImpTemplate.on('cancel', () => { reject("Cancelled"); } );
                elemInputImpTemplate.on('change', (evt) => { resolve( evt.target.files[0] ); } );
            }
        );
    }

    static #fetchTransformInput(file) {
        return new Promise(
            (resolve, reject) => {
                if (file === null) resolve(null);
                const reader = new FileReader();
                // Set up FileReader event handlers...
                reader.onloadend = () => resolve(reader.result);
                reader.onerror = () => reject("Cancelled");
                // Process file...
                reader.readAsText(file);
            }
        );
    }

}
