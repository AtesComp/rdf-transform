/*
 *  CLASS RDFTransform
 *
 *  A class holding the RDF Transform baseline setting and functions
 */
class RDFTransform {
    // This Client-side RDFTransform.KEY matches Server-side RDFTransform.KEY
    // NOTE: "get KEY()" sets "KEY" for retrieval.
    static get KEY() { return "rdf-transform"; }

    static get strEXTENSION() { return "RDFTransform"; }
    // RDF Transform Version Control
    static strVERSION_MAJOR = "2";
    static strVERSION_MINOR = "1";
    static strVERSION_MICRO = "0";
    static strVERSION =
        RDFTransform.strVERSION_MAJOR + "." +
        RDFTransform.strVERSION_MINOR + "." +
        RDFTransform.strVERSION_MICRO;

    // NOTE: Even though the expression is currently 'only GREL', we allow for future change
    //      by a setDefaults() modification.
    static gstrValueSourceRow = "row_index";            // ...the Value Source for Rows
    static gstrValueSourceRec = "record_id";            // ...the Value Source for Records
    static gstrValueSourceIndex;                        // ...the Value Source to use: Row or Record
    static gstrDefaultExpLang = "grel";                 // ...the default (and only) Expression Language
    static gstrDefaultExpCode;                          // ...the default Expression for the current (and only) language, GREL
    static gstrExpressionIndexRow = "row.index";        // ...the Index Expression for Rows
    static gstrExpressionIndexRec = "row.record.index"; // ...the Index Expression for Records
    static gstrExpressionIndex;                         // ...the Index Expression to use: Row or Record
    static gstrIndexTitle;                              // ...the column title for the index: "Row" or "Record"
    static gbRowBased = true;                           // ...the type of indexing used: Row (true) or Record (false)

    // subjectMappings
    static gstrSubjectMappings = "subjectMappings";
    // prefix
    static gstrPrefix = "prefix";
    static gstrLocalPart = "localPart";
    // valueType.type
    static gstrValueType = "valueType";
    static gstrIRI = "iri";
    static gstrLiteral = RDFTransformCommon.gstrLiteral;
    static gstrLanguageLiteral = "language_literal";
    static gstrDatatypeLiteral = "datatype_literal";
    static gstrBNode = "bnode";
    static gstrValueBNode = "value_bnode";
    // valueSource.source
    static gstrValueSource = "valueSource";
    static gstrConstant = "constant";
    static gstrColumn = "column";
    // expression
    static gstrExpression = "expression";
    static gstrLanguage = "language";
    static gstrCode = "code";
    static gstrCodeValue = "value";
    // typeMappings
    static gstrTypeMappings = "typeMappings";
    // propertyMappings
    static gstrPropertyMappings = "propertyMappings";
    // objectMappings
    static gstrObjectMappings = "objectMappings";

    // Setup default Master Root Node (copy as needed)...
    static gnodeMasterRoot = {};
    static {
        this.gnodeMasterRoot.valueSource = {};
        this.gnodeMasterRoot.valueSource.source = null; // ...to be replaced with row / record index
        this.gnodeMasterRoot.expression = {};
        this.gnodeMasterRoot.expression.language = RDFTransform.gstrDefaultExpLang;
        this.gnodeMasterRoot.expression.code = null; // ...to be replaced with default language expression
        this.gnodeMasterRoot.propertyMappings = [];
    }

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
            RDFTransform.gstrValueSourceIndex = RDFTransform.gstrValueSourceRow;
            RDFTransform.gstrIndexTitle = $.i18n("rdft-dialog/title-row");
        }
        else { // ...Record Based...
            RDFTransform.gstrExpressionIndex = RDFTransform.gstrExpressionIndexRec;
            RDFTransform.gstrValueSourceIndex = RDFTransform.gstrValueSourceRec;
            RDFTransform.gstrIndexTitle = $.i18n("rdft-dialog/title-rec");
        }

        // Setup default Master Root Node...
        // ...assume Cell As Resource with Index Expression and No Properties...
        // Root Nodes:
        //  Defaults to "iri" but can be "bnode" and "value_bnode"
        //
        //  Default Value Type = IMPLIED "valueType": { "type": "iri" }
        //      as most Root Nodes are expected to be IRIs
        //      so valueType is not set in the Master Root

        //  Default Value Source = "valueSource" : { "source" : ("row_index" || "record_id") }
        //      IMPLIES bIsIndex = true
        RDFTransform.gnodeMasterRoot.valueSource.source = RDFTransform.gstrValueSourceIndex;

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
}

