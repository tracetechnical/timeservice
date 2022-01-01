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

@Component
class TimeService(val mqttService: MqttService, val sunCalc: SunriseSunsetCalculator) {
    private val lastValues: MutableMap<String,String> = emptyMap<String,String>().toMutableMap()
    private var tickTock = false
    private val DAYS: Array<String> = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri")
    @Scheduled(fixedRate = 1000)
    fun reportTime() {
        val isWeekday = parseInt(getDateSegment("e")) < 6
        val isWeekend = parseInt(getDateSegment("e")) >= 6
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
        val now = Calendar.getInstance()
        val hour = now.get(HOUR_OF_DAY)
        var bedtimeStr = System.getenv("BEDTIME_HOUR")
        var bedtime = 23
        if(bedtimeStr != null) {
            bedtime = Integer.parseInt(bedtimeStr)
        }
        println("Bedtime set to $bedtime:00")
        val sunrise = sunCalc.getCivilSunriseCalendarForDate(now).get(HOUR_OF_DAY)
        val sunset = sunCalc.getCivilSunsetCalendarForDate(now).get(HOUR_OF_DAY) + 1
        val daytime = IntRange(sunrise, sunset - 1).contains(hour)
        val evening = IntRange(sunset, bedtime - 1).contains(hour)
        val night = IntRange(bedtime, 23).contains(hour) || hour < sunrise
        diffPublish("timeOfDay/daytime", "$daytime")
        diffPublish("timeOfDay/evening", "$evening")
        diffPublish("timeOfDay/night", "$night")
        workoutSun(now)
    }

    private fun workoutSun(now: Calendar) {
        val sunrise = sunCalc.getCivilSunriseCalendarForDate(now)
        val sunset = sunCalc.getCivilSunsetCalendarForDate(now)
        val sunDelta = sunset.get(HOUR_OF_DAY)-sunrise.get(HOUR_OF_DAY)
        val sunDegreesPerHour: Float = 180F / sunDelta
        val currentSunriseDeltaHour = now.get(HOUR_OF_DAY) - sunrise.get(HOUR_OF_DAY)
        val fractionalHour: Float = now.get(MINUTE) / 60F
        val currentSunPositionDegrees: Float = currentSunriseDeltaHour * (sunDegreesPerHour + fractionalHour)
        val sunUp = currentSunPositionDegrees < 180
        val sunDown = currentSunPositionDegrees >= 180
        diffPublish("sun/rise", sunrise.time.toString())
        diffPublish("sun/set", sunset.time.toString())
        diffPublish("sun/up", "$sunUp")
        diffPublish("sun/down", "$sunDown")
        diffPublish("sun/position", "$currentSunPositionDegrees")
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
