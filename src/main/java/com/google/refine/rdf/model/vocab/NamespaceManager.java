package com.google.refine.rdf.model.vocab;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreType;

@JsonIgnoreType
public class NamespaceManager {
	private Map<String, String> prefixMap = new HashMap<String, String>();

	public NamespaceManager(InputStream inStream) throws IOException {
		BufferedReader buffReader = new BufferedReader( new InputStreamReader(inStream) );
		String strLine, strPrefix = null, strNamespace = null;

		//	Read Prefix file lines...
		//  	There should be 2 entries per line:
		//			Prefix, Namespace
		//		Each entry should be separated by whitespace.
		String[] astrTokens;
		while ( (strLine = buffReader.readLine() ) != null) {
			// Parse entries...
			astrTokens = strLine.split("\\s+");

			// Are there enough entries?
			if (astrTokens.length < 2)
				continue;
			
			// Organize entries...
			strPrefix    = astrTokens[0];
			strNamespace = astrTokens[1];

			// Store prefix map...
			prefixMap.put(strPrefix, strNamespace);
		}
	}

	public String getNamespace(String strPrefix) {
		return prefixMap.get(strPrefix);
	}
}
