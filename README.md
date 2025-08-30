# Video-Streaming-Service

  A video streaming service, featuring Client-Server communication with java sockets, a load balancing server, the use of a logger (log4j) and video streaming and stream capturing (video download) using FFMPEG. Using FFMPEG on the server also tries to convert saved videos so that there are a range of qualities (240p, 360p, 480p, 720p, 1080p) and a range of video formats (".avi", ".mp4", ".mkv"). Through the use of threads, multiple clients can stream from a single server and with the load balancer in place you can run a system that includes multiple servers to simulate a modern streaming service.

I developed this project for a course in UNIWA and later refactored it and tried to make it a JAR.

How to run (outdated):
  - Execute the MainServer.java (this starts the load balancing server on port "1111").
  - Execute the Server.java (with a port number as its CLI argument except "1111"), this connects a streaming server to the load balancer.
  - Execute the Client.java to start a GUI and select an available video to stream (or download).
  

Notes (outdated, next update will probably include the ffmpeg in the JAR):
- You must download [ffmpeg](https://www.ffmpeg.org/download.html) (unzip it) and place it the "resources/ffmpeg" directory so that the executables can be accessed from within the project, e.g. "resources/ffmpeg/bin/ffmpeg.exe"

- The client does a download speed test and sends information about the speed to the server. To do that it uses the [JSpeedTest](https://github.com/bertrandmartel/speed-test-lib/tree/master) library and downloads from one of their servers, if this function doesn't work check their github page for a fresh list of servers and change the URL.

- This project was developed in the Eclipse IDE, some of the components i installed are:
  m2e-wtp - JAX-RS configurator for WTP (Optional)
  m2e-wtp - JPA configurator for WTP (Optional)
  m2e-wtp - JSF configurator for WTP (Optional)
  m2e-wtp - Maven Integration for WTP
