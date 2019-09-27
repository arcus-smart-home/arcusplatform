# Client Bridge

The client bridge allows the user to interact with the Arcus Platform. The client bridge provides a handful of RESTful APIs for things like getting the product catalog, inviting users, etc. Additionally, the client bridge hosts a websocket server that the clients (e.g. iOS, Android, and Web applications) use to send messages to the Platform bus.

You can run a single instance of the client-bridge, or run multiple and configure different message prefixes depending on the class of user or application.

How to run the server:
=====================
  * Method 1:
com.iris.bus.NettoSphereServer is the server class. If you are in Eclipse/STS, I just right click on it and choose "run as Java application". It was an example, and I haven't made it better yet, so to quit you just type "quit" in the Eclipse/STS console.
  * Method 2:
I've added a gradle task, so type 'gradle run' to execute the server, and Ctrl-C to end it.

###How to run the test (Yes, we need something better...):
  1. Start the platform with "gradle startPlatform"  
  2. connect to Cassandra. Here's how I do it  
  /Applications/apache-cassandra-2.1.2/bin/cqlsh cassandra.eyeris  
  3. create the keyspace  
  CREATE KEYSPACE dev WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};  
  quit  
  4. Run ModelManager to create the schema  
  cd \[wherever_you_have_i2_checked_out\]/platform/arcus-modelmanager/  
  gradle installApp  
  cd build/install/modelmanager/bin  
  ./modelmanager -P dev  
  Y  
  5. Add the test user either via oculus or mobile App  
  6. Run the test  
  gradle --info cleanTest test  
