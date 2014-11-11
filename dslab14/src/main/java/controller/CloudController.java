package controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import util.Config;
import cli.Command;
import cli.Shell;

public class CloudController implements ICloudControllerCli, Runnable {

	private Config config;
	private Config userConfig;
	private	Map<String, User> users;
	private Map<Integer, Node> nodes;
	private ServerSocket serverSocket;
	private DatagramSocket datagramSocket;
	private Shell shell;
	private Timer isAvailableTimer;
	

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
	public CloudController(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream, Config userConfig) {
		this.config = config;
		this.userConfig = userConfig;
		this.users = new HashMap<String, User>();
		this.nodes = new HashMap<Integer, Node>();
		this.shell = new Shell(componentName, userRequestStream, userResponseStream);
		/*
		 * Register all commands the Shell should support.
		 * this class implements all desired commands.
		 */
		shell.register(this);
	}

	@Override
	public void run()
	{
		new Thread(shell).start();
		
		try {
			shell.writeLine("starting cloud-controller...");
		} catch (IOException e1) {
			exit();
		}

		this.users = readUserConfig();	

		// create and start a new TCP ServerSocket
		try
		{
			serverSocket = new ServerSocket(config.getInt("tcp.port"));
			
			// handle incoming connections from client in a separate thread
			new ListenerThreadTCP(serverSocket, users, nodes).start();
		} catch (IOException e)
		{
			System.err.println("Cannot listen on TCP port");
			exit();
		}
		// create and start a new UDP DatagramSocket
		try
		{
			// constructs a datagram socket and binds it to the specified port
			datagramSocket = new DatagramSocket(config.getInt("udp.port"));
	
			// create a new thread to listen for incoming packets
			new ListenerThreadUDP(datagramSocket, nodes).start();
		} catch (IOException e)
		{
			System.err.println("Cannot listen on UDP port");
			exit();
		}
		
		checkAvailableNodes();
		
		try {
			shell.writeLine("Controller is up! Enter command.");
		} catch (IOException e1) {
			exit();
		}
	}

	/**
	 * Checks if the Nodes are available (sent the last isAlivePackages between the now and the node.timeout
	 * Sets the status of the nodes to offline or online 
	 */
	private void checkAvailableNodes()
	{
		TimerTask isAvailableTimerTask = new IsAvailableTimerTask(nodes, config.getInt("node.timeout"));
		isAvailableTimer = new Timer(true);
		isAvailableTimer.scheduleAtFixedRate(isAvailableTimerTask, 0, config.getInt("node.checkPeriod"));
	}

	@Override
	@Command
	public String nodes() throws IOException {
		if(nodes.size() == 0)
			return "No known Nodes.";
		
		String info = "";
		int i = 0;
		for(Node n : nodes.values())
		{
			i++;
			info += i + ". " + n.toString() + "\n";
		}
		
		//info = info.substring(0, info.length()-3);
		return info;
	}

	@Override
	@Command
	public String users() throws IOException
	{
		if(users.size() == 0)
			return "No known Users.";
		
		String info = "";
		
		int i=0;
		for(User user : users.values())
		{
			i++;
			info += i + ". " + user.toString() + "\n";
		}
		
		//info = info.substring(0, info.length()-3);
		return info;
	}

	@Override
	@Command
	public String exit() {
		// Stop the Shell from listening for commands
		shell.close();
		
		//logout each logged in user
		logoutAllUsers();
				
		isAvailableTimer.cancel();
				
		if (serverSocket != null && !serverSocket.isClosed())
		{
			try {
				serverSocket.close();
			} catch (IOException e) {
				// Ignored because we cannot handle it
			}
		}
		
		if (datagramSocket != null && !datagramSocket.isClosed())
		{
			datagramSocket.close();
		}
		
		return "Exit CloudController";
	}
	
	private void logoutAllUsers()
	{
		if(users != null)
		{
			for(User u : users.values())
			{
				u.setStatus(Status.offline);
			}
		}
	}

	/**
	 * reads the userConfig and initializes the Users
	 * @return initialized List of Users
	 */
	private Map<String, User> readUserConfig()
	{
		Map<String, User> users = new HashMap<String, User>();
		Set<String> configKeys = userConfig.listKeys();

		for(String key : configKeys)
		{
			String username = key.split("\\.")[0];
			String property = key.split("\\.")[1];
			
			User user;
			if(users.containsKey(username))
			{
				user = users.get(username);
			}else
			{
				user = new User();
				user.setUsername(username);
				user.setStatus(Status.offline);
			}
			
			if(property.equals("password")) {
				user.setPassword(userConfig.getString(username + "." + property));
			} else if(property.equals("credits")) {
				user.setCredits(userConfig.getInt(username + "." + property));
			}
			
			users.put(username, user);
		}

		return users;
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link CloudController}
	 *            component
	 */
	public static void main(String[] args) {
		CloudController cloudController = new CloudController(args[0],
				new Config("controller"), System.in, System.out, new Config("user"));
		// Start the instance in a new thread
		new Thread((Runnable) cloudController).start();
	}
}
