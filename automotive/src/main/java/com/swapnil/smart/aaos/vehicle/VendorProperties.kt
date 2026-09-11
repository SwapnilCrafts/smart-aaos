package com.swapnil.smart.aaos.vehicle

/**
 * Custom vendor VHAL properties added to the emulator by this project.
 *
 * A vehicle property ID is not an arbitrary number. It is a bit field, and the
 * VHAL derives the property's group, area and data type from it:
 *
 *   0x21400001
 *     2_______   group  VENDOR (0x20000000); SYSTEM is 0x10000000
 *     _1______   area   GLOBAL (0x01000000); SEAT, DOOR, WINDOW etc. exist too
 *     __4_____   type   INT32  (0x00400000); FLOAT 0x00600000, STRING 0x00100000
 *     ___00001   id     our own unique number within the vendor group
 *
 * That layout is why the type has to be right: the VHAL reads the type out of
 * the ID itself, so asking for an INT32 property as a float does not fail with
 * a clear error, it simply does not match anything.
 *
 * The VENDOR group is the part an OEM owns. Anything under 0x20000000 is free
 * for a manufacturer to define for their own hardware, and Google will never
 * assign a conflicting meaning to it. This is how a real car exposes things
 * the standard VHAL has no property for - a specific drive mode, a
 * manufacturer's battery health metric, a service interval.
 *
 * Defined on the device in
 *   /vendor/etc/automotive/vhalconfig/SmartAaosVendorProperties.json
 * and installed by tools/install-vendor-properties.sh.
 *
 * Reading any of these requires CAR_VENDOR_EXTENSION, which is
 * signature|privileged - so it only works when the app is installed as a
 * privileged system app. See FINDINGS.md.
 */
object VendorProperties {

    private const val GROUP_VENDOR = 0x20000000
    private const val AREA_GLOBAL = 0x01000000
    private const val TYPE_INT32 = 0x00400000
    private const val TYPE_FLOAT = 0x00600000
    private const val TYPE_STRING = 0x00100000

    private fun vendorId(type: Int, uniqueId: Int): Int =
        GROUP_VENDOR or AREA_GLOBAL or type or uniqueId

    /** Drive mode, writable: the app can change it and the VHAL stores it. */
    val DRIVE_MODE: Int = vendorId(TYPE_INT32, 0x0001)

    /** Distance until the next service, read-only. */
    val SERVICE_DUE_KM: Int = vendorId(TYPE_FLOAT, 0x0002)

    /** Manufacturer's own battery health grade, read-only. */
    val BATTERY_HEALTH: Int = vendorId(TYPE_STRING, 0x0003)

    /** Permission gating every vendor property. */
    const val PERMISSION = "android.car.permission.CAR_VENDOR_EXTENSION"

    const val DRIVE_MODE_ECO = 0
    const val DRIVE_MODE_NORMAL = 1
    const val DRIVE_MODE_SPORT = 2

    fun driveModeLabel(mode: Int?): String = when (mode) {
        DRIVE_MODE_ECO -> "Eco"
        DRIVE_MODE_NORMAL -> "Normal"
        DRIVE_MODE_SPORT -> "Sport"
        null -> "unavailable"
        else -> "unknown ($mode)"
    }

    /** Cycles Eco -> Normal -> Sport -> Eco. */
    fun nextDriveMode(current: Int?): Int = when (current) {
        DRIVE_MODE_ECO -> DRIVE_MODE_NORMAL
        DRIVE_MODE_NORMAL -> DRIVE_MODE_SPORT
        else -> DRIVE_MODE_ECO
    }
}
