package com.wendellugalds.kingofbozo.util

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.core.view.WindowCompat
import android.view.Window
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.color.MaterialColors
import com.wendellugalds.kingofbozo.R

object SystemBarUtil {

    fun applySystemBarColors(
        window: Window,
        root: View,
        statusBarAttr: Int = R.attr.customBackground,
        navBarAttr: Int = R.attr.customBackground
    ) {
        val statusBarColor = MaterialColors.getColor(root, statusBarAttr)
        val navBarColor = MaterialColors.getColor(root, navBarAttr)
        setSystemBarColors(window, root, statusBarColor, navBarColor)
    }

    fun setSystemBarColors(window: Window, root: View, statusBarColor: Int, navBarColor: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            window.statusBarColor = statusBarColor
            window.navigationBarColor = navBarColor
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        val controller = WindowInsetsControllerCompat(window, root)
        controller.isAppearanceLightStatusBars = isColorLight(statusBarColor)
        controller.isAppearanceLightNavigationBars = isColorLight(navBarColor)
    }

    private fun isColorLight(color: Int): Boolean {
        val darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness < 0.5
    }
}
