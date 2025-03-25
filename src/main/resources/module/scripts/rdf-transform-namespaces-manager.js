/*
 *  Class RDFTransformNamespacesManager
 *
 *  Manages the Namespaces for the current RDF Transform.
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

class RDFTransformNamespacesManager {
    /** @type RDFTransformDialog */
    #dialog;

    #theNamespaces;
    #theVocabManager;

    constructor(dialog) {
        this.#dialog = dialog;
        this.#theVocabManager = new RDFTransformVocabManager(this);

        // Namespaces have not been initialized...
        // Initialize after construction due to await'ing on defaults when needed!
    }

    async init() {
        this.#dialog.waitOnNamespaces();

        // Get existing namespaces (clone namespaces)...
        var theNamespaces = this.#dialog.getNamespaces();

        if ( ! theNamespaces ) {
            var data = null;
            this.#theNamespaces = {} // ...empty object, no namespaces
            try {
                data = await this.#getDefaults();
            }
            catch (evt) {
                // ...ignore error, no namespaces...
            }
            var bError = false;
            if (data !== null && "namespaces" in data) {
                this.#theNamespaces = data.namespaces; // ...new defaults namespaces
            }
            else { // (data === null || data.code === "error")
                bError = true;
            }
            // We might have namespace errors...
            if (bError) {
                // @ts-ignore
                alert( $.i18n('rdft-vocab/error-retrieve-default') );
            }
        }
        else {
            this.#theNamespaces = JSON.parse( JSON.stringify( theNamespaces ) );
            this.#saveNamespaces();
        }
        this.#renderNamespaces();
    }

    reset() {
        this.#dialog.waitOnNamespaces();
        // Get existing namespaces (clone namespaces)...
        this.#theNamespaces = JSON.parse( JSON.stringify( this.#dialog.getNamespaces() ) );
        this.#saveNamespaces();
        this.#renderNamespaces();
    }

    /*
     * Method: getDefaults()
     *
     *  Get the Default Namespaces from the server.  As this method returns a Promise, it expects
     *  the caller is an "async" function "await"ing the results of the Promise.
     *
     */
    #getDefaults() {
        return new Promise(
            (resolve, reject) => {
                // GET default namespaces in ajax
                // @ts-ignore
                $.ajax(
                    {   url  : "command/rdf-transform/get-default-namespaces",
                        type : "GET",
                        async: false, // ...wait on results
                        data : { "project" : theProject.id },
                        dataType : "json",
                        success : (result, strStatus, xhr) => { resolve(result); },
                        error   : (xhr, strStatus, error) => { resolve(null); }
                    }
                );
            }
        );
    }

    #saveNamespaces(onDoneSave) {
        Refine.postCSRF(
            "command/rdf-transform/save-namespaces",
            {   "project" : theProject.id,
                "namespaces" : this.#theNamespaces
            },
            (data) => { if (onDoneSave) { onDoneSave(data); } },
            "json"
        );
    }

    showManageWidget() {
        this.#theVocabManager.show( () => this.#renderNamespaces() );
    }

    #renderNamespaces() {
        this.#dialog.updateNamespaces(this.#theNamespaces);
    }

    isNull() {
        if (this.#theNamespaces === null) {
            return true;
        }
        return false;
    }

    isEmpty() {
        if (this.#theNamespaces === null || Object.keys(this.#theNamespaces).length === 0) {
            return true;
        }
        return false;
    }

    getNamespaces(){
        return this.#theNamespaces
    }

    removeNamespace(strPrefixFind) {
        if (strPrefixFind in this.#theNamespaces) {
            delete this.#theNamespaces[strPrefixFind];
            this.#dialog.updatePreview();
        }
    }

    addNamespace(strMessage, strPrefixGiven, onDoneAdd) {
        var dlgNamespaceAdder = new RDFTransformNamespaceAdder(this);
        dlgNamespaceAdder.show(
            strMessage,
            strPrefixGiven,
            (strPrefix, strNamespace) => {
                // NOTE: RDFTransformNamespaceAdder should have validated the
                //      prefix information, so no checks are required here.

                // Add the Prefix and its Namespace...
                this.#theNamespaces[strPrefix] = strNamespace;
                this.#saveNamespaces();
                this.#renderNamespaces();

                if (onDoneAdd) {
                    onDoneAdd(strPrefix);
                }
                this.#dialog.updatePreview();
            }
        );
    }

    hasPrefix(strPrefixFind) {
        if (strPrefixFind in this.#theNamespaces) {
            return true;
        }
        return false;
    }

    getNamespaceOfPrefix(strPrefixFind) {
        if (strPrefixFind in this.#theNamespaces) {
            return this.#theNamespaces[strPrefixFind];
        }
        return null;
    }
}
