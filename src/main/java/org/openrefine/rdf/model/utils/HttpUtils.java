/*
 *  Class HttpUtils
 *
 *  A class to hold the static HTTP Utility functions.  Provides "get"
 *  functionality given a URL.
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

package org.openrefine.rdf.model.utils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.openrefine.rdf.RDFTransform;
import org.openrefine.rdf.model.Util;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.core5.http.HttpEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpUtils {
    private static Logger logger = LoggerFactory.getLogger("RDFT:HttpUtils"); // HttpUtils.class.getSimpleName()

    public static final String USER_AGENT =
        "Mozilla/5.0 (compatible;) OpenRefine/3.6.2 " +
        RDFTransform.EXTENSION + "/" + RDFTransform.VERSION;
    public static final int CONNECTION_TIMEOUT = 10;
    public static final int SOCKET_TIMEOUT = 60;
    private static final int MAX_REDIRECTS = 3;

    private static CloseableHttpClient createClient() {
        //HttpParams httpParams = new BasicHttpParams();
        //httpParams.setParameter(CoreProtocolPNames.USER_AGENT, USER_AGENT);
        //httpParams.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, SOCKET_TIMEOUT);
        //httpParams.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, CONNECTION_TIMEOUT);
        //httpParams.setBooleanParameter(ClientPNames.HANDLE_REDIRECTS ,true);
        //httpParams.setIntParameter(ClientPNames.MAX_REDIRECTS, MAX_REDIRECTS);
        //return new DefaultHttpClient(httpParams);

        CloseableHttpClient client =
            HttpClients.custom()
//              .setSSLSocketFactory(
//                  new SSLConnectionSocketFactory(
//                      SSLContexts.createSystemDefault(),
//                      new String[] { "TLSv1.2" },
//                      null,
//                      SSLConnectionSocketFactory.getDefaultHostnameVerifier() ) )
//              .setConnectionTimeToLive(1, TimeUnit.MINUTES)
//              .setDefaultSocketConfig(
//                  SocketConfig.custom()
//                      .setSoTimeout(SOCKET_TIMEOUT)
//                      .build() )
                .setUserAgent(USER_AGENT)
                .setDefaultRequestConfig(
                    RequestConfig.custom()
                        .setConnectTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)
                        .setResponseTimeout(SOCKET_TIMEOUT, TimeUnit.SECONDS)
                        .setRedirectsEnabled(true)
                        .setMaxRedirects(MAX_REDIRECTS)
//                      .setCookieSpec(CookieSpecs.STANDARD_STRICT)
                        .build() )
                .build();

        return client;
    }

    public static HttpEntity get(String strURL) throws IOException {
        if ( Util.isDebugMode() ) HttpUtils.logger.info("DEBUG: GET request at " + strURL);
        HttpGet getter = new HttpGet(strURL);
        return HttpUtils.get(getter);
    }

    public static HttpEntity get(String strURL, String accept) throws IOException {
        if ( Util.isDebugMode() ) HttpUtils.logger.info("DEBUG: GET request at " + strURL);
        HttpGet getter = new HttpGet(strURL);
        getter.setHeader("Accept", accept);
        return HttpUtils.get(getter);
    }

    private static HttpEntity get(HttpGet getter) throws IOException {
        CloseableHttpClient client = HttpUtils.createClient();
        CloseableHttpResponse response = client.execute(getter);
        if ( response.getCode() == 200 ) {
            return response.getEntity();
        }
        else {
            String strErrorMessage =
                "GET request failed: " +
                response.getCode() + " " + response.getReasonPhrase();
            HttpUtils.logger.error("ERROR: " + strErrorMessage);
            throw new ClientProtocolException(strErrorMessage);
        }
    }
}
