package com.tomasps.slurmmanag.widget

import androidx.compose.ui.unit.dp

/**
 * M3 shape-scale tokens for Glance widgets.
 *
 * Maps to the MD3 shape corner scale:
 *   full         = 9999dp  — pill / stadium (buttons, badges, status chips)
 *   extraLarge   =   28dp  — bottom sheets, dialogs → widget outer card
 *   large        =   16dp  — FABs, navigation drawer → inner section cards
 *   medium       =   12dp  — cards → list row containers
 *   small        =    8dp  — text fields, menus → tight chips
 *   extraSmall   =    4dp  — snackbars, chip tails
 */
object WidgetShapes {
    val Full       = 9999.dp
    val ExtraLarge =   28.dp
    val Large      =   16.dp
    val Medium     =   12.dp
    val Small      =    8.dp
    val ExtraSmall =    4.dp
}
