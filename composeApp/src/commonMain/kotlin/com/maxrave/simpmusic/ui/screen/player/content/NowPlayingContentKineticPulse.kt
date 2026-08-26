package com.maxrave.simpmusic.ui.screen.player.content

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.domain.mediaservice.handler.RepeatState
import com.maxrave.simpmusic.expect.ui.MediaPlayerView
import com.maxrave.simpmusic.extension.formatDuration
import com.maxrave.simpmusic.ui.component.ExplicitBadge
import com.maxrave.simpmusic.ui.component.rememberHolderPainter
import com.maxrave.simpmusic.ui.icon.Favorite
import com.maxrave.simpmusic.ui.icon.FavoriteBorder
import com.maxrave.simpmusic.ui.icon.Info
import com.maxrave.simpmusic.ui.icon.Lyrics
import com.maxrave.simpmusic.ui.icon.MoreVert
import com.maxrave.simpmusic.ui.icon.Pause
import com.maxrave.simpmusic.ui.icon.PlayArrow
import com.maxrave.simpmusic.ui.icon.PlaylistAdd
import com.maxrave.simpmusic.ui.icon.QueueMusic
import com.maxrave.simpmusic.ui.icon.Repeat
import com.maxrave.simpmusic.ui.icon.RepeatOne
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.Shuffle
import com.maxrave.simpmusic.ui.icon.SkipNext
import com.maxrave.simpmusic.ui.icon.SkipPrevious
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.UIEvent
import kotlin.math.roundToLong

/**
 * Kinetic Pulse — Pulse's own Now Playing identity.
 *
 * Motion-as-brand: a near-black stage where the light comes from the artwork's palette glow and
 * the violet accent; a deterministic waveform seekbar instead of a plain slider; and a play/pause
 * control that breathes while audio runs and stills when paused. The bottom navigation mirrors
 * the waveform idea, so player and chrome share one visual language.
 *
 * Fixed layout (no vertical scroll) so every element fits one screen; wide windows cap the
 * content column at 560dp and centre it.
 */

// Pure black — the simplest, cleanest backdrop. The bottom is solid black; the artwork
// sits above it with a gradient bleed; no tint needed.
private val KineticBackdrop = Color.Black

// Waveform resolution. A fixed count keeps geometry predictable across widths; each bar's
// height comes from a deterministic per-track PRNG, so the shape survives seeks,
// recompositions and restarts — it reads as "the song's fingerprint", not random noise.
private const val WAVEFORM_BAR_COUNT = 56

