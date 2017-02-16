package deepstreamhub.demo_chat_app;

import java.util.UUID;

/**
 * Created by alexharley on 16/02/17.
 */

public class User {

    private String id;
    private String email;

    public User(String id, String email) {
        this.id = id;
        this.email = email;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }
}
