/*
 *  CLASS RDFTransform
 *
 *  A class holding the RDF Transform baseline setting and functions
 */
class RDFTransform {
    // This Client-side RDFTransform.KEY matches Server-side RDFTransform.KEY
    // NOTE: "get KEY()" sets "KEY" for retrieval.
    static get KEY() { return "rdf-transform"; };

    static get strExtension() { return "RDFTransform"; }
    static strVersion = "2.0.0";

    // NOTE: Even though the expression is currently 'only GREL', we allow for future change
    //      by a setDefaults() modification.
    static gstrValueSourceRow = "row_index";            // ...the Value Source for Rows
    static gstrValueSourceRec = "record_id";            // ...the Value Source for Records
    static gstrValueSource;                             // ...the Value Source to use: Row or Record
    static gstrDefaultExpLang = "grel";                 // ...the default (and only) Expression Language
    static gstrDefaultExpCode;                          // ...the default Expression for the current (and only) language, GREL
    static gstrExpressionIndexRow = "row.index";        // ...the Index Expression for Rows
    static gstrExpressionIndexRec = "row.record.index"; // ...the Index Expression for Records
    static gstrExpressionIndex;                         // ...the Index Expression to use: Row or Record
    static gstrIndexTitle;                              // ...the column title for the index: "Row" or "Record"
    static gbRowBased = true;                           // ...the type of indexing used: Row (true) or Record (false)

    // Setup default Master Root Node (cloneDeep() as needed)...
    static gnodeMasterRoot = {};
    static {
        this.gnodeMasterRoot.valueSource = {};
        this.gnodeMasterRoot.valueSource.source = null; // ...to be replaced with row / record index
        this.gnodeMasterRoot.expression = {};
        this.gnodeMasterRoot.expression.language = RDFTransform.gstrDefaultExpLang;
        this.gnodeMasterRoot.expression.code = null; // ...to be replaced with default language expression
        this.gnodeMasterRoot.propertyMappings = [];
    };

    // Setup default Master Object Node (cloneDeep() as needed)...
    static gnodeMasterObject = {};
    static {
        this.gnodeMasterObject.valueType = {};
        this.gnodeMasterObject.valueType.type = "literal";
        this.gnodeMasterObject.valueSource = {};
        this.gnodeMasterObject.valueSource.source = "column";
        this.gnodeMasterObject.valueSource.columnName = null; // ...to be replaced with column.name
    };

    static setDefaults() {
        // NOTE: We can't set these variables as static class statements since they depend on
        //       OpenRefine's "theProject".  The classes are loaded when the extension is
        //       loaded and before a project is selected.  Therefore, "theProject" is
        //       incomplete until project selection.
        //      Then, these values are "locked in" for each RDFTransform session (when the
        //       dialog is displayed).  They may change between sessions, such as the index
        //       method changing from row to record or record to row.

        // The Default Expression setting is reset each time the dialog is opened since we
        // don't know if this is the first time, project changed, or system values changed...
        RDFTransform.gstrDefaultExpCode =
            theProject
            .scripting[RDFTransform.gstrDefaultExpLang]
            .defaultExpression;

        // The Row / Record Expression Index setting must be reset each time the dialog is
        // opened in case it changed in the OpenRefine UI...
        RDFTransform.gbRowBased = ( theProject.rowModel.mode === "row-based" );

        if (RDFTransform.gbRowBased) { // ...Row Based...
            RDFTransform.gstrExpressionIndex = RDFTransform.gstrExpressionIndexRow;
            RDFTransform.gstrValueSource = RDFTransform.gstrValueSourceRow;
            RDFTransform.gstrIndexTitle = $.i18n("rdft-dialog/title-row");
        }
        else { // ...Record Based...
            RDFTransform.gstrExpressionIndex = RDFTransform.gstrExpressionIndexRec;
            RDFTransform.gstrValueSource = RDFTransform.gstrValueSourceRec;
            RDFTransform.gstrIndexTitle = $.i18n("rdft-dialog/title-rec");
        }

        // Setup default Master Root Node...
        // ...assume Cell As Resource with Index Expression and No Properties...
        // Root Nodes:
        //  Defaults to "iri" but can be "bnode" and "value_bnode"
        //
        //  Default Value Type = IMPLIED "valueType": { "type": "iri" }
        //      as most Root Nodes are expected to be IRIs
        //RDFTransform.gnodeMasterRoot.valueType.type = "iri";

        //  Default Value Source = "valueSource" : { "source" : ("row_index" || "record_id") }
        //      IMPLIES bIsIndex = true
        RDFTransform.gnodeMasterRoot.valueSource.source = RDFTransform.gstrValueSource;

        //  Default Expression = "expression" : { "language": "grel", "code": ("row.index" || "row.record.index") }
        RDFTransform.gnodeMasterRoot.expression.code = RDFTransform.gstrExpressionIndex;

        //  Default Property Mappings = [], but will be filled with:
        //      Property IRIs based on existing Column Names and
        //          related Literal Objects base on Column Values
   }

