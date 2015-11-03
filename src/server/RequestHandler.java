package server;

public class RequestHandler implements Runnable {
	
	Server server;
	
	public RequestHandler(Server server) {
		this.server = server;
	}

	@Override
	public void run() {
		while(true) {
			int requesters = this.server.getNumberOfRequesters();
			long connections = server.getConnections();
			if(requesters > 0 && connections < 10) {
				server.runNext();
			}
		}
	}

}
