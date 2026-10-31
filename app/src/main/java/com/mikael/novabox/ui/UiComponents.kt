package com.mikael.novabox.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val NovaBackground = Color(0xFF080D0A)
val NovaPanel = Color(0xFF111A14)
val NovaPanelRaised = Color(0xFF19251C)
val NovaGreen = Color(0xFF9BFF62)
val NovaMuted = Color(0xFF91A096)
val NovaBorder = Color(0xFF26372B)
val NovaWarning = Color(0xFFFFCF77)
val NovaText = Color(0xFFF2F7F1)

@Composable
fun PageHeader(
    eyebrow: String,
    title: String,
    subtitle: String,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(eyebrow.uppercase(), color = NovaGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text(title, color = NovaText, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = NovaMuted, fontSize = 12.sp)
        }
        trailing?.invoke()
    }
}

@Composable
fun PrimaryAction(
    text: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(NovaGreen).clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = NovaBackground, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(9.dp))
        }
        Text(text, color = NovaBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
fun InfoNotice(title: String, body: String, warning: Boolean = false) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (warning) Color(0xFF2A2417) else Color(0xFF17231A),
        shape = RoundedCornerShape(15.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = if (warning) NovaWarning else NovaGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(body, color = NovaMuted, fontSize = 11.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
fun SmallTag(text: String) {
    Box(
        Modifier.clip(RoundedCornerShape(8.dp)).background(NovaPanelRaised)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(text, color = NovaMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun StatusLabel(text: String, ready: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(6.dp).background(if (ready) NovaGreen else NovaWarning, CircleShape))
        Text(text, color = if (ready) NovaGreen else NovaWarning, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun EmptyCard(icon: ImageVector, title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(NovaPanel).border(1.dp, NovaBorder, RoundedCornerShape(20.dp)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = NovaGreen, modifier = Modifier.size(34.dp))
        Text(title, color = NovaText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(subtitle, color = NovaMuted, fontSize = 11.sp, lineHeight = 16.sp)
    }
}

@Composable
fun SelectableChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier.clip(RoundedCornerShape(11.dp))
            .background(if (selected) Color(0xFF203326) else NovaPanelRaised)
            .border(1.dp, if (selected) Color(0xFF4F9F35) else NovaBorder, RoundedCornerShape(11.dp))
            .clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(text, color = if (selected) NovaGreen else NovaMuted, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (selected) {
            Spacer(Modifier.size(4.dp))
            Icon(Icons.Default.Check, contentDescription = null, tint = NovaGreen, modifier = Modifier.size(12.dp))
        }
    }
}
