/*
 * RDFTransformCommon Class
 *
 *	Utility class for all occasions
 */
class RDFTransformCommon {
	// Globals...
	static g_strRDFT_BLANK     = "blank";
	static g_strRDFT_LITERAL   = "literal";
	static g_strRDFT_RESOURCE  = "resource";
	static g_strRDFT_CELLAS    = "cell-as-";
	static g_strRDFT_CBLANK    = RDFTransformCommon.g_strRDFT_CELLAS + RDFTransformCommon.g_strRDFT_BLANK;
	static g_strRDFT_CLITERAL  = RDFTransformCommon.g_strRDFT_CELLAS + RDFTransformCommon.g_strRDFT_LITERAL;
	static g_strRDFT_CRESOURCE = RDFTransformCommon.g_strRDFT_CELLAS + RDFTransformCommon.g_strRDFT_RESOURCE;

	// Locals...
	static strRE_IRI = String.raw`([a-z]([a-z]|\d|\+|-|\.)*):(\/\/(((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:)*@)?((\[(|(v[\da-f]{1,}\.(([a-z]|\d|-|\.|_|~)|[!\$&'\(\)\*\+,;=]|:)+))\])|((\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5]))|(([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=])*)(:\d*)?)(\/(([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)*)*|(\/((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)+(\/(([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)*)*)?)|((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)+(\/(([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)*)*)|((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)){0})(\?((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)|[\uE000-\uF8FF]|\/|\?)*)?(\#((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)|\/|\?)*)?`;
	static strRE_IRI_COMPLETE = "^" + RDFTransformCommon.strRE_IRI + "$";
	static strRE_IRI_EACH = "(" + RDFTransformCommon.strRE_IRI + ")";

	// IRI RegExp match entire string...
	static reIRI_COMPLETE_i = new RegExp(RDFTransformCommon.strRE_IRI_COMPLETE,  "i" );
	// IRI RegExp match on each occurance in string...
	static reIRI_EACH_ig = new RegExp(RDFTransformCommon.strRE_IRI_EACH, "ig");

	static strRE_LINE_TERMINAL = String.raw`\r?\n|\r|\p{Zl}|\p{Zp}`;
	// Line Terminal RegExp match on each occurance in multiline string...
	static reLINE_TERMINAL_gm = new RegExp(RDFTransformCommon.strRE_LINE_TERMINAL, "gm");

	/*
	 * Method toIRILink(strText)
	 *
	 *	Find every IRI in string and convert each to href link
	 */
	static toIRILink(strText) {
		return strText.replace(RDFTransformCommon.reIRI_EACH_ig, "<a href='$1'>$1</a>");
	}

	/*
	 * Method validateIRI(strText)
	 *
	 *	Test that ALL of the given string is a single IRI
	 */
	static validateIRI(strIRI) {
		return RDFTransformCommon.reIRI_COMPLETE_i.test(strIRI);
	}

	/*
	 * Method toHTMLBreaks(strText)
	 *
	 *	Converts all line terminals to HTML breaks
	 */
	 static toHTMLBreaks(strText) {
		return strText.replace(RDFTransformCommon.reLINE_TERMINAL_gm, "<br />");
	 }

	/*
	 * Method shortenLiteral(strLiteral)
	 *
	 *	Shorted a Literal element's string to fit the maximum displayable length
	 */
	static shortenLiteral(strLiteral) {
		// No good string?
		if (! strLiteral)
			return "ERROR: NO STRING";

		var iMax = RDFTransformUINode.iMaxNodeLength;

		// Short string?
		if (strLiteral.length <= iMax)
			return strLiteral;

		var iHalf     = iMax / 2;
		var iPreHalf  = Math.ceil(iHalf) - 1;
		var iPostHalf = strLiteral.length - Math.floor(iHalf) + 2;
		var strPre  = strLiteral.substring(0, iPreHalf);
		var strPost = strLiteral.substring(iPostHalf);
		return ( strPre + '...' + strPost );
	}

	/*
	 * Method shortenResource(strResource)
	 *
	 *	Shorted a Resource element's string to fit the maximum displayable length
	 */
	static shortenResource(strResource) {
		// No good string?
		if (! strResource)
			return "ERROR: NO STRING";

		var iMax = RDFTransformUINode.iMaxNodeLength;

		// Short string?
		if (strResource.length <= iMax)
			return strResource;

		// A Resource has the following:
		//   schema://authority/path?query#fragment

		//
		// Try to shorten all other strings...
		//
		var istrAuth  = strResource.indexOf('//');
		var istrPath  = strResource.indexOf('/', istrAuth + 2)
		var istrQuery = strResource.indexOf('?');
		var istrFrag  = strResource.indexOf('#');

		// Absolute IRI?
		if (istrAuth > 0) {
			// Get a lead...
			var strLead = null;

			//   Order is important: shortest possible to longest lead
			// Start to Path...
			if (istrPath > 0) {
				strLead = strResource.substring(0, istrPath)
			}
			// Start to Query...
			else if (istrQuery > 0) {
				strLead = strResource.substring(0, istrQuery)
			}
			// Start to Fragment...
			else if (istrFrag > 0) {
				strLead = strResource.substring(0, istrFrag)
			}
			// Too long? Get "schema://" only...
			if (strLead == null || strLead + 6 > iMax) { // 3 for the "..." and 3 for minimum remains
				strLead = strResource.substring(0, istrAuth + 2)
			}
			var iLead = strLead.length;

			// Get Remains...
			var strRemains = null;
			var iLen = 0;
			// Order is important: longest possible to shortest remainder...
			// Path to end...
			if ( istrPath > 0 ) {
				strRemains = strResource.substring(istrPath);
				iLen = iLead + strRemains.length + 3;
			}
			// No remains or too long? Query to end...
			if ( ( strRemains == null || iLen > iMax ) && istrQuery > 0 ) {
				strRemains = strResource.substring(istrQuery);
				iLen = iLead + strRemains.length + 3;
			}
			// No remains or too long? Fragment to end...
			if ( ( strRemains == null || iLen > iMax ) && istrFrag > 0) {
				strRemains = strResource.substring(istrFrag);
				iLen = iLead + strRemains.length + 3;
			}

			// All good? Get the results...
			if ( strRemains != null && iLen <= iMax ) {
				return (strLead + "..." + strRemains);
			}
		}
		// Otherwise, treat it as a literal...
		return RDFTransformCommon.shortenLiteral(strResource);
	}
}
