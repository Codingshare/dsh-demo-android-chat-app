package deepstreamhub.demo_chat_app;

import java.util.UUID;

/**
 * Created by alexharley on 16/02/17.
 */

public class User {

    private String id;
    private String email;
    private boolean online;

    public User(String id, String email, boolean online) {
        this.id = id;
        this.email = email;
        this.online = online;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }
}
