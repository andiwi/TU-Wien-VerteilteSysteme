package controller;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;

import util.Keys;

import java.io.File;
import java.io.IOException;

public class HMACChannel{

	public HMACChannel(){}
	
	public byte[] hmac(byte[] s, String path) throws IOException {
		File f = new File(path);
		Key secretKey = Keys.readSecretKey(f);
		// make sure to use the right ALGORITHM for what you want to do (see text)
		Mac hMac=null;
		try {
			hMac = Mac.getInstance("HmacSHA256");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		try {
			hMac.init(secretKey);
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		}
		// MESSAGE is the message to sign in bytes
		hMac.update(s);
		byte[] hash = hMac.doFinal();
		return hash;
	}

}
