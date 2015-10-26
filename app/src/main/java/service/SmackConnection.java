package service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.preference.PreferenceManager;
import android.util.Log;

import com.ehorizon.moveslikejabber.events.ChatEvent;
import com.ehorizon.moveslikejabber.events.ChatStateEvent;
import com.ehorizon.moveslikejabber.pojo.Contact;

import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.chatstates.ChatState;
import org.jivesoftware.smackx.chatstates.ChatStateListener;
import org.jivesoftware.smackx.chatstates.ChatStateManager;
import org.jivesoftware.smackx.muc.InvitationListener;
import org.jivesoftware.smackx.muc.InvitationRejectionListener;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.ping.PingFailedListener;
import org.jivesoftware.smackx.ping.PingManager;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import de.duenndns.ssl.MemorizingTrustManager;
import de.greenrobot.event.EventBus;


/**
 * Created by Furuha on 27.12.2014.
 */
public class SmackConnection implements ConnectionListener, ChatManagerListener, RosterListener, ChatMessageListener, PingFailedListener,
        ChatStateListener, InvitationListener, InvitationRejectionListener, MessageListener {

    private  String jid;
    private Chat mChat;
    private boolean isGroup;

    public static enum ConnectionState {
        CONNECTED, CONNECTING, RECONNECTING, DISCONNECTED;
    }

    private static final String TAG = "SMACK";
    private final Context mApplicationContext;
    private final String mPassword;
    private final String mUsername;
    private final String mPort;
    private final String mHost;
    private final String mServiceName;

    public static XMPPTCPConnection mConnection;
    private MultiUserChatManager mucManager;
    private ArrayList<String> mRoster;
    private BroadcastReceiver mReceiver;
    private EventBus mEventBus;

    public static List<Contact> presence ;

    public SmackConnection(Context pContext) {
        Log.i(TAG, "ChatConnection()");
        mApplicationContext = pContext.getApplicationContext();
        mPassword = PreferenceManager.getDefaultSharedPreferences(mApplicationContext).getString("xmpp_password", "jez");
        jid = PreferenceManager.getDefaultSharedPreferences(mApplicationContext).getString("xmpp_jid", "jez@ehorizon.com");
        mPort = PreferenceManager.getDefaultSharedPreferences(mApplicationContext).getString("xmpp_port", "5222");
        mHost = PreferenceManager.getDefaultSharedPreferences(mApplicationContext).getString("xmpp_host", "localhost");
        mServiceName = jid.split("@")[1];
        mUsername = jid.split("@")[0];
        mEventBus = EventBus.getDefault();
        if(!mEventBus.isRegistered(this)){

            mEventBus.register(this);
        }


    }




    public void connect() throws IOException, XMPPException, SmackException {
        Log.i(TAG, "connect()");

        XMPPTCPConnectionConfiguration.Builder builder = XMPPTCPConnectionConfiguration.builder();
        builder.setServiceName(mServiceName);
        builder.setHost(mHost);
        builder.setPort(Integer.parseInt(mPort));
        builder.setResource(jid);
        builder.setUsernameAndPassword(mUsername, mPassword);

        builder.setKeystoreType("BKS");
        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("TLS");
            MemorizingTrustManager mtm = new MemorizingTrustManager(mApplicationContext);
            sslContext.init(null, new X509TrustManager[]{mtm}, new java.security.SecureRandom());
            builder.setCustomSSLContext(sslContext);
            builder.setHostnameVerifier(mtm.wrapHostnameVerifier(new org.apache.http.conn.ssl.StrictHostnameVerifier()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }


        mConnection = new XMPPTCPConnection(builder.build());

        Roster.getInstanceFor(mConnection).setRosterLoadedAtLogin(true);
        //Set ConnectionListener here to catch initial connect();
        mConnection.addConnectionListener(this);


        mConnection.connect();
        mConnection.login();

        PingManager.setDefaultPingInterval(600); //Ping every 10 minutes
        PingManager pingManager = PingManager.getInstanceFor(mConnection);
        pingManager.registerPingFailedListener(this);

        setupSendMessageReceiver();

        ChatManager.getInstanceFor(mConnection).addChatListener(this);

       // ChatManager.getInstanceFor(mConnection).createChat("leq@192.168.63.196",new );
        Roster.getInstanceFor(mConnection).addRosterListener(this);
        Roster.getInstanceFor(mConnection).setSubscriptionMode(Roster.SubscriptionMode.accept_all);

    }

    public void disconnect() {
        Log.i(TAG, "disconnect()");
        if(mConnection != null){
            mConnection.disconnect();
        }

        mConnection = null;
        if(mReceiver != null){
            mApplicationContext.unregisterReceiver(mReceiver);
            mReceiver = null;
        }
    }


    private void rebuildRoster() {


        mRoster = new ArrayList<>();
        presence = new ArrayList<>();
        String status;
        for (RosterEntry entry : Roster.getInstanceFor(mConnection).getEntries()) {
            if(Roster.getInstanceFor(mConnection).getPresence(entry.getUser()).isAvailable()){
                status = "Online";
            } else {
                status = "Offline";
            }
            mRoster.add(entry.getUser() + ": " + status);
            presence.add(new Contact(entry.getUser(), status.equals("Online") ? true : false));
        }
        ChatEvent event = new ChatEvent(ChatEvent.UPDATE_PRESENCE);
        mEventBus.post(event);
    /*    Intent intent = new Intent(SmackService.NEW_ROSTER);
        intent.setPackage(mApplicationContext.getPackageName());
        intent.putStringArrayListExtra(SmackService.BUNDLE_ROSTER, mRoster);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        }
        mApplicationContext.sendBroadcast(intent);*/
    }

    private void setupSendMessageReceiver() {
        mReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(SmackService.SEND_MESSAGE)) {
                    sendMessage(intent.getStringExtra(SmackService.BUNDLE_MESSAGE_BODY), intent.getStringExtra(SmackService.BUNDLE_TO));
                }
            }

        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(SmackService.SEND_MESSAGE);
        mApplicationContext.registerReceiver(mReceiver, filter);
    }

    public void onEvent(ChatStateEvent e){
        if(e.isIsMine()&&!isGroup) {
            try {
                Log.d("State","Publishing state "+e.getChatState().toString());
                ChatStateManager.getInstance(mConnection).setCurrentState(e.getChatState(), mChat);
            } catch (SmackException.NotConnectedException e1) {
                e1.printStackTrace();
            }
        }


    }
    public void onEvent(ChatEvent e){
        Log.d("onEvent",""+e.getChatState());
        switch (e.getChatState()){
            case ChatEvent.CREATE_CHAT:
                Log.i(TAG, "sendMessage()");
                isGroup = false;
                ChatManager.getInstanceFor(mConnection).createChat(e.getToId(), this);
                break;
            case ChatEvent.CREATE_CONFERENCE:
                mucManager = MultiUserChatManager.getInstanceFor(mConnection);
                isGroup = true;
                createConference(e.getToId());
                break;
            case ChatEvent.JOIN_CONFERENCE:
                mucManager = MultiUserChatManager.getInstanceFor(mConnection);
                isGroup = true;
                joinChat(e.getToId());
                break;

        }

    }

    private void sendMessage(String body, String toJid) {

        try {
            if(!isGroup)
            mChat.sendMessage(body);
            else
            sendGroupMessage(toJid, body);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
    }

    //ChatListener

    @Override
    public void stateChanged(Chat chat, ChatState state) {
        Log.d("Message","stateChange");
    }

    @Override
    public void invitationReceived(XMPPConnection conn, final MultiUserChat room, final String inviter, String reason, String password, Message message) {
        Log.d(TAG, "Inviting ...... " + inviter + ":" + reason + ":" + room.getRoom());
        try {
            room.join(jid);
        } catch (SmackException.NoResponseException e) {
            e.printStackTrace();
        } catch (XMPPException.XMPPErrorException e) {
            e.printStackTrace();
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void processMessage(Message message) {
        Log.d(TAG, "group message : " + message.getBody() + " - " + message.getFrom());
        if(message.getType() == Message.Type.groupchat && message.getBody() != null){
            ChatEvent event = new ChatEvent(ChatEvent.NEW_MESSAGE);
            event.setMessage(message.getBody());
            event.setFromId(message.getFrom());
            mEventBus.post(event);
        }
    }

    @Override
    public void invitationDeclined(String invitee, String reason) {
        Log.d(TAG, "rejected ... " + invitee + ":" + reason);

    }

    @Override
    public void chatCreated(Chat chat, boolean createdLocally) {

        Log.i(TAG, "chatCreated()");
        mChat = chat;
        mChat.addMessageListener(this);
        mChat.addMessageListener(new ChatStateListener() {
            @Override
            public void stateChanged(Chat chat, ChatState state) {
                mEventBus.post(new ChatStateEvent(state, false));
            }

            @Override
            public void processMessage(Chat chat, Message message) {

            }
        });


    }

    //MessageListener

    @Override
    public void processMessage(Chat chat, Message message) {
        Log.d(TAG, "indi message" + message.getBody() + " - " + chat.getParticipant());
        Log.i(TAG, "processMessage()");
        if (message.getType().equals(Message.Type.chat) || message.getType().equals(Message.Type.normal)) {
            if (message.getBody() != null) {

                ChatEvent event = new ChatEvent(ChatEvent.NEW_MESSAGE);
                event.setMessage(message.getBody());
                event.setFromId(message.getFrom());
                mEventBus.post(event);

 /*               Intent intent = new Intent(SmackService.NEW_MESSAGE);
                intent.setPackage(mApplicationContext.getPackageName());
                intent.putExtra(SmackService.BUNDLE_MESSAGE_BODY, message.getBody());
                intent.putExtra(SmackService.BUNDLE_FROM_JID, message.getFrom());
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                }
                mApplicationContext.sendBroadcast(intent);*/
                Log.i(TAG, "processMessage() BroadCast send");
            }
        }
    }

    private void sendGroupMessage(String roomName, String body){
        Log.d(TAG, "sendGroupMessage ... " + roomName);
        MultiUserChat muc = mucManager.getMultiUserChat(roomName);

        try {
            muc.sendMessage(body);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

    }

    private void createConference(String roomName){
        Log.d(TAG, "start createConference");
        MultiUserChat muc = mucManager.getMultiUserChat(roomName);
        try {
            muc.create(roomName);
            muc.addInvitationRejectionListener(new InvitationRejectionListener() {
                @Override
                public void invitationDeclined(String invitee, String reason) {
                    Log.d(TAG, "invitationDeclined");
                }
            });

            muc.sendConfigurationForm(new Form(DataForm.Type.submit));
            //inviteToChat("kevkev@ehorizon.com", roomName);
        } catch (XMPPException.XMPPErrorException e) {
            e.printStackTrace();
        } catch (SmackException e) {
            e.printStackTrace();
        }

    }

    private void joinChat(String roomName){
        Log.d(TAG, "joining ... " + roomName);
        MultiUserChat muc = mucManager.getMultiUserChat(roomName);
        try {
            muc.addMessageListener(this);
            muc.join(jid);
        } catch (SmackException.NoResponseException e) {
            e.printStackTrace();
        } catch (XMPPException.XMPPErrorException e) {
            e.printStackTrace();
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

    }

    private void inviteToChat(String id, String roomName){
        Log.d(TAG, "inviteToChat ... " + id);
        MultiUserChat muc = mucManager.getMultiUserChat(roomName);
        try {
            muc.invite(id, "Join me");
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }

    }

    //ConnectionListener

    @Override
    public void connected(XMPPConnection connection) {
        SmackService.sConnectionState = ConnectionState.CONNECTED;
        Log.i(TAG, "connected()");
    }

    @Override
    public void authenticated(XMPPConnection connection, boolean resumed) {
        SmackService.sConnectionState = ConnectionState.CONNECTED;
        Log.i(TAG, "authenticated()");
    }



    @Override
    public void connectionClosed() {
        SmackService.sConnectionState = ConnectionState.DISCONNECTED;
        Log.i(TAG, "connectionClosed()");
    }

    @Override
    public void connectionClosedOnError(Exception e) {
        SmackService.sConnectionState = ConnectionState.DISCONNECTED;
        Log.i(TAG, "connectionClosedOnError()");
    }

    @Override
    public void reconnectingIn(int seconds) {
        SmackService.sConnectionState = ConnectionState.RECONNECTING;
        Log.i(TAG, "reconnectingIn()");
    }

    @Override
    public void reconnectionSuccessful() {
        SmackService.sConnectionState = ConnectionState.CONNECTED;
        Log.i(TAG, "reconnectionSuccessful()");
    }

    @Override
    public void reconnectionFailed(Exception e) {
        SmackService.sConnectionState = ConnectionState.DISCONNECTED;
        Log.i(TAG, "reconnectionFailed()");
    }

    //RosterListener

    @Override
    public void entriesAdded(Collection<String> addresses) {
        Log.i(TAG, "entriesAdded()");
        rebuildRoster();
    }

    @Override
    public void entriesUpdated(Collection<String> addresses) {
        Log.i(TAG, "entriesUpdated()");
        rebuildRoster();
    }

    @Override
    public void entriesDeleted(Collection<String> addresses) {
        Log.i(TAG, "entriesDeleted()");
        rebuildRoster();
    }

    @Override
    public void presenceChanged(Presence presence) {
        Log.i(TAG, "presenceChanged()");
        rebuildRoster();
    }

    //PingFailedListener

    @Override
    public void pingFailed() {
        Log.i(TAG, "pingFailed()");
    }
}
