#include <stdio.h> 
#include <usb.h> 
#include <string.h>

#define VENDOR_ID 0x16C0
#define PRODUCT_ID 0x05DC

#define SIZE_CHUNK 64

typedef struct bl_page
{
	unsigned short base_address;
	unsigned short page_size;
	unsigned short size_chunk;
	struct bl_page *prev;
	struct bl_page *next;
	unsigned char *data;
} bl_page;

struct usb_dev_handle *programmer;
FILE *fp;
bl_page first_page;

int debug_level = 0;

void debug (int level, char *msg);

void debug (int level, char *msg) {
	if (level >= debug_level) {
		return;
	}
	fprintf (stderr, "DEBUG: %s\n", msg);
}

#define xtod(c) ((c>='0' && c<='9') ? c-'0' : ((c>='A' && c<='F') ? \
                c-'A'+10 : ((c>='a' && c<='f') ? c-'a'+10 : 0)))
int string_to_hex(char *str, int len)
{
	char *numstr = str;
    int rdo = 0;
    while (len--)
    {
	    rdo <<= 4;
	    rdo |= xtod(numstr[0]);
	    numstr++;
    }
    return rdo;
}

void exit2(int i)
{
	bl_page *p, *q;
	p = &first_page;
	int a = 1;
	while(p != NULL)
	{
		q = p->next;
		if (p->data != NULL)
		{
			free(p->data);
			if (p->prev != NULL)
				free(p);
		}
		p = q;
		a++;
	}
	if (programmer != NULL)
	{
		usb_release_interface(programmer, 0);
		usb_close(programmer);
	}
	if (fp)
		fclose(fp);
	exit(i);
}

usb_dev_handle *find_programmer(void)
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

unsigned char buffer[1024];
int readEP1(int timeout)
{
	int numBytes = usb_interrupt_read(
        programmer,         // handle obtained with usb_open()
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
	        programmer,             // handle obtained with usb_open()
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
		    	else
		    		printf("Inconsistence Response!\n");
		    } else
		    	printf("ZB Timeout! - %i\n", rtype);
	    } else
	    	printf("USB Timeout - %i\n", numBytes);
	    
    	while (readEP1(500) >= 0);
    	retries--;
	}
	
	return -1;
}


bl_page *get_page(unsigned short address, unsigned short page_size)
{
	bl_page *p;
	
	unsigned short base_address = address - (address % page_size);
	
	p = &first_page;
	
	while (1)
	{
		if (p->page_size == 0)
		{
			p->base_address = base_address;
			p->page_size = page_size;
			p->size_chunk = SIZE_CHUNK;
			p->data = (unsigned char*) malloc(page_size);
  			memset(p->data, 0xff, page_size);
			return p;
		}
		
		if (p->base_address == base_address)
			return p;
		
		if (p->next != NULL)
			p = p->next;
		else {
			bl_page *new_page = (bl_page*) malloc(sizeof(bl_page));
			new_page->base_address = base_address;
			new_page->page_size = page_size;
			new_page->size_chunk = SIZE_CHUNK;
			new_page->data = (unsigned char*) malloc(page_size);
			p->next = new_page;
			new_page->prev = p;
			new_page->next = NULL;
  			memset(new_page->data, 0xff, page_size);
  			p = new_page;
			return p;
		}
	}
}

unsigned short get_crc16(void)
{
	unsigned short crc = 0xFFFF;
	bl_page *p = &first_page;

	while (p != NULL)
	{
		int i;
		for (i = 0; i < p->page_size; i++)
		{
			unsigned char data = p->data[i];
			data ^= crc & 0xFF;
			data ^= data << 4;
			crc = ((((unsigned short)data << 8)
				| (crc >> 8)) 
				^ ((data >> 4) & 0xFF) 
				^ ((unsigned short)data << 3));
		}
		p = p->next;
	}
		
	return crc;
}

