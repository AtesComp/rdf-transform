/*
 *  RDF Transform Reserve
 *
 *  A place to reserve code not currently used by any process, class, or function.
 *
 *  This file is NOT intended to be loaded for use by the controller for this extension.
 *  See: src/main/resources/module/MOD-INF/controller.js
 *
 *  Copyright 2024 Keven L. Ates
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

/*
 *  The following is extracted from rdf-transform-common.js
 *
 *  *******************************************************
 *  * DO NOT REPLACE RDFTransformCommon WITH THIS CODE!!! *
 *  *******************************************************
 *  Instead, add or modify code as needed.
 *
 */

class Reserve_RDFTransformCommon {

    //
    // Private Variable...
    //

    // -------------------------------------------------------
    // RFC3987
    //
    // The following is a reproduction of RFC3987 to validate
    // IRIs in JavaScript.
    //
    // See: https://datatracker.ietf.org/doc/html/rfc3987.html
    //

    static #strRE_IPRIVATE = "\\u{E000}-\\u{F8FF}\\u{F0000}-\\u{FFFFD}\\u{100000}-\\u{10FFFD}";

    static #strRE_UCSCHAR =
        "\\u{000A0}-\\u{0D7FF}\\u{0F900}-\\u{0FDCF}\\u{0FDF0}-\\u{0FFEF}" +
        "\\u{10000}-\\u{1FFFD}\\u{20000}-\\u{2FFFD}\\u{30000}-\\u{3FFFD}" +
        "\\u{40000}-\\u{4FFFD}\\u{50000}-\\u{5FFFD}\\u{60000}-\\u{6FFFD}" +
        "\\u{70000}-\\u{7FFFD}\\u{80000}-\\u{8FFFD}\\u{90000}-\\u{9FFFD}" +
        "\\u{A0000}-\\u{AFFFD}\\u{B0000}-\\u{BFFFD}\\u{C0000}-\\u{CFFFD}" +
        "\\u{D0000}-\\u{DFFFD}\\u{E1000}-\\u{EFFFD}";

    static #strRE_SUB_DELIMS = "!\\$&'\\(\\)\\*\\+,;=";
    static #strRE_SUB_DELIMS_GRP = "[" + Reserve_RDFTransformCommon.#strRE_SUB_DELIMS + "]";

    static #strRE_GEN_DELIMS = ":\\/\\?\\#\\[\\]@";

    static #strRE_RESERVED = Reserve_RDFTransformCommon.#strRE_SUB_DELIMS + Reserve_RDFTransformCommon.#strRE_GEN_DELIMS;

    static #strRE_ALPHA = "a-z";
    static #strRE_ALPHA_GRP = "[" + Reserve_RDFTransformCommon.#strRE_ALPHA + "]";
    static #strRE_DIGIT = "\\d";
    static #strRE_ALPHA_DIGIT = Reserve_RDFTransformCommon.#strRE_ALPHA + Reserve_RDFTransformCommon.#strRE_DIGIT;

    static #strRE_HEX = "[\\da-f]";

    static #strRE_PCT_ENCODED = "%" + Reserve_RDFTransformCommon.#strRE_HEX + "{2}";
    static #strRE_PCT_ENCODED_GRP = "(?:" + Reserve_RDFTransformCommon.#strRE_PCT_ENCODED + ")";

    static #strRE_UNRESERVED = "-" + Reserve_RDFTransformCommon.#strRE_ALPHA_DIGIT + "\\._~";
    static #strRE_UNRESERVED_GRP = "(?:[" + Reserve_RDFTransformCommon.#strRE_UNRESERVED + "])";

    static #strRE_IUNRESERVED = Reserve_RDFTransformCommon.#strRE_UNRESERVED + Reserve_RDFTransformCommon.#strRE_UCSCHAR;
    static #strRE_IUNRESERVED_GRP = "(?:[" + Reserve_RDFTransformCommon.#strRE_IUNRESERVED + "])";

    static #strRE_SCHEME =
        Reserve_RDFTransformCommon.#strRE_ALPHA_GRP +
        "(?:[-" + Reserve_RDFTransformCommon.#strRE_ALPHA_DIGIT + "\\+\\.])*";

    static #strRE_IUNSUBS = Reserve_RDFTransformCommon.#strRE_IUNRESERVED + Reserve_RDFTransformCommon.#strRE_SUB_DELIMS;

