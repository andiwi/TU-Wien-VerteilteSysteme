package controller;

import java.util.Date;
import java.util.Map;
import java.util.TimerTask;

public class IsAvailableTimerTask extends TimerTask
{
	private Map<Integer, Node> nodes;
	private int timeout;

	public IsAvailableTimerTask(Map<Integer, Node> nodes, int timeout)
	{
		this.nodes = nodes;
		this.timeout = timeout;
	}

	@Override
	public void run()
	{
		
		for (Node n : nodes.values())
		{
			if ((new Date().getTime() - n.getLastIsAlivePackage().getTime()) > timeout)
			{
				n.setStatus(Status.offline);
			} else
			{
				n.setStatus(Status.online);
			}
		}
	}
}
