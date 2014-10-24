package controller;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cli.Shell;

public class ListenerThreadTCP extends Thread
{
	private ServerSocket serverSocket;
	private Map<String, User> users;
	private Map<Integer, Node> nodes;
	private Shell controllerShell;

	public ListenerThreadTCP(ServerSocket serverSocket, Shell controllerShell, Map<String, User> users, Map<Integer, Node> nodes) {
		this.serverSocket = serverSocket;
		this.controllerShell = controllerShell;
		this.users = users;
		this.nodes = nodes;
	}
	
	public void run() {

		try {
			controllerShell.writeLine("starting new ListenerThreadTCP...");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		ExecutorService executor = Executors.newFixedThreadPool(users.size());
		
		while (true) {
			Socket socket = null;
			try
			{
				// wait for Client to connect
				socket = serverSocket.accept();
				
				// handle incoming connections from client with a ThreadPool in a separate thread
				executor.execute(new ClientThreadTCP(socket, this.controllerShell, users, nodes));
				
			} catch (IOException e)
			{
				System.err
						.println("Error occurred while waiting for/communicating with client: "
								+ e.getMessage());
				break;
			} 
		}
	}
}