int main(int argc, char *argv[])
{ 
	first_page.prev = NULL; 
	first_page.next = NULL;
	first_page.data = NULL;
	first_page.page_size = 0;
	
	char *dst_node = argv[1];
	char *hex_file = argv[2];
	
	//ToDo check que dst_node sea de 2 digitos y hexadecimal
	
	fp = fopen(hex_file,"r");
	if(fp) {
		//fclose(fp);
	} else {
		fprintf(stderr, "File %s doesn't exist\n", hex_file);
		exit2(-1);
	}
	
	char szBuf[1000];
	int sz = 0;

	usb_init();
	
	programmer = find_programmer();
	buffer[0] = 0;
	buffer[1] = 1;
	
	if (programmer != NULL)
	{
		usb_set_configuration(programmer, 1);
		usb_claim_interface(programmer, 0);
		
		//Empty the libusb buffer
		/*printf("Freeing up buffer\n");
		while (readEP1(500) >= 0);
		printf("Buffer clear\n");*/
		
		if (!heartbeat())
		{
			fprintf(stderr, "NO Heartbear!\n");
			exit2(-1);
		}
			
		sz = sprintf(szBuf, "SC-%s-IBL", dst_node);
		if (send(szBuf, sz, 3) == 1)
		{
			if (buffer[0] == 0x00)
			{
				sz = sprintf(szBuf, "SC-%s-SBL", dst_node);
				send(szBuf, sz, 3);
				printf("Waiting 3 seconds for Node to go into bootloader\n");
				sleep(3);
				sz = sprintf(szBuf, "SC-%s-IBL", dst_node);
				if (send(szBuf, sz, 3) != 1 || buffer[0] == 0x00)
				{
					printf("Node can't switch to Bootloader\n");
					return -1;
				} else if (buffer[0] != 0x01) {
					printf("Invalid IBL return\n");
					exit2(-1);
				}
			} else if (buffer[0] != 0x01) {
				printf("Invalid IBL return\n");
				exit2(-1);
			}
		} else {
			printf("Invalid IBL return\n");
			exit2(-1);
		}
		
		sz = sprintf(szBuf, "SC-%s-GPS", dst_node);
		if (send(szBuf, sz, 3) == 2)
		{
			unsigned short min_address = 0xFFFF;
			unsigned short max_address = 0x0000;
			unsigned short page_size = (unsigned short) ((buffer[0] << 8) + buffer[1]);
			printf("Page Size: %i\n", page_size);
			
			char line [128]; /* or other suitable maximum line size */
 
      		while(fgets(line, sizeof(line), fp) != NULL ) /* read a line */
      		{
	      		//printf("%s", line);
	      		if (line[0] != ':')
	      		{
		      		printf("%s\nFile Format Error\n", line);
		      		exit2(-1);
	      		}
	      		
	      		unsigned char reg_type = string_to_hex(line + 7, 2);
				if (reg_type == 0)
				{
					unsigned char data_cant = string_to_hex(line + 1, 2);
					unsigned short address = string_to_hex(line + 3, 4);
					if (min_address > address)
						min_address = address;
					char *data = line + 9;
					int i;
					for (i = 0; i < data_cant; i++)
					{
						bl_page *page = get_page(address + i, page_size);
						unsigned char idx = address + i - page->base_address;
						if (max_address < page->base_address + page->page_size)
							max_address = page->base_address + page->page_size;
						if (idx > page_size)
						{
							printf("Address Out Of Page Range\n");
							exit2(-1);
						}
						page->data[idx] = string_to_hex(data + (i * 2), 2);
					}
				} else if (reg_type == 1)
					break;
				else {
					printf("Invalid reg_type: 0x%02X", reg_type);
					exit2(-1);
				}
	      		
      		}
      		fclose(fp);
      		fp = NULL;
      		
      		//Writing
      		bl_page *p = &first_page;
      		while (p != NULL)
      		{
      			unsigned short page_offset = 0;
      			int j;
      			int redo_page = 0;
      			for (j = 0; j < p->page_size; j+= p->size_chunk)
				{
      				int sz = sprintf(szBuf, "SC-%s-BWP-%04X-%02X-", dst_node, p->base_address, page_offset);
      				int this_chunk = p->size_chunk;
      				if (this_chunk + j > p->page_size)
      					this_chunk = p->page_size % p->size_chunk;
      				memcpy(szBuf + sz, p->data + j, this_chunk);
      				page_offset += this_chunk;
      				
      				int o;
      				for (o = 0; o < sz; o++)
      					printf("%c", szBuf[o]);
      				printf("\nRdo:");
		    		/*char *sp = szBuf + sz;
		    		
		    		for (o = 0; o < this_chunk; o++)
		    		{
						printf("%02X-", (unsigned char)sp[0]);
						sp++;
		    		}
					printf("\n");*/
					
					sz = send(szBuf, sz + this_chunk, 0);
					if (sz == -1)
					{
      					redo_page = 1;
      					j = p->page_size; //Force Exit
					}
      				for (o = 0; o < sz; o++)
      					printf("%02X-", buffer[o]);
      				printf("\n");
		        }
		        if (!redo_page)
		        	p = p->next;
      		}
      		
      		//Verifying
			printf("Verifying uploaded firmware...\n");
			unsigned short crc16 = get_crc16();
			sz = sprintf(szBuf, "SC-%s-CRC-%04X-%04X", dst_node, min_address, max_address);
			printf("%s\n", szBuf);
			int rdo = send(szBuf, sz, 3);
			int crc_ok = rdo == 2 && crc16 == ((buffer[0] << 8) | buffer[1]);
			if (crc_ok)
			{
				printf("FIRMWARE UPDATE OK\n");
				sz = sprintf(szBuf, "SC-%s-QBL", dst_node);
				printf("Jumping to app...\n");
				send(szBuf, sz, 3);
			} else {
				printf("FIRMWARE VERIFICATION ERROR\n");
				printf("0x%04X\n", crc16);
				printf("0x%04X\n", ((buffer[0] << 8) | buffer[1]));
			}
		}
		exit2(0);
	} else {
		printf("Device Not Found.\n");
		exit2(-1);
	}
} 


