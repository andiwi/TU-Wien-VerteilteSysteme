package controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import util.Keys;

public class ClientThreadTCP extends Thread
{
	private Socket socket;
	private ConcurrentMap<String, User> users;
	private ConcurrentMap<Integer, Node> nodes;
	private User loggedInUser;
	private IvParameterSpec ivParameterSpec;
	private BufferedReader reader;
	private PrintWriter writer;
	private Base64Channel b;
	private AESChannel a;
	SecretKey originalKey;

	public ClientThreadTCP(Socket socket, ConcurrentMap<String, User> users, ConcurrentMap<Integer, Node> nodes)
	{
		this.socket = socket;
		this.users = users;
		this.nodes = nodes;
		b = new Base64Channel();
		a = new AESChannel();
		Security.addProvider(new BouncyCastleProvider());
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
			} catch (IOException e1)
			{
				// Ignored because we cannot handle it
			}
		
		if(writer != null)
			writer.close();
		
		if (socket != null)
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
	private String doRequestCommand(String request) throws FileNotFoundException
	{
		String[] parts = new String[10];
		String text = null;
		if(loggedInUser==null){
			try {
				try {
					text = new String(decryption(b.decode(request)));
					System.out.println("Message 1 received!");
					parts = text.split(" ");
				} catch (IOException e) {
					e.printStackTrace();
				}
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				e.printStackTrace();
			} catch (BadPaddingException e) {
				e.printStackTrace();
			}
		}
		else
		{
			try {
				text = new String(a.decrypt(b.decode(request), originalKey, ivParameterSpec));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		parts = text.split(" ");
		
		if(parts[0].equals("!authenticate") && parts.length == 3)
		{
			return authenticate(parts[1], parts[2]);
		}	
		
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
			return compute(text.substring(9));
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
		if(loggedInUser!=null)
			{
			logout();
			}
		return "EXIT";
	}
	
	private String credits()
	{
		if(loggedInUser == null){
			String text = "You have to login a User first!";
			return a.encrypt(text, originalKey, ivParameterSpec);
		}
		String text = "You have " + loggedInUser.getCredits() + " credits left";
		return a.encrypt(text, originalKey, ivParameterSpec);
	}
	
	private String buy(int credits)
	{
		if(loggedInUser == null){
			String text = "You have to login a User first!";
			return a.encrypt(text, originalKey, ivParameterSpec);
		}
		loggedInUser.setCredits(loggedInUser.getCredits() + credits);
		String text = "You now have " + loggedInUser.getCredits() + " credits.";
		return a.encrypt(text, originalKey, ivParameterSpec);
	}
	
	private String listAvailableOperations()
	{
		if(loggedInUser == null){
			String text = "You have to login a User first!";
			return a.encrypt(text, originalKey, ivParameterSpec);
		}
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
		return a.encrypt(response, originalKey, ivParameterSpec);
	}

	private String compute(String term)
	{
		if(loggedInUser == null){
			String text = "You have to login a User first!";
			return a.encrypt(text, originalKey, ivParameterSpec);
		}
		List<String> arguments = new ArrayList<String>();
		arguments.addAll(Arrays.asList(term.split(" ")));
		
		if(isOdd(arguments.size()))
		{
			int numberOfOperators = arguments.size()/2; //truncates the decimal digits
			
			if((50*numberOfOperators) > loggedInUser.getCredits()){
				String text = "Error: User has not enough Credits.";
				return a.encrypt(text, originalKey, ivParameterSpec);
			}
			while(arguments.size() > 1)
			{
				int number1;
				char operator;
				int number2;
				
				try
				{
					number1 = Integer.parseInt(arguments.remove(0));
					String operatorStr = arguments.remove(0);
					if(operatorStr.length() != 1){
						String text = "Illegal operator: " + operatorStr;
						return a.encrypt(text, originalKey, ivParameterSpec);
					}
					operator = operatorStr.charAt(0);
					number2 = Integer.parseInt(arguments.remove(0));
				}catch(NumberFormatException e)
				{
					String text = "Error: Illegal Arguments.";
					return a.encrypt(text, originalKey, ivParameterSpec);
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
							return a.encrypt(result, originalKey, ivParameterSpec);
						}else
						{
							loggedInUser.setCredits(loggedInUser.getCredits() - 50);
							node.setUsage(node.getUsage() + (50 * result.length())); //if computation was processed successfully the usage of the node increases.
							arguments.add(0, result);
						}
						
					}else{
						String text = "Cannot compute the request (no known Node)";
						return a.encrypt(text, originalKey, ivParameterSpec);
					}
				}
				if(trials == 10){
					String text = "Cannot compute the request (no known Node)";
					return a.encrypt(text, originalKey, ivParameterSpec);
				}
			}
			String text = arguments.get(0);
			return a.encrypt(text, originalKey, ivParameterSpec);
		}else {
			String text = "Wrong number of terms";
			return a.encrypt(text, originalKey, ivParameterSpec);
		}
		
	}
	
