package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClientThreadTCP extends Thread
{
	private Socket socket;
	private Map<String, User> users;
	private Map<Integer, Node> nodes;
	private User loggedInUser;
	private BufferedReader reader;
	private PrintWriter writer;

	public ClientThreadTCP(Socket socket, Map<String, User> users, Map<Integer, Node> nodes)
	{
		this.socket = socket;
		this.users = users;
		this.nodes = nodes;
	}
	
	public void run()
	{
		try
		{
			// prepare the input reader for the socket
			reader = new BufferedReader(
					new InputStreamReader(socket.getInputStream()));
			// prepare the writer for responding to clients requests
			writer = new PrintWriter(socket.getOutputStream(),
					true);

					
			String request;
			// read client requests			
			while ((request = reader.readLine()) != null)
			{
				String response = doRequestCommand(request);
				
				if(response.equals("EXIT"))
					break;
				
				writer.println(response);
				
				if(Thread.interrupted())
					break;
			}
			closeAllStreams();
		}catch(IOException e)
		{
			closeAllStreams();
		}
	}

	private void closeAllStreams()
	{
		if(reader != null)
			try
			{
				reader.close();
				//socket.getInputStream().close();
			} catch (IOException e1)
			{
				// Ignored because we cannot handle it
			}
		
		if(writer != null)
			writer.close();
		
		if (socket != null && !socket.isClosed())
		{
			try {
				socket.close();
			} catch (IOException e) {
				// Ignored because we cannot handle it
			}
		}	
	}

	/**
	 * Checks if the request contains a known Command and does the command.
	 * @param request
	 * @return response of the command
	 */
	private String doRequestCommand(String request)
	{
		String parts[] = request.split(" ");
		
		if(parts[0].equals("!login") && parts.length == 3)
			return login(parts[1], parts[2]);
		
		if(parts[0].equals("!exit") && parts.length == 1)
			return exit();
		
		if(parts[0].equals("!credits") && parts.length == 1)
			return credits();
		
		if(parts[0].equals("!buy") && parts.length == 2)
		{
			try
			{
				int credits = Integer.parseInt(parts[1]);
				return buy(credits);
			}catch(NumberFormatException e)
			{
				return "Wrong argument. \n" + listAvailableCommands();
			}	
		}
		
		if(parts[0].equals("!list") && parts.length == 1)
			return listAvailableOperations();
		
		if(parts[0].equals("!compute") && parts.length >= 4)
		{	
			return compute(request.substring(9));
		}
		if(parts[0].equals("!logout") && parts.length == 1)
			return logout();
		
		return "Unknown Command. " + listAvailableCommands();
	}

	private synchronized String login(String username, String password)
	{
		if(loggedInUser == null)
		{
			if(users.containsKey(username))
			{
				User candidate = users.get(username);
				if(candidate.getStatus() == Status.online)
				{
					return "User is already logged in from another client!";
				}
				if(candidate.getPassword().equals(password))
				{
					loggedInUser = candidate;
					loggedInUser.setStatus(Status.online);
					
					return "Successfully logged in!";
				}
			}
			return "Wrong username or password";
		}else return "You are already logged in!";
	}
	
	private String exit()
	{
		logout();
		return "EXIT";
	}
	
	private String credits()
	{
		if(loggedInUser == null)
			return "You have to login a User first!";
		
		return "You have " + loggedInUser.getCredits() + " credits left";
	}
	
	private String buy(int credits)
	{
		if(loggedInUser == null)
			return "You have to login a User first!";
		
		loggedInUser.setCredits(loggedInUser.getCredits() + credits);
		return "You now have " + loggedInUser.getCredits() + " credits.";
	}
	
	private String listAvailableOperations()
	{
		if(loggedInUser == null)
			return "You have to login a User first!";
		
		Set<Character> availOperations = new HashSet<Character>();
		
		for(Node node : nodes.values())
		{
			for(Character op : node.getOperators())
			{
				if(!availOperations.contains(op))
					availOperations.add(op);
			}
		}
		
		String response = "";
		
		for(Character c : availOperations)
		{
			response += c;
		}
		return response;
	}

	private String compute(String term)
	{
		if(loggedInUser == null)
			return "You have to login a User first!";
		
		List<String> arguments = new ArrayList<String>();
		arguments.addAll(Arrays.asList(term.split(" ")));
		
		if(isOdd(arguments.size()))
		{
			int numberOfOperators = arguments.size()/2; //truncates the decimal digits
			
			if((50*numberOfOperators) > loggedInUser.getCredits())
				return "Error: User has not enough Credits.";
			
			while(arguments.size() > 1)
			{
				int number1;
				char operator;
				int number2;
				
				try
				{
					number1 = Integer.parseInt(arguments.remove(0));
					String operatorStr = arguments.remove(0);
					if(operatorStr.length() != 1)
						return "Illegal operator: " + operatorStr;
					
					operator = operatorStr.charAt(0);
					number2 = Integer.parseInt(arguments.remove(0));
				}catch(NumberFormatException e)
				{
					return "Error: Illegal Arguments.";
				}
				
				boolean tryAgain = true;
				int trials = 0; //counter that prevents infinity loops
				while(tryAgain && (trials < 10))
				{
					tryAgain = false;
					trials++;
					
					Node node = getBestNodeForOperator(operator);
					if(node != null)
					{
						String result = sendToNode(number1, operator, number2, node);
						if(result.equals("Error: Cannot connect to Node."))
						{
							node.setStatus(Status.offline);
							tryAgain = true;
						}else if(result.startsWith("Error:"))
						{
							loggedInUser.setCredits(loggedInUser.getCredits() - 50); //For each computation the user has to pay credits
							return result;
						}else
						{
							loggedInUser.setCredits(loggedInUser.getCredits() - 50);
							node.setUsage(node.getUsage() + (50 * result.length())); //if computation was processed successfully the usage of the node increases.
							arguments.add(0, result);
						}
						
					}else return "Cannot compute the request (no known Node)";
				}
				if(trials == 10)
					return "Cannot compute the request (no known Node)";
			}
			return arguments.get(0);
		}else return "Wrong number of terms";
	}
	
	private String logout()
	{
		if(loggedInUser == null)
			return "You have to login a User first!";
		
		loggedInUser.setStatus(Status.offline);
		String userState = loggedInUser.toString();
		loggedInUser = null;
		
		return "Successfully logged out. " + userState;
	}

	/**
	 * 
	 * @return A list of all available commands
	 */
	private String listAvailableCommands()
	{
		List<String> commands = new ArrayList<String>();
		commands.add("!login <username> <password> - Log in the user.");
		commands.add("!credits - Requests the user's current amount of credits.");
		commands.add("!buy <credits> - Allows the user to increase his/her amount of credits.");
		commands.add("!list - Gets the list of arithmetic operations that can be used.");
		commands.add("!compute <term> - Sends the given mathematical term to the controller, who returns the result or an appropriate error message in case of a failure.");
		commands.add("!logout - Log out the currently logged in user, and drop any state information from memory that the client has associated with this user.");
		commands.add("!exit - Shutdown the client");
		
		String list = "";
		for(String command : commands)
		{
			list += command + " ";
		}
		list.substring(0, list.length()-1);
		return list;
	}
	
	/**
	 * Sends the request via TCP to the Node 
	 * @param number1
	 * @param operator
	 * @param number2
	 * @param node
	 * @return
	 */
	private String sendToNode(int number1, char operator, int number2,
			Node node)
	{
		try
		{
			Socket nodeSocket = new Socket(node.getInetAddress(), node.getTcpPort());
			PrintWriter nodeWriter = new PrintWriter(
					nodeSocket.getOutputStream(), true);
			
			BufferedReader nodeReader = new BufferedReader(
					new InputStreamReader(nodeSocket.getInputStream()));
			
			nodeWriter.println("!compute " + number1 + " " + operator + " " + number2);
			String response = nodeReader.readLine();
			
			nodeWriter.close();
			nodeReader.close();
			nodeSocket.close();
			
			return response;
			
		} catch (IOException e)
		{
			return "Error: Cannot connect to Node.";
		}
	}

	/**
	 * Checks if there is a registered Node which can handle the operator
	 * @param operator
	 * @return If there is more than one Node available it returns the Node with the minimal Usage statistic
	 */
	private Node getBestNodeForOperator(char operator)
	{
		Node bestNode = null;
		
		for(Node n : nodes.values())
		{
			if(n.getOperators().contains(operator) && n.getStatus() == Status.online)
			{
				if(bestNode == null)
				{
					bestNode = n;
				}else if(bestNode.getUsage() > n.getUsage())
				{
					bestNode = n;
				}
			}
		}
		
		return bestNode;
	}
	
	/**
	 * Checks if a int is even or odd.
	 * @param length
	 * @return true if it is odd.
	 */
	private boolean isOdd(int x)
	{
		if ((x & 1) == 1)
		{
			return true;
		}else return false;
	}
}