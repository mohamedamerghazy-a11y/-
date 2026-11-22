package com.example.core.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import com.example.MainActivity
import com.example.R
import com.example.data.model.AppIconTheme

/**
 * Manages switching the Android home-screen launcher icon dynamically across the 4 pre-packaged
 * `<activity-alias>` entries declared in `AndroidManifest.xml` (Default, Offers, Ramadan, Eid).
 */
object AppIconManager {

  private val aliasPackagePrefix: String
    get() = MainActivity::class.java.`package`?.name ?: "com.example"

  fun componentNameFor(context: Context, theme: AppIconTheme): ComponentName =
    ComponentName(context.packageName, "$aliasPackagePrefix.${theme.aliasSimpleName}")

  @DrawableRes
  fun previewDrawableFor(theme: AppIconTheme): Int = when (theme) {
    AppIconTheme.DEFAULT -> R.drawable.app_logo
    AppIconTheme.OFFERS -> R.drawable.ic_launcher_offers_art
    AppIconTheme.RAMADAN -> R.drawable.ic_launcher_ramadan_art
    AppIconTheme.EID -> R.drawable.ic_launcher_eid_art
  }

  fun currentActiveTheme(context: Context): AppIconTheme {
    val pm = context.applicationContext.packageManager
    // First look for an explicitly ENABLED alias
    for (theme in AppIconTheme.entries) {
      val state = runCatching {
        pm.getComponentEnabledSetting(componentNameFor(context, theme))
      }.getOrDefault(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
      if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
        return theme
      }
    }
    // Otherwise default alias is enabled in manifest by default
    return AppIconTheme.DEFAULT
  }

  /**
   * Switches the active launcher `<activity-alias>` to [targetTheme] if not already active.
   * Always enables the target alias FIRST before disabling the others so the launcher never sees
   * zero active entry points.
   *
   * @return `true` if a change was applied, `false` if [targetTheme] was already active or failed.
   */
  fun applyIconTheme(context: Context, targetTheme: AppIconTheme): Boolean {
    val appCtx = context.applicationContext
    if (currentActiveTheme(appCtx) == targetTheme) {
      return false
    }

    val pm = appCtx.packageManager
    return runCatching {
      // 1. Enable the new alias first so there is always at least one active launcher icon
      pm.setComponentEnabledSetting(
        componentNameFor(appCtx, targetTheme),
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
        PackageManager.DONT_KILL_APP,
      )

      // 2. Disable all other aliases
      for (theme in AppIconTheme.entries) {
        if (theme != targetTheme) {
          pm.setComponentEnabledSetting(
            componentNameFor(appCtx, theme),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
          )
        }
      }
      true
    }.getOrDefault(false)
  }
}
