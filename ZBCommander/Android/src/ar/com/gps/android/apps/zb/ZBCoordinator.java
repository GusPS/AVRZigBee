package ar.com.gps.android.apps.zb;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.util.Log;

public class ZBCoordinator
{
	private UsbDeviceConnection usbDeviceConnection = null;
	private UsbEndpoint endpointInt = null;
	private boolean canUse = false;

	public ZBCoordinator(Context context, String actionUsbPermission)
	{
		super();
		UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
		HashMap<String, UsbDevice> deviceList = manager.getDeviceList();

		Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
		UsbDevice device = null;
		while (deviceIterator.hasNext())
		{
			UsbDevice devi = deviceIterator.next();
			// TODO Remove hardcoded IDs
			if (devi.getProductId() == 0x05dc && devi.getVendorId() == 0x16c0)
				device = devi;
			// TODO Make sure this is the ZBCoordinator using RAW Descriptors
		}

		PendingIntent mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(actionUsbPermission), 0);

		if (device != null && !manager.hasPermission(device))
			manager.requestPermission(device, mPermissionIntent);

		if (device != null && manager.hasPermission(device))
		{
			UsbInterface intf = device.getInterface(0);
			this.endpointInt = intf.getEndpoint(0);
			this.usbDeviceConnection = manager.openDevice(device);
			this.usbDeviceConnection.claimInterface(intf, false);
			this.canUse = true;
		}
	}

	public boolean isHearbeatOk()
	{
		if (usbDeviceConnection == null)
			return false;

		byte[] buffer =
		{ 1 };
		// USB_TYPE_VENDOR | USB_RECIP_DEVICE | USB_ENDPOINT_IN
		usbDeviceConnection.controlTransfer(UsbConstants.USB_TYPE_VENDOR | UsbConstants.USB_ENDPOINT_XFER_CONTROL
				| UsbConstants.USB_DIR_IN, 130, 1, 0, buffer, 1, 1000);
		return buffer[0] == 2;
	}

	public ZBResult send(byte[] buffer)
	{
		if (usbDeviceConnection == null)
			return null;

		// USB_TYPE_VENDOR | USB_RECIP_DEVICE | USB_ENDPOINT_OUT
		int realNumBytes = usbDeviceConnection.controlTransfer(UsbConstants.USB_TYPE_VENDOR
				| UsbConstants.USB_ENDPOINT_XFER_CONTROL | UsbConstants.USB_DIR_OUT, 132, 0, 0, buffer, buffer.length,
				1000);

		ZBResult rdo = new ZBResult();
		rdo.setLength(realNumBytes);
		rdo.data = new byte[realNumBytes];
		for (int i = 0; i < realNumBytes; i++)
			rdo.data[i] = buffer[i];
		if (realNumBytes < 0)
			rdo.setStatus(ZBResultStatus.TRANSFER_ERROR);
		else
			rdo.setStatus(ZBResultStatus.TRANSFER_OK);
		Log.d("USB", rdo.getStatus().toString());
		return rdo;
	}

	public ZBResult readEP1(int numBytes)
	{
		if (usbDeviceConnection == null)
			return null;

		Log.d("USB", "Reading " + numBytes + " bytes from EP1");
		ZBResult rdo = new ZBResult();
		rdo.setStatus(ZBResultStatus.TRANSFER_OK);

		UsbRequest request = new UsbRequest();
		request.initialize(usbDeviceConnection, endpointInt);
		ByteBuffer buffer = ByteBuffer.allocate(numBytes);
		request.queue(buffer, numBytes);
		usbDeviceConnection.requestWait();
		rdo.setLength(numBytes);
		rdo.data = buffer.array();
		for (int i = 0; i < numBytes; i++)
			Log.d("USB" + i, rdo.data[i] + "");

		return rdo;
	}

	public void close()
	{
		if (this.usbDeviceConnection != null)
			this.usbDeviceConnection.close();
	}

	public boolean isCanUse()
	{
		return canUse;
	}

}
