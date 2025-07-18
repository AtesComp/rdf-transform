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

class RDFTransformResourceDialog {
    #strText;
    #elemPosition;
    #strDefault;
    #strLookForType;
    #projectID;
    /** @type RDFTransformDialog */
    #dialog;
    #onDone;

    constructor(strText, elemPosition, strDefault, strLookForType, projectID, dialog, onDone) {
        this.#strText = strText;
        this.#elemPosition = elemPosition;
        this.#strDefault = strDefault;
        this.#strLookForType = strLookForType;
        this.#projectID = projectID;
        this.#dialog = dialog;
        this.#onDone = onDone;
    }

    show() {
        // Can we finish?  If not, there is no point in continuing...
        if ( ! this.#onDone ) {
            alert(
                // @ts-ignore
                $.i18n('rdft-dialog/error') + "\n" +
                // @ts-ignore
                $.i18n("rdft-dialog/missing-proc") );
            return;
        }

        var menu = MenuSystem.createMenu().width('331px'); // ...331px fits Suggest Term
        menu.html(
'<div id="rdf-transform-menu-search" class="rdf-transform-menu-search">' +
  '<span>' +
    // @ts-ignore
    $.i18n('rdft-dialog/search-for') + ' ' + this.#strLookForType + ":" +
  '</span>' +
  '<input type="text" bind="rdftNewResourceIRI" >' +
'</div>'
        );

        MenuSystem.showMenu(menu, () => {} );
        // @ts-ignore
        MenuSystem.positionMenuLeftRight(menu, $(this.#elemPosition));

        var elements = DOM.bind(menu);
        elements.rdftNewResourceIRI
        .val(this.#strDefault)
        .suggestTerm(
            {   "project" : this.#projectID.toString(),
                "type"    : this.#strLookForType,
                "parent"  : '.rdf-transform-menu-search'
            }
        )
        .bind('fb-select', // ...select existing item...
            async (evt, data) => {
                // Variable "data" is a JSON object from the RDFTransform SearchResultItem class
                // containing key:value entries:
                //  iri, label, desc, prefix, namespace, localPart, description

                MenuSystem.dismissAll();

                var strIRI = null;
                var strLabel = null;
                var strDesc = null;
                var strPrefix = null;
                var strNamespace = null;
                var strLocalPart = null;
                var strLongDescription = null;

                if ( data !== null && typeof data === 'object' && !Array.isArray(data) ) {
                    if (RDFTransform.gstrIRI in data) {
                        strIRI = data.iri;
                    }
                    if ("label" in data) {
                        strLabel = data.label;
                    }
                    if ("desc" in data) {
                        strDesc = data.desc;
                    }
                    if (RDFTransform.gstrPrefix in data) {
                        strPrefix = data.prefix;
                    }
                    if ("namespace" in data) {
                        strNamespace = data.namespace;
                    }
                    if (RDFTransform.gstrLocalPart in data) {
                        strLocalPart = data.localPart;
                    }
                    if ("description" in data) {
                        strLongDescription = data.description;
                    }
                }
                /* DEBUG
                alert("DEBUG: Select: Existing Item:\n" +
                        "   IRI: " + strIRI + "\n" +
                        " Label: " + strLabel + "\n" +
                        "  Desc: " + strDesc + "\n" +
                        "Prefix: " + strPrefix + "\n" +
                        "    NS: " + strNamespace + "\n" +
                        " lPart: " + strLocalPart + "\n" +
                        " LDesc: " + strLongDescription
                );
                */

                /** @type {{prefix?: string, localPart?: string}} */
                var obj = null;
                if (strLocalPart) { // ...not null or ""
                    if (strPrefix != null) {
                        // Prefixed...
                        obj = {};
                        obj.prefix = strPrefix;
                        obj.localPart = strLocalPart;
                    }
                    else if (strNamespace != null) {
                        // Not Prefixed: Full IRI...
                        obj = {};
                        obj.prefix = null;
                        obj.localPart = strNamespace + strLocalPart;
                    }
                }
                else if (strIRI) {
                    // Full IRI...
                    //await this.#extractPrefixLocalPart(strIRI,"  IRI: ");
                    obj = {};
                    obj.prefix = null;      // No Prefix
                    obj.localPart = strIRI; // Full IRI
                }

                if (obj !== null) {
                    // Process existing or not-added IRI object...
                    this.#onDone(obj);
                }
                else {
                    alert(
                        // @ts-ignore
                        $.i18n('rdft-dialog/alert-iri') + "\n" +
                        // @ts-ignore
                        $.i18n('rdft-dialog/alert-iri-invalid') + "\n" +
                        "Invalid Resource object for selection!\n" + // TODO: $.i18n()
                        "IRI: " + strIRI
                    );
                }
            }
        )
        .bind('fb-select-new', // ...add new item...
            async (evt, data) => {
                // Variable "data" is the raw IRI input value from the dialog

                var strIRI = null;
                var strLabel = null;
                var strDesc = null;
                var strPrefix = null;
                var strNamespace = null;
                var strLocalPart = null;
                var strLongDescription = null;

                // If there is a possible IRI...
                if ( data !== null && typeof data === 'string' ) {
                    strIRI = data;
                    // A leading ':' for Base IRI encoding is an invalid IRI, so remove for test...
                    var strTestIRI = strIRI;
                    const bBaseIRI = (strIRI[0] === ':');
                    if (bBaseIRI) {
                        strTestIRI = strIRI.substring(1);
                    }
                    // Does the IRI look like a prefixed IRI?
                    //      1 : Yes | 0 : No, but IRI | -1 : Not an IRI
                    var iPrefixedIRI = await RDFTransformCommon.isPrefixedQName(strTestIRI);
                    // Is it an IRI?
                    if ( iPrefixedIRI >= 0 ) {
                        MenuSystem.dismissAll();

                        // If it's a good BaseIRI prefixed IRI...
                        if (bBaseIRI) {
                            iPrefixedIRI = 2; // ...then it's a BaseIRI prefixed IRI
                        }

                        // Is it a prefixed IRI?
                        if ( iPrefixedIRI === 1 ) {
                            // Divide the IRI into Prefix and LocalPart portions...
                            strPrefix = RDFTransformCommon.getPrefixFromQName(strIRI);
                            strLocalPart = RDFTransformCommon.getSuffixFromQName(strIRI);
                            // Get the Namespace of the Prefix...
                            strNamespace = this.#dialog.getNamespacesManager().getNamespaceOfPrefix(strPrefix);
                            // Get the Full IRI from the Prefixed IRI...
                            strIRI =
                                RDFTransformCommon.getFullIRIFromQName(
                                    strIRI,
                                    this.#dialog.getBaseIRI(),
                                    this.#dialog.getNamespacesManager().getNamespaces()
                                );
                            strLabel = strIRI;
                        }
                        // or Is it an BaseIRI prefixed IRI?
                        else if ( iPrefixedIRI === 2 ) {
                            strPrefix = ""; // ...use Base IRI Prefix
                            strNamespace = this.#dialog.getBaseIRI();
                            strLocalPart = strIRI.substring(1);
                            strLabel = strIRI;
                            strIRI = strNamespace + strLocalPart;
                        }
                        // or Is it an non-prefixed IRI?
                        else if ( iPrefixedIRI === 0 ) {
                            strLabel = strIRI; // ...take it as is...
                        }
                    }
                    // Otherwise, it's a BAD IRI!
                }
                /* DEBUG
                alert("DEBUG: Select New: Add Item:\n" +
                    "   IRI: " + strIRI + "\n" +
                    " Label: " + strLabel + "\n" +
                    "  Desc: " + strDesc + "\n" +
                    "Prefix: " + strPrefix + "\n" +
                    "    NS: " + strNamespace + "\n" +
                    " lPart: " + strLocalPart + "\n" +
                    " LDesc: " + strLongDescription
                );
                */

                /** @type {{prefix?: string, localPart?: string}} */
                var obj = null;

                // If there are valid parts decribed from the given IRI...
                if (strLocalPart) { // ...not null or ""
                    // Is there an existing prefix matching the given prefix?
                    if ( strPrefix === "" || this.#dialog.getNamespacesManager().hasPrefix(strPrefix) ) {
                        // ...yes (BaseIRI or already managed namespace), add resource...
                        obj = {};
                        obj.prefix    = strPrefix;
                        obj.localPart = strLocalPart;
                    }
                    // No, then this prefix is not using the BaseIRI prefix and is not managed...
                    else if (strPrefix) {
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
                            }
                        );
                    }
                    // Otherwise, check for a namespace without a prefix...
                    else if (strNamespace != null) {
                        // Not Prefixed: Full IRI...
                        obj = {};
                        obj.prefix = null;
                        obj.localPart = strNamespace + strLocalPart;
                    }
                }
                // Otherwise, if there is a good IRI...
                else if (strIRI && strIRI !== ":") {
                    // If it has a Base IRI prefix...
                    if (strIRI[0] === ':' && strIRI.length > 1) {
                        obj = {};
                        obj.prefix    = ""; // ...use Base IRI Prefix
                        obj.localPart = strIRI.substring(1);
                    }
                    // Otherwise, it's a Full IRI...
                    else {
                        obj = {};
                        obj.prefix    = null;   // No Prefix
                        obj.localPart = strIRI; // Full IRI
                    }
                }

                // Do we have a good resource (obj) to add?
                if (obj !== null) {
                    // Add new IRI object to suggestions...
                    var term = {};
                    term.iri = strIRI;
                    term.label = strIRI;
                    //term.desc
                    if (obj.prefix != null) {
                        term.prefix = obj.prefix;
                    }
                    if (strNamespace != null) {
                        term.namespace = strNamespace;
                    }
                    term.localPart = obj.localPart;
                    //term.description

                    this.#handlerAddSuggestTerm(evt, this.#strLookForType, term);

                    // Process new IRI object...
                    this.#onDone(obj);
                }
                else {
                    alert(
                        // @ts-ignore
                        $.i18n('rdft-dialog/alert-iri') + "\n" +
                        // @ts-ignore
                        $.i18n('rdft-dialog/alert-iri-invalid') + "\n" +
                        "Invalid Resource object for selection!\n" + // TODO: $.i18n()
                        "IRI: " + strIRI
                    );
                }

            }
        );
        elements.rdftNewResourceIRI.focus();
    }

    #handlerAddSuggestTerm(evt, strType, params) {
        evt.preventDefault();

        params.project = theProject.id;
        params.type = strType;

        // @ts-ignore
        var funcDismissBusy = DialogSystem.showBusy($.i18n('rdft-vocab/adding-term') + ' ' + params.iri);

        Refine.postCSRF(
            gstrCommandRDFTransform + gstrAddSuggestTerm,
            params,
            (data) => {
                if (data.code === "error") {
                    // @ts-ignore
                    alert($.i18n('rdft-vocab/error-adding-term') + ': [' + params.iri + "]");
                }
                funcDismissBusy();
            },
            "json"
        );
    }
}
