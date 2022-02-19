/*
 *  CLASS RDFTransformNamespaceAdder
 *
 *  A class for adding a new Prefix to the Prefix Manager list
 */
class RDFTransformNamespaceAdder {
	#namespacesManager;
	#dialog;
	#elements;
	#level;

	#onDoneAdding;

	constructor(namespacesManager) {
		this.#namespacesManager = namespacesManager;

		this.#dialog = $( DOM.loadHTML(RDFTransform.KEY, "scripts/dialogs/rdf-transform-prefix-add.html") );
		this.#elements = DOM.bind(this.#dialog);
		this.#level = DialogSystem.showDialog(this.#dialog);

		this.#elements.dialogHeader.html(                   $.i18n('rdft-prefix/header')          );
		this.#elements.rdf_transform_prefix_prefix.html(    $.i18n('rdft-prefix/prefix') + ":"    );
		this.#elements.rdf_transform_prefix_namespace.html( $.i18n('rdft-prefix/namespace') + ":" );
		this.#elements.rdf_transform_prefix_voc.html(       $.i18n('rdft-prefix/voc')             );
		this.#elements.rdf_transform_prefix_only.html(      $.i18n('rdft-prefix/prefix-add-only') );
		this.#elements.rdf_transform_prefix_fetch.html(     $.i18n('rdft-prefix/fetch')           );
		this.#elements.rdf_transform_prefix_imp.html(       $.i18n('rdft-prefix/imp')             );
		this.#elements.rdf_transform_prefix_file.html(      $.i18n('rdft-prefix/file') + ":"      );
		this.#elements.rdf_transform_prefix_format.html(    $.i18n('rdft-prefix/format') + ":"    );
		this.#elements.rdf_transform_prefix_auto.html(      $.i18n('rdft-prefix/auto')            );
		this.#elements.rdf_transform_prefix_turtle.html(    $.i18n('rdft-prefix/turtle')          );
		this.#elements.rdf_transform_prefix_rdfxml.html(    $.i18n('rdft-prefix/rdfxml')          );
		this.#elements.rdf_transform_prefix_n3.html(        $.i18n('rdft-prefix/n3')              );
		this.#elements.rdf_transform_prefix_ntriple.html(   $.i18n('rdft-prefix/ntriple')         );
		this.#elements.rdf_transform_prefix_jsonld.html(    $.i18n('rdft-prefix/jsonld')          );
		this.#elements.rdf_transform_prefix_nquads.html(    $.i18n('rdft-prefix/nquads')          );
		this.#elements.rdf_transform_prefix_rdfjson.html(   $.i18n('rdft-prefix/rdfjson')         );
		this.#elements.rdf_transform_prefix_trig.html(      $.i18n('rdft-prefix/trig')            );
		this.#elements.rdf_transform_prefix_trix.html(      $.i18n('rdft-prefix/trix')            );
		this.#elements.rdf_transform_prefix_binary.html(    $.i18n('rdft-prefix/binary')          );

		this.#elements.buttonOK.html( $.i18n('rdft-buttons/ok') );
		this.#elements.buttonCancel.html( $.i18n('rdft-buttons/cancel') );
		this.#elements.buttonAdvanced.html( $.i18n('rdft-prefix/vocab-import') + "..." );

	}

