package com.example.infusoryapp

import android.os.Bundle
import android.view.Choreographer
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.infusoryapp.databinding.ActivityMainBinding
import com.example.infusoryapp.databinding.DialogSelectModelBinding
import com.example.infusoryapp.model.ModelItem
import com.example.infusoryapp.ui.ModelContainerView
import com.example.infusoryapp.ui.ModelSelectionAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val activeContainers = mutableListOf<ModelContainerView>()

    // FPS Meter
    private var lastFrameTimeNanos: Long = 0
    private var frameCount: Int = 0
    private var fpsCalcStartTimeNanos: Long = 0
    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (fpsCalcStartTimeNanos == 0L) {
                fpsCalcStartTimeNanos = frameTimeNanos
            }
            frameCount++
            val elapsedNanos = frameTimeNanos - fpsCalcStartTimeNanos
            if (elapsedNanos >= 1_000_000_000L) { // 1 second
                val fps = (frameCount * 1_000_000_000L / elapsedNanos).toInt()
                binding.tvFpsCounter.text = "FPS: $fps"
                frameCount = 0
                fpsCalcStartTimeNanos = frameTimeNanos
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private val availableModels = listOf(
        ModelItem("bulb", "Light Bulb", "models/Bulb.glb", "Detailed light bulb with filament & base nodes"),
        ModelItem("fiagena", "Fiagena Engine", "models/Fiagena.glb", "Complex mechanical engine component"),
        ModelItem("lungs", "Human Lungs", "models/Lungs.glb", "Anatomical human lung structure with labeled lobes"),
        ModelItem("microscope", "Lab Microscope", "models/Microscope.glb", "Scientific microscope with lens & stage parts"),
        ModelItem("solarsystem", "Solar System", "models/solarsystem.glb", "3D Solar System planetary model")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.fabAddModel.setOnClickListener {
            showModelSelectionBottomSheet()
        }

        updateModelCountBadge()
    }

    override fun onResume() {
        super.onResume()
        fpsCalcStartTimeNanos = 0L
        frameCount = 0
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    override fun onPause() {
        super.onPause()
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    private fun showModelSelectionBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val dialogBinding = DialogSelectModelBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        val adapter = ModelSelectionAdapter(availableModels) { selectedModel ->
            dialog.dismiss()
            addModelToCanvas(selectedModel)
        }

        dialogBinding.rvModelList.layoutManager = LinearLayoutManager(this)
        dialogBinding.rvModelList.adapter = adapter
        dialog.show()
    }

    private fun addModelToCanvas(item: ModelItem) {
        val container = ModelContainerView(this)

        // Calculate offset position for cascading layout
        val count = activeContainers.size
        val density = resources.displayMetrics.density
        val offsetX = (count * 30 * density).toInt()
        val offsetY = (count * 30 * density).toInt()

        val params = FrameLayout.LayoutParams(
            (300 * density).toInt(),
            (300 * density).toInt()
        )
        container.layoutParams = params

        container.post {
            container.x = offsetX.toFloat().coerceAtMost(binding.canvasRoot.width - (200 * density))
            container.y = (offsetY.toFloat() + (60 * density)).coerceAtMost(binding.canvasRoot.height - (200 * density))
        }

        container.setupModel(item)

        container.onCloseRequested = {
            binding.canvasRoot.removeView(container)
            activeContainers.remove(container)
            updateModelCountBadge()
            Toast.makeText(this, "Model removed", Toast.LENGTH_SHORT).show()
        }

        binding.canvasRoot.addView(container)
        activeContainers.add(container)
        updateModelCountBadge()

        Toast.makeText(this, "Loaded ${item.name}", Toast.LENGTH_SHORT).show()
    }

    private fun updateModelCountBadge() {
        binding.tvModelCount.text = "Models: ${activeContainers.size}"
    }

    override fun onDestroy() {
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        for (container in activeContainers) {
            try {
                container.destroy()
            } catch (_: Exception) {}
        }
        activeContainers.clear()
        super.onDestroy()
    }
}
