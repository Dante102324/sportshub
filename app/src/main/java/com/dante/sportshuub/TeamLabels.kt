package com.dante.sportshuub

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan

object TeamLabels {

    // "ALA" or, for a ranked college team, a small "#3" in front: "#3 ALA".
    fun withRank(name: String, rank: Int?): CharSequence {
        if (rank == null) return name

        val prefix = "#$rank "
        val text = SpannableString(prefix + name)

        text.setSpan(
            RelativeSizeSpan(0.7f),
            0,
            prefix.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        text.setSpan(
            ForegroundColorSpan(Color.LTGRAY),
            0,
            prefix.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return text
    }
}
