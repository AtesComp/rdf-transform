/*
 *  CLASS RDFTransformUINode
 *
 *  The UI node manager for the RDF Transform dialog
 */
class RDFTransformUINode {
    static iMaxNodeLength = 35;

    #dialog;
    #node;
    #options;
    #linkUIs;
    #detailsRendered;
    #tdMain;
    #tdToggle;
    #tdDetails;
    #expanded;

    #theNodeLabel;
    #level;

    #collapsedDetailDiv;
    #expandedDetailDiv;
    #tableLinks;

    #checkedTrue;
    #disabledTrue;
    #disabledFalse;
    #strLangInputID;
    #strTypeInputID;

    constructor(dialog, node, table, options) {
        this.#dialog = dialog;
        this.#node = node;
        this.#options = options;

        this.#linkUIs = [];
        this.#detailsRendered = false;

        this.#checkedTrue = { checked : true };
        this.#disabledTrue = { disabled : true };
        this.#disabledFalse = { disabled : false };
        this.#strLangInputID = '#rdf-content-lang-input';
        this.#strTypeInputID = '#rdf-content-type-input';

        var tr = table.insertRow(table.rows.length);
        this.#tdMain    = tr.insertCell(0);
        this.#tdToggle  = tr.insertCell(1);
        this.#tdDetails = tr.insertCell(2);

        $(this.#tdMain)
        .addClass("rdf-transform-node-main")
        .attr("width", "250")
        .addClass("padded");
        $(this.#tdToggle)
        .addClass("rdf-transform-node-toggle")
        .attr("width", "1%")
        .addClass("padded")
        .hide();
        $(this.#tdDetails)
        .addClass("rdf-transform-node-details")
        .attr("width", "62%")
        .hide();

        this.#renderMain();
        //this.renderTypes();
        this.#expanded = this.#options.expanded;
        if (this.#isExpandable()) {
            this.#showExpandable();
        }
    }

    #renderMain() {
        $(this.#tdMain).empty();

        var bExpandable = this.#isExpandable();
        var htmlType = '';
        if ( bExpandable )
        {
            htmlType =
'<tr>' +
  '<td>' +
    '<table width="100%" class="rdf-transform-types-table" bind="rdftTypesTable">' +
      '<tr bind="rdftTypesTR">' +
        '<td bind="rdftTypesTD"></td>' +
      '</tr>' +
      '<tr bind="rdftAddTypeTR">' +
        '<td>' +
          '<div class="padded">' +
            '<a href="#" class="action" bind="rdftAddType">' + $.i18n('rdft-dialog/add-type') + '</a>' +
          '</div>' +
        '</td>' +
      '</tr>' +
    '</table>' +
  '</td>' +
'</tr>';
        }

        var html = $(
'<table width="100%">' +
  '<tr>' +
    '<td bind="rdftNodeLabel"></td>' +
  '</tr>' +
  htmlType +
'</table>'
        )
        .appendTo(this.#tdMain);

        var elements = DOM.bind(html);
        this.#theNodeLabel = elements.rdftNodeLabel;
        if (bExpandable) {
            var typesTable = $('<table width="100%"></table>')[0];
            if (this.#node.rdfTypes && this.#node.rdfTypes.length > 0) {
                // Create each type display with removal icon...
                for (var iIndex = 0; iIndex < this.#node.rdfTypes.length; iIndex++) {
                    var tr = typesTable.insertRow(typesTable.rows.length);

                    // Set index for "onClick" callback handler...
                    // NOTE: Use "let" (not "var") to correct loop index scoping in
                    //       the "onClick" callback handler.
                    let iLocalIndex = iIndex;

                    var img = $('<img />')
                        .attr("title", $.i18n('rdft-dialog/remove-type'))
                        .attr("src", "images/close.png")
                        .css("cursor", "pointer")
                        .click(
                            () => {
                                this.#removeNodeRDFType(iLocalIndex);
                            }
                        );
                    $(tr).append( $('<td></td>').append(img) );
                    $(tr).append(
                        $('<td></td>')
                        .append(
                            $('<a href="#" class="action"></a>')
                            .text(
                                RDFTransformCommon.shortenResource(
                                    this.#getTypeName( this.#node.rdfTypes[iIndex] )
                                )
                            )
                            .click( (evt) => {
                                    evt.preventDefault();
                                    this.#showNodeRDFType( $(evt.target), iLocalIndex );
                                }
                            )
                        )
                    );
                }
                elements.rdftTypesTD.html(typesTable);
            }
            else {
                elements.rdftTypesTR.remove();
            }
            elements.rdftAddType
            .click(
                (evt) => {
                    evt.preventDefault();
                    this.#addRDFType(evt.currentTarget);
                }
            );
        }
        var ahref =
            $('<a href="javascript:{}"></a>')
            .addClass("rdf-transform-node-tag")
            .appendTo(this.#theNodeLabel)
            .click(
                () => {
                    this.showNodeConfigDialog();
                }
            );

        if (this.#node.nodeType == RDFTransformCommon.g_strRDFT_CRESOURCE ||
            this.#node.nodeType == RDFTransformCommon.g_strRDFT_CLITERAL) {
            // Index IRI
            // ColName IRI
            // Index Literal
            // ColName Cell

            var strNodeLabel = ' IRI';
            if (this.#node.nodeType === RDFTransformCommon.g_strRDFT_CLITERAL) {
                strNodeLabel = this.#node.isRowNumberCell ? ' Literal' : ' ' + $.i18n('rdft-dialog/cell');
            }

            var span = $('<span></span>').addClass("rdf-transform-node-label");
            var strSPAN = "";
            if (this.#node.isRowNumberCell) {
                strSPAN = '[' + $.i18n('rdft-dialog/row-index') + ']';
            }
            else if ("columnName" in this.#node) {
                strSPAN = this.#node.columnName;
            }
            else {
                strNodeLabel = "Configure?"; // TODO: Make $.i18n('rdft-dialog/configure')
            }
            span.text(strSPAN + strNodeLabel);
            ahref.append(span);
        }
        else if (this.#node.nodeType == RDFTransformCommon.g_strRDFT_RESOURCE) {
            if ("value" in this.#node) {
                ahref.html( RDFTransformCommon.shortenResource(this.#node.value) );
            }
            else {
                ahref.html( $.i18n('rdft-dialog/which-res') );
            }
        }
        else if (this.#node.nodeType == RDFTransformCommon.g_strRDFT_LITERAL) {
            if ("value" in this.#node) {
                ahref.html( RDFTransformCommon.shortenLiteral(this.#node.value) );
            }
            else {
                ahref.html( $.i18n('rdft-dialog/what-val') );
            }
        }
        else if (this.#node.nodeType == RDFTransformCommon.g_strRDFT_BLANK) {
            ahref.html("[blank]");
        }
        else if (this.#node.nodeType == RDFTransformCommon.g_strRDFT_CBLANK) {
            ahref.html("[blank] " + $.i18n('rdft-dialog/cell'));
        }

        //Types
        /*var aux_table = $('<table>').appendTo($(this.tdMain));
        aux_table.append($('<tr>').append(td));
        this.typesTd = $('<td>').attr("width", "250").appendTo($('<tr>').appendTo(aux_table));
        this.renderTypes();*/
    }

    #isExpandable() {
        return (this.#node.nodeType == RDFTransformCommon.g_strRDFT_RESOURCE ||
                this.#node.nodeType == RDFTransformCommon.g_strRDFT_CRESOURCE ||
                this.#node.nodeType == RDFTransformCommon.g_strRDFT_BLANK ||
                this.#node.nodeType == RDFTransformCommon.g_strRDFT_CBLANK);
    }

    show() {
        if (this.#expanded) {
            this.#collapsedDetailDiv.hide();
            this.#expandedDetailDiv.show();
        }
        else {
            this.#collapsedDetailDiv.show();
            this.#expandedDetailDiv.hide();
        }
    }

    #showExpandable() {
        $(this.#tdToggle).show();
        $(this.#tdDetails).show();

        if (this.#detailsRendered) {
            return;
        }
        this.#detailsRendered = true;

        this.#collapsedDetailDiv =
            $('<div></div>')
            .appendTo(this.#tdDetails)
            .addClass("padded")
            .html("...");
        this.#expandedDetailDiv =
            $('<div></div>')
            .appendTo(this.#tdDetails)
            .addClass("rdf-transform-detail-container");

        this.#renderDetails();

        this.show();

        //$(this.tdToggle).html("&nbsp;");
        $('<img />')
        .attr("src", this.#expanded ? "images/expanded.png" : "images/collapsed.png")
        .appendTo(this.#tdToggle)
        .click(
            (evt) => {
                this.#expanded = !this.#expanded;
                $(evt.currentTarget)
                .attr("src", this.#expanded ? "images/expanded.png" : "images/collapsed.png");
                this.show();
            }
        );
    }

    #renderDetails() {
        this.#tableLinks =
            $('<table></table>').addClass("rdf-transform-links-table-layout")
            .appendTo(this.#expandedDetailDiv)[0];

        if ("links" in this.#node && this.#node.links !== null) {
            for (const link of this.#node.links) {
                this.#linkUIs.push(
                    new RDFTransformUILink(
                        this.#dialog,
                        link,
                        this.#tableLinks,
                        { expanded: true },
                        this
                    ));
            }
        }

        var divFooter = $('<div></div>')
            .addClass("padded")
            .appendTo(this.#expandedDetailDiv);

        $('<a href="javascript:{}"></a>').addClass("action").text( $.i18n('rdft-dialog/add-prop') )
        .appendTo(divFooter)
        .click(
            () => {
                var newLink = { // ...defaults...
                    property: null,
                    target: {
                        nodeType: RDFTransformCommon.g_strRDFT_CLITERAL
                    }
                };
                this.#linkUIs.push(
                    new RDFTransformUILink(
                        this.#dialog,
                        newLink,
                        this.#tableLinks,
                        {
                            expanded: true,
                            mustBeCellTopic: false
                        },
                        this
                    )
                );
            }
        );
    }

    #makeRowIndexChoice(tableColumns, isChecked) {
        const strText = '[' + $.i18n('rdft-dialog/row-index') + ']';
        var tr = tableColumns.insertRow(tableColumns.rows.length);
        var td = tr.insertCell(0);
        $(td).addClass('rdf-transform-node-bottom-separated');
        var tdRadio = $('<input />')
            .attr("type", "radio").val("") // No Value == No Column Name
            .attr("name", "rdf-column-radio")
            .attr("id", "rdf-radio-row-index")
            .prop("checked", isChecked)
            .click(
                () => {
                    $("#rdf-constant-value-input").prop(this.#disabledTrue);
                }
            );
        tdRadio.appendTo(td);

        td = tr.insertCell(1);
        $(td).addClass('rdf-transform-node-bottom-separated');
        var tdLabel = $('<label></label>')
            .attr("for", "rdf-radio-row-index")
            .text(strText);
        tdLabel.appendTo(td);
    }

    #makeColumnChoice(tableColumns, column, iPad = 0) {
        var tr = tableColumns.insertRow(tableColumns.rows.length);
        var td = tr.insertCell(0);
        if (iPad < 0) {
            $(td).addClass('rdf-transform-node-bottom-padded');
        }
        else if (iPad > 0) {
            $(td).addClass('rdf-transform-node-top-padded');
        }
        var strID = "rdf-radio-column" + column.cellIndex;
        var tdRadio = $('<input />')
            .attr("type", "radio").val(column.name)
            .attr("name", "rdf-column-radio")
            .attr("id", strID)
            .click(
                () => {
                    $("#rdf-constant-value-input").prop(this.#disabledTrue);
                }
            );
        if (column.name == this.#node.columnName) {
            tdRadio.prop(this.#checkedTrue);
        }
        tdRadio.appendTo(td);

        td = tr.insertCell(1);
        var tdLabel = $('<label></label>').attr("for", strID).text(column.name);
        tdLabel.appendTo(td);
    }

    #makeConstantValueChoice(tableColumns, isChecked, value) {
        const strText = $.i18n('rdft-dialog/constant-val');
        var tr = tableColumns.insertRow(tableColumns.rows.length);
        var td = tr.insertCell(0);
        $(td).addClass('rdf-transform-node-top-separated');
        var tdRadio = $('<input />')
            .attr("type", "radio").val("") // No Value == No Column Name
            .attr("name", "rdf-column-radio")
            .attr("id", "rdf-constant-value-radio")
            .prop({ checked : isChecked })
            .click(
                () => {
                    $("#rdf-constant-value-input").prop(this.#disabledFalse);
                }
            );
        tdRadio.appendTo(td);

        td = tr.insertCell(1);
        $(td).addClass('rdf-transform-node-top-separated');
        var tdLabel = $('<label></label>')
            .attr("for", "rdf-constant-value-radio")
            .text(strText);
        tdLabel.appendTo(td);

        // Insert another row (just like the rdf-content-* table)...
        var tr = tableColumns.insertRow(tableColumns.rows.length);
        var td = tr.insertCell(0); // Spacer
        var td = tr.insertCell(1);
        $(td).attr("colspan", "2");
        var tdInput = $('<input />')
            .attr("id", "rdf-constant-value-input")
            .attr("type", "text").val(value)
            .attr("size", "25")
            .prop({ disabled : !isChecked });
        tdInput.appendTo(td);
    }

    #initInputs(elements) {
        elements.rdf_content_lang_input.add(elements.rdf_content_type_input).prop(this.#disabledTrue);

        //
        // Set initial values and property settings...
        //
        if ( this.#node.nodeType === RDFTransformCommon.g_strRDFT_RESOURCE ||
             this.#node.nodeType === RDFTransformCommon.g_strRDFT_CRESOURCE ) {
            // Resource node...
            elements.rdf_content_iri_radio.prop(this.#checkedTrue);
        }
        else if ( this.#node.nodeType === RDFTransformCommon.g_strRDFT_LITERAL ||
                  this.#node.nodeType === RDFTransformCommon.g_strRDFT_CLITERAL ) {
            // Literal node...
            if (this.#node.lang) {
                // ...with Language tag...
                elements.rdf_content_lang_radio.prop(this.#checkedTrue);
                elements.rdf_content_lang_input.prop(this.#disabledFalse).val(this.#node.lang);
            }
            else {
                // ...with Datatype tag...
                if (this.#node.valueType) {
                    // Standard Datatype tags...
                    if (this.#node.valueType === 'http://www.w3.org/2001/XMLSchema#int') {
                        elements.rdf_content_int_radio.prop(this.#checkedTrue);
                    }
                    else if (this.#node.valueType === 'http://www.w3.org/2001/XMLSchema#double') {
                        elements.rdf_content_non_int_radio.prop(this.#checkedTrue);
                    }
                    else if (this.#node.valueType === 'http://www.w3.org/2001/XMLSchema#date') {
                        elements.rdf_content_date_radio.prop(this.#checkedTrue);
                    }
                    else if (this.#node.valueType === 'http://www.w3.org/2001/XMLSchema#dateTime') {
                        elements.rdf_content_date_time_radio.prop(this.#checkedTrue);
                    }
                    else if (this.#node.valueType === 'http://www.w3.org/2001/XMLSchema#boolean') {
                        elements.rdf_content_boolean_radio.prop(this.#checkedTrue);
                    }
                    else {
                        // Custom Datatype tag...
                        elements.rdf_content_type_radio.prop(this.#checkedTrue);
                        elements.rdf_content_type_input.prop(this.#disabledFalse).val(this.#node.valueType);
                    }
                }
                else {
                    // No tag, simple string...
                    elements.rdf_content_txt_radio.prop(this.#checkedTrue);
                }
            }
        }
        else if ( this.#node.nodeType === RDFTransformCommon.g_strRDFT_BLANK ||
                  this.#node.nodeType === RDFTransformCommon.g_strRDFT_CBLANK ) {
            // Blank Resource node...
            elements.rdf_content_blank_radio.prop(this.#checkedTrue);
        }

        // Set cell expression...
        var expression = RDFTransform.strDefaultExpression; // ...default expression
        if (this.#node.expression) {
            expression = this.#node.expression;
        }
        elements.rdf_cell_expr.empty().text(expression);

        //
        // Click Events...
        //
    
        // All Content radios except Language and Custom...
        elements.rdf_content_iri_radio
        .add(elements.rdf_content_txt_radio)
        .add(elements.rdf_content_int_radio)
        .add(elements.rdf_content_non_int_radio)
        .add(elements.rdf_content_date_radio)
        .add(elements.rdf_content_date_time_radio)
        .add(elements.rdf_content_boolean_radio)
        .add(elements.rdf_content_blank_radio)
        .click(
            () => {
                $(this.#strLangInputID).add(this.#strTypeInputID).prop(this.#disabledTrue);
            }
        );

        // Content radio Language...
        elements.rdf_content_lang_radio
        .click(
            () => {
                $(this.#strLangInputID).prop(this.#disabledFalse);
                $(this.#strTypeInputID).prop(this.#disabledTrue);
            }
        );

        // Content radio Custom...
        elements.rdf_content_type_radio
        .click(
            () => {
                $(this.#strLangInputID).prop(this.#disabledTrue);
                $(this.#strTypeInputID).prop(this.#disabledFalse);
            }
        );

        // Edit & Preview...
        elements.expEditPreview
        .click(
            (evt) => {
                evt.preventDefault();
                // Get the Node's Subtype: "resource", "literal", or "blank"...
                const nodeSubtype = $("input[name='rdf-content-radio']:checked").val();

                // For Constant Node Types...
                if ( $("#rdf-constant-value-radio").is(':checked') ) {
                    // Constant Node Types...
                    const strValue = $('#rdf-constant-value-input').val();
                    if (nodeSubtype === RDFTransformCommon.g_strRDFT_RESOURCE) {
                        // Constant Resource Node...
                        alert( $.i18n('rdft-dialog/alert-cresource') + " <" + strValue + ">" );
                    }
                    else if (nodeSubtype === RDFTransformCommon.g_strRDFT_LITERAL) {
                        // Constant Literal Node...
                        alert( $.i18n('rdft-dialog/alert-cliteral') + " '" + strValue + "'" );
                    }
                    else if (nodeSubtype === RDFTransformCommon.g_strRDFT_BLANK) {
                        // Constant Blank Node...
                        alert( $.i18n('rdft-dialog/alert-cblank') );
                    }
                }
                // For all other Index (Row / Record) or Cell based Node Types...
                else {
                    // Get the column name from the value of the checked column radio...
                    // NOTE: An empty column name == a Row / Record Index
                    const strColumnName = $("input[name='rdf-column-radio']:checked").val();
                    const strExpression = $("#rdf-cell-expr").text();
                    const isIRI = ( nodeSubtype === RDFTransformCommon.g_strRDFT_RESOURCE );
                    if (nodeSubtype === RDFTransformCommon.g_strRDFT_BLANK) {
                        // Blank (not much to do)...
                        alert( $.i18n('rdft-dialog/alert-blank') );
                    }
                    else { // Expression preview...
                        this.#preview(strColumnName, strExpression, isIRI);
                    }
                }
            }
        );
    }

    #getResultJSON() {
        var nodeType = $("#rdf-constant-value-radio").is(':checked') ? '' : RDFTransformCommon.g_strRDFT_CELLAS;
        var nodeSubtype = $("input[name='rdf-content-radio']:checked").val();

        // Prepare node (the return value)...
        var node = {};

        // Dynamically add keys with values as needed...
        node["nodeType"] = nodeType + nodeSubtype;

        // All Literal Nodes...
        if (nodeSubtype === RDFTransformCommon.g_strRDFT_LITERAL) {
            // Get language...
            if ( $('#rdf-content-lang-radio').prop('checked') ) {
                node.lang = $('#rdf-content-lang-input').val();
            }
            else {
                // Get value type...
                if ( $('#rdf-content-int-radio').prop('checked') ) {
                    node.valueType = 'http://www.w3.org/2001/XMLSchema#int';
                }
                else if ( $('#rdf-content-non-int-radio').prop('checked') ) {
                    node.valueType = 'http://www.w3.org/2001/XMLSchema#double';
                }
                else if ( $('#rdf-content-date-radio').prop('checked') ) {
                    node.valueType = 'http://www.w3.org/2001/XMLSchema#date';
                }
                else if ( $('#rdf-content-date-time-radio').prop('checked') ) {
                    node.valueType = 'http://www.w3.org/2001/XMLSchema#dateTime';
                }
                else if ( $('#rdf-content-boolean-radio').prop('checked') ) {
                    node.valueType = 'http://www.w3.org/2001/XMLSchema#boolean';
                }
                else if ( $('#rdf-content-type-radio').prop('checked') ) {
                    // Check for custom datatype IRI value...
                    var value = $('#rdf-content-type-input').val();
                    if (!value) {
                        alert( $.i18n('rdft-dialog/alert-custom') );
                        return null;
                    }
                    node.valueType = value;
                }
            }
        }

        // All Cell-based Nodes...
        if (nodeType === RDFTransformCommon.g_strRDFT_CELLAS) {
            // Prepare columnName...
            const strColumnName = $("input[name='rdf-column-radio']:checked").val();

            // Prepare isRowNumberCell...
            node.isRowNumberCell = true;

            // Is good columnName...
            if (strColumnName && strColumnName != '') {
                // ...get isRowNumberCell...
                node.isRowNumberCell = false;
                // ...get columnName...
                node.columnName = strColumnName;
            }

            // Not a Cell-based BNode?
            if (nodeSubtype !== RDFTransformCommon.g_strRDFT_BLANK) {
                // ...get expression...
                node.expression = $('#rdf-cell-expr').text();
            }
        }

        // All Value Expression (Constant) Nodes...
        else {
            // Not a constant BNode?
            if (nodeSubtype !== RDFTransformCommon.g_strRDFT_BLANK) {
                // ...check for value...
                var value = $('#rdf-constant-value-input').val();
                if (!value) {
                    alert( $.i18n('rdft-dialog/alert-enter-const') );
                    return null;
                }
                // ...get value...
                node.value = value;
            }
        }
        return node;
    }

    showNodeConfigDialog() {
        var frame = DialogSystem.createDialog();

        //frame.width("490px")
        frame
        .css({  minWidth: "490px",
                minHeight: "300px"
            });

        $('<div></div>').addClass("dialog-header").text( $.i18n('rdft-dialog/rdf-node') )
        .appendTo(frame);

        var body = $('<div class="grid-layout layout-full"></div>')
            .addClass("dialog-body rdf-transform")
            .appendTo(frame);

        var footer = $('<div></div>')
            .addClass("dialog-footer")
            .appendTo(frame);

        /*--------------------------------------------------
         * Body
         *--------------------------------------------------
         */
        var html = $(DOM.loadHTML('rdf-transform', 'scripts/dialogs/rdf-transform-node-config.html'));

        var elements = DOM.bind(html);
        elements.useContent.text(     $.i18n('rdft-dialog/use-content') + '...'  );
        elements.contentUsed.text(    $.i18n('rdft-dialog/content-used') + '...' );
        elements.useExpression.text(  $.i18n('rdft-dialog/use-exp') + '...'      );
        elements.expEditPreview.text( $.i18n('rdft-dialog/preview-edit')         );

        elements.asIRI.text(      $.i18n('rdft-as/iri')        );
        elements.asText.text(     $.i18n('rdft-as/text')       );
        elements.asLang.text(     $.i18n('rdft-as/lang') + ":" );
        elements.asInt.text(      $.i18n('rdft-as/int')        );
        elements.asNonInt.text(   $.i18n('rdft-as/nonint')     );
        elements.asDate.text(     $.i18n('rdft-as/date')       );
        elements.asDateTime.text( $.i18n('rdft-as/datetime')   );
        elements.asBool.text(     $.i18n('rdft-as/bool')       );
        elements.asCustom.text(   $.i18n('rdft-as/custom')     );
        elements.asBlank.text(    $.i18n('rdft-as/blank')      );

        var tableColumns =
            $('<table></table>').appendTo(elements.columnLeft)[0];
            //.attr("cellspacing", "5")
            //.attr("cellpadding", "0")

            html.appendTo(body);

        // Interrogation...
        var isCellNode = this.#node.nodeType.startsWith(RDFTransformCommon.g_strRDFT_CELLAS);
        var isRowIndex = this.#node.isRowNumberCell !== undefined ? this.#node.isRowNumberCell : false;
        //var isNewNode = !isRowIndex && isCellNode;
        // Since the above isNewNode is used in the #makeRowIndexChoice() below,
        // the compound truth statement (isRowIndex || isNewNode) can be simplified...
        // (isRowIndex || isNewNode) =
        //  isRowIndex || (! isRowIndex && isCellNode) =
        // (isRowIndex || ! isRowIndex) && (isRowIndex || isCellNode) =
        // isRowIndex || isCellNode
        // ...and this tells us that node is a ResourceNode (true) or LiteralNode (false),
        //    which choice should have the "checked" property...
        var isResource = isRowIndex || isCellNode;

        // Add Row Number...
        //#makeRowIndexChoice(tableColumns, (isRowIndex || isNewNode));
        this.#makeRowIndexChoice(tableColumns, isResource);

        // Add Column Name...
        var columns = theProject.columnModel.columns;
        var column;
        var iPad;
        for (var iIndex = 0; iIndex < columns.length; iIndex++) {
            column = columns[iIndex];
            iPad = ( iIndex == 0 ? 1 : ( iIndex == columns.length - 1 ? -1 : 0 ) );
            this.#makeColumnChoice(tableColumns, column, iPad);
        }

        // Add constant value...
        var strConstVal = (typeof this.#node.value == 'undefined' ? '' : this.#node.value);
        this.#makeConstantValueChoice(tableColumns, !isResource, strConstVal);

        // Initilize inputs...
        this.#initInputs(elements);

        /*--------------------------------------------------
         * Footer
         *--------------------------------------------------
         */

        $('<button></button>')
        .addClass('button')
        .html( $.i18n('rdft-buttons/ok') )
        .click(
            () => {
                var node = this.#getResultJSON();
                if (node !== null) {
                    if (this.#node.rdfTypes) {
                        node.rdfTypes = cloneDeep(this.#node.rdfTypes);
                    }

                    DialogSystem.dismissUntil(this.#level - 1);

                    this.#node = node;
                    /*if ('columnIndex' in node) {
                        if (node.columnIndex !== -1) {
                            this.node.columnName = theProject.columnModel.columns[node.columnIndex].name;
                        }
                        else {
                            this.node.isRowNumberCell = true;
                        }
                    }*/
                    this.#render();
                    this.#dialog.updatePreview();
                }
            }
        )
        .appendTo(footer);

        $('<button></button>')
        .addClass('button')
        .text( $.i18n('rdft-buttons/cancel') )
        .click(
            () => {
                DialogSystem.dismissUntil(this.#level - 1);
            }
        )
        .appendTo(footer);

        this.#level = DialogSystem.showDialog(frame);
    }

    #preview(strColumnName, strExpression, isIRI) {
        const iRowRecordIndex = -1;
        const iCellIndex =
            ( strColumnName ? // A non-empty string or an empty string
                // Look up the cell index by column name...
                RDFTransform.findColumn(strColumnName).cellIndex :
                // No column name == Row / record Index...
                iRowRecordIndex
            );
        const objColumn = {
            'cellIndex'  : iCellIndex,
            'columnName' : strColumnName
        };
        const isRowNumberCell = (iCellIndex == iRowRecordIndex);
        const onDone = (strExpression) => {
            if (strExpression !== null) {
                strExpression = strExpression.substring(5); // ...remove "grel:"
            }
            $("#rdf-cell-expr").empty().text(strExpression);
        };

        // Data Preview: Resource (IRI) or Literal...
        const dialogDataTable = new RDFDataTableView( this.#dialog.getBaseIRI(), isIRI );
        dialogDataTable.preview(objColumn, strExpression, isRowNumberCell, onDone);
    }

    #render() {
        this.#renderMain();
        if (this.#isExpandable()) {
            this.#showExpandable();
        }
        else {
            this.#hideExpandable();
        }
    }

    removeLink(linkUI) {
        var iLinkIndex = this.#linkUIs.lastIndexOf(linkUI);
        if (iLinkIndex >= 0) {
            this.#linkUIs.splice(iLinkIndex, 1);
            this.#dialog.updatePreview();
        }
    }

    #hideExpandable() {
        $(this.#tdToggle).hide();
        $(this.#tdDetails).hide();
    }

    #addRDFType(element) {
        new RDFTransformResourceDialog(
            element, 'class', theProject.id, this.#dialog,
            (obj) => {
                this.#addNodeRDFType(obj.id, obj.name);
            }
        );
    }

    #addNodeRDFType(id, name) {
        if (!this.#node.rdfTypes) {
            this.#node.rdfTypes = [];
        }
        this.#node.rdfTypes
        .push(
            {   'iri'   : id,
                'cirie' : name
            }
        );
        this.#renderMain();
        this.#dialog.updatePreview();
    }

    #removeNodeRDFType(iIndex) {
        this.#node.rdfTypes.splice(iIndex, 1);
        this.#renderMain();
        this.#dialog.updatePreview();
    }

    #showNodeRDFType(target, iIndex) {
        var menu = MenuSystem.createMenu(); // ...size doesn't matter since we fit
        menu.html(
'<div bind="rdftTypeContainer">' +
  '<span class="rdf-transform-iri-text" bind="rdftTypeText" style="overflow: hidden;" /></span>' +
  '<button class="button" bind="buttonOK">' +
    $.i18n('rdft-buttons/ok') +
  '</button>' +
'</div>'
        );

        MenuSystem.showMenu(menu, () => {});
        MenuSystem.positionMenuLeftRight(menu, target);

        var strIRI = this.#node.rdfTypes[iIndex].iri;
        var strCIRIE= this.#node.rdfTypes[iIndex].cirie;
        var strText = strIRI;
        if (strIRI.localeCompare(strCIRIE) != 0 ) {
            strText =
                '  IRI: ' + strIRI + '\n' +
                'CIRIE: ' + strCIRIE;
        }

        var elements = DOM.bind(menu);

        // Set the display text...
        elements.rdftTypeText.html('<pre>' + strText + '</pre>');

        // Resize to fit display text..
        elements.rdftTypeText.on('change',
            (evt, divContainer, menuContainer) => {
                $(evt.target)
                .width(1)
                .height(1)
                .width(evt.target.scrollWidth)
                .height(evt.target.scrollHeight);
                //.css('resize', 'none');
                $(divContainer)
                .width(1)
                .width(divContainer.context.scrollWidth);
                $(menuContainer)
                .width(1)
                .width(menuContainer[0].scrollWidth);
            }
        );
        elements.rdftTypeText.trigger('change', [ elements.rdftTypeContainer, menu ]);
        elements.buttonOK.click( () => { MenuSystem.dismissAll(); } );
    }

    #getTypeName(prefix) {
        if (!prefix) {
            return '';
        }
        if (prefix.cirie !== undefined && prefix.cirie !== '') {
            return prefix.cirie;
        }
        else {
            return prefix.iri;
        }
    }

    getJSON() {
        var result = null;
        var getLinks = false;

        if ( this.#node.nodeType.startsWith(RDFTransformCommon.g_strRDFT_CELLAS) ) {
            // For "cell-as-*", the node must have either a column name or
            // is a row/record index...
            if (!("columnName" in this.#node || "isRowNumberCell" in this.#node)) {
                return null;
            }

            if (this.#node.nodeType == RDFTransformCommon.g_strRDFT_CRESOURCE) {
                result = {
                    "nodeType"   : this.#node.nodeType,
                    "expression" : this.#node.expression,
                    "rdfTypes"   : [],
                    "links"      : []
                };
                getLinks = true;
            }
            else if (this.#node.nodeType == RDFTransformCommon.g_strRDFT_CLITERAL) {
                result = {
                    "nodeType"   : this.#node.nodeType,
                    "expression" : this.#node.expression
                };
                if (this.#node.valueType) {
                    result.valueType = this.#node.valueType;
                }
                if (this.#node.lang) {
                    result.lang = this.#node.lang;
                }
            }
            else if (this.#node.nodeType == RDFTransformCommon.g_strRDFT_CBLANK) {
                result = {
                    "nodeType" : this.#node.nodeType,
                    "rdfTypes" : [],
                    "links"    : []
                };
                getLinks = true;
            }

            if (this.#node.columnName) {
                result.columnName = this.#node.columnName;
            }
            result.isRowNumberCell = this.#node.isRowNumberCell;
        }
        else if (this.#node.nodeType == RDFTransformCommon.g_strRDFT_RESOURCE) {
            if (!("value" in this.#node) || !this.#node.value) {
                return null;
            }
            result = {
                "nodeType" : this.#node.nodeType,
                "value"    : this.#node.value,
                "rdfTypes" : [],
                "links"    : []
            };
            getLinks = true;
        }
        else if (this.#node.nodeType == RDFTransformCommon.g_strRDFT_LITERAL) {
            if (!("value" in this.#node) || !this.#node.value) {
                return null;
            }
            result = {
                "nodeType" : this.#node.nodeType,
                "value"    : this.#node.value,
            };
            if (this.#node.valueType) {
                result.valueType = this.#node.valueType;
            }
            if (this.#node.lang) {
                result.lang = this.#node.lang;
            }
        }
        else if (this.#node.nodeType == RDFTransformCommon.g_strRDFT_BLANK) {
            result = {
                "nodeType" : this.#node.nodeType,
                "rdfTypes" : [],
                "links"    : []
            };
            getLinks = true;
        }

        if (!result) {
            return null;
        }

        if (getLinks) {
            if (this.#node.rdfTypes) {
                for (const rdfType of this.#node.rdfTypes) {
                    result.rdfTypes
                    .push(
                        {   "iri"   : rdfType.iri,
                            "cirie" : rdfType.cirie
                        }
                    );
                }
            }

            for (const linkUI of this.#linkUIs) {
                var link = linkUI.getJSON();
                if (link !== null) {
                    result.links.push(link);
                }
            }
        }

        return result;
    }
}
