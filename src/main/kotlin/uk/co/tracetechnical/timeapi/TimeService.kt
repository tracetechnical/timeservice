package uk.co.tracetechnical.timeapi

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.lang.Integer.parseInt
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.Calendar.HOUR_OF_DAY
import java.util.Calendar.MINUTE
import java.util.EnumSet.range


data class Sun(val rise: String, val set: String, val up: Boolean, val down: Boolean, val position: Float)

@Component
class TimeService(val mqttService: MqttService, val sunCalc: SunriseSunsetCalculator) {
    private val lastValues: MutableMap<String,String> = emptyMap<String,String>().toMutableMap()
    private var tickTock = false
    private val DAYS: Array<String> = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri")

    var bedtime = getBedtimeVal()

    fun getBedtimeVal(): Int {
        var bedtimeStr = System.getenv("BEDTIME_HOUR")
        var out = 22
        if (bedtimeStr != null) {
            out = Integer.parseInt(bedtimeStr)
        }
        println("Env var BEDTIME_HOUR was set to $out:00")
        return out
    }

    @Scheduled(fixedRate = 1000)
    fun reportTime() {
        val isWeekday = parseInt(getDateSegment("e")) < 6
        val isWeekend = parseInt(getDateSegment("e")) >= 6
        val now = Calendar.getInstance()

        tickTock = !tickTock
        diffPublish("time/tick", "$tickTock")
        diffPublish("time/day", getDateSegment("dd"))
        diffPublish("time/dayOfWeek","${DAYS.indexOf(getDateSegment("EE"))}")
        diffPublish("time/isWeekday", "$isWeekday")
        diffPublish("time/isWeekend", "$isWeekend")

        diffPublish("time/dayOfYear", getDateSegment("DD"))
        diffPublish("time/month", getDateSegment("MM"))
        diffPublish("time/year", getDateSegment("YY"))
        diffPublish("time/longYear", getDateSegment("YYYY"))
        diffPublish("time/hour", getDateSegment("HH"))
        diffPublish("time/minute", getDateSegment("mm"))
        diffPublish("time/second", getDateSegment("ss"))

        val sun = workoutSun(now)
        diffPublish("sun/rise", sun.rise)
        diffPublish("sun/set", sun.set)
        diffPublish("sun/up", "${sun.up}")
        diffPublish("sun/down", "${sun.down}")
        diffPublish("sun/position", "${sun.position}")

        val hour = now.get(HOUR_OF_DAY)
        val sunrise = sunCalc.getCivilSunriseCalendarForDate(now).get(HOUR_OF_DAY)
        val sunset = sunCalc.getCivilSunsetCalendarForDate(now).get(HOUR_OF_DAY) + 1
        val daytime = IntRange(sunrise, sunset - 1).contains(hour) && sun.up
        val evening = IntRange(sunset, bedtime - 1).contains(hour)
        val night = IntRange(bedtime, 23).contains(hour) || hour < sunrise && !sun.up

        diffPublish("timeOfDay/daytime", "$daytime")
        diffPublish("timeOfDay/evening", "$evening")
        diffPublish("timeOfDay/night", "$night")
    }

    private fun workoutSun(now: Calendar): Sun {
        val sunrise = sunCalc.getCivilSunriseCalendarForDate(now)
        val sunset = sunCalc.getCivilSunsetCalendarForDate(now)
        val sunDelta = sunset.get(HOUR_OF_DAY)-sunrise.get(HOUR_OF_DAY)
        val sunDegreesPerHour: Float = 180F / sunDelta
        val currentSunriseDeltaHour = now.get(HOUR_OF_DAY) - sunrise.get(HOUR_OF_DAY)
        val fractionalHour: Float = now.get(MINUTE) / 60F
        val currentSunPositionDegrees: Float = currentSunriseDeltaHour * (sunDegreesPerHour + fractionalHour)
        val sunUp = currentSunPositionDegrees < 180 && currentSunPositionDegrees > 0
        val sunDown = currentSunPositionDegrees >= 180 || currentSunPositionDegrees <= 0

        return Sun(sunrise.time.toString(), sunset.time.toString(), sunUp, sunDown, currentSunPositionDegrees)
    }

    private fun diffPublish(topic: String, value: String) {
        if(!lastValues.containsKey(topic) || (lastValues.containsKey(topic) && lastValues[topic] != value)) {
            mqttService.publish(topic, value, true)
            lastValues[topic] = value
        }
    }

    fun getDateSegment(pattern: String): String {
        val formatter = DateTimeFormatter.ofPattern(pattern)
        return LocalDateTime.now().format(formatter)!!
    }
}
