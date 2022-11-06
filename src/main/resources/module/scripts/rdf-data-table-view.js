/*
 *  RDF Element Expression Preview
 *
 *  The following code relies on the following OpenRefine code:
 *    DataTableView
 *      from OpenRefine/main/webapp/modules/core/scripts/views/data-table/data-table-view.js
 *    ExpressionPreviewDialog
 *    ExpressionPreviewDialog.Widget
 *      from OpenRefine/main/webapp/modules/core/scripts/dialogs/expression-preview-dialog.js
 *
 *  Most of this code is completely independent from OpenRefine's version as it previews the
 *  RDF transform of a given element (subject, property, or object) selected in the RDF Transform
 *  editor.  However, it does harness the fundamental dialog display used by the
 *  ExpressionPreviewDialog.Widget.
 *
 *  Copyright 2022 Keven L. Ates
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

/*
 *  CLASS RDFDataTableView
 *
 *  This class is completely independent from OpenRefine's DataTableView as it previews the
 *  RDF transform of a given element (subject, property, or object) selected in the RDF Transform
 *  editor.
 */
class RDFDataTableView {
    #strTitle;
    #strBaseIRI;
    #bIsResource; // Resource OR Literal
    #strPrefix;

    constructor(baseIRI, bIsResource, strPrefix) {
        this.#strTitle =
            ( bIsResource ?
                $.i18n('rdft-dialog/preview-iri-val') :
                $.i18n('rdft-dialog/preview-lit-val') );
        this.#strBaseIRI = baseIRI;
        this.#bIsResource = bIsResource;
        this.#strPrefix = strPrefix;
    }

    getTitle() {
        return this.#strTitle;
    }

    getBaseIRI() {
        return this.#strBaseIRI;
    }

    isResource() {
        return this.#bIsResource;
    }

    getPrefix() {
        return this.#strPrefix;
    }

    preview(objColumn, strExpression, bIsIndex, onDone) {
        // Use OpenRefine's DataTableView.sampleVisibleRows() to preview the working RDFTransform on a sample
        // of the parent data...
        //   FROM: OpenRefine/main/webapp/modules/core/scripts/views/data-table/data-table-view.js
        //   NOTE: On objColumn === null, just return the rows (no column information)
        const iRowLimit = DataTableView.sampleVisibleRows(objColumn);
        var strColumnName = ""; // ...for row index processing (missing column information)
        if (objColumn !== null) {
            strColumnName = objColumn.columnName
        }

        const dlgRDFExpPreview = new RDFExpressionPreviewDialog(this, onDone);
        dlgRDFExpPreview.preview(strColumnName, iRowLimit, strExpression, bIsIndex);
    }
}

/****************************************************************************************************
 * Hijack OpenRefine's ExpressionPreviewDialog and ExpressionPreviewDialog.Widget
 *   FROM: OpenRefine/main/webapp/modules/core/scripts/dialogs/expression-preview-dialog.js
 ****************************************************************************************************
 *
 * NOTE: If OpenRefine modifies ExpressionPreviewDialog and the associated
 *       ExpressionPreviewDialog.Widget, then RDFExpressionPreviewDialog and its associated
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
 *       This creates a new ComboObj with proper overwrites, in order, with the OldObj and NewObj.
 *       The native "class" inheritance does this without the need for the 3rd object.
 *
 *       The optional merge recursive (or "deep copy") boolean should be used to fully merge
 *       objects:
 *         $.extend(true, ComboObj.prototype, OldObj.prototype, NewObj.prototype);
 *
 * NOTE: As ExpressionPreviewDialog DOES NOT have any meaningful prototypes (except constructor
 *       which we don't need), ExpressionPreviewDialog is not required and
 *       RDFExpressionPreviewDialog is written as an independent class.  However, OpenRefine's
 *       ExpressionPreviewDialog.Widget is required and extended by our RDFExpPreviewDialogWidget.
 *       Then, there are meaningful prototype functions from ExpressionPreviewDialog.Widget
 *       that should be maintained.
 *       i.e., RDFExpPreviewDialogWidget extends ExpressionPreviewDialog.Widget
 *       i.e., RDFExpPreviewDialogWidget subclassOf ExpressionPreviewDialog.Widget
 *
 *       The global ExpressionPreviewDialog_WidgetCopy Object manages transition to
 *       RDFExpPreviewDialogWidget.  See the ExpressionPreviewDialog_WidgetCopy object and
 *       RDFExpPreviewDialogWidget class below.
 *           Uses Object.create() for copy.
 *           Uses 'extends' class keyword.
 *
 ****************************************************************************************************/

