package com.example.myapplication

import android.graphics.drawable.Drawable
import android.os.UserHandle

/**
 * AppModel: A clean data representation of an installed application.
 * 
 * Unlike the system's ResolveInfo, this model is lightweight and contains 
 * only the necessary information for the Launcher UI.
 * 
 * @property label The display name of the app (e.g., "Chrome").
 * @property packageName The unique package identifier (e.g., "com.android.chrome").
 * @property icon The drawable icon for the app.
 * @property userHandle Represents the user profile (e.g., Personal vs. Work).
 * @property lastUpdated A timestamp used to force UI refreshes via DiffUtil.
 */
data class AppModel(
    val label: String,
    val packageName: String,
    val icon: Drawable,
    val userHandle: UserHandle,
    val lastUpdated: Long = 0L
)
