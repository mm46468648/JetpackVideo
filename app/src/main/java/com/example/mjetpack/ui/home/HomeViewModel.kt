package com.example.mjetpack.ui.home

import android.util.Log
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import androidx.paging.ItemKeyedDataSource
import androidx.paging.PagedList
import com.alibaba.fastjson.TypeReference
import com.example.lib_network.ApiResponse
import com.example.lib_network.ApiService
import com.example.lib_network.JsonCallback
import com.example.lib_network.Request
import com.example.mjetpack.model.Feed
import com.example.mjetpack.ui.AbsViewModel
import com.example.mjetpack.ui.MutablePageKeyedDataSource
import com.example.mjetpack.ui.login.UserManager
import java.util.*
import java.util.Collections.emptyList
import java.util.concurrent.atomic.AtomicBoolean


class HomeViewModel : AbsViewModel<Feed>() {

    @Volatile
    private var witchCache = true
    private val cacheLiveData = MutableLiveData<PagedList<Feed>>()
    private val loadAfter = AtomicBoolean(false)

    fun getCacheLiveData(): MutableLiveData<PagedList<Feed>>{
        return cacheLiveData
    }

    inner class FeedDataSource : ItemKeyedDataSource<Int, Feed>() {
        override fun loadInitial(
            params: LoadInitialParams<Int>,
            callback: LoadInitialCallback<Feed>
        ) { //加载初始化数据的
            Log.e("homeviewmodel", "loadInitial: ")
            loadData(0, params.requestedLoadSize, callback)
            witchCache = false
        }

        override fun loadAfter(
            params: LoadParams<Int>,
            callback: LoadCallback<Feed>
        ) { //向后加载分页数据的
            Log.e("homeviewmodel", "loadAfter: ")
            loadData(params.key, params.requestedLoadSize, callback)
        }

        override fun loadBefore(
            params: LoadParams<Int>,
            callback: LoadCallback<Feed>
        ) {
            callback.onResult(kotlin.collections.emptyList())
            //能够向前加载数据的
        }

        override fun getKey(item: Feed): Int {
            return item.id
        }
    }
//    val mDataSource:DataSource<Int,Feed> = object :ItemKeyedDataSource<Int,Feed>(){
//        override fun loadInitial(
//            params: LoadInitialParams<Int>,
//            callback: LoadInitialCallback<Feed>
//        ) {
//            loadData(0,params.requestedLoadSize,callback)
//            witchCache = false
//        }
//
//        override fun loadAfter(params: LoadParams<Int>, callback: LoadCallback<Feed>) {
//            loadData(params.key,params.requestedLoadSize,callback)
//        }
//
//        override fun loadBefore(params: LoadParams<Int>, callback: LoadCallback<Feed>) {
//            callback.onResult(Collections.emptyList())
//
//        }
//
//        override fun getKey(item: Feed): Int {
//            return item.id
//        }
//
//    }

    private fun loadData(key: Int,count:Int, callback: ItemKeyedDataSource.LoadCallback<Feed>) {
//feeds/queryHotFeedsList
        //feeds/queryHotFeedsList
        if (key > 0) {
            loadAfter.set(true)
        }
        val request = ApiService.get<Any>("/feeds/queryHotFeedsList")
            .addParam("feedType", null)
            .addParam("userId", UserManager.get().userId)
            .addParam("feedId", key)
            .addParam("pageCount", 10)
            .responseType(object : TypeReference<ArrayList<Feed?>?>() {}.getType())

        if (witchCache) {
            request.cacheStrategy(Request.CACHE_ONLY)
            request.execute(object : JsonCallback<List<Feed?>?>() {
                override fun onCacheSuccess(response: ApiResponse<List<Feed?>?>) {
                    Log.e("loadData", "onCacheSuccess:${response.body?.size} ")
                    val dataSource = MutablePageKeyedDataSource<Feed>()
                    response.body?.let { dataSource.data.addAll(it) }

                    val pagedList: PagedList<Feed> = dataSource.buildNewPagedList(config)
                    cacheLiveData.postValue(pagedList)
//                    下面的不可取，否则会报
//                    java.lang.IllegalStateException: callback.onResult already called, cannot call again.
//                    if (response.body != null) {
//                        callback.onResult(response.body);
//                    }
                }
            })
        }

        try {
            val netRequest = if (witchCache) request.clone() else request
            netRequest.cacheStrategy(if (key == 0) Request.NET_CACHE else Request.NET_ONLY)
            val response: ApiResponse<List<Feed>?> = netRequest.execute() as  ApiResponse<List<Feed>?>
            val data =
                if (response.body == null) emptyList<Feed>() else response.body!!
            callback.onResult(data)
            if (key > 0) { //通过BoundaryPageData发送数据 告诉UI层 是否应该主动关闭上拉加载分页的动画
                (boundaryPageData as MutableLiveData<Boolean>).postValue(data.size > 0)
                loadAfter.set(false)
            }
        } catch (e: CloneNotSupportedException) {
            e.printStackTrace()
        }

        Log.e("loadData", "loadData: key:$key")

    }

    override fun createDataSource(): DataSource<Int,Feed> {
        return FeedDataSource()
    }

    fun loadAfter(id: Int, callback: ItemKeyedDataSource.LoadCallback<Feed>) {
        if (loadAfter.get()) {
            callback.onResult(kotlin.collections.emptyList<Feed>())
            return
        }
        ArchTaskExecutor.getIOThreadExecutor().execute { loadData(id, config.pageSize,callback) }
    }
}