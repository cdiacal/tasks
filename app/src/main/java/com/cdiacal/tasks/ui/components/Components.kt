package com.cdiacal.tasks.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cdiacal.tasks.R
import com.cdiacal.tasks.ui.theme.brushHero
import com.cdiacal.tasks.ui.theme.colorFondoIcono

private val ButtonShape = RoundedCornerShape(12.dp)

/** Flat panel: surface color + 1 dp outline, no elevation. Pass [onClick] to make it tappable. */
@Composable
fun TarjetaPanel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val color = MaterialTheme.colorScheme.surface
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = color, border = border) {
            Box(Modifier.padding(16.dp)) { content() }
        }
    } else {
        Surface(modifier = modifier, shape = shape, color = color, border = border) {
            Box(Modifier.padding(16.dp)) { content() }
        }
    }
}

/** Gradient button. Disabled = 45 % opacity. No ripple. */
@Composable
fun BotonPrincipal(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icono: ImageVector? = null,
) {
    Row(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .clip(ButtonShape)
            .background(brushHero())
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        if (icono != null) Icon(icono, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        Text(texto, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Surface button with a 1 dp outline. */
@Composable
fun BotonSecundario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .clip(ButtonShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, ButtonShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            texto,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Single pill: active = primary fill, inactive = surfaceVariant fill. */
@Composable
fun Pastilla(texto: String, activa: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(100))
            .background(if (activa) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            texto,
            fontSize = 12.sp,
            fontWeight = if (activa) FontWeight.SemiBold else FontWeight.Medium,
            color = if (activa) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/** Horizontally scrollable pill tabs. */
@Composable
fun FilaPestanas(
    etiquetas: List<String>,
    seleccionada: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        etiquetas.forEachIndexed { index, etiqueta ->
            Pastilla(etiqueta, activa = index == seleccionada, onClick = { onSelect(index) })
        }
    }
}

/** Rounded-square tile with an emoji (or the initial); tone chosen by id % 8. */
@Composable
fun IconoInicial(
    inicial: String,
    id: Long,
    modifier: Modifier = Modifier,
    tamano: Int = 36,
    emoji: String? = null,
) {
    val emojiVisible = emoji?.takeIf { it.isNotBlank() }
    Box(
        modifier = modifier
            .size(tamano.dp)
            .clip(RoundedCornerShape((tamano * 0.3f).dp))
            .background(colorFondoIcono(id)),
        contentAlignment = Alignment.Center,
    ) {
        if (emojiVisible != null) {
            Text(emojiVisible, fontSize = (tamano * 0.4f).sp, maxLines = 1)
        } else {
            Text(
                inicial,
                fontSize = (tamano * 0.38f).sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** Main header: navy gradient, rounded bottom corners, holds the screen's single key figure. */
@Composable
fun CabeceraHero(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(brushHero())
            .statusBarsPadding()
            .padding(start = 20.dp, end = 8.dp, top = 20.dp, bottom = 26.dp),
        content = content,
    )
}

@Composable
fun CifraHero(valor: String, modifier: Modifier = Modifier) {
    Text(
        valor,
        modifier = modifier,
        color = Color.White,
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-1).sp,
        maxLines = 1,
    )
}

@Composable
fun MiniDatoHero(etiqueta: String, valor: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(etiqueta, color = Color.White.copy(alpha = 0.8f), fontSize = 10.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(valor, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}


/**
 * Header of a secondary screen: a navy gradient bar (16 dp radius) with a white back arrow, the
 * title (and optional subtitle) and any [acciones] on the right.
 */
@Composable
fun CabeceraSimple(
    titulo: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    acciones: @Composable RowScope.() -> Unit = {},
) {
    Box(modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(brushHero())
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
            }
            Column(Modifier.weight(1f)) {
                Text(titulo, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitulo != null) {
                    Text(subtitulo, color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            acciones()
        }
    }
}

/** A flat bordered card with an optional small section title, for grouping related controls. */
@Composable
fun SeccionTarjeta(
    titulo: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    TarjetaPanel(modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (titulo != null) {
                Text(titulo, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            content()
        }
    }
}
