package com.google.refine.rdf.model.vocab;


public class RDFSProperty extends RDFNode{

    public RDFSProperty(String[] astrLoader) {
        super(astrLoader);
    }

    public RDFSProperty(String strIRI) {
        super(strIRI);
    }

    @Override
    public String getType() {
        //return RDFSProperty.class.getSimpleName();
        return "property";
    }
}
