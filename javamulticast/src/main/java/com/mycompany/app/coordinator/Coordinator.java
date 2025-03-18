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
    private static int messageTimeout;
    private static ConcurrentHashMap<Integer, Connection> connections = new ConcurrentHashMap<>();
    private static ConcurrentLinkedDeque<Message> messageQueue = new ConcurrentLinkedDeque<>();
    private static ConcurrentLinkedDeque<Message> messageList = new ConcurrentLinkedDeque<>();

    public static void main(String args[]) throws IOException {
        //Thread pool
        ExecutorService threadPool = Executors.newFixedThreadPool(5);

        //Open Server Socket
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new java.net.InetSocketAddress(portNumber));
        serverSocketChannel.configureBlocking(false);

        //Open Selector
        Selector selector = Selector.open();

        //Register server with selector
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("Server started on port: " + portNumber);

        //handle connections and socket readiness
        while (true) {
            //call selector
            selector.select();

            //get keys
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> keys = selectedKeys.iterator();

            //check each socket
            while(keys.hasNext()) {
                SelectionKey key = keys.next();
                keys.remove();

                if (key.isAcceptable()) {
                    //accpting new connections
                    ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
                    SocketChannel clientChannel = serverChannel.accept();
                    clientChannel.configureBlocking(false);

                    //register client with selector
                    clientChannel.register(selector, SelectionKey.OP_READ);
                    System.out.println("Client connected: " + clientChannel.getRemoteAddress());
                } else if (key.isReadable()) {
                    //handle client requests
                    //threadPool.submit(new DoSomething(clientChannel));
                    new HandleRequest(key).run();
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
                        int id = Integer.parseInt(parsedRequest[1]);
                        String ip = parsedRequest[2];
                        int port = Integer.parseInt(parsedRequest[3]);
                        if (connections.containsKey(id)) {
                            //TODO: Send error
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

                        //TODO: Send ack
                        sendMessage(participant, "Registered!");
                        break;
                    }
                    case "deregister": {
                        //format -> deregister id
                        int id = Integer.parseInt(parsedRequest[1]);
                        if (!connections.containsKey(id)) {
                            //TODO: Send error
                            sendMessage(participant, "Not Registered!");
                            break;
                        }
                        
                        connections.remove(id);
                        //TODO: is connection closed?
                        //participant.close();
                        //key.cancel();

                        //TODO: Send ack
                        sendMessage(participant, "Deregistered!");
                        break;
                    }
                    case "disconnect": {
                        //format -> disconnect id
                        int id = Integer.parseInt(parsedRequest[1]);
                        if (!connections.containsKey(id) || !connections.get(id).isOnline) {
                            //TODO: Send error
                            sendMessage(participant, "Not registered or already disconnected!");
                            break;
                        }

                        connections.get(id).isOnline = false;

                        //TODO: Send ack
                        sendMessage(participant, "Disconnected!");
                        break;
                    }
                    case "reconnect": {
                        //format -> reconnect id timeoflastmessage
                        int id = Integer.parseInt(parsedRequest[1]);
                        if (!connections.containsKey(id) || connections.get(id).isOnline) {
                            //TODO: Send error
                            sendMessage(participant, "Not registered or already online!");
                            break;
                        }

                        connections.get(id).isOnline = true;
                        //TODO: send messages that were not sent

                        //TODO: Send ack
                        sendMessage(participant, "Reconnected!");
                        break;
                    }
                    case "multicast": {
                        //format multicast id message
                        int id = Integer.parseInt(parsedRequest[1]);
                        if (!connections.contains(id) || !connections.get(id).isOnline) {
                            //TODO: send error
                            sendMessage(participant, "Not registered or not online!");
                            break;
                        }

                        Message newMessage = new Message();
                        newMessage.idsToSend = connections.keys();
                        newMessage.message = parsedRequest[2];
                        newMessage.time = LocalDateTime.now();
                        messageQueue.add(newMessage);
                        messageList.add(newMessage);

                        //TODO: send ack
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

    //TODO: Maybe change it from thorwing an error to catching the error and returning -1
    //TODO: Also maybe migrate to a util file since 
    public static void sendMessage (SocketChannel channel, String message) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(MAX_MESSAGE_LENGTH);
        buf.put(message.getBytes());
        buf.flip();
        channel.write(buf);
    }


    //call by value cant return message and possible errors at the same time without a created type
    /* 
    public static int recieveMessage (SocketChannel channel, String message) throws IOException{
        ByteBuffer buf = ByteBuffer.allocate(MAX_MESSAGE_LENGTH);
        int bytesRead = channel.read(buf);

        if (bytesRead == -1) {
            return -1; //Read Error
        }
        if (bytesRead == 0) {
            return 0; //sometimes there is nothing to read
        }

        buf.flip();
        message = new String(buf.array(), 0, buf.limit());
        return 1;
    }
    */
}
