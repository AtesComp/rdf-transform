/*
 *  CLASS RDFTransformUIProperty
 *
 *  The UI property manager for the alignment dialog
 */
class RDFTransformUIProperty {
    #dialog;
    #property;
    #options;
    #parentUINode;

    #tr;
    #tdMain;

    #collapsedDetailDiv;
    #expandedDetailDiv;

    constructor(dialog, property, table, options, parentUINode) {
        this.#dialog = dialog;
        this.#property = property;
        this.#options = options;
        this.#parentUINode = parentUINode;

        // Make sure target node exists...
        if ( ! this.#property.target) {
            this.#property.target = {};
            this.#property.target.nodeType = RDFTransformCommon.g_strRDFT_CLITERAL;
        }

        this.#tr = table.insertRow(table.rows.length);
        this.#tdMain  = this.#tr.insertCell(0);
        var tdToggle  = this.#tr.insertCell(1);
        var tdDetails = this.#tr.insertCell(2);

        $(this.#tdMain)
        .addClass("rdf-transform-property-main")
        .attr("width", "250")
        .addClass("padded");
        $(tdToggle)
        .addClass("rdf-transform-property-toggle")
        .attr("width", "3%")
        .addClass("padded");
        $(tdDetails)
        .addClass("rdf-transform-property-details");

        this.#collapsedDetailDiv =
            $('<div></div>')
            .appendTo(tdDetails)
            .addClass("padded")
            .html("...");
        this.#expandedDetailDiv =
            $('<div></div>')
            .appendTo(tdDetails)
            .addClass("rdf-transform-detail-container");

        this.show();

        $('<img />')
        .attr("src", this.#options.expanded ? "images/expanded.png" : "images/collapsed.png")
        .appendTo(tdToggle)
        .click(
            (evt) => {
                this.#options.expanded = !this.#options.expanded;
                $(evt.currentTarget)
                .attr("src", this.#options.expanded ? "images/expanded.png" : "images/collapsed.png");
                this.show();
            }
        );

        this.#renderMain();
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
        $(this.#tdMain).empty();
        var propertyIRI = this.#getTypeName(this.#property);
        var label = propertyIRI || "property?";

        $('<img />')
        .attr("title", $.i18n('rdft-dialog/remove-property'))
        .attr("src", "images/close.png")
        .css("cursor", "pointer")
        .prependTo(this.#tdMain)
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

        var ahrefProperty = $('<a href="javascript:{}"></a>')
            .addClass("rdf-transform-property")
            .html(RDFTransformCommon.shortenResource(label))
            .click( (evt) => { this.#startEditProperty(evt); } );
        $('<img />').attr("src", "images/arrow-start.png").prependTo(ahrefProperty);
        $('<img />').attr("src", "images/arrow-end.png").appendTo(ahrefProperty);

        this.#tdMain.append(ahrefProperty);
    }

    #renderDetails() {
        if (this.targetUI) {
            this.targetUI.dispose();
        }
        if (this.tableDetails) {
            this.tableDetails.remove();
        }

        this.tableDetails =
            $('<table></table>')
            .addClass("rdf-transform-details-table-layout")
            .appendTo(this.#expandedDetailDiv);
        this.targetUI =
            new RDFTransformUINode(
                this.#dialog,
                this.#property.target,
                this.tableDetails[0],
                { expanded : // true or false...
                    ( "propertyMappings" in this.#property.target &&
                      this.#property.target.propertyMappings.length > 0 )
                }
            );
    }

    #startEditProperty(evt) {
        new RDFTransformResourceDialog(
            evt.target, 'property', theProject.id, this.#dialog,
            (obj) => {
                this.#property.prefix  = obj.prefix;
                this.#property.pathIRI = obj.pathIRI;
                this.#dialog.updatePreview();
                this.#renderMain();
            }
        );
    }

    #getTypeName(theProperty) {
        if (! theProperty ) {
            return '';
        }
        if ("prefix" in theProperty && theProperty.prefix !== null) {
            return theProperty.prefix + ":" + theProperty.pathIRI;
        }
        else {
            return theProperty.pathIRI;
        }
    }

    getJSON() {
        var objProp = null;
        if ("pathIRI" in this.#property && this.#property.pathIRI !== null)
        {
            objProp = {};

            if ("prefix" in this.#property && this.#property.prefix !== null) {
                objProp.prefix = this.#property.prefix;
            }

            objProp.valueType = {};
            objProp.valueType.type = "iri";

            objProp.valueSource = {};
            objProp.valueSource.source = "constant";
            objProp.valueSource.constant = this.#property.pathIRI;

            if ("targetUI" in this && this.targetUI !== null) {
                var targetJSON = this.targetUI.getJSON();
                if (targetJSON !== null) {
                    objProp.subjectMappings = [];
                    objProp.subjectMappings.push(targetJSON);
                }
            }
        }
        return objProp;
    }
}
