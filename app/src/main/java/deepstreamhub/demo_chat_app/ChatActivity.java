package deepstreamhub.demo_chat_app;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.URISyntaxException;
import java.util.ArrayList;


public class ChatActivity extends AppCompatActivity {

    private DeepstreamFactory factory;
    private DeepstreamClient client;
    private Context ctx;
    private StateRegistry stateRegistry;
    private Button postButton;
    private EditText textField;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        ctx = getApplicationContext();
        factory = DeepstreamFactory.getInstance();
        stateRegistry = StateRegistry.getInstance();

        postButton = (Button) findViewById(R.id.post);
        textField = (EditText) findViewById(R.id.input_message);

        Bundle extras = getIntent().getExtras();

        String userId = extras.getString("userId");

        Log.w("dsh", "my id: " + stateRegistry.getUserId());
        Log.w("dsh", "chatting to: " + userId);

        try {
            client = factory.getClient(ctx.getString(R.string.dsh_login_url)); // todo replace this with getClient()
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        String myId = stateRegistry.getUserId();

        // list is either myId::userId or userId::myId
        String chatName = myId + "::" + userId;
        List chatList = client.record.getList(chatName);

        if (chatList.isEmpty()) {
            chatName = userId + "::" + myId;
        }

        chatList = client.record.getList(chatName);

        if (chatList.isEmpty()) {
            initialiseChat(chatName, chatList);
        }

        stateRegistry.setCurrentChatName(chatName);
        stateRegistry.setCurrentChatList(chatList);

        String[] entries = chatList.getEntries();
        Log.w("dsh", entries.length + " entries in chat: " + chatName);
        Log.w("dsh", Arrays.toString(entries));
        final ArrayList<Message> messages = new ArrayList<>();

        for (String message : entries) {
            Record msgRecord = client.record.getRecord(message);
            JsonObject msgJson = msgRecord.get().getAsJsonObject();
            Log.w("dsh", "message: " + msgJson.toString());
            Message m = new Message(
                    msgJson.get("email").getAsString(),
                    msgJson.get("content").getAsString(),
                    msgJson.get("id").getAsString()
            );
            messages.add(m);
        }

        final ChatAdapter adapter = new ChatAdapter(this, messages);

        final ListView listView = (ListView) findViewById(R.id.chat_list);
        listView.setAdapter(adapter);

        chatList.subscribe(new ListEntryChangedListener() {
            @Override
            public void onEntryAdded(String listName, String entry, int position) {
                Log.w("dsh", "message added " + entry);
                Record record = client.record.getRecord(listName + "/" + entry);
                JsonObject msgJson = record.get().getAsJsonObject();
                Message message = new Message(
                        msgJson.get("email").getAsString(),
                        msgJson.get("content").getAsString(),
                        msgJson.get("id").getAsString()
                );
                Log.w("dsh", "new message " + message.toString());
                messages.add(message);
                adapter.add(message);
            }

            @Override
            public void onEntryRemoved(String listName, String entry, int position) {
                // we don't support entries being removed yet
            }

            @Override
            public void onEntryMoved(String listName, String entry, int position) {
                // not concerned about this in a chat application
            }
        });

        postButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String input = textField.getText().toString();
                if (input.isEmpty()) {
                    return;
                }

                String msgName = stateRegistry.getCurrentChatName() + "/" + UUID.randomUUID();
                Record msgRecord = client.record.getRecord(msgName);
                Log.w("dsh", "writing: " + input);
                Log.w("dsh", "record name: " + msgName);
                Log.w("dsh", "adding " + msgName + " to list: " + stateRegistry.getCurrentChatName());
                Message message = new Message(
                        stateRegistry.getEmail(),
                        input,
                        stateRegistry.getUserId()
                );
                msgRecord.set(stateRegistry.getGson().toJsonTree(message));
                stateRegistry.getCurrentChatList().addEntry(msgName);
                textField.setText("");
            }
        });
    }

    private void initialiseChat(String chatName, List chatList) {
        String uuid = UUID.randomUUID().toString();
        String welcomeRecordName = chatName + "/" + uuid;
        Record welcomeRecord = client.record.getRecord(welcomeRecordName);
        Message welcomeMessage = new Message(
                "Welcome Robot",
                "Welcome to the beginning of this chat!",
                uuid
        );
        JsonElement jsonMsg = stateRegistry.getGson().toJsonTree(welcomeMessage);
        Log.w("dsh", "initialising chat " + chatName + " with message " + jsonMsg.toString());
        welcomeRecord.set(jsonMsg);
        chatList.addEntry(welcomeRecordName);
    }
}
