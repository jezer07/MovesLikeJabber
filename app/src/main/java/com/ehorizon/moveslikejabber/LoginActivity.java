package com.ehorizon.moveslikejabber;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import service.SmackConnection;
import service.SmackService;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener{

    Button connect;
    EditText user, pass, host, port;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        connect = (Button) findViewById(R.id.connect);
        user = (EditText) findViewById(R.id.username);
        pass = (EditText) findViewById(R.id.passsword);
        host = (EditText) findViewById(R.id.host);
        port = (EditText) findViewById(R.id.port);

        connect.setOnClickListener(this);

        if(!SmackService.getState().equals(SmackConnection.ConnectionState.DISCONNECTED)){
            connect.setText("Disconnect");
            this.startActivity(new Intent(this, MainActivity.class));
        }
    }


    @Override
    public void onClick(View v) {
        connect();
    }

    private void connect() {
        if(!verifyJabberID(user.getText().toString())){
            Toast.makeText(this, "Invalid JID", Toast.LENGTH_SHORT).show();
            return;
        }

        if(SmackService.getState().equals(SmackConnection.ConnectionState.DISCONNECTED)){

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            prefs.edit()
                    .putString("xmpp_jid", user.getText().toString())
                    .putString("xmpp_password", pass.getText().toString())
                    .putString("xmpp_port", port.getText().toString())
                    .putString("xmpp_host", host.getText().toString())
                    .commit();

            connect.setText("Disconnect");
            Intent intent = new Intent(this, SmackService.class);
            this.startService(intent);

            this.startActivity(new Intent(this, MainActivity.class));
        } else {
            connect.setText("Connect");
            Intent intent = new Intent(this, SmackService.class);
            this.stopService(intent);
        }

    }

    public static boolean verifyJabberID(String jid){
        try {
            String parts[] = jid.split("@");
            if (parts.length != 2 || parts[0].length() == 0 || parts[1].length() == 0){
                return false;
            }
        } catch (NullPointerException e) {
            return false;
        }
        return true;
    }
}
