package com.digitalminds.beej.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.digitalminds.beej.R

/** Todos los colores de la marca viven aquí (y en res/values/colors.xml para recursos XML). */
object Brand {
    val Black = Color(0xFF000000)
    val Card = Color(0xFF151517)
    val Panel = Color(0xFF1F2023)
    val Ferrari = Color(0xFFDC0000)
    val RedBright = Color(0xFFFF2A1F)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF9A9CA4)
    val IconBg = Color(0xFFBEBEBE)
    val Error = Color(0xFFFF5C8A)
}

val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_bold, FontWeight.Bold)
)

/** Para marca y detalles especiales. */
val Michroma = FontFamily(Font(R.font.michroma_regular, FontWeight.Normal))

/** Para letras y palabras: la I mayúscula y la l minúscula no se confunden. */
val Atkinson = FontFamily(
    Font(R.font.atkinson_regular, FontWeight.Normal),
    Font(R.font.atkinson_bold, FontWeight.Bold)
)

object Type {
    val Body = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal, fontSize = 15.sp, color = Brand.TextPrimary)
    val BodySecondary = Body.copy(color = Brand.TextSecondary, fontSize = 14.sp)
    val Label = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Brand.TextSecondary)
    val Card = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 17.sp, color = Brand.TextPrimary)
    val Row = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.Bold, fontSize = 19.sp, letterSpacing = 1.sp, color = Brand.TextPrimary)
    val WordBig = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.Bold, fontSize = 38.sp, letterSpacing = 3.sp, color = Brand.TextPrimary)
    val Letter = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Brand.TextPrimary)
    val Wordmark = TextStyle(fontFamily = Michroma, fontWeight = FontWeight.Normal, fontSize = 11.sp, letterSpacing = 4.sp, color = Brand.TextSecondary)
}

@Composable
fun BeeJTheme(content: @Composable () -> Unit) {
    @Suppress("UNUSED_VARIABLE") val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Brand.Ferrari,
            onPrimary = Brand.TextPrimary,
            background = Brand.Black,
            onBackground = Brand.TextPrimary,
            surface = Brand.Card,
            onSurface = Brand.TextPrimary,
            error = Brand.Error
        ),
        content = content
    )
}
