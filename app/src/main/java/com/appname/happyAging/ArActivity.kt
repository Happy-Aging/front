package com.appname.happyAging

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.HitResult
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode

class ArActivity : AppCompatActivity() {
    private lateinit var arFragment: ArFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ar)
        showInstructionDialog()

        arFragment = supportFragmentManager.findFragmentById(R.id.ar_fragment) as ArFragment

        val selectedItem = intent.getStringExtra("SELECTED_ITEM")
        selectedItem?.let { itemId ->
            arFragment.setOnTapArPlaneListener { hitResult, _, _ ->
                placeImage(hitResult, itemId)
            }
        }
    }
    private fun showInstructionDialog() {
        AlertDialog.Builder(this)
            .setTitle("유의사항")
            .setMessage("- 바닥이 평평한 곳에 카메라를 2~5초 비추세요.\n\n- 화면에 흰 점들이 나타나면 화면을 터치하세요.")
            .setPositiveButton("확인 완료") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    private fun placeImage(hitResult: HitResult, itemId: String) {
        val imageResId = when (itemId) {
            "item1" -> R.drawable.img_car_walker
            "item2" -> R.drawable.img_bed
            "item3" -> R.drawable.img_car_walker_two
            "item4" -> R.drawable.img_chair
            else -> return
        }

        ViewRenderable.builder()
            .setView(this, R.layout.ar_image_view)
            .build()
            .thenAccept { renderable ->
                val imageView = renderable.view as ImageView
                imageView.setImageResource(imageResId)
                Log.d("ArActivity", "로드할 이미지 모델: ${imageResId}")


                val anchor = hitResult.createAnchor()
                val anchorNode = AnchorNode(anchor).apply {
                    setParent(arFragment.arSceneView.scene)
                }

                val viewNode = TransformableNode(arFragment.transformationSystem).apply {
                    this.renderable = renderable
                    setParent(anchorNode)
                }
            }
            .exceptionally { throwable ->
                Log.e("ArActivity", "모델 로딩 실패: $itemId", throwable)
                null
            }
    }

    companion object {
        private val modelMap = mutableMapOf<String, ModelRenderable>()
    }
}
