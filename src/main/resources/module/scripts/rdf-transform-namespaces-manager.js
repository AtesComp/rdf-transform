/*
 * RDFTransformNamespacesManager
 *
 *   The Prefix Manager for the RDF Transform dialog
 */

class RDFTransformNamespacesManager {
	static globalNamespaces = null;

	#theNamespaces;

	#dialog;

	constructor(dialog) {
		this.#dialog = dialog;

		// Namespaces have not been initialized...
		// Initialize after construction!
	}

	async init() {
		this.#dialog.theNamespaces.empty().html('<img src="images/small-spinner.gif" />');
		this.#theNamespaces = this.#dialog.getNamespaces(); // ...existing namespaces

		if ( ! this.#theNamespaces ) {
			var data = null;
			this.#theNamespaces = {} // ...empty object, no namespaces
			try {
				data = await this.#getDefaults();
			}
			catch (evt) {
				// ...ignore error, no namespaces...
			}
			var bError = false;
			if (data !== null && "namespaces" in data) {
				this.#theNamespaces = data.namespaces; // ...new defaults namespaces
			}
			else { // (data === null || data.code === "error")
				bError = true;
			}
			// We might have namespace errors...
			if (bError) {
				alert("ERROR: Could not retrieve default namespaces!"); // TODO: $.i18n()
			}
			this.show();
		}
		else {
			this.#save();
			this.show();
		}
		RDFTransformNamespacesManager.globalNamespaces = this.#theNamespaces;
	}

	reset() {
		this.#dialog.theNamespaces.empty().html('<img src="images/small-spinner.gif" />');
		this.#theNamespaces = this.#dialog.getNamespaces();
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
				"namespaces" : this.#theNamespaces
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
		for (const strPrefix in this.#theNamespaces) {
			this.#render(strPrefix, this.#theNamespaces[strPrefix]);
		}
		// Add button...
		var linkAdd = $('<a href="#" class="add-namespace-box">' + $.i18n('rdft-prefix/add') + '</a>');
		this.#dialog.theNamespaces.append(linkAdd);
		//var imgAdd =
		//	$('<img />')
		//	.attr('src', 'images/add.png')
		//	.css('cursor', 'pointer');
		//var buttonAdd =
		//	$('<button />')
		//	.addClass('button')
		//	.append(imgAdd)
		//	.append(" " + $.i18n('rdft-prefix/add'))
		//	.on("click",
		//		(evt) => {
		//			evt.preventDefault();
		//			this.addNamespace(false, false, false);
		//		}
		//	);
		//this.#dialog.theNamespaces.append(buttonAdd);

		// Manage button...
		var linkManage = $('<a href="#" class="manage-vocabularies-box">' + $.i18n('rdft-prefix/manage') + '</a>')
		this.#dialog.theNamespaces.append(linkManage);
		//var buttonManage =
		//	$('<button />')
		//	.addClass('button')
		//	.html('<img src="images/configure.png" /> ' + $.i18n('rdft-prefix/manage'))
		//	.on("click",
		//		(evt) => {
		//			evt.preventDefault();
		//			this.#showManageWidget();
		//		}
		//	);
		//this.#dialog.theNamespaces.append(buttonManage);

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

	isNull() {
		if (this.#theNamespaces === null) {
			return true;
		}
		return false;
	}

	isEmpty() {
		if (this.#theNamespaces === null || Object.keys(this.#theNamespaces).length === 0) {
			return true;
		}
		return false;
	}

	getNamespaces(){
		return this.#theNamespaces
	}

	removeNamespace(strPrefixFind) {
		if (strPrefixFind in this.#theNamespaces) {
			delete this.#theNamespaces[strPrefixFind];
			this.#dialog.updatePreview();
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
				this.#theNamespaces[strPrefix] = strNamespace;
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
		if (strPrefixFind in this.#theNamespaces) {
			return true;
		}
		return false;
	}

	getNamespaceOfPrefix(strPrefixFind) {
		if (strPrefixFind in this.#theNamespaces) {
			return this.#theNamespaces[strPrefixFind];
		}
		return null;
	}
}
