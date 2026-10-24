# damelume developer notes

Android Pomodoro app (visible name **Lume**). Package `com.marcopt74.damelume`. Single `:app` module. Layers: `domain` (pure), `data` (DataStore + Room), `ui` (Compose), `system` (alarm, notifications, sound, boot, locale).

Quality gate:

```bash
./gradlew ktlintCheck detekt lint testDebugUnitTest assembleDebug
```

CI (`.github/workflows/ci.yml`) runs `./gradlew lint testDebugUnitTest assembleDebug` on JDK 17.

## Acceptance criteria

| AC | Implementation | Tests |
|---|---|---|
| AC-1 | `TimerScreen.kt`, `TimerUiState.kt`, `values/strings.xml`, `values-pt/strings.xml` | `TimerScreenTest.initialState_showsDefaults` |
| AC-2 | `TimerEngine.start` | `TimerEngineTest.start_setsEndsAt` |
| AC-3 | `TimerEngine.pause` | `TimerEngineTest.pause_keepsRemaining` |
| AC-4 | `TimerEngine.reset` | `TimerEngineTest.reset_keepsMode` |
| AC-5 | `TimeFormatter.kt`, `TimerUiState.from` derives remaining from `endsAt` | `TimeFormatterTest`, `TimerViewModelTest.remainingDerivedFromEndsAt` |
| AC-6 | `TimerEngine.complete` when cycle &lt; 3 | `TimerEngineTest.completeWork_switchesToBreak_incrementsCount`, `complete_ignoredAbove200ms` |
| AC-7 | `TimerEngine.complete` for short break | `TimerEngineTest.completeBreak_noIncrement` |
| AC-8 | `TimerEngine.skip`, `TimerUiState.showSkip` | `TimerEngineTest.skipBreak`, `TimerScreenTest.skipBreak_onlyInBreak` |
| AC-9 | `SessionCounter.kt`, plurals `session_count` / `session_word` | `SessionDotsTest`, `TimerScreenTest.pluralSessions`, `TimerEngineTest.resetSessions_resetsCycle` |
| AC-10 | `CompletedSessionDao.todayCount`, `TimerEngine.resetSessions` | `CompletedSessionDaoTest.todayCount_respectsResetAndDate`, `TimerRepositoryTest.resetSessions_keepsHistory` |
| AC-11 | Room count filtered by local date and `sessionsResetAt` | `CompletedSessionDaoTest.todayCount_respectsResetAndDate`, `TimerEngineTest.reconcile_newDay` |
| AC-12 | `TimerEngine.setMinutes` clamps | `TimerEngineTest.setWork_clamps_1_90`, `setBreak_clamps_1_30` |
| AC-13 | Remaining resets only when stopped in the edited mode | `TimerEngineTest.setCurrentModeMinutes_resetsRemainingWhenStopped`, `setOtherMode_keepsRemaining` |
| AC-14 | `TimerUiState.steppersEnabled`, `DurationStepper` alpha | `TimerScreenTest.steppersDisabledWhileRunning` |
| AC-15 | `LumePreferencesDataSource` keys `work_minutes`, `break_minutes` | `LumePreferencesDataSourceTest.persistsDurationsAndSound` |
| AC-16 | `SoundToggle.kt`, `sound_enabled` | `LumePreferencesDataSourceTest.persistsDurationsAndSound`, `TimerScreenTest.soundToggle_descriptionFlips` |
| AC-17 | `TimerRepository.applyEffects` + `AndroidChimePlayer` (`USAGE_NOTIFICATION_EVENT`) | `TimerRepositoryTest.completeInForeground_playsChimeOnlyIfEnabled` |
| AC-18 | `AndroidTimerNotifier.showCompletion`, channels in `NotificationChannels` | `TimerNotifierTest.completion_usesChannelBySound_andTexts` |
| AC-19 | `AndroidAlarmScheduler` (`setExactAndAllowWhileIdle`, `RTC_WAKEUP`, immutable explicit PendingIntent) | `AndroidAlarmSchedulerTest.schedulesExactAtEndsAt_cancelsOnPause` |
| AC-20 | `TimerAlarmReceiver` → `TimerAlarmHandler` → `Complete(expectedEndsAt)` | `TimerAlarmReceiverTest.completesOnce_idempotentOnStaleAlarm` |
| AC-21 | Ongoing notification id 42, chronometer countdown, pause/reset/skip actions | `TimerNotifierTest.ongoing_runningHasChronometerCountdownAndActions`, `ongoing_pausedStatic`, `noOngoingWhenIdle` |
| AC-22 | `NotificationActionReceiver` → `NotificationActionHandler` | `NotificationActionReceiverTest.pause_resume_reset_skip` |
| AC-23 | Completion title/body from the finished and next mode, `autoCancel` | `TimerNotifierTest.completion_usesChannelBySound_andTexts` |
| AC-24 | `NotificationPermissionState.shouldRequestOnFirstStart`; `TimerScreen.handlePrimary` still starts the timer | `NotificationPermissionStateTest` |
| AC-25 | Banner + rationale dialog; dismiss marker `notif_banner_dismissed_for_ends_at` | `NotificationPermissionStateTest`, `TimerScreenTest.permissionBanner_shownWhenDenied_opensSettings` |
| AC-26 | `ExactAlarmCapability` (inexact + banner only on API 31–32) | `ExactAlarmCapabilityTest` |
| AC-27 | `TimerEngine.reconcile` future `endsAt` reschedules | `TimerEngineTest.reconcile_running_future` |
| AC-28 | Reconcile past end: sound only if delay &lt; 120s, `endedAt = endsAt` | `TimerEngineTest.reconcile_past_lt120s_sound`, `reconcile_past_ge120s_silent`, `reconcile_newDay` |
| AC-29 | `BootReceiver` / `BootHandler` | `BootReceiverTest.reschedulesFuture_completesPast` |
| AC-30 | Paused state has no `endsAt`; reconcile cancels the alarm | `TimerRepositoryTest.pausedSurvivesRecreation` |
| AC-31 | Retained state in `TimerTestActivity` | `TimerScreenTest.stateSurvivesRecreation` |
| AC-32 | `LocaleToggle`, `AndroidLocaleApplicator` (`AppCompatDelegate.setApplicationLocales`) | `LocaleToggleTest.switchesStrings_andPersists`, `StringsParityTest` |
| AC-33 | Dial content description `remaining`; icon descriptions; toggle/radio roles | `TimerScreenA11yTest.controlsExposeNamesStateAndTouchTargets` |
| AC-34 | Reference control sizes: locale 28dp, stepper 36dp, sound 44dp, reset 56dp | `TimerScreenA11yTest.controlsExposeNamesStateAndTouchTargets` |
| AC-35 | `LumePalette` contrast for work and muted text; break accent is the site orange | `ContrastTest` |
| AC-36 | `reducedMotion` skips the frame loop and the live-dot pulse | `DialTest.reducedMotion_noContinuousAnimation` |
| AC-37 | Work `#2F6DF6`, break `#EF6C1C`, running background 70% soft | `ThemeTest.breakModeUsesSiteOrange`, `modeAccents_workAndBreakShareOneAccent`, `runningBackgroundBlend` |
| AC-38 | Space on the focused root | `TimerScreenTest.spaceKeyToggles` |
| AC-39 | Every work completion enters a stopped break, including the fourth | `TimerEngineTest.completeFourthWork_stillEntersBreak`, `TimerAlarmReceiverTest.fourthWork_whileDead_entersBreak` |
| AC-40 | Session dots are today's count mod 4, with 4 filled on a multiple of 4 | `SessionDotsTest.dotsFollowTodayCountModFour` |
| AC-41 | Skip break returns to stopped work and cancels alarm | `TimerEngineTest.skipBreak`, `TimerRepositoryTest.skipBreak_cancelsAlarmAndOngoing` |
| AC-42 | Reset sessions zeros today's count and keeps the current phase | `TimerEngineTest.resetSessions_keepsPhase`, `TimerRepositoryTest.resetSessions_keepsHistory` |
| AC-43 | Break status strings and dial description | `TimerScreenTest.breakLabels_ptAndEn`, `TimerScreenA11yTest.dialDescription_break` |
| AC-44 | A stored `long_break` mode reloads as work | `LumePreferencesDataSourceTest.storedLongBreakReloadsAsWork` |
| AC-45 | Boot of a future break reschedules | `BootReceiverTest.break_reschedules` |

