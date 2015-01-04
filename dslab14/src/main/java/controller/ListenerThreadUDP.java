package controller;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import util.Config;

public class ListenerThreadUDP extends Thread
{
	private DatagramSocket datagramSocket;
	private ConcurrentMap<Integer, Node> nodes;
	private int rMax;
	
	public ListenerThreadUDP(Config config, DatagramSocket datagramSocket,
			ConcurrentMap<Integer, Node> nodes, int rMax) {
		this.datagramSocket = datagramSocket;
		this.nodes = nodes;
		this.rMax = rMax;
	}

	public void run()
	{
		byte[] buffer;
		DatagramPacket packet;
		
		ExecutorService executor = Executors.newFixedThreadPool(20);
		
		while (true)
		{
			buffer = new byte[1024];
			// create a datagram packet of specified length (buffer.length)
			packet = new DatagramPacket(buffer, buffer.length);

			// wait for incoming packets from node
			try
			{
				datagramSocket.receive(packet);
			
				// handle incoming connections from node with a ThreadPool in a separate thread
				executor.execute(new NodeThreadUDP(datagramSocket, packet, nodes, rMax));
			} catch (IOException e)
			{
				executor.shutdown();
				break;
			}
		}
	}
}
