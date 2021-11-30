package com.google.refine.rdf.expr.func.str;

import java.util.Properties;
import java.util.regex.Pattern;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

import org.eclipse.rdf4j.common.net.ParsedIRI;

/*
 * forIRI: Convert string to qualify as an IRI component
 */

public class forIRI implements Function {

    public Object call(Properties bindings, Object[] args) {
        if (args.length != 1) {
            return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects a single string!");
        }
        if (args[0] == null) {
            return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " Cannot convert Null string!");
        }
        String strConvert = args[0].toString();
        if ( strConvert.length() == 0 ) {
            return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " Cannot convert empty string!");
        }

        int iTry = 0;
        do {
            // Check if it's already an acceptable IRI (absolute or relative)...
            try {
                new ParsedIRI(strConvert);
                break;
            }
            catch (Exception ex) {
                // ...continue by narrowing the conversion string...
            }
            switch (iTry) {
                case 0:
                    // Replace whitespace and unallowed characters with underscores...
                    strConvert =
                        strConvert.
                            replaceAll("\uC2A0", " ").replaceAll("\\h", " ").
                            replaceAll("[\\p{Whitespace}" + Pattern.quote("<>\"{}|\\^`") + "]+", "_");
                    break;
                case 1:
                    // Replace any NOT Supported characters with underscores...
                    strConvert = strConvert.replaceAll("[^-\\p{N}\\p{L}_\\.~:/\\?#\\[\\]@\\%!\\$&'\\(\\)\\*\\+,;=]+", "_");
                    break;
                case 2:
                    // Replace (multiple) leading ":/+" or "/+" with underscores...
                    strConvert = strConvert.replaceFirst("^(:?/+)+", "_");
                    break;
                case 3:
                    // Replace sub-delim characters with underscores...
                    strConvert = strConvert.replaceAll("[!\\$&'\\(\\)\\*\\+,;=]+", "_");
                    break;
                case 4:
                    // Replace gen-delim (but not ":" and "/") characters with underscores...
                    strConvert = strConvert.replaceAll("[\\?#\\[\\]@]+", "_");
                    break;
                case 5:
                    // Replace "/" characters with underscores...
                    strConvert = strConvert.replaceAll("/+", "_");
                    break;
                case 6:
                    // Replace ":" characters with underscores...
                    strConvert = strConvert.replaceAll(":+", "_");
                    break;
                default:
                    // Replace all but Unreserved characters with underscores...
                    strConvert = strConvert.replaceAll("[^-\\p{N}\\p{L}_\\.~", "_");
                    break;
            }
            strConvert = strConvert.replaceAll("__+", "_");
            if (iTry >= 7)
                break;
            ++iTry;
        } while (true);

        return strConvert;
    }

    @Override
    public String getDescription() {
            return "forIRI() is intended to prepare a string for use as or within an IRI.\n" +
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
