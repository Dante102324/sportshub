package com.dante.sportshuub

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity

// Shared layout for the team, player and league pages:
// back button + logo + title, a row of tabs, and a scrolling content area.
class PageScaffold(private val activity: FragmentActivity) {

    val root = LinearLayout(activity)

    val titleView = TextView(activity)
    val subtitleView = TextView(activity)
    val logoView = ImageView(activity)

    val content = LinearLayout(activity)

    private val tabViews = mutableListOf<TextView>()
    private var selectedTab = 0
    private var onTabSelected: (Int) -> Unit = {}

    private val gray = Color.rgb(180, 180, 185)

    init {
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(TvUi.backgroundColor)
        root.setPadding(dp(50), dp(30), dp(50), dp(24))
    }

    // Call once, in this order, then setContentView(root).
    fun build(
        title: String,
        subtitle: String,
        tabs: List<String>,
        onTab: (Int) -> Unit
    ) {
        onTabSelected = onTab

        // ---- Header ----

        val header = LinearLayout(activity)
        header.orientation = LinearLayout.HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL

        header.addView(
            chip("←  BACK") { activity.finish() },
            LinearLayout.LayoutParams(-2, dp(48)).apply { setMargins(0, 0, dp(24), 0) }
        )

        logoView.scaleType = ImageView.ScaleType.CENTER_INSIDE
        header.addView(
            logoView,
            LinearLayout.LayoutParams(dp(76), dp(76)).apply { setMargins(0, 0, dp(18), 0) }
        )

        val texts = LinearLayout(activity)
        texts.orientation = LinearLayout.VERTICAL

        titleView.text = title
        titleView.textSize = 30f
        titleView.typeface = Typeface.DEFAULT_BOLD
        titleView.setTextColor(Color.WHITE)
        titleView.maxLines = 1

        subtitleView.text = subtitle
        subtitleView.textSize = 16f
        subtitleView.setTextColor(gray)
        subtitleView.maxLines = 1

        texts.addView(titleView)
        texts.addView(subtitleView)

        header.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))

        root.addView(header, LinearLayout.LayoutParams(-1, dp(84)))

        // ---- Tabs ----

        val bar = LinearLayout(activity)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.setPadding(0, dp(10), 0, dp(10))

        for (i in tabs.indices) {
            val tab = TextView(activity)

            tab.text = tabs[i]
            tab.textSize = 16f
            tab.typeface = Typeface.DEFAULT_BOLD
            tab.gravity = Gravity.CENTER
            tab.setPadding(dp(18), dp(10), dp(18), dp(10))
            tab.isFocusable = true
            tab.isFocusableInTouchMode = true

            tab.setOnClickListener { select(i) }
            tab.setOnFocusChangeListener { _, _ -> styleTabs() }

            tabViews.add(tab)
            bar.addView(
                tab,
                LinearLayout.LayoutParams(-2, -2).apply { setMargins(0, 0, dp(8), 0) }
            )
        }

        root.addView(bar)
        styleTabs()

        // ---- Content ----

        content.orientation = LinearLayout.VERTICAL

        val scroll = ScrollView(activity)
        scroll.isFocusable = false
        scroll.addView(content)

        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
    }

    fun select(index: Int) {
        selectedTab = index
        styleTabs()
        onTabSelected(index)
    }

    val currentTab: Int get() = selectedTab

    private fun styleTabs() {
        for (i in tabViews.indices) {
            val tab = tabViews[i]
            val bg = GradientDrawable()

            bg.cornerRadius = dp(22).toFloat()
            bg.setColor(if (i == selectedTab) Color.rgb(65, 65, 72) else Color.TRANSPARENT)
            if (tab.isFocused) bg.setStroke(dp(3), Color.WHITE)

            tab.background = bg
            tab.setTextColor(if (i == selectedTab) Color.WHITE else gray)
        }
    }

    // ---------------------------------------------------------
    // Content helpers
    // ---------------------------------------------------------

    fun showMessage(text: String) {
        content.removeAllViews()
        content.addView(message(text))
    }

    fun message(text: String): TextView {
        val view = TextView(activity)
        view.text = text
        view.textSize = 18f
        view.setTextColor(gray)
        view.setPadding(0, dp(20), 0, dp(20))
        return view
    }

    // Draws the tables and returns the body rows in order (for focusing one).
    fun showTables(
        tables: List<SimpleTable>,
        onRow: ((SimpleRow) -> Unit)? = null
    ): List<View> {

        content.removeAllViews()

        val rows = mutableListOf<View>()
        for (table in tables) {
            rows.addAll(addTable(table, onRow))
        }
        return rows
    }

    private fun addTable(table: SimpleTable, onRow: ((SimpleRow) -> Unit)?): List<View> {

        if (table.title.isNotEmpty()) {
            val title = TextView(activity)
            title.text = table.title
            title.textSize = 19f
            title.typeface = Typeface.DEFAULT_BOLD
            title.setTextColor(Color.WHITE)
            title.setPadding(dp(8), dp(14), 0, dp(6))
            content.addView(title)
        }

        // Fit the stat columns to the screen width.
        val screenDp = activity.resources.displayMetrics.widthPixels /
            activity.resources.displayMetrics.density
        val labelDp = 230f
        val columns = table.columns.size.coerceAtLeast(1)
        val cellDp = ((screenDp - 100f - labelDp) / columns).coerceIn(44f, 90f).toInt()

        content.addView(buildRow("", table.columns, cellDp, header = true))

        val bodyRows = mutableListOf<View>()

        for (row in table.rows) {
            val view = buildRow(row.label, row.values, cellDp, header = false)

            if (onRow != null) {
                view.setOnClickListener { onRow(row) }
            }

            content.addView(view)
            bodyRows.add(view)
        }
        return bodyRows
    }

    private fun buildRow(
        label: String,
        values: List<String>,
        cellDp: Int,
        header: Boolean
    ): LinearLayout {

        val row = LinearLayout(activity)

        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(dp(8), dp(4), dp(8), dp(4))

        val color = if (header) gray else Color.WHITE

        val name = TextView(activity)
        name.text = label
        name.textSize = 17f
        name.setTextColor(color)
        name.maxLines = 1
        name.ellipsize = android.text.TextUtils.TruncateAt.END
        if (header) name.typeface = Typeface.DEFAULT_BOLD
        row.addView(name, LinearLayout.LayoutParams(0, dp(38), 1f))

        for (value in values) {
            val cell = TextView(activity)
            cell.text = value
            cell.textSize = 16f
            cell.gravity = Gravity.CENTER
            cell.setTextColor(color)
            cell.maxLines = 1
            if (header) cell.typeface = Typeface.DEFAULT_BOLD
            row.addView(cell, LinearLayout.LayoutParams(dp(cellDp), dp(38)))
        }

        if (!header) {
            row.isFocusable = true
            row.isFocusableInTouchMode = true
            styleRow(row, false)
            row.setOnFocusChangeListener { v, hasFocus -> styleRow(v, hasFocus) }
        }
        return row
    }

    private fun styleRow(view: View, focused: Boolean) {
        val bg = GradientDrawable()
        bg.cornerRadius = dp(8).toFloat()
        bg.setColor(if (focused) Color.rgb(50, 50, 58) else Color.TRANSPARENT)
        if (focused) bg.setStroke(dp(2), Color.WHITE)
        view.background = bg
    }

    // ---------------------------------------------------------
    // Images
    // ---------------------------------------------------------

    // Loads any picture (logo or headshot) through the logo cache.
    fun loadImage(url: String, target: ImageView) {

        if (!url.startsWith("http")) return

        val key = "page_" + url.hashCode().toString().replace("-", "m")

        val team = Team(
            id = key,
            name = key,
            abbreviation = key,
            sport = "",
            league = "",
            logoFileName = key,
            logoUrl = url
        )

        LogoManager.loadLogo(activity, team) { bitmap ->
            if (bitmap != null) target.setImageBitmap(bitmap)
        }
    }

    fun chip(text: String, onClick: () -> Unit): TextView {

        val view = TextView(activity)

        view.text = text
        view.textSize = 16f
        view.typeface = Typeface.DEFAULT_BOLD
        view.setTextColor(Color.WHITE)
        view.gravity = Gravity.CENTER
        view.setPadding(dp(22), 0, dp(22), 0)
        view.isFocusable = true
        view.isFocusableInTouchMode = true

        fun style(focused: Boolean) {
            view.background = GradientDrawable().apply {
                setColor(Color.rgb(38, 38, 43))
                cornerRadius = dp(26).toFloat()
                if (focused) setStroke(dp(3), Color.WHITE)
            }
        }

        style(false)
        view.setOnFocusChangeListener { _, hasFocus -> style(hasFocus) }
        view.setOnClickListener { onClick() }

        return view
    }

    fun dp(value: Int): Int = TvUi.dp(activity, value)
}
