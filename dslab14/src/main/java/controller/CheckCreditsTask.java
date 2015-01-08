package controller;

import java.rmi.RemoteException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentMap;

import admin.INotificationCallback;

public class CheckCreditsTask extends TimerTask
{
	private	ConcurrentMap<String, User> users;
	private String username;
	private int credits;
	INotificationCallback callback;
	private Timer checkTimer;
	
	public CheckCreditsTask(ConcurrentMap<String, User> users, String username, int credits, INotificationCallback callback, Timer checkTimer)
	{
		this.users = users;
		this.username = username;
		this.credits = credits;
		this.callback = callback;
		this.checkTimer = checkTimer;
	}
	
	@Override
	public void run()
	{
		System.out.println("check credits for: " + username);
		User u = users.get(username);
		
		if(u != null && u.getCredits() < credits)
		{
			try
			{
				callback.notify(username, credits);
			} catch (RemoteException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			checkTimer.cancel();
		}
	}
}
