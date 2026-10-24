package com.dante.sportshuub

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.provider.Settings
import android.util.Log

// Sports Hub as the TV's home screen.
//
// While the app is the device's home app, the Home button and boot go to it.
// "Exit to TV home menu" (in Settings) switches that off until Sports Hub is
// opened again from the TV's own menu, or the device restarts.
object LauncherMode {

    private const val PREFS = "sports_hub_launcher"
    private const val KEY_EXITED = "exited"
    private const val KEY_BOOT_COUNT = "exit_boot_count"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun bootCount(context: Context): Int =
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)

    // True after "Exit to TV home menu", until the app is opened again or the
    // device restarts (a restart goes back to Sports Hub).
    fun isExited(context: Context): Boolean {
        val p = prefs(context)
        if (!p.getBoolean(KEY_EXITED, false)) return false
        return p.getInt(KEY_BOOT_COUNT, -2) == bootCount(context)
    }

    fun markExited(context: Context) {
        prefs(context).edit()
            .putBoolean(KEY_EXITED, true)
            .putInt(KEY_BOOT_COUNT, bootCount(context))
            .apply()
    }

    fun clearExited(context: Context) {
        prefs(context).edit().putBoolean(KEY_EXITED, false).apply()
    }

    // The TV's own home menu: any home app other than this one.
    private fun findSystemHome(context: Context): ResolveInfo? {

        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)

        val all = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.queryIntentActivities(
                homeIntent,
                PackageManager.ResolveInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.queryIntentActivities(homeIntent, 0)
        }

        val others = all.filter { it.activityInfo.packageName != context.packageName }

        return others.firstOrNull { it.activityInfo.packageName.contains("launcher", true) }
            ?: others.firstOrNull()
    }

    // Opens the TV's home menu. Returns false if the device has none.
    fun openSystemHome(context: Context): Boolean {

        val target = findSystemHome(context) ?: return false

        return try {
            context.startActivity(
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .setComponent(
                        ComponentName(target.activityInfo.packageName, target.activityInfo.name)
                    )
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        } catch (e: Exception) {
            Log.e("LauncherMode", "Couldn't open the TV home menu: ${e.message}")
            false
        }
    }
}
