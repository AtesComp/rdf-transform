/*
 *  CLASS RDFTransformResourceDialog
 *
 *  The resource manager for the RDF Transform dialog
 */
class RDFTransformResourceDialog {
    #element;
    #strLookForType;
    #projectID;
    #dialog;
    #onDone;

    constructor(element, strLookForType, projectID, dialog, onDone) {
        this.#element = element;
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
    $.i18n('rdft-dialog/search-for') + ' ' + this.#strLookForType + ':' +
  '</span>' +
  '<input type="text" bind="rdftNewResourceIRI" >' +
'</div>'
        );

        MenuSystem.showMenu(menu, () => {} );
        MenuSystem.positionMenuLeftRight(menu, $(this.#element));

        var elements = DOM.bind(menu);
        elements.rdftNewResourceIRI
        .suggestTerm(
            {	"project" : this.#projectID.toString(),
                "type"    : this.#strLookForType,
                "parent"  : '.rdf-transform-menu-search'
            }
        )
        .bind('fb-select', // ...select existing item...
            async (evt, data) => {
                MenuSystem.dismissAll();
                alert("DEBUG: Existing Item:\n" + data);
                //var obj = {
                //    "iri"   : data,
                //    "cirie" : data
                //}
                /** @type {{prefix?: string, localPart?: string}} */
                var obj = null;
                var iPrefixedIRI = await this.#dialog.namespacesManager.isPrefixedQName(data);
                if ( iPrefixedIRI == 1 ) { // ...Prefixed IRI
                    var strPrefix = this.#dialog.namespacesManager.getPrefixFromQName(data);
                    var strLocalPart = this.#dialog.namespacesManager.getSuffixFromQName(data);
                    // IRI   = Namespace of Prefix + strLocalPart
                    // CIRIE = strResPrefix + ":" + strLocalPart
                    obj = {};
                    obj.prefix = strPrefix;   // Given Prefix
                    obj.localPart = strLocalPart; // Path portion of IRI
                    this.#onDone(obj);
                }
                else if ( iPrefixedIRI == 0 ) { // ...Full IRI
                    // IRI   = Namespace of Prefix + strLocalPart
                    // CIRIE = strResPrefix + ":" + strLocalPart
                    obj = {};
                    obj.prefix = null;  // No Prefix
                    obj.localPart = data; // Full IRI
                    this.#onDone(obj);
                }
                else { // iPrefixedIRI == -1 // ...Bad IRI
                    alert(
                        $.i18n('rdft-dialog/alert-iri') + "\n" +
                        $.i18n('rdft-dialog/alert-iri-invalid') + "\n" +
                        data
                    );
                }
            }
        )
        .bind('fb-select-new', // ...add new item...
            async (evt, data) => {
                alert("DEBUG: Add Item:\n" + data);

                // Does the data look like a prefixed IRI?
                var iPrefixedIRI = await this.#dialog.namespacesManager.isPrefixedQName(data);
                // Is it a prefixed IRI?
                if ( iPrefixedIRI == 1 ) {
                    MenuSystem.dismissAll();

                    // Divide the data into Prefix and LocalPart portions...
                    var strPrefix = this.#dialog.namespacesManager.getPrefixFromQName(data);
                    var strLocalPart = this.#dialog.namespacesManager.getSuffixFromQName(data);

                    // Is there an existing prefix matching the given prefix?
                    if ( this.#dialog.namespacesManager.hasPrefix(strPrefix) ) {
                        // ...yes, add resource...
                        // IRI   = Namespace of Prefix + strLocalPart
                        // CIRIE = strResPrefix + ":" + strLocalPart
                        /** @type {{prefix?: string, localPart?: string}} */
                        var obj = {};
                        obj.prefix = strPrefix;   // Given Prefix
                        obj.localPart = strLocalPart; // Path portion of IRI
                        this.#onDone(obj);
                    }
                    // No, then this prefix is not recorded...
                    else {
                        // ...create prefix (which may change in here) and add (re-prefixed) resource...
                        //
                        // NOTE: We are passing a function to RDFTransformNamespacesManager.addNamespace() which,
                        //      in turn, passes it in an event function to RDFTransformNamespaceAdder.show().
                        this.#dialog.namespacesManager.addNamespace(
                            $.i18n('rdft-dialog/unknown-pref') + ': <em>' + strPrefix + '</em> ',
                            strPrefix,
                            (strResPrefix) => {
                                // NOTE: Only the prefix (without the related IRI) is returned.  We don't need
                                //      the IRI...addNamespace() added it.  We will get the IRI from the prefix
                                //      manager later to ensure the IRI is present.
                                /** @type {{prefix?: string, localPart?: string}} */
                                var obj = null;
                                // Do the original and resulting prefix match?
                                // NOTE: It can change via edits in RDFTransformNamespaceAdder.show()
                                if ( strPrefix.normalize() == strResPrefix.normalize() ) {
                                    // ...yes, set as before...
                                    // IRI   = Namespace of Prefix + strLocalPart
                                    // CIRIE = strResPrefix + ":" + strLocalPart
                                    obj = {};
                                    obj.prefix = strPrefix;   // Given Prefix
                                    obj.localPart = strLocalPart; // Path portion of IRI
                                }
                                // No, then adjust...
                                else {
                                    // ...get new Namespace of the Prefix to validate...
                                    var strResNamespace =
                                        this.#dialog.namespacesManager.getNamespaceOfPrefix(strResPrefix);
                                    // Ensure the prefix's IRI was added...
                                    if ( strResNamespace != null ) {
                                        // IRI   = Namespace of Prefix + strLocalPart
                                        // CIRIE = strResPrefix + ":" + strLocalPart
                                        obj = {};
                                        obj.prefix = strResPrefix; // New Prefix
                                        obj.localPart = strLocalPart;  // Path portion of IRI
                                    }
                                    // If not, abort the resource addition with a null obj...
                                }

                                // Do we have a good resource (obj) to add?
                                if (obj != null) {
                                    // ...yes, add resource...
                                    this.#onDone(obj);
                                }
                            }
                        );
                    }
                }
                // Is it a full IRI?
                else if ( iPrefixedIRI == 0 ) {
                    MenuSystem.dismissAll();

                    //new RDFTransformResourceResolveDialog(this.#element, data, this.#onDone);
                    // ...take it as is...
                    /** @type {{prefix?: string, localPart?: string}} */
                    var obj = {};
                    obj.prefix = null;  // No Prefix
                    obj.localPart = data; // Full IRI
                    this.#onDone(obj);
                }
                // Is it a BAD IRI?
                else { // iPrefixedIRI == -1
                    alert(
                        $.i18n('rdft-dialog/alert-iri') + "\n" +
                        $.i18n('rdft-dialog/alert-iri-invalid') + "\n" +
                        data
                    );
                }
            }
        );
        elements.rdftNewResourceIRI.focus();
    }
}

