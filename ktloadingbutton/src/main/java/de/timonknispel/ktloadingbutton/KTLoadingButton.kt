package de.timonknispel.ktloadingbutton

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.core.graphics.ColorUtils
import java.util.*
import kotlin.math.ceil

class KTLoadingButton : View {

    private var state: State = State.NONE

    private var isSucceed = true
    private var isDoResult = false
    private var isDoneWithResult = false

    private var buttonText = ""
    private var buttonColor = 0
    private var successColor = 0
    private var failColor = 0
    private var textSize = 0
    private var _loadingBGPaint = 0
    private var progressStyle = ProgressStyle.INTERMEDIATE

    private var submitAnim: ValueAnimator? = null
    private var loadingAnim: ValueAnimator? = null
    private var resultAnim: ValueAnimator? = null

    private var mWidth: Int = 0
    private var mHeight: Int = 0

    private var maxWidth: Int = 0
    private var maxHeight: Int = 0

    private var x: Int = 0
    private var y: Int = 0

    private lateinit var bgPaint: Paint
    private lateinit var loadingPaint: Paint
    private lateinit var resultPaint: Paint
    private lateinit var textPaint: Paint
    private lateinit var loadingBGPaint: Paint

    private lateinit var buttonPath: Path
    private lateinit var loadPath: Path
    private lateinit var dst: Path
    private lateinit var pathMeasure: PathMeasure
    private lateinit var resultPath: Path

    private lateinit var circleLeft: RectF
    private lateinit var circleMid: RectF
    private lateinit var circleRight: RectF

    private var textWidth: Int = 0
    private var textHeight: Int = 0

    private var loadValue = 0F
    private var currentProgress = 0F

    private var resetHandler = Handler()
    private var shouldAutoResetAfterResult: Boolean = true

