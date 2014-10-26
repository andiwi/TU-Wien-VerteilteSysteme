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
			/*
			 * create a new tcp socket at specified host and port
			 */
			this.socket = new Socket(config.getString("controller.host"),
					config.getInt("controller.tcp.port"));
						
			// create a writer to send messages to the server
			this.serverWriter = new PrintWriter(
					socket.getOutputStream(), true);
			
			// create a reader to retrieve messages send by the server
			this.serverReader = new BufferedReader(
					new InputStreamReader(socket.getInputStream()));
			
			new Thread(shell).start();

			shell.writeLine("Client is up! Enter command");

		} catch (UnknownHostException e) {
			System.out.println("Cannot connect to host: " + e.getMessage());
			closeAllStreams();
			
		} catch (IOException e) {
			System.out.println(e.getClass().getSimpleName() + ": "
					+ e.getMessage());
			closeAllStreams();
		}
	}

	@Override
	@Command
	public String login(String username, String password) throws IOException
	{
		serverWriter.println("!login " + username + " " + password);
		return getResponse();
	}

	@Override
	@Command
	public String logout() throws IOException {
		serverWriter.println("!logout");
		return getResponse();
	}

	@Override
	@Command
	public String credits() throws IOException {
		serverWriter.println("!credits");
		return getResponse();
	}

	@Override
	@Command
	public String buy(long credits) throws IOException {
		serverWriter.println("!buy " + credits);
		return getResponse();
	}

	@Override
	@Command
	public String list() throws IOException {
		serverWriter.println("!list");
		return getResponse();
	}

	@Override
	@Command
	public String compute(String term) throws IOException {
		serverWriter.println("!compute " + term);
		return getResponse();
	}

	@Override
	@Command
	public String exit() throws IOException {
		serverWriter.println("!exit");
		closeAllStreams();
		
		return "Exit Client";
	}
	
	private void closeAllStreams()
	{
		// Stop the Shell from listening for commands
		shell.close();
		
		if (serverWriter != null)
			serverWriter.close();
		
		if (serverReader != null)
			try
			{
				serverReader.close();
			} catch (IOException e) {
				// Ignored because we cannot handle it
			}
		
		if (socket != null && !socket.isClosed())
		{
			try {
				socket.close();
			} catch (IOException e) {
				// Ignored because we cannot handle it
			}
		}
	}
	
	private String getResponse()
	{
		String response;
		try
		{
			response = serverReader.readLine();
		}catch(Exception e)
		{
			response = "Connection lost.";
			closeAllStreams();
		}
		return response;
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

}
