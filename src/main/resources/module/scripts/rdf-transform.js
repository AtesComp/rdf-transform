/*
 *  CLASS RDFTransform
 *
 *  A class holding the RDF Transform functionality
 */
class RDFTransform {
    // This Client-side KEY matches Server-side RDFTransform.KEY
    // NOTE: "get KEY()" set "KEY" for retrieval.
    static get KEY() { return 'rdf_transform' };

    static strDefaultExpressionLanguage = 'grel'; // ...the default (and only) Expression Language
    static strDefaultExpression; // ...the Default Expression for the current (and only) language, GREL
    static strExpressionIndex; // ...the Index Expression to use: Row or Record
    static strRowBasedIndex = "row.index";          // ...Row Index Expression
    static strRecBasedIndex = "row.record.index";   // ...Record Index Expression
    static strIndexTitle; // ...the column title for the index: "Row" or "Record"
    static bRowBased = true; // ...the type of indexing used: Row (true) or Record (false)

    static setDefaults() {
        // NOTE: We can't set these variables as class statements since they depend on
        //       OpenRefine's "theProject".  The classes are loaded when the extension is
        //       loaded and before a project is selected.  Therefore, "theProject" is
        //       incomplete until project selection.

        // The Default Expression setting is reset each time the dialog is opened since we
        // don't know if this is the first time or project change...
        RDFTransform.strDefaultExpression =
            theProject
            .scripting[RDFTransform.strDefaultExpressionLanguage]
            .defaultExpression;

        // The Row / Record Expression Index setting must be reset each time the dialog is
        // opened in case it changed in the OpenRefine UI...
        RDFTransform.bRowBased = ( theProject.rowModel.mode == "row-based" );

        RDFTransform.strExpressionIndex =
            ( RDFTransform.bRowBased ?
                RDFTransform.strRowBasedIndex :
                RDFTransform.strRecBasedIndex );

        RDFTransform.strIndexTitle =
            ( RDFTransform.bRowBased ? $.i18n("rdft-dialog/title-row") : $.i18n("rdft-dialog/title-rec") );
    }

    static findColumn(columnName) {
        for (const column of theProject.columnModel.columns) {
            if (column && column.name == columnName) {
                return column;
            }
        }
        return null;
    }
};

/*
 *  CLASS RDFTransformDialog
 *
 *  The RDF Transform dialog class for transforming data to RDF
 */
class RDFTransformDialog {
    iSampleLimit = 20; // TODO: Modify for user input

    thePrefixes;
    prefixesManager;

    #theTransform;
    #nodeUIs;

    #dialog;
    #elements;
    #level;
    #paneTransform;
    #panePreview;
    #tableNodes;

    #iDiffFrameHeight;
    #iLastDiffFrameHeight;

    constructor(theTransform) {
        // The transform defaults are set here since "theProject" is not completely
        // populated until after the main OpenRefine display is active and a project is
        // selected...
        RDFTransform.setDefaults();

        this.#init(theTransform);
        this.#buildBody();
    }

    static #createRootNode() {
        // Setup default Root Node...
        const nodeRoot =
            {   "nodeType"        : RDFTransformCommon.g_strRDFT_CRESOURCE,
                "expression"      : RDFTransform.strExpressionIndex,
                "isRowNumberCell" : true,
                "links"           : []
            };

