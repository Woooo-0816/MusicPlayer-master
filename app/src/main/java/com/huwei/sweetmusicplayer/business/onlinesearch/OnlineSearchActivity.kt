package com.huwei.sweetmusicplayer.business.onlinesearch

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle

import android.support.v4.app.Fragment
import android.text.Html
import android.text.TextUtils
import android.view.View

import com.bumptech.glide.Glide
import com.huwei.sweetmusicplayer.R
import com.huwei.sweetmusicplayer.business.BottomPlayActivity

import com.huwei.sweetmusicplayer.data.models.baidumusic.po.QueryResult
import com.huwei.sweetmusicplayer.ui.adapters.PagerAdapter
import com.huwei.sweetmusicplayer.data.contants.IntentExtra
import com.hwangjr.rxbus.RxBus
import kotlinx.android.synthetic.main.activity_online_search.*
import kotlinx.android.synthetic.main.activity_songscan.*

import java.util.Arrays

/**
 * 在线音乐搜索的结果页面
 *
 * @author Jayce
 * @date 2015/8/17
 */
open class OnlineSearchActivity : BottomPlayActivity(), OnlineSearchContract.View {

    private val pageNo = 1
    private val pageSize = 50
    private var query: String = ""

    private var mPresenter: OnlineSearchContract.Presenter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_online_search)
        initView()
        initListener()

        handleIntent(intent)

        OnlineSearchPresenter(this, this)
        if (!TextUtils.isEmpty(query)) {
            mPresenter!!.doQuery(query, pageNo, pageSize)
        }
        RxBus.get().register(mPresenter)
    }

    override fun onDestroy() {
        super.onDestroy()

        RxBus.get().unregister(mPresenter)
    }

    internal fun initView() {
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setHomeButtonEnabled(true)
    }

    internal fun initListener() {
        toolbar!!.setNavigationOnClickListener { v -> onBackClicked(v) }
    }

    internal fun handleIntent(intent: Intent) {
        if (Intent.ACTION_SEARCH == intent.action) {
            query = intent.getStringExtra(SearchManager.QUERY)
            title = query
        }
    }

    internal fun handleLinkView(queryMergeResp: QueryResult?) {
        if (queryMergeResp == null) {
            return
        }
        when (queryMergeResp.rqt_type) {
            2 -> {
                ll_link_view!!.visibility = View.VISIBLE
                if (queryMergeResp.artist_info != null &&
                        queryMergeResp.artist_info.artist_list != null &&
                        queryMergeResp.artist_info.artist_list.size > 0) {
                    val artist = queryMergeResp.artist_info.artist_list[0]
                    if (artist != null) {
                        Glide.with(mContext).load(artist.avatar_middle).into(iv_img!!)
                        tv_primary!!.text = Html.fromHtml(artist.author)
                        tv_second!!.text = String.format(mContext.resources.getString(R.string.artist_second_text), artist.song_num, artist.album_num)
                    }
                }
            }
            3 -> {
                ll_link_view!!.visibility = View.VISIBLE
                if (queryMergeResp.album_info != null &&
                        queryMergeResp.album_info.album_list != null &&
                        queryMergeResp.album_info.album_list.size > 0) {
                    val album = queryMergeResp.album_info.album_list[0]
                    if (album != null) {
                        Glide.with(mContext).load(album.pic_small).into(iv_img!!)
                        tv_primary!!.text = album.title
                        tv_second!!.text = album.author
                    }
                }
            }
            else -> ll_link_view!!.visibility = View.GONE
        }
    }

    override fun setPresenter(presenter: OnlineSearchContract.Presenter) {
        mPresenter = presenter
    }

    override fun showResultPage(queryMergeResp: QueryResult) {
        handleLinkView(queryMergeResp)

        val songFragment = SearchSongFragment()
        var bundle = Bundle()
        songFragment.arguments = bundle
        bundle.putParcelable(IntentExtra.EXTRA_SONGINFO, queryMergeResp.song_info)

        val artistFragment = SearchArtistFragment()
        bundle = Bundle()
        artistFragment.arguments = bundle
        bundle.putParcelable(IntentExtra.EXTRA_ARTISTINFO, queryMergeResp.artist_info)

        val albumFragment = SearchAlbumFragment()
        bundle = Bundle()
        albumFragment.arguments = bundle
        bundle.putParcelable(IntentExtra.EXTRA_ALBUMINFO, queryMergeResp.album_info)

        viewpager!!.adapter = object : PagerAdapter(supportFragmentManager, Arrays.asList<Fragment>(songFragment, artistFragment, albumFragment)) {
            override fun getPageTitle(position: Int): CharSequence {
                return TAB_TITLES[position]
            }
        }

        tabLayout!!.setupWithViewPager(viewpager)
    }

    fun doQuery(pageNo: Int, onGetQueryData: OnGetQueryData?) {
        //todo暂时只搜索 1-50个  后续加入下拉刷新列表
        mPresenter!!.doQuery(query, pageNo, pageSize, onGetQueryData)
    }

    interface OnGetQueryData {
        fun onGetData(queryMergeResp: QueryResult?)
    }

    companion object {
        val TAB_TITLES = arrayOf("歌曲", "歌手", "专辑")
    }
}
