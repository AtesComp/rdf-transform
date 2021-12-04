package com.google.refine.rdf.utils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.google.refine.rdf.Util;

//import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
//import org.apache.hc.client5.http.protocol.RedirectStrategy;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.core5.http.HttpEntity;

//import org.apache.http.HttpEntity;
//import org.apache.http.HttpResponse;
//import org.apache.http.client.ClientProtocolException;
//import org.apache.http.client.HttpClient;
//import org.apache.http.client.methods.HttpGet;
//import org.apache.http.client.params.ClientPNames;
//import org.apache.http.impl.client.DefaultHttpClient;
//import org.apache.http.params.BasicHttpParams;
//import org.apache.http.params.CoreConnectionPNames;
//import org.apache.http.params.CoreProtocolPNames;
//import org.apache.http.params.HttpParams;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpUtils {
	private static Logger logger = LoggerFactory.getLogger("RDFT:HttpUtils"); // HttpUtils.class.getSimpleName()

	public static final String USER_AGENT = "OpenRefine RDF Transform Extension";
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
//				.setSSLSocketFactory(
//					new SSLConnectionSocketFactory(
//						SSLContexts.createSystemDefault(),
//						new String[] { "TLSv1.2" },
//						null,
//						SSLConnectionSocketFactory.getDefaultHostnameVerifier() ) )
//				.setConnectionTimeToLive(1, TimeUnit.MINUTES)
//				.setDefaultSocketConfig(
//					SocketConfig.custom()
//						.setSoTimeout(SOCKET_TIMEOUT)
//						.build() )
				.setUserAgent(USER_AGENT)
				.setDefaultRequestConfig(
					RequestConfig.custom()
						.setConnectTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)
						.setResponseTimeout(SOCKET_TIMEOUT, TimeUnit.SECONDS)
						.setRedirectsEnabled(true)
						.setMaxRedirects(MAX_REDIRECTS)
//						.setCookieSpec(CookieSpecs.STANDARD_STRICT)
						.build() )
				.build();

		return client;
    }

	public static HttpEntity get(String strURL) throws IOException {
		if ( Util.isDebugMode() ) logger.info("DEBUG: GET request at " + strURL);
        HttpGet getter = new HttpGet(strURL);
        return get(getter);
	}

	public static HttpEntity get(String strURL, String accept) throws IOException {
		if ( Util.isDebugMode() ) logger.info("DEBUG: GET request at " + strURL);
        HttpGet getter = new HttpGet(strURL);
        getter.setHeader("Accept", accept);
        return get(getter);
	}

	private static HttpEntity get(HttpGet getter) throws IOException {
		CloseableHttpClient client = createClient();
		CloseableHttpResponse response = client.execute(getter);
		if ( response.getCode() == 200 ) {
			return response.getEntity();
		}
		else {
			String strErrorMessage =
				"Error performing GET request: " +
				response.getCode() + " " + response.getReasonPhrase();
			logger.error(strErrorMessage);
			throw new ClientProtocolException(strErrorMessage);
		}
	}
}
