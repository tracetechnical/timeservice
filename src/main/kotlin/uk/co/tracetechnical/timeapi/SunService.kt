package uk.co.tracetechnical.timeapi

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator
import org.springframework.stereotype.Service
import uk.co.tracetechnical.timeapi.models.Sun
import java.util.Calendar

@Service
class SunService(val sunCalc: SunriseSunsetCalculator) {
    fun sunIsUp(currentSunPositionDegrees: Float): Boolean =
        currentSunPositionDegrees < 180 && currentSunPositionDegrees > 0

    fun sunIsDown(currentSunPositionDegrees: Float): Boolean =
        currentSunPositionDegrees >= 180 || currentSunPositionDegrees <= 0

    fun getSunrise(now: Calendar): Int = sunCalc.getCivilSunriseCalendarForDate(now).get(Calendar.HOUR_OF_DAY)
    fun getSunset(now: Calendar): Int = sunCalc.getCivilSunsetCalendarForDate(now).get(Calendar.HOUR_OF_DAY) + 1

    fun calculateSunParameters(now: Calendar): Sun {
        val sunrise = sunCalc.getCivilSunriseCalendarForDate(now)
        val sunset = sunCalc.getCivilSunsetCalendarForDate(now)

        val currentSunPositionDegrees: Float = getCurrentSunPosition(sunset, sunrise, now)

        return Sun(
            sunrise.time,
            sunset.time,
            sunIsUp(currentSunPositionDegrees),
            sunIsDown(currentSunPositionDegrees),
            currentSunPositionDegrees
        )
    }

    private fun getCurrentSunPosition(sunset: Calendar, sunrise: Calendar, now: Calendar): Float {
        val sunDelta = sunset.get(Calendar.HOUR_OF_DAY) - sunrise.get(Calendar.HOUR_OF_DAY)
        val sunDegreesPerHour: Float = 180F / sunDelta
        val currentSunriseDeltaHour = now.get(Calendar.HOUR_OF_DAY) - sunrise.get(Calendar.HOUR_OF_DAY)
        val fractionalHour: Float = now.get(Calendar.MINUTE) / 60F

        return currentSunriseDeltaHour * (sunDegreesPerHour + fractionalHour)
    }
}