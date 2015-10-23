package com.ehorizon.moveslikejabber.events;

/**
 * Created by phjecr on 10/23/15.
 */
public class ChatEvent {

    private int mChatState;

    public String getToId() {
        return mToId;
    }

    public void setToId(String toId) {
        this.mToId = toId;
    }

    private String mToId;
    public static final int CREATE_CHAT = 1;

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
}
