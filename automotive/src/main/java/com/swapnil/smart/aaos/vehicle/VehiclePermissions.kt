package com.swapnil.smart.aaos.vehicle

import android.content.Context
import android.content.pm.PackageManager

/**
 * The car permissions this app can actually obtain at runtime, and the ones it
 * cannot.
 *
 * Protection levels as declared by com.android.car.updatable on AAOS API 35
 * (`adb shell dumpsys package permissions`, field `prot=`):
 *
 *   CAR_INFO, CAR_POWERTRAIN            normal      -> granted at install
 *   CAR_SPEED, CAR_ENERGY               dangerous   -> must be requested
 *   CAR_ENGINE_DETAILED, CAR_MILEAGE,
 *   CAR_IDENTIFICATION                  privileged  -> /system/priv-app only
 *
 * The dangerous pair is the interesting one: it is easy to assume everything
 * interesting in the VHAL is privileged and give up, but speed and fuel level
 * are ordinary runtime permissions. They just have to be asked for, exactly
 * like location. Until then CarPropertyManager reports the property as
 * unsupported rather than throwing, which is why the app silently falls back
 * to simulated values.
 *
 * Two traps worth knowing when checking this over adb on AAOS:
 *
 *  - The driver is user 10, not user 0. `pm grant <pkg> <perm>` targets user 0
 *    and appears to succeed while changing nothing for the running app;
 *    `pm grant --user 10 ...` is the one that works.
 *  - The automotive module has no Activity, so the request has to go through
 *    CarContext.requestPermissions(), which asks the car host to show the
 *    dialog on the head unit.
 */
object VehiclePermissions {

    /** Runtime (dangerous) car permissions: requestable by any installed app. */
    val RUNTIME: List<String> = listOf(
        "android.car.permission.CAR_SPEED",
        "android.car.permission.CAR_ENERGY"
    )

    /**
     * Declared in the manifest but only grantable to a privileged system app.
     * Listed here so the UI can explain *why* RPM, odometer and VIN stay
     * simulated instead of pretending the data is unavailable for no reason.
     */
    val PRIVILEGED: List<String> = listOf(
        "android.car.permission.CAR_ENGINE_DETAILED",
        "android.car.permission.CAR_MILEAGE",
        "android.car.permission.CAR_IDENTIFICATION"
    )

    fun isGranted(context: Context, permission: String): Boolean =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    /** Runtime permissions still missing; empty means there is nothing to ask for. */
    fun missingRuntime(context: Context): List<String> =
        RUNTIME.filter { !isGranted(context, it) }

    fun grantedPrivileged(context: Context): List<String> =
        PRIVILEGED.filter { isGranted(context, it) }

    /** Short label for a permission, for log lines and UI rows. */
    fun shortName(permission: String): String = permission.substringAfterLast('.')
}
