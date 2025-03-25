/*
 *  Class RDFTransformResourceDialog
 *
 *  Manages the resources for the RDF Transform dialog.
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

/*
 *  The following is extracted from rdf-transform-resource.js
 *
 *  Reserved code not currently used by any process, class, or function.
 *
 *  *******************************************************
 *  * DO NOT REPLACE RDFTransformResource WITH THIS CODE!!! *
 *  *******************************************************
 *  Instead, add or modify code as needed.
 *
 */

class RDFTransformResourceDialog {
    async #extractPrefixLocalPart(strIRI, strResp) {
        /** @type {{prefix?: string, localPart?: string}} */
        var obj = null;
        var iPrefixedIRI = await RDFTransformCommon.isPrefixedQName(strIRI);
        if ( iPrefixedIRI === 1 ) { // ...Prefixed IRI
            var strPrefix = RDFTransformCommon.getPrefixFromQName(strIRI);
            var strLocalPart = RDFTransformCommon.getSuffixFromQName(strIRI);
            // IRI   = Namespace of Prefix + strLocalPart
            // CIRIE = strResPrefix + ":" + strLocalPart
            obj = {};
            obj.prefix = strPrefix;       // Given Prefix
            obj.localPart = strLocalPart; // Path portion of IRI
            this.#onDone(obj);
        }
        else if ( iPrefixedIRI === 0 ) { // ...Full IRI
            // IRI   = Namespace of Prefix + strLocalPart
            // CIRIE = strResPrefix + ":" + strLocalPart
            obj = {};
            obj.prefix = null;      // No Prefix
            obj.localPart = strIRI; // Full IRI
            this.#onDone(obj);
        }
        else { // iPrefixedIRI === -1 // ...Bad IRI
            alert(
                // @ts-ignore
                $.i18n('rdft-dialog/alert-iri') + "\n" +
                // @ts-ignore
                $.i18n('rdft-dialog/alert-iri-invalid') + "\n" +
                strResp + strIRI
            );
        }
    }

    async #addPrefixLocalPart(strIRI, strResp) {
        /** @type {{prefix?: string, localPart?: string}} */
        var obj = null;

        // Does the IRI look like a prefixed IRI?
        var iPrefixedIRI = await RDFTransformCommon.isPrefixedQName(strIRI);
        // Is it a prefixed IRI?
        if ( iPrefixedIRI === 1 ) {
            MenuSystem.dismissAll();

            // Divide the IRI into Prefix and LocalPart portions...
            var strPrefix = RDFTransformCommon.getPrefixFromQName(strIRI);
            var strLocalPart = RDFTransformCommon.getSuffixFromQName(strIRI);

            // Is there an existing prefix matching the given prefix?
            if ( strPrefix === "" || this.#dialog.getNamespacesManager().hasPrefix(strPrefix) ) {
                // ...yes (BaseIRI or already managed), add resource...
                obj = {};
                obj.prefix    = strPrefix;    // Given Prefix
                obj.localPart = strLocalPart; // Path portion of IRI
                this.#onDone(obj);
            }
            // No, then this prefix is not using the BaseIRI prefix and is not managed...
            else {
                // ...create prefix (which may change in here) and add (re-prefixed) resource...
                //
                // NOTE: We are passing a function to RDFTransformNamespacesManager.addNamespace() which,
                //      in turn, passes it as an event function to RDFTransformNamespaceAdder.show().
                this.#dialog.getNamespacesManager().addNamespace(
                    // @ts-ignore
                    $.i18n('rdft-dialog/unknown-pref') + ': <em>' + strPrefix + '</em> ',
                    strPrefix,
                    (strResPrefix) => {
                        // NOTE: Only the prefix (without the related IRI) is returned.  We don't need
                        //      the IRI...addNamespace() added it.  We will get the IRI from the prefix
                        //      manager later to ensure the IRI is present.
                        // Do the original and resulting prefix match?
                        // NOTE: It can change via edits in RDFTransformNamespaceAdder.show()
                        if ( strPrefix.normalize() === strResPrefix.normalize() ) {
                            // ...yes, set as before...
                            obj = {};
                            obj.prefix    = strPrefix;    // Given Prefix
                            obj.localPart = strLocalPart; // Path portion of IRI
                        }
                        // No, then adjust...
                        else {
                            // ...get new Namespace of the Prefix to validate...
                            var strResNamespace =
                                this.#dialog.getNamespacesManager().getNamespaceOfPrefix(strResPrefix);
                            // Ensure the prefix's IRI was added...
                            if ( strResNamespace != null ) {
                                obj = {};
                                obj.prefix    = strResPrefix; // New Prefix
                                obj.localPart = strLocalPart; // Path portion of IRI
                            }
                            // If not, abort the resource addition with a null obj...
                        }

                        // Do we have a good resource (obj) to add?
                        if (obj !== null) {
                            // ...yes, add resource...
                            this.#onDone(obj);
                        }
                    }
                );
            }
        }
        // Is it a full IRI?
        else if ( iPrefixedIRI === 0 ) {
            MenuSystem.dismissAll();

            // ...take it as is...
            //new RDFTransformResourceResolveDialog(this.#element, data, this.#onDone);
            /** @type {{prefix?: string, localPart?: string}} */
            obj = {};
            obj.prefix = null;  // No Prefix
            obj.localPart = strIRI; // Full IRI
            this.#onDone(obj);
        }
        // Is it a BAD IRI?
        else { // iPrefixedIRI === -1
            alert(
                // @ts-ignore
                $.i18n('rdft-dialog/alert-iri') + "\n" +
                // @ts-ignore
                $.i18n('rdft-dialog/alert-iri-invalid') + "\n" +
                strResp + strIRI
            );
        }
    }
}

