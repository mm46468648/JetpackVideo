package com.example.mjetpack.ui.home

import androidx.lifecycle.Observer
import androidx.paging.ItemKeyedDataSource
import androidx.paging.PagedList
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lib_annotation.FragmentDestination
import com.example.mjetpack.model.Feed
import com.example.mjetpack.ui.AbsListFragment
import com.example.mjetpack.ui.MutablePageKeyedDataSource
import com.scwang.smartrefresh.layout.api.RefreshLayout


@FragmentDestination(pageUrl = "main/tabs/home",asStarter = true)
class HomeFragment : AbsListFragment<Feed,HomeViewModel>() {

    override fun afterCreateView() {

        mViewModel.getCacheLiveData().observe(this,object : Observer<PagedList<Feed>> {
            override fun onChanged(result: PagedList<Feed>) {
                //只有当新数据集合大于0 的时候，才调用adapter.submitList
//否则可能会出现 页面----有数据----->被清空-----空布局
                submitList(result)
            }
        })
    }
    override fun getAdapter(): PagedListAdapter<Feed, RecyclerView.ViewHolder> {
        val feedType = if (arguments == null) "all" else arguments!!.getString("feedType")
        return FeedAdapter(this.requireContext(),feedType!!) as PagedListAdapter<Feed, RecyclerView.ViewHolder>
    }

    override fun onLoadMore(refreshLayout: RefreshLayout) {
        val currentList = adapter.currentList

        if (currentList == null || currentList.size <= 0) {
            finishRefresh(false)
            return
        }
        val feed: Feed? = currentList?.get(adapter.itemCount - 1)
        mViewModel.loadAfter(feed?.id?:0, object : ItemKeyedDataSource.LoadCallback<Feed>() {
            override fun onResult(data: List<Feed>) {
                val config: PagedList.Config = currentList?.getConfig()!!
                if (data != null && data.size > 0) { //这里 咱们手动接管 分页数据加载的时候 使用MutableItemKeyedDataSource也是可以的。
//由于当且仅当 paging不再帮我们分页的时候，我们才会接管。所以 就不需要ViewModel中创建的DataSource继续工作了，所以使用
//MutablePageKeyedDataSource也是可以的
                    val dataSource = MutablePageKeyedDataSource<Feed>()
                    //这里要把列表上已经显示的先添加到dataSource.data中
//而后把本次分页回来的数据再添加到dataSource.data中
                    dataSource.data.addAll(currentList)
                    dataSource.data.addAll(data)
                    val pagedList: PagedList<Feed> = dataSource.buildNewPagedList(config)
                    submitList(pagedList)
                }
            }
        })
    }

    override fun onRefresh(refreshLayout: RefreshLayout) {
        //invalidate 之后Paging会重新创建一个DataSource 重新调用它的loadInitial方法加载初始化数据
        //详情见：LivePagedListBuilder#compute方法
        //invalidate 之后Paging会重新创建一个DataSource 重新调用它的loadInitial方法加载初始化数据
//详情见：LivePagedListBuilder#compute方法
        mViewModel.dataSource.invalidate()
    }


}