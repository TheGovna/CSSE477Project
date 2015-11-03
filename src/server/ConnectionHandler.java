/*
 * ConnectionHandler.java
 * Oct 7, 2012
 *
 * Simple Web Server (SWS) for CSSE 477
 * 
 * Copyright (C) 2012 Chandan Raj Rupakheti
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
 */

package server;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;

import plugins.IPlugin;
import plugins.IServlet;
import protocol.AbstractRequest;
import protocol.DeleteRequest;
import protocol.GetRequest;
import protocol.HttpRequest;
import protocol.HttpResponse;
import protocol.HttpResponseFactory;
import protocol.PostRequest;
import protocol.Protocol;
import protocol.ProtocolException;
import protocol.PutRequest;

/**
 * This class is responsible for handling a incoming request by creating a
 * {@link HttpRequest} object and sending the appropriate response be creating a
 * {@link HttpResponse} object. It implements {@link Runnable} to be used in
 * multi-threaded environment.
 * 
 * @author Chandan R. Rupakheti (rupakhet@rose-hulman.edu)
 */
public class ConnectionHandler implements Runnable {
	private Server server;
	private Socket socket;
	
	private long requestTimer;
	private HashMap<String, AbstractRequest> map;

	public ConnectionHandler(Server server, Socket socket) {
		this.server = server;
		this.socket = socket;
		
		this.map = new HashMap<String, AbstractRequest>();
		this.map.put(Protocol.GET, new GetRequest(this.server));
		this.map.put(Protocol.POST, new PostRequest(this.server));
		this.map.put(Protocol.PUT, new PutRequest(this.server));
		this.map.put(Protocol.DELETE, new DeleteRequest(this.server));
	}

	/**
	 * @return the socket
	 */
	public Socket getSocket() {
		return socket;
	}

