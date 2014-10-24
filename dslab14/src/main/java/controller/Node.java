package controller;

import java.net.InetAddress;
import java.util.Date;
import java.util.Set;

public class Node
{
	private String ip;
	private int tcpPort;
	private InetAddress inetAddress;
	private Status status;
	private int usage;
	private Date lastIsAlivePackage;
	private Set<Character> operators;
	
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public int getTcpPort() {
		return tcpPort;
	}
	public void setTcpPort(int tcpPort) {
		this.tcpPort = tcpPort;
	}
	public Status getStatus() {
		return status;
	}
	public void setStatus(Status status) {
		this.status = status;
	}
	public int getUsage() {
		return usage;
	}
	public void setUsage(int usage) {
		this.usage = usage;
	}
	
	public Date getLastIsAlivePackage() {
		return lastIsAlivePackage;
	}
	public void setLastIsAlivePackage(Date lastIsAlivePackage) {
		this.lastIsAlivePackage = lastIsAlivePackage;
	}
	
	public Set<Character> getOperators() {
		return operators;
	}
	public void setOperators(Set<Character> operators) {
		this.operators = operators;
	}
	
	@Override
	public String toString() {
		return "IP: " + ip + " Port: " + tcpPort + " " + status	+ " Usage: " + usage;
	}
	public InetAddress getInetAddress()
	{
		return inetAddress;
	}
	public void setInetAddress(InetAddress inetAddress)
	{
		this.inetAddress = inetAddress;
	}
	
	
	
}
