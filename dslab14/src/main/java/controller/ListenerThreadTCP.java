package controller;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ListenerThreadTCP extends Thread
{
	private ServerSocket serverSocket;
	private Map<String, User> users;
	private Map<Integer, Node> nodes;
	private boolean running;

	public ListenerThreadTCP(ServerSocket serverSocket, Map<String, User> users, Map<Integer, Node> nodes) {
		this.serverSocket = serverSocket;
		this.users = users;
		this.nodes = nodes;
		this.running = true;
	}
	
	public void run() {

		ExecutorService executor = Executors.newFixedThreadPool(users.size());
		
		while (running) {
			Socket socket = null;
			try
			{
				// wait for Client to connect
				socket = serverSocket.accept();
				
				// handle incoming connections from client with a ThreadPool in a separate thread
				executor.execute(new ClientThreadTCP(socket, users, nodes));
								
			} catch (IOException e)
			{
				executor.shutdownNow();
				running = false;
			} 
		}
	}
}
