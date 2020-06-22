package com.example.mjetpack

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import com.example.lib_common.utils.StatusBar
import com.example.mjetpack.model.Destination
import com.example.mjetpack.ui.login.UserManager
import com.example.mjetpack.utils.AppConfig
import com.example.mjetpack.utils.NavgraphBuilder
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    lateinit var navController:NavController
    override fun onCreate(savedInstanceState: Bundle?) {

        //由于 启动时设置了 R.style.launcher 的windowBackground属性
//势必要在进入主页后,把窗口背景清理掉
        setTheme(R.style.AppTheme)

        //启用沉浸式布局，白底黑字
        //启用沉浸式布局，白底黑字
        StatusBar.fitSystemBar(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val hostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
//        val appBarConfiguration = AppBarConfiguration(setOf(
//            R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications))
//        setupActionBarWithNavController(navController, appBarConfiguration)
        NavigationUI.setupWithNavController(navView,navController)
//        navView.setupWithNavController(navController)

        NavgraphBuilder.build(navController,this,hostFragment?.id?:R.id.nav_host_fragment)

        navView.setOnNavigationItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        val destConfig: HashMap<String, Destination> = AppConfig.getDestConfig()!!
        val iterator: Iterator<Map.Entry<String, Destination>> =
            destConfig.entries.iterator()
        //遍历 target destination 是否需要登录拦截
        //遍历 target destination 是否需要登录拦截
        while (iterator.hasNext()) {
            val entry: Map.Entry<String, Destination> = iterator.next()
            val value: Destination = entry.value
            if (value != null && !UserManager.get().isLogin() && value.needLogin && value.id === menuItem.itemId) {
                UserManager.get().login(this)
                    .observe(this,
                        Observer<Any?> { nav_view.setSelectedItemId(menuItem.itemId) })
                return false
            }
        }
        navController.navigate(menuItem.itemId)
        return menuItem.title.isNotEmpty()
    }

    override fun onBackPressed() { //        boolean shouldIntercept = false;
//
//当前正在显示的页面destinationId
        val currentPageId = navController.currentDestination!!.id
        //APP页面路导航结构图  首页的destinationId
        val homeDestId = navController.graph.startDestination
        //如果当前正在显示的页面不是首页，而我们点击了返回键，则拦截。
        if (currentPageId != homeDestId) {
            nav_view.setSelectedItemId(homeDestId)
            return
        }
        //否则 finish，此处不宜调用onBackPressed。因为navigation会操作回退栈,切换到之前显示的页面。
        finish()
    }
}
