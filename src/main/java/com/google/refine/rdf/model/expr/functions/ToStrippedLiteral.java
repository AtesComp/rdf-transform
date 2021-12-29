package com.google.refine.rdf.model.expr.functions;

import java.util.Properties;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

public class ToStrippedLiteral implements Function {

    public Object call(Properties bindings, Object[] args) {
        if (args.length != 1) {
            return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects a single string!");
        }
        if (args[0] == null) {
            return null;
        }
        String strConvert = args[0].toString();
        if ( strConvert.isEmpty() ) {
            return strConvert;
        }
        return strConvert.replaceAll("\uC2A0", " ").replaceAll("\\h", " ").strip();
    }

    @Override
    public String getDescription() {
            return "toStrippedLiteral() is intended to minimally prepare a literal.\n" +
                    "    1. Replace non-breaking space characters with normal spaces.\n" +
                    "    2. Replace horitontal whitespace with normal spaces.\n" +
                    "    3. Strip the ends of the string removing whitespace.";
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
