package com.mycompany.app.participant;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class Participant {

    /*
     * Time of last message
     * One thread to send a message
     * One thread to recieve data and write to conf
     */
    public static void main(String args[]) throws IOException {

        Scanner input = new Scanner(System.in);

        InetSocketAddress server = new InetSocketAddress("localhost", 8080);

        SocketChannel channel = null;
        channel = SocketChannel.open();
        channel.connect(server);

        // Wait until the connection is established
        while (!channel.finishConnect()) {

            // You could perform other tasks here while waiting for the connection to finish
            // For example, a small sleep to avoid busy-waiting
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } //try
            
        } //while


        String cmd = "";
        String msg = null;
        while (cmd.equalsIgnoreCase("quit") == false) {

            System.out.print("Enter a command: ");
            cmd = input.nextLine();

            switch (cmd) {
                case "register":

                    break;
                
                case "deregister":

                    break;
                
                case "multicast":
                    System.out.print("Enter a message to send: ");
                    msg = input.nextLine();
                    SendMessage(msg, channel);
                    break;
                case "default":
                    break;
            } //switch
        } //while

        channel.close();
        input.close();

    } //main


    private static void SendMessage(String msg, SocketChannel channel) {
        if (msg == null) {
            return;
        } //if

        //Not sure where to get the Participant id from so I just hard coded
        //1 for now
        String cmdToServer = "multicast " + "1" + " " + msg;

        try {
            ByteBuffer buf = ByteBuffer.wrap(cmdToServer.getBytes());

            while (buf.hasRemaining()) {
                channel.write(buf);
            } //while

            buf.clear();
            int bytesRead = channel.read(buf);
            if (bytesRead > 0) {
                buf.flip();
                byte[] resp = new byte[buf.remaining()];
                buf.get(resp);
                System.out.println(new String(resp));
            } //if
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } //try
    } //SendMessage
    
} //Participant
