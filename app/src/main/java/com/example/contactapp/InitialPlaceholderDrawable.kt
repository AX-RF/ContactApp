
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat

class InitialPlaceholderDrawable(
    private val initials: String,
    context: Context
) : Drawable() {

    private val textPaint: Paint
    private val circlePaint: Paint
    private val density = context.resources.displayMetrics.density

    init {
        // Initialize Paint for drawing text (initials)
        textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = 24f * density // Text size based on screen density
        }

        // Initialize Paint for drawing the circle background
        circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = getInitialColor(initials, context) // Assign color based on initials
            style = Paint.Style.FILL
        }
    }

    // Function to generate a consistent color based on the initials hash code
    private fun getInitialColor(initials: String, context: Context): Int {
        val colorResourceIds = intArrayOf(
            android.R.color.holo_red_dark,
            android.R.color.holo_green_dark,
            android.R.color.holo_orange_dark,
            android.R.color.holo_purple,
            android.R.color.holo_blue_dark,
            android.R.color.darker_gray
        )
        // Use a modulo of the hash code to consistently map initials to a color
        val index = kotlin.math.abs(initials.hashCode() % colorResourceIds.size)
        return ContextCompat.getColor(context, colorResourceIds[index])
    }

    override fun draw(canvas: Canvas) {
        val width = bounds.width()
        val height = bounds.height()
        val radius = kotlin.math.min(width, height) / 2f
        val centerX = width / 2f
        val centerY = height / 2f

        // 1. Draw the circle
        canvas.drawCircle(centerX, centerY, radius, circlePaint)

        // 2. Calculate and draw the text (initials)
        val textBounds = Rect()
        textPaint.getTextBounds(initials, 0, initials.length, textBounds)

        // Adjust text position to center it perfectly vertically
        val textY = centerY + (textBounds.height() / 2f)

        canvas.drawText(initials, centerX, textY, textPaint)
    }

    override fun setAlpha(alpha: Int) {
        textPaint.alpha = alpha
        circlePaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        textPaint.colorFilter = colorFilter
        circlePaint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.OPAQUE
}