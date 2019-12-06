package com.huwei.sweetmusicplayer.ui.adapters;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.huwei.sweetmusicplayer.R;
import com.huwei.sweetmusicplayer.data.models.baidumusic.po.Album;
import com.huwei.sweetmusicplayer.frameworks.image.GlideApp;

import java.util.List;

/**
 *  专辑列表适配器
 * @author jerry
 * @date 2015/12/30
 */
public class AlbumAdapter extends BaseAdapter {

    private Context mContext;
    private List<Album> albums;

    public AlbumAdapter(Context context, List<Album> albums) {
        this.mContext = context;
        this.albums = albums;
    }

    @Override
    public int getCount() {
        return albums.size();
    }

    @Override
    public Object getItem(int position) {
        return albums.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.listitem_online_album, null);
            viewHolder = new ViewHolder();
            viewHolder.iv_album = (ImageView) convertView.findViewById(R.id.iv_album);
            viewHolder.tv_album = (TextView) convertView.findViewById(R.id.tv_album);
            convertView.setTag(viewHolder);
        }

        final Album album = (Album) getItem(position);
        viewHolder = (ViewHolder) convertView.getTag();

        viewHolder.tv_album.setText(Html.fromHtml(mContext.getString(R.string.tab_albums) + ":" + album.title));
        GlideApp.with(mContext).load(album.getPic()).placeholder(R.drawable.album_default).into(viewHolder.iv_album);
        return convertView;
    }

    class ViewHolder {
        ImageView iv_album;
        TextView tv_album;
    }
}
