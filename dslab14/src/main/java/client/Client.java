package client;

import java.io.BufferedReader;
import java.io.File;
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
import util.Keys;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import controller.Base64Channel;

public class Client implements IClientCli, Runnable {

	
	private Config config;
	private Socket socket;
	private PrintWriter serverWriter;
	private BufferedReader serverReader;
	private Shell shell;
	private ConnectorThread connector;
	Base64Channel b = new Base64Channel();
	
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
	@Command
	public String authenticate(String username) throws IOException {
		
		Cipher cipher;
		byte[] enctext = null;
		
		// generates a 32 byte secure random number
		SecureRandom secureRandom = new SecureRandom();
		final byte[] number = new byte[32];
		secureRandom.nextBytes(number);
		
		byte[] encnumber = b.encode(number);
		
		try {
			cipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
			// MODE is the encryption/decryption mode
			// KEY is either a private, public or secret key
			// IV is an init vector, needed for AES
			
			File f = new File("keys/client/controller.pub.pem");
			try {
				cipher.init(Cipher.ENCRYPT_MODE, Keys.readPublicPEM(f), secureRandom);
				
				String authenticate = "!authenticate " + username + " " + encnumber;
				byte[] enclogin = cipher.doFinal(authenticate.getBytes());
				enctext = b.encode(enclogin);
				
				
			} catch (InvalidKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BadPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
				
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		doRequest(enctext.toString());
		String s = getResponse();
		
		doRequest("");
		return getResponse();
		
		
		
		
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
