/****************************************************************************************************
 *  RDF Element Expression Preview
 *
 *	The following code relies on the following OpenRefine code:
 *    DataTableView
 *      from OpenRefine/main/webapp/modules/core/scripts/views/data-table/data-table-view.js
 *    ExpressionPreviewDialog
 *    ExpressionPreviewDialog.Widget
 *      from OpenRefine/main/webapp/modules/core/scripts/dialogs/expression-preview-dialog.js
 *
 *  Most of this code is completely independent from OpenRefine's version as it previews the
 *	RDF transform of a given element (subject, property, or object) selected in the RDF Transform
 *  editor.  However, it does harness the fundamental dialog display used by the
 *  ExpressionPreviewDialog.Widget.
 *
 ****************************************************************************************************/

/*
 *  CLASS RDFDataTableView
 *
 *	This class is completely independent from OpenRefine's DataTableView as it previews the
 *	RDF transform of a given element (subject, property, or object) selected in the RDF Transform
 *  editor.
 */
class RDFDataTableView {
	#strBaseIRI;
	#bIsResource; // Resource OR Literal
	#strTitle;

	constructor(baseIRI, bIsResource) {
		this.#strBaseIRI = baseIRI;
		this.#bIsResource = bIsResource;
		this.#strTitle =
			( bIsResource ?
				$.i18n('rdft-dialog/preview-iri-val') :
				$.i18n('rdft-dialog/preview-lit-val') );
	}

	getBaseIRI() {
		return this.#strBaseIRI;
	}

	getTitle() {
		return this.#strTitle;
	}

	isResource() {
		return this.#bIsResource;
	}

	preview(objColumn, strExpression, bIsIndex, onDone) {
		// Use OpenRefine's DataTableView.sampleVisibleRows() to preview the working RDFTransform on a sample
		// of the parent data...
		//	 FROM: OpenRefine/main/webapp/modules/core/scripts/views/data-table/data-table-view.js
		//   NOTE: On objColumn == null, just return the rows (no column information)
		const rows = DataTableView.sampleVisibleRows(objColumn);
		var strColumnName = ""; // ...for row index processing (missing column information)
		if (objColumn !== null) {
			strColumnName = objColumn.columnName
		}

		const dlgRDFExpPreview = new RDFExpressionPreviewDialog(this, onDone);
		dlgRDFExpPreview.preview(strColumnName, rows, strExpression, bIsIndex);
	}
}

/****************************************************************************************************
 * Hijack OpenRefine's ExpressionPreviewDialog and ExpressionPreviewDialog.Widget
 *   FROM: OpenRefine/main/webapp/modules/core/scripts/dialogs/expression-preview-dialog.js
 ****************************************************************************************************
 *
 * NOTE: In the event OpenRefine modifies ExpressionPreviewDialog and the associated
 *       ExpressionPreviewDialog.Widget, RDFExpressionPreviewDialog and its associated
 *       WidgetDescendant may need to be modified as well!
 *
 * NOTE: $.extend() is a JQuery mechanism for applying a type of inheritance for "non-class"
 *       function implementations.  The inheritance has a peculiar result different from "class"
 *       inheritance.  For example:
 *         $.extend(Obj1.prototype, Obj2.prototype);
 *       overwrites first object's internal prototype elements with the second object's same
 *       named prototype elements.  Therefore, $.extend() actually merges, with Obj2 precedence,
 *       into Obj1.  This has consequences for something like:
 *         $.extend(NewObj.prototype, OldObj.prototype);
 *       Then, NewObj is no longer reliable as the process overwrites any added NewObj
 *       functionality with the original OldObj functionality.
 *       The reverse:
 *         $.extend(OldObj.prototype, NewObj.prototype);
 *       Results in the original OldObj functionality overwritten by the NewObj functionality
 *       which will likely interfere with older object dependent functionality.
 *       Therefore, the solution is to create a third object with proper inheritance:
 *         $.extend(ComboObj.prototype, OldObj.prototype, NewObj.prototype);
 *		 This creates a new ComboObj with proper overwrites, in order, with the OldObj and NewObj.
 *       The native "class" inheritance does this without the need for the 3rd object.
 *
 *       The optional merge recursive (or "deep copy") boolean should be used to fully merge
 *       objects:
 *         $.extend(true, ComboObj.prototype, OldObj.prototype, NewObj.prototype);
 *
 * NOTE: As ExpressionPreviewDialog DOES NOT have any meaningful prototypes (except constructor
 *       which we don't need), ExpressionPreviewDialog is not required and
 *       RDFExpressionPreviewDialog is written as an independent class.
 *
 * NOTE: OpenRefine's ExpressionPreviewDialog.Widget is extended by our RDFWidget as
 *       there are meaningful prototype functions from the parent object that should be maintained.
 *		 i.e., RDFWidget extends ExpressionPreviewDialog.Widget
 *		 i.e., RDFWidget subclassOf ExpressionPreviewDialog.Widget
 *
 * OLD CODE:
 *       // ERRONIOUS Merge by $.extend()
 *       //$.extend(true, RDFWidget.prototype, ExpressionPreviewDialog.Widget.prototype);
 *       // CORRECTED Merge by $.extend()
 *       $.extend(true, RDFCopyWidget.prototype,
 *                      ExpressionPreviewDialog.Widget.prototype),
 *                      RDFWidget.prototype);
 *
 * MOVED: Moved code to global ExpressionPreviewDialog_WidgetCopy...
 *          See the ExpressionPreviewDialog_WidgetCopy object and RDFWidget class below.
 *          Changed $.extend() to Object.create() for copy.
 *          Use class 'extends".
 *
 ****************************************************************************************************/

