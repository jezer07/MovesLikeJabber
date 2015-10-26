package com.ehorizon.moveslikejabber;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import com.ehorizon.moveslikejabber.adapters.ContactsAdapter;
import com.ehorizon.moveslikejabber.pojo.Contact;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

public class ContactsActivity extends AppCompatActivity {



    @Bind(R.id.contact_list)
    RecyclerView mContactList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ButterKnife.bind(this);

        mContactList.setHasFixedSize(true);
        mContactList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        List<Contact> contacts = new ArrayList<>();
        contacts.add(new Contact("jez@ehorizon.com",true));
        contacts.add(new Contact("kev@ehorizon.com",false));


        ContactsAdapter contactsAdapter = new ContactsAdapter(this,contacts);
        mContactList.setAdapter(contactsAdapter);



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

}
