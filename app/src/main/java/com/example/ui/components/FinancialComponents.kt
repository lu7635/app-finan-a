package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Client
import com.example.data.Debt
import com.example.data.Payment
import com.example.ui.theme.*
import com.example.viewmodel.DebtorSummary
import com.example.viewmodel.DayFinanceSummary
import com.example.viewmodel.FiadoViewModel

/**
 * 2. CABEÇALHO
 * À esquerda: Ícone/menu do aplicativo e Nome do aplicativo.
 * À direita: Ícone de notificações e Ícone de configurações/menu.
 */
@Composable
fun FinancialTopHeader(
    viewModel: FiadoViewModel,
    overdueCount: Int,
    onNotificationsClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("financial_top_header"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Logo/Icon + App Name
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { /* decorative or home refresh */ }
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(PrimaryBlueMain)
                    .testTag("app_logo_icon"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = "Logo",
                    tint = PureWhite,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Controle financeiro",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextDarkPrimary,
                    fontSize = 18.sp
                )
                Text(
                    text = "Gestão de Recebimentos",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        // Right: Notifications & Settings
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Notification icon with badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SurfaceSubtle)
                    .clickable { onNotificationsClick() }
                    .testTag("notifications_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notificações",
                    tint = TextDarkPrimary,
                    modifier = Modifier.size(20.dp)
                )
                if (overdueCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 6.dp, end = 6.dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(DangerRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (overdueCount > 9) "9+" else overdueCount.toString(),
                            color = PureWhite,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Menu / Settings icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SurfaceSubtle)
                    .clickable { onSettingsClick() }
                    .testTag("settings_menu_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Configurações",
                    tint = TextDarkPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 3. CARD PRINCIPAL & 4. TRÊS BOTÕES PRINCIPAIS
 * Substitui "Your Total Portfolio Value" por "Total Recebido"
 * Mostra indicadores resumidos dinâmicos: +12,4% este mês, +R$ 1.286,00 hoje
 * Na parte inferior do card:
 * [ TOTAL GANHO ] [ VALORES A RECEBER ] [ VALORES RECEBIDOS ]
 */
@Composable
fun MainReceivedFinanceCard(
    totalReceived: Double,
    totalEarned: Double,
    totalPending: Double,
    todayReceived: Double,
    monthGrowth: Double,
    payments: List<Payment> = emptyList(),
    onTotalGanhoClick: () -> Unit,
    onValoresAReceberClick: () -> Unit,
    onValoresRecebidosClick: () -> Unit,
    viewModel: FiadoViewModel
) {
    var selectedPeriod by remember { mutableStateOf("Esse mês") }
    var isPeriodMenuExpanded by remember { mutableStateOf(false) }

    val periodValue = remember(selectedPeriod, payments) {
        viewModel.calculateReceivedForPeriod(payments, selectedPeriod)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .testTag("main_received_finance_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F3A78),
                            Color(0xFF0D2F64)
                        )
                    )
                )
                .padding(22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Label: Total Recebido
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Recebido",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 15.sp
                    )

                    // Small indicator badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.16f))
                            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = selectedPeriod,
                            color = PureWhite,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Big Hero Amount referente ao período selecionado
                AnimatedContent(
                    targetState = periodValue,
                    label = "HeroReceivedAmount"
                ) { value ->
                    Text(
                        text = viewModel.formatCurrency(value),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = PureWhite,
                        fontSize = 32.sp,
                        letterSpacing = (-0.5).sp,
                        modifier = Modifier.testTag("hero_total_received_amount")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Único Botão de Seleção de Período (Hoje, Esta semana, Esse mês, Esse ano)
                Box(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                        .clickable { isPeriodMenuExpanded = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("period_selector_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (selectedPeriod) {
                                "Hoje" -> Icons.Default.Today
                                "Esta semana" -> Icons.Default.DateRange
                                "Esse ano" -> Icons.Default.CalendarToday
                                else -> Icons.Default.Event
                            },
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = "Período: $selectedPeriod",
                            color = PureWhite,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Selecionar período",
                            tint = PureWhite.copy(alpha = 0.9f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (isPeriodMenuExpanded) {
                    Dialog(onDismissRequest = { isPeriodMenuExpanded = false }) {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                                .testTag("period_selector_dialog")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Selecionar Período",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDarkPrimary,
                                        fontSize = 17.sp
                                    )
                                    IconButton(
                                        onClick = { isPeriodMenuExpanded = false },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Fechar",
                                            tint = TextMuted,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                listOf("Hoje", "Esta semana", "Esse mês", "Esse ano").forEach { option ->
                                    val isSelected = selectedPeriod == option
                                    val optValue = viewModel.calculateReceivedForPeriod(payments, option)

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable {
                                                selectedPeriod = option
                                                isPeriodMenuExpanded = false
                                            }
                                            .testTag("period_option_${option.lowercase().replace(" ", "_")}"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) SurfaceSecondary else Color(0xFFF8FAFC)
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) PrimaryBlueMain.copy(alpha = 0.5f) else Color(0xFFE2E8F0)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = when (option) {
                                                        "Hoje" -> Icons.Default.Today
                                                        "Esta semana" -> Icons.Default.DateRange
                                                        "Esse ano" -> Icons.Default.CalendarToday
                                                        else -> Icons.Default.Event
                                                    },
                                                    contentDescription = null,
                                                    tint = if (isSelected) PrimaryBlueMain else TextMuted,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        text = option,
                                                        color = if (isSelected) PrimaryBlueDark else TextDarkPrimary,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        fontSize = 14.sp
                                                    )
                                                    Text(
                                                        text = viewModel.formatCurrency(optValue),
                                                        color = if (isSelected) PrimaryBlueMain else TextMuted,
                                                        fontSize = 12.sp,
                                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                                    )
                                                }
                                            }
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Selecionado",
                                                    tint = PrimaryBlueMain,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // TRÊS BOTÕES PRINCIPAIS (Mesmo tamanho, bordas arredondadas, ícone minimalista, texto e valor)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. TOTAL GANHO
                    MainCardActionButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.TrendingUp,
                        title = "TOTAL GANHO",
                        value = viewModel.formatCurrency(totalEarned),
                        onClick = onTotalGanhoClick,
                        testTag = "btn_total_ganho"
                    )

                    // 2. VALORES A RECEBER
                    MainCardActionButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Schedule,
                        title = "A RECEBER",
                        value = viewModel.formatCurrency(totalPending),
                        onClick = onValoresAReceberClick,
                        testTag = "btn_valores_a_receber"
                    )

                    // 3. VALORES RECEBIDOS
                    MainCardActionButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Payments,
                        title = "RECEBIDOS",
                        value = viewModel.formatCurrency(totalReceived),
                        onClick = onValoresRecebidosClick,
                        testTag = "btn_valores_recebidos"
                    )
                }
            }
        }
    }
}

