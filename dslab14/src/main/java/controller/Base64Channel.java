package controller;

import java.io.IOException;
import org.bouncycastle.util.encoders.Base64;

public class Base64Channel implements Channel
{

	public Base64Channel(){
		
	};
	
	// decode from Base64 format 
	@Override
	public String decode(String s) throws IOException {
		byte[] base64Message = s.getBytes();
		byte[] encryptedMessage = Base64.decode(base64Message);
		return encryptedMessage.toString();
	}
	
	// encode into Base64 format 
	@Override
	public byte[] encode(byte[] s) throws IOException {
		return Base64.encode(s);
	}

}
