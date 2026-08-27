package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.model.AppLanguage
import com.example.data.model.AppThemeSetting
import com.example.ui.components.AddTransactionChoiceSheet
import com.example.ui.components.BottomNavTab
import com.example.ui.components.PinLockScreen
import com.example.ui.components.RemindersBottomSheet
import com.example.ui.components.SuDhanBottomNav
import com.example.ui.components.SuDhanTopBar
import com.example.ui.screens.AddTransactionScreen
import com.example.ui.screens.CalculatorScreen
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel

enum class AppView {
    MAIN_TABS,
    CALCULATOR,
    TRANSACTIONS
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val appTheme by viewModel.appTheme.collectAsState()
            val isDarkTheme = when (appTheme) {
                AppThemeSetting.SYSTEM -> isSystemInDarkTheme()
                AppThemeSetting.LIGHT -> false
                AppThemeSetting.DARK -> true
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                val isUnlocked by viewModel.isAppUnlocked.collectAsState()
                val isPinEnabled by viewModel.isPinLockEnabled.collectAsState()
                val language by viewModel.appLanguage.collectAsState()
                val toastMessage by viewModel.toastMessage.collectAsState()

                // Toast Feedback
                LaunchedEffect(toastMessage) {
                    toastMessage?.let {
                        Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show()
                        viewModel.clearToast()
                    }
                }

                if (!isUnlocked && isPinEnabled) {
                    // Security PIN Lock Screen
                    PinLockScreen(
                        onPinEntered = { pin -> viewModel.unlockApp(pin) },
                        onSuccess = {}
                    )
                } else {
                    SuDhanMainApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun SuDhanMainApp(viewModel: MainViewModel) {
    val language by viewModel.appLanguage.collectAsState()
    val isPinEnabled by viewModel.isPinLockEnabled.collectAsState()
    val reminders by viewModel.reminders.collectAsState()

    var currentView by remember { mutableStateOf(AppView.MAIN_TABS) }
    var selectedTab by remember { mutableStateOf(BottomNavTab.HOME) }
    var addScreenInitialTab by remember { mutableIntStateOf(0) }
    var addScreenPreselectedLoanId by remember { mutableStateOf<Long?>(null) }

    var showAddChoiceSheet by remember { mutableStateOf(false) }
    var showRemindersSheet by remember { mutableStateOf(false) }

    // Handle back button when inside subviews (Calculator / Transactions)
    BackHandler(enabled = currentView != AppView.MAIN_TABS || selectedTab != BottomNavTab.HOME) {
        if (currentView != AppView.MAIN_TABS) {
            currentView = AppView.MAIN_TABS
        } else if (selectedTab != BottomNavTab.HOME) {
            selectedTab = BottomNavTab.HOME
        }
    }

    Scaffold(
        topBar = {
            SuDhanTopBar(
                language = language,
                reminderCount = reminders.size,
                isPinEnabled = isPinEnabled,
                onSearchClick = {
                    currentView = AppView.TRANSACTIONS
                },
                onNotificationsClick = {
                    showRemindersSheet = true
                },
                onLanguageToggle = {
                    viewModel.setLanguage(if (language == AppLanguage.ENGLISH) AppLanguage.BENGALI else AppLanguage.ENGLISH)
                },
                onCalculatorClick = {
                    currentView = if (currentView == AppView.CALCULATOR) AppView.MAIN_TABS else AppView.CALCULATOR
                },
                onTransactionsClick = {
                    currentView = if (currentView == AppView.TRANSACTIONS) AppView.MAIN_TABS else AppView.TRANSACTIONS
                },
                onLockClick = {
                    viewModel.lockApp()
                }
            )
        },
        bottomBar = {
            SuDhanBottomNav(
                selectedTab = selectedTab,
                language = language,
                onTabSelected = { tab ->
                    currentView = AppView.MAIN_TABS
                    if (tab == BottomNavTab.ADD) {
                        showAddChoiceSheet = true
                    } else {
                        selectedTab = tab
                    }
                },
                onAddClick = {
                    showAddChoiceSheet = true
                }
            )
        },
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentView) {
                AppView.CALCULATOR -> {
                    CalculatorScreen()
                }
                AppView.TRANSACTIONS -> {
                    TransactionsScreen(viewModel = viewModel)
                }
                AppView.MAIN_TABS -> {
                    when (selectedTab) {
                        BottomNavTab.HOME -> {
                            DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToMoneyLent = {
                                    addScreenInitialTab = 0
                                    addScreenPreselectedLoanId = null
                                    selectedTab = BottomNavTab.ADD
                                },
                                onNavigateToMoneyBorrowed = {
                                    addScreenInitialTab = 1
                                    addScreenPreselectedLoanId = null
                                    selectedTab = BottomNavTab.ADD
                                },
                                onNavigateToRecordPayment = { loanId ->
                                    addScreenInitialTab = 2
                                    addScreenPreselectedLoanId = loanId
                                    selectedTab = BottomNavTab.ADD
                                },
                                onNavigateToCustomers = {
                                    selectedTab = BottomNavTab.CUSTOMERS
                                },
                                onNavigateToCalculator = {
                                    currentView = AppView.CALCULATOR
                                },
                                onNavigateToTransactions = {
                                    currentView = AppView.TRANSACTIONS
                                },
                                onLoanClick = { loanSummary ->
                                    addScreenInitialTab = 2
                                    addScreenPreselectedLoanId = loanSummary.loan.id
                                    selectedTab = BottomNavTab.ADD
                                }
                            )
                        }
                        BottomNavTab.CUSTOMERS -> {
                            CustomersScreen(
                                viewModel = viewModel,
                                onNavigateToRecordPayment = { loanId ->
                                    addScreenInitialTab = 2
                                    addScreenPreselectedLoanId = loanId
                                    selectedTab = BottomNavTab.ADD
                                }
                            )
                        }
                        BottomNavTab.ADD -> {
                            AddTransactionScreen(
                                viewModel = viewModel,
                                initialTab = addScreenInitialTab,
                                preselectedLoanId = addScreenPreselectedLoanId,
                                onTransactionComplete = {
                                    selectedTab = BottomNavTab.HOME
                                }
                            )
                        }
                        BottomNavTab.REPORTS -> {
                            ReportsScreen(viewModel = viewModel)
                        }
                        BottomNavTab.SETTINGS -> {
                            SettingsScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet: Add Transaction Choice
    if (showAddChoiceSheet) {
        AddTransactionChoiceSheet(
            language = language,
            onDismiss = { showAddChoiceSheet = false },
            onSelectMoneyLent = {
                addScreenInitialTab = 0
                addScreenPreselectedLoanId = null
                selectedTab = BottomNavTab.ADD
                currentView = AppView.MAIN_TABS
            },
            onSelectMoneyBorrowed = {
                addScreenInitialTab = 1
                addScreenPreselectedLoanId = null
                selectedTab = BottomNavTab.ADD
                currentView = AppView.MAIN_TABS
            },
            onSelectRecordPayment = {
                addScreenInitialTab = 2
                addScreenPreselectedLoanId = null
                selectedTab = BottomNavTab.ADD
                currentView = AppView.MAIN_TABS
            }
        )
    }

    // Reminders & Overdue Notifications Sheet
    if (showRemindersSheet) {
        RemindersBottomSheet(
            reminders = reminders,
            language = language,
            onDismiss = { showRemindersSheet = false },
            onRecordPaymentClick = { loanId ->
                addScreenInitialTab = 2
                addScreenPreselectedLoanId = loanId
                selectedTab = BottomNavTab.ADD
                currentView = AppView.MAIN_TABS
            }
        )
    }
}
