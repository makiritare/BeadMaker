package com.example.beadmaker.ui.model

enum class InteractionMode(val id: String) {
    Paint("paint"),
    Fill("fill"),
    Line("line"),
    Template("template"),
    Grid("grid");

    companion object {
        val default = Paint

        fun fromId(id: String): InteractionMode {
            return entries.find { it.id == id } ?: default
        }
    }
}
