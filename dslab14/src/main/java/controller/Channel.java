package controller;

import java.io.IOException;

public abstract interface Channel {

//		//Method for receiving messages from a specific TCPSocket
//		String receive() throws IOException;
//
//		//Method for sending messages from a specific TCPSocket
//		String send() throws IOException;

		//Method to decode a message with base64 encryption
		String decode(String s) throws IOException;
		
		//Method to encode a message with base64 encryption
		byte[] encode(byte[] s) throws IOException;
}
