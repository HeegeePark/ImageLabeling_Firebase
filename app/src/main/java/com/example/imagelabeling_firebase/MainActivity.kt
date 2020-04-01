package com.example.imagelabeling_firebase

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.imagelabeling_firebase.Helper.InternetCheck
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.cloud.FirebaseVisionCloudDetectorOptions
import com.google.firebase.ml.vision.cloud.label.FirebaseVisionCloudLabel
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.label.FirebaseVisionLabel
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetectorOptions
import com.wonderkiln.camerakit.*
import dmax.dialog.SpotsDialog
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    lateinit var waitingDialog:android.app.AlertDialog

    override fun onResume() {
        super.onResume()
        camera_view.start()
    }

    override fun onPause() {
        super.onPause()
        camera_view.stop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        waitingDialog = SpotsDialog.Builder()
            .setContext(this)
            .setCancelable(false)
            .setMessage("Please waiting...")
            .build()

        btn_detect.setOnClickListener {
            camera_view.start()
            camera_view.captureImage()
        }

        camera_view.addCameraKitListener(object :CameraKitEventListener{
            override fun onVideo(p0: CameraKitVideo?) {
            }

            override fun onEvent(p0: CameraKitEvent?) {
            }

            override fun onImage(p0: CameraKitImage?) {
                waitingDialog.show()

                var bitmap = p0!!.bitmap
                bitmap=Bitmap.createScaledBitmap(bitmap,camera_view.width,camera_view.height,false)
                camera_view.stop()

                runDetector(bitmap)
            }

            override fun onError(p0: CameraKitError?) {
                Log.d("onError","error")
            }
        })
    }

    private fun runDetector(bitmap: Bitmap?) {
        val image=FirebaseVisionImage.fromBitmap(bitmap!!)

        InternetCheck(object :InternetCheck.Consumer{
            override fun accept(isConnected: Boolean?) {
                if(isConnected!!) {
                    // Use Cloud Detector
                    val options = FirebaseVisionCloudDetectorOptions.Builder()
                        .setMaxResults(1)   // Get highest result
                        .build()

                    val detector=FirebaseVision.getInstance().getVisionCloudLabelDetector(options)

                    detector.detectInImage(image)
                        .addOnFailureListener { e-> Log.d("EDMTEROR",e.message) }
                        .addOnSuccessListener { result->processResultFromCloud(result) }
                }
                else{
                    // Use on Device
                    val options = FirebaseVisionLabelDetectorOptions.Builder()
                        .setConfidenceThreshold(0.8f)   // Get highest result
                        .build()

                    val detector=FirebaseVision.getInstance().getVisionLabelDetector(options)

                    detector.detectInImage(image)
                        .addOnFailureListener { e-> Log.d("EDMTEROR",e.message) }
                        .addOnSuccessListener { result->processResultFromDevice(result) }
                }
            }

        })

    }

    private fun processResultFromDevice(result: List<FirebaseVisionLabel>) {
        for(label in result)
            Toast.makeText(this,"Device Result: "+label.label,Toast.LENGTH_SHORT).show()
        waitingDialog.dismiss()

    }

    private fun processResultFromCloud(result: List<FirebaseVisionCloudLabel>) {
        for(label in result)
            Toast.makeText(this,"Cloud Result: "+label.label,Toast.LENGTH_SHORT).show()
        waitingDialog.dismiss()
    }
}
