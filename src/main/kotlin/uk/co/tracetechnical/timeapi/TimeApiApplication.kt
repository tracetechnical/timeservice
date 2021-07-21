package uk.co.tracetechnical.timeapi

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator
import com.luckycatlabs.sunrisesunset.dto.Location
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class TimeApiApplication {
    @Bean
    fun getSunCalculator(): SunriseSunsetCalculator {
        val location = Location("50.968920", "-3.231940")
        return SunriseSunsetCalculator(location, "Europe/London")
    }
}

fun main(args: Array<String>) {
    runApplication<TimeApiApplication>(*args)
}
