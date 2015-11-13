/*
 * WebServer.java
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

import java.io.IOException;
import java.util.HashMap;

import plugins.IPlugin;
import plugins.IServlet;
import protocol.HttpRequest;
import protocol.HttpResponse;
import protocol.Protocol;
import protocol.ProtocolException;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.MessageProperties;

public class WorkerServer implements Runnable {
	private String QUEUE_NAME;
	private Server server;
	private ConnectionFactory factory;
	private Connection connection;
	private Channel channel;

	public WorkerServer(String queue, Server server) {
		this.QUEUE_NAME = queue;
		this.server = server;
	}

	@Override
	public void run() {
		//System.out.println("Running worker queue for: " + QUEUE_NAME);
		try {
			factory = new ConnectionFactory();
			factory.setHost("localhost");
			connection = factory.newConnection();
			channel = connection.createChannel();
			boolean durable = true;
			channel.queueDeclare(QUEUE_NAME, durable, false, false, null);
			System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

			channel.basicQos(10);

			Consumer consumer = new DefaultConsumer(channel) {
				@Override
				public void handleDelivery(String consumerTag, Envelope envelope,
						AMQP.BasicProperties properties, byte[] body)
								throws IOException {
					String message = new String(body, "UTF-8");
					HttpRequest request = createRequest(message);
					System.out.println(" [x] Received '" + message + "'");
					try {
						doWork(request);
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						System.out.println(" [x] Done");

						// send a proper acknowledgment from the worker, once we're done with a task
						channel.basicAck(envelope.getDeliveryTag(), false);
					}
				}

				private HttpRequest createRequest(String message) {
					String[] requestParts = message.split("\\" + Protocol.DELIMITER);
					String method = requestParts[0];
					String uri = requestParts[1];
					String version = requestParts[2];
					HashMap<String, String> header = new HashMap<String, String>();
					for(int i = 3; i<requestParts.length-2; i+=2){
						header.put(requestParts[i], requestParts[i + 1]);
					}
					String body = requestParts[requestParts.length - 2];
					String key = requestParts[requestParts.length - 1];
					return new HttpRequest(method, uri, version, header, body, key);
				}
			};
			channel.basicConsume(QUEUE_NAME, false, consumer);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void doWork(HttpRequest request) throws Exception {
		// Parse the request
		try {
			String[] uri = request.getUri().split("/");
			String requestTypeString = request.getMethod();
			String pluginString = uri[1];
			String servletString = uri[2];

			HashMap<String, IPlugin> plugins = server.getPlugins();
			IPlugin currPlugin = plugins.get(pluginString);

			if (currPlugin == null) {
				throw new ProtocolException(Protocol.NOT_FOUND_CODE,
						"This plugin doesn't exist");
			}



			IServlet servlet = currPlugin.getServlet(requestTypeString + ":/" + servletString);

			if (servlet == null) {
				System.out.println("servlet is null");
				throw new ProtocolException(Protocol.NOT_FOUND_CODE,
						"This servlet doesn't exist");
			}
			System.out.println("requestTypeString: " + requestTypeString);
			HttpResponse response = servlet.processRequest(request, null);

			// publish response to response queue
			Connection connection2 = factory.newConnection();
			Channel channel2 = connection2.createChannel();
			boolean durable = true;
			//System.out.println("response: " + response.getBytes(request.getKey()));
			channel2.queueDeclare(Protocol.RESPONSE_QUEUE, durable, false, false, null);
			channel2.basicPublish("", Protocol.RESPONSE_QUEUE, MessageProperties.PERSISTENT_TEXT_PLAIN, response.getBytes(request.getKey()));
		} catch (ProtocolException e) {
			e.printStackTrace();
		}
	}
}
