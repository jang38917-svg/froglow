package com.froglow.app.ui.screens.main.tabs

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.froglow.app.data.SessionStore
import com.froglow.app.network.ApiService
import com.froglow.app.network.ReminderDto
import com.froglow.app.network.ReminderRequest
import com.froglow.app.network.ReminderUpdateRequest
import com.froglow.app.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

private val MyPageBg = Color(0xFFFFFEFC)

private val CardBg = Color(0xFFFFFFFF)
private val CardBorder = Color(0xFFE4D9C8)

private val PrimaryText = Color(0xFF2F2B26)
private val SecondaryText = Color(0xFF7C756C)

private val GreenPrimary = Color(0xFF77A95B)
private val GreenSoft = Color(0xFFEAF4E2)

private val YellowSoft = Color(0xFFFFF4DD)
private val RedSoft = Color(0xFFFFEFEF)
private val RedText = Color(0xFFE95F62)

private val ReminderGreenPrimary = Color(0xFF8EAC78)
private val ReminderGreenSoft = Color(0xFFF2F7ED)

@Composable
fun MyPageTab(
    sessionStore: SessionStore,
    onRoutineChangeClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }

    var profileUri by remember {
        mutableStateOf(loadProfileImageUri(context))
    }

    val loginId = remember { loadLoginId(context) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            saveProfileImageUri(context, it.toString())
            profileUri = it.toString()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MyPageBg),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            PageTitle()
        }

        item {
            ProfileSection(
                loginId = loginId,
                profileUri = profileUri,
                onProfileClick = {
                    imagePickerLauncher.launch("image/*")
                }
            )
        }

        item {
            QuickMenuCard(
                onReminderClick = { showReminderDialog = true },
                onRoutineChangeClick = onRoutineChangeClick,
                onLogoutClick = { showLogoutDialog = true }
            )
        }

        item {
            SettingsCard(
                onReminderClick = { showReminderDialog = true },
                onRoutineChangeClick = onRoutineChangeClick,
                onLogoutClick = { showLogoutDialog = true }
            )
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showReminderDialog) {
        ReminderSettingsDialog(
            onDismiss = { showReminderDialog = false }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = CardBg,
            title = {
                Text(
                    text = "로그아웃",
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "정말 로그아웃할까요?",
                    color = SecondaryText
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        scope.launch {
                            sessionStore.logout()
                        }
                    }
                ) {
                    Text(
                        text = "로그아웃",
                        color = RedText,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(
                        text = "취소",
                        color = SecondaryText
                    )
                }
            }
        )
    }
}

@Composable
private fun PageTitle() {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "마이페이지",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = PrimaryText
        )

        Text(
            text = "내 정보와 루틴을 관리해요",
            style = MaterialTheme.typography.bodyMedium,
            color = SecondaryText
        )
    }
}

@Composable
private fun ProfileSection(
    loginId: String,
    profileUri: String?,
    onProfileClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color(0x0A000000),
                spotColor = Color(0x0A000000)
            ),
        shape = RoundedCornerShape(28.dp),
        color = CardBg,
        border = BorderStroke(1.dp, CardBorder),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier
                    .size(116.dp)
                    .clickable { onProfileClick() },
                shape = CircleShape,
                color = GreenSoft,
                border = BorderStroke(1.dp, CardBorder),
                tonalElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (profileUri != null) {
                        AsyncImage(
                            model = profileUri,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = "🐸",
                            style = MaterialTheme.typography.displaySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = loginId,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = GreenSoft,
                tonalElevation = 0.dp
            ) {
                Text(
                    text = "프로필 사진 변경",
                    modifier = Modifier
                        .clickable { onProfileClick() }
                        .padding(horizontal = 18.dp, vertical = 9.dp),
                    fontWeight = FontWeight.Bold,
                    color = GreenPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun QuickMenuCard(
    onReminderClick: () -> Unit,
    onRoutineChangeClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color(0x0A000000),
                spotColor = Color(0x0A000000)
            ),
        shape = RoundedCornerShape(28.dp),
        color = CardBg,
        border = BorderStroke(1.dp, CardBorder),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickMenuItem(
                modifier = Modifier.weight(1f),
                icon = "🔔",
                label = "알림 설정",
                backgroundColor = GreenSoft,
                textColor = GreenPrimary,
                onClick = onReminderClick
            )

            QuickMenuItem(
                modifier = Modifier.weight(1f),
                icon = "📸",
                label = "루틴 변경",
                backgroundColor = YellowSoft,
                textColor = Color(0xFFD79A27),
                onClick = onRoutineChangeClick
            )

            QuickMenuItem(
                modifier = Modifier.weight(1f),
                icon = "🚪",
                label = "로그아웃",
                backgroundColor = RedSoft,
                textColor = RedText,
                onClick = onLogoutClick
            )
        }
    }
}

