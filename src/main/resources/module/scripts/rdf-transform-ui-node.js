/*
 *  CLASS RDFTransformUINode
 *
 *  The UI node manager for the RDF Transform dialog
 */
class RDFTransformUINode {
    static iMaxNodeLength = 35;

    #dialog;
    #node;
    #bIsRoot;
    #propertyUIs;
    #options;

    #bIsVarNode;
    #eType;

    #tableDetails;

    #tdMain;
    #tdToggle;
    #tdDetails;

    #theNodeLabel;
    #level;

    #collapsedDetailDiv;
    #expandedDetailDiv;

    #checkedTrue;
    #disabledTrue;
    #disabledFalse;
    #strLangInputID;
    #strTypeInputID;

    // Setup default Master Object Node (cloneDeep() as needed)...
    static gnodeMasterObject = {};
    static {
        this.gnodeMasterObject.valueType = {};
        this.gnodeMasterObject.valueType.type = "literal";
        this.gnodeMasterObject.valueSource = {};
        this.gnodeMasterObject.valueSource.source = null; // ...to be replaced with row / record index
    };

    // Setup default Master Property Edge (cloneDeep() as needed)...
    static gpropMasterProperty = {};
    static {
        this.gpropMasterProperty.prefix = null;
        this.gpropMasterProperty.localPart = null;
        this.gpropMasterProperty.nodeObject = null; // ...to be replaced with Object Node
    }

    constructor(theDialog, theNode, bIsRoot, theProperties, theOptions) {
        this.#dialog = theDialog;
        this.#node = theNode; // ...a Transform Node
        this.#bIsRoot = bIsRoot;
        this.#options = theOptions;

        //
        // Process any Properties for the Node...
        //
        //      Nodes optionally have Properties.
        //

        this.#propertyUIs = [];

        if (theProperties !== null) {
            for (const theProperty of theProperties) {
                // Set up the Property UI...
                var propertyUI = new RDFTransformUIProperty(
                    this.#dialog,
                    theProperty,
                    { expanded: true },
                    this
                );
                this.#propertyUIs.push(propertyUI);
            }
        }

        this.#checkedTrue = { "checked" : true };
        this.#disabledTrue = { "disabled" : true };
        this.#disabledFalse = { "disabled" : false };
        this.#strLangInputID = '#rdf-content-lang-input';
        this.#strTypeInputID = '#rdf-content-type-input';

        // Based on the node,
        //  1. Set the Variable vs Constant boolean
        //  2. Set the Node Enumeration Type: Resource, Blank, or Literal
        this.#initilizeNodeTypes();
    }

    getNode() {
        return this.#node;
    }

    setPropertyUIs(propertyUIs) {
        this.#propertyUIs = propertyUIs;
    }

