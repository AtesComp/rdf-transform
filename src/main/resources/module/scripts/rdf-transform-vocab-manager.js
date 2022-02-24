import { RDFTransform } from "./rdf-transform";

class RDFTransformVocabManager {
	#namespacesManager;

	#level;
	#elements;

	constructor(namespacesManager) {
		this.#namespacesManager = namespacesManager;
	}

	show() {
		var dialog = $(DOM.loadHTML(RDFTransform.KEY, "scripts/dialogs/rdf-transform-vocab-manager.html"));
		this.#level = DialogSystem.showDialog(dialog);
		this.#elements = DOM.bind(dialog);

		this.#elements.dialogHeader.html($.i18n('rdft-vocab/header'));
		this.#elements.buttonAddNamespace.html($.i18n('rdft-buttons/add-namespace'));
		this.#elements.buttonOK.html($.i18n('rdft-buttons/ok'));
		this.#elements.buttonCancel.html($.i18n('rdft-buttons/cancel'));

		this.#elements.buttonCancel
		.on("click", () => { this.#dismiss(); } );

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
				this.#namespacesManager.show();
				this.#dismiss();
			}
		);
	}

	#getRemoveHandler(strPrefix) {
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

	#getRefreshHandler(strPrefix, strNamespace) {
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
			$('<tr>').addClass('rdf-table-even')
			.append($('<th/>').text($.i18n('rdft-vocab/prefix')))
			.append($('<th/>').text($.i18n('rdft-vocab/iri')))
			.append($('<th/>').text($.i18n('rdft-vocab/delete')))
			.append($('<th/>').text($.i18n('rdft-vocab/refresh')))
		);

		var bEven = false;
		for (const strPrefix in this.#namespacesManager.namespaces) {
			const strNamespace = this.#namespacesManager.namespaces[strPrefix];
			/** @type {HTMLElement} */
			// @ts-ignore
			var htmlRemoveNamespace =
				$('<a/>')
				.text( $.i18n('rdft-vocab/delete') )
				.attr('href', '#')
				.on("click", this.#getRemoveHandler(strPrefix) );
			/** @type {HTMLElement} */
			// @ts-ignore
			var htmlRefreshNamespace =
				$('<a/>')
				.text( $.i18n('rdft-vocab/refresh') )
				.attr('href', '#')
				.on("click", this.#getRefreshHandler(strPrefix, strNamespace) );
			var tr = $('<tr/>').addClass(bEven ? 'rdf-table-even' : 'rdf-table-odd')
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

export { RDFTransformVocabManager }
