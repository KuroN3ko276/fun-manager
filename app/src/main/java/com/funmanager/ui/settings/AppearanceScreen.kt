package com.funmanager.ui.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.funmanager.FunManagerApp
import com.funmanager.R
import com.funmanager.ui.components.AppBackground
import com.funmanager.ui.components.GlassCard
import com.funmanager.ui.components.MoneyText
import com.funmanager.ui.theme.Accent
import com.funmanager.ui.theme.ExpenseColor
import com.funmanager.ui.theme.FunManagerTheme
import com.funmanager.ui.theme.IncomeColor
import com.funmanager.ui.theme.paletteFor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme

@Composable
fun AppearanceRoute(onBack: () -> Unit) {
    val container = (LocalContext.current.applicationContext as FunManagerApp).container
    val accent by container.accent.collectAsStateWithLifecycle()
    AppearanceScreen(accent, onBack, container::setAccent)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppearanceScreen(selected: Accent, onBack: () -> Unit, onSelect: (Accent) -> Unit) {
    val dark = isSystemInDarkTheme()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.appearance)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                },
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.appearance_preview), style = MaterialTheme.typography.labelLarge)
                    MoneyText(12_450_000, style = MaterialTheme.typography.headlineMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        MoneyText(15_000_000, color = IncomeColor)
                        MoneyText(2_550_000, color = ExpenseColor)
                    }
                }
            }
            Text(stringResource(R.string.accent_color), style = MaterialTheme.typography.titleMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = 4,
            ) {
                Accent.entries.forEach { accent ->
                    AccentSwatch(accent, dark, accent == selected, Modifier.weight(1f)) { onSelect(accent) }
                }
            }
            Text(
                stringResource(R.string.appearance_dark_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AccentSwatch(accent: Accent, dark: Boolean, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val p = paletteFor(accent, dark)
    Column(
        modifier.selectable(selected, onClick = onClick, role = Role.RadioButton),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(56.dp)
                .background(Brush.linearGradient(listOf(Color(p.primary), Color(p.tertiary))), CircleShape)
                .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = Color(p.onPrimary))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            accent.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppearancePreview() {
    FunManagerTheme(Accent.OCEAN) {
        AppBackground { AppearanceScreen(Accent.OCEAN, onBack = {}, onSelect = {}) }
    }
}
