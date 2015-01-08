package admin;

import cli.Command;
import cli.Shell;
import controller.IAdminConsole;
import model.ComputationRequestInfo;
import util.Config;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.Key;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

/**
 * Please note that this class is not needed for Lab 1, but will later be
 * used in Lab 2. Hence, you do not have to implement it for the first
 * submission.
 */
public class AdminConsole implements IAdminConsole, Runnable {

	private Config config;
	private IAdminConsole server;
	private Shell shell;

	/**
	 * @param componentName
	 *            the name of the component - represented in the prompt
	 * @param config
	 *            the configuration to use
	 * @param userRequestStream
	 *            the input stream to read user input from
	 * @param userResponseStream
	 *            the output stream to write the console output to
	 */
	public AdminConsole(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {

		this.config = config;
		this.shell = new Shell(componentName, userRequestStream, userResponseStream);
		shell.register(this);
	}

	@Override
	public void run() {
		try {
			// obtain registry that was created by the server
			Registry registry = LocateRegistry.getRegistry(
					config.getString("controller.host"),
					config.getInt("controller.rmi.port"));
			// look for the bound server remote-object implementing the IServer
			// interface
			server = (IAdminConsole) registry.lookup(config
					.getString("binding.name"));
		} catch (RemoteException e) {
			throw new RuntimeException(
					"Error while obtaining registry/server-remote-object.", e);
		} catch (NotBoundException e) {
			throw new RuntimeException(
					"Error while looking for server-remote-object.", e);
		}
		
		try
		{
			new Thread(shell).start();
			shell.writeLine("AdminConsole is up! Enter command");
		} catch (IOException e)
		{
			System.out.println(e.getClass().getSimpleName() + ": "
					+ e.getMessage());
			exit();
		}
		
		
	}

	private void exit()
	{
		// TODO Auto-generated method stub
		
	}
	
	@Command("getLogs")
	public String getLogsCall() {
		List<ComputationRequestInfo> response;
		try
		{
			response = server.getLogs();
		} catch (RemoteException e)
		{
			// TODO Auto-generated catch block
			return e.getMessage().toString();
		}
		
		if(response.isEmpty())
		{
			return "There are no log files.";
		}else
		{
			String logs = "";
			for(ComputationRequestInfo i : response)
			{
				logs += i.toString() +"\n";
			}
			return logs;
		}
	}
	
	@Command("statistics")
	public String statisticsCall() {
		LinkedHashMap<Character, Long> response;
		try
		{
			response = server.statistics();
		} catch (RemoteException e)
		{
			// TODO Auto-generated catch block
			return e.getMessage().toString();
		}
		
		if(response.isEmpty())
		{
			return "There are no statistics.";
		}else
		{
			String statistics = "";
			for(Entry<Character, Long> e : response.entrySet())
			{
				statistics += e.getKey() + " " + e.getValue() + "\n";
			}
						
			return statistics;
		}
	}
	
	@Command("subscribe")
	public String subscribeCall(String username, String credits)
	{
		int creditsInt = Integer.parseInt(credits);
		
		// create a remote object of this server object
		try
		{
			INotificationCallback callback = (INotificationCallback) UnicastRemoteObject
								.exportObject(new NotificationCallback(), 0);
			
			boolean success = server.subscribe(username, creditsInt, callback);
			
			if(success)
				return "Successfully subscribed for user " + username;
			else return "Subscription not successful for user " + username;
			
		} catch (RemoteException e1)
		{
			e1.printStackTrace();
			return "Subscription not successful for user " + username;
		}
	}
	
	

	/**
	 * @param args
	 *            the first argument is the name of the {@link AdminConsole}
	 *            component
	 */
	public static void main(String[] args) {
		AdminConsole adminConsole = new AdminConsole(args[0], new Config(
				"admin"), System.in, System.out);
		
		new Thread((Runnable) adminConsole).start();
	}
	
	@Override
	public boolean subscribe(String username, int credits,
			INotificationCallback callback) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<ComputationRequestInfo> getLogs() throws RemoteException {
		// TODO Auto-generated method stub
		System.out.println("HIER");
		return null;
	}

	@Override
	public LinkedHashMap<Character, Long> statistics() throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Key getControllerPublicKey() throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setUserPublicKey(String username, byte[] key)
			throws RemoteException {
		// TODO Auto-generated method stub
	}
}
