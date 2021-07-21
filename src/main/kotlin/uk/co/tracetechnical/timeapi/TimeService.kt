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

@Component
class TimeService(val mqttService: MqttService, val sunCalc: SunriseSunsetCalculator) {
    private val lastValues: MutableMap<String,String> = emptyMap<String,String>().toMutableMap()
    private var tickTock = false
    @Scheduled(fixedRate = 1000)
    fun reportTime() {
        val isWeekday = parseInt(getDateSegment("e")) < 6
        val isWeekend = parseInt(getDateSegment("e")) >= 6
        tickTock = !tickTock
        diffPublish("time/tick", "$tickTock")
        diffPublish("time/day", getDateSegment("dd"))
        diffPublish("time/dayOfWeek", getDateSegment("e"))
        diffPublish("time/isWeekday", "$isWeekday")
        diffPublish("time/isWeekend", "$isWeekend")
        diffPublish("time/dayOfYear", getDateSegment("DD"))
        diffPublish("time/month", getDateSegment("MM"))
        diffPublish("time/year", getDateSegment("YY"))
        diffPublish("time/longYear", getDateSegment("YYYY"))
        diffPublish("time/hour", getDateSegment("HH"))
        diffPublish("time/minute", getDateSegment("mm"))
        diffPublish("time/second", getDateSegment("ss"))
        workoutSun()
    }

    private fun workoutSun() {
        val now = Calendar.getInstance()
        val sunrise = sunCalc.getCivilSunriseCalendarForDate(now)
        val sunset = sunCalc.getCivilSunsetCalendarForDate(now)
        val sunUp = sunrise.before(now) && sunset.after(now)
        val sunDown = sunrise.before(now) && sunset.before(now)
        val sunDelta = sunset.get(HOUR_OF_DAY)-sunrise.get(HOUR_OF_DAY)
        val sunDegreesPerHour: Float = 180F / sunDelta
        val currentSunriseDeltaHour = now.get(HOUR_OF_DAY) - sunrise.get(HOUR_OF_DAY)
        val fractionalHour: Float = now.get(MINUTE) / 60F
        val currentSunPositionDegrees: Float = currentSunriseDeltaHour * (sunDegreesPerHour + fractionalHour)
        diffPublish("sun/rise", sunrise.time.toString())
        diffPublish("sun/set", sunset.time.toString())
        diffPublish("sun/up", "$sunUp")
        diffPublish("sun/down", "$sunDown")
        diffPublish("sun/position", "$currentSunPositionDegrees")
    }

    private fun diffPublish(topic: String, value: String) {
        if(!lastValues.containsKey(topic) || (lastValues.containsKey(topic) && lastValues[topic] != value)) {
            mqttService.publish(topic, value)
            lastValues[topic] = value
        }
    }

    fun getDateSegment(pattern: String): String {
        val formatter = DateTimeFormatter.ofPattern(pattern)
        return LocalDateTime.now().format(formatter)!!
    }
}