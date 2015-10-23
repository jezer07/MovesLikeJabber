package com.ehorizon.moveslikejabber.events;

/**
 * Created by phjecr on 10/23/15.
 */
public class ChatEvent {

    private int mChatState;

    private String mToId;
    private String mFromId;
    private String mMessage;
    public static final int CREATE_CHAT = 1;
    public static final int NEW_MESSAGE = 2;
    public static final int UPDATE_PRESENCE = 3;


    public String getToId() {
        return mToId;
    }

    public void setToId(String toId) {
        this.mToId = toId;
    }


    public ChatEvent(int state) {
        mChatState = state;
    }
    public ChatEvent(int state,String toId) {
        mChatState = state;
        mToId = toId;
    }
    public int getChatState() {
        return mChatState;
    }

    public void setChatState(int chatState) {
        this.mChatState = chatState;
    }

    public String getMessage() {
        return mMessage;
    }

    public void setMessage(String message) {
        this.mMessage = message;
    }

    public String getFromId() {
        return mFromId;
    }

    public void setFromId(String fromId) {
        this.mFromId = fromId;
    }
}
