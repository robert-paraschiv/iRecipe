package com.rokudoz.irecipe.Models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.Map;

public class UserLastSeen {
    private Boolean online;

    private Map<String, String> last_seen;

    public UserLastSeen(Boolean online, Map<String, String> last_seen) {
        this.online = online;
        this.last_seen = last_seen;
    }

    public UserLastSeen() {
    }

    public Boolean getOnline() {
        return online;
    }

    public void setOnline(Boolean online) {
        this.online = online;
    }

    public Map<String, String> getLast_seen() {
        return last_seen;
    }

    public void setLast_seen(Map<String, String> last_seen) {
        this.last_seen = last_seen;
    }
}