@Composable
private fun MainCardActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF164785)) // Tonalidade azul ligeiramente diferente
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 6.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = PureWhite,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                color = PureWhite,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 5. CLIENTES DEVEDORES
 * Substitui "Top Holdings"
 * Canto superior direito: "Ver todos"
 * Ordenação automática priorizando clientes com pendências mais antigas (maior tempo de atraso)
 */
@Composable
fun DebtorsSection(
    debtors: List<DebtorSummary>,
    onViewAllClick: () -> Unit,
    onClientClick: (Int) -> Unit,
    viewModel: FiadoViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("debtors_section")
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Clientes Devedores",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextDarkPrimary,
                    fontSize = 18.sp
                )
                if (debtors.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(DangerRedLight)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${debtors.size}",
                            color = DangerRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            TextButton(
                onClick = onViewAllClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.testTag("btn_view_all_debtors")
            ) {
                Text(
                    text = "Ver todos",
                    color = PrimaryBlueMain,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = PrimaryBlueMain,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (debtors.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Tudo em dia!",
                            fontWeight = FontWeight.Bold,
                            color = TextDarkPrimary,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Nenhum cliente possui pendências no momento.",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            // Display top 3-4 debtors prioritized by delay
            val topDebtors = debtors.take(4)
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                topDebtors.forEach { debtor ->
                    DebtorRowCard(
                        debtor = debtor,
                        onClick = { onClientClick(debtor.client.id) },
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun DebtorRowCard(
    debtor: DebtorSummary,
    onClick: () -> Unit,
    viewModel: FiadoViewModel
) {
    val days = debtor.oldestDebtDays
    // Visual indicator: red if >30 days, amber if >14 days, blue if recent
    val (badgeBg, badgeText, statusDotColor) = when {
        days >= 30 -> Triple(DangerRedLight, DangerRed, DangerRed)
        days >= 14 -> Triple(WarningAmberLight, WarningAmber, WarningAmber)
        else -> Triple(SurfaceSecondary, PrimaryBlueMain, PrimaryBlueMain)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("debtor_card_${debtor.client.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, BorderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Circular Avatar + Name + Secondary info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Circular Avatar with Initials
                val initials = debtor.client.name.split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .map { it.first().uppercase() }
                    .joinToString("")
                    .ifEmpty { "C" }

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(SurfaceSecondary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlueMain,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = debtor.client.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = debtor.oldestDebtDescription.ifBlank { "${debtor.pendingDebtsCount} pendência(s)" },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: Pending Value + Delay Badge + Indicator
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = viewModel.formatCurrency(debtor.totalPending),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextDarkPrimary,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(statusDotColor)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$days dias em atraso",
                            color = badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * 6. CALENDÁRIO FINANCEIRO & 7. INTERAÇÃO DO CALENDÁRIO
 * Mostra dias do mês com dados financeiros de cada data
 * Navegação entre meses: < Setembro 2026 >
 * Opção para visualizar: Dia | Semana | Mês (Padrão: MÊS)
 */
@Composable
fun FinancialCalendarSection(
    viewModel: FiadoViewModel,
    clients: List<Client>,
    debts: List<Debt>,
    payments: List<Payment>,
    onDayClick: (DayFinanceSummary) -> Unit
) {
    val currentView = viewModel.calendarViewMode
    val totalDays = viewModel.getDaysInCurrentMonth()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("financial_calendar_section"),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, BorderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header: Title & View Selector [ Dia | Semana | Mês ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Calendário",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextDarkPrimary,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Movimentações financeiras",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                // View Mode Switcher [ Dia | Semana | Mês ]
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceSubtle)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    listOf("Dia", "Semana", "Mês").forEach { mode ->
                        val isSelected = currentView == mode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) PrimaryBlueMain else Color.Transparent)
                                .clickable { viewModel.setCalendarView(mode) }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .testTag("calendar_view_$mode"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode,
                                color = if (isSelected) PureWhite else TextDarkSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Month Navigation: < Setembro 2026 >
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceSecondary)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.previousMonth() },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("btn_prev_month")
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Mês anterior",
                        tint = PrimaryBlueMain
                    )
                }

                Text(
                    text = viewModel.getMonthNameFormatted(),
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlueDark,
                    fontSize = 14.sp
                )

                IconButton(
                    onClick = { viewModel.nextMonth() },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("btn_next_month")
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Próximo mês",
                        tint = PrimaryBlueMain
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Weekdays Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                listOf("D", "S", "T", "Q", "Q", "S", "S").forEach { dayLabel ->
                    Text(
                        text = dayLabel,
                        color = TextMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Days Grid
            val daysList = (1..totalDays).toList()
            val chunkedWeeks = daysList.chunked(7)

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                chunkedWeeks.forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        week.forEach { dayNumber ->
                            val daySummary = viewModel.getDayFinanceSummary(dayNumber, clients, debts, payments)
                            val isSelected = viewModel.selectedCalendarDay == dayNumber

                            CalendarDayCell(
                                dayNumber = dayNumber,
                                isSelected = isSelected,
                                hasReceived = daySummary.totalReceived > 0,
                                hasPending = daySummary.totalPending > 0,
                                onClick = {
                                    viewModel.selectedCalendarDay = dayNumber
                                    onDayClick(daySummary)
                                }
                            )
                        }

                        // Pad last row if fewer than 7 days
                        val remaining = 7 - week.size
                        repeat(remaining) {
                            Spacer(modifier = Modifier.width(38.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Selected Day Quick Highlight Banner
            val activeDay = viewModel.selectedCalendarDay ?: 1
            val activeSummary = viewModel.getDayFinanceSummary(activeDay, clients, debts, payments)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceSubtle)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                    .clickable { onDayClick(activeSummary) }
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Dia ${String.format("%02d", activeDay)} • ${activeSummary.formattedDate}",
                            fontWeight = FontWeight.Bold,
                            color = TextDarkPrimary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (activeSummary.totalReceived > 0) {
                                Text(
                                    text = "Recebido: ${viewModel.formatCurrency(activeSummary.totalReceived)}",
                                    color = SuccessGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (activeSummary.totalPending > 0) {
                                Text(
                                    text = "Pendente: ${viewModel.formatCurrency(activeSummary.totalPending)}",
                                    color = DangerRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (activeSummary.totalReceived == 0.0 && activeSummary.totalPending == 0.0) {
                                Text(
                                    text = "Sem movimentações registradas",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Ver detalhes",
                            color = PrimaryBlueMain,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = PrimaryBlueMain,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    dayNumber: Int,
    isSelected: Boolean,
    hasReceived: Boolean,
    hasPending: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) PrimaryBlueMain else Color.Transparent
            )
            .border(
                width = if (isSelected) 0.dp else 1.dp,
                color = if (isSelected) Color.Transparent else BorderSubtle.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .testTag("cal_day_$dayNumber"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = String.format("%02d", dayNumber),
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (isSelected) PureWhite else TextDarkPrimary,
                fontSize = 11.5.sp
            )

            // Indicators row for received / pending dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                if (hasReceived) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) PureWhite else SuccessGreen)
                    )
                }
                if (hasPending) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) PureWhite else DangerRed)
                    )
                }
            }
        }
    }
}

/**
 * Dialog detalhado exibido ao tocar em um dia no Calendário
 * Mostra: Total recebido, total a receber, total ganho, quantidade de clientes, pagamentos realizados, novas pendências.
 */
@Composable
fun DayDetailModalSheet(
    summary: DayFinanceSummary,
    onDismiss: () -> Unit,
    viewModel: FiadoViewModel,
    onAddPaymentClick: () -> Unit,
    onAddDebtClick: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("day_detail_modal_dialog"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Movimentações do Dia",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextDarkPrimary,
                            fontSize = 18.sp
                        )
                        Text(
                            text = summary.formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = TextDarkSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4 Metric Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Recebido
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SuccessGreenLight)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Recebido", fontSize = 10.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
                            Text(
                                text = viewModel.formatCurrency(summary.totalReceived),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = SuccessGreen
                            )
                        }
                    }

                    // A Receber
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DangerRedLight)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("A Receber", fontSize = 10.sp, color = DangerRed, fontWeight = FontWeight.Bold)
                            Text(
                                text = viewModel.formatCurrency(summary.totalPending),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = DangerRed
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Total Ganho
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceSecondary)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Total Ganho", fontSize = 10.sp, color = PrimaryBlueMain, fontWeight = FontWeight.Bold)
                            Text(
                                text = viewModel.formatCurrency(summary.totalEarned),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = PrimaryBlueDark
                            )
                        }
                    }

                    // Clientes
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceSubtle)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Clientes", fontSize = 10.sp, color = TextDarkSecondary, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${summary.clientsCount} cliente(s)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = TextDarkPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // List of Activities
                Text(
                    text = "Registros desta data",
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (summary.payments.isEmpty() && summary.debts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhuma movimentação financeira nesta data.",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        summary.payments.forEach { p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceSubtle)
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Pagamento Recebido", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDarkPrimary)
                                        Text(p.notes ?: "Recebimento confirmado", fontSize = 10.sp, color = TextMuted)
                                    }
                                }
                                Text(
                                    text = viewModel.formatCurrency(p.value),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = SuccessGreen
                                )
                            }
                        }

                        summary.debts.forEach { d ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceSubtle)
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = DangerRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(d.description, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDarkPrimary)
                                        Text("Status: ${d.status}", fontSize = 10.sp, color = TextMuted)
                                    }
                                }
                                Text(
                                    text = viewModel.formatCurrency(d.value),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = TextDarkPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onAddDebtClick()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlueMain)
                    ) {
                        Text("Nova Pendência", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            onDismiss()
                            onAddPaymentClick()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlueMain)
                    ) {
                        Text("Registrar Pgto", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Modal exibido ao tocar nos 3 botões do Card Principal:
 * TOTAL GANHO | VALORES A RECEBER | VALORES RECEBIDOS
 */
@Composable
fun ValueDetailDialog(
    type: String, // "ganho", "a_receber", "recebidos"
    onDismiss: () -> Unit,
    debts: List<Debt>,
    payments: List<Payment>,
    clients: List<Client>,
    viewModel: FiadoViewModel
) {
    val title = when (type) {
        "ganho" -> "Detalhamento: Total Ganho (Mês Atual)"
        "a_receber" -> "Detalhamento: Valores a Receber"
        else -> "Detalhamento: Histórico Total Recebido"
    }

    val subtitle = when (type) {
        "ganho" -> "Recebido no Mês Atual"
        "a_receber" -> "Total Pendente Acumulado"
        else -> "Histórico Geral Recebido"
    }

    val totalAmount = when (type) {
        "ganho" -> viewModel.calculateTotalGanhoCurrentMonth(payments)
        "a_receber" -> viewModel.calculateTotalAReceber(debts)
        else -> viewModel.calculateTotalReceivedHistory(payments)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextDarkPrimary,
                        fontSize = 16.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            when (type) {
                                "ganho" -> SuccessGreenLight
                                "a_receber" -> DangerRedLight
                                else -> SurfaceSecondary
                            }
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Text(subtitle, fontSize = 11.5.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = viewModel.formatCurrency(totalAmount),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = when (type) {
                                "ganho" -> SuccessGreen
                                "a_receber" -> DangerRed
                                else -> PrimaryBlueDark
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Registros Correspondentes", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDarkPrimary)
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (type == "ganho") {
                        val currentMonthPayments = viewModel.getPaymentsForCurrentMonth(payments)
                        if (currentMonthPayments.isEmpty()) {
                            Text(
                                text = "Nenhum recebimento registrado neste mês (R$ 0,00).",
                                color = TextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            currentMonthPayments.forEach { p ->
                                val clientName = clients.firstOrNull { it.id == p.clientId }?.name ?: "Cliente #${p.clientId}"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SurfaceSubtle)
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(clientName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDarkPrimary)
                                        Text(viewModel.formatDate(if (p.receivedAt > 0) p.receivedAt else p.createdAt), fontSize = 11.sp, color = TextMuted)
                                    }
                                    Text(
                                        "+ ${viewModel.formatCurrency(p.value)}",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SuccessGreen,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    } else if (type == "a_receber") {
                        val pendingDebts = viewModel.getPendingDebts(debts)
                        if (pendingDebts.isEmpty()) {
                            Text(
                                text = "Nenhum valor pendente a receber no momento.",
                                color = TextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            pendingDebts.forEach { d ->
                                val clientName = clients.firstOrNull { it.id == d.clientId }?.name ?: "Cliente #${d.clientId}"
                                val pendingVal = (d.value - d.paidValue).coerceAtLeast(0.0)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SurfaceSubtle)
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(clientName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDarkPrimary)
                                        Text("${d.description} • ${viewModel.formatDate(d.createdAt)}", fontSize = 11.sp, color = TextMuted)
                                    }
                                    Text(
                                        viewModel.formatCurrency(pendingVal),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DangerRed,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    } else {
                        // "recebidos" (todos os pagamentos)
                        if (payments.isEmpty()) {
                            Text(
                                text = "Nenhum pagamento registrado no histórico.",
                                color = TextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            payments.forEach { p ->
                                val clientName = clients.firstOrNull { it.id == p.clientId }?.name ?: "Cliente #${p.clientId}"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SurfaceSubtle)
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(clientName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDarkPrimary)
                                        Text(viewModel.formatDate(if (p.receivedAt > 0) p.receivedAt else p.createdAt), fontSize = 11.sp, color = TextMuted)
                                    }
                                    Text(viewModel.formatCurrency(p.value), fontWeight = FontWeight.ExtraBold, color = SuccessGreen, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlueMain)
                ) {
                    Text("Fechar Detalhes", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Modal exibido ao clicar em "Ver todos" na seção Clientes Devedores
 */
@Composable
fun AllDebtorsDialog(
    debtors: List<DebtorSummary>,
    onDismiss: () -> Unit,
    onClientClick: (Int) -> Unit,
    viewModel: FiadoViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = debtors.filter {
        it.client.name.contains(searchQuery, ignoreCase = true) ||
        it.client.phone.contains(searchQuery)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(8.dp),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Todos os Devedores",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextDarkPrimary,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Ordenados pelo maior tempo de atraso",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar cliente devedor...", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlueMain,
                        unfocusedBorderColor = BorderSubtle,
                        focusedContainerColor = SurfaceSubtle,
                        unfocusedContainerColor = SurfaceSubtle
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered) { debtor ->
                        DebtorRowCard(
                            debtor = debtor,
                            onClick = {
                                onDismiss()
                                onClientClick(debtor.client.id)
                            },
                            viewModel = viewModel
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlueMain)
                ) {
                    Text("Concluído", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
