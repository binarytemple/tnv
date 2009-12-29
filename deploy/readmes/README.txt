TNV depicts network traffic by visualizing packets and links between local and remote hosts. 

TNV is intended for network traffic analysis to facilitate learning what constitutes 'normal' activity on a network, investigating packet details security events, or network troubleshooting. TNV can open saved libpcap (from tcpdump, windump, ethereal, etc.) formatted files or capture live packets on the wire, and export data in libpcap format or save the data to a MySQL database to enable examining trends over time. 


Required Software
  Java JRE: J2SE version 1.5+. (if not installed, download and install latest jre from http://java.sun.com/javase/downloads/)

  pcap library: Standard on most Linux distributions and Mac OS X (if it is not installed, download and install the pcap library from: http://tcpdump.org/). For Windows, you must download and install the winpcap library prior to running TNV from: http://www.winpcap.org/

TNV also uses the following libraries which are included in the download:
 -Jpcap (version 0.16) / Released under Mozilla Public License 1.1 (MPL 1.1) pre-compiled for Linux, Windows, and Mac OS X (for other operating systems, you will need to download, compile, and install jpcap)
 -Piccolo (version 1.2) / Copyright (c) 2003-2006, University of Maryland. All rights reserved. Released under BSD License.
 -H2 (version 1.2.126) / Released under H2 License 1.0 (modified Mozilla Public License 1.1) and Eclipse Public License 1.0 (EPL) Licenses.
 -MySQL Connector/J (version 5.1.10) / Released under GPL.


Note to Linux Users: If you get an error: Failed dependencies: libstdc++-libc6.2-2.so.3 is needed, then you need to install the rpm compat-libstdc++

More information and the latest version can be found at:
http://tnv.sourceforge.net/
