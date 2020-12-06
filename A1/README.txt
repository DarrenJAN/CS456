CS456 Assignment 1
Yuzhan Jiang  20680648 

Two source code file:
	server.py client.py 

Two modified scripts:
	server.sh  client.sh 

Instructions:
	Run server first:
		./server.sh <req_code>

	Then run client:
		./client.sh <server_address> <n_port> <req_code> <message>

	Note:
		1. <req_code> should be integer, eg: 13 
		2. <server_addres>s usually be 127.0.0.1 if it's in linux.student.cs environment 
		3. ./server.sh will print the server port number as <n_port>
		4. <message> format 'your message is balalala'
		5. you can run ./client.sh many times once you run ./server
	
Test:
	All codes are tested in the linux.student.cs environmnet and they are able to run in a single machine or two diiferent student.cs machines

Makefile:
	Since I used python for this assignment, there is no need to submit Makefile
