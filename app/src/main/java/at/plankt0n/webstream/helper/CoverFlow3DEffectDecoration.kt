package at.plankt0n.webstream.helper

import android.graphics.Canvas
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class CoverFlow3DEffectDecoration(
    private val minScale: Float = 0.8f,
    private val maxRotation: Float = 30f
) : RecyclerView.ItemDecoration() {

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val centerX = parent.width / 2f

        for (i in 0 until parent.childCount) {
            val child: View = parent.getChildAt(i)
            val childCenterX = (child.left + child.right) / 2f
            val distanceFromCenter = abs(centerX - childCenterX)

            val maxDistance = centerX
            val scale = max(minScale, 1 - (distanceFromCenter / maxDistance) * (1 - minScale))
            val rotation = min(maxRotation, (distanceFromCenter / maxDistance) * maxRotation)

            // Wende Skalierung und Rotation an
            child.scaleX = scale
            child.scaleY = scale

            // Drehe um die Y-Achse je nach Position (links/rechts)
            child.rotationY = if (childCenterX < centerX) rotation else -rotation
        }
    }
}
