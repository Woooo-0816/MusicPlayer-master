package com.huwei.sweetmusicplayer.business.interfaces;

/**
 * 在线搜索,搜索建议 抽象的接口
 * @author jayce
 * @date 2015/08/18
 */
public interface IQueryReuslt {
    enum QueryType {
        None,Song,Album,Artist
    }

    //抽象一个公共的方法显示名称
    String getName();

    //抽象一个公共的方法显示type
    QueryType getSearchResultType();
}
