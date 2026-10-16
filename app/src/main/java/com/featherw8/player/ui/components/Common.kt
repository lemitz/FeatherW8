package com.featherw8.player.ui.components

import com.featherw8.player.R
import android.util.Size
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.featherw8.player.ui.model.AppPage
import com.featherw8.player.ui.theme.CardBlack
import com.featherw8.player.ui.theme.MutedText

@Composable
internal fun AppToolbar(page: AppPage, title: String?, onSettings: () -> Unit, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (page == AppPage.LIBRARY || page == AppPage.PLAYLISTS) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                ImageIconLogo()
                Text("FeatherW8", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
            IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, contentDescription = "Settings") }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                Text(title.orEmpty(), fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.size(48.dp))
        }
    }
}

@Composable
internal fun ImageIconLogo() {
    androidx.compose.foundation.Image(
        painter = painterResource(R.drawable.featherw8_icon),
        contentDescription = "FeatherW8",
        modifier = Modifier.size(30.dp).clip(CircleShape)
    )
}

@Composable
internal fun SectionLabel(label: String) {
    Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold)
}

@Composable
internal fun EmptyCard(title: String, message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = CardBlack), shape = RoundedCornerShape(15.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(17.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(message, color = MutedText, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
