/*
 *  CLASS RDFTransformResourceDialog
 *
 *  The resource manager for the RDF Transform dialog
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
                $.i18n('rdft-dialog/error') + "\n" +
                $.i18n("rdft-dialog/missing-proc") );
            return;
        }

        var menu = MenuSystem.createMenu().width('331px'); // ...331px fits Suggest Term
        menu.html(
'<div id="rdf-transform-menu-search" class="rdf-transform-menu-search">' +
  '<span>' +
    $.i18n('rdft-dialog/search-for') + ' ' + this.#strLookForType + ":" +
  '</span>' +
  '<input type="text" bind="rdftNewResourceIRI" >' +
'</div>'
        );

        MenuSystem.showMenu(menu, () => {} );
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
                    this.#onDone(obj);
                }
                else {
                    alert(
                        $.i18n('rdft-dialog/alert-iri') + "\n" +
                        $.i18n('rdft-dialog/alert-iri-invalid') + "\n" +
                        "The selection does not have a valid Resource object!\n" + // TODO: $.i18n()
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
                    const bBaseIRI = (strIRI[0] === ':');
                    var strTestIRI = strIRI;
                    if (bBaseIRI) {
                        strTestIRI = strIRI.substring(1);
                    }
                    // Does the IRI look like a prefixed IRI?
                    var iPrefixedIRI = await RDFTransformCommon.isPrefixedQName(strTestIRI);
                    // Is it a good IRI?
                    if ( iPrefixedIRI >= 0 ) {
                        MenuSystem.dismissAll();

                        // If it's a good BaseIRI prefixed IRI...
                        if (bBaseIRI) {
                            iPrefixedIRI = 0; // ...then it's otherwise not prefixed, just good
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
                        // Is it a good IRI?
                        else if ( iPrefixedIRI === 0 ) {
                            // Does it have a Base IRI prefix?  Yes...
                            if (bBaseIRI) {
                                strPrefix = ""; // ...use Base IRI Prefix
                                strNamespace = this.#dialog.getBaseIRI();
                                strLocalPart = strIRI.substring(1);
                                strLabel = strIRI;
                                strIRI = strNamespace + strLocalPart;
                            }
                            // Otherwise, it's a Full IRI...
                            else {
                                // ...take it as is...
                                strLabel = strIRI;
                            }
                        }
                        // Otherwise, it's a BAD IRI!
                    }
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
                        // ...yes (BaseIRI or already managed), add resource...
                        obj = {};
                        obj.prefix    = strPrefix;
                        obj.localPart = strLocalPart;
                    }
                    // No, then this prefix is not using the BaseIRI prefix and is not managed...
                    else if (strPrefix) {
                        // ...create prefix (which may change in here) and add (re-prefixed) resource...
                        //
                        // NOTE: We are passing a function to RDFTransformNamespacesManager.addNamespace() which,
                        //      in turn, passes it in an event function to RDFTransformNamespaceAdder.show().
                        this.#dialog.getNamespacesManager().addNamespace(
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
                else if (strIRI) {
                    obj = {};
                    // If it has a Base IRI prefix...
                    if (strIRI[0] === ':') {
                        obj.prefix    = ""; // ...use Base IRI Prefix
                        obj.localPart = strIRI.substring(1);
                    }
                    // Otherwise, it's a Full IRI...
                    else {
                        obj.prefix    = null;   // No Prefix
                        obj.localPart = strIRI; // Full IRI
                    }
                }

                // Do we have a good resource (obj) to add?
                if (obj !== null) {
                    // ...yes, add resource...
                    this.#onDone(obj);
                }
                else {
                    alert(
                        $.i18n('rdft-dialog/alert-iri') + "\n" +
                        $.i18n('rdft-dialog/alert-iri-invalid') + "\n" +
                        "The selection does not have a valid Resource object!\n" + // TODO: $.i18n()
                        "IRI: " + strIRI
                    );
                }

            }
        );
        elements.rdftNewResourceIRI.focus();
    }

    /*
    async #extractPrefixLocalPart(strIRI, strResp) {
        /** @type {{prefix?: string, localPart?: string}} * /
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
                $.i18n('rdft-dialog/alert-iri') + "\n" +
                $.i18n('rdft-dialog/alert-iri-invalid') + "\n" +
                strResp + strIRI
            );
        }
    }
    */

    /*
    async #addPrefixLocalPart(strIRI, strResp) {
        /** @type {{prefix?: string, localPart?: string}} * /
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
                // IRI   = Namespace of Prefix + strLocalPart
                // CIRIE = strResPrefix + ":" + strLocalPart
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
                //      in turn, passes it in an event function to RDFTransformNamespaceAdder.show().
                this.#dialog.getNamespacesManager().addNamespace(
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
                            // IRI   = Namespace of Prefix + strLocalPart
                            // CIRIE = strResPrefix + ":" + strLocalPart
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
                                // IRI   = Namespace of Prefix + strLocalPart
                                // CIRIE = strResPrefix + ":" + strLocalPart
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
            /** @type {{prefix?: string, localPart?: string}} * /
            obj = {};
            obj.prefix = null;  // No Prefix
            obj.localPart = strIRI; // Full IRI
            this.#onDone(obj);
        }
        // Is it a BAD IRI?
        else { // iPrefixedIRI === -1
            alert(
                $.i18n('rdft-dialog/alert-iri') + "\n" +
                $.i18n('rdft-dialog/alert-iri-invalid') + "\n" +
                strResp + strIRI
            );
        }
    }
    */
}

/*
 *  CLASS RDFTransformResourceResolveDialog
 *
 *  The resource resolver for the resource manager dialog
 */
/*
class RDFTransformResourceResolveDialog {
    #onDone;

    constructor(element, defaultVal, onDone) {
        this.#onDone = onDone;

        var menu = MenuSystem.createMenu().width('400px'); // ...6:1 on input size
        menu.html(
'<div class="rdf-transform-menu-search">' +
  '<span class="rdf-transform-node-label">IRI: ' +
    '<small>(' + $.i18n('rdft-dialog/resolve') + ')</small>' +
  '</span>' +
  '<input type="text" size="50" bind="rdftNewResourceIRI"><br/>' +
  '<button class="button" bind="buttonApply">' +
    $.i18n('rdft-buttons/apply') +
  '</button>' +
  '<button class="button" bind="buttonCancel">' +
    $.i18n('rdft-buttons/cancel') +
  '</button>' +
'</div>'
        );
        MenuSystem.showMenu(menu, () => {} );
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
                    alert( $.i18n('rdft-dialog/alert-iri') );
                    return;
                }
                if ( ! await RDFTransformCommon.validateIRI(strIRI) ) {
                    alert(
                        $.i18n('rdft-dialog/alert-iri') + "\n" +
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
*/
