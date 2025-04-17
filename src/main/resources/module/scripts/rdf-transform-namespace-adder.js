/*
 *  Class RDFTransformNamespaceAdder
 *
 *  Adds a Namespace to the current RDF Transform.
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

class RDFTransformNamespaceAdder {
    #namespacesManager;
    #dlgPrefixAdd;
    #elements;
    #level;

    #onDoneAdding;

    constructor(namespacesManager) {
        this.#namespacesManager = namespacesManager;

        // Load RDF Transform's Prefix Add Dialog...
        this.#dlgPrefixAdd =
            // @ts-ignore
            $(DOM.loadHTML(RDFTransform.KEY, "scripts/dialogs/rdf-transform-prefix-add.html"))
                .filter(".dialog-frame");

        // Connect all the Prefix Add Dialog's "bind" elements to this
        //      RDF Transform Namespace Adder instance...
        this.#elements = DOM.bind(this.#dlgPrefixAdd);

        // @ts-ignore
        this.#elements.dialogHeader.html(                   $.i18n('rdft-prefix/header')          );
        // @ts-ignore
        this.#elements.rdf_transform_prefix_prefix.html(    $.i18n('rdft-prefix/prefix') + ":"    );
        // @ts-ignore
        this.#elements.rdf_transform_prefix_namespace.html( $.i18n('rdft-prefix/namespace') + ":" );
        // @ts-ignore
        this.#elements.rdf_transform_prefix_voc.html(       $.i18n('rdft-prefix/vocab-terms')     );
        // @ts-ignore
        this.#elements.rdf_transform_prefix_only.html(      $.i18n('rdft-prefix/prefix-add-only') );
        // @ts-ignore
        this.#elements.rdf_transform_prefix_fetch.html(     $.i18n('rdft-prefix/fetch')           );
        // @ts-ignore
        this.#elements.rdf_transform_prefix_imp.html(       $.i18n('rdft-prefix/import')          );
        // @ts-ignore
        this.#elements.rdf_transform_prefix_file.html(      $.i18n('rdft-prefix/file') + ":"      );
        // @ts-ignore
        this.#elements.rdf_transform_prefix_format.html(    $.i18n('rdft-prefix/format') + ":"    );
        // @ts-ignore
        this.#elements.rdf_transform_prefix_auto.html(      $.i18n('rdft-prefix/auto')            );
        // @ts-ignore
        this.#elements.rdf_transform_prefix_turtle.html(    $.i18n('rdft-prefix/turtle')          );
        // @ts-ignore
        this.#elements.rdf_transform_prefix_rdfxml.html(    $.i18n('rdft-prefix/rdfxml')          );
        // @ts-ignore
        this.#elements.rdf_transform_prefix_n3.html(        $.i18n('rdft-prefix/n3')              );
        // @ts-ignore
        this.#elements.rdf_transform_prefix_ntriple.html(   $.i18n('rdft-prefix/ntriple')         );
        // @ts-ignore
        this.#elements.rdf_transform_prefix_jsonld.html(    $.i18n('rdft-prefix/jsonld')          ); // default version is JSON-LD 1.1
        // @ts-ignore
        this.#elements.rdf_transform_prefix_nquads.html(    $.i18n('rdft-prefix/nquads')          );
        // @ts-ignore
        this.#elements.rdf_transform_prefix_rdfjson.html(   $.i18n('rdft-prefix/rdfjson')         );
        // @ts-ignore
        this.#elements.rdf_transform_prefix_trig.html(      $.i18n('rdft-prefix/trig')            );
        // @ts-ignore
        this.#elements.rdf_transform_prefix_trix.html(      $.i18n('rdft-prefix/trix')            );
        // @ts-ignore
        this.#elements.rdf_transform_prefix_rdfthrift.html( $.i18n('rdft-prefix/rdfthrift')       );

        // @ts-ignore
        this.#elements.rdf_transform_prefix_note.html( $.i18n('rdft-prefix/namespace-note') );

        // @ts-ignore
        this.#elements.buttonOK.html(          $.i18n('rdft-buttons/ok')                   );
        // @ts-ignore
        this.#elements.buttonCancel.html(      $.i18n('rdft-buttons/cancel')               );
        // @ts-ignore
        this.#elements.buttonVocabImport.html( $.i18n('rdft-buttons/vocab-import') + "..." );

        this.#level = DialogSystem.showDialog(this.#dlgPrefixAdd);
    }

    show(strMessage, strPrefix, onDoneAdding) {
        if (strMessage) {
            this.#elements.message.addClass('message').html(strMessage);
        }

        if (strPrefix) {
            this.#elements.prefix.val(strPrefix);
            this.#suggestNamespace(strPrefix);
        }

        this.#onDoneAdding = onDoneAdding;

        this.#elements.file_upload_form
        .submit( async (evt) => {
            evt.preventDefault();

            var strPrefix = this.#elements.prefix.val();
            var strNamespace = this.#elements.namespace.val();
            var strFetchOption =
                this.#elements.vocab_import_table
                .find('input[name="vocab_fetch_method"]:checked')
                .val();

            //
            // Test the user supplied prefix and namespace...
            //
            var bUndefinedNamespace = (strNamespace === undefined || strNamespace === "");
            if ( ! bUndefinedNamespace && ! await RDFTransformCommon.validateNamespace(strNamespace) ) {
                // NOTE: The validatePrefix() call does its own alert dialog.
                // Let the user try again...
                return;
            }
            var bDefinedPrefix = this.#namespacesManager.hasPrefix(strPrefix);
            if (bUndefinedNamespace || bDefinedPrefix) {
                var strAlert =
                    // @ts-ignore
                    $.i18n('rdft-prefix/prefix') +
                    ' "' + strPrefix + '" ' +
                    ( bUndefinedNamespace ?
                        // @ts-ignore
                        $.i18n('rdft-prefix/must-define') :
                        // @ts-ignore
                        $.i18n('rdft-prefix/already-defined')
                    );
                alert(strAlert);
                // Let the user try again...
                return;
            }

            //
            // All Good: Process the Prefix Info for addition on the server...
            //
            var funcDismissBusy = null;

            let funcSuccess = (data) => {
                if (data.code === "error") {
                    alert(
                        // @ts-ignore
                        $.i18n('rdft-vocab/error-adding') + ': ' + strPrefix + "\n" +
                        data.message );
                }
                else if (this.#onDoneAdding) {
                    // Since we've successfully added the Prefix Info on the server,
                    // add it to the client for viewing...
                    this.#onDoneAdding(strPrefix, strNamespace);
                }
                funcDismissBusy();
                this.#dismiss();
            }

            if (strFetchOption === 'file') {
                // Prepare the form values by id attributes...
                // @ts-ignore
                $('#vocab-project').val(theProject.id);
                // @ts-ignore
                $('#vocab-prefix').val(strPrefix);
                // @ts-ignore
                $('#vocab-namespace').val(strNamespace);

                funcDismissBusy =
                    DialogSystem.showBusy(
                        // @ts-ignore
                        $.i18n('rdft-prefix/prefix-by-upload') + ' ' + strNamespace +
                        '<br />File: ' + this.#elements.file[0].files[0].name );

                Refine.wrapCSRF(
                    (token) => {
                        // Requires JQuery Form plugin...
                        // @ts-ignore
                        $(evt.currentTarget).ajaxSubmit(
                            {
                                url      : "command/rdf-transform/add-namespace-from-file",
                                type     : "post",
                                dataType : "json",
                                headers  : { 'X-CSRF-TOKEN': token },
                                success  : funcSuccess
                            }
                        );
                    }
                );
            }
            else { // strFetchOption === "prefix" or "web"
                // Prepare the data values...
                var postData = {};
                postData.project   = theProject.id;
                postData.prefix    = strPrefix;
                postData.namespace = strNamespace;
                postData.fetch     = strFetchOption;
                postData.fetchURL  = strNamespace;

                if (strFetchOption === 'web') {
                    // @ts-ignore
                    funcDismissBusy = DialogSystem.showBusy($.i18n('rdft-prefix/prefix-by-web') + ' ' + strNamespace);
                }
                else { // if (fetchOption === 'prefix') {
                    // @ts-ignore
                    funcDismissBusy = DialogSystem.showBusy($.i18n('rdft-prefix/prefix-add') + ' ' + strNamespace);
                }

                Refine.postCSRF(
                    "command/rdf-transform/add-namespace",
                    postData,
                    funcSuccess,
                    "json"
                );
            }
        });

        this.#elements.buttonOK
        .on("click", () => { this.#elements.file_upload_form.submit(); } );

        this.#elements.buttonCancel
        .on("click", () => { this.#dismiss(); } );

        this.#elements.buttonVocabImport
        .on("click", () => {
                this.#elements.vocab_import_table.show();
                // @ts-ignore
                $('#button-vocab-import').hide();
                // @ts-ignore
                $('#button-vocab-import').prop("disabled", "true");
            }
        );

        this.#elements.vocab_import_table
        .hide()
        .find('input[name="vocab_fetch_method"]')
        .on("click", (evt) => {
                // @ts-ignore
                var bHideUpload = ( $(evt.currentTarget).val() !== 'file' );
                this.#elements.vocab_import_table
                .find('.upload_file_inputs')
                .prop('disabled', bHideUpload);
            }
        );

        this.#elements.prefix
        // @ts-ignore
        .change( (evt) => { this.#suggestNamespace( $(evt.currentTarget).val() ); } )
        .focus();
    }

    #suggestNamespace(strPrefix) {
        if ( ! this.#elements.namespace.val() )
        {
            // @ts-ignore
            $.get(
                'command/rdf-transform/suggest-namespace',
                { "prefix": strPrefix },
                (data) => {
                    if (data !== null && data.code === "ok") {
                        let message = JSON.parse(data.message);
                        if ( message.namespace ) {
                            this.#elements.namespace.val(message.namespace);
                            let strNamespaceNote = '(';
                            let strPrefixCC = '<a target="_blank" href="http://prefix.cc">prefix.cc</a>';
                            if ( this.#elements.message.text() ) {
                                strNamespaceNote +=
                                    // @ts-ignore
                                    $.i18n('rdft-prefix/suggestion') +
                                    ' <em>' + strPrefixCC + '</em> ' +
                                    // @ts-ignore
                                    $.i18n('rdft-prefix/provided');
                            }
                            else {
                                // @ts-ignore
                                strNamespaceNote += $.i18n('rdft-prefix/suggested') + ' ' + strPrefixCC;
                            }
                            strNamespaceNote += ')';
                            this.#elements.namespace_note.html( strNamespaceNote );
                        }
                    }
                },
                "json"
            );
        }
    }

    #dismiss() {
        DialogSystem.dismissUntil(this.#level - 1);
    }
}
