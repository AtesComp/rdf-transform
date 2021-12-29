package com.google.refine.rdf.model.vocab;


public class RDFSClass extends RDFNode{

    public RDFSClass(String[] astrLoader) {
        super(astrLoader);
    }

    public RDFSClass(String strIRI) {
    	super(strIRI);
    }

    @Override
    public String getType() {
        //return RDFSClass.class.getSimpleName();
        return "class";
    }
}
