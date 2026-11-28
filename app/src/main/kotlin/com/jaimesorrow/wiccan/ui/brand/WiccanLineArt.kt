package com.jaimesorrow.wiccan.ui.brand

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.jaimesorrow.wiccan.domain.model.Content
import com.jaimesorrow.wiccan.domain.model.ContentCategory
import com.wiccan.app.R

/** Thin gold line-art decor replacing former nodpi reference bitmaps. */
enum class WiccanLineArtKind(@DrawableRes val resId: Int) {
    Wheel(R.drawable.art_line_wheel),
    Cauldron(R.drawable.art_line_cauldron),
    Crystals(R.drawable.art_line_crystals),
    Divination(R.drawable.art_line_divination),
    GrimoireBook(R.drawable.art_line_grimoire_book),
    Handwritten(R.drawable.art_line_handwritten),
    Herbs(R.drawable.art_line_herbs),
    Journal(R.drawable.art_line_journal),
    Moon(R.drawable.art_line_moon),
    Mortar(R.drawable.art_line_mortar),
    Zodiac(R.drawable.art_line_zodiac),
    CategoryLunar(R.drawable.art_line_category_lunar),
    CategoryCord(R.drawable.art_line_category_cord),
    HomeHero(R.drawable.art_line_home_hero),
    Parchment(R.drawable.art_line_parchment),
    Botanical(R.drawable.art_line_botanical),
    TripleMoon(R.drawable.art_line_triple_moon),
    FloralI(R.drawable.art_line_floral_i),
    FloralR(R.drawable.art_line_floral_r),
    Candle(R.drawable.art_line_candle),
    CrystalAmethyst(R.drawable.art_line_crystal_amethyst),
    CrystalCitrine(R.drawable.art_line_crystal_citrine),
    CrystalClear(R.drawable.art_line_crystal_clear),
    CrystalLabradorite(R.drawable.art_line_crystal_labradorite),
    CrystalMoonstone(R.drawable.art_line_crystal_moonstone),
    CrystalObsidian(R.drawable.art_line_crystal_obsidian),
    CrystalRose(R.drawable.art_line_crystal_rose),
    CrystalSmoky(R.drawable.art_line_crystal_smoky),
    Petal(R.drawable.art_line_petal),
}

@Composable
fun WiccanLineArt(
    kind: WiccanLineArtKind,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Image(
        painter = painterResource(kind.resId),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}

internal fun Content.lessonLineArt(): WiccanLineArtKind {
    val categoryArt = when (category) {
        ContentCategory.CORD_KNOT_WORK, ContentCategory.CORD_KNOT_MAGIC -> WiccanLineArtKind.CategoryCord
        ContentCategory.LUNAR_PRACTICES,
        ContentCategory.MOON_MAGIC_TIMING,
        ContentCategory.MOON_AND_SEASONAL,
        -> WiccanLineArtKind.CategoryLunar
        else -> WiccanLineArtKind.HomeHero
    }
    return when (Math.floorMod(id.hashCode(), 5)) {
        0 -> WiccanLineArtKind.CategoryLunar
        1 -> WiccanLineArtKind.CategoryCord
        else -> categoryArt
    }
}

internal fun ContentCategory.lineArt(): WiccanLineArtKind? = when (this) {
    ContentCategory.LUNAR_PRACTICES, ContentCategory.MOON_MAGIC_TIMING, ContentCategory.MOON_AND_SEASONAL ->
        WiccanLineArtKind.CategoryLunar
    ContentCategory.CORD_KNOT_WORK, ContentCategory.CORD_KNOT_MAGIC -> WiccanLineArtKind.CategoryCord
    else -> null
}
