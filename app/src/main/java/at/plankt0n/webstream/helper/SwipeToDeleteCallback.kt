package at.plankt0n.webstream.helper

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import at.plankt0n.webstream.R

abstract class SwipeToDeleteCallback(context: Context) :
    ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

    private val deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_recycle)
    private val backgroundColor = ContextCompat.getColor(context, R.color.red)
    private val background = ColorDrawable()

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val itemHeight = itemView.bottom - itemView.top

        // Hintergrund zeichnen
        background.color = backgroundColor
        background.setBounds(
            itemView.right + dX.toInt(),
            itemView.top,
            itemView.right,
            itemView.bottom
        )
        background.draw(c)

        // Icon zeichnen
        deleteIcon?.let {
            val iconTop = itemView.top + (itemHeight - it.intrinsicHeight) / 2
            val iconMargin = (itemHeight - it.intrinsicHeight) / 2
            val iconLeft = itemView.right - iconMargin - it.intrinsicWidth
            val iconRight = itemView.right - iconMargin
            val iconBottom = iconTop + it.intrinsicHeight

            it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
            it.draw(c)
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}
