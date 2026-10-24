package com.marcopt74.damelume.ui.timer

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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
import com.marcopt74.damelume.ui.theme.CardShape
import com.marcopt74.damelume.ui.theme.EaseSmoothOut
import com.marcopt74.damelume.ui.theme.LocalModeColors
import com.marcopt74.damelume.ui.theme.LocalReducedMotion
import com.marcopt74.damelume.ui.theme.LumePalette
import com.marcopt74.damelume.ui.theme.LumeTheme
import com.marcopt74.damelume.ui.theme.LumeType
import com.marcopt74.damelume.ui.theme.lumeShadowBorder
import com.marcopt74.damelume.ui.theme.timerDigitSizeSp

@Composable
fun TimerScreen(viewModel: TimerViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.onResume()
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
    onFrame: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LumeTheme(mode = state.mode, running = state.isRunning, reducedMotion = reducedMotion) {
        val colors = LocalModeColors.current
        val focusRequester = remember { FocusRequester() }
        var showRationale by remember { mutableStateOf(false) }
        val permissionLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                onAction(TimerAction.MarkNotificationAsked)
            }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
        RunningClockEffect(state = state, reducedMotion = reducedMotion, onFrame = onFrame)
        RecentsTitle(state)
        val onPrimary = {
            handlePrimary(state, onAction, permissionLauncher) { showRationale = true }
        }
        BoxWithConstraints(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
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
            ProportionalPage(
                viewportHeight = maxHeight,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .widthIn(max = 576.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                header = { Header(state = state, accent = colors.accent, onAction = onAction) },
                banners = { PermissionBanners(state = state, onAction = onAction) },
                dial = { DialBlock(state = state, accent = colors.accent) },
                controls = { Controls(state = state, onPrimary = onPrimary, onAction = onAction) },
                card = { DurationCard(state = state, accent = colors.accent, onAction = onAction) },
            )
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
private fun ProportionalPage(
    viewportHeight: Dp,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit,
    banners: @Composable () -> Unit,
    dial: @Composable () -> Unit,
    controls: @Composable () -> Unit,
    card: @Composable () -> Unit,
) {
    Layout(
        content = {
            Box(Modifier.fillMaxWidth()) { header() }
            Box(Modifier.fillMaxWidth()) { banners() }
            Box(Modifier.fillMaxSize()) { dial() }
            Box(Modifier.fillMaxWidth()) { controls() }
            Box(Modifier.fillMaxWidth()) { card() }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val viewport = viewportHeight.roundToPx()
        val horizontal = 20.dp.roundToPx()
        val vertical = 12.dp.roundToPx()
        val baseGap = 16.dp.roundToPx()
        val contentWidth = (constraints.maxWidth - horizontal * 2).coerceAtLeast(0)
        val loose = Constraints(maxWidth = contentWidth)
        val headerPlaceable = measurables[0].measure(loose)
        val bannerPlaceable = measurables[1].measure(loose)
        val controlsHeight = measurables[3].maxIntrinsicHeight(contentWidth)
        val cardHeight = measurables[4].maxIntrinsicHeight(contentWidth)
        val bannerGap = if (bannerPlaceable.height > 0) baseGap else 0
        val spacing =
            pageSpacing(
                viewportHeight = viewport,
                contentWidth = contentWidth,
                headerHeight = headerPlaceable.height,
                bannerHeight = bannerPlaceable.height,
                controlsHeight = controlsHeight,
                cardHeight = cardHeight,
                bannerGap = bannerGap,
                verticalPadding = vertical,
                baseGap = baseGap,
                minDial = 200.dp.roundToPx(),
                maxDial = 448.dp.roundToPx(),
            )
        val group = Constraints(maxWidth = spacing.dialSide)
        val dialPlaceable = measurables[2].measure(Constraints.fixed(spacing.dialSide, spacing.dialSide))
        val controlsPlaceable = measurables[3].measure(group)
        val cardPlaceable = measurables[4].measure(group)
        val groupX = horizontal + (contentWidth - spacing.dialSide) / 2
        val used =
            vertical * 2 +
                headerPlaceable.height +
                bannerGap +
                bannerPlaceable.height +
                spacing.gapBeforeDial +
                dialPlaceable.height +
                spacing.gapAfterDial +
                controlsPlaceable.height +
                spacing.gapBeforeCard +
                cardPlaceable.height
        val height = maxOf(spacing.height, used)
        layout(constraints.maxWidth, height) {
            var y = vertical
            headerPlaceable.place(horizontal, y)
            y += headerPlaceable.height
            if (bannerPlaceable.height > 0) {
                y += bannerGap
                bannerPlaceable.place(horizontal, y)
                y += bannerPlaceable.height
            }
            y += spacing.gapBeforeDial
            dialPlaceable.place(groupX, y)
            y += dialPlaceable.height
            y += spacing.gapAfterDial
            controlsPlaceable.place(groupX, y)
            y += controlsPlaceable.height
            y += spacing.gapBeforeCard
            cardPlaceable.place(groupX, y)
        }
    }
}

@Composable
private fun RunningClockEffect(
    state: TimerUiState,
    reducedMotion: Boolean,
    onFrame: (Long) -> Unit,
) {
    if (!state.isRunning) return
    LaunchedEffect(state.endsAtEpochMs, reducedMotion) {
        if (reducedMotion) {
            while (true) {
                onFrame(System.currentTimeMillis())
                kotlinx.coroutines.delay(1_000)
            }
        } else {
            while (true) {
                androidx.compose.runtime.withFrameNanos { onFrame(System.currentTimeMillis()) }
            }
        }
    }
}

@Composable
private fun RecentsTitle(state: TimerUiState) {
    val context = LocalContext.current
    val activity = context as? Activity
    val modeLabel = stringResource(if (state.mode == Mode.WORK) R.string.work else R.string.break_label)
    val runningTitle = stringResource(R.string.window_title, state.timeText, modeLabel)
    val idleTitle = stringResource(R.string.app_name)
    SideEffect {
        val label = if (state.isRunning) runningTitle else idleTitle
        activity?.setTaskDescription(ActivityManager.TaskDescription(label))
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
private fun NotificationRationaleDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .lumeShadowBorder(CardShape)
                    .background(Color.White)
                    .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.notif_rationale_title),
                style = LumeType.button,
                color = Color(LumePalette.FG),
            )
            Text(
                text = stringResource(R.string.notif_rationale_body),
                style = LumeType.body,
                color = Color(LumePalette.FG),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                LumeButton(onClick = onDismiss, kind = LumeButtonKind.Ghost) {
                    Text(text = stringResource(R.string.dismiss), style = LumeType.buttonSmall)
                }
                LumeButton(onClick = onConfirm, kind = LumeButtonKind.Ghost) {
                    Text(text = stringResource(R.string.notif_rationale_allow), style = LumeType.buttonSmall)
                }
            }
        }
    }
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
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(R.string.app_name), style = LumeType.display, color = Color(LumePalette.FG))
            Text(text = stringResource(R.string.tagline), style = LumeType.tagline, color = Color(LumePalette.MUTED))
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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
    accent: Color,
) {
    val status = stringResource(statusRes(state.status))
    val description = stringResource(R.string.remaining, status, state.timeText)
    val digitSize = timerDigitSizeSp(LocalConfiguration.current.screenWidthDp)
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Dial(
            remainingMs = state.remainingMs,
            totalMs = state.totalMs,
            running = state.isRunning,
            reducedMotion = LocalReducedMotion.current,
            accent = accent,
            modifier =
                Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = description },
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusDot(running = state.isRunning, color = accent)
                Text(
                    text = status.uppercase(),
                    color = Color(LumePalette.MUTED),
                    style = LumeType.status,
                )
            }
            Text(
                text = state.timeText,
                color = Color(LumePalette.FG),
                textAlign = TextAlign.Center,
                style = LumeType.digits(digitSize),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

@Composable
private fun StatusDot(
    running: Boolean,
    color: Color,
) {
    if (!running) {
        Box(
            modifier =
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(LumePalette.FG).copy(alpha = 0.20f)),
        )
        return
    }
    val reduced = LocalReducedMotion.current
    val scale =
        if (reduced) {
            1f
        } else {
            val transition = rememberInfiniteTransition(label = "liveDot")
            val animated by transition.animateFloat(
                initialValue = 1f,
                targetValue = 0.82f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(durationMillis = 1800, easing = EaseSmoothOut),
                        repeatMode = RepeatMode.Reverse,
                    ),
                label = "liveDotScale",
            )
            animated
        }
    val alpha = if (reduced) 1f else 0.4f + (scale - 0.82f) / 0.18f * 0.6f
    Box(
        modifier =
            Modifier
                .size(6.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
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
    val paused = state.primary == PrimaryControl.PAUSE
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LumeButton(
                onClick = { onAction(TimerAction.Reset) },
                kind = LumeButtonKind.Reset,
                accent = colors.accent,
                description = stringResource(R.string.reset),
            ) {
                androidx.compose.material3.Icon(
                    painter = painterResource(R.drawable.ic_lume_rotate_ccw),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
            LumeButton(
                onClick = onPrimary,
                modifier = Modifier.weight(1f).height(56.dp),
                kind = LumeButtonKind.Primary,
                accent = colors.accent,
            ) {
                Box(modifier = Modifier.size(20.dp)) {
                    LumeSwapIcon(
                        visible = paused,
                        painter = painterResource(R.drawable.ic_lume_pause),
                        modifier = Modifier.size(20.dp),
                    )
                    LumeSwapIcon(
                        visible = !paused,
                        painter = painterResource(R.drawable.ic_lume_play),
                        modifier = Modifier.size(20.dp),
                        nudge = 2.dp,
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = stringResource(primaryRes(state.primary)), style = LumeType.button)
            }
        }
        if (state.showSkip) {
            LumeButton(
                onClick = { onAction(TimerAction.SkipBreak) },
                kind = LumeButtonKind.Ghost,
                accent = colors.accent,
            ) {
                androidx.compose.material3.Icon(
                    painter = painterResource(R.drawable.ic_lume_skip),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(text = stringResource(R.string.skip_break), style = LumeType.buttonSmall)
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
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .lumeShadowBorder(CardShape),
    ) {
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
            valueDescription = stringResource(R.string.edit_work_minutes),
            onDecrease = { onAction(TimerAction.SetWorkMinutes(state.workMinutes - 1)) },
            onIncrease = { onAction(TimerAction.SetWorkMinutes(state.workMinutes + 1)) },
            onValueChange = { onAction(TimerAction.SetWorkMinutes(it)) },
        )
        HorizontalDivider(color = Color(LumePalette.DIVIDER), thickness = 1.dp)
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
            valueDescription = stringResource(R.string.edit_break_minutes),
            onDecrease = { onAction(TimerAction.SetBreakMinutes(state.breakMinutes - 1)) },
            onIncrease = { onAction(TimerAction.SetBreakMinutes(state.breakMinutes + 1)) },
            onValueChange = { onAction(TimerAction.SetBreakMinutes(it)) },
        )
    }
}

internal data class PageSpacing(
    val dialSide: Int,
    val height: Int,
    val gapBeforeDial: Int,
    val gapAfterDial: Int,
    val gapBeforeCard: Int,
)

internal fun pageSpacing(
    viewportHeight: Int,
    contentWidth: Int,
    headerHeight: Int,
    bannerHeight: Int,
    controlsHeight: Int,
    cardHeight: Int,
    bannerGap: Int,
    verticalPadding: Int,
    baseGap: Int,
    minDial: Int,
    maxDial: Int,
): PageSpacing {
    val cap = minOf(contentWidth, maxDial).coerceAtLeast(1)
    val chrome = verticalPadding * 2 + baseGap * 3 + bannerGap + headerHeight + bannerHeight + controlsHeight + cardHeight
    val available = viewportHeight - chrome
    val dialSide =
        when {
            available >= cap -> cap
            available >= minDial -> available
            else -> minOf(cap, minDial)
        }
    val contentHeight = chrome + dialSide
    val height = maxOf(contentHeight, viewportHeight)
    val extra = (height - contentHeight).coerceAtLeast(0)
    val growBeforeDial = (extra * 0.45f).toInt()
    val growAfterDial = (extra * 0.35f).toInt()
    val growBeforeCard = extra - growBeforeDial - growAfterDial
    return PageSpacing(
        dialSide = dialSide,
        height = height,
        gapBeforeDial = baseGap + growBeforeDial,
        gapAfterDial = baseGap + growAfterDial,
        gapBeforeCard = baseGap + growBeforeCard,
    )
}

private fun statusRes(status: TimerStatus): Int =
    when (status) {
        TimerStatus.WORK -> R.string.work
        TimerStatus.WORKING -> R.string.working
        TimerStatus.PAUSED -> R.string.paused
        TimerStatus.BREAK -> R.string.break_label
        TimerStatus.RESTING -> R.string.resting
        TimerStatus.BREAK_PAUSED -> R.string.break_paused
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
