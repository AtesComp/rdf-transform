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
    #propertyUIs;
    #detailsRendered;
    #tdMain;
    #tdToggle;
    #tdDetails;
    #expanded;

    #theNodeLabel;
    #level;

    #collapsedDetailDiv;
    #expandedDetailDiv;
    #tableProperties;

    #checkedTrue;
    #disabledTrue;
    #disabledFalse;
    #strLangInputID;
    #strTypeInputID;

    constructor(dialog, node, table, options) {
        this.#dialog = dialog;
        this.#node = node;
        this.#options = options;

        this.#propertyUIs = [];
        this.#detailsRendered = false;

        this.#checkedTrue = { "checked" : true };
        this.#disabledTrue = { "disabled" : true };
        this.#disabledFalse = { "disabled" : false };
        this.#strLangInputID = '#rdf-content-lang-input';
        this.#strTypeInputID = '#rdf-content-type-input';

        var tr = table.insertRow();
        this.#tdMain    = tr.insertCell();
        this.#tdToggle  = tr.insertCell();
        this.#tdDetails = tr.insertCell();

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
        if ( this.#isExpandable() ) {
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
                    var tr = typesTable.insertRow();

                    // Set index for "onClick" callback handler...
                    // NOTE: Use "let" (not "var") to correct loop index scoping in
                    //       the "onClick" callback handler.
                    let iLocalIndex = iIndex;

                    var td = tr.insertCell();
                    var img = $('<img />')
                        .attr("title", $.i18n('rdft-dialog/remove-type'))
                        .attr("src", "images/close.png")
                        .css("cursor", "pointer")
                        .click(
                            () => {
                                this.#removeNodeRDFType(iLocalIndex);
                            }
                        );
                    $(td).append(img);

                    td = tr.insertCell();
                    $(td).append(
                        $('<a href="#" class="action"></a>')
                        .text(
                            RDFTransformCommon.shortenResource(
                                this.#getTypeName( this.#node.rdfTypes[iIndex] )
                            )
                        )
                        .click(
                            (evt) => {
                                evt.preventDefault();
                                this.#showNodeRDFType( $(evt.target), iLocalIndex );
                            }
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
            // NOTE: The Node is a:
            //      "[Index] IRI", "ColName IRI",
            //      "[Index] Literal", "ColName Literal",
            //      "Configure?"

            var strNodeLabel = $.i18n('rdft-as/iri');
            if (this.#node.nodeType === RDFTransformCommon.g_strRDFT_CLITERAL) {
                strNodeLabel = $.i18n('rdft-as/literal');
            }

            var strSPAN = "Configure?"; // TODO: Make $.i18n('rdft-dialog/configure')
            if (this.#node.bIsIndex) {
                strSPAN = "[" + $.i18n("rdft-dialog/index") + "] " + strNodeLabel;
            }
            else if ("columnName" in this.#node) {
                strSPAN = this.#node.columnName + " " + strNodeLabel;
            }
 
            var span =
                $("<span></span>")
                .addClass("rdf-transform-node-label")
                .text(strSPAN);

            ahref.append(span);
        }
        else if (this.#node.nodeType == RDFTransformCommon.g_strRDFT_RESOURCE) {
            var strResource = $.i18n('rdft-dialog/which-res');
            if ("value" in this.#node) {
                strResource = RDFTransformCommon.shortenResource(this.#node.value);
            }
            ahref.html(strResource);
        }
        else if (this.#node.nodeType == RDFTransformCommon.g_strRDFT_LITERAL) {
            var strLiteral = $.i18n('rdft-dialog/what-val');
            if ("value" in this.#node) {
                strLiteral = RDFTransformCommon.shortenLiteral(this.#node.value);
            }
            ahref.html(strLiteral);
        }
        else if (this.#node.nodeType == RDFTransformCommon.g_strRDFT_BLANK) {
            ahref.html( "[" + $.i18n('rdft-as/blank') + "] " + $.i18n('rdft-dialog/constant-val') );
        }
        else if (this.#node.nodeType == RDFTransformCommon.g_strRDFT_CBLANK) {
            ahref.html( "[" + $.i18n('rdft-as/blank') + "] " + $.i18n('rdft-dialog/cell') );
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
        this.#tableProperties =
            $('<table></table>').addClass("rdf-transform-property-table-layout")
            .appendTo(this.#expandedDetailDiv)[0];

        if ("properties" in this.#node && this.#node.properties !== null) {
            for (const theProperty of this.#node.properties) {
                this.#propertyUIs.push(
                    new RDFTransformUIProperty(
                        this.#dialog,
                        theProperty,
                        this.#tableProperties,
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
                var theNewProperty = {}; // ...defaults...
                theNewProperty.prefix = null;
                theNewProperty.pathIRI = null;
                theNewProperty.nodeObject = {};
                theNewProperty.nodeObject.nodeType = RDFTransformCommon.g_strRDFT_CLITERAL;
                var options = {};
                options.expanded = true;
                options.mustBeCellTopic = false;
                this.#propertyUIs.push(
                    new RDFTransformUIProperty(
                        this.#dialog,
                        theNewProperty,
                        this.#tableProperties,
                        options,
                        this
                    )
                );
            }
        );
    }

    #buildIndexChoice(tableColumns, isChecked) {
        // Prepare NEW Radio Control Row...
        var tr = tableColumns.insertRow();

        var td;
        var strID = "rdf-radio-row-index";

        // Radio Control...
        td = tr.insertCell();
        $(td).addClass('rdf-transform-node-bottom-separated');
        var radio = $('<input />')
            .attr("type", "radio").val("") // ...== not a Column Node
            .attr("name", "rdf-column-radio")
            .attr("id", strID)
            .prop("checked", isChecked)
            .click(
                () => {
                    $("#rdf-constant-value-input").prop(this.#disabledTrue);
                }
            );
        $(td).append(radio);

        // Label for Radio...
        td = tr.insertCell();
        $(td).addClass('rdf-transform-node-bottom-separated');
        var label = $('<label></label>')
            .attr("for", strID)
            .text('[' + $.i18n('rdft-dialog/index') + ']');
        $(td).append(label);
    }

    #buildColumnChoice(tableColumns, column, iPad = 0) {
        // Prepare NEW Radio Control Row...
        var tr = tableColumns.insertRow();

        var td;
        var strID = "rdf-radio-column" + column.cellIndex;

        // Radio Control...
        td = tr.insertCell();
        if (iPad > 0) { // ...First Row Padding for Separator
            $(td).addClass('rdf-transform-node-top-padded');
        }
        if (iPad < 0 || iPad > 1) { // ...Last Row Padding for Separator
            $(td).addClass('rdf-transform-node-bottom-padded');
        }
        var radio = $('<input />')
            .attr("type", "radio").val(column.name) // ...== a Column Node
            .attr("name", "rdf-column-radio")
            .attr("id", strID)
            .click(
                () => {
                    $("#rdf-constant-value-input").prop(this.#disabledTrue);
                }
            );
        if (column.name == this.#node.columnName) {
            radio.prop(this.#checkedTrue);
        }
        $(td).append(radio);

        // Label for Radio...
        td = tr.insertCell();
        if (iPad > 0) { // ...First Row Padding for Separator
            $(td).addClass('rdf-transform-node-top-padded');
        }
        if (iPad < 0 || iPad > 1) { // ...Last Row Padding for Separator
            $(td).addClass('rdf-transform-node-bottom-padded');
        }
        var label = $('<label></label>')
            .attr("for", strID)
            .text(column.name);
        $(td).append(label);
    }

    #buildConstantChoice(tableColumns, isChecked) {
        // Prepare NEW Radio Control Row...
        var tr = tableColumns.insertRow();

        var td;
        var strID = "rdf-constant-value-radio";

        // Radio Control...
        td = tr.insertCell();
        $(td).addClass('rdf-transform-node-top-separated');
        var tdRadio = $('<input />')
            .attr("type", "radio").val("")  // ...== not a Column Node
            .attr("name", "rdf-column-radio")
            .attr("id", strID)
            .prop("checked", isChecked)
            .click(
                () => {
                    $("#rdf-constant-value-input").prop(this.#disabledFalse);
                }
            );
        $(td).append(tdRadio);

        // Label for Radio...
        td = tr.insertCell();
        $(td).addClass('rdf-transform-node-top-separated');
        var tdLabel = $('<label></label>')
            .attr("for", strID)
            .text( $.i18n('rdft-dialog/constant-val') );
        $(td).append(tdLabel);

        // Prepare NEW Text Control Row for this Radio Control...
        tr = tableColumns.insertRow();

        // Text Control for Radio...
        tr.insertCell(); // ...spacer
        td = tr.insertCell(); // ...align Textbox with Label
        $(td).attr("colspan", "2");
        const strConstVal = (this.#node.value == null ? '' : this.#node.value);
        var tdInput = $('<input />')
            .attr("id", "rdf-constant-value-input")
            .attr("type", "text").val(strConstVal)
            .attr("size", "25")
            .prop("disabled", ! isChecked);
        $(td).append(tdInput);
    }

    #initInputs(elements) {
        elements.rdf_content_lang_input
        .add(elements.rdf_content_type_input)
        .prop(this.#disabledTrue);

        //
        // Set initial values and property settings...
        //
        if ( this.#node.nodeType === RDFTransformCommon.g_strRDFT_RESOURCE ||
             this.#node.nodeType === RDFTransformCommon.g_strRDFT_CRESOURCE ) {
            //
            // Resource node...
            //
            elements.rdf_content_iri_radio.prop(this.#checkedTrue);
        }
        else if ( this.#node.nodeType === RDFTransformCommon.g_strRDFT_LITERAL ||
                  this.#node.nodeType === RDFTransformCommon.g_strRDFT_CLITERAL ) {
            //
            // Literal node...
            //
            if ("dataType" in this.#node) { // ...with Datatype tag... http://www.w3.org/2001/XMLSchema#
                // Standard Datatype tags...
                if (this.#node.dataType === 'int') {
                    elements.rdf_content_int_radio.prop(this.#checkedTrue);
                }
                else if (this.#node.dataType === 'double') {
                    elements.rdf_content_double_radio.prop(this.#checkedTrue);
                }
                else if (this.#node.dataType === 'date') {
                    elements.rdf_content_date_radio.prop(this.#checkedTrue);
                }
                else if (this.#node.dataType === 'dateTime') {
                    elements.rdf_content_date_time_radio.prop(this.#checkedTrue);
                }
                else if (this.#node.dataType === 'boolean') {
                    elements.rdf_content_boolean_radio.prop(this.#checkedTrue);
                }
                else {
                    // Custom Datatype tag...
                    elements.rdf_content_type_radio.prop(this.#checkedTrue);
                    elements.rdf_content_type_input.prop(this.#disabledFalse).val(this.#node.dataType);
                }
            }
            else if ("language" in this.#node) { // ...with Language tag...
                elements.rdf_content_lang_radio.prop(this.#checkedTrue);
                elements.rdf_content_lang_input.prop(this.#disabledFalse).val(this.#node.language);
            }
            else { // No tag, simple string...
                elements.rdf_content_txt_radio.prop(this.#checkedTrue);
            }
        }
        else if ( this.#node.nodeType === RDFTransformCommon.g_strRDFT_BLANK ||
                  this.#node.nodeType === RDFTransformCommon.g_strRDFT_CBLANK ) {
            //
            // Blank node...
            //
            elements.rdf_content_blank_radio.prop(this.#checkedTrue);
        }

        // Set cell expression...
        var expression = RDFTransform.g_strDefaultExpression; // ...default expression
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
        .add(elements.rdf_content_double_radio)
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
                elements.rdf_content_lang_input.prop(this.#disabledFalse);
                elements.rdf_content_type_input.prop(this.#disabledTrue);
            }
        );

        // Content radio Custom...
        elements.rdf_content_type_radio
        .click(
            () => {
                elements.rdf_content_lang_input.prop(this.#disabledTrue);
                elements.rdf_content_type_input.prop(this.#disabledFalse);
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
                    const bIsIRI = ( nodeSubtype === RDFTransformCommon.g_strRDFT_RESOURCE );
                    if (nodeSubtype === RDFTransformCommon.g_strRDFT_BLANK) {
                        // Blank (not much to do)...
                        alert( $.i18n('rdft-dialog/alert-blank') );
                    }
                    else { // Expression preview...
                        this.#preview(strColumnName, strExpression, bIsIRI);
                    }
                }
            }
        );
    }

    //
    // Method #getResultJSON()
    //
    //  Construct a node object from the dialog contents:
    //      node.nodeType           | All Nodes
    //      node.language           | All Literal Nodes
    //      node.dataType           | All Literal Nodes
    //      node.bIsIndex           | All Cell Nodes
    //      node.columnName         | Column Cell Nodes
    //      node.expression         | Non-Blank Cell Nodes
    //      node.value              | Non-Blank Constant Nodes
    //      node.rdfTypes           | All Resource Nodes
    //
    //      node.nodeType   | Node Type    | MUST: All              | ("cell-as-" or "") + ("resource" or "literal" or "blank")
    //      node.language   | Language ID  |  OPT: All Literals     | append "@" + language ID
    //      node.dataType   | Datatype     |  OPT: All Literals     | append "^^" + dataType
    //      node.isIndex    | isIndex bool | MUST: All Cells        | true OR false
    //      node.columnName | Column Name  | MUST: Column Cells     | a column reference
    //      node.expression | Expression   | MUST: Non-Blank Cells  |
    //      node.value      | Const Value  | MUST: Non-Blank Consts |
    //
    //      All Nodes
    //          node.nodeType = ("cell-as-" or "") + ("resource" or "literal" or "blank") 
    //      Index Node -----------------------| Column Node ----------------------| Constant Node --------------------|
    //          node.isIndex == true          |     node.isIndex == false         |                                   |
    //                                        |     node.columnName               |                                   |
    //                                        |                                   |                                   |
    //          "cell-as-resource" -----------|     "cell-as-resource" -----------|     "resource" -------------------|
    //              node.expression           |         node.expression           |         node.value                |
    //                                        |                                   |                                   |
    //          "cell-as-literal" ------------|     "cell-as-literal" ------------|     "literal" --------------------|
    //              node.language ||          |         node.language ||          |         node.language ||          |
    //                node.dataType || none   |           node.dataType || none   |           node.dataType || none   |
    //              node.expression           |         node.expression           |                                   |
    //                                        |                                   |                                   |
    //          "cell-as-blank" --------------|     "cell-as-blank" --------------|     "blank" ----------------------|
    //                                        |                                   |                                   |
    //
    #getResultJSON() {
        var strNodeType = $("#rdf-constant-value-radio").is(':checked') ? '' : RDFTransformCommon.g_strRDFT_CELLAS;
        var strNodeSubtype = $("input[name='rdf-content-radio']:checked").val();

        // Prepare node (the return value)...
        var theNode = {};

        // Dynamically add keys with values as needed...
        // NOTE: The "nodeType" reflects the node's designation:
        //      ("cell-as-" or "") + ("resource" or "literal" or "blank")
        //      For "cell-as-":
        //          Index Nodes:  "bIsIndex" == true
        //          Column Nodes: "bIsIndex" == false
        //      For "": Constant Node
        theNode.nodeType = strNodeType + strNodeSubtype;

        // All Literal Nodes...
        if (strNodeSubtype === RDFTransformCommon.g_strRDFT_LITERAL) {
            // Get language...
            if ( $('#rdf-content-lang-radio').prop('checked') ) {
                theNode.language = $('#rdf-content-lang-input').val();
            }
            else {
                // Get value type...
                theNode.dataType = {};
                theNode.dataType.namespace = "xsd"; // http://www.w3.org/2001/XMLSchema#
                if ( $('#rdf-content-int-radio').prop('checked') ) {
                    theNode.dataType.type = 'int';
                }
                else if ( $('#rdf-content-double-radio').prop('checked') ) {
                    theNode.dataType.type = 'double';
                }
                else if ( $('#rdf-content-date-radio').prop('checked') ) {
                    theNode.dataType.type = 'date';
                }
                else if ( $('#rdf-content-date-time-radio').prop('checked') ) {
                    theNode.dataType.type = 'dateTime';
                }
                else if ( $('#rdf-content-boolean-radio').prop('checked') ) {
                    theNode.dataType.type = 'boolean';
                }
                else if ( $('#rdf-content-type-radio').prop('checked') ) {
                    // Check for custom dataType IRI value...
                    var strValue = $('#rdf-content-type-input').val();
                    if ( strValue.length === 0 ) {
                        alert( $.i18n('rdft-dialog/alert-custom') );
                        return null;
                    }
                    delete theNode.dataType.namespace;
                    theNode.dataType.type = strValue;
                }
            }
        }

        // All Cell-based Nodes...
        if (strNodeType === RDFTransformCommon.g_strRDFT_CELLAS) {
            // Prepare bIsIndex...
            theNode.bIsIndex = true;

            // Prepare columnName...
            const strColumnName = $("input[name='rdf-column-radio']:checked").val();
            if (strColumnName.length > 0) { // ...good columnName...
                // ...not an Index Cell...
                theNode.bIsIndex = false;
                // ...get columnName...
                theNode.columnName = strColumnName;
            }

            // Not a Cell-based BNode?
            if (strNodeSubtype !== RDFTransformCommon.g_strRDFT_BLANK) {
                // ...get expression...
                var expression = $('#rdf-cell-expr').text();
                if ( expression === null || expression.length === 0 ) {
                    alert( $.i18n('rdft-dialog/alert-enter-exp') );
                    return null;
                }
                theNode.expression = expression;
            }
        }

        // All Value Expression (Constant) Nodes...
        else {
            // Not a constant BNode?
            if (strNodeSubtype !== RDFTransformCommon.g_strRDFT_BLANK) {
                // ...check for value...
                var strValue = $('#rdf-constant-value-input').val();
                if ( strValue.length === 0 ) {
                    alert( $.i18n('rdft-dialog/alert-enter-const') );
                    return null;
                }
                // ...get value...
                theNode.value = strValue;
            }
        }
        return theNode;
    }

    showNodeConfigDialog() {
        if (theProject.columnModel.columns.length < 1)
            return;

        var frame = DialogSystem.createDialog();

        //frame.width("490px")
        frame
        .css({  minWidth: "490px",
                width: "490px",
                minHeight: "300px",
            });

        /*--------------------------------------------------
         * Header
         *--------------------------------------------------
         */

         var header =
            $('<div></div>')
            .addClass("dialog-header")
            .text( $.i18n('rdft-dialog/rdf-node') );

        /*--------------------------------------------------
         * Body
         *--------------------------------------------------
         */

        var body =
            $('<div class="grid-layout layout-full"></div>')
            .addClass("dialog-body rdf-transform");

        var html = $(DOM.loadHTML('rdf-transform', 'scripts/dialogs/rdf-transform-node-config.html'));

        var elements = DOM.bind(html);
        elements.useContent.text(     $.i18n('rdft-dialog/use-content') + '...'  );
        elements.contentUsed.text(    $.i18n('rdft-dialog/content-used') + '...' );
        elements.useExpression.text(  $.i18n('rdft-dialog/use-exp') + '...'      );
        elements.expEditPreview.text( $.i18n('rdft-dialog/edit-preview')         );

        elements.asIRI.text(      $.i18n('rdft-as/iri')        );
        elements.asText.text(     $.i18n('rdft-as/text')       );
        elements.asLang.text(     $.i18n('rdft-as/lang') + ":" );
        elements.asInt.text(      $.i18n('rdft-as/int')        );
        elements.asDouble.text(   $.i18n('rdft-as/double')     );
        elements.asDate.text(     $.i18n('rdft-as/date')       );
        elements.asDateTime.text( $.i18n('rdft-as/datetime')   );
        elements.asBool.text(     $.i18n('rdft-as/bool')       );
        elements.asCustom.text(   $.i18n('rdft-as/custom')     );
        elements.asBlank.text(    $.i18n('rdft-as/blank')      );

        var tableColumns =
            $('<table></table>').appendTo(elements.columnLeft)[0];
            //.attr("cellspacing", "5")
            //.attr("cellpadding", "0")

        // Interrogation...
        var isCellNode = this.#node.nodeType.startsWith(RDFTransformCommon.g_strRDFT_CELLAS);
        var isRowIndex = this.#node.bIsIndex != null ? this.#node.bIsIndex : false;
        //var isNewNode = !isRowIndex && isCellNode;
        //this.#makeIndexChoice(tableColumns, (isRowIndex || isNewNode));
        // NOTE: Since the above isNewNode was used in #makeIndexChoice() as shown,
        //      the compound truth statement (isRowIndex || isNewNode) can be simplified:
        //          (isRowIndex || isNewNode) =
        //          (isRowIndex || (! isRowIndex && isCellNode) ) =
        //              \-------------^-------------^
        //          (isRowIndex || ! isRowIndex) && (isRowIndex || isCellNode) =
        //          (true) && (isRowIndex || isCellNode) =
        //          isRowIndex || isCellNode
        //      This tells us that this.#node is either a ResourceNode (true) or
        //      a LiteralNode (false) and indicates which choice should have the
        //      "checked" property...
        var isResource = isRowIndex || isCellNode;

        // Add Row Number Radio Row...
        // NOTE: Always ResourceNode
        this.#buildIndexChoice(tableColumns, isResource); 

        //
        // Add Column Name Radio Rows...
        //
        // NOTE: A ResourceNode OR A LiteralNode
        var columns = theProject.columnModel.columns;
        if (columns.length === 1) {
            // Process first and last column...
            // NOTE: Pad top and bottom for SINGLE column
            this.#buildColumnChoice(tableColumns, columns[0], 2);
        }
        else {
            // Process first column...
            // NOTE: Pad top of top row
            this.#buildColumnChoice(tableColumns, columns[0], 1);
            // Loop through all but first and last column...
            const iLoopLast = columns.length - 1;
            for (var iColumn = 1; iColumn < iLoopLast; iColumn++) {
                // Process column...
                // NOTE: No pad for middle rows
                this.#buildColumnChoice(tableColumns, columns[iColumn]);
            }
            // Process last column...
            // NOTE: Pad bottom of bottom row
            this.#buildColumnChoice(tableColumns, columns[iLoopLast], -1);
        }

        // Add Constant Value Radio Row...
        this.#buildConstantChoice(tableColumns, !isResource);

        // Initilize inputs...
        this.#initInputs(elements);

        body.append(html);

        /*--------------------------------------------------
         * Footer
         *--------------------------------------------------
         */

         var footer =
            $('<div></div>')
            .addClass("dialog-footer");
     
         var buttonOK =
            $('<button></button>')
            .addClass('button')
            .html( $.i18n('rdft-buttons/ok') )
            .click(
                () => {
                    var node = this.#getResultJSON();
                    if (node !== null) {
                        if ("rdfTypes" in this.#node) {
                            // Copy existing RDF Types to new node...
                            node.rdfTypes = cloneDeep(this.#node.rdfTypes);
                        }
                        this.#node = node;
                        DialogSystem.dismissUntil(this.#level - 1);
                        this.#render();
                        this.#dialog.updatePreview();
                    }
                }
            );

        var buttonCancel =
            $('<button></button>')
            .addClass('button')
            .text( $.i18n('rdft-buttons/cancel') )
            .click(
                () => {
                    DialogSystem.dismissUntil(this.#level - 1);
                }
            );

        footer.append(buttonOK);
        footer.append(buttonCancel);
    
        /*--------------------------------------------------
         * Assemble Dialog
         *--------------------------------------------------
         */

        frame.append(header, body, footer)

        this.#level = DialogSystem.showDialog(frame);
    }

    #preview(strColumnName, strExpression, bIsIRI) {
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
        const bIsIndex = (iColumnIndex == iIndexColumn);
        var objColumn = null; // ...just get the rows
        if ( ! bIsIndex ) {
            objColumn = {}; // ...get the rows and the related column values
            objColumn.cellIndex = iColumnIndex;
            objColumn.columnName = strColumnName;
        }
        const onDone = (strExp) => {
            if (strExp !== null) {
                strExp = strExp.substring(5); // ...remove "grel:"
            }
            $("#rdf-cell-expr").empty().text(strExp);
        };

        // Data Preview: Resource (IRI) or Literal...
        const dialogDataTable = new RDFDataTableView( this.#dialog.getBaseIRI(), bIsIRI );
        dialogDataTable.preview(objColumn, strExpression, bIsIndex, onDone);
    }

    #render() {
        this.#renderMain();
        if ( this.#isExpandable() ) {
            this.#showExpandable();
        }
        else {
            this.#hideExpandable();
        }
    }

    removeProperty(propertyUI) {
        var iPropertyIndex = this.#propertyUIs.lastIndexOf(propertyUI);
        if (iPropertyIndex >= 0) {
            this.#propertyUIs.splice(iPropertyIndex, 1);
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
            (theType) => {
                this.#addNodeRDFType(theType);
            }
        );
    }

    #addNodeRDFType(theType) {
        if ( ! this.#node.rdfTypes ) {
            this.#node.rdfTypes = [];
        }
        // IRI   = prefix namespace + theType.pathIRI
        // CIRIE = theType.prefix + ":" + theType.pathIRI
        this.#node.rdfTypes.push(theType);
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

        var strPrefix = this.#node.rdfTypes[iIndex].prefix;
        var strPathIRI= this.#node.rdfTypes[iIndex].pathIRI;
        var strText = strPathIRI; // ...display just the IRI
        // If the prefix is present, display both...
        if (strPrefix) {
            strText =
                ' Prefix: ' + strPrefix + '\n' +
                'PathIRI: ' + strPathIRI;
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

    #getTypeName(theType) {
        if ( ! theType ) {
            return "<ERROR: No Type!>";
        }
        if ("prefix" in theType && theType.prefix !== null) {
            return theType.prefix + ":" + theType.pathIRI;
        }
        else  if ("pathIRI" in theType && theType.pathIRI !== null) {
            return theType.pathIRI;
        }
        else {
            return "type?";
        }
    }

    getJSON() {
        var result = {};
        result.valueSource = {};
        result.valueType = {};
        var bGetProperties = false;

        if ("namespace" in this.#node) {
            result.namespace = this.#node.namespace;
        }

        //
        // CELL Nodes...
        //
        if ( this.#node.nodeType.startsWith(RDFTransformCommon.g_strRDFT_CELLAS) ) {
            // For "cell-as-*", the node must have either:
            //      a column name or
            //      is an index cell...
            const bIsIndex = ("bIsIndex" in this.#node);
            if ( ! ("columnName" in this.#node || bIsIndex) ) {
                return null; // ...otherwise, the node is not as expected.
            }

            if (bIsIndex) {
                result.valueSource.source = RDFTransform.g_strExpressionSource;
            }
            else {
                result.valueSource.source = "column";
                result.valueSource.columnName = this.#node.columnName;
            }
        }
        //
        // CONSTANT Nodes...
        //
        else {
            if ( ! "value" in this.#node) {
                return null;
            }

            result.valueSource.source = "constant";
            result.valueSource.constant = this.#node.value;
        }

        //
        // RESOURCE Nodes...
        //
        if (this.#node.nodeType == RDFTransformCommon.g_strRDFT_CRESOURCE || 
            this.#node.nodeType == RDFTransformCommon.g_strRDFT_RESOURCE ) {
            result.valueType.type = "iri";
            bGetProperties = true;
        }
        //
        // LITERAL Nodes...
        //
        else if (this.#node.nodeType == RDFTransformCommon.g_strRDFT_CLITERAL ||
                 this.#node.nodeType == RDFTransformCommon.g_strRDFT_LITERAL ) {
            if ("dataType" in this.#node) {
                result.valueType.type = "datatype_literal";
                result.valueType.datatype = {};
                result.valueType.datatype.namespace = this.#node.dataType.namespace;
                result.valueType.datatype.valueSource = {};
                result.valueType.datatype.valueSource.source = "constant";
                result.valueType.datatype.valueSource.constant = this.#node.dataType.type;
                if ("expression" in this.#node.dataType) {
                    result.valueType.datatype.expression = {};
                    result.valueType.datatype.expression.language = "grel";
                    result.valueType.datatype.expression.code = this.#node.expression;
                }
            }
            else if ("language" in this.#node) {
                result.valueType.type = "language_literal";
                result.valueType.language = this.#node.language;
            }
            else {
                result.valueType.type = "literal";
            }
        }
        //
        // BLANK Nodes...
        //
        else if (this.#node.nodeType == RDFTransformCommon.g_strRDFT_CBLANK ||
                 this.#node.nodeType == RDFTransformCommon.g_strRDFT_BLANK) {
            if (this.#node.nodeType == RDFTransformCommon.g_strRDFT_CBLANK) {
                result.valueType.type = "bnode";
            }
            else {
                result.valueType.type = "value_bnode";
            }
            bGetProperties = true;
        }

        //
        // EXPRESSION...
        ///
        if ("expression" in this.#node && this.#node.expression !== "value") {
            result.expression = {};
            result.expression.language = "grel";
            result.expression.code = this.#node.expression;
        }

        if (bGetProperties) {
            if ("rdfTypes" in this.#node && this.#node.rdfTypes.length > 0) {
                result.typeMappings = [];
                var objType;
                for (const rdfType of this.#node.rdfTypes) {
                    objType = {};
                    if ("prefix" in rdfType && rdfType.prefix != null) {
                        objType.prefix = rdfType.prefix;
                    }
                    objType.valueSource = {
                        "source"   : "constant",
                        "constant" : rdfType.pathIRI
                    };
                    result.typeMappings.push(objType);
                }
            }

            if (this.#propertyUIs && this.#propertyUIs.length > 0) {
                result.propertyMappings = [];
                for (const propertyUI of this.#propertyUIs) {
                    const property = propertyUI.getJSON();
                    if (property !== null) {
                        result.propertyMappings.push(property);
                    }
                }
            }
        }

        return result;
    }
}
