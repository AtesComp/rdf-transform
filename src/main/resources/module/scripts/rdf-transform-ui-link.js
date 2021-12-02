/*
 *  CLASS RDFTransformUILink
 *
 *  The UI link manager for the alignment dialog
 */
class RDFTransformUILink {
    #dialog;
    #link;
    #options;
    #parentUINode;

    #tr;
    #tdMain;

    #collapsedDetailDiv;
    #expandedDetailDiv;

    constructor(dialog, link, table, options, parentUINode) {
        this.#dialog = dialog;
        this.#link = link;
        this.#options = options;
        this.#parentUINode = parentUINode;

        // Make sure target node exists...
        if (! this.#link.target) {
            this.#link.target = { nodeType: RDFTransformCommon.g_strRDFT_CLITERAL };
        }

        this.#tr = table.insertRow(table.rows.length);
        this.#tdMain  = this.#tr.insertCell(0);
        var tdToggle  = this.#tr.insertCell(1);
        var tdDetails = this.#tr.insertCell(2);

        $(this.#tdMain)
        .addClass("rdf-transform-link-main")
        .attr("width", "250")
        .addClass("padded");
        $(tdToggle)
        .addClass("rdf-transform-link-toggle")
        .attr("width", "3%")
        .addClass("padded");
        $(tdDetails)
        .addClass("rdf-transform-link-details");

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
        var linkIRI = this.getTypeName(this.#link);
        var label = linkIRI || "property?";

        $('<img />')
        .attr("title", $.i18n('rdft-dialog/remove-property'))
        .attr("src", "images/close.png")
        .css("cursor", "pointer")
        .prependTo(this.#tdMain)
        .click(
            () => {
                setTimeout(
                    () => {
                        this.#parentUINode.removeLink(this);
                        this.#tr.parentNode.removeChild(this.#tr);
                        this.#dialog.updatePreview();
                    },
                    100
                );
            }
        );

        var ahrefLink = $('<a href="javascript:{}"></a>')
            .addClass("rdf-transform-link")
            .html(RDFTransformCommon.shortenResource(label))
            .appendTo(this.#tdMain)
            .click( (evt) => { this.#startEditProperty(evt); } );

        $('<img />').attr("src", "images/arrow-start.png").prependTo(ahrefLink);
        $('<img />').attr("src", "images/arrow-end.png").appendTo(ahrefLink);
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
        this.targetUI = new RDFTransformUINode(
            this.#dialog,
            this.#link.target,
            this.tableDetails[0],
            { expanded: "links" in this.#link.target && this.#link.target.links.length > 0 });
    }

    #startEditProperty(evt) {
        new RDFTransformResourceDialog(
            evt.target, 'property', theProject.id, this.#dialog,
            (obj) => {
                this.#link.iri   = obj.id;
                this.#link.cirie = obj.name;
                this.#dialog.updatePreview();
                this.#renderMain();
            }
        );
    }

    getTypeName(prefix) {
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
        if ("iri"    in this.#link && this.#link.iri    !== null &&
            "target" in this.#link && this.#link.target !== null) {
            var targetJSON = this.targetUI.getJSON();
            if (targetJSON !== null) {
                return { iri    : this.#link.iri,
                         cirie  : this.#link.cirie,
                         target : targetJSON };
            }
        }
        return null;
    }
}
