package com.x.leo.refrashviews

import android.content.Context
import android.graphics.Rect
import android.support.v4.widget.ViewDragHelper
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListView
import java.io.Serializable

/**
 * @作者:XLEO
 * @创建日期: 2017/8/25 10:51
 * @描述:${TODO}
 *
 * @更新者:${Author}$
 * @更新时间:${Date}$
 * @更新描述:${TODO}
 * @下一步：
 */

class RefreshLayout(ctx: Context, attr: AttributeSet?) : ViewGroup(ctx, attr) {
    constructor(ctx: Context) : this(ctx, null)

    private var topRefrashViewId: Int = -1
    private var bottomRefrashViewId: Int = -1
    private var mainViewId: Int = -1
    private var topRefrashView: View? = null

    private var bottomView: View? = null

    private var mainView: View? = null

    private var isTopRefrash: Boolean = false
    private var isBottomRefrash: Boolean = false
    var onRefrashListener: OnRefrashListener? = null

    companion object {
        const val STATEDIRECTION = 0x0012.shl(3)
        const val STATEIDLE = 0x0011.shl(3)
        const val STATEDRAGING = 0x0013.shl(3)
        const val STATENOTDRAG = 0x0014.shl(3)

        const val DIR_NONE: Int = 0x1122
        const val DIR_DOWN: Int = 0x1123
        const val DIR_UP: Int = 0x1124
        const val DO_LOG = false
    }

    private var currentState = STATEIDLE


    private var direction: Int = DIR_NONE

    private var offset: Int = 0

    private var doReDispatchEvent: Boolean = false
    private val dragHelper: ViewDragHelper by lazy {
        ViewDragHelper.create(this, 1.0f, object : ViewDragHelper.Callback() {
            override fun tryCaptureView(child: View?, pointerId: Int): Boolean {
                if (child!! == mainView) {
                    return true
                }
                return false
            }

            override fun clampViewPositionVertical(child: View?, top: Int, dy: Int): Int {
                var realTop = top

                if (doSetOffset) {
                    val newTop = top - offset
                    doSetOffset = false
                    realTop = newTop
                    logd("==top:" + realTop + ";dy:" + dy + ";threadId:" + Thread.currentThread().id + ";isTopRefrash:" + isTopRefrash + ";currentState:" + currentState + ";time:" + System.currentTimeMillis())
                }
                logd("top:" + realTop + ";dy:" + dy + ";isTopRefrash:" + isTopRefrash + ";currentState:" + currentState + ";time:" + System.currentTimeMillis())
                if (isTopRefrash && realTop < 0) {
                    realTop = 0
                    doReDispatchEvent = true

                } else if (isBottomRefrash && realTop > 0) {
                    realTop = 0
                    doReDispatchEvent = true
                } else {
                    if (!(isTopRefrashable() && realTop >= 0) && !(isBottomRefrashable() && realTop <= 0)) {
                        realTop = 0
                        doReDispatchEvent = true
                    }
                }
                return realTop
            }

            override fun clampViewPositionHorizontal(child: View?, left: Int, dx: Int): Int {
                return child!!.left
            }

            override fun onViewReleased(releasedChild: View?, xvel: Float, yvel: Float) {
                resetViews()
            }

            override fun onViewPositionChanged(changedView: View?, left: Int, top: Int, dx: Int, dy: Int) {
                // logd("newtop:" + top + ";dy:" + dy)
                locateViews(top)
                if (doReDispatchEvent) {
                    //redispatchEvent()
                    doReDispatchEvent = false
                }
            }
        })

    }

    private fun redispatchEvent() {
        getDirection()
        isDirectionPermited()
        when (currentState) {
            STATEDRAGING -> {
                if (dragHelper.activePointerId == localEvent!!.getPointerId(localEvent!!.actionIndex))
                    dragHelper.processTouchEvent(localEvent)
            }
            STATENOTDRAG -> {
                mainView!!.dispatchTouchEvent(localEvent)
            }
            else -> {
            }
        }
    }

    private fun locateViews(dy: Int) {
        when (dy > 0) {
            true -> {
                if (isBottomRefrash) {
                    bottomMoveBy(dy)
                } else {
                    if (topRefrashView != null) {
                        topMoveBy(dy)
                        isTopRefrash = true
                    }
                }
            }
            else -> {
                if (isTopRefrash) {
                    topMoveBy(dy)
                } else {
                    if (bottomView != null) {
                        bottomMoveBy(dy)
                        isBottomRefrash = true
                    }
                }
            }
        }
    }

