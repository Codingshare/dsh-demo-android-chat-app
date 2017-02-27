package deepstreamhub.demo_chat_app;

import com.google.gson.Gson;

import io.deepstream.List;
import io.deepstream.Record;

public class StateRegistry {

    private static StateRegistry instance = new StateRegistry();

    public static StateRegistry getInstance() {
        return instance;
    }

    private String userId;
    private String email;
    private Gson gson;

    StateRegistry() {
    }

    void setUserId(String userId) {
        this.userId = userId;
    }

    String getUserId() {
        return this.userId;
    }

    public Gson getGson() {
        return gson;
    }

    public void setGson(Gson gson) {
        this.gson = gson;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}