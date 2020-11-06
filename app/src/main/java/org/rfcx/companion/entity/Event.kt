package org.rfcx.companion.entity

enum class Event(val id: String) {
    LOGOUT ("logout"),
    CLICK_PIN("click_pin"),
    SEE_DETAIL("see_detail"),
    CONNECT_CREATE_DEPLOYMENT("connect_create_deployment"),
    DELETE_DEPLOYMENT("delete_deployment"),
    CHANGE_COORDINATES("change_coordinates"),
    CREATE_NEW_GROUP("create_new_group"),
    SAVE_NEW_GROUP("save_new_group"),
    DELETE_LOCATION_GROUP("delete_location_group"),
    CHANGE_THEME("change_theme"),
    SEND_FEEDBACK("send_feedback"),
    ADD_FEEDBACK_IMAGES("add_feedback_images"),
    SEARCH_LOCATION("search_location"),
    SELECT_LOCATION("select_location"),
    EDIT_LOCATION("edit_location")
}
