/*
 *  CLASS RDFTransformUIProperty
 *
 *  The UI property manager for the alignment dialog
 */
class RDFTransformUIProperty {
    #dialog;
    #property; // contains prefix, localPart, nodeObject
    #options;
    #parentUINode;

    #tr;
    #tdMain;

    #collapsedDetailDiv;
    #expandedDetailDiv;

    constructor(theDialog, theProperty, theTable, theOptions, parentUINode) {
        this.#dialog = theDialog;
        this.#property = theProperty;
        this.#options = theOptions;
        this.#parentUINode = parentUINode;

        // Make sure an Object node exists for the property...
        if ( ! this.#property.nodeObject) {
            this.#property.nodeObject = {};
            this.#property.nodeObject.nodeType = RDFTransformCommon.g_strRDFT_CLITERAL;
        }

        this.#collapsedDetailDiv = $('<div></div>')
            .addClass("padded")
            .html("...");
        this.#expandedDetailDiv = $('<div></div>')
            .addClass("rdf-transform-detail-container");

        var imgExpand = $('<img />')
            .attr("src", this.#options.expanded ? "images/expanded.png" : "images/collapsed.png")
            .click(
                (evt) => {
                    this.#options.expanded = !this.#options.expanded;
                    $(evt.currentTarget)
                    .attr("src", this.#options.expanded ? "images/expanded.png" : "images/collapsed.png");
                    this.show();
                }
            );

        this.#tr = theTable.insertRow(theTable.rows.length);
        this.#tdMain  = this.#tr.insertCell(0);
        var tdToggle  = this.#tr.insertCell(1);
        var tdDetails = this.#tr.insertCell(2);

        $(this.#tdMain)
            .addClass("rdf-transform-property-main")
            .attr("width", "250")
            .addClass("padded");
        this.#renderMain();

        $(tdToggle)
            .addClass("rdf-transform-property-toggle")
            .attr("width", "3%")
            .addClass("padded")
            .append(imgExpand);

        $(tdDetails)
            .addClass("rdf-transform-property-details")
            .append(this.#collapsedDetailDiv)
            .append(this.#expandedDetailDiv);
        this.show();
        this.#renderDetails();
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

    #renderMain() {
        var imgClose = $('<img />')
            .attr("title", $.i18n('rdft-dialog/remove-property'))
            .attr("src", "images/close.png")
            .css("cursor", "pointer")
            .click(
                () => {
                    setTimeout(
                        () => {
                            this.#parentUINode.removeProperty(this);
                            this.#tr.parentNode.removeChild(this.#tr);
                            this.#dialog.updatePreview();
                        },
                        100
                    );
                }
            );
        var imgArrowStart = $('<img />').attr("src", "images/arrow-start.png");
        var imgArrowEnd = $('<img />').attr("src", "images/arrow-end.png");
        var ahrefProperty = $('<a href="javascript:{}"></a>')
            .addClass("rdf-transform-property")
            .html(
                RDFTransformCommon.shortenResource(
                    this.#getTypeName(this.#property)
                )
            )
            .click( (evt) => { this.#editProperty(evt.target); } );


        $(this.#tdMain)
            .empty()
            .append(imgClose)
            .append(imgArrowStart)
            .append(ahrefProperty)
            .append(imgArrowEnd);
    }

    #renderDetails() {
        if (this.tableDetails) {
            this.tableDetails.remove();
        }
        this.tableDetails =
            $('<table></table>')
            .addClass("rdf-transform-details-table-layout");
        this.#expandedDetailDiv.append(this.tableDetails);

        var optionsObject = {};
        optionsObject.expanded = this.#isObjectExpandable();

        if (this.nodeObjectUI) {
            this.nodeObjectUI.dispose();
        }
        this.nodeObjectUI =
            new RDFTransformUINode(
                this.#dialog,
                this.#property.nodeObject,
                false,
                this.tableDetails[0],
                optionsObject
            );
    }

    #isObjectExpandable() {
        return ("properties" in this.#property.nodeObject &&
                this.#property.nodeObject.properties.length > 0);
    }

    #getTypeName(theProperty) {
        if (! theProperty ) {
            return "<ERROR: No Property!>";
        }
        if ("prefix" in theProperty && theProperty.prefix !== null) {
            return theProperty.prefix + ":" + theProperty.localPart;
        }
        else if ("localPart" in theProperty && theProperty.localPart !== null) {
            return theProperty.localPart;
        }
        else {
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
        this.#property.prefix  = theProperty.prefix;
        this.#property.localPart = theProperty.localPart;
        this.#renderMain();
        this.#dialog.updatePreview();
    }

    getTransformExport() {
        var theProperty = null;
        if ("localPart" in this.#property && this.#property.localPart !== null)
        {
            theProperty = {};

            if ("prefix" in this.#property && this.#property.prefix !== null) {
                theProperty.prefix = this.#property.prefix;
            }

            // For properties, "iri" valueType is implied, so the following is NOT needed:
            //theProperty.valueType = {};
            //theProperty.valueType.type = "iri";

            // TODO: Currently, all properties are "constant".  Change to allow
            //      column with expression.
            theProperty.valueSource = {};
            theProperty.valueSource.source = "constant";
            theProperty.valueSource.constant = this.#property.localPart;

            if ("nodeObjectUI" in this && this.nodeObjectUI !== null) {
                var nodeObjectJSON = this.nodeObjectUI.getTransformExport();
                if (nodeObjectJSON !== null) {
                    theProperty.objectMappings = [];
                    theProperty.objectMappings.push(nodeObjectJSON);
                }
            }
        }
        return theProperty;
    }
}
