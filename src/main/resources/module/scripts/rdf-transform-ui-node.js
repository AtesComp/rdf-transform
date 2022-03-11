/*
 *  CLASS RDFTransformUINode
 *
 *  The Node Manager UI for the RDF Transform Dialog
 */
class RDFTransformUINode {
    #dialog;
    #node;
    #bIsRoot;
    #propertyUIs;
    #bIsExpanded;
    #propUISubject;

    #bIsVarNode;
    #eType;

    #tableDetails;

    #tdMain;
    #tdToggle;
    #tdDetails;

    #imgExpand;
    #imgNone;

    #theNodeLabel;

    #collapsedDetailDiv;
    #expandedDetailDiv;

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
            // ...set it as the default node (clone default)...
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

        // Based on the node,
        //  1. Set the Variable vs Constant boolean
        //  2. Set the Node Enumeration Type: Resource, Blank, or Literal
        this.#initializeNodeTypes();

        this.#imgExpand =
            $('<img />')
            .attr("src", ModuleWirings[RDFTransform.KEY] +
                        (this.#bIsExpanded ? "images/collapse.png" : "images/expand.png"))
            .on("click",
                (evt) => {
                    this.#bIsExpanded = ! this.#bIsExpanded;
                    $(evt.currentTarget)
                    .attr("src", ModuleWirings[RDFTransform.KEY] +
                                (this.#bIsExpanded ? "images/collapse.png" : "images/expand.png"));
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

    isRootNode() {
        // Default to Non-Root Node...
        return ( this.#bIsRoot === null ? false : this.#bIsRoot );
    }

    isVariableNode() {
        // Default to Varible Node...
        return ( this.#bIsVarNode === null ? true : this.#bIsVarNode );
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
                    var imgDelete = $('<img />')
                        .attr("title", $.i18n('rdft-dialog/remove-type'))
                        .attr("src", ModuleWirings[RDFTransform.KEY] + "images/delete.png")
                        .css("cursor", "pointer")
                        .on("click",
                            () => {
                                this.#removeNodeRDFType(iLocalIndex);
                            }
                        );
                    $(td).append(imgDelete);

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

        if (bNodeLabel) {
            strNodeText = strNodeLabel + strNodeText;
        }

        // Add the Node Label as a Span reference since text contains an IRI and
        // can be interpreted as an IRL (HTML would render a link)...
        var ahref =
            $('<a href="javascript:{}" />')
                .addClass("rdf-transform-node")
                .on("click",
                    () => {
                        var confNodeUI = new RDFTransformUINodeConfig(this.#dialog, this, this.#eType);
                        confNodeUI.processView();
                    }
                );
        ahref.append(
            $("<span />")
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
        var menuViewType = MenuSystem.createMenu(); // ...size doesn't matter since we fit
        menuViewType.html(
'<div bind="rdftTypeContainer">' +
  '<span class="rdf-transform-iri-text" bind="rdftTypeText" style="overflow: hidden;" /></span>' +
  '<button class="button" bind="buttonOK">' + $.i18n('rdft-buttons/ok') + '</button>' +
'</div>'
        );

        MenuSystem.showMenu(menuViewType, () => {});
        MenuSystem.positionMenuLeftRight(menuViewType, target);

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
            var strNamespace = this.#dialog.getNamespacesManager().getNamespaceOfPrefix(strPrefix);
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

        //
        // Functionalize Elements...
        //

        var elements = DOM.bind(menuViewType);

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
        elements.rdftTypeText.trigger('change', [ elements.rdftTypeContainer, menuViewType ]);
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
                    // Default Property (clone default)...
                    var theProperty = JSON.parse( JSON.stringify(RDFTransformUINode.#propDefault) );
                    // Default Node for Property (clone default)...
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

    updateNode(theNode) {
        var theTypeMappings = null;
        // If Old Node has Type Mappings, retain them for the New Node...
        if (RDFTransform.gstrTypeMappings in this.#node) {
            theTypeMappings = this.#node.typeMappings;
        }
        // Property Mappings are reserved in #propertyUIs

        this.#node = theNode;
        if (theTypeMappings !== null) {
            this.#node.typeMappings = theTypeMappings;
        }

        this.#initializeNodeTypes(); // ...re-initialize for new node
        this.#render();
        this.#show();
        this.#dialog.updatePreview();
        if (this.#propUISubject != null) {
            this.#propUISubject.render();
        }
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
