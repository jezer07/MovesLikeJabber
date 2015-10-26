package com.ehorizon.moveslikejabber.pojo;

/**
 * Created by phjecr on 10/26/15.
 */
public class Contact {

    private String id;
    private boolean status;

    public Contact(String id, boolean status) {
        this.id = id;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean getStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }





}
