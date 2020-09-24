package org.rfcx.audiomoth.entity

enum class Screen(val id: String) {
    LOGIN("Login"),
    MAP("Map"),
    PROFILE("Profile"),
    COORDINATES("Coordinates"),
    FEEDBACK("Feedback"),
    CHOOSE_DEVICE("Choose Device"),
    LOCATION("Location "),
    MAP_PICKER("Map Picker"),
    MAP_SEARCH_RESULT("Map Search Result"),
    CONNECT_GUARDIAN("Connect Guardian"),
    GUARDIAN_REGISTER("Guardian Register"),
    GUARDIAN_SIGNAL("Guardian Signal"),
    GUARDIAN_SELECT_PROFILE("Guardian Select Profile"),
    GUARDIAN_CONFIGURE("Guardian Configure"),
    GUARDIAN_MICROPHONE("Guardian Microphone"),
    GUARDIAN_SOLAR_PANEL("Guardian Solar Panel"),
    GUARDIAN_CHECKIN_TEST("Guardian CheckIn Test"),
    GUARDIAN_DEPLOY("Guardian Deploy"),
    GUARDIAN_DIAGNOSTIC("Guardian Diagnostic")
}