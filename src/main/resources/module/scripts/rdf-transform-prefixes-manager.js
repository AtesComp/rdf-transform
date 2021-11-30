/*
 * RDFTransformPrefixesManager
 *
 *   The Prefix Manager for the RDF Transform dialog
 */

class RDFTransformPrefixesManager {
	static globalPrefixes = null;

	prefixes;

	#dialog;

	constructor(dialog) {
		this.#dialog = dialog;

		// Prefixes have not been initialized...
		// Initialize after construction!
	}

	async initPrefixes() {
		this.#dialog.thePrefixes.empty().html('<img src="images/small-spinner.gif" />');
		this.prefixes = this.#dialog.getPrefixes();

		if ( ! this.prefixes ) {
			var waitOnPrefixes =
				async () => {
					return await this.#getDefaultPrefixes();
			}
			var data = await waitOnPrefixes();
			if (data.prefixes) {
				this.prefixes = data.prefixes;
				this.showPrefixes(this.prefixes);
			}
		}
		else {
			this.#savePrefixes();
			this.showPrefixes();
		}
		RDFTransformPrefixesManager.globalPrefixes = this.prefixes;
	}

	#getDefaultPrefixes() {
		return new Promise(
			(resolve, reject) => {
				$.get(
					"command/rdf-transform/get-default-prefixes",
					{ "project" : theProject.id },
					(data) => { resolve(data); },
					"json"
				);
			}
		);
	}

	#savePrefixes(onDoneSave) {
		Refine.wrapCSRF(
			(token) => {
				$.post(
					"command/rdf-transform/save-prefixes",
					{
						"project"    : theProject.id,
						"csrf_token" : token,
						"prefixes"   : JSON.stringify(this.prefixes)
					},
					(data) => {
						if (onDoneSave) {
							onDoneSave(data);
						}
					},
					"json"
				);
			}
		);
	}

	#showManagePrefixesWidget() {
		var vocabManager = new VocabManager(this);
		vocabManager.show();
	}

	showPrefixes() {
		this.#dialog.thePrefixes.empty();
		for (const prefix of this.prefixes) {
			this.#renderPrefix(prefix.name, prefix.iri);
		}
		// Add button...
		$('<a href="#" class="add-prefix-box">' + $.i18n('rdft-prefix/add') + '</a>')
		.click(
			(evt) => {
				evt.preventDefault();
				this.addPrefix(false, false, false);
			}
		)
		.appendTo(this.#dialog.thePrefixes);

		// Manage button...
		$('<a href="#" class="manage-vocabularies-box">' + $.i18n('rdft-prefix/manage') + '</a>')
		.click(
			(evt) => {
				evt.preventDefault();
				this.#showManagePrefixesWidget();
			}
		)
		.appendTo(this.#dialog.thePrefixes);

		// TODO: Add refresh all button
	}

	#renderPrefix(prefix, iri) {
		this.#dialog.thePrefixes
		.append(
			$('<span/>')
			.addClass('rdf-transform-prefix-box')
			.attr('title', iri)
			.text(prefix)
		);
	}

	removePrefix(name) {
		var iIndex = 0;
		for (const prefix of this.prefixes) {
			if (name === prefix.name) {
				this.prefixes.splice(iIndex, 1);
				iIndex--;
			}
			iIndex++;
		}
	}
	
	addPrefix(message, prefix, onDoneAdd) {
		var widget = new PrefixAdder(this);
		widget.show(
			message,
			prefix,
			(name, iri) => {
				this.prefixes.push(
					{	"name" : name,
						"iri"  : iri
					}
				);
				this.#savePrefixes(
					() => {
						this.showPrefixes();
					}
				);
				if (onDoneAdd) {
					onDoneAdd();
				}
			}
		);
	}
	
	hasPrefix(name) {
		for (const prefix of this.prefixes) {
			if (prefix.name === name) {
				return true;
			}
		}
		return false;
	}

	/*
	 * Some utility functions...
	 */

	static isPrefixedQName(strQName) {
		// TODO: This is NOT an IRI match!
		return strQName.match(/[_a-zA-Z][-_a-zA-Z0-9]*:($|([_a-zA-Z][-_a-zA-Z0-9]*))$/);
	}

	static deAssembleQName(strQName) {
		var iIndex = strQName.indexOf(':');
		if (iIndex === -1) {
			return {
				"prefix"    : null,
				"localPart" : strQName
			};
		}
		return {
			"prefix"    : strQName.substring(0, iIndex),
			"localPart" : strQName.substring(iIndex + 1)
		};
	}

	static getPrefixFromQName(strQName) {
		var iIndex = strQName.indexOf(':');
		if (iIndex === -1) {
			return null;
		}
		return strQName.substring(0, iIndex);
	}

	static getFullIRIFromQName(strPrefixedQName) {
		var strIRI = RDFTransformPrefixesManager.deAssembleQName(strPrefixedQName);
		if ( !strIRI.prefix ) {
			return null;
		}
		for (const prefix of RDFTransformPrefixesManager.globalPrefixes) {
			if (prefix.name === strIRI.prefix) {
				return prefix.iri + strIRI.localPart;
			}
		}
		return null;
	}
};
