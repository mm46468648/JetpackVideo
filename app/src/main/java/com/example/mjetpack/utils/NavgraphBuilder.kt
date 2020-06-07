package com.example.mjetpack.utils

import android.content.ComponentName
import androidx.fragment.app.FragmentActivity
import androidx.navigation.ActivityNavigator
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.NavGraphNavigator
import com.example.lib_common.global.AppGlobals
import com.example.mjetpack.navigator.FixFragmentNavigator

class NavgraphBuilder {

    companion object {
        fun build(controller: NavController,activity:FragmentActivity,contentId:Int) {
            val navigatorProvider = controller.navigatorProvider
//            val fragmentNavigator =
//                navigatorProvider.getNavigator<FragmentNavigator>(FragmentNavigator::class.java)
            val activityNavigator =
                navigatorProvider.getNavigator<ActivityNavigator>(ActivityNavigator::class.java)

            //将fragmentNavigator换成fixFragmentNavigator
            val fragmentNavigator = FixFragmentNavigator(activity,activity.supportFragmentManager,contentId)
            navigatorProvider.addNavigator(fragmentNavigator)
            val navGraph = NavGraph(NavGraphNavigator(navigatorProvider))
            val destConfig = AppConfig.getDestConfig()
            destConfig?.values?.forEach {
                if (it.isFragment) {
                    val createDestination = fragmentNavigator.createDestination()
                    createDestination.className = it.className
                    createDestination.id = it.id
                    createDestination.addDeepLink(it.pageUrl)

                    navGraph.addDestination(createDestination)
                } else {
                    val createDestination = activityNavigator.createDestination()
                    createDestination.id = it.id
                    createDestination.addDeepLink(it.pageUrl)
                    createDestination.setComponentName(
                        ComponentName(
                            AppGlobals.getApplication()?.packageName ?: "", it.className
                        )
                    )
                    navGraph.addDestination(createDestination)
                }
                if(it.asStarter){
                    navGraph.startDestination = it.id
                }
            }
            controller.graph = navGraph
        }
    }
}