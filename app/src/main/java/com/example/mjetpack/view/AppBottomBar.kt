package com.example.mjetpack.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.TextUtils
import android.util.AttributeSet
import com.example.mjetpack.R
import com.example.mjetpack.model.BottomBar
import com.example.mjetpack.utils.AppConfig
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomnavigation.LabelVisibilityMode

class AppBottomBar @SuppressLint("RestrictedApi") constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
) : BottomNavigationView(context, attrs, defStyleAttr) {
    private val config: BottomBar?

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null) : this(
        context,
        attrs,
        0
    ) {
    }

    private fun dp2Px(dpValue: Int): Int {
        val metrics = context.resources.displayMetrics
        return (metrics.density * dpValue + 0.5f).toInt()
    }

    private fun getItemId(pageUrl: String?): Int {
        val destination = AppConfig.getDestConfig()!![pageUrl] ?: return -1
        return destination.id
    }

    companion object {
        private val sIcons = intArrayOf(
            R.drawable.icon_tab_home,
            R.drawable.icon_tab_sofa,
            R.drawable.icon_tab_publish,
            R.drawable.icon_tab_find,
            R.drawable.icon_tab_mine
        )
    }

    init {
        config = AppConfig.getBottomBarConfig()
        val state = arrayOfNulls<IntArray>(2)
        state[0] = intArrayOf(android.R.attr.state_selected)
        state[1] = intArrayOf()
        val colors = intArrayOf(
            Color.parseColor(config!!.activeColor),
            Color.parseColor(config.inActiveColor)
        )
        val stateList = ColorStateList(state, colors)
        itemTextColor = stateList
        itemIconTintList = stateList
        //LABEL_VISIBILITY_LABELED:设置按钮的文本为一直显示模式
//LABEL_VISIBILITY_AUTO:当按钮个数小于三个时一直显示，或者当按钮个数大于3个且小于5个时，被选中的那个按钮文本才会显示
//LABEL_VISIBILITY_SELECTED：只有被选中的那个按钮的文本才会显示
//LABEL_VISIBILITY_UNLABELED:所有的按钮文本都不显示
        labelVisibilityMode = LabelVisibilityMode.LABEL_VISIBILITY_LABELED
        val tabs = config.tabs
        for (tab in tabs!!) {
            if (!tab.enable) {
                continue
            }
            val itemId = getItemId(tab.pageUrl)
            if (itemId < 0) {
                continue
            }
            val menuItem = menu.add(0, itemId, tab.index, tab.title)
            menuItem.setIcon(sIcons[tab.index])
        }
        //此处给按钮icon设置大小
        var index = 0
        for (tab in config.tabs!!) {
            if (!tab.enable) {
                continue
            }
            val itemId = getItemId(tab.pageUrl)
            if (itemId < 0) {
                continue
            }
            val iconSize = dp2Px(tab.size)
            val menuView = getChildAt(0) as BottomNavigationMenuView
            val itemView =
                menuView.getChildAt(index) as BottomNavigationItemView
            itemView.setIconSize(iconSize)
            if (TextUtils.isEmpty(tab.title)) {
                val tintColor =
                    if (TextUtils.isEmpty(tab.tintColor)) Color.parseColor("#ff678f") else Color.parseColor(
                        tab.tintColor
                    )
                itemView.setIconTintList(ColorStateList.valueOf(tintColor))
                //禁止掉点按时 上下浮动的效果
                itemView.setShifting(false)
                /**
                 * 如果想要禁止掉所有按钮的点击浮动效果。
                 * 那么还需要给选中和未选中的按钮配置一样大小的字号。
                 *
                 * 在MainActivity布局的AppBottomBar标签增加如下配置，
                 * @style/active，@style/inActive 在style.xml中
                 * app:itemTextAppearanceActive="@style/active"
                 * app:itemTextAppearanceInactive="@style/inActive"
                 */
            }
            index++
        }
        //底部导航栏默认选中项
        if (config.selectTab != 0) {
            val selectTab =
                config.tabs!![config.selectTab]
            if (selectTab.enable) {
                val itemId = getItemId(selectTab.pageUrl)
                //这里需要延迟一下 再定位到默认选中的tab
//因为 咱们需要等待内容区域,也就NavGraphBuilder解析数据并初始化完成，
//否则会出现 底部按钮切换过去了，但内容区域还没切换过去
                post { selectedItemId = itemId }
            }
        }
    }
}