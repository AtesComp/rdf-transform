/*
 *  Class ToIRIString
 *
 *  A GREL based function that processes a string into an IRI representation.
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
 */

package org.openrefine.rdf.model.expr.functions;

import java.util.Properties;
import java.util.regex.Pattern;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

import org.openrefine.rdf.model.Util;

import org.apache.jena.iri.IRI;

/*
 * Class ToIRIString: Convert string to qualify as an RDF IRI component
 *
 *  NOTE: We don't check for a leading scheme.  We could append the baseIRI
 *      by retrieving the current baseIRI setting from the binding properties.
 */

public class ToIRIString implements Function {

    public Object call(Properties bindings, Object[] args) {
        //String strBaseIRI = bindings.get("baseIRI").toString();

        if (args.length != 1) {
            return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects one string!");
        }
        if (args[0] == null) {
            return new EvalError("null");
        }
        String strConvert = args[0].toString();
        if ( strConvert.isEmpty() ) {
            return new EvalError("empty string");
        }
        return ToIRIString.toIRIString(strConvert);
    }

    static public String toIRIString(String strConvert) {
        String strUnderscore = "_";

        // As basic cleaning:
        // 1. always replace whitespace and unallowed characters with underscores
        // 2. condense underscores
        strConvert =
            Util.replaceAll(strConvert,
                    "[" + Util.WHITESPACE + Pattern.quote("<>\"{}|\\^`") + "]+",
                    strUnderscore)
            .replaceAll("__+", strUnderscore);

        int iTry = 0;
        do {
            // Test if it's an acceptable IRI now (absolute or relative)...
            IRI theIRI = Util.buildIRI(strConvert, true);
            if (theIRI == null) {
                if (iTry > 6) {
                    strConvert = null; // ...cannot convert to IRI
                    break;
                }
                // ...continue by narrowing the conversion string...
            }
            else break;

            switch (iTry) {
                case 0:
                    // Replace any unsupported characters with underscores...
                    strConvert =
                        Util.replaceAll(strConvert,
                            "[^-\\p{N}\\p{L}_\\.~:/\\?#\\[\\]@\\%!\\$&'\\(\\)\\*\\+,;=]+",
                            strUnderscore);
                    break;
                case 1:
                    // Replace (multiple) leading ":/+" or "/+" with underscores...
                    strConvert = Util.replaceFirst(strConvert, "^(:?/+)+", strUnderscore);
                    break;
                case 2:
                    // Replace sub-delim characters with underscores...
                    strConvert =
                        Util.replaceAll(strConvert, "[!\\$&'\\(\\)\\*\\+,;=]+", strUnderscore);
                    break;
                case 3:
                    // Replace gen-delim (but not ":" and "/") characters with underscores...
                    strConvert =
                        Util.replaceAll(strConvert, "[\\?#\\[\\]@]+", strUnderscore);
                    break;
                case 4:
                    // Replace "/" characters with underscores...
                    strConvert =
                        Util.replaceAll(strConvert, "/+", strUnderscore);
                    break;
                case 5:
                    // Replace ":" characters with underscores...
                    strConvert =
                        Util.replaceAll(strConvert, ":+", strUnderscore);
                    break;
                default: //...should not occur but here for completeness...
                    // Replace all but Unreserved characters with underscores...
                    strConvert =
                        Util.replaceAll(strConvert, "[^-\\p{N}\\p{L}_\\.~]+", strUnderscore);
                    break;
            }
            // Condense underscores...
            strConvert = strConvert.replaceAll("__+", strUnderscore);
            ++iTry;
        } while (true);

        return strConvert;
    }

    @Override
    public String getDescription() {
            return "toIRIString() is intended to prepare a string for use as or within an IRI.\n" +
                   "    1. Replace whitespace characters with underscores.\n" +
                   "    2. Replace unsupported characters with underscores.\n" +
                   "    3. Progressively replace with underscores:\n" +
                   "       a. multiple leading :/ or / and all following /\n" +
                   "       b. SubDelimiters: !$&'()*+,;=\n" +
                   "       c. Most General Delimiters: ?#[]@\n" +
                   "       d. /\n" +
                   "       e. :\n" +
                   "    AND replace any repeat underscores with a single underscore.";
    }

    @Override
    public String getParams() {
        return "String s";
    }

    @Override
    public String getReturns() {
        return "String";
    }
}