    private var isReleaseing: Boolean = false

    override fun computeScroll() {
        if (dragHelper.continueSettling(true)) {
            invalidate()
        } else {
            if (isReleaseing) {
                isBottomRefrash = false
                isTopRefrash = false
                isReleaseing = false
            }
        }
    }

    private fun resetViews() {
        logd("reset")
        isReleaseing = true
        if (mainView != null) {
            dragHelper.settleCapturedViewAt(paddingLeft, paddingTop)
        }
        invalidate()
        if (isTopRefrash) {
            if (topRefrashView!!.translationY > topViewHeight * 1 / 2)
                onRefrashListener?.onTopRefrash()
        }
        if (isBottomRefrash) {
            if ((-bottomView!!.translationY) > bottomViewHeight * 1 / 2)
                onRefrashListener?.onBottomRefrash()
        }
    }

    private fun topMoveBy(dy: Int) {
        if (dy <= 0) {
            topRefrashView!!.translationY = 0f
            //logd("topview:transY:" + topRefrashView!!.translationY)
            onRefrashListener?.onTopViewMove(0)
            isTopRefrash = false
            locateViews(dy)
        } else {
            topRefrashView!!.translationY = dy.toFloat()
            //logd("topview:transY:" + topRefrashView!!.translationY)
            onRefrashListener?.onTopViewMove(dy)
        }
    }

    private fun bottomMoveBy(dy: Int) {
        if (dy >= 0) {
            bottomView!!.translationY = 0f
            onRefrashListener?.onBottomViewMove(0)
            isBottomRefrash = false
            locateViews(dy)
        } else {
            bottomView!!.translationY = dy.toFloat()
            onRefrashListener?.onBottomViewMove(dy)
        }
    }


    init {
        if (attr != null) {
            val attrs = ctx.obtainStyledAttributes(attr!!, R.styleable.RefreshLayout)
            topRefrashViewId = attrs.getResourceId(R.styleable.RefreshLayout_topView, -1)
            bottomRefrashViewId = attrs.getResourceId(R.styleable.RefreshLayout_bottomView, -1)
            mainViewId = attrs.getResourceId(R.styleable.RefreshLayout_mainView, -1)
        }
    }


