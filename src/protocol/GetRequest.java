/*
 * GetRequest.java
 * Oct 18, 2015
 *
 * Simple Web Server (SWS) for EE407/507 and CS455/555
 * 
 * Copyright (C) 2011 Chandan Raj Rupakheti, Clarkson University
 * 
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License 
 * as published by the Free Software Foundation, either 
 * version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/lgpl.html>.
 * 
 * Contact Us:
 * Chandan Raj Rupakheti (rupakhcr@clarkson.edu)
 * Department of Electrical and Computer Engineering
 * Clarkson University
 * Potsdam
 * NY 13699-5722
 * http://clarkson.edu/~rupakhcr
 */
 
package protocol;

import java.io.File;

import server.Server;

public class GetRequest extends AbstractRequest {
	
	public GetRequest() {}
	
	public GetRequest(Server server) {
		this.server = server;
	}
	
	public GetRequest(HttpRequest request, Server server) {
		this.request = request;
		this.server = server;
	}

	/* (non-Javadoc)
	 * @see protocol.AbstractRequest#execute()
	 */
	@Override
	public HttpResponse execute() throws Exception {
		// Handling GET request here
		// Get relative URI path from request
		String uri = request.getUri();
		// Get root directory path from server
		String rootDirectory = server.getRootDirectory();
		// Combine them together to form absolute file path
		File file = new File(rootDirectory + uri);
		// Check if the file exists
		if(file.exists()) {
			if(file.isDirectory()) {
				// Look for default index.html file in a directory
				String location = rootDirectory + uri + System.getProperty("file.separator") + Protocol.DEFAULT_FILE;
				file = new File(location);
				if(file.exists()) {
					// Lets create 200 OK response
					response = HttpResponseFactory.createRequestWithFile(file, Protocol.CLOSE, Protocol.GET);
				}
				else {
					// File does not exist so lets create 404 file not found code
					response = HttpResponseFactory.createRequest("404",Protocol.CLOSE);
				}
			}
			else { // Its a file
				// Lets create 200 OK response
				response = HttpResponseFactory.createRequestWithFile(file, Protocol.CLOSE, Protocol.GET);
			}
		}
		else {
			// File does not exist so lets create 404 file not found code
			response = HttpResponseFactory.createRequest("404",Protocol.CLOSE);
		}
		
		return response;
	}

}
