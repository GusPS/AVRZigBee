package ar.com.gps.android.apps.zb;

import android.content.Context;
import android.util.Log;

public class ZBCmd
{
	private String cmd;
	private Integer zbAddr;
	private Context context;
	private String actionUsbPermission;

	public ZBCmd(Context applicationContext, String actionUsbPermission, Integer zbAddr, String cmd)
	{
		this.context = applicationContext;
		this.actionUsbPermission = actionUsbPermission;
		this.zbAddr = zbAddr;
		this.cmd = cmd;
	}

	public RdoCmd run(String cmd)
	{
		ZBCoordinator zbc = new ZBCoordinator(this.context, this.actionUsbPermission);
		RdoCmd rdo = null;
		if (zbc.isCanUse())
			try
			{
				rdo = run2(zbc);
				rdo.setNodeId(this.zbAddr);
				rdo.setCmd(cmd);
			} finally
			{
				zbc.close();
			}
		return rdo;
	}

	private RdoCmd run2(ZBCoordinator zbc)
	{
		RdoCmd rta = new RdoCmd();
		rta.setResult(RdoCmd.CMD_OK);

		if (!zbc.isHearbeatOk())
			rta.setResult(RdoCmd.NO_HEARTBEAT);

		Log.d("USB", "Hay Heartbeat");
		Log.d("ZBC", "Enviando:" + "SC-" + String.format("%02X", zbAddr) + "-" + this.cmd);
		ZBResult rdo = send(zbc, ("SC-" + String.format("%02X", zbAddr) + "-" + this.cmd).getBytes(), 3);
		if (rdo.getStatus() == ZBResultStatus.TRANSFER_OK)
			rta.setData(rdo.data, rdo.getLength());
		else
			rta.setResult(RdoCmd.CMD_ERROR);
		
		return rta;
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
				Log.d("RTR", "Retries: " + retries);
				if (retries <= 0)
					break;
			}
		}

		rdo.setStatus(ZBResultStatus.TRANSFER_ERROR);
		return rdo;
	}

}
