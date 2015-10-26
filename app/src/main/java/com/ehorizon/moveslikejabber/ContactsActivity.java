package com.ehorizon.moveslikejabber;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.ehorizon.moveslikejabber.adapters.ContactsAdapter;
import com.ehorizon.moveslikejabber.events.ChatEvent;
import com.ehorizon.moveslikejabber.listener.RecyclerItemClickListener;
import com.ehorizon.moveslikejabber.pojo.Contact;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.roster.Roster;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;
import service.SmackConnection;

public class ContactsActivity extends AppCompatActivity {


    public static final String CONTACT_ID = "contact_id";
    public static final String IS_GROUP = "IS_GROUP";
    @Bind(R.id.contact_list)
    RecyclerView mContactList;

    ContactsAdapter mContactsAdapter;
    List<Contact> contacts;


    EventBus mEventBus;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ButterKnife.bind(this);

        mContactList.setHasFixedSize(true);
        mContactList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        contacts = new ArrayList<>();



        mContactsAdapter = new ContactsAdapter(this,contacts);
        mContactList.setAdapter(mContactsAdapter);

        mContactList.addOnItemTouchListener(
                new RecyclerItemClickListener(this, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                            Intent i = new Intent(ContactsActivity.this,MainActivity.class);
                        String id = mContactsAdapter.getContacts().get(position).getId();
                            Log.d("Contact",id);
                            i.putExtra(CONTACT_ID, id);
                             i.putExtra(IS_GROUP,false);
                            startActivity(i);
                    }
                })
        );


/*
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/
    }

    @OnClick(R.id.fab)
        void newContact(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("New Contact");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                try {
                    Roster.getInstanceFor(SmackConnection.mConnection).createEntry(input.getText().toString(),input.getText().toString(),null);
                } catch (SmackException.NotLoggedInException e) {
                    e.printStackTrace();
                } catch (SmackException.NoResponseException e) {
                    e.printStackTrace();
                } catch (XMPPException.XMPPErrorException e) {
                    e.printStackTrace();
                } catch (SmackException.NotConnectedException e) {
                    e.printStackTrace();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.contacts,menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(item.getItemId()==R.id.action_new_conference){

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("New Conference");

            final EditText input = new EditText(this);
            builder.setView(input);

            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //TODO create conference
                    mEventBus.post(new ChatEvent(ChatEvent.CREATE_CONFERENCE, input.getText().toString()));
                    Intent i = new Intent(ContactsActivity.this,MainActivity.class);
                    i.putExtra(CONTACT_ID, input.getText().toString());
                    i.putExtra(IS_GROUP,true);
                    startActivity(i);
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder.show();
        }else{

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Join Conference");

            final EditText input = new EditText(this);
            builder.setView(input);

            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //TODO Join Conference
                    mEventBus.post(new ChatEvent(ChatEvent.JOIN_CONFERENCE, input.getText().toString()));
                    Intent i = new Intent(ContactsActivity.this,MainActivity.class);
                    i.putExtra(IS_GROUP,true);
                    i.putExtra(CONTACT_ID, input.getText().toString());
                    startActivity(i);
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder.show();
        }
        return super.onOptionsItemSelected(item);
    }
}
