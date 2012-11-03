#include <stdio.h> 
#include <usb.h> 
#include <string.h>

#define VENDOR_ID 0x16C0
#define PRODUCT_ID 0x05DC

struct usb_dev_handle *zbc;

void exit2(int i)
{
	if (zbc != NULL)
	{
		usb_release_interface(zbc, 0);
		usb_close(zbc);
	}
	exit(i);
}

usb_dev_handle *find_coordinator(void)
{
	struct usb_bus    *bus;
	struct usb_device *dev;

	usb_find_busses();
	usb_find_devices();
	for(bus = usb_get_busses(); bus; bus = bus->next)
	{
    	for(dev = bus->devices; dev; dev = dev->next)
	    {
		    if (dev->descriptor.idVendor == VENDOR_ID
		    	&& dev->descriptor.idProduct == PRODUCT_ID)
		    	return usb_open(dev);
    	}
	}
	
	return NULL;
}

int heartbeat(void)
{
	char buf[] = {1};
	
	int realNumBytes = usb_control_msg(
        zbc,             // handle obtained with usb_open()
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

unsigned char buffer[1024];
int readEP1(int timeout)
{
	int numBytes = usb_interrupt_read(
        zbc,         // handle obtained with usb_open()
        USB_ENDPOINT_IN | 1,// identifies endpoint 1
        buffer,             // data buffer
        sizeof(buffer),     // maximum amount to read
        timeout
    );
    
    /*printf("%i:", numBytes);
    
	int i;
	for (i = 0; i < numBytes; i++)
	{
		printf("%02X", buffer[i]);
		if (i != numBytes - 1)
			printf("-");
	}
	printf("\n");*/
	
    return numBytes;
}

int send(char *command, int size, int retries)
{
	char *sp = command;
	int rt = 0;
	/*int o;
	for (o = 0; o < size; o++)
	{
		printf("%c", (unsigned char)sp[0]);
		sp++;
	}
	printf("\n");*/
	
	//int retries = 3;
					
	while (retries >= 0)
	{
		int realNumBytes = usb_control_msg(
	        zbc,             // handle obtained with usb_open()
	        USB_TYPE_VENDOR | USB_RECIP_DEVICE | USB_ENDPOINT_OUT, // bRequestType
	        132,      			// bRequest
	        0,              	// wValue
	        0,              	// wIndex
	        command,            // pointer to destination buffer
	        size,	// wLength
	        1000				//timeoutInMilliseconds
	    );
	    
	    
	    //Read header response
		int numBytes = readEP1(3000);
	    
	    if (numBytes == 2)
	    {
		    int length = buffer[0];
		    int rtype = buffer[1];
		    
		    if (rtype == 1)
		    {
				numBytes = readEP1(500);
		    	if (numBytes == length - 1)
					return numBytes;
		    	else {
		    		//printf("Inconsistence Response!\n");
		    		rt = -1;
		    	}
		    } else {
		    	//printf("ZB Timeout! - %i\n", rtype);
		    	rt = -2;
		    }
	    } else {
	    	//printf("USB Timeout - %i\n", numBytes);
	    	rt = -3;
	    }
	    
    	while (readEP1(500) > 0);
    	retries--;
	}
	
	return rt;
}

int main (int argc, char *argv[])
{
	char szBuf[1000];
	int sz = 0;
	
	char *dst_node;
	char *cmd;
	
	if (argc == 2)
		cmd = argv[1];
	else if (argc == 3) {
		dst_node = argv[1];
		cmd = argv[2];
	} else {
		printf("ARGS!.\n");
		exit2(-1);
	}

	usb_init();
	
	zbc = find_coordinator();
	
	if (zbc != NULL)
	{
		usb_set_configuration(zbc, 1);
		usb_claim_interface(zbc, 0);
		
		//Empty the libusb buffer
		//printf("Freeing up buffer\n");
		while (readEP1(50) > 0);
		//printf("Buffer clear\n");
		
		if (!heartbeat())
		{
			fprintf(stderr, "NO Heartbear!\n");
			exit2(-1);
		}
		
		if (argc == 2)
			sz = send(cmd, strlen(cmd), 3);
		else {
			sz = sprintf(szBuf, "SC-%s-%s", dst_node, cmd);
			sz = send(szBuf, sz, 3);
		}
		if (sz == -1)
			printf("Inconsistence Response!");
		else if (sz == -2)
			printf("ZB Timeout!");
		else if (sz == -3)
			printf("USB Timeout");
		//printf("Rdo:");
		int i;
		for (i = 0; i < sz; i++)
		{
			printf("%02X", buffer[i]);
			if (i != sz - 1)
				printf("-");
		}
		printf("\n");
			
		exit2(0);
	} else {
		printf("Device Not Found.\n");
		exit2(-1);
	}
}

