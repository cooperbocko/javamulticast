package com.mycompany.app.coordinator;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Set;

import com.mycompany.app.utils.Connection;
import com.mycompany.app.utils.Message;

public class Coordinator {
    private static final int MAX_MESSAGE_LENGTH = 4096;
    private static int portNumber = 8080;
    private static int messageTimeout = 30; //in seconds
    
    // Store participant connections with participant IDs as keys 
    private static ConcurrentHashMap<Integer, Connection> connections = new ConcurrentHashMap<Integer, Connection>();
    
    // Queue for messages to be processed and stored 
    private static ConcurrentLinkedDeque<Message> messageQueue = new ConcurrentLinkedDeque<Message>();
    private static ConcurrentLinkedDeque<Message> messageList = new ConcurrentLinkedDeque<Message>();

    public static void main(String args[]) throws IOException {
        // Thread pool
        ExecutorService threadPool = Executors.newFixedThreadPool(5);

        // Open Server Socket
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new java.net.InetSocketAddress(portNumber));
        serverSocketChannel.configureBlocking(false);

        // Open Selector
        Selector selector = Selector.open();

        // Register server with selector
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("Server started on port: " + portNumber);

        // Handle connections and socket readiness
        while (true) {
            // Call selector
            selector.select();

            // Get keys
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> keys = selectedKeys.iterator();

            // Check each socket
            while(keys.hasNext()) {
                System.out.println(LocalDateTime.now().toString());
                SelectionKey key = keys.next();
                keys.remove();

                if (key.isAcceptable()) {
                    // Accepting new connections
                    ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
                    SocketChannel clientChannel = serverChannel.accept();
                    clientChannel.configureBlocking(false);

                    // Register client with selector
                    clientChannel.register(selector, SelectionKey.OP_READ);
                    System.out.println("Client connected: " + clientChannel.getRemoteAddress());
                } else if (key.isReadable()) {
                    //handle client requests
                    //threadPool.submit(new DoSomething(clientChannel));
                    new HandleRequest(key).run();
                }
                else if (key.isWritable()) {

                }
            }
        }

    }

    //handle request
    static class HandleRequest implements Runnable {
        private final SocketChannel participant;
        private final SelectionKey key;

        public HandleRequest(SelectionKey key) {
            this.participant = (SocketChannel) key.channel();
            this.key = key;
        }

        @Override
        public void run() {
            try {
                ByteBuffer buf = ByteBuffer.allocate(MAX_MESSAGE_LENGTH);
                int bytesRead = participant.read(buf);
                if (bytesRead == -1) {
                    System.out.println("Read Error");
                    participant.close();
                    key.cancel();
                    System.out.println("Participant Disconnect"); 
                    return; //cannot read from channel -> ERROR
                }
                if (bytesRead == 0) {
                    return; //sometimes there is nothing to read
                }

                //parse request and send response
                buf.flip();
                String request = new String(buf.array(), 0, buf.limit());
                System.out.println("participant " + participant.getRemoteAddress() + ": " + request);
                String[] parsedRequest = request.split(" ");
                
                switch(parsedRequest[0].toLowerCase()) {
                    case "register": {
                        //format -> register id ip port (assume it is correct format)
                        if (parsedRequest.length < 4) {
                            sendMessage(participant, "invalid format!");
                            break;
                        }

                        int id = Integer.parseInt(parsedRequest[1]);
                        String ip = parsedRequest[2];
                        int port = Integer.parseInt(parsedRequest[3]);
                        if (connections.containsKey(id)) {
                            sendMessage(participant, "Already Registered!");
                            break;
                        }

                        //create connection and add to map
                        Connection conn = new Connection();
                        conn.id = id;
                        conn.readIp = ip;
                        conn.readPort = port;
                        conn.isOnline = true;
                        connections.put(id, conn);

                        sendMessage(participant, "Registered!");
                        break;
                    }
                    case "deregister": {
                        //format -> deregister id
                        if (parsedRequest.length < 2) {
                            sendMessage(participant, "invalid format!");
                            break;
                        }

                        int id = Integer.parseInt(parsedRequest[1]);
                        if (!connections.containsKey(id)) {
                            sendMessage(participant, "Not Registered!");
                            break;
                        }
                        
                        connections.remove(id);
                        //TODO: is connection closed?
                        //participant.close();
                        //key.cancel();

                        sendMessage(participant, "Deregistered!");
                        break;
                    }
                    case "disconnect": {
                        //format -> disconnect id
                        if (parsedRequest.length < 2) {
                            sendMessage(participant, "invalid format!");
                            break;
                        }

                        int id = Integer.parseInt(parsedRequest[1]);
                        if (!connections.containsKey(id) || !connections.get(id).isOnline) {
                            sendMessage(participant, "Not registered or already disconnected!");
                            break;
                        }

                        connections.get(id).isOnline = false;

                        sendMessage(participant, "Disconnected!");
                        break;
                    }
                    case "reconnect": {
                        //format -> reconnect id timeoflastmessage
                        if (parsedRequest.length < 3) {
                            sendMessage(participant, "invalid format!");
                            break;
                        }

                        int id = Integer.parseInt(parsedRequest[1]);
                        if (!connections.containsKey(id) || connections.get(id).isOnline) {
                            sendMessage(participant, "Not registered or already online!");
                            break;
                        }

                        connections.get(id).isOnline = true;
                        String timeoflastmessage = parsedRequest[2];
                        LocalDateTime time = LocalDateTime.parse(timeoflastmessage);
                        addReconnectMessages(id, time);

                        sendMessage(participant, "Reconnected!");
                        break;
                    }
                    case "multicast": {
                        //format multicast id message
                        if (parsedRequest.length < 3) {
                            sendMessage(participant, "invalid format!");
                            break;
                        }

                        int id = Integer.parseInt(parsedRequest[1]);
                        if (!connections.containsKey(id) || !connections.get(id).isOnline) {
                            sendMessage(participant, "Not registered or not online!");
                            break;
                        }

                        Message newMessage = new Message();
                        Set<Integer> ids = connections.keySet();
                        newMessage.idsToSend = new Integer[0]; //TODO: check that the instantiation works
                        newMessage.idsToSend = ids.toArray(newMessage.idsToSend);
                        newMessage.message = parsedRequest[2];
                        newMessage.time = LocalDateTime.now();
                        messageQueue.add(newMessage);
                        messageList.add(newMessage);

                        sendMessage(participant, "Message Sent!");
                        break;
                    }
                    default: {
                        sendMessage(participant, "Unknown Request: " + parsedRequest[0]);
                        break;
                    }
                }
            } catch (IOException e) {
                System.out.println("Participant Disconnect"); 
                key.cancel();
            }
        }
    }

    //sends message to participant at a channel. MUST USE IT FOR ALL MESSAGES TO CLIENT
    public static void sendMessage (SocketChannel channel, String message) throws IOException {
        byte[] messageBytes = message.getBytes();
        ByteBuffer buf = ByteBuffer.allocate(4 + MAX_MESSAGE_LENGTH);
        buf.putInt(messageBytes.length);
        buf.put(message.getBytes());
        buf.flip();
        channel.write(buf);
    }

    public static void addReconnectMessages(int id, LocalDateTime time) {
        //Create an iterator copy of message list, should be able to safely remove from front since new entries enter from the back
        LocalDateTime now = LocalDateTime.now().minusSeconds(messageTimeout);
        Iterator<Message> messages = messageList.iterator();
        while(messages.hasNext()) {
            Message message = messages.next();
            //remove all exprired messages
            if (now.isAfter(message.time)) {
                messageList.removeFirst();
            } else {
                //add messasge only if past the time specified
                if (time.isBefore(message.time)) {
                    Message newMessage = new Message();
                    Integer[] ids = {id};
                    newMessage.idsToSend = ids;
                    newMessage.message = message.message;
                    newMessage.time = message.time;
                    messageQueue.add(newMessage); //dont add to message list since we have already sent this messsage before
                }
            }
        }
    }
}
