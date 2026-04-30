package com.example.beadmaker.ui.model

import com.example.beadmaker.R

enum class BeadShape(
    val id: String,
    val labelRes: Int
) {
    Circle("circle", R.string.shape_circle),
    RoundedRectangle("rounded_rectangle", R.string.shape_rounded_rectangle);

    companion object {
        val defaults = Circle

        fun fromId(id: String): BeadShape {
            return entries.find { it.id == id } ?: defaults
        }
    }
}
