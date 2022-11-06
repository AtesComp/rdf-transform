/*
 *  Class ToStrippedLiteral
 *
 *  A GREL based function that end strips a string and converts other consecutive
 *  whitespace to a single space for an RDF Literal.
 *
 *  Copyright 2022 Keven L. Ates
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

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

import org.openrefine.rdf.model.Util;

/*
 * Class ToStrippedLiteral: Convert string to qualify as an RDF Literal
 *
 *  NOTE: We don't check for a leading scheme.  We could append the baseIRI
 *      by retrieving the current baseIRI setting from the binding properties.
 */

public class ToStrippedLiteral implements Function {

    public Object call(Properties bindings, Object[] args) {
        //String strBaseIRI = bindings.get("baseIRI").toString();

        if (args.length != 1) {
            return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects a single string!");
        }
        if (args[0] == null) {
            return null;
        }
        String strConvert = args[0].toString();
        if ( strConvert.isEmpty() ) {
            return new EvalError("empty string");
        }

        // Replace each whitespace character with a simple space character and strip the ends...
        strConvert = Util.replaceAllWhitespace(strConvert).strip();

        return strConvert;
    }

    @Override
    public String getDescription() {
            return "toStrippedLiteral() is intended to minimally clean a literal:\n" +
                    "    1. Replace each whitespace character, including horitontal\n" +
                    "       whitespace, with a normal space.\n" +
                    "    2. Strip the ends of the string removing whitespace.\n" +
                    "There is no requirement to use this function to prepare literals.";
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
