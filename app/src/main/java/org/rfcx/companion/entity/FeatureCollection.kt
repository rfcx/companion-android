package org.rfcx.companion.entity

open class FeatureCollection(
    var type: String = "",
    var features: Array<Feature>

)

open class Feature(
    var type: String = "",
    var properties: Color,
    var geometry: Geometry
)

open class Color(
    var color: String = ""
)

open class Geometry(
    var type: String = "",
    var coordinates: Array<Array<Double>>
)
