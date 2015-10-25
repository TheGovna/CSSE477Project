/*
 * IServlet.java
 * Oct 25, 2015
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
 
package plugins;

import protocol.HttpRequest;

public class IServlet {
	String servletName;
	String requestType;
	String body;
	
	public IServlet(String[] line) {
		this.servletName = line[1];
		this.requestType = line[0];
		
		if (line.length == 4) {
			this.body = line[3];
		}

	}

	/**
	 * @param request 
	 * 
	 */
	public void genRequest(HttpRequest request) {
		request.setMethod(this.requestType);
		request.setUri("/"+this.servletName);
		
		if (body != null) {
			request.appendBody(this.body);
		}

	}
}
