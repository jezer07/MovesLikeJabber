package com.ehorizon.moveslikejabber.events;

import org.jivesoftware.smackx.chatstates.ChatState;

/**
 * Created by phjecr on 10/23/15.
 */
public class ChatStateEvent {


    private boolean mIsMine;
    ChatState mChatState;


    public ChatStateEvent(ChatState state,boolean isMine){
        mIsMine = isMine;
        mChatState= state;
    }
    public ChatState getChatState() {
        return mChatState;
    }

    public void setChatState(ChatState ChatState) {
        this.mChatState = ChatState;
    }

    public boolean isIsMine() {
        return mIsMine;
    }

    public void setIsMine(boolean isMine) {
        this.mIsMine = isMine;
    }
}
