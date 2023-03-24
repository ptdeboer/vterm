# Makefile


#All
ALL:ptty.lxe ptty-debug.lxe


#Targets
ptty.lxe: ptty.cpp
	gcc -Wall -g ptty.cpp -o ptty.lxe

ptty-debug.lxe: ptty.cpp
	gcc -DDEBUG -Wall -g ptty.cpp -o ptty-debug.lxe


# Clean
clean:
	rm ptty.lxe ptty-debug.lxe
