package ar.com.gps.android.apps.zbasp;

public class BLPage
{

	public byte[] data;
	private int baseAddress;
	private int pageSize;
	private int sizeChunk;

	public BLPage(int baseAddress, int pageSize, int sizeChunk)
	{
		this.baseAddress = baseAddress;
		this.pageSize = pageSize;
		this.sizeChunk = sizeChunk;
		data = new byte[this.pageSize];
		for (int i = 0; i < data.length; i++)
			data[i] = (byte) 0xFF;
	}

	public int getBaseAddress()
	{
		return baseAddress;
	}

	public int getPageSize()
	{
		return pageSize;
	}

	public int getSizeChunk()
	{
		return sizeChunk;
	}

}
