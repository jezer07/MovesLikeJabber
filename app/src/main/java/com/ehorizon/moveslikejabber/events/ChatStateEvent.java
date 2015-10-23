package com.ehorizon.moveslikejabber.events;

import org.jivesoftware.smackx.chatstates.ChatState;

/**
 * Created by phjecr on 10/23/15.
 */
public class ChatStateEvent {



    ChatState mChatState;

    public ChatStateEvent(ChatState state){
        mChatState= state;
    }
    public ChatState getChatState() {
        return mChatState;
    }

    public void setChatState(ChatState ChatState) {
        this.mChatState = ChatState;
    }

}
