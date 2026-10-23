package com.marcopt74.damelume.ui.timer

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.marcopt74.damelume.data.prefs.LumePreferencesDataSource
import com.marcopt74.damelume.domain.model.AppLocale
import com.marcopt74.damelume.system.locale.AndroidLocaleApplicator
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class LocaleToggleTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun switchesStrings_andPersists() =
        runTest {
            val selected = androidx.compose.runtime.mutableStateOf(AppLocale.PT)
            compose.setContent {
                LocaleToggle(
                    selected = selected.value,
                    groupDescription = "Language",
                    ptLabel = "PT",
                    enLabel = "EN",
                    accent = Color(0xFF2F6DF6),
                    onSelect = { selected.value = it },
                )
            }
            compose.onNodeWithText("PT").assertIsSelected()
            compose.onNodeWithText("EN").performClick()
            compose.waitForIdle()
            assertThat(selected.value).isEqualTo(AppLocale.EN)
            compose.onNodeWithText("EN").assertIsSelected()

            val context = ApplicationProvider.getApplicationContext<Context>()
            val file = File(context.filesDir, "locale.preferences_pb")
            val source = LumePreferencesDataSource(PreferenceDataStoreFactory.create { file })
            source.setLocale(AppLocale.EN)
            assertThat(source.current().settings.locale).isEqualTo(AppLocale.EN)

            val applicator = AndroidLocaleApplicator()
            applicator.apply(AppLocale.EN)
            assertThat(applicator.current()).isEqualTo(AppLocale.EN)
            applicator.apply(AppLocale.PT)
            assertThat(applicator.current()).isEqualTo(AppLocale.PT)
        }
}
