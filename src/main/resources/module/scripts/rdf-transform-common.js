/*
 *  Class RDFTransformCommon
 *
 *  Utility class for all occasions
 *
 *  Copyright 2025 Keven L. Ates
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

class RDFTransformCommon {
    static iMaxNodeLength = 35;
    static iMaxExpLength = 35;

    static gstrResource = "resource";
    static gstrBlank = "blank";
    static gstrLiteral = "literal";

    /*
     * NodeType Class
     *
     *  A categorical type class to manage nodes
     */
    static NodeType = class {
        // NOTE: The following string names ("resource", "blank", "literal") are intentionally set
        //  to the values of the HTML Input group titled 'rdf-content-radio' in the dialog file
        //  rdf-transform-node-config.html.  The type (getType) is generally evaluated via the "checked"
        //  'rdf-content-radio' value:
        //      $("input[name='rdf-content-radio']:checked").val() === a name of one of the below...
        //
        static Resource = new this(RDFTransformCommon.gstrResource);
        static Blank    = new this(RDFTransformCommon.gstrBlank);
        static Literal  = new this(RDFTransformCommon.gstrLiteral);

        #name;

        constructor(name) {
            this.#name = name;
        }

        getName() {
            return this.#name;
        }

        static getType(strType) {
            if ( strType === RDFTransformCommon.gstrResource ) {
                return RDFTransformCommon.NodeType.Resource;
            }
            else if ( strType === RDFTransformCommon.gstrLiteral ) {
                return RDFTransformCommon.NodeType.Literal;
            }
            else if ( strType === RDFTransformCommon.gstrBlank ) {
                return RDFTransformCommon.NodeType.Blank;
            }
            return null;
        }
    }

    /*
     * Method toIRIString(strText)
     *
     *  Convert string to an IRI string
     */
    static async toIRIString(strText) {
        // NOTE: Replaced the local string conversion to IRI with the
        //       server-side toIRIString function.  See: rdf-transform-reserve.js to
        //       reimplement.

        if ( ! strText ) {
            return null;
        }

        // To UTF-16...
        //  from() encodes with encoding
        //  toString() decodes from encoding to UTF-16
        //let strConvert = Buffer.from(strText).toString();
        let strConvert = strText.toString();

        if ( strConvert === "" ) {
            return null;
        }

        let data = null;
        try {
            data = await RDFTransformCommon.#convertToIRIString(strConvert);
        }
        catch (evt) {
            // ...ignore error, no IRI...
        }
        if (data !== null && data.code === "ok") {
            return data.message;
        }
        return null;
    }

    static #convertToIRIString(strConvert) {
        return new Promise(
            (resolve, reject) => {
                let params = { [RDFTransform.gstrConvertToIRI] : strConvert };

                // @ts-ignore
                $.ajax(
                    {   url  : gstrCommandRDFTransform + gstrConvertToIRI,
                        type : 'GET',
                        async: false, // ...wait on results
                        data : params,
                        dataType : "json",
                        success : (data, strStatus, xhr) => { resolve(data); },
                        error   : (xhr, strStatus, error) => { resolve(null) }
                    }
                );
            }
        );
    }

    /*
     * Method validateIRI(strIRI)
     *
     *  Test that ALL of the given string is a single IRI
     */
    static async validateIRI(strIRI) {
        // NOTE: Replaced the local IRI validation regular expression with the
        //       server-side IRI parser.  See: rdf-transform-reserve.js to
        //       reimplement.
        //return RDFTransformCommon.#reIRI_COMPLETE_iu.test(strIRI);

        let data = null;
        try {
            data = await RDFTransformCommon.#getValidIRI(strIRI);
        }
        catch (evt) {
            // ...ignore error, bad IRI...
        }
        return (data != null && data.code === "ok");
    }

    static #getValidIRI(strIRI) {
        return new Promise(
            (resolve, reject) => {
                var params = { [RDFTransform.gstrIRI] : strIRI };

                // @ts-ignore
                $.ajax(
                    {   url  : gstrCommandRDFTransform + gstrValidateIRI,
                        type : 'GET',
                        async: false, // ...wait on results
                        data : params,
                        dataType : "json",
                        success : (data, strStatus, xhr) => { resolve(data); },
                        error   : (xhr, strStatus, error) => { resolve(null) }
                    }
                );
            }
        );
    }

    /*
     * Method validateNamespace(strIRI)
     *
     *  Test that ALL of the given string is a single IRI and properly ends
     *  with a prefix suffix "/" or "#"
     */
    static async validateNamespace(strIRI) {
        function endsWith(strTest, strSuffix) {
            return strTest.indexOf(strSuffix, strTest.length - strSuffix.length) !== -1;
        }

        if ( ! await RDFTransformCommon.validateIRI(strIRI) ) {
            alert(
                // @ts-ignore
                $.i18n('rdft-dialog/alert-iri') + "\n" +
                // @ts-ignore
                $.i18n('rdft-dialog/alert-iri-invalid') + "\n" +
                strIRI
            );
            return false;
        }

        if ( !endsWith(strIRI, "/") && !endsWith(strIRI, "#") ) {
            var ans = confirm(
                // @ts-ignore
                $.i18n('rdft-dialog/confirm-one') + "\n" +
                // @ts-ignore
                $.i18n('rdft-dialog/confirm-two'));
            if (ans == false)
                return false;
        }

        return true;
    }

    /*
     * Method shortenExpression(strExp)
     *
     *  Shorted an Expression's string to fit the maximum displayable length
     */
    static shortenExpression(strExp) {
        return RDFTransformCommon.shortenString(strExp, RDFTransformCommon.iMaxExpLength);
    }

    /*
     * Method shortenLiteral(strLiteral)
     *
     *  Shorted a Literal element's string to fit the maximum displayable length
     */
     static shortenLiteral(strLiteral) {
        return RDFTransformCommon.shortenString(strLiteral, RDFTransformCommon.iMaxNodeLength);
    }

    /*
     * Method shortenString(strToShorten, iMaxLen)
     *
     *  Shorted a string to fit the maximum given length by cutting the middle to '...'
     */
     static shortenString(strToShorten, iMaxLen) {
        // No good string?
        if (! strToShorten)
            return "ERROR: NO STRING";

        // Short string?
        if (strToShorten.length <= iMaxLen)
            return strToShorten;

        var iHalf     = iMaxLen / 2;
        var iPreHalf  = Math.ceil(iHalf) - 1;
        var iPostHalf = strToShorten.length - Math.floor(iHalf) + 2;
        var strPre  = strToShorten.substring(0, iPreHalf);
        var strPost = strToShorten.substring(iPostHalf);
        return ( strPre + '...' + strPost );
    }

    /*
     * Method shortenResource(strResource)
     *
     *  Shorted a Resource element's string to fit the maximum displayable length
     */
    static shortenResource(strResource) {
        // No good string?
        if (! strResource)
            return "ERROR: NO STRING";

        var iMax = RDFTransformCommon.iMaxNodeLength;

        // Short string?
        if (strResource.length <= iMax)
            return strResource;

        // A Resource has the following:
        //   schema://authority/path?query#fragment

        //
        // Try to shorten all other strings...
        //
        var iIndexAuth  = strResource.indexOf('//');
        var iIndexPath  = strResource.indexOf('/', iIndexAuth + 2)
        var iIndexQuery = strResource.indexOf('?');
        var iIndexFrag  = strResource.indexOf('#');

        // Absolute IRI?
        if (iIndexAuth > 0) {
            // Get a lead...
            var strLead = null;

            //   Order is important: shortest possible to longest lead
            // Start to Path...
            if (iIndexPath > 0) {
                strLead = strResource.substring(0, iIndexPath)
            }
            // Start to Query...
            else if (iIndexQuery > 0) {
                strLead = strResource.substring(0, iIndexQuery)
            }
            // Start to Fragment...
            else if (iIndexFrag > 0) {
                strLead = strResource.substring(0, iIndexFrag)
            }
            // Too long? Get "schema://" only...
            if (strLead === null || strLead + 6 > iMax) { // 3 for the "..." and 3 for minimum remains
                strLead = strResource.substring(0, iIndexAuth + 2)
            }
            var iLead = strLead.length;

            // Get Remains...
            var strRemains = null;
            var iLen = 0;
            // Order is important: longest possible to shortest remainder...
            // Path to end...
            if ( iIndexPath > 0 ) {
                strRemains = strResource.substring(iIndexPath);
                iLen = iLead + strRemains.length + 3;
            }
            // No remains or too long? Query to end...
            if ( ( strRemains === null || iLen > iMax ) && iIndexQuery > 0 ) {
                strRemains = strResource.substring(iIndexQuery);
                iLen = iLead + strRemains.length + 3;
            }
            // No remains or too long? Fragment to end...
            if ( ( strRemains === null || iLen > iMax ) && iIndexFrag > 0) {
                strRemains = strResource.substring(iIndexFrag);
                iLen = iLead + strRemains.length + 3;
            }

            // All good? Get the results...
            if ( strRemains !== null && iLen <= iMax ) {
                return (strLead + "..." + strRemains);
            }
        }
        // Otherwise, treat it as a literal...
        return RDFTransformCommon.shortenLiteral(strResource);
    }

    static getPreferences(params) {
        return new Promise(
            (resolve, reject) => {
                // @ts-ignore
                $.ajax(
                    {   url  : gstrCommandRDFTransform + gstrGetPreferences,
                        type : 'GET',
                        async: false, // ...wait on results
                        data : params,
                        dataType : "json",
                        success : (data, strStatus, xhr) => { resolve(data); },
                        error   : (xhr, strStatus, error) => { resolve(null) }
                    }
                );
            }
        );
    }

    /*
     * Method saveFile(strTemplate, strName, strExt, strType, strDesc)
     *
     *  Save an RDF Transform template to local storage for later use/reuse
     *      strTemplate: the string containing the RDF Transform template in
     *          JSON format
     *      strName: the suggested file name
     *      strExt: the file name extension
     *      strDesc: the description displayed for the file extension
     */
    static async saveFile(strTemplate, strFilename, strExt, strType, strDesc) {
        const blobPart = [];
        blobPart[0] = strTemplate;
        // Create the blob object...
        var theBlob =
            new Blob( blobPart, { "type" : strType } );

        // Get the File Handler...
        const fileHandle =
            // @ts-ignore
            await window.showSaveFilePicker(
                {   "excludeAcceptAllOption" : true,
                    "suggestedName" : strFilename,
                    "types" :
                    [ { "description" : strDesc,
                        "accept" : { [strType] : [ "." + strExt ] } } ]
                }
            );

        // Get the File Stream...
        const fileStream = await fileHandle.createWritable();

        // Write the file...
        await fileStream.write(theBlob);
        await fileStream.close();
    }

    /*
     * Method readFile(strTemplate, strName, strExt, strType, strDesc)
     *
     *  Read an RDF Transform template from local storage for use
     *      strName: the suggested file name
     *      strExt: the file name extension
     *      strDesc: the description displayed for the file extension
     *
     *      Returns:
     *      strTemplate: the string containing the RDF Transform template in
     *          JSON format
     */
    static async readFile(strExt, strType, strDesc) {
        // Get the File Handler...
        const [ fileHandle ] =
            // @ts-ignore
            await window.showOpenFilePicker(
                {   "excludeAcceptAllOption" : true,
                    "multiple" : false,
                    "types" :
                    [ { "description" : strDesc,
                        "accept" : { [strType] : [ "." + strExt ] } } ]
                }
            );

        // Get the File data...
        const file = await fileHandle.getFile();
        const strTemplate = await file.text();

        return strTemplate;
    }

    /*
     * Qualified Name IRI functions...
     */

    static async isPrefixedQName(strQName) {
        if ( await RDFTransformCommon.validateIRI(strQName) ) {
            var iIndex = strQName.indexOf(':'); // ...first ':'
            if ( strQName.substring(iIndex, iIndex + 3) !== "://" ) {
                return 1; // ...prefixed
            }
            return 0; // ...not prefixed, but good IRI
        }
        return -1; // ...bad IRI
    }

    static getPrefixFromQName(strQName) {
        var iIndex = strQName.indexOf(':');
        if (iIndex === -1) {
            return null;
        }
        // NOTE: Same start (0) and end (0) === "" (baseIRI)
        return strQName.substring(0, iIndex);
    }

    static getSuffixFromQName(strQName) {
        var iIndex = strQName.indexOf(':');
        if (iIndex === -1) {
            return null;
        }
        return strQName.substring(iIndex + 1);
    }

    static getFullIRIFromQName(strPrefixedQName, strBaseIRI, theNamespaces) {
        var objIRIParts = this.#deAssembleQName(strPrefixedQName);
        if ( objIRIParts.prefix === null ) { // ...as Existing Full IRI...
            return objIRIParts.localPart;
        }
        if (objIRIParts.prefix in theNamespaces) { // ...as Known Namespace Full IRI...
            return theNamespaces[objIRIParts.prefix] + objIRIParts.localPart;
        }
        if ( objIRIParts.prefix === "" ) { // ...as BaseIRI appended Full IRI...
            return strBaseIRI + objIRIParts.localPart;
        }
        // ...as Unknown Namespace treated as Full IRI...
        return objIRIParts.prefix + ":" + objIRIParts.localPart;
    }

    static #deAssembleQName(strQName) {
        var bFull = (strQName.indexOf("://") >= 0);
        var iIndex = strQName.indexOf(':');
        var obj = {};
        if (bFull || iIndex === -1) { // ...Full or No ':' Separator, then treat as Full...
            obj.prefix = null;
            obj.localPart = strQName;
        }
        else { // ...otherwise, divide into Prefix and Local Part...
            obj.prefix = strQName.substring(0, iIndex);
            obj.localPart = strQName.substring(iIndex + 1);
        }
        return obj;
    }
}
