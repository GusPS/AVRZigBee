package ar.com.gps.android.apps.zbasp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.xjava.delegates.MethodDelegate;

import android.widget.ProgressBar;

public class OTAProgrammer
{

	private static final int SIZE_CHUNK = 64;
	private MethodDelegate publishProgres;
	private File hexFile;
	private Integer zbAddr;
	private ProgressBar progressBar;

	public OTAProgrammer(Integer zbAddr, File hexFile, ProgressBar progressBar)
	{
		this.zbAddr = zbAddr;
		this.hexFile = hexFile;
		this.progressBar = progressBar;
	}

	public Integer program()
	{
		ZBCoordinator zbc = new ZBCoordinator();
		if (zbc.isHearbeatOk())
			return Integer.valueOf(-4);
		// TODO Verificar si el nodo está en BL, sino switchear a BL
		ZBResult rdo = send(zbc, ("SC-" + String.format("%02X", zbAddr) + "-IBL").getBytes(), 3);
		if (rdo.getStatus() == ZBResultStatus.TRANSFER_ERROR)
			return Integer.valueOf(-5);

		// Check if Bootloader
		if (rdo.data[0] == 0x00)
		{
			send(zbc, ("SC-" + String.format("%02X", zbAddr) + "-SBL").getBytes(), 3);
			// ToDo Toast
			// ("Waiting 3 seconds for Node to go into bootloader\n");
			bootloaderDelay();
			rdo = send(zbc, ("SC-" + String.format("%02X", zbAddr) + "-IBL").getBytes(), 3);
			if (rdo.getLength() != 1 || rdo.data[0] == 0x00)
			{
				return Integer.valueOf(-7);
			} else if (rdo.data[0] != 0x01)
			{
				return Integer.valueOf(-8);
			}
		} else if (rdo.data[0] != 0x01)
		{
			return Integer.valueOf(-6);
		}

		int pageSize = 128; // TODO
		int minAddress = 0xFFFF;
		int maxAddress = 0x0000;
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(hexFile));
			String line;

