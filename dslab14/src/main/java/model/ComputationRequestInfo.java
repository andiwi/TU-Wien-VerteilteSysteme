package model;

import java.io.Serializable;

/**
 * Please note that this class is not needed for Lab 1, but will later be
 * used in Lab 2. Hence, you do not have to implement it for the first
 * submission.
 */
public class ComputationRequestInfo implements Serializable, Comparable<ComputationRequestInfo> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5918463948462046890L;
	
	private String timestamp;
	private String node;
	private String mathematicalTerm;
	
	public String getTimestamp()
	{
		return timestamp;
	}
	public void setTimestamp(String timestamp)
	{
		this.timestamp = timestamp;
	}
	public String getNode()
	{
		return node;
	}
	public void setNode(String node)
	{
		this.node = node;
	}
	public String getMathematicalTerm()
	{
		return mathematicalTerm;
	}
	public void setMathematicalTerm(String mathematicalTerm)
	{
		this.mathematicalTerm = mathematicalTerm;
	}
	
	@Override
	public String toString()
	{
		return timestamp + " [" + node + "]: " + mathematicalTerm;
	}
	@Override
	public int compareTo(ComputationRequestInfo o)
	{
		return this.getTimestamp().compareTo(o.getTimestamp());
	}
}
