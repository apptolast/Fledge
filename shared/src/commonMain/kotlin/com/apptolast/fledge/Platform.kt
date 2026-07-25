package com.apptolast.fledge

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform