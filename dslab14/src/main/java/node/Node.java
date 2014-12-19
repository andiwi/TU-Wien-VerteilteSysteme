package node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import util.Config;
import cli.Command;
import cli.Shell;

public class Node implements INodeCli, Runnable {

	private Config config;
	private Shell shell;
	private ServerSocket serverSocket;
	private DatagramSocket datagramSocket;
	private Set<Character> operators;
	private Timer isAliveTimer;
	private String componentName;
	private AtomicInteger resources;
	
	//counts how many nodes gave back a !ok after the !hello message
	//is Integer.MIN_VALUE if one node gave back a !nok
	private AtomicInteger okNodes; 
	private int nodesInNetwork;
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
	public Node(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.config = config;
		this.shell = new Shell(componentName, userRequestStream, userResponseStream);
		this.componentName = componentName;
		shell.register(this);
		this.resources = new AtomicInteger(0);
		this.okNodes = new AtomicInteger(0);
	}

	@Override
	public void run() {
		
		this.operators = readOperatorConfig();
		
		new Thread(shell).start();
		
		try {
			shell.writeLine("starting node...");
		} catch (IOException e1) {
			exit();
		}
		
		// create and start a new TCP ServerSocket
		try
		{
			serverSocket = new ServerSocket(config.getInt("tcp.port"));
			// handle incoming connections from controller with a ThreadPool in a separate thread
			new ListenerThreadTCP(this, serverSocket, operators, componentName, config).start();
		} catch (IOException e)
		{
			System.err.println("Cannot listen on TCP port." + e);
			exit();
		}
		
		boolean joinedNetwork = sendHelloMessage();
		if(joinedNetwork && okNodes.get() >= 0)
		{
			sendingIsAlivePackages();
		
			try {
				shell.writeLine("Node is up! Enter command.");
			} catch (IOException e1) {
				exit();
			}
		}else 
		{
			try {
				shell.writeLine("Node cannot join network because there are too little resources. Please shutdown the node.");
			} catch (IOException e1) {
				exit();
			}
		}
	}

	private Set<Character> readOperatorConfig()
	{
		Set<Character> operators = new HashSet<Character>();
		
		String operatorStr = config.getString("node.operators");
		
		for(int i = 0; i < operatorStr.length(); i++)
		{
			operators.add(operatorStr.charAt(i));
		}
		return operators;
	}

	private boolean sendHelloMessage()
	{
		try {
			// open a new DatagramSocket
			datagramSocket = new DatagramSocket();

			byte[] buffer;
			DatagramPacket packet;
			
			String message = "!hello";
			// convert the input String to a byte[]
			buffer = message.getBytes();
			
			// create the datagram packet with all the necessary information
			// for sending the packet to the server
			packet = new DatagramPacket(buffer, buffer.length,
					InetAddress.getByName(config.getString("controller.host")),
					config.getInt("controller.udp.port"));
			
			datagramSocket.send(packet);
			
			//Get response 
			byte[] responseBuffer = new byte[1024];
			// create a datagram packet of specified length (buffer.length)
			DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
			datagramSocket.receive(responsePacket);
			
			// receive the data from the packet
			String response = new String(responsePacket.getData());
			response = response.substring(0, responsePacket.getLength());
			return handleHelloResponse(response);
			
		} catch (SocketException e)
		{
			exit();
		} catch (UnknownHostException e) 
		{
			exit();
		} catch (IOException e)
		{
			exit();
		}
		
		return false;
		
	}
	
	private boolean handleHelloResponse(String response)
	{
		String[] responseStrings = response.split("\n");
		
		nodesInNetwork = responseStrings.length-2;
		
		List<NodeModel> nodes = new ArrayList<NodeModel>();
		
		for(int i = 1; i < responseStrings.length-1; i++)
		{
			String[] split = responseStrings[i].split(":");
			NodeModel n = new NodeModel();
			n.setIp(split[0]);
			n.setTcpPort(Integer.parseInt(split[1]));
			nodes.add(n);
		}
		
		int rMax = Integer.parseInt(responseStrings[responseStrings.length-1]);
		
		int resourceForEachNode;
		if(nodesInNetwork > 0)
		{
			resourceForEachNode = rMax / (nodesInNetwork+1);
		}else
		{
			resourceForEachNode = rMax;
		}
		
		if(resourceForEachNode >= config.getInt("node.rmin")) 
		{
			if(nodes.isEmpty()) {
				return true; //this is the first node in the network
			}else
			{
				return twoPhaseCommit(resourceForEachNode, nodes);
			}	
		}
		return false;
	}

	private boolean twoPhaseCommit(int resourceForEachNode, List<NodeModel> nodes)
	{
		ExecutorService pool = Executors.newFixedThreadPool(nodes.size());
		
		for(NodeModel n : nodes)
		{
			pool.execute(new TwoPhaseCommitHandlerThread(n.getIp(), n.getTcpPort(), resourceForEachNode));
		}
		//wait till all threads are finished
		pool.shutdown();
		try 
		{
			pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			return false;
		}
		return true;
	}

	private void sendingIsAlivePackages()
	{
		try {
			byte[] buffer;
			DatagramPacket packet;
			
			String isAliveMessage = "!alive " + config.getString("tcp.port") + " " + config.getString("node.operators");

			// convert the input String to a byte[]
			buffer = isAliveMessage.getBytes();
			// create the datagram packet with all the necessary information
			// for sending the packet to the server
			packet = new DatagramPacket(buffer, buffer.length,
					InetAddress.getByName(config.getString("controller.host")),
					config.getInt("controller.udp.port"));

			TimerTask isAliveTimerTask = new IsAliveTimerTask(datagramSocket, packet);
			isAliveTimer = new Timer(true);
			isAliveTimer.scheduleAtFixedRate(isAliveTimerTask, 0, config.getInt("node.alive"));
				
		} catch (UnknownHostException e) {
			System.err.print(e);
			exit();
		}
	}

	@Override
	@Command
	public String exit() {
		// Stop the Shell from listening for commands
		shell.close();				
				
		if(isAliveTimer != null)
			isAliveTimer.cancel();
				
		if (serverSocket != null)
		{
			try {
				serverSocket.close();
			} catch (IOException e) {
				// Ignored because we cannot handle it
			}
		}
		
		if (datagramSocket != null)
		{
			datagramSocket.close();
		}
		
		return "Exit Node";
	}

	@Override
	public String history(int numberOfRequests) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Node} component,
	 *            which also represents the name of the configuration
	 */
	public static void main(String[] args) {
		Node node = new Node(args[0], new Config(args[0]), System.in,
				System.out);
		new Thread((Runnable) node).start();
	}

	// --- Commands needed for Lab 2. Please note that you do not have to
	// implement them for the first submission. ---

	@Override
	public String resources() throws IOException {
		return this.resources +"";
	}

	public void setResources(int resourceForNode)
	{
		this.resources.set(resourceForNode);
	}
	
	public AtomicInteger getOkNodes()
	{
		return this.okNodes;
	}

	public void setOkNodes(int okNodes)
	{
		this.okNodes.set(okNodes);
	}
	
	
	
	
	
	public class TwoPhaseCommitHandlerThread extends Thread
	{
		private String ip;
		private int port;
		private int resourceForNode;

		private PrintWriter nodeWriter;
		private BufferedReader nodeReader;
		private Socket socket;

		public TwoPhaseCommitHandlerThread(String ip, int port,
				int resourceForNode)
		{
			this.ip = ip;
			this.port = port;
			this.resourceForNode = resourceForNode;
		}
		
		@Override
		public void run() {
			int connectionCounter = 0;
			// try to get connection
			while (socket == null && connectionCounter < 10)
			{
				try
				{
					/*
					 * create a new tcp socket at specified host and port
					 */
					socket = new Socket(ip, port);

					// create a writer to send messages to the other node
					nodeWriter = new PrintWriter(socket.getOutputStream(), true);

					// create a reader to retrieve messages send by the other node
					nodeReader = new BufferedReader(new InputStreamReader(
							socket.getInputStream()));

					shell.writeLine("Connected to other node!");
				} catch (UnknownHostException e)
				{
					socket = null;
					nodeWriter = null;
					nodeReader = null;
				} catch (IOException e)
				{
					socket = null;
					nodeWriter = null;
					nodeReader = null;
				}
				connectionCounter++;
			}
			
			sendShareAndHandleResponse();
			closeAllStreams();
		}
		
		private void sendShareAndHandleResponse()
		{
			String request = "!share " + resourceForNode;
			nodeWriter.println(request);
			
			String response;
			try
			{
				response = nodeReader.readLine();
			

				if (response.equals("!ok")) 
				{
					getOkNodes().getAndIncrement();
				}else if (response.equals("!nok"))
				{
					getOkNodes().getAndSet(Integer.MIN_VALUE);
				}
				while(getOkNodes().get() < nodesInNetwork && getOkNodes().get() >= 0)
				{
					try
					{
						sleep(1);
					} catch (InterruptedException e)
					{
						getOkNodes().getAndSet(Integer.MIN_VALUE);
						sendRollback();
					}
				}
				
				if(getOkNodes().get() == nodesInNetwork)
				{
					sendCommit();
				}else if(getOkNodes().get() < 0)
				{
					sendRollback();
				}
			} catch (IOException e1)
			{
				getOkNodes().getAndSet(Integer.MIN_VALUE);
				sendRollback();
				closeAllStreams();
			}
		}

		private void sendCommit()
		{
			String request = "!commit " + resourceForNode;
			nodeWriter.println(request);
		}

		private void sendRollback()
		{
			String request = "!rollback " + resourceForNode;
			nodeWriter.println(request);
		}
		
		private void closeAllStreams()
		{
			if (nodeWriter != null)
				nodeWriter.close();

			if (nodeReader != null)
				try
				{
					nodeReader.close();
				} catch (IOException e)
				{
					// Ignored because we cannot handle it
				}

			if (socket != null)
			{
				try
				{
					socket.close();
				} catch (IOException e)
				{
					// Ignored because we cannot handle it
				}
			}
		}
	}

	


	
}