@Composable
fun NowPlayingContentKineticPulse(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    val colorScheme = MaterialTheme.colorScheme

    // Glow follows the track palette (animated by the shell); while it is still on its initial
    // black we fall back to the brand accent so the first paint already looks lit.
    val paletteColor = state.startColor.value
    val glowColor = if (paletteColor == Color.Black) colorScheme.primary else paletteColor

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(KineticBackdrop)
                .drawBehind {
                    // One soft radial pool of light behind where the artwork sits.
                    val radius = size.maxDimension * 0.62f
                    val center = Offset(size.width / 2f, size.height * 0.30f)
                    drawCircle(
                        brush =
                            Brush.radialGradient(
                                colors =
                                    listOf(
                                        glowColor.copy(alpha = 0.26f),
                                        glowColor.copy(alpha = 0.08f),
                                        Color.Transparent,
                                    ),
                                center = center,
                                radius = radius,
                            ),
                        radius = radius,
                        center = center,
                    )
                },
    ) {
        // Layout: the outer Box is pure black. The artwork is a centred card in the top
        // portion with a soft gradient bleed into the black bottom. Controls sit at the
        // bottom on solid black — never hidden, never clipped.

        // 1. Top bar — anchored at the top.
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            PulseTopBar(state = state, actions = actions)
        }

        // 2. Artwork — centred card, max 320dp wide, aspect 1:1, in the upper portion.
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.TopCenter,
        ) {
            PulseArtworkPager(
                state = state,
                actions = actions,
                modifier =
                    Modifier
                        .padding(top = 56.dp) // below top bar
                        .widthIn(max = 320.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .shadow(
                            elevation = 24.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = Color.Black,
                            spotColor = Color.Black.copy(alpha = 0.5f),
                        ),
            )
            // Gradient bleed from the artwork into the black background below.
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                1f to KineticBackdrop,
                            ),
                        ),
            )
        }

        // 3. Bottom content — track info, controls, always on solid black.
        // We intentionally ignore controlLayoutAlpha — Kinetic Pulse always shows controls.
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            // Push everything to the bottom with a single weight spacer.
            Spacer(modifier = Modifier.weight(1f))

            PulseTrackInfo(state = state)

            Spacer(modifier = Modifier.height(6.dp))

            PulseInlineLyric(state = state, actions = actions)

            Spacer(modifier = Modifier.height(12.dp))

            PulseWaveformSeekbarSection(state = state, actions = actions)

            Spacer(modifier = Modifier.height(10.dp))

            PulseControlsRow(state = state, actions = actions)

            Spacer(modifier = Modifier.height(4.dp))

            PulseBottomActionsRow(state = state, actions = actions)

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ─────────────────────────────────── Top bar ───────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PulseTopBar(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        IconButton(onClick = { actions.onDismiss() }) {
            Icon(
                imageVector = state.dismissIcon,
                contentDescription = null,
                tint = Color.White,
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = "PULSE",
                style = typo().labelSmall,
                color = Color.White.copy(alpha = 0.55f),
                textAlign = TextAlign.Center,
            )
            Text(
                text = state.screenData.playlistName,
                style = typo().labelMedium,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(align = Alignment.CenterVertically)
                        .basicMarquee(iterations = Int.MAX_VALUE),
            )
        }
        IconButton(onClick = { actions.onShowFullscreenLyrics() }) {
            Icon(
                imageVector = SimpIcons.Lyrics,
                contentDescription = null,
                tint = Color.White,
            )
        }
        IconButton(onClick = { actions.onShowMoreSheet() }) {
            Icon(
                imageVector = SimpIcons.MoreVert,
                contentDescription = null,
                tint = Color.White,
            )
        }
    }
}