    static #strRE_IREG_NAME_SUBGRP =
        "(?:[" +
            Reserve_RDFTransformCommon.#strRE_IUNSUBS +
        "])|" + Reserve_RDFTransformCommon.#strRE_PCT_ENCODED_GRP;
    static #strRE_IREG_NAME = "(?:" + Reserve_RDFTransformCommon.#strRE_IREG_NAME_SUBGRP + ")*";

    static #strRE_ISEGMENT_NC_BASE = Reserve_RDFTransformCommon.#strRE_IUNSUBS + "@";
    static #strRE_ISEGMENT_NC =
        "(?:[" +
            Reserve_RDFTransformCommon.#strRE_ISEGMENT_NC_BASE +
        "])|" + Reserve_RDFTransformCommon.#strRE_PCT_ENCODED_GRP;

    static #strRE_IPCHAR_BASE = Reserve_RDFTransformCommon.#strRE_ISEGMENT_NC_BASE + ":";
    static #strRE_IPCHAR =
        "(?:[" +
            Reserve_RDFTransformCommon.#strRE_IPCHAR_BASE +
        "])|" + Reserve_RDFTransformCommon.#strRE_PCT_ENCODED_GRP;

    static #strRE_ISEGMENT_BASE = "(?:" + Reserve_RDFTransformCommon.#strRE_IPCHAR + ")";
    static #strRE_ISEGMENT = Reserve_RDFTransformCommon.#strRE_ISEGMENT_BASE + "*";
    static #strRE_ISEGMENT_NZ = Reserve_RDFTransformCommon.#strRE_ISEGMENT_BASE + "+";
    static #strRE_ISEGMENT_NZ_NC = "(?:" + Reserve_RDFTransformCommon.#strRE_ISEGMENT_NC + ")+";

    static #strRE_IPATH_ABEMPTY = "(?:\\/" + Reserve_RDFTransformCommon.#strRE_ISEGMENT + ")*";
    static #strRE_IPATH_ROOTLESS =
        Reserve_RDFTransformCommon.#strRE_ISEGMENT_NZ +
        Reserve_RDFTransformCommon.#strRE_IPATH_ABEMPTY;
    static #strRE_IPATH_ABSOLUTE = "\\/" + "(?:" + Reserve_RDFTransformCommon.#strRE_IPATH_ROOTLESS + ")?";
    static #strRE_IPATH_EMPTY = "";

    static #strRE_DEC_OCTET = "(?:0{0,2}\\d|0{0,1}[1-9]\\d|1\\d\\d|2[0-4]\\d|25[0-5])";
    static #strRE_IPV4 = Reserve_RDFTransformCommon.#strRE_DEC_OCTET + "(?:\\." + Reserve_RDFTransformCommon.#strRE_DEC_OCTET + "){3}";

    static #strRE_H16 = Reserve_RDFTransformCommon.#strRE_HEX + "{1,4}";
    static #strRE_LS32 =
        "(?:" +
            Reserve_RDFTransformCommon.#strRE_H16 + ":" + Reserve_RDFTransformCommon.#strRE_H16 + "|" +
            Reserve_RDFTransformCommon.#strRE_IPV4 +
        ")";
    static #strRE_IPV6 =
                      "(?:" + Reserve_RDFTransformCommon.#strRE_H16 + ":){6}" + Reserve_RDFTransformCommon.#strRE_LS32 + "|" +
                    "::(?:" + Reserve_RDFTransformCommon.#strRE_H16 + ":){5}" + Reserve_RDFTransformCommon.#strRE_LS32 + "|" +
        "(?:" +                                                     Reserve_RDFTransformCommon.#strRE_H16 + ")?" +
                    "::(?:" + Reserve_RDFTransformCommon.#strRE_H16 + ":){4}" + Reserve_RDFTransformCommon.#strRE_LS32 + "|" +
        "(?:" + "(?:" + Reserve_RDFTransformCommon.#strRE_H16 + ":){0,1}" + Reserve_RDFTransformCommon.#strRE_H16 + ")?" +
                    "::(?:" + Reserve_RDFTransformCommon.#strRE_H16 + ":){3}" + Reserve_RDFTransformCommon.#strRE_LS32 + "|" +
        "(?:" + "(?:" + Reserve_RDFTransformCommon.#strRE_H16 + ":){0,2}" + Reserve_RDFTransformCommon.#strRE_H16 + ")?" +
                    "::(?:" + Reserve_RDFTransformCommon.#strRE_H16 + ":){2}" + Reserve_RDFTransformCommon.#strRE_LS32 + "|" +
        "(?:" + "(?:" + Reserve_RDFTransformCommon.#strRE_H16 + ":){0,3}" + Reserve_RDFTransformCommon.#strRE_H16 + ")?" +
                    "::"    + Reserve_RDFTransformCommon.#strRE_H16 + ":" +     Reserve_RDFTransformCommon.#strRE_LS32 + "|" +
        "(?:" + "(?:" + Reserve_RDFTransformCommon.#strRE_H16 + ":){0,4}" + Reserve_RDFTransformCommon.#strRE_H16 + ")?" +
                    "::" +                                              Reserve_RDFTransformCommon.#strRE_LS32 + "|" +
        "(?:" + "(?:" + Reserve_RDFTransformCommon.#strRE_H16 + ":){0,5}" + Reserve_RDFTransformCommon.#strRE_H16 + ")?" +
                    "::" +                                              Reserve_RDFTransformCommon.#strRE_H16 + "|" +
        "(?:" + "(?:" + Reserve_RDFTransformCommon.#strRE_H16 + ":){0,6}" + Reserve_RDFTransformCommon.#strRE_H16 + ")?" +
                    "::";