	show(message, prefix, onDoneAdding) {
		if (message) {
			this.#elements.message.addClass('message').html(message);
		}

		if (prefix) {
			this.#elements.prefix.val(prefix);
			this.#suggestNamespace(prefix);
		}

		this.#onDoneAdding = onDoneAdding;

		this.#elements.file_upload_form
		.submit( async (evt) => {
			evt.preventDefault();

			var strPrefix = this.#elements.prefix.val();
			var strNamespace = this.#elements.namespace.val();
			var strFetchOption =
				this.#elements.fetching_options_table
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
					$.i18n('rdft-prefix/prefix') +
					' "' + strPrefix + '" ' +
					( bUndefinedNamespace ?
						$.i18n('rdft-prefix/must-define') :
						$.i18n('rdft-prefix/already-defined')
					);
				alert(strAlert);
				// Let the user try again...
				return;
			}

			//
			// All Good: Process the Prefix Info for addition on the server...
			//
			var postCmd = null;
			var postData = {};
			var dismissBusy = null;

			if (strFetchOption === 'file') {
				// Prepare the form values by id attributes...
				$('#vocab-project').val(theProject.id);
				$('#vocab-prefix').val(strPrefix);
				$('#vocab-namespace').val(strNamespace);

				postCmd = "command/rdf-transform/add-namespace-from-file";
				dismissBusy = DialogSystem.showBusy($.i18n('rdft-prefix/prefix-by-upload') + ' ' + strNamespace);
			}
			else {
				postCmd = "command/rdf-transform/add-namespace";
				// Prepare the data values...
				postData.project   = theProject.id;
				postData.prefix    = strPrefix;
				postData.namespace = strNamespace;
				postData.fetch     = strFetchOption;
				postData.fetchURL  = strNamespace;
	
				if (strFetchOption === 'web') {
					dismissBusy = DialogSystem.showBusy($.i18n('rdft-prefix/prefix-by-web') + ' ' + strNamespace);
				}
				else { // if (fetchOption === 'prefix') {
					dismissBusy = DialogSystem.showBusy($.i18n('rdft-prefix/prefix-add') + ' ' + strNamespace);
				}
			}

			Refine.postCSRF(
				postCmd,
				postData,
				(data) => {
					if (data.code === "error") {
						alert($.i18n('rdft-vocab/error-adding') + ': ' + strPrefix);
					}
					else if (this.#onDoneAdding) {
						// Since we've successfully added the Prefix Info on the server,
						// add it to the client for viewing...
						this.#onDoneAdding(strPrefix, strNamespace);
					}
					dismissBusy();
					this.#dismiss();
				},
				"json"
			);
		});

		this.#elements.buttonOK
		.on("click", () => { this.#elements.file_upload_form.submit(); } );

		this.#elements.buttonCancel
		.on("click", () => { this.#dismiss(); } );

		this.#elements.buttonAdvanced
		.on("click", () => {
				this.#elements.fetching_options_table.show();
				$('#button-advanced-options').hide();
				$('#button-advanced-options').prop("disabled", "true");
			}
		);

		this.#elements.fetching_options_table
		.hide()
		.find('input[name="vocab_fetch_method"]')
		.on("click", (evt) => {
				var bHideUpload = ( $(evt.currentTarget).val() !== 'file' );
				this.#elements.fetching_options_table
				.find('.upload_file_inputs')
				.prop('disabled', bHideUpload);
			}
		);

		this.#elements.prefix
		.change( (evt) => { this.#suggestNamespace( $(evt.currentTarget).val() ); } )
		.focus();
	}

	#suggestNamespace(prefix) {
		$.get(
			'command/rdf-transform/suggest-namespace',
			{ prefix: prefix },
			(data) => {
				if ( !this.#elements.namespace.val() && data.namespace ) {
					this.#elements.namespace.val(data.namespace);
					if ( this.#elements.message.text() ) {
						this.#elements.namespace_note.html(
							'(' + $.i18n('rdft-prefix/suggestion') +
							' <em><a target="_blank" href="http://prefix.cc">prefix.cc</a></em> ' +
							$.i18n('rdft-prefix/provided') + ')'
						);
					}
					else {
						this.#elements.namespace_note.html(
							'(' + $.i18n('rdft-prefix/suggested') +
							' <a target="_blank" href="http://prefix.cc">prefix.cc</a>)'
						);
					}
				}
			},
			"json"
		);
	}

	#dismiss() {
		DialogSystem.dismissUntil(this.#level - 1);
	}
}