	private String logout()
	{
		if(loggedInUser == null){
			String text = "You have to login a User first!";
			return a.encrypt(text, originalKey, ivParameterSpec);
		}
		loggedInUser.setStatus(Status.offline);
		String userState = loggedInUser.toString();
		loggedInUser = null;
		String text = "Successfully logged out. " + userState;
		return a.encrypt(text, originalKey, ivParameterSpec);
	}
	
	private String authenticate(String username, String randomnumber)
	{
		
		// generates a 32 byte secure random number
				SecureRandom secureRandom = new SecureRandom();
				final byte[] number = new byte[32];
				secureRandom.nextBytes(number);
				
				
				byte[] encnumber = null;
				try {
					encnumber = b.encode(number);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				
		//generates 256 Bit secret Key		
				KeyGenerator generator = null;
				try {
					generator = KeyGenerator.getInstance("AES");
				} catch (NoSuchAlgorithmException e2) {
					e2.printStackTrace();
				}
				// KEYSIZE is in bits
				int i = 256;
				generator.init(i);
				SecretKey key = generator.generateKey();
				byte[] enckey = null;
				try {
					enckey = b.encode(key.getEncoded());
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				
		// generates a 16 byte secure random number
				SecureRandom secureRandom2 = new SecureRandom();
				final byte[] number2 = new byte[16];
				secureRandom2.nextBytes(number2);
				byte[] encnumber2 = null;
				try {
					encnumber2 = b.encode(number2);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
		
		//Encrypting Message
				Cipher cipher=null;
				byte[] enctext = null;
				
				
					try {
						cipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
					} catch (NoSuchAlgorithmException e1) {
						e1.printStackTrace();
					} catch (NoSuchPaddingException e1) {
						e1.printStackTrace();
					}
					// MODE is the encryption/decryption mode
					// KEY is either a private, public or secret key
					// IV is an init vector, needed for AES
					
					File f = new File("keys/controller/"+username+".pub.pem");
					
					
						try {
							cipher.init(Cipher.ENCRYPT_MODE, Keys.readPublicPEM(f), secureRandom);
						} catch (InvalidKeyException e1) {
							e1.printStackTrace();
						} catch (FileNotFoundException e1){
							System.out.println("Authentication Stopped!");
							return "Wrong Username";
						} catch (IOException e1) {
							e1.printStackTrace();
						}
						
						String ok = "!ok " + randomnumber + " " + new String(encnumber) + " " +  new String(enckey) + " " + new String(encnumber2);
						System.out.println("Message 2 sent!");
						byte[] encok=null;
						try {
							encok = cipher.doFinal(ok.getBytes());
						} catch (IllegalBlockSizeException e1) {
							e1.printStackTrace();
						} catch (BadPaddingException e1) {
							e1.printStackTrace();
						}
						try {
							enctext = b.encode(encok);
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					
				
		writer.println(new String(enctext));
		try {
			String lastmessage = reader.readLine();
			System.out.println("Message 3 received!");
			byte[] encmessage = b.decode(lastmessage);
			
			//Decrypt third message
			
			Cipher cipher2=null;
			try {
				cipher2 = Cipher.getInstance("AES/CTR/NoPadding");
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				e.printStackTrace();
			}
			originalKey = new SecretKeySpec(key.getEncoded(), 0, key.getEncoded().length, "AES");
			ivParameterSpec = new IvParameterSpec(number2);
			try {
				try {
					cipher2.init(Cipher.DECRYPT_MODE, originalKey, ivParameterSpec);
				} catch (InvalidAlgorithmParameterException e) {
					e.printStackTrace();
				}
			} catch (InvalidKeyException e) {
				e.printStackTrace();
			}
			byte[] decmessage = null;
			try {
				decmessage = cipher2.doFinal(encmessage);
			} catch (IllegalBlockSizeException e) {
				e.printStackTrace();
			} catch (BadPaddingException e) {
				e.printStackTrace();
			}
			if(loggedInUser == null && new String(decmessage).equals(new String(encnumber)))
			{
				if(users.containsKey(username))
				{
					User candidate = users.get(username);
					if(candidate.getStatus() == Status.online)
					{
						//return "User is already logged in from another client!";
						return "User is already logged in from another client!";
					}
					else
					{
						loggedInUser = candidate;
						loggedInUser.setStatus(Status.online);
						
						//return "Successfully logged in!";
						return loggedInUser.getUsername() + " has been successfully authenticated!";
					}
				}
				else
					return "User nicht vorhanden!";

			}else 
				//return "You are already logged in!";
				return "You are already logged in!";

	
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "Authentication failed!";
		
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
		commands.add("!authenticate <username> - Authentifizierung über RSA und AES.");
		
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
	
	private byte[] decryption(byte[] text) throws NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException{
		

		Cipher cipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
		File f = new File("keys/controller/controller.pem");
		try {
			cipher.init(Cipher.DECRYPT_MODE, Keys.readPrivatePEM(f));
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		byte[] cipherData = cipher.doFinal(text);
		return cipherData;
	}
}