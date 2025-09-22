/*
 * LibrePods - AirPods liberated from Apple’s ecosystem
 *
 * Copyright (C) 2025 LibrePods contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package me.kavishdevar.librepods.screens

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.CupertinoMaterials
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.composables.ConfirmationDialog
import me.kavishdevar.librepods.composables.StyledSwitch
import me.kavishdevar.librepods.services.ServiceManager
import me.kavishdevar.librepods.utils.AACPManager
import me.kavishdevar.librepods.utils.ATTHandles
import me.kavishdevar.librepods.utils.parseTransparencySettingsResponse
import me.kavishdevar.librepods.utils.sendTransparencySettings
import kotlin.io.encoding.ExperimentalEncodingApi

private var debounceJob: Job? = null
private var phoneMediaDebounceJob: Job? = null
private const val TAG = "AccessibilitySettings"

@SuppressLint("DefaultLocale")
@ExperimentalHazeMaterialsApi
@OptIn(ExperimentalMaterial3Api::class, ExperimentalEncodingApi::class)
@Composable
fun HearingAidScreen(navController: NavController) {
    val isDarkTheme = isSystemInDarkTheme()
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val verticalScrollState  = rememberScrollState()
    val hazeState = remember { HazeState() }
    val snackbarHostState = remember { SnackbarHostState() }
    val attManager = ServiceManager.getService()?.attManager ?: throw IllegalStateException("ATTManager not available")

    val aacpManager = remember { ServiceManager.getService()?.aacpManager }

    val showDialog = remember { mutableStateOf(false) }

    val hearingAidEnabled = remember {
        val aidStatus = aacpManager?.controlCommandStatusList?.find { it.identifier == AACPManager.Companion.ControlCommandIdentifiers.HEARING_AID }
        val assistStatus = aacpManager?.controlCommandStatusList?.find { it.identifier == AACPManager.Companion.ControlCommandIdentifiers.HEARING_ASSIST_CONFIG }
        mutableStateOf((aidStatus?.value?.getOrNull(1) == 0x01.toByte()) && (assistStatus?.value?.getOrNull(0) == 0x01.toByte()))
    }

    Scaffold(
        containerColor = if (isSystemInDarkTheme()) Color(
            0xFF000000
        ) else Color(
            0xFFF2F2F7
        ),
        topBar = {
            val darkMode = isSystemInDarkTheme()
            val mDensity = remember { mutableFloatStateOf(1f) }

            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.hearing_aid),
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (darkMode) Color.White else Color.Black,
                            fontFamily = FontFamily(Font(R.font.sf_pro))
                        )
                    )
                },
                modifier = Modifier
                    .hazeEffect(
                        state = hazeState,
                        style = CupertinoMaterials.thick(),
                        block = fun HazeEffectScope.() {
                            alpha =
                                if (verticalScrollState.value > 60.dp.value * mDensity.floatValue) 1f else 0f
                        })
                    .drawBehind {
                        mDensity.floatValue = density
                        val strokeWidth = 0.7.dp.value * density
                        val y = size.height - strokeWidth / 2
                        if (verticalScrollState.value > 60.dp.value * density) {
                            drawLine(
                                if (darkMode) Color.DarkGray else Color.LightGray,
                                Offset(0f, y),
                                Offset(size.width, y),
                                strokeWidth
                            )
                        }
                    },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .hazeSource(hazeState)
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(verticalScrollState),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val hearingAidListener = remember {
                object : AACPManager.ControlCommandListener {
                    override fun onControlCommandReceived(controlCommand: AACPManager.ControlCommand) {
                        if (controlCommand.identifier == AACPManager.Companion.ControlCommandIdentifiers.HEARING_AID.value ||
                            controlCommand.identifier == AACPManager.Companion.ControlCommandIdentifiers.HEARING_ASSIST_CONFIG.value) {
                            val aidStatus = aacpManager?.controlCommandStatusList?.find { it.identifier == AACPManager.Companion.ControlCommandIdentifiers.HEARING_AID }
                            val assistStatus = aacpManager?.controlCommandStatusList?.find { it.identifier == AACPManager.Companion.ControlCommandIdentifiers.HEARING_ASSIST_CONFIG }
                            hearingAidEnabled.value = (aidStatus?.value?.getOrNull(1) == 0x01.toByte()) && (assistStatus?.value?.getOrNull(0) == 0x01.toByte())
                        }
                    }
                }
            }

            val mediaAssistEnabled = remember { mutableStateOf(false) }
            val adjustMediaEnabled = remember { mutableStateOf(false) }
            val adjustPhoneEnabled = remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                aacpManager?.registerControlCommandListener(AACPManager.Companion.ControlCommandIdentifiers.HEARING_AID, hearingAidListener)
                aacpManager?.registerControlCommandListener(AACPManager.Companion.ControlCommandIdentifiers.HEARING_ASSIST_CONFIG, hearingAidListener)
            }

            DisposableEffect(Unit) {
                onDispose {
                    aacpManager?.unregisterControlCommandListener(AACPManager.Companion.ControlCommandIdentifiers.HEARING_AID, hearingAidListener)
                    aacpManager?.unregisterControlCommandListener(AACPManager.Companion.ControlCommandIdentifiers.HEARING_ASSIST_CONFIG, hearingAidListener)
                }
            }

            fun onChange(value: Boolean) {
                if (value) {
                    showDialog.value = true
                } else {
                    aacpManager?.sendControlCommand(AACPManager.Companion.ControlCommandIdentifiers.HEARING_AID.value, byteArrayOf(0x01, 0x02))
                    aacpManager?.sendControlCommand(AACPManager.Companion.ControlCommandIdentifiers.HEARING_ASSIST_CONFIG.value, 0x02.toByte())
                    hearingAidEnabled.value = value
                }
            }

            fun onAdjustPhoneChange(value: Boolean) {
                adjustPhoneEnabled.value = value
            }

            fun onAdjustMediaChange(value: Boolean) {
                adjustMediaEnabled.value = value
            }

            Text(
                text = stringResource(R.string.hearing_aid).uppercase(),
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Light,
                    color = textColor.copy(alpha = 0.6f),
                    fontFamily = FontFamily(Font(R.font.sf_pro))
                ),
                modifier = Modifier.padding(8.dp, bottom = 2.dp)
            )

            val backgroundColor = if (isDarkTheme) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundColor, RoundedCornerShape(14.dp))
            ) {
                val isDarkThemeLocal = isSystemInDarkTheme()
                var backgroundColorHA by remember { mutableStateOf(if (isDarkThemeLocal) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)) }
                val animatedBackgroundColorHA by animateColorAsState(targetValue = backgroundColorHA, animationSpec = tween(durationMillis = 500))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    backgroundColorHA = if (isDarkThemeLocal) Color(0x40888888) else Color(0x40D9D9D9)
                                    tryAwaitRelease()
                                    backgroundColorHA = if (isDarkThemeLocal) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
                                },
                                onTap = {
                                    onChange(value = !hearingAidEnabled.value)
                                }
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = stringResource(R.string.hearing_aid), modifier = Modifier.weight(1f), fontSize = 16.sp, color = textColor)
                    StyledSwitch(
                        checked = hearingAidEnabled.value,
                        onCheckedChange = {
                            onChange(value = it)
                        },
                    )
                }

                HorizontalDivider(
                    thickness = 1.5.dp,
                    color = Color(0x40888888),
                    modifier = Modifier
                        .padding(start = 12.dp, end = 0.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("hearing_aid_adjustments") }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.adjustments),
                        fontSize = 16.sp,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = textColor
                    )
                }
            }

            Text(
                text = stringResource(R.string.hearing_aid_description),
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Light,
                    color = (if (isSystemInDarkTheme()) Color.White else Color.Black).copy(alpha = 0.6f),
                    fontFamily = FontFamily(Font(R.font.sf_pro))
                ),
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            AccessibilityToggle(
                text = stringResource(R.string.media_assist),
                mutableState = mediaAssistEnabled,
                independent = true,
                description = stringResource(R.string.media_assist_description),
                title = stringResource(R.string.media_assist).uppercase()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column (
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundColor, RoundedCornerShape(14.dp))
            ) {
                val isDarkThemeLocal = isSystemInDarkTheme()
                var backgroundColorAdjustMedia by remember { mutableStateOf(if (isDarkThemeLocal) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)) }
                val animatedBackgroundColorAdjustMedia by animateColorAsState(targetValue = backgroundColorAdjustMedia, animationSpec = tween(durationMillis = 500))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    backgroundColorAdjustMedia = if (isDarkThemeLocal) Color(0x40888888) else Color(0x40D9D9D9)
                                    tryAwaitRelease()
                                    backgroundColorAdjustMedia = if (isDarkThemeLocal) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
                                },
                                onTap = {
                                    onAdjustMediaChange(!adjustMediaEnabled.value)
                                }
                            )
                        }
                        .background(
                            animatedBackgroundColorAdjustMedia,
                            RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.adjust_media),
                        modifier = Modifier.weight(1f),
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontFamily = FontFamily(Font(R.font.sf_pro)),
                            fontWeight = FontWeight.Normal,
                            color = textColor
                        )
                    )
                    StyledSwitch(
                        checked = adjustMediaEnabled.value,
                        onCheckedChange = {
                            onAdjustMediaChange(it)
                        },
                    )
                }

                HorizontalDivider(
                    thickness = 1.5.dp,
                    color = Color(0x40888888),
                    modifier = Modifier
                        .padding(start = 12.dp, end = 0.dp)
                )

                var backgroundColorAdjustPhone by remember { mutableStateOf(if (isDarkThemeLocal) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)) }
                val animatedBackgroundColorAdjustPhone by animateColorAsState(targetValue = backgroundColorAdjustPhone, animationSpec = tween(durationMillis = 500))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    backgroundColorAdjustPhone = if (isDarkThemeLocal) Color(0x40888888) else Color(0x40D9D9D9)
                                    tryAwaitRelease()
                                    backgroundColorAdjustPhone = if (isDarkThemeLocal) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
                                },
                                onTap = {
                                    onAdjustPhoneChange(!adjustPhoneEnabled.value)
                                }
                            )
                        }
                        .background(
                            animatedBackgroundColorAdjustPhone,
                            RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp)
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.adjust_calls),
                        modifier = Modifier.weight(1f),
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontFamily = FontFamily(Font(R.font.sf_pro)),
                            fontWeight = FontWeight.Normal,
                            color = textColor
                        )
                    )
                    StyledSwitch(
                        checked = adjustPhoneEnabled.value,
                        onCheckedChange = {
                            onAdjustPhoneChange(it)
                        },
                    )
                }
            }
        }
    }

    ConfirmationDialog(
        showDialog = showDialog,
        title = "Enable Hearing Aid",
        message = "Enabling Hearing Aid will disable Headphone Accommodation and Customized Transparency Mode.",
        confirmText = "Enable",
        dismissText = "Cancel",
        onConfirm = {
            showDialog.value = false
            val enrolled = aacpManager?.controlCommandStatusList?.find { it.identifier == AACPManager.Companion.ControlCommandIdentifiers.HEARING_AID }?.value?.getOrNull(0) == 0x01.toByte()
            if (!enrolled) {
                aacpManager?.sendControlCommand(AACPManager.Companion.ControlCommandIdentifiers.HEARING_AID.value, byteArrayOf(0x01, 0x01))
            } else {
                aacpManager.sendControlCommand(AACPManager.Companion.ControlCommandIdentifiers.HEARING_AID.value, byteArrayOf(0x01, 0x01))
            }
            aacpManager?.sendControlCommand(AACPManager.Companion.ControlCommandIdentifiers.HEARING_ASSIST_CONFIG.value, 0x01.toByte())
            hearingAidEnabled.value = true
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val data = attManager.read(ATTHandles.TRANSPARENCY)
                    val parsed = parseTransparencySettingsResponse(data)
                    if (parsed != null) {
                        val disabledSettings = parsed.copy(enabled = false)
                        sendTransparencySettings(attManager, disabledSettings)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error disabling transparency: ${e.message}")
                }
            }
        },
        hazeState = hazeState
    )
}
