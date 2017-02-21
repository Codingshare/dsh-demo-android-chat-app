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

import java.net.URISyntaxException;
import java.util.ArrayList;

import io.deepstream.DeepstreamClient;
import io.deepstream.DeepstreamFactory;
import io.deepstream.Event;
import io.deepstream.List;
import io.deepstream.ListChangedListener;
import io.deepstream.ListEntryChangedListener;
import io.deepstream.Record;
import io.deepstream.RecordEventsListener;


public class ChatOverviewActivity extends AppCompatActivity {

    private DeepstreamFactory factory;
    private DeepstreamClient client;
    private Context ctx;
    private StateRegistry stateRegistry;

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

        final List userIds = client.record.getList("users");

        final ArrayList<User> users = new ArrayList();

        for (int i = 0; i < userIds.getEntries().length; i++) {
            Record userRecord = client.record.getRecord("users/" + userIds.getEntries()[i]);
            String email = userRecord.get("email").getAsString();
            String id = userRecord.get("id").getAsString();
            boolean online = userRecord.get("online").getAsBoolean();
            users.add(new User(id, email, online));
        }

        // users don't want to see themselves in list
        int index = -1;
        for (int i = 0; i < users.size(); i++) {
            User u = users.get(i);
            if (u.getId().equals(stateRegistry.getUserId())) {
                index = i;
                break;
            }
        }
        users.remove(index);

        Log.w("dsh", "users in list " + users.toString());
        final UserAdapter adapter = new UserAdapter(this, users);


        ListView listView = (ListView) findViewById(R.id.user_list);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(ctx, ChatActivity.class);
                String userId = users.get(position).getId();
                i.putExtra("userId", userId);
                startActivity(i);
            }
        });

        userIds.subscribe(new ListEntryChangedListener() {
            @Override
            public void onEntryAdded(String listName, final String userId, int position) {
                Log.w("dsh", "onEntryAdded:" + userId);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Record userRecord = client.record.getRecord("users/" + userId);
                        String email = userRecord.get("email").getAsString();
                        String id = userRecord.get("id").getAsString();
                        boolean online = userRecord.get("online").getAsBoolean();
                        adapter.add(new User(id, email, online));
                    }
                });
            }

            @Override
            public void onEntryRemoved(String s, String s1, int i) {
                // todo
            }

            @Override
            public void onEntryMoved(String s, String s1, int i) {
                // not relevant
            }
        });
    }
}
