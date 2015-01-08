package admin;

import java.io.Serializable;
import java.rmi.RemoteException;

public class NotificationCallback implements INotificationCallback,
		Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -6309051212252537075L;
	
	public NotificationCallback() {
		
	}
	
	@Override
	public void notify(String username, int credits) throws RemoteException
	{
		System.out.println("Notification: " + username + " has less than " + credits + " credits.");
	}

}