    static findColumn(columnName) {
        for (const column of theProject.columnModel.columns) {
            if (column && column.name === columnName) {
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

    theNamespaces;
    namespacesManager;

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
        this.#nodeUIs = [];

        await this.#init(theTransform);
        this.#buildBody();

        // Initialize namespaces...
        this.theNamespaces = this.#elements.rdftEditNamespaces; // ...used in RDFTransformNamespacesManager
        this.namespacesManager = new RDFTransformNamespacesManager(this);
        await this.namespacesManager.init();

        // Initialize baseIRI...
        this.#replaceBaseIRI(this.#theTransform.baseIRI || location.origin + '/', false);

        // Initialize transform view...
        this.#processTransformTab();
        this.#processPreviewTab();
    }

    async #init(theTransform) {
        //
        // theTransform has the base structure:
        //   { "baseIRI" : "", "namespaces" : {}, "subjectMappings" : [] };
        //
        var localTransform = {};
        // Is the transform valid?  Yes, then use it...
        if ( typeof theTransform === 'object' && theTransform !== null ) {
            localTransform = theTransform;
        }
        // Set the transform for modification...
        this.#theTransform = JSON.parse(JSON.stringify(localTransform)); // ...clone current transform

        // ...For NEW Transforms, set up the baseIRI, namespaces, and subjectMappings...

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
        if ( ! (    "subjectMappings" in this.#theTransform &&
                    Array.isArray(this.#theTransform.subjectMappings) ) ) {
            this.#theTransform.subjectMappings = [];
        }
        // Does the transform have any root nodes?  No, then set the initial root node...
        if ( this.#theTransform.subjectMappings.length === 0) {
            // Get the new node...
            var nodeUI = await this.#createInitialRootNodeUI();
            this.#theTransform.subjectMappings.push( nodeUI.getNode() );
        }
        // Otherwise, use the existing Transform data...
        else {
            // Process the existing nodes...
            for (const nodeSubject of this.#theTransform.subjectMappings) {
                //
                // Update the Index Expression code with any indexing change (row to record, record to row)...
                //

                // Find old cruft...
                var strOldValSrc;
                var bExpExists = ("expression" in nodeSubject);
                var strOldExp;
                if ( RDFTransform.gbRowBased ) { // Now we are Row based...
                    strOldValSrc = RDFTransform.gstrValueSourceRec;
                    if (bExpExists) {
                        strOldExp = RDFTransform.gstrExpressionIndexRec;
                    }
                }
                else { // Now we are Record based...
                    strOldValSrc = RDFTransform.gstrValueSourceRow;
                    if (bExpExists) {
                        strOldExp = RDFTransform.gstrExpressionIndexRow;
                    }
                }
                var bValSrcHasOld = ( nodeSubject.valueSource.source === strOldValSrc );
                var bExpHasOld = false;
                if (bExpExists) {
                    bExpHasOld = ( nodeSubject.expression.code.indexOf( strOldExp ) !== -1 );
                }

                // Replace with current cruft...
                if (bValSrcHasOld) {
                    nodeSubject.valueSource.source = RDFTransform.gstrValueSource;
                    if (bExpHasOld) {
                        nodeSubject.expression.code.replaceAll(strOldExp, RDFTransform.gstrExpressionIndex)
                    }
                }

                //
                // Process the node for display...
                //

                var nodeUI = RDFTransformUINode.getTransformImport(this, nodeSubject);
                if (nodeUI !== null) {
                    this.#nodeUIs.push(nodeUI);
                }
            }
        }
    }

    /*
     * Method #createInitialRootNodeData()
     *
     *   A method that produces the initial root node (source) used to display a
     *   transform, a Row / Record based index node, with a set of properties to all
     *   the column data.
     *   The properties serve as a (property, object) list for the (source) root node
     *   to form an RDF triple set.  For each data column, a property is set to null
     *   (an unset IRI name) and an object is set to the column data (by column name)
     *   and declared constant literal data.
     */
    async #createInitialRootNodeUI() {
        // Get a new root node...
        var nodeRoot = this.#createRootNode();

        // Add default properties to default root node...
        var properties = [];
        // Construct properties as "column name" IRIs connected to "column name" literal objects
        var iColumnCount = 0;
        for (const column of theProject.columnModel.columns) {
            if (column) {
                iColumnCount++;
                // Default Object of the Property (new Object for each Property)...
                var nodeObject = JSON.parse(JSON.stringify(RDFTransform.gnodeMasterObject));
                nodeObject.valueSource.columnName = column.name;

                // Default property...
                var strPrefix = ""; // ...use BaseIRI prefix
                var strIRI = await RDFTransformCommon.toIRIString(column.name);
                if (strIRI === null || strIRI.length === 0) {
                    // Use baseIRI prefix and set Local Part to default column name...
                    strIRI = "column_" + iColumnCount.toString();
                }
                const iIndexProto = strIRI.indexOf("://");
                if (iIndexProto === -1) {
                    const iIndexPrefix = strIRI.indexOf(":");
                    if (iIndexPrefix === 0) { // ...begins with ':'
                        // Use baseIRI prefix and set Local Part (chop 1st)...
                        strIRI = strIRI.substring(1);
                    }
                    else if (iIndexPrefix > 0) { // ...somewhere past beginning...
                        if (iIndexPrefix + 1 === strIRI.length) { // ...ends with ':'
                            // Use baseIRI prefix and set Local Part (chop last)...
                            strIRI = strIRI.substring(0, strIRI.length - 1);
                        }
                        else { // ...in the middle
                            const strTestPrefix = strIRI.substring(0, iIndexPrefix);
                            if ( this.namespacesManager.hasPrefix(strTestPrefix) ) { // ...existing prefix
                                // Use the given prefix and set Local Part (after prefix)...
                                strPrefix = strTestPrefix;
                                strIRI = strIRI.substring(iIndexPrefix + 1);
                            }
                            // Otherwise, same as iIndexPrefix === -1...
                        }
                    }
                    // Otherwise, iIndexPrefix === -1
                    //      Then, we use baseIRI prefix and take the IRI as the local part
                }
                else if (iIndexProto === 0) { // ...should never occur
                    // Use baseIRI prefix and set Local Part (chop 1st 3)...
                    strIRI = strIRI.substring(3);
                }
                else { // iIndexProto > 0, ...a good protocol exists since it's an IRI, so it's a Full IRI
                    // Use no prefix, i.e., take the IRI as a Full IRI...
                    strPrefix = null;
                }
                var theProperty = {};
                theProperty.prefix = strPrefix;
                theProperty.localPart = strIRI;
                theProperty.nodeObject = nodeObject;
                properties.push(theProperty);
            }
        }

        return this.#createNewNodeIU(nodeRoot, properties);
    }

    #createRootNode() {
        // Retrieve a copy of the Master Root Node...
        return JSON.parse(JSON.stringify(RDFTransform.gnodeMasterRoot));
    }

    #createNewNodeIU(node, properties) {
        // Store the node data for the UI...
        var nodeUI = new RDFTransformUINode(
            this, // dialog
            node,
            true, // bIsRoot
            properties,
            { expanded: true }
        );
        this.#nodeUIs.push(nodeUI);
        return nodeUI;
    }

