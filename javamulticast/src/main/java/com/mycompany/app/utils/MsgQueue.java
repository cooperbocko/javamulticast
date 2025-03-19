package com.mycompany.app.utils;

import java.util.ArrayList;
import java.util.HashMap;

public class MsgQueue {

    private static MsgQueue instance = null;
    private HashMap<Integer, ArrayList<Msg>> queue = null;

    private MsgQueue() {
        queue = new HashMap<Integer, ArrayList<Msg>>();
    } // constructor

    public static synchronized MsgQueue getInstance() {
        if (instance == null) {
            instance = new MsgQueue();
        } // if
        return instance;
    } // returns the single instance

    public void addMsg(int id, String message) {
        if (!queue.containsKey(id)) {
            queue.put(id, new ArrayList<Msg>());
        } // if

        queue.get(id).add(new Msg(message));

    } // addMsg

    public void addMsg(int[] ids, String message) {
        for (int id : ids) {
            addMsg(id, message);
        }
    } // addMsg

    public String[] getMessages(int id) {
        ArrayList<Msg> iMsg = queue.get(id);
        String[] returnArray = new String[iMsg.size()];
        for (int i = 0; i < iMsg.size(); i++) {
            returnArray[i] = iMsg.get(i).getMessage();
        }
        return returnArray;
    } // getMessages

    public void purgeQueue(int id) {
        queue.remove(id);
    } // purge all message when deregistered

}
