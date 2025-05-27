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

    #handlerRemove(strPrefix, strNamespace, strLocation, strLocType) {
        return (evtHandler) => {
            evtHandler.preventDefault();
            // @ts-ignore
            var funcDismissBusy = DialogSystem.showBusy($.i18n('rdft-vocab/deleting-pref') + ' ' + strPrefix);

            Refine.postCSRF(
                "command/rdf-transform/remove-namespace",
                {   "project" :  theProject.id,
                    "prefix":    strPrefix,
                    "namespace": strNamespace,
                    "location":  strLocation,
                    "loctype":   strLocType
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
                    funcDismissBusy();
                },
                "json"
            );
        };
    }

    #handlerRefresh(strPrefix, strNamespace, strLocation, strLocType) {
        return (evtHandler) => {
            evtHandler.preventDefault();
            if (strLocType === "NONE") return;
            if ( window.confirm(
                    // @ts-ignore
                    $.i18n('rdft-vocab/desc-one') + ' "' + strLocation + '"\n' + $.i18n('rdft-vocab/desc-two') ) )
            {
                var postData = null;

                var funcDismissBusy = null;
                var msgAlert = null;

                if (strLocType === "URL") {
                    postData = {
                        "project":   theProject.id,
                        "prefix":    strPrefix,
                        "namespace": strNamespace,
                        "location":  strLocation,
                        "loctype":   strLocType
                    };

                    // @ts-ignore
                    funcDismissBusy = DialogSystem.showBusy($.i18n('rdft-vocab/refresh-namespaces') + ' ' + strPrefix);
                    var postCmd = "command/rdf-transform/add-namespace-from-URL";
                    // @ts-ignore
                    msgAlert = $.i18n('rdft-vocab/alert-wrong') + ': ';

                    let funcSuccess = (data) => {
                        // @ts-ignore
                        if (data.code === "error") alert(msgAlert + data.message);
                        this.#renderBody();
                        funcDismissBusy();
                    }

                    Refine.postCSRF(
                        postCmd,
                        postData,
                        funcSuccess,
                        "json"
                    );
                }
                else { // ...if (strLocType === a file type) {
                    postData = new FormData();
                    postData.append("project", theProject.id);
                    postData.append("prefix", strPrefix);
                    postData.append("namespace", strNamespace);
                    postData.append("location", strLocation);
                    postData.append("loctype", strLocType);
                    // ...don't include "uploaded_file" to force strLocation use!

                    // @ts-ignore
                    funcDismissBusy = DialogSystem.showBusy($.i18n('rdft-prefix/prefix-by-upload') + ' ' +
                                    strNamespace + '<br />File: ' + strLocation );
                    var postCmd = "command/rdf-transform/add-namespace-from-file";
                    // @ts-ignore
                    msgAlert = $.i18n('rdft-vocab/error-adding') + ': ' + strPrefix + "\n";

                    let funcSuccess = (data) => {
                        // @ts-ignore
                        if (data.code === "error") alert(msgAlert + data.message);
                        this.#renderBody();
                        funcDismissBusy();
                    }

                    Refine.wrapCSRF(
                        (token) => {
                            // Requires JQuery Form plugin...
                            // @ts-ignore
                            $.ajax(
                                {
                                    url         : postCmd,
                                    type        : 'POST',
                                    data        : postData,
                                    dataType    : "json",
                                    processData : false,
                                    contentType : false,
                                    headers     : { 'X-CSRF-TOKEN': token },
                                    success     : funcSuccess
                                }
                            );
                        }
                    );
                }
            }
        };
    }

    #renderBody() {
        var table = this.#elements.tableNamespaces;
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
        //console.debug( "RDFTransformVocabManager : #renderBody : theNamespaces :\n" + JSON.stringify(theNamespaces, null, 2) );
        for (const cstrPrefix in theNamespaces) {
            let strNamespace = '';
            let strLocation  = '';
            let strLocType   = 'NONE';
            // If the Namespaces are constructed as the DEPRECATED version...
            if ( typeof theNamespaces[cstrPrefix] === 'string' ) {
                strNamespace = theNamespaces[cstrPrefix];
                // ...assume a URL Namespace...
                strLocation  = strNamespace;
                strLocType   = 'URL';
            }
            else {
                strNamespace = theNamespaces[cstrPrefix].namespace;
                strLocation  = theNamespaces[cstrPrefix].location;
                strLocType   = theNamespaces[cstrPrefix].loctype;
            }

            const cstrNamespace = strNamespace;
            const cstrLocation  = strLocation;
            const cstrLocType   = strLocType;

            var htmlRemoveNamespace =
                // @ts-ignore
                $('<a/>')
                // @ts-ignore
                .text( $.i18n('rdft-vocab/delete') )
                .attr('href', '#')
                .on("click", this.#handlerRemove(cstrPrefix, cstrNamespace, cstrLocation, cstrLocType) );
            // @ts-ignore
            var htmlRefreshNamespace = $('');
            if ( strLocType !== 'NONE' ) {
                htmlRefreshNamespace =
                    // @ts-ignore
                    $('<a/>')
                    // @ts-ignore
                    .text( $.i18n('rdft-vocab/refresh') )
                    .attr('href', '#')
                    .on("click", this.#handlerRefresh(cstrPrefix, cstrNamespace, cstrLocation, cstrLocType) );
            }
            var tr =
                // @ts-ignore
                $('<tr/>').addClass(bEven ? 'rdf-transform-table-even' : 'rdf-transform-table-odd')
                // @ts-ignore
                .append( $('<td>').text(cstrPrefix) )
                // @ts-ignore
                .append( $('<td>').text(cstrNamespace) )
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