@Composable
private fun QuickMenuItem(
    modifier: Modifier = Modifier,
    icon: String,
    label: String,
    backgroundColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(92.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        color = backgroundColor,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = label,
                color = textColor,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun SettingsCard(
    onReminderClick: () -> Unit,
    onRoutineChangeClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color(0x0A000000),
                spotColor = Color(0x0A000000)
            ),
        shape = RoundedCornerShape(28.dp),
        color = CardBg,
        border = BorderStroke(1.dp, CardBorder),
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp)) {
            Text(
                text = "설정",
                color = SecondaryText,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(10.dp))

            SettingRow("알림 설정", onReminderClick)
            HorizontalDivider(color = CardBorder)
            SettingRow("루틴 변경", onRoutineChangeClick)
            HorizontalDivider(color = CardBorder)
            SettingRow("로그아웃", onLogoutClick, danger = true)
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    onClick: () -> Unit,
    danger: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.SemiBold,
            color = if (danger) RedText else PrimaryText,
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            text = ">",
            color = SecondaryText,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ReminderSettingsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var amReminder by remember { mutableStateOf<ReminderDto?>(null) }
    var pmReminder by remember { mutableStateOf<ReminderDto?>(null) }
    var lipReminder by remember { mutableStateOf<ReminderDto?>(null) }
    var handReminder by remember { mutableStateOf<ReminderDto?>(null) }
    var mistReminder by remember { mutableStateOf<ReminderDto?>(null) }

    var amEnabled by remember { mutableStateOf(false) }
    var pmEnabled by remember { mutableStateOf(false) }
    var lipEnabled by remember { mutableStateOf(false) }
    var handEnabled by remember { mutableStateOf(false) }
    var mistEnabled by remember { mutableStateOf(false) }

    var amTime by remember { mutableStateOf("09:00") }
    var pmTime by remember { mutableStateOf("22:00") }

    var lipStartTime by remember { mutableStateOf("09:00") }
    var handStartTime by remember { mutableStateOf("09:00") }
    var mistStartTime by remember { mutableStateOf("09:00") }

    var lipInterval by remember { mutableStateOf("2") }
    var handInterval by remember { mutableStateOf("3") }
    var mistInterval by remember { mutableStateOf("4") }

    LaunchedEffect(Unit) {
        loading = true
        errorMessage = null

        try {
            val api = RetrofitClient.create(context)
            val reminders = withContext(Dispatchers.IO) {
                api.getReminders()
            }

            fun findRoutineReminder(key: String): ReminderDto? {
                return reminders.firstOrNull {
                    it.reminder_type.equals("ROUTINE", ignoreCase = true) &&
                            it.routine_key.equals(key, ignoreCase = true)
                }
            }

            fun findProductReminder(labelKeyword: String): ReminderDto? {
                return reminders.firstOrNull {
                    it.reminder_type.equals("PRODUCT", ignoreCase = true) &&
                            (
                                    it.label?.contains(labelKeyword, ignoreCase = true) == true ||
                                            it.label?.contains("${labelKeyword} 알림", ignoreCase = true) == true
                                    )
                }
            }

            amReminder = findRoutineReminder("AM")
            pmReminder = findRoutineReminder("PM")
            lipReminder = findProductReminder("립밤")
            handReminder = findProductReminder("핸드크림")
            mistReminder = findProductReminder("미스트")

            amEnabled = amReminder?.is_active ?: false
            pmEnabled = pmReminder?.is_active ?: false
            lipEnabled = lipReminder?.is_active ?: false
            handEnabled = handReminder?.is_active ?: false
            mistEnabled = mistReminder?.is_active ?: false

            amTime = amReminder?.remind_time?.takeIf { it.isNotBlank() } ?: "09:00"
            pmTime = pmReminder?.remind_time?.takeIf { it.isNotBlank() } ?: "22:00"

            lipStartTime = lipReminder?.remind_time?.takeIf { it.isNotBlank() } ?: "09:00"
            handStartTime = handReminder?.remind_time?.takeIf { it.isNotBlank() } ?: "09:00"
            mistStartTime = mistReminder?.remind_time?.takeIf { it.isNotBlank() } ?: "09:00"

            lipInterval = lipReminder?.interval_hours?.toString() ?: "2"
            handInterval = handReminder?.interval_hours?.toString() ?: "3"
            mistInterval = mistReminder?.interval_hours?.toString() ?: "4"
        } catch (e: Exception) {
            errorMessage = "알림 정보를 불러오지 못했습니다."
            android.util.Log.e("REMINDER_SETTING", "load failed", e)
        } finally {
            loading = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp),
            shape = RoundedCornerShape(30.dp),
            color = CardBg,
            border = BorderStroke(1.dp, CardBorder),
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "알림 설정",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )

                    Text(
                        text = "루틴과 보습 알림 시간을 관리해요",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText
                    )
                }

                if (loading) {
                    ReminderInfoCard(
                        title = "불러오는 중",
                        description = "알림 정보를 불러오고 있어요."
                    )
                } else {
                    ReminderSectionTitle("루틴 알림")

                    ReminderTimeEditor(
                        title = "아침 루틴 알림",
                        enabled = amEnabled,
                        time = amTime,
                        onEnabledChange = { amEnabled = it },
                        onTimeChange = { amTime = it }
                    )

                    ReminderTimeEditor(
                        title = "저녁 루틴 알림",
                        enabled = pmEnabled,
                        time = pmTime,
                        onEnabledChange = { pmEnabled = it },
                        onTimeChange = { pmTime = it }
                    )

                    HorizontalDivider(color = CardBorder)

                    ReminderSectionTitle("사용자 설정 알림")

                    IntervalReminderEditor(
                        title = "립밤",
                        enabled = lipEnabled,
                        startTime = lipStartTime,
                        interval = lipInterval,
                        onEnabledChange = { lipEnabled = it },
                        onStartTimeChange = { lipStartTime = it },
                        onIntervalChange = { lipInterval = it }
                    )

                    IntervalReminderEditor(
                        title = "핸드크림",
                        enabled = handEnabled,
                        startTime = handStartTime,
                        interval = handInterval,
                        onEnabledChange = { handEnabled = it },
                        onStartTimeChange = { handStartTime = it },
                        onIntervalChange = { handInterval = it }
                    )

                    IntervalReminderEditor(
                        title = "미스트",
                        enabled = mistEnabled,
                        startTime = mistStartTime,
                        interval = mistInterval,
                        onEnabledChange = { mistEnabled = it },
                        onStartTimeChange = { mistStartTime = it },
                        onIntervalChange = { mistInterval = it }
                    )

                    ReminderInfoCard(
                        title = "알림 안내",
                        description = "시작 시간은 처음 알림이 울릴 시간이고, 반복 간격은 그 시간부터 몇 시간마다 울릴지입니다."
                    )

                    if (errorMessage != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = RedSoft,
                            tonalElevation = 0.dp
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                color = RedText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !saving
                    ) {
                        Text(
                            text = "닫기",
                            color = SecondaryText,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.size(8.dp))

                    Button(
                        onClick = {
                            if (
                                !isValidTime(amTime) ||
                                !isValidTime(pmTime) ||
                                !isValidTime(lipStartTime) ||
                                !isValidTime(handStartTime) ||
                                !isValidTime(mistStartTime)
                            ) {
                                errorMessage = "시간 형식이 올바르지 않습니다. 예: 09:00"
                                return@Button
                            }

                            val lipIntervalInt = lipInterval.toIntOrNull()
                            val handIntervalInt = handInterval.toIntOrNull()
                            val mistIntervalInt = mistInterval.toIntOrNull()

                            if (
                                lipIntervalInt == null || lipIntervalInt <= 0 ||
                                handIntervalInt == null || handIntervalInt <= 0 ||
                                mistIntervalInt == null || mistIntervalInt <= 0
                            ) {
                                errorMessage = "반복 간격은 1 이상의 숫자로 입력해주세요."
                                return@Button
                            }

                            scope.launch {
                                saving = true
                                errorMessage = null

                                try {
                                    val api = RetrofitClient.create(context)

                                    withContext(Dispatchers.IO) {
                                        saveReminder(
                                            api = api,
                                            existing = amReminder,
                                            reminderType = "ROUTINE",
                                            routineKey = "AM",
                                            frequency = "DAILY",
                                            intervalHours = null,
                                            remindTime = normalizeTime(amTime),
                                            label = "아침 루틴 알림",
                                            enabled = amEnabled
                                        )

                                        saveReminder(
                                            api = api,
                                            existing = pmReminder,
                                            reminderType = "ROUTINE",
                                            routineKey = "PM",
                                            frequency = "DAILY",
                                            intervalHours = null,
                                            remindTime = normalizeTime(pmTime),
                                            label = "저녁 루틴 알림",
                                            enabled = pmEnabled
                                        )

                                        saveReminder(
                                            api = api,
                                            existing = lipReminder,
                                            reminderType = "PRODUCT",
                                            routineKey = null,
                                            frequency = "EVERY_N_HOURS",
                                            intervalHours = lipIntervalInt,
                                            remindTime = normalizeTime(lipStartTime),
                                            label = "립밤 알림",
                                            enabled = lipEnabled
                                        )

                                        saveReminder(
                                            api = api,
                                            existing = handReminder,
                                            reminderType = "PRODUCT",
                                            routineKey = null,
                                            frequency = "EVERY_N_HOURS",
                                            intervalHours = handIntervalInt,
                                            remindTime = normalizeTime(handStartTime),
                                            label = "핸드크림 알림",
                                            enabled = handEnabled
                                        )

                                        saveReminder(
                                            api = api,
                                            existing = mistReminder,
                                            reminderType = "PRODUCT",
                                            routineKey = null,
                                            frequency = "EVERY_N_HOURS",
                                            intervalHours = mistIntervalInt,
                                            remindTime = normalizeTime(mistStartTime),
                                            label = "미스트 알림",
                                            enabled = mistEnabled
                                        )
                                    }

                                    onDismiss()
                                } catch (e: Exception) {
                                    val detail = if (e is HttpException) {
                                        e.response()?.errorBody()?.string()
                                    } else {
                                        null
                                    }

                                    errorMessage = "알림 저장에 실패했습니다."
                                    android.util.Log.e(
                                        "REMINDER_SETTING",
                                        "save failed detail=$detail",
                                        e
                                    )
                                } finally {
                                    saving = false
                                }
                            }
                        },
                        enabled = !loading && !saving,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ReminderGreenPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = if (saving) "저장 중" else "저장",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderSectionTitle(
    title: String
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = PrimaryText
    )
}

@Composable
private fun ReminderInfoCard(
    title: String,
    description: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = ReminderGreenSoft,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = ReminderGreenPrimary
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryText
            )
        }
    }
}

