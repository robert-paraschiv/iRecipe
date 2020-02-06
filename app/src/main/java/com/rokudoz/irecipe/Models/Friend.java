package com.rokudoz.irecipe.Models;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Friend {
    private String friend_user_id;
    private String friend_user_name;
    private String friend_user_profilePic;
    private String friend_status;
    private @ServerTimestamp
    Date friendTimeStamp;

    public Friend(String friend_user_id, String friend_user_name, String friend_user_profilePic, String friend_status, Date friendTimeStamp) {
        this.friend_user_id = friend_user_id;
        this.friend_user_name = friend_user_name;
        this.friend_user_profilePic = friend_user_profilePic;
        this.friend_status = friend_status;
        this.friendTimeStamp = friendTimeStamp;
    }

    public Friend() {
    }

    public String getFriend_user_id() {
        return friend_user_id;
    }

    public void setFriend_user_id(String friend_user_id) {
        this.friend_user_id = friend_user_id;
    }

    public String getFriend_status() {
        return friend_status;
    }

    public void setFriend_status(String friend_status) {
        this.friend_status = friend_status;
    }

    public Date getFriendTimeStamp() {
        return friendTimeStamp;
    }

    public void setFriendTimeStamp(Date friendTimeStamp) {
        this.friendTimeStamp = friendTimeStamp;
    }

    public String getFriend_user_name() {
        return friend_user_name;
    }

    public void setFriend_user_name(String friend_user_name) {
        this.friend_user_name = friend_user_name;
    }

    public String getFriend_user_profilePic() {
        return friend_user_profilePic;
    }

    public void setFriend_user_profilePic(String friend_user_profilePic) {
        this.friend_user_profilePic = friend_user_profilePic;
    }

    @Override
    public String toString() {
        return "Friend{" +
                "friend_user_id='" + friend_user_id + '\'' +
                ", friend_user_name='" + friend_user_name + '\'' +
                ", friend_user_profilePic='" + friend_user_profilePic + '\'' +
                ", friend_status='" + friend_status + '\'' +
                ", friendTimeStamp=" + friendTimeStamp +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Friend))
            return false;
        if (obj == this)
            return true;
        return this.friend_user_id.equals(((Friend) obj).friend_user_id);
    }
}
