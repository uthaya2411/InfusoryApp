package com.example.infusoryapp.parser

import android.content.Context
import android.util.Log
import com.example.infusoryapp.model.PartLabel
import org.json.JSONObject
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object GlbLabelParser {

    private const val TAG = "GlbLabelParser"
    private const val GLB_MAGIC = 0x46546C67 // "glTF"
    private const val CHUNK_TYPE_JSON = 0x4E4F534A // "JSON"

    fun parseLabelsFromAsset(context: Context, assetPath: String): List<PartLabel> {
        val labels = mutableListOf<PartLabel>()
        try {
            context.assets.open(assetPath).use { inputStream ->
                val jsonString = extractJsonFromGlb(inputStream) ?: return emptyList()
                val jsonObject = JSONObject(jsonString)
                val nodesArray = jsonObject.optJSONArray("nodes") ?: return emptyList()

                for (i in 0 until nodesArray.length()) {
                    val nodeObj = nodesArray.getJSONObject(i)
                    val nodeName = nodeObj.optString("name", "Node_$i")

                    val extras = nodeObj.optJSONObject("extras")
                    val labelText = extras?.optString("prop", "") ?: ""

                    if (labelText.isNotBlank()) {
                        val translationArray = nodeObj.optJSONArray("translation")
                        val localTranslation = if (translationArray != null && translationArray.length() == 3) {
                            floatArrayOf(
                                translationArray.getDouble(0).toFloat(),
                                translationArray.getDouble(1).toFloat(),
                                translationArray.getDouble(2).toFloat()
                            )
                        } else null

                        labels.add(
                            PartLabel(
                                nodeName = nodeName,
                                labelText = labelText,
                                localTranslation = localTranslation
                            )
                        )
                        Log.d(TAG, "Found label in $assetPath -> Node: $nodeName, Label: $labelText")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing GLB labels from $assetPath", e)
        }
        return labels
    }

    private fun extractJsonFromGlb(inputStream: InputStream): String? {
        val headerBytes = ByteArray(12)
        if (inputStream.read(headerBytes) != 12) return null

        val headerBuffer = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN)
        val magic = headerBuffer.int
        if (magic != GLB_MAGIC) {
            Log.e(TAG, "Invalid GLB magic header: $magic")
            return null
        }

        val version = headerBuffer.int
        val totalLength = headerBuffer.int

        val chunkHeaderBytes = ByteArray(8)
        if (inputStream.read(chunkHeaderBytes) != 8) return null

        val chunkBuffer = ByteBuffer.wrap(chunkHeaderBytes).order(ByteOrder.LITTLE_ENDIAN)
        val chunkLength = chunkBuffer.int
        val chunkType = chunkBuffer.int

        if (chunkType != CHUNK_TYPE_JSON) {
            Log.e(TAG, "First chunk is not JSON: $chunkType")
            return null
        }

        val jsonBytes = ByteArray(chunkLength)
        var bytesRead = 0
        while (bytesRead < chunkLength) {
            val count = inputStream.read(jsonBytes, bytesRead, chunkLength - bytesRead)
            if (count == -1) break
            bytesRead += count
        }

        return String(jsonBytes, 0, bytesRead, Charsets.UTF_8)
    }
}
