/*
 *  Class RDFTransformUIProperty
 *
 *  The Property Manager UI for the RDF Transform Dialog.
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

class RDFTransformUIProperty {
    /** @type RDFTransformDialog */
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
    #imgArrowStart;
    #imgArrowEnd;

    #collapsedDetailDiv;
    #expandedDetailDiv;

    // Setup default Master Object Property (copy as needed)...
    static #propObjectDefault = {};
    static {
        this.#propObjectDefault.prefix    = null; // ...holds CIRIE Prefix (if used)
        this.#propObjectDefault.localPart = null; // ...holds CIRIE LocalPart (or Full IRI)
    }

    static getDefaultProperty(){
        return JSON.parse( JSON.stringify(RDFTransformUIProperty.#propObjectDefault) );
    }

    static #strEmptyProperty = "Property?";

    constructor(theDialog, theProperty, theNodes, bIsExpanded, theSubjectNodeUI) {
        this.#dialog = theDialog;
        this.#property = theProperty; // ...contains CIRIE (prefix, localPart)
        this.#bIsExpanded = bIsExpanded;
        this.#nodeUISubject = theSubjectNodeUI;

        // If the Property is null...
        if (this.#property == null) {
            // ...set it as the default property (clone default)...
            this.#property = RDFTransformUIProperty.getDefaultProperty();
        }

        //
        // Process any Properties for the Object Nodes...
        //
        //      Properties should always have Nodes but an RDF Transform may be
        //      a work in progress.  Therefore, Properties optionally have Nodes.
        //
        this.#nodeUIs = null;

        if (theNodes !== null) {
            // ...create each Node's Object Node UI and process its Property Mappings...
            for (const theNode of theNodes) {
                this.#processNode(null, theNode);
            }
        }
        // Otherwise, expect the Property's Object Nodes and Object Node UIs will be set later
        // by the setNodeUIs() method.

        // A Property is always a Resource and has a Constant source value.
        // TODO: Currently, all properties are "constant".  Change to allow
        //      column with expression (Variable).

        this.#imgExpand =
            // @ts-ignore
            $('<img />')
            .attr("src", ModuleWirings[RDFTransform.KEY] +
                        (this.#bIsExpanded ? "images/collapse.png" : "images/expand.png"))
            .on("click",
                (evt) => {
                    this.#bIsExpanded = !this.#bIsExpanded;
                    // @ts-ignore
                    $(evt.currentTarget)
                        .attr("src", ModuleWirings[RDFTransform.KEY] +
                                (this.#bIsExpanded ? "images/collapse.png" : "images/expand.png"));
                    this.#show();
                }
            );
        // @ts-ignore
        this.#imgNone = $('<img />');
        // @ts-ignore
        this.#imgArrowStart = $('<img />').attr("src", ModuleWirings[RDFTransform.KEY] + "images/arrow-start.png");
        // @ts-ignore
        this.#imgArrowEnd = $('<img />').attr("src", ModuleWirings[RDFTransform.KEY] + "images/arrow-end.png");
    }

    #processNode(theNodeUI, theNode) {
        var bIsExpanded = false; // ...no presumed Object's Property Mappings to process.

        // If Property Mappings exist (it's also a Resource Node)...
        if (RDFTransform.gstrPropertyMappings in theNode &&
            theNode.propertyMappings !== null &&
            theNode.propertyMappings.length > 0)
        {
            // ...prepare to process Object's Property Mappings...
            bIsExpanded = true;
            console.log("DEBUG DeadCode: #processNode: bIsExpanded = true");
        }

        // If we need to create a Node UI...
        if (theNodeUI === null) {
            // Create a Node UI containing the Object Node...
            theNodeUI = new RDFTransformUINode(
                this.#dialog,
                theNode,
                false, // ...an Object Node (not Root)
                null, // ...process and set properties below if expanded
                true, // ...expand new nodes for user convenience
                this // ...Object Nodes require a Subject Property UI
            );
            if (this.#nodeUIs === null) {
                this.#nodeUIs = [];
            }
            this.#nodeUIs.push(theNodeUI);
        }

        // If we need to process Object Node Property Mappings (for a Resource)...
        if (bIsExpanded) {
            // Process the related Property UIs...
            var theProperties = [];
            for (const theJSONProperty of theNode.propertyMappings) {
                // Process the property for display...
                var thePropertyUI =
                    RDFTransformUIProperty.getTransformImport(this.#dialog, theJSONProperty, theNodeUI);
                if (thePropertyUI !== null) {
                    theProperties.push(thePropertyUI);
                }
            }
            // ...and set the Property UIs for the Node UI...
            if (theProperties.length > 0) {
                theNodeUI.setPropertyUIs(theProperties);
                console.log("DEBUG DeadCode: #processNode: setPropertyUIs()");
            }
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
        for (const theNodeUI of this.#nodeUIs) {
            var theNode = theNodeUI.getNode();
            // Process the Object Node's Property Mappings...
            this.#processNode(theNodeUI, theNode);
        }
    }

    #hasNodeUIs() {
        return (this.#nodeUIs !== null && this.#nodeUIs.length > 0);
    }

    processView(theTable) {
        this.#tr = theTable.insertRow(); // ...the property's "tr" is removable, so preserve
        this.#tdMain    = this.#tr.insertCell(); // 0
        this.#tdToggle  = this.#tr.insertCell(); // 1
        this.#tdDetails = this.#tr.insertCell(); // 2

        // @ts-ignore
        this.#collapsedDetailDiv = $('<div />').addClass("padded");
        // @ts-ignore
        this.#expandedDetailDiv = $('<div />').addClass("rdf-transform-node-container");

        // @ts-ignore
        $(this.#tdMain)
            .addClass("rdf-transform-property-main")
            .addClass("padded");
        // @ts-ignore
        $(this.#tdToggle)
            .addClass("rdf-transform-property-toggle")
            .addClass("padded")
            .append(this.#imgExpand)
            .append(this.#imgNone);
        // @ts-ignore
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
            // @ts-ignore
            $('<a href="javascript:{}" />')
                .addClass("rdf-transform-property")
                .on("click",
                    (evt) => {
                        this.#editProperty(
                            this.#getPropertyName(this.#property),
                            evt.currentTarget // ...for popup memu position
                        );
                    }
                );
        refProperty.append(
            // @ts-ignore
            $("<span />")
                .addClass("rdf-transform-property-label")
                .text( RDFTransformCommon.shortenResource( this.#getPropertyName(this.#property) ) )
        );

        var imgDeleteProp =
            // @ts-ignore
            $('<img />')
            // @ts-ignore
            .attr("title", $.i18n('rdft-dialog/remove-property'))
            .attr("src", ModuleWirings[RDFTransform.KEY] + "images/delete.png")
            .css("cursor", "pointer")
            .on("click",
                () => {
                    setTimeout(
                        () => {
                            this.#tr.remove(); // ...first, for view update
                            this.#nodeUISubject.removeProperty(this); // ...second, for management
                        },
                        100
                    );
                }
            );

        // @ts-ignore
        $(this.#tdMain)
            .empty()
            .append(imgDeleteProp, this.#imgArrowStart, refProperty, this.#imgArrowEnd);
    }

    #renderDetails() {
        if (this.#tableDetails) {
            this.#tableDetails.remove();
        }
        // @ts-ignore
        this.#tableDetails = $('<table />').addClass("rdf-transform-node-table-layout");
        this.#expandedDetailDiv.append(this.#tableDetails);

        if ( this.#hasNodeUIs() ) {
            for (const theNodeUI of this.#nodeUIs) {
                theNodeUI.processView(this.#tableDetails[0]);
            }
        }

        //
        // Append "Add object..." to node list...
        //
        var refAddObjNode =
            // @ts-ignore
            $('<a href="javascript:{}" />')
            .addClass("action")
            // @ts-ignore
            .text( $.i18n('rdft-dialog/add-object') + '...' )
            .on("click",
                () => {
                    // Default Object Node (clone default)...
                    var theNode = RDFTransformUINode.getDefaultNode();

                    // Set up the Node UI...
                    var theNodeUI = this.#processNode(null, theNode);
                    theNodeUI.processView(this.#tableDetails[0]); // ...and view the new Node
                    // If this Property has just added it's 1st Node AND
                    //    a Subject Node has this Property...
                    if (this.#nodeUIs.length === 1 && this.#nodeUISubject !== null) {
                        this.#nodeUISubject.render(); // ...update the Subject Node's view (expandable)
                    }
                }
            );
        // @ts-ignore
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

    #isExpandable() { // ...always expand properties
        return true; //( this.#hasNodeUIs() );
    }

    #showExpandable() {
        // @ts-ignore
        $(this.#imgExpand).show();
        // @ts-ignore
        $(this.#imgNone).hide();
    }

    #hideExpandable() {
        // @ts-ignore
        $(this.#imgExpand).hide();
        // @ts-ignore
        $(this.#imgNone).show();
    }

    removeNode(theNodeUI) {
        // Get last matching Node...
        var iNodeIndex = this.#nodeUIs.lastIndexOf(theNodeUI);
        // If found...
        if (iNodeIndex >= 0) {
            this.#nodeUIs.splice(iNodeIndex, 1); // ...remove Node from this Property...
            this.render(); // ...and update the Property's view
            // If this Property has just removed it's last Node Object AND
            //    a Subject Node has this Property...
            if (this.#nodeUIs.length === 0) {
                this.#nodeUIs = null;
                if (this.#nodeUISubject !== null) {
                    this.#nodeUISubject.render(); // ...update the Subject Node's view (expandable)
                }
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
            return RDFTransformUIProperty.#strEmptyProperty;
        }
    }

    #editProperty(strProperty, elemPosition) {
        var strDefault =
            ( RDFTransformUIProperty.#strEmptyProperty === strProperty ?
                "" : strProperty );
        var theDialog =
            new RDFTransformResourceDialog(
                strProperty, elemPosition, strDefault, 'property', theProject.id, this.#dialog,
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

        /** @type {{
         *      prefix?: string,
         *      valueSource?: {
         *          source?: string,
         *          constant?: string
         *      },
         *      objectMappings?: [ ...any ]
         * }}
         */
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

        // Object Mappings...
        if ( this.#hasNodeUIs() ) {
            theProperty.objectMappings = [];
            for (const theNodeUI of this.#nodeUIs) {
                const theNode = theNodeUI.getTransformExport();
                if (theNode !== null) {
                    theProperty.objectMappings.push(theNode);
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
        //      The structure is a hacked, condensed representation from the store
        //      designed for ease of use.
        //
        /** @type {{
         *      prefix?: string,
         *      localPart?: string
         *      objectMappings?: [ ...any ]
         *  }}
         */
        var theProperty = null;

        if ( ! (RDFTransform.gstrValueSource in theJSONProperty) ) {
            theProperty = RDFTransformUIProperty.getDefaultProperty(); // ...default property
        }
        else {
            theProperty = {};

            // Prefix...
            theProperty.prefix = null;
            if (RDFTransform.gstrPrefix in theJSONProperty) {
                theProperty.prefix = theJSONProperty.prefix;
            }

            // Value Source (we know exists)...
            // Value Source Constant...
            // TODO: Currently, all properties are "constant".  Change to allow
            //      column with expression.
            theProperty.localPart = null;
            if (RDFTransform.gstrConstant in theJSONProperty.valueSource)
            {
                theProperty.localPart = theJSONProperty.valueSource.constant;
            }
        }

        //
        // Set up the Property UI...
        //
        var thePropertyUI =
            new RDFTransformUIProperty(
                theDialog,
                theProperty,
                null, // ...process and set nodes later
                true, // ...always expand Property UIs
                theSubjectNodeUI // ...Subject Node UI
                // ...Object Node UIs are set below and are used to set theProperty's Object Nodes
            );

        // Object Mappings...
        if (RDFTransform.gstrObjectMappings in theJSONProperty &&
            theJSONProperty.objectMappings !== null &&
            Array.isArray(theJSONProperty.objectMappings) &&
            theJSONProperty.objectMappings.length > 0)
        {
            var theNodeUIs = [];
            for (const theJSONNode of theJSONProperty.objectMappings) {
                // Process the Object Node for display...
                var theNodeUI =
                    RDFTransformUINode.getTransformImport(theDialog, theJSONNode, false, thePropertyUI);
                if (theNodeUI !== null) {
                    theNodeUIs.push(theNodeUI);
                }
            }
            if (theNodeUIs.length > 0) {
                thePropertyUI.setNodeUIs(theNodeUIs);
            }
        }

        return thePropertyUI;
    }
}
