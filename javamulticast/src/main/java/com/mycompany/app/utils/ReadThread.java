package com.mycompany.app.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;

import com.mycompany.app.participant.Participant;

public class ReadThread implements Runnable {
    private static final int MAX_MESSAGE_LENGTH = 4096;
    private int port;
    private ServerSocketChannel channel;
    private SocketChannel coordinator;
    private String fileName;

    public ReadThread(int port, String fileName) {
        this.port = port;
        this.fileName = fileName;
    }

    @Override
    public void run() {
        try {
            //open channel
            channel = ServerSocketChannel.open();
            channel.bind(new java.net.InetSocketAddress(port));

            while (true) {
                //waits until a new connection and reads the response and then closes the connection
                //System.out.println("Waiting for multi");
                coordinator = channel.accept();
                read(coordinator);
                coordinator.close();
                //System.out.println("Multi Message Recieved!");
            }

        } catch (Exception e) {

        } finally {
            /* 
            if (channel.isOpen()) {
                try {
                    channel.close();
                } catch (Exception e) {

                }
            }
                */
        }
    }

    private int read(SocketChannel channel) throws IOException {

        ByteBuffer rbuf = ByteBuffer.allocate(4);
        int error = Participant.readFull(channel, rbuf, 4);
        if (error <= 0) {
            if (error == 0) {
                System.out.println("EOF");
            } else {
                System.out.println("Read Error");
            }
            return -1;
        }

        rbuf.flip();
        int len = rbuf.getInt();

        if (len > MAX_MESSAGE_LENGTH) {
            System.out.println("Too Long");
            return -1;
        }

        rbuf = ByteBuffer.allocate(len);

        error = Participant.readFull(channel, rbuf, len);
        if (error <= 0) {
            System.out.println("Read Error");
            return -1;
        }

        rbuf.flip();
        String response = new String(rbuf.array(), 0, rbuf.limit());
        //System.out.println("Wrirting response: " + response);

        //write to file
        FileWriter writer = new FileWriter(fileName, true);
        writer.write(response + '\n');
        writer.close();
        return 1;
    }
    
}


