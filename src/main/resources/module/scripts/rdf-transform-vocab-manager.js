/*
 *  Class RDFTransformVocabManager
 *
 *  The Vocabulary Manager UI for the RDF Transform Dialog.
 *
 *  Copyright 2022 Keven L. Ates
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
        var dialog =
            $(DOM.loadHTML(RDFTransform.KEY, "scripts/dialogs/rdf-transform-vocab-manager.html"))
                .filter('.dialog-frame');
        this.#level = DialogSystem.showDialog(dialog);
        this.#elements = DOM.bind(dialog);

        this.#elements.dialogHeader.html($.i18n('rdft-vocab/header'));
        this.#elements.buttonAddNamespace.html($.i18n('rdft-buttons/add-namespace'));
        this.#elements.buttonOK.html($.i18n('rdft-buttons/ok'));
        //this.#elements.buttonCancel.html($.i18n('rdft-buttons/cancel'));

        //this.#elements.buttonCancel
        //.on("click", () => { this.#dismiss(); } );

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

        this.#renderBody();

        this.#elements.buttonOK
        .on("click",
            () => {
                if (onDone) {
                    onDone();
                }
                this.#dismiss();
            }
        );
    }

    #handlerRemove(strPrefix) {
        return (evtHandler) => {
            evtHandler.preventDefault();
            var dismissBusy = DialogSystem.showBusy($.i18n('rdft-vocab/deleting-pref') + ' ' + strPrefix);

            Refine.postCSRF(
                "command/rdf-transform/remove-prefix",
                {   "project" : theProject.id,
                    "prefix": strPrefix
                },
                (data) => {
                    if (data.code === "error") {
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
                    $.i18n('rdft-vocab/desc-one') + ' "' + strNamespace + '"\n' +
                    $.i18n('rdft-vocab/desc-two') ) )
            {
                var dismissBusy =
                    DialogSystem.showBusy($.i18n('rdft-vocab/refresh-pref') + ' ' + strPrefix);

                Refine.postCSRF(
                    "command/rdf-transform/refresh-prefix",
                    {   "project" : theProject.id,
                        "prefix": strPrefix,
                        'namespace': strNamespace,
                    },
                    (data) => {
                        if (data.code === "error") {
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
            $('<tr>').addClass('rdf-transform-table-even')
            .append($('<th/>').text($.i18n('rdft-vocab/prefix')))
            .append($('<th/>').text($.i18n('rdft-vocab/iri')))
            .append($('<th/>').text($.i18n('rdft-vocab/delete')))
            .append($('<th/>').text($.i18n('rdft-vocab/refresh')))
        );

        var bEven = false;
        const theNamespaces = this.#namespacesManager.getNamespaces();
        for (const strPrefix in theNamespaces) {
            const strNamespace = theNamespaces[strPrefix];
            /** @type {HTMLElement} */
            // @ts-ignore
            var htmlRemoveNamespace =
                $('<a/>')
                .text( $.i18n('rdft-vocab/delete') )
                .attr('href', '#')
                .on("click", this.#handlerRemove(strPrefix) );
            /** @type {HTMLElement} */
            // @ts-ignore
            var htmlRefreshNamespace =
                $('<a/>')
                .text( $.i18n('rdft-vocab/refresh') )
                .attr('href', '#')
                .on("click", this.#handlerRefresh(strPrefix, strNamespace) );
            var tr = $('<tr/>').addClass(bEven ? 'rdf-transform-table-even' : 'rdf-transform-table-odd')
                .append( $('<td>').text(strPrefix) )
                .append( $('<td>').text(strNamespace) )
                .append( $('<td>').html(htmlRemoveNamespace) )
                .append( $('<td>').html(htmlRefreshNamespace) );
            table.append(tr);
            bEven = !bEven;
        }

    }

    #dismiss() {
        DialogSystem.dismissUntil(this.#level - 1);
    }
}