    static #strRE_IPVFUTURE =
        "v" + Reserve_RDFTransformCommon.#strRE_HEX + "+\\." +
        "(?:[" +
            Reserve_RDFTransformCommon.#strRE_UNRESERVED +
            Reserve_RDFTransformCommon.#strRE_SUB_DELIMS +
            ":" +
        "])+";

    static #strRE_IP_LITERAL =
        "\\[" +
            "(?:" + Reserve_RDFTransformCommon.#strRE_IPV6 + ")" + "|" +
            "(?:" + Reserve_RDFTransformCommon.#strRE_IPVFUTURE + ")" +
        "\\]";

    static #strRE_IUSERINFO = "(?:" + Reserve_RDFTransformCommon.#strRE_IREG_NAME_SUBGRP + "|" + ":" + ")*";
    static #strRE_IUSERINFO_OPT = "(?:" + Reserve_RDFTransformCommon.#strRE_IUSERINFO + "@)?";

    static #strRE_IHOST =
        "(?:" +
            Reserve_RDFTransformCommon.#strRE_IP_LITERAL + "|" +
            Reserve_RDFTransformCommon.#strRE_IPV4 + "|" +
            Reserve_RDFTransformCommon.#strRE_IREG_NAME +
        ")";

    static #strRE_PORT = "\\d*";
    static #strRE_PORT_OPT = "(?:" + Reserve_RDFTransformCommon.#strRE_PORT + ")?";

    static #strRE_IAUTHORITY =
        Reserve_RDFTransformCommon.#strRE_IUSERINFO_OPT +
        Reserve_RDFTransformCommon.#strRE_IHOST +
        Reserve_RDFTransformCommon.#strRE_PORT_OPT;

    static #strRE_IHIER_PART =
        "(?:" +
            "(?:\\/\\/" +
                Reserve_RDFTransformCommon.#strRE_IAUTHORITY +
                Reserve_RDFTransformCommon.#strRE_IPATH_ABEMPTY +
            ")" +
            "|" +
            Reserve_RDFTransformCommon.#strRE_IPATH_ABSOLUTE + "|" +
            Reserve_RDFTransformCommon.#strRE_IPATH_ROOTLESS + "|" +
            Reserve_RDFTransformCommon.#strRE_IPATH_EMPTY +
        ")+"; // ... +, since IPATH_EMPTY === "", therefore {0,1} cardinality

    static #strRE_IQ_IF_BASE_CHARS = Reserve_RDFTransformCommon.#strRE_IPCHAR_BASE + "\\/\\?";

    static #strRE_IQUERY_CHARS =
        "(?:[" +
            Reserve_RDFTransformCommon.#strRE_IQ_IF_BASE_CHARS +
            Reserve_RDFTransformCommon.#strRE_IPRIVATE +
        "])|" + Reserve_RDFTransformCommon.#strRE_PCT_ENCODED_GRP;
    static #strRE_IQUERY = "(?:" + Reserve_RDFTransformCommon.#strRE_IQUERY_CHARS + ")*";
    static #strRE_IQUERY_OPT = "(?:\\?" + Reserve_RDFTransformCommon.#strRE_IQUERY + ")?";

