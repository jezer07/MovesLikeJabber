package service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import java.io.IOException;

public class SmackService extends Service {


    public static final String NEW_MESSAGE = "de.meisterfuu.smackdemo.newmessage";
    public static final String TYPING = "de.meisterfuu.smackdemo.typing";
    public static final String SEND_MESSAGE = "de.meisterfuu.smackdemo.sendmessage";
    public static final String SEND_TYPING = "de.meisterfuu.smackdemo.sendtyping";
    public static final String NEW_ROSTER = "de.meisterfuu.smackdemo.newroster";

    public static final String BUNDLE_FROM_JID = "b_from";
    public static final String BUNDLE_MESSAGE_BODY = "b_body";
    public static final String BUNDLE_ROSTER = "b_body";
    public static final String BUNDLE_TO = "b_to";

    public static SmackConnection.ConnectionState sConnectionState;

    public static SmackConnection.ConnectionState getState() {
        if(sConnectionState == null){
            return SmackConnection.ConnectionState.DISCONNECTED;
        }
        return sConnectionState;
    }

    private boolean mActive;
    private Thread mThread;
    private Handler mTHandler;



    private SmackConnection mConnection;

    public SmackService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        start();
        return Service.START_STICKY;
    }
    public SmackConnection getConnection() {
        return mConnection;
    }

    public void setConnection(SmackConnection mConnection) {
        this.mConnection = mConnection;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        stop();
    }

    public void start() {
        if (!mActive) {
            mActive = true;

            // Create ConnectionThread Loop
            if (mThread == null || !mThread.isAlive()) {
                mThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Looper.prepare();
                        mTHandler = new Handler();
                        initConnection();
                        Looper.loop();
                    }

                });
                mThread.start();
            }

        }
    }

    public void stop() {
        mActive = false;
        mTHandler.post(new Runnable() {

            @Override
            public void run() {
                if(mConnection != null){
                    mConnection.disconnect();
                }
            }
        });

    }

    private void initConnection() {
        if(mConnection == null){
            mConnection = SmackConnection.getInstance(this);
        }
        try {
            mConnection.connect();
        } catch (IOException | SmackException | XMPPException e) {
            e.printStackTrace();
        }
    }
}
