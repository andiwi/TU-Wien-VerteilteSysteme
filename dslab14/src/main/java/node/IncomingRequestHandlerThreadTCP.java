package node;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import controller.Base64Channel;
import controller.HMACChannel;

import model.ComputationRequestInfo;
import util.Config;

public class IncomingRequestHandlerThreadTCP extends Thread {

	private Node node;
	private Socket socket;
	private Set<Character> operators;
	private BufferedReader reader;
	private PrintWriter writer;
	private String componentName;
	private Config config;
	private ObjectOutputStream objectOutputStream;
	private Base64Channel b;
	private HMACChannel h;
	
	// SimpleDateFormat is not thread-safe, so give one to each thread
    private static final ThreadLocal<SimpleDateFormat> dateFormatter = new ThreadLocal<SimpleDateFormat>(){
        @Override
        protected SimpleDateFormat initialValue()
        {
            return new SimpleDateFormat("yyyyMMdd_HHmmss.SSS");
        }
    };
	
	public IncomingRequestHandlerThreadTCP(Node node, Socket socket, Set<Character> operators, String componentName, Config config) {
		this.node = node;
		this.socket = socket;
		this.operators = operators;
		this.componentName = componentName;
		this.config = config;
		b = new Base64Channel();
		h = new HMACChannel();
	}
	
	public void run()
	{
		try
		{
			// prepare the input reader for the socket
			reader = new BufferedReader(
					new InputStreamReader(socket.getInputStream()));
			// prepare the writer for responding to clients requests
			//writer = new PrintWriter(socket.getOutputStream(),
			//		true);
			// prepare objectOutputStream for responding to admin console requests
			//objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
			
			String request;
			// read client requests
			while ((request = reader.readLine()) != null)
			{
				
				if(isDTORequest(request))
				{
					//prepare objectOutputStream for responding to admin console requests
					objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
					if(request.equals("!getLogs"))
					{
						objectOutputStream.writeObject(getLogs());
					}
					objectOutputStream.flush();
				
				}else
				{
					if(writer == null)
					{
						writer = new PrintWriter(socket.getOutputStream(),
								true);
					}
					
					String response = doRequestCommand(request);
					if(response != null)
						writer.println(response);
					
					String parts[] = request.split(" ");
					if(parts[1].startsWith("!compute") && response.split(" ").length < 3){
					
						String[] subparts = request.split("!compute ");
						String after = subparts[1];
						createLogFile(after, response);
					}
				}
				
				
				if (Thread.interrupted())
					break;
			}
			closeAllStreams();
		}catch(IOException e)
		{
			closeAllStreams();
		}	
	}

	private List<ComputationRequestInfo> getLogs()
	{
		String pathStr = config.getString("log.dir");
		File directory = new File(pathStr);
		File[] logFiles = directory.listFiles();
		
		List<ComputationRequestInfo> logs = new ArrayList<ComputationRequestInfo>();
		if(logFiles != null)
		{
			for(int i = 0; i < logFiles.length; i++)
			{
				//read file and save to ComputationRequestInfoList
			    try
				{
			    	String filename = logFiles[i].getName();
			    	String[] filenameSplitted = filename.split("_"); 
			    	String timestamp = filenameSplitted[0] + filenameSplitted[1];
			    	String node = filenameSplitted[2];
			    	node = node.split("\\.")[0];
			    	
					List<String> lines = Files.readAllLines(logFiles[i].toPath(), StandardCharsets.UTF_8);
					
					ComputationRequestInfo rInfo = new ComputationRequestInfo();
					rInfo.setMathematicalTerm(lines.get(0) + " = " + lines.get(1));
					rInfo.setNode(node);
					rInfo.setTimestamp(timestamp);
					
					logs.add(rInfo);
				} catch (IOException e)
				{
					return logs;
				}
			}
		}
		return logs;
	}

	private boolean isDTORequest(String request)
	{
		if(request.equals("!getLogs"))
			return true;
		
		return false;
	}

	private synchronized void createLogFile(String row1, String row2)
	{
		String date = dateFormatter.get().format(new Date());
		
		try
		{
			String pathStr = config.getString("log.dir") + File.separator + date + "_" + componentName + ".log";
			Path path = Paths.get(pathStr);
			Files.createDirectories(path.getParent());

	        try {
	           File file = Files.createFile(path).toFile();
	           
	           FileWriter fw = new FileWriter(file);
	           BufferedWriter bw = new BufferedWriter(fw);
	           bw.append(row1);
	           bw.newLine();
	           bw.append(row2);
	           bw.close();
	           fw.close();
	           
	        } catch (FileAlreadyExistsException e) {
	            System.out.println("already exists: " + e.getMessage());
	        }  
		}catch (IOException e)
		{
			e.printStackTrace();
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
		
		if(objectOutputStream != null)
			try
			{
				objectOutputStream.close();
			} catch (IOException e1)
			{
				// Ignored because we cannot handle it
			}
		
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
		
		if(parts[1].equals("!compute") && parts.length == 5)
		{	
			try
			{
				int number1 = Integer.parseInt(parts[2]);
				String operatorStr = parts[3];
				if(operatorStr.length() != 1)
					return "Error: Illegal operator: " + operatorStr;
				
				char operator = operatorStr.charAt(0);
				int number2 = Integer.parseInt(parts[4]);
				
				String originalmessage = parts[1] + " " + parts[2] + " " + parts[3] + " " + parts[4];
				byte[] dechmac = null;
				try {
					dechmac = b.decode(parts[0]);
				} catch (IOException e) {
					e.printStackTrace();
				}
				String path = config.getString("hmac.key");
				byte[]hmac = null;
				try {
					hmac = h.hmac(originalmessage.getBytes(), path);
				} catch (IOException e) {
					e.printStackTrace();
				}
				String hmacanswer = parts[0] + " !tampered " + parts[2] + " " + parts[3] + " " + parts[4];
				if (MessageDigest.isEqual(hmac, dechmac)){
					return compute(number1, operator, number2);
				}
				else{
					return hmacanswer;
				}
				
			}catch(NumberFormatException e)
			{
				return "Error: Illegal Arguments.";
			}
		}else if(parts[0].equals("!share") && parts.length == 2)
		{
			int resourceForNode = Integer.parseInt(parts[1]);
			return share(resourceForNode);
		}else if(parts[0].equals("!commit") && parts.length == 2)
		{
			int resourceForNode = Integer.parseInt(parts[1]);
			return commit(resourceForNode);
		}else if(parts[0].equals("!rollback") && parts.length == 2)
		{
			int resourceForNode = Integer.parseInt(parts[1]);
			return rollback(resourceForNode);
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
	
	public String share(int resourceForNode) {
		if(resourceForNode >= config.getInt("node.rmin"))
			return "!ok";
			
		return "!nok";
	}
	
	public String commit(int resourceForNode) {
		this.node.setResources(resourceForNode);
		return null;
	}
	
	private String rollback(int resourceForNode)
	{
		return null;
	}
}
