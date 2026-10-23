package com.marcopt74.damelume.ui.timer

import android.Manifest
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.marcopt74.damelume.R
import com.marcopt74.damelume.domain.TimerEngine
import com.marcopt74.damelume.domain.model.AppLocale
import com.marcopt74.damelume.domain.model.Mode
import com.marcopt74.damelume.domain.model.TimerAction
import com.marcopt74.damelume.system.notify.notificationRationale
import com.marcopt74.damelume.ui.theme.EaseSmoothOut
import com.marcopt74.damelume.ui.theme.LocalModeColors
import com.marcopt74.damelume.ui.theme.LumePalette
import com.marcopt74.damelume.ui.theme.LumeTheme
import com.marcopt74.damelume.ui.theme.LumeTypography

@Composable
fun TimerScreen(viewModel: TimerViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = LocalActivity.current
    LaunchedEffect(lifecycleOwner, activity) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.onResume(notificationRationale(activity, Build.VERSION.SDK_INT))
        }
    }
    TimerContent(
        state = state,
        reducedMotion = rememberReducedMotion(),
        onAction = viewModel::onAction,
        onFrame = viewModel::onFrame,
    )
}

@Composable
fun TimerContent(
    state: TimerUiState,
    reducedMotion: Boolean,
    onAction: (TimerAction) -> Unit,
    onFrame: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LumeTheme(mode = state.mode, running = state.isRunning, reducedMotion = reducedMotion) {
        val colors = LocalModeColors.current
        val focusRequester = remember { FocusRequester() }
        var showRationale by remember { mutableStateOf(false) }
        var permissionInFlight by remember { mutableStateOf(false) }
        val permissionLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                permissionInFlight = false
                onAction(TimerAction.MarkNotificationAsked)
            }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
        RunningClockEffect(state = state, reducedMotion = reducedMotion, onFrame = onFrame)
        WindowTitleEffect(state = state)
        val onPrimary = {
            handlePrimary(
                state = state,
                onAction = onAction,
                launcher = permissionLauncher,
                permissionInFlight = permissionInFlight,
                markInFlight = { permissionInFlight = true },
                showRationale = { showRationale = true },
            )
        }
        Box(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .focusRequester(focusRequester)
                    .focusable()
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.Spacebar) {
                            onAction(if (state.isRunning) TimerAction.Pause else TimerAction.Start)
                            true
                        } else {
                            false
                        }
                    },
        ) {
            Column(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .widthIn(max = 576.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 56.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                Header(state = state, onAction = onAction)
                PermissionBanners(state = state, onAction = onAction)
                DialBlock(state = state, reducedMotion = reducedMotion, accent = colors.accent)
                Controls(state = state, onPrimary = onPrimary, onAction = onAction)
                DurationCard(state = state, accent = colors.accent, onAction = onAction)
            }
        }
        if (showRationale) {
            NotificationRationaleDialog(
                onConfirm = {
                    showRationale = false
                    permissionInFlight = true
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
                onDismiss = { showRationale = false },
            )
        }
    }
}

@Composable
private fun RunningClockEffect(
    state: TimerUiState,
    reducedMotion: Boolean,
    onFrame: () -> Unit,
) {
    LaunchedEffect(state.isRunning, state.endsAtEpochMs, reducedMotion) {
        if (!state.isRunning) return@LaunchedEffect
        if (reducedMotion) {
            while (true) {
                onFrame()
                kotlinx.coroutines.delay(1_000)
            }
        } else {
            while (true) {
                withFrameNanos { onFrame() }
            }
        }
    }
}

@Composable
private fun WindowTitleEffect(state: TimerUiState) {
    val activity = LocalActivity.current
    val status = stringResource(statusRes(state.status))
    val title =
        if (state.isRunning) {
            stringResource(R.string.window_title, status, state.timeText)
        } else {
            stringResource(R.string.app_name)
        }
    SideEffect {
        activity?.title = title
    }
}

