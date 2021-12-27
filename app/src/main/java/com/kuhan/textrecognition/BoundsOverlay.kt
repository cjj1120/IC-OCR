package com.kuhan.textrecognition

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.google.mlkit.vision.text.Text
import kotlin.math.max
import kotlin.math.min
import android.util.Log
class BoundsOverlay(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private val lock = Any()

    private val rectPaint: Paint = Paint()
    private var text: Text? = null
    private val outlines: List<RectF>? get() = text?.textBlocks?.map { RectF(it.boundingBox) }

    // Matrix for transforming from image coordinates to overlay view coordinates.
    private val transformationMatrix = Matrix()
    private var imageWidth = 0
    private var imageHeight = 0

    // The factor of overlay View size to image size. Anything in the image coordinates need to be
    // scaled by this amount to fit with the area of overlay View.
    private var scaleFactor = 1.0f

    // The number of horizontal pixels needed to be cropped on each side to fit the image with the
    // area of overlay View after scaling.
    private var postScaleWidthOffset = 0f

    // The number of vertical pixels needed to be cropped on each side to fit the image with the
    // area of overlay View after scaling.
    private var postScaleHeightOffset = 0f
    private var isImageFlipped = false
    private var needUpdateTransformation = true

    init {
        rectPaint.color = Color.WHITE
        rectPaint.style = Paint.Style.STROKE
        rectPaint.strokeWidth = 4.0f
        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> needUpdateTransformation = true }
    }

    /** Draws the text block annotations for position, size, and raw value on the supplied canvas. */
    override fun onDraw(canvas: Canvas?) {
        canvas ?: return
        synchronized(lock) {
            updateTransformationIfNeeded()
            outlines?.forEach { drawText(it, canvas) }
        }
    }

    fun add(text: Text, imageWidth: Int, imageHeight: Int, isFlipped: Boolean = false) {
        if (imageWidth < 0) throw Throwable("image width must be positive")
        if (imageHeight < 0) throw Throwable("image width must be positive")
        synchronized(lock) {
            this.imageWidth = imageWidth
            this.imageHeight = imageHeight
            isImageFlipped = isFlipped
            needUpdateTransformation = true
            this.text = text
        }

        postInvalidate()
    }

    fun clear() {
        synchronized(lock) { text = null }
        postInvalidate()
    }

    private fun drawText(rect: RectF, canvas: Canvas) {
        val x0 = translateX(rect.left)
        val x1 = translateX(rect.right)
        rect.left = min(x0, x1)
        rect.right = max(x0, x1)
        rect.top = translateY(rect.top)
        rect.bottom = translateY(rect.bottom)
        canvas.drawRect(rect, rectPaint)
    }

    private fun translateX(x: Float): Float {
        return if (isImageFlipped) width - (scale(x) - postScaleWidthOffset)
        else scale(x) - postScaleWidthOffset
    }

    private fun translateY(y: Float): Float = scale(y) - postScaleHeightOffset

    private fun scale(imagePixel: Float): Float = imagePixel * scaleFactor

    private fun updateTransformationIfNeeded() {
        if (!needUpdateTransformation || imageWidth <= 0 || imageHeight <= 0) return
        val viewAspectRatio = width.toFloat() / height
        val imageAspectRatio = imageWidth.toFloat() / imageHeight
        postScaleWidthOffset = 0f
        postScaleHeightOffset = 0f
        if (viewAspectRatio > imageAspectRatio) {
            // The image needs to be vertically cropped to be displayed in this view.
            scaleFactor = width.toFloat() / imageWidth
            postScaleHeightOffset = (width.toFloat() / imageAspectRatio - height) / 2
        } else {
            // The image needs to be horizontally cropped to be displayed in this view.
            scaleFactor = height.toFloat() / imageHeight
            postScaleWidthOffset = (height.toFloat() * imageAspectRatio - width) / 2
        }
        transformationMatrix.reset()
        transformationMatrix.setScale(scaleFactor, scaleFactor)
        transformationMatrix.postTranslate(-postScaleWidthOffset, -postScaleHeightOffset)
        if (isImageFlipped) transformationMatrix.postScale(-1f, 1f, width / 2f, height / 2f)
        needUpdateTransformation = false
    }
}
