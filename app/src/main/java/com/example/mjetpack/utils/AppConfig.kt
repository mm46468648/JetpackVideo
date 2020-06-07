package com.example.mjetpack.utils

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.TypeReference
import com.example.lib_common.global.AppGlobals
import com.example.mjetpack.model.BottomBar
import com.example.mjetpack.model.Destination
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader


class AppConfig {
    companion object{
        private var sDestConfig: HashMap<String, Destination>? = null
        private var sBottomBar: BottomBar? = null
//        private val sSofaTab: SofaTab? = null, private  var sFindTabConfig:SofaTab? = null

        fun getDestConfig(): HashMap<String, Destination>? {
            if (sDestConfig == null) {
                val content: String = parseFile("destination.json")
                sDestConfig = JSON.parseObject(
                    content,
                    object : TypeReference<HashMap<String, Destination>?>() {})
            }
            return sDestConfig
        }

        fun getBottomBarConfig(): BottomBar? {
            if (sBottomBar == null) {
                val content = parseFile("main_tabs_config.json")
                sBottomBar = JSON.parseObject(content, BottomBar::class.java)
            }
            return sBottomBar
        }
        private fun parseFile(fileName: String): String{
            val assets = AppGlobals.getApplication()!!.assets
            var `is`: InputStream? = null
            var br: BufferedReader? = null
            val builder = StringBuilder()
            try {
                `is` = assets.open(fileName)
                br = BufferedReader(InputStreamReader(`is`))
                var line: String? = null
                while (br.readLine().also({ line = it }) != null) {
                    builder.append(line)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                try {
                    if (`is` != null) {
                        `is`.close()
                    }
                    if (br != null) {
                        br.close()
                    }
                } catch (e: Exception) {
                }
            }
            return builder.toString()
        }
    }
}