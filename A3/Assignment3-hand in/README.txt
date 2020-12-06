CS455 Assigment3 
Yuzhan Jiang (20680648)

Note:
I have to code file named Router.java and Packet.java while are Java code, so our compiler is Javac, and I built my program and tested on two different machines 6 terminals in the students.cs.uwaterloo.ca environment.

How to run my program?
	-Firstly, use make command to compile
	-Secondly, use the following command to generate 6 random unused port numbers:
		comm -23 <(seq 1024 65535 | sort) <(ss -tan | awk '{print $4}' | cut - 
		d':' -f2 | grep "[0-9]\{1,5\}" | sort -u) | shuf | head -n 6 
	-Then, on the first machine, run the command:
		./nse-linux386 host2 port1 
	-Then, on the second machine, open 5 terminals, for each terminals runs the corresponding command:
		Java router 1 host1 port2 
		Java router 2 host1 port3
		Java router 3 host1 port4
		Java router 4 host1 port5
		Java router 5 host1 port6
	-Finally, you will see five routet.log files
（Note that: host name usually can be ubuntu1804-002, ubuntu1804-004, ubuntu1804-008
		

