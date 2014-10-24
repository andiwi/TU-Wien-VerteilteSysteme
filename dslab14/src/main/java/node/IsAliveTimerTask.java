package node;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.TimerTask;

public class IsAliveTimerTask extends TimerTask {

	private DatagramSocket datagramSocket;
	private DatagramPacket datagramPacket;
	
	IsAliveTimerTask(DatagramSocket datagramSocket, DatagramPacket datagramPacket)
	{
		this.datagramSocket = datagramSocket;
		this.datagramPacket = datagramPacket;
	}
	
	@Override
	public void run() {
		// send request-packet to server
		try {
			datagramSocket.send(datagramPacket);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
