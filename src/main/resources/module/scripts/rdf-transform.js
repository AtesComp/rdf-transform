/*
 *  CLASS RDFTransform
 *
 *  A class holding the RDF Transform baseline setting and functions
 */
class RDFTransform {
    // This Client-side RDFTransform.KEY matches Server-side RDFTransform.KEY
    // NOTE: "get KEY()" sets "KEY" for retrieval.
    static get KEY() { return "rdf-transform"; };

    static get g_strExtension() { return "RDFTransform"; }
    static g_strVersion = "2.0.0";

    // NOTE: Even though the expression is currently 'only GREL', we allow for future change
    //      by a setDefaults() modification.
    static g_strDefaultExpressionLanguage = 'grel'; // ...the default (and only) Expression Language
    static g_strDefaultExpression;                  // ...the default Expression for the current (and only) language, GREL
    static g_strExpressionIndex;                    // ...the Index Expression to use: Row or Record
    static g_strExpressionSource;                   // ...the Source Expression to use: Row or Record
    static g_strIndexTitle;                         // ...the column title for the index: "Row" or "Record"
    static g_bRowBased = true;                      // ...the type of indexing used: Row (true) or Record (false)

    // Setup default Master Root Node...
    static g_nodeMasterRoot = {};

    static setDefaults() {
        // NOTE: We can't set these variables as static class statements since they depend on
        //       OpenRefine's "theProject".  The classes are loaded when the extension is
        //       loaded and before a project is selected.  Therefore, "theProject" is
        //       incomplete until project selection.

        // The Default Expression setting is reset each time the dialog is opened since we
        // don't know if this is the first time or project change...
        RDFTransform.g_strDefaultExpression =
            theProject
            .scripting[RDFTransform.g_strDefaultExpressionLanguage]
            .defaultExpression;

        // The Row / Record Expression Index setting must be reset each time the dialog is
        // opened in case it changed in the OpenRefine UI...
        RDFTransform.g_bRowBased = ( theProject.rowModel.mode === "row-based" );

        if (RDFTransform.g_bRowBased) {
            RDFTransform.g_strExpressionIndex = "row.index";        // ...Row Index Expression
            RDFTransform.g_strExpressionSource = "row_index";       // ...Row Source Expression
            RDFTransform.g_strIndexTitle = $.i18n("rdft-dialog/title-row");
        }
        else {
            RDFTransform.g_strExpressionIndex = "row.record.index"; // ...Record Index Expression
            RDFTransform.g_strExpressionSource = "record_id";       // ...Record Source Expression
            RDFTransform.g_strIndexTitle = $.i18n("rdft-dialog/title-rec");
        }

        // Setup default Master Root Node...
        // ...assume Cell As Resource with Index Expression and No Properties...
        RDFTransform.g_nodeMasterRoot.nodeType = RDFTransformCommon.g_strRDFT_CRESOURCE;
        RDFTransform.g_nodeMasterRoot.expression = RDFTransform.g_strExpressionIndex;
        RDFTransform.g_nodeMasterRoot.isIndex = true;
        RDFTransform.g_nodeMasterRoot.properties = [];
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

    constructor() {
        // The transform defaults are set here since "theProject" is not completely
        // populated until after the main OpenRefine display is active and a project
        // is selected.  Since we only have one RDFTransformDialog per project. the
        // defaults can be safely set during the one and only RDFTransform construction...
        RDFTransform.setDefaults();

        // The RDFTransform has not been initialized...
		// Initialize after construction!
    }

    async initTransform(theTransform) {
        await this.#init(theTransform);
        this.#buildBody();

        // Initialize namespaces...
        this.thePrefixes = this.#elements.rdftEditPrefixes; // ...used in RDFTransformPrefixesManager
        this.prefixesManager = new RDFTransformPrefixesManager(this);
        await this.prefixesManager.initPrefixes();

        // Initialize baseIRI...
        this.#replaceBaseIRI(this.#theTransform.baseIRI || location.origin + '/', false);

        // Initialize transform view...
        this.#processTransformTab();
        this.#processPreviewTab();
    }

    async #init(theTransform) {
        //
        // theTransform has the base structure:
        //   { "baseIRI" : "", "namespaces" : [], "subjectMappings" : [] };
        //
        var cloneTransform = {};
        // Is the transform valid?  No, then set a baseline...
        if ( typeof theTransform === 'object' && theTransform !== null ) {
            cloneTransform = theTransform;
        }
        // Clone the transform for modification...
        this.#theTransform = cloneDeep(cloneTransform); // ...clone current transform

        // ...and set up the baseIRI, namespaces, and subjectMappings...

        //
        // Base IRI
        //   The baseIRI can be null and will be set to a default later...
        // --------------------------------------------------------------------------------

        //
        // Namespaces
        //   The namespaces can be null and will be set to a default later...
        // --------------------------------------------------------------------------------

        //
        // Subject Mappings
        //   The subject mappings must mave at least one default root node...
        // --------------------------------------------------------------------------------

        // Does the transform have a Subject Mappings array?  No, then set an array...
        if ( ! ( "subjectMappings" in this.#theTransform && this.#theTransform.subjectMappings ) ) {
            this.#theTransform.subjectMappings = [];
        }
        // Does the transform have any root nodes?  No, then set the initial root node...
        if ( this.#theTransform.subjectMappings.length === 0) {
            var nodeRoot = await this.#createInitialRootNode();
            this.#theTransform.subjectMappings.push(nodeRoot);
        }

        this.#nodeUIs = []; // ...array of RDFTransformUINode
    }

    /*
     * Method #createInitialRootNode()
     *
     *   A method that produces the initial root node (source) used to display a
     *   transform, a Row / Record based index node, with a set of properties to all
     *   the column data.
     *   The properties serve as a (property, object) list for the (source) root node
     *   to form an RDF triple set.  For each data column, a property is set to null
     *   (an unset IRI name) and an object is set to the column data (by column name)
     *   and declared constant literal data.
     */
    async #createInitialRootNode() {
        // Get a new root node...
        var nodeRoot = this.#createRootNode();

        // Add default properties to default root node...
        var properties = [];
        // Construct properties as "column name" IRIs connected to "column name" literal objects
        for (const column of theProject.columnModel.columns) {
            if (column) {
                // Default object of the property (new object each property)...
                var nodeObject = {};
                nodeObject.nodeType =  RDFTransformCommon.g_strRDFT_CLITERAL;
                nodeObject.columnName = column.name;

                // Default property...
                var strIRI = await RDFTransformCommon.toIRIString(nodeObject.columnName);
                if (strIRI !== null && strIRI.length > 0 && strIRI.indexOf("://") === -1 && strIRI[0] !== ":") {
                    // Use baseIRI...
                    strIRI = ":" + strIRI;
                }
                var theProperty = {};
                theProperty.prefix = null;
                theProperty.pathIRI = strIRI;
                theProperty.nodeObject = nodeObject;
                properties.push(theProperty);
            }
        }
        nodeRoot.properties = properties;

        return nodeRoot;
    }

