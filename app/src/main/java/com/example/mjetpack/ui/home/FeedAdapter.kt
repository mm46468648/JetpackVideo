package com.example.mjetpack.ui.home

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.mjetpack.BR
import com.example.mjetpack.databinding.LayoutFeedTypeImageBinding
import com.example.mjetpack.databinding.LayoutFeedTypeVideoBinding
import com.example.mjetpack.model.Feed


class FeedAdapter(var mContext: Context, var mCategory: String) :
    PagedListAdapter<Feed, FeedAdapter.ViewHolder>(object : DiffUtil.ItemCallback<Feed>() {

        override fun areItemsTheSame(oldItem: Feed, newItem: Feed): Boolean {
            return oldItem.id === newItem.id
        }

        override fun areContentsTheSame(oldItem: Feed, newItem: Feed): Boolean {
            return oldItem.equals(newItem)
        }
    }) {
    lateinit var inflater: LayoutInflater
    init {
        inflater = LayoutInflater.from(mContext)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        var viewDataBinding: ViewDataBinding = if (viewType == Feed.TYPE_VIDEO) {
            LayoutFeedTypeVideoBinding.inflate(inflater,parent,false)
        } else {
            LayoutFeedTypeImageBinding.inflate(inflater,parent,false)
        }
        return ViewHolder(viewDataBinding.root, viewDataBinding)
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position)?.id ?: -1
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position)?.let { holder.bindData(it) }
    }

    inner class ViewHolder(
        val itemView: View,
        val viewDataBinding: ViewDataBinding
    ) : RecyclerView.ViewHolder(itemView) {

        fun bindData(feed: Feed) {
            viewDataBinding.setVariable(BR.feed,feed)
            viewDataBinding.setVariable(BR.lifeCycleOwner,mContext)
            if (viewDataBinding is LayoutFeedTypeImageBinding) {
//                viewDataBinding.feed = feed
//                viewDataBinding.lifecycleOwner = mContext as LifecycleOwner
                viewDataBinding.feedImage.bindData(feed.width, feed.height, 16, feed.cover)
            } else if (viewDataBinding is LayoutFeedTypeVideoBinding) {
//                viewDataBinding.feed = feed
//                viewDataBinding.lifecycleOwner = mContext as LifecycleOwner
                viewDataBinding.listPlayerView.bindData(mCategory,feed.width,feed.height,feed.cover,feed.url)
            }
//            viewDataBinding.setVariable(BR.lifeCycleOwner,mContext as LifecycleOwner)
        }
    }
}