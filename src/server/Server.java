/*
 * Server.java
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
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import gui.WebServer;
import plugins.IPlugin;
import plugins.WatchDir;
import protocol.HttpRequest;
import protocol.HttpResponse;
import protocol.HttpResponseFactory;
import protocol.Protocol;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

/**
 * This represents a welcoming server for the incoming TCP request from a HTTP
 * client such as a web browser.
 * 
 * @author Chandan R. Rupakheti (rupakhet@rose-hulman.edu)
 */
public class Server implements Runnable {
	private String rootDirectory;
	private int port;
	private boolean stop;
	private ServerSocket welcomeSocket;
	private WatchDir wd;

	private long connections;
	private long serviceTime;

	private WebServer window;

	private String host;
	private HashMap<String, Integer> clientRequests;
	private HashMap<HttpResponse, String> responseClients;
	private HashMap<Integer, String> responseCodes;
	private ArrayList<String> bannedClients;
	private int counter;
	
	private ConnectionFactory factory;
	private Connection connection;
	private Channel channel;
	
	public HashMap<String, ConnectionHandler> clients;

	/**
	 * @param rootDirectory
	 * @param port
	 */
	public Server(String rootDirectory, int port, String host, WebServer window) throws Exception {
		this.rootDirectory = rootDirectory;
		this.port = port;
		this.host = host;
		this.stop = false;
		this.connections = 0;
		this.serviceTime = 0;
		this.window = window;
		this.counter = 0;

		// performance improvement - queue
		this.clientRequests = new HashMap<String, Integer>();
		this.bannedClients = new ArrayList<String>();
		this.clients = new HashMap<String, ConnectionHandler>();
		
		// initialize responseCodes map
		this.responseCodes = new HashMap<Integer, String>();
		this.responseCodes.put(Protocol.OK_CODE, Protocol.GET);
		this.responseCodes.put(Protocol.POST_CODE, Protocol.POST);
		this.responseCodes.put(Protocol.PUT_CODE, Protocol.PUT);
		this.responseCodes.put(Protocol.DELETE_CODE, Protocol.DELETE);
		
		// create Worker Servers
		WorkerServer wsGet = new WorkerServer(Protocol.GET_QUEUE, this);
		WorkerServer wsGet2 = new WorkerServer(Protocol.GET_QUEUE, this);
		WorkerServer wsPut = new WorkerServer(Protocol.PUT_QUEUE, this);
		WorkerServer wsPost = new WorkerServer(Protocol.POST_QUEUE, this);
		WorkerServer wsDelete = new WorkerServer(Protocol.DELETE_QUEUE, this);
		
		Thread getThread = new Thread(wsGet);
		Thread get2Thread = new Thread(wsGet2);
		Thread putThread = new Thread(wsPut);
		Thread postThread = new Thread(wsPost);
		Thread deleteThread = new Thread(wsDelete);
		
		getThread.start();
		get2Thread.start();
		putThread.start();
		postThread.start();
		deleteThread.start();
		
		// retrieve responses from responses queue
		factory = new ConnectionFactory();
		factory.setHost("localhost");
		connection = factory.newConnection();
		channel = connection.createChannel();
		boolean durable = true;
		channel.queueDeclare(Protocol.RESPONSE_QUEUE, durable, false, false, null);
		
		channel.basicQos(1);
		
		final Consumer consumer = new DefaultConsumer(channel) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
					throws IOException {
				String message = new String(body, "UTF-8");
				System.out.println(" [x] Received '" + message + "'");
				try {
					HttpResponse response;
					
					String[] requestParts = message.split("\\" + Protocol.DELIMITER);
					String status = requestParts[0];
					String key = requestParts[requestParts.length - 1];
					
					String requestType = responseCodes.get(Integer.parseInt(status));
					
					
					if (requestType != null) {
						String file = requestParts[1];
						File f = new File(file);
						
						response = HttpResponseFactory.createRequestWithFile(f, Protocol.CLOSE, requestType);
					} else {
						response = HttpResponseFactory.createRequest(status, Protocol.CLOSE);
					}
					ConnectionHandler ch = clients.get(key);
					ch.setResponse(response);
					
				} catch(Exception e) {
					e.printStackTrace();
				} finally {
					System.out.println(" [x] Done");
					channel.basicAck(envelope.getDeliveryTag(), false); // send a proper acknowledgment from the worker, once we're done with a task
				}
			}
		};
		channel.basicConsume(Protocol.RESPONSE_QUEUE, false, consumer);
	}

	/**
	 * Gets the root directory for this web server.
	 * 
	 * @return the rootDirectory
	 */
	public String getRootDirectory() {
		return rootDirectory;
	}

	/**
	 * Gets the port number for this web server.
	 * 
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Returns connections serviced per second. Synchronized to be used in
	 * threaded environment.
	 * 
	 * @return
	 */
	public synchronized double getServiceRate() {
		if (this.serviceTime == 0)
			return Long.MIN_VALUE;
		double rate = this.connections / (double) this.serviceTime;
		rate = rate * 1000;
		return rate;
	}

	/**
	 * Increments number of connection by the supplied value. Synchronized to be
	 * used in threaded environment.
	 * 
	 * @param value
	 */
	public synchronized void incrementConnections(long value) {
		this.connections += value;
	}

	/**
	 * Increments the service time by the supplied value. Synchronized to be
	 * used in threaded environment.
	 * 
	 * @param value
	 */
	public synchronized void incrementServiceTime(long value) {
		this.serviceTime += value;
	}

	/**
	 * The entry method for the main server thread that accepts incoming TCP
	 * connection request and creates a {@link ConnectionHandler} for the
	 * request.
	 */
	public void run() {
		try {
			this.wd = new WatchDir();
			Thread t = new Thread(wd);
			t.start();

			InetAddress host = InetAddress.getByName(this.host);
			this.welcomeSocket = new ServerSocket(port, 5, host);

			long timer = System.currentTimeMillis();

			// Now keep welcoming new connections until stop flag is set to true
			while (true) {
				// getting responses from response queue
				
				// Listen for incoming socket connection
				// This method block until somebody makes a request
				Socket connectionSocket = this.welcomeSocket.accept();

				// Come out of the loop if the stop flag is set
				if (this.stop)
					break;

				String key = "" + connectionSocket.getInetAddress();
				String handlerKey = "" + connectionSocket.getPort(); 
				
				if (!this.bannedClients.contains(key)) {

					// Create a handler for this incoming connection and start
					// the handler in a new thread
					ConnectionHandler handler = new ConnectionHandler(this, connectionSocket);
					
					this.clients.put(key + ":" + handlerKey, handler);
					
					Thread thread = new Thread(handler);

					counter++;

					// This is the number of requests the client has made
					if (this.clientRequests.containsKey(key)) {
						int count = this.clientRequests.get(key);
						this.clientRequests.put(key, count + 1);
					} else {
						this.clientRequests.put(key, 1);
					}
					

					// start the handler in a new thread
					thread.start();

				}
				if (System.currentTimeMillis() - timer > 1000) {
					System.out.println("Number of things served: " + counter);
					timer = System.currentTimeMillis();
					counter = 0;
					System.out.println("The number of banned clients is " + this.bannedClients.size());
					for (Entry<String, Integer> e : this.clientRequests.entrySet()) {
						if (e.getValue() > 50) {
							this.bannedClients.add(e.getKey());
							System.out.println(e.getKey() + " is now banned!");
						}
						this.clientRequests.put(e.getKey(), 0);
					}

				}

			}
			this.welcomeSocket.close();
		} catch (Exception e) {
			window.showSocketException(e);
		}
	}

	/**
	 * Stops the server from listening further.
	 */
	public synchronized void stop() {
		if (this.stop)
			return;

		// Set the stop flag to be true
		this.stop = true;
		try {
			// This will force welcomeSocket to come out of the blocked accept()
			// method
			// in the main loop of the start() method
			Socket socket = new Socket(InetAddress.getLocalHost(), port);

			// We do not have any other job for this socket so just close it
			socket.close();
		} catch (Exception e) {
		}
	}

	/**
	 * Checks if the server is stopped or not.
	 * 
	 * @return
	 */
	public boolean isStoped() {
		if (this.welcomeSocket != null)
			return this.welcomeSocket.isClosed();
		return true;
	}

	/**
	 * @return map of plugins
	 */
	public HashMap<String, IPlugin> getPlugins() {
		return wd.getPlugins();
	}

	public long getConnections() {
		return this.connections;
	}
}