@Composable
private fun ReminderTimeEditor(
    title: String,
    enabled: Boolean,
    time: String,
    onEnabledChange: (Boolean) -> Unit,
    onTimeChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = if (enabled) CardBg else Color(0xFFFAF8F4),
        border = BorderStroke(1.dp, CardBorder),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Text(
                        text = if (enabled) "알림 켜짐" else "알림 꺼짐",
                        color = if (enabled) ReminderGreenPrimary else SecondaryText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ReminderGreenPrimary,
                        checkedBorderColor = ReminderGreenPrimary,
                        uncheckedThumbColor = Color(0xFF8A8392),
                        uncheckedTrackColor = Color(0xFFF1EDF5),
                        uncheckedBorderColor = Color(0xFF8A8392)
                    )
                )
            }

            OutlinedTextField(
                value = time,
                onValueChange = onTimeChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("알림 시간 예: 09:00") },
                singleLine = true,
                enabled = enabled,
                shape = RoundedCornerShape(18.dp)
            )
        }
    }
}

@Composable
private fun IntervalReminderEditor(
    title: String,
    enabled: Boolean,
    startTime: String,
    interval: String,
    onEnabledChange: (Boolean) -> Unit,
    onStartTimeChange: (String) -> Unit,
    onIntervalChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = if (enabled) CardBg else Color(0xFFFAF8F4),
        border = BorderStroke(1.dp, CardBorder),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Text(
                        text = if (enabled) "반복 알림 켜짐" else "반복 알림 꺼짐",
                        color = if (enabled) ReminderGreenPrimary else SecondaryText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ReminderGreenPrimary,
                        checkedBorderColor = ReminderGreenPrimary,
                        uncheckedThumbColor = Color(0xFF8A8392),
                        uncheckedTrackColor = Color(0xFFF1EDF5),
                        uncheckedBorderColor = Color(0xFF8A8392)
                    )
                )
            }

            OutlinedTextField(
                value = startTime,
                onValueChange = onStartTimeChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("시작 시간 예: 09:00") },
                singleLine = true,
                enabled = enabled,
                shape = RoundedCornerShape(18.dp)
            )

            OutlinedTextField(
                value = interval,
                onValueChange = onIntervalChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("반복 간격 예: 3") },
                singleLine = true,
                enabled = enabled,
                shape = RoundedCornerShape(18.dp)
            )
        }
    }
}

