package com.example.mjetpack.ui.home

import android.R
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.example.lib_annotation.FragmentDestination
import com.example.mjetpack.databinding.FragmentSofaBinding
import com.example.mjetpack.model.SofaTab
import com.example.mjetpack.utils.AppConfig
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.tabs.TabLayoutMediator.TabConfigurationStrategy
import java.util.*

@FragmentDestination(pageUrl = "main/tabs/sofa", asStarter = false)
open class SofaFragment : Fragment() {


    private var binding: FragmentSofaBinding? = null
    protected var viewPager2: ViewPager2? = null
    protected var tabLayout: TabLayout? = null
    private var tabConfig: SofaTab? = null
    private var tabs: ArrayList<SofaTab.Tabs>? = null

    //private Map<Integer, Fragment> mFragmentMap = new HashMap<>();
    private var mediator: TabLayoutMediator? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSofaBinding.inflate(inflater, container, false)
        return binding?.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewPager2 = binding!!.viewPager
        tabLayout = binding!!.tabLayout
        tabConfig = getTabConfig()
        tabs = ArrayList<SofaTab.Tabs>()
        for (tab in tabConfig?.tabs!!) {
            if (tab.enable) {
                tabs!!.add(tab)
            }
        }
        //限制页面预加载
        viewPager2?.setOffscreenPageLimit(ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT)
        //viewPager2默认只有一种类型的Adapter。FragmentStateAdapter
//并且在页面切换的时候 不会调用子Fragment的setUserVisibleHint ，取而代之的是onPause(),onResume()、
        viewPager2?.setAdapter(object :
            FragmentStateAdapter(childFragmentManager, this.lifecycle) {
            override fun createFragment(position: Int): Fragment { //                Fragment fragment = mFragmentMap.get(position);
//                if (fragment == null) {
//                    fragment = getTabFragment(position);
//                    mFragmentMap.put(position, fragment);
//                }
//这里不需要自己保管了,FragmentStateAdapter内部自己会管理已实例化的fragment对象。
                return getTabFragment(position)
            }

            override fun getItemCount(): Int {
                return tabs!!.size
            }
        })
        tabLayout?.setTabGravity(tabConfig?.tabGravity!!)
        //viewPager2 就不能和再用TabLayout.setUpWithViewPager()了
//取而代之的是TabLayoutMediator。我们可以在onConfigureTab()方法的回调里面 做tab标签的配置
//其中autoRefresh的意思是:如果viewPager2 中child的数量发生了变化，也即我们调用了adapter#notifyItemChanged()前后getItemCount不同。
//要不要 重新刷野tabLayout的tab标签。视情况而定,像咱们sofaFragment的tab数量一旦固定了是不会变的，传true/false  都问题不大
        mediator =
            TabLayoutMediator(tabLayout!!, viewPager2!!, true,
                TabConfigurationStrategy { tab, position ->
                    tab.customView = makeTabView(position)
                })
        mediator!!.attach()
        viewPager2?.registerOnPageChangeCallback(mPageChangeCallback)
        //切换到默认选择项,那当然要等待初始化完成之后才有效
        viewPager2?.post(Runnable { viewPager2?.setCurrentItem(tabConfig?.select?:0, false) })
    }

    var mPageChangeCallback: OnPageChangeCallback = object : OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            val tabCount = tabLayout!!.tabCount
            for (i in 0 until tabCount) {
                val tab = tabLayout!!.getTabAt(i)
                val customView = tab!!.customView as TextView?
                if (tab.position == position) {
                    customView!!.textSize = (tabConfig?.activeSize?:16).toFloat()
                    customView.setTypeface(Typeface.DEFAULT_BOLD)
                } else {
                    customView!!.textSize = (tabConfig?.normalSize)?.toFloat()?:14f
                    customView.setTypeface(Typeface.DEFAULT)
                }
            }
        }
    }

    private fun makeTabView(position: Int): View? {
        val tabView = TextView(context)
        val states = arrayOfNulls<IntArray>(2)
        states[0] = intArrayOf(R.attr.state_selected)
        states[1] = intArrayOf()
        val colors = intArrayOf(
            Color.parseColor(tabConfig?.activeColor),
            Color.parseColor(tabConfig?.normalColor)
        )
        val stateList = ColorStateList(states, colors)
        tabView.setTextColor(stateList)
        tabView.setText(tabs!![position].title)
        tabView.textSize = tabConfig?.normalSize?.toFloat()?:14f
        return tabView
    }

    open fun getTabFragment(position: Int): Fragment {
        return HomeFragment.newInstance(tabs!![position].tag)!!
    }

    open fun getTabConfig(): SofaTab? {
        return AppConfig.getSofaTabConfig()
    }


    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        val fragments =
            childFragmentManager.fragments
        for (fragment in fragments) {
            if (fragment.isAdded && fragment.isVisible) {
                fragment.onHiddenChanged(hidden)
                break
            }
        }
    }

    override fun onDestroy() {
        mediator!!.detach()
        viewPager2!!.unregisterOnPageChangeCallback(mPageChangeCallback)
        super.onDestroy()
    }


}