CFLAGS = -g -Wall

CC = gcc
LIBS = -lusb 
INCLUDES =
OBJS = main.o
SRCS = main.c
HDRS =
TARGET = zbasp


all: build

# The variable $@ has the value of the target. In this case $@ = psort
build: main.o ${OBJS}
	${CC} ${CFLAGS} ${INCLUDES} -o $(TARGET) ${OBJS} ${LIBS}

.c.o:
	${CC} ${CFLAGS} ${INCLUDES} -c $<

depend: 
	makedepend ${SRCS}

clean:
	rm *.o $(TARGET)

print:
	more Makefile $(HDRS) $(SRCS) | enscript -2r -p listing.ps
