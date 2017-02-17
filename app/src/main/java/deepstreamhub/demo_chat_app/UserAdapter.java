package deepstreamhub.demo_chat_app;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class UserAdapter extends ArrayAdapter {

    public UserAdapter(Context context, ArrayList<User> users) {
        super(context, 0, users);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        User user = (User) getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_user_entry, parent, false);
        }

        TextView email = (TextView) convertView.findViewById(R.id.email);
        TextView status = (TextView) convertView.findViewById(R.id.online_status);

        email.setText(user.getEmail());
        String onlineStatus = user.isOnline() ? "online" : "offline";
        status.setText(onlineStatus);

        return convertView;
    }
}
