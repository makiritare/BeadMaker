package com.example.beadmaker.ui.model

import com.example.beadmaker.R

enum class StitchLayoutStyle {
    Even, Staggered
}

enum class StitchMode(
    val id: String,
    val labelRes: Int,
    val layoutStyle: StitchLayoutStyle
) {
    Square("square", R.string.stitch_square, StitchLayoutStyle.Even),
    Peyote("peyote", R.string.stitch_peyote, StitchLayoutStyle.Staggered),
    Peyote2Drop("peyote_2drop", R.string.stitch_peyote_2drop, StitchLayoutStyle.Staggered),
    Peyote3Drop("peyote_3drop", R.string.stitch_peyote_3drop, StitchLayoutStyle.Staggered),
    Brick("brick", R.string.stitch_brick, StitchLayoutStyle.Staggered);

    companion object {
        val defaults = Square
        
        fun fromId(id: String): StitchMode {
            return entries.find { it.id == id } ?: defaults
        }
    }
}
