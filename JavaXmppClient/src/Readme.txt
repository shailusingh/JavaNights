Readme
------------------------------------------
	This is a Java client program that act as a xmpp client. 
	You should have a xmpp server setup and accessible.
	Not to mention, you should have Java installed and working.

Dependencies:
	Checkout jars under /lib
		-smack-3.4.1-0cec571.jar
		-smackx-3.4.1-0cec571.jar
		-xpp3-1.1.4c.jar


Usage:

java -classpath .;lib/smack-3.4.1-0cec571.jar;lib/smackx-3.4.1-0cec571.jar;lib/xpp3-1.1.4c.jar JavaXmppClient <HOST> <USER> <PWD>
