JFLAGS = -g
JVM = java
JC = javac
FILE= 

.SUFFIXES: .java .class

.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
        receiver.java \
        sender.java \
        packet.java 

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class