    hasProperties() {
        return (this.#propertyUIs.length > 0);
    }

    processView(theTable) {
        var tr = theTable.insertRow();
        this.#tdMain    = tr.insertCell();
        this.#tdToggle  = tr.insertCell();
        this.#tdDetails = tr.insertCell();

        var imgExpand =
            $('<img />')
            .attr("src", this.#options.expanded ? "images/expanded.png" : "images/collapsed.png")
            .click(
                (evt) => {
                    this.#options.expanded = ! this.#options.expanded;
                    $(evt.currentTarget)
                    .attr("src", this.#options.expanded ? "images/expanded.png" : "images/collapsed.png");
                    this.show();
                }
            );

        this.#collapsedDetailDiv =
            $('<div></div>')
            .addClass("padded")
            .html("...");
        this.#expandedDetailDiv =
            $('<div></div>')
            .addClass("rdf-transform-detail-container");

        $(this.#tdMain)
            .addClass("rdf-transform-node-main")
            .attr("width", "250")
            .addClass("padded");
        $(this.#tdToggle)
            .addClass("rdf-transform-node-toggle")
            .attr("width", "1%")
            .addClass("padded")
            .append(imgExpand);
        $(this.#tdDetails)
            .addClass("rdf-transform-node-details")
            .attr("width", "62%")
            .append(this.#collapsedDetailDiv)
            .append(this.#expandedDetailDiv);

        this.#render();

        this.#renderDetails(); // ...one time only

        this.show();
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
            /** @type {HTMLTableElement} */
            // @ts-ignore
            var typesTable = $('<table width="100%"></table>')[0];
            if ("typeMappings" in this.#node && this.#node.typeMappings.length > 0) {
                // Create each type display with removal icon...
                for (var iIndex = 0; iIndex < this.#node.typeMappings.length; iIndex++) {
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
                                this.#getTypeName( this.#node.typeMappings[iIndex] )
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
        var strReference = null;
        var bHTML = true; // Use HTML or APPEND Span?

        //
        // CELL Nodes...
        //
        if (this.#bIsVarNode) {
            if (this.#eType !== RDFTransformCommon.NodeType.Blank) {
                // NOTE: The Node is a:
                //      "[Index] IRI", "ColName IRI",
                //      "[Index] Literal", "ColName Literal",
                //      "Configure?"

                var strNodeLabel = $.i18n('rdft-as/iri');
                if (this.#eType === RDFTransformCommon.NodeType.Literal) {
                    strNodeLabel = $.i18n('rdft-as/literal');
                }

                var strSpanText = "Configure?"; // TODO: Make $.i18n('rdft-dialog/configure')
                if (this.#node.valueSource.source === "column") {
                    strSpanText = this.#node.valueSource.columnName + " " + strNodeLabel;
                }
                else {
                    strSpanText = "[" + $.i18n("rdft-dialog/index") + "] " + strNodeLabel;
                }

                strReference =
                    $("<span></span>")
                    .addClass("rdf-transform-node-label")
                    .text(strSpanText);
                bHTML = false;
            }
            else { // this.#eType === RDFTransformCommon.NodeType.Blank
                strReference = "[" + $.i18n('rdft-as/blank') + "] " + $.i18n('rdft-dialog/cell');
            }
        }
        //
        // CONSTANT Nodes...
        //
        else {
            var strConst = null;
            if ("constant" in this.#node.valueSource) {
                strConst = RDFTransformCommon.shortenResource(this.#node.valueSource.constant);
            }
            if (this.#eType === RDFTransformCommon.NodeType.Resource) {
                strReference = $.i18n('rdft-dialog/which-res');
                if (strConst !== null) {
                    strReference = strConst;
                }
            }
            else if (this.#eType === RDFTransformCommon.NodeType.Literal) {
                strReference = $.i18n('rdft-dialog/what-val');
                if (strConst !== null) {
                    strReference = strConst;
                }
            }
            else if (this.#eType === RDFTransformCommon.NodeType.Blank) {
                strReference = "[" + $.i18n('rdft-as/blank') + "] " + $.i18n('rdft-dialog/constant-val');
            }
        }

        if (bHTML) { // ...add as HTML reference...
            ahref.html(strReference);
        }
        else { // ...add as appended Span reference...
            ahref.append(strReference);
        }

        //Types
        /*var aux_table = $('<table>').appendTo($(this.tdMain));
        aux_table.append($('<tr>').append(td));
        this.typesTd = $('<td>').attr("width", "250").appendTo($('<tr>').appendTo(aux_table));
        this.renderTypes();*/
    }

    #renderDetails() {
        if (this.#tableDetails) {
            this.#tableDetails.remove();
        }
        this.#tableDetails =
            $('<table></table>')
            .addClass("rdf-transform-property-table-layout");
        this.#expandedDetailDiv.append(this.#tableDetails);

        if (this.#propertyUIs !== null) {
            for (const propertyUI of this.#propertyUIs) {
                propertyUI.processView(this.#tableDetails[0]);
            }
        }

        var divFooter = $('<div></div>')
            .addClass("padded")
            .appendTo(this.#expandedDetailDiv);

        $('<a href="javascript:{}"></a>').addClass("action").text( $.i18n('rdft-dialog/add-prop') )
        .appendTo(divFooter)
        .click(
            () => {
                var nodeObject = JSON.parse(JSON.stringify(RDFTransformUINode.gnodeMasterObject));
                nodeObject.valueSource.source = RDFTransform.gstrValueSource;

                var theProperty = JSON.parse(JSON.stringify(RDFTransformUINode.gpropMasterProperty)); // ...defaults...
                theProperty.nodeObject = nodeObject;

                // Set up the Property UI...
                var propertyUI = new RDFTransformUIProperty(
                    this.#dialog,
                    theProperty,
                    { expanded: true },
                    this
                );
                this.#propertyUIs.push(propertyUI);
                propertyUI.processView(this.#tableDetails[0]);
            }
        );
    }

    show() {
        if (this.#options.expanded) {
            this.#collapsedDetailDiv.hide();
            this.#expandedDetailDiv.show();
        }
        else {
            this.#collapsedDetailDiv.show();
            this.#expandedDetailDiv.hide();
        }
    }

    #isExpandable() {
        return (this.#eType === RDFTransformCommon.NodeType.Resource ||
                this.#eType === RDFTransformCommon.NodeType.Blank);
    }

    #showExpandable() {
        $(this.#tdToggle).show();
        $(this.#tdDetails).show();
    }

    #hideExpandable() {
        $(this.#tdToggle).hide();
        $(this.#tdDetails).show();
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
            .attr("type", "radio").val("") // ...== not a Column Node, an Index
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
        if ("columnName" in this.#node.valueSource && column.name === this.#node.valueSource.columnName) {
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
            .attr("type", "radio").val("")  // ...== not a Column Node, a Constant
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
        const strConstVal =
            ( ("constant" in this.#node.valueSource &&
               this.#node.valueSource.constant !== null) ? this.#node.valueSource.constant : '');
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
        if ( this.#eType === RDFTransformCommon.NodeType.Resource ) {
            //
            // Resource node...
            //
            elements.rdf_content_iri_radio.prop(this.#checkedTrue);
        }
        else if ( this.#eType === RDFTransformCommon.NodeType.Literal ) {
            //
            // Literal node...
            //
            if ("datatype" in this.#node.valueType) { // ...with Datatype tag... http://www.w3.org/2001/XMLSchema#
                var strType = null;
                if ("constant" in this.#node.valueType.datatype.valueSource) {
                    strType = this.#node.valueType.datatype.valueSource.constant;
                }
                // Standard Datatype tags...
                if (strType === 'int') {
                    elements.rdf_content_int_radio.prop(this.#checkedTrue);
                }
                else if (strType === 'double') {
                    elements.rdf_content_double_radio.prop(this.#checkedTrue);
                }
                else if (strType === 'date') {
                    elements.rdf_content_date_radio.prop(this.#checkedTrue);
                }
                else if (strType === 'dateTime') {
                    elements.rdf_content_date_time_radio.prop(this.#checkedTrue);
                }
                else if (strType === 'boolean') {
                    elements.rdf_content_boolean_radio.prop(this.#checkedTrue);
                }
                else {
                    // Custom Datatype tag...
                    elements.rdf_content_type_radio.prop(this.#checkedTrue);
                    elements.rdf_content_type_input.prop(this.#disabledFalse).val(strType);
                }
            }
            else if ("language" in this.#node.valueType) { // ...with Language tag...
                elements.rdf_content_lang_radio.prop(this.#checkedTrue);
                elements.rdf_content_lang_input.prop(this.#disabledFalse).val(this.#node.valueType.language);
            }
            else { // No tag, simple string...
                elements.rdf_content_txt_radio.prop(this.#checkedTrue);
            }
        }
        else if ( this.#eType === RDFTransformCommon.NodeType.Blank ) {
            //
            // Blank node...
            //
            elements.rdf_content_blank_radio.prop(this.#checkedTrue);
        }

        // Set cell expression...
        // TODO: Future code language.  It's all "grel" currently.
        var strExpCode = RDFTransform.gstrDefaultExpCode; // ...default expression
        if ("expression" in this.#node && "code" in this.#node.expression ) {
            strExpCode = this.#node.expression.code;
        }
        elements.rdf_cell_expr.empty().text(strExpCode);

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

                // Validate the node type information...
                if ( ! this.#validateNodeTypes() ) {
                    return;
                }

                // For Index (Row / Record) or Cell based Node Types...
                if (this.#bIsVarNode) {
                    // Get the column name from the value of the checked column radio...
                    // NOTE: An empty column name == a Row / Record Index (Constant is eliminated)
                    const strColumnName = $("input[name='rdf-column-radio']:checked").val();
                    const strExpression = $("#rdf-cell-expr").text();
                    const bIsResource = ( this.#eType === RDFTransformCommon.NodeType.Resource );
                    if ( this.#eType === RDFTransformCommon.NodeType.Blank ) {
                        // Blank (not much to do)...
                        alert( $.i18n('rdft-dialog/alert-blank') );
                    }
                    else { // Expression preview...
                        this.#preview(strColumnName, strExpression, bIsResource);
                    }
                }
                // For Constant Node Types...
                else {
                    // Constant Node Types...
                    const strValue = $('#rdf-constant-value-input').val();
                    if ( this.#eType === RDFTransformCommon.NodeType.Resource ) {
                        // Constant Resource Node...
                        alert( $.i18n('rdft-dialog/alert-cresource') + " <" + strValue + ">" );
                    }
                    else if ( this.#eType === RDFTransformCommon.NodeType.Literal ) {
                        // Constant Literal Node...
                        alert( $.i18n('rdft-dialog/alert-cliteral') + " '" + strValue + "'" );
                    }
                    else if ( this.#eType === RDFTransformCommon.NodeType.Blank ) {
                        // Constant Blank Node...
                        alert( $.i18n('rdft-dialog/alert-cblank') );
                    }
                }
            }
        );
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
        this.#bIsVarNode = ! ( $("#rdf-constant-value-radio").is(':checked') );

        // Determine the Node's RDF Type: "resource", "literal", or "blank"...
        const strNodeType = $("input[name='rdf-content-radio']:checked").val();
        this.#eType = RDFTransformCommon.NodeType.getType(strNodeType);

        if ( this.#eType === null ) {
            alert( $.i18n('rdft-data/alert-RDF-type') );
            return false;
        }
        return true;
    }

    //
    // Method #initilizeNodeTypes()
    //
    //  From existing node on construction.  See #getResultJSON()
    //
    //  Get the Node Type information:
    //      1. Node Value Type: Variable or Constant
    //      2. Node RDF Type: "resource", "literal", or "blank"
    //  When the Node's RDF Type cannot be determined, return a failed indicator (false)
    //
    #initilizeNodeTypes() {
        // Determine the Node's Value Type: Variable or Constant
        //      by testing the node's value source type:
        //      Variable == "row_index", "record_id", "column"
        //      Constant == "constant"
        this.#bIsVarNode = (this.#node.valueSource.source !== "constant");

        // Determine the Node's RDF Type: "resource", "literal", or "blank"...
        var strNodeType = "literal";
        if ( ! ("valueType" in this.#node) || this.#node.valueType.type === "iri") {
            strNodeType = "resource";
        }
        else if (   this.#node.valueType.type === "bnode" ||
                    this.#node.valueType.type === "value_bnode" ) {
            strNodeType = "blank";
        }
        this.#eType = RDFTransformCommon.NodeType.getType(strNodeType);

        if ( this.#eType === null ) {
            alert( $.i18n('rdft-data/alert-RDF-type') );
            return false;
        }
        return true;
    }

    #preview(strColumnName, strExpression, bIsResource) {
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
        const onDone = (strExp) => {
            if (strExp !== null) {
                strExp = strExp.substring(5); // ...remove "grel:"
            }
            $("#rdf-cell-expr").empty().text(strExp);
        };

        // Data Preview: Resource or Literal...
        const dialogDataTable = new RDFDataTableView( this.#dialog.getBaseIRI(), bIsResource );
        dialogDataTable.preview(objColumn, strExpression, bIsIndex, onDone);
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

        var html = $(DOM.loadHTML(RDFTransform.KEY, 'scripts/dialogs/rdf-transform-node-config.html'));

        var elements = DOM.bind(html);
        elements.useContent.text(     $.i18n('rdft-dialog/use-content') + '...'  );
        elements.contentUsed.text(    $.i18n('rdft-dialog/content-used') + '...' );

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

        elements.useExpression.text(  $.i18n('rdft-dialog/use-exp') + '...'      );
        elements.expEditPreview.text( $.i18n('rdft-dialog/edit-preview')         );

        var tableColumns =
            $('<table></table>').appendTo(elements.columnLeft)[0];
            //.attr("cellspacing", "5")
            //.attr("cellpadding", "0")

        // Add Row/Record Radio Row...
        // NOTE: Always ResourceNode
        this.#buildIndexChoice(tableColumns, this.#bIsVarNode);

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
        this.#buildConstantChoice(tableColumns, !(this.#bIsVarNode));

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
                        // If Old Node has Type Mappings, move them to New Node
                        if ("typeMappings" in this.#node) {
                            // Copy existing types to new node...
                            //node.typeMappings = cloneDeep(this.#node.typeMappings);
                            node.typeMappings = this.#node.typeMappings;
                        }
                        // Property Mappings are reserved in #propertyUIs

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

    //
    // Method #getResultJSON()
    //
    //  Construct a node object from the dialog contents:
    //    theNode.valueType = {}
    //    For RESOURCE:
    //      theNode.valueType.type = "iri"
    //    For BLANK:
    //      theNode.valueType.type = "bnode" || "value_bnode"
    //    For LITERAL:
    //      theNode.valueType.type = "literal" ||
    //                               "language_literal" ||
    //                               "datatype_literal"
    //      theNode.valueType.language = <language>
    //      theNode.valueType.datatype = {}
    //      theNode.valueType.datatype.namespace = <dt_namespace>
    //      theNode.valueType.datatype.valueSource = {}
    //      theNode.valueType.datatype.valueSource.source = "constant"
    //      theNode.valueType.datatype.valueSource.constant = <xsd/other type>
    //
    //    theNode.valueSource = {}
    //    For CELL-based:
    //      INDEX:
    //        theNode.valueSource.source = "row_index" || "record_id"
    //      COLUMN:
    //        theNode.valueSource.source = "column"
    //        theNode.valueSource.columnName = <columnName>
    //      For RESOURCE or LITERAL:
    //        theNode.expression = {};
    //        theNode.expression.language = "grel"
    //        theNode.expression.code = <expression>
    //    For CONSTANT-based:
    //      theNode.valueSource.source = "constant";
    //      theNode.valueSource.constant = <constValue>;
    //
    //      INDEX Node --------| COLUMN Node -------| CONSTANT Node -----|
    //        row_index OR     |   column           |   constant         |
    //        record_id        |     columnName     |     constValue     |
    // RESOURCE                |                    |                    |
    //        iri              |   "                |   "                |
    //        expression       |   "                |                    |
    // LITERAL                 |                    |                    |
    //        literal          |   "                |   "                |
    //          none           |     "              |     "              |
    //        language_literal |   "                |   "                |
    //          language       |     "              |     "              |
    //        datatype_literal |   "                |   "                |
    //          constType      |     "              |     "              |
    //        expression       |   "                |                    |
    //  BLANK                  |                    |                    |
    //        bnode OR         |   "                |   "                |
    //        value_bnode      |                    |                    |
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

        theNode.valueType = {};
        theNode.valueSource = {};

        // All Resource Nodes...
        if ( this.#eType === RDFTransformCommon.NodeType.Resource ) {
            //  We don't need "iri" types for root nodes as they are common.
            //  Store for all others...
            if ( ! this.#bIsRoot ) {
                theNode.valueType.type = "iri"; // ...implied for root nodes if missing
            }
        }

        // All Blank Nodes...
        else if ( this.#eType === RDFTransformCommon.NodeType.Blank ) {
            if (this.#bIsVarNode) {
                theNode.valueType.type = "bnode";
            }
            else { // ...Constant...
                theNode.valueType.type = "value_bnode";
            }
        }

        // All Literal Nodes...
        else if ( this.#eType === RDFTransformCommon.NodeType.Literal ) {
            // Check for simple text literal...
            if ( $('#rdf-content-txt-radio').prop('checked') ) {
                theNode.valueType.type = "literal";
            }
            // Check for language literal...
            else if ( $('#rdf-content-lang-radio').prop('checked') ) {
                theNode.valueType.type = "language_literal";
                theNode.valueType.language = $('#rdf-content-lang-input').val();
            }
            // Check for datatype literal...
            else {
                theNode.valueType.type = "datatype_literal";
                theNode.valueType.datatype = {};
                theNode.valueType.datatype.valueSource = {};
                theNode.valueType.datatype.valueSource.source = "constant";
                // TODO: Expand from "constant" to allow "cell" datatypes with expressions...
                //if (dataType "expression" && "code" in "expression" ) {
                //    theNode.valueType.datatype.expression = {};
                //    theNode.valueType.datatype.expression.language = RDFTransform.g_strDefaultExpLang;
                //    theNode.valueType.datatype.expression.code = strExpCode;
                //}

                // Check for custom datatype literal...
                if ( $('#rdf-content-type-radio').prop('checked') ) {
                    // Check for custom dataType IRI value...
                    /** @type {string} */
                    // @ts-ignore
                    var strConstVal = $('#rdf-content-type-input').val();
                    if ( strConstVal !== null && strConstVal.length === 0 ) {
                        alert( $.i18n('rdft-dialog/alert-custom') );
                        return null;
                    }
                    // TODO: Extract namespace if present...
                    //theNode.valueType.datatype.namespace =
                    theNode.valueType.datatype.valueSource.constant = strConstVal;
                }
                // Otherwise, popular XSD datatype literal...
                else {
                    // TODO: Check for namespace "xsd" exists...
                    theNode.valueType.datatype.namespace = "xsd"; // http://www.w3.org/2001/XMLSchema#
                    if ( $('#rdf-content-int-radio').prop('checked') ) {
                        theNode.valueType.datatype.valueSource.constant = 'int';
                    }
                    else if ( $('#rdf-content-double-radio').prop('checked') ) {
                        theNode.valueType.datatype.valueSource.constant = 'double';
                    }
                    else if ( $('#rdf-content-date-radio').prop('checked') ) {
                        theNode.valueType.datatype.valueSource.constant = 'date';
                    }
                    else if ( $('#rdf-content-date-time-radio').prop('checked') ) {
                        theNode.valueType.datatype.valueSource.constant = 'dateTime';
                    }
                    else if ( $('#rdf-content-boolean-radio').prop('checked') ) {
                        theNode.valueType.datatype.valueSource.constant = 'boolean';
                    }
                    if ( ! ("constant" in theNode.valueType.datatype.valueSource) ) {
                        alert( $.i18n('rdft-data/internal-error') );
                        return null;
                    }
                }
            }
        }

        // All Cell-based Nodes (NOT Constant)...
        if (this.#bIsVarNode) {
            // Prepare for Row/Record or Column...
            /** @type {string} */
            // @ts-ignore
            const strColumnName = $("input[name='rdf-column-radio']:checked").val();
            if (strColumnName.length === 0) { // ...Row or Record Index...
                theNode.valueSource.source = RDFTransform.gstrValueSource;
            }
            else { // ...Column...
                theNode.valueSource.source = "column";
                theNode.valueSource.columnName = strColumnName;
            }

            // For Resource or Literal (NOT Blank) Nodes,
            //  Get the Expression...
            //      (Blank Nodes don't use Expressions)
            if (this.#eType !== RDFTransformCommon.NodeType.Blank) {
                // Set expression...
                var strExpCode = $('#rdf-cell-expr').text();
                if ( strExpCode === null || strExpCode.length === 0 ) {
                    alert( $.i18n('rdft-dialog/alert-enter-exp') );
                    return null;
                }
                //  We don't need "value" expressions as they are common.
                //  Store all others...
                if ( strExpCode !== "value" ) {
                    theNode.expression = {};
                    theNode.expression.language = RDFTransform.gstrDefaultExpLang;
                    theNode.expression.code = strExpCode;
                }
            }
        }

        // All Constant-based Nodes...
        else {
            // Set value...
            /** @type {string} */
            // @ts-ignore
            var strConstVal = $('#rdf-constant-value-input').val();
            if ( strConstVal.length === 0 ) {
                alert( $.i18n('rdft-dialog/alert-enter-const') );
                return null;
            }
            // ...get value...
            theNode.valueSource.source = "constant";
            theNode.valueSource.constant = strConstVal;
        }

        // All good...
        return theNode;
    }

    removeProperty(propertyUI) {
        var iPropertyIndex = this.#propertyUIs.lastIndexOf(propertyUI);
        if (iPropertyIndex >= 0) {
            this.#propertyUIs.splice(iPropertyIndex, 1);

            this.#render();
            this.#dialog.updatePreview();
        }
    }

    #addRDFType(element) {
        var theDialog =
            new RDFTransformResourceDialog(
                element, 'class', theProject.id, this.#dialog,
                (theCIRIE) => { this.#addNodeRDFType(theCIRIE); }
            );
        theDialog.show();
    }

    /**
     * @param {Object} theCIRIE
     * @param {string} theCIRIE.prefix
     * @param {string} theCIRIE.localPart
     */
    #addNodeRDFType(theCIRIE) {
        // A Condensed IRI Expression (CIRIE) is:
        //      a prefix for a namespace +
        //      the local part of an IRI (the Full IRI - the starting namespace)
        //
        //      CIRIE = strPrefix + ":" + strLocalPart
        //
        // In this case, the prefix may be missing which means the local part
        // is a Full IRI (not a CIRIE)

        // Check for BAD CIRIE
        if ( ! ( theCIRIE && "localPart" in theCIRIE ) ) {
            return;
        }
        if ( ! ( "typeMappings" in this.#node ) ) {
            this.#node.typeMappings = [];
        }

        var theType = {};
        if ( "prefix" in theCIRIE ) {
            theType.prefix = theCIRIE.prefix;
        }
        theType.valueSource = {};
        theType.valueSource.source = "constant";
        theType.valueSource.constant = theCIRIE.localPart;
        this.#node.typeMappings.push(theType);

        this.#render();
        this.#dialog.updatePreview();
    }

    #removeNodeRDFType(iIndex) {
        this.#node.typeMappings.splice(iIndex, 1);

        this.#render();
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

        var strPrefix = null;
        if ( "prefix" in this.#node.typeMappings[iIndex] ) {
            strPrefix = this.#node.typeMappings[iIndex].prefix;
        }
        var strLocalPart= this.#node.typeMappings[iIndex].valueSource.constant;
        var strText = "Full: " + strLocalPart; // ...default: display just the IRI (Full IRI)
        // If the prefix is present, display both...
        if (strPrefix) {
            strText = "CIRIE: " + strPrefix + ':' + strLocalPart;
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
        // Prefixed IRI (CIRIE)...
        if ("prefix" in theType && theType.prefix !== null) {
            return theType.prefix + ":" + theType.valueSource.constant;
        }
        // Full IRI (no prefix)...
        else if ("valueSource" in theType && theType.valueSource !== null) {
            return theType.valueSource.constant;
        }
        // Neither: Type exists but is empty...
        else {
            return "type?";
        }
    }

    getTransformExport() {
        var theNode = {};

        //
        // Prefix...
        //
        if ("prefix" in this.#node) {
            theNode.prefix = this.#node.prefix;
        }

        //
        // Value Type...
        //
        if ("valueType" in this.#node) {
            theNode.valueType = this.#node.valueType;
        }

        //
        // Value Source...
        //
        if ("valueSource" in this.#node) {
            theNode.valueSource = this.#node.valueSource;
        }

        //
        // Expressions...
        //
        if ( "expression" in this.#node ) {
            theNode.expression = this.#node.expression;
        }

        //
        // Resource and Blank Nodes (NOT Literal)...
        //
        if (this.#node.valueSource.type !== "literal" &&
            this.#node.valueSource.type !== "language_literal" &&
            this.#node.valueSource.type !== "datatype_literal")
        {
            //
            // Type Mappings...
            //
            if ("typeMappings" in this.#node && this.#node.typeMappings.length > 0) {
                //theNode.typeMappings = cloneDeep(this.#node.typeMappings);
                theNode.typeMappings = this.#node.typeMappings;
            }
            //
            // Property Mappings...
            //
            if (this.#propertyUIs && this.#propertyUIs.length > 0) {
                theNode.propertyMappings = [];
                for (const propertyUI of this.#propertyUIs) {
                    const property = propertyUI.getTransformExport();
                    if (property !== null) {
                        theNode.propertyMappings.push(property);
                    }
                }
            }
        }

        return theNode;
    }

    static getTransformImport(theDialog, theNode) {
        var node = {};

        //
        // Prefix...
        //
        if ("prefix" in theNode) {
            node.prefix = theNode.prefix;
        }

        //
        // Value Type...
        //
        if ("valueType" in theNode) {
            node.valueType = theNode.valueType;
        }

        //
        // Value Source...
        //
        if ("valueSource" in theNode) {
            node.valueSource = theNode.valueSource;
        }

        //
        // Expressions...
        //
        if ( "expression" in theNode ) {
            node.expression = theNode.expression;
        }

        //
        // Set up the NodeUI Store...
        //
        var nodeUI = new RDFTransformUINode(
            theDialog, // dialog
            node,
            true, // bIsRoot
            null, // ...process and set properties later
            { expanded : true }
        );

        //
        // Resource and Blank Nodes (NOT Literal)...
        //
        if (theNode.valueSource.type !== "literal" &&
            theNode.valueSource.type !== "language_literal" &&
            theNode.valueSource.type !== "datatype_literal")
        {
            //
            // Type Mappings...
            //
            if ("typeMappings" in theNode && theNode.typeMappings.length > 0) {
                //theNode.typeMappings = cloneDeep(theNode.typeMappings);
                node.typeMappings = theNode.typeMappings;
            }

            //
            // Property Mappings...
            //
            var propertyUIs = null;
            if ("propertyMappings" in theNode &&
                theNode.propertyMappings !== null &&
                theNode.propertyMappings.length > 0)
            {
                propertyUIs = [];
                for (const theProperty of theNode.propertyMappings) {
                    // Process the property for display...
                    var propertyUI = RDFTransformUIProperty.getTransformImport(theDialog, theProperty, nodeUI);
                    if (propertyUI !== null) {
                        propertyUIs.push(propertyUI);
                    }
                }
                nodeUI.setPropertyUIs(propertyUIs);
            }
        }

        return nodeUI;
    }
}
