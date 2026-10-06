package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Client
import com.example.data.Debt
import com.example.data.Payment
import com.example.ui.components.DayDetailModalSheet
import com.example.ui.components.DebtorRowCard
import com.example.ui.components.FinancialCalendarSection
import com.example.ui.theme.*
import com.example.viewmodel.DayFinanceSummary
import com.example.viewmodel.FiadoViewModel
import com.example.viewmodel.Screen

@Composable
fun CalendarScreen(viewModel: FiadoViewModel) {
    val clients by viewModel.clientsFlow.collectAsStateWithLifecycle()
    val debts by viewModel.debtsFlow.collectAsStateWithLifecycle()
    val payments by viewModel.paymentsFlow.collectAsStateWithLifecycle()

    var selectedDaySummary by remember { mutableStateOf<DayFinanceSummary?>(null) }
    var filterType by remember { mutableStateOf("todos") } // "todos", "recebidos", "pendentes"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .testTag("calendar_screen_scroll"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        item {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryBlueMain),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Calendário",
                        tint = PureWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Calendário Financeiro",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextDarkPrimary,
                        fontSize = 20.sp
                    )
                    Text(
                        text = "Acompanhamento diário de movimentações",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Calendar Component
        item {
            FinancialCalendarSection(
                viewModel = viewModel,
                clients = clients,
                debts = debts,
                payments = payments,
                onDayClick = { selectedDaySummary = it }
            )
        }

        // Filter Chips: Todos | Recebidos | Pendências
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "todos" to "Todos os Dias",
                    "recebidos" to "Dias com Recebimentos",
                    "pendentes" to "Dias com Pendências"
                ).forEach { (key, label) ->
                    val isSelected = filterType == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { filterType = key },
                        label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlueMain,
                            selectedLabelColor = PureWhite,
                            containerColor = SurfaceWhite,
                            labelColor = TextDarkPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSelected) PrimaryBlueMain else BorderSubtle,
                            enabled = true,
                            selected = isSelected
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // Month Movement Summary List
        item {
            Text(
                text = "Resumo das Datas de ${viewModel.getMonthNameFormatted()}",
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }

        val totalDays = viewModel.getDaysInCurrentMonth()
        val allDaysSummaries = (1..totalDays).map { dayNum ->
            viewModel.getDayFinanceSummary(dayNum, clients, debts, payments)
        }.filter { summary ->
            when (filterType) {
                "recebidos" -> summary.totalReceived > 0
                "pendentes" -> summary.totalPending > 0
                else -> summary.totalReceived > 0 || summary.totalPending > 0
            }
        }

        if (allDaysSummaries.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhuma movimentação para o filtro selecionado neste mês.",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(allDaysSummaries) { daySummary ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .clickable { selectedDaySummary = daySummary },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceSecondary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = String.format("%02d", daySummary.dayNumber),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PrimaryBlueDark,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = daySummary.formattedDate,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDarkPrimary,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "${daySummary.clientsCount} cliente(s) • ${daySummary.payments.size} pgto(s)",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            if (daySummary.totalReceived > 0) {
                                Text(
                                    text = "+${viewModel.formatCurrency(daySummary.totalReceived)}",
                                    color = SuccessGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            if (daySummary.totalPending > 0) {
                                Text(
                                    text = viewModel.formatCurrency(daySummary.totalPending),
                                    color = DangerRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedDaySummary?.let { summary ->
        DayDetailModalSheet(
            summary = summary,
            onDismiss = { selectedDaySummary = null },
            viewModel = viewModel,
            onAddPaymentClick = { selectedDaySummary = null },
            onAddDebtClick = { selectedDaySummary = null }
        )
    }
}

@Composable
fun ReportsScreen(viewModel: FiadoViewModel) {
    val clients by viewModel.clientsFlow.collectAsStateWithLifecycle()
    val debts by viewModel.debtsFlow.collectAsStateWithLifecycle()
    val payments by viewModel.paymentsFlow.collectAsStateWithLifecycle()

    val totalReceived = remember(payments) { viewModel.calculateTotalReceivedHistory(payments) }
    val totalPending = remember(debts) { viewModel.calculateTotalAReceber(debts) }
    val totalGanhoCurrentMonth = remember(payments) { viewModel.calculateTotalGanhoCurrentMonth(payments) }
    val debtors = remember(clients, debts) { viewModel.getDebtorsSortedByDelay(clients, debts) }

    val totalActiveVolume = totalReceived + totalPending
    val delinquencyRate = if (totalActiveVolume > 0) (totalPending / totalActiveVolume) * 100 else 0.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .testTag("reports_screen_scroll"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryBlueMain),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "Relatórios",
                        tint = PureWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Relatórios Financeiros",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextDarkPrimary,
                        fontSize = 20.sp
                    )
                    Text(
                        text = "Indicadores de recebimento e inadimplência",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Summary Cards
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SuccessGreenLight),
                        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Total Ganho (Mês)", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(viewModel.formatCurrency(totalGanhoCurrentMonth), color = SuccessGreen, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = DangerRedLight),
                        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("A Receber", color = DangerRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(viewModel.formatCurrency(totalPending), color = DangerRed, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceSecondary),
                    border = BorderStroke(1.dp, BorderSoftBlue)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Índice de Inadimplência", color = PrimaryBlueMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("${debtors.size} clientes com débitos ativos", color = TextMuted, fontSize = 11.sp)
                        }
                        Text(
                            text = String.format("%.1f%%", delinquencyRate),
                            color = PrimaryBlueDark,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Inadimplência por faixa de atraso
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Inadimplência por Tempo de Atraso",
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                val critical = debtors.filter { it.oldestDebtDays >= 30 }
                val warning = debtors.filter { it.oldestDebtDays in 14..29 }
                val recent = debtors.filter { it.oldestDebtDays < 14 }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SeverityBadge(modifier = Modifier.weight(1f), label = "> 30 dias", count = critical.size, color = DangerRed, bg = DangerRedLight)
                    SeverityBadge(modifier = Modifier.weight(1f), label = "14-29 dias", count = warning.size, color = WarningAmber, bg = WarningAmberLight)
                    SeverityBadge(modifier = Modifier.weight(1f), label = "< 14 dias", count = recent.size, color = PrimaryBlueMain, bg = SurfaceSecondary)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Ranking of Debtors by Delay
        item {
            Text(
                text = "Prioridade de Cobrança (Mais Antigas Primeiro)",
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }

        if (debtors.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                ) {
                    Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                        Text("Sem clientes devedores no momento.", color = TextMuted)
                    }
                }
            }
        } else {
            items(debtors) { debtor ->
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    DebtorRowCard(
                        debtor = debtor,
                        onClick = { viewModel.navigateTo(Screen.ClientDetails(debtor.client.id)) },
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun SeverityBadge(
    modifier: Modifier = Modifier,
    label: String,
    count: Int,
    color: Color,
    bg: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("$count clientes", color = color, fontSize = 14.sp, fontWeight = FontWeight.Black)
        }
    }
}
