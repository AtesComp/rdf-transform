/*
 *  CLASS PrefixAdder
 *
 *  A class for adding a new Prefix to the Prefix Manager list
 */
class PrefixAdder {
	#prefixesManager;
	#dialog;
	#elements;
	#level;

	#onDoneShow;

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

		this.#elements.buttonOK.html( $.i18n('rdft-buttons/ok') );
		this.#elements.buttonCancel.html( $.i18n('rdft-buttons/cancel') );
		this.#elements.buttonAdvanced.html( $.i18n('rdft-prefix/vocab-import') + "..." );

	}

	show(message, prefix, onDoneShow) {
		if (message) {
			this.#elements.message.addClass('message').html(message);
		}

		if (prefix) {
			this.#elements.prefix.val(prefix);
			this.#suggestIRI(prefix);
		}

		this.#onDoneShow = onDoneShow;

		Refine.wrapCSRF( (token) => {
			this.#elements.file_upload_form
			.submit( (evt) => {
				evt.preventDefault();

				var fetchOption =
					this.#elements.fetching_options_table
					.find('input[name="vocab_fetch_method"]:checked')
					.val();

				var name = this.#elements.prefix.val();
				var iri = this.#elements.iri.val();

				var bBlankIRI = (iri == undefined || iri == "");
				var bDefinedPrefix = this.#prefixesManager.hasPrefix(name);
				if (bBlankIRI || bDefinedPrefix) {
					var strAlert =
						$.i18n('rdft-prefix/pref') +
						' "' + name + '" ' +
						( bBlankIRI ?
							$.i18n('rdft-prefix/iri-cannot-be-blank') :
							$.i18n('rdft-prefix/defined')
						);
					alert(strAlert);
					return;
				}

				var dismissBusy;

				if (fetchOption === 'file') {
					// Prepare values...
					$('#vocab-hidden-prefix').val(name);
					$('#vocab-hidden-iri').val(iri);
					$('#vocab-hidden-project').val(theProject.id);

					dismissBusy = DialogSystem.showBusy($.i18n('rdft-prefix/voc-upload') + ' ' + iri);

					$(evt.currentTarget)
					.post(
						{
							"url"      : "command/rdf-transform/upload-file-add-prefix",
							"dataType" : "json",
							"headers"  : { 'X-CSRF-TOKEN': token },
							"success"  : (data) => {
								dismissBusy();
								if (data.code === 'error') {
									alert("Error: " + data.message);
								}
								else {
									if (this.#onDoneShow) {
										this.#onDoneShow(name, iri);
										this.#dismiss();
									}
								}
							}
						}
					);
					return false;
				}
				else if (fetchOption === 'web') {
					dismissBusy = DialogSystem.showBusy($.i18n('rdft-prefix/web-import') + ' ' + iri);
				}
				else if (fetchOption === 'prefix') {
					dismissBusy = DialogSystem.showBusy($.i18n('rdft-prefix/prefix-only') + ' ' + iri);
				}

				$.post("command/rdf-transform/add-prefix",
					{
						"csrf_token"   : token,
						"name"         : name,
						"iri"          : iri,
						"fetch-url"    : iri,
						"project"      : theProject.id,
						"fetch"        : fetchOption
					},
					(data) => {
						dismissBusy();
						if (data.code === "error") {
							alert('Error:' + data.message);
						}
						else {
							if (this.#onDoneShow) {
								this.#onDoneShow(name, iri);
								this.#dismiss();
							}
						}
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
				this.#elements.fetching_options_table
				.show();
				$('#button_advanced_options')
				.hide();
				$('#button_advanced_options')
				.prop("disabled", "true");
			}
		);

		this.#elements.fetching_options_table
		.hide()
		.find('input[name="vocab_fetch_method"]')
		.click( (evt) => {
				var upload = $(evt.currentTarget).val() !== 'file';
				this.#elements.fetching_options_table
				.find('.upload_file_inputs')
				.prop('disabled', upload);
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