// ─────────────────────────────── Artwork pager ─────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PulseArtworkPager(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    modifier: Modifier = Modifier,
) {
    val isRepeatOne = state.controllerState.repeatState is RepeatState.One
    val noIndication = remember { MutableInteractionSource() }

    HorizontalPager(
        state = state.artworkPagerState,
        modifier = modifier,
        beyondViewportPageCount = 1,
        userScrollEnabled = !isRepeatOne && state.artworkQueue.isNotEmpty(),
        key = { idx ->
            val vid = state.artworkQueue.getOrNull(idx)?.videoId.orEmpty()
            "kinetic_artwork_${vid}_$idx"
        },
    ) { page ->
        val pageTrack = state.artworkQueue.getOrNull(page)
        val isCurrentPage = page == state.currentOrderIndex
        val pageHasCanvas = isCurrentPage && state.screenData.canvasData != null

        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .fillMaxSize()
                    // Tap toggles controls only while a canvas covers this page (matches Classic).
                    .clickable(
                        enabled = pageHasCanvas,
                        onClick = { actions.onToggleControls() },
                        indication = null,
                        interactionSource = noIndication,
                    ),
        ) {
            if (pageHasCanvas) {
                // Canvas replaces the artwork in-place, inside the same rounded frame.
                Crossfade(
                    targetState = state.screenData.canvasData?.isVideo,
                    animationSpec = tween(durationMillis = 300),
                    label = "kineticCanvasCrossfade",
                ) { isVideo ->
                    if (isVideo == true) {
                        state.screenData.canvasData?.url?.let { url ->
                            MediaPlayerView(
                                url = url,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    } else {
                        AsyncImage(
                            model =
                                ImageRequest
                                    .Builder(LocalPlatformContext.current)
                                    .data(state.screenData.canvasData?.url)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .diskCacheKey(state.screenData.canvasData?.url)
                                    .crossfade(true)
                                    .build(),
                            placeholder = rememberHolderPainter(),
                            error = rememberHolderPainter(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            } else {
                val thumbUrl = pageTrack?.thumbnails?.maxByOrNull { it.width * it.height }?.url
                AsyncImage(
                    model =
                        ImageRequest
                            .Builder(LocalPlatformContext.current)
                            .data(thumbUrl)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .diskCacheKey(thumbUrl)
                            .crossfade(true)
                            .build(),
                    placeholder = rememberHolderPainter(),
                    error = rememberHolderPainter(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

// ─────────────────────────────── Track info block ──────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PulseTrackInfo(state: NowPlayingContentState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (state.screenData.isExplicit) {
                ExplicitBadge(modifier = Modifier.padding(end = 8.dp))
            }
            Text(
                text = state.screenData.nowPlayingTitle,
                style = typo().titleLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier =
                    Modifier
                        .widthIn(max = 440.dp)
                        .basicMarquee(iterations = Int.MAX_VALUE),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = state.screenData.artistName,
            style = typo().bodyMedium,
            color = Color.White.copy(alpha = 0.65f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
    }
}

// ─────────────────────────────── Inline lyric line ─────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PulseInlineLyric(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    val lyrics = state.screenData.lyricsData?.lyrics
    val hasSyncedLyrics =
        lyrics != null &&
            lyrics.syncType != null &&
            lyrics.syncType != "UNSYNCED" &&
            lyrics.lines != null
    val lineText =
        if (!hasSyncedLyrics ||
            state.screenData.canvasData != null ||
            state.currentLyricLineIndex < 0
        ) {
            ""
        } else {
            lyrics
                ?.lines
                ?.getOrNull(state.currentLyricLineIndex)
                ?.words
                ?.stripRichSyncTimestamps()
                .orEmpty()
        }

    Crossfade(
        targetState = lineText,
        animationSpec = tween(durationMillis = 300),
        label = "kineticInlineLyric",
        modifier = Modifier.fillMaxWidth(),
    ) { text ->
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            if (text.isNotEmpty()) {
                Text(
                    text = text,
                    style = typo().labelLarge,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 40.dp)
                            .basicMarquee(iterations = Int.MAX_VALUE)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                            ) { actions.onShowFullscreenLyrics() },
                )
            }
        }
    }
}

// ────────────────────────────── Waveform seekbar ───────────────────────────────

@Composable
private fun PulseWaveformSeekbarSection(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    val colorScheme = MaterialTheme.colorScheme
    val rawFraction = (state.sliderValue / 100f).coerceIn(0f, 1f)
    // Animate the fraction so the waveform bars slide smoothly instead of jumping.
    val progressFraction by animateFloatAsState(
        targetValue = rawFraction,
        animationSpec = tween(durationMillis = 120, easing = LinearEasing),
        label = "waveformProgress",
    )
    val seekEnabled = state.timelineState.total > 0L && !state.timelineState.loading

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
    ) {
        PulseWaveformSeekbar(
            progressFraction = progressFraction,
            seed = state.screenData.thumbnailURL.orEmpty().ifEmpty { state.screenData.nowPlayingTitle },
            playedColor = colorScheme.primary,
            unplayedColor = Color.White.copy(alpha = 0.22f),
            enabled = seekEnabled,
            modifier = Modifier.fillMaxWidth(),
            onSeekFraction = { fraction -> actions.onSliderChange(fraction * 100f) },
            onSeekCommit = { actions.onSliderChangeFinished() },
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = formatDuration((state.timelineState.total * progressFraction).roundToLong()),
                style = typo().labelSmall,
                color = Color.White.copy(alpha = 0.6f),
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text =
                    if (state.timelineState.total > 0L) {
                        "-" + formatDuration((state.timelineState.total * (1f - progressFraction)).roundToLong())
                    } else {
                        formatDuration(0L)
                    },
                style = typo().labelSmall,
                color = Color.White.copy(alpha = 0.6f),
            )
        }
    }
}

/**
 * The signature control: a bar-waveform whose filled portion tracks playback.
 *
 * Uses Row + Box with weight(1f) instead of Canvas because Canvas has no intrinsic size
 * and silently renders at 0×0 inside Column rows. Each bar is a separate composable that
 * gets guaranteed width from weight distribution.
 *
 * Interaction maps onto the shell's slider contract exactly like the M3 Slider does elsewhere:
 * live drags call [onSeekFraction] (which lands in `onSliderChange`, flipping the shell into
 * sliding mode), release calls [onSeekCommit] (`onSliderChangeFinished` performs the seek).
 */
@Composable
private fun PulseWaveformSeekbar(
    progressFraction: Float,
    seed: String,
    playedColor: Color,
    unplayedColor: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onSeekFraction: (Float) -> Unit,
    onSeekCommit: () -> Unit,
) {
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val shownFraction = dragFraction ?: progressFraction
    val amplitudes = remember(seed) { generatePulseAmplitudes(seed, WAVEFORM_BAR_COUNT) }
    val boundary = (shownFraction.coerceIn(0f, 1f) * WAVEFORM_BAR_COUNT)

    val gestureModifier =
        if (enabled) {
            Modifier
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onSeekFraction(fraction)
                        onSeekCommit()
                    }
                }.pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            dragFraction = fraction
                            onSeekFraction(fraction)
                        },
                        onDragEnd = {
                            onSeekCommit()
                            dragFraction = null
                        },
                        onDragCancel = { dragFraction = null },
                    ) { change, _ ->
                        change.consume()
                        val fraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        dragFraction = fraction
                        onSeekFraction(fraction)
                    }
                }
        } else {
            Modifier
        }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .height(44.dp)
                .then(gestureModifier),
    ) {
        amplitudes.forEachIndexed { index, amplitude ->
            val barFraction = (amplitude * 0.72f + 0.14f) // height as fraction of container
            val barColor =
                when {
                    index + 1 <= boundary -> playedColor
                    index >= boundary -> unplayedColor
                    else -> playedColor // boundary bar — mostly played
                }
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(barFraction)
                        .background(barColor, RoundedCornerShape(50)),
            )
        }
    }
}

