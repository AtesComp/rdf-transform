/*
 *  CLASS RDFTransformUINode
 *
 *  The UI node manager for the RDF Transform dialog
 */
class RDFTransformUINode {
    #dialog;
    #node;
    #bIsRoot;
    #propertyUIs;
    #bIsExpanded;
    #propUISubject;

    #bIsVarNode;
    #bIsVarNodeConfig;
    #eType;

    #tableDetails;

    #tdMain;
    #tdToggle;
    #tdDetails;

    #imgExpand;
    #imgNone;

    #theNodeLabel;
    #level;

    #collapsedDetailDiv;
    #expandedDetailDiv;

    #checkedTrue;
    #disabledTrue;
    #disabledFalse;

    // Setup default Master Object Node (copy as needed)...
    static #nodeObjectDefault = {};
    static {
        this.#nodeObjectDefault.valueType = {};
        this.#nodeObjectDefault.valueType.type = RDFTransform.gstrLiteral;
        this.#nodeObjectDefault.valueSource = {};
        this.#nodeObjectDefault.valueSource.source = null; // ...hold's row / record index as default
    }

    // Setup default Master Property Edge (copy as needed)...
    static #propDefault = {};
    static {
        this.#propDefault.prefix    = null; // ...holds CIRIE Prefix (if used)
        this.#propDefault.localPart = null; // ...holds CIRIE LocalPart (or Full IRI)
        this.#propDefault.nodeObject = null; // ...hold's Object Node of Property
    }

