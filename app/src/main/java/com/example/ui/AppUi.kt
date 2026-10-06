package com.example.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.example.data.Client
import com.example.data.Debt
import com.example.data.Payment
import com.example.data.Product
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.viewmodel.DebtorSummary
import com.example.viewmodel.DayFinanceSummary
import com.example.viewmodel.FiadoViewModel
import com.example.viewmodel.Screen
import com.example.viewmodel.SelectedDayStatus
import java.util.*

@Composable
fun MainTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Voltar",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            Spacer(modifier = Modifier.width(16.dp))
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (actions != null) {
            Row(content = actions)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainScreen(viewModel: FiadoViewModel) {
    val currentScreen = viewModel.currentScreen
    val context = LocalContext.current

    // Set up back handler to pop custom screen stack
    BackHandler(enabled = viewModel.screenStack.size > 1) {
        viewModel.navigateBack()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Keep bottom nav bar fixed at the bottom for all screens, as requested
            BottomNavBar(
                currentScreen = currentScreen,
                onTabSelected = { tab ->
                    viewModel.navigateTo(tab)
                },
                viewModel = viewModel
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(spring()) togetherWith fadeOut(spring())
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    is Screen.Home -> HomeScreen(viewModel = viewModel)
                    is Screen.Clients -> ClientsListScreen(viewModel = viewModel)
                    is Screen.Calendar -> CalendarScreen(viewModel = viewModel)
                    is Screen.Reports -> ReportsScreen(viewModel = viewModel)
                    is Screen.Products -> ProductsScreen(viewModel = viewModel)
                    is Screen.Account -> AccountSettingsScreen(viewModel = viewModel)
                    is Screen.ClientDetails -> ClientProfileScreen(clientId = screen.clientId, viewModel = viewModel)
                    is Screen.AddClient -> AddClientScreen(viewModel = viewModel)
                    is Screen.AddDebt -> AddDebtScreen(clientId = screen.clientId, viewModel = viewModel)
                    is Screen.SyncSettings -> SyncSettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun BottomNavBar(
    currentScreen: Screen,
    onTabSelected: (Screen) -> Unit,
    viewModel: FiadoViewModel
) {
    val activeTab = when (currentScreen) {
        is Screen.Home -> Screen.Home
        is Screen.Clients, is Screen.AddClient, is Screen.ClientDetails, is Screen.AddDebt -> Screen.Clients
        is Screen.Calendar -> Screen.Calendar
        is Screen.Products -> Screen.Products
        is Screen.Account, is Screen.SyncSettings, is Screen.Reports -> Screen.Account
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bottom_nav_bar"),
        color = PureWhite,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        NavigationBar(
            containerColor = PureWhite,
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 1. Início
            NavigationBarItem(
                selected = activeTab == Screen.Home,
                onClick = { onTabSelected(Screen.Home) },
                icon = { Icon(Icons.Default.Home, contentDescription = "Início") },
                label = { Text("Início", fontSize = 11.sp, fontWeight = if (activeTab == Screen.Home) FontWeight.Bold else FontWeight.Medium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PureWhite,
                    selectedTextColor = PrimaryBlueDark,
                    indicatorColor = PrimaryBlueMain,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted
                ),
                modifier = Modifier.testTag("nav_tab_home")
            )

            // 2. Clientes
            NavigationBarItem(
                selected = activeTab == Screen.Clients,
                onClick = { onTabSelected(Screen.Clients) },
                icon = { Icon(Icons.Default.People, contentDescription = "Clientes") },
                label = { Text("Clientes", fontSize = 11.sp, fontWeight = if (activeTab == Screen.Clients) FontWeight.Bold else FontWeight.Medium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PureWhite,
                    selectedTextColor = PrimaryBlueDark,
                    indicatorColor = PrimaryBlueMain,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted
                ),
                modifier = Modifier.testTag("nav_tab_clients")
            )

            // 3. Calendário
            NavigationBarItem(
                selected = activeTab == Screen.Calendar,
                onClick = { onTabSelected(Screen.Calendar) },
                icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Calendário") },
                label = { Text("Calendário", fontSize = 11.sp, fontWeight = if (activeTab == Screen.Calendar) FontWeight.Bold else FontWeight.Medium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PureWhite,
                    selectedTextColor = PrimaryBlueDark,
                    indicatorColor = PrimaryBlueMain,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted
                ),
                modifier = Modifier.testTag("nav_tab_calendar")
            )

            // 4. Produtos
            NavigationBarItem(
                selected = activeTab == Screen.Products,
                onClick = { onTabSelected(Screen.Products) },
                icon = { Icon(Icons.Default.Inventory2, contentDescription = "Produtos") },
                label = { Text("Produtos", fontSize = 11.sp, fontWeight = if (activeTab == Screen.Products) FontWeight.Bold else FontWeight.Medium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PureWhite,
                    selectedTextColor = PrimaryBlueDark,
                    indicatorColor = PrimaryBlueMain,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted
                ),
                modifier = Modifier.testTag("nav_tab_products")
            )

            // 5. Conta
            NavigationBarItem(
                selected = activeTab == Screen.Account,
                onClick = { onTabSelected(Screen.Account) },
                icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Conta") },
                label = { Text("Conta", fontSize = 11.sp, fontWeight = if (activeTab == Screen.Account) FontWeight.Bold else FontWeight.Medium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PureWhite,
                    selectedTextColor = PrimaryBlueDark,
                    indicatorColor = PrimaryBlueMain,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted
                ),
                modifier = Modifier.testTag("nav_tab_account")
            )
        }
    }
}

@Composable
fun HomeScreen(viewModel: FiadoViewModel) {
    val clients by viewModel.clientsFlow.collectAsStateWithLifecycle()
    val debts by viewModel.debtsFlow.collectAsStateWithLifecycle()
    val payments by viewModel.paymentsFlow.collectAsStateWithLifecycle()

    val debtors = remember(clients, debts) {
        viewModel.getDebtorsSortedByDelay(clients, debts)
    }

    val totalReceived = remember(payments) { viewModel.calculateTotalReceivedHistory(payments) }
    val totalPending = remember(debts) { viewModel.calculateTotalAReceber(debts) }
    val totalGanhoCurrentMonth = remember(payments) { viewModel.calculateTotalGanhoCurrentMonth(payments) }
    val todayReceived = remember(payments) { viewModel.calculateTodayReceived(payments) }
    val monthGrowth = remember(payments) { viewModel.calculateMonthGrowth(payments) }

    var selectedDayModal by remember { mutableStateOf<DayFinanceSummary?>(null) }
    var valueDetailType by remember { mutableStateOf<String?>(null) } // "ganho", "a_receber", "recebidos"
    var showAllDebtorsDialog by remember { mutableStateOf(false) }
    var showDayDetails by remember { mutableStateOf<SelectedDayStatus>(SelectedDayStatus.None) }

    // Dialog states
    var isNotificationOpen by remember { mutableStateOf(false) }
    var isRegisterPaymentOpen by remember { mutableStateOf(false) }
    var isRegisterPendingOpen by remember { mutableStateOf(false) }
    var isRegisterSaleOpen by remember { mutableStateOf(false) }
    var isNewClientOpen by remember { mutableStateOf(false) }
    var isPendingClientsOpen by remember { mutableStateOf(false) }
    
    // Edit debt state
    var editDebtTarget by remember { mutableStateOf<Debt?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isDark = when (viewModel.themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .testTag("home_screen_scroll"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // 1. CABEÇALHO
        item {
            FinancialTopHeader(
                viewModel = viewModel,
                overdueCount = debtors.size,
                onNotificationsClick = { isNotificationOpen = true },
                onSettingsClick = { viewModel.navigateTo(Screen.Account) }
            )
        }

        // 2. CARD PRINCIPAL (TOTAL RECEBIDO + 3 BOTÕES: TOTAL GANHO | A RECEBER | RECEBIDOS)
        item {
            MainReceivedFinanceCard(
                totalReceived = totalReceived,
                totalEarned = totalGanhoCurrentMonth,
                totalPending = totalPending,
                todayReceived = todayReceived,
                monthGrowth = monthGrowth,
                payments = payments,
                onTotalGanhoClick = { valueDetailType = "ganho" },
                onValoresAReceberClick = { valueDetailType = "a_receber" },
                onValoresRecebidosClick = { valueDetailType = "recebidos" },
                viewModel = viewModel
            )
        }

        // 3. CLIENTES DEVEDORES (Ver todos)
        item {
            DebtorsSection(
                debtors = debtors,
                onViewAllClick = { showAllDebtorsDialog = true },
                onClientClick = { clientId -> viewModel.navigateTo(Screen.ClientDetails(clientId)) },
                viewModel = viewModel
            )
        }

        // 4. CALENDÁRIO FINANCEIRO
        item {
            FinancialCalendarSection(
                viewModel = viewModel,
                clients = clients,
                debts = debts,
                payments = payments,
                onDayClick = { daySummary -> selectedDayModal = daySummary }
            )
        }
    }

    // -------------------------------------------------------------------------
    // DIALOGS & OVERLAYS
    // -------------------------------------------------------------------------

    selectedDayModal?.let { summary ->
        DayDetailModalSheet(
            summary = summary,
            onDismiss = { selectedDayModal = null },
            viewModel = viewModel,
            onAddPaymentClick = {
                selectedDayModal = null
                isRegisterPaymentOpen = true
            },
            onAddDebtClick = {
                selectedDayModal = null
                isRegisterPendingOpen = true
            }
        )
    }

    valueDetailType?.let { type ->
        ValueDetailDialog(
            type = type,
            onDismiss = { valueDetailType = null },
            debts = debts,
            payments = payments,
            clients = clients,
            viewModel = viewModel
        )
    }

    if (showAllDebtorsDialog) {
        AllDebtorsDialog(
            debtors = debtors,
            onDismiss = { showAllDebtorsDialog = false },
            onClientClick = { clientId ->
                showAllDebtorsDialog = false
                viewModel.navigateTo(Screen.ClientDetails(clientId))
            },
            viewModel = viewModel
        )
    }

    // Clientes Pendentes Dialog
    if (isPendingClientsOpen) {
        val pendingClientsList = clients.map { client ->
            val clientDebts = debts.filter { it.clientId == client.id && it.status != "Pago" }
            val amount = clientDebts.sumOf { it.value - it.paidValue }
            Triple(client, amount, clientDebts.size)
        }.filter { it.second > 0 }.sortedByDescending { it.second }

        Dialog(onDismissRequest = { isPendingClientsOpen = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("pending_clients_dialog"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF101B2B) else Color.White
                ),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF1D3557) else Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Clientes Devedores",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF0F4C81)
                            )
                            Text(
                                text = "${pendingClientsList.size} com débitos ativos",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF8C8C8C)
                            )
                        }
                        IconButton(onClick = { isPendingClientsOpen = false }) {
                            Icon(
                                imageVector = Icons.Default.Close, 
                                contentDescription = "Fechar", 
                                tint = if (isDark) Color.White else Color(0xFF4A6572)
                            )
                        }
                    }

                    if (pendingClientsList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Ninguém com parcelas em atraso!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isDark) Color.White else Color(0xFF101B2B),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 380.dp)
                                .verticalScroll(scrollState),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pendingClientsList.forEach { (client, balance, count) ->
                                val initials = client.name.split(" ")
                                    .filter { it.isNotBlank() }
                                    .map { it.first().uppercase() }
                                    .take(2)
                                    .joinToString("")

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (isDark) Color(0xFF1D2C3F) else Color(0xFFFFF1F0),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (isDark) Color(0xFF2E4B6E) else Color(0xFFFFCCC7),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            isPendingClientsOpen = false
                                            viewModel.navigateTo(Screen.ClientDetails(client.id))
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(if (isDark) Color(0xFF3B1D1D) else Color(0xFFFFD8D6)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = initials.ifBlank { "?" },
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Black,
                                                color = if (isDark) Color(0xFFEF5350) else Color(0xFFE53935)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column {
                                            Text(
                                                text = client.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (isDark) Color.White else Color(0xFF1D1B20),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "$count débito(s) em aberto",
                                                fontSize = 11.sp,
                                                color = if (isDark) Color(0xFFB0B0B0) else Color(0xFF5D5D5D)
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = viewModel.formatCurrency(balance),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = if (isDark) Color(0xFFEF5350) else Color(0xFFE53935)
                                        )
                                        Text(
                                            text = "Ver detalhes",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFF80BFFF) else Color(0xFF0F4C81),
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = { isPendingClientsOpen = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFF1D3557) else Color(0xFF0F4C81),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Fechar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Notificações Dialog
    if (isNotificationOpen) {
        Dialog(onDismissRequest = { isNotificationOpen = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Notificações & Alertas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F4C81)
                        )
                        IconButton(onClick = { isNotificationOpen = false }) {
                            Icon(Icons.Default.Close, "Fechar", tint = Color(0xFF4A6572))
                        }
                    }

                    // Alerts calculation
                    val nowLocal = System.currentTimeMillis()
                    val overdueList14 = clients.filter { client ->
                        val clientDebts = debts.filter { it.clientId == client.id && it.status != "Pago" }
                        val oldest = clientDebts.minByOrNull { it.createdAt }
                        if (oldest != null) {
                            val age = ((nowLocal - oldest.createdAt) / (24 * 3600 * 1000L)).toInt()
                            age >= 14
                        } else false
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        NotificationItem(
                            icon = Icons.Default.Info,
                            color = Color(0xFFF44336),
                            title = "Clientes em Atraso",
                            description = if (overdueList14.isNotEmpty()) {
                                "Há ${overdueList14.size} clientes com débitos pendentes em atraso há mais de 14 dias!"
                            } else {
                                "Nenhum cliente com atraso superior a 2 semanas. Excelente!"
                            }
                        )

                        NotificationItem(
                            icon = Icons.Default.ArrowDownward,
                            color = Color(0xFF4CAF50),
                            title = "Novos Recebimentos",
                            description = "Você recebeu total de ${viewModel.formatCurrency(totalReceived)} registrados historicamente no sistema."
                        )

                        NotificationItem(
                            icon = Icons.Default.ShoppingCart,
                            color = Color(0xFF9C27B0),
                            title = "Vendas Registradas",
                            description = "Vendas e serviços estão organizados no calendário e relatórios por dia."
                        )

                        NotificationItem(
                            icon = Icons.Default.Share,
                            color = Color(0xFF2196F3),
                            title = "Lembretes de Cobrança",
                            description = "Dica: Toque em um cliente nas seções de atraso para compartilhar um relatório direto via WhatsApp."
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { isNotificationOpen = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F4C81))
                    ) {
                        Text("Entendido", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Registrar Recebimento Dialog
    if (isRegisterPaymentOpen) {
        var selectedClientId by remember { mutableStateOf<Int?>(null) }
        var valueStr by remember { mutableStateOf("") }
        var obsStr by remember { mutableStateOf("") }
        var selectedPaymentMethod by remember { mutableStateOf("PIX") }
        val paymentOptionList = listOf("PIX", "Dinheiro", "Crédito", "Débito", "C. Bancária", "Boleto", "Outro")

        Dialog(onDismissRequest = { isRegisterPaymentOpen = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Registrar Recebimento",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F4C81)
                        )
                        IconButton(onClick = { isRegisterPaymentOpen = false }) {
                            Icon(Icons.Default.Close, "Fechar", tint = Color(0xFF4A6572))
                        }
                    }

                    // Client Selector View
                    InteractiveClientSelector(
                        clients = clients,
                        selectedClientId = selectedClientId,
                        onClientSelected = { selectedClientId = if (it == -1) null else it }
                    )

                    if (selectedClientId != null) {
                        val clientPending = debts.filter { it.clientId == selectedClientId && it.status != "Pago" }.sumOf { it.value - it.paidValue }
                        
                        Text(
                            text = "Valor Pendente do Cliente: ${viewModel.formatCurrency(clientPending)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF44336)
                        )

                        OutlinedTextField(
                            value = valueStr,
                            onValueChange = { valueStr = it },
                            label = { Text("Valor Recebido (R$)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF0F4C81))
                        )

                        Text(
                            text = "Forma de Pagamento",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F4C81)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            paymentOptionList.forEach { option ->
                                val isSelected = selectedPaymentMethod == option
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedPaymentMethod = option },
                                    label = { Text(option) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF0F4C81),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        OutlinedTextField(
                            value = obsStr,
                            onValueChange = { obsStr = it },
                            label = { Text("Observações (opcional)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF0F4C81))
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                val valueDbl = valueStr.toDoubleOrNull() ?: 0.0
                                if (valueDbl <= 0.0) {
                                    Toast.makeText(context, "Digite um valor válido!", Toast.LENGTH_SHORT).show()
                                } else {
                                    val fullNotes = "Método: $selectedPaymentMethod" + if (obsStr.isNotBlank()) " - $obsStr" else ""
                                    viewModel.registerGenericClientPayment(selectedClientId!!, valueDbl, fullNotes)
                                    Toast.makeText(context, "Recebimento registrado com sucesso!", Toast.LENGTH_SHORT).show()
                                    isRegisterPaymentOpen = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Confirmar Recebimento", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Registrar Valor a Receber Dialog
    if (isRegisterPendingOpen) {
        var selectedClientId by remember { mutableStateOf<Int?>(null) }
        var typeSelected by remember { mutableStateOf("Serviço") } // "Produto" or "Serviço"
        var valueStr by remember { mutableStateOf("") }
        var descriptionStr by remember { mutableStateOf("") }
        var obsStr by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { isRegisterPendingOpen = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Registrar Valor a Receber",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F4C81)
                        )
                        IconButton(onClick = { isRegisterPendingOpen = false }) {
                            Icon(Icons.Default.Close, "Fechar", tint = Color(0xFF4A6572))
                        }
                    }

                    // Client Selector View
                    InteractiveClientSelector(
                        clients = clients,
                        selectedClientId = selectedClientId,
                        onClientSelected = { selectedClientId = if (it == -1) null else it }
                    )

                    if (selectedClientId != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF0F4F8), RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("Serviço", "Produto").forEach { op ->
                                val isSel = typeSelected == op
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSel) Color(0xFF0F4C81) else Color.Transparent)
                                        .clickable { typeSelected = op },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = op,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) Color.White else Color(0xFF0F4C81),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = descriptionStr,
                            onValueChange = { descriptionStr = it },
                            label = { Text("Nome do produto ou serviço") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF0F4C81))
                        )

                        OutlinedTextField(
                            value = valueStr,
                            onValueChange = { valueStr = it },
                            label = { Text("Valor (R$)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF0F4C81))
                        )

                        OutlinedTextField(
                            value = obsStr,
                            onValueChange = { obsStr = it },
                            label = { Text("Observações (opcional)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF0F4C81))
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                val valueDbl = valueStr.toDoubleOrNull() ?: 0.0
                                if (valueDbl <= 0.0 || descriptionStr.isBlank()) {
                                    Toast.makeText(context, "Nome e Valor válidos são necessários!", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.addDebtToClient(
                                        clientId = selectedClientId!!,
                                        type = typeSelected,
                                        description = descriptionStr,
                                        value = valueDbl,
                                        notes = obsStr
                                    )
                                    Toast.makeText(context, "Valor pendente registrado com sucesso!", Toast.LENGTH_SHORT).show()
                                    isRegisterPendingOpen = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Salvar Pendência", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Registrar Venda Dialog
    if (isRegisterSaleOpen) {
        var selectedClientId by remember { mutableStateOf<Int?>(null) }
        var descriptionStr by remember { mutableStateOf("") }
        var qtyStr by remember { mutableStateOf("1") }
        var valueUnitStr by remember { mutableStateOf("") }
        var obsStr by remember { mutableStateOf("") }
        var clientHasPaid by remember { mutableStateOf(false) }

        val qtyInt = qtyStr.toIntOrNull() ?: 1
        val valUnitDbl = valueUnitStr.toDoubleOrNull() ?: 0.0
        val totalVal = qtyInt * valUnitDbl

        Dialog(onDismissRequest = { isRegisterSaleOpen = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Registrar Venda",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F4C81)
                        )
                        IconButton(onClick = { isRegisterSaleOpen = false }) {
                            Icon(Icons.Default.Close, "Fechar", tint = Color(0xFF4A6572))
                        }
                    }

                    // Client Selector View
                    InteractiveClientSelector(
                        clients = clients,
                        selectedClientId = selectedClientId,
                        onClientSelected = { selectedClientId = if (it == -1) null else it }
                    )

                    if (selectedClientId != null) {
                        OutlinedTextField(
                            value = descriptionStr,
                            onValueChange = { descriptionStr = it },
                            label = { Text("Nome do produto ou venda") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF0F4C81))
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = qtyStr,
                                onValueChange = { qtyStr = it },
                                label = { Text("Qtd") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF0F4C81))
                            )
                            OutlinedTextField(
                                value = valueUnitStr,
                                onValueChange = { valueUnitStr = it },
                                label = { Text("Vlr Unitário (R$)") },
                                modifier = Modifier.weight(2f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF0F4C81))
                            )
                        }

                        // Total Calculation Value box
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF1F8FF), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Valor Total", fontWeight = FontWeight.Bold, color = Color(0xFF0F4C81))
                            Text(
                                text = viewModel.formatCurrency(totalVal),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F4C81)
                            )
                        }

                        // Question: Cliente pagou ?
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "O Cliente já efetuou o pagamento?",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF101B2B)
                            )
                            Switch(
                                checked = clientHasPaid,
                                onCheckedChange = { clientHasPaid = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF0F4C81))
                            )
                        }

                        OutlinedTextField(
                            value = obsStr,
                            onValueChange = { obsStr = it },
                            label = { Text("Observações (opcional)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF0F4C81))
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (totalVal <= 0.0 || descriptionStr.isBlank()) {
                                    Toast.makeText(context, "Nome, Qtd e Valor unitário válidos!", Toast.LENGTH_SHORT).show()
                                } else {
                                    // Save Client purchase
                                    viewModel.addDebtToClient(
                                        clientId = selectedClientId!!,
                                        type = "Produto",
                                        description = descriptionStr,
                                        value = totalVal,
                                        notes = obsStr
                                    )
                                    if (clientHasPaid) {
                                        viewModel.registerGenericClientPayment(
                                            selectedClientId!!,
                                            totalVal,
                                            "Compra de $descriptionStr paga no ato"
                                        )
                                    }
                                    Toast.makeText(context, "Venda registrada com sucesso!", Toast.LENGTH_SHORT).show()
                                    isRegisterSaleOpen = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Concluir Venda", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Novo Cliente Dialog
    if (isNewClientOpen) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var whatsapp by remember { mutableStateOf("") }
        var isWhatsappSame by remember { mutableStateOf(false) }
        var address by remember { mutableStateOf("") }
        var obsStr by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { isNewClientOpen = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Novo Cliente",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F4C81)
                        )
                        IconButton(onClick = { isNewClientOpen = false }) {
                            Icon(Icons.Default.Close, "Fechar", tint = Color(0xFF4A6572))
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome Completo") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF0F4C81))
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Telefone") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF0F4C81))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("WhatsApp é o mesmo do telefone?", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4A6572))
                        Switch(
                            checked = isWhatsappSame,
                            onCheckedChange = {
                                isWhatsappSame = it
                                if (it) whatsapp = phone
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF0F4C81))
                        )
                    }

                    if (!isWhatsappSame) {
                        OutlinedTextField(
                            value = whatsapp,
                            onValueChange = { whatsapp = it },
                            label = { Text("WhatsApp") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF0F4C81))
                        )
                    }

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Endereço Completo") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF0F4C81))
                    )

                    OutlinedTextField(
                        value = obsStr,
                        onValueChange = { obsStr = it },
                        label = { Text("Observações") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF0F4C81))
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                Toast.makeText(context, "Nome completo é obrigatório!", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.registerClient(
                                    name = name,
                                    phone = phone,
                                    whatsapp = if (isWhatsappSame) phone else whatsapp,
                                    email = null,
                                    address = address,
                                    notes = obsStr
                                )
                                Toast.makeText(context, "Cliente cadastrado com sucesso!", Toast.LENGTH_SHORT).show()
                                isNewClientOpen = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F4C81)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Salvar Cliente", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    // Direct Edit Debt register Dialog
    if (editDebtTarget != null) {
        val debtToEdit = editDebtTarget!!
        var editDesc by remember { mutableStateOf(debtToEdit.description) }
        var editType by remember { mutableStateOf(debtToEdit.type) } // "Produto" or "Serviço"
        var editValueStr by remember { mutableStateOf(debtToEdit.value.toString()) }
        var editStatus by remember { mutableStateOf(debtToEdit.status) } // "Pendente", "Parcial", "Pago"
        var editNotes by remember { mutableStateOf(debtToEdit.notes ?: "") }

        Dialog(onDismissRequest = { editDebtTarget = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Editar Registro de Débito",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F4C81)
                        )
                        IconButton(onClick = { editDebtTarget = null }) {
                            Icon(Icons.Default.Close, "Fechar", tint = Color(0xFF4A6572))
                        }
                    }

                    OutlinedTextField(
                        value = editDesc,
                        onValueChange = { editDesc = it },
                        label = { Text("Descrição / Nome") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF0F4C81))
                    )

                    // Type selection (Row)
                    Text("Tipo", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF0F4C81))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF0F4F8), RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Serviço", "Produto").forEach { op ->
                            val isSel = editType == op
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) Color(0xFF0F4C81) else Color.Transparent)
                                    .clickable { editType = op },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = op,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else Color(0xFF0F4C81),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editValueStr,
                        onValueChange = { editValueStr = it },
                        label = { Text("Valor Total (R$)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF0F4C81))
                    )

                    // Status selection (Row)
                    Text("Status", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF0F4C81))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF0F4F8), RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Pendente", "Parcial", "Pago").forEach { st ->
                            val isSel = editStatus == st
                            val stColor = when (st) {
                                "Pago" -> Color(0xFF4CAF50)
                                "Parcial" -> Color(0xFFFF9800)
                                else -> Color(0xFFF44336)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) stColor else Color.Transparent)
                                    .clickable { editStatus = st },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = st,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else Color(0xFF0F4C81),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text("Observações") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF0F4C81))
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { editDebtTarget = null },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancelar")
                        }
                        Button(
                            onClick = {
                                val valDbl = editValueStr.toDoubleOrNull() ?: debtToEdit.value
                                scope.launch {
                                    val updatedDebt = debtToEdit.copy(
                                        description = editDesc,
                                        type = editType,
                                        value = valDbl,
                                        status = editStatus,
                                        notes = if (editNotes.isBlank()) null else editNotes
                                    )
                                    viewModel.repository.updateDebt(updatedDebt)
                                    Toast.makeText(context, "Registro atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                                    showDayDetails = SelectedDayStatus.None
                                    editDebtTarget = null
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F4C81)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Salvar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeHeader(
    viewModel: FiadoViewModel,
    clients: List<Client>,
    debts: List<Debt>,
    payments: List<Payment>
) {
    val context = LocalContext.current
    val isDark = when (viewModel.themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    val monthNames = when (viewModel.languageCode) {
        "en" -> listOf("", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        "es" -> listOf("", "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")
        else -> listOf("", "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro")
    }
    val monthTitle = "${monthNames[viewModel.calendarMonth]} ${viewModel.calendarYear}"

    val initials = viewModel.loggedInName.split(" ")
        .filter { it.isNotBlank() }
        .map { it.first().uppercase() }
        .take(2)
        .joinToString("")

    // Calculations
    val totalPendingAmount = debts.filter { it.status != "Pago" }.sumOf { it.value - it.paidValue }
    val totalReceived = payments.sumOf { it.value }
    val pendingCount = debts.count { it.status != "Pago" }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = viewModel.t("app_title").uppercase(),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = monthTitle,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            
            // Avatar & Share row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "Relatório Geral - Total Pendente: ${viewModel.formatCurrency(totalPendingAmount)}")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Compartilhar"))
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (isDark) Color(0xFF1E2A38) else Color(0xFFE3F2FD),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (isDark) Color(0xFF0F4C81) else Color(0xFFD1E8FF),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFD1E8FF) else Color(0xFF0B2545)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Stats Summary Grid (3 Columns)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Stat 1: Total Devido
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (isDark) Color(0xFF3B2F2F) else Color(0xFFFFECE9),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isDark) Color(0xFF8C1D18) else Color(0xFFF2B8B5),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Total Devido",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFF2B8B5) else Color(0xFFB3261E),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = viewModel.formatCurrency(totalPendingAmount),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color(0xFFFFB4AB) else Color(0xFFB3261E),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Stat 2: Recebido
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (isDark) Color(0xFF223E28) else Color(0xFFEFFAEF),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isDark) Color(0xFF81C784) else Color(0xFFA5D6A7),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Recebido",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = viewModel.formatCurrency(totalReceived),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color(0xFFA5D6A7) else Color(0xFF2E7D32),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Stat 3: Pendentes
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (isDark) Color(0xFF132332) else Color(0xFFEAF4FC),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isDark) Color(0xFF90CAF9).copy(alpha = 0.5f) else Color(0xFF90CAF9),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Pendentes",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF90CAF9) else Color(0xFF0F4C81),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = pendingCount.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color(0xFFD1E8FF) else Color(0xFF101B2B)
                    )
                }
            }
        }
    }
}

@Composable
fun CalendarWidget(
    viewModel: FiadoViewModel,
    debts: List<Debt>,
    clients: List<Client>,
    onDaySelected: (SelectedDayStatus) -> Unit
) {
    val monthNames = when (viewModel.languageCode) {
        "en" -> listOf("", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        "es" -> listOf("", "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")
        else -> listOf("", "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro")
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = {
            if (viewModel.calendarMonth == 1) {
                viewModel.calendarMonth = 12
                viewModel.calendarYear -= 1
            } else {
                viewModel.calendarMonth -= 1
            }
        }) {
            Icon(Icons.Default.KeyboardArrowLeft ?: Icons.Default.Refresh, "Mês Anterior")
        }

        Text(
            text = "${monthNames[viewModel.calendarMonth]} ${viewModel.calendarYear}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("calendar_month_title")
        )

        IconButton(onClick = {
            if (viewModel.calendarMonth == 12) {
                viewModel.calendarMonth = 1
                viewModel.calendarYear += 1
            } else {
                viewModel.calendarMonth += 1
            }
        }) {
            Icon(Icons.Default.KeyboardArrowRight ?: Icons.Default.Refresh, "Próximo Mês")
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Days of week row
    val daysOfWeek = when (viewModel.languageCode) {
        "en" -> listOf("S", "M", "T", "W", "T", "F", "S")
        "es" -> listOf("D", "L", "M", "M", "J", "V", "S")
        else -> listOf("D", "S", "T", "Q", "Q", "S", "S")
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
        daysOfWeek.forEach { day ->
            Text(
                text = day,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.width(36.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Simple calendar math generator
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.YEAR, viewModel.calendarYear)
    calendar.set(Calendar.MONTH, viewModel.calendarMonth - 1)
    calendar.set(Calendar.DAY_OF_MONTH, 1)

    val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val startDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sunday, 1 = Monday...

    val totalSlots = 42 // 6 weeks Matrix
    val weeks = mutableListOf<List<Int?>>()
    var currentWeek = mutableListOf<Int?>()

    for (i in 0 until startDayOfWeek) {
        currentWeek.add(null)
    }

    for (day in 1..maxDay) {
        if (currentWeek.size == 7) {
            weeks.add(currentWeek)
            currentWeek = mutableListOf()
        }
        currentWeek.add(day)
    }

    while (currentWeek.size < 7) {
        currentWeek.add(null)
    }
    weeks.add(currentWeek)

    // Match calendar days with active debts to show status indicators
    weeks.forEach { week ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            week.forEach { day ->
                if (day == null) {
                    Box(modifier = Modifier.size(36.dp))
                } else {
                    val isSelected = viewModel.selectedCalendarDay == day

                    // Check if this day has debts
                    val debtsOnDay = debts.filter {
                        val debtCal = Calendar.getInstance()
                        debtCal.timeInMillis = it.createdAt
                        debtCal.get(Calendar.YEAR) == viewModel.calendarYear &&
                                debtCal.get(Calendar.MONTH) == (viewModel.calendarMonth - 1) &&
                                debtCal.get(Calendar.DAY_OF_MONTH) == day
                    }

                    val dotColor = if (debtsOnDay.isNotEmpty()) {
                        val hasPending = debtsOnDay.any { it.status == "Pendente" }
                        val hasPartial = debtsOnDay.any { it.status == "Parcial" }
                        when {
                            hasPending -> Color(0xFFEF4444) // Red
                            hasPartial -> Color(0xFFF59E0B) // Amber
                            else -> Color(0xFF10B981)       // Green
                        }
                    } else {
                        null
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                            .clickable {
                                viewModel.selectedCalendarDay = day
                                if (debtsOnDay.isNotEmpty()) {
                                    val topDebt = debtsOnDay.first()
                                    val matchedClient = clients.firstOrNull { it.id == topDebt.clientId }
                                    onDaySelected(
                                        SelectedDayStatus.Exists(
                                            dateStr = String.format("%02d/%02d/%d", day, viewModel.calendarMonth, viewModel.calendarYear),
                                            clientName = matchedClient?.name ?: "Cliente Desconhecido",
                                            value = topDebt.value,
                                            type = topDebt.type,
                                            time = viewModel.formatDateWithTime(topDebt.createdAt).substringAfter("às "),
                                            status = topDebt.status
                                        )
                                    )
                                } else {
                                    onDaySelected(SelectedDayStatus.None)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = day.toString(),
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            if (dotColor != null) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color.White else dotColor)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OverdueListSection(
    title: String,
    minDays: Int,
    maxDays: Int,
    clients: List<Client>,
    debts: List<Debt>,
    payments: List<Payment>,
    viewModel: FiadoViewModel,
    testTag: String
) {
    val now = System.currentTimeMillis()
    val oneDayMillis = 24 * 60 * 60 * 1000L

    // For each client, calculate oldest pending debt, total due, etc.
    val items = clients.mapNotNull { client ->
        val clientDebts = debts.filter { it.clientId == client.id && it.status != "Pago" }
        if (clientDebts.isEmpty()) return@mapNotNull null

        val oldestDebt = clientDebts.minByOrNull { it.createdAt } ?: return@mapNotNull null
        val ageDays = ((now - oldestDebt.createdAt) / oneDayMillis).toInt()

        if (ageDays in minDays..maxDays) {
            val totalPendingAmount = clientDebts.sumOf { it.value - it.paidValue }
            val countPending = clientDebts.size
            
            Triple(client, totalPendingAmount, countPending)
        } else {
            null
        }
    }

    val isCritical = minDays >= 30
    val isDark = when (viewModel.themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF49454F),
                modifier = Modifier.weight(1f)
            )
            
            if (items.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(
                            if (isCritical) {
                                if (isDark) Color(0xFF8C1D18) else Color(0xFFF9DEDC)
                            } else {
                                if (isDark) Color(0xFF49454F) else Color(0xFFF3EFFF)
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    val countSuff = when (viewModel.languageCode) {
                        "en" -> if (items.size == 1) "Pending" else "Pending"
                        "es" -> if (items.size == 1) "Pendiente" else "Pendientes"
                        else -> if (items.size == 1) "Pendente" else "Pendentes"
                    }
                    Text(
                        text = "${items.size} $countSuff",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isCritical) {
                            if (isDark) Color(0xFFF2B8B5) else Color(0xFFB3261E)
                        } else {
                            if (isDark) Color(0xFF90CAF9) else Color(0xFF0F4C81)
                        }
                    )
                }
            }
        }

        if (items.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1E1E24) else Color(0xFFFDF7FF)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isDark) Color(0xFF33303C) else Color(0xFFCAC4D0).copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "All good",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = viewModel.t("no_overdue"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items.forEach { (client, balance, count) ->
                val clientInitials = client.name.split(" ")
                    .filter { it.isNotBlank() }
                    .map { it.first().uppercase() }
                    .take(2)
                    .joinToString("")

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            viewModel.navigateTo(Screen.ClientDetails(client.id))
                        },
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isCritical) {
                            if (isDark) Color(0xFF8C1D18) else Color(0xFFF2B8B5)
                        } else {
                            if (isDark) Color(0xFF33303C) else Color(0xFFE7E0EC)
                        }
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCritical) {
                            if (isDark) Color(0xFF372121) else Color(0xFFF9DEDC)
                        } else {
                            if (isDark) Color(0xFF1D1B20) else Color(0xFFFFFFFF)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Avatar Badge
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isCritical) {
                                            if (isDark) Color(0xFF8C1D18) else Color(0xFFFFFFFF)
                                        } else {
                                            if (isDark) Color(0xFF0F4C81) else Color(0xFFD1E8FF)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = clientInitials,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isCritical) {
                                        if (isDark) Color(0xFFF2B8B5) else Color(0xFFB3261E)
                                    } else {
                                        if (isDark) Color(0xFF90CAF9) else Color(0xFF0B2545)
                                    }
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column {
                                Text(
                                    text = client.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCritical) {
                                        if (isDark) Color(0xFFF2B8B5) else Color(0xFF1D1B20)
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                
                                val clientDebts = debts.filter { it.clientId == client.id && it.status != "Pago" }
                                val oldest = clientDebts.minByOrNull { it.createdAt }
                                val ageText = if (oldest != null) {
                                    val days = ((now - oldest.createdAt) / oneDayMillis).toInt()
                                    "${viewModel.formatDate(oldest.createdAt)} • ${viewModel.t("debt_time").format(days)}"
                                } else {
                                    "${count} débitos"
                                }
                                
                                Text(
                                    text = ageText,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = if (isCritical) {
                                        if (isDark) Color(0xFFE2B8B5) else Color(0xFF49454F)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = viewModel.formatCurrency(balance),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = if (isCritical) {
                                        if (isDark) Color(0xFFFFB4AB) else Color(0xFFB3261E)
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    }
                                )
                                if (!isCritical && count > 1) {
                                    Text(
                                        text = "${count} débitos",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            // Rounded small backing white circle with chevron "›"
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isCritical) {
                                            if (isDark) Color(0xFF4E3737) else Color(0xFFFFFFFF)
                                        } else {
                                            if (isDark) Color(0xFF2C2834) else Color(0xFFF7F2FA)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight ?: Icons.Default.PlayArrow,
                                    contentDescription = "Ver detalhes",
                                    tint = if (isCritical) {
                                        if (isDark) Color(0xFFF2B8B5) else Color(0xFFB3261E)
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClientsListScreen(viewModel: FiadoViewModel) {
    val clients by viewModel.clientsFlow.collectAsStateWithLifecycle()
    val debts by viewModel.debtsFlow.collectAsStateWithLifecycle()
    val searchPattern by viewModel.searchPattern.collectAsStateWithLifecycle()

    var showQuickAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("clients_screen")
    ) {
        // Upper Fixed Header Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = { viewModel.navigateTo(Screen.AddClient) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_create_client"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = viewModel.t("create_client"),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = viewModel.t("create_client"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Modern textfield standard styling
                OutlinedTextField(
                    value = searchPattern,
                    onValueChange = { viewModel.searchPattern.value = it },
                    placeholder = { Text(viewModel.t("search_hint"), fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, "Search") },
                    trailingIcon = {
                        if (searchPattern.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchPattern.value = "" }) {
                                Icon(Icons.Default.Close, "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_bar"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
            }
        }

        // List Section (in alphabet sequence)
        val filteredClients = remember(clients, debts, searchPattern) {
            clients.filter { client ->
                val matchesClient = client.name.contains(searchPattern, ignoreCase = true) ||
                        client.phone.contains(searchPattern)
                
                // Also search inside product descriptions or types
                val clientsDebts = debts.filter { it.clientId == client.id }
                val matchesDebts = clientsDebts.any {
                    it.description.contains(searchPattern, ignoreCase = true) ||
                            it.type.contains(searchPattern, ignoreCase = true)
                }

                matchesClient || matchesDebts
            }
        }

        if (filteredClients.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Empty list",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = viewModel.t("no_clients"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("clients_list"),
                contentPadding = PaddingValues(16.dp, bottom = 80.dp)
            ) {
                items(filteredClients) { client ->
                    val clientDebts = debts.filter { it.clientId == client.id && it.status != "Pago" }
                    val totalDue = clientDebts.sumOf { it.value - it.paidValue }
                    
                    ListItemClientCard(
                        client = client,
                        totalDue = totalDue,
                        lastMovement = client.lastMovement,
                        onNavigate = {
                            viewModel.navigateTo(Screen.ClientDetails(client.id))
                        },
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun ListItemClientCard(
    client: Client,
    totalDue: Double,
    lastMovement: Long,
    onNavigate: () -> Unit,
    viewModel: FiadoViewModel
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onNavigate() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle avatar initials
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                val initial = if (client.name.isNotEmpty()) client.name.first().uppercase() else "C"
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = client.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${viewModel.t("last_mov")}: ${viewModel.formatDate(lastMovement)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Deve: " + viewModel.formatCurrency(totalDue),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (totalDue > 0.0) Color(0xFFEF4444) else Color(0xFF10B981)
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight ?: Icons.Default.Refresh,
                    contentDescription = "Acessar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun AddClientScreen(viewModel: FiadoViewModel) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            MainTopBar(
                title = viewModel.t("create_client"),
                onBack = { viewModel.navigateBack() }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome Completo") },
                modifier = Modifier.fillMaxWidth().testTag("input_client_name"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text(viewModel.t("phone")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth().testTag("input_client_phone"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
            )

            OutlinedTextField(
                value = whatsapp,
                onValueChange = { whatsapp = it },
                label = { Text(viewModel.t("whatsapp") + " (apenas números)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth().testTag("input_client_whatsapp"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(viewModel.t("email")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
            )

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text(viewModel.t("address")) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(viewModel.t("notes")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (name.isBlank() || phone.isBlank()) {
                        Toast.makeText(context, "Nome e telefone são obrigatórios", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.registerClient(name, phone, whatsapp, email, address, notes)
                        viewModel.navigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_save_client"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Salvar Cliente", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun AddDebtScreen(clientId: Int, viewModel: FiadoViewModel) {
    var type by remember { mutableStateOf("Produto") } // "Produto" or "Serviço"
    var description by remember { mutableStateOf("") }
    var valueStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    // Product selector states
    val products by viewModel.productsFlow.collectAsStateWithLifecycle()
    var selectedProducts by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) } // Map of ProductId to Quantity
    var isCustomProduct by remember { mutableStateOf(false) }
    var customQuantity by remember { mutableStateOf(1) }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            MainTopBar(
                title = viewModel.t("add_debt"),
                onBack = { viewModel.navigateBack() }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Type Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { 
                        type = "Produto"
                        selectedProducts = emptyMap()
                        description = ""
                        valueStr = ""
                        customQuantity = 1
                        isCustomProduct = false
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (type == "Produto") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (type == "Produto") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(viewModel.t("product"))
                }

                Button(
                    onClick = { 
                        type = "Serviço" 
                        selectedProducts = emptyMap()
                        description = ""
                        valueStr = ""
                        customQuantity = 1
                        isCustomProduct = false
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (type == "Serviço") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (type == "Serviço") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(viewModel.t("service"))
                }
            }

            if (type == "Produto" && products.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isCustomProduct) "Produto Personalizado" else "Selecione os Produtos:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    TextButton(
                        onClick = { 
                            isCustomProduct = !isCustomProduct 
                            description = ""
                            valueStr = ""
                            customQuantity = 1
                            selectedProducts = emptyMap()
                        }
                    ) {
                        Text(if (isCustomProduct) "Ver Lista de Produtos" else "Produto Personalizado")
                    }
                }
            }

            if (type == "Produto" && (products.isEmpty() || isCustomProduct)) {
                Text(
                    text = "Dados do Produto Personalizado:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Nome do Produto Personalizado") },
                    modifier = Modifier.fillMaxWidth().testTag("input_custom_product_desc"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                )

                OutlinedTextField(
                    value = valueStr,
                    onValueChange = { valueStr = it },
                    label = { Text("Preço Unitário (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("input_custom_product_val"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Quantidade",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val priceVal = valueStr.toDoubleOrNull() ?: 0.0
                        Text(
                            text = "Total: R$ %.2f".format(priceVal * customQuantity),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (customQuantity > 1) {
                                    customQuantity--
                                }
                            },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape).size(36.dp)
                        ) {
                            Icon(Icons.Default.Remove, "Diminuir", tint = MaterialTheme.colorScheme.primary)
                        }

                        Text(
                            text = customQuantity.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = {
                                customQuantity++
                            },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape).size(36.dp)
                        ) {
                            Icon(Icons.Default.Add, "Aumentar", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            } else if (type == "Produto" && products.isNotEmpty() && !isCustomProduct) {
                // Vertical list of products with checkable rows
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    products.forEach { prod ->
                        val qty = selectedProducts[prod.id]
                        val isSelected = qty != null
                        val hasStockLimit = prod.stockQuantity != null
                        val availableStock = prod.stockQuantity ?: 0
                        val isOutOfStock = hasStockLimit && availableStock <= 0

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isOutOfStock) {
                                    if (isSelected) {
                                        selectedProducts = selectedProducts - prod.id
                                    } else {
                                        selectedProducts = selectedProducts + (prod.id to 1)
                                    }
                                }
                                .testTag("select_product_${prod.id}"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = when {
                                    isOutOfStock -> Color.LightGray.copy(alpha = 0.5f)
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    else -> Color(0xFFE0E0E0)
                                }
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isOutOfStock -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                                    else -> MaterialTheme.colorScheme.surface
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        enabled = !isOutOfStock,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                selectedProducts = selectedProducts + (prod.id to 1)
                                            } else {
                                                selectedProducts = selectedProducts - prod.id
                                            }
                                        }
                                    )
                                    Column {
                                        Text(
                                            text = prod.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = when {
                                                isOutOfStock -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                                isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "R$ %.2f".format(prod.price),
                                                fontSize = 12.sp,
                                                color = when {
                                                    isOutOfStock -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                                    else -> MaterialTheme.colorScheme.primary
                                                },
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "•",
                                                fontSize = 12.sp,
                                                color = if (isOutOfStock) Color.LightGray else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                            Text(
                                                text = if (hasStockLimit) {
                                                    if (isOutOfStock) "Sem Estoque" else "Estoque: $availableStock"
                                                } else {
                                                    "Estoque: Sem limite"
                                                },
                                                fontSize = 12.sp,
                                                color = when {
                                                    isOutOfStock -> Color.Red.copy(alpha = 0.7f)
                                                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                                fontWeight = if (isOutOfStock) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }

                                if (isSelected && !isOutOfStock) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                val currentQty = qty ?: 1
                                                if (currentQty > 1) {
                                                    selectedProducts = selectedProducts + (prod.id to currentQty - 1)
                                                } else {
                                                    selectedProducts = selectedProducts - prod.id
                                                }
                                            },
                                            modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape).size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, "Diminuir", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        }

                                        Text(
                                            text = (qty ?: 1).toString(),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )

                                        IconButton(
                                            onClick = {
                                                val currentQty = qty ?: 1
                                                if (hasStockLimit && currentQty >= availableStock) {
                                                    Toast.makeText(context, "Estoque insuficiente! Máximo disponível: $availableStock", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    selectedProducts = selectedProducts + (prod.id to currentQty + 1)
                                                }
                                            },
                                            modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape).size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Add, "Aumentar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (selectedProducts.isNotEmpty()) {
                    val totalSum = selectedProducts.entries.sumOf { (productId, qty) ->
                        val price = products.firstOrNull { it.id == productId }?.price ?: 0.0
                        price * qty
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Total Selecionado: R$ %.2f".format(totalSum),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            } else {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Nome do serviço") },
                    modifier = Modifier.fillMaxWidth().testTag("input_debt_desc"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                )

                OutlinedTextField(
                    value = valueStr,
                    onValueChange = { valueStr = it },
                    label = { Text(viewModel.t("value") + " (Múltiplos de mil centavos ou R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("input_debt_val"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                )
            }

            // Date Selection Row
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Data do Débito",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .clickable {
                            val currentCal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
                            android.app.DatePickerDialog(
                                context,
                                { _, selectedYear, selectedMonth, selectedDay ->
                                    val newCalendar = Calendar.getInstance()
                                    newCalendar.set(Calendar.YEAR, selectedYear)
                                    newCalendar.set(Calendar.MONTH, selectedMonth)
                                    newCalendar.set(Calendar.DAY_OF_MONTH, selectedDay)
                                    selectedDateMillis = newCalendar.timeInMillis
                                },
                                currentCal.get(Calendar.YEAR),
                                currentCal.get(Calendar.MONTH),
                                currentCal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Data do Débito",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = viewModel.formatDate(selectedDateMillis),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Toque para alterar caso não tenha sido hoje",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Alterar data",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(viewModel.t("notes") + " (opcional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (type == "Produto" && products.isNotEmpty() && !isCustomProduct) {
                        if (selectedProducts.isEmpty()) {
                            Toast.makeText(context, "Selecione pelo menos um produto", Toast.LENGTH_SHORT).show()
                        } else {
                            var hasStockError = false
                            selectedProducts.forEach { (productId, qty) ->
                                val prod = products.firstOrNull { it.id == productId }
                                if (prod != null && prod.stockQuantity != null && qty > prod.stockQuantity) {
                                    Toast.makeText(context, "O produto ${prod.name} possui apenas ${prod.stockQuantity} unidades em estoque.", Toast.LENGTH_LONG).show()
                                    hasStockError = true
                                }
                            }

                            if (!hasStockError) {
                                selectedProducts.forEach { (productId, qty) ->
                                    val prod = products.firstOrNull { it.id == productId }
                                    if (prod != null) {
                                        viewModel.addDebtToClient(
                                            clientId = clientId,
                                            type = "Produto",
                                            description = prod.name,
                                            value = prod.price * qty,
                                            notes = if (notes.isBlank()) null else notes,
                                            createdAt = selectedDateMillis,
                                            productId = prod.id,
                                            productPrice = prod.price,
                                            quantity = qty
                                        )
                                        if (prod.stockQuantity != null) {
                                            viewModel.updateProduct(prod.copy(stockQuantity = prod.stockQuantity - qty))
                                        }
                                    }
                                }
                                viewModel.navigateBack()
                            }
                        }
                    } else if (type == "Produto" && (products.isEmpty() || isCustomProduct)) {
                        val priceDbl = valueStr.toDoubleOrNull()
                        if (description.isBlank() || priceDbl == null || priceDbl <= 0.0) {
                            Toast.makeText(context, "Insira um nome e preço unitário válido para o produto personalizado", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.addDebtToClient(
                                clientId = clientId,
                                type = "Produto",
                                description = description,
                                value = priceDbl * customQuantity,
                                notes = if (notes.isBlank()) null else notes,
                                createdAt = selectedDateMillis,
                                productId = null,
                                productPrice = priceDbl,
                                quantity = customQuantity
                            )
                            viewModel.navigateBack()
                        }
                    } else {
                        val finalValue = valueStr.toDoubleOrNull()
                        if (description.isBlank() || finalValue == null || finalValue <= 0.0) {
                            Toast.makeText(context, "Preencha a descrição e um valor numérico válido", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.addDebtToClient(
                                clientId = clientId,
                                type = type,
                                description = description,
                                value = finalValue,
                                notes = notes,
                                createdAt = selectedDateMillis,
                                productId = null,
                                productPrice = null,
                                quantity = null
                            )
                            viewModel.navigateBack()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_save_debt"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (type == "Produto" && products.isNotEmpty() && !isCustomProduct) "Confirmar Produtos" else "Salvar Débito",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun ClientProfileScreen(clientId: Int, viewModel: FiadoViewModel) {
    val clients by viewModel.clientsFlow.collectAsStateWithLifecycle()
    val debts by viewModel.debtsFlow.collectAsStateWithLifecycle()
    val payments by viewModel.paymentsFlow.collectAsStateWithLifecycle()

    val client = clients.firstOrNull { it.id == clientId }
    val context = LocalContext.current

    if (client == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Cliente não encontrado!")
        }
        return
    }

    val clientDebts = debts.filter { it.clientId == clientId }
    val clientPayments = payments.filter { it.clientId == clientId }

    val totalDue = clientDebts.sumOf { it.value }
    val totalPaid = clientPayments.sumOf { it.value }
    val totalRemaining = totalDue - totalPaid

    var expandedDebtToPay by remember { mutableStateOf<Debt?>(null) }
    var payAmount by remember { mutableStateOf("") }
    var payObs by remember { mutableStateOf("") }
    var selectedPayMethod by remember { mutableStateOf("Dinheiro") }

    var showGenericPaymentDialog by remember { mutableStateOf(false) }
    var genericPayAmount by remember { mutableStateOf("") }
    var genericPayObs by remember { mutableStateOf("") }
    var selectedGenericPayMethod by remember { mutableStateOf("Dinheiro") }

    var showDeleteFirstConfirmation by remember { mutableStateOf(false) }
    var showDeleteSecondConfirmation by remember { mutableStateOf(false) }

    val isDark = when (viewModel.themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    Scaffold(
        topBar = {
            MainTopBar(
                title = client.name,
                onBack = { viewModel.navigateBack() },
                actions = {
                    IconButton(onClick = {
                        showDeleteFirstConfirmation = true
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Excluir Cliente", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Card Header Info
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = client.name.first().uppercase(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(client.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(client.phone, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        if (!client.notes.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Nota: ${client.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Financial Summary Block
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FinancialMiniCard(
                        title = viewModel.t("due"),
                        value = viewModel.formatCurrency(totalDue),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        darkMode = isDark
                    )

                    FinancialMiniCard(
                        title = viewModel.t("paid"),
                        value = viewModel.formatCurrency(totalPaid),
                        color = Color(0xFF10B981),
                        modifier = Modifier.weight(1f),
                        darkMode = isDark
                    )

                    FinancialMiniCard(
                        title = viewModel.t("balance"),
                        value = viewModel.formatCurrency(totalRemaining),
                        color = Color(0xFFEF4444),
                        modifier = Modifier.weight(1f),
                        darkMode = isDark
                    )
                }
            }

            // Action Buttons (Add Debt & Register general payment)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.navigateTo(Screen.AddDebt(client.id)) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("btn_add_debt"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Add, "Novo débito")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(viewModel.t("add_debt"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = { showGenericPaymentDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("btn_register_client_payment"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, "Receber valor", tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Receber Valor", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                    }
                }
            }

            // Debts List History
            item {
                Text(
                    text = "Histórico de Débitos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (clientDebts.isEmpty()) {
                item {
                    Text(
                        viewModel.t("no_debts"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                items(clientDebts) { debt ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = debt.description,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (debt.productId != null && debt.productPrice != null && debt.quantity != null) {
                                        Text(
                                            text = "Preço Un.: ${viewModel.formatCurrency(debt.productPrice)} | Qtd: ${debt.quantity}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Text(
                                        text = viewModel.formatDateWithTime(debt.createdAt),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                val sColor = when (debt.status) {
                                    "Pago" -> Color(0xFF10B981)
                                    "Parcial" -> Color(0xFFF59E0B)
                                    else -> Color(0xFFEF4444)
                                }

                                Badge(containerColor = sColor.copy(alpha = 0.15f)) {
                                    Text(
                                        text = viewModel.t(debt.status.lowercase()),
                                        color = sColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = viewModel.formatCurrency(debt.value) + " (${viewModel.t(debt.type.lowercase())})",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                if (debt.status != "Pago") {
                                    Button(
                                        onClick = {
                                            expandedDebtToPay = debt
                                            payAmount = (debt.value - debt.paidValue).toString()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("btn_register_pay")
                                    ) {
                                        Text("Pagar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (debt.paidValue > 0.0) {
                                Text(
                                    text = viewModel.t("partial_pay").format(viewModel.formatCurrency(debt.paidValue)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF10B981)
                                )
                            }

                            if (!debt.notes.isNullOrBlank()) {
                                Text(
                                    text = "Nota: ${debt.notes}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // Payments History Section
            item {
                Text(
                    text = "Histórico de Pagamentos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (clientPayments.isEmpty()) {
                item {
                    Text(
                        "Nenhum pagamento recebido.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                items(clientPayments) { payment ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.08f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = viewModel.formatCurrency(payment.value),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF10B981)
                                )
                                Text(
                                    text = viewModel.formatDateWithTime(payment.createdAt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!payment.notes.isNullOrBlank()) {
                                    Text(
                                        text = payment.notes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(Icons.Default.Check, "Pago", tint = Color(0xFF10B981))
                        }
                    }
                }
            }
        }
    }

    // Payment Dialog Dialog form
    if (expandedDebtToPay != null) {
        val debt = expandedDebtToPay!!
        Dialog(onDismissRequest = { expandedDebtToPay = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = viewModel.t("add_payment_for"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Dívida: ${debt.description} -> restante: ${viewModel.formatCurrency(debt.value - debt.paidValue)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = payAmount,
                        onValueChange = { payAmount = it },
                        label = { Text(viewModel.t("payment_val")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("input_pay_amount")
                    )

                    Text(
                        text = "Meio de pagamento",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("💵 Dinheiro", "⚡ Pix", "💳 Cartão").forEach { method ->
                            val cleanMethod = method.substring(2)
                            val isSelected = selectedPayMethod == cleanMethod
                            val baseColor = when (cleanMethod) {
                                "Dinheiro" -> Color(0xFF10B981)
                                "Pix" -> Color(0xFF06B6D4)
                                else -> Color(0xFF0F4C81)
                            }
                            val containerColor = if (isSelected) baseColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(containerColor)
                                    .clickable { selectedPayMethod = cleanMethod }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = method,
                                    color = contentColor,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = payObs,
                        onValueChange = { payObs = it },
                        label = { Text(viewModel.t("payment_obs")) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val valueToPay = payAmount.toDoubleOrNull()
                            if (valueToPay == null || valueToPay <= 0.0) {
                                Toast.makeText(context, "Insira um valor válido", Toast.LENGTH_SHORT).show()
                            } else {
                                val finalObs = if (payObs.isBlank()) "Meio de pagamento: $selectedPayMethod" else "[$selectedPayMethod] $payObs"
                                viewModel.addPaymentToDebt(debt.id, valueToPay, finalObs)
                                expandedDebtToPay = null
                                selectedPayMethod = "Dinheiro"
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("dialog_confirm_pay"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text(viewModel.t("register_payment"), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    if (showGenericPaymentDialog) {
        Dialog(onDismissRequest = { showGenericPaymentDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Registrar Valor Recebido",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Cliente: ${client.name}\nSaldo devedor total: ${viewModel.formatCurrency(totalRemaining)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = genericPayAmount,
                        onValueChange = { genericPayAmount = it },
                        label = { Text("Valor recebido") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("input_generic_pay_amount")
                    )

                    Text(
                        text = "Meio de pagamento",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("💵 Dinheiro", "⚡ Pix", "💳 Cartão").forEach { method ->
                            val cleanMethod = method.substring(2)
                            val isSelected = selectedGenericPayMethod == cleanMethod
                            val baseColor = when (cleanMethod) {
                                "Dinheiro" -> Color(0xFF10B981)
                                "Pix" -> Color(0xFF06B6D4)
                                else -> Color(0xFF0F4C81)
                            }
                            val containerColor = if (isSelected) baseColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(containerColor)
                                    .clickable { selectedGenericPayMethod = cleanMethod }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = method,
                                    color = contentColor,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = genericPayObs,
                        onValueChange = { genericPayObs = it },
                        label = { Text("Observação (opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val valueToPay = genericPayAmount.toDoubleOrNull()
                            if (valueToPay == null || valueToPay <= 0.0) {
                                Toast.makeText(context, "Insira um valor válido", Toast.LENGTH_SHORT).show()
                            } else {
                                val finalObs = if (genericPayObs.isBlank()) "Meio de pagamento: $selectedGenericPayMethod" else "[$selectedGenericPayMethod] $genericPayObs"
                                viewModel.registerGenericClientPayment(client.id, valueToPay, finalObs)
                                genericPayAmount = ""
                                genericPayObs = ""
                                selectedGenericPayMethod = "Dinheiro"
                                showGenericPaymentDialog = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("dialog_confirm_generic_pay"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("Confirmar", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    if (showDeleteFirstConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteFirstConfirmation = false },
            title = {
                Text(
                    text = "Confirmar Exclusão",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Você tem certeza de que deseja excluir o cliente \"${client.name}\"? Esta ação removerá o registro do cliente.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteFirstConfirmation = false
                        showDeleteSecondConfirmation = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_client_step_1")
                ) {
                    Text("Excluir", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteFirstConfirmation = false },
                    modifier = Modifier.testTag("cancel_delete_client_step_1")
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showDeleteSecondConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteSecondConfirmation = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Aviso Urgente",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Excluir Definitivamente?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            text = {
                Text(
                    text = "Atenção: Esta ação é TOTALMENTE IRREVERSÍVEL. Todos os dados, débitos ativos e histórico de pagamentos de \"${client.name}\" serão apagados permanentemente do sistema.\n\nDeseja realmente confirmar a exclusão definitiva?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteSecondConfirmation = false
                        viewModel.removeClient(client)
                        viewModel.navigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_client_step_2")
                ) {
                    Text("Sim, Excluir Definitivamente", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteSecondConfirmation = false },
                    modifier = Modifier.testTag("cancel_delete_client_step_2")
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun FinancialMiniCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    darkMode: Boolean
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (darkMode) Color(0xFF151F32) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AccountSettingsScreen(viewModel: FiadoViewModel) {
    val clients by viewModel.clientsFlow.collectAsStateWithLifecycle()
    val debts by viewModel.debtsFlow.collectAsStateWithLifecycle()
    val payments by viewModel.paymentsFlow.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var isSyncingByHand by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("account_screen"),
        contentPadding = PaddingValues(16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App header
        item {
            Text(
                text = viewModel.t("tab_account"),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }

        // Login / Account UI with multi-account support and persistent state
        item {
            var showEmailAuthDialog by remember { mutableStateOf(false) }
            var authEmailInput by remember { mutableStateOf("") }
            var authNameInput by remember { mutableStateOf("") }
            var authPasswordInput by remember { mutableStateOf("") }
            var isCreateAccountMode by remember { mutableStateOf(false) }
            var authDialogError by remember { mutableStateOf("") }
            var isDialogLoading by remember { mutableStateOf(false) }

            if (showEmailAuthDialog) {
                AlertDialog(
                    onDismissRequest = {
                        if (!isDialogLoading) showEmailAuthDialog = false
                    },
                    title = {
                        Text(
                            text = if (isCreateAccountMode) "Criar Nova Conta" else "Acessar Conta",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Selecione uma conta rápida ou digite seu e-mail para carregar todos os dados persistidos:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Quick account selector chips for seamless switching & verification
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = authEmailInput == "luishenriquesilrib2006@gmail.com",
                                    onClick = {
                                        authEmailInput = "luishenriquesilrib2006@gmail.com"
                                        authNameInput = "Luis Henrique"
                                        isCreateAccountMode = false
                                    },
                                    label = { Text("Conta 1 (Luis)", fontSize = 12.sp) }
                                )
                                FilterChip(
                                    selected = authEmailInput == "maria@gmail.com",
                                    onClick = {
                                        authEmailInput = "maria@gmail.com"
                                        authNameInput = "Maria"
                                        isCreateAccountMode = false
                                    },
                                    label = { Text("Conta 2 (Maria)", fontSize = 12.sp) }
                                )
                            }

                            OutlinedTextField(
                                value = authNameInput,
                                onValueChange = { authNameInput = it },
                                label = { Text("Nome (opcional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = authEmailInput,
                                onValueChange = { authEmailInput = it },
                                label = { Text("E-mail *") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = authPasswordInput,
                                onValueChange = { authPasswordInput = it },
                                label = { Text("Senha (opcional para nuvem)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            if (authDialogError.isNotEmpty()) {
                                Text(
                                    text = authDialogError,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        isCreateAccountMode = !isCreateAccountMode
                                        authDialogError = ""
                                    }
                                ) {
                                    Text(
                                        text = if (isCreateAccountMode) "Já tem conta? Entrar" else "Novo? Criar conta",
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val email = authEmailInput.trim()
                                if (email.isBlank() || !email.contains("@")) {
                                    authDialogError = "Por favor, insira um e-mail válido."
                                    return@Button
                                }
                                isDialogLoading = true
                                authDialogError = ""

                                if (authPasswordInput.isNotBlank() && viewModel.isFirebaseReady()) {
                                    if (isCreateAccountMode) {
                                        viewModel.firebaseRegister(email, authPasswordInput.trim()) { success, err ->
                                            isDialogLoading = false
                                            if (success) {
                                                showEmailAuthDialog = false
                                            } else {
                                                authDialogError = err ?: "Falha ao registrar conta."
                                            }
                                        }
                                    } else {
                                        viewModel.firebaseLogin(email, authPasswordInput.trim()) { success, err ->
                                            isDialogLoading = false
                                            if (success) {
                                                showEmailAuthDialog = false
                                            } else {
                                                authDialogError = err ?: "Falha ao autenticar."
                                            }
                                        }
                                    }
                                } else {
                                    val name = authNameInput.trim().ifEmpty { email.substringBefore("@") }
                                    viewModel.loginWithAccount(name, email) {
                                        isDialogLoading = false
                                        showEmailAuthDialog = false
                                    }
                                }
                            },
                            enabled = !isDialogLoading,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isDialogLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                            } else {
                                Text(if (isCreateAccountMode) "Criar e Entrar" else "Entrar")
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showEmailAuthDialog = false },
                            enabled = !isDialogLoading
                        ) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (viewModel.isLoggedIn) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            val initLetter = if (viewModel.loggedInName.isNotEmpty()) viewModel.loggedInName.first().uppercase() else "U"
                            Text(initLetter, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(viewModel.loggedInName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(viewModel.loggedInEmail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            Text(
                                text = "Plano ${viewModel.userPlan}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    authEmailInput = ""
                                    authNameInput = ""
                                    authPasswordInput = ""
                                    authDialogError = ""
                                    isCreateAccountMode = false
                                    showEmailAuthDialog = true
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Trocar Conta", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }

                            Button(
                                onClick = { viewModel.firebaseLogout() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(viewModel.t("logout"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    } else {
                        var isProcessing by remember { mutableStateOf(false) }
                        var firebaseMessage by remember { mutableStateOf("") }

                        val googleSignInLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.StartActivityForResult()
                        ) { result ->
                            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                            try {
                                val account = task.getResult(ApiException::class.java)
                                val idToken = account?.idToken
                                if (idToken != null) {
                                    isProcessing = true
                                    viewModel.loginWithGoogle(idToken) { success, errorMsg ->
                                        isProcessing = false
                                        if (success) {
                                            firebaseMessage = ""
                                        } else {
                                            firebaseMessage = errorMsg ?: "Falha ao entrar com Google."
                                        }
                                    }
                                } else {
                                    firebaseMessage = "Não foi possível obter o token do Google."
                                }
                            } catch (e: Exception) {
                                firebaseMessage = "A autenticação do Google falhou: ${e.localizedMessage}"
                            }
                        }

                        Text("Sua Conta", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Entre com sua conta para acessar seus dados sincronizados de forma permanente.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        Spacer(modifier = Modifier.height(20.dp))

                        if (firebaseMessage.isNotEmpty()) {
                            Text(firebaseMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        if (isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        } else {
                            Button(
                                onClick = {
                                    val clientId = viewModel.getGoogleWebClientId()
                                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                        .requestIdToken(clientId)
                                        .requestEmail()
                                        .build()
                                    val googleSignInClient = GoogleSignIn.getClient(context, gso)
                                    googleSignInClient.signOut().addOnCompleteListener {
                                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                                    .testTag("google_login_button"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.onSurface,
                                    contentColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Text("G", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF4285F4), modifier = Modifier.padding(end = 8.dp))
                                    Text(
                                        text = "Entrar com o Gmail",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = {
                                    authEmailInput = ""
                                    authNameInput = ""
                                    authPasswordInput = ""
                                    authDialogError = ""
                                    isCreateAccountMode = false
                                    showEmailAuthDialog = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                                    .testTag("email_login_button"),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Entrar com E-mail ou Outra Conta",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }

        // Statistics Summary block (Estatísticas)
        item {
            val totalPending = debts.filter { it.status != "Pago" }.sumOf { it.value - it.paidValue }
            val clientCount = clients.size
            val debtCount = debts.size
            val totalReceivedThisMonth = viewModel.calculateReceivedForPeriod(payments, "Esse mês")
            val lateClients = clients.count { client ->
                val clientDebts = debts.filter { it.clientId == client.id && it.status != "Pago" }
                if (clientDebts.isEmpty()) false
                else {
                    val oldest = clientDebts.minOf { it.createdAt }
                    (System.currentTimeMillis() - oldest) > (30 * 24 * 60 * 60 * 1000L)
                }
            }

            Column {
                Text(
                    text = viewModel.t("statistics"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatLabelValue(viewModel.t("total_rec_month"), viewModel.formatCurrency(totalReceivedThisMonth), Color(0xFF10B981))
                        StatLabelValue(viewModel.t("total_pend_amt"), viewModel.formatCurrency(totalPending), Color(0xFFEF4444))
                        StatLabelValue(viewModel.t("client_qty"), clientCount.toString(), MaterialTheme.colorScheme.onSurface)
                        StatLabelValue(viewModel.t("debt_qty"), debtCount.toString(), MaterialTheme.colorScheme.onSurface)
                        StatLabelValue("Clientes inadimplentes (>30 dias)", lateClients.toString(), Color(0xFFEF4444))
                    }
                }
            }
        }

        // Appearance config (Aparências / Claro-Escuro)
        item {
            Column {
                Text(
                    text = viewModel.t("appearance"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        listOf("light", "dark").forEach { mode ->
                            val isSelected = viewModel.themeMode == mode
                            val label = when (mode) {
                                "light" -> viewModel.t("theme_light")
                                else -> viewModel.t("theme_dark")
                            }

                            Button(
                                onClick = { viewModel.updateThemeMode(mode) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(label, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }



        // Backup and Restore Actions (Sincronização e Backup simplificado)
        item {
            Column {
                Text(
                    text = viewModel.t("backup_sync"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.navigateTo(Screen.SyncSettings)
                        }
                        .testTag("sync_navigation_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Sincronização",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Sincronizar Dados",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Opções de backup local e salvamento na nuvem",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight ?: Icons.Default.PlayArrow,
                            contentDescription = "Abrir Opções de Sincronização",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }


    }
}

@Composable
fun StatLabelValue(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.ExtraBold, color = valueColor)
    }
}

@Composable
fun SyncSettingsScreen(viewModel: FiadoViewModel) {
    val context = LocalContext.current
    var isSyncingLocal by remember { mutableStateOf(false) }
    var isSyncingCloud by remember { mutableStateOf(false) }

    val isFirebaseActive = viewModel.isFirebaseReady()
    val isLoggedIn = viewModel.isLoggedIn
    val loggedInEmail = viewModel.loggedInEmail

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TopBar / Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateBack() },
                modifier = Modifier.testTag("sync_back_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Voltar",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Backup e Sincronização",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("sync_settings_scroll"),
            contentPadding = PaddingValues(16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Introductory Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync Info",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Mantenha seus dados seguros",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Escolha salvar no armazenamento seguro do seu celular ou sincronize automaticamente na nuvem usando seu Gmail.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Cloud Backup options
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Sincronização em Nuvem (Google)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (!isFirebaseActive) {
                                Text(
                                    text = "O serviço de sincronização na nuvem não está configurado no momento.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else if (!isLoggedIn) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Você não está logado. Faça login com seu Gmail primeiro para conseguir sincronizar os dados na nuvem.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Button(
                                        onClick = {
                                            viewModel.navigateTo(Screen.Account)
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Ir para Tela de Login", fontSize = 12.sp)
                                    }
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Conectado como: $loggedInEmail",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = "Sincronize clientes, débitos e histórico de pagamentos de forma totalmente segura.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            isSyncingCloud = true
                                            viewModel.triggerBackupCloud { success ->
                                                isSyncingCloud = false
                                                Toast.makeText(
                                                    context,
                                                    if (success) "Sincronizado na Nuvem com sucesso!" else "Falha ao sincronizar na nuvem",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        if (isSyncingCloud) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                                        } else {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Salvar na Nuvem", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                Text("Enviar dados", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            isSyncingCloud = true
                                            viewModel.triggerRestoreCloud { success ->
                                                isSyncingCloud = false
                                                Toast.makeText(
                                                    context,
                                                    if (success) "Dados baixados com sucesso!" else "Nenhum dado encontrado na nuvem para baixar",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                    ) {
                                        if (isSyncingCloud) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                                        } else {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Baixar da Nuvem", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                Text("Restaurar dados", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.7f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Local Backup Options
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Segurança Local (Dispositivo)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Gere um backup local instantâneo das contas guardado com segurança em seu armazenamento privado do celular.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        isSyncingLocal = true
                                        viewModel.triggerBackupLocal { success ->
                                            isSyncingLocal = false
                                            Toast.makeText(
                                                context,
                                                if (success) "Backup local salvo com sucesso!" else "Falha ao gravar backup local",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    if (isSyncingLocal) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Criar Backup Local", fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                            Text("Salvar cópia", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        viewModel.triggerRestoreLocal { success ->
                                            Toast.makeText(
                                                context,
                                                if (success) "Dados locais restaurados com sucesso!" else "Nenhum arquivo de backup local encontrado",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Restaurar Backup", fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                        Text("Ler do celular", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// HELPER COMPOSABLES & INTEGRATIONS FOR CUSTOM ACTIONS, CALENDAR & ALERTS
// -----------------------------------------------------------------------------

@Composable
fun ModernHomeHeader(
    viewModel: FiadoViewModel,
    clients: List<Client>,
    debts: List<Debt>,
    onNotificationClick: () -> Unit
) {
    val hrs = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hrs in 5..11 -> "Bom dia"
        hrs in 12..17 -> "Boa tarde"
        else -> "Boa noite"
    }

    val initials = viewModel.loggedInName.split(" ")
        .filter { it.isNotBlank() }
        .map { it.first().uppercase() }
        .take(2)
        .joinToString("")

    // Calculate overdue alert count
    val now = System.currentTimeMillis()
    val overdueCount = clients.count { client ->
        val clientDebts = debts.filter { it.clientId == client.id && it.status != "Pago" }
        val oldest = clientDebts.minByOrNull { it.createdAt }
        if (oldest != null) {
            val age = ((now - oldest.createdAt) / (24 * 3600 * 1000L)).toInt()
            age >= 14
        } else false
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp)
            .testTag("modern_home_header"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar circle
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFFE3F2FD))
                .border(1.5.dp, Color(0xFF0F4C81), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials.ifBlank { "U" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F4C81)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF4A6572)
            )
            Text(
                text = viewModel.loggedInName.ifBlank { "Usuário" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF101B2B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Notification Icon with Badge
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF0F4F8))
                .clickable { onNotificationClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notificações",
                tint = Color(0xFF0F4C81)
            )

            if (overdueCount > 0) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF5350))
                )
            }
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(98.dp)
            .clickable { onClick() }
            .testTag("quick_action_${title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE6EBF0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF101B2B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = Color(0xFF758A99),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ModernCalendarWidget(
    viewModel: FiadoViewModel,
    clients: List<Client>,
    debts: List<Debt>,
    payments: List<Payment>,
    onDaySelected: (SelectedDayStatus) -> Unit
) {
    val monthNames = listOf("", "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro")
    val daysOfWeek = listOf("D", "S", "T", "Q", "Q", "S", "S")

    val calendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, viewModel.calendarYear)
        set(Calendar.MONTH, viewModel.calendarMonth - 1)
        set(Calendar.DAY_OF_MONTH, 1)
    }

    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayInWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 0-indexed for Sunday

    Column(modifier = Modifier.fillMaxWidth()) {
        // Month Selector Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${monthNames[viewModel.calendarMonth]} ${viewModel.calendarYear}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF101B2B)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = {
                    if (viewModel.calendarMonth == 1) {
                        viewModel.calendarMonth = 12
                        viewModel.calendarYear -= 1
                    } else {
                        viewModel.calendarMonth -= 1
                    }
                }) {
                    Icon(Icons.Default.ChevronLeft, "Mês anterior", tint = Color(0xFF0F4C81))
                }
                IconButton(onClick = {
                    if (viewModel.calendarMonth == 12) {
                        viewModel.calendarMonth = 1
                        viewModel.calendarYear += 1
                    } else {
                        viewModel.calendarMonth += 1
                    }
                }) {
                    Icon(Icons.Default.ChevronRight, "Próximo mês", tint = Color(0xFF0F4C81))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Week Headers
        Row(modifier = Modifier.fillMaxWidth()) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color(0xFF758A99)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Days Grid Calendar Calculation
        val totalCells = (daysInMonth + firstDayInWeek)
        var rowCount = totalCells / 7
        if (totalCells % 7 != 0) rowCount++

        for (row in 0 until rowCount) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNum = cellIndex - firstDayInWeek + 1

                    if (dayNum in 1..daysInMonth) {
                        // Check if debts scheduled on this day
                        val cellCal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, viewModel.calendarYear)
                            set(Calendar.MONTH, viewModel.calendarMonth - 1)
                            set(Calendar.DAY_OF_MONTH, dayNum)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        val dayStart = cellCal.timeInMillis
                        val dayEnd = dayStart + 24 * 3600 * 1000L - 1

                        val dayDebts = debts.filter { it.createdAt in dayStart..dayEnd }
                        val hasDebts = dayDebts.isNotEmpty()
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (hasDebts) Color(0xFFE3F2FD) else Color.Transparent)
                                .clickable {
                                    if (hasDebts) {
                                        val firstDebt = dayDebts.first()
                                        val cName = clients.firstOrNull { it.id == firstDebt.clientId }?.name ?: "Cliente Desconhecido"
                                        val dCal = Calendar.getInstance().apply { timeInMillis = firstDebt.createdAt }
                                        val dStr = String.format("%02d/%02d/%d", dCal.get(Calendar.DAY_OF_MONTH), dCal.get(Calendar.MONTH) + 1, dCal.get(Calendar.YEAR))
                                        val tStr = String.format("%02d:%02d", dCal.get(Calendar.HOUR_OF_DAY), dCal.get(Calendar.MINUTE))
                                        
                                        onDaySelected(
                                            SelectedDayStatus.Exists(
                                                dateStr = dStr,
                                                clientName = cName,
                                                type = firstDebt.type,
                                                value = firstDebt.value,
                                                status = firstDebt.status,
                                                time = tStr
                                            )
                                        )
                                    } else {
                                        onDaySelected(SelectedDayStatus.None)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = dayNum.toString(),
                                    fontSize = 12.sp,
                                    fontWeight = if (hasDebts) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (hasDebts) Color(0xFF0F4C81) else Color(0xFF101B2B)
                                )
                                if (hasDebts) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF0F4C81))
                                    )
                                }
                            }
                        }
                    } else {
                        // Empty cell
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    icon: ImageVector,
    color: Color,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF101B2B)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF4A6572),
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun InteractiveClientSelector(
    clients: List<Client>,
    selectedClientId: Int?,
    onClientSelected: (Int) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filteredClients = clients.filter { it.name.contains(query, ignoreCase = true) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Selecione o Cliente",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F4C81)
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Pesquisar cliente...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = { Icon(Icons.Default.Search, "Pesquisar") },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF0F4C81))
        )

        if (selectedClientId != null) {
            val matchedSelected = clients.firstOrNull { it.id == selectedClientId }
            if (matchedSelected != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE3F2FD), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, "Selecionado", tint = Color(0xFF0F4C81), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = matchedSelected.name,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F4C81)
                        )
                    }
                    Text(
                        text = "Alterar",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F4C81),
                        modifier = Modifier
                            .clickable { onClientSelected(-1) }
                            .padding(4.dp)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                filteredClients.take(5).forEach { cl ->
                    Card(
                        modifier = Modifier
                            .clickable { onClientSelected(cl.id) }
                            .testTag("selector_client_${cl.id}"),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Text(
                            cl.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            color = Color(0xFF101B2B)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProductsScreen(viewModel: FiadoViewModel) {
    val products by viewModel.productsFlow.collectAsStateWithLifecycle()
    val debts by viewModel.debtsFlow.collectAsStateWithLifecycle()

    val bestSellingProducts = remember(products, debts) {
        viewModel.getBestSellingProducts(products, debts, "Todo o período")
    }

    val isDark = when (viewModel.themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    var isRankingDialogOpen by remember { mutableStateOf(false) }
    var isAddEditProductOpen by remember { mutableStateOf(false) }
    var selectedProductForEdit by remember { mutableStateOf<com.example.data.Product?>(null) }
    
    val primaryColor = PrimaryBlueMain
    val cardBgColor = if (isDark) Color(0xFF1E2D40) else PureWhite
    val textPrimaryColor = if (isDark) PureWhite else TextDarkPrimary
    val textSecondaryColor = if (isDark) Color(0xFF90A4AE) else TextMuted

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedProductForEdit = null
                    isAddEditProductOpen = true
                },
                containerColor = primaryColor,
                contentColor = PureWhite,
                shape = CircleShape,
                modifier = Modifier.testTag("add_product_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Produto")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Título & Subtítulo da aba Produtos
            item {
                Column {
                    Text(
                        text = "Produtos",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryBlueDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Catálogo de produtos e ranking de vendas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }

            // 2. Seção "Produtos mais vendidos" (Topo da tela, ordenado do mais para o menos vendido)
            item {
                BestSellingProductsSection(
                    ranking = bestSellingProducts,
                    onViewAllClick = { isRankingDialogOpen = true },
                    onProductClick = { product ->
                        selectedProductForEdit = product
                        isAddEditProductOpen = true
                    },
                    viewModel = viewModel
                )
            }

            // 3. Título de "Todos os produtos"
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Todos os produtos (${products.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlueDark,
                        fontSize = 16.sp
                    )
                }
            }

            // 4. Listagem completa de todos os produtos
            if (products.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceSubtle)
                        .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = TextMuted.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "Nenhum produto cadastrado",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextDarkSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Toque no botão '+' abaixo para cadastrar seu primeiro produto.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(products) { product ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedProductForEdit = product
                                isAddEditProductOpen = true
                            }
                            .testTag("product_item_${product.id}"),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        colors = CardDefaults.cardColors(containerColor = cardBgColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SurfaceSecondary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = product.name,
                                        tint = PrimaryBlueMain,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = product.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimaryColor
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = viewModel.formatCurrency(product.price),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = PrimaryBlueMain,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "•",
                                            color = textSecondaryColor.copy(alpha = 0.5f),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = if (product.stockQuantity != null) "Estoque: ${product.stockQuantity}" else "Estoque: Sem limite",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = textSecondaryColor
                                        )
                                    }
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        selectedProductForEdit = product
                                        isAddEditProductOpen = true
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Edit, "Editar", tint = Color(0xFFFF9800), modifier = Modifier.size(20.dp))
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.deleteProduct(product)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Delete, "Excluir", tint = Color(0xFFF44336), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (isRankingDialogOpen) {
        ProductRankingDialog(
            products = products,
            debts = debts,
            onDismissRequest = { isRankingDialogOpen = false },
            onProductClick = { product ->
                selectedProductForEdit = product
                isAddEditProductOpen = true
            },
            viewModel = viewModel
        )
    }

    if (isAddEditProductOpen) {
        var nameStr by remember { mutableStateOf(selectedProductForEdit?.name ?: "") }
        var priceStr by remember { mutableStateOf(selectedProductForEdit?.price?.toString() ?: "") }
        var stockStr by remember { mutableStateOf(selectedProductForEdit?.stockQuantity?.toString() ?: "") }
        val context = LocalContext.current

        Dialog(onDismissRequest = { isAddEditProductOpen = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E2D40) else Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = if (selectedProductForEdit == null) "Cadastrar Produto" else "Editar Produto",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )

                    OutlinedTextField(
                        value = nameStr,
                        onValueChange = { nameStr = it },
                        label = { Text("Nome do Produto") },
                        modifier = Modifier.fillMaxWidth().testTag("product_name_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimaryColor,
                            unfocusedTextColor = textPrimaryColor,
                            focusedBorderColor = primaryColor
                        )
                    )

                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Preço (R$)") },
                        modifier = Modifier.fillMaxWidth().testTag("product_price_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimaryColor,
                            unfocusedTextColor = textPrimaryColor,
                            focusedBorderColor = primaryColor
                        )
                    )

                    OutlinedTextField(
                        value = stockStr,
                        onValueChange = { stockStr = it },
                        label = { Text("Quantidade em Estoque (Opcional)") },
                        modifier = Modifier.fillMaxWidth().testTag("product_stock_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimaryColor,
                            unfocusedTextColor = textPrimaryColor,
                            focusedBorderColor = primaryColor
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { isAddEditProductOpen = false }) {
                            Text("Cancelar", color = textSecondaryColor)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val priceDbl = priceStr.toDoubleOrNull() ?: 0.0
                                val stockInt = stockStr.toIntOrNull()
                                if (nameStr.isBlank() || priceDbl <= 0.0) {
                                    Toast.makeText(context, "Nome e Preço válidos são necessários!", Toast.LENGTH_SHORT).show()
                                } else {
                                    if (selectedProductForEdit == null) {
                                        viewModel.addProduct(nameStr, priceDbl, stockInt)
                                        Toast.makeText(context, "Produto cadastrado com sucesso!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.updateProduct(selectedProductForEdit!!.copy(name = nameStr, price = priceDbl, stockQuantity = stockInt))
                                        Toast.makeText(context, "Produto atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                                    }
                                    isAddEditProductOpen = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Salvar", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
