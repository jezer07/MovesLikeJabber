package service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.preference.PreferenceManager;
import android.util.Log;

import org.jivesoftware.smack.ConnectionListener;
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
import org.jivesoftware.smackx.ping.PingFailedListener;
import org.jivesoftware.smackx.ping.PingManager;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import de.duenndns.ssl.MemorizingTrustManager;


/**
 * Created by Furuha on 27.12.2014.
 */
public class SmackConnection implements ConnectionListener, ChatManagerListener, RosterListener, ChatMessageListener, PingFailedListener, ChatStateListener {

    @Override
    public void stateChanged(Chat chat, ChatState state) {
        Log.d("Message","stateChange");
    }

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

    private XMPPTCPConnection mConnection;
    private ArrayList<String> mRoster;
    private BroadcastReceiver mReceiver;

    public SmackConnection(Context pContext) {
        Log.i(TAG, "ChatConnection()");

        mApplicationContext = pContext.getApplicationContext();
        mPassword = PreferenceManager.getDefaultSharedPreferences(mApplicationContext).getString("xmpp_password", "jez");
        String jid = PreferenceManager.getDefaultSharedPreferences(mApplicationContext).getString("xmpp_jid", "jez@ehorizon.com");
        mPort = PreferenceManager.getDefaultSharedPreferences(mApplicationContext).getString("xmpp_port", "5222");
        mHost = PreferenceManager.getDefaultSharedPreferences(mApplicationContext).getString("xmpp_host", "localhost");
        mServiceName = jid.split("@")[1];
        mUsername = jid.split("@")[0];



    }

    public void connect() throws IOException, XMPPException, SmackException {
        Log.i(TAG, "connect()");

        XMPPTCPConnectionConfiguration.Builder builder = XMPPTCPConnectionConfiguration.builder();
        builder.setServiceName(mServiceName);
        builder.setHost(mHost);
        builder.setPort(Integer.parseInt(mPort));
        builder.setResource("SmackAndroidTestClient");
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
        ChatManager.getInstanceFor(mConnection).createChat("leq@192.168.63.196",this);
        Roster.getInstanceFor(mConnection).addRosterListener(this);

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
        String status;
        for (RosterEntry entry : Roster.getInstanceFor(mConnection).getEntries()) {
            if(Roster.getInstanceFor(mConnection).getPresence(entry.getUser()).isAvailable()){
                status = "Online";
            } else {
                status = "Offline";
            }
            mRoster.add(entry.getUser()+ ": " + status);
        }

        Intent intent = new Intent(SmackService.NEW_ROSTER);
        intent.setPackage(mApplicationContext.getPackageName());
        intent.putStringArrayListExtra(SmackService.BUNDLE_ROSTER, mRoster);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        }
        mApplicationContext.sendBroadcast(intent);
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

    private void sendMessage(String body, String toJid) {
        Log.i(TAG, "sendMessage()");
        Chat chat = ChatManager.getInstanceFor(mConnection).createChat(toJid, this);
        try {
            chat.sendMessage(body);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
    }

    //ChatListener

    @Override
    public void chatCreated(Chat chat, boolean createdLocally) {
        Log.i(TAG, "chatCreated()");
        chat.addMessageListener(this);

    }

    //MessageListener

    @Override
    public void processMessage(Chat chat, Message message) {
        Log.d("Message",message.toString());
        Log.i(TAG, "processMessage()");
        if (message.getType().equals(Message.Type.chat) || message.getType().equals(Message.Type.normal)) {
            if (message.getBody() != null) {
                Intent intent = new Intent(SmackService.NEW_MESSAGE);
                intent.setPackage(mApplicationContext.getPackageName());
                intent.putExtra(SmackService.BUNDLE_MESSAGE_BODY, message.getBody());
                intent.putExtra(SmackService.BUNDLE_FROM_JID, message.getFrom());
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                }
                mApplicationContext.sendBroadcast(intent);
                Log.i(TAG, "processMessage() BroadCast send");
            }
        }else if(ChatState.composing.equals(message)){
            Intent intent = new Intent(SmackService.TYPING);
            intent.setPackage(mApplicationContext.getPackageName());
            intent.putExtra(SmackService.BUNDLE_MESSAGE_BODY, " is typing");
            intent.putExtra(SmackService.BUNDLE_FROM_JID, message.getFrom());
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            }
            mApplicationContext.sendBroadcast(intent);
            Log.i(TAG, "processMessage() BroadCast send");

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
