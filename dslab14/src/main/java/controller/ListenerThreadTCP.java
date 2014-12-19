package controller;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ListenerThreadTCP extends Thread
{
	private ServerSocket serverSocket;
	private ConcurrentMap<String, User> users;
	private ConcurrentMap<Integer, Node> nodes;
	private boolean running;

	public ListenerThreadTCP(ServerSocket serverSocket, ConcurrentMap<String, User> users, ConcurrentMap<Integer, Node> nodes) {
		this.serverSocket = serverSocket;
		this.users = users;
		this.nodes = nodes;
		this.running = true;
	}
	
	public void run() {

		ExecutorService executor = Executors.newFixedThreadPool(users.size());
		List<Socket> sockets = new ArrayList<Socket>();
		while (running) {
			Socket socket = null;
			try
			{
				// wait for Client to connect
				socket = serverSocket.accept();
				sockets.add(socket);
				// handle incoming connections from client with a ThreadPool in a separate thread
				executor.execute(new ClientThreadTCP(socket, users, nodes));
								
			} catch (IOException e)
			{
				executor.shutdownNow();
				for(Socket s : sockets)
					try
					{
						s.close();
					} catch (IOException e1)
					{
						// Ignored because we cannot handle it
					}
				running = false;
			} 
		}
	}
}