			while ((line = br.readLine()) != null)
			{
				if (line.charAt(0) != ':')
				{
					br.close();
					return Integer.valueOf(1);
				}

				int regType = Integer.parseInt(line.substring(7, 9), 16);
				if (regType == 0)
				{
					int data_cant = Integer.parseInt(line.substring(1, 3), 16);
					int address = Integer.parseInt(line.substring(3, 4), 16);
					if (minAddress > address)
						minAddress = address;
					for (int i = 0; i < data_cant; i++)
					{
						BLPage page = getPage(address + i, pageSize);
						int idx = address + i - page.getBaseAddress();
						if (maxAddress < page.getBaseAddress() + page.getPageSize())
							maxAddress = page.getBaseAddress() + page.getPageSize();
						if (idx > pageSize)
						{
							br.close();
							return Integer.valueOf(2);
						}
						page.data[idx] = (char) Integer.parseInt(line.substring(9 + (i * 2), 11 + (i * 2)), 16);
					}
				} else if (regType == 1)
					break;
				else
				{
					br.close();
					return Integer.valueOf(2);
				}
			}
			br.close();
		} catch (IOException ioe)
		{
			ioe.printStackTrace();
		}

		progressBar.setMax(BLPages.size());
		int pn = 0;
		for (Map.Entry<Integer, BLPage> entry : BLPages.entrySet())
		{
			BLPage blPage = entry.getValue();
			while (true)
			{
				boolean redoPage = false;
				int page_offset = 0;
				int j;
				for (j = 0; j < blPage.getPageSize(); j += blPage.getSizeChunk())
				{
					String cmd = "SC-" + String.format("%02X", zbAddr) + "-BWP-"
							+ String.format("%04X", blPage.getBaseAddress()) + "-" + String.format("%02X", page_offset)
							+ "-";
					cmd.toCharArray();
					int this_chunk = blPage.getSizeChunk();
					if (this_chunk + j > blPage.getPageSize())
						this_chunk = blPage.getPageSize() % blPage.getSizeChunk();
					byte[] buffer = new byte[cmd.length() + this_chunk];
					for (int i = 0; i < cmd.length(); i++)
						buffer[i] = (byte) cmd.charAt(i);
					for (int i = 0; i < this_chunk; i++)
						buffer[i + cmd.length()] = (byte) blPage.data[i + j];
					page_offset += this_chunk;

					rdo = send(zbc, buffer, 0);

					if (rdo.getStatus() == ZBResultStatus.TRANSFER_ERROR)
					{
						redoPage = true;
						break;
					}
				}
				if (!redoPage)
					break;
			}
			try
			{
				publishProgres.invoke(Integer.valueOf(++pn));
			} catch (Exception e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// CRC Check
		int crc16 = getCrc16();
		rdo = send(zbc,
				("SC-" + String.format("%02X", zbAddr) + "-CRC-" + String.format("%04X", minAddress) + "-" + String
						.format("%04X", maxAddress)).getBytes(), 3);
		boolean crc_ok = rdo.getLength() == 2 && crc16 == ((rdo.data[0] << 8) | rdo.data[1]);
		if (crc_ok)
		{
			rdo = send(zbc, ("SC-" + String.format("%02X", zbAddr) + "-QBL").getBytes(), 3);
		} else
		{
			return Integer.valueOf(-3);
		}

		return Integer.valueOf(0);
	}

	private void bootloaderDelay()
	{
		try
		{
			Thread.sleep(3000);
		} catch (InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private int getCrc16()
	{
		short crc = (short) 0xFFFF;
		for (Map.Entry<Integer, BLPage> entry : BLPages.entrySet())
		{
			BLPage blPage = entry.getValue();
			for (int i = 0; i < blPage.getPageSize(); i++)
			{
				char data = blPage.data[i];
				data ^= crc & 0xFF;
				data ^= data << 4;
				crc = (short) ((((short) data << 8) | (crc >> 8)) ^ ((data >> 4) & 0xFF) ^ ((short) data << 3));
			}
		}

		return crc;
	}

	private ZBResult send(ZBCoordinator zbc, byte[] buffer, int retries)
	{
		ZBResult rdo = new ZBResult();

		while (true)
		{
			zbc.send(buffer);
			// Read header response
			ZBResult rta = zbc.readEP1(3000);

			try
			{
				if (rta.getLength() == 2)
				{
					int length = rta.data[0];
					int rtype = rta.data[1];

					if (rtype == 1)
					{
						rta = zbc.readEP1(500);
						if (rta.getLength() == length - 1)
						{
							rdo.setLength(rta.getLength());
							rdo.setStatus(ZBResultStatus.TRANSFER_OK);
							return rdo;
						} else
							throw new ZBException("Inconsistence Response");
					} else
						throw new ZBException("ZB Timeout! - " + rtype);
				} else
					throw new ZBException("USB Timeout - " + rta.getLength());
			} catch (ZBException zbe)
			{
				while (zbc.readEP1(500).getLength() >= 0)
					;
				retries--;
				if (retries <= 0)
					break;
			}

		}

		rdo.setStatus(ZBResultStatus.TRANSFER_ERROR);
		return rdo;
	}

	Map<Integer, BLPage> BLPages = new TreeMap<Integer, BLPage>();

	private BLPage getPage(int address, int pageSize)
	{
		int baseAddress = address - (address % pageSize);

		BLPage blPage = BLPages.get(baseAddress);

		if (blPage == null)
		{
			blPage = new BLPage(baseAddress, pageSize, SIZE_CHUNK);
			BLPages.put(blPage.getBaseAddress(), blPage);
		}
		return blPage;
	}

	public void setPublishProgress(MethodDelegate publishProgress)
	{
		this.publishProgres = publishProgress;
	}

}