private suspend fun saveReminder(
    api: ApiService,
    existing: ReminderDto?,
    reminderType: String,
    routineKey: String?,
    frequency: String,
    intervalHours: Int?,
    remindTime: String,
    label: String,
    enabled: Boolean
) {
    android.util.Log.e(
        "REMINDER_SETTING",
        "saveReminder existing=${existing?.id}, type=$reminderType, key=$routineKey, frequency=$frequency, interval=$intervalHours, time=$remindTime, label=$label, enabled=$enabled"
    )

    if (existing == null && !enabled) {
        return
    }

    if (existing == null) {
        api.createReminder(
            ReminderRequest(
                reminder_type = reminderType,
                routine_key = routineKey,
                frequency = frequency,
                interval_hours = intervalHours,
                remind_time = remindTime,
                timezone = "Asia/Seoul",
                label = label,
                is_active = true
            )
        )
    } else {
        api.updateReminder(
            reminderId = existing.id,
            request = ReminderUpdateRequest(
                reminder_type = reminderType,
                routine_key = routineKey,
                frequency = frequency,
                interval_hours = intervalHours,
                remind_time = remindTime,
                timezone = "Asia/Seoul",
                label = label,
                is_active = enabled
            )
        )
    }
}

private fun isValidTime(value: String): Boolean {
    val parts = value.split(":")
    if (parts.size != 2) return false

    val hour = parts[0].toIntOrNull() ?: return false
    val minute = parts[1].toIntOrNull() ?: return false

    return hour in 0..23 && minute in 0..59
}

private fun normalizeTime(value: String): String {
    val parts = value.split(":")
    val hour = parts[0].toInt().toString().padStart(2, '0')
    val minute = parts[1].toInt().toString().padStart(2, '0')
    return "$hour:$minute"
}

private fun saveProfileImageUri(context: Context, uri: String) {
    context.getSharedPreferences("mypage_pref", Context.MODE_PRIVATE)
        .edit()
        .putString("profile_image_uri", uri)
        .apply()
}

private fun loadProfileImageUri(context: Context): String? {
    return context.getSharedPreferences("mypage_pref", Context.MODE_PRIVATE)
        .getString("profile_image_uri", null)
}

private fun loadLoginId(context: Context): String {
    return context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        .getString("login_id", null)
        ?: "Froglow User"
}