    fun logd(s: String) {
        if (DO_LOG) {
            Log.d("RefreshLayout", s)
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        dragHelper.shouldInterceptTouchEvent(ev)
        return true
    }


    private fun isDirectionPermited(): Boolean {
        when (mainView) {
            is AdapterView<*> -> {
                val listView = mainView as ListView
                if (listView.adapter == null || listView.adapter.count <= 0) {
                    currentState = STATEDRAGING
                    return true
                }
                if (listView.firstVisiblePosition == 0 && isChildTopVisible(listView.getChildAt(0)) && isTopRefrashable()) {
                    currentState = STATEDRAGING
                    return true

                }
                if (listView.lastVisiblePosition == listView.adapter.count - 1 && isChildBottomVisible(listView.getChildAt(listView.childCount - 1)) && isBottomRefrashable()) {
                    currentState = STATEDRAGING
                    return true
                }
            }
            is RecyclerView -> {
                val recycler = mainView as RecyclerView

                if (recycler.layoutManager is LinearLayoutManager) {
                    if (recycler.adapter == null && direction != DIR_NONE) {
                        currentState = STATEDRAGING
                        return true
                    }
                    val linearManager = recycler.layoutManager as LinearLayoutManager
                    //获取第一个可见view的位置
                    val firstItemPosition = linearManager.findFirstCompletelyVisibleItemPosition()
                    if (firstItemPosition <= 0 && isTopRefrashable()) {
                        changeToDragState()
                        return true
                    }
                    //获取最后一个可见view的位置
                    val lastItemPosition = linearManager.findLastCompletelyVisibleItemPosition()
                    if ((recycler.adapter == null || recycler.adapter.itemCount == 0 || lastItemPosition == recycler.adapter.itemCount - 1) && isBottomRefrashable()) {
                        changeToDragState()
                        return true
                    }

                } else if (recycler.layoutManager is StaggeredGridLayoutManager) {
                    val stagger = recycler.layoutManager as StaggeredGridLayoutManager
                    if (stagger.findViewByPosition(0).windowVisibility != View.GONE && isTopRefrashable()) {
                        changeToDragState()
                        return true
                    } else if ((recycler.adapter == null || recycler.adapter.itemCount == 0 || stagger.findViewByPosition(recycler.adapter.itemCount - 1).windowVisibility != View.GONE) && isBottomRefrashable()) {
                        changeToDragState()
                        return true
                    }
                }
            }
            is ViewGroup -> {
                val view = mainView as ViewGroup
                if (view.childCount <= 0) {
                    if (direction != DIR_NONE) {
                        changeToDragState()
                        return true
                    } else {
                        changeToNotDragState()
                        return false
                    }
                }
                if (view.getChildAt(0).windowVisibility != View.GONE && isTopRefrashable()) {
                    changeToDragState()
                    return true
                } else if (view.getChildAt(view.childCount - 1).windowVisibility != View.GONE && isBottomRefrashable()) {
                    changeToDragState()
                    return true
                }
            }
            else -> {
                changeToDragState()
                return true
            }
        }
        changeToNotDragState()
        return false
    }


    private fun isChildTopVisible(childAt: View?): Boolean {
        if (childAt == null) {
            return false
        }
        return try {
            val mainRect = Rect()
            mainView!!.getGlobalVisibleRect(mainRect)
            val rect = Rect()
            childAt.getGlobalVisibleRect(rect)
            rect.top >= mainRect.top
        } catch (e: Throwable) {
            true
        }
    }

    private fun isChildBottomVisible(childAt: View?): Boolean {
        if (childAt == null) {
            return false
        }
        return try {
            val mainRect = Rect()
            mainView!!.getGlobalVisibleRect(mainRect)
            val rect = Rect()
            childAt.getGlobalVisibleRect(rect)
            rect.bottom <= mainRect.bottom
        } catch (e: Throwable) {
            true
        }
    }

    private fun changeToNotDragState() {
        currentState = STATENOTDRAG
    }

    private fun isBottomRefrashable(): Boolean {
        if (bottomView == null) {
            return false
        } else if (bottomView!!.translationY < 0 || direction == DIR_UP) {
            return true
        } else {
            return false
        }
    }

    private fun isTopRefrashable(): Boolean {
        if (topRefrashView == null) {
            return false
        } else if (direction == DIR_DOWN || (topRefrashView != null && topRefrashView!!.translationY > 0)) {
            return true
        } else {
            return false
        }
    }

    private val eventLog: EventLogHolder by lazy {
        EventLogHolder()
    }

    class EventLogHolder {
        var start: MotionEventBean? = null
        var last: MotionEventBean? = null
        var current: MotionEventBean? = null
        fun initStart(event: MotionEvent) {
            start = MotionEventBean(event.actionMasked, event.x, event.y)
            addEvent(event)
        }

        fun addEvent(event: MotionEvent) {
            if (current != null) {
                moveCurrentToLast()
                current!!.updateDatas(event)
            } else {
                current = MotionEventBean(event.actionMasked, event.x, event.y)
            }
        }

        private fun moveCurrentToLast() {
            if (last == null) {
                last = MotionEventBean(current!!.type, current!!.x, current!!.y)
            } else {
                last!!.updateDatas(current!!)
            }
        }

        fun clearHolder() {
            start = null
            last = null
            current = null
        }
    }

    private var localEvent: MotionEvent? = null
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                eventLog.initStart(event)
                if (isReleaseing) {
                    changeToDragState()
                    dragHelper.processTouchEvent(event)
                } else {
                    isReleaseing = false
                    mainView!!.dispatchTouchEvent(event)
                    dragHelper.processTouchEvent(event)
                    currentState = STATEDIRECTION
                    logd("down" + ";threadId:" + Thread.currentThread().id)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.actionIndex == 0) {
                    eventLog.addEvent(event)
                    localEvent = event
                    redispatchEvent()
                    isReleaseing = false
//                logd("move")
                }
            }
            MotionEvent.ACTION_UP -> {
                if (currentState == STATEDRAGING) {
                    dragHelper.processTouchEvent(event)
                } else if (currentState == STATENOTDRAG) {
                    mainView!!.dispatchTouchEvent(event)
                } else {
                    mainView!!.dispatchTouchEvent(event)
                }
                currentState = STATEIDLE
                direction = DIR_NONE
                doSetOffset = false
                eventLog.clearHolder()
                logd("up" + ";threadId:" + Thread.currentThread().id)
            }
            else -> {
                logd("else_state" + event!!.action)
            }
        }
        return true
    }

    private fun changeToDragState() {
        if (currentState == STATENOTDRAG) {
            calcuteOffset()
        }
        currentState = STATEDRAGING
    }

    private var doSetOffset: Boolean = false

    fun calcuteOffset() {

        offset = ((eventLog.last!!.y - eventLog.start!!.y) + 0.5f).toInt()
        if (direction == DIR_DOWN && offset > 0) {
            doSetOffset = true
            logd("offset:" + offset + ";threadId:" + Thread.currentThread().id + ":startY:" + eventLog.start!!.y + ";currY:" + eventLog.last!!.y + ";dir:" + direction + ";time:" + System.currentTimeMillis())
        } else if (direction == DIR_UP && offset < 0) {
            doSetOffset = true
            logd("offset:" + offset + ";threadId:" + Thread.currentThread().id + ":startY:" + eventLog.start!!.y + ";currY:" + eventLog.last!!.y + ";dir:" + direction + ";time:" + System.currentTimeMillis())
        } else {
            doSetOffset = false
        }
    }

    private var isDirectionChanged: Boolean = false

    private fun getDirection() {
        if (eventLog.last != null) {
            var newDirection = DIR_NONE
            val abs = Math.abs(eventLog.current!!.y - eventLog.last!!.y)
            if (abs == 0.0f) {
                newDirection = DIR_NONE
            } else {
                val fl = Math.abs(eventLog.current!!.x - eventLog.last!!.x) / abs

                if (fl <= 0.5f) {
                    if (eventLog.last!!.y > eventLog.current!!.y) {
                        newDirection = DIR_UP
                    } else {
                        newDirection = DIR_DOWN
                    }
                } else {
                    newDirection = DIR_NONE
                }
            }
            if (direction != newDirection) {
                isDirectionChanged = true
                direction = newDirection
            } else {
                isDirectionChanged = false
            }
        }
    }

    private var topViewHeight: Int = 0

    private var bottomViewHeight: Int = 0

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        measureChildren(widthMeasureSpec, heightMeasureSpec)
        if (topRefrashView != null) {
            topViewHeight = topRefrashView!!.measuredHeight
        }
        if (bottomView != null) {
            bottomViewHeight = bottomView!!.measuredHeight
        }
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (topRefrashView != null) {
            topRefrashView!!.layout(0, -topViewHeight, measuredWidth, 0)
        }
        if (mainView != null) {
            mainView!!.layout(paddingLeft, paddingTop, measuredWidth - paddingRight, measuredHeight - paddingBottom)
        }
        if (bottomView != null) {
            bottomView!!.layout(0, measuredHeight, measuredWidth, measuredHeight + bottomViewHeight)
        }
    }

    override fun onFinishInflate() {
        if (childCount > 0) {
            for (i in 0..childCount - 1) {
                when (getChildAt(i).id) {
                    -1 -> {
                        throw IllegalArgumentException("child with NOID is not supported")
                    }
                    topRefrashViewId -> {
                        topRefrashView = getChildAt(i)
                    }
                    bottomRefrashViewId -> {
                        bottomView = getChildAt(i)
                    }
                    mainViewId -> {
                        mainView = getChildAt(i)
                    }
                    else -> {
                    }
                }
            }
        }
        super.onFinishInflate()

    }
}

interface OnRefrashListener {
    fun onTopRefrash()
    fun onBottomRefrash()
    fun onTopViewMove(transY: Int)
    fun onBottomViewMove(transY: Int)
}

abstract class OnRefrashAdapter : OnRefrashListener {
    override fun onBottomViewMove(transY: Int) {
    }

    override fun onTopViewMove(transY: Int) {
    }
}

data class MotionEventBean(var type: Int, var x: Float, var y: Float) : Serializable {
    fun updateDatas(event: MotionEvent) {
        type = event.actionMasked
        x = event.x
        y = event.y
    }

    fun updateDatas(event: MotionEventBean) {
        type = event.type
        x = event.x
        y = event.y
    }
}