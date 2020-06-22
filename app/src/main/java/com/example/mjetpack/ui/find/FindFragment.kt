package com.example.mjetpack.ui.find

import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.lib_annotation.FragmentDestination
import com.example.mjetpack.model.SofaTab
import com.example.mjetpack.ui.home.SofaFragment
import com.example.mjetpack.utils.AppConfig

@FragmentDestination(pageUrl = "main/tabs/find")
class FindFragment : SofaFragment() {
    override fun getTabFragment(position: Int): Fragment {
        val tab: SofaTab.Tabs = getTabConfig()!!.tabs[position]
        return TagListFragment.newInstance(tab.tag)
    }

    override fun onAttachFragment(childFragment: Fragment) {
        super.onAttachFragment(childFragment)

        val string = childFragment.arguments?.getString(TagListFragment.KEY_TAG_TYPE)
        if(string == "onlyFollow"){
            ViewModelProviders.of(childFragment).get(TagListViewModel::class.java)
                .switchTabLiveData.observe(this,object :Observer<Any>{
                override fun onChanged(t: Any?) {
                        viewPager2?.setCurrentItem(1)
                }

            })
        }
    }
    override fun getTabConfig(): SofaTab? {
        return AppConfig.getFindTabConfig()
    }

}