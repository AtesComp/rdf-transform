/*
 *  CLASS RDFTransformUIProperty
 *
 *  The UI property manager for the alignment dialog
 */
class RDFTransformUIProperty {
    #dialog;
    #property;
    #bIsExpanded;
    #nodeuiSubject;
    #nodeuiObject;

    #tableDetails;

    #tr;
    #tdMain;
    #tdToggle;
    #tdDetails;

    #imgExpand;
    #imgNone;

    #collapsedDetailDiv;
    #expandedDetailDiv;

    static #nodeObjectDefault = {};
    static {
        this.#nodeObjectDefault.valueType = {};
        this.#nodeObjectDefault.valueType.type = RDFTransform.gstrLiteral;
        this.#nodeObjectDefault.valueSource = {};
        this.#nodeObjectDefault.valueSource.source = RDFTransform.gstrValueSourceIndex;
    }

    constructor(theDialog, theProperty, bIsExpanded, theSubjectNodeUI) {
        this.#dialog = theDialog;
        this.#property = theProperty; // ...contains CIRIE (prefix, localPart) and a Transform Node (nodeObject)
        this.#bIsExpanded = bIsExpanded;
        this.#nodeuiSubject = theSubjectNodeUI;
        this.#nodeuiObject = null;

        //
        // Properties will always have Object Nodes, even if a default.
        //
        // NOTES:
        //      A property with a valid Object Node and null Object Node UI will create an
        //      Object Node UI from the given Property's Object Node.
        //      A Property with a null Object node indicates a valid Object Node UI will be
        //      given later containing a valid Object Node to set in the Property.


        // If the Property's Object node is present (not null)...
        if ( this.#property.nodeObject !== null) {
            // ...create it's Object Node UI and process it's Property Mappings...
            this.#processObjectNodePropertyMappings(true);
        }
        // Otherwise, expect the Property's Object Node and Object Node UI will be set later
        // by the setObjectNodeUI() method.

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
    }

    getProperty() {
        return this.#property;
    }

    setObjectNodeUI(theObjectNodeUI) {
        this.#nodeuiObject = theObjectNodeUI;

        // Set the Property's Object Node from the Object Node UI...
        this.#property.nodeObject = this.#nodeuiObject.getNode();
        // ...and process it's Property Mappings...
        this.#processObjectNodePropertyMappings();
    }

    #processObjectNodePropertyMappings(bCreateObjectNodeUI = false) {
        var bIsExpanded = false; // ...no presumed Object Node Property Mappings to process.

        // If Property Mappings exist (it's also a Resource Node)...
        if (RDFTransform.gstrPropertyMappings in this.#property.nodeObject &&
            this.#property.nodeObject.propertyMappings !== null &&
            this.#property.nodeObject.propertyMappings.length > 0)
        {
            // ...prepare to process Object Node Property Mappings...
            bIsExpanded = true;
        }

        // If we need to create a Node UI...
        if (bCreateObjectNodeUI) {
            // Create a Node UI containing the Object Node...
            this.#nodeuiObject = new RDFTransformUINode(
                this.#dialog,
                this.#property.nodeObject,
                false, // ...an Object Node (not Root)
                null, // ...process and set properties below if expanded
                bIsExpanded,
                this // ...Object Nodes require a Subject Property UI
            );
        }

        // If we need to process Object Node Property Mappings (for a Resource)...
        if (bIsExpanded) {
            // Process the related Property UIs...
            var theProperties = [];
            for (const theJSONProperty of this.#property.nodeObject.propertyMappings) {
                // Process the property for display...
                var propertyUI =
                    RDFTransformUIProperty.getTransformImport(this.#dialog, theJSONProperty, this.#nodeuiObject);
                if (propertyUI !== null) {
                    theProperties.push(propertyUI);
                }
            }
            // ...and set the Property UIs for the Node UI...
            this.#nodeuiObject.setPropertyUIs(theProperties);
        }
        // Otherwise, the Object Node is a Resource without Property Mappings or it's a Literal

        // The Node UI will have it's processView() called when this Property
        // has it's processView() call #renderDetails().
    }

