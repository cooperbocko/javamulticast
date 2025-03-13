Server:
message list {time, message}
client map/list {id, {socket, isRegistered, isConnected, portnumber}}
message queue
selector

one thread for handling connections
one thread for handling which connections are ready to be executed -> a thread pool for handling the requests
one thread for sending out the multicast messages

need a protocol? {message length, message}

one thread to handle client commands
one thread to handle sending messagesA
or
event loop with a message queue

Client:
time of last message recived
