package org.openrefine.rdf.model.vocab;


public class RDFTProperty extends RDFTNode{

    public RDFTProperty(String[] astrLoader) {
        super(astrLoader);
    }

    public RDFTProperty(String strIRI) {
        super(strIRI);
    }

    @Override
    public String getType() {
        //return RDFTProperty.class.getSimpleName();
        return "property";
    }
}
