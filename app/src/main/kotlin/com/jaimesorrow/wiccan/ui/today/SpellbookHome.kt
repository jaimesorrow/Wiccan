package com.jaimesorrow.wiccan.ui.today

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaimesorrow.wiccan.domain.model.Content
import com.jaimesorrow.wiccan.domain.model.ContentType
import com.jaimesorrow.wiccan.ui.archive.displayName
import com.jaimesorrow.wiccan.ui.brand.WiccanBotanicalAccent
import com.jaimesorrow.wiccan.ui.brand.WiccanFullLogo
import com.jaimesorrow.wiccan.ui.brand.wiccanEmblemColor
import com.wiccan.app.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val ParchmentInk = Color(0xFF33200E)
private val FrameGold = Color(0xFFC5913B)
private val ButtonGold = Color(0xFFE8BC61)

@Composable
internal fun SpellbookHome(
    content: Content?,
    onOpenToday: () -> Unit,
    onOpenGrimoire: () -> Unit,
    onOpenCabinet: () -> Unit,
    onOpenCoven: () -> Unit,
) {
    val accent = wiccanEmblemColor()
    val heroShape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        accent.copy(alpha = 0.06f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WiccanFullLogo(width = 144.dp, modifier = Modifier.padding(bottom = 4.dp))
            WiccanBotanicalAccent(width = 116.dp)
            WelcomeLunarCard()
            Image(
                painter = painterResource(R.drawable.wiccan_home_hero),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.48f)
                    .shadow(10.dp, heroShape)
                    .clip(heroShape)
                    .border(1.dp, FrameGold, heroShape),
            )
            DailySpellCard(content = content, onOpenToday = onOpenToday)
            SabbatCalendarCard()
            CosmicTimingCard()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onOpenCabinet,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent.copy(alpha = 0.18f),
                        contentColor = accent,
                    ),
                ) { Text("My Cabinet") }
                Button(
                    onClick = onOpenCoven,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent.copy(alpha = 0.18f),
                        contentColor = accent,
                    ),
                ) { Text("The Coven") }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = accent.copy(alpha = 0.5f))
                TextButton(onClick = onOpenGrimoire, modifier = Modifier.weight(4f)) {
                    Text(
                        text = stringResource(R.string.today_open_grimoire),
                        color = accent,
                        textAlign = TextAlign.Center,
                    )
                }
                HorizontalDivider(modifier = Modifier.weight(1f), color = accent.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun WelcomeLunarCard() {
    val today = LocalDate.now()
    val phase = moonPhaseFor(today)
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF080808), shape)
            .border(1.dp, FrameGold.copy(alpha = 0.82f), shape)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            text = "WELCOME, SEEKER",
            style = MaterialTheme.typography.labelLarge,
            color = ButtonGold,
        )
        MoonPhaseTrack(activeIndex = phase.trackIndex)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = phase.symbol,
                style = MaterialTheme.typography.displaySmall,
                color = ButtonGold,
            )
            Spacer(Modifier.padding(horizontal = 6.dp))
            Column {
                Text(
                    text = phase.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFFF7F4EB),
                )
                Text(
                    text = today.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFD9CDBB),
                )
            }
        }
        Text(
            text = phase.timingMeaning,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFF0E7D8),
        )
        Text(
            text = "Today’s timing: ${phase.todaySuggestion}",
            style = MaterialTheme.typography.bodySmall,
            color = ButtonGold,
        )
    }
}

@Composable
private fun MoonPhaseTrack(activeIndex: Int) {
    val phases = listOf("●", "◔", "◐", "◕", "○", "◕", "◑", "◔")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        phases.forEachIndexed { index, symbol ->
            Text(
                text = symbol,
                color = if (index == activeIndex) ButtonGold else Color(0xFF665B4B),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun SabbatCalendarCard() {
    val today = LocalDate.now()
    val sabbat = sabbatFor(today)
    val month = YearMonth.from(today)
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF11100D), shape)
            .border(1.dp, FrameGold.copy(alpha = 0.7f), shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text("YEAR OF THE SABBAT", style = MaterialTheme.typography.labelLarge, color = ButtonGold)
        Text(
            month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFF7F4EB),
        )
        MonthCalendar(month = month, today = today)
        Text(sabbat.name, style = MaterialTheme.typography.titleLarge, color = Color(0xFFF7F4EB))
        Text(sabbat.dateLabel, style = MaterialTheme.typography.labelMedium, color = ButtonGold)
        Text(sabbat.description, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFD9CDBB))
        HorizontalDivider(color = FrameGold.copy(alpha = .35f))
        Text("Seasonal focus: ${sabbat.focus}", style = MaterialTheme.typography.bodySmall, color = ButtonGold)
    }
}

