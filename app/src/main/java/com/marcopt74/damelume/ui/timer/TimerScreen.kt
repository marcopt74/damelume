package com.marcopt74.damelume.ui.timer

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.marcopt74.damelume.ui.theme.EaseSmoothOut
import com.marcopt74.damelume.ui.theme.LocalModeColors
import com.marcopt74.damelume.ui.theme.LumeTheme

@Composable
fun TimerScreen(viewModel: TimerViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.onResume()
        }
    }
    val reducedMotion = rememberReducedMotion()
    TimerContent(
        state = state,
        reducedMotion = reducedMotion,
        onAction = viewModel::onAction,
        onFrame = viewModel::onFrame,
    )
}

@Composable
fun TimerContent(
    state: TimerUiState,
    reducedMotion: Boolean,
    onAction: (TimerAction) -> Unit,
    onFrame: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LumeTheme(mode = state.mode, running = state.isRunning, reducedMotion = reducedMotion) {
        val colors = LocalModeColors.current
        val focusRequester = remember { FocusRequester() }
        var showRationale by remember { mutableStateOf(false) }
        val permissionLauncher =
            rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { onAction(TimerAction.MarkNotificationAsked) }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
        RunningClockEffect(state = state, reducedMotion = reducedMotion, onFrame = onFrame)
        val configuration = LocalConfiguration.current
        val wide =
            configuration.screenWidthDp >= 600 ||
                configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val onPrimary = {
            handlePrimary(state, onAction, permissionLauncher) { showRationale = true }
        }
        Box(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
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
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Header(state = state, accent = colors.accent, onAction = onAction)
                PermissionBanners(state = state, onAction = onAction)
                TimerBody(
                    state = state,
                    reducedMotion = reducedMotion,
                    wide = wide,
                    onPrimary = onPrimary,
                    onAction = onAction,
                )
            }
        }
        if (showRationale) {
            NotificationRationaleDialog(
                onConfirm = {
                    showRationale = false
                    onAction(TimerAction.MarkNotificationAsked)
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
    onFrame: (Long) -> Unit,
) {
    if (state.isRunning && !reducedMotion) {
        LaunchedEffect(state.endsAtEpochMs) {
            while (true) {
                withFrameNanos { onFrame(System.currentTimeMillis()) }
            }
        }
    } else if (state.isRunning) {
        LaunchedEffect(state.endsAtEpochMs) {
            while (true) {
                onFrame(System.currentTimeMillis())
                kotlinx.coroutines.delay(1_000)
            }
        }
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
            actionLabel = stringResource(R.string.banner_exact_alarm),
            dismissLabel = stringResource(R.string.dismiss),
            onAction = { context.startActivity(exactAlarmSettingsIntent(context.packageName)) },
            onDismiss = null,
        )
    }
}

@Composable
private fun TimerBody(
    state: TimerUiState,
    reducedMotion: Boolean,
    wide: Boolean,
    onPrimary: () -> Unit,
    onAction: (TimerAction) -> Unit,
) {
    val colors = LocalModeColors.current
    if (wide) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DialBlock(state = state, reducedMotion = reducedMotion, accent = colors.accent, modifier = Modifier.weight(1f))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Controls(state = state, onPrimary = onPrimary, onAction = onAction)
                DurationCard(state = state, accent = colors.accentText, onAction = onAction)
            }
        }
    } else {
        DialBlock(state = state, reducedMotion = reducedMotion, accent = colors.accent)
        Controls(state = state, onPrimary = onPrimary, onAction = onAction)
        DurationCard(state = state, accent = colors.accentText, onAction = onAction)
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
    launcher: androidx.activity.result.ActivityResultLauncher<String>,
    showRationale: () -> Unit,
) {
    val starting = state.primary != PrimaryControl.PAUSE
    onAction(if (state.isRunning) TimerAction.Pause else TimerAction.Start)
    if (!starting) return
    when {
        state.requestNotificationOnStart -> {
            onAction(TimerAction.MarkNotificationAsked)
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        state.showNotificationRationale -> showRationale()
    }
}

@Composable
private fun Header(
    state: TimerUiState,
    accent: Color,
    onAction: (TimerAction) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 30.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1C1B19),
            )
            Text(
                text = stringResource(R.string.tagline),
                color = Color(0xFF6D6962),
                fontSize = 14.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            LocaleToggle(
                selected = state.locale ?: AppLocale.PT,
                groupDescription = stringResource(R.string.language),
                ptLabel = stringResource(R.string.locale_pt),
                enLabel = stringResource(R.string.locale_en),
                accent = accent,
                onSelect = { onAction(TimerAction.SetLocale(it)) },
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SoundToggle(
                enabled = state.soundEnabled,
                description = stringResource(if (state.soundEnabled) R.string.sound_off else R.string.sound_on),
                accent = accent,
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
                accent = accent,
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
    modifier: Modifier = Modifier,
) {
    val status = stringResource(statusRes(state.status))
    val description = stringResource(R.string.remaining, status, state.timeText)
    val maxDial = if (LocalConfiguration.current.screenWidthDp >= 600) 448.dp else 320.dp
    Box(modifier = modifier.widthIn(max = maxDial), contentAlignment = Alignment.Center) {
        Dial(
            remainingMs = state.remainingMs,
            totalMs = state.totalMs,
            running = state.isRunning,
            reducedMotion = reducedMotion,
            accent = accent,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = description },
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.isRunning) {
                    LiveDot(reducedMotion = reducedMotion, color = accent)
                    Spacer(modifier = Modifier.size(8.dp))
                }
                Text(
                    text = status.uppercase(),
                    color = accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            Text(
                text = state.timeText,
                color = Color(0xFF1C1B19),
                textAlign = TextAlign.Center,
                style =
                    TextStyle(
                        fontSize = 64.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFeatureSettings = "tnum, lnum",
                    ),
            )
        }
    }
}

@Composable
private fun LiveDot(
    reducedMotion: Boolean,
    color: Color,
) {
    val scale =
        if (reducedMotion) {
            1f
        } else {
            val transition = rememberInfiniteTransition(label = "liveDot")
            val animated by transition.animateFloat(
                initialValue = 1f,
                targetValue = 0.82f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(durationMillis = 900, easing = EaseSmoothOut),
                        repeatMode = RepeatMode.Reverse,
                    ),
                label = "liveDotScale",
            )
            animated
        }
    Box(
        modifier =
            Modifier
                .size(8.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = if (reducedMotion) 1f else 0.4f + (scale - 0.82f) / 0.18f * 0.6f
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
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = { onAction(TimerAction.Reset) },
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
            ) {
                Icon(
                    imageVector = Icons.Filled.Replay,
                    contentDescription = stringResource(R.string.reset),
                )
            }
            Button(
                onClick = onPrimary,
                modifier =
                    Modifier
                        .weight(1f)
                        .height(56.dp),
                shape = RoundedCornerShape(50),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = colors.button,
                        contentColor = colors.onButton,
                    ),
            ) {
                Crossfade(targetState = state.primary, label = "primaryIcon") { primary ->
                    Icon(
                        imageVector =
                            if (primary == PrimaryControl.PAUSE) {
                                Icons.Filled.Pause
                            } else {
                                Icons.Filled.PlayArrow
                            },
                        contentDescription = null,
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(primaryRes(state.primary)),
                    fontWeight = FontWeight.SemiBold,
                )
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
private fun DurationCard(
    state: TimerUiState,
    accent: Color,
    onAction: (TimerAction) -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x141C1B19), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFF5F4F1),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            DurationStepper(
                label = stringResource(R.string.work),
                value = state.workMinutes,
                unit = stringResource(R.string.minutes),
                min = TimerEngine.WORK_MIN,
                max = TimerEngine.WORK_MAX,
                enabled = state.steppersEnabled,
                active = state.mode == Mode.WORK,
                accent = accent,
                decreaseDescription = stringResource(R.string.decrease_work),
                increaseDescription = stringResource(R.string.increase_work),
                onDecrease = { onAction(TimerAction.SetWorkMinutes(state.workMinutes - 1)) },
                onIncrease = { onAction(TimerAction.SetWorkMinutes(state.workMinutes + 1)) },
            )
            DurationStepper(
                label = stringResource(R.string.break_label),
                value = state.breakMinutes,
                unit = stringResource(R.string.minutes),
                min = TimerEngine.BREAK_MIN,
                max = TimerEngine.BREAK_MAX,
                enabled = state.steppersEnabled,
                active = state.mode == Mode.BREAK,
                accent = accent,
                decreaseDescription = stringResource(R.string.decrease_break),
                increaseDescription = stringResource(R.string.increase_break),
                onDecrease = { onAction(TimerAction.SetBreakMinutes(state.breakMinutes - 1)) },
                onIncrease = { onAction(TimerAction.SetBreakMinutes(state.breakMinutes + 1)) },
            )
            DurationStepper(
                label = stringResource(R.string.long_break),
                value = state.longBreakMinutes,
                unit = stringResource(R.string.minutes),
                min = TimerEngine.LONG_BREAK_MIN,
                max = TimerEngine.LONG_BREAK_MAX,
                enabled = state.steppersEnabled,
                active = state.mode == Mode.LONG_BREAK,
                accent = accent,
                decreaseDescription = stringResource(R.string.decrease_long_break),
                increaseDescription = stringResource(R.string.increase_long_break),
                onDecrease = { onAction(TimerAction.SetLongBreakMinutes(state.longBreakMinutes - 1)) },
                onIncrease = { onAction(TimerAction.SetLongBreakMinutes(state.longBreakMinutes + 1)) },
            )
        }
    }
}

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

@Composable
private fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    val scale =
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
    return scale == 0f
}

private suspend fun withFrameNanos(onFrame: (Long) -> Unit) {
    androidx.compose.runtime.withFrameNanos { onFrame(System.currentTimeMillis()) }
}
