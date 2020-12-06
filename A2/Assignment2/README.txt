CS456 Assignment 2 
Yuzhan Jiang (20680648)

Note: 
These are Java code, so our compiler is javac, and have to test on the three different machines in the students.cs.uwaterloo.ca environment.

How to run my program? 
	-First, use make command to compile
	-Secondly, use the following command to generate 4 random port numbers:
		comm -23 <(seq 1024 65535 | sort) <(ss -tan | awk '{print $4}' | cut - 
		d’:’ -f2 | grep "[0-9]\{1,5\}" | sort -u) | shuf | head -n 4

	-Then, for the first machine, run the command:
		./nEmulator port1 host2  port3 port4 host3 port2 1 0.2 0 

	-Then, for the second machine, run the command:
		java receiver host1 port4 port3 “out.txt” 

	-Then, for the third machine, run the command:
		java sender host1 port1 port2 “in.txt” 

	Finally,  u will see the out.txt has the same content with in.txt

	(Note that: host name usually can be ubuntu1804-00x, x: 2,4 and 8 or nettop20, nettop18, nettop16)
	
	