/** Deterministic per-track bar heights: an FNV-1a hash seeds an xorshift32 generator. */
private fun generatePulseAmplitudes(seed: String, count: Int): FloatArray {
    var h: Int = -2128831035 // 0x811C9DC5 — FNV offset basis
    for (ch in seed) {
        h = h xor ch.code
        h *= 16777619 // FNV prime — Int overflow wraps identically on every platform
    }
    if (h == 0) h = 0x6D2B79F5.toInt() // non-zero guarantee for xorshift
    return FloatArray(count) {
        h = h xor (h shl 13)
        h = h xor (h ushr 17)
        h = h xor (h shl 5)
        val raw = ((h ushr 8) and 0xFF) / 255f
        0.22f + 0.78f * raw
    }
}

// ────────────────────────────── Controls rows ──────────────────────────────────

@Composable
private fun PulseControlsRow(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    val colorScheme = MaterialTheme.colorScheme
    val accent = colorScheme.primary
    val inactive = Color.White.copy(alpha = 0.55f)
    val isRepeatOne = state.controllerState.repeatState is RepeatState.One

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth(),
    ) {
        IconButton(onClick = { actions.onUIEvent(UIEvent.Shuffle) }) {
            Icon(
                imageVector = SimpIcons.Shuffle,
                contentDescription = null,
                tint = if (state.controllerState.isShuffle) accent else inactive,
            )
        }
        IconButton(
            enabled = state.controllerState.isPreviousAvailable,
            onClick = { actions.onUIEvent(UIEvent.Previous) },
        ) {
            Icon(SimpIcons.SkipPrevious, contentDescription = null, tint = Color.White)
        }

        PulseBreathingPlayButton(
            isPlaying = state.controllerState.isPlaying,
            accent = accent,
            onToggle = { actions.onUIEvent(UIEvent.PlayPause) },
        )

        IconButton(
            enabled = state.controllerState.isNextAvailable,
            onClick = { actions.onUIEvent(UIEvent.Next) },
        ) {
            Icon(SimpIcons.SkipNext, contentDescription = null, tint = Color.White)
        }
        IconButton(onClick = { actions.onUIEvent(UIEvent.Repeat) }) {
            Icon(
                imageVector = if (isRepeatOne) SimpIcons.RepeatOne else SimpIcons.Repeat,
                contentDescription = null,
                tint = if (state.controllerState.repeatState !is RepeatState.None) accent else inactive,
            )
        }
    }
}

