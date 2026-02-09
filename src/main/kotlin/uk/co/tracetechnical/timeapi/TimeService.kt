package uk.co.tracetechnical.timeapi

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.co.tracetechnical.timeapi.models.Sun
import java.lang.Integer.parseInt
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Calendar.HOUR_OF_DAY

@Component
class TimeService(val mqttService: MqttService, val sunService: SunService) {
    private val lastValues: MutableMap<String, String> = emptyMap<String, String>().toMutableMap()
    private val noDiffCount: MutableMap><String, Int> = emptyMap<String, Int>().toMutableMap()
    private var tickTock = false
    private val DAYS: Array<String> = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    var bedtime = getBedtimeVal()

    private final fun getBedtimeVal(): Int {
        val bedtimeStr = System.getenv("BEDTIME_HOUR")
        var out = 22
        if (bedtimeStr != null) {
            out = parseInt(bedtimeStr)
        }
        println("Env var BEDTIME_HOUR was set to $out:00")
        return out
    }

    @Scheduled(fixedRate = 1000)
    fun reportTime() {
        val now = Calendar.getInstance()

        performTick()

        publishDayOfWeekData()
        publishYearData()
        publishHMSData()

        val sun = publishSunData(now)

        publishTimeOfDay(now, sun)
    }

    private fun publishTimeOfDay(now: Calendar, sun: Sun) {
        val hour = now.get(HOUR_OF_DAY)
        val sunrise = sunService.getSunrise(now)
        val sunset = sunService.getSunset(now)
        val daytime = IntRange(sunrise, sunset - 1).contains(hour) && sun.up
        val evening = IntRange(sunset, bedtime - 1).contains(hour)
        val night = IntRange(bedtime, 23).contains(hour) || hour < sunrise && !sun.up

        diffPublish("timeOfDay/daytime", "$daytime")
        diffPublish("timeOfDay/evening", "$evening")
        diffPublish("timeOfDay/night", "$night")
    }

    private fun publishSunData(now: Calendar): Sun {
        val sun = sunService.calculateSunParameters(now)
        diffPublish("sun/rise", "${sun.rise}")
        diffPublish("sun/set", "${sun.set}")
        diffPublish("sun/up", "${sun.up}")
        diffPublish("sun/down", "${sun.down}")
        diffPublish("sun/position", "${sun.position}")
        return sun
    }

    private fun publishHMSData() {
        diffPublish("time/hour", getDateSegment("HH"))
        diffPublish("time/minute", getDateSegment("mm"))
        diffPublish("time/second", getDateSegment("ss"))
        diffPublish("time/fulltime", getDateSegment("HH:mm"))
        diffPublish("time/fulltimesecs", getDateSegment("HH:mm:ss"))
    }

    private fun publishYearData() {
        diffPublish("time/dayOfYear", getDateSegment("DD"))
        diffPublish("time/month", getDateSegment("MM"))
        diffPublish("time/year", getDateSegment("YY"))
        diffPublish("time/longYear", getDateSegment("YYYY"))
        diffPublish("time/fulldate", getDateSegment("yyyy-MM-dd"))
    }

    private fun publishDayOfWeekData() {
        val isWeekday = parseInt(getDateSegment("e")) < 6
        val isWeekend = parseInt(getDateSegment("e")) >= 6
        diffPublish("time/day", getDateSegment("dd"))
        diffPublish("time/dayOfWeek", "${DAYS.indexOf(getDateSegment("EE"))}")
        diffPublish("time/isWeekday", "$isWeekday")
        diffPublish("time/isWeekend", "$isWeekend")
    }

    private fun performTick() {
        tickTock = !tickTock
        diffPublish("time/tick", "$tickTock")
    }

    private fun diffPublish(topic: String, value: String) {
        if (!lastValues.containsKey(topic) || (lastValues.containsKey(topic) && lastValues[topic] != value)) {
            println("No diff counter for ${topic} was ${noDiffCount[topic]}")
            mqttService.publish(topic, value, true)
            lastValues[topic] = value
            noDiffCount[topic] = 0
        } else {
            noDiffCount[topic] += 1
        }
    }

    fun getDateSegment(pattern: String): String {
        val formatter = DateTimeFormatter.ofPattern(pattern)
        return LocalDateTime.now().format(formatter)!!
    }
}