@Composable
private fun PermissionBanners(
    state: TimerUiState,
    onAction: (TimerAction) -> Unit,
) {
    val context = LocalContext.current
    if (state.showNotificationBanner) {
        PermissionBanner(
            message = stringResource(R.string.banner_notifications_off),
            actionLabel = stringResource(R.string.open_settings),
            dismissLabel = stringResource(R.string.dismiss),
            onAction = { context.startActivity(notificationSettingsIntent(context.packageName)) },
            onDismiss = { onAction(TimerAction.DismissNotificationBanner) },
        )
    }
    if (state.showExactAlarmBanner) {
        PermissionBanner(
            message = stringResource(R.string.banner_exact_alarm),
            actionLabel = stringResource(R.string.banner_exact_alarm_action),
            dismissLabel = stringResource(R.string.dismiss),
            onAction = { context.startActivity(exactAlarmSettingsIntent(context.packageName)) },
            onDismiss = null,
        )
    }
}

@Composable
private fun NotificationRationaleDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.notif_rationale_title)) },
        text = { Text(stringResource(R.string.notif_rationale_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.notif_rationale_allow)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dismiss)) }
        },
    )
}

private fun handlePrimary(
    state: TimerUiState,
    onAction: (TimerAction) -> Unit,
    launcher: ActivityResultLauncher<String>,
    permissionInFlight: Boolean,
    markInFlight: () -> Unit,
    showRationale: () -> Unit,
) {
    val starting = state.primary != PrimaryControl.PAUSE
    onAction(if (state.isRunning) TimerAction.Pause else TimerAction.Start)
    if (!starting || permissionInFlight) return
    when {
        state.requestNotificationOnStart -> {
            markInFlight()
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        state.showNotificationRationale -> showRationale()
    }
}

@Composable
private fun Header(
    state: TimerUiState,
    onAction: (TimerAction) -> Unit,
) {
    val colors = LocalModeColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = Color(LumePalette.FG),
            )
            Text(
                text = stringResource(R.string.tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(LumePalette.MUTED),
            )
            Spacer(modifier = Modifier.height(8.dp))
            LocaleToggle(
                selected = state.locale ?: AppLocale.PT,
                groupDescription = stringResource(R.string.language),
                ptLabel = stringResource(R.string.locale_pt),
                enLabel = stringResource(R.string.locale_en),
                accent = colors.button,
                onSelect = { onAction(TimerAction.SetLocale(it)) },
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SoundToggle(
                enabled = state.soundEnabled,
                description = stringResource(if (state.soundEnabled) R.string.sound_off else R.string.sound_on),
                accent = colors.accent,
                onToggle = { onAction(TimerAction.SetSoundEnabled(!state.soundEnabled)) },
            )
            SessionCounter(
                count = state.todayCount,
                countLabel = pluralStringResource(R.plurals.session_word, state.todayCount),
                countDescription = pluralStringResource(R.plurals.session_count, state.todayCount, state.todayCount),
                cycleWorkCount = state.cycleWorkCount,
                cycleDescription = stringResource(R.string.cycle_progress_cd, state.cycleWorkCount),
                resetLabel = stringResource(R.string.reset_sessions),
                resetEnabled = state.resetSessionsEnabled,
                accent = colors.accent,
                onReset = { onAction(TimerAction.ResetSessions) },
            )
        }
    }
}

@Composable
private fun DialBlock(
    state: TimerUiState,
    reducedMotion: Boolean,
    accent: Color,
) {
    val status = stringResource(statusRes(state.status))
    val description = stringResource(R.string.remaining, status, state.timeText)
    val width = screenWidth()
    val maxDial = if (width >= 768.dp) 448.dp else 320.dp
    val timerSize = (width.value * 0.07f + 16f).coerceIn(52f, 84f).sp
    Box(
        modifier =
            Modifier
                .widthIn(max = maxDial)
                .fillMaxWidth()
                .aspectRatio(1f)
                .clearAndSetSemantics {
                    contentDescription = description
                    liveRegion = LiveRegionMode.Polite
                },
        contentAlignment = Alignment.Center,
    ) {
        Dial(
            remainingMs = state.remainingMs,
            totalMs = state.totalMs,
            running = state.isRunning,
            reducedMotion = reducedMotion,
            accent = accent,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(running = state.isRunning, reducedMotion = reducedMotion, accent = accent)
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = status.uppercase(),
                    color = Color(LumePalette.MUTED),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.2.sp,
                )
            }
            Text(
                text = state.timeText,
                color = Color(LumePalette.FG),
                textAlign = TextAlign.Center,
                style = LumeTypography.timer.copy(fontSize = timerSize),
            )
        }
    }
}

