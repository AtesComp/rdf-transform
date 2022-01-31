class RDFTransformVocabManager {
	#prefixesManager;

	#level;
	#elements;

	constructor(prefixesManager) {
		this.#prefixesManager = prefixesManager;
	}

	show() {
		var dialog = $(DOM.loadHTML("rdf-transform", "scripts/dialogs/rdf-transform-vocab-manager.html"));
		this.#level = DialogSystem.showDialog(dialog);
		this.#elements = DOM.bind(dialog);

		this.#elements.dialogHeader.html($.i18n('rdft-vocab/header'));
		this.#elements.buttonAddPrefix.html($.i18n('rdft-buttons/add-prefix'));
		this.#elements.buttonOK.html($.i18n('rdft-buttons/ok'));
		this.#elements.buttonCancel.html($.i18n('rdft-buttons/cancel'));

		this.#elements.buttonCancel
		.click( () => { this.#dismiss(); } );

		this.#elements.buttonAddPrefix
		.click(
			(evt) => {
				evt.preventDefault();
				this.#prefixesManager.addPrefix(
					false, false,
					() => {
						this.#renderBody();
					}
				);
			}
		);

		this.#renderBody();

		this.#elements.buttonOK
		.click(
			() => {
				this.#prefixesManager.showPrefixes();
				this.#dismiss();
			}
		);
	}

	#getDeleteHandler(strPrefix) {
		return (evtHandler) => {
			evtHandler.preventDefault();
			var dismissBusy = DialogSystem.showBusy($.i18n('rdft-vocab/deleting-pref') + ' ' + strPrefix);

			Refine.wrapCSRF(
				(token) => {
					$.post(
						'command/rdf-transform/remove-prefix',
						{
							'prefix': strPrefix,
							'project': theProject.id,
							'csrf_token': token,
						},
						(data) => {
							dismissBusy();
							if (data.code === 'error') {
								alert($.i18n('rdft-vocab/error-deleting') + ': ' + strPrefix);
							}
							else {
								this.#prefixesManager.removePrefix(strPrefix);
							}
							this.#renderBody();
						}
					);
				}
			);
		};
	}

	#getRefreshHandler(strPrefix, strNamespace) {
		return (evtHandler) => {
			evtHandler.preventDefault();
			if ( window.confirm(
					$.i18n('rdft-vocab/desc-one') + ' "' + strNamespace + '"\n' +
					$.i18n('rdft-vocab/desc-two') ) )
			{
				var dismissBusy =
					DialogSystem.showBusy($.i18n('rdft-vocab/refresh-pref') + ' ' + strPrefix);
				Refine.wrapCSRF(
					(token) => {
						$.post('command/rdf-transform/refresh-prefix',
							{	'prefix': strPrefix,
								'namespace': strNamespace,
								'project': theProject.id,
								'csrf_token': token
							},
							(data) => {
								dismissBusy();
								if (data.code === 'error') {
									alert($.i18n('rdft-vocab/alert-wrong') + ': ' + data.message);
								}
							}
						);
					}
				);
			}
		};
	}

	#renderBody() {
		var table = this.#elements.prefixesTable;
		table.empty();
		table.append(
			$('<tr>').addClass('rdf-table-even')
			.append($('<th/>').text($.i18n('rdft-vocab/prefix')))
			.append($('<th/>').text($.i18n('rdft-vocab/iri')))
			.append($('<th/>').text($.i18n('rdft-vocab/delete')))
			.append($('<th/>').text($.i18n('rdft-vocab/refresh')))
		);

		var bEven = false;
		for (const strPrefix in this.#prefixesManager.prefixes) {
			var strNamespace = this.#prefixesManager.prefixes[strPrefix];
			var delete_handle =
				$('<a/>')
				.text( $.i18n('rdft-vocab/delete') )
				.attr('href', '#')
				.click( this.#getDeleteHandler(strPrefix) );
			var refresh_handle =
				$('<a/>')
				.text( $.i18n('rdft-vocab/refresh') )
				.attr('href', '#')
				.click( this.#getRefreshHandler(strPrefix, strNamespace) );
			var tr = $('<tr/>').addClass(bEven ? 'rdf-table-even' : 'rdf-table-odd')
				.append($('<td>').text(strPrefix))
				.append($('<td>').text(strPrefix.iri))
				.append($('<td>').html(delete_handle))
				.append($('<td>').html(refresh_handle));
			table.append(tr);
			bEven = !bEven;
		}

	}

	#dismiss() {
		DialogSystem.dismissUntil(this.#level - 1);
	}
}