    constructor(theDialog, theNode, bIsRoot, theProperties, bIsExpanded, theSubjectPropertyUI = null) {
        this.#dialog = theDialog;
        this.#node = theNode; // ...a Transform Node
        this.#bIsRoot = bIsRoot; // Root or Object Node
        this.#bIsExpanded = bIsExpanded;
        this.#propUISubject = theSubjectPropertyUI; // ...Subject's Property connected to this Object

        // If the Node is null...
        if (this.#node == null) {
            // ...set it as the default node...
            this.#node = JSON.parse( JSON.stringify(RDFTransformUINode.#nodeObjectDefault) );
        }

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
                    true,
                    this // ...Subject Node UI
                    // ...Object Node UI is set by theProperty's Object Node
                );
                this.#propertyUIs.push(propertyUI);
            }
        }

        this.#checkedTrue = { "checked" : true };
        this.#disabledTrue = { "disabled" : true };
        this.#disabledFalse = { "disabled" : false };

        // Based on the node,
        //  1. Set the Variable vs Constant boolean
        //  2. Set the Node Enumeration Type: Resource, Blank, or Literal
        this.#initializeNodeTypes();

        this.#imgExpand =
            $('<img />')
            .attr("src", this.#bIsExpanded ? "images/expanded.png" : "images/collapsed.png")
            .on("click",
                (evt) => {
                    this.#bIsExpanded = ! this.#bIsExpanded;
                    $(evt.currentTarget)
                    .attr("src", this.#bIsExpanded ? "images/expanded.png" : "images/collapsed.png");
                    this.#show();
                }
            );
        this.#imgNone = $('<img />');
    }

    //
    // Method #initializeNodeTypes()
    //
    //  From existing node on construction.  See #getResultJSON()
    //
    //  Get the Node Type information:
    //      1. Node Value Type: Variable (true) or Constant (false)
    //      2. Node RDF Type: "resource", "literal", or "blank"
    //  When the Node's RDF Type cannot be determined, return a failed indicator (false)
    //
    #initializeNodeTypes() {
        // Determine the Node's Value Type: Variable or Constant
        //      by testing the node's value source type:
        //      Variable == "row_index", "record_id", "column"
        //      Constant == "constant"
        this.#bIsVarNode = null;
        if (this.#node.valueSource.source !== null) {
            this.#bIsVarNode = (this.#node.valueSource.source !== RDFTransform.gstrConstant);
        }

        // Determine the Node's RDF Type: "resource", "literal", or "blank"...
        var strNodeType = null;
        if ( ! (RDFTransform.gstrValueType in this.#node) ||
            this.#node.valueType.type === RDFTransform.gstrIRI )
        {
            strNodeType = "resource";
        }
        else if (
            this.#node.valueType.type === RDFTransform.gstrLiteral ||
            this.#node.valueType.type === RDFTransform.gstrLanguageLiteral ||
            this.#node.valueType.type === RDFTransform.gstrDatatypeLiteral )
        {
            strNodeType = RDFTransform.gstrLiteral;
        }
        else if (
            this.#node.valueType.type === RDFTransform.gstrBNode ||
            this.#node.valueType.type === RDFTransform.gstrValueBNode )
        {
            strNodeType = "blank";
        }
        this.#eType = RDFTransformCommon.NodeType.getType(strNodeType);

        if ( this.#eType === null ) {
            alert( $.i18n('rdft-data/alert-RDF-type') );
            return false;
        }
        return true;
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
        var tr = theTable.insertRow(); // ...the node's "tr" is not removable, so don't preserve
        this.#tdMain    = tr.insertCell(); // 0
        this.#tdToggle  = tr.insertCell(); // 1
        this.#tdDetails = tr.insertCell(); // 2

        this.#collapsedDetailDiv =
            $('<div></div>')
            .addClass("padded");
        this.#expandedDetailDiv =
            $('<div></div>')
            .addClass("rdf-transform-detail-container");

        $(this.#tdMain)
            .addClass("rdf-transform-node-main")
            .addClass("padded");
        $(this.#tdToggle)
            .addClass("rdf-transform-node-toggle")
            .addClass("padded")
            .append(this.#imgExpand)
            .append(this.#imgNone);
        $(this.#tdDetails)
            .addClass("rdf-transform-node-details")
            .append(this.#collapsedDetailDiv)
            .append(this.#expandedDetailDiv);

        this.#render();

        this.#renderDetails(); // ...one time only

        this.#show();
    }

    #render() {
        this.#renderMain();
        if ( this.isExpandable() ) {
            this.#collapsedDetailDiv.html("...");
            this.#showExpandable();
        }
        else {
            this.#collapsedDetailDiv.html("");
            this.#hideExpandable();
        }
    }

    #renderMain() {
        var bExpandable = this.isExpandable();
        var strHTMLType = '';
        if ( bExpandable )
        {
            strHTMLType =
'<tr>' +
  '<td>' +
    '<table width="100%" class="rdf-transform-types-table" bind="rdftTypesTable">' +
      '<tr bind="rdftTypesTR">' +
        '<td bind="rdftTypesTD"></td>' +
      '</tr>' +
      '<tr bind="rdftAddTypeTR">' +
        '<td>' +
          '<div class="padded">' +
            '<a href="#" class="action" bind="rdftAddType">' + $.i18n('rdft-dialog/add-type') + '...' + '</a>' +
          '</div>' +
        '</td>' +
      '</tr>' +
    '</table>' +
  '</td>' +
'</tr>';
        }
        var strHTML = $(
'<table width="100%">' +
  '<tr>' +
    '<td bind="rdftNodeLabel"></td>' +
  '</tr>' +
  strHTMLType +
'</table>'
        );
        $(this.#tdMain).empty().append(strHTML);

        var elements = DOM.bind(strHTML);
        this.#theNodeLabel = elements.rdftNodeLabel;
        if (bExpandable) {
            /** @type {HTMLTableElement} */
            // @ts-ignore
            var typesTable = $('<table width="100%"></table>')[0];
            if (RDFTransform.gstrTypeMappings in this.#node && this.#node.typeMappings.length > 0) {
                // Create each type display with removal icon...
                for (var iIndex = 0; iIndex < this.#node.typeMappings.length; iIndex++) {
                    var tr = typesTable.insertRow();

                    // Set index for "onClick" callback handler...
                    // NOTE: Use "let" (not "var") to correct loop index scoping in
                    //       the "onClick" callback handler.
                    let iLocalIndex = iIndex;

                    var td = tr.insertCell();
                    var imgClose = $('<img />')
                        .attr("title", $.i18n('rdft-dialog/remove-type'))
                        .attr("src", "images/close.png")
                        .css("cursor", "pointer")
                        .on("click",
                            () => {
                                this.#removeNodeRDFType(iLocalIndex);
                            }
                        );
                    $(td).append(imgClose);

                    td = tr.insertCell();
                    $(td).append(
                        $('<a href="#" class="action"></a>')
                        .text(
                            RDFTransformCommon.shortenResource(
                                this.#getTypeName( this.#node.typeMappings[iIndex] )
                            )
                        )
                        .on("click",
                            (evt) => {
                                evt.preventDefault();
                                this.#renderNodeRDFType( $(evt.target), iLocalIndex );
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
            .on("click",
                (evt) => {
                    evt.preventDefault();
                    this.#addRDFType(evt.currentTarget);
                }
            );
        }

        // NOTE: The Node is a:
        //      "[Index] (I/B/L)", "ColName (I/B/L)", "ConstVal (I/B/L)",
        //      "Configure? (I/B/L)"
        //      "Resource?"
        //      "Constant value"
        //      "Value?"
        var strNodeLabel = "R"; //$.i18n('rdft-as/iri')[0];
        if (this.#eType === RDFTransformCommon.NodeType.Blank) {
            strNodeLabel = "B"; //$.i18n('rdft-as/blank')[0];
        }
        if (this.#eType === RDFTransformCommon.NodeType.Literal) {
            strNodeLabel = "L"; //$.i18n('rdft-as/literal')[0];
        }
        strNodeLabel = strNodeLabel + ": ";

        // When a Node is not well defined, defaults...
        var strNodeText = $.i18n("rdft-dialog/configure"); // Configure?
        var bNodeLabel = false;

        if (this.#bIsVarNode !== null) {
            //
            // CELL Nodes...
            //
            if (this.#bIsVarNode) {
                // If Definite Source: "row_index", "record_id", "column"...
                if (this.#node.valueSource.source !== null) {
                    // If a Blank Resource...
                    if (this.#eType === RDFTransformCommon.NodeType.Blank) {
                        strNodeText = $.i18n('rdft-as/blank');
                        bNodeLabel = true;
                    }
                    // Else If a Column-based Resource or Literal...
                    else if (this.#node.valueSource.source === RDFTransform.gstrColumn) {
                        strNodeText =
                            RDFTransformCommon.shortenLiteral(this.#node.valueSource.columnName);
                        bNodeLabel = true;
                    }
                    // Otherwise, an Index-based (row or record) Resource or Literal...
                    else {
                        strNodeText = "[" + $.i18n("rdft-dialog/index") + "]";
                        bNodeLabel = true;
                    }
                }
            }
            //
            // CONSTANT Nodes...
            //
            else {
                // Definite Source: "constant"...
                if (RDFTransform.gstrConstant in this.#node.valueSource) {
                    var strConst = this.#node.valueSource.constant;

                    if (this.#eType === RDFTransformCommon.NodeType.Resource) {
                        strNodeText = $.i18n('rdft-dialog/which-res');
                        if (strConst !== null) {
                            strNodeText = RDFTransformCommon.shortenResource(strConst);
                            bNodeLabel = true;
                        }
                    }
                    else if (this.#eType === RDFTransformCommon.NodeType.Blank) {
                        strNodeText = $.i18n('rdft-dialog/constant-val');
                        if (strConst !== null) {
                            // Even though it's a Resource, treat the name like a Literal...
                            strNodeText = RDFTransformCommon.shortenLiteral(strConst);
                            bNodeLabel = true;
                        }
                    }
                    else if (this.#eType === RDFTransformCommon.NodeType.Literal) {
                        strNodeText = $.i18n('rdft-dialog/what-val');
                        if (strConst !== null) {
                            strNodeText = RDFTransformCommon.shortenLiteral(strConst);
                            bNodeLabel = true;
                        }
                    }
                }
            }
        }

        if (! this.#bIsRoot && bNodeLabel) {
            strNodeText = strNodeLabel + strNodeText;
        }

        // Add the Node Label as a Span reference since text is an IRI and
        // can be interpreted as an IRL (HTML would render a link)...
        var ahref =
            $('<a href="javascript:{}"></a>')
                .addClass("rdf-transform-node")
                .on("click",
                    () => {
                        this.#renderNodeConfigDialog();
                    }
                );
        ahref.append(
            $("<span></span>")
                .addClass("rdf-transform-node-label")
                .text(strNodeText)
        );
        this.#theNodeLabel
            .empty()
            .append(ahref);

        //Types
        /*var aux_table = $('<table>').appendTo($(this.tdMain));
        aux_table.append($('<tr>').append(td));
        this.typesTd = $('<td>').width("250px").appendTo($('<tr>').appendTo(aux_table));
        this.renderTypes();*/
    }

    #renderNodeRDFType(target, iIndex) {
        var menu = MenuSystem.createMenu(); // ...size doesn't matter since we fit
        menu.html(
'<div bind="rdftTypeContainer">' +
  '<span class="rdf-transform-iri-text" bind="rdftTypeText" style="overflow: hidden;" /></span>' +
  '<button class="button" bind="buttonOK">' + $.i18n('rdft-buttons/ok') + '</button>' +
'</div>'
        );

        MenuSystem.showMenu(menu, () => {});
        MenuSystem.positionMenuLeftRight(menu, target);

        var strPrefix = null;
        if ( RDFTransform.gstrPrefix in this.#node.typeMappings[iIndex] ) {
            strPrefix = this.#node.typeMappings[iIndex].prefix;
        }
        var strLocalPart = this.#node.typeMappings[iIndex].valueSource.constant;
        const strFullLabel = "Full: ";
        const strCIRIELabel = "CIRIE: ";
        var strText;
        // If the prefix is present, display Full IRI and CIRIE...
        if (strPrefix) {
            var strNamespace = this.#dialog.namespacesManager.getNamespaceOfPrefix(strPrefix);
            if (strNamespace) { // Namespace exists...
                strText =
                    " " + strFullLabel + strNamespace + strLocalPart + "\n" +
                    strCIRIELabel + strPrefix + ":" + strLocalPart;
            }
            else { // Namespace is not identified with the Prefix...
                // ...then, the combined Prefix and Local Part is a Full IRI...
                strText = strFullLabel + strPrefix + ":" + strLocalPart;
            }
        }
        // Otherwise, the Local Part is the Full IRI...
        else {
            strText = strFullLabel + strLocalPart;
        }

        var elements = DOM.bind(menu);

        // Set the display text...
        elements.rdftTypeText.html('<pre>' + strText + '</pre>');

        // Resize to fit display text..
        elements.rdftTypeText
            .on('change',
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
        elements.buttonOK.on("click", () => { MenuSystem.dismissAll(); });
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

        //
        // Append "Add property..." to property list...
        //
        var refAddProp = $('<a href="javascript:{}"></a>')
            .addClass("action")
            .text( $.i18n('rdft-dialog/add-prop') + '...' )
            .on("click",
                () => {
                    // Default Property...
                    var theProperty = JSON.parse( JSON.stringify(RDFTransformUINode.#propDefault) );
                    // Default Node for Property...
                    var nodeObject = JSON.parse( JSON.stringify(RDFTransformUINode.#nodeObjectDefault) );
                    theProperty.nodeObject = nodeObject;

                    // Set up the Property UI...
                    var propertyUI = new RDFTransformUIProperty(
                        this.#dialog,
                        theProperty,
                        true,
                        this // ...Subject Node UI
                    );
                    this.#propertyUIs.push(propertyUI); // ...add a Property to this Node...
                    propertyUI.processView(this.#tableDetails[0]); // ...and view the new Property
                    // If a Subject has this Node as an Object AND
                    //      this Node has just added it's 1st Property...
                    if (this.#propUISubject !== null && this.#propertyUIs.length === 1) {
                        this.#propUISubject.render(); // ...update the Subject's Property view (expandable)
                    }
                }
            );
        var divFooter = $('<div></div>')
            .addClass("padded");
        divFooter.append(refAddProp);
        this.#expandedDetailDiv.append(divFooter);
    }

    #show() {
        if (this.#bIsExpanded) {
            this.#collapsedDetailDiv.hide();
            this.#expandedDetailDiv.show();
        }
        else {
            this.#collapsedDetailDiv.show();
            this.#expandedDetailDiv.hide();
        }
    }

    isExpandable() {
        return (this.#eType !== RDFTransformCommon.NodeType.Literal);
    }

    #showExpandable() {
        //$(this.#tdToggle).empty().append(this.#imgExpand);
        //$(this.#tdToggle).show();
        $(this.#imgExpand).show();
        $(this.#imgNone).hide();
        $(this.#tdDetails).show();
    }

    #hideExpandable() {
        //$(this.#tdToggle).empty().append(this.#imgNone);
        //$(this.#tdToggle).hide();
        $(this.#imgExpand).hide();
        $(this.#imgNone).show();
        $(this.#tdDetails).hide();
    }

    removeProperty(propertyUI) {
        // Get last matching Property...
        var iPropertyIndex = this.#propertyUIs.lastIndexOf(propertyUI);
        // If found...
        if (iPropertyIndex >= 0) {
            this.#propertyUIs.splice(iPropertyIndex, 1); // ...remove Property from this Node...
            this.#render(); // ...and update the Node's view
            // If a Subject has this Node as an Object AND
            //      this Node has just removed it's last Property...
            if (this.#propUISubject !== null && this.#propertyUIs.length === 0) {
                this.#propUISubject.render();
            }
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
        if ( ! ( theCIRIE && RDFTransform.gstrLocalPart in theCIRIE ) ) {
            return;
        }
        if ( ! ( RDFTransform.gstrTypeMappings in this.#node ) ) {
            this.#node.typeMappings = [];
        }

        var theType = {};
        if ( RDFTransform.gstrPrefix in theCIRIE ) {
            theType.prefix = theCIRIE.prefix;
        }
        theType.valueSource = {};
        theType.valueSource.source = RDFTransform.gstrConstant;
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

    #getTypeName(theType) {
        if ( ! theType ) {
            return "<ERROR: No Type!>";
        }
        if (RDFTransform.gstrValueSource in theType && theType.valueSource !== null) {
            // Prefixed IRI (CIRIE)...
            if (RDFTransform.gstrPrefix in theType && theType.prefix !== null) {
                return theType.prefix + ":" + theType.valueSource.constant;
            }
            // Full IRI (no prefix)...
            return theType.valueSource.constant;
        }
        else { // Type exists but doesn't have "the juice"...
            return "type?";
        }
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
            .on("click",
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
            .on("click",
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
            .on("click",
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
            ( (RDFTransform.gstrConstant in this.#node.valueSource &&
               this.#node.valueSource.constant !== null) ? this.#node.valueSource.constant : '');
        var tdInput = $('<input />')
            .attr("id", "rdf-constant-value-input")
            .attr("type", "text").val(strConstVal)
            .attr("size", "25")
            .prop("disabled", ! isChecked);
        $(td).append(tdInput);
    }

    #initInputs(elements) {
        //
        // Set Prefix Selections...
        //
        const strOption = '<option value="{V}">{T}</option>';
        elements.rdf_prefix_select
            .append(
                strOption
                    .replace(/{V}/, '')
                    .replace(/{T}/, $.i18n('rdft-dialog/choose-none') ) );
        elements.rdf_prefix_select
            .append(
                strOption
                    .replace(/{V}/, ':')
                    .replace(/{T}/, ': (' + $.i18n('rdft-dialog/base-iri') + ')') );
        const theNamespaces = this.#dialog.namespacesManager.getNamespaces();
        if (theNamespaces != null) {
            for (const strPrefix in theNamespaces) {
                // const theNamespace = theNamespaces[strPrefix]);
                elements.rdf_prefix_select
                    .append( strOption.replace(/{V}/, strPrefix + ':').replace(/{T}/, strPrefix) );
            }
        }
        // Set the prefix selection if it matches an existing prefix...
        elements.rdf_prefix_select.find("option:selected").prop("selected", false);
        elements.rdf_prefix_select.find("option:first").prop("selected", "selected"); // ...default: select "Choose / None"
        if (RDFTransform.gstrPrefix in this.#node && this.#node.prefix !== null) {
            const strPrefix = this.#node.prefix + ':';
            const selOptions = elements.rdf_prefix_select.find("option");
            const iLen = selOptions.length;
            for (var iIndex = 0; iIndex < iLen; iIndex++) {
                if (selOptions[iIndex].value === strPrefix) {
                    elements.rdf_prefix_select.prop('selectedIndex', iIndex);
                    break;
                }
            }
        }

        // Disable Language and Custom Data Type inputs...
        elements.rdf_content_lang_input
            .add(elements.rdf_content_type_input)
            .prop(this.#disabledTrue);

        //
        // Set initial values and property settings...
        //
        if ( this.#eType === RDFTransformCommon.NodeType.Resource ) {
            // Enable Prefix Select...
            elements.rdf_prefix.find("*").prop(this.#disabledFalse);

            //
            // Resource node...
            //
            elements.rdf_content_iri_radio.prop(this.#checkedTrue);
        }
        else if ( this.#eType === RDFTransformCommon.NodeType.Literal ) {
            // Disable Prefix Select...
            elements.rdf_prefix.find("*").prop(this.#disabledTrue);

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
                    elements.rdf_content_dtype_radio.prop(this.#checkedTrue);
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
            // Disable Prefix Select...
            elements.rdf_prefix.find("*").prop(this.#disabledTrue);

            //
            // Blank node...
            //
            elements.rdf_content_blank_radio.prop(this.#checkedTrue);
        }

        // Set cell expression...
        // TODO: Future code language.  It's all "grel" currently.
        var strExpCode = RDFTransform.gstrDefaultExpCode; // ...default expression
        if (RDFTransform.gstrExpression in this.#node && "code" in this.#node.expression ) {
            strExpCode = this.#node.expression.code;
        }
        elements.rdf_cell_expr.empty().text(strExpCode);

        //
        // Click Events...
        //

        // Prefix...
        elements.rdf_prefix_select
        .on("change",
            () => {

            }
        );

        // Resource Content radio...
        elements.rdf_content_iri_radio
        .on("click",
            () => {
                elements.rdf_prefix.find("*").prop(this.#disabledFalse);
                elements.rdf_content_lang_input
                .add(elements.rdf_content_type_input)
                .prop(this.#disabledTrue);
            }
        );

        // All Literal Content radios and Blank Content radio...
        elements.rdf_content_txt_radio
        .add(elements.rdf_content_int_radio)
        .add(elements.rdf_content_double_radio)
        .add(elements.rdf_content_date_radio)
        .add(elements.rdf_content_date_time_radio)
        .add(elements.rdf_content_boolean_radio)
        .add(elements.rdf_content_blank_radio)
        .on("click",
            () => {
                elements.rdf_prefix.find("*").prop(this.#disabledTrue);
                elements.rdf_content_lang_input
                .add(elements.rdf_content_type_input)
                .prop(this.#disabledTrue);
            }
        );

        // Content radio Language...
        elements.rdf_content_lang_radio
        .on("click",
            () => {
                elements.rdf_prefix.find("*").prop(this.#disabledTrue);
                elements.rdf_content_lang_input.prop(this.#disabledFalse);
                elements.rdf_content_type_input.prop(this.#disabledTrue);
            }
        );

        // Content radio Custom Data Type...
        elements.rdf_content_dtype_radio
        .on("click",
            () => {
                elements.rdf_prefix.find("*").prop(this.#disabledTrue);
                elements.rdf_content_lang_input.prop(this.#disabledTrue);
                elements.rdf_content_type_input.prop(this.#disabledFalse);
            }
        );

        //
        // Expression Edit & Preview...
        //
        elements.expEditPreview
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
                    const strColumnName = $("input[name='rdf-column-radio']:checked").val();
                    const strExpression = $("#rdf-cell-expr").text();
                    const bIsResource = ( this.#eType === RDFTransformCommon.NodeType.Resource );
                    var strPrefix = null;
                    if (bIsResource) {
                        var selPrefix = elements.rdf_prefix_select;
                        strPrefix = selPrefix.val();
                    }
                    if ( this.#eType === RDFTransformCommon.NodeType.Blank ) {
                        // Blank (not much to do)...
                        alert( $.i18n('rdft-dialog/alert-blank') );
                    }
                    else { // Expression preview...
                        this.#expressionEditAndPreview(strColumnName, strExpression, bIsResource, strPrefix);
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

        //
        // View Management...
        //
        if (this.#bIsRoot) {
            // Root nodes can only be resources, so we only allow resource elements...
            elements.rdf_content_txt_radio
            .add(elements.rdf_content_int_radio)
            .add(elements.rdf_content_double_radio)
            .add(elements.rdf_content_date_radio)
            .add(elements.rdf_content_date_time_radio)
            .add(elements.rdf_content_boolean_radio)
            .add(elements.rdf_content_lang_radio)
            .add(elements.rdf_content_dtype_radio)
            .prop(this.#disabledTrue);
            // ...and never turn them on!
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
        this.#bIsVarNodeConfig = ! ( $("#rdf-constant-value-radio").is(':checked') );

        // Determine the Node's RDF Type: "resource", "literal", or "blank"...
        const strNodeType = $("input[name='rdf-content-radio']:checked").val();
        this.#eType = RDFTransformCommon.NodeType.getType(strNodeType);

        if ( this.#eType === null ) {
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
        const onDone = (strExp) => {
            if (strExp !== null) {
                strExp = strExp.substring(5); // ...remove "grel:"
            }
            $("#rdf-cell-expr").empty().text(strExp);
        };

        // Data Preview: Resource or Literal...
        const dialogDataTable = new RDFDataTableView( this.#dialog.getBaseIRI(), bIsResource, strPrefix );
        dialogDataTable.preview(objColumn, strExpression, bIsIndex, onDone);
    }

    #renderNodeConfigDialog() {
        this.#bIsVarNodeConfig = // ...default to Varible Node...
            ( this.#bIsVarNode === null ? true : this.#bIsVarNode );

        if (theProject.columnModel.columns.length < 1)
            return;

        var frame = DialogSystem.createDialog();

        frame
        .css({  minWidth: "500px",
                width: "500px",
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
        elements.prefixSelect.text( $.i18n('rdft-prefix/prefix') + ": " );

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

        elements.columnLeft.append($('<table></table>')[0]);
        var tableColumns = $('<table></table>')[0];
        elements.columnLeft.append(tableColumns)

        //
        // Add Row/Record Radio Row...
        //
        // NOTE: Always ResourceNode
        this.#buildIndexChoice(tableColumns, this.#bIsVarNodeConfig);

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

        //
        // Add Constant Value Radio Row...
        //
        // NOTE: A ResourceNode OR A LiteralNode
        this.#buildConstantChoice(tableColumns, ! this.#bIsVarNodeConfig);

        // Initilize inputs...
        this.#initInputs(elements);

        body.append(html);

        /*--------------------------------------------------
         * Footer
         *--------------------------------------------------
         */

         var footer = $('<div></div>').addClass("dialog-footer");

         var buttonOK =
            $('<button />')
            .addClass('button')
            .html( $.i18n('rdft-buttons/ok') )
            .on("click",
                () => {
                    var node = this.#getResultJSON();
                    if (node !== null) {
                        // If Old Node has Type Mappings, move them to New Node
                        if (RDFTransform.gstrTypeMappings in this.#node) {
                            // Copy existing types to new node...
                            //node.typeMappings = cloneDeep(this.#node.typeMappings);
                            node.typeMappings = this.#node.typeMappings;
                        }
                        // Property Mappings are reserved in #propertyUIs

                        this.#node = node;
                        this.#initializeNodeTypes(); // ...re-initialize for new node
                        DialogSystem.dismissUntil(this.#level - 1);
                        this.#render();
                        this.#show();
                        this.#dialog.updatePreview();
                        if (this.#propUISubject != null) {
                            this.#propUISubject.render();
                        }

                    }
                }
            );

        var buttonCancel =
            $('<button />')
            .addClass('button')
            .text( $.i18n('rdft-buttons/cancel') )
            .on("click",
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
            if ( $('#rdf-content-txt-radio').prop('checked') ) {
                theNode.valueType.type = RDFTransform.gstrLiteral;
            }
            // Check for language literal...
            else if ( $('#rdf-content-lang-radio').prop('checked') ) {
                theNode.valueType.type = RDFTransform.gstrLanguageLiteral;
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
                if ( $('#rdf-content-type-radio').prop('checked') ) {
                    // Check for custom dataType IRI value...
                    // @ts-ignore
                    strConstVal = $('#rdf-content-type-input').val();
                    if ( strConstVal !== null && strConstVal.length === 0 ) {
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
                    if ( ! (RDFTransform.gstrConstant in theNode.valueType.datatype.valueSource) ) {
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
            // @ts-ignore
            strConstVal = $('#rdf-constant-value-input').val();
            if ( strConstVal.length === 0 ) {
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

    getTransformExport() {
        // If the Node is unconfigured...
        if ( ! (RDFTransform.gstrValueSource in this.#node) ||
            this.#node.valueSource.source === null)
        {
            return null;
        }

        /** @type {{
         *      prefix?: string,
         *      valueType?: {
         *          type: string,
         *          language: string,
         *          datatype: {
         *              prefix: string,
         *              valueSource: { source: string, constant: string } } },
         *      valueSource?: { source: string, constant?: string, columnName?: string },
         *      expression?: { language: string, code: string },
         *      typeMappings?: {},
         *      propertyMappings?: [ ...any ]
         *  }}
         */
         var theNode = {};

         // Prefix...
        if (RDFTransform.gstrPrefix in this.#node) {
            theNode.prefix = this.#node.prefix;
        }

        // Value Type...
        if (RDFTransform.gstrValueType in this.#node) {
            theNode.valueType = this.#node.valueType;
        }

        // Value Source (we know it's configured)...
        if (RDFTransform.gstrValueSource in this.#node) {
            theNode.valueSource = this.#node.valueSource;
        }

        // Expressions...
        if ( RDFTransform.gstrExpression in this.#node ) {
            theNode.expression = this.#node.expression;
        }

        // Resource and Blank Nodes (NOT Literal)...
        if ( ! (RDFTransform.gstrValueType in this.#node) || // ...a Root Node
            // ...an Object Node & not a Literal type...
            (   this.#node.valueType.type !== RDFTransform.gstrLiteral &&
                this.#node.valueType.type !== RDFTransform.gstrLanguageLiteral &&
                this.#node.valueType.type !== RDFTransform.gstrDatatypeLiteral ) )
        {
            // Type Mappings...
            if (RDFTransform.gstrTypeMappings in this.#node &&
                this.#node.typeMappings.length > 0)
            {
                //theNode.typeMappings = cloneDeep(this.#node.typeMappings);
                theNode.typeMappings = this.#node.typeMappings;
            }

            // Property Mappings...
            if (this.#propertyUIs && this.#propertyUIs.length > 0) {
                theNode.propertyMappings = [];
                for (const propertyUI of this.#propertyUIs) {
                    const theProperty = propertyUI.getTransformExport();
                    if (theProperty !== null) {
                        theNode.propertyMappings.push(theProperty);
                    }
                }
            }
        }

        if ( theNode === {} ) {
            return null;
        }
        return theNode;
    }

    static getTransformImport(theDialog, theJSONNode, bIsRoot = true, theSubjectPropertyUI = null) {
        /** @type {{
         *      prefix?: string,
         *      valueType?: {
         *          type: string,
         *          language: string,
         *          datatype: {
         *              prefix: string,
         *              valueSource: { source: string, constant: string } } },
         *      valueSource?: { source: string, constant?: string, columnName?: string },
         *      expression?: { language: string, code: string },
         *      typeMappings?: {},
         *      propertyMappings?: [ ...any ]
         *  }}
         */
         var theNode = null;

        // If the JSON Node represents an unconfigured node, set it to the default node...
        if ( ! (RDFTransform.gstrValueSource in theJSONNode) ) {
            theNode = JSON.parse(JSON.stringify(RDFTransformUINode.#nodeObjectDefault)); // ...default node
        }
        else {
            theNode = {};

            // Prefix...
            if (RDFTransform.gstrPrefix in theJSONNode) {
                theNode.prefix = theJSONNode.prefix;
            }

            // Value Type...
            if (RDFTransform.gstrValueType in theJSONNode) {
                theNode.valueType = theJSONNode.valueType;
            }

            // Value Source (we know exists)...
            theNode.valueSource = theJSONNode.valueSource;

            // Expressions...
            if ( RDFTransform.gstrExpression in theJSONNode ) {
                theNode.expression = theJSONNode.expression;
            }
        }

        // Set up the Node UI...
        var theNodeUI = new RDFTransformUINode(
            theDialog,
            theNode,
            bIsRoot, // ...a Root or Object Node
            null, // ...process and set properties later
            true,
            theSubjectPropertyUI // ...for an Object Node
        );

        // Resource and Blank Nodes (NOT Literal)...
        if ( ! (RDFTransform.gstrValueType in theJSONNode) || // ...a Root Node
            (   // ...an Object Node & not a Literal type
                theJSONNode.valueType.type !== RDFTransform.gstrLiteral &&
                theJSONNode.valueType.type !== RDFTransform.gstrLanguageLiteral &&
                theJSONNode.valueType.type !== RDFTransform.gstrDatatypeLiteral ) )
        {
            // Type Mappings...
            if (RDFTransform.gstrTypeMappings in theJSONNode &&
                theJSONNode.typeMappings.length > 0)
            {
                //theNode.typeMappings = cloneDeep(theNode.typeMappings);
                theNode.typeMappings = theJSONNode.typeMappings;
            }

            // Property Mappings...
            var propertyUIs = null;
            if (RDFTransform.gstrPropertyMappings in theJSONNode &&
                theJSONNode.propertyMappings !== null &&
                theJSONNode.propertyMappings.length > 0)
            {
                propertyUIs = [];
                for (const theJSONProperty of theJSONNode.propertyMappings) {
                    // Process the property for display...
                    var propertyUI =
                        RDFTransformUIProperty.getTransformImport(theDialog, theJSONProperty, theNodeUI);
                    if (propertyUI !== null) {
                        propertyUIs.push(propertyUI);
                    }
                }
                theNodeUI.setPropertyUIs(propertyUIs);
            }
        }

        return theNodeUI;
    }
}
