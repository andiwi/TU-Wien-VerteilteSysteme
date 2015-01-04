package controller;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

public class NodeThreadUDP extends Thread
{
	private DatagramSocket datagramSocket;
	private DatagramPacket packet;
	private ConcurrentMap<Integer, Node> nodes;
	private int rMax;

	public NodeThreadUDP(DatagramSocket datagramSocket, DatagramPacket packet,
			ConcurrentMap<Integer, Node> nodes, int rMax)
	{
		this.datagramSocket = datagramSocket;
		this.packet = packet;
		this.nodes = nodes;
		this.rMax = rMax;
	}

	public void run()
	{
		// get the data from the packet
		String request = new String(packet.getData());

		if(request.substring(0,6).equals(("!hello")))
		{
			handleHelloMessage();
		}else
		{
			handleAliveMessage(request);
			
		}
	}

	private void handleHelloMessage()
	{
		SocketAddress address = packet.getSocketAddress();
		
		byte[] responseBuffer = new byte[1024];
		DatagramPacket responsePacket;
		
		String message = "!init";
		for(Node n : nodes.values()) {
			if(n.getStatus() == Status.online)
				message += "\n" + n.getIp() + ":" + n.getTcpPort();
		}
		
		message += "\n" + rMax;
		
		
		// convert the input String to a byte[]
		responseBuffer = message.getBytes();
		
		// create the datagram packet with all the necessary information
		// for sending the packet to the node
		try
		{
			responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length,
					address);
			datagramSocket.send(responsePacket);
		} catch (SocketException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}

	private void handleAliveMessage(String request)
	{
		String[] requestStrings = request.split(" ");
		
		if (requestStrings.length == 3)
		{
			String isAlive = requestStrings[0];
			Integer tcpPort = Integer.parseInt(requestStrings[1]);
			String operators = requestStrings[2];

			if (isAlive.equals("!alive"))
			{
				Node n;
				if (nodes.containsKey(tcpPort))
				{
					n = nodes.get(tcpPort);

				} else
				// new Node
				{
					n = new Node();
				}

				n.setLastIsAlivePackage(new Date());
				n.setStatus(Status.online);
				n.setTcpPort(tcpPort);
				n.setInetAddress(packet.getAddress());
				n.setIp(packet.getAddress().getHostAddress());

				Set<Character> operatorSet = new HashSet<Character>();
				int alivePortLength = requestStrings[0].length() + requestStrings[1].length() + 2; //calculates the length of the !alive and the port string
				for(int i = 0; i < packet.getLength()-alivePortLength; i++)
				{
					
					if(operators.charAt(i) != ' ')
						operatorSet.add(operators.charAt(i));
				}
				n.setOperators(operatorSet);

				nodes.put(tcpPort, n);
			}
		}
	}
}
