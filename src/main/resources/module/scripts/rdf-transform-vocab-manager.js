class RDFTransformVocabManager {
	constructor(prefixesManager) {
		this._prefixesManager = prefixesManager;
	}

	show() {
		var dialog = $(DOM.loadHTML("rdf-transform", "scripts/dialogs/rdf-transform-vocab-manager.html"));
		this._level = DialogSystem.showDialog(dialog);
		this._elements = DOM.bind(dialog);

		this._elements.dialogHeader.html($.i18n('rdft-vocab/header'));
		this._elements.buttonAddPrefix.html($.i18n('rdft-buttons/add-prefix'));
		this._elements.buttonOK.html($.i18n('rdft-buttons/ok'));
		this._elements.buttonCancel.html($.i18n('rdft-buttons/cancel'));

		this._elements.buttonCancel
		.click( () => { this.#dismiss(); } );

		this._elements.buttonAddPrefix
		.click(
			(evt) => {
				evt.preventDefault();
				this._prefixesManager.addPrefix(
					false, false,
					() => {
						this.#renderBody();
					}
				);
			}
		);

		this.#renderBody();

		this._elements.buttonOK
		.click(
			() => {
				this._prefixesManager.showPrefixes();
				this.#dismiss();
			}
		);
	}

	#getDeleteHandler(name) {
		return (evtHandler) => {
			evtHandler.preventDefault();
			var dismissBusy = DialogSystem.showBusy($.i18n('rdft-vocab/deleting-pref') + ' ' + name);

			Refine.wrapCSRF(
				(token) => {
					$.post(
						'command/rdf-transform/remove-prefix',
						{
							'name': name,
							'project': theProject.id,
							'csrf_token': token,
						},
						(data) => {
							dismissBusy();
							if (data.code === 'error') {
								// TODO: Update to proper error handling...
								console.log($.i18n('rdft-vocab/error-deleting'));
							}
							else {
								this._prefixesManager.removePrefix(name);
								this.#renderBody();
							}
						}
					);
				}
			);
		};
	}

	#getRefreshHandler(name, iri) {
		return (evtHandler) => {
			evtHandler.preventDefault();
			if ( window.confirm(
					$.i18n('rdft-vocab/desc-one') + ' "' + iri + '"\n' +
					$.i18n('rdft-vocab/desc-two') ) )
			{
				var dismissBusy =
					DialogSystem.showBusy($.i18n('rdft-vocab/refresh-pref') + ' ' + name);
				Refine.wrapCSRF(
					(token) => {
						$.post('command/rdf-transform/refresh-prefix',
							{	'name': name,
								'iri': iri,
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
		var table = this._elements.prefixesTable;
		table.empty();
		table.append(
			$('<tr>').addClass('rdf-table-even')
			.append($('<th/>').text($.i18n('rdft-vocab/prefix')))
			.append($('<th/>').text($.i18n('rdft-vocab/iri')))
			.append($('<th/>').text($.i18n('rdft-vocab/delete')))
			.append($('<th/>').text($.i18n('rdft-vocab/refresh')))
		);

		var bEven = false;
		for (const prefix of this._prefixesManager.prefixes) {
			var name = prefix.name;
			var iri = prefix.iri;
			var delete_handle =
				$('<a/>')
				.text( $.i18n('rdft-vocab/delete') )
				.attr('href', '#')
				.click( this.#getDeleteHandler(name) );
			var refresh_handle =
				$('<a/>')
				.text( $.i18n('rdft-vocab/refresh') )
				.attr('href', '#')
				.click( this.#getRefreshHandler(name, iri) );
			var tr = $('<tr/>').addClass(bEven ? 'rdf-table-even' : 'rdf-table-odd')
				.append($('<td>').text(prefix.name))
				.append($('<td>').text(prefix.iri))
				.append($('<td>').html(delete_handle))
				.append($('<td>').html(refresh_handle));
			table.append(tr);
			bEven = !bEven;
		}

	}

	#dismiss() {
		DialogSystem.dismissUntil(this._level - 1);
	}
}
