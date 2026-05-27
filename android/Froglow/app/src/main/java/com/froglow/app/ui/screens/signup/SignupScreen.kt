package com.froglow.app.ui.screens.signup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate

private val ScreenBg = Color(0xFFFFFEFC)

private val FieldBg = Color(0xFFFFFFFF)
private val FieldBorder = Color(0xFFE4D9C8)

private val PrimaryText = Color(0xFF2F2B26)
private val SecondaryText = Color(0xFF7C756C)

private val ConfirmGreen = Color(0xFF77A95B)
private val GreenSoft = Color(0xFFEAF4E2)
private val DisabledGreen = Color(0xFFDDEAD6)

private val ErrorRed = Color(0xFFE95F62)
private val ErrorSoft = Color(0xFFFFEFEF)

@Composable
fun SignupScreen(
    onSignupSuccessGoLogin: () -> Unit,
    onBack: () -> Unit
) {
    val vm: SignupViewModel = viewModel()
    val state by vm.state.collectAsState()

    var loginId by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("2000-01-01") }
    var showBirthDialog by remember { mutableStateOf(false) }

    val canSubmit =
        !state.loading &&
                loginId.isNotBlank() &&
                email.isNotBlank() &&
                password.isNotBlank() &&
                name.isNotBlank() &&
                gender.isNotBlank() &&
                birthDate.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 36.dp)
    ) {
        Text(
            text = "회원가입",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = PrimaryText
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "기본 정보를 입력하고 계정을 만들어주세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = SecondaryText
        )

        Spacer(modifier = Modifier.height(26.dp))

        SignupTextField(
            value = loginId,
            onValueChange = { loginId = it },
            label = "아이디",
            enabled = !state.loading
        )

        Spacer(modifier = Modifier.height(12.dp))

        SignupTextField(
            value = email,
            onValueChange = { email = it },
            label = "이메일",
            enabled = !state.loading,
            keyboardType = KeyboardType.Email
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = {
                Text(
                    text = "비밀번호",
                    color = SecondaryText
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            enabled = !state.loading,
            shape = RoundedCornerShape(20.dp),
            colors = signupTextFieldColors()
        )

        Spacer(modifier = Modifier.height(12.dp))

        SignupTextField(
            value = name,
            onValueChange = { name = it },
            label = "이름",
            enabled = !state.loading
        )

        Spacer(modifier = Modifier.height(18.dp))

        SectionLabel(text = "성별")

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GenderButton(
                text = "남성",
                value = "m",
                selectedGender = gender,
                enabled = !state.loading,
                modifier = Modifier.weight(1f)
            ) {
                gender = "m"
            }

            GenderButton(
                text = "여성",
                value = "f",
                selectedGender = gender,
                enabled = !state.loading,
                modifier = Modifier.weight(1f)
            ) {
                gender = "f"
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        SectionLabel(text = "생년월일")

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clickable(enabled = !state.loading) {
                    showBirthDialog = true
                },
            shape = RoundedCornerShape(20.dp),
            color = FieldBg,
            border = BorderStroke(1.dp, FieldBorder),
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = birthDate,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = PrimaryText
                )

                Text(
                    text = "선택",
                    color = ConfirmGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (state.errorMessage.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = ErrorSoft,
                tonalElevation = 0.dp
            ) {
                Text(
                    text = state.errorMessage,
                    color = ErrorRed,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Button(
            onClick = {
                vm.updateLoginId(loginId.trim())
                vm.updateEmail(email.trim())
                vm.updatePassword(password)
                vm.updateName(name.trim())
                vm.updateGender(gender)
                vm.updateBirthDate(birthDate)

                vm.signup {
                    onSignupSuccessGoLogin()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = canSubmit,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ConfirmGreen,
                contentColor = Color.White,
                disabledContainerColor = DisabledGreen,
                disabledContentColor = Color.White.copy(alpha = 0.85f)
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                focusedElevation = 0.dp,
                hoveredElevation = 0.dp,
                disabledElevation = 0.dp
            )
        ) {
            if (state.loading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "가입 중...",
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "회원가입",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !state.loading,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, FieldBorder),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = FieldBg,
                contentColor = PrimaryText
            )
        ) {
            Text(
                text = "뒤로",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    if (showBirthDialog) {
        BirthDatePickerDialog(
            currentDate = birthDate,
            onDismiss = { showBirthDialog = false },
            onConfirm = {
                birthDate = it
                showBirthDialog = false
            }
        )
    }
}

@Composable
private fun SectionLabel(
    text: String
) {
    Text(
        text = text,
        color = SecondaryText,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun BirthDatePickerDialog(
    currentDate: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val parsed = runCatching { LocalDate.parse(currentDate) }
        .getOrElse { LocalDate.of(2000, 1, 1) }

    var year by remember { mutableStateOf(parsed.year) }
    var month by remember { mutableStateOf(parsed.monthValue) }
    var day by remember { mutableStateOf(parsed.dayOfMonth) }

    val maxDay = remember(year, month) {
        LocalDate.of(year, month, 1).lengthOfMonth()
    }

    if (day > maxDay) day = maxDay

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = FieldBg,
            border = BorderStroke(1.dp, FieldBorder),
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = "생년월일 선택",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )

                DateAdjustRow(
                    label = "년도",
                    value = "${year}년",
                    onMinus = { if (year > 1900) year-- },
                    onPlus = { if (year < LocalDate.now().year) year++ }
                )

                DateAdjustRow(
                    label = "월",
                    value = "${month}월",
                    onMinus = {
                        if (month > 1) {
                            month--
                        } else {
                            month = 12
                            if (year > 1900) year--
                        }
                    },
                    onPlus = {
                        if (month < 12) {
                            month++
                        } else {
                            month = 1
                            if (year < LocalDate.now().year) year++
                        }
                    }
                )

                DateAdjustRow(
                    label = "일",
                    value = "${day}일",
                    onMinus = { if (day > 1) day-- },
                    onPlus = { if (day < maxDay) day++ }
                )

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = GreenSoft,
                    tonalElevation = 0.dp
                ) {
                    Text(
                        text = "선택 날짜: ${formatDate(year, month, day)}",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        color = ConfirmGreen,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "취소",
                            color = SecondaryText
                        )
                    }

                    Button(
                        onClick = {
                            onConfirm(formatDate(year, month, day))
                        },
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ConfirmGreen,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "확인",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DateAdjustRow(
    label: String,
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.width(54.dp),
            fontWeight = FontWeight.Bold,
            color = PrimaryText
        )

        OutlinedButton(
            onClick = onMinus,
            modifier = Modifier.size(44.dp),
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, FieldBorder),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = FieldBg,
                contentColor = ConfirmGreen
            )
        ) {
            Text(
                text = "-",
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = value,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = PrimaryText
        )

        OutlinedButton(
            onClick = onPlus,
            modifier = Modifier.size(44.dp),
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, FieldBorder),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = FieldBg,
                contentColor = ConfirmGreen
            )
        ) {
            Text(
                text = "+",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatDate(
    year: Int,
    month: Int,
    day: Int
): String {
    return "%04d-%02d-%02d".format(year, month, day)
}

@Composable
private fun SignupTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                text = label,
                color = SecondaryText
            )
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(20.dp),
        colors = signupTextFieldColors()
    )
}

@Composable
private fun GenderButton(
    text: String,
    value: String,
    selectedGender: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val selected = selectedGender == value

    Surface(
        modifier = modifier
            .height(56.dp)
            .clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = if (selected) GreenSoft else FieldBg,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) ConfirmGreen else FieldBorder
        ),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$text ($value)",
                color = if (selected) ConfirmGreen else PrimaryText,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun signupTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = FieldBg,
    unfocusedContainerColor = FieldBg,
    disabledContainerColor = FieldBg,
    focusedBorderColor = ConfirmGreen,
    unfocusedBorderColor = FieldBorder,
    focusedLabelColor = ConfirmGreen,
    unfocusedLabelColor = SecondaryText,
    cursorColor = ConfirmGreen,
    focusedTextColor = PrimaryText,
    unfocusedTextColor = PrimaryText,
    disabledTextColor = SecondaryText
)