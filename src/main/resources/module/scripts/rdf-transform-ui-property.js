/*
 *  CLASS RDFTransformUIProperty
 *
 *  The Property Manager UI for the RDF Transform Dialog
 */
class RDFTransformUIProperty {
    #dialog;
    #property;
    #bIsExpanded;
    #nodeUIs;
    #nodeUISubject;

    #tableDetails;

    #tr;
    #tdMain;
    #tdToggle;
    #tdDetails;

    #imgExpand;
    #imgNone;
    #imgDeleteProp;
    #imgArrowStart;
    #imgArrowEnd;

    #collapsedDetailDiv;
    #expandedDetailDiv;

    // Setup default Master Object Property (copy as needed)...
    static #propObjectDefault = {};
    static {
        this.#propObjectDefault.prefix    = null; // ...holds CIRIE Prefix (if used)
        this.#propObjectDefault.localPart = null; // ...holds CIRIE LocalPart (or Full IRI)
        this.#propObjectDefault.theNodes = null; // ...hold's Object Nodes of Property
    }

    static getDefaultProperty(){
        return JSON.parse( JSON.stringify(RDFTransformUIProperty.#propObjectDefault) );
    }

    constructor(theDialog, theProperty, bIsExpanded, theSubjectNodeUI) {
        this.#dialog = theDialog;
        this.#property = theProperty; // ...contains CIRIE (prefix, localPart) and Transform Node List (theNodes)
        this.#bIsExpanded = bIsExpanded;
        this.#nodeUISubject = theSubjectNodeUI;

        // If the Property is null...
        if (this.#property == null) {
            // ...set it as the default property (clone default)...
            this.#property = RDFTransformUIProperty.getDefaultProperty();
        }

        //
        // Properties will always have Object Nodes, even if a default.
        //
        // NOTES:
        //      A property with a valid Object Node and null Object Node UI will create an
        //      Object Node UI from the given Property's Object Node.
        //      A Property with a null Object node indicates a valid Object Node UI will be
        //      given later containing a valid Object Node to set in the Property.

        //
        // Process any Properties for the Object Nodes...
        //
        //      Nodes optionally have Properties.
        //
        this.#nodeUIs = null;

        if (this.#property.theNodes !== null) {
            this.#nodeUIs = [];
            // ...create the Nodes' Object Node UIs and process their Property Mappings...
            for (const theNode of this.#property.theNodes) {
                this.#processObjectNodePropertyMappings(null, theNode);
            }
        }
        // Otherwise, expect the Property's Object Nodes and Object Node UIs will be set later
        // by the setNodeUIs() method.

        this.#imgExpand =
            $('<img />')
            .attr("src", ModuleWirings[RDFTransform.KEY] +
                        (this.#bIsExpanded ? "images/collapse.png" : "images/expand.png"))
            .on("click",
                (evt) => {
                    this.#bIsExpanded = !this.#bIsExpanded;
                    $(evt.currentTarget)
                        .attr("src", ModuleWirings[RDFTransform.KEY] +
                                (this.#bIsExpanded ? "images/collapse.png" : "images/expand.png"));
                    this.#show();
                }
            );
        this.#imgNone = $('<img />');
        this.#imgDeleteProp = $('<img />')
            .attr("title", $.i18n('rdft-dialog/remove-property'))
            .attr("src", ModuleWirings[RDFTransform.KEY] + "images/delete.png")
            .css("cursor", "pointer")
            .on("click",
                () => {
                    setTimeout(
                        () => {
                            this.#tr.remove(); // ...first
                            this.#nodeUISubject.removeProperty(this); // ...second, for view update
                        },
                        100
                    );
                }
            );
        this.#imgArrowStart = $('<img />').attr("src", "images/arrow-start.png");
        this.#imgArrowEnd = $('<img />').attr("src", "images/arrow-end.png");
    }

    #processObjectNodePropertyMappings(theNodeUI, theNode) {
        var bIsExpanded = false; // ...no presumed Object Node Property Mappings to process.

        // If Property Mappings exist (it's also a Resource Node)...
        if (RDFTransform.gstrPropertyMappings in theNode &&
            theNode.propertyMappings !== null &&
            theNode.propertyMappings.length > 0)
        {
            // ...prepare to process Object Node Property Mappings...
            bIsExpanded = true;
        }

        // If we need to create a Node UI...
        if (theNodeUI === null) {
            // Create a Node UI containing the Object Node...
            theNodeUI = new RDFTransformUINode(
                this.#dialog,
                theNode,
                false, // ...an Object Node (not Root)
                null, // ...process and set properties below if expanded
                bIsExpanded,
                this // ...Object Nodes require a Subject Property UI
            );
            this.#nodeUIs.push(theNodeUI);
        }

        // If we need to process Object Node Property Mappings (for a Resource)...
        if (bIsExpanded) {
            // Process the related Property UIs...
            var theProperties = [];
            for (const theJSONProperty of theNode.propertyMappings) {
                // Process the property for display...
                var propertyUI =
                    RDFTransformUIProperty.getTransformImport(this.#dialog, theJSONProperty, theNodeUI);
                if (propertyUI !== null) {
                    theProperties.push(propertyUI);
                }
            }
            // ...and set the Property UIs for the Node UI...
            theNodeUI.setPropertyUIs(theProperties);
        }
        // Otherwise, the Object Node is a Resource without Property Mappings or it's a Literal

        // The Node UIs will have their processView() called when this Property
        // has it's processView() call #renderDetails().
        return theNodeUI;
    }

    getProperty() {
        return this.#property;
    }

    setNodeUIs(theNodeUIs) {
        this.#nodeUIs = theNodeUIs;

        // Set the Property's Object Nodes from the Object Node UIs...
        this.#property.theNodes = [];
        for (const theNodeUI of this.#nodeUIs) {
            var theNode = theNodeUI.getNode();
            this.#property.theNodes.push(theNode);
            // Process the Property's Object Node Property Mappings...
            this.#processObjectNodePropertyMappings(theNodeUI, theNode);
        }
    }

    processView(theTable) {
        this.#tr = theTable.insertRow(); // ...the preperty's "tr" is removable, so preserve
        this.#tdMain    = this.#tr.insertCell(); // 0
        this.#tdToggle  = this.#tr.insertCell(); // 1
        this.#tdDetails = this.#tr.insertCell(); // 2

        this.#collapsedDetailDiv = $('<div />').addClass("padded");
        this.#expandedDetailDiv = $('<div />').addClass("rdf-transform-node-container");

        $(this.#tdMain)
            .addClass("rdf-transform-property-main")
            .addClass("padded");
        $(this.#tdToggle)
            .addClass("rdf-transform-property-toggle")
            .addClass("padded")
            .append(this.#imgExpand)
            .append(this.#imgNone);
        $(this.#tdDetails)
            .addClass("rdf-transform-property-details")
            .append(this.#collapsedDetailDiv)
            .append(this.#expandedDetailDiv);

        this.render();

        this.#renderDetails(); // ...one time only

        this.#show();
    }

    render() {
        this.#renderMain();
        if ( this.#isExpandable() ) {
            this.#collapsedDetailDiv.html("...");
            this.#showExpandable();
        }
        else {
            this.#collapsedDetailDiv.html("");
            this.#hideExpandable();
        }
    }

    #renderMain() {
        var refProperty =
            $('<a href="javascript:{}" />')
                .addClass("rdf-transform-property")
                .on("click", (evt) => { this.#editProperty(evt.target); } );
        refProperty.append(
            $("<span />")
                .addClass("rdf-transform-property-label")
                .text( RDFTransformCommon.shortenResource( this.#getPropertyName(this.#property) ) )
        );

        $(this.#tdMain)
            .empty()
            .append(this.#imgDeleteProp, this.#imgArrowStart, refProperty, this.#imgArrowEnd);
    }

    #renderDetails() {
        if (this.#tableDetails) {
            this.#tableDetails.remove();
        }
        this.#tableDetails = $('<table />').addClass("rdf-transform-node-table-layout");
        this.#expandedDetailDiv.append(this.#tableDetails);

        if (this.#nodeUIs !== null && this.#nodeUIs.length > 0) {
            for (const theNodeUI of this.#nodeUIs) {
                theNodeUI.processView(this.#tableDetails[0]);
            }
        }

        //
        // Append "Add object..." to node list...
        //
        var refAddObjNode = $('<a href="javascript:{}" />')
            .addClass("action")
            .text( $.i18n('rdft-dialog/add-object') + '...' )
            .on("click",
                () => {
                    if (this.#nodeUIs === null) {
                        this.#nodeUIs = [];
                    }

                    // Default Object Node (clone default)...
                    var theNode = RDFTransformUINode.getDefaultNode();

                    // Set up the Node UI...
                    var theNodeUI = this.#processObjectNodePropertyMappings(null, theNode);
                    theNodeUI.processView(this.#tableDetails[0]); // ...and view the new Node
                    // If a Subject has this Property AND
                    //      this Property has just added it's 1st Node...
                    if (this.#nodeUISubject !== null && this.#nodeUIs.length === 1) {
                        this.#nodeUISubject.render(); // ...update the Subject's Node view (expandable)
                    }
                }
            );
        var divNodeDetail = $('<div />').addClass("padded");
        divNodeDetail.append(refAddObjNode);
        this.#expandedDetailDiv.append(divNodeDetail);

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

    #isExpandable() {
        return (this.#nodeUIs !== null && this.#nodeUIs.length > 0);
    }

    #showExpandable() {
        $(this.#imgExpand).show();
        $(this.#imgNone).hide();
    }

    #hideExpandable() {
        $(this.#imgExpand).hide();
        $(this.#imgNone).show();
    }

    removeNode(nodeUI) {
        // Get last matching Node...
        var iNodeIndex = this.#nodeUIs.lastIndexOf(nodeUI);
        // If found...
        if (iNodeIndex >= 0) {
            this.#nodeUIs.splice(iNodeIndex, 1); // ...remove Node from this Property...
            var bRenderSubjectNode = false;
            if (this.#nodeUIs.length === 0) {
                // Default Object Node (clone default)...
                var theNode = RDFTransformUINode.getDefaultNode();

                // Set up the Node UI...
                var theNodeUI = this.#processObjectNodePropertyMappings(null, theNode);
                theNodeUI.processView(this.#tableDetails[0]); // ...and view the new Node
                bRenderSubjectNode = true;
            }
            this.render(); // ...and update the Property's view
            // If a Subject Node has this Property AND
            //      this Property has just removed it's last Node Object...
            if (this.#nodeUISubject !== null && bRenderSubjectNode) {
                this.#nodeUISubject.render();
            }
            this.#dialog.updatePreview();
        }
    }

    #getPropertyName(theProperty) {
        if (! theProperty ) {
            return "<ERROR: No Property!>";
        }
        if (RDFTransform.gstrLocalPart in theProperty && theProperty.localPart !== null) {
            // Prefixed IRI (CIRIE)...
            if (RDFTransform.gstrPrefix in theProperty && theProperty.prefix !== null) {
                return theProperty.prefix + ":" + theProperty.localPart;
            }
            // Full IRI (no prefix)...
            return theProperty.localPart;
        }
        else { // Property exists but doesn't have "the juice"...
            return "Property?";
        }
    }

    #editProperty(element) {
        var theDialog =
            new RDFTransformResourceDialog(
                element, 'property', theProject.id, this.#dialog,
                (theProperty) => { this.#editPropertyInfo(theProperty); }
            )
        theDialog.show();
    }

    #editPropertyInfo(theProperty) {
        this.#property.prefix = null;
        if (RDFTransform.gstrPrefix in theProperty && theProperty.prefix !== null) {
            this.#property.prefix = theProperty.prefix;
        }
        this.#property.localPart = null;
        if (RDFTransform.gstrLocalPart in theProperty && theProperty.localPart !== null) {
            // Full or LocalPart (preprocessed)...
            this.#property.localPart = theProperty.localPart;
        }
        this.render();
        this.#dialog.updatePreview();
    }

    getTransformExport() {
        if ( ! (RDFTransform.gstrLocalPart in this.#property) || this.#property.localPart === null) {
            return null;
        }

        /** @type {{prefix?: string, valueSource?: {source?: string, constant?: string}, objectMappings?: any[]}} */
        var theProperty = {};

        if (RDFTransform.gstrPrefix in this.#property && this.#property.prefix !== null) {
            theProperty.prefix = this.#property.prefix;
        }

        // For properties, "iri" valueType is implied, so the following is NOT needed:
        //theProperty.valueType = {};
        //theProperty.valueType.type = RDFTransform.gstrIRI;

        // TODO: Currently, all properties are "constant".  Change to allow
        //      column with expression.
        theProperty.valueSource = {};
        theProperty.valueSource.source = RDFTransform.gstrConstant;
        theProperty.valueSource.constant = this.#property.localPart;

        if (this.#nodeUIs !== null) {
            for (const theNodeUI of this.#nodeUIs) {
                var nodeObjectJSON = theNodeUI.getTransformExport();
                if (nodeObjectJSON !== null) {
                    theProperty.objectMappings = [];
                    theProperty.objectMappings.push(nodeObjectJSON);
                }
            }
        }

        return theProperty;
    }

    static getTransformImport(theDialog, theJSONProperty, theSubjectNodeUI) {
        if (theJSONProperty === null) {
            return null;
        }

        //
        // Prepare theProperty for the Property UI...
        //
        var theProperty = {};
        theProperty.prefix = null;
        if (RDFTransform.gstrPrefix in theJSONProperty && theJSONProperty.prefix !== null) {
            theProperty.prefix = theJSONProperty.prefix;
        }
        // TODO: Currently, all properties are "constant".  Change to allow
        //      column with expression.
        theProperty.localPart = null;
        if (RDFTransform.gstrValueSource in theJSONProperty &&
            theJSONProperty.valueSource !== null &&
            RDFTransform.gstrConstant in theJSONProperty.valueSource &&
            theJSONProperty.valueSource.constant !== null )
        {
            theProperty.localPart = theJSONProperty.valueSource.constant;
        }
        theProperty.theNodes = null; // ...default: no Nodes list
        // NOTE: A null Nodes list is a hint to create it from the Object Node UIs list to the
        //      Property UI during the Object Mapping process below.

        //
        // Set up the Property UI...
        //
        var thePropertyUI =
            new RDFTransformUIProperty(
                theDialog,
                theProperty,
                true,
                theSubjectNodeUI // ...Subject Node UI
                // ...Object Node UI is set below and is used to set theProperty's Object Node
            );

        // Object Mappings...
        var nodeUIs = [];
        if (RDFTransform.gstrObjectMappings in theJSONProperty &&
            theJSONProperty.objectMappings !== null &&
            Array.isArray(theJSONProperty.objectMappings) &&
            theJSONProperty.objectMappings.length > 0)
        {
            for (const theJSONNode of theJSONProperty.objectMappings) {
                // Process the Object Node for display...
                var theNodeUI =
                    RDFTransformUINode.getTransformImport(theDialog, theJSONNode, false, thePropertyUI);
                if (theNodeUI !== null) {
                    nodeUIs.push(theNodeUI);
                }
            }
        }
        else { // ...there is no Object Mappings for the Property, so create an unconfigured Node...
            var theNodeUI =
                new RDFTransformUINode(
                    theDialog,
                    null, // ...get the default node
                    false,
                    null,
                    false,
                    thePropertyUI
                );
            if (theNodeUI !== null) {
                nodeUIs.push(theNodeUI);
            }
        }
        thePropertyUI.setNodeUIs(nodeUIs);
        // NOTE: theProperty with a null theNodes list informs the setNodeUIs() 
        //      method to create theNodes list with the Nodes imported in each
        //      Node UI.  If a Node UI has a null Node, use the default Node.

        return thePropertyUI;
    }
}
