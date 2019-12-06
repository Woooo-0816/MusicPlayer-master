package com.huwei.sweetmusicplayer.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.huwei.sweetmusicplayer.R;
import com.huwei.sweetmusicplayer.business.mv.PlayMvActivity;
import com.huwei.sweetmusicplayer.data.models.baidumusic.po.Song;

import java.util.List;

/**
 * 在线音乐列表的适配器
 *
 * @author jerry
 * @date 2015-11-20
 */
public class SongAdapter extends BaseAdapter {

    private Context mContext;
    private List<Song> songList;

    public SongAdapter(Context context, List<Song> songList) {
        this.mContext = context;
        this.songList = songList;
    }

    @Override
    public int getCount() {
        return songList.size();
    }

    @Override
    public Object getItem(int position) {
        return songList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.listitem_online_music, null);
            viewHolder = new ViewHolder();
            viewHolder.tv_song = (TextView) convertView.findViewById(R.id.tv_song);
            viewHolder.tv_artist = (TextView) convertView.findViewById(R.id.tv_artist);
            viewHolder.ivMv = (ImageView) convertView.findViewById(R.id.ivMv);
            convertView.setTag(viewHolder);
        }

        final Song song = (Song) getItem(position);

        viewHolder = (ViewHolder) convertView.getTag();
        viewHolder.tv_song.setText(song.title);
        viewHolder.tv_artist.setText(song.author);
        viewHolder.ivMv.setVisibility(song.hasMobileMv() || song.hasMv() ? View.VISIBLE : View.INVISIBLE);

        viewHolder.ivMv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mContext.startActivity(PlayMvActivity.Companion.getStartActIntent(mContext, song.song_id));
            }
        });

        return convertView;
    }

    class ViewHolder {
        TextView tv_song;
        TextView tv_artist;
        ImageView ivMv;
    }
}
