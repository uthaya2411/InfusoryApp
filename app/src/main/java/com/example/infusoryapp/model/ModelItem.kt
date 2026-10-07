package com.example.infusoryapp.model

data class ModelItem(
    val id: String,
    val name: String,
    val assetPath: String,
    val description: String
)

enum class ContainerMode {
    NORMAL,      // Move & Resize container
    INTERACTION  // Rotate & Zoom 3D model
}

data class PartLabel(
    val nodeName: String,
    val labelText: String,
    val localTranslation: FloatArray? = null,
    var screenX: Float = 0f,
    var screenY: Float = 0f,
    var isVisibleOnScreen: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PartLabel

        if (nodeName != other.nodeName) return false
        if (labelText != other.labelText) return false
        if (localTranslation != null) {
            if (other.localTranslation == null) return false
            if (!localTranslation.contentEquals(other.localTranslation)) return false
        } else if (other.localTranslation != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = nodeName.hashCode()
        result = 31 * result + labelText.hashCode()
        result = 31 * result + (localTranslation?.contentHashCode() ?: 0)
        return result
    }
}