/*
 *  CLASS RDFTransformDialog
 *
 *  The RDF Transform dialog class for transforming data to RDF
 */
class RDFTransformDialog {
    iSampleLimit = 20; // TODO: Modify for user input, defaults to 10 in Util.Preferences

    #namespacesManager;

    #theTransform; // ...holds all the stuffs
    #nodeUIs;

    #dialog;
    #elements;
    #imgLargeSpinner;
    #imgLineBounce;
    #level;
    #tableNodes;

    #bUpdatePreview;

    #iResize;
    #iDiffFrameHeight;
    #iLastDiffFrameHeight;
    #iBodyHeadInit;
    #iBaseTransformPaneHeight;
    #iBasePreviewPaneHeight;

    constructor(theTransform) {
        // The transform defaults are set here since "theProject" is not completely
        // populated until after the main OpenRefine display is active and a project
        // is selected.  Since we only have one RDFTransformDialog per project. the
        // defaults can be safely set during the one and only RDFTransform construction...
        RDFTransform.setDefaults();

        this.#imgLargeSpinner =
            $('<img />')
                .attr('src', ModuleWirings[RDFTransform.KEY] + 'images/large_spinner.gif')
                .attr('title', $.i18n('rdft-dialog/loading') + '...');

        this.#imgLineBounce =
            $('<img />')
                .attr('src', ModuleWirings[RDFTransform.KEY] + 'images/line_bounce.gif');

        this.#buildBody();

        this.#nodeUIs = [];

        // The RDFTransformDialog has not been fully initialized!
        // Initialize by asynchronous setTimeout() for:
        //  1. server-side required processing (i.e., after construction)
        //  2. 0.5 second wait for dialog to fully display
        setTimeout( () => { this.#initTransform(theTransform); }, 500 );
    }

    async #initTransform(theTransform) {
        // Setup the underlying data...
        await this.#init(theTransform);

        // Initialize baseIRI...
        this.#replaceBaseIRI(this.#theTransform.baseIRI || location.origin + '/', false);

        // Initialize namespaces...
        await this.#processNamespaces();

        // Initialize transform view...
        this.#processTransformTab();
        this.updatePreview();
    }

    async #init(theTransform) {
        //
        // theTransform has the base structure:
        //   { "baseIRI" : "", "namespaces" : {}, "subjectMappings" : [] };
        //
        var localTransform = {}; // ...default when no transform given or bad
        // Is the transform valid?  Yes, then use it...
        if ( typeof theTransform === 'object' && theTransform !== null ) {
            localTransform = theTransform;
        }
        // Set the transform for modification (clone current transform)...
        this.#theTransform = JSON.parse( JSON.stringify(localTransform) );

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
        if ( ! (    RDFTransform.gstrSubjectMappings in this.#theTransform &&
                    Array.isArray(this.#theTransform.subjectMappings) ) )
        {
            this.#theTransform.subjectMappings = [];
        }

        var nodeUI = null;
        // Does the transform have any root nodes?  No, then set the initial root node...
        if ( this.#theTransform.subjectMappings.length === 0) {
            // Get the new node...
            nodeUI = await this.#createInitialRootNodeUI();
            this.#theTransform.subjectMappings.push( nodeUI.getTransformExport() );
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
                var bExpExists = (RDFTransform.gstrExpression in nodeSubject);
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
                    bExpHasOld = ( nodeSubject.expression.code.indexOf( strOldExp ) >= 0 );
                }

                // Replace with current cruft...
                if (bValSrcHasOld) {
                    nodeSubject.valueSource.source = RDFTransform.gstrValueSourceIndex;
                    if (bExpHasOld) {
                        nodeSubject.expression.code.replaceAll(strOldExp, RDFTransform.gstrExpressionIndex)
                    }
                }

                //
                // Process the node for display...
                //
                nodeUI = RDFTransformUINode.getTransformImport(this, nodeSubject);
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
        var theRootNode = this.#createRootNode();
        var theRootNodeUI = this.#createNewNodeIU(theRootNode, null);

        // Add default properties to default root node...
        var thePropertyUIs = [];
        // Construct properties as "column name" IRIs connected to "column name" literal objects
        var iColumnCount = 0;
        for (const column of theProject.columnModel.columns) {
            if (column) {
                iColumnCount++;

                // Set up the Property values...
                var strPrefix = ""; // ...use BaseIRI prefix
                var strIRI = await RDFTransformCommon.toIRIString(column.name);
                if (strIRI === null || strIRI.length === 0) {
                    // Use BaseIRI prefix and set Local Part to default column name...
                    strIRI = "column_" + iColumnCount.toString();
                }
                const iIndexProto = strIRI.indexOf("://");
                if (iIndexProto === -1) { // ...Not Found
                    const iIndexPrefix = strIRI.indexOf(":");
                    if (iIndexPrefix === 0) { // ...begins with ':'
                        // Use BaseIRI prefix and set Local Part (chop 1st)...
                        strIRI = strIRI.substring(1);
                    }
                    else if (iIndexPrefix > 0) { // ...somewhere past beginning...
                        if (iIndexPrefix + 1 === strIRI.length) { // ...ends with ':'
                            // Use BaseIRI prefix and set Local Part (chop last)...
                            strIRI = strIRI.substring(0, strIRI.length - 1);
                        }
                        else { // ...in the middle
                            const strTestPrefix = strIRI.substring(0, iIndexPrefix);
                            if ( this.#namespacesManager.hasPrefix(strTestPrefix) ) { // ...existing prefix
                                // Use the given prefix and set Local Part (after prefix)...
                                strPrefix = strTestPrefix;
                                strIRI = strIRI.substring(iIndexPrefix + 1);
                            }
                            // Otherwise, same as iIndexPrefix === -1...
                        }
                    }
                    // Otherwise, iIndexPrefix === -1
                    //      Then, we use BaseIRI prefix and take the IRI as the local part
                }
                else if (iIndexProto === 0) { // Found at Start...should never occur
                    // Use BaseIRI prefix and set Local Part (chop 1st 3)...
                    strIRI = strIRI.substring(3);
                }
                else { // Found Within...iIndexProto > 0 and it's an IRI, so it's a Full IRI
                    // Use no prefix, i.e., take the IRI as a Full IRI...
                    strPrefix = null;
                }

                // Set a new Property...
                var theProperty = {};
                theProperty.prefix = strPrefix;
                theProperty.localPart = strIRI;
                // Get a new PropertyUI...
                var thePropertyUI = this.#createNewPropertyIU(theProperty, theRootNodeUI);

                // Set a new Object Node based on the column for the Property...
                var theNode = JSON.parse( JSON.stringify( RDFTransformUINode.getDefaultNode() ) );
                theNode.valueSource.source = RDFTransform.gstrColumn;
                theNode.valueSource.columnName = column.name;
                // Get a new NodeUI...
                var theNodeUI = this.#createNewNodeIU(theNode, thePropertyUI);
                // Create a NodeUIs array of one NodeUI...
                var theNodeUIs = [];
                theNodeUIs.push(theNodeUI); // ...only one Node per Property!

                // Update the PropertyUI with the NodeUIs array...
                thePropertyUI.setNodeUIs(theNodeUIs);

                // Add the PropertyUI to the PropertyUIs array...
                thePropertyUIs.push(thePropertyUI);
            }
        }
        // Update the RootNodeUI with the PropertyUIs array...
        theRootNodeUI.setPropertyUIs(thePropertyUIs);

        return theRootNodeUI;
    }

    #createRootNode() {
        // Retrieve a copy of the Master Root Node...
        return JSON.parse( JSON.stringify( RDFTransform.gnodeMasterRoot ) );
    }

    removeRootNode(theNodeUI) {
        // Get last matching Node...
        var iNodeIndex = this.#nodeUIs.lastIndexOf(theNodeUI);
        // If found...
        if (iNodeIndex >= 0) {
            this.#nodeUIs.splice(iNodeIndex, 1); // ...remove Node from this Property...
            this.#refreshSubjectMappings();
            this.updatePreview();
        }

    }

    #refreshSubjectMappings() {
        this.#theTransform.subjectMappings = [];

        for (const nodeUI of this.#nodeUIs) {
            this.#theTransform.subjectMappings.push( nodeUI.getTransformExport() );
        }
    }

    #createNewPropertyIU(theProperty, theSubjectNodeUI) {
        // Store the node data for the UI...
        var thePropUI = new RDFTransformUIProperty(
            this,
            theProperty,
            null,
            true, // ...expand new property for user convenience
            theSubjectNodeUI
        );
        return thePropUI;
    }

