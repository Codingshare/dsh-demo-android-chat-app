package deepstreamhub.demo_chat_app;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;


public class ChatAdapter extends ArrayAdapter {
    public ChatAdapter(Context context, ArrayList<Message> messages) {
        super(context, 0, messages);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        Message message = (Message) getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_chat_entry, null);
        }

        TextView contents = (TextView) convertView.findViewById(R.id.contents);
        TextView writer = (TextView) convertView.findViewById(R.id.writerEmail);
        contents.setText(message.getContent());
        writer.setText(message.getWriterEmail());

        return convertView;
    }

}