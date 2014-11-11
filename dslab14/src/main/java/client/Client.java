package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import cli.Command;
import cli.Shell;
import util.Config;

public class Client implements IClientCli, Runnable {

	private Config config;
	private Socket socket;
	private PrintWriter serverWriter;
	private BufferedReader serverReader;
	private Shell shell;
	private ConnectorThread connector;
	
	/**
	 * @param componentName
	 *            the name of the component - represented in the prompt
	 * @param config
	 *            the configuration to use
	 * @param userRequestStream
	 *            the input stream to read user input from
	 * @param userResponseStream
	 *            the output stream to write the console output to
	 */
	public Client(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream)
	{
		this.config = config;		
		this.socket = null;
		this.shell = new Shell(componentName, userRequestStream, userResponseStream);
		shell.register(this);
	}

	@Override
	public void run()
	{
		try {
			connector = new ConnectorThread();
			connector.start();
			
			new Thread(shell).start();
			shell.writeLine("Client is up! Enter command");
			
		} catch (IOException e) {
			System.out.println(e.getClass().getSimpleName() + ": "
					+ e.getMessage());
			closeAllStreams();
		}
	}
	
	private String getResponse()
	{
		String response;
		try
		{
			response = serverReader.readLine();
		} catch (Exception e)
		{
			response = "Connection lost.";
			if (connector == null)
			{
				connector = new ConnectorThread();
				connector.start();
			}
		}
		return response;
	}

	private boolean doRequest(String request)
	{
		try
		{
			serverWriter.println(request);
			return true;
		} catch (Exception e)
		{
			if (connector == null)
			{
				connector = new ConnectorThread();
				connector.start();
			}
		}
		return false;
	}

	@Override
	@Command
	public String login(String username, String password)
	{
		doRequest("!login " + username + " " + password);
		return getResponse();
	}

	@Override
	@Command
	public String logout() throws IOException {
		doRequest("!logout");
		return getResponse();
	}

	@Override
	@Command
	public String credits() throws IOException {
		doRequest("!credits");
		return getResponse();
	}

	@Override
	@Command
	public String buy(long credits) throws IOException {
		doRequest("!buy " + credits);
		return getResponse();
	}

	@Override
	@Command
	public String list() throws IOException {
		doRequest("!list");
		return getResponse();
	}

	@Override
	@Command
	public String compute(String term) throws IOException {
		doRequest("!compute " + term);
		return getResponse();
	}

	@Override
	@Command
	public String exit() throws IOException {
		doRequest("!exit");
		closeAllStreams();
		
		return "Exit Client";
	}
	
	private void closeAllStreams()
	{
		// Stop the Shell from listening for commands
		shell.close();
		if(connector != null)
			connector.setTryToConnect(false);
		
		if (serverWriter != null)
			serverWriter.close();
		
		if (serverReader != null)
			try
			{
				serverReader.close();
			} catch (IOException e) {
				// Ignored because we cannot handle it
			}
		
		if (socket != null)
		{
			try {
				socket.close();
			} catch (IOException e) {
				// Ignored because we cannot handle it
			}
		}
	}
	
	

	/**
	 * @param args
	 *            the first argument is the name of the {@link Client} component
	 * @throws IOException 
	 */
	public static void main(String[] args) {
		Client client = new Client(args[0], new Config("client"), System.in,
				System.out);
		// Start the instance in a new thread
		new Thread((Runnable) client).start();
	}

	// --- Commands needed for Lab 2. Please note that you do not have to
	// implement them for the first submission. ---

	@Override
	public String authenticate(String username) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	
	
	
	
	
	
	public class ConnectorThread extends Thread
	{
		private boolean tryToConnect;
				
		public ConnectorThread() {
			this.tryToConnect = true;
		}
		
		public void run() {
			socket = null;
			try
			{
				shell.writeLine("Try to get connection...");
			} catch (IOException e1)
			{}
			while(tryToConnect && socket == null) {
				try {
					/*
					 * create a new tcp socket at specified host and port
					 */
		            socket = new Socket(config.getString("controller.host"),
							config.getInt("controller.tcp.port"));
		            
		            // create a writer to send messages to the server
					serverWriter = new PrintWriter(
							socket.getOutputStream(), true);
					
					// create a reader to retrieve messages send by the server
					serverReader = new BufferedReader(
							new InputStreamReader(socket.getInputStream()));
					
					shell.writeLine("Connected to server!");
		        } catch (UnknownHostException e) {
		            socket = null;
		            serverWriter = null;
		            serverReader = null;
		        } catch (IOException e) {
		            socket = null;
		            serverWriter = null;
		            serverReader = null;
		        }
			}
			connector = null;
		}
		
		public void setTryToConnect(boolean tryToConnect)
		{
			this.tryToConnect = tryToConnect;
		}
	}
	
}
