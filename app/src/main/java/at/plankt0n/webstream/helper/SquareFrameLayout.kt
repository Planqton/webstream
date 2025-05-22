package at.plankt0n.webstream.helper

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

class SquareFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Höhe beibehalten, Breite anpassen
        val height = MeasureSpec.getSize(heightMeasureSpec)
        val squareSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        super.onMeasure(squareSpec, heightMeasureSpec)
    }
}
