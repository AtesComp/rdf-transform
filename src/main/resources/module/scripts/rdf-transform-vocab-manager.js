/*
 *  Class RDFTransformVocabManager
 *
 *  The Vocabulary Manager UI for the RDF Transform Dialog.
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

class RDFTransformVocabManager {
    /** @type RDFTransformNamespacesManager */
    #namespacesManager;

    #level;
    #elements;

    constructor(namespacesManager) {
        this.#namespacesManager = namespacesManager;
    }

    show(onDone) {
        // Load RDF Transform's Vocabulary Manager Dialog...
        var dialog =
            // @ts-ignore
            $(DOM.loadHTML(RDFTransform.KEY, "scripts/dialogs/rdf-transform-vocab-manager.html"))
                .filter('.dialog-frame');

        // Connect all the Vocabulary Manager Dialog's "bind" elements to this
        //      RDF Transform Vocabulary Manager instance...
        this.#elements = DOM.bind(dialog);

        // @ts-ignore
        this.#elements.dialogHeader.html(       $.i18n('rdft-vocab/header')          );
        // @ts-ignore
        this.#elements.buttonAddNamespace.html( $.i18n('rdft-buttons/add-namespace') );
        // @ts-ignore
        this.#elements.buttonOK.html(           $.i18n('rdft-buttons/ok')            );

        this.#elements.buttonAddNamespace
        .on("click",
            (evt) => {
                evt.preventDefault();
                this.#namespacesManager.addNamespace(
                    false, false,
                    () => {
                        this.#renderBody();
                    }
                );
            }
        );

        this.#elements.buttonOK
        .on("click",
            () => {
                if (onDone) {
                    onDone();
                }
                this.#dismiss();
            }
        );

        this.#level = DialogSystem.showDialog(dialog);

        this.#renderBody();
    }

    #handlerRemove(strPrefix) {
        return (evtHandler) => {
            evtHandler.preventDefault();
            // @ts-ignore
            var dismissBusy = DialogSystem.showBusy($.i18n('rdft-vocab/deleting-pref') + ' ' + strPrefix);

            Refine.postCSRF(
                "command/rdf-transform/remove-prefix",
                {   "project" : theProject.id,
                    "prefix": strPrefix
                },
                (data) => {
                    if (data.code === "error") {
                        // @ts-ignore
                        alert($.i18n('rdft-vocab/error-deleting') + ': ' + strPrefix);
                    }
                    else {
                        this.#namespacesManager.removeNamespace(strPrefix);
                    }
                    this.#renderBody();
                    dismissBusy();
                },
                "json"
            );
        };
    }

    #handlerRefresh(strPrefix, strNamespace) {
        return (evtHandler) => {
            evtHandler.preventDefault();
            if ( window.confirm(
                    // @ts-ignore
                    $.i18n('rdft-vocab/desc-one') + ' "' + strNamespace + '"\n' +
                    // @ts-ignore
                    $.i18n('rdft-vocab/desc-two') ) )
            {
                var dismissBusy =
                    // @ts-ignore
                    DialogSystem.showBusy($.i18n('rdft-vocab/refresh-pref') + ' ' + strPrefix);

                Refine.postCSRF(
                    "command/rdf-transform/refresh-prefix",
                    {   "project" : theProject.id,
                        "prefix": strPrefix,
                        'namespace': strNamespace,
                    },
                    (data) => {
                        if (data.code === "error") {
                            // @ts-ignore
                            alert($.i18n('rdft-vocab/alert-wrong') + ': ' + data.message);
                        }
                        this.#renderBody();
                        dismissBusy();
                    },
                    "json"
                );
            }
        };
    }

    #renderBody() {
        var table = this.#elements.namespacesTable;
        table.empty();
        table.append(
            // @ts-ignore
            $('<tr>').addClass('rdf-transform-table-even')
            // @ts-ignore
            .append($('<th/>').text($.i18n('rdft-vocab/prefix')))
            // @ts-ignore
            .append($('<th/>').text($.i18n('rdft-vocab/iri')))
            // @ts-ignore
            .append($('<th/>').text($.i18n('rdft-vocab/delete')))
            // @ts-ignore
            .append($('<th/>').text($.i18n('rdft-vocab/refresh')))
        );

        var bEven = false;
        const theNamespaces = this.#namespacesManager.getNamespaces();
        for (const strPrefix in theNamespaces) {
            const strNamespace = theNamespaces[strPrefix];
            /** @type {HTMLElement} */
            var htmlRemoveNamespace =
                // @ts-ignore
                $('<a/>')
                // @ts-ignore
                .text( $.i18n('rdft-vocab/delete') )
                .attr('href', '#')
                .on("click", this.#handlerRemove(strPrefix) );
            /** @type {HTMLElement} */
            var htmlRefreshNamespace =
                // @ts-ignore
                $('<a/>')
                // @ts-ignore
                .text( $.i18n('rdft-vocab/refresh') )
                .attr('href', '#')
                .on("click", this.#handlerRefresh(strPrefix, strNamespace) );
            var tr =
                // @ts-ignore
                $('<tr/>').addClass(bEven ? 'rdf-transform-table-even' : 'rdf-transform-table-odd')
                // @ts-ignore
                .append( $('<td>').text(strPrefix) )
                // @ts-ignore
                .append( $('<td>').text(strNamespace) )
                // @ts-ignore
                .append( $('<td>').html(htmlRemoveNamespace) )
                // @ts-ignore
                .append( $('<td>').html(htmlRefreshNamespace) );
            table.append(tr);
            bEven = !bEven;
        }

    }

    #dismiss() {
        DialogSystem.dismissUntil(this.#level - 1);
    }
}
