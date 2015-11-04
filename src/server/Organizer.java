/*
 * Organizer.java
 * Nov 3, 2015
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
 
package server;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

import protocol.HttpRequest;
import protocol.Protocol;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class Organizer {
	
	private HashMap<String, String> queueMap;
	private ConnectionFactory factory;
	Connection connection;
	Channel channel;
	
	public Organizer() throws IOException, TimeoutException {
		this.queueMap = new HashMap<String, String>();
		this.queueMap.put(Protocol.GET, Protocol.GET_QUEUE);
		this.queueMap.put(Protocol.POST, Protocol.POST_QUEUE);
		this.queueMap.put(Protocol.PUT, Protocol.PUT_QUEUE);
		this.queueMap.put(Protocol.DELETE, Protocol.DELETE_QUEUE);
		
		factory = new ConnectionFactory();
	    factory.setHost("localhost");
	}
	
	public void sendRequest(HttpRequest request) throws Exception {
		connection = factory.newConnection();
	    channel = connection.createChannel();
	    
		boolean durable = true;
		
		String queue = queueMap.get(request.getMethod());
	    channel.queueDeclare(queue, durable, false, false, null);
	    channel.basicPublish("", queue, null, request.getBytes());
	    
	    channel.close();
	    connection.close();
	}
	
}