@Composable
private fun StatusDot(
    running: Boolean,
    reducedMotion: Boolean,
    accent: Color,
) {
    val transition = rememberInfiniteTransition(label = "liveDot")
    val pulse = running && !reducedMotion
    val animated by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (pulse) 0.82f else 1f,
        animationSpec =
            if (pulse) {
                infiniteRepeatable(
                    animation = tween(durationMillis = 1_800, easing = EaseSmoothOut),
                    repeatMode = RepeatMode.Reverse,
                )
            } else {
                infiniteRepeatable(animation = tween(durationMillis = 1), repeatMode = RepeatMode.Restart)
            },
        label = "liveDotScale",
    )
    val color = if (running) accent else Color(LumePalette.FG).copy(alpha = 0.20f)
    Box(
        modifier =
            Modifier
                .size(6.dp)
                .graphicsLayer {
                    scaleX = if (pulse) animated else 1f
                    scaleY = if (pulse) animated else 1f
                    alpha = if (pulse) 0.4f + (animated - 0.82f) / 0.18f * 0.6f else 1f
                }.clip(CircleShape)
                .background(color),
    )
}

@Composable
private fun Controls(
    state: TimerUiState,
    onPrimary: () -> Unit,
    onAction: (TimerAction) -> Unit,
) {
    val colors = LocalModeColors.current
    val maxDial = if (screenWidth() >= 768.dp) 448.dp else 320.dp
    Column(
        modifier = Modifier.widthIn(max = maxDial).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val resetInteraction = remember { MutableInteractionSource() }
            val resetScale = pressScale(resetInteraction)
            OutlinedButton(
                onClick = { onAction(TimerAction.Reset) },
                interactionSource = resetInteraction,
                modifier =
                    Modifier.size(56.dp).graphicsLayer {
                        scaleX = resetScale
                        scaleY = resetScale
                    },
                shape = CircleShape,
            ) {
                Icon(imageVector = Icons.Filled.Replay, contentDescription = stringResource(R.string.reset))
            }
            val primaryInteraction = remember { MutableInteractionSource() }
            val primaryScale = pressScale(primaryInteraction)
            Button(
                onClick = onPrimary,
                interactionSource = primaryInteraction,
                modifier =
                    Modifier
                        .weight(1f)
                        .height(56.dp)
                        .graphicsLayer {
                            scaleX = primaryScale
                            scaleY = primaryScale
                        },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = colors.button, contentColor = colors.onButton),
            ) {
                PrimaryGlyph(state.primary)
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = stringResource(primaryRes(state.primary)), fontWeight = FontWeight.SemiBold)
            }
        }
        if (state.showSkip) {
            TextButton(
                onClick = { onAction(TimerAction.SkipBreak) },
                modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
                Icon(Icons.Filled.SkipNext, contentDescription = null)
                Spacer(modifier = Modifier.size(6.dp))
                Text(text = stringResource(R.string.skip_break))
            }
        }
    }
}

@Composable
private fun PrimaryGlyph(primary: PrimaryControl) {
    val showPause = primary == PrimaryControl.PAUSE
    val amount by animateFloatAsState(
        targetValue = if (showPause) 1f else 0f,
        animationSpec = tween(durationMillis = PrimaryMotion.ICON_MS, easing = EaseSmoothOut),
        label = "primaryIcon",
    )
    Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
        FadingIcon(Icons.Filled.Pause, amount)
        FadingIcon(Icons.Filled.PlayArrow, 1f - amount)
    }
}

@Composable
private fun FadingIcon(
    image: ImageVector,
    amount: Float,
) {
    val hidden = (1f - amount).coerceIn(0f, 1f)
    Icon(
        imageVector = image,
        contentDescription = null,
        modifier =
            Modifier
                .graphicsLayer {
                    alpha = amount
                    val scale = PrimaryMotion.HIDDEN_SCALE + (1f - PrimaryMotion.HIDDEN_SCALE) * amount
                    scaleX = scale
                    scaleY = scale
                }.then(if (hidden > 0.05f) Modifier.blur((4f * hidden).dp) else Modifier),
    )
}

@Composable
private fun pressScale(interaction: MutableInteractionSource): Float {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) PrimaryMotion.PRESS_SCALE else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "press",
    )
    return scale
}