Chime asset: `tools/gen_chime.py` writes `app/src/test/resources/lume_chime.wav` and `app/src/main/res/raw/lume_chime.ogg`. `ChimeAssetTest` checks the raw resource, WAV duration (1.0–1.1 s) and dominant frequencies 523.25 / 659.25 / 783.99 Hz.

Fonts: Fraunces and Outfit under `app/src/main/res/font/`. SIL OFL texts are in `third_party/fonts/` and `app/src/main/assets/OFL.txt`.

## Deviations

- The screen matches https://da-melume.grok.me: one accent per mode (`#2F6DF6` work, `#EF6C1C` break), Outfit for UI text, Fraunces only for the word Lume, Lucide stroke icons, and a single column. White on `#EF6C1C` is the site button and is below WCAG AA.
- There is no long break. Work and break alternate. The four dots follow today's completed work count. A stored `long_break` value reloads as work and keeps its remaining time and alarm so the pending intent can still be cancelled or completed.
- `local_date` is the local date of `endedAt` (the scheduled end), so a work session that ends before midnight stays on that day even if the process reconciles after midnight. The daily counter therefore follows AC-11/AC-28 rather than counting that session on the new day.
- A timely completion starts the next phase immediately and posts a notice. A completion 120 seconds or more after the scheduled end stays stopped and does not chime.
- First launch is Portuguese, matching the reference site. The in-app toggle still persists EN or PT.
- The completion chime plays through the alarm stream whenever sound is on, including when the app is in the background. The background notice itself stays silent so the chime is not played twice.
- Receiver unit tests drive `TimerAlarmHandler`, `NotificationActionHandler` and `BootHandler` (the code the receivers call) instead of a Hilt-injected `BroadcastReceiver`, so they run on the JVM without `@HiltAndroidTest`.
- `TimerViewModelTest.remainingDerivedFromEndsAt` asserts `TimerUiState.from`, which is the mapping the ViewModel's `combine` uses.
- `android.newDsl=false` and `android.builtInKotlin=false` keep the classic Android Gradle DSL so ktlint, detekt and Hilt keep working on AGP 9. Both flags are removed in AGP 10.
- The repository already had a platform commit (`Initial commit`) before the authorised `chore: initial commit`. `main` was not force-pushed. Nothing after that commit is on `main`.

## Follow-ups

Manual QA from spec §8 is not in CI: forced Doze, reboot with a running timer, force-stop then reopen, TalkBack end to end (R7). Play review of `USE_EXACT_ALARM` (R1), aggressive OEM alarm delays (R2), a dismissible ongoing notification on Android 14+ (R3), and manual clock changes (R4) are unchanged from the spec.