	/**
	 * The entry point for connection handler. It first parses incoming request
	 * and creates a {@link HttpRequest} object, then it creates an appropriate
	 * {@link HttpResponse} object and sends the response back to the client
	 * (web browser).
	 */
	public void run() {
		// Get the start time
		long start = System.currentTimeMillis();

		InputStream inStream = null;
		OutputStream outStream = null;

		try {
			inStream = this.socket.getInputStream();
			outStream = this.socket.getOutputStream();
		} catch (Exception e) {
			// Cannot do anything if we have exception reading input or output
			// stream
			// May be have text to log this for further analysis?
			e.printStackTrace();

			// Increment number of connections by 1
			server.incrementConnections(1);
			// Get the end time
			long end = System.currentTimeMillis();
			this.server.incrementServiceTime(end - start);
			return;
		}

		// At this point we have the input and output stream of the socket
		// Now lets create a HttpRequest object
		HttpRequest request = null;
		HttpResponse response = null;
		try {
			request = HttpRequest.read(inStream);
			this.requestTimer = System.currentTimeMillis();

			// Parse the request
			String[] uri = request.getUri().split("/");
			if (uri.length == 2 && !uri[0].equals("")) {
				String requestTypeString = request.getMethod();
				String pluginString = uri[0];
				String servletString = uri[1];
				
				HashMap<String, IPlugin> plugins = this.server.getPlugins();
				IPlugin currPlugin = plugins.get(pluginString);

				if (currPlugin == null) {
					throw new ProtocolException(Protocol.NOT_FOUND_CODE,
							"This plugin doesn't exist");
				}

				IServlet servlet = currPlugin.getServlet(servletString);

				if (servlet == null) {
					throw new ProtocolException(Protocol.NOT_FOUND_CODE,
							"This servlet doesn't exist");
				}
				System.out.println(requestTypeString);
				switch (requestTypeString) {
				case Protocol.GET:
					response = servlet.doGet(request, response);
					break;
				case Protocol.POST:
					response = servlet.doPost(request, response);
					break;
				case Protocol.PUT:
					response = servlet.doPut(request, response);
					break;
				case Protocol.DELETE:
					response = servlet.doDelete(request, response);
					break;
				}

			}else{
				for(String s: uri){
					System.out.println(s);
				}
			}
		} catch (ProtocolException pe) {
			// We have some sort of protocol exception. Get its status code and
			// create response
			// We know only two kind of exception is possible inside
			// fromInputStream
			// Protocol.BAD_REQUEST_CODE and Protocol.NOT_SUPPORTED_CODE
			int status = pe.getStatus();
			if (status == Protocol.BAD_REQUEST_CODE) {
				response = HttpResponseFactory.createRequest("400",
						Protocol.CLOSE);
			} else if (status == Protocol.NOT_FOUND_CODE) {
				response = HttpResponseFactory.createRequest("404",
						Protocol.CLOSE);
			}
			// TODO: Handle version not supported code as well
		} catch (Exception e) {
			e.printStackTrace();
			// For any other error, we will create bad request response as well
			response = HttpResponseFactory.createRequest("400", Protocol.CLOSE);
		}

		if (response != null) {
			// Means there was an error, now write the response object to the
			// socket
			try {
				response.write(socket.getOutputStream());
				// System.out.println(response);
			} catch (Exception e) {
				// We will ignore this exception
				e.printStackTrace();
			}

			// Increment number of connections by 1
			server.incrementConnections(1);
			// Get the end time
			long end = System.currentTimeMillis();
			this.server.incrementServiceTime(end - start);
			return;
		}

		// We reached here means no error so far, so lets process further
		try {
			// Fill in the code to create a response for version mismatch.
			// You may want to use constants such as Protocol.VERSION,
			// Protocol.NOT_SUPPORTED_CODE, and more.
			// You can check if the version matches as follows
			if (!request.getVersion().equalsIgnoreCase(Protocol.VERSION)) {
				// Here you checked that the "Protocol.VERSION" string is not
				// equal to the
				// "request.version" string ignoring the case of the letters in
				// both strings
				// TODO: Fill in the rest of the code here
			}

			AbstractRequest req = map.get(request.getMethod());
			req.setRequest(request);
			req.setResponse(response);
			System.out.println(req);
			response = req.execute();

			/*
			 * else if(request.getMethod().equalsIgnoreCase(Protocol.GET)) { //
			 * Map<String, String> header = request.getHeader(); // String date
			 * = header.get("if-modified-since"); // String hostName =
			 * header.get("host");
			 * 
			 * // Handling GET request here // Get relative URI path from
			 * request String uri = request.getUri(); // Get root directory path
			 * from server String rootDirectory = server.getRootDirectory(); //
			 * Combine them together to form absolute file path File file = new
			 * File(rootDirectory + uri); // Check if the file exists
			 * if(file.exists()) { if(file.isDirectory()) { // Look for default
			 * index.html file in a directory String location = rootDirectory +
			 * uri + System.getProperty("file.separator") +
			 * Protocol.DEFAULT_FILE; file = new File(location);
			 * if(file.exists()) { // Lets create 200 OK response response =
			 * HttpResponseFactory.createRequest(file, Protocol.CLOSE); } else {
			 * // File does not exist so lets create 404 file not found code
			 * response =
			 * HttpResponseFactory.createRequest("404",Protocol.CLOSE); } } else
			 * { // Its a file // Lets create 200 OK response response =
			 * HttpResponseFactory.createRequest(file, Protocol.CLOSE); } } else
			 * { // File does not exist so lets create 404 file not found code
			 * response =
			 * HttpResponseFactory.createRequest("404",Protocol.CLOSE); } }
			 */
		} catch (Exception e) {
			e.printStackTrace();
		}

		// TODO: So far response could be null for protocol version mismatch.
		// So this is a temporary patch for that problem and should be removed
		// after a response object is created for protocol version mismatch.
		if (response == null) {
			response = HttpResponseFactory.createRequest("400", Protocol.CLOSE);
		}

		try {
			// Write response and we are all done so close the socket
			response.write(socket.getOutputStream());
			System.out.println("It took " + (System.currentTimeMillis()-this.requestTimer) + " milliseconds to serve this request.");
			socket.close();
		} catch (Exception e) {
			// We will ignore this exception
			e.printStackTrace();
		}

		// Increment number of connections by 1
		server.incrementConnections(1);
		// Get the end time
		long end = System.currentTimeMillis();
		this.server.incrementServiceTime(end - start);
	}
}
