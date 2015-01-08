package controller;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class AESChannel implements Channel
{
	Base64Channel b = new Base64Channel();
	
	public AESChannel(){}

	@Override
	public byte[] decode(String s) throws IOException {
		return null;
	}

	@Override
	public byte[] encode(byte[] s) throws IOException {
		return null;
	}
	
	public String encrypt (String text, SecretKey key, IvParameterSpec iv)
	{
		Cipher cipher = null;
		
		
		try {
			cipher = Cipher.getInstance("AES/CTR/NoPadding");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		}

		try {
			cipher.init(Cipher.ENCRYPT_MODE, key, iv);
		} catch (InvalidKeyException e1) {
			e1.printStackTrace();
		} catch (InvalidAlgorithmParameterException e1) {
			e1.printStackTrace();
		}
		
			
		byte[] encthird = null;
		try {
			encthird = cipher.doFinal(text.getBytes());
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		}
		byte[] enctext = null;
		try {
			enctext = b.encode(encthird);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return new String(enctext);
	}
	
	public String decrypt (byte[] text, SecretKey key, IvParameterSpec iv)
	{
		Cipher cipher = null;
		
		
		try {
			cipher = Cipher.getInstance("AES/CTR/NoPadding");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		}

		try {
			cipher.init(Cipher.DECRYPT_MODE, key, iv);
		} catch (InvalidKeyException e1) {
			e1.printStackTrace();
		} catch (InvalidAlgorithmParameterException e1) {
			e1.printStackTrace();
		}
		
			
		byte[] enctext = null;
		try {
			enctext = cipher.doFinal(text);
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		}
		
		return new String(enctext);
	}
	
}
