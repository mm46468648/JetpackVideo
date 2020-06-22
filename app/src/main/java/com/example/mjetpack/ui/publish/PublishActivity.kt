package com.example.mjetpack.ui.publish

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.alibaba.fastjson.JSONObject
import com.example.lib_annotation.ActivityDestination
import com.example.lib_common.dialog.LoadingDialog
import com.example.lib_common.utils.FileUtils
import com.example.lib_network.ApiResponse
import com.example.lib_network.ApiService
import com.example.lib_network.JsonCallback
import com.example.mjetpack.R
import com.example.mjetpack.databinding.ActivityLayoutPublishBinding
import com.example.mjetpack.model.Feed
import com.example.mjetpack.model.TagList
import com.example.mjetpack.ui.login.UserManager
import java.util.*

@ActivityDestination(pageUrl = "main/tabs/publish", needLogin = true)
class PublishActivity : AppCompatActivity(), View.OnClickListener {

    lateinit var  mBinding: ActivityLayoutPublishBinding
    private var width = 0
    private  var height:Int = 0
    private var filePath: String? = null
    private  var coverFilePath:kotlin.String? = null
    private var isVideo = false
    private var mTagList: TagList? = null
    private var coverUploadUUID: UUID? = null
    private  var fileUploadUUID:java.util.UUID? = null
    private var coverUploadUrl: String? = null
    private  var fileUploadUrl:kotlin.String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding =  DataBindingUtil.setContentView<ActivityLayoutPublishBinding>(this,R.layout.activity_layout_publish)