    static #strRE_IFRAGMENT_CHARS =
        "(?:[" +
            Reserve_RDFTransformCommon.#strRE_IQ_IF_BASE_CHARS +
        "])|" + Reserve_RDFTransformCommon.#strRE_PCT_ENCODED_GRP;
    static #strRE_IFRAGMENT = "(?:" + Reserve_RDFTransformCommon.#strRE_IFRAGMENT_CHARS + ")*";
    static #strRE_IFRAGMENT_OPT = "(?:#" + Reserve_RDFTransformCommon.#strRE_IFRAGMENT + ")?";

    static #strRE_IRI =
        Reserve_RDFTransformCommon.#strRE_SCHEME + ":" +
        Reserve_RDFTransformCommon.#strRE_IHIER_PART +
        Reserve_RDFTransformCommon.#strRE_IQUERY_OPT +
        Reserve_RDFTransformCommon.#strRE_IFRAGMENT_OPT;

    //
    // End of RFC3987
    // -------------------------------------------------------

    // -------------------------------------------------------
    // Find IRIs
    //

    static #strRE_IRI_EACH = "(?:" + Reserve_RDFTransformCommon.#strRE_IRI + ")";
    static #strRE_IRI_COMPLETE = "^" + Reserve_RDFTransformCommon.#strRE_IRI_EACH + "$";

    // IRI RegExp match entire string...
    static #reIRI_COMPLETE_iu = new RegExp(Reserve_RDFTransformCommon.#strRE_IRI_COMPLETE, "iu" );
    // IRI RegExp match on each occurance in string...
    static #reIRI_EACH_igu = new RegExp(Reserve_RDFTransformCommon.#strRE_IRI_EACH, "igu");

    //
    // End of: Find IRIs
    // -------------------------------------------------------

    // -------------------------------------------------------
    // Find Line Terminals
    //

    static #strRE_LINE_TERMINAL = "\\r?\\n|\\r|\\p{Zl}|\\p{Zp}";
    // Line Terminal RegExp match on each occurance in multiline string...
    static #reLINE_TERMINAL_gm = new RegExp(Reserve_RDFTransformCommon.#strRE_LINE_TERMINAL, "gmu");

    //
    // End of: Find Line Terminals
    // -------------------------------------------------------