    var touchListener: (() -> Unit)? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        val typedArray =
            context?.obtainStyledAttributes(attrs, R.styleable.KTLoadingButton, defStyleAttr, 0)
        if (typedArray != null) {
            if (typedArray.getString(R.styleable.KTLoadingButton_buttonName) != null) {
                buttonText =
                    typedArray.getString(R.styleable.KTLoadingButton_buttonName).orEmpty()
            }
            if (typedArray.getBoolean(R.styleable.KTLoadingButton_allCaps, true)) {
                buttonText = buttonText.toUpperCase(Locale.getDefault())
            }

            buttonColor = typedArray.getColor(
                R.styleable.KTLoadingButton_buttonColor,
                Color.parseColor("#373737")
            )
            successColor = typedArray.getColor(
                R.styleable.KTLoadingButton_succeedColor,
                Color.parseColor("#4CAF50")
            )
            failColor = typedArray.getColor(
                R.styleable.KTLoadingButton_failedColor,
                Color.parseColor("#F44336")
            )
            _loadingBGPaint = typedArray.getColor(
                R.styleable.KTLoadingButton_loadingBackgroundColor,
                -1
            )
            textSize = typedArray.getDimension(
                R.styleable.KTLoadingButton_buttonTextSize,
                sp2px(16f).toFloat()
            ).toInt()
            shouldAutoResetAfterResult = typedArray.getBoolean(
                R.styleable.KTLoadingButton_autoResetButtonAfterResult,
                true
            )
            progressStyle =
                ProgressStyle.formID(
                    typedArray.getInt(
                        R.styleable.KTLoadingButton_progressStyle,
                        ProgressStyle.INTERMEDIATE.id
                    )
                )
            typedArray.recycle()
        }
        this.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        setup()
    }

    private fun setup() {
        bgPaint = Paint().apply {
            color = buttonColor
            style = Paint.Style.STROKE
            strokeWidth = 5f
            isAntiAlias = true
        }

        loadingPaint = Paint().apply {
            color = buttonColor
            style = Paint.Style.STROKE
            strokeWidth = 9f
            isAntiAlias = true
        }

        loadingBGPaint = Paint().apply {
            color = if (_loadingBGPaint != -1) _loadingBGPaint else ColorUtils.setAlphaComponent(
                buttonColor,
                70
            )
            style = Paint.Style.STROKE
            strokeWidth = 9f
            isAntiAlias = false
        }

        resultPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 9f
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }

        textPaint = Paint().apply {
            color = buttonColor
            strokeWidth = (this@KTLoadingButton.textSize / 6F)
            this.textSize = this@KTLoadingButton.textSize.toFloat()
            isAntiAlias = true
        }

        textWidth = getTextWidth(textPaint, buttonText)
        textHeight = getTextHeight(textPaint, buttonText)

        buttonPath = Path()
        loadPath = Path()
        resultPath = Path()
        dst = Path()
        circleMid = RectF()
        circleLeft = RectF()
        circleRight = RectF()
        pathMeasure = PathMeasure()
    }

    fun startLoading() {
        if (state == State.NONE) {
            startSubmitAnim()
        }
    }

    fun doResult(isSucceed: Boolean, onDone: (() -> Unit)? = null) {
        if (state == State.NONE || state == State.RESULT || isDoResult) {
            return
        }
        isDoResult = true
        this.isSucceed = isSucceed
        if (state == State.LOADING) {
            startResultAnim(onDone)
        }
    }

    fun reset() {
        submitAnim?.cancel()
        loadingAnim?.cancel()
        resultAnim?.cancel()
        state = State.NONE
        mWidth = maxWidth
        mHeight = maxHeight
        isSucceed = false
        isDoResult = false
        currentProgress = 0f
        setup()
        invalidate()
    }

    fun setProgress(progress: Float) {
        if (progress < 0 || progress > 100) {
            return
        }
        currentProgress = (progress * 0.01).toFloat()
        if (progressStyle == ProgressStyle.PROGRESS) {
            if (state != State.LOADING) {
                startLoading()
            }
            invalidate()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = View.MeasureSpec.getMode(widthMeasureSpec)
        var widthSize = View.MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = View.MeasureSpec.getMode(heightMeasureSpec)
        var heightSize = View.MeasureSpec.getSize(heightMeasureSpec)

        if (widthMode == View.MeasureSpec.AT_MOST) {
            widthSize = textWidth + 100
        }

        if (heightMode == View.MeasureSpec.AT_MOST) {
            heightSize = (textHeight * 3F).toInt()
        }

        if (heightSize > widthSize) {
            heightSize = (widthSize * 0.25F).toInt()
        }

        mWidth = widthSize - 5
        mHeight = heightSize - 5
        x = (widthSize * 0.5F).toInt()
        y = (heightSize * 0.5F).toInt()
        maxWidth = mWidth
        maxHeight = mHeight

        setMeasuredDimension(widthSize, heightSize)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.translate(x.toFloat(), y.toFloat())
        drawButton(canvas)
        if (state == State.NONE || state == State.SUBMIT && mWidth > textWidth) {
            drawButtonText(canvas)
        }
        if (state == State.LOADING) {
            drawLoading(canvas)
        }
        if (state == State.RESULT) {
            drawResult(canvas, isSucceed)
        }
    }

    private fun drawButton(canvas: Canvas) {
        buttonPath.reset()
        circleLeft.set(
            (-mWidth / 2).toFloat(),
            (-mHeight / 2).toFloat(),
            (-mWidth / 2 + mHeight).toFloat(),
            (mHeight / 2).toFloat()
        )
        buttonPath.arcTo(circleLeft, 90f, 180f)
        buttonPath.lineTo((mWidth / 2 - mHeight / 2).toFloat(), (-mHeight / 2).toFloat())
        circleRight.set(
            (mWidth / 2 - mHeight).toFloat(),
            (-mHeight / 2).toFloat(),
            (mWidth / 2).toFloat(),
            (mHeight / 2).toFloat()
        )
        buttonPath.arcTo(circleRight, 270f, 180f)
        buttonPath.lineTo((-mWidth / 2 + mHeight / 2).toFloat(), (mHeight / 2).toFloat())
        canvas.drawPath(buttonPath, bgPaint)
    }

    private fun drawLoading(canvas: Canvas) {
        dst.reset()
        circleMid.set(
            (-maxHeight / 2F),
            (-maxHeight / 2F),
            (maxHeight / 2F),
            (maxHeight / 2F)
        )
        loadPath.addArc(circleMid, 270F, 359.999f)
        pathMeasure.setPath(loadPath, true)
        var startD = 0f
        val stopD: Float
        if (progressStyle == ProgressStyle.INTERMEDIATE) {
            startD = (pathMeasure.length * loadValue)
            stopD = startD + ((pathMeasure.length / 2) * loadValue)
        } else {
            stopD = pathMeasure.length * currentProgress
        }
        pathMeasure.getSegment(startD, stopD, dst, true)
        canvas.drawPath(dst, loadingPaint)
        canvas.drawCircle(
            canvas.clipBounds.exactCenterX(),
            canvas.clipBounds.exactCenterY(),
            maxHeight / 2F,
            loadingBGPaint
        )
    }

    private fun drawResult(canvas: Canvas, isSucceed: Boolean) {
        if (isSucceed) {
            resultPath.moveTo((-mHeight / 6).toFloat(), 0f)
            resultPath.lineTo(0f, (-mHeight / 6 + (1 + Math.sqrt(5.0)) * mHeight / 12).toFloat())
            resultPath.lineTo((mHeight / 6).toFloat(), (-mHeight / 6).toFloat())
        } else {
            resultPath.moveTo((-mHeight / 6).toFloat(), (mHeight / 6).toFloat())
            resultPath.lineTo((mHeight / 6).toFloat(), (-mHeight / 6).toFloat())
            resultPath.moveTo((-mHeight / 6).toFloat(), (-mHeight / 6).toFloat())
            resultPath.lineTo((mHeight / 6).toFloat(), (mHeight / 6).toFloat())
        }
        canvas.drawPath(resultPath, resultPaint)
    }

    private fun drawButtonText(canvas: Canvas) {
        textPaint.alpha = (mWidth - textWidth) * 255 / (maxWidth - textWidth)
        canvas.drawText(buttonText, (-textWidth / 2).toFloat(), getTextBaseLineOffset(), textPaint)
    }

    private fun startSubmitAnim() {
        state = State.SUBMIT
        submitAnim = ValueAnimator.ofInt(maxWidth, maxHeight)
        submitAnim?.apply {
            addUpdateListener { animation ->
                mWidth = animation.animatedValue as Int
                if (mWidth == mHeight) {
                    bgPaint.color = Color.TRANSPARENT
                }
                invalidate()
            }

            duration = 300
            interpolator = AccelerateInterpolator()

            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (isDoResult) {
                        startResultAnim(null)
                    } else {
                        startLoadingAnim()
                    }
                }

                override fun onAnimationCancel(animation: Animator) {
                }

                override fun onAnimationRepeat(animation: Animator) {
                }
            })
        }?.start()
    }

    private fun startLoadingAnim() {
        state = State.LOADING
        if (progressStyle == ProgressStyle.PROGRESS) {
            invalidate()
            return
        }
        loadingAnim = ValueAnimator.ofFloat(0.0f, 1.0f).apply {
            addUpdateListener { animation ->
                loadValue = animation.animatedValue as Float
                invalidate()
            }
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
        }
        loadingAnim?.start()
    }

    private fun startResultAnim(onDone: (() -> Unit)?) {
        state = State.RESULT
        isDoneWithResult = false
        loadingAnim?.cancel()

        resultAnim = ValueAnimator.ofInt(maxHeight, maxWidth).apply {
            addUpdateListener { animation ->
                mWidth = animation.animatedValue as Int
                resultPaint.alpha = (mWidth - mHeight) * 255 / (maxWidth - maxHeight)
                if (mWidth == mHeight) {
                    if (isSucceed) {
                        bgPaint.color = successColor
                    } else {
                        bgPaint.color = failColor
                    }
                    bgPaint.style = Paint.Style.FILL_AND_STROKE
                }
                invalidate()
            }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                }

                override fun onAnimationEnd(animation: Animator) {
                    postDelayed({
                        onDone?.invoke()
                        isDoneWithResult = true
                    }, 500)
                    if (shouldAutoResetAfterResult) {
                        resetHandler.postDelayed({
                            reset()
                        }, 1500)
                    }
                }

                override fun onAnimationCancel(animation: Animator) {
                }

                override fun onAnimationRepeat(animation: Animator) {
                }
            })
            duration = 300
            interpolator = AccelerateInterpolator()
        }
        resultAnim?.start()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_UP -> {
                touchListener?.invoke()
                when (state) {
                    State.NONE -> startSubmitAnim()
                    State.RESULT -> if (isDoneWithResult) {
                        resetHandler.removeCallbacksAndMessages(null)
                        reset()
                        return true
                    }
                    else -> return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (submitAnim != null) {
            submitAnim?.cancel()
        }
        if (loadingAnim != null) {
            loadingAnim?.cancel()
        }
        if (resultAnim != null) {
            resultAnim?.cancel()
        }
    }

    private fun sp2px(sp: Float): Int {
        val fontScale = context.resources.displayMetrics.scaledDensity
        return (sp * fontScale + 0.5f).toInt()
    }

    private fun getTextWidth(paint: Paint, str: String?): Int {
        var mRet = 0
        if (!str.isNullOrEmpty()) {
            val len = str.length
            val widths = FloatArray(len)
            paint.getTextWidths(str, widths)
            for (j in 0 until len) {
                mRet += ceil(widths[j].toDouble()).toInt()
            }
        }
        return mRet
    }

    private fun getTextHeight(paint: Paint, str: String): Int {
        val rect = Rect()
        paint.getTextBounds(str, 0, str.length, rect)
        return rect.height()
    }

    private fun getTextBaseLineOffset(): Float {
        val fm = textPaint.fontMetrics
        return -(fm.bottom + fm.top) / 2
    }
}

enum class State {
    NONE, LOADING, RESULT, SUBMIT;
}

enum class ProgressStyle(val id: Int) {
    PROGRESS(1), INTERMEDIATE(0);

    companion object {
        fun formID(id: Int) = enumValues<ProgressStyle>().find { it.id == id } ?: INTERMEDIATE
    }
}