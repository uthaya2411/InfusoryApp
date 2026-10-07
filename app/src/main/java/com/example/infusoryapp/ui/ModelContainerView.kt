package com.example.infusoryapp.ui

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.infusoryapp.R
import com.example.infusoryapp.databinding.ItemModelContainerBinding
import com.example.infusoryapp.model.ContainerMode
import com.example.infusoryapp.model.ModelItem
import com.example.infusoryapp.model.PartLabel
import com.example.infusoryapp.parser.GlbLabelParser
import com.google.android.filament.gltfio.FilamentInstance
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.collision.Vector3
import io.github.sceneview.node.ModelNode
import kotlin.math.max

class ModelContainerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val TAG = "ModelContainerView"
    private val binding: ItemModelContainerBinding =
        ItemModelContainerBinding.inflate(LayoutInflater.from(context), this, true)

    private var modelItem: ModelItem? = null
    private var currentMode: ContainerMode = ContainerMode.NORMAL
    private var isLabelsVisible: Boolean = false

    private var modelNode: ModelNode? = null
    private val partLabels = mutableListOf<PartLabel>()
    private val labelTextViews = mutableMapOf<PartLabel, TextView>()

    var onCloseRequested: (() -> Unit)? = null

    // Touch, Drag & Rotation tracking
    private var dX = 0f
    private var dY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isScaling = false

    private val scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            isScaling = true
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor

            if (currentMode == ContainerMode.NORMAL) {
                // Resize container card bounds ONLY
                val minSizePx = (MIN_CONTAINER_SIZE_DP * resources.displayMetrics.density).toInt()
                val maxSizePx = (MAX_CONTAINER_SIZE_DP * resources.displayMetrics.density).toInt()

                val newWidth = (width * scaleFactor).toInt().coerceIn(minSizePx, maxSizePx)
                val newHeight = (height * scaleFactor).toInt().coerceIn(minSizePx, maxSizePx)

                applyContainerSize(newWidth, newHeight)
            } else {
                // Interaction Mode: Pinch-to-zoom 3D model content
                modelNode?.let { node ->
                    val currentScale = node.scale.x
                    val newScale = (currentScale * scaleFactor).coerceIn(0.2f, 5.0f)
                    node.scale = Float3(newScale, newScale, newScale)
                }
            }

            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            isScaling = false
        }
    })

    companion object {
        private const val MIN_CONTAINER_SIZE_DP = 200
        private const val MAX_CONTAINER_SIZE_DP = 600
    }

    init {
        setupClickListeners()
        updateModeUI()
    }

    fun setupModel(item: ModelItem) {
        this.modelItem = item
        binding.tvModelName.text = item.name

        // Parse 3D part labels from GLB metadata
        val parsedLabels = GlbLabelParser.parseLabelsFromAsset(context, item.assetPath)
        partLabels.clear()
        partLabels.addAll(parsedLabels)

        // Load 3D model into SceneView using SceneView's ModelLoader
        binding.sceneView.post {
            try {
                binding.sceneView.modelLoader.loadModelInstanceAsync(
                    fileLocation = item.assetPath,
                    onResult = { instance: FilamentInstance? ->
                        // Post back to Main UI Thread to update views safely
                        post {
                            if (instance != null) {
                                val node = ModelNode(modelInstance = instance).apply {
                                    scaleToUnitCube(1.0f)
                                }
                                binding.sceneView.addChildNode(node)
                                modelNode = node
                            }
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error loading model GLB: ${item.assetPath}", e)
            }
        }

        // Frame callback for dynamic label projection every frame
        binding.sceneView.onFrame = { _ ->
            if (isLabelsVisible) {
                updateLabelsProjection()
            }
        }
    }

    private fun setupClickListeners() {
        // Container Zoom In / Out Buttons (Resize container frame ONLY)
        binding.btnZoomIn.setOnClickListener {
            resizeContainer(+40)
        }

        binding.btnZoomOut.setOnClickListener {
            resizeContainer(-40)
        }

        binding.btnToggleInteraction.setOnClickListener {
            toggleMode()
        }

        binding.btnToggleLabels.setOnClickListener {
            toggleLabels()
        }

        binding.btnClose.setOnClickListener {
            destroy()
            onCloseRequested?.invoke()
        }
    }

    private fun resizeContainer(deltaDp: Int) {
        val minSizePx = (MIN_CONTAINER_SIZE_DP * resources.displayMetrics.density).toInt()
        val maxSizePx = (MAX_CONTAINER_SIZE_DP * resources.displayMetrics.density).toInt()
        val stepPx = (deltaDp * resources.displayMetrics.density).toInt()

        val newWidth = (width + stepPx).coerceIn(minSizePx, maxSizePx)
        val newHeight = (height + stepPx).coerceIn(minSizePx, maxSizePx)

        applyContainerSize(newWidth, newHeight)
    }

    private fun applyContainerSize(newWidth: Int, newHeight: Int) {
        if (newWidth <= 0 || newHeight <= 0) return

        val params = layoutParams
        params.width = newWidth
        params.height = newHeight
        layoutParams = params

        binding.sceneView.requestLayout()
        if (isLabelsVisible) {
            updateLabelsProjection()
        }
    }

    private fun toggleMode() {
        currentMode = if (currentMode == ContainerMode.NORMAL) {
            ContainerMode.INTERACTION
        } else {
            ContainerMode.NORMAL
        }
        updateModeUI()
    }

    private fun updateModeUI() {
        if (currentMode == ContainerMode.NORMAL) {
            binding.tvModeBadge.text = "MOVE/RESIZE"
            binding.tvModeBadge.setBackgroundResource(R.drawable.bg_mode_badge_normal)
            binding.containerResizeBar.setBackgroundResource(R.drawable.bg_mode_badge_normal)
            binding.cardContainer.strokeColor = ContextCompat.getColor(context, R.color.border_normal)
        } else {
            binding.tvModeBadge.text = "ROTATE/ZOOM"
            binding.tvModeBadge.setBackgroundResource(R.drawable.bg_mode_badge_interaction)
            binding.containerResizeBar.setBackgroundResource(R.drawable.bg_mode_badge_interaction)
            binding.cardContainer.strokeColor = ContextCompat.getColor(context, R.color.border_interactive)
        }
    }

    private fun toggleLabels() {
        isLabelsVisible = !isLabelsVisible
        if (isLabelsVisible) {
            binding.btnToggleLabels.setColorFilter(ContextCompat.getColor(context, R.color.border_interactive))
            createLabelViews()
        } else {
            binding.btnToggleLabels.setColorFilter(ContextCompat.getColor(context, R.color.text_white))
            removeLabelViews()
        }
    }

    private fun createLabelViews() {
        binding.labelsContainer.removeAllViews()
        labelTextViews.clear()

        for (label in partLabels) {
            val tv = TextView(context).apply {
                text = label.labelText
                textSize = 11f
                setTextColor(ContextCompat.getColor(context, R.color.text_white))
                setBackgroundResource(R.drawable.bg_part_label)
                setPadding(16, 8, 16, 8)
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            }
            binding.labelsContainer.addView(tv)
            labelTextViews[label] = tv
        }
    }

    private fun removeLabelViews() {
        binding.labelsContainer.removeAllViews()
        labelTextViews.clear()
        binding.connectorOverlay.setConnections(emptyList())
    }

    @Suppress("DEPRECATION")
    private fun updateLabelsProjection() {
        val connections = mutableListOf<LineConnection>()
        val cameraNode = binding.sceneView.cameraNode
        val node = modelNode ?: return

        for (label in partLabels) {
            val tv = labelTextViews[label] ?: continue

            // Resolve 3D world position of named GLB node from SceneView node graph
            val targetNode = node.nodes.firstOrNull { it.name == label.nodeName }
            val worldPosVector = if (targetNode != null) {
                Vector3(targetNode.worldPosition.x, targetNode.worldPosition.y, targetNode.worldPosition.z)
            } else if (label.localTranslation != null) {
                val t = label.localTranslation
                val scale = node.scale.x
                Vector3(
                    node.worldPosition.x + t[0] * scale,
                    node.worldPosition.y + t[1] * scale,
                    node.worldPosition.z + t[2] * scale
                )
            } else {
                Vector3(node.worldPosition.x, node.worldPosition.y, node.worldPosition.z)
            }

            // Project 3D world position to 2D viewport screen coordinates
            val screenPos = cameraNode.worldToScreenPoint(worldPosVector)
            val px = screenPos.x
            val py = screenPos.y

            // Anchor 2D label TextView at projected screen coordinates
            tv.x = px + 20f
            tv.y = py - 30f
            tv.visibility = View.VISIBLE

            // Draw connector line from 3D projected point (px, py) to 2D label card
            connections.add(
                LineConnection(
                    nodeX = px,
                    nodeY = py,
                    labelX = tv.x,
                    labelY = tv.y + (tv.height / 2f)
                )
            )
        }

        binding.connectorOverlay.setConnections(connections)
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        // Allow button bar clicks to process normally
        val topBarRect = Rect()
        binding.topBar.getHitRect(topBarRect)
        if (topBarRect.contains(event.x.toInt(), event.y.toInt())) {
            return false
        }

        val resizeBarRect = Rect()
        binding.containerResizeBar.getHitRect(resizeBarRect)
        if (resizeBarRect.contains(event.x.toInt(), event.y.toInt())) {
            return false
        }

        // Intercept touch events for container drag/pinch (Normal Mode) or 3D rotation/zoom (Interaction Mode)
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Pass event to ScaleGestureDetector for 2-finger pinch
        scaleGestureDetector.onTouchEvent(event)

        val parentView = parent as? ViewGroup ?: return false

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                dX = x - event.rawX
                dY = y - event.rawY
                lastTouchX = event.rawX
                lastTouchY = event.rawY
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isScaling && event.pointerCount == 1) {
                    val deltaX = event.rawX - lastTouchX
                    val deltaY = event.rawY - lastTouchY

                    if (currentMode == ContainerMode.NORMAL) {
                        // Normal Mode: Move container view on canvas
                        val newX = (event.rawX + dX).coerceIn(
                            0f, max(0f, (parentView.width - width).toFloat())
                        )
                        val newY = (event.rawY + dY).coerceIn(
                            0f, max(0f, (parentView.height - height).toFloat())
                        )
                        x = newX
                        y = newY
                    } else {
                        // Interaction Mode: Rotate 3D model inside viewport
                        modelNode?.let { node ->
                            val rotX = (node.rotation.x + deltaY * 0.5f) % 360f
                            val rotY = (node.rotation.y + deltaX * 0.5f) % 360f
                            node.rotation = Float3(rotX, rotY, node.rotation.z)
                        }
                    }

                    lastTouchX = event.rawX
                    lastTouchY = event.rawY
                }
            }
        }
        return true
    }

    fun destroy() {
        try {
            binding.sceneView.onFrame = null
            removeLabelViews()

            modelNode?.let { node ->
                try {
                    node.destroy()
                } catch (_: Exception) {}
            }
            modelNode = null

            try {
                binding.sceneView.destroy()
            } catch (_: Exception) {}
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying SceneView container", e)
        }
    }
}