    /*
     * Method toIRILink(strText)
     *
     *  Find every IRI in string and convert each to href link
     */
    static toIRILink(strText) {
        return strText.replace(Reserve_RDFTransformCommon.#reIRI_EACH_igu, "<a href='$1'>$1</a>");
    }

    /*
     * Method toHTMLBreaks(strText)
     *
     *  Converts all line terminals to HTML breaks
     */
     static toHTMLBreaks(strText) {
        return strText.replace(Reserve_RDFTransformCommon.#reLINE_TERMINAL_gm, "<br />");
    }

    /*
     * Method toIRIString(strIRI)
     *
     *  Convert string to an IRI string
     */
    static async toIRIString(strText) {
        if ( ! strText ) {
            return null;
        }

        // To UTF-16...
        //  from() encodes with encoding
        //  toString() decodes from encoding to UTF-16
        //var strConvert = Buffer.from(strText).toString();
        var strConvert = strText.toString();

        if ( strConvert === "" ) {
            return null;
        }

        var iTry = 0; // case 0: No replacements...
        var strReplace = null;
        // While the IRI (absolute or relative) is NOT valid...
        while ( ! ( await RDFTransformCommon.validateIRI(strConvert) ) ) {
            ++iTry;
            //  If the try count is out of range...
            if (iTry > 8) {
                strConvert = null; // ...cannot use the text as an IRI
                break;
            }
            //
            // Continue by narrowing the conversion string...
            //
            switch (iTry) {
                case 1:
                    // Replace whitespace and unallowed characters with underscores...
                    strReplace = strConvert.replace(/[\u{C2A0}\p{C}\p{Z}<>"{}|^`]+/gu, "_");
                    //strReplace = strConvert.replace(/[\p{Cc}\p{Co}\p{Cn}\p{Z}<>"{}|^`]+/gu, "_");
                    break;
                case 2:
                    // Replace any unsupported characters with underscores...
                    strReplace = strConvert.replace(/[^-\p{N}\p{L}_.~:/?#[\]@%!$&'()*+,;=]+/gu, "_");
                    break;
                case 3:
                    // Replace (multiple) leading ":/+" or "/+" with nothing (remove) (first occurrences, not global)...
                    strReplace = strConvert.replace(/^(:?\/+)+/u, "");
                    break;
                case 4:
                    // Replace sub-delim characters with underscores...
                    strReplace = strConvert.replace(/[!$&'()*+,;=]+/gu, "_");
                    break;
                case 5:
                    // Replace gen-delim (but not ":" and "/") characters with underscores...
                    strReplace = strConvert.replace(/[?#[\]@]+/gu, "_");
                    break;
                case 6:
                    // Replace "/" characters with underscores...
                    strReplace = strConvert.replace(/\/+/gu, "_");
                    break;
                case 7:
                    // Replace ":" characters with underscores...
                    strReplace = strConvert.replace(/:+/gu, "_");
                    break;
                case 8:
                default:
                    // Replace all but Unreserved characters with underscores...
                    strReplace = strConvert.replace(/[^-\p{N}\p{L}_\\.~]+/gu, "_");
                    break;
            }
            // Condense any underscores...
            strConvert = strReplace.replace(/__+/gu, "_");
        }

        return strConvert;
    }

}

class Reserve_RDFTransformResourceDialog {
    async #extractPrefixLocalPart(strIRI, strResp) {
        /** @type {{prefix?: string, localPart?: string}} */
        var obj = null;
        var iPrefixedIRI = await RDFTransformCommon.isPrefixedQName(strIRI);
        if ( iPrefixedIRI === 1 ) { // ...Prefixed IRI
            var strPrefix = RDFTransformCommon.getPrefixFromQName(strIRI);
            var strLocalPart = RDFTransformCommon.getSuffixFromQName(strIRI);
            // IRI   = Namespace of Prefix + strLocalPart
            // CIRIE = strResPrefix + ":" + strLocalPart
            obj = {};
            obj.prefix = strPrefix;       // Given Prefix
            obj.localPart = strLocalPart; // Path portion of IRI
            this.#onDone(obj);
        }
        else if ( iPrefixedIRI === 0 ) { // ...Full IRI
            // IRI   = Namespace of Prefix + strLocalPart
            // CIRIE = strResPrefix + ":" + strLocalPart
            obj = {};
            obj.prefix = null;      // No Prefix
            obj.localPart = strIRI; // Full IRI
            this.#onDone(obj);
        }
        else { // iPrefixedIRI === -1 // ...Bad IRI
            alert(
                $.i18n('rdft-dialog/alert-iri') + "\n" +
                $.i18n('rdft-dialog/alert-iri-invalid') + "\n" +
                strResp + strIRI
            );
        }
    }

    async #addPrefixLocalPart(strIRI, strResp) {
        /** @type {{prefix?: string, localPart?: string}} */
        var obj = null;

        // Does the IRI look like a prefixed IRI?
        var iPrefixedIRI = await RDFTransformCommon.isPrefixedQName(strIRI);
        // Is it a prefixed IRI?
        if ( iPrefixedIRI === 1 ) {
            MenuSystem.dismissAll();

            // Divide the IRI into Prefix and LocalPart portions...
            var strPrefix = RDFTransformCommon.getPrefixFromQName(strIRI);
            var strLocalPart = RDFTransformCommon.getSuffixFromQName(strIRI);

            // Is there an existing prefix matching the given prefix?
            if ( strPrefix === "" || this.#dialog.getNamespacesManager().hasPrefix(strPrefix) ) {
                // ...yes (BaseIRI or already managed), add resource...
                obj = {};
                obj.prefix    = strPrefix;    // Given Prefix
                obj.localPart = strLocalPart; // Path portion of IRI
                this.#onDone(obj);
            }
            // No, then this prefix is not using the BaseIRI prefix and is not managed...
            else {
                // ...create prefix (which may change in here) and add (re-prefixed) resource...
                //
                // NOTE: We are passing a function to RDFTransformNamespacesManager.addNamespace() which,
                //      in turn, passes it as an event function to RDFTransformNamespaceAdder.show().
                this.#dialog.getNamespacesManager().addNamespace(
                    $.i18n('rdft-dialog/unknown-pref') + ': <em>' + strPrefix + '</em> ',
                    strPrefix,
                    (strResPrefix) => {
                        // NOTE: Only the prefix (without the related IRI) is returned.  We don't need
                        //      the IRI...addNamespace() added it.  We will get the IRI from the prefix
                        //      manager later to ensure the IRI is present.
                        // Do the original and resulting prefix match?
                        // NOTE: It can change via edits in RDFTransformNamespaceAdder.show()
                        if ( strPrefix.normalize() === strResPrefix.normalize() ) {
                            // ...yes, set as before...
                            obj = {};
                            obj.prefix    = strPrefix;    // Given Prefix
                            obj.localPart = strLocalPart; // Path portion of IRI
                        }
                        // No, then adjust...
                        else {
                            // ...get new Namespace of the Prefix to validate...
                            var strResNamespace =
                                this.#dialog.getNamespacesManager().getNamespaceOfPrefix(strResPrefix);
                            // Ensure the prefix's IRI was added...
                            if ( strResNamespace != null ) {
                                obj = {};
                                obj.prefix    = strResPrefix; // New Prefix
                                obj.localPart = strLocalPart; // Path portion of IRI
                            }
                            // If not, abort the resource addition with a null obj...
                        }

                        // Do we have a good resource (obj) to add?
                        if (obj !== null) {
                            // ...yes, add resource...
                            this.#onDone(obj);
                        }
                    }
                );
            }
        }
        // Is it a full IRI?
        else if ( iPrefixedIRI === 0 ) {
            MenuSystem.dismissAll();

            // ...take it as is...
            //new RDFTransformResourceResolveDialog(this.#element, data, this.#onDone);
            /** @type {{prefix?: string, localPart?: string}} */
            obj = {};
            obj.prefix = null;  // No Prefix
            obj.localPart = strIRI; // Full IRI
            this.#onDone(obj);
        }
        // Is it a BAD IRI?
        else { // iPrefixedIRI === -1
            alert(
                $.i18n('rdft-dialog/alert-iri') + "\n" +
                $.i18n('rdft-dialog/alert-iri-invalid') + "\n" +
                strResp + strIRI
            );
        }
    }
}

/*
 *  CLASS RDFTransformResourceResolveDialog
 *
 *  The resource resolver for the resource manager dialog
 */
class Reserve_RDFTransformResourceResolveDialog {
    #onDone;

    constructor(element, defaultVal, onDone) {
        this.#onDone = onDone;

        var menu = MenuSystem.createMenu().width('400px'); // ...6:1 on input size
        menu.html(
'<div class="rdf-transform-menu-search">' +
  '<span class="rdf-transform-node-label">IRI: ' +
    '<small>(' + $.i18n('rdft-dialog/resolve') + ')</small>' +
  '</span>' +
  '<input type="text" size="50" bind="rdftNewResourceIRI"><br/>' +
  '<button class="button" bind="buttonApply">' +
    $.i18n('rdft-buttons/apply') +
  '</button>' +
  '<button class="button" bind="buttonCancel">' +
    $.i18n('rdft-buttons/cancel') +
  '</button>' +
'</div>'
        );
        MenuSystem.showMenu(menu, () => {} );
        MenuSystem.positionMenuLeftRight(menu, $(element));

        var elements = DOM.bind(menu);
        elements.rdftNewResourceIRI
        .val(defaultVal)
        .focus()
        .select();

        elements.buttonCancel
        .on("click", () => { MenuSystem.dismissAll(); } );

        elements.buttonApply
        .on("click",
            async () => {
                var strIRI = elements.rdftNewResourceIRI.val();
                if (!strIRI) {
                    alert( $.i18n('rdft-dialog/alert-iri') );
                    return;
                }
                if ( ! await RDFTransformCommon.validateIRI(strIRI) ) {
                    alert(
                        $.i18n('rdft-dialog/alert-iri') + "\n" +
                        $.i18n('rdft-dialog/alert-iri-invalid') + "\n" +
                        strIRI
                    );
                    return;
                }
                MenuSystem.dismissAll();
                //if (strIRI.charAt(0) === ':') {
                //    strIRI = strIRI.substring(1);
                //}
                var obj = {
                    "iri"   : strIRI, // Full IRI
                    "cirie" : strIRI  // Prefixed IRI
                };
                this.#onDone(obj);
            }
        );
    }
}
