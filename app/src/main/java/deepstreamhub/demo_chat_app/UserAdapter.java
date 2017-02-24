package deepstreamhub.demo_chat_app;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

public class UserAdapter extends BaseAdapter {

    private LinkedHashMap<String, User> users;
    private Context context;

    public UserAdapter(Context context, LinkedHashMap<String, User> users) {
        this.context = context;
        this.users = users;
    }

    @Override
    public int getCount() {
        return this.users.size();
    }

    @Override
    public Object getItem(int position) {
        List<String> idList = new ArrayList<String>(users.keySet());
        String userId = idList.get(position);
        User user = users.get(userId);
        return user;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        User userData = (User) getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_user_entry, parent, false);
        }

        TextView email = (TextView) convertView.findViewById(R.id.email);
        TextView status = (TextView) convertView.findViewById(R.id.online_status);

        email.setText(userData.getEmail());
        String onlineStatus = userData.isOnline() ? "online" : "offline";
        status.setText(onlineStatus);
        return convertView;
    }
}
