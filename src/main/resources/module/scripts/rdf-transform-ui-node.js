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
            //      <Index IRI> or <ColName IRI> or "Index Literal" or "ColName Literal"

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
            .text('[' + $.i18n('rdft-dialog/row-index') + ']');
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

    //
    // Method #getResultJSON()
    //
    //  Construct a node object from the dialog contents:
    //      node.nodeType           | All Nodes
    //      node.lang               | All Literals
    //      node.valueType          | All Literals
    //      node.isRowNumberCell    | All Cell Resources
    //      node.columnName         | Column Cell Resources
    //      node.expression         | Non-Blank Cell Nodes
    //      node.value              | Non-Blank Constant Nodes
    //
    //      node.nodeType   | Node Type    | MUST: All              | ("cell-as-" or "") + ("resource" or "literal" or "blank")
    //      node.language   | Language ID  |  OPT: All Literals     | append "@" + language ID
    //      node.dataType   | Datatype     |  OPT: All Literals     | append "^^" + datatype
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
    //              node.lang ||              |         node.lang ||              |         node.lang ||              |
    //                node.valueType || none  |           node.valueType || none  |           node.valueType || none  |
    //              node.expression           |         node.expression           |                                   |
    //                                        |                                   |                                   |
    //          "cell-as-blank" --------------|     "cell-as-blank" --------------|     "blank" ----------------------|
    //                                        |                                   |                                   |
    //
    #getResultJSON() {
        var strNodeType = $("#rdf-constant-value-radio").is(':checked') ? '' : RDFTransformCommon.g_strRDFT_CELLAS;
        var strNodeSubtype = $("input[name='rdf-content-radio']:checked").val();

        // Prepare node (the return value)...
        var node = {};

        // Dynamically add keys with values as needed...
        // NOTE: The "nodeType" reflects the node's designation:
        //      ("cell-as-" or "") + ("resource" or "literal" or "blank")
        //      For "cell-as-":
        //          Index Nodes:  "isRowNumberCell" == true
        //          Column Nodes: "isRowNumberCell" == false
        //      For "": Constant Node
        node.nodeType = strNodeType + strNodeSubtype;

        // All Literal Nodes...
        if (strNodeSubtype === RDFTransformCommon.g_strRDFT_LITERAL) {
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
                    if ( value.length == 0 ) {
                        alert( $.i18n('rdft-dialog/alert-custom') );
                        return null;
                    }
                    node.valueType = value;
                }
            }
        }

        // All Cell-based Nodes...
        if (strNodeType === RDFTransformCommon.g_strRDFT_CELLAS) {
            // Prepare isRowNumberCell...
            node.isRowNumberCell = true;

            // Prepare columnName...
            const strColumnName = $("input[name='rdf-column-radio']:checked").val();
            if (strColumnName.length > 0) { // ...good columnName...
                // ...not an Index Cell...
                node.isRowNumberCell = false;
                // ...get columnName...
                node.columnName = strColumnName;
            }

            // Not a Cell-based BNode?
            if (strNodeSubtype !== RDFTransformCommon.g_strRDFT_BLANK) {
                // ...get expression...
                node.expression
                var expression = $('#rdf-cell-expr').text();
                if ( expression.length == 0 ) {
                    alert( $.i18n('rdft-dialog/alert-enter-exp') );
                    return null;
                }
            }
        }

        // All Value Expression (Constant) Nodes...
        else {
            // Not a constant BNode?
            if (strNodeSubtype !== RDFTransformCommon.g_strRDFT_BLANK) {
                // ...check for value...
                var value = $('#rdf-constant-value-input').val();
                if ( value.length == 0 ) {
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

        // Interrogation...
        var isCellNode = this.#node.nodeType.startsWith(RDFTransformCommon.g_strRDFT_CELLAS);
        var isRowIndex = this.#node.isRowNumberCell != null ? this.#node.isRowNumberCell : false;
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
        if (columns.length == 1) {
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
                        if (this.#node.rdfTypes) {
                            node.rdfTypes = cloneDeep(this.#node.rdfTypes);
                        }
                        this.#node = node;
                        /*if ('columnIndex' in node) {
                            if (node.columnIndex !== -1) {
                                this.node.columnName = theProject.columnModel.columns[node.columnIndex].name;
                            }
                            else {
                                this.node.isRowNumberCell = true;
                            }
                        }*/
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

    #preview(strColumnName, strExpression, isIRI) {
        const iRowRecColumnIndex = -1;
        const iColumnIndex =
            ( strColumnName ? // A non-empty string or an empty string
                // Look up the cell index by column name...
                RDFTransform.findColumn(strColumnName).cellIndex :
                // No column name == Row / record Index...
                iRowRecColumnIndex
            );
        const isRowNumberCell = (iColumnIndex == iRowRecColumnIndex);
        var objColumn = null; // ...just get the rows
        if ( ! isRowNumberCell ) {
            objColumn = // ...get the rows and the related column values
                {   'cellIndex'  : iColumnIndex,
                    'columnName' : strColumnName
                }
        };
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
        if ( this.#isExpandable() ) {
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
                this.#addNodeRDFType(obj.iri, obj.cirie);
            }
        );
    }

    #addNodeRDFType(strIRI, strCIRIE) {
        if ( ! this.#node.rdfTypes ) {
            this.#node.rdfTypes = [];
        }
        this.#node.rdfTypes
        .push(
            {   'iri'   : strIRI,
                'cirie' : strCIRIE
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
        var strText = strIRI; // ...display just the IRI
        // If the IRI and the CIRIE differ, display both...
        if (strIRI.localeCompare(strCIRIE) != 0 ) {
            strText =
                ' IRI : ' + strIRI + '\n' +
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
        var bGetLinks = false;

        if ( this.#node.nodeType.startsWith(RDFTransformCommon.g_strRDFT_CELLAS) ) {
            // For "cell-as-*", the node must have either a column name or
            // is an index cell...
            if ( ! ("columnName" in this.#node || "isRowNumberCell" in this.#node) ) {
                return null;
            }

            if (this.#node.nodeType == RDFTransformCommon.g_strRDFT_CRESOURCE) {
                result = {
                    "nodeType"   : this.#node.nodeType,
                    "expression" : this.#node.expression,
                    "rdfTypes"   : [],
                    "links"      : []
                };
                bGetLinks = true;
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
                bGetLinks = true;
            }

            if (this.#node.columnName) {
                result.columnName = this.#node.columnName;
            }
            result.isRowNumberCell = this.#node.isRowNumberCell;
        }
        else if (this.#node.nodeType == RDFTransformCommon.g_strRDFT_RESOURCE) {
            if ( ! ("value" in this.#node) || ! this.#node.value) {                     ! ( "value" in this.#node && this.#node.value)
                return null;
            }
            result = {
                "nodeType" : this.#node.nodeType,
                "value"    : this.#node.value,
                "rdfTypes" : [],
                "links"    : []
            };
            bGetLinks = true;
        }
        else if (this.#node.nodeType == RDFTransformCommon.g_strRDFT_LITERAL) {
            if ( ! ("value" in this.#node) || ! this.#node.value) {
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
            bGetLinks = true;
        }

        if ( result == null ) {
            return null;
        }

        if (bGetLinks) {
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
