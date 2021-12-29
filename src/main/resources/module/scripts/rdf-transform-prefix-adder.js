/*
 *  CLASS RDFTransformPrefixAdder
 *
 *  A class for adding a new Prefix to the Prefix Manager list
 */
class RDFTransformPrefixAdder {
	#prefixesManager;
	#dialog;
	#elements;
	#level;

	#onDoneAdding;

	constructor(prefixesManager) {
		this.#prefixesManager = prefixesManager;

		this.#dialog = $( DOM.loadHTML("rdf-transform", "scripts/dialogs/rdf-transform-prefix-add.html") );
		this.#elements = DOM.bind(this.#dialog);
		this.#level = DialogSystem.showDialog(this.#dialog);

		this.#elements.dialogHeader.html(                 $.i18n('rdft-prefix/header')      );
		this.#elements.rdf_transform_prefix_pre.html(     $.i18n('rdft-prefix/pre') + ":"   );
		this.#elements.rdf_transform_prefix_iri.html(     $.i18n('rdft-prefix/iri') + ":"   );
		this.#elements.rdf_transform_prefix_voc.html(     $.i18n('rdft-prefix/voc')         );
		this.#elements.rdf_transform_prefix_only.html(    $.i18n('rdft-prefix/prefix')      );
		this.#elements.rdf_transform_prefix_fetch.html(   $.i18n('rdft-prefix/fetch')       );
		this.#elements.rdf_transform_prefix_imp.html(     $.i18n('rdft-prefix/imp')         );
		this.#elements.rdf_transform_prefix_file.html(    $.i18n('rdft-prefix/file') + ":"  );
		this.#elements.rdf_transform_prefix_format.html(  $.i18n('rdft-prefix/format') + ":");
		this.#elements.rdf_transform_prefix_auto.html(    $.i18n('rdft-prefix/auto')        );
		this.#elements.rdf_transform_prefix_turtle.html(  $.i18n('rdft-prefix/turtle')      );
		this.#elements.rdf_transform_prefix_rdfxml.html(  $.i18n('rdft-prefix/rdfxml')      );
		this.#elements.rdf_transform_prefix_n3.html(      $.i18n('rdft-prefix/n3')          );
		this.#elements.rdf_transform_prefix_ntriple.html( $.i18n('rdft-prefix/ntriple')     );
		this.#elements.rdf_transform_prefix_jsonld.html(  $.i18n('rdft-prefix/jsonld')      );
		this.#elements.rdf_transform_prefix_nquads.html(  $.i18n('rdft-prefix/nquads')      );
		this.#elements.rdf_transform_prefix_rdfjson.html( $.i18n('rdft-prefix/rdfjson')     );
		this.#elements.rdf_transform_prefix_trig.html(    $.i18n('rdft-prefix/trig')        );
		this.#elements.rdf_transform_prefix_trix.html(    $.i18n('rdft-prefix/trix')        );
		this.#elements.rdf_transform_prefix_binary.html(  $.i18n('rdft-prefix/binary')      );

		this.#elements.buttonOK.html( $.i18n('rdft-buttons/ok') );
		this.#elements.buttonCancel.html( $.i18n('rdft-buttons/cancel') );
		this.#elements.buttonAdvanced.html( $.i18n('rdft-prefix/vocab-import') + "..." );

	}

