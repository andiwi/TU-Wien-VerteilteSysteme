package node;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import util.Config;
import controller.ClientThreadTCP;
import cli.Shell;

public class ListenerThreadTCP extends Thread{

	private ServerSocket serverSocket;
	private Shell nodeShell;
	private Set<Character> operators;
	
	public ListenerThreadTCP(ServerSocket serverSocket, Shell nodeShell, Set<Character> operators) {
		this.serverSocket = serverSocket;
		this.nodeShell = nodeShell;
		this.operators = operators;
	}
	
	public void run() {
		try {
			nodeShell.writeLine("starting new ListenerThreadTCP...");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		ExecutorService executor = Executors.newCachedThreadPool();
		
		while (true) {
			Socket socket = null;
			try
			{
				// wait for Client to connect
				socket = serverSocket.accept();
				
				// handle incoming connections from client with a ThreadPool in a separate thread
				executor.execute(new ComputeThreadTCP(socket, this.nodeShell, this.operators));
				
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
