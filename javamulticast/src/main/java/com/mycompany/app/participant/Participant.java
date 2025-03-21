package com.mycompany.app.participant;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

import com.mycompany.app.utils.ParticipantSendUtils;

public class Participant {

    /*
     * Time of last message
     * One thread to send a message
     * One thread to recieve data and write to conf
     */

    private static int participantId; // Unique participant ID

    private static SocketChannel channel = null;


    public static void main(String args[]) throws IOException {

        Scanner input = new Scanner(System.in);

        InetSocketAddress server = new InetSocketAddress("localhost", 8080);

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


        //Thread for entering a message or registering / deregistering
        new Thread(() -> {
            String cmd = "";
            while (cmd.equalsIgnoreCase("quit") == false) {

                System.out.print("Enter a command: ");
                cmd = input.nextLine();

                switch (cmd) {
                    case "register":

                        break;
                    
                    case "deregister":

                        break;
                    
                    case "multicast":
                        ParticipantSendUtils.SendMessage(input, channel, participantId);
                        break;

                    case "default":
                        break;
                } //switch
            } //while

            //The user has entered quit into the command line
            try {
                channel.close();
                input.close();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            } //try
        }).start();


        //Thread for receiving messages from the server
        new Thread(() -> {

        }).start();

    } //main

} //Participant
