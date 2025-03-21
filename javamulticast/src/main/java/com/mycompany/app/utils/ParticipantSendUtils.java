package com.mycompany.app.utils;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class ParticipantSendUtils {

    private static String EnterMessage(Scanner input) {
        System.out.print("Enter a message to send: ");
        String msg = input.nextLine().trim();

        while (msg == null || msg.equals("")) {
            System.out.print("You must enter a valid message: ");
            msg = input.nextLine().trim();
        } //while

        return msg;
    } //EnterMessage


    public static void SendMessage(Scanner input, SocketChannel channel, int participantId) {

        String msg = EnterMessage(input);

        if (msg == null) {
            return;
        } //if

        String cmdToServer = "multicast " + participantId + " " + msg;

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
    
}
