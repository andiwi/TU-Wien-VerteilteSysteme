package node;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import util.Config;

public class ListenerThreadTCP extends Thread{

	private Node node;
	private ServerSocket serverSocket;
	private Set<Character> operators;
	private boolean running;
	private String componentName;
	private Config config;
	
	public ListenerThreadTCP(Node node, ServerSocket serverSocket, Set<Character> operators, String componentName, Config config) {
		this.node = node;
		this.serverSocket = serverSocket;
		this.operators = operators;
		this.running = true;
		this.componentName = componentName;
		this.config = config;
	}
	
	public void run()
	{
		ExecutorService executor = Executors.newCachedThreadPool();
		
		while (running) {
			Socket socket = null;
			try
			{
				// wait for Client to connect
				socket = serverSocket.accept();
				
				// handle incoming connections from client with a ThreadPool in a separate thread
				executor.execute(new IncomingRequestHandlerThreadTCP(node, socket, this.operators, this.componentName, this.config));
				
			} catch (IOException e)
			{
				executor.shutdownNow();
				running = false;
			} 
		}
	}

}