        // Retrieve a copy...
        return cloneDeep(nodeRoot);
    }

    /*
     * Method #createInitialRootNode()
     *
     *   A Class method that produces the initial root node (source) used to display
     *   itself, a Row / Record based index node, with a set of links to all the column
     *   data.
     *   The links serve as a (property, object) list for the (source) root node to
     *   form an RDF triple set.  For each data column, a property is set to null (an
     *   unset IRI name) and an object is set to the column data (by column name) and
     *   declared constant literal data.
     */
    static #createInitialRootNode() {
        // Get a new root node...
        var nodeRoot = RDFTransformDialog.#createRootNode();

        // Add default children...
        var links = [];
        for (const column of theProject.columnModel.columns) {
            if (column) {
                var target = {
                    "nodeType"   : RDFTransformCommon.g_strRDFT_CLITERAL,
                    "columnName" : column.name
                };
                links.push(
                    {   "iri"    : null,
                        "cirie"  : null,
                        "target" : target
                    }
                );
            }
        }
        nodeRoot.links = links;

        return nodeRoot;
    }

    #init(theTransform) {
        //
        // theTransform has the base structure:
        //   { "baseIRI" : "", "prefixes" : [], "rootNodes" : [] };
        //
        var cloneTransform = theTransform;
        // Is the transform valid?  No, then set a baseline...
        if ( !theTransform ) {
            cloneTransform = {};
        }
        // Clone the transform for modification...
        this.#theTransform = cloneDeep(cloneTransform); // ...clone current transform

        // ...and set up the baseIRI, prefixes, and rootNodes...

        //
        // Base IRI
        //   The baseIRI can be null and will be set to a default later...
        // --------------------------------------------------------------------------------

        //
        // Prefixes
        //   The prefixes can be null and will be set to a default later...
        // --------------------------------------------------------------------------------

        //
        // Root Nodes
        //   The root nodes must mave at least one default node...
        // --------------------------------------------------------------------------------

        // Does the transform have a root node array?  No, then set an array...
        if ( ! this.#theTransform.hasOwnProperty("rootNodes") || ! this.#theTransform.rootNodes ) {
            this.#theTransform.rootNodes = [];
        }
        // Does the transform have any root nodes?  No, then set the initial root node...
        if ( this.#theTransform.rootNodes.length == 0) {
            this.#theTransform.rootNodes.push( RDFTransformDialog.#createInitialRootNode() );
        }

        this.#nodeUIs = []; // ...array of RDFTransformUINode
    }

    #buildBody() {
        this.#dialog = $(DOM.loadHTML('rdf-transform', 'scripts/dialogs/rdf-transform.html'));
        this.#dialog.resizable();

        this.#elements = DOM.bind(this.#dialog);

        this.#elements.dialogHeader.text(         $.i18n('rdft-dialog/header')                 );
        this.#elements.rdftDescription.text(      $.i18n('rdft-dialog/desc')                   );
        this.#elements.rdftBaseIRIText.text(      $.i18n('rdft-dialog/base-iri') + ':'         );
        this.#elements.rdftEditBaseIRI.text(      $.i18n('rdft-dialog/edit')                   );
        this.#elements.rdftBaseIRIValue.text(     this.#theTransform.baseIRI                   );
        this.#elements.rdftTabTransformText.text( $.i18n('rdft-dialog/tab-transform')          );
        this.#elements.rdftTabPreviewText.text(   $.i18n('rdft-dialog/tab-preview')            );
        this.#elements.rdftPrefixesText.text(     $.i18n('rdft-dialog/available-prefix') + ':' );
        this.#elements.rdftAddRootNode.text(      $.i18n('rdft-buttons/add-root')              );
        this.#elements.rdftExpTemplate.text(      $.i18n('rdft-buttons/export-template')       );
        this.#elements.rdftSaveTransform.text(    $.i18n('rdft-buttons/save')                  );
        this.#elements.buttonOK.text(             $.i18n('rdft-buttons/ok')                    );
        this.#elements.buttonCancel.text(         $.i18n('rdft-buttons/cancel')                );

        const strSample =
            $.i18n('rdft-dialog/sample-turtle', this.iSampleLimit) +
            ( RDFTransform.bRowBased ? $.i18n("rdft-dialog/sample-row") : $.i18n("rdft-dialog/sample-rec") );;
        this.#elements.rdftSampleTurtleText.html( strSample );

        this.#elements.buttonOK
        .click(
            () => {
                this.#doSave();
                DialogSystem.dismissUntil(this.#level - 1);
            }
        );
        this.#elements.buttonCancel
        .click( () => DialogSystem.dismissUntil(this.#level - 1) );

        this.#functionalizeBody();

        this.#level = DialogSystem.showDialog(this.#dialog);

        // Get transform pane as element with class "rdf-transform-data-transform"...
        //   AFTER show...
        this.#paneTransform = $(".rdf-transform-data-transform");

        // Get preview pane as element with class "rdf-transform-data-preview"...
        //   AFTER show...
        this.#panePreview = $(".rdf-transform-data-preview");
    }

    #functionalizeBody() {
        // Hook up the Transform and Preview tabs...
        //$( "#rdftTabs", this.#elements.dialogBody ).tabs();
        this.#elements.rdftTabs.tabs();

        // Hook up the BaseIRI Editor...
        this.#elements.rdftEditBaseIRI
        .click( (evt) => {
                evt.preventDefault();
                this.#editBaseIRI( $(evt.target) );
            }
        );

        // Hook up the Add New Root Node button...
        this.#elements.rdftAddRootNode
        .click( (evt) => {
                evt.preventDefault();
                var nodeRootNew = RDFTransformDialog.#createRootNode();
                this.#theTransform.rootNodes.push(nodeRootNew);
                this.#nodeUIs.push(
                    new RDFTransformUINode(
                        /* dialog */  this,
                        /* node */    nodeRootNew,
                        /* table */   this.#tableNodes,
                        /* options */ { expanded: true }
                    )
                );
            }
        );

        // Hook up the Export RDF Template button...
        this.#elements.rdftExpTemplate
        .click( (evt) => {
                evt.preventDefault();
                this.#doExport();
            }
        );

        // Hook up the Save RDFTransform button...
        this.#elements.rdftSaveTransform
        .click( (evt) => {
                evt.preventDefault();
                this.#doSave();
            }
        );

        // Hook up resize...
        this.#iDiffFrameHeight = 0;
        this.#iLastDiffFrameHeight = this.#iDiffFrameHeight;
        var doResize;
        this.#dialog.resize(
            () => {
                clearTimeout(doResize);
                doResize = setTimeout(
                    () => {
                        this.#iDiffFrameHeight = this.#dialog.height() - 600;
                        if (this.#iDiffFrameHeight != this.#iLastDiffFrameHeight) {
                            this.#paneTransform.height(320 + this.#iDiffFrameHeight);
                            this.#panePreview.height(345 + this.#iDiffFrameHeight);
                            this.#iLastDiffFrameHeight = this.#iDiffFrameHeight;
                        }
                    },
                    100 // ...do it 1/10 second after no more resizing,
                        //    otherwise, keep resetting timeout...
                );
            }
        );
    }

    async initTransform() {
        // Initialize prefixes...
        this.thePrefixes = this.#elements.rdftEditPrefixes; // ...used in RDFTransformPrefixesManager
        this.prefixesManager = new RDFTransformPrefixesManager(this);
        await this.prefixesManager.initPrefixes();

        // Initialize baseIRI...
        this.#replaceBaseIRI(this.#theTransform.baseIRI || location.origin + '/', false);

        this.#processTransformTab();
        this.#processPreviewTab();
    }

    #doExport() {
        this.#doSave();
        RDFExportTemplate.exportTemplate();
    }

    #doSave() {
        var theTransform = this.getJSON();
        Refine.postProcess(
            /* module */    'rdf-transform',
            /* command */   'save-rdf-transform',
            /* params */    {},
            /* body */      { [RDFTransform.KEY] : JSON.stringify( theTransform ) },
            /* updateOps */ {},
            /* callbacks */
            {   onDone: () => {
                    theProject.overlayModels.RDFTransform = theTransform;
                }
            }
        );
    }

    #processTransformTab() {
        //
        // Process Transform Tab
        //
        this.#paneTransform.empty()

        var table =
            $('<table></table>')
            .addClass("rdf-transform-table-layout");
        this.#tableNodes = table[0];

        for (const nodeRoot of this.#theTransform.rootNodes) {
            if (nodeRoot) {
                this.#nodeUIs.push(
                    new RDFTransformUINode(
                        /* dialog */  this,
                        /* node */    nodeRoot,
                        /* table */   this.#tableNodes,
                        /* options */ { expanded: true }
                    )
                );
            }
        }
        table.appendTo(this.#paneTransform);

    }

    #processPreviewTab() {
        //
        // Process Preview Tab
        //
        var theTransform = this.getJSON();
        this.#panePreview.empty();
        $('<img />')
        .attr('src', 'images/large-spinner.gif')
        .attr('title', $.i18n('rdft-dialog/loading') + '...')
        .appendTo(this.#panePreview);

        // Consult the oracle on the RDF Preview...
        Refine.postProcess(
            /* module */    "rdf-transform",
            /* command */   'preview-rdf',
            /* params */    {},
            /* body */      {   [RDFTransform.KEY] : JSON.stringify( theTransform ),
                                "engine"           : JSON.stringify( ui.browsingEngine.getJSON() ),
                                "sampleLimit"      : this.iSampleLimit
                            },
            /* updateOps */ {},
            /* callbacks */ {   onDone: (data) => {
                                    //var strPreview = RDFTransformCommon.toHTMLBreaks( data.v.toString() );
                                    var strPreview =
                                        data.v.toString()
                                        .replace(/&/g, "&amp;")
                                        .replace(/</g, "&lt;")
                                        .replace(/>/g, "&gt;")
                                        .replace(/"/g, "&quot;");
                                    this.#panePreview.empty();
                                    //this.#panePreview.html( RDFTransformCommon.toIRILink(strPreview) );
                                    this.#panePreview.html("<pre>" + strPreview + "</pre>");
                                }
                            }
        );
    }

    #editBaseIRI(target) {
        var menu = MenuSystem.createMenu().width('300px'); // ...6:1 on input size
        menu.html(
'<div id="rdf-transform-base-iri-value">' +
  '<input type="text" bind="rdftNewBaseIRIValue" size="50" />' +
  '<button class="button" bind="buttonApply">' +
    $.i18n('rdft-buttons/apply') +
  '</button>' +
  '<button class="button" bind="buttonCancel">' +
    $.i18n('rdft-buttons/cancel') +
  '</button>' +
'</div>'
        );

        MenuSystem.showMenu(menu, () => {});
        MenuSystem.positionMenuLeftRight(menu, target);

        var elements = DOM.bind(menu);
        elements.rdftNewBaseIRIValue.val(this.#theTransform.baseIRI).focus().select();

        elements.buttonApply
        .click(
            async () => {
                function endsWith(strTest, strSuffix) {
                    return strTest.indexOf(strSuffix, strTest.length - strSuffix.length) !== -1;
                }

                var strIRI = elements.rdftNewBaseIRIValue.val();
                if ( ! await RDFTransformCommon.validateIRI(strIRI) ) {
                    alert(
                        $.i18n('rdft-dialog/alert-iri') + "\n" +
                        $.i18n('rdft-dialog/alert-iri-invalid') + "\n" +
                        strIRI
                    );
                    return;
                }

                if ( !endsWith(strIRI, "/") && !endsWith(strIRI, "#") ) {
                    var ans = confirm(
                        $.i18n('rdft-dialog/confirm-one') + "\n" +
                        $.i18n('rdft-dialog/confirm-two'));
                    if (ans == false)
                        return;
                }

                MenuSystem.dismissAll();
                this.#replaceBaseIRI(strIRI);
                this.#processPreviewTab();
            }
        );

        elements.buttonCancel.click( () => { MenuSystem.dismissAll(); } );
    }

    #replaceBaseIRI(strIRI, bSave = true) {
        if (strIRI != this.#theTransform.baseIRI) {
            this.#theTransform.baseIRI = strIRI;
        }

        if (bSave) { // ...save the base IRI...
            // Refine.postCSRF(
            //     "command/rdf-transform/save-baseIRI" + "?" + $.param({ "project" : theProject.id }),
            //     { "baseIRI" : strIRI },
            //     (data) => {
            //         if (data.code === "error") {
            //             alert($.i18n('rdft-dialog/error') + ':' + data.message);
            //             return; // ...don't replace or update anything
            //         }
            //     },
            //     "json");
            Refine.postCSRF(
                "command/rdf-transform/save-baseIRI",
                {   "project" : theProject.id,
                    "baseIRI" : strIRI
                },
                (data) => {
                    if (data.code === "error") {
                        alert($.i18n('rdft-dialog/error') + ':' + data.message);
                        return; // ...don't replace or update anything
                    }
                },
                "json"
            );
        }
        this.#elements.rdftBaseIRIValue.empty().text(strIRI);
    }

    updatePreview() {
        this.#processPreviewTab();
    }

    getBaseIRI() {
        return this.#theTransform.baseIRI;
    }

    getPrefixes() {
        return this.#theTransform.prefixes;
    }

    getJSON() {
        // Get the current base IRI...
        var baseIRI = this.#theTransform.baseIRI;

        // Get the current prefixes...
        var prefixes = [];
        if (typeof this.prefixesManager.prefixes != 'undefined' &&
            this.prefixesManager.prefixes != null)
        {
            for (const prefix of this.prefixesManager.prefixes) {
                if (prefix) {
                    prefixes.push(
                        {   "name" : prefix.name,
                            "iri"  : prefix.iri
                        }
                    );
                }
            }
        }
        //else {
        //    alert("No prefixes!");
        //}

        // Get the current root nodes...
        var rootNodes = [];
        for (const nodeUI of this.#nodeUIs) {
            if (nodeUI) {
                var node = nodeUI.getJSON();
                if (node !== null) {
                    rootNodes.push(node);
                }
            }
        }

        return {
            "baseIRI"   : baseIRI,
            "prefixes"  : prefixes,
            "rootNodes" : rootNodes
        };
    }
};
