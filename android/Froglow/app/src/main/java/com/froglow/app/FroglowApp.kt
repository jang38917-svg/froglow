package com.froglow.app

import android.app.Application
import android.widget.Toast
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.froglow.app.data.SessionStore
import com.froglow.app.navigation.Routes
import com.froglow.app.network.RetrofitClient
import com.froglow.app.network.SurveySubmitRequest
import com.froglow.app.reminder.ReminderScheduler
import com.froglow.app.session.SessionManager
import com.froglow.app.ui.screens.camera.CameraScreen
import com.froglow.app.ui.screens.camera.CapturedPhotoTempStore
import com.froglow.app.ui.screens.camera.PhotoDateSelectScreen
import com.froglow.app.ui.screens.login.LoginScreen
import com.froglow.app.ui.screens.main.MainScreen
import com.froglow.app.ui.screens.main.home.CalendarDetailScreen
import com.froglow.app.ui.screens.main.home.DiagnosisResultScreen
import com.froglow.app.ui.screens.main.products.ProductDetailScreen
import com.froglow.app.ui.screens.main.products.ProductsViewModel
import com.froglow.app.ui.screens.signup.SignupScreen
import com.froglow.app.ui.screens.skin_type.SkinTypeChoice
import com.froglow.app.ui.screens.skin_type.SkinTypeScreen
import com.froglow.app.ui.screens.survey.SurveyScreen
import kotlinx.coroutines.launch

