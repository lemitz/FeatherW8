package com.featherw8.player.ui.screens

import android.graphics.Typeface
import android.util.Size
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.featherw8.player.ui.player.PlaybackProgressBar
import com.featherw8.player.ui.theme.AccentOptions
import com.featherw8.player.ui.theme.CardBlack
import com.featherw8.player.ui.theme.DeviceProfile
import com.featherw8.player.ui.theme.FeatherRed
import com.featherw8.player.ui.theme.MutedText
import com.featherw8.player.ui.theme.PrimaryText

@Composable
internal fun SettingsScreen(
    profile: DeviceProfile,
    dotFontEnabled: Boolean,
    onDotFontChange: (Boolean) -> Unit,
    accentId: String,
    onAccentChange: (String) -> Unit,
    minimumSizeKb: Float,
    onMinimumSizeChange: (Float) -> Unit,
    minimumDurationSeconds: Float,
    onMinimumDurationChange: (Float) -> Unit,
    onScan: () -> Unit,
    bottomOverlayPadding: Dp = 0.dp
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(bottom = bottomOverlayPadding)) {
        Text("Library filters", color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
        Card(colors = CardDefaults.cardColors(containerColor = CardBlack), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                RangeSetting("Minimum file size", minimumSizeKb, "KB", 0f..1500f, 50f, onMinimumSizeChange)
                HorizontalDivider(color = Color.White.copy(alpha = .06f))
                RangeSetting("Minimum duration", minimumDurationSeconds, "sec", 0f..40f, 5f, onMinimumDurationChange)
            }
        }

        Text("Accent color", color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 19.dp, bottom = 8.dp))
        Card(colors = CardDefaults.cardColors(containerColor = CardBlack), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 13.dp)) {
                Text("Choose an accent", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                AccentColorPicker(accentId = accentId, onAccentChange = onAccentChange, modifier = Modifier.padding(top = 12.dp))
            }
        }

        Text("Library scan", color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 19.dp, bottom = 8.dp))
        Card(colors = CardDefaults.cardColors(containerColor = CardBlack), shape = RoundedCornerShape(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onScan).padding(horizontal = 13.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = .06f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = PrimaryText, modifier = Modifier.size(17.dp))
                }
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text("Scan Library", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Text("Search audio across internal storage", color = MutedText, fontSize = 9.sp, modifier = Modifier.padding(top = 3.dp))
                }
            }
        }

        Text("System", color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 19.dp, bottom = 8.dp))
        Card(colors = CardDefaults.cardColors(containerColor = CardBlack), shape = RoundedCornerShape(16.dp)) {
            Column {
                if (profile == DeviceProfile.NOTHING) {
                    SettingToggleRow(
                        title = "Nothing dot font",
                        subtitle = "Use the dotted typeface across the app",
                        checked = dotFontEnabled,
                        onCheckedChange = onDotFontChange
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = .06f))
                    SettingInfoRow("Text style", if (dotFontEnabled) "NDot55 system typeface" else "System sans-serif", Icons.Filled.Settings)
                } else {
                    SettingInfoRow("Typeface", "System default", Icons.Filled.Settings)
                }
                if (profile == DeviceProfile.REALME) {
                    HorizontalDivider(color = Color.White.copy(alpha = .06f))
                    SettingInfoRow("Mini Capsule", "Automatic on this Realme device", Icons.Filled.Waves, badge = "AUTO")
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
internal fun AccentColorPicker(accentId: String, onAccentChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AccentOptions.forEach { option ->
            val selected = option.id == accentId
            Column(
                modifier = Modifier.width(58.dp).clickable { onAccentChange(option.id) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(option.color)
                        .border(2.dp, if (selected) Color.White else Color.Transparent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) Text("✓", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Text(option.label, fontSize = 9.sp, color = if (selected) PrimaryText else MutedText, modifier = Modifier.padding(top = 5.dp))
            }
        }
    }
}

@Composable
internal fun SettingToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 10.dp)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MutedText, fontSize = 9.sp, modifier = Modifier.padding(top = 3.dp))
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

internal fun snapToStep(value: Float, range: ClosedFloatingPointRange<Float>, stepSize: Float): Float {
    val safeValue = value.takeIf { it.isFinite() } ?: range.start
    val clamped = safeValue.coerceIn(range)
    if (stepSize <= 0f) return clamped
    val stepIndex = ((clamped - range.start) / stepSize).roundToInt()
    return (range.start + stepIndex * stepSize).coerceIn(range)
}

@Composable
internal fun RangeSetting(
    label: String,
    value: Float,
    unit: String,
    range: ClosedFloatingPointRange<Float>,
    stepSize: Float,
    onChange: (Float) -> Unit
) {
    val currentValue = snapToStep(value, range, stepSize)
    val rangeWidth = (range.endInclusive - range.start).coerceAtLeast(1f)
    val divisions = ((range.endInclusive - range.start) / stepSize).roundToInt().coerceAtLeast(1)
    val discreteSteps = (divisions - 1).coerceAtLeast(0)
    val progress = ((currentValue - range.start) / rangeWidth).coerceIn(0f, 1f)

    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("${currentValue.toInt()} $unit", fontSize = 10.sp, color = FeatherRed)
        }
        PlaybackProgressBar(
            value = progress,
            enabled = true,
            modifier = Modifier.fillMaxWidth(),
            steps = discreteSteps,
            emitDuringDrag = true,
            onSeek = { fraction ->
                val rawValue = range.start + fraction.coerceIn(0f, 1f) * (range.endInclusive - range.start)
                onChange(snapToStep(rawValue, range, stepSize))
            }
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 1.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${range.start.toInt()} $unit", color = MutedText, fontSize = 9.sp)
            Text("${range.endInclusive.toInt()} $unit", color = MutedText, fontSize = 9.sp)
        }
    }
}

@Composable
internal fun SettingInfoRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, badge: String? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = .06f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = PrimaryText, modifier = Modifier.size(17.dp))
        }
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MutedText, fontSize = 9.sp, modifier = Modifier.padding(top = 3.dp))
        }
        if (badge != null) Text(badge, color = FeatherRed, fontWeight = FontWeight.Bold, fontSize = 9.sp)
    }
}
