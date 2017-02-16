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
import java.util.Arrays;

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

        final ArrayList<String> userEmails = new ArrayList();

        for (int i = 0; i < userIds.getEntries().length; i++) {
            Record userRecord = client.record.getRecord("users/" + userIds.getEntries()[i]);
            String email = userRecord.get("email").getAsString();
            userEmails.add(email);
        }

        userEmails.remove(stateRegistry.getUserId()); // don't want users to see themselves in list

        Log.w("dsh", "users in list " + userEmails.toString());
        final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, userEmails);


        ListView listView = (ListView) findViewById(R.id.user_list);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(ctx, ChatActivity.class);
                String userId = userIds.getEntries()[position];
                i.putExtra("userId", userId);
                startActivity(i);
            }
        });

        userIds.subscribe(new ListEntryChangedListener() {
            @Override
            public void onEntryAdded(String listName, String userId, int position) {
                Record userRecord = client.record.getRecord("users/" + userId);
                String email = userRecord.get("email").getAsString();
                adapter.add(email);
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
