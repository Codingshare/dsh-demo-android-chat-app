package deepstreamhub.demo_chat_app;

import com.google.gson.Gson;

import io.deepstream.List;

/**
 * Created by alexharley on 16/02/17.
 */

public class StateRegistry {

    private static StateRegistry instance = new StateRegistry();

    public static StateRegistry getInstance() {
        return instance;
    }

    private String userId;
    private String email;
    private Gson gson;
    private List currentChatList;
    private String currentChatName;

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

    public List getCurrentChatList() {
        return currentChatList;
    }

    public void setCurrentChatList(List currentChatList) {
        this.currentChatList = currentChatList;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCurrentChatName() {
        return currentChatName;
    }

    public void setCurrentChatName(String currentChatName) {
        this.currentChatName = currentChatName;
    }
}