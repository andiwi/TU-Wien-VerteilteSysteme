package controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.rmi.RemoteException;
import java.security.Key;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import model.ComputationRequestInfo;
import admin.INotificationCallback;

public class RemoteObjectRMI implements IAdminConsole
{
	private ConcurrentMap<Integer, Node> nodes;
	private	ConcurrentMap<String, User> users;
	private ConcurrentMap<Character, AtomicLong> statistics;
	private List<Timer> timerList;
	
	public RemoteObjectRMI(ConcurrentMap<Integer, Node> nodes, ConcurrentMap<Character, AtomicLong> statistics, ConcurrentMap<String, User> users)
	{
		this.nodes = nodes;
		this.users = users;
		this.statistics = statistics;	
		this.timerList = new ArrayList<Timer>();
	}
	
	@Override
	public boolean subscribe(String username, int credits,
			INotificationCallback callback) throws RemoteException
	{
		Timer checkTimer = new Timer(true);
		TimerTask checkCreditsTask = new CheckCreditsTask(users, username, credits, callback, checkTimer);
		checkTimer.scheduleAtFixedRate(checkCreditsTask, 0, 1000);
		timerList.add(checkTimer);
		return true;
	}

	@Override
	public List<ComputationRequestInfo> getLogs() throws RemoteException
	{
		List<ComputationRequestInfo> logs = new ArrayList<ComputationRequestInfo>();
		
		for(Node node : nodes.values())
		{
			try
			{
				Socket nodeSocket = new Socket(node.getInetAddress(), node.getTcpPort());
				
				PrintWriter nodeWriter = new PrintWriter(
						nodeSocket.getOutputStream(), true);
				
				nodeWriter.println("!getLogs");
								
				InputStream inputStream = nodeSocket.getInputStream();
				ObjectInputStream oiStream = new ObjectInputStream(inputStream);
				@SuppressWarnings("unchecked")
				List<ComputationRequestInfo> cInfoList = (List<ComputationRequestInfo>) oiStream.readObject();
				logs.addAll(cInfoList);
								
				nodeWriter.close();
				oiStream.close();
				inputStream.close();
				nodeSocket.close();
			} catch (IOException e)
			{
				return logs;
			} catch (ClassNotFoundException e)
			{
				return logs;
			}
		}
		
		Collections.sort(logs);
		return logs;
	}

	@Override
	public LinkedHashMap<Character, Long> statistics() throws RemoteException
	{
		Set<Entry<Character, Long>> set = new HashSet<Entry<Character, Long>>();
		for(Entry<Character, AtomicLong> e : statistics.entrySet())
		{
			
			set.add(new AbstractMap.SimpleEntry<Character, Long>(e.getKey(), e.getValue().get()));
		}
		
		List<Map.Entry<Character, Long>> list = new LinkedList<Map.Entry<Character, Long>>(set);
		 
		Collections.sort(list, Collections.reverseOrder(new Comparator<Map.Entry<Character,Long>>() {
			public int compare(Map.Entry<Character, Long> e1, Map.Entry<Character, Long> e2) {
				return (e1.getValue().compareTo(e2.getValue()));
			}
		}));
	 
		LinkedHashMap<Character, Long> sortedMap = new LinkedHashMap<Character, Long>();

		for(Map.Entry<Character, Long> entry : list)
		{
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		
		return sortedMap;
	}

	@Override
	public Key getControllerPublicKey() throws RemoteException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setUserPublicKey(String username, byte[] key)
			throws RemoteException
	{
		// TODO Auto-generated method stub
		
	}
	
	public void exit() {
		for(Timer t : timerList)
		{
			t.cancel();
		}
	}
}