    #createRootNode() {
        // Retrieve a copy of the Master Root Node...
        return cloneDeep(RDFTransform.g_nodeMasterRoot);
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
        this.#elements.rdftImpTemplate.text(      $.i18n('rdft-buttons/import-template')       );
        this.#elements.rdftExpTemplate.text(      $.i18n('rdft-buttons/export-template')       );
        this.#elements.rdftSaveTransform.text(    $.i18n('rdft-buttons/save')                  );
        this.#elements.buttonOK.text(             $.i18n('rdft-buttons/ok')                    );
        this.#elements.buttonCancel.text(         $.i18n('rdft-buttons/cancel')                );

        const strSample =
            $.i18n('rdft-dialog/sample-turtle', this.iSampleLimit) +
            ( RDFTransform.g_bRowBased ? $.i18n("rdft-dialog/sample-row") : $.i18n("rdft-dialog/sample-rec") );;
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
                var nodeRootNew = this.#createRootNode();
                this.#theTransform.subjectMappings.push(nodeRootNew);
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

        // Hook up the Import RDF Template button...
        this.#elements.rdftImpTemplate
        .click( (evt) => {
                evt.preventDefault();
                this.#doImport();
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

    #doImport() {
        this.#doSave(); // ...for undo

        var theTransform = null;
        theTransform = RDFImportTemplate.importTemplate()
        if ( theTransform == null ) {
            return;
        }
        this.#theTransform = cloneDeep(theTransform);

        // Initialize namespaces...
        this.prefixesManager.resetPrefixes();

        // Initialize baseIRI...
        this.#replaceBaseIRI( this.#theTransform.baseIRI );

        // Initialize transform view...
        this.#processTransformTab();
        this.#processPreviewTab();
    }

    #doExport() {
        this.#doSave(); // ...for undo

        const theTransform = this.getJSON();
        RDFExportTemplate.exportTemplate( theTransform );
    }

    #doSave() {
        const theTransform = this.getJSON();
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

        for (const nodeRoot of this.#theTransform.subjectMappings) {
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
        const theTransform = this.getJSON();
        /*DEBUG*/ console.log(theTransform);
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
                                    const strPreview =
                                        data.v.toString()
                                        .replace(/&/g, "&amp;")
                                        .replace(/</g, "&lt;")
                                        .replace(/>/g, "&gt;")
                                        .replace(/"/g, "&quot;");
                                    this.#panePreview.empty();
                                    //this.#panePreview.html( RDFTransformCommon.toIRIProperty(strPreview) );
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
                var strIRI = elements.rdftNewBaseIRIValue.val();

                if ( ! await RDFTransformCommon.validatePrefix(strIRI) ) {
                    return;
                }

                // All Good, set the project's BaseIRI...
                MenuSystem.dismissAll();
                this.#replaceBaseIRI(strIRI);
                this.#processPreviewTab();
            }
        );

        elements.buttonCancel
        .click( () => { MenuSystem.dismissAll(); } );
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

    getNamespaces() {
        return this.#theTransform.namespaces;
    }

    getJSON() {
        var theTransform = {};
        theTransform.extension = RDFTransform.g_strExtension;
        theTransform.version   = RDFTransform.g_strVersion;

        // Get the current base IRI...
        theTransform.baseIRI = this.#theTransform.baseIRI;

        // Get the current namespaces...
        theTransform.namespaces = {};
        if (this.prefixesManager.prefixes != null)
        {
            theTransform.namespaces = this.prefixesManager.prefixes;
        }
        //else {
        //    alert("No prefixes!");
        //}

        // Get the current Subject Mapping nodes...
        theTransform.subjectMappings = [];
        for (const nodeUI of this.#nodeUIs) {
            if (nodeUI) {
                var node = nodeUI.getJSON();
                if (node !== null) {
                    theTransform.subjectMappings.push(node);
                }
            }
        }

        return theTransform;
    }
};
