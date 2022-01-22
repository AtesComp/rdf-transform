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
			//var waitOnPrefixes =
			//	async () => {
			//		return await this.#getDefaultPrefixes();
			//}
			var data = await this.#getDefaultPrefixes();
			if (data.prefixes) {
				this.prefixes = data.prefixes;
				this.showPrefixes(this.prefixes);
			}
			else {

			}
		}
		else {
			this.#savePrefixes();
			this.showPrefixes();
		}
		RDFTransformPrefixesManager.globalPrefixes = this.prefixes;
	}

	resetPrefixes() {
		this.#dialog.thePrefixes.empty().html('<img src="images/small-spinner.gif" />');
		this.prefixes = this.#dialog.getPrefixes();
		this.#savePrefixes();
		this.showPrefixes();
	}

	async #getDefaultPrefixes() {
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
		var vocabManager = new RDFTransformVocabManager(this);
		vocabManager.show();
	}

	showPrefixes() {
		this.#dialog.thePrefixes.empty();
		for (const strPrefix of this.prefixes) {
			this.#renderPrefix(strPrefix, this.prefixes[strPrefix]);
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

	#renderPrefix(strPrefix, strNamespace) {
		this.#dialog.thePrefixes
		.append(
			$('<span/>')
			.addClass('rdf-transform-prefix-box')
			.attr('title', strNamespace)
			.text(strPrefix)
		);
	}

	removePrefix(strPrefixFind) {
		var iIndex = 0;
		for (const strPrefix of this.prefixes) {
			if (strPrefixFind === strPrefix) {
				this.prefixes.splice(iIndex, 1);
				iIndex--;
			}
			iIndex++;
		}
	}
	
	addPrefix(strMessage, strPrefixGiven, onDoneAdd) {
		var widget = new RDFTransformPrefixAdder(this);
		widget.show(
			strMessage,
			strPrefixGiven,
			(strPrefix, strNamespace) => {
				// NOTE: The RDFTransformPrefixAdder should have validated the
				//		prefix information, so no checks are required here.

				// Add the Prefix and its Namespace...
				var obj = {
					[strPrefix] : strNamespace
				}
				this.prefixes.push(obj);
				this.#savePrefixes();
				this.showPrefixes();

				if (onDoneAdd) {
					onDoneAdd(strPrefix);
				}
			}
		);
	}
	
	hasPrefix(strPrefixFind) {
		for (const prefix of this.prefixes) {
			if (prefix === strPrefixFind) {
				return true;
			}
		}
		return false;
	}

	getNamespaceOfPrefix(strPrefixFind) {
		for (const strPrefix of this.prefixes) {
			if (strPrefix === strPrefixFind) {
				return this.prefixes[strPrefix];
			}
		}
		return null;
	}

	/*
	 * Some utility functions...
	 */

	async isPrefixedQName(strQName) {
		if ( await RDFTransformCommon.validateIRI(strQName) ) {
			var iIndex = strQName.indexOf(':');
			if ( strQName.substring(iIndex, iIndex + 3) != "://" ) {
				return 1; // ...prefixed
			}
			return 0; // ...not prefixed, but good IRI
		}
		return -1; // ...bad IRI
	}

	getPrefixFromQName(strQName) {
		var iIndex = strQName.indexOf(':');
		if (iIndex === -1) {
			return null;
		}
		return strQName.substring(0, iIndex);
	}

	getSuffixFromQName(strQName) {
		var iIndex = strQName.indexOf(':');
		if (iIndex === -1) {
			return null;
		}
		return strQName.substring(iIndex + 1);
	}

	getFullIRIFromQName(strPrefixedQName) {
		var objIRI = this.#deAssembleQName(strPrefixedQName);
		if ( !objIRI.prefix ) {
			return null;
		}
		for (const prefix of RDFTransformPrefixesManager.globalPrefixes) {
			if (prefix === objIRI.prefix) {
				return RDFTransformPrefixesManager.globalPrefixes[prefix] + objIRI.localPart;
			}
		}
		return null;
	}

	#deAssembleQName(strQName) {
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
};