    #createNewNodeIU(theNode, theSubjectPropUI) {
        // Store the node data for the UI...
        var theNodeUI = new RDFTransformUINode(
            this,
            theNode,
            (theSubjectPropUI === null),
            null,
            true, // ...expand new nodes for user convenience
            theSubjectPropUI // ...null for Root Nodes
        );
        if (theSubjectPropUI === null) { // ...a Root Node
            this.#nodeUIs.push(theNodeUI); // ...store it
        }
        return theNodeUI;
    }

    #buildBody() {
        this.#dialog = $(DOM.loadHTML(RDFTransform.KEY, 'scripts/dialogs/rdf-transform.html'));
        this.#dialog.resizable();

        this.#elements = DOM.bind(this.#dialog);

        this.#elements.dialogHeader.text(         $.i18n('rdft-dialog/header')                 );
        this.#elements.rdftDescription.text(      $.i18n('rdft-dialog/desc')                   );
        this.#elements.rdftBaseIRIText.text(      $.i18n('rdft-dialog/base-iri') + ':'         );
        this.#elements.rdftEditBaseIRI.text(      $.i18n('rdft-dialog/edit')                   );
        this.#elements.rdftBaseIRIValue.text(     "Waiting on transform..."                    );
        this.#elements.rdftTabTransformText.text( $.i18n('rdft-dialog/tab-transform')          );
        this.#elements.rdftTabPreviewText.text(   $.i18n('rdft-dialog/tab-preview')            );
        this.#elements.rdftPrefixesText.text(     $.i18n('rdft-dialog/available-prefix') + ':' );
        this.#elements.buttonAddRootNode.text(    $.i18n('rdft-buttons/add-root')              );
        this.#elements.buttonImpTemplate.text(    $.i18n('rdft-buttons/import-template')       );
        this.#elements.buttonExpTemplate.text(    $.i18n('rdft-buttons/export-template')       );
        this.#elements.buttonSaveTransform.text(  $.i18n('rdft-buttons/save')                  );
        this.#elements.buttonOK.text(             $.i18n('rdft-buttons/ok')                    );
        this.#elements.buttonCancel.text(         $.i18n('rdft-buttons/cancel')                );

        var imgAddPrefix =
            $('<img />')
            .attr('src', ModuleWirings[RDFTransform.KEY] + 'images/add.png')
            .css('cursor', 'pointer');
        this.#elements.buttonAddPrefix
            .append(imgAddPrefix)
            .append(" " + $.i18n('rdft-prefix/add'));

        var imgManagePrefixes =
            $('<img />')
            .attr('src', ModuleWirings[RDFTransform.KEY] + 'images/configure.png')
            .css('cursor', 'pointer');
        this.#elements.buttonManagePrefixes
            .append(imgManagePrefixes)
            .append(" " + $.i18n('rdft-prefix/manage'));

        // TODO: Add refresh all button

        const strSample =
            $.i18n('rdft-dialog/sample-turtle', this.iSampleLimit) +
            ( RDFTransform.gbRowBased ? $.i18n("rdft-dialog/sample-row") : $.i18n("rdft-dialog/sample-rec") );
        this.#elements.rdftSampleTurtleText.html( strSample );

        // Set initial spinners...
        this.#elements.rdftPrefixesContainer.append(this.#imgLineBounce);
        this.#elements.rdftTransformData.append(this.#imgLargeSpinner);

        this.#functionalizeDialog();

        this.#level = DialogSystem.showDialog(this.#dialog);

        //
        //   AFTER show...
        //

        // Set Description box minimum height to its initial height...
        const iDescHeight = this.#elements.rdftDescription.height();
        this.#elements.rdftDescription.css({ "minHeight" : "" + iDescHeight + "px" });

        // Get the base pane heights as the initial heights...
        this.#iBaseTransformPaneHeight = this.#elements.rdftTransformData.height();
        this.#iBasePreviewPaneHeight = this.#elements.rdftPreviewData.height();

        // Get the initial Body Header div height as its initial height...
        this.#iBodyHeadInit = this.#elements.dialogBodyHead.height();
    }

    #functionalizeDialog() {
        // Hook up the Transform and Preview tabs...
        this.#elements.rdftTabs
            .tabs(
                { activate :
                    (evt, ui ) => {
                        // If Preview tab activation...
                        if ( ui.newPanel.is("#rdf-transform-tab-preview") ) {
                            // Process any outstanding preview updates...
                            this.#processPreviewTab();
                        }
                    }
                }
            );

        // Hook up the BaseIRI Editor...
        this.#elements.rdftEditBaseIRI
            .on("click", (evt) => {
                    evt.preventDefault();
                    this.#editBaseIRI( $(evt.target) );
                }
            );

        // Hook up the Add Prefix button...
        this.#elements.buttonAddPrefix
            .on("click",
                (evt) => {
                    evt.preventDefault();
                    this.#namespacesManager.addNamespace(false, false, false);
                }
            );

        // Hook up the Manage Prefix button...
        this.#elements.buttonManagePrefixes
            .on("click",
                (evt) => {
                    evt.preventDefault();
                    this.#namespacesManager.showManageWidget();
                }
            );

        // Hook up the Add New Root Node button...
        this.#elements.buttonAddRootNode
            .on("click", (evt) => {
                    evt.preventDefault();
                    var nodeRoot = this.#createRootNode();
                    var nodeUI = this.#createNewNodeIU(nodeRoot, null); // ...adds to #nodeUIs
                    this.#theTransform.subjectMappings.push(nodeRoot);
                    nodeUI.processView(this.#tableNodes);
                }
            );

        // Hook up the Import RDF Template button...
        this.#elements.buttonImpTemplate
            .on("click", (evt) => {
                    evt.preventDefault();
                    this.#doImport();
                }
            );

        // Hook up the Export RDF Template button...
        this.#elements.buttonExpTemplate
            .on("click", (evt) => {
                    evt.preventDefault();
                    this.#doExport();
                }
            );

        // Hook up the Save RDFTransform button...
        this.#elements.buttonSaveTransform
            .on("click", (evt) => {
                    evt.preventDefault();
                    this.#doSave();
                }
            );

        this.#elements.buttonOK
            .on("click", () => {
                    this.#doSave();
                    $(document).off("keydown", this.#doKeypress);
                    DialogSystem.dismissUntil(this.#level - 1);
                }
            );
        this.#elements.buttonCancel
            .on("click", () => {
                $(document).off("keydown", this.#doKeypress);
                DialogSystem.dismissUntil(this.#level - 1);
            }
        );

        // Hook up resize...
        this.#iDiffFrameHeight = 0;
        this.#iLastDiffFrameHeight = 0;
        this.#iResize = 0;
        this.#dialog
            .on("resize",
                (evt, ui) => {
                    clearTimeout(this.#iResize);
                    this.#iResize = setTimeout(
                        () => {
                            this.#iDiffFrameHeight = ui.size.height - 600;
                            if (this.#iDiffFrameHeight != this.#iLastDiffFrameHeight) {
                                const iDiff = this.#iDiffFrameHeight +
                                                ( this.#iBodyHeadInit - this.#elements.dialogBodyHead.height() );
                                this.#elements.rdftTransformData.height(this.#iBaseTransformPaneHeight + iDiff);
                                this.#elements.rdftPreviewData.height(this.#iBasePreviewPaneHeight + iDiff);
                                this.#iLastDiffFrameHeight = this.#iDiffFrameHeight;
                            }
                        },
                        100 // ...do it 1/10 second after no more resizing,
                            //    otherwise, keep resetting timeout...
                    );
                }
            );

        // Prevent OpenRefine from processing ESC and closing
        $(document).on("keydown", this.#doKeypress);
    }

    #doKeypress(evt) {
        // Catch "ESC" key...
        if (evt.key == "Escape") {
            evt.preventDefault();
            return false;
        }
    }

    async #doImport() {
        this.#doSave(); // ...for undo

        var theTransform = null;
        theTransform = await RDFImportTemplate.importTemplate();
        if ( theTransform === null ) {
            return;
        }
        //DialogSystem.dismissUntil(this.#level - 1); // ...kill this dialog
        this.#elements.rdftTransformData.empty().append(this.#imgLargeSpinner);
        this.#initTransform(theTransform);
    }

    #doExport() {
        const theTransform = this.#doSave(); // ...for undo
        RDFExportTemplate.exportTemplate(theTransform);
    }

    #doSave() {
        const theTransform = this.getTransformExport();
        /* DEBUG
        console.log(theTransform);
        */

        // Update the oracle on the RDF Transform...
        Refine.postProcess(
            RDFTransform.KEY,       // module
            'save-rdf-transform',   // command
            {},                     // params
            { [RDFTransform.KEY] : JSON.stringify( theTransform ) },
            {},                     // updateOps
            {   onDone: (data) => { // callbacks
                    theProject.overlayModels.RDFTransform = theTransform;
                    if (data.code === "error") {
                        alert($.i18n('rdft-dialog/error') + ": " + "Save failed!"); // TODO: $.i18n()
                    }
                    else if (data.code === "pending") {
                        alert("Save pending history queue processing."); // TODO: $.i18n()
                    }
                }
            }
        );
        return theTransform;
    }

    async #processNamespaces() {
        // Initialize namespaces...
        this.#namespacesManager = new RDFTransformNamespacesManager(this);
        await this.#namespacesManager.init();
    }

    #processTransformTab() {
        //
        // Process Transform Tab
        //
        this.#elements.rdftTransformData.empty().append(this.#imgLargeSpinner);

        var table = $('<table />').addClass("rdf-transform-pane-table-layout");
        this.#tableNodes = table[0];

        for (const nodeUI of this.#nodeUIs) {
            nodeUI.processView(this.#tableNodes);
        }

        this.#elements.rdftTransformData.empty().append(table);
    }

    #processPreviewTab() {
        if (! this.#bUpdatePreview) {
            return;
        }
        this.#bUpdatePreview = false;

        //
        // Process Preview Tab
        //
        const theTransform = this.getTransformExport();
        /* DEBUG
        console.log(theTransform);
        */
        this.#elements.rdftPreviewData.empty().append(this.#imgLargeSpinner);

        // Consult the oracle on the RDF Preview...
        Refine.postProcess(
            RDFTransform.KEY,       // module
            "preview-rdf",          // command
            {},                     // params
            {   [RDFTransform.KEY] : JSON.stringify( theTransform ),
                "engine"           : JSON.stringify( ui.browsingEngine.getJSON() ),
                "SampleLimit"      : this.iSampleLimit
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

                    this.#elements.rdftPreviewData.empty().html("<pre>" + strPreview + "</pre>");
                }
            }
        );
    }

    getBaseIRI() {
        return this.#theTransform.baseIRI;
    }

    #editBaseIRI(target) {
        var menu = MenuSystem.createMenu().width('300px'); // ...6:1 on input size
        menu.html(
'<div id="rdf-transform-base-iri-value">' +
  '<input type="text" bind="rdftNewBaseIRIValue" size="50" />' +
  '<button class="button" bind="buttonApply">'  + $.i18n('rdft-buttons/apply')  + '</button>' +
  '<button class="button" bind="buttonCancel">' + $.i18n('rdft-buttons/cancel') + '</button>' +
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
                // If the Preview Tab is active...
                if (this.#elements.rdftTabs.tabs('option', 'active') === 1) {
                    // Process the Preview Tab...
                    this.#processPreviewTab();
                }
                // Otherwise, reserve Preview Tab processing for later...
                else {
                    this.updatePreview();
                }
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
            Refine.postCSRF(
                "command/rdf-transform/save-baseIRI",
                {   "project" : theProject.id,
                    "baseIRI" : strIRI
                },
                (data) => {
                    if (data.code === "error") {
                        alert($.i18n('rdft-dialog/error') + ": " + data.message);
                        return; // ...don't replace or update anything
                    }
                },
                "json"
            );
        }
        this.#elements.rdftBaseIRIValue.empty().text(strIRI);
    }

    getNamespaces() {
        return this.#theTransform.namespaces;
    }

    getNamespacesManager() {
        return this.#namespacesManager;
    }

    waitOnNamespaces() {
        this.#elements.rdftPrefixesContainer.empty().append(this.#imgLineBounce);
    }

    updateNamespaces(theNamespaces) {
        this.#elements.rdftPrefixesContainer.empty();
        for (const strPrefix in theNamespaces) {
            this.#elements.rdftPrefixesContainer.append(
                $('<span/>')
                    .addClass('rdf-transform-prefix-box')
                    .attr('title', theNamespaces[strPrefix])
                    .text(strPrefix)
            );
        }
    }

    updatePreview() {
        // Set the Preview to update when we select the Preview Tab...
        this.#bUpdatePreview = true;
    }

    getTransformExport() {
        var theTransform = {};
        theTransform.extension = RDFTransform.strEXTENSION;
        theTransform.version   = RDFTransform.strVERSION;

        // Get the current base IRI...
        theTransform.baseIRI = this.#theTransform.baseIRI;

        // Get the current namespaces...
        theTransform.namespaces = {};
        if ( ! this.#namespacesManager.isNull() )
        {
            theTransform.namespaces = this.#namespacesManager.getNamespaces();
        }

        // Get the current Subject Mapping nodes...
        theTransform.subjectMappings = [];
        for (const nodeUI of this.#nodeUIs) {
            if (nodeUI) {
                var nodeSubject = nodeUI.getTransformExport();
                if (nodeSubject !== null) {
                    theTransform.subjectMappings.push(nodeSubject);
                }
            }
        }

        return theTransform;
    }
}
