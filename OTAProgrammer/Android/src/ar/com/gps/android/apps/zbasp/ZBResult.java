package ar.com.gps.android.apps.zbasp;

public class ZBResult
{
	private int length;
	private ZBResultStatus status;
	public byte[] data;
	
	public int getLength()
	{
		return length;
	}
	public void setLength(int length)
	{
		this.length = length;
	}
	public ZBResultStatus getStatus()
	{
		return status;
	}
	public void setStatus(ZBResultStatus status)
	{
		this.status = status;
	}
	
}
