package uk.co.tracetechnical.timeapi.models

import java.util.Date

data class Sun(val rise: Date, val set: Date, val up: Boolean, val down: Boolean, val position: Float)
