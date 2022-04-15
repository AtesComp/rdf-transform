package org.openrefine.rdf.command;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CodeResponse {
    private String strCode;
    private String strMessage;
    
    public CodeResponse() {
        this.strCode = "ok";
    }
    
    public CodeResponse(String strMessage) {
        setResponse(false, strMessage);
    }

    public CodeResponse(String strMessage, boolean bError) {
        setResponse(bError, strMessage);
    }

    @JsonProperty("code")
    public String getCode() {
        return strCode;
    }

    @JsonProperty("message")
    public String getMessage() {
        return strMessage;
    }

    private void setResponse(boolean bError, String strMessage) {
        this.strCode = "ok";
        if (bError) {
            this.strCode = "error";
        }
        this.strMessage = strMessage;
    };
    
    public static final CodeResponse ok = new CodeResponse("ok");
    public static final CodeResponse error = new CodeResponse("error");
}
