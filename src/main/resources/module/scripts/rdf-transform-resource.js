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
        .bind('fb-select', // ...selected existing item...
            (evt, data) => {
                MenuSystem.dismissAll();
                if (! this.#onDone) {
                    alert( $.i18n('rdft-dialog/error') + "\nMissing onDone processor!" );
                    return;
                }
                this.#onDone(data);
            }
        )
        .bind('fb-select-new', // ...add new item...
            (evt, data) => {
                MenuSystem.dismissAll();
                if (! this.#onDone) {
                    alert( $.i18n('rdft-dialog/error') + "\nMissing onDone processor!" );
                    return;
                }
                // Does the data look like a prefixed IRI?
                if ( RDFTransformPrefixesManager.isPrefixedQName(data) ) {
                    // Check that the prefix is defined...
                    var strPrefix = RDFTransformPrefixesManager.getPrefixFromQName(data);
                    // Is there an existing prefix matching the given prefix?
                    if ( this.#dialog.prefixesManager.hasPrefix(strPrefix) ) {
                        // ...yes, add resource...
                        var strIRI = RDFTransformPrefixesManager.getFullIRIFromQName(data);
                        var obj = {
                            "id"   : strIRI,
                            "name" : data
                        }
                        this.#onDone(obj);
                    }
                    else {
                        // ...no, create prefix (which may change) and add (re-prefixed) resource...
                        this.#dialog.prefixesManager.addPrefix(
                            $.i18n('rdft-dialog/unknown-pref') + ': <em>' + strPrefix + '</em> ',
                            strPrefix,
                            (strResPrefix) => {
                                // NOTE: Only the prefix (without the related IRI) is returned.
                                //      We likely don't need the IRI.  If we do, we will get the IRI
                                //      later to ensure the IRI is present in the prefix manager.
                                var obj = null;
                                // Do the old and new prefixes match?
                                if (strPrefix == strResPrefix) {
                                    // ...yes, set as before...
                                    var strIRI = RDFTransformPrefixesManager.getFullIRIFromQName(data);
                                    obj = {
                                        "id"   : strIRI,
                                        "name" : data
                                    }
                                }
                                else {
                                    // ...no, get new prefix IRI and adjust the prior user data...
                                    strResPrefixIRI =
                                        this.#dialog.prefixesManager.getIRIOfPrefix(strResPrefix);
                                    // Ensure the prefix's IRI was added...
                                    if ( strResPrefixIRI != null ) {
                                        var strSuffixIRI =
                                            this.#dialog.prefixesManager.getSuffixFromQName(data);
                                        var obj = {
                                            "id"   : strResPrefixIRI + strSuffixIRI,
                                            "name" : strResPrefix + ":" + strSuffixIRI
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
                else { // ...anything else...
                    new RDFTransformResourceResolveDialog(this.#element, data, this.#onDone);
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
        .click( () => { MenuSystem.dismissAll(); } );
    
        elements.buttonApply
        .click(
            () => {
                var strIRI = elements.rdftNewResourceIRI.val();
                if (!strIRI) {
                    alert( $.i18n('rdft-dialog/alert-iri') );
                    return;
                }
                if ( ! RDFTransformCommon.validateIRI(strIRI) ) {
                    alert( $.i18n('rdft-dialog/alert-iri' + "\nIRI is not valid:\n" + strIRI) );
                    return;
                }
                MenuSystem.dismissAll();
                //if (strIRI.charAt(0) === ':') {
                //    strIRI = strIRI.substring(1);
                //}
                var obj = {
                    "id"   : strIRI,
                    "name" : strIRI
                };
                this.#onDone(obj);
            }
        );
    }    
} 
