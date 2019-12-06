package com.huwei.sweetmusicplayer.ui.adapters;

import android.content.Context;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.huwei.sweetmusicplayer.R;
import com.huwei.sweetmusicplayer.data.models.AbstractMusic;
import com.huwei.sweetmusicplayer.data.models.baidumusic.po.Album;
import com.huwei.sweetmusicplayer.data.models.baidumusic.po.Artist;
import com.huwei.sweetmusicplayer.data.models.baidumusic.po.Song;
import com.huwei.sweetmusicplayer.business.core.MusicManager;
import com.huwei.sweetmusicplayer.business.interfaces.IQueryReuslt;

import java.util.ArrayList;
import java.util.List;

/**
 * 在线搜索结果适配器
 *
 * @author jayce
 * @date 2015/08/18
 */
public class SearchResultAdapter extends BaseAdapter {
    public static final String TAG="SearchResultAdapter";

    private Context mContext;

    private List<IQueryReuslt> data = new ArrayList<>();
    List<AbstractMusic> songs = new ArrayList<>();

    private boolean isFisrtSong;

    private IQueryReuslt.QueryType lastType = IQueryReuslt.QueryType.None;

    public SearchResultAdapter(Context mContext) {
        this.mContext = mContext;
    }

    public void add(IQueryReuslt iSearchReuslt) {
        data.add(iSearchReuslt);

        if (iSearchReuslt instanceof Song) {
            songs.add((Song) iSearchReuslt);
        }

    }

    public void addAll(List add) {
        data.addAll(add);

        Log.i(TAG,"dataSize:"+data.size());
    }


    public List<IQueryReuslt> getData() {
        return data;
    }

    public List<AbstractMusic> getSongs() {
        return songs;
    }

    public void setData(List<IQueryReuslt> data) {
        this.data = data;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    //考虑到Song，Album，Artist都是分块排好，不会出现交叉的现象
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;

        IQueryReuslt ISearchReuslt = (IQueryReuslt) getItem(position);
        IQueryReuslt.QueryType type = ISearchReuslt.getSearchResultType();
        switch (type) {
            case Song:
                if (type != lastType || convertView == null || convertView.getTag() == null) {
                    isFisrtSong = false;

                    viewHolder = new ViewHolder();
                    convertView = LayoutInflater.from(mContext).inflate(R.layout.listitem_searchresult_song, null);
                    viewHolder.tv_songname = (TextView) convertView.findViewById(R.id.tv_songname);
                    viewHolder.tv_info = (TextView) convertView.findViewById(R.id.tv_info);
                    viewHolder.selected_view = convertView.findViewById(R.id. selected_view);

                    convertView.setTag(viewHolder);
                } else {
                    viewHolder = (ViewHolder) convertView.getTag();
                }

                Song song = (Song) ISearchReuslt;
                viewHolder.tv_songname.setText(song.title);
                viewHolder.tv_info.setText(song.author);

                if(MusicManager.isIndexNowPlaying(data, position)){
                    viewHolder.selected_view.setVisibility(View.VISIBLE);
                }else{
                    viewHolder.selected_view.setVisibility(View.GONE);
                }

                break;
            case Album:
                convertView = LayoutInflater.from(mContext).inflate(R.layout.listitem_searchresult_album, null);
                ImageView iv_album = (ImageView) convertView.findViewById(R.id.iv_album);
                TextView tv_album = (TextView) convertView.findViewById(R.id.tv_album);

                Album album = (Album) ISearchReuslt;
                Glide.with(mContext).load(album.pic_small).into(iv_album);

                tv_album.setText(Html.fromHtml(mContext.getString(R.string.tab_albums) + ":" + album.title));
                break;
            case Artist:
                convertView = LayoutInflater.from(mContext).inflate(R.layout.listitem_searchresult_artist, null);
                ImageView iv_artist = (ImageView) convertView.findViewById(R.id.iv_artist);
                TextView tv_artist = (TextView) convertView.findViewById(R.id.tv_artist);

                Artist artist = (Artist) ISearchReuslt;
                tv_artist.setText(artist.getName());

                Glide.with(mContext).load(artist.avatar_middle).into(iv_artist);

                tv_artist.setText(mContext.getString(R.string.tab_artists) + ":" + artist.author);
                break;
        }
        lastType = type;


        return convertView;
    }

    //搜出来的专辑和歌手会只有一个或者比较少不需要viewHolder
    static class ViewHolder {
        TextView tv_songname;
        TextView tv_info;   //保存专辑  歌手信息
        View selected_view; //紫色选中方块
    }
}