/*
 *  CLASS RDFExpressionPreviewDialog
 *
 *	This class is essentially a copy of OpenRefine's ExpressionPreviewDialog modified to
 *	preview the RDF transform of a given element (subject, property, or object) selected
 *	in the RDF Transform editor.
 *
 *  NOTE: No need to inherit as it replaces the ExpressionPreviewDialog object completely.
 */
class RDFExpressionPreviewDialog {
	#dtvManager;
	#frame;
	#onDone;
	#elements
	#level;
	#previewWidget;

	static #generateWidgetHTMLforGREL() {
		//
		// As per OpenRefine's ExpressionPreviewDialog.generateWidgetHTML() with our modifications....
		//

		// Load OpenRefine's Expression Preview Dialog...
		var html = DOM.loadHTML("core", "scripts/dialogs/expression-preview-dialog.html");

		// ...and set it for the current default expression language...
		var languageOptions = [];
		var info = theProject.scripting[RDFTransform.gstrDefaultExpLang];
		languageOptions.push(
			'<option value="' + RDFTransform.gstrDefaultExpLang + '">' +
			info.name +
			'</option>'
		);

		return html.replace("$LANGUAGE_OPTIONS$", languageOptions.join(""));
	}

	constructor(dtvManager, onDone)
	{
		this.#dtvManager = dtvManager;
		//
		// As per OpenRefine's ExpressionPreviewDialog...
		//
		this.#onDone = onDone;

		this.#frame = DialogSystem.createDialog();

		var header = $('<div></div>').addClass("dialog-header");
		var body   = $('<div></div>').addClass("dialog-body");
		var footer = $('<div></div>').addClass("dialog-footer");
		var html   = $( RDFExpressionPreviewDialog.#generateWidgetHTMLforGREL() );
		this.#elements = DOM.bind(html);

		// Substitute our button for OpenRefine's ExpressionPreviewDialog button...
		var buttonOK = $('<button></button>').addClass('button').text( $.i18n('rdft-buttons/ok') );
		buttonOK.click(
			() => {
				DialogSystem.dismissUntil(this.#level - 1);
				this.#onDone( this.#previewWidget.getExpression(true) );
			}
		);

		// Substitute our button for OpenRefine's ExpressionPreviewDialog button...
		var buttonCancel = $('<button></button>').addClass('button').text( $.i18n('rdft-buttons/cancel') );
		buttonCancel.click( () => {
				DialogSystem.dismissUntil(this.#level - 1);
			}
		);

		header.text( this.#dtvManager.getTitle() );

		html.appendTo(body);

		buttonOK.appendTo(footer);
		buttonCancel.appendTo(footer);

		footer.css(
			{	"position" : "absolute",
			 	"bottom" : "0px" }
		);

		header.appendTo(this.#frame);
		body.appendTo(this.#frame);
		footer.appendTo(this.#frame);
	}

	preview(strColumnName, rows, strExpression, bIsIndex) {
		this.#frame
		.css( { "minWidth" : "700px" } )
		.resizable()
		.position(
		//	{	my: "center center",
		//		at: "center center",
		//		of: "#parent"	}
		);

		// TODO: Fix for Language on these strings...
        this.#elements.or_dialog_preview.text( "Preview" );
        this.#elements.or_dialog_history.text( "History" );
        this.#elements.or_dialog_help.text( "Help" );

        $( "#expression-preview-tabs", this.#frame ).tabs();

		this.#level = DialogSystem.showDialog(this.#frame);

		// Substitute our widget for OpenRefine's ExpressionPreviewDialog widget...
		this.#previewWidget =
			new RDFWidget(
				this.#dtvManager.getBaseIRI(), strColumnName, rows, strExpression,
				this.#dtvManager.isResource(), bIsIndex, this.#elements
			);
		this.#previewWidget.preview();
	}
}

/*
 * Object ExpressionPreviewDialog_WidgetCopy
 *
 * Copy ExpressionPreviewDialog.Widget for local modification.
 *
 * ExpressionPreviewDialog.Widget DOES NOT have a constructor per se, so create an intermediate
 * object with a proper constructor to inherit and overwrite.
 *
 * ExpressionPreviewDialog.Widget has the following prototype functions:
 *   getExpression = function(commit)
 *   _getLanguage = function()
 *   _renderHelpTab = function()
 *   _renderHelp = function(data)
 *   _renderExpressionHistoryTab = function()
 *   _renderExpressionHistory = function(data)
 *   _renderStarredExpressionsTab = function()
 *   _renderStarredExpressions = function(data)
 *   _scheduleUpdate = function()
 *   update = function()
 *   _prepareUpdate = function(params)
 *   _renderPreview = function(expression, data)
 */
function ExpressionPreviewDialog_WidgetCopy() {};
ExpressionPreviewDialog_WidgetCopy.prototype = Object.create(ExpressionPreviewDialog.Widget.prototype);
ExpressionPreviewDialog_WidgetCopy.prototype.constructor = ExpressionPreviewDialog_WidgetCopy;

/*
 *  CLASS RDFWidget
 *
 *	This class inherits from OpenRefine's ExpressionPreviewDialog.Widget modified to
 *	preview the RDF transform of a given element (subject, property, or object) selected
 *	in the RDF Transform editor.
 */
 class RDFWidget extends ExpressionPreviewDialog_WidgetCopy {
	//
	// As per OpenRefine's ExpressionPreviewDialog.Widget with our modifications...
	//
	// --------------------------------------------------------------------------------
	// The following are the parent object variables:
	//   this.expression  MAINTAIN - used by parent functions we call
	//   this._elmts      MAINTAIN - used by parent functions we call
	//   this._cellIndex  UNUSED
	//   this._rowIndices RENAMED #rowIndices - NOT used by any parent functions we call
	//   this._values     RENAMED #rowValues  - NOT used by any parent functions we call
	//   this._results    UNUSED
	//   this._timerID    MAINTAIN - used by parent functions we call
	// --------------------------------------------------------------------------------

	//
	// Variables
	// --------------------
	//
	// NOTE: Underscore (_) variables are holdovers from the older OpenRefine class
	//      and are required for proper processing using the parent functions.
	//
	// --------------------------------------------------------------------------------

	// Public...
	expression;

	// Private...
	_elmts;
	#bIsResource; // Resource OR Literal
	#bIsIndex;
	#baseIRI;
	#columnName;
	#rowIndices;
	#rowValues;
	_timerID;
	_tabContentWidth;

	//
	// Methods
	// --------------------
	//
	// NOTE: Underscore (_) methods are holdovers from the older OpenRefine class
	//      and are required for proper processing using the parent functions.
	//
	// --------------------------------------------------------------------------------

	//
	// Method constructor(): OVERRIDE Base
	//
	constructor(strBaseIRI, strColumnName, rows, strExpression,	bIsResource, bIsIndex, elements)
	{
		super(); // ...empty constructor to get "this"

		this.#baseIRI = strBaseIRI;
		this.#columnName = strColumnName;
		this.#rowIndices = rows.rowIndices;
		this.#rowValues = rows.values;

		this.expression = strExpression;
		if (strExpression === null || strExpression.length === 0 ) {
			this.expression = RDFTransform.gstrDefaultExpCode; // ...use default expression
		}

		this.#bIsResource = bIsResource;
		this.#bIsIndex = bIsIndex;
		this._elmts = elements;

		this._timerID = null; // ...used by _scheduleUpdate()

		// NOT REQUIRED: GREL is currently the only language available for RDFTransform
		// --------------------------------------------------------------------------------
		//this._elmts.expressionPreviewLanguageSelect[0].value = language;
		//this._elmts.expressionPreviewLanguageSelect
		//		$.cookie("scripting.lang", sel.value);
		//		this.update();
		//	}
		//);

		this._elmts.expressionPreviewTextarea
		.val(this.expression)
		.keyup( () => {	this._scheduleUpdate();	} )
		.select()
		.focus();

		this._tabContentWidth = this._elmts.expressionPreviewPreviewContainer.width() + "px";

		// Skip unneeded Widget or_dialog_* elements

		// Reset history to default display value...
		$("#expression-preview-tabs-history").attr("display", "");
		// Reset help to default display value...
		$("#expression-preview-tabs-help").attr("display", "");
	}

	preview() {
		this.update();
		this._renderExpressionHistoryTab();
		this._renderHelpTab();
	}

	//
	// Method update(): OVERRIDE Base
	//
	update() {
		//
		// As per OpenRefine's ExpressionPreviewDialog.Widget.update() with our modifications...
		//
		this.expression =
			this._elmts.expressionPreviewTextarea[0].value.trim();
		var params = {
			"project"    : theProject.id,
			"expression" : this.expression,
			"rowIndices" : JSON.stringify(this.#rowIndices),
			"isIRI"      : this.#bIsResource ? "1" : "0",
			"columnName" : this.#bIsIndex ? "" : this.#columnName,
			"baseIRI"    : this.#baseIRI
		};
		//this._prepareUpdate(params); // ...empty function, not overridden

		$.get(
			// URL:
			"command/rdf-transform/preview-rdf-expression",
			// Data:
			params,
			// Success:
			(data) => {
				// Handle any data errors in the Preview Rendering...
				this._renderPreview(data);
			},
			// DataType:
			"json"
		);
	}

	//
	// Method _renderPreview(): OVERRIDES base function
	//
	_renderPreview(data) {
		const bIndices = ( data.indicies != null );
		const bResults = ( data.results != null );
		const bAbsolutes = ( this.#bIsResource && data.absolutes != null );

		//
		// Process status...
		//
		var statusElem = this._elmts.expressionPreviewParsingStatus.empty();
		var statusMessage;
		statusElem.removeClass("error");
		// If some error...
		if (data.code == "error" || data.results == null) {
			// General error...
			statusElem.addClass("error");
			statusMessage = $.i18n('rdft-data/internal-error');
			// Defined error...
			if (data.message) {
				// Parsing error...
				if (data.type == "parser") {
					statusMessage = data.message;
				}
				// Absolute IRI error...
				else if (data.type == "absolute") {
					statusMessage = "ABS: " + data.message;
				}
				// Other error...
				else if (data.type == "other") {
					statusMessage = "Other: " + data.message;
				}
			}
		}
		// Otherwise, all good...
		else {
			statusMessage = $.i18n('rdft-data/no-syntax-error');
		}
		statusElem.text(statusMessage);

		//
		// Set up data table...
		//
		this._tabContentWidth = this._elmts.expressionPreviewPreviewContainer.width() + "px";
		// Let the "expressionPreviewPreviewContainer" control the width of the table...
		//var container = this._elmts.expressionPreviewPreviewContainer.empty().width(this._tabContentWidth);
		var container = this._elmts.expressionPreviewPreviewContainer.empty();

		// Create data table...
		var table = $('<table width="100%" height="100%"></table>').appendTo(container)[0];

		// Create table column headings...
		var tr = table.insertRow(0);
		var tdValue = (this.#bIsIndex ? "Index" : "Value");
		$( tr.insertCell(0) ).addClass("expression-preview-heading").text(RDFTransform.gstrIndexTitle);
		$( tr.insertCell(1) ).addClass("expression-preview-heading").text(tdValue);
		$( tr.insertCell(2) ).addClass("expression-preview-heading").text("Expression");
		if (this.#bIsResource) { // ...for resources, add the IRI resolution column...
			tdValue = $.i18n('rdft-data/table-resolved');
			$( tr.insertCell(3) ).addClass("expression-preview-heading").text(tdValue);
		}

		//
		// Process rows (data.results) for data table...
		//
		if (bResults) {
			var tr = null;
			var tdElem = null;
			// Loop on "data.results" as that is the primary reason to process...
			//   NOTE: Since "bResults", then "data.results" have a good index length.
			for (var iIndex = 0; iIndex < data.results.length; iIndex++) {
				// Create a row...
				tr = table.insertRow(table.rows.length);

				// Row is up to 4 cells...
				// 0           | 1           | 2           | 3 (Optional)|
				// ------------+-------------+-------------+-------------|
				// Row/Rec     | Raw Row     | Expression  | Abs IRI of  |
				// Index       | Index Value |  Result     | Expression  |
				// (1 based)   | (0 based)   |             |             |
				// ------------+-------------+-------------+-------------'

				// Populate row index...
				tdValue = (iIndex + 1) + "?";
				if (bIndices) {
					tdValue = String( parseInt( data.indicies[iIndex] ) + 1 ) + ".";
				}
				tdElem = $( tr.insertCell(0) ); //.attr("width", "1%");
				tdElem.html( tdValue );

				// Populate row index or raw value for expression...
				tdValue = "";
				if (bIndices && this.#bIsIndex) {
					// Row index "column"...
					tdValue = data.indicies[iIndex];
				}
				else {
					// Row values (raw) for real column...
					tdValue = this.#rowValues[iIndex];
				}
				tdElem = $( tr.insertCell(1) ).addClass("expression-preview-value");
				tdElem.html( tdValue );

				// Populate results for expression evaluation...
				tdElem = $( tr.insertCell(2) ).addClass("expression-preview-value");
				tdValue = data.results[iIndex];
				this.#renderValue(tdElem, tdValue);

				// Populate Absolute IRI of results, if applicable...
				if (bAbsolutes) {
					var tdElem = $( tr.insertCell(3) ).addClass("expression-preview-value");
					var tdValue = data.absolutes[iIndex];
					//if (!tdValue) {
					//	tdElem.css( {"font-style": "italic"} );
					//	tdValue = "Unresolved IRI";
					//}
					//console.log(tdValue);
					//console.log( $.isPlainObject(tdValue) );
					this.#renderValue(tdElem, tdValue);
				}
			}
		}
	}

	//
	// Method renderValue()
	//
	#renderValue(tdElem, tdValue) {
		// Does a value exist?
		if (tdValue !== null && tdValue !== undefined) {
			// Is the value an error message? (value created as an object {"message":"..."})
			if ( $.isPlainObject(tdValue) ) {
				//console.log(tdValue);
				$('<span></span>')
				.addClass("expression-preview-special-value")
				.text($.i18n('rdft-data/error') + ": " + tdValue.message)
				.appendTo(tdElem);
			}
			// Otherwise, good value...
			else {
				tdElem.text(tdValue);
			}
		}
		// Otherwise, no value (that's ok, no problem)...
		else {
			$('<span>null</span>')
			.addClass("expression-preview-special-value")
			.appendTo(tdElem);
		}
	}
}
