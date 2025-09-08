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

        // Get the default namespaces...
        var objDefaultNamespaces = null;
        try {
            var data = null;
            data = await this.#getDefaults();
            if (data !== null && "namespaces" in data) objDefaultNamespaces = data.namespaces;
        }
        catch (evt) {
            // ...ignore error, no default namespaces...
        }

        // Get existing namespaces...
        var theNamespaces = this.#dialog.getNamespaces();

        if ( ! theNamespaces ) {
            // Get default namespaces...
            this.#theNamespaces = {} // ...empty object, no namespaces
            var bError = false;
            if (objDefaultNamespaces !== null) {
                // Clone default namespaces...
                this.#theNamespaces = JSON.parse( JSON.stringify(objDefaultNamespaces) ); // ...new defaults namespaces
            }
            else { // (objDefaultNamespaces === null)
                bError = true;
            }
            // We might have namespace errors...
            if (bError) {
                // @ts-ignore
                alert( $.i18n('rdft-vocab/error-retrieve-default') );
            }
        }
        else {
            // Clone existing namespaces...
            var theNamespacesJSON = JSON.parse( JSON.stringify( theNamespaces ) );

            // Check Namespaces for version update...
            Object.keys(theNamespacesJSON).forEach(
                strPrefix => {
                    // If a Namespace object is a NEW version...
                    if ( typeof theNamespacesJSON[strPrefix] === 'object' && "namespace" in theNamespacesJSON[strPrefix] ) return;

                    // Otherwise, update the OLD version...
                    var strNamespace = theNamespacesJSON[strPrefix];
                    var strLocation = "";
                    var strLocType = "NONE";
                    if (objDefaultNamespaces !== null && strPrefix in objDefaultNamespaces) {
                        strNamespace = objDefaultNamespaces[strPrefix].namespace;
                        strLocation = objDefaultNamespaces[strPrefix].location;
                        strLocType = objDefaultNamespaces[strPrefix].loctype;
                    }
                    theNamespacesJSON[strPrefix] = {
                        "namespace": strNamespace,
                        "location": strLocation,
                        "loctype": strLocType
                    };
                }
            );
            this.#theNamespaces = theNamespacesJSON;
        }
        this.#saveNamespaces();
        this.#renderNamespaces();
    }

    async reset() {
        await this.init();
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
                    {   url  : gstrCommandRDFTransform + gstrGetDefaultNamespaces,
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
            gstrCommandRDFTransform + gstrSaveNamespaces,
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
            (strPrefix, strNamespace, strLocation, strLocType) => {
                // NOTE: RDFTransformNamespaceAdder should have validated the
                //      prefix information, so no checks are required here.

                // Add the Prefix and its Namespace...
                this.#theNamespaces[strPrefix] = {
                    "namespace": strNamespace,
                    "location": strLocation,
                    "loctype": strLocType
                };
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
            if (typeof this.#theNamespaces[strPrefixFind] === "string") return this.#theNamespaces[strPrefixFind];
            else return this.#theNamespaces[strPrefixFind].namespace;
        }
        return null;
    }
}
