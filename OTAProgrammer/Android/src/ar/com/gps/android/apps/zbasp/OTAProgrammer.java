package ar.com.gps.android.apps.zbasp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.xjava.delegates.MethodDelegate;

import android.widget.ProgressBar;

public class OTAProgrammer
{

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
		// TODO Abrir USB
		// TODO Verificar Heartbeat
		// TODO Verificar si el nodo está en BL, sino switchear a BL
		// TODO Obtener el PageSize
		// TODO Abrir el .hex en modo lectura
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(hexFile));
			String line;

			int min_address = 0xFFFF;
			int max_address = 0x0000;
			int pageSize = 128; // TODO

			while ((line = br.readLine()) != null)
			{
				if (line.charAt(0) != ':')
					return Integer.valueOf(1);

				int reg_type = Integer.parseInt(line.substring(7, 9), 16);
				if (reg_type == 0)
				{
					int data_cant = Integer.parseInt(line.substring(1, 3), 16);
					int address = Integer.parseInt(line.substring(3, 4), 16);
					if (min_address > address)
						min_address = address;
					for (int i = 0; i < data_cant; i++)
					{
						BLPage page = getPage(address + i, pageSize);
						int idx = address + i - page.getBaseAddress();
						if (max_address < page.getBaseAddress() + page.getPageSize())
							max_address = page.getBaseAddress() + page.getPageSize();
						if (idx > pageSize)
						{
							return Integer.valueOf(2);
						}
						page.data[idx] = Integer.parseInt(line.substring(9 + (i * 2), 11 + (i * 2)), 16);
					}
				} else if (reg_type == 1)
					break;
				else
				{
					return Integer.valueOf(2);
				}
			}
			br.close();
		} catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
		// TODO Leer todo el .hex armado los pages

		progressBar.setMax(100);
		// TODO Recorrer todas las páginas y mandarlas vía ZB
		// TODO Verificar el CRC

		for (int i = 0; i <= 100; i++)
		{
			try
			{
				publishProgres.invoke(Integer.valueOf(i));
			} catch (Exception e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try
			{
				Thread.sleep(10);
			} catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}

		return Integer.valueOf(0);
	}

	private BLPage getPage(int i, int pageSize)
	{
		// TODO Auto-generated method stub
		return new BLPage();
	}

	public void setPublishProgress(MethodDelegate publishProgress)
	{
		this.publishProgres = publishProgress;
	}

}
