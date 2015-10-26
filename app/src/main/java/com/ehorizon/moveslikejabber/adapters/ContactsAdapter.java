package com.ehorizon.moveslikejabber.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ehorizon.moveslikejabber.R;
import com.ehorizon.moveslikejabber.pojo.Contact;

import java.util.List;

/**
 * Created by phjecr on 10/26/15.
 */
public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ViewHolder> {


    private Context mContext;
    private List<Contact> mContacts;

    public ContactsAdapter(Context context,List<Contact> contacts) {
        this.mContext = context;
        this.mContacts = contacts;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(mContext).inflate(R.layout.row_contacts,parent,false);
        ViewHolder viewHolder = new ViewHolder(view);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.contactName.setText(mContacts.get(position).getId());
        if(mContacts.get(position).getStatus())
            holder.contactPresence.setImageDrawable(mContext.getResources().getDrawable(R.drawable.online_state));
        else
            holder.contactPresence.setImageDrawable(mContext.getResources().getDrawable(R.drawable.offline_state));
    }

    @Override
    public int getItemCount() {
        return mContacts.size();
    }

    static public class ViewHolder extends RecyclerView.ViewHolder{

        TextView contactName;
        ImageView contactPresence;

        public ViewHolder(View itemView) {
            super(itemView);
            contactName = (TextView)itemView.findViewById(R.id.contact_id);
            contactPresence = (ImageView)itemView.findViewById(R.id.presence);



        }
    }
}
