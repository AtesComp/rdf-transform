package com.google.refine.rdf.operation;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.Properties;

import com.google.refine.rdf.RDFTransform;
import com.google.refine.rdf.Util;

import com.fasterxml.jackson.core.JsonGenerator;

import com.google.refine.history.Change;
import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.Pool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RDFTransformChange implements Change {
	private final static Logger logger = LoggerFactory.getLogger("RDFT:RDFTransChange");

    final private RDFTransform transformCurrent;
    private RDFTransform transformPrevious = null;

    public RDFTransformChange(RDFTransform theTransform) {
        this.transformCurrent = theTransform;
    }

    public void apply(Project theProject) {
        synchronized (theProject) {
            // Store the working transform as the new previous stored transform...
            this.transformPrevious = (RDFTransform) theProject.overlayModels.get(RDFTransform.EXTENSION);
            // Replace the working transform with the current stored transform...
            theProject.overlayModels.put(RDFTransform.EXTENSION, this.transformCurrent);
        }
    }

    public void revert(Project theProject) {
        synchronized (theProject) {
            // If the transform is NEW (no previous), remove the transform (reset to no transform)...
            if (this.transformPrevious == null) {
                theProject.overlayModels.remove(RDFTransform.EXTENSION);
            }
            // Otherwise, replace the working transform with the previous stored transform...
            else {
                theProject.overlayModels.put(RDFTransform.EXTENSION, this.transformPrevious);
            }
        }
    }

    public void save(Writer theWriter, Properties theOptions) throws IOException {
        theWriter.write("new=");
        RDFTransformChange.writeRDFTransform(this.transformCurrent, theWriter);
        theWriter.write('\n');
        theWriter.write("old=");
        RDFTransformChange.writeRDFTransform(this.transformPrevious, theWriter);
        theWriter.write('\n');
        theWriter.write("/ec/\n"); // ...end of change marker
    }

    public void setPrevious(RDFTransform transformPrevious) {
        this.transformPrevious = transformPrevious;
    }

    static public Change load(LineNumberReader theReader, Pool thePool)
            throws Exception {
        RDFTransform transformPrevious = null;
        RDFTransform transformCurrent = null;

        String strLine;
        while ( ( ( strLine = theReader.readLine() ) != null ) && ! ( "/ec/".equals(strLine) ) ) {
            int iEqualIndex = strLine.indexOf('=');
            CharSequence cseqField = strLine.subSequence(0, iEqualIndex);
            String strValue = strLine.substring(iEqualIndex + 1);

            if ("old".equals(cseqField) && strValue.length() > 0) {
                transformPrevious =
                    RDFTransform.reconstruct(
                        ParsingUtilities.evaluateJsonStringToObjectNode(strValue)
                    );
            }
            else if ("new".equals(cseqField) && strValue.length() > 0) {
                transformCurrent =
                    RDFTransform.reconstruct(
                        ParsingUtilities.evaluateJsonStringToObjectNode(strValue)
                    );
            }
        }

        RDFTransformChange theChange = new RDFTransformChange(transformCurrent);
        theChange.setPrevious(transformPrevious);
        return theChange;
    }

    static private void writeRDFTransform(RDFTransform theTransform, Writer theWriter)
            throws IOException {
        if (theTransform != null) {
            JsonGenerator jsonWriter = ParsingUtilities.mapper.getFactory().createGenerator(theWriter);
            try {
                theTransform.write(jsonWriter);
            }
            catch (UnsupportedOperationException ex) {
                logger.error("ERROR: Writing RDFTransform: ", ex);
                if ( Util.isVerbose() ) ex.printStackTrace();
            }
        }
    }
}
