package com.example.takumi.uimock;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.BaseAdapter;
import android.content.Context;

public class ListViewAdapter extends BaseAdapter {
    static class ViewHolder {
        TextView textView;
        ImageView imageView;
    }

    private LayoutInflater inflater;
    private int layoutId;
    private Callee[] calleeList;

    ListViewAdapter(Context context, int layoutId, Callee[] calleeList) {
        super();
        this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.layoutId = layoutId;
        this.calleeList = calleeList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(layoutId, parent, false);
            holder = new ViewHolder();
            holder.imageView = convertView.findViewById(R.id.imageView);
            holder.textView = convertView.findViewById(R.id.textView);

            convertView.setTag(holder);
        } else{
            holder = (ViewHolder) convertView.getTag();
        }
        holder.imageView.setImageResource(calleeList[position].getIcon());
        holder.textView.setText(calleeList[position].getName());

        return convertView;
    }

    @Override
    public int getCount() { return calleeList.length; }

    @Override
    public Object getItem(int pos) { return null; }

    @Override
    public long getItemId(int pos) { return 0; }
}
