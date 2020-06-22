package com.example.mjetpack.ui.publish

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.text.TextUtils
import android.util.Rational
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.CameraX.LensFacing
import androidx.camera.core.Preview.OnPreviewOutputUpdateListener
import androidx.camera.core.VideoCapture.OnVideoSavedListener
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.example.mjetpack.R
import com.example.mjetpack.databinding.ActivityLayoutCaptureBinding
import com.example.mjetpack.view.RecordView
import java.io.File
import java.util.*

public class CaptureActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityLayoutCaptureBinding
    private val deniedPermission =
        ArrayList<String>()

    private val mLensFacing = LensFacing.BACK
    private val rotation = Surface.ROTATION_0
    private val resolution = Size(1280, 720)
    private val rational = Rational(9, 16)
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture? = null
    private var textureView: TextureView? = null
    private var takingPicture = false
    private var outputFilePath: String? = null



    companion object{
         val PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
        )
        val PERMISSION_CODE = 1000
        const val REQ_CAPTURE = 10001

        const val RESULT_FILE_PATH = "file_path"
        const val RESULT_FILE_WIDTH = "file_width"
        const val RESULT_FILE_HEIGHT = "file_height"
        const val RESULT_FILE_TYPE = "file_type"
        open fun startActivityForResult(activity: Activity) {
            val intent = Intent(activity, CaptureActivity::class.java)
            activity.startActivityForResult(intent, CaptureActivity.REQ_CAPTURE)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_layout_capture)
        ActivityCompat.requestPermissions(
            this,
            PERMISSIONS,
            PERMISSION_CODE
        )

        mBinding.recordView.setOnRecordListener(object : RecordView.onRecordListener {
            override fun onClick() {
                takingPicture = true
                val file = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    System.currentTimeMillis().toString() + ".jpeg"
                )
                mBinding.captureTips.visibility = View.INVISIBLE
                imageCapture!!.takePicture(file, object : ImageCapture.OnImageSavedListener {
                    override fun onImageSaved(file: File) {
                        onFileSaved(file)
                    }

                    override fun onError(
                        useCaseError: ImageCapture.UseCaseError,
                        message: String,
                        cause: Throwable?
                    ) {
                        showErrorToast(message)
                    }
                })
            }

            @SuppressLint("RestrictedApi")
            override fun onLongClick() {
                takingPicture = false
                val file = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    System.currentTimeMillis().toString() + ".mp4"
                )
                videoCapture!!.startRecording(file, object : OnVideoSavedListener {
                    override fun onVideoSaved(file: File) {
                        onFileSaved(file)
                    }

                    override fun onError(
                        useCaseError: VideoCapture.UseCaseError,
                        message: String,
                        cause: Throwable?
                    ) {
                        showErrorToast(message)
                    }
                })
            }

            @SuppressLint("RestrictedApi")
            override fun onFinish() {
                videoCapture?.stopRecording()
            }
        })
    }

    private fun onFileSaved(file: File) {

        outputFilePath = file.absolutePath
        val mimeType = if (takingPicture) "image/jpeg" else "video/mp4"
        MediaScannerConnection.scanFile(
            this,
            arrayOf<String>(outputFilePath!!),
            arrayOf(mimeType),
            null
        )
        PreviewActivity.startActivityForResult(this, outputFilePath, !takingPicture, "完成")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CODE) {
            deniedPermission.clear()
            for (i in permissions.indices) {
                val permission = permissions[i]
                val result = grantResults[i]
                if (result != PackageManager.PERMISSION_GRANTED) {
                    permission?.let { deniedPermission.add(it) }
                }
            }
            if (deniedPermission.isEmpty()) {
                bindCameraX()
            } else {
                AlertDialog.Builder(this)
                    .setMessage(getString(R.string.capture_permission_message))
                    .setNegativeButton(
                        getString(R.string.capture_permission_no)
                    ) { dialog, which ->
                        dialog.dismiss()
                        finish()
                    }
                    .setPositiveButton(
                        getString(R.string.capture_permission_ok)
                    ) { dialog, which ->
                        val denied =
                            arrayOfNulls<String>(deniedPermission.size)
                        ActivityCompat.requestPermissions(
                            this@CaptureActivity,
                            deniedPermission.toArray(denied),
                            PERMISSION_CODE
                        )
                    }.create().show()
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun bindCameraX() {
        CameraX.unbindAll()
        //查询一下当前要使用的设备摄像头(比如后置摄像头)是否存在
        var hasAvailableCameraId = false
        try {
            hasAvailableCameraId = CameraX.hasCameraWithLensFacing(mLensFacing)
        } catch (e: CameraInfoUnavailableException) {
            e.printStackTrace()
        }
        if (!hasAvailableCameraId) {
            showErrorToast("无可用的设备cameraId!,请检查设备的相机是否被占用")
            finish()
            return
        }
        //查询一下是否存在可用的cameraId.形式如：后置："0"，前置："1"
        var cameraIdForLensFacing: String? = null
        try {
            cameraIdForLensFacing = CameraX.getCameraFactory().cameraIdForLensFacing(mLensFacing)
        } catch (e: CameraInfoUnavailableException) {
            e.printStackTrace()
        }
        if (TextUtils.isEmpty(cameraIdForLensFacing)) {
            showErrorToast("无可用的设备cameraId!,请检查设备的相机是否被占用")
            finish()
            return
        }
        val config = PreviewConfig.Builder() //前后摄像头
            .setLensFacing(mLensFacing) //旋转角度
            .setTargetRotation(rotation) //分辨率
            .setTargetResolution(resolution) //宽高比
            .setTargetAspectRatio(rational)
            .build()
        preview = Preview(config)
        imageCapture = ImageCapture(
            ImageCaptureConfig.Builder()
                .setTargetAspectRatio(rational)
                .setTargetResolution(resolution)
                .setLensFacing(mLensFacing)
                .setTargetRotation(rotation).build()
        )
        videoCapture = VideoCapture(
            VideoCaptureConfig.Builder()
                .setTargetRotation(rotation)
                .setLensFacing(mLensFacing)
                .setTargetResolution(resolution)
                .setTargetAspectRatio(rational) //视频帧率
                .setVideoFrameRate(25) //bit率
                .setBitRate(3 * 1024 * 1024).build()
        )
        preview?.setOnPreviewOutputUpdateListener(OnPreviewOutputUpdateListener { output ->
            textureView = mBinding!!.textureView
            val parent = textureView?.getParent() as ViewGroup
            parent.removeView(textureView)
            parent.addView(textureView, 0)
            textureView?.setSurfaceTexture(output.surfaceTexture)
        })
        //上面配置的都是我们期望的分辨率
        val newUseList: MutableList<UseCase> = ArrayList()
        newUseList.add(preview!!)
        newUseList.add(imageCapture!!)
//        newUseList.add(videoCapture!!)
        //下面我们要查询一下 当前设备它所支持的分辨率有哪些，然后再更新一下 所配置的几个usecase
        val resolutions =
            CameraX.getSurfaceManager()
                .getSuggestedResolutions(cameraIdForLensFacing, null, newUseList)
        val iterator: Iterator<Map.Entry<UseCase, Size>> =
            resolutions.entries.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            val useCase = next.key
            val value = next.value
            val update: MutableMap<String?, Size> =
                HashMap()
            update[cameraIdForLensFacing] = value
            useCase.updateSuggestedResolution(update)
        }
//        CameraX.bindToLifecycle(this, preview, imageCapture, videoCapture)
        CameraX.bindToLifecycle(this, preview,imageCapture)
//        CameraX.bindToLifecycle(this, preview,videoCapture)
    }

    private fun showErrorToast(message: String) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(this@CaptureActivity, message, Toast.LENGTH_SHORT).show()
        } else {
            runOnUiThread {
                Toast.makeText(
                    this@CaptureActivity,
                    message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PreviewActivity.REQ_PREVIEW && resultCode == Activity.RESULT_OK) {
            val intent = Intent()
            intent.putExtra(RESULT_FILE_PATH, outputFilePath)
            //当设备处于竖屏情况时，宽高的值 需要互换，横屏不需要
            intent.putExtra(RESULT_FILE_WIDTH, resolution.height)
            intent.putExtra(RESULT_FILE_HEIGHT, resolution.width)
            intent.putExtra(RESULT_FILE_TYPE, !takingPicture)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    override fun onDestroy() {
        CameraX.unbindAll()
        super.onDestroy()
    }
}