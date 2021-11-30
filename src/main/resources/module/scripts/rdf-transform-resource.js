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
            {	"type"        : '' + projectID,
                "type_strict" : strLookForType,
                "parent"      : '.rdf-transform-menu-search'
            }
        )
        .bind('fb-select',
            (evt, data) => {
                MenuSystem.dismissAll();
                if (this.#onDone) {
                    this.#onDone(data);
                }
            }
        )
        .bind('fb-select-new',
            (evt, value) => {
                MenuSystem.dismissAll();
                if ( RDFTransformPrefixesManager.isPrefixedQName(value) ) {
                    // Check that the prefix is defined...
                    var prefix = RDFTransformPrefixesManager.getPrefixFromQName(value);
                    if ( this.#dialog.prefixesManager.hasPrefix(prefix) ) {
                        var strIRI = RDFTransformPrefixesManager.getFullIRIFromQName(value);
                        if (this.#onDone) {
                            this.#onDone(
                                {   "name" : value,
                                    "id"   : strIRI
                                }
                            );
                        }
                        MenuSystem.dismissAll();
                        return;
                    }
                    else {
                        this.#dialog.prefixesManager.addPrefix(
                            '<em>' + prefix + '</em> ' + $.i18n('rdft-dialog/unknown-pref'),
                            prefix, false
                        );
                    }
                }
                else {
                    new RDFTransformResourceResolveDialog(this.#element, value, this.#onDone);
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
                    alert($.i18n('rdft-dialog/alert-iri'));
                    return;
                }
                MenuSystem.dismissAll();
                //if (strIRI.charAt(0) === ':') {
                //    strIRI = strIRI.substring(1);
                //}
                var obj = {
                    "id"   : strIRI,
                    "name" : RDFTransformCommon.validateIRI(strIRI) ? strIRI : ':' + strIRI
                };
                this.#onDone(obj);
            }
        );
    }    
} 
