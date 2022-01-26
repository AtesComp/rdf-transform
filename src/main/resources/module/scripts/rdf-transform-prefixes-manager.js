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
		this.prefixes = this.#dialog.getPrefixes(); // ...existing prefixes

		if ( ! this.prefixes ) {
			var data = null;
			this.prefixes = []; // ...empty array, no prefixes
			try {
				data = await this.#getDefaultPrefixes();
			}
			catch (evt) {
				// ...ignore error, no prefixes...
			}
			if (data !== null && data.prefixes) {
				this.prefixes = data.prefixes; // ...new defaults prefixes
			}
			this.showPrefixes(this.prefixes);
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

	/*
	 * Method: getDefaultPrefixes()
	 *
	 * 	Get the Default Prefixes from the server.  As this method returns a Promise, it expects
	 *  the caller is an "async" function "await"ing the results of the Promise.
	 * 
	 */
	#getDefaultPrefixes() {
		return new Promise(
			(resolve, reject) => {
				// GET default prefixes in ajax
				$.ajax(
					{	url  : "command/rdf-transform/get-default-prefixes",
						type : 'GET',
						async: false, // ...wait on results
						data : { "project" : theProject.id },
						dataType : "json",
						success : (result, strStatus, xhr) => { resolve(result); },
						error   : (xhr, strStatus, error) => { resolve(null); }
					}
				);
			}
		);
	}

	#savePrefixes(onDoneSave) {
		Refine.wrapCSRF(
			(token) => {
				$.ajax(
					{	url  : "command/rdf-transform/save-prefixes",
						type : 'POST',
						data : {
							"project"    : theProject.id,
							"csrf_token" : token,
							"prefixes"   : JSON.stringify(this.prefixes)
						},
						dataType : "json",
						success : (data) => { if (onDoneSave) { onDoneSave(data); } }
					}
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