@Composable
fun FroglowApp(
    application: Application,
    sessionStore: SessionStore = remember { SessionStore(application) }
) {
    val navController = rememberNavController()
    val productsVM = remember { ProductsViewModel(application) }
    val scope = rememberCoroutineScope()

    val isLoggedIn by sessionStore.isLoggedIn.collectAsState()
    val isSurveyDone by sessionStore.isSurveyDone.collectAsState()

    val startDestination = remember(isLoggedIn, isSurveyDone) {
        when {
            !isLoggedIn -> Routes.LOGIN
            !isSurveyDone -> Routes.SKIN_TYPE
            else -> Routes.MAIN
        }
    }

    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            navController.navigate(Routes.LOGIN) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            try {
                val api = RetrofitClient.create(application)
                val reminders = api.getReminders()
                ReminderScheduler.scheduleAll(application, reminders)
                android.util.Log.d("REMINDER_DEBUG", "reminders scheduled = ${reminders.size}")
            } catch (e: Exception) {
                android.util.Log.e("REMINDER_DEBUG", "reminder schedule failed", e)
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = { needsSurvey, token ->
                    val newSessionManager = SessionManager(application)

                    newSessionManager.saveAccessToken(token)
                    sessionStore.setLoggedIn(true)
                    sessionStore.setAccessToken(token)
                    sessionStore.setSurveyDone(!needsSurvey)

                    val next = if (needsSurvey) Routes.SKIN_TYPE else Routes.MAIN
                    navController.navigate(next) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onSignupClick = {
                    navController.navigate(Routes.SIGNUP)
                }
            )
        }

        composable(Routes.SIGNUP) {
            SignupScreen(
                onSignupSuccessGoLogin = {
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.SKIN_TYPE) {
            SkinTypeScreen(
                onConfirm = { choice ->
                    when (choice) {
                        SkinTypeChoice.DRY -> {
                            scope.launch {
                                try {
                                    val api = RetrofitClient.create(application)
                                    api.submitSurvey(
                                        SurveySubmitRequest(
                                            preset_skin_type = "건성",
                                            answers = emptyList()
                                        )
                                    )

                                    sessionStore.setSkinType("건성")

                                    navController.navigate("${Routes.CAMERA}?mode=normal") {
                                        popUpTo(Routes.SKIN_TYPE) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        application,
                                        e.message ?: "건성 저장 실패",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }

                        SkinTypeChoice.OILY -> {
                            scope.launch {
                                try {
                                    val api = RetrofitClient.create(application)
                                    api.submitSurvey(
                                        SurveySubmitRequest(
                                            preset_skin_type = "지성",
                                            answers = emptyList()
                                        )
                                    )

                                    sessionStore.setSkinType("지성")

                                    navController.navigate("${Routes.CAMERA}?mode=normal") {
                                        popUpTo(Routes.SKIN_TYPE) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        application,
                                        e.message ?: "지성 저장 실패",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }

                        SkinTypeChoice.COMBINATION -> {
                            scope.launch {
                                try {
                                    val api = RetrofitClient.create(application)
                                    api.submitSurvey(
                                        SurveySubmitRequest(
                                            preset_skin_type = "복합성",
                                            answers = emptyList()
                                        )
                                    )

                                    sessionStore.setSkinType("복합성")

                                    navController.navigate("${Routes.CAMERA}?mode=normal") {
                                        popUpTo(Routes.SKIN_TYPE) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        application,
                                        e.message ?: "복합성 저장 실패",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }

                        SkinTypeChoice.UNKNOWN -> {
                            navController.navigate(Routes.SURVEY) {
                                launchSingleTop = true
                            }
                        }
                    }
                }
            )
        }

        composable(Routes.SURVEY) {
            SurveyScreen(
                onNavigateToCamera = {
                    navController.navigate("${Routes.CAMERA}?mode=normal") {
                        popUpTo(Routes.SKIN_TYPE) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = "${Routes.CAMERA}?mode={mode}",
            arguments = listOf(
                navArgument("mode") {
                    type = NavType.StringType
                    defaultValue = "normal"
                }
            )
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "normal"

            android.util.Log.d("NAV_DEBUG", "Camera route 진입 성공 mode=$mode")

            CameraScreen(
                onBack = {
                    navController.popBackStack()
                },
                onPhotosDone = { photoUris ->
                    CapturedPhotoTempStore.setPhotos(photoUris)

                    if (mode == "routine_change") {
                        navController.navigate("${Routes.DIAGNOSIS_RESULT}?date=&mode=routine_change") {
                            popUpTo("${Routes.CAMERA}?mode={mode}") { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        navController.navigate("${Routes.PHOTO_DATE_SELECT}?mode=normal") {
                            popUpTo("${Routes.CAMERA}?mode={mode}") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        composable(
            route = "${Routes.PHOTO_DATE_SELECT}?mode={mode}",
            arguments = listOf(
                navArgument("mode") {
                    type = NavType.StringType
                    defaultValue = "normal"
                }
            )
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "normal"
            val photoUris = CapturedPhotoTempStore.getPhotos()

            PhotoDateSelectScreen(
                photoUris = photoUris,
                onBack = {
                    navController.popBackStack()
                },
                onSaveDone = { selectedDate ->
                    navController.navigate("${Routes.DIAGNOSIS_RESULT}?date=$selectedDate&mode=$mode") {
                        popUpTo("${Routes.PHOTO_DATE_SELECT}?mode={mode}") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = "${Routes.DIAGNOSIS_RESULT}?date={date}&mode={mode}",
            arguments = listOf(
                navArgument("date") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("mode") {
                    type = NavType.StringType
                    defaultValue = "normal"
                }
            )
        ) { backStackEntry ->
            val selectedDate = backStackEntry.arguments?.getString("date")
            val mode = backStackEntry.arguments?.getString("mode") ?: "normal"

            DiagnosisResultScreen(
                selectedDateString = selectedDate,
                routineMode = mode,
                onBack = {
                    navController.popBackStack()
                },
                onGoHome = {
                    scope.launch {
                        sessionStore.setSurveyDone(true)
                    }

                    CapturedPhotoTempStore.clear()

                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.DIAGNOSIS_RESULT) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.MAIN) {
            MainScreen(
                sessionStore = sessionStore,
                viewModel = productsVM,
                onOpenProductDetail = { productId ->
                    navController.navigate("${Routes.PRODUCT_DETAIL}/$productId")
                },
                onCameraClick = {
                    navController.navigate("${Routes.CAMERA}?mode=normal")
                },
                onRoutineChangeClick = {
                    navController.navigate("${Routes.CAMERA}?mode=routine_change")
                },
                onOpenCalendarDetail = { date ->
                    navController.navigate("${Routes.CALENDAR_DETAIL}/$date")
                }
            )
        }

        composable(
            route = "${Routes.PRODUCT_DETAIL}/{productId}",
            arguments = listOf(
                navArgument("productId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getLong("productId") ?: -1L

            val products by productsVM.products.collectAsState()

            val product = products.firstOrNull { it.id.toLong() == productId }

            when {
                products.isEmpty() -> {
                    Text("제품 정보를 불러오는 중입니다...")
                }

                product == null -> {
                    Text("제품을 찾을 수 없습니다.")
                }

                else -> {
                    ProductDetailScreen(product = product)
                }
            }
        }

        composable(
            route = "${Routes.CALENDAR_DETAIL}/{date}",
            arguments = listOf(
                navArgument("date") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date").orEmpty()

            CalendarDetailScreen(
                selectedDate = date,
                onBack = { navController.popBackStack() },
                onOpenProductDetail = { productId ->
                    navController.navigate("${Routes.PRODUCT_DETAIL}/$productId")
                }
            )
        }
    }
}