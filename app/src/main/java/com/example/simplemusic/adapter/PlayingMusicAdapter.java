package com.example.simplemusic.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.simplemusic.bean.Music;
import com.example.simplemusic.R;

import java.util.List;

public class PlayingMusicAdapter extends BaseAdapter {

    private List<Music>            mData;
    private LayoutInflater         mInflater;
    private int                    mResource;
    private Context                mContext;
    private onDeleteButtonListener monDeleteButtonListener;

    public PlayingMusicAdapter(Context context, int resId, List<Music> data)
    {
        mContext = context;
        mData = data;
        mInflater = LayoutInflater.from(context);
        mResource = resId;
    }

    @Override
    public int getCount() {
        return mData != null ? mData.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return mData != null ? mData.get(position): null ;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        Music item = mData.get(position);
        View view;
        ViewHolder holder;

        if (convertView == null) {
            view = mInflater.inflate(mResource, parent, false);
            holder = new ViewHolder();
            holder.title = view.findViewById(R.id.playingmusic_title);
            holder.artist = view.findViewById(R.id.playingmusic_artist);
            holder.delete = view.findViewById(R.id.delete);
            view.setTag(holder);
        }
        else {
            view = convertView;
            holder = (ViewHolder) view.getTag();
        }
        holder.title.setText(item.title);
        holder.artist.setText(item.artist);
        holder.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                monDeleteButtonListener.onClick(position);
            }
        });
        return view;
    }

    class ViewHolder{
        TextView title;
        TextView artist;
        ImageView delete;
    }

    public interface onDeleteButtonListener {
        void onClick(int i);
    }

    public void setOnDeleteButtonListener(onDeleteButtonListener monDeleteButtonListener) {
        this.monDeleteButtonListener = monDeleteButtonListener;
    }

}
