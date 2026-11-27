package com.jaimesorrow.wiccan.ui.today

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaimesorrow.wiccan.data.location.lastKnownCoordinate
import com.jaimesorrow.wiccan.domain.model.Content
import com.jaimesorrow.wiccan.sky.CelestialCalculator
import com.jaimesorrow.wiccan.sky.ClassicalPlanet
import com.jaimesorrow.wiccan.ui.brand.WiccanFullLogo
import com.jaimesorrow.wiccan.ui.theme.GrimoireCandleGlow
import com.jaimesorrow.wiccan.ui.theme.GrimoireDashedConnector
import com.jaimesorrow.wiccan.ui.theme.GrimoireFloralBackdrop
import com.jaimesorrow.wiccan.ui.theme.GrimoireHomeSectionGlyph
import com.jaimesorrow.wiccan.ui.theme.GrimoireHomeSectionTile
import com.jaimesorrow.wiccan.ui.theme.GrimoireInkLabel
import com.jaimesorrow.wiccan.ui.theme.GrimoireMoonPhases
import com.jaimesorrow.wiccan.ui.theme.GrimoireOrnamentalDivider
import com.jaimesorrow.wiccan.ui.theme.GrimoireSettingsHeaderAccent
import com.jaimesorrow.wiccan.ui.theme.MidnightGrimoireTheme
import com.jaimesorrow.wiccan.ui.theme.WiccanDisplayFont
import com.jaimesorrow.wiccan.ui.theme.WiccanEditorialFont
import com.jaimesorrow.wiccan.ui.theme.WiccanPaletteColors
import com.jaimesorrow.wiccan.ui.theme.grimoireCornerFlourishes
import com.wiccan.app.R
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle

private val CardShape = RoundedCornerShape(6.dp)
private val Gold = WiccanPaletteColors.Grimoire.Border
private val Ink = WiccanPaletteColors.Grimoire.Text
private val Muted = WiccanPaletteColors.Grimoire.Muted

@Composable
internal fun SpellbookHome(
    content: Content?,
    lessonLocked: Boolean,
    onOpenToday: () -> Unit,
    onOpenPaywall: () -> Unit,
    onOpenGrimoire: () -> Unit,
) {
    MidnightGrimoireTheme {
        val context = LocalContext.current
        var coordinate by remember { mutableStateOf(lastKnownCoordinate(context)) }
        val locationPermission = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted -> if (granted) coordinate = lastKnownCoordinate(context) }
        val now = remember { java.time.Instant.now() }
        val zone = remember { ZoneId.systemDefault() }
        val today = LocalDate.now()
        val moon = remember(today) { CelestialCalculator.moon(now) }
        val sabbat = remember(today) { CelestialCalculator.sabbatFor(today) }
        val hour = remember(coordinate) { CelestialCalculator.planetaryHour(now, zone, coordinate) }
        val voidOfCourse = remember(today) { CelestialCalculator.voidOfCourse(now) }
        Box(modifier = Modifier.fillMaxSize().background(WiccanPaletteColors.Grimoire.Background)) {
            GrimoireFloralBackdrop(Modifier.fillMaxSize())
            GrimoireSettingsHeaderAccent(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(width = 180.dp, height = 96.dp),
            )
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                WiccanFullLogo(width = 148.dp)
                GrimoireInkLabel(stringResource(R.string.motif_today_page))
                WelcomeCard(moon = moon, today = today)
                HomeSectionTileRow()
                GrimoireOrnamentalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                GrimoireDashedConnector(Modifier.fillMaxWidth().height(28.dp))
                LessonCard(
                    content = content,
                    locked = lessonLocked,
                    onOpen = { if (lessonLocked) onOpenPaywall() else onOpenToday() },
                )
                WheelCard(today = today, sabbat = sabbat)
                CosmicCard(
                    moon = moon,
                    hour = hour,
                    voidOfCourse = voidOfCourse,
                    hasLocation = coordinate != null,
                    onRequestLocation = { locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                )
                PlateAccents()
                Text(
                    text = stringResource(R.string.today_open_grimoire),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .clickable(onClick = onOpenGrimoire)
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    color = WiccanPaletteColors.Grimoire.Gold,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun HomeSectionTileRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GrimoireHomeSectionTile(
            label = stringResource(R.string.home_today_lesson),
            glyph = GrimoireHomeSectionGlyph.LESSON,
            modifier = Modifier.weight(1f),
        )
        GrimoireHomeSectionTile(
            label = stringResource(R.string.home_wheel_title),
            glyph = GrimoireHomeSectionGlyph.WHEEL,
            modifier = Modifier.weight(1f),
        )
        GrimoireHomeSectionTile(
            label = stringResource(R.string.home_cosmic_title),
            glyph = GrimoireHomeSectionGlyph.COSMIC,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun WelcomeCard(moon: com.jaimesorrow.wiccan.sky.MoonObservation, today: LocalDate) {
    GrimoireCard {
        Text(
            text = stringResource(R.string.home_welcome),
            color = WiccanPaletteColors.Grimoire.Gold,
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = WiccanDisplayFont),
        )
        GrimoireMoonPhases(active = moon.trackIndex, activePhaseName = moon.name)
        Text(
            text = moon.name,
            color = Ink,
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = WiccanEditorialFont),
        )
        Text(
            text = stringResource(R.string.home_moon_in_sign, moon.zodiacSign, percent(moon.illumination)),
            color = Muted,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(text = moon.timingGuidance, color = Ink, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = today.toString(),
            color = WiccanPaletteColors.Grimoire.Gold,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun LessonCard(content: Content?, locked: Boolean, onOpen: () -> Unit) {
    GrimoireCard {
        Text(
            text = stringResource(R.string.home_today_lesson),
            color = WiccanPaletteColors.Grimoire.Gold,
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = WiccanDisplayFont),
        )
        Text(
            text = when {
                locked -> stringResource(R.string.home_locked_lesson_title)
                content != null -> content.title
                else -> stringResource(R.string.home_lesson_waiting)
            },
            color = Ink,
            style = MaterialTheme.typography.titleLarge.copy(fontFamily = WiccanEditorialFont),
        )
        Text(
            text = if (locked) {
                stringResource(R.string.home_locked_lesson_body)
            } else {
                stringResource(R.string.home_lesson_open_hint)
            },
            color = Muted,
            style = MaterialTheme.typography.bodySmall,
        )
        GrimoireAction(
            text = stringResource(if (locked) R.string.home_unlock_lesson else R.string.home_open_lesson),
            onClick = onOpen,
            modifier = Modifier.fillMaxWidth().testTag("home-daily-action"),
        )
    }
}

@Composable
private fun WheelCard(today: LocalDate, sabbat: com.jaimesorrow.wiccan.sky.SabbatObservance) {
    GrimoireCard {
        Text(
            text = stringResource(R.string.home_wheel_title),
            color = Ink,
            style = MaterialTheme.typography.headlineSmall,
        )
        Image(
            painter = painterResource(R.drawable.wiccan_photo_wheel),
            contentDescription = stringResource(R.string.home_wheel_image),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(92.dp).clip(CardShape),
        )
        MonthGrid(today)
        Text(
            text = if (sabbat.observedToday) sabbat.name else stringResource(R.string.home_next_sabbat, sabbat.name),
            color = Ink,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = WiccanEditorialFont),
        )
        Text(
            text = sabbat.date.toString(),
            color = WiccanPaletteColors.Grimoire.Gold,
            style = MaterialTheme.typography.labelSmall,
        )
        Text(text = sabbat.historicalSignificance, color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun CosmicCard(
    moon: com.jaimesorrow.wiccan.sky.MoonObservation,
    hour: com.jaimesorrow.wiccan.sky.PlanetaryHour?,
    voidOfCourse: com.jaimesorrow.wiccan.sky.VoidOfCourse,
    hasLocation: Boolean,
    onRequestLocation: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(WiccanPaletteColors.Parchment.Background)
            .border(1.dp, WiccanPaletteColors.Parchment.Border, CardShape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.home_cosmic_title),
            color = WiccanPaletteColors.Parchment.Ink,
            style = MaterialTheme.typography.headlineSmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            PlanetaryDial(active = hour?.planet)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_planetary_hour_label),
                    color = WiccanPaletteColors.Grimoire.Bronze,
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    text = if (hour != null) {
                        stringResource(R.string.home_hour_of, hour.planet.name.lowercase().replaceFirstChar { it.titlecase() })
                    } else {
                        stringResource(R.string.home_hour_unavailable)
                    },
                    color = WiccanPaletteColors.Parchment.Ink,
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = WiccanEditorialFont),
                )
                if (!hasLocation) {
                    Text(
                        text = stringResource(R.string.home_use_location),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .clickable(onClick = onRequestLocation)
                            .padding(horizontal = 4.dp, vertical = 12.dp),
                        color = WiccanPaletteColors.Grimoire.Bronze,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
        DarkTile {
            Text(
                text = stringResource(R.string.home_tides_title),
                color = WiccanPaletteColors.Grimoire.Gold,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = stringResource(
                    R.string.home_moon_in_sign,
                    moon.zodiacSign,
                    percent(moon.illumination),
                ) + " · " + moon.name,
                color = Ink,
                style = MaterialTheme.typography.bodySmall,
            )
            MoonLightChart(moon.illumination)
            Text(
                text = stringResource(R.string.home_tides_unavailable),
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        DarkTile {
            Text(
                text = stringResource(R.string.home_voc_title),
                color = WiccanPaletteColors.Grimoire.Gold,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = if (voidOfCourse.active) {
                    stringResource(R.string.home_voc_active, voidOfCourse.nextSign)
                } else {
                    stringResource(R.string.home_voc_clear, voidOfCourse.currentSign)
                },
                color = Ink,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        DarkTile {
            Text(
                text = stringResource(R.string.home_alignment_title),
                color = WiccanPaletteColors.Grimoire.Gold,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = stringResource(R.string.home_alignment, moon.zodiacSign, moon.name),
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun GrimoireCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(WiccanPaletteColors.Grimoire.CardFill)
            .border(1.dp, Gold, CardShape)
            .grimoireCornerFlourishes()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) { content() }
}

@Composable
private fun DarkTile(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(WiccanPaletteColors.Grimoire.Background)
            .border(1.dp, Gold, RoundedCornerShape(4.dp))
            .grimoireCornerFlourishes(arm = 10.dp)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) { content() }
}

@Composable
private fun GrimoireAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(WiccanPaletteColors.Grimoire.Border, WiccanPaletteColors.Grimoire.Gold),
                ),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = WiccanPaletteColors.Grimoire.OnGold,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

@Composable
private fun PlateAccents() {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(
            R.drawable.grimoire_crystal_labradorite,
            R.drawable.grimoire_crystal_amethyst,
            R.drawable.grimoire_petal,
            R.drawable.grimoire_crystal_rose,
            R.drawable.grimoire_crystal_moonstone,
        ).forEach { art ->
            Image(
                painter = painterResource(art),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
            )
        }
    }
}

@Composable
private fun MonthGrid(today: LocalDate) {
    val month = YearMonth.from(today)
    val labels = listOf("M", "T", "W", "T", "F", "S", "S")
    val offset = month.atDay(1).dayOfWeek.value - 1
    val cells = List(offset) { null as LocalDate? } + (1..month.lengthOfMonth()).map(month::atDay)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = month.month.getDisplayName(TextStyle.FULL, LocalLocale.current.platformLocale),
            color = WiccanPaletteColors.Grimoire.Gold,
            style = MaterialTheme.typography.labelSmall,
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            labels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    color = Gold,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    val selected = date == today
                    Box(
                        modifier = Modifier.weight(1f).height(22.dp).then(
                            if (selected) Modifier.clip(CircleShape).background(WiccanPaletteColors.Grimoire.Gold) else Modifier,
                        ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = date?.dayOfMonth?.toString().orEmpty(),
                            color = if (selected) WiccanPaletteColors.Grimoire.OnGold else Ink,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun MoonLightChart(illumination: Double) {
    Canvas(modifier = Modifier.fillMaxWidth().height(36.dp)) {
        val gold = WiccanPaletteColors.Grimoire.Gold
        val ink = WiccanPaletteColors.Parchment.Ink
        val radius = size.height * 0.42f
        val center = Offset(radius + 4.dp.toPx(), size.height / 2f)
        drawCircle(ink.copy(alpha = 0.2f), radius, center)
        drawArc(
            color = gold,
            startAngle = -90f,
            sweepAngle = (illumination.coerceIn(0.0, 1.0) * 360.0).toFloat(),
            useCenter = true,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
        )
        drawCircle(gold, radius, center, style = Stroke(width = 1.dp.toPx()))
    }
}

@Composable
private fun PlanetaryDial(active: ClassicalPlanet?) {
    val planets = ClassicalPlanet.entries
    Canvas(modifier = Modifier.size(92.dp)) {
        val gold = WiccanPaletteColors.Grimoire.Border
        drawCircle(gold, style = Stroke(width = 1.dp.toPx()))
        drawCircle(gold, radius = size.minDimension * 0.34f, style = Stroke(width = 1.dp.toPx()))
        planets.forEachIndexed { index, planet ->
            val angle = (index / planets.size.toFloat()) * 2f * Math.PI.toFloat() - (Math.PI / 2f).toFloat()
            val radius = size.minDimension * 0.38f
            val center = Offset(size.width / 2f + kotlin.math.cos(angle) * radius, size.height / 2f + kotlin.math.sin(angle) * radius)
            drawCircle(
                color = if (planet == active) WiccanPaletteColors.Grimoire.Gold else gold.copy(alpha = 0.45f),
                radius = if (planet == active) 5.dp.toPx() else 3.dp.toPx(),
                center = center,
            )
        }
    }
}

private fun percent(illumination: Double): Int = (illumination * 100.0).toInt().coerceIn(0, 100)
