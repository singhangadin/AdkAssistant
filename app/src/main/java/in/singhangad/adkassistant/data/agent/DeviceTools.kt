package `in`.singhangad.adkassistant.data.agent

import android.os.Build
import com.google.adk.kt.annotations.Param
import com.google.adk.kt.annotations.Tool
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Plain Kotlin functions the agent is allowed to call.
 *
 * Each `@Tool` method becomes a function the model can invoke; its KDoc and the
 * `@Param` descriptions are what the model reads to decide *when* and *how* to
 * call it. The KSP processor turns these into `generatedTools()` at compile time.
 */
class DeviceTools {

    /**
     * Returns the current wall-clock time for a given IANA time-zone id.
     *
     * @param timeZoneId an IANA time-zone id such as "Asia/Kolkata" or
     *   "America/New_York". Defaults to the device's own zone when blank.
     */
    @Tool
    fun getCurrentTime(
        @Param("IANA time-zone id, e.g. 'Asia/Kolkata'. Empty for the device zone.")
        timeZoneId: String,
    ): Map<String, String> {
        val zone = if (timeZoneId.isBlank()) TimeZone.getDefault()
        else TimeZone.getTimeZone(timeZoneId)
        val formatter = SimpleDateFormat("EEE, dd MMM yyyy, HH:mm:ss", Locale.US)
            .apply { timeZone = zone }
        return mapOf(
            "timeZone" to zone.id,
            "time" to formatter.format(Date()),
        )
    }

    /** Returns basic information about the device the app is running on. */
    @Tool
    fun getDeviceInfo(): Map<String, String> = mapOf(
        "manufacturer" to Build.MANUFACTURER,
        "model" to Build.MODEL,
        "androidVersion" to Build.VERSION.RELEASE,
        "sdkInt" to Build.VERSION.SDK_INT.toString(),
    )
}
