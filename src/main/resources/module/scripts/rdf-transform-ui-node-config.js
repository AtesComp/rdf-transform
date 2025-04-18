/*
 *  Class RDFTransformUINodeConfig
 *
 *  The Configuration UI for the Node Manager UI.
 *
 *  Copyright 2025 Keven L. Ates
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

class RDFTransformUINodeConfig {
    #strRadioIndex = "rdf-radio-row-index";
    #strRadioColumn = "rdf-radio-column";
    #strRadioConstant = "rdf-constant-value-radio";
    #strInputConstant = "rdf-constant-value-input";

    /** @type RDFTransformDialog */
    #dialog;
    #nodeUI;
    #eType;

    #node;
    #bIsRoot;
    #bIsVarNodeConfig;

    #elements;
    #rdf_cell_expr;
    #rdf_cell;
    #rdf_cell_orig;
    #rdf_cell_expr_orig;

    #level;

    #checkedTrue;
    #disabledTrue;
    #disabledFalse;

    #iLastFrameHeight;
    #iLastFrameWidth;

    #iLastColLeftHeight;
    #iLastColLeftWidth;
    #iMinColLeftHeight;
    #iMinColLeftWidth;

    constructor(theDialog, theNodeUI, theEType) {
        this.#dialog = theDialog
        this.#nodeUI = theNodeUI;
        this.#eType = theEType;

        this.#node = this.#nodeUI.getNode();
        this.#bIsRoot = this.#nodeUI.isRootNode();
        this.#bIsVarNodeConfig = this.#nodeUI.isVariableNode();

        this.#checkedTrue = { "checked" : true };
        this.#disabledTrue = { "disabled" : true };
        this.#disabledFalse = { "disabled" : false };
    }

    processView() {
        if (theProject.columnModel.columns.length < 1) {
            return;
        }

        // Create this RDF Transform Node Configuration Dialog's main parts...
        var frame = DialogSystem.createDialog();

        // @ts-ignore
        var header = $('<div />').addClass("dialog-header");
        // @ts-ignore
        var body = $('<div />')
            .addClass("grid-layout")
            .addClass("layout-full")
            .addClass("dialog-body")
            .addClass("rdf-transform");
        // @ts-ignore
        var footer = $('<div />').addClass("dialog-footer");

        // Load RDF Transform's Node Configuration Table...
        var tableNodeConfig =
            // @ts-ignore
            $(DOM.loadHTML(RDFTransform.KEY, "scripts/dialogs/rdf-transform-node-config.html"))
                .filter('.rdf-transform-node-config-table');

        this.#elements = DOM.bind(tableNodeConfig);
        // @ts-ignore
        this.#elements.prefixSelect.text( $.i18n('rdft-prefix/prefix') + ": " );

        // @ts-ignore
        this.#elements.useContent.text(     $.i18n('rdft-dialog/use-content') + '...'  );
        // @ts-ignore
        this.#elements.contentUsed.text(    $.i18n('rdft-dialog/content-used') + '...' );

        // @ts-ignore
        this.#elements.asIRI.text(      $.i18n('rdft-as/iri')        );
        // @ts-ignore
        this.#elements.asText.text(     $.i18n('rdft-as/text')       );
        // @ts-ignore
        this.#elements.asLang.text(     $.i18n('rdft-as/lang') + ":" );
        // @ts-ignore
        this.#elements.asInt.text(      $.i18n('rdft-as/int')        );
        // @ts-ignore
        this.#elements.asDouble.text(   $.i18n('rdft-as/double')     );
        // @ts-ignore
        this.#elements.asDate.text(     $.i18n('rdft-as/date')       );
        // @ts-ignore
        this.#elements.asDateTime.text( $.i18n('rdft-as/datetime')   );
        // @ts-ignore
        this.#elements.asBool.text(     $.i18n('rdft-as/bool')       );
        // @ts-ignore
        this.#elements.asCustom.text(   $.i18n('rdft-as/custom')     );
        // @ts-ignore
        this.#elements.asBlank.text(    $.i18n('rdft-as/blank')      );

        // @ts-ignore
        this.#elements.useExpression.text(  $.i18n('rdft-dialog/use-exp') + '...'      );
        // @ts-ignore
        this.#elements.expEditPreview.text( $.i18n('rdft-dialog/edit-preview')         );

        // Table Columns Left and Right:
        // ----------------------------------------------------------------------
        //  The Left Table Column is constructed dynamically from the
        //      1. Row/Record,
        //      2. data columns, and
        //      3. constant controls
        //  The Right Table Column is constructed statically from the given HTML

        //  Create the Left Table Column...
        // @ts-ignore
        var tableColumnLeft = $('<table></table>')[0];
        this.#elements.columnLeft
            .addClass("rdf-transform-column-scroll")
            .append(tableColumnLeft);

        // Add Row/Record Radio Row (NOTE: Always ResourceNode)...
        this.#buildIndexChoice(tableColumnLeft);

        // Add Column Name Radio Rows (NOTE: A ResourceNode OR A LiteralNode)...
        var columns = theProject.columnModel.columns;
        if (columns.length === 1) {
            // Process first and last column...
            // NOTE: Pad top and bottom for SINGLE column
            this.#buildColumnChoice(tableColumnLeft, columns[0], 2);
        }
        else {
            // Process first column (pad top of top row)...
            this.#buildColumnChoice(tableColumnLeft, columns[0], 1);
            // Loop through all but first and last column...
            const iLoopLast = columns.length - 1;
            for (var iColumn = 1; iColumn < iLoopLast; iColumn++) {
                // Process column (no padding for middle rows)...
                this.#buildColumnChoice(tableColumnLeft, columns[iColumn]);
            }
            // Process last column (pad bottom of bottom row)...
            this.#buildColumnChoice(tableColumnLeft, columns[iLoopLast], -1);
        }

        // Add Constant Value Radio Row (NOTE: A ResourceNode OR A LiteralNode)...
        this.#buildConstantChoice(tableColumnLeft);

        // Initilize inputs...
        this.#initInputs();

        // Create the "OK" button...
        var buttonOK =
            // @ts-ignore
            $('<button />')
            .addClass('button')
            // @ts-ignore
            .html( $.i18n('rdft-buttons/ok') )
            .on("click",
                () => {
                    var node = this.#getResultJSON();
                    if (node !== null) {
                        DialogSystem.dismissUntil(this.#level - 1);
                        this.#nodeUI.updateNode(node);
                    }
                }
            );

        // Create the "Cancel" button...
        var buttonCancel =
            // @ts-ignore
            $('<button />')
            .addClass('button')
            // @ts-ignore
            .text( $.i18n('rdft-buttons/cancel') )
            .on("click",
                () => {
                    DialogSystem.dismissUntil(this.#level - 1);
                }
            );

        // Assemble the dialog...
        // @ts-ignore
        header.text( $.i18n('rdft-dialog/rdf-node') );
        body.append(tableNodeConfig);
        footer.append(buttonOK, buttonCancel);
        frame
            .append(header, body, footer)
            .css(
                { "min-height" : "400px",
                  "min-width"  : "500px"
                }
            )
            .resizable();

        this.#level = DialogSystem.showDialog(frame);

        //
        //   AFTER show...
        //

        // Hook up resize...
        frame
            .on("resize",
                () => {
                    // Diff = Current - Last
                    const iDiffFrameHeight = frame.height() - this.#iLastFrameHeight;
                    const iDiffFrameWidth = frame.width() - this.#iLastFrameWidth;
                    // If there is a detected change...
                    if ( iDiffFrameHeight != 0 || iDiffFrameWidth != 0 )
                    {
                        // ...update the Column Left height and width...
                        this.#resizeColumnLeft(iDiffFrameHeight, iDiffFrameWidth);

                        this.#iLastFrameHeight = frame.height();
                        this.#iLastFrameWidth = frame.width();
                    }
                }
            );

        // Force Frame and ColumnLeft to initial minimal sizes...
        this.#iLastFrameHeight = frame.height();
        this.#iLastFrameWidth  = frame.width();
        this.#iLastColLeftHeight = this.#elements.columnLeft.height();
        this.#iLastColLeftWidth  = this.#elements.columnLeft.width();
        var iDiffHeight = 400 - this.#iLastFrameHeight;
        var iDiffWidth  = 500 - this.#iLastFrameWidth;
        this.#iMinColLeftHeight = this.#iLastColLeftHeight + iDiffHeight;
        this.#iMinColLeftWidth  = this.#iLastColLeftWidth  + iDiffWidth;
        frame.height(400);
        frame.width(500);
        this.#elements.columnLeft.height(this.#iMinColLeftHeight);
        this.#elements.columnLeft.width(this.#iMinColLeftWidth);
        this.#iLastFrameHeight = 400;
        this.#iLastFrameWidth  = 500;
        this.#iLastColLeftHeight = this.#iMinColLeftHeight;
        this.#iLastColLeftWidth  = this.#iMinColLeftWidth;
    }

    //
    // Method resizeColumnLeft(): Resize the ColumnLeft with a Frame resize...
    //
    #resizeColumnLeft(iDiffHeight, iDiffWidth) {
        if (iDiffHeight != 0) {
            var iNewH = this.#iLastColLeftHeight + iDiffHeight;
            if (iNewH < this.#iMinColLeftHeight) {
                iNewH = this.#iMinColLeftHeight;
            }
            this.#elements.columnLeft.height(iNewH);
            this.#iLastColLeftHeight = iNewH;
        }

        if (iDiffWidth != 0) {
            var iNewW = this.#iLastColLeftWidth + iDiffWidth;
            if (iNewW < this.#iMinColLeftWidth) {
                iNewW = this.#iMinColLeftWidth;
            }
            this.#elements.columnLeft.width(iNewW);
            this.#iLastColLeftWidth = iNewW;
        }
    }


    #buildIndexChoice(tableColumn) {
        // Prepare NEW Radio Control Row...
        var tr = tableColumn.insertRow();

        var td;

        // Radio Control...
        td = tr.insertCell();
        // @ts-ignore
        $(td).addClass('rdf-transform-node-bottom-separated');
        var tdRadio =
            // @ts-ignore
            $('<input />')
            .attr("type", "radio").val("") // ...== not a Column Node, an Index
            .attr("name", "rdf-column-radio")
            .attr("id", this.#strRadioIndex)
            .on("click",
                () => {
                    // @ts-ignore
                    $("#rdf-constant-value-input").prop(this.#disabledTrue);
                    // If the recorded cell type is NOT the same as the current...
                    if (this.#rdf_cell !== this.#strRadioIndex) {
                        // ...change the cell type to the current...
                        this.#rdf_cell = this.#strRadioIndex;
                        // If the cell type is the same as the original...
                        if (this.#rdf_cell === this.#rdf_cell_orig) {
                            // ...change the expression back to the original expression...
                            this.#rdf_cell_expr = this.#rdf_cell_expr_orig;
                        }
                        // Otherwise...
                        else {
                            // ...change the expression to the default for this type...
                            this.#rdf_cell_expr = RDFTransform.gstrExpressionIndex; // row or record
                        }
                        // ...change the expression displayed to the current expression...
                        this.#elements.rdf_cell_expr
                        .empty()
                        .text( RDFTransformCommon.shortenExpression(this.#rdf_cell_expr) );
                    }
                }
            );
        if (this.#bIsVarNodeConfig) {
            tdRadio.prop(this.#checkedTrue);
            this.#rdf_cell = this.#strRadioIndex;
            this.#rdf_cell_orig = this.#strRadioIndex;
        }
        // @ts-ignore
        $(td).append(tdRadio);

        // Label for Radio...
        td = tr.insertCell();
        // @ts-ignore
        $(td).addClass('rdf-transform-node-bottom-separated');
        // @ts-ignore
        var label = $('<label />')
            .attr("for", this.#strRadioIndex)
            // @ts-ignore
            .text('[' + $.i18n('rdft-dialog/index') + ']')
            .attr("bind", "asIndex");
        this.#elements.asIndex = label;
        // @ts-ignore
        $(td).append(label);
    }

    #buildColumnChoice(tableColumn, column, iPad = 0) {
        // Prepare NEW Radio Control Row...
        var tr = tableColumn.insertRow();

        var td;
        var strID = this.#strRadioColumn + column.cellIndex;

        // Radio Control...
        td = tr.insertCell();
        if (iPad > 0) { // ...First Row Padding for Separator
            // @ts-ignore
            $(td).addClass('rdf-transform-node-top-padded');
        }
        if (iPad < 0 || iPad > 1) { // ...Last Row Padding for Separator
            // @ts-ignore
            $(td).addClass('rdf-transform-node-bottom-padded');
        }
        var tdRadio =
            // @ts-ignore
            $('<input />')
            .attr("type", "radio").val(column.name) // ...== a Column Node
            .attr("name", "rdf-column-radio")
            .attr("id", strID)
            .on("click",
                () => {
                    // @ts-ignore
                    $("#rdf-constant-value-input").prop(this.#disabledTrue);

                    // If the recorded cell+column type is NOT the same as the current...
                    if (this.#rdf_cell != this.#strRadioColumn) {
                        // Determine if the recorded cell type is the same as the current...
                        var bSameOldType = ( this.#rdf_cell.indexOf(this.#strRadioColumn) === 0 );
                        // ...change the cell+column type to the current...
                        this.#rdf_cell = this.#strRadioColumn;
                        // Determine if the current cell type is the same as the original...
                        var bSameOrigType = ( this.#rdf_cell_orig.indexOf(this.#strRadioColumn) === 0 );

                        // If the recorded cell type was NOT the same as the current...
                        if ( ! bSameOldType ) {
                            // If the current cell type is the same as the original...
                            if (bSameOrigType) {
                                // ...change the expression back to the original expression...
                                this.#rdf_cell_expr = this.#rdf_cell_expr_orig;
                            }
                            // Otherwise...
                            else {
                                // ...change the expression to the default for this type...
                                this.#rdf_cell_expr = RDFTransform.gstrDefaultExpCode;
                            }
                            // ...change the expression displayed to the current expression...
                            this.#elements.rdf_cell_expr
                            .empty()
                            .text( RDFTransformCommon.shortenExpression(this.#rdf_cell_expr) );
                        }
                        // Otherwise, the recorded cell type is the same as the current...
                        //  ...so keep the recorded column expression (try to use it)...
                    }
                }
            );
        if ("columnName" in this.#node.valueSource && column.name === this.#node.valueSource.columnName) {
            tdRadio.prop(this.#checkedTrue);
            this.#rdf_cell = this.#strRadioColumn;
            this.#rdf_cell_orig = this.#strRadioColumn;
        }
        // @ts-ignore
        $(td).append(tdRadio);

        // Label for Radio...
        td = tr.insertCell();
        if (iPad > 0) { // ...First Row Padding for Separator
            // @ts-ignore
            $(td).addClass('rdf-transform-node-top-padded');
        }
        if (iPad < 0 || iPad > 1) { // ...Last Row Padding for Separator
            // @ts-ignore
            $(td).addClass('rdf-transform-node-bottom-padded');
        }
        var label =
            // @ts-ignore
            $('<label />')
            .attr("for", strID)
            .text(column.name)
            .attr("bind", "asColumn" + column.cellIndex);
        this.#elements["asColumn" + column.cellIndex] = label;
        // @ts-ignore
        $(td).append(label);
    }

    #buildConstantChoice(tableColumn) {
        // Prepare NEW Radio Control Row...
        var tr = tableColumn.insertRow();

        var td;

        // Radio Control...
        td = tr.insertCell();
        // @ts-ignore
        $(td).addClass('rdf-transform-node-top-separated');
        var tdRadio =
            // @ts-ignore
            $('<input />')
            .attr("type", "radio").val("")  // ...== not a Column Node, a Constant
            .attr("name", "rdf-column-radio")
            .attr("id", this.#strRadioConstant)
            .on("click",
                () => {
                    // @ts-ignore
                    $("#rdf-constant-value-input").prop(this.#disabledFalse);
                    // If the recorded cell type is NOT the same as the current...
                    if (this.#rdf_cell !== this.#strRadioConstant) {
                        // ...change the cell type to the current...
                        this.#rdf_cell = this.#strRadioConstant;
                        // If the cell type is the same as the original...
                        if (this.#rdf_cell === this.#rdf_cell_orig) {
                            // ...change the expression back to the original expression...
                            this.#rdf_cell_expr = this.#rdf_cell_expr_orig;
                        }
                        // Otherwise...
                        else {
                            // ...change the expression to the default for this type...
                            this.#rdf_cell_expr = RDFTransform.gstrDefaultExpCode;
                        }
                        // ...change the expression displayed to the current expression...
                        this.#elements.rdf_cell_expr
                        .empty()
                        .text( RDFTransformCommon.shortenExpression(this.#rdf_cell_expr) );
                    }
                }
            );
        if (! this.#bIsVarNodeConfig) {
            tdRadio.prop(this.#checkedTrue);
            this.#rdf_cell = this.#strRadioConstant;
            this.#rdf_cell_orig = this.#strRadioConstant;
        }
        // @ts-ignore
        $(td).append(tdRadio);

        // Label for Radio...
        td = tr.insertCell();
        // @ts-ignore
        $(td).addClass('rdf-transform-node-top-separated');
        var label =
            // @ts-ignore
            $('<label />')
            .attr("for", this.#strRadioConstant)
            // @ts-ignore
            .text( $.i18n('rdft-dialog/constant-val') )
            .attr("bind", "asConstant");
        this.#elements.asConstant = label;
        // @ts-ignore
        $(td).append(label);

        // Prepare NEW Text Control Row for this Radio Control...
        tr = tableColumn.insertRow();

        // Text Control for Radio...
        tr.insertCell(); // ...spacer
        td = tr.insertCell(); // ...align Textbox with Label
        // @ts-ignore
        $(td).attr("colspan", "2");
        const strConstVal =
            ( (RDFTransform.gstrConstant in this.#node.valueSource &&
               this.#node.valueSource.constant !== null) ? this.#node.valueSource.constant : '');
        // @ts-ignore
        var tdInput = $('<input />')
            .attr("id", this.#strInputConstant)
            .attr("type", "text").val(strConstVal)
            .attr("size", "25");
        if (this.#bIsVarNodeConfig) {
            tdInput.prop(this.#disabledTrue);
        }
        // @ts-ignore
        $(td).append(tdInput);
    }

    #initInputs() {
        //
        // Set Prefix Selections...
        //
        const strOption = '<option value="{V}">{T}</option>';
        this.#elements.rdf_prefix_select
            .append(
                strOption
                    .replace(/{V}/, '')
                    // @ts-ignore
                    .replace(/{T}/, $.i18n('rdft-dialog/choose-none') ) );
        this.#elements.rdf_prefix_select
            .append(
                strOption
                    .replace(/{V}/, ':')
                    // @ts-ignore
                    .replace(/{T}/, ': (' + $.i18n('rdft-dialog/base-iri') + ')') );
        const theNamespaces = this.#dialog.getNamespacesManager().getNamespaces();
        if (theNamespaces != null) {
            for (const strPrefix in theNamespaces) {
                // const theNamespace = theNamespaces[strPrefix]);
                this.#elements.rdf_prefix_select
                    .append( strOption.replace(/{V}/, strPrefix + ':').replace(/{T}/, strPrefix) );
            }
        }
        // Set the prefix selection if it matches an existing prefix...
        this.#elements.rdf_prefix_select.find("option:selected").prop("selected", false);
        this.#elements.rdf_prefix_select.find("option:first").prop("selected", "selected"); // ...default: select "Choose / None"
        if (RDFTransform.gstrPrefix in this.#node && this.#node.prefix !== null) {
            const strPrefix = this.#node.prefix + ':';
            const selOptions = this.#elements.rdf_prefix_select.find("option");
            const iLen = selOptions.length;
            for (var iIndex = 0; iIndex < iLen; iIndex++) {
                if (selOptions[iIndex].value === strPrefix) {
                    this.#elements.rdf_prefix_select.prop('selectedIndex', iIndex);
                    break;
                }
            }
        }

        // Disable Language and Custom Data Type inputs...
        this.#elements.rdf_content_lang_input
            .add(this.#elements.rdf_content_dtype_input)
            .prop(this.#disabledTrue);

        //
        // Set initial values and property settings...
        //
        if ( this.#eType === RDFTransformCommon.NodeType.Resource ) {
            // Enable Prefix Select...
            this.#elements.rdf_prefix.find("*").prop(this.#disabledFalse);
            this.#elements.rdf_prefix.find("*").removeClass("view-disabled");

            //
            // Resource node...
            //
            this.#elements.rdf_content_iri_radio.prop(this.#checkedTrue);
        }
        else if ( this.#eType === RDFTransformCommon.NodeType.Literal ) {
            // Disable Prefix Select...
            this.#elements.rdf_prefix.find("*").prop(this.#disabledTrue);
            this.#elements.rdf_prefix.find("*").addClass("view-disabled");

            //
            // Literal node...
            //
            if ("datatype" in this.#node.valueType) { // ...with Datatype tag... http://www.w3.org/2001/XMLSchema#
                var strType = null;
                if (RDFTransform.gstrConstant in this.#node.valueType.datatype.valueSource) {
                    strType = this.#node.valueType.datatype.valueSource.constant;
                }
                // Standard Datatype tags...
                if (strType === 'int') {
                    this.#elements.rdf_content_int_radio.prop(this.#checkedTrue);
                }
                else if (strType === 'double') {
                    this.#elements.rdf_content_double_radio.prop(this.#checkedTrue);
                }
                else if (strType === 'date') {
                    this.#elements.rdf_content_date_radio.prop(this.#checkedTrue);
                }
                else if (strType === 'dateTime') {
                    this.#elements.rdf_content_date_time_radio.prop(this.#checkedTrue);
                }
                else if (strType === 'boolean') {
                    this.#elements.rdf_content_boolean_radio.prop(this.#checkedTrue);
                }
                else {
                    // Custom Datatype tag...
                    this.#elements.rdf_content_dtype_radio.prop(this.#checkedTrue);
                    this.#elements.rdf_content_dtype_input.prop(this.#disabledFalse).val(strType);
                }
            }
            else if ("language" in this.#node.valueType) { // ...with Language tag...
                this.#elements.rdf_content_lang_radio.prop(this.#checkedTrue);
                this.#elements.rdf_content_lang_input.prop(this.#disabledFalse).val(this.#node.valueType.language);
            }
            else { // No tag, simple string...
                this.#elements.rdf_content_txt_radio.prop(this.#checkedTrue);
            }
        }
        else if ( this.#eType === RDFTransformCommon.NodeType.Blank ) {
            // Disable Prefix Select...
            this.#elements.rdf_prefix.find("*").prop(this.#disabledTrue);
            this.#elements.rdf_prefix.find("*").addClass("view-disabled");

            //
            // Blank node...
            //
            this.#elements.rdf_content_blank_radio.prop(this.#checkedTrue);
        }

        // Set cell expression...
        // TODO: Future code language.  It's all "grel" currently.
        var strExpCode = RDFTransform.gstrDefaultExpCode; // ...default expression
        if (RDFTransform.gstrExpression in this.#node && "code" in this.#node.expression ) {
            strExpCode = this.#node.expression.code;
        }
        this.#rdf_cell_expr_orig = strExpCode;
        this.#rdf_cell_expr = strExpCode;
        this.#elements.rdf_cell_expr.empty().text( RDFTransformCommon.shortenExpression(strExpCode) );

        //
        // Click Events...
        //

        // Prefix...
        this.#elements.rdf_prefix_select
        .on("change",
            () => {
                // NOP
            }
        );

        // Resource Content radio...
        this.#elements.rdf_content_iri_radio
        .on("click",
            () => {
                this.#elements.rdf_prefix.find("*").prop(this.#disabledFalse);
                this.#elements.rdf_prefix.find("*").removeClass("view-disabled");
                this.#elements.rdf_content_lang_input
                .add(this.#elements.rdf_content_dtype_input)
                .prop(this.#disabledTrue);
            }
        );

        // All Literal Content radios and Blank Content radio...
        this.#elements.rdf_content_txt_radio
        .add(this.#elements.rdf_content_int_radio)
        .add(this.#elements.rdf_content_double_radio)
        .add(this.#elements.rdf_content_date_radio)
        .add(this.#elements.rdf_content_date_time_radio)
        .add(this.#elements.rdf_content_boolean_radio)
        .add(this.#elements.rdf_content_blank_radio)
        .on("click",
            () => {
                this.#elements.rdf_prefix.find("*").prop(this.#disabledTrue);
                this.#elements.rdf_prefix.find("*").addClass("view-disabled");
                this.#elements.rdf_content_lang_input
                .add(this.#elements.rdf_content_tdype_input)
                .prop(this.#disabledTrue);
            }
        );

        // Content radio Language...
        this.#elements.rdf_content_lang_radio
        .on("click",
            () => {
                this.#elements.rdf_prefix.find("*").prop(this.#disabledTrue);
                this.#elements.rdf_prefix.find("*").addClass("view-disabled");
                this.#elements.rdf_content_lang_input.prop(this.#disabledFalse);
                this.#elements.rdf_content_dtype_input.prop(this.#disabledTrue);
            }
        );

        // Content radio Custom Data Type...
        this.#elements.rdf_content_dtype_radio
        .on("click",
            () => {
                this.#elements.rdf_prefix.find("*").prop(this.#disabledTrue);
                this.#elements.rdf_prefix.find("*").addClass("view-disabled");
                this.#elements.rdf_content_lang_input.prop(this.#disabledTrue);
                this.#elements.rdf_content_dtype_input.prop(this.#disabledFalse);
            }
        );

        //
        // Expression Edit & Preview...
        //
        this.#elements.expEditPreview
        .on("click",
            (evt) => {
                evt.preventDefault();

                // Validate the node type information...
                if ( ! this.#validateNodeTypes() ) {
                    return;
                }

                // For Index (Row / Record) or Cell based Node Types...
                if (this.#bIsVarNodeConfig) {
                    // Get the column name from the value of the checked column radio...
                    // NOTE: An empty column name == a Row / Record Index (Constant is eliminated)
                    // @ts-ignore
                    const strColumnName = $("input[name='rdf-column-radio']:checked").val();
                    const strExpression = this.#rdf_cell_expr;
                    const bIsResource = ( this.#eType === RDFTransformCommon.NodeType.Resource );
                    var strPrefix = null;
                    if (bIsResource) {
                        var selPrefix = this.#elements.rdf_prefix_select;
                        strPrefix = selPrefix.val();
                    }
                    if ( this.#eType === RDFTransformCommon.NodeType.Blank ) {
                        // Blank (not much to do)...
                        // @ts-ignore
                        alert( $.i18n('rdft-dialog/alert-blank') );
                    }
                    else { // Expression preview...
                        this.#expressionEditAndPreview(strColumnName, strExpression, bIsResource, strPrefix);
                    }
                }
                // For Constant Node Types...
                else {
                    // Constant Node Types...
                    // @ts-ignore
                    const strValue = $('#rdf-constant-value-input').val();
                    if ( this.#eType === RDFTransformCommon.NodeType.Resource ) {
                        // Constant Resource Node...
                        // @ts-ignore
                        alert( $.i18n('rdft-dialog/alert-cresource') + " <" + strValue + ">" );
                    }
                    else if ( this.#eType === RDFTransformCommon.NodeType.Literal ) {
                        // Constant Literal Node...
                        // @ts-ignore
                        alert( $.i18n('rdft-dialog/alert-cliteral') + " '" + strValue + "'" );
                    }
                    else if ( this.#eType === RDFTransformCommon.NodeType.Blank ) {
                        // Constant Blank Node...
                        // @ts-ignore
                        alert( $.i18n('rdft-dialog/alert-cblank') );
                    }
                }
            }
        );

        //
        // View Management...
        //
        if (this.#bIsRoot) {
            // Root nodes can only be resources, so we only allow resource elements...
            this.#elements.rdf_content_txt_radio
            .add(this.#elements.rdf_content_lang_radio)
            .add(this.#elements.rdf_content_int_radio)
            .add(this.#elements.rdf_content_double_radio)
            .add(this.#elements.rdf_content_date_radio)
            .add(this.#elements.rdf_content_date_time_radio)
            .add(this.#elements.rdf_content_boolean_radio)
            .add(this.#elements.rdf_content_dtype_radio)
            .prop(this.#disabledTrue); // ...and never turn them on!

            this.#elements.asText
            .add(this.#elements.asLang)
            .add(this.#elements.asInt)
            .add(this.#elements.asDouble)
            .add(this.#elements.asDate)
            .add(this.#elements.asDateTime)
            .add(this.#elements.asBool)
            .add(this.#elements.asData)
            .add(this.#elements.asCustom)
            .addClass("view-disabled");
        }
    }

    //
    // Method #validateNodeTypes()
    //
    //  From dialog changes.
    //
    //  Get the Node Type information:
    //      1. Node Value Type: Variable or Constant
    //      2. Node RDF Type: "resource", "literal", or "blank"
    //  When the Node's RDF Type cannot be determined, return a failed indicator (false)
    //
    #validateNodeTypes() {
        // Determine the Node's Value Type: Variable or Constant
        //      by testing if Constant Radio button is checked...
        // @ts-ignore
        this.#bIsVarNodeConfig = ! ( $("#rdf-constant-value-radio").prop('checked') );

        // Determine the Node's RDF Type: "resource", "literal", or "blank"...
        // @ts-ignore
        const strNodeType = $("input[name='rdf-content-radio']:checked").val();
        this.#eType = RDFTransformCommon.NodeType.getType(strNodeType);

        if ( this.#eType === null ) {
            // @ts-ignore
            alert( $.i18n('rdft-data/alert-RDF-type') );
            return false;
        }
        return true;
    }

    #expressionEditAndPreview(strColumnName, strExpression, bIsResource, strPrefix) {
        // NOTE: The column (cell) index is a zero (0) based index.
        //      When there is no "Row / Record Index column", we use -1 to represent it.
        const iIndexColumn = -1;
        const iColumnIndex =
            ( strColumnName ? // A non-empty string or an empty string
                // Look up the column index by column name...
                RDFTransform.findColumn(strColumnName).cellIndex :
                // No column name == Row / Record Index "column"...
                iIndexColumn
            );
        const bIsIndex = (iColumnIndex === iIndexColumn);
        /** @type {{cellIndex?: number, columnName?: string}} */
        var objColumn = null; // ...just get the rows
        if ( ! bIsIndex ) {
            objColumn = {}; // ...get the rows and the related column values
            objColumn.cellIndex = iColumnIndex;
            objColumn.columnName = strColumnName;
        }
        const onDone = (strExpCode) => {
            if (strExpCode !== null) {
                strExpCode = strExpCode.substring(5); // ...remove "grel:"
            }
            this.#rdf_cell_expr = strExpCode;
            this.#elements.rdf_cell_expr.empty().text( RDFTransformCommon.shortenExpression(strExpCode) );
            // If the new cell type is the same as the original cell type...
            if (this.#rdf_cell === this.#rdf_cell_orig) {
                // ...update the original expression to the new expression...
                this.#rdf_cell_expr_orig = strExpCode;
            }
        };

        // Data Preview: Resource or Literal...
        const dialogDataTable = new RDFDataTableView( this.#dialog.getBaseIRI(), bIsResource, strPrefix );
        dialogDataTable.preview(objColumn, strExpression, bIsIndex, onDone);
    }

    //
    // Method #getResultJSON()
    //
    //  Construct a node object from the dialog contents:
    //    theNode =
    //    { prefix: <prefix>,
    //      valueType: {
    //  RESOURCE... type: "iri"
    //  BLANK...... type: "bnode" | "value_bnode"
    //  LITERAL.... type: "literal" | "language_literal" | "datatype_literal",
    //             language: <language>,
    //             datatype: {
    //               prefix: <dt_prefix>
    //               valueSource: { source: "constant", constant: <xsd/other type> }
    //             }
    //      }
    //      valueSource: {
    //  CELL-based...
    //    INDEX.... source: "row_index" | "record_id"
    //    COLUMN... source: "column", columnName: <columnName>
    //    RESOURCE or LITERAL (not BLANK)...
    //              expression: { language: "grel", code: <expression> }
    //  CONSTANT-based...
    //              source: "constant", constant: <constValue>
    //      }
    //      typeMappings:     ...are not processed by the NodeConfig dialog
    //      propertyMappings: ...are not processed by the NodeConfig dialog
    //    }
    //
    #getResultJSON() {
        // Validate the node type information...
        if ( ! this.#validateNodeTypes() ) {
            return;
        }

        // Prepare the JSON node (the return value)...
        var theNode = {};

        //
        // Dynamically add keys with values as needed...
        //

        theNode.valueSource = {};
        /** @type {string} */
        var strConstVal = null;

        // All Resource Nodes...
        if ( this.#eType === RDFTransformCommon.NodeType.Resource ) {
            //
            // Get the prefix, if any...
            //
            /** @type {{ val: Function, find: Function }} */
            // @ts-ignore
            var selPrefix = $('#rdf-prefix-select');
            // Get the selection value...
            const strPrefixVal = selPrefix.val();
            // Get the selection text (for prefix)...
            var strPrefixText = null;
            if (strPrefixVal !== '') {
                if ( strPrefixVal === ':' ) {
                    strPrefixText = "";
                }
                else {
                    strPrefixText = selPrefix.find(":selected").text();
                }
            }
            if (strPrefixText !== null) {
                theNode.prefix = strPrefixText;
            }

            //  We don't need "iri" types for root nodes as they are common.
            //  Store for all others...
            if ( ! this.#bIsRoot ) {
                theNode.valueType = {};
                theNode.valueType.type = RDFTransform.gstrIRI; // ...implied for root nodes if missing
            }
        }

        // All Blank Nodes...
        else if ( this.#eType === RDFTransformCommon.NodeType.Blank ) {
            theNode.valueType = {};
            if (this.#bIsVarNodeConfig) { // Variable BNode...
                theNode.valueType.type = RDFTransform.gstrBNode;
            }
            else { // Constant Value BNode...
                theNode.valueType.type = RDFTransform.gstrValueBNode;
            }
        }

        // All Literal Nodes...
        else if ( this.#eType === RDFTransformCommon.NodeType.Literal ) {
            theNode.valueType = {};
            // Check for simple text literal...
            // @ts-ignore
            if ( $('#rdf-content-txt-radio').prop('checked') ) {
                theNode.valueType.type = RDFTransform.gstrLiteral;
            }
            // Check for language literal...
            // @ts-ignore
            else if ( $('#rdf-content-lang-radio').prop('checked') ) {
                theNode.valueType.type = RDFTransform.gstrLanguageLiteral;
                // @ts-ignore
                theNode.valueType.language = $('#rdf-content-lang-input').val();
            }
            // Check for datatype literal...
            else {
                theNode.valueType.type = RDFTransform.gstrDatatypeLiteral;
                theNode.valueType.datatype = {};
                theNode.valueType.datatype.valueSource = {};
                theNode.valueType.datatype.valueSource.source = RDFTransform.gstrConstant;
                // TODO: Expand from "constant" to allow "cell" datatypes with expressions...
                //if (theNode.valueType.dataType.type === "expression" && "code" in "expression" ) {
                //    theNode.valueType.datatype.expression = {};
                //    theNode.valueType.datatype.expression.language = RDFTransform.g_strDefaultExpLang;
                //    theNode.valueType.datatype.expression.code = strExpCode;
                //}

                // Check for custom datatype literal...
                // @ts-ignore
                if ( $('#rdf-content-dtype-radio').prop('checked') ) {
                    // Check for custom dataType IRI value...
                    // @ts-ignore
                    strConstVal = $('#rdf-content-dtype-input').val();
                    if ( strConstVal !== null && strConstVal.length === 0 ) {
                        // @ts-ignore
                        alert( $.i18n('rdft-dialog/alert-custom') );
                        return null;
                    }
                    // Extract prefix if present and reduce constant to local part...
                    theNode.valueType.datatype.valueSource.constant = strConstVal; // ...default: Full or not prefixed
                    if (strConstVal.indexOf('://') < 0) { // ...not Full...
                        var iIndex = strConstVal.indexOf(':');
                        if (iIndex === 0) { // ...at beginning, so Base IRI...
                            theNode.valueType.datatype.prefix = ""; // ...prefix
                            theNode.valueType.datatype.valueSource.constant =
                                strConstVal.substring(1); // ...local part
                        }
                        else if (iIndex > 0) { // ...inside, so split...
                            theNode.valueType.datatype.prefix =
                                strConstVal.substring(0, iIndex); // ...prefix
                            theNode.valueType.datatype.valueSource.constant =
                                strConstVal.substring(iIndex + 1); // ...local part
                        }
                    }
                }
                // Otherwise, popular XSD datatype literal...
                else {
                    // TODO: Check for prefix "xsd" exists...
                    theNode.valueType.datatype.prefix = "xsd"; // http://www.w3.org/2001/XMLSchema#
                    // @ts-ignore
                    if ( $('#rdf-content-int-radio').prop('checked') ) {
                        theNode.valueType.datatype.valueSource.constant = 'int';
                    }
                    // @ts-ignore
                    else if ( $('#rdf-content-double-radio').prop('checked') ) {
                        theNode.valueType.datatype.valueSource.constant = 'double';
                    }
                    // @ts-ignore
                    else if ( $('#rdf-content-date-radio').prop('checked') ) {
                        theNode.valueType.datatype.valueSource.constant = 'date';
                    }
                    // @ts-ignore
                    else if ( $('#rdf-content-date-time-radio').prop('checked') ) {
                        theNode.valueType.datatype.valueSource.constant = 'dateTime';
                    }
                    // @ts-ignore
                    else if ( $('#rdf-content-boolean-radio').prop('checked') ) {
                        theNode.valueType.datatype.valueSource.constant = 'boolean';
                    }
                    if ( ! (RDFTransform.gstrConstant in theNode.valueType.datatype.valueSource) ) {
                        // @ts-ignore
                        alert( $.i18n('rdft-data/internal-error') + "\n" + "No Constant source value in type's datatype.");
                        return null;
                    }
                }
            }
        }

        // All Cell-based Nodes (NOT Constant)...
        if (this.#bIsVarNodeConfig) {
            // Prepare for Row/Record or Column...
            /** @type {string} */
            // @ts-ignore
            const strColumnName = $("input[name='rdf-column-radio']:checked").val();
            if (strColumnName.length === 0) { // ...Row or Record Index...
                theNode.valueSource.source = RDFTransform.gstrValueSourceIndex;
            }
            else { // ...Column...
                theNode.valueSource.source = RDFTransform.gstrColumn;
                theNode.valueSource.columnName = strColumnName;
            }

            // For Resource or Literal (NOT Blank) Nodes,
            //  Get the Expression...
            //      (Blank Nodes don't use Expressions)
            // TODO: Check for correctness--blank node expressions may eval:
            //      True or False for variable blank node
            //      calculated blank node name as per constant blank nodes
            if (this.#eType !== RDFTransformCommon.NodeType.Blank) {
                // Set expression...
                var strExpCode = this.#rdf_cell_expr;
                if ( strExpCode === null || strExpCode.length === 0 ) {
                    // @ts-ignore
                    alert( $.i18n('rdft-dialog/alert-enter-exp') );
                    return null;
                }
                //  We don't need "value" expressions as they are common.
                //  Store all others...
                if ( strExpCode !== RDFTransform.gstrCodeValue ) {
                    theNode.expression = {};
                    theNode.expression.language = RDFTransform.gstrDefaultExpLang;
                    theNode.expression.code = strExpCode;
                }
            }
        }

        // All Constant-based Nodes...
        else {
            // Set value...
            // @ts-ignore
            strConstVal = $('#rdf-constant-value-input').val();
            if ( strConstVal.length === 0 ) {
                // @ts-ignore
                alert( $.i18n('rdft-dialog/alert-enter-const') );
                return null;
            }
            // ...get value...
            theNode.valueSource.source = RDFTransform.gstrConstant;
            theNode.valueSource.constant = strConstVal;
        }

        // All good...
        return theNode;
    }
}