    #buildBody() {
        this.#dialog = $(DOM.loadHTML(RDFTransform.KEY, 'scripts/dialogs/rdf-transform.html'));
        this.#dialog.resizable();

        this.#elements = DOM.bind(this.#dialog);

        this.#elements.dialogHeader.text(         $.i18n('rdft-dialog/header')                 );
        this.#elements.rdftDescription.text(      $.i18n('rdft-dialog/desc')                   );
        this.#elements.rdftBaseIRIText.text(      $.i18n('rdft-dialog/base-iri') + ':'         );
        this.#elements.rdftEditBaseIRI.text(      $.i18n('rdft-dialog/edit')                   );
        this.#elements.rdftBaseIRIValue.text(     this.#theTransform.baseIRI                   );
        this.#elements.rdftTabTransformText.text( $.i18n('rdft-dialog/tab-transform')          );
        this.#elements.rdftTabPreviewText.text(   $.i18n('rdft-dialog/tab-preview')            );
        this.#elements.rdftNamespacesText.text(     $.i18n('rdft-dialog/available-prefix') + ':' );
        this.#elements.rdftAddRootNode.text(      $.i18n('rdft-buttons/add-root')              );
        this.#elements.rdftImpTemplate.text(      $.i18n('rdft-buttons/import-template')       );
        this.#elements.rdftExpTemplate.text(      $.i18n('rdft-buttons/export-template')       );
        this.#elements.rdftSaveTransform.text(    $.i18n('rdft-buttons/save')                  );
        this.#elements.buttonOK.text(             $.i18n('rdft-buttons/ok')                    );
        this.#elements.buttonCancel.text(         $.i18n('rdft-buttons/cancel')                );

