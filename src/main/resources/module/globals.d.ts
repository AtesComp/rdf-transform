declare var Refine: any;
declare var theProject: any;
declare var MenuSystem: any;
declare var DialogSystem: any;
declare var DataTableView: any;
declare var ExpressionPreviewDialog: any;
declare var ExporterManager: any;
declare var ExtensionBar: any;
declare var importPackage: any;
declare var DOM: any;
declare var ui: any;
declare var module: any;
declare var com: any;
declare var org: any;
declare var Packages: any;
declare var butterfly: any;

//declare var RDFTransform: class;
//declare var RDFTransformDialog: class;
//declare var RDFTransformResourceDialog: class;
//declare var RDFTransformUINode: class;
//declare var RDFTransformVocabManager: class;
//declare var RDFTransformNamespacesManager: class;
//declare var RDFTransformNamespaceAdder: class;
//declare var RDFTransformUIProperty: class;
//declare var RDFTransformCommon: class;
//declare var RDFImportTemplate: class;
//declare var RDFExportTemplate: class;
//declare var RDFDataTableView: class;

interface RDFWidget {
    _scheduleUpdate(): any;
    _renderExpressionHistoryTab(): any;
    _renderHelpTab(): any;
}
interface JQueryStatic {
    i18n(value: string, options?: any): string;
    i18n(): { load(dict: string, lang: string) };
    suggest: function | {suggestTerm: any, suggest: {prototype: any, defaults: any}};
}
interface JQuery {
    tabs(): any;
}
