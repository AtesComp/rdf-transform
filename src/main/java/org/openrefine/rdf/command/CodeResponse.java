/*
 *  Class CodeResponse
 *
 *  A class to hold a general server code response indicating success ("ok")
 *  or failure ("error") and its associated message.
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
    public static final CodeResponse error = new CodeResponse("error", true);
}
