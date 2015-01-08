package controller;

import java.io.IOException;

public abstract interface Channel {


		//Method to decode a message with base64 encryption
		byte[] decode(String s) throws IOException;
		
		//Method to encode a message with base64 encryption
		byte[] encode(byte[] s) throws IOException;
}