/*
 *  CLASS RDFTransformResourceResolveDialog
 *
 *  The resource resolver for the resource manager dialog
 */
class Reserve_RDFTransformResourceResolveDialog {
    #onDone;

    constructor(element, defaultVal, onDone) {
        this.#onDone = onDone;

        var menu = MenuSystem.createMenu().width('400px'); // ...6:1 on input size
        menu.html(
'<div class="rdf-transform-menu-search">' +
  '<span class="rdf-transform-node-label">IRI: ' +
    '<small>(' +
      // @ts-ignore
      $.i18n('rdft-dialog/resolve') +
    ')</small>' +
  '</span>' +
  '<input type="text" size="50" bind="rdftNewResourceIRI"><br/>' +
  '<button class="button" bind="buttonApply">' +
    // @ts-ignore
    $.i18n('rdft-buttons/apply') +
  '</button>' +
  '<button class="button" bind="buttonCancel">' +
    // @ts-ignore
    $.i18n('rdft-buttons/cancel') +
  '</button>' +
'</div>'
        );
        MenuSystem.showMenu(menu, () => {} );
        // @ts-ignore
        MenuSystem.positionMenuLeftRight(menu, $(element));

        var elements = DOM.bind(menu);
        elements.rdftNewResourceIRI
        .val(defaultVal)
        .focus()
        .select();

        elements.buttonCancel
        .on("click", () => { MenuSystem.dismissAll(); } );

        elements.buttonApply
        .on("click",
            async () => {
                var strIRI = elements.rdftNewResourceIRI.val();
                if (!strIRI) {
                    // @ts-ignore
                    alert( $.i18n('rdft-dialog/alert-iri') );
                    return;
                }
                if ( ! await RDFTransformCommon.validateIRI(strIRI) ) {
                    alert(
                        // @ts-ignore
                        $.i18n('rdft-dialog/alert-iri') + "\n" +
                        // @ts-ignore
                        $.i18n('rdft-dialog/alert-iri-invalid') + "\n" +
                        strIRI
                    );
                    return;
                }
                MenuSystem.dismissAll();
                //if (strIRI.charAt(0) === ':') {
                //    strIRI = strIRI.substring(1);
                //}
                var obj = {
                    "iri"   : strIRI, // Full IRI
                    "cirie" : strIRI  // Prefixed IRI
                };
                this.#onDone(obj);
            }
        );
    }
}
