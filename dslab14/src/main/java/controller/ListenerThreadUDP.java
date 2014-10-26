package controller;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ListenerThreadUDP extends Thread
{
	private DatagramSocket datagramSocket;
	private Map<Integer, Node> nodes;
	
	public ListenerThreadUDP(DatagramSocket datagramSocket,
			Map<Integer, Node> nodes) {
		this.datagramSocket = datagramSocket;
		this.nodes = nodes;
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

			// wait for incoming packets from client
			try
			{
				datagramSocket.receive(packet);
			
				// handle incoming connections from client with a ThreadPool in a separate thread
				executor.execute(new NodeThreadUDP(packet, nodes));
			} catch (IOException e)
			{
				executor.shutdown();
				break;
			}
		}
	}
}