    processView(theTable) {
        this.#tr = theTable.insertRow(); // ...the preperty's "tr" is removable, so preserve
        this.#tdMain    = this.#tr.insertCell(); // 0
        this.#tdToggle  = this.#tr.insertCell(); // 1
        this.#tdDetails = this.#tr.insertCell(); // 2

        this.#collapsedDetailDiv =
            $('<div></div>')
            .addClass("padded");
        this.#expandedDetailDiv =
            $('<div></div>')
            .addClass("rdf-transform-detail-container");

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
        var imgDelete = $('<img />')
            .attr("title", $.i18n('rdft-dialog/remove-property'))
            .attr("src", ModuleWirings[RDFTransform.KEY] + "images/delete.png")
            .css("cursor", "pointer")
            .on("click",
                () => {
                    setTimeout(
                        () => {
                            //this.#tr.parentNode.removeChild(this.#tr);
                            this.#tr.remove(); // ...first
                            this.#nodeuiSubject.removeProperty(this); // ...second, for view update
                        },
                        100
                    );
                }
            );
        var imgArrowStart = $('<img />').attr("src", "images/arrow-start.png");
        var imgArrowEnd = $('<img />').attr("src", "images/arrow-end.png");
        var ahref =
            $('<a href="javascript:{}"></a>')
                .addClass("rdf-transform-property")
                .on("click", (evt) => { this.#editProperty(evt.target); } );
        ahref.append(
            $("<span></span>")
                .addClass("rdf-transform-property-label")
                .text( RDFTransformCommon.shortenResource( this.#getPropertyName(this.#property) ) )
        );

        $(this.#tdMain)
            .empty()
            .append(imgDelete)
            .append(imgArrowStart)
            .append(ahref)
            .append(imgArrowEnd);
    }

    #renderDetails() {
        if (this.#tableDetails) {
            this.#tableDetails.remove();
        }
        this.#tableDetails =
            $('<table></table>')
            .addClass("rdf-transform-details-table-layout");
        this.#expandedDetailDiv.append(this.#tableDetails);

        if (this.#nodeuiObject !== null) { // TODO: Expand for Node List
            this.#nodeuiObject.processView(this.#tableDetails[0]);
        }
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
        return ( this.#nodeuiObject !== null  && this.#nodeuiObject.isExpandable() );
    }

    #showExpandable() {
        //$(this.#tdToggle).empty().append(this.#imgExpand);
        //$(this.#tdToggle).show();
        $(this.#imgExpand).show();
        $(this.#imgNone).hide();
        //$(this.#tdDetails).show();
    }

    #hideExpandable() {
        //$(this.#tdToggle).empty().append(this.#imgNone);
        //$(this.#tdToggle).hide();
        $(this.#imgExpand).hide();
        $(this.#imgNone).show();
        //$(this.#tdDetails).hide();
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

        if (this.#nodeuiObject !== null) {
            var nodeObjectJSON = this.#nodeuiObject.getTransformExport();
            if (nodeObjectJSON !== null) {
                theProperty.objectMappings = [];
                theProperty.objectMappings.push(nodeObjectJSON);
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
        theProperty.nodeObject = null; // ...default: no Object Node
        // NOTE: A null Object Node is a hint to copy it from the Object Node UI to the
        //      Property UI during the Object Mapping process below.

        //
        // Set up the Property UI...
        //
        var theSubjectPropertyUI =
            new RDFTransformUIProperty(
                theDialog,
                theProperty,
                true,
                theSubjectNodeUI // ...Subject Node UI
                // ...Object Node UI is set below and is used to set theProperty's Object Node
            );

        var theObjectNodeUI = null;
        if (RDFTransform.gstrObjectMappings in theJSONProperty &&
            theJSONProperty.objectMappings !== null &&
            Array.isArray(theJSONProperty.objectMappings) &&
            theJSONProperty.objectMappings.length > 0)
        {
            // TODO: Currently, a property contains at most one Object node.  Change to allow
            //      multiple Object nodes.
            var theJSONNode = theJSONProperty.objectMappings[0];

            // Process the JSON Node into an Object Node for the Object Node UI display...
            theObjectNodeUI =
                RDFTransformUINode.getTransformImport(theDialog, theJSONNode, false, theSubjectPropertyUI);
        }
        else { // ...there is no Object Mappings for the Property, so create an unconfigured Node...
            theObjectNodeUI =
                new RDFTransformUINode(
                    theDialog,
                    null, // ...get the default node
                    false,
                    null,
                    false,
                    theSubjectPropertyUI
                );
        }
        theSubjectPropertyUI.setObjectNodeUI(theObjectNodeUI);
        // NOTE: theProperty with a null Object Node and the given Object Node UI informs the
        //      setObjectNodeUI() method to set theProperty's Object Node with the Node imported
        //      for the Object Node UI.  If the Object Node UI has a null Node, use the default
        //      Node.

        return theSubjectPropertyUI;
    }
}