        mBinding.actionClose.setOnClickListener(this)
        mBinding.actionPublish.setOnClickListener(this)
        mBinding.actionDeleteFile.setOnClickListener(this)
        mBinding.actionAddTag.setOnClickListener(this)
        mBinding.actionAddFile.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        val id = v?.id
        when (id) {
            R.id.action_close -> showExitDialog()
            R.id.action_publish -> publish()
            R.id.action_add_tag -> {
                val fragment = TagBottomSheetDialogFragment()
                fragment.setOnTagItemSelectedListener(object : TagBottomSheetDialogFragment.OnTagItemSelectedListener {
                    override fun onTagItemSelected(tagList: TagList) {
                        mTagList = tagList
                        mBinding.actionAddTag.setText(tagList.title)
                    }
                })
                fragment.show(supportFragmentManager, "tag_dialog")
            }
            R.id.action_add_file -> CaptureActivity.startActivityForResult(this)
            R.id.action_delete_file -> {
                mBinding.actionAddFile.visibility = View.VISIBLE
                mBinding.fileContainer.visibility = View.GONE
                mBinding.cover.setImageDrawable(null)
                filePath = null
                width = 0
                height = 0
                isVideo = false
            }
        }
    }

    private fun showExitDialog() {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.publish_exit_message))
            .setNegativeButton(getString(R.string.publish_exit_action_cancel), null)
            .setPositiveButton(
                getString(R.string.publish_exit_action_ok)
            ) { dialog, which ->
                dialog.dismiss()
                finish()
            }.create().show()
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == CaptureActivity.REQ_CAPTURE && data != null) {
            width = data.getIntExtra(CaptureActivity.RESULT_FILE_WIDTH, 0)
            height = data.getIntExtra(CaptureActivity.RESULT_FILE_HEIGHT, 0)
            filePath = data.getStringExtra(CaptureActivity.RESULT_FILE_PATH)
            isVideo = data.getBooleanExtra(CaptureActivity.RESULT_FILE_TYPE, false)
            showFileThumbnail()
        }
    }

    private fun showFileThumbnail() {
        if (TextUtils.isEmpty(filePath)) {
            return
        }
        mBinding.actionAddFile.visibility = View.GONE
        mBinding.fileContainer.visibility = View.VISIBLE
        mBinding.cover.setImageUrl(filePath)
        mBinding.videoIcon.visibility = if (isVideo) View.VISIBLE else View.GONE
        mBinding.cover.setOnClickListener {
            PreviewActivity.startActivityForResult(
                this@PublishActivity,
                filePath,
                isVideo,
                null
            )
        }
    }
    private fun publish() {
        showLoading()
        val workRequests: MutableList<OneTimeWorkRequest> =
            ArrayList()
        if (!TextUtils.isEmpty(filePath)) {
            if (isVideo) { //生成视频封面文件
                FileUtils.generateVideoCover(filePath)
                    .observe(this, Observer<String> { coverPath ->
                        coverFilePath = coverPath
                        val request: OneTimeWorkRequest = getOneTimeWorkRequest(coverPath)
                        coverUploadUUID = request.id
                        workRequests.add(request)
                        enqueue(workRequests)
                    })
            }
            val request: OneTimeWorkRequest = getOneTimeWorkRequest(filePath!!)
            fileUploadUUID = request.id
            workRequests.add(request)
            //如果是视频文件则需要等待封面文件生成完毕后再一同提交到任务队列
//否则 可以直接提交了
            if (!isVideo) {
                enqueue(workRequests)
            }
        } else {
            publishFeed()
        }
    }

    private fun enqueue(workRequests: List<OneTimeWorkRequest>) {
        val workContinuation =
            WorkManager.getInstance(this@PublishActivity).beginWith(workRequests)
        workContinuation.enqueue()
        workContinuation.workInfosLiveData.observe(
            this@PublishActivity,
            Observer { workInfos ->
                //block runing enuqued failed susscess finish
                var completedCount = 0
                var failedCount = 0
                for (workInfo in workInfos) {
                    val state = workInfo.state
                    val outputData = workInfo.outputData
                    val uuid = workInfo.id
                    if (state == WorkInfo.State.FAILED) { // if (uuid==coverUploadUUID)是错的
                        if (uuid == coverUploadUUID) {
                            showToast(getString(R.string.file_upload_cover_message))
                        } else if (uuid == fileUploadUUID) {
                            showToast(getString(R.string.file_upload_original_message))
                        }
                        failedCount++
                    } else if (state == WorkInfo.State.SUCCEEDED) {
                        val fileUrl = outputData.getString("fileUrl")
                        if (uuid == coverUploadUUID) {
                            coverUploadUrl = fileUrl
                        } else if (uuid == fileUploadUUID) {
                            fileUploadUrl = fileUrl
                        }
                        completedCount++
                    }
                }
                if (completedCount >= workInfos.size) {
                    publishFeed()
                } else if (failedCount > 0) {
                    dismissLoading()
                }
            })
    }

    private fun publishFeed() {
        ApiService.post<JSONObject>("/feeds/publish")
            .addParam("coverUrl", coverUploadUrl)
            .addParam("fileUrl", fileUploadUrl)
            .addParam("fileWidth", width)
            .addParam("fileHeight", height)
            .addParam("userId", UserManager.get().getUserId())
            .addParam("tagId", if (mTagList == null) 0 else mTagList!!.tagId)
            .addParam("tagTitle", if (mTagList == null) "" else mTagList!!.title)
            .addParam("feedText", mBinding.inputView.text.toString())
            .addParam("feedType", if (isVideo) Feed.TYPE_VIDEO else Feed.TYPE_IMAGE_TEXT)
            .execute(object : JsonCallback<JSONObject?>() {
                override fun onSuccess(response: ApiResponse<JSONObject?>?) {
                    showToast(getString(R.string.feed_publisj_success))
                    finish()
                    dismissLoading()
                }

                override fun onError(response: ApiResponse<JSONObject?>) {
                    showToast(response.message)
                    dismissLoading()
                }
            })
    }

    private var mLoadingDialog: LoadingDialog? = null

    private fun showLoading() {
        if (mLoadingDialog == null) {
            mLoadingDialog = LoadingDialog(this)
            mLoadingDialog?.setLoadingText(getString(R.string.feed_publish_ing))
        }
        mLoadingDialog?.show()
    }

    private fun dismissLoading() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            if (mLoadingDialog != null) {
                mLoadingDialog?.dismiss()
            }
        } else {
            runOnUiThread {
                if (mLoadingDialog != null) {
                    mLoadingDialog?.dismiss()
                }
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun getOneTimeWorkRequest(filePath: String): OneTimeWorkRequest {
        val inputData = Data.Builder()
            .putString("file", filePath)
            .build()
        //        @SuppressLint("RestrictedApi") Constraints constraints = new Constraints();
//        //设备存储空间充足的时候 才能执行 ,>15%
//        constraints.setRequiresStorageNotLow(true);
//        //必须在执行的网络条件下才能好执行,不计流量 ,wifi
//        constraints.setRequiredNetworkType(NetworkType.UNMETERED);
//        //设备的充电量充足的才能执行 >15%
//        constraints.setRequiresBatteryNotLow(true);
//        //只有设备在充电的情况下 才能允许执行
//        constraints.setRequiresCharging(true);
//        //只有设备在空闲的情况下才能被执行 比如息屏，cpu利用率不高
//        constraints.setRequiresDeviceIdle(true);
//        //workmanager利用contentObserver监控传递进来的这个uri对应的内容是否发生变化,当且仅当它发生变化了
//        //我们的任务才会被触发执行，以下三个api是关联的
//        constraints.setContentUriTriggers(null);
//        //设置从content变化到被执行中间的延迟时间，如果在这期间。content发生了变化，延迟时间会被重新计算
//这个content就是指 我们设置的setContentUriTriggers uri对应的内容
//        constraints.setTriggerContentUpdateDelay(0);
//        //设置从content变化到被执行中间的最大延迟时间
//这个content就是指 我们设置的setContentUriTriggers uri对应的内容
//        constraints.setTriggerMaxContentDelay(0);
        return OneTimeWorkRequest.Builder(UploadFileWorker::class.java)
            .setInputData(inputData) //                .setConstraints(constraints)
//                //设置一个拦截器，在任务执行之前 可以做一次拦截，去修改入参的数据然后返回新的数据交由worker使用
//                .setInputMerger(null)
//                //当一个任务被调度失败后，所要采取的重试策略，可以通过BackoffPolicy来执行具体的策略
//                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
//                //任务被调度执行的延迟时间
//                .setInitialDelay(10, TimeUnit.SECONDS)
//                //设置该任务尝试执行的最大次数
//                .setInitialRunAttemptCount(2)
//                //设置这个任务开始执行的时间
//                //System.currentTimeMillis()
//                .setPeriodStartTime(0, TimeUnit.SECONDS)
//                //指定该任务被调度的时间
//                .setScheduleRequestedAt(0, TimeUnit.SECONDS)
//                //当一个任务执行状态编程finish时，又没有后续的观察者来消费这个结果，难么workamnager会在
//                //内存中保留一段时间的该任务的结果。超过这个时间，这个结果就会被存储到数据库中
//                //下次想要查询该任务的结果时，会触发workmanager的数据库查询操作，可以通过uuid来查询任务的状态
//                .keepResultsForAtLeast(10, TimeUnit.SECONDS)
            .build()
    }

    private fun showToast(message: String) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        } else {
            runOnUiThread {
                Toast.makeText(this@PublishActivity, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

}
