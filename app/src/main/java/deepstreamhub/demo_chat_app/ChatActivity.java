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
import io.deepstream.SnapshotResult;


public class ChatActivity extends AppCompatActivity {

    private DeepstreamFactory factory;
    private DeepstreamClient client;
    private Context ctx;
    private StateRegistry stateRegistry;
    private Button postButton;
    private EditText textField;
    private TextView isTypingField;
    private ArrayList<Message> messages;
    private ChatAdapter adapter;
    private ListView listView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Bundle extras = getIntent().getExtras();
        final String userId = extras.getString("userId");

        ctx = this;
        factory = DeepstreamFactory.getInstance();
        stateRegistry = StateRegistry.getInstance();

        postButton = (Button) findViewById(R.id.post);
        textField = (EditText) findViewById(R.id.input_message);
        isTypingField = (TextView) findViewById(R.id.is_typing_field);
        textField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                Record stateRecord = client.record.getRecord(stateRegistry.getCurrentChatName() + "/state");
                if (s.toString().length() > 0) {
                    stateRecord.set(stateRegistry.getUserId() + ".isTyping", true);
                } else {
                    stateRecord.set(stateRegistry.getUserId() + ".isTyping", false);
                }
            }
        });


        try {
            client = factory.getClient(ctx.getString(R.string.dsh_login_url)); // todo replace this with getClient()
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        String[] tempChatArray = new String[]{ stateRegistry.getUserId(), userId };
        Arrays.sort(tempChatArray);
        String chatName = Arrays.toString(tempChatArray);
        List chatList = client.record.getList(chatName);

        if (chatList.isEmpty()) {
            initialiseChat(chatName, userId);
        }

        stateRegistry.setCurrentChatName(chatName);
        stateRegistry.setCurrentChatList(chatList);

        String[] entries = chatList.getEntries();
        messages = new ArrayList<>();

        for (String msgId : entries) {
            addMessage(msgId);
        }
        adapter = new ChatAdapter(this, messages);

        listView = (ListView) findViewById(R.id.chat_list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Message currentMsg = messages.get(position);
                final Record msgRecord = client.record.getRecord(stateRegistry.getCurrentChatName() + "/" + currentMsg.getMsgId());

                // don't want to allow editing other peoples messages
                if (!currentMsg.getWriterId().equals(stateRegistry.getUserId())) {
                    return;
                }

                final EditText editText = new EditText(getApplicationContext());
                editText.setText(currentMsg.getContent());
                new AlertDialog.Builder(ctx)
                        .setView(editText)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                String newContent = editText.getText().toString();
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
            public void onEntryAdded(String listName, final String msgId, final int position) {
                Log.w("dsh", "entry added:" + msgId);
                addMessage(msgId);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
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
        stateRecord.setMergeStrategy(MergeStrategy.REMOTE_WINS);
        stateRecord.subscribe(userId + ".isTyping", new RecordPathChangedCallback() {
            @Override
            public void onRecordPathChanged(String recordName, String path, final JsonElement data) {
                Log.w("dsh", "onRecordPathChanged:" + data.toString());
                SnapshotResult result = client.record.snapshot("users/" + userId);
                final String email = result.getData().getAsJsonObject().get("email").getAsString();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        boolean isTyping = data.getAsBoolean();
                        if (isTyping) {
                            isTypingField.setText(email + " is typing...");
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

    private void addMessage(String msgId) {
        Record msgRecord = client.record.getRecord(msgId);
        msgRecord.subscribe("content", new ChatItemUpdate(messages.size() - 1, messages, adapter));
        JsonObject msgJson = msgRecord.get().getAsJsonObject();
        Message m = new Message(
                msgJson.get("email").getAsString(),
                msgJson.get("content").getAsString(),
                msgJson.get("id").getAsString(),
                msgJson.get("msgId").getAsString()
        );
        messages.add(m);
    }

    private void initialiseChat(String chatName, String theirId) {
        // state record
        Record stateRecord = client.record.getRecord(chatName + "/state");
        JsonObject userMetaData = new JsonObject();
        userMetaData.addProperty("isTyping", false);
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
        public void onRecordPathChanged(String recordName, String path, JsonElement data) {
            Message msgToEdit = messages.get(position);
            msgToEdit.setContent(data.getAsString());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.notifyDataSetChanged();
                }
            });
        }
    }
}