	async show(message, prefix, onDoneAdding) {
		if (message) {
			this.#elements.message.addClass('message').html(message);
		}

		if (prefix) {
			this.#elements.prefix.val(prefix);
			this.#suggestIRI(prefix);
		}

		this.#onDoneAdding = onDoneAdding;

		Refine.wrapCSRF( (token) => {
			this.#elements.file_upload_form
			.submit( async (evt) => {
				evt.preventDefault();

				var fetchOption =
					this.#elements.fetching_options_table
					.find('input[name="vocab_fetch_method"]:checked')
					.val();

				var strPrefix = this.#elements.prefix.val();
				var strIRI = this.#elements.iri.val();

				//
				// Test the user supplied prefix and IRI...
				//
				var bBlankIRI = (strIRI == undefined || strIRI == "");
				if ( ! bBlankIRI && ! await RDFTransformCommon.validatePrefix(strIRI) ) {
					// NOTE: The validatePrefix() call does its own alert dialog.
					// Let the user try again...
					return;
				}
				var bDefinedPrefix = this.#prefixesManager.hasPrefix(strPrefix);
				if (bBlankIRI || bDefinedPrefix) {
					var strAlert =
						$.i18n('rdft-prefix/pref') +
						' "' + strPrefix + '" ' +
						( bBlankIRI ?
							$.i18n('rdft-prefix/iri-cannot-be-blank') :
							$.i18n('rdft-prefix/defined')
						);
					alert(strAlert);
					// Let the user try again...
					return;
				}

				//
				// All Good: Process the Prefix Info for addition on the server...
				//
				var dismissBusy = null;
				var postCmd = "command/rdf-transform/add-prefix";
				// Prepare the data values...
				var postData = {
					"csrf_token" : token,
					"name"       : strPrefix,
					"iri"        : strIRI,
					"fetch-url"  : strIRI,
					"project"    : theProject.id,
					"fetch"      : fetchOption
				};

				if (fetchOption === 'file') {
					// Prepare the form values by id attributes...
					$('#vocab-prefix').val(strPrefix);
					$('#vocab-iri').val(strIRI);
					$('#vocab-project').val(theProject.id);

					postCmd = "command/rdf-transform/add-prefix-from-file";
					postData = {
						"csrf_token" : token,
						"dataType" : "json"
					};
					dismissBusy = DialogSystem.showBusy($.i18n('rdft-prefix/prefix-by-upload') + ' ' + strIRI);
				}
				else if (fetchOption === 'web') {
					dismissBusy = DialogSystem.showBusy($.i18n('rdft-prefix/prefix-by-web') + ' ' + strIRI);
				}
				else if (fetchOption === 'prefix') {
					dismissBusy = DialogSystem.showBusy($.i18n('rdft-prefix/prefix-only') + ' ' + strIRI);
				}

				$.post(
					postCmd,
					postData,
					(data) => {
						dismissBusy();
						if (data.code === 'error') {
							alert("Error: " + data.message);
						}
						else if (this.#onDoneAdding) {
							// Since we've successfully added the Prefix Info on the server,
							// add it to the client for viewing...
							this.#onDoneAdding(strPrefix, strIRI);
						}
						this.#dismiss();
					}
				);
			});
		});

		this.#elements.buttonOK
		.click( () => { this.#elements.file_upload_form.submit(); } );

		this.#elements.buttonCancel
		.click( () => { this.#dismiss(); } );

		this.#elements.buttonAdvanced
		.click( () => {
				this.#elements.fetching_options_table.show();
				$('#button-advanced-options').hide();
				$('#button-advanced-options').prop("disabled", "true");
			}
		);

		this.#elements.fetching_options_table
		.hide()
		.find('input[name="vocab_fetch_method"]')
		.click( (evt) => {
				var bHideUpload = ( $(evt.currentTarget).val() !== 'file' );
				this.#elements.fetching_options_table
				.find('.upload_file_inputs')
				.prop('disabled', bHideUpload);
			}
		);

		this.#elements.prefix
		.change( (evt) => { this.#suggestIRI( $(evt.currentTarget).val() ); } )
		.focus();
	}

	#suggestIRI(prefix) {
		$.get(
			'command/rdf-transform/get-prefix-cc-iri',
			{ prefix: prefix },
			(data) => {
				if ( !this.#elements.iri.val() && data.iri ) {
					this.#elements.iri.val(data.iri);
					if ( this.#elements.message.text() ) {
						this.#elements.iri_note.html(
							'(' + $.i18n('rdft-prefix/suggestion') +
							' <em><a target="_blank" href="http://prefix.cc">prefix.cc</a></em> ' +
							$.i18n('rdft-prefix/provided') + ')'
						);
					}
					else {
						this.#elements.iri_note.html(
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
