package com.atharok.btremote

import android.app.Application
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.atharok.btremote.domain.entities.remoteInput.keyboard.KeyboardLanguage
import com.atharok.btremote.domain.entities.settings.AppearanceSettings
import com.atharok.btremote.domain.entities.settings.RemoteSettings
import com.atharok.btremote.domain.entities.settings.ThemeEntity
import com.atharok.btremote.domain.repositories.DataStoreRepository
import com.atharok.btremote.domain.usecases.SettingsUseCase
import com.atharok.btremote.presentation.viewmodel.SettingsViewModel
import com.atharok.btremote.ui.screens.SettingsScreen
import java.lang.reflect.Proxy
import java.util.Locale
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36, 37], application = Application::class, qualifiers = "en-rUS-w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsResourcesTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun themeAndKeyboardLabelsFollowResourcesWhenContextRemainsUnchanged() {
        val context = compose.activity.createConfigurationContext(
            Configuration(compose.activity.resources.configuration).apply { setLocale(Locale.US) }
        )
        val frenchResources = context.createConfigurationContext(
            Configuration(context.resources.configuration).apply { setLocale(Locale.FRANCE) }
        ).resources
        val resources = mutableStateOf(context.resources)
        val settings = Proxy.newProxyInstance(
            DataStoreRepository::class.java.classLoader, arrayOf(DataStoreRepository::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getAppearanceSettingsFlow" -> flowOf(AppearanceSettings(theme = ThemeEntity.LIGHT, useDynamicColors = false))
                "getRemoteSettingsFlow" -> flowOf(RemoteSettings(keyboardLanguage = KeyboardLanguage.ENGLISH_US))
                "showRemoteButtonsInNotification", "hideBluetoothActivationButton" -> flowOf(false)
                else -> error("Unexpected settings call: ${method.name}")
            }
        } as DataStoreRepository
        val viewModel = SettingsViewModel(SettingsUseCase(settings))
        val themeLabel = R.string.theme_light
        val keyboardLabel = R.string.keyboard_english_us
        assertNotEquals(context.getString(themeLabel), frenchResources.getString(themeLabel))
        assertNotEquals(context.getString(keyboardLabel), frenchResources.getString(keyboardLabel))

        compose.setContent {
            // Keep Context in English so context.getString cannot hide a stale resource read.
            CompositionLocalProvider(
                LocalContext provides context,
                LocalConfiguration provides resources.value.configuration,
                LocalResources provides resources.value
            ) {
                MaterialTheme {
                    SettingsScreen({}, {}, settingsViewModel = viewModel)
                }
            }
        }
        compose.onNodeWithText(context.getString(themeLabel)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(keyboardLabel)).performScrollTo().assertIsDisplayed()

        compose.runOnIdle { resources.value = frenchResources }

        compose.onNodeWithText(frenchResources.getString(keyboardLabel)).performScrollTo().assertIsDisplayed()
        // Anchor above the theme row so its center stays below the pinned app bar.
        compose.onNodeWithText(frenchResources.getString(R.string.appearance)).performScrollTo()
        compose.onNodeWithText(frenchResources.getString(themeLabel)).assertIsDisplayed()
        compose.onNodeWithText(frenchResources.getString(R.string.theme)).assertIsDisplayed().performClick()
        compose.onNodeWithText(frenchResources.getString(R.string.theme_dark)).assertIsDisplayed()
    }
}
