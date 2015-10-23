package com.ehorizon.moveslikejabber;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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

import java.util.ArrayList;

import de.greenrobot.event.EventBus;
import service.SmackService;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{


    private ListView mListView;
    private Button mButtonSend;
    private Button ok;
    private EditText recipient;
    private EditText mEditTextMessage;
    private TextView recipientName, state;
    private ImageView mImageView;
    private EventBus mEventBus;
    private Dialog recipientDialog;


    private ChatMessageAdapter mAdapter;
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
            ok = (Button) recipientDialog.findViewById(R.id.ok );
            ok.setOnClickListener(this);
            recipientDialog.show();
        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mEventBus.unregister(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mListView = (ListView) findViewById(R.id.listView);
        mButtonSend = (Button) findViewById(R.id.btn_send);
        mEditTextMessage = (EditText) findViewById(R.id.et_message);
        mEventBus = EventBus.getDefault();
 /*       if (!mEventBus.isRegistered(this))
            mEventBus.register(this);*/
        mEditTextMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            mEventBus.post(new ChatStateEvent(ChatState.composing));
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
            }
        });

        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //sendMessage();
            }
        });


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
            mEventBus.post(new ChatEvent(ChatEvent.CREATE_CHAT,toId));
        }
    }
}
