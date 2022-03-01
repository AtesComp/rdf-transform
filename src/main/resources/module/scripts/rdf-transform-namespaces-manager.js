/*
 * RDFTransformNamespacesManager
 *
 *   The Prefix Manager for the RDF Transform dialog
 */

class RDFTransformNamespacesManager {
	static globalNamespaces = null;

	namespaces;

	#dialog;

	constructor(dialog) {
		this.#dialog = dialog;

		// Namespaces have not been initialized...
		// Initialize after construction!
	}

	async init() {
		this.#dialog.theNamespaces.empty().html('<img src="images/small-spinner.gif" />');
		this.namespaces = this.#dialog.getNamespaces(); // ...existing namespaces

		if ( ! this.namespaces ) {
			var data = null;
			this.namespaces = {} // ...empty object, no namespaces
			try {
				data = await this.#getDefaults();
			}
			catch (evt) {
				// ...ignore error, no namespaces...
			}
			var bError = false;
			if (data !== null && "namespaces" in data) {
				this.namespaces = data.namespaces; // ...new defaults namespaces
			}
			else { // (data === null || data.code === "error")
				bError = true;
			}
			// We might have changed data for errors...
			if (bError) {
				alert("ERROR: Could not retrieve default namespaces!");
			}
			this.show();
		}
		else {
			this.#save();
			this.show();
		}
		RDFTransformNamespacesManager.globalNamespaces = this.namespaces;
	}

	reset() {
		this.#dialog.theNamespaces.empty().html('<img src="images/small-spinner.gif" />');
		this.namespaces = this.#dialog.getNamespaces();
		this.#save();
		this.show();
	}

	/*
	 * Method: getDefaults()
	 *
	 * 	Get the Default Namespaces from the server.  As this method returns a Promise, it expects
	 *  the caller is an "async" function "await"ing the results of the Promise.
	 *
	 */
	#getDefaults() {
		return new Promise(
			(resolve, reject) => {
				// GET default namespaces in ajax
				$.ajax(
					{	url  : "command/rdf-transform/get-default-namespaces",
						type : "GET",
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

	#save(onDoneSave) {
		Refine.postCSRF(
			"command/rdf-transform/save-namespaces",
			{   "project" : theProject.id,
				"namespaces" : this.namespaces
			},
			(data) => { if (onDoneSave) { onDoneSave(data); } },
			"json"
		);
	}

	#showManageWidget() {
		var vocabManager = new RDFTransformVocabManager(this);
		vocabManager.show();
	}

	show() {
		this.#dialog.theNamespaces.empty();
		for (const strPrefix in this.namespaces) {
			this.#render(strPrefix, this.namespaces[strPrefix]);
		}
		// Add button...
		$('<a href="#" class="add-namespace-box">' + $.i18n('rdft-prefix/add') + '</a>')
		.on("click",
			(evt) => {
				evt.preventDefault();
				this.addNamespace(false, false, false);
			}
		)
		.appendTo(this.#dialog.theNamespaces);

		// Manage button...
		$('<a href="#" class="manage-vocabularies-box">' + $.i18n('rdft-prefix/manage') + '</a>')
		.on("click",
			(evt) => {
				evt.preventDefault();
				this.#showManageWidget();
			}
		)
		.appendTo(this.#dialog.theNamespaces);

		// TODO: Add refresh all button
	}

	#render(strPrefix, strNamespace) {
		this.#dialog.theNamespaces
		.append(
			$('<span/>')
			.addClass('rdf-transform-prefix-box')
			.attr('title', strNamespace)
			.text(strPrefix)
		);
	}

	removeNamespace(strPrefixFind) {
		var iIndex = 0;
		for (const strPrefix in this.namespaces) {
			if (strPrefixFind === strPrefix) {
				this.namespaces.splice(iIndex, 1);
				iIndex--;
				this.#dialog.updatePreview();
			}
			iIndex++;
		}
	}

	addNamespace(strMessage, strPrefixGiven, onDoneAdd) {
		var widget = new RDFTransformNamespaceAdder(this);
		widget.show(
			strMessage,
			strPrefixGiven,
			(strPrefix, strNamespace) => {
				// NOTE: The RDFTransformNamespaceAdder should have validated the
				//		prefix information, so no checks are required here.

				// Add the Prefix and its Namespace...
				this.namespaces[strPrefix] = strNamespace;
				this.#save();
				this.show();

				if (onDoneAdd) {
					onDoneAdd(strPrefix);
				}
				this.#dialog.updatePreview();
			}
		);
	}

	hasPrefix(strPrefixFind) {
		for (const strPrefix in this.namespaces) {
			if (strPrefix === strPrefixFind) {
				return true;
			}
		}
		return false;
	}

	getNamespaceOfPrefix(strPrefixFind) {
		for (const strPrefix in this.namespaces) {
			if (strPrefix === strPrefixFind) {
				return this.namespaces[strPrefix];
			}
		}
		return null;
	}

	/*
	 * Some utility functions...
	 */

	async isPrefixedQName(strQName) {
		if ( await RDFTransformCommon.validateIRI(strQName) ) {
			var iIndex = strQName.indexOf(':'); // ...first ':'
			if ( strQName.substring(iIndex, iIndex + 3) !== "://" ) {
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
		// NOTE: Same start and end === "" (baseIRI)
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
		var objIRIParts = this.#deAssembleQName(strPrefixedQName);
		if ( objIRIParts.prefix === null ) {
			return objIRIParts.localPart;
		}
		if (objIRIParts.prefix in RDFTransformNamespacesManager.globalNamespaces) {
			return RDFTransformNamespacesManager.globalNamespaces[objIRIParts.prefix] +
				RDFTransformCommon.escapeLocalPart(objIRIParts.localPart);
		}
		if ( objIRIParts.prefix === "" ) {
			return this.#dialog.getBaseIRI() +
				RDFTransformCommon.escapeLocalPart(objIRIParts.localPart);
		}
		return objIRIParts.prefix + ":" + objIRIParts.localPart;
	}

	#deAssembleQName(strQName) {
		var iFull = strQName.indexOf("://");
		var iIndex = strQName.indexOf(':');
		var obj = {};
		if (iFull !== -1 || iIndex === -1) {
			obj.prefix = null;
			obj.localPart = strQName
		}
		else {
			obj.prefix = strQName.substring(0, iIndex),
			obj.localPart =
				RDFTransformCommon.unescapeLocalPart( strQName.substring(iIndex + 1) )
		}
		return obj;
	}
}
