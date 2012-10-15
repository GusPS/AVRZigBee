package ar.com.gps.android.apps.zbasp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.xjava.delegates.MethodDelegate;

import android.content.Context;
import android.util.Log;
import android.widget.ProgressBar;

public class OTAProgrammer
{

	private static final int SIZE_CHUNK = 64;
	private MethodDelegate publishProgres;
	private File hexFile;
	private Integer zbAddr;
	private ProgressBar progressBar;
	private Context context;
	private String actionUsbPermission;

	public OTAProgrammer(Context context, String actionUsbPermission, Integer zbAddr, File hexFile,
			ProgressBar progressBar)
	{
		this.context = context;
		this.actionUsbPermission = actionUsbPermission;
		this.zbAddr = zbAddr;
		this.hexFile = hexFile;
		this.progressBar = progressBar;
	}

	public RdoProgram program()
	{
		ZBCoordinator zbc = new ZBCoordinator(this.context, this.actionUsbPermission);
		RdoProgram rdo = null;
		if (zbc.isCanUse())
			try
			{
				rdo = program2(zbc);
			} finally
			{
				zbc.close();
			}
		return rdo;
	}

	public RdoProgram program2(ZBCoordinator zbc)
	{
		if (!zbc.isHearbeatOk())
			return RdoProgram.NO_HEARTBEAT;

		ZBResult rdo = send(zbc, ("SC-" + String.format("%02X", zbAddr) + "-IBL").getBytes(), 3);
		if (rdo.getStatus() == ZBResultStatus.TRANSFER_ERROR)
			return RdoProgram.NO_IBL_RESPONSE;

		// Check if Bootloader
		if (rdo.data[0] == 0x00)
		{
			send(zbc, ("SC-" + String.format("%02X", zbAddr) + "-SBL").getBytes(), 3);
			try
			{
				publishProgres.invoke(Integer.valueOf(-1));
			} catch (Exception e)
			{
			}
			bootloaderDelay();
			rdo = send(zbc, ("SC-" + String.format("%02X", zbAddr) + "-IBL").getBytes(), 3);
			if (rdo.getLength() != 1 || rdo.data[0] == 0x00)
				return RdoProgram.CANT_SWITCH_BOOTLOADER;
			else if (rdo.data[0] != 0x01)
				return RdoProgram.SBL_INVALID_RESPONSE;
		} else if (rdo.data[0] != 0x01)
			return RdoProgram.IBL_INVALID_RESPONSE;

		rdo = send(zbc, ("SC-" + String.format("%02X", zbAddr) + "-GPS").getBytes(), 3);
		if (rdo.getLength() != 2)
			return RdoProgram.CANT_GET_PAGESIZE;
		int pageSize = ((rdo.data[0] << 8) & 0xFF) + (rdo.data[1] & 0xFF);
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
					return RdoProgram.INVALID_HEX_FILE;
				}

				int regType = Integer.parseInt(line.substring(7, 9), 16);
				if (regType == 0)
				{
					int data_cant = Integer.parseInt(line.substring(1, 3), 16);
					int address = Integer.parseInt(line.substring(3, 7), 16);
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
							return RdoProgram.IDX_GT_PAGESIZE;
						}
						page.data[idx] = (byte) (Integer.parseInt(line.substring(9 + (i * 2), 11 + (i * 2)), 16) & 0xFF);
					}
				} else if (regType == 1)
					break;
				else
				{
					br.close();
					return RdoProgram.UNKOWN_REGTYPE;
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
					Log.d("SND", cmd);
					int this_chunk = blPage.getSizeChunk();
					if (this_chunk + j > blPage.getPageSize())
						this_chunk = blPage.getPageSize() % blPage.getSizeChunk();
					byte[] buffer = new byte[cmd.length() + this_chunk];
					for (int i = 0; i < cmd.length(); i++)
						buffer[i] = (byte) cmd.charAt(i);
					for (int i = 0; i < this_chunk; i++)
						buffer[i + cmd.length()] = (byte) blPage.data[i + j];
					page_offset += this_chunk;

					String out = "";
					for (int q = cmd.length(); q < buffer.length; q++)
					{
						out += String.format("%02X", buffer[q]) + "-";
					}
					Log.d("SND", out);

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
			}
		}

		// CRC Check
		int crc16 = getCrc16();
		String cmd = "SC-" + String.format("%02X", zbAddr) + "-CRC-" + String.format("%04X", minAddress) + "-"
				+ String.format("%04X", maxAddress);
		rdo = send(zbc, cmd.getBytes(), 3);
		boolean crc_ok = (rdo.getLength() == 2) && crc16 == (((rdo.data[0] & 0xFF) << 8) | (rdo.data[1] & 0xFF));
		if (crc_ok)
		{
			send(zbc, ("SC-" + String.format("%02X", zbAddr) + "-QBL").getBytes(), 3);
			return RdoProgram.OTA_OK;
		} else
			return RdoProgram.CRC_ERROR;
	}

	private void bootloaderDelay()
	{
		try
		{
			Thread.sleep(3000);
		} catch (InterruptedException e)
		{
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
				byte data = blPage.data[i];
				data ^= crc & 0xFF;
				data ^= data << 4;

				crc = (short) ((((data & 0xFF) << 8) | ((crc & 0xFFFF) >> 8)) ^ ((data & 0xFF) >> 4) ^ ((data & 0xFF) << 3));
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
			ZBResult rta = zbc.readEP1(2);

			try
			{
				int length = rta.data[0];
				int rtype = rta.data[1];

				if (rtype == 1)
					return zbc.readEP1(length - 1);
				else
					throw new ZBException("ZB Timeout! - " + rtype);
			} catch (ZBException zbe)
			{
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
