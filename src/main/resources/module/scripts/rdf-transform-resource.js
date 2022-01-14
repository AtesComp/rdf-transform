/*
 *  CLASS RDFTransformResourceDialog
 *
 *  The resource manager for the RDF Transform dialog
 */
class RDFTransformResourceDialog {
    #element;
    #dialog;
    #onDone;

    constructor(element, strLookForType, projectID, dialog, onDone) {
        this.#element = element;
        this.#dialog = dialog;
        this.#onDone = onDone;

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
    $.i18n('rdft-dialog/search-for') + ' ' + strLookForType + ':' +
  '</span>' +
  '<input type="text" bind="rdftNewResourceIRI" >' +
'</div>'
        );

        MenuSystem.showMenu(menu, () => {} );
        MenuSystem.positionMenuLeftRight(menu, $(element));

        var elements = DOM.bind(menu);
        elements.rdftNewResourceIRI
        .suggestTerm(
            {	"type"        : projectID.toString(),
                "type_strict" : strLookForType,
                "parent"      : '.rdf-transform-menu-search'
            }
        )
        .bind('fb-select', // ...select existing item...
            (evt, data) => {
                MenuSystem.dismissAll();
                alert("DEBUG: Existing Item:\n" +
                    data
                );
                //var obj = {
                //    "iri"   : data,
                //    "cirie" : data
                //}
                this.#onDone(data);
            }
        )
        .bind('fb-select-new', // ...add new item...
            async (evt, data) => {
                // Does the data look like a prefixed IRI?
                var iPrefixedIRI = await this.#dialog.prefixesManager.isPrefixedQName(data);
                // Is it a prefixed IRI?
                if ( iPrefixedIRI == 1 ) {
                    MenuSystem.dismissAll();

                    // Check that the prefix is defined...
                    var strPrefix = this.#dialog.prefixesManager.getPrefixFromQName(data);
                    // Is there an existing prefix matching the given prefix?
                    if ( this.#dialog.prefixesManager.hasPrefix(strPrefix) ) {
                        // ...yes, add resource...
                        var strIRI = this.#dialog.prefixesManager.getFullIRIFromQName(data);
                        var obj = {
                            "iri"   : strIRI,
                            "cirie" : data
                        }
                        this.#onDone(obj);
                    }
                    // No, then this prefix is not recorded...
                    else {
                        // ...create prefix (which may change in here) and add (re-prefixed) resource...
                        //
                        // NOTE: We are passing a function to RDFTransformPrefixesManager.addPrefix() which,
                        //      in turn, passes it in an event function to RDFTransformPrefixAdder.show().
                        this.#dialog.prefixesManager.addPrefix(
                            $.i18n('rdft-dialog/unknown-pref') + ': <em>' + strPrefix + '</em> ',
                            strPrefix,
                            (strResPrefix) => {
                                // NOTE: Only the prefix (without the related IRI) is returned.  We don't need
                                //      the IRI...addPrefix() added it.  We will get the IRI from the prefix
                                //      manager later to ensure the IRI is present.
                                var obj = null;
                                // Do the original and resulting prefixes match?
                                // NOTE: It can change via edits in RDFTransformPrefixAdder.show()
                                if ( strPrefix.normalize() == strResPrefix.normalize() ) {
                                    // ...yes, set as before...
                                    var strIRI = this.#dialog.prefixesManager.getFullIRIFromQName(data);
                                    obj = {
                                        "iri"   : strIRI, // Full IRI
                                        "cirie" : data    // Prefixed IRI
                                    }
                                }
                                // No, then adjust...
                                else {
                                    // ...get new prefix IRI and adjust the prior user data...
                                    var strResPrefixIRI =
                                        this.#dialog.prefixesManager.getIRIOfPrefix(strResPrefix);
                                    // Ensure the prefix's IRI was added...
                                    if ( strResPrefixIRI != null ) {
                                        var strSuffixIRI =
                                            this.#dialog.prefixesManager.getSuffixFromQName(data);
                                        obj = {
                                            "iri"   : strResPrefixIRI + strSuffixIRI,   // Full IRI
                                            "cirie" : strResPrefix + ":" + strSuffixIRI // Prefixed IRI
                                        }
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
                    var obj = {
                        "iri"   : data, // Full IRI
                        "cirie" : data  // Prefixed IRI
                    };
                    this.#onDone(obj);
                }
                // Is it a BAD IRI?
                else { // iPrefixedIRI == -1
                    alert(
                        $.i18n('rdft-dialog/alert-iri') + "\n" +
                        $.i18n('rdft-dialog/alert-iri-invalid') + "\n" +
                        data
                    );
                    return;
                }
            }
        )
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
//                     "id"   : strIRI, // Full IRI
//                     "name" : strIRI  // Prefixed IRI
//                 };
//                 this.#onDone(obj);
//             }
//         );
//     }    
// } 
