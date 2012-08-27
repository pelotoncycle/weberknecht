/*
 *  Copyright (C) 2012 Roderick Baier
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 *  	http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 */

package de.roderick.weberknecht;

import java.net.URI;
import java.util.HashMap;

import java.util.LinkedHashMap;
import org.apache.commons.codec.binary.Base64;


public class WebSocketHandshake
{
	private URI url = null;
	private String origin = null;
	private String protocol = null;
	private String nonce = null;
	
	
	public WebSocketHandshake(URI url, String protocol, String origin)
	{
		this.url = url;
		this.protocol = protocol;
		this.origin = origin;
		this.nonce = this.createNonce();
	}
	
	
	public byte[] getHandshake()
	{
		String path = url.getPath();
		String host = url.getHost();
		
		if (url.getPort() != -1) {
			host += ":" + url.getPort();
		}
		
		LinkedHashMap<String, String> header = new LinkedHashMap<String, String>();
		header.put("Host", host);
		header.put("Upgrade", "websocket");
		header.put("Connection", "Upgrade");
		header.put("Sec-WebSocket-Version", String.valueOf(WebSocket.getVersion()));
		header.put("Sec-WebSocket-Key", this.nonce);
		
		if (this.protocol != null) {
			header.put("Sec-WebSocket-Protocol", this.protocol);
		}
		
		if (this.origin != null) {
			header.put("Origin", this.origin);
		}
		
		String handshake = "GET " + path + " HTTP/1.1\r\n";
		handshake += this.generateHeader(header);
		handshake += "\r\n";
		
		byte[] handshakeBytes = new byte[handshake.getBytes().length];
		System.arraycopy(handshake.getBytes(), 0, handshakeBytes, 0, handshake.getBytes().length);
		
		return handshakeBytes;
	}
	
	
	private String generateHeader(LinkedHashMap<String, String> headers) {
		String header = new String();
		for (String headerName : headers.keySet()) {
			header += headerName + ": " + headers.get(headerName) + "\r\n";
		}
		return header;
	}
	
	
	private String createNonce() {
        byte[] nonce = new byte[16];
        for (int i = 0; i < 16; i++) {
            nonce[i] = (byte) rand(0, 255);
        }
        return Base64.encodeBase64String(nonce);
    }
	
	
	public void verifyServerStatusLine(String statusLine)
		throws WebSocketException
	{
		int statusCode = Integer.valueOf(statusLine.substring(9, 12));
		
		if (statusCode == 407) {
			throw new WebSocketException("connection failed: proxy authentication not supported");
		}
		else if (statusCode == 404) {
			throw new WebSocketException("connection failed: 404 not found");
		}
		else if (statusCode != 101) {
			throw new WebSocketException("connection failed: unknown status code " + statusCode);
		}
	}
	
	
	public void verifyServerHandshakeHeaders(HashMap<String, String> headers)
		throws WebSocketException
	{
		if (!headers.get("Upgrade").toLowerCase().equals("websocket")) {
			throw new WebSocketException("connection failed: missing header field in server handshake: Upgrade");
		}
		else if (!headers.get("Connection").equals("Upgrade")) {
			throw new WebSocketException("connection failed: missing header field in server handshake: Connection");
		}
	}
	
	
	private int rand(int min, int max)
	{
		int rand = (int) (Math.random() * max + min);
		return rand;
	}
}
