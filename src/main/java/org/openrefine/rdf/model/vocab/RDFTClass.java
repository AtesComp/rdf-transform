package org.openrefine.rdf.model.vocab;


public class RDFTClass extends RDFTNode{

    public RDFTClass(String[] astrLoader) {
        super(astrLoader);
    }

    public RDFTClass(String strIRI) {
        super(strIRI);
    }

    @Override
    public String getType() {
        //return RDFTClass.class.getSimpleName();
        return "class";
    }
}
