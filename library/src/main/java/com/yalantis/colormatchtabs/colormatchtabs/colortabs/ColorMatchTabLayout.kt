package com.yalantis.colormatchtabs.colormatchtabs.colortabs

import android.content.Context
import android.content.res.Configuration
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.WindowManager
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import com.yalantis.colormatchtabs.colormatchtabs.MenuToggleListener
import com.yalantis.colormatchtabs.colormatchtabs.R
import com.yalantis.colormatchtabs.colormatchtabs.listeners.OnColorTabSelectedListener
import com.yalantis.colormatchtabs.colormatchtabs.menu.ArcMenu
import com.yalantis.colormatchtabs.colormatchtabs.model.ColorTab
import com.yalantis.colormatchtabs.colormatchtabs.utils.getDimen

/**
 * Created by anna on 10.05.17.
 */
class ColorMatchTabLayout : HorizontalScrollView, MenuToggleListener {

    companion object {
        private const val INVALID_WIDTH = -1
    }

    internal lateinit var tabStrip: SlidingTabStrip
    internal var tabs: MutableList<ColorTab> = mutableListOf()
    private var tabSelectedListener: OnColorTabSelectedListener? = null
    internal var selectedTab: ColorTab? = null
    internal var tabMaxWidth = Integer.MAX_VALUE
    internal var previousSelectedTab: ColorTabView? = null

    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initLayout(attrs, defStyleAttr)
    }

    private fun initLayout(attrs: AttributeSet?, defStyleAttr: Int) {
        isHorizontalScrollBarEnabled = false
        tabStrip = SlidingTabStrip(context)
        super.addView(tabStrip, 0, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ColorMatchTabLayout)
        initViewTreeObserver(typedArray)
    }

    private fun initViewTreeObserver(typedArray: TypedArray) {
        typedArray.getDimensionPixelSize(R.styleable.TabLayout_tabMinWidth, INVALID_WIDTH)
        typedArray.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, originHeightMeasureSpec: Int) {
        var heightMeasureSpec = originHeightMeasureSpec
        // If we have a MeasureSpec which allows us to decide our height, try and use the default
        // height
        val idealHeight = (getDimen(R.dimen.default_height) + paddingTop + paddingBottom)
        when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.AT_MOST -> heightMeasureSpec = MeasureSpec.makeMeasureSpec(
                    Math.min(idealHeight, MeasureSpec.getSize(heightMeasureSpec)), MeasureSpec.EXACTLY)
            MeasureSpec.UNSPECIFIED -> heightMeasureSpec = MeasureSpec.makeMeasureSpec(idealHeight, MeasureSpec.EXACTLY)
        }

        if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            // If we don't have an unspecified width spec, use the given size to calculate
            // the max tab width
            val systemService = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val selectTabWidth = if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) getDimen(R.dimen.tab_max_width) else getDimen(R.dimen.tab_max_width_horizontal)
            val probable = (systemService.defaultDisplay.width - selectTabWidth) / (tabs.size - 1)
            tabMaxWidth = if (probable < getDimen(R.dimen.default_width)) getDimen(R.dimen.default_width) else probable

        }

        // Now super measure itself using the (possibly) modified height spec
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

    }

    /**
     * Method add color tab to this layout. The tab will be added at the end of the list.
     * If this is the first tab to be added it will become the selected tab.
     * @param tab ColorTab to add
     */
    fun addTab(tab: ColorTab) {
        tab.isSelected = tabs.isEmpty()
        if (tab.isSelected) {
            selectedTab = tab
        }
        addColorTabView(tab)
    }

    private fun addColorTabView(tab: ColorTab) {
        configureTab(tab, tabs.size)
        tabStrip.addView(tab.tabView, tab.position, createLayoutParamsForTabs())
    }

    /**
     * Create and return new {@link ColorTab}. You need to manually add this using
     * {@link #addTab(ColorTab)} or a related method. For customize created tab use {@link ColorTabAdapter}
     *
     * @return A new ColorTab
     * @see #addTab(ColorTab)
     */
    fun newTab() = ColorTab().apply {
        tabView = createTabView(this)
    }

    private fun createTabView(tab: ColorTab) = ColorTabView(context).apply {
        this.tab = tab
    }

    private fun configureTab(tab: ColorTab, position: Int) {
        tab.position = position
        tabs.add(position, tab)

        val count = tabs.size
        for (i in position + 1..count - 1) {
            tabs.get(i).position = i
        }
    }

    private fun createLayoutParamsForTabs(): LinearLayout.LayoutParams {
        val lp = LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        updateTabViewLayoutParams(lp)
        return lp
    }

    private fun updateTabViewLayoutParams(lp: LinearLayout.LayoutParams) {
        lp.width = LinearLayout.LayoutParams.WRAP_CONTENT
    }

    /**
     * Returns the number of tabs currently registered in layout.
     *
     *  @return ColorTab count
     */
    fun count() = tabs.size

    /**
     * Returns the tab at the specified index.
     */
    fun getTabAt(index: Int): ColorTab? {
        if (index < 0 || index >= count()) {
            return null
        } else {
            return tabs[index]
        }
    }

    internal fun setScrollPosition(position: Int, positionOffset: Float, updateSelectedText: Boolean) {
        val roundedPosition = Math.round(position + positionOffset)
        if (roundedPosition < 0 || roundedPosition >= tabStrip.childCount) {
            return
        }

        // Update the 'selected state' view as we scroll, if enabled
        if (updateSelectedText) {
            setSelectedTabView(roundedPosition)
        }
    }

    fun addOnColorTabSelectedListener(tabSelectedListener: OnColorTabSelectedListener) {
        this.tabSelectedListener = tabSelectedListener
    }

    private fun setSelectedTabView(position: Int) {
        val tabCount = tabStrip.childCount
        if (position < tabCount) {
            (0..tabCount - 1).map {
                val child = tabStrip.getChildAt(it)
                child.isSelected = it == position
            }
        }
    }

    fun setSelectedTab(position: Int) {
        select(getTabAt(position))
    }

    internal fun select(colorTab: ColorTab?) {
        if (colorTab == selectedTab) {
            return
        } else {
            previousSelectedTab = getSelectedTabView()
            selectedTab?.isSelected = false
            selectedTab = colorTab
            selectedTab?.isSelected = true
        }
        tabSelectedListener?.onSelectedTab(colorTab)
    }

    internal fun getSelectedTabView() = tabStrip.getChildAt(selectedTab?.position ?: 0) as ColorTabView?

    fun addArcMenu(arcMenu: ArcMenu) = arcMenu.apply {
        arcMenu.listOfTabs = tabs
        arcMenu.menuToggleListener = tabStrip.menuToggleListener
    }

    override fun onOpenMenu() = tabStrip.onOpenMenu()

    override fun onCloseMenu() = tabStrip.onCloseMenu()

}