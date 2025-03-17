package com.mycompany.app.coordinator;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
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
                    SocketChannel clientChannel = (SocketChannel) key.channel();
                    //threadPool.submit(new DoSomething(clientChannel));
                    new HandleRequest(clientChannel, key).run();
                }
            }
        }

    }

    //handle request
    static class HandleRequest implements Runnable {
        private final SocketChannel participant;
        private final SelectionKey key;

        public HandleRequest(SocketChannel participant, SelectionKey key) {
            this.participant = participant;
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
                    return;
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
                        break;
                    }
                    case "deregister": {
                        //format -> deregister id
                        //remove from map and close channel/key
                        int id = Integer.parseInt(parsedRequest[1]);
                        if (!connections.containsKey(id)) {
                            //TODO: Send error
                            break;
                        }
                        connections.remove(id);
                        //TODO: is connection closed?
                        //participant.close();
                        //key.cancel();

                        //TODO: Send ack
                        break;
                    }
                    case "disconnect": {
                        //format -> disconnect id
                        int id = Integer.parseInt(parsedRequest[1]);
                        if (!connections.containsKey(id) || !connections.get(id).isOnline) {
                            //TODO: Send error
                            break;
                        }

                        connections.get(id).isOnline = false;
                        //TODO: Send ack
                        break;
                    }
                    case "reconnect": {
                        //format -> reconnect id timeoflastmessage
                        int id = Integer.parseInt(parsedRequest[1]);
                        if (!connections.containsKey(id) || connections.get(id).isOnline) {
                            //TODO: Send error
                            break;
                        }

                        connections.get(id).isOnline = true;
                        //TODO: send messages that were not sent
                        //TODO: Send ack
                        break;
                    }
                    case "multicast": {
                        //format multicast message
                    }
                }
                
                String response = "World!";
                buf.clear();
                buf.put(response.getBytes());
                buf.flip();
                participant.write(buf);

            } catch (IOException e) {
                System.out.println("Participant Disconnect"); 
                key.cancel();
            }
        }

    }
    /*
     * static class DoSomething implements Runnable {
        private final SocketChannel clientChannel;
        private final SelectionKey key;

        public DoSomething(SocketChannel clientChannel, SelectionKey key) {
            this.clientChannel = clientChannel;
            this.key = key;
        }

        @Override
        public void run() {
            try {
                ByteBuffer buf = ByteBuffer.allocate(256);
                int bytesRead = clientChannel.read(buf);

                if (bytesRead == -1) {
                    System.out.println("Client disconnected: " + clientChannel.getRemoteAddress());
                    clientChannel.close();
                    return;
                }

                if (bytesRead == 0) {
                    return;
                }

                buf.flip();
                String message = new String(buf.array(), 0, buf.limit());
                System.out.println("Clinet: " + message);

                String response = "World!";
                buf.clear();
                buf.put(response.getBytes());
                buf.flip();
                clientChannel.write(buf);
            } catch (IOException e) {
                System.out.println("Client Disconnect");
                key.cancel();
            }
        }
    }
     */
    
}