/**
 * The breathing play button: scales and pulses its halo on an ~2.3s cycle while audio runs;
 * everything rests at scale 1f when paused. The infinite transition values are read
 * conditionally so a paused player pays no recomposition cost for them.
 */
@Composable
private fun PulseBreathingPlayButton(
    isPlaying: Boolean,
    accent: Color,
    onToggle: () -> Unit,
) {
    val breath = rememberInfiniteTransition(label = "pulseBreath")
    val breathScale by breath.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.07f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1150, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "pulseBreathScale",
    )
    val breathGlow by breath.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1150, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "pulseBreathGlow",
    )

    val scale = if (isPlaying) breathScale else 1f
    val ringAlpha = if (isPlaying) breathGlow * 0.45f else 0.22f
    val glowAlpha = if (isPlaying) breathGlow * 0.30f else 0.12f
    val noIndication = remember { MutableInteractionSource() }

    Box(contentAlignment = Alignment.Center) {
        // Breathing halo ring.
        Spacer(
            modifier =
                Modifier
                    .size(106.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }.border(width = 2.dp, color = accent.copy(alpha = ringAlpha), shape = CircleShape),
        )
        // Soft radial glow under the button itself.
        Spacer(
            modifier =
                Modifier
                    .size(96.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }.drawBehind {
                        drawCircle(
                            brush =
                                Brush.radialGradient(
                                    colors = listOf(accent.copy(alpha = glowAlpha), Color.Transparent),
                                ),
                        )
                    },
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(84.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }.shadow(elevation = 24.dp, shape = CircleShape, spotColor = accent.copy(alpha = 0.7f))
                    .background(
                        brush = Brush.verticalGradient(listOf(lerp(accent, Color.White, 0.18f), accent)),
                        shape = CircleShape,
                    ).clickable(
                        interactionSource = noIndication,
                        indication = null,
                        onClick = onToggle,
                    ),
        ) {
            Crossfade(
                targetState = isPlaying,
                animationSpec = tween(durationMillis = 180),
                label = "pulsePlayPauseIcon",
            ) { playing ->
                Icon(
                    imageVector = if (playing) SimpIcons.Pause else SimpIcons.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(38.dp),
                )
            }
        }
    }
}

@Composable
private fun PulseBottomActionsRow(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    val colorScheme = MaterialTheme.colorScheme
    val muted = Color.White.copy(alpha = 0.75f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth(),
    ) {
        IconButton(onClick = { actions.onUIEvent(UIEvent.ToggleLike) }) {
            Icon(
                imageVector = if (state.likeStatus || state.controllerState.isLiked) SimpIcons.Favorite else SimpIcons.FavoriteBorder,
                contentDescription = null,
                tint = if (state.likeStatus || state.controllerState.isLiked) colorScheme.primary else muted,
            )
        }
        IconButton(onClick = { actions.onShowAddToPlaylist() }) {
            Icon(SimpIcons.PlaylistAdd, contentDescription = null, tint = muted)
        }
        IconButton(onClick = { actions.onShowQueue() }) {
            Icon(SimpIcons.QueueMusic, contentDescription = null, tint = muted)
        }
        IconButton(onClick = { actions.onShowInfo() }) {
            Icon(SimpIcons.Info, contentDescription = null, tint = muted)
        }
    }
}
