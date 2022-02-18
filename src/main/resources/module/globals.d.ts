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
