package deepstreamhub.demo_chat_app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.gson.JsonElement;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;

import io.deepstream.DeepstreamClient;
import io.deepstream.DeepstreamFactory;
import io.deepstream.Event;
import io.deepstream.List;
import io.deepstream.ListChangedListener;
import io.deepstream.ListEntryChangedListener;
import io.deepstream.PresenceEventListener;
import io.deepstream.Record;
import io.deepstream.RecordEventsListener;
import io.deepstream.RecordPathChangedCallback;


public class ChatOverviewActivity extends AppCompatActivity {

    private DeepstreamFactory factory;
    private DeepstreamClient client;
    private Context ctx;
    private StateRegistry stateRegistry;
    private UserAdapter adapter;
    private LinkedHashMap<String, User> users;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_overview);

        ctx = getApplicationContext();
        factory = DeepstreamFactory.getInstance();
        stateRegistry = StateRegistry.getInstance();

        try {
            client = factory.getClient(ctx.getString(R.string.dsh_login_url)); // todo replace this with getClient()
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        final List userList = client.record.getList("users");
        final String[] userIds = userList.getEntries();
        users = new LinkedHashMap<>();
        for (String userId : userIds) {
            if (!userId.equals(stateRegistry.getUserId())) {
                addUser(userId);
            }
        }
        Log.w("dsh", "users in list " + users.toString());
        adapter = new UserAdapter(this, users);
        ListView listView = (ListView) findViewById(R.id.user_list);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(ctx, ChatActivity.class);
                java.util.List<String> idList = new ArrayList<String>(users.keySet());
                String userId = idList.get(position);
                i.putExtra("userId", userId);
                startActivity(i);
            }
        });

        userList.subscribe(new ListEntryChangedListener() {
            @Override
            public void onEntryAdded(String listName, final String userId, final int position) {
                Log.w("dsh", "onEntryAdded:" + userId);
                addUser(userId);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onEntryRemoved(String s, String s1, int i) {
                // not in scope of tutorial
            }

            @Override
            public void onEntryMoved(String s, String s1, int i) {
                // not relevant
            }
        });

        client.presence.subscribe(new PresenceEventListener() {
            @Override
            public void onClientLogin(String userId) {
                Log.w("dsh", "onClientLogin:" + userId);
                User user = users.get(userId);
                user.setOnline(true);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onClientLogout(String userId) {
                Log.w("dsh", "onClientLogout:" + userId);
                User user = users.get(userId);
                user.setOnline(false);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        });
    }

    private void addUser(String id) {
        Record userRecord = client.record.getRecord("users/" + id);
        Log.w("dsh", userRecord.get().getAsJsonObject().toString());
        String email = userRecord.get("email").getAsString();
        boolean online = userRecord.get("online").getAsBoolean();
        users.put(id, new User(
                id,
                email,
                online)
        );
        userRecord.discard();
    }
}
