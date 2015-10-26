package com.ehorizon.moveslikejabber;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.ehorizon.moveslikejabber.events.ChatEvent;
import com.ehorizon.moveslikejabber.events.ChatStateEvent;

import org.jivesoftware.smackx.chatstates.ChatState;
import org.jivesoftware.smackx.receipts.DeliveryReceipt;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;
import service.SmackConnection;
import service.SmackService;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{


    private ListView mListView;
    private Button mButtonSend;
    private Button ok;
    private EditText recipient;
    private EditText mEditTextMessage;
    private TextView recipientName, state;
    private ImageView mImageView;
    private ImageView ivPresence;
    private EventBus mEventBus;
    private Dialog recipientDialog;

    private BroadcastReceiver mReceiver;
    private ChatMessageAdapter mAdapter;
    private static MainActivity instance;
    private String toId;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==R.id.action_create){
            recipientDialog = new Dialog(this, R.style.AppTheme);
            recipientDialog.setContentView(R.layout.recipient_list);
            recipient = (EditText) recipientDialog.findViewById(R.id.name);
            ivPresence = (ImageView) findViewById(R.id.presence);
            ok = (Button) recipientDialog.findViewById(R.id.ok );
            ok.setOnClickListener(this);
            recipientDialog.show();
        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mEventBus.post(new ChatStateEvent(ChatState.inactive, true));

    }

    @Override
    protected void onResume() {
        super.onResume();
        mEventBus.post(new ChatStateEvent(ChatState.active, true));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mEventBus.post(new ChatStateEvent(ChatState.gone, true));
        mEventBus.unregister(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        instance = this;
        mListView = (ListView) findViewById(R.id.listView);
        mButtonSend = (Button) findViewById(R.id.btn_send);
        mEditTextMessage = (EditText) findViewById(R.id.et_message);
        mEventBus = EventBus.getDefault();
        if (!mEventBus.isRegistered(this))
            mEventBus.register(this);
        mEditTextMessage.addTextChangedListener(new TextWatcher() {
                @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            mEventBus.post(new ChatStateEvent(ChatState.composing,true));
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });


        mImageView = (ImageView) findViewById(R.id.iv_image);
        recipientName = (TextView) findViewById(R.id.recipient);
        state = (TextView) findViewById(R.id.state);

        mAdapter = new ChatMessageAdapter(this, new ArrayList<ChatMessage>());
        mListView.setAdapter(mAdapter);


        mButtonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = mEditTextMessage.getText().toString();
                if (TextUtils.isEmpty(message)) {
                    return;
                }
                sendMessage(message);
                mEditTextMessage.setText("");
                mEventBus.post(new ChatStateEvent(ChatState.active, true));
            }
        });

        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //sendMessage();
            }
        });


    }
    public void onEventMainThread(ChatStateEvent e){
        if(!e.isIsMine()) {
            Log.d("State","Receiving state "+e.getChatState().toString());
            state.setText(" is "+ e.getChatState().toString());

        }



    }
    private void sendMessage(String message) {
        ChatMessage chatMessage = new ChatMessage(message, true, false);
        mAdapter.add(chatMessage);
        Intent intent = new Intent(SmackService.SEND_MESSAGE);
        intent.setPackage(this.getPackageName());
        intent.putExtra(SmackService.BUNDLE_MESSAGE_BODY, message);
        intent.putExtra(SmackService.BUNDLE_TO, toId);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        }
        this.sendBroadcast(intent);


    //        mimicOtherMessage(message);
    }
    public void onEventMainThread(ChatEvent e) {

        switch (e.getChatState()) {
            case ChatEvent.NEW_MESSAGE:
                Log.d("Message", "New Message");
                String from = e.getFromId();
                String msg = e.getMessage();
//                        Log.d("ChatEvent", "id : " + toId + ":" + from);
//                        if(from.equals(toId)) {
                ChatMessage message = new ChatMessage(msg, false, false);
                mAdapter.add(message);
                mListView.setSelection(mListView.getCount());
//                        }
                break;
            case ChatEvent.UPDATE_PRESENCE:
                updatePresence();
                break;
        }


    }
    private void mimicOtherMessage(String message) {
        ChatMessage chatMessage = new ChatMessage(message, false, false);
        mAdapter.add(chatMessage);
    }

    private void sendMessage() {
        ChatMessage chatMessage = new ChatMessage(null, true, true);
        mAdapter.add(chatMessage);

        mimicOtherMessage();
    }

    private void mimicOtherMessage() {
        ChatMessage chatMessage = new ChatMessage(null, false, true);
        mAdapter.add(chatMessage);
    }


    @Override
    public void onClick(View v) {
        if(v == ok){
           toId = recipient.getText().toString();
            recipientName.setText(toId);
            state.setText("Idle");
            recipientDialog.dismiss();
            mEventBus.post(new ChatEvent(ChatEvent.CREATE_CHAT, toId));
            mEventBus.post(new ChatEvent(ChatEvent.CREATE_CONFERENCE, toId));
            updatePresence();
        }
    }

    public void updatePresence(){
        Log.d("kevin", "ID : " + toId + ":" + SmackConnection.presence.get(toId));
        ivPresence.setVisibility(View.VISIBLE);
        if(SmackConnection.presence.get(toId)){
            ivPresence.setImageDrawable(this.getResources().getDrawable(R.drawable.online_state));
        }else{
            ivPresence.setImageDrawable(this.getResources().getDrawable(R.drawable.offline_state));
        }
    }

    public static MainActivity getInstance(){
        if(instance == null)
            instance = new MainActivity();
        return instance;
    }
}