        const strSample =
            $.i18n('rdft-dialog/sample-turtle', this.iSampleLimit) +
            ( RDFTransform.gbRowBased ? $.i18n("rdft-dialog/sample-row") : $.i18n("rdft-dialog/sample-rec") );;
        this.#elements.rdftSampleTurtleText.html( strSample );

        this.#elements.buttonOK
        .on("click", () => {
                this.#doSave();
                DialogSystem.dismissUntil(this.#level - 1);
            }
        );
        this.#elements.buttonCancel
        .on("click", () => DialogSystem.dismissUntil(this.#level - 1) );

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
        .on("click", (evt) => {
                evt.preventDefault();
                this.#editBaseIRI( $(evt.target) );
            }
        );

        // Hook up the Add New Root Node button...
        this.#elements.rdftAddRootNode
        .on("click", (evt) => {
                evt.preventDefault();
                var nodeRoot = this.#createRootNode();
                this.#theTransform.subjectMappings.push(nodeRoot);
                var nodeUI = this.#createNewNodeIU(nodeRoot, null);
                nodeUI.processView(this.#tableNodes);
            }
        );

        // Hook up the Import RDF Template button...
        this.#elements.rdftImpTemplate
        .on("click", (evt) => {
                evt.preventDefault();
                this.#doImport();
            }
        );

        // Hook up the Export RDF Template button...
        this.#elements.rdftExpTemplate
        .on("click", (evt) => {
                evt.preventDefault();
                this.#doExport();
            }
        );

        // Hook up the Save RDFTransform button...
        this.#elements.rdftSaveTransform
        .on("click", (evt) => {
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
        if ( theTransform === null ) {
            return;
        }
        this.initTransform(theTransform)
    }

    #doExport() {
        this.#doSave(); // ...for undo

        const theTransform = this.getTransformExport();
        RDFExportTemplate.exportTemplate( theTransform );
    }

    #doSave() {
        const theTransform = this.getTransformExport();
        Refine.postProcess(
            RDFTransform.KEY,       // module
            'save-rdf-transform',   // command
            {},                     // params
            { [RDFTransform.KEY] : JSON.stringify( theTransform ) },
            {},                     // updateOps
            {   onDone: (data) => { // callbacks
                    theProject.overlayModels.RDFTransform = theTransform;
                    if (data.code === "error") {
                        alert($.i18n('rdft-dialog/error') + ":" + "Save failed!");
                    }
                    else if (data.code === "pending") {
                        alert( $.i18n("Save pending history queue processing.") );
                    }
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
            $('<table></table>').addClass("rdf-transform-table-layout");
        this.#tableNodes = table[0];

        for (const nodeUI of this.#nodeUIs) {
            nodeUI.processView(this.#tableNodes);
        }

        table.appendTo(this.#paneTransform);
    }

    #processPreviewTab() {
        //
        // Process Preview Tab
        //
        const theTransform = this.getTransformExport();
        /*DEBUG*/ console.log(theTransform);
        this.#panePreview.empty();
        $('<img />')
        .attr('src', 'images/large-spinner.gif')
        .attr('title', $.i18n('rdft-dialog/loading') + '...')
        .appendTo(this.#panePreview);

        // Consult the oracle on the RDF Preview...
        Refine.postProcess(
            "rdf-transform",        // module
            "preview-rdf",          // command
            {},                     // params
            {   [RDFTransform.KEY] : JSON.stringify( theTransform ),
                "engine"           : JSON.stringify( ui.browsingEngine.getJSON() ),
                "sampleLimit"      : this.iSampleLimit
            },
            {},                     // updateOps
            {   onDone: (data) => { // callbacks
                    //var strPreview = RDFTransformCommon.toHTMLBreaks( data.message.toString() );
                    var strPreview = "ERROR: Could not process RDF preview!" // TODO: $.i18n()
                    if (data.code === "ok") {
                        strPreview =
                            data.message.toString()
                            .replace(/&/g, "&amp;")
                            .replace(/</g, "&lt;")
                            .replace(/>/g, "&gt;")
                            .replace(/"/g, "&quot;");
                    }
                    // Otherwise, data.code === "error"

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
        .on("click",
            async () => {
                var strIRI = elements.rdftNewBaseIRIValue.val();

                if ( ! await RDFTransformCommon.validateNamespace(strIRI) ) {
                    return;
                }

                // All Good, set the project's BaseIRI...
                MenuSystem.dismissAll();
                this.#replaceBaseIRI(strIRI);
                this.#processPreviewTab();
            }
        );

        elements.buttonCancel
        .on("click", () => { MenuSystem.dismissAll(); } );
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
            //             alert($.i18n('rdft-dialog/error') + ":" + data.message);
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
                        alert($.i18n('rdft-dialog/error') + ":" + data.message);
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

    getTransformExport() {
        var theTransform = {};
        theTransform.extension = RDFTransform.strExtension;
        theTransform.version   = RDFTransform.strVersion;

        // Get the current base IRI...
        theTransform.baseIRI = this.#theTransform.baseIRI;

        // Get the current namespaces...
        theTransform.namespaces = {};
        if (this.namespacesManager.namespaces != null)
        {
            theTransform.namespaces = this.namespacesManager.namespaces;
        }
        //else {
        //    alert("No namespaces!");
        //}

        // Get the current Subject Mapping nodes...
        theTransform.subjectMappings = [];
        for (const nodeUI of this.#nodeUIs) {
            if (nodeUI) {
                var node = nodeUI.getTransformExport();
                if (node !== null) {
                    theTransform.subjectMappings.push(node);
                }
            }
        }

        return theTransform;
    }
};
