package controller;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cli.Shell;
import util.Config;

public class ListenerThreadUDP extends Thread
{
	private DatagramSocket datagramSocket;
	private Map<Integer, Node> nodes;
	private Shell controllerShell;
	
	public ListenerThreadUDP(DatagramSocket datagramSocket, Shell controllerShell,
			Map<Integer, Node> nodes) {
		this.datagramSocket = datagramSocket;
		this.controllerShell = controllerShell;
		this.nodes = nodes;
	}

	public void run() {

		try {
			controllerShell.writeLine("starting new ListenerThreadUDP...");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		byte[] buffer;
		DatagramPacket packet;
		try
		{
			while (true)
			{
				buffer = new byte[1024];
				// create a datagram packet of specified length (buffer.length)
				/*
				 * Keep in mind that: in UDP, packet delivery is not
				 * guaranteed,and the order of the delivery/processing is not
				 * guaranteed
				 */
				packet = new DatagramPacket(buffer, buffer.length);

				// wait for incoming packets from client
				datagramSocket.receive(packet);
				
				//ExecutorService executor = Executors.newCachedThreadPool();
				// handle incoming connections from client with a ThreadPool in a separate thread
				//executor.execute(new NodeThreadUDP(packet, this.controllerShell, nodes));
				// handle incoming packets in a new Thread
				new NodeThreadUDP(packet, this.controllerShell, nodes).start();
				
			}
		}catch(Exception e)
		{
			
		}
	}
}
