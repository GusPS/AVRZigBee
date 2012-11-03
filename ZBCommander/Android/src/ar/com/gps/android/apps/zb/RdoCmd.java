package ar.com.gps.android.apps.zb;

public class RdoCmd
{

	public static final int CMD_OK = 0;
	public static final int NO_HEARTBEAT = 1;
	public static final int CMD_ERROR = 2;

	private int result = -1;
	private byte[] data;
	private Integer nodeId;
	private String cmd;

	public int getResult()
	{
		return result;
	}

	public void setResult(int result)
	{
		this.result = result;
	}

	public byte[] getData()
	{
		return data;
	}

	public void setData(byte[] data2, int length)
	{
		data = new byte[length];
		System.arraycopy(data2, 0, data, 0, length);
	}

	public Integer getNodeId()
	{
		return nodeId;
	}

	public void setNodeId(Integer nodeId)
	{
		this.nodeId = nodeId;
	}

	public String getCmd()
	{
		return cmd;
	}

	public void setCmd(String cmd)
	{
		this.cmd = cmd;
	}

}
