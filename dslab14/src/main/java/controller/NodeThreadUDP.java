package controller;

import java.net.DatagramPacket;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cli.Shell;

public class NodeThreadUDP extends Thread
{
	private DatagramPacket packet;
	private Shell controllerShell;
	private Map<Integer, Node> nodes;

	public NodeThreadUDP(DatagramPacket packet, Shell controllerShell,
			Map<Integer, Node> nodes)
	{
		this.packet = packet;
		this.controllerShell = controllerShell;
		this.nodes = nodes;
	}

	public void run()
	{
		// get the data from the packet
		String request = new String(packet.getData());

		String[] requestStrings = request.split(" ");

		if (requestStrings.length == 3)
		{
			String isAlive = requestStrings[0];
			String tcpPortStr = requestStrings[1];
			Integer tcpPort = Integer.parseInt(tcpPortStr);
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
