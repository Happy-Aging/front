package com.example.happy_aging

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode

class ArActivity : AppCompatActivity() {
    private lateinit var arFragment: ArFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ar)

        arFragment = supportFragmentManager.findFragmentById(R.id.ar_fragment) as ArFragment

        // 아이템이 선택되었을 때 호출되는 메서드
        val selectedItem = intent.getStringExtra("SELECTED_ITEM")
        selectedItem?.let { loadModel(it) }

        // 사용자가 평면을 탭했을 때 모델을 배치하는 리스너 설정
        arFragment.setOnTapArPlaneListener { hitResult, _, _ ->
            placeModel(hitResult, selectedItem ?: "")
        }
    }

    private fun loadModel(itemId: String) {
        val modelUri = Uri.parse("models/$itemId.glb") // models 폴더 내의 해당 파일
        ModelRenderable.builder()
            .setSource(this, modelUri)
            .build()
            .thenAccept { renderable ->
                // 모델이 로드되면 해당 모델을 저장
                modelMap[itemId] = renderable
            }
            .exceptionally { throwable ->
                // 에러 처리 로직
                null
            }
    }

    private fun placeModel(hitResult: HitResult, itemName: String) {
        val anchor: Anchor = hitResult.createAnchor()
        val anchorNode = AnchorNode(anchor).apply {
            setParent(arFragment.arSceneView.scene)
        }

        val modelRenderable = modelMap[itemName]
        modelRenderable?.let {
            val modelNode = TransformableNode(arFragment.transformationSystem).apply {
                renderable = modelRenderable
                setParent(anchorNode)
            }
        }
    }

    companion object {
        private val modelMap = mutableMapOf<String, ModelRenderable>()
    }
}
