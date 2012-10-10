package ar.com.gps.android.apps.zbasp;

public class ZBCoordinator
{

	public boolean isHearbeatOk()
	{
		// TODO Auto-generated method stub
		
		char buf[] = {1};
		
		int realNumBytes = usb_control_msg(
	        programmer,             // handle obtained with usb_open()
	        USB_TYPE_VENDOR | USB_RECIP_DEVICE | USB_ENDPOINT_IN, // bRequestType
	        130,      			// bRequest
	        1,              	// wValue
	        0,              	// wIndex
	        buf,             // pointer to destination buffer
	        1,  				// wLength
	        1000				//timeoutInMilliseconds
	    );
	    
	    return (buf[0] == 2);

	}

	public ZBResult send(byte[] buffer)
	{
		// TODO Auto-generated method stub
		int realNumBytes = usb_control_msg(programmer, // handle obtained
				// with usb_open()
				USB_TYPE_VENDOR | USB_RECIP_DEVICE | USB_ENDPOINT_OUT, // bRequestType
				132, // bRequest
				0, // wValue
				0, // wIndex
				command, // pointer to destination buffer
				size, // wLength
				1000 // timeoutInMilliseconds
		);
	}

	public ZBResult readEP1(int msTimeOut)
	{
		// TODO Auto-generated method stub
		int numBytes = usb_interrupt_read(programmer, // handle obtained with
				// usb_open()
				USB_ENDPOINT_IN | 1,// identifies endpoint 1
				buffer, // data buffer
				sizeof(buffer), // maximum amount to read
				msTimeOut);

		/*
		 * printf("%i:", numBytes);
		 * 
		 * int i; for (i = 0; i < numBytes; i++) { printf("%02X", buffer[i]); if
		 * (i != numBytes - 1) printf("-"); } printf("\n");
		 */

		return numBytes;

	}

}
