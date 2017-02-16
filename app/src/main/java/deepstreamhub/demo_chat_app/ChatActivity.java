package deepstreamhub.demo_chat_app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.json.JSONArray;

import java.net.URISyntaxException;
import java.util.ArrayList;

import io.deepstream.DeepstreamClient;
import io.deepstream.DeepstreamFactory;
import io.deepstream.List;
import io.deepstream.ListEntryChangedListener;
import io.deepstream.Record;
import io.deepstream.RecordPathChangedCallback;

/**
 * Created by alexharley on 16/02/17.
 */

public class ChatActivity extends AppCompatActivity {

    private DeepstreamFactory factory;
    private DeepstreamClient client;
    private Context ctx;
    private StateRegistry stateRegistry;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        ctx = getApplicationContext();
        factory = DeepstreamFactory.getInstance();
        stateRegistry = StateRegistry.getInstance();

        Bundle extras = getIntent().getExtras();

        String userId = extras.getString("userId");

        try {
            client = factory.getClient(ctx.getString(R.string.dsh_login_url)); // todo replace this with getClient()
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        String chatList = stateRegistry.getUserId() + "::" + userId;

        final List chatHistory = client.record.getList(chatList);

        String[] entries = chatHistory.getEntries();

        Log.w("dsh", entries.length + " entries in chat: " + chatList);

        Record chatRecord;
        if (entries.length == 0) {
            chatRecord = client.record.getRecord(chatList + "/" + 0);
            chatRecord.set("messages", new Message[]{});
            stateRegistry.setCurrentChatRecord(chatRecord);
        } else {
            String latestChat = entries[entries.length - 1];
            chatRecord = client.record.getRecord(latestChat);
        }
        stateRegistry.setCurrentChatRecord(chatRecord);

        JsonArray jsonMessages = chatRecord.get("messages").getAsJsonArray();
        final ArrayList<Message> messages = new ArrayList<>();

        for (JsonElement j : jsonMessages) {
            JsonObject json = j.getAsJsonObject();
            Message m = new Message(
                    json.get("writer").getAsString(),
                    json.get("contents").getAsString()
            );
            messages.add(m);
        }

        final ChatAdapter adapter = new ChatAdapter(this, messages);

        final ListView listView = (ListView) findViewById(R.id.chat_list);
        listView.setAdapter(adapter);

        chatRecord.subscribe("messages", new RecordPathChangedCallback() {
            @Override
            public void onRecordPathChanged(String recordName, String path, JsonElement data) {
                // either entry added or entry modified
                Record currentChatRecord = stateRegistry.getCurrentChatRecord();
                JsonArray jsonMessages = currentChatRecord.get("messages").getAsJsonArray();

                if (jsonMessages.size() == jsonMessages.size()) {
                    Log.w("dsh", "list entry has been edited " + path + " with data: " + data.toString());
                } else {
                    Log.w("dsh", "new messages " + data.toString());
                    Message m = new Message(
                            data.getAsJsonObject().get("writer").getAsString(),
                            data.getAsJsonObject().get("contents").getAsString()
                    );
                    messages.add(m);
                    adapter.add(m);
                }

            }
        });
    }
}
