package ar.com.gps.android.apps.zbasp;

import java.util.HashMap;
import java.util.Iterator;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

public class ZBCoordinator
{
	private UsbDeviceConnection usbDeviceConnection = null;

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

		if (device != null)
		{
			device.getInterface(0);
			this.usbDeviceConnection = manager.openDevice(device);
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
		// TODO Auto-generated method stub

		/*
		 * int realNumBytes = usb_control_msg(programmer, // handle obtained //
		 * with usb_open() USB_TYPE_VENDOR | USB_RECIP_DEVICE |
		 * USB_ENDPOINT_OUT, // bRequestType 132, // bRequest 0, // wValue 0, //
		 * wIndex command, // pointer to destination buffer size, // wLength
		 * 1000 // timeoutInMilliseconds );
		 */

		return new ZBResult();
	}

	public ZBResult readEP1(int msTimeOut)
	{
		// TODO Auto-generated method stub
		/*
		 * int numBytes = usb_interrupt_read(programmer, // handle obtained with
		 * // usb_open() USB_ENDPOINT_IN | 1,// identifies endpoint 1 buffer, //
		 * data buffer sizeof(buffer), // maximum amount to read msTimeOut);
		 */

		/*
		 * printf("%i:", numBytes);
		 * 
		 * int i; for (i = 0; i < numBytes; i++) { printf("%02X", buffer[i]); if
		 * (i != numBytes - 1) printf("-"); } printf("\n");
		 */

		return new ZBResult();

	}

	public void close()
	{
		if (this.usbDeviceConnection != null)
			this.usbDeviceConnection.close();
	}

}
