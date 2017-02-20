package deepstreamhub.demo_chat_app;

import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import io.deepstream.DeepstreamClient;
import io.deepstream.DeepstreamFactory;
import io.deepstream.List;
import io.deepstream.ListChangedListener;
import io.deepstream.ListEntryChangedListener;
import io.deepstream.MergeStrategy;
import io.deepstream.Record;
import io.deepstream.RecordPathChangedCallback;


public class ChatActivity extends AppCompatActivity {

    private DeepstreamFactory factory;
    private DeepstreamClient client;
    private Context ctx;
    private StateRegistry stateRegistry;
    private Button postButton;
    private EditText textField;
    private TextView isTypingField;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        ctx = this;
        factory = DeepstreamFactory.getInstance();
        stateRegistry = StateRegistry.getInstance();

        postButton = (Button) findViewById(R.id.post);
        textField = (EditText) findViewById(R.id.input_message);
        isTypingField = (TextView) findViewById(R.id.is_typing_field);
        textField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                Log.w("dsh", "beforeTextChanged:" + s + " start:" + start + " count:" + count + " after:" + after);

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.w("dsh", "onTextChanged:" + s + " start:" + start + " before:" + before + " count:" + count);
            }

            @Override
            public void afterTextChanged(Editable s) {
                Log.w("dsh", "afterTextChanged");
                Log.w("dsh", s.toString());
                Record stateRecord = client.record.getRecord(stateRegistry.getCurrentChatName() + "/state");
                Log.w("dsh", "state record contents: " + stateRecord.get().toString());
                Log.w("dsh", stateRecord.get(stateRegistry.getUserId()).toString());
                stateRecord.set("random", "super cool");
                Log.w("dsh", stateRecord.get().toString());
                if (s.toString().length() > 0) {
                    Log.w("dsh", ".isTyping:true");
                    stateRecord.set(stateRegistry.getUserId() + ".isTyping", true);
                } else {
                    Log.w("dsh", ".isTyping:false");
                    stateRecord.set(stateRegistry.getUserId() + ".isTyping", false);
                }
            }
        });

        Bundle extras = getIntent().getExtras();

        final String userId = extras.getString("userId");

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
            initialiseChat(chatName, chatList, userId);
        }

        stateRegistry.setCurrentChatName(chatName);
        stateRegistry.setCurrentChatList(chatList);

        String[] entries = chatList.getEntries();
        Log.w("dsh", entries.length + " entries in chat: " + chatName);
        Log.w("dsh", Arrays.toString(entries));
        final ArrayList<Message> messages = new ArrayList<>();
        final ChatAdapter adapter = new ChatAdapter(this, messages);

        for (int i = 0; i < entries.length; i++) {
            String message = entries[i];
            Record msgRecord = client.record.getRecord(message);
            msgRecord.subscribe("content", new ChatItemUpdate(i, messages, adapter));
            JsonObject msgJson = msgRecord.get().getAsJsonObject();
            Log.w("dsh", "message: " + msgJson.toString());
            Message m = new Message(
                    msgJson.get("email").getAsString(),
                    msgJson.get("content").getAsString(),
                    msgJson.get("id").getAsString(),
                    msgJson.get("msgId").getAsString()
            );
            messages.add(m);
        }



        final ListView listView = (ListView) findViewById(R.id.chat_list);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Message currentMsg = messages.get(position);
                final Record msgRecord = client.record.getRecord(stateRegistry.getCurrentChatName() + "/" + currentMsg.getMsgId());
                Log.w("dsh", "position:" + position + " getting record " + stateRegistry.getCurrentChatName() + "/" + currentMsg.getMsgId());
                Log.w("dsh", msgRecord.get().toString());

                // don't want to allow editing other peoples messages
                // put link to permissioning tutorial here
                if (!currentMsg.getWriterId().equals(stateRegistry.getUserId())) {
                    return;
                }

                final EditText editText = new EditText(getApplicationContext());
                editText.setText(currentMsg.getContent());
                new AlertDialog.Builder(ctx)
                        //.setTitle("")
                        //.setMessage("Paste in the link of an image to moustachify!")
                        .setView(editText)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                String newContent = editText.getText().toString();
                                Log.w("dsh", "setting msg content to:" + newContent);
                                msgRecord.set("content", newContent);
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        })
                        .show();
            }
        });

        chatList.subscribe(new ListEntryChangedListener() {
            @Override
            public void onEntryAdded(String listName, final String entry, final int position) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.w("dsh", "message added " + entry);
                        Record record = client.record.getRecord(entry);
                        record.subscribe("content", new ChatItemUpdate(position, messages, adapter));
                        JsonObject msgJson = record.get().getAsJsonObject();
                        Log.w("dsh", "new message " + msgJson.toString());
                        final Message message = new Message(
                                msgJson.get("email").getAsString(),
                                msgJson.get("content").getAsString(),
                                msgJson.get("id").getAsString(),
                                msgJson.get("msgId").getAsString()
                        );
                        messages.add(message);
                        adapter.notifyDataSetChanged();
                    }
                });
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
                String msgId = UUID.randomUUID().toString();
                String msgName = stateRegistry.getCurrentChatName() + "/" + msgId;
                Record msgRecord = client.record.getRecord(msgName);
                Log.w("dsh", "writing: " + input);
                Log.w("dsh", "record name: " + msgName);
                Log.w("dsh", "adding " + msgName + " to list: " + stateRegistry.getCurrentChatName());
                Message message = new Message(
                        stateRegistry.getEmail(),
                        input,
                        stateRegistry.getUserId(),
                        msgId
                );
                msgRecord.set(stateRegistry.getGson().toJsonTree(message));
                stateRegistry.getCurrentChatList().addEntry(msgName);
                textField.setText("");
            }
        });

        final Record stateRecord = client.record.getRecord(chatName + "/state");
        stateRecord.set(stateRegistry.getUserId() + ".email", stateRegistry.getEmail());
        stateRecord.setMergeStrategy(MergeStrategy.LOCAL_WINS);
        stateRecord.subscribe(userId + ".isTyping", new RecordPathChangedCallback() {
            @Override
            public void onRecordPathChanged(String s, String s1, final JsonElement jsonElement) {
                Log.w("dsh", "onRecordPathChanged:" + jsonElement.toString());
                final String theirEmail = stateRecord.get(userId + ".email").getAsString();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        boolean isTyping = jsonElement.getAsBoolean();
                        if (isTyping) {
                            isTypingField.setText(theirEmail + " is typing...");
                        } else {
                            isTypingField.setText("");
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // todo: unsubscribe all records
    }

    private void updateItemInList(final int position, final String editedContent, final ArrayList<Message> messages, final ChatAdapter adapter) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Message msgToEdit = messages.get(position);
                msgToEdit.setContent(editedContent);
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void initialiseChat(String chatName, List chatList, String theirId) {
        // welcome message
        String uuid = UUID.randomUUID().toString();
        String welcomeRecordName = chatName + "/" + uuid;
        Record welcomeRecord = client.record.getRecord(welcomeRecordName);
        Message welcomeMessage = new Message(
                "Welcome Robot",
                "Welcome to the beginning of this chat!",
                uuid,
                uuid
        );
        JsonElement jsonMsg = stateRegistry.getGson().toJsonTree(welcomeMessage);
        Log.w("dsh", "initialising chat " + chatName + " with message " + jsonMsg.toString());
        welcomeRecord.set(jsonMsg);
        chatList.addEntry(welcomeRecordName);

        // state record
        Record stateRecord = client.record.getRecord(chatName + "/state");
        JsonObject userMetaData = new JsonObject();
        userMetaData.addProperty("isTyping", false);
        userMetaData.addProperty("email", stateRegistry.getEmail());
        stateRecord.set(stateRegistry.getUserId(), userMetaData);
        stateRecord.set(theirId, userMetaData);
    }

    private class ChatItemUpdate implements RecordPathChangedCallback {

        private int position;
        private ArrayList<Message> messages;
        private ChatAdapter adapter;

        ChatItemUpdate(int position, ArrayList<Message> messages, ChatAdapter adapter) {
            this.position = position;
            this.messages = messages;
            this.adapter = adapter;
        }

        @Override
        public void onRecordPathChanged(String s, String s1, JsonElement data) {
            Log.w("dsh", "Updating item:" + position + " to:" + data.getAsString());
            updateItemInList(position, data.getAsString(), messages, adapter);
        }
    }
}
