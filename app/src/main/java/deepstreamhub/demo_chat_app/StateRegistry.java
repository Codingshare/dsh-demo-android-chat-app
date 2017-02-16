package deepstreamhub.demo_chat_app;

import com.google.gson.Gson;

import io.deepstream.Record;

/**
 * Created by alexharley on 16/02/17.
 */

public class StateRegistry {

    private static StateRegistry instance = new StateRegistry();

    public static StateRegistry getInstance() {
        return instance;
    }

    private String userId;
    private Gson gson;
    private Record currentChatRecord;

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

    public Record getCurrentChatRecord() {
        return currentChatRecord;
    }

    public void setCurrentChatRecord(Record currentChatRecord) {
        this.currentChatRecord = currentChatRecord;
    }
}