@Composable
private fun MonthCalendar(month: YearMonth, today: LocalDate) {
    val weekdayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
    val firstOffset = month.atDay(1).dayOfWeek.value - DayOfWeek.MONDAY.value
    val cells = List(firstOffset) { null } + (1..month.lengthOfMonth()).map(month::atDay)
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdayLabels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = FrameGold,
                    textAlign = TextAlign.Center,
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(26.dp)
                            .then(
                                if (date == today) {
                                    Modifier
                                        .clip(RoundedCornerShape(13.dp))
                                        .background(ButtonGold)
                                } else {
                                    Modifier
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = date?.dayOfMonth?.toString().orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (date == today) ParchmentInk else Color(0xFFD9CDBB),
                        )
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun CosmicTimingCard() {
    val now = LocalDateTime.now()
    val timing = cosmicTimingFor(now)
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF080808), shape)
            .border(1.dp, FrameGold.copy(alpha = .82f), shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("COSMIC TIMING", style = MaterialTheme.typography.labelLarge, color = ButtonGold)
        TimingSection(
            symbol = timing.planetSymbol,
            title = "The Hour of ${timing.planet}",
            body = timing.planetMeaning,
        )
        HorizontalDivider(color = FrameGold.copy(alpha = .35f))
        TimingSection(
            symbol = "☾",
            title = "Tides & Astro-Weather",
            body = "${timing.moonTransit}. ${timing.tidalGuidance}",
        )
        HorizontalDivider(color = FrameGold.copy(alpha = .35f))
        TimingSection(
            symbol = "✦",
            title = "VoC & Alignments",
            body = timing.alignmentGuidance,
        )
    }
}

@Composable
private fun TimingSection(symbol: String, title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(19.dp))
                .border(1.dp, FrameGold, RoundedCornerShape(19.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(symbol, color = ButtonGold, style = MaterialTheme.typography.titleLarge)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = Color(0xFFF7F4EB), style = MaterialTheme.typography.titleMedium)
            Text(body, color = Color(0xFFD9CDBB), style = MaterialTheme.typography.bodySmall)
        }
    }
}

private data class MoonPhasePresentation(
    val name: String,
    val symbol: String,
    val timingMeaning: String,
    val todaySuggestion: String,
    val trackIndex: Int,
)

private fun moonPhaseFor(date: LocalDate): MoonPhasePresentation {
    val cycleDay = ((date.toEpochDay() - LocalDate.of(2000, 1, 6).toEpochDay()) % 30 + 30) % 30
    return when (cycleDay.toInt()) {
        in 0..3 -> MoonPhasePresentation("New Moon", "●", "A quiet threshold for setting intentions and beginning gently.", "Name one clear intention before you begin your practice.", 0)
        in 4..7 -> MoonPhasePresentation("Waxing Crescent", "☽", "Energy is gathering; take a small, committed step toward what you are growing.", "Choose a simple action that supports your intention.", 1)
        in 8..11 -> MoonPhasePresentation("First Quarter", "◐", "A moment for resolve, adjustment, and hands-on work.", "Meet one obstacle with practical care.", 2)
        in 12..16 -> MoonPhasePresentation("Waxing Gibbous", "◔", "Refine what is already in motion and notice what needs tending.", "Review your materials and adapt with intention.", 3)
        in 17..20 -> MoonPhasePresentation("Full Moon", "○", "Illumination invites gratitude, release, and a wider view of your path.", "Honor what has grown, then release what no longer serves.", 4)
        in 21..24 -> MoonPhasePresentation("Waning Gibbous", "◕", "Share wisdom, reflect on results, and make room for integration.", "Write one insight in your Grimoire.", 5)
        in 25..27 -> MoonPhasePresentation("Last Quarter", "◑", "A clearing phase for boundaries, discernment, and gentle endings.", "Simplify your next step.", 6)
        else -> MoonPhasePresentation("Waning Crescent", "☾", "Rest, restore, and listen for what wants to emerge next.", "Keep your practice small and restorative.", 7)
    }
}

private data class SabbatPresentation(val name: String, val dateLabel: String, val description: String, val focus: String)

private fun sabbatFor(date: LocalDate): SabbatPresentation = when (date.monthValue) {
    1, 2 -> SabbatPresentation("Imbolc", "Observed near February 1", "A Gaelic seasonal festival later embraced in modern Pagan practice, marking the first signs of spring and the returning light.", "Cleansing and fresh beginnings")
    3, 4 -> SabbatPresentation("Ostara", "Observed at the spring equinox", "A modern Pagan equinox celebration centered on the balance of daylight and darkness and the renewal of the growing season.", "Balance and new growth")
    5, 6 -> SabbatPresentation("Beltane", "Observed near May 1", "A Gaelic fire festival associated with the opening of summer, protection, fertility, vitality, and communal celebration.", "Joy and creation")
    7, 8 -> SabbatPresentation("Lughnasadh", "Observed near August 1", "A Gaelic harvest festival associated with Lugh, seasonal gatherings, skilled craft, and the first fruits of the harvest.", "Gratitude and craft")
    9, 10 -> SabbatPresentation("Mabon", "Observed at the autumn equinox", "A modern Pagan name for the autumn equinox, when many practitioners reflect on balance, harvest, and gratitude.", "Harvest and balance")
    else -> SabbatPresentation("Samhain", "Observed from October 31", "A Gaelic seasonal threshold marking the end of harvest and opening of winter, commonly associated with remembrance of the dead.", "Remembrance and release")
}

private data class CosmicTimingPresentation(
    val planet: String,
    val planetSymbol: String,
    val planetMeaning: String,
    val moonTransit: String,
    val tidalGuidance: String,
    val alignmentGuidance: String,
)

private fun cosmicTimingFor(now: LocalDateTime): CosmicTimingPresentation {
    val planets = listOf(
        Triple("Saturn", "♄", "Support boundaries, patience, structure, and work meant to endure."),
        Triple("Jupiter", "♃", "Favor learning, generosity, confidence, and measured expansion."),
        Triple("Mars", "♂", "Direct energy toward courage, protection, movement, and decisive action."),
        Triple("Sun", "☉", "Center vitality, clarity, recognition, and purpose."),
        Triple("Venus", "♀", "Tend harmony, beauty, affection, reconciliation, and creative pleasure."),
        Triple("Mercury", "☿", "Support study, communication, travel, writing, and careful exchange."),
        Triple("Moon", "☽", "Turn inward toward intuition, home, dreams, memory, and emotional care."),
    )
    val planet = planets[(now.dayOfWeek.value + now.hour) % planets.size]
    val phase = moonPhaseFor(now.toLocalDate())
    return CosmicTimingPresentation(
        planet = planet.first,
        planetSymbol = planet.second,
        planetMeaning = planet.third,
        moonTransit = "${phase.name} influence",
        tidalGuidance = "Use the lunar phase as a reflective timing cue; local coastal tides depend on your location.",
        alignmentGuidance = "Void-of-course alerts require verified ephemeris data. Until available, favor reflection over time-sensitive decisions and follow today’s ${phase.name.lowercase(Locale.getDefault())} guidance.",
    )
}

@Composable
private fun DailySpellCard(content: Content?, onOpenToday: () -> Unit) {
    val sectionLabel = when (content?.type) {
        ContentType.SPELL -> R.string.today_spell
        ContentType.LESSON -> R.string.today_lesson
        ContentType.POTION -> R.string.today_potion
        ContentType.UNKNOWN -> R.string.home_today_practice
        null -> R.string.today_lesson
    }
    val actionLabel = when (content?.type) {
        ContentType.SPELL -> R.string.home_open_spell
        ContentType.LESSON -> R.string.home_open_lesson
        ContentType.POTION -> R.string.home_open_potion
        ContentType.UNKNOWN -> R.string.home_open_practice
        null -> R.string.home_unlock_lesson
    }
    val outerShape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, outerShape)
            .background(
                Brush.verticalGradient(listOf(Color(0xFFE8C693), Color(0xFFF8E7C2), Color(0xFFE5BE85))),
                outerShape,
            )
            .border(1.5.dp, FrameGold, outerShape)
            .padding(4.dp)
            .border(1.dp, ParchmentInk.copy(alpha = 0.40f), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = ParchmentInk.copy(alpha = 0.45f))
            Text(
                text = stringResource(sectionLabel),
                modifier = Modifier.weight(3f),
                style = MaterialTheme.typography.labelLarge,
                color = ParchmentInk,
                textAlign = TextAlign.Center,
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = ParchmentInk.copy(alpha = 0.45f))
        }
        Text(
            text = content?.title ?: stringResource(R.string.home_locked_lesson_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.displaySmall,
            color = ParchmentInk,
            textAlign = TextAlign.Center,
        )
        Text(
            text = content?.category?.displayName() ?: stringResource(R.string.home_locked_lesson_body),
            style = MaterialTheme.typography.bodyMedium,
            color = ParchmentInk,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onOpenToday,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .testTag("home-daily-action"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ButtonGold, contentColor = ParchmentInk),
        ) {
            Text(
                text = if (content == null) {
                    stringResource(R.string.home_unlock_lesson)
                } else {
                    stringResource(actionLabel)
                },
                modifier = Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Icon(
                painter = painterResource(R.drawable.ic_home_arrow),
                contentDescription = null,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
