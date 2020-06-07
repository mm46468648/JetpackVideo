package com.example.mjetpack.ui.publish

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.lib_annotation.ActivityDestination
import com.example.mjetpack.R

@ActivityDestination(pageUrl = "main/tabs/publish", needLogin = true)
class PublicActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture)
    }
}
