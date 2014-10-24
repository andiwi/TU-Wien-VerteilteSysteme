package node;

import util.Config;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import node.ListenerThreadTCP;
import cli.Command;
import cli.Shell;

public class Node implements INodeCli, Runnable {

	private Config config;
	private Shell shell;
	private ServerSocket serverSocket;
	private Set<Character> operators;

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
		shell.register(this);
	}

	@Override
	public void run() {
		
		this.operators = readOperatorConfig();
		
		new Thread(shell).start();
		
		try {
			shell.writeLine("starting node...");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		// create and start a new TCP ServerSocket
		try
		{
			serverSocket = new ServerSocket(config.getInt("tcp.port"));
			// handle incoming connections from client with a ThreadPool in a separate thread
			new ListenerThreadTCP(this.serverSocket, this.shell, operators).start();
		} catch (IOException e)
		{
			throw new RuntimeException("Cannot listen on TCP port.", e);
		}
		
		sendingIsAlivePackages();
		try {
			shell.writeLine("Node is up! Enter command.");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
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

	private void sendingIsAlivePackages()
	{
		try {
			// open a new DatagramSocket
			DatagramSocket datagramSocket = new DatagramSocket();

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
			Timer isAliveTimer = new Timer(true);
			isAliveTimer.scheduleAtFixedRate(isAliveTimerTask, 0, config.getInt("node.alive"));
				
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	@Command
	public String exit() throws IOException {
		// TODO Auto-generated method stub
		return null;
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
		// TODO Auto-generated method stub
		return null;
	}

}