/*
 *  CLASS RDFExpressionPreviewDialog
 *
 *  This class is essentially a copy of OpenRefine's ExpressionPreviewDialog modified to
 *  preview the RDF transform of a given element (subject, property, or object) selected
 *  in the RDF Transform editor.
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

    #iLastFrameHeight;
    #iLastFrameWidth;

    #iLastEditorHeight;
    #iLastEditorWidth;

    #bFrameNotShown;

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
        html = html.replace( "$LANGUAGE_OPTIONS$", languageOptions.join("") );

        return html;
    }

    constructor(dtvManager, onDone)
    {
        this.#dtvManager = dtvManager;
        //
        // As per OpenRefine's ExpressionPreviewDialog...
        //
        this.#onDone = onDone;

        this.#frame = DialogSystem.createDialog();
        this.#frame
            .addClass("dialog-frame")
            .addClass("rdf-transform-exp-preview-frame");

        var header = $('<div />').addClass("dialog-header");
        var body   = $('<div />').addClass("dialog-body");
        var footer = $('<div />').addClass("dialog-footer");
        var html   = $( RDFExpressionPreviewDialog.#generateWidgetHTMLforGREL() );
        this.#elements = DOM.bind(html);

        // Substitute our button for OpenRefine's ExpressionPreviewDialog button...
        var buttonOK = $('<button />').addClass('button').text( $.i18n('rdft-buttons/ok') );
        buttonOK.on("click",
            () => {
                DialogSystem.dismissUntil(this.#level - 1);
                this.#onDone( this.#previewWidget.getExpression(true) );
            }
        );

        // Substitute our button for OpenRefine's ExpressionPreviewDialog button...
        var buttonCancel = $('<button />').addClass('button').text( $.i18n('rdft-buttons/cancel') );
        buttonCancel.on("click",
            () => { DialogSystem.dismissUntil(this.#level - 1); }
        );

        header.text( this.#dtvManager.getTitle() );

        body.append(html);

        footer.append(buttonOK, buttonCancel);

        this.#frame
            .append(header, body, footer)
            .css({ "min-width" : "700px" })
            .resizable();

        this.#bFrameNotShown = true;
    }

    preview(strColumnName, iRowLimit, strExpression, bIsIndex) {
        this.#elements.or_dialog_preview.text( $.i18n('rdft-dialog/tab-preview') );
        this.#elements.or_dialog_history.text( $.i18n('rdft-dialog/tab-history') );
        this.#elements.or_dialog_help.text( $.i18n('rdft-dialog/tab-help') );

        $( "#expression-preview-tabs", this.#frame ).tabs();

        // Substitute our widget for OpenRefine's ExpressionPreviewDialog widget...
        this.#previewWidget =
            new RDFExpPreviewDialogWidget(
                this.#dtvManager.getBaseIRI(), strColumnName, iRowLimit, strExpression,
                this.#dtvManager.isResource(), this.#dtvManager.getPrefix(),
                bIsIndex, this.#elements
            );
        this.#previewWidget.preview();

        this.#level = DialogSystem.showDialog(this.#frame);

        // Hook up resize...
        this.#frame
            .on("resize",
                () => {
                    // Diff = Current - Last
                    const iDiffFrameHeight = this.#frame.height() - this.#iLastFrameHeight;
                    const iDiffFrameWidth = this.#frame.width() - this.#iLastFrameWidth;
                    // If there is a detected change...
                    if ( iDiffFrameHeight != 0 || iDiffFrameWidth != 0 )
                    {
                        // ...update the editor width...
                        this.#resizeEditor(0, iDiffFrameWidth)
                        // ...update the tabs height...
                        this.#previewWidget.resizeTabs(iDiffFrameHeight, 0);

                        this.#iLastFrameHeight = this.#frame.height();
                        this.#iLastFrameWidth = this.#frame.width();
                    }
                }
            );

        const resizeObserver =
            new ResizeObserver(
                () => {
                    // On first call...
                    if (this.#bFrameNotShown) {
                        this.#iLastEditorHeight = this.#elements.expressionPreviewTextarea.height();
                        this.#iLastEditorWidth = this.#elements.expressionPreviewTextarea.width();
                        this.#elements.expressionPreviewTextarea.css(
                            { "min-height" : "" + this.#iLastEditorHeight + "px",
                              "min-width"  : "" + this.#iLastEditorWidth +  "px" }
                        );
                        this.#previewWidget.setTabDimensions();
                        this.#bFrameNotShown = false;
                        return;
                    }
                    // Diff = Current - Last
                    const iDiffEditorHeight =
                        this.#elements.expressionPreviewTextarea.height() - this.#iLastEditorHeight;
                    const iDiffEditorWidth =
                        this.#elements.expressionPreviewTextarea.width() - this.#iLastEditorWidth;
                    // If there is a detected change...
                    if ( iDiffEditorHeight != 0 || iDiffEditorWidth != 0 )
                    {
                        // ...update the frame...
                        this.#resizeFrame(iDiffEditorHeight, iDiffEditorWidth)
                        // No need to update the tabs--they resize with frame.

                        this.#iLastEditorHeight = this.#elements.expressionPreviewTextarea.height();
                        this.#iLastEditorWidth = this.#elements.expressionPreviewTextarea.width();
                    }
                }
            );

        this.#iLastFrameHeight = this.#frame.height();
        this.#iLastFrameWidth = this.#frame.width();
        this.#frame.css(
            { "min-height" : "" + this.#iLastFrameHeight + "px",
              "min-width"  : "" + this.#iLastFrameWidth +  "px" }
        );

        this.#iLastEditorHeight = this.#elements.expressionPreviewTextarea.height();
        this.#iLastEditorWidth = this.#elements.expressionPreviewTextarea.width();
        this.#elements.expressionPreviewTextarea.css(
            { "min-height" : "" + this.#iLastEditorHeight + "px",
              "min-width"  : "" + this.#iLastEditorWidth +  "px" }
        );

        this.#previewWidget.setTabDimensions();
        resizeObserver.observe( this.#elements.expressionPreviewTextarea[0] );
    }

    //
    // Method resizeFrame(): Resize the Frame with a Editor resize...
    //
    #resizeFrame(iDiffHeight, iDiffWidth) {
        var iDiffTabH = 0;
        if (iDiffHeight != 0) {
            var iMinFrameHeight = parseInt( this.#frame.css('min-height'), 10);
            var iNewH = this.#frame.height() + iDiffHeight;
            if (iNewH < iMinFrameHeight ) {
                if (this.#frame.height() == iMinFrameHeight) {
                    iDiffTabH = -iDiffHeight;
                }
                else {
                    iDiffTabH = iMinFrameHeight - iNewH;
                }
                iNewH = iMinFrameHeight;
            }
            this.#frame.height(iNewH);
            this.#iLastFrameHeight = iNewH;
            var iBufferHeight =
                this.#elements.expressionPreviewPreviewContainer.height() -
                parseInt( this.#elements.expressionPreviewPreviewContainer.css('min-height'), 10);
            iMinFrameHeight = iNewH - iBufferHeight;
            this.#frame.css(
                { "min-height" : "" + iMinFrameHeight + "px" }
            );
        }

        var iDiffTabW = 0;
        if (iDiffWidth != 0) {
            var iMinFrameWidth = parseInt( this.#frame.css('min-width'), 10);
            var iNewW = this.#frame.width() + iDiffWidth;
            if (iNewW < iMinFrameWidth) {
                if (this.#frame.width() == iMinFrameWidth) {
                    iDiffTabW = -iDiffWidth;
                }
                else {
                    iDiffTabW = iMinFrameWidth - iNewW;
                }
                iNewW = iMinFrameWidth;
            }
            this.#frame.width(iNewW);
            this.#iLastFrameWidth = iNewW;
        }

        if (iDiffTabH != 0 || iDiffTabW != 0) {
            this.#previewWidget.resizeTabs(iDiffTabH, iDiffTabW);
        }
    }

    //
    // Method resizeEditor(): Resize the Editor with a Frame resize...
    //
    #resizeEditor(iDiffHeight, iDiffWidth) {
        if (iDiffHeight != 0) {
            const iNewH = this.#elements.expressionPreviewTextarea.height() + iDiffHeight;
            this.#elements.expressionPreviewTextarea.height(iNewH);
            this.#iLastEditorHeight = this.#elements.expressionPreviewTextarea.height();
        }

        if (iDiffWidth != 0) {
            const iNewW = this.#elements.expressionPreviewTextarea.width() + iDiffWidth;
            this.#elements.expressionPreviewTextarea.width(iNewW);
            this.#iLastEditorWidth = this.#elements.expressionPreviewTextarea.width();
        }
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
function ExpressionPreviewDialog_WidgetCopy() {}
ExpressionPreviewDialog_WidgetCopy.prototype = Object.create(ExpressionPreviewDialog.Widget.prototype);
ExpressionPreviewDialog_WidgetCopy.prototype.constructor = ExpressionPreviewDialog_WidgetCopy;

/*
 *  CLASS RDFExpPreviewDialogWidget
 *
 *  This class inherits from OpenRefine's ExpressionPreviewDialog.Widget modified to
 *  preview the RDF transform of a given element (subject, property, or object) selected
 *  in the RDF Transform editor.
 */
 class RDFExpPreviewDialogWidget extends ExpressionPreviewDialog_WidgetCopy {
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

    //
    // Maintained OLD Variables:
    //
    // Public...
    expression;
    // Private...
    _elmts;
    _timerID;
    //_tabContentWidth;

    //
    // New or Mod-able Dialog Variables:
    //
    // Private...
    #bIsResource; // Resource OR Literal
    #strPrefix;
    #bIsIndex;
    #strBaseIRI;
    #strColumnName;
    #aiRowIndices;
    #astrRowValues;

    #iLastTabHeight;
    #iLastTabWidth;
    #iMinTabHeight;
    #iMinTabWidth;

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
    constructor(strBaseIRI, strColumnName, rows, strExpression, bIsResource,
        strPrefix, bIsIndex, elements)
    {
        super(); // ...parent constructor to get "this"

        this.#strBaseIRI = strBaseIRI;
        this.#strColumnName = strColumnName;
        this.#aiRowIndices = rows.rowIndices;
        this.#astrRowValues = rows.values;

        this.expression = strExpression;
        if (strExpression === null || strExpression.length === 0 ) {
            this.expression = RDFTransform.gstrDefaultExpCode; // ...use default expression
        }

        this.#bIsResource = bIsResource;
        this.#strPrefix = strPrefix;
        this.#bIsIndex = bIsIndex;
        this._elmts = elements;

        this._timerID = null; // ...used by _scheduleUpdate()

        // NOT REQUIRED: GREL is currently the only language available for RDFTransform
        // --------------------------------------------------------------------------------
        //this._elmts.expressionPreviewLanguageSelect[0].value = language;
        //this._elmts.expressionPreviewLanguageSelect
        //      $.cookie("scripting.lang", sel.value);
        //      this.update();
        //  }
        //);

        this._elmts.expressionPreviewTextarea
            .val(this.expression)
            .keyup( () => { this._scheduleUpdate(); } )
            .select()
            .focus();

        //this._tabContentWidth = this._elmts.expressionPreviewPreviewContainer.width() + "px";

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

    setTabDimensions() {
        this.#iMinTabHeight = this._elmts.expressionPreviewPreviewContainer.height();
        this.#iMinTabWidth = this._elmts.expressionPreviewPreviewContainer.width();

        const strHeight = "" + this.#iMinTabHeight + "px";
        const objMinHeight = { "min-height" : strHeight };
        this._elmts.expressionPreviewPreviewContainer.css(objMinHeight);
        this._elmts.expressionPreviewHistoryContainer.css(objMinHeight);
        this._elmts.expressionPreviewHelpTabBody.css(objMinHeight);

        const strWidth = "" + this.#iMinTabWidth + "px";
        const objMinWidth = { "min-width" : strWidth };
        this._elmts.expressionPreviewPreviewContainer.css(objMinWidth);
        this._elmts.expressionPreviewHistoryContainer.css(objMinWidth);
        this._elmts.expressionPreviewHelpTabBody.css(objMinWidth);

        this.#iLastTabHeight = this.#iMinTabHeight;
        this.#iLastTabWidth = this.#iMinTabWidth;
    }

    //
    // Method resize(): Resize the Tabs with a Frame resize...
    //
    resizeTabs(iDiffHeight, iDiffWidth) {
        if (iDiffHeight != 0) {
            var iNewH = this.#iLastTabHeight + iDiffHeight;
            if (iNewH < this.#iMinTabHeight) {
                iNewH = this.#iMinTabHeight;
            }
            this._elmts.expressionPreviewPreviewContainer.height(iNewH);
            this._elmts.expressionPreviewHistoryContainer.height(iNewH);
            this._elmts.expressionPreviewHelpTabBody.height(iNewH);
            this.#iLastTabHeight = iNewH;
        }

        if (iDiffWidth != 0) {
            var iNewW = this.#iLastTabWidth + iDiffWidth;
            if (iNewW < this.#iMinTabWidth) {
                iNewW = this.#iMinTabWidth;
            }
            this._elmts.expressionPreviewPreviewContainer.width(iNewW);
            this._elmts.expressionPreviewHistoryContainer.width(iNewW);
            this._elmts.expressionPreviewHelpTabBody.width(iNewW);
            this.#iLastTabWidth = iNewW;
        }
    }

    //
    // Method update(): OVERRIDE Base
    //
    update() {
        //
        // As per OpenRefine's ExpressionPreviewDialog.Widget.update() with our modifications...
        //
        this.expression = this._elmts.expressionPreviewTextarea[0].value.trim();
        var params = {
            "project"    : theProject.id,
            "expression" : this.expression,
            "rowIndices" : JSON.stringify(this.#aiRowIndices),
            "isIRI"      : this.#bIsResource ? "1" : "0",
            "prefix"     : this.#strPrefix === null ? "" : this.#strPrefix,
            "columnName" : this.#bIsIndex ? "" : this.#strColumnName,
            "baseIRI"    : this.#strBaseIRI
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
        if (data.code === "error" || data.results == null) {
            // General error...
            statusElem.addClass("error");
            statusMessage = $.i18n('rdft-data/internal-error');
            // Defined error...
            if (data.message) {
                // Parsing error...
                if (data.type === "parser") {
                    statusMessage = data.message;
                }
                // Absolute IRI error...
                else if (data.type === "absolute") {
                    statusMessage = "ABS: " + data.message;
                }
                // Other error...
                else if (data.type === "other") {
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
        //this._tabContentWidth = this._elmts.expressionPreviewPreviewContainer.width() + "px";
        // Let the "expressionPreviewPreviewContainer" control the width of the table...
        //var container = this._elmts.expressionPreviewPreviewContainer.empty().width(this._tabContentWidth);
        this._elmts.expressionPreviewPreviewContainer.empty();

        // Create data table...
        /** @type {HTMLTableElement} */
        // @ts-ignore
        var table = $('<table width="100%" height="100%"></table>');
        this._elmts.expressionPreviewPreviewContainer.append(table);
        var tableBody = table[0];

        // Create table column headings...
        var tr = tableBody.insertRow(0);
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
            /** @type {HTMLTableRowElement} */
            tr = null;
            var tdElem = null;
            // Loop on "data.results" as that is the primary reason to process...
            //   NOTE: Since "bResults", then "data.results" have a good index length.
            for (var iIndex = 0; iIndex < data.results.length; iIndex++) {
                // Create a row...
                tr = tableBody.insertRow(tableBody.rows.length);

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
                tdElem = $( tr.insertCell(0) ); //.width("1%");
                tdElem.html( tdValue );

                // Populate row index or raw value for expression...
                tdValue = "";
                if (bIndices && this.#bIsIndex) {
                    // Row index "column"...
                    tdValue = data.indicies[iIndex];
                }
                else {
                    // Row values (raw) for real column...
                    tdValue = this.#astrRowValues[iIndex];
                }
                tdElem = $( tr.insertCell(1) ).addClass("expression-preview-value");
                tdElem.html( tdValue );

                // Populate results for expression evaluation...
                tdElem = $( tr.insertCell(2) ).addClass("expression-preview-value");
                tdValue = data.results[iIndex];
                this.#renderValue(tdElem, tdValue);

                // Populate Absolute IRI of results, if applicable...
                if (bAbsolutes) {
                    tdElem = $( tr.insertCell(3) ).addClass("expression-preview-value");
                    tdValue = data.absolutes[iIndex];
                    //if (!tdValue) {
                    //  tdElem.css( {"font-style": "italic"} );
                    //  tdValue = "Unresolved IRI";
                    //}
                    /* DEBUG
                    console.log(tdValue);
                    console.log( $.isPlainObject(tdValue) );
                    */
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
        if (tdValue != null) {
            // Is the value an error message? (value created as an object {"message":"..."})
            if ( $.isPlainObject(tdValue) ) {
                /* DEBUG
                console.log(tdValue);
                */
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