/*
 *  CLASS RDFTransformResourceResolveDialog
 *
 *  The resource resolver for the resource manager dialog
 */
// class RDFTransformResourceResolveDialog {
//     #onDone;
//
//     constructor(element, defaultVal, onDone) {
//         this.#onDone = onDone;
//
//         var menu = MenuSystem.createMenu().width('400px'); // ...6:1 on input size
//         menu.html(
// '<div class="rdf-transform-menu-search">' +
//   '<span class="rdf-transform-node-label">IRI: ' +
//     '<small>(' + $.i18n('rdft-dialog/resolve') + ')</small>' +
//   '</span>' +
//   '<input type="text" size="50" bind="rdftNewResourceIRI"><br/>' +
//   '<button class="button" bind="buttonApply">' +
//     $.i18n('rdft-buttons/apply') +
//   '</button>' +
//   '<button class="button" bind="buttonCancel">' +
//     $.i18n('rdft-buttons/cancel') +
//   '</button>' +
// '</div>'
//         );
//         MenuSystem.showMenu(menu, () => {} );
//         MenuSystem.positionMenuLeftRight(menu, $(element));
//
//         var elements = DOM.bind(menu);
//         elements.rdftNewResourceIRI
//         .val(defaultVal)
//         .focus()
//         .select();
//
//         elements.buttonCancel
//         .click( () => { MenuSystem.dismissAll(); } );
//
//         elements.buttonApply
//         .click(
//             async () => {
//                 var strIRI = elements.rdftNewResourceIRI.val();
//                 if (!strIRI) {
//                     alert( $.i18n('rdft-dialog/alert-iri') );
//                     return;
//                 }
//                 if ( ! await RDFTransformCommon.validateIRI(strIRI) ) {
//                     alert(
//                         $.i18n('rdft-dialog/alert-iri') + "\n" +
//                         $.i18n('rdft-dialog/alert-iri-invalid') + "\n" +
//                         strIRI
//                     );
//                     return;
//                 }
//                 MenuSystem.dismissAll();
//                 //if (strIRI.charAt(0) === ':') {
//                 //    strIRI = strIRI.substring(1);
//                 //}
//                 var obj = {
//                     "iri"   : strIRI, // Full IRI
//                     "cirie" : strIRI  // Prefixed IRI
//                 };
//                 this.#onDone(obj);
//             }
//         );
//     }
// }