@Composable
private fun DurationCard(
    state: TimerUiState,
    accent: Color,
    onAction: (TimerAction) -> Unit,
) {
    val maxDial = if (screenWidth() >= 768.dp) 448.dp else 320.dp
    Surface(
        modifier =
            Modifier
                .widthIn(max = maxDial)
                .fillMaxWidth()
                .border(1.dp, Color(0x141C1B19), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp,
    ) {
        Column {
            DurationRow(
                label = stringResource(R.string.work),
                value = state.workMinutes,
                min = TimerEngine.WORK_MIN,
                max = TimerEngine.WORK_MAX,
                enabled = state.steppersEnabled,
                active = state.mode == Mode.WORK,
                accent = accent,
                decrease = stringResource(R.string.decrease_work),
                increase = stringResource(R.string.increase_work),
                onDecrease = { onAction(TimerAction.SetWorkMinutes(state.workMinutes - 1)) },
                onIncrease = { onAction(TimerAction.SetWorkMinutes(state.workMinutes + 1)) },
            )
            DurationRow(
                label = stringResource(R.string.break_label),
                value = state.breakMinutes,
                min = TimerEngine.BREAK_MIN,
                max = TimerEngine.BREAK_MAX,
                enabled = state.steppersEnabled,
                active = state.mode == Mode.BREAK,
                accent = accent,
                decrease = stringResource(R.string.decrease_break),
                increase = stringResource(R.string.increase_break),
                onDecrease = { onAction(TimerAction.SetBreakMinutes(state.breakMinutes - 1)) },
                onIncrease = { onAction(TimerAction.SetBreakMinutes(state.breakMinutes + 1)) },
            )
            DurationRow(
                label = stringResource(R.string.long_break),
                value = state.longBreakMinutes,
                min = TimerEngine.LONG_BREAK_MIN,
                max = TimerEngine.LONG_BREAK_MAX,
                enabled = state.steppersEnabled,
                active = state.mode == Mode.LONG_BREAK,
                accent = accent,
                decrease = stringResource(R.string.decrease_long_break),
                increase = stringResource(R.string.increase_long_break),
                onDecrease = { onAction(TimerAction.SetLongBreakMinutes(state.longBreakMinutes - 1)) },
                onIncrease = { onAction(TimerAction.SetLongBreakMinutes(state.longBreakMinutes + 1)) },
            )
        }
    }
}

@Composable
private fun DurationRow(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    enabled: Boolean,
    active: Boolean,
    accent: Color,
    decrease: String,
    increase: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    DurationStepper(
        label = label,
        value = value,
        unit = stringResource(R.string.minutes),
        min = min,
        max = max,
        enabled = enabled,
        active = active,
        accent = accent,
        decreaseDescription = decrease,
        increaseDescription = increase,
        onDecrease = onDecrease,
        onIncrease = onIncrease,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
private fun screenWidth(): Dp {
    val pixels = LocalWindowInfo.current.containerSize.width
    return with(LocalDensity.current) { pixels.toDp() }
}

@Composable
internal fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    var reduced by remember { mutableStateOf(animatorScale(context) == 0f) }
    DisposableEffect(context) {
        val observer =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    reduced = animatorScale(context) == 0f
                }
            }
        val uri: Uri = Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)
        context.contentResolver.registerContentObserver(uri, false, observer)
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    return reduced
}

private fun animatorScale(context: android.content.Context): Float =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)

private fun statusRes(status: TimerStatus): Int =
    when (status) {
        TimerStatus.WORK -> R.string.work
        TimerStatus.WORKING -> R.string.working
        TimerStatus.PAUSED -> R.string.paused
        TimerStatus.BREAK -> R.string.break_label
        TimerStatus.RESTING -> R.string.resting
        TimerStatus.BREAK_PAUSED -> R.string.break_paused
        TimerStatus.LONG_BREAK -> R.string.long_break
        TimerStatus.LONG_RESTING -> R.string.long_resting
        TimerStatus.LONG_BREAK_PAUSED -> R.string.long_break_paused
    }

private fun primaryRes(primary: PrimaryControl): Int =
    when (primary) {
        PrimaryControl.START -> R.string.start
        PrimaryControl.PAUSE -> R.string.pause
        PrimaryControl.RESUME -> R.string.resume
    }

internal fun notificationSettingsIntent(packageName: String): Intent =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    }

internal fun exactAlarmSettingsIntent(packageName: String): Intent =
    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
        data = android.net.Uri.parse("package:$packageName")
    }

private suspend fun withFrameNanos(onFrame: () -> Unit) {
    androidx.compose.runtime.withFrameNanos { onFrame() }
}
