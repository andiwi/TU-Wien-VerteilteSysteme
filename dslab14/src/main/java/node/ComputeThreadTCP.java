package node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import util.Config;
import cli.Command;
import cli.Shell;

public class ComputeThreadTCP extends Thread {

	private Socket socket;
	private Shell nodeShell;
	private Config config;
	private Set<Character> operators;
	
	public ComputeThreadTCP(Socket socket, Shell nodeShell, Set<Character> operators) {
		this.socket = socket;
		this.nodeShell = nodeShell;
		this.operators = operators;
	}
	
	public void run()
	{
	
		try
		{
			nodeShell.writeLine("starting new ComputeThreadTCP...");
		} catch (IOException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try
		{
			// prepare the input reader for the socket
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(socket.getInputStream()));
			// prepare the writer for responding to clients requests
			PrintWriter writer = new PrintWriter(socket.getOutputStream(),
					true);

			String request;
			// read client requests
			while ((request = reader.readLine()) != null) {

				nodeShell.writeLine("CloudController sent the following request: "
						+ request);

				
				String response = doRequestCommand(request);
				writer.println(response);
			}
		}catch(IOException e)
		{
			// TODO Auto-generated method stub
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
		
		if(parts[0].equals("!compute") && parts.length == 4)
		{	
			try
			{
				int number1 = Integer.parseInt(parts[1]);
				String operatorStr = parts[2];
				if(operatorStr.length() != 1)
					return "Error: Illegal operator: " + operatorStr;
				
				char operator = operatorStr.charAt(0);
				int number2 = Integer.parseInt(parts[3]);
				
				return compute(number1, operator, number2);
			}catch(NumberFormatException e)
			{
				return "Error: Illegal Arguments.";
			}
		}
		return "Error: Unknown Command.";
	}
	
	private String compute(int number1, char operator, int number2)
	{
		if(operators.contains(operator))
		{
			if(operator == '+')
				return number1 + number2 +"";
			
			if(operator == '-')
				return number1 - number2 +"";
			
			if(operator == '*')
				return number1 * number2 +"";
			
			if(operator == '/')
			{
				if(number2 == 0)
					return "Error: division by 0";
				
				double number1D = number1;
				double number2D = number2;
				
				double result = number1D / number2D;
				
				return Math.round(result) +"";
			}
		}
		return "Error: Operator is not supported";
	}
}
