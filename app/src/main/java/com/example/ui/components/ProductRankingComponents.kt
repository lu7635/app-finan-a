package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Debt
import com.example.data.Product
import com.example.ui.theme.*
import com.example.viewmodel.BestSellingProduct
import com.example.viewmodel.FiadoViewModel

/**
 * Seção "Produtos mais vendidos" para a aba Produtos.
 * Exibe os produtos com maior volume de vendas em ordem decrescente.
 */
@Composable
fun BestSellingProductsSection(
    ranking: List<BestSellingProduct>,
    onViewAllClick: () -> Unit,
    onProductClick: (Product) -> Unit = {},
    viewModel: FiadoViewModel
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("best_selling_products_section"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, BorderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Título + "Ver todos"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🥇",
                            fontSize = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Produtos mais vendidos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlueDark,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Classificação por unidades vendidas",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                if (ranking.isNotEmpty()) {
                    TextButton(
                        onClick = onViewAllClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("btn_view_all_best_sellers")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Ver todos",
                                color = PrimaryBlueMain,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Ver todos",
                                tint = PrimaryBlueMain,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (ranking.isEmpty()) {
                // Estado vazio quando nenhum produto tem vendas registradas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceSubtle)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = TextMuted.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Ainda não há vendas registradas.",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextDarkSecondary
                        )
                        Text(
                            text = "Conforme você lançar fiados e débitos de produtos para os clientes, o ranking dos mais vendidos aparecerá aqui automaticamente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                // Listagem dos produtos mais vendidos (Top 4 no card principal)
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ranking.take(4).forEach { item ->
                        BestSellingProductCard(
                            item = item,
                            onClick = { onProductClick(item.product) },
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card individual de produto no ranking com destaques para Top 3.
 */
@Composable
fun BestSellingProductCard(
    item: BestSellingProduct,
    onClick: () -> Unit = {},
    viewModel: FiadoViewModel
) {
    val rank = item.rank

    // Estilos visuais especiais para o Top 3
    val rankBorder = when (rank) {
        1 -> BorderStroke(1.5.dp, Color(0xFFF59E0B))
        2 -> BorderStroke(1.dp, Color(0xFF94A3B8))
        3 -> BorderStroke(1.dp, Color(0xFFD97706))
        else -> BorderStroke(1.dp, Color(0xFFE2E8F0))
    }

    val rankCardBg = when (rank) {
        1 -> Color(0xFFFFFBEB) // Fundo sutilmente dourado para o 1º lugar
        2 -> Color(0xFFF8FAFC) // Fundo sutil prateado
        3 -> Color(0xFFFFF7ED) // Fundo sutil bronze
        else -> PureWhite
    }

    val rankBadgeBg = when (rank) {
        1 -> Color(0xFFFEF3C7)
        2 -> Color(0xFFE2E8F0)
        3 -> Color(0xFFFFEDD5)
        else -> SurfaceSubtle
    }

    val rankTextColor = when (rank) {
        1 -> Color(0xFFB45309)
        2 -> Color(0xFF475569)
        3 -> Color(0xFFC2410C)
        else -> TextDarkSecondary
    }

    val performancePillText = when (rank) {
        1 -> "🏆 1º Lugar"
        2 -> "🥈 2º Lugar"
        3 -> "🥉 3º Lugar"
        else -> "#$rank no Ranking"
    }

    val performancePillBg = when (rank) {
        1 -> Color(0xFFFEF3C7)
        2 -> Color(0xFFF1F5F9)
        3 -> Color(0xFFFFEDD5)
        else -> SurfaceSubtle
    }

    val performancePillColor = when (rank) {
        1 -> Color(0xFFB45309)
        2 -> Color(0xFF475569)
        3 -> Color(0xFFC2410C)
        else -> TextMuted
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("best_seller_item_${item.product.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = rankCardBg),
        border = rankBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = if (rank == 1) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Posição no Ranking (Número + Medalha)
            Box(
                modifier = Modifier
                    .size(if (rank == 1) 36.dp else 32.dp)
                    .clip(CircleShape)
                    .background(rankBadgeBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rank.toString(),
                    fontWeight = FontWeight.ExtraBold,
                    color = rankTextColor,
                    fontSize = if (rank == 1) 15.sp else 13.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Imagem / Ícone do Produto
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when (rank) {
                            1 -> Color(0xFFFDE68A)
                            2 -> Color(0xFFE2E8F0)
                            3 -> Color(0xFFFED7AA)
                            else -> SurfaceSecondary
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (rank) {
                        1 -> Icons.Default.WorkspacePremium
                        2 -> Icons.Default.Inventory2
                        3 -> Icons.Default.ShoppingBag
                        else -> Icons.Default.Category
                    },
                    contentDescription = item.product.name,
                    tint = when (rank) {
                        1 -> Color(0xFFB45309)
                        2 -> Color(0xFF475569)
                        3 -> Color(0xFFC2410C)
                        else -> PrimaryBlueMain
                    },
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Nome do Produto + Quantidade vendida + Faturamento
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.product.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Quantidade vendida
                    Text(
                        text = "${item.totalQuantitySold} ${if (item.totalQuantitySold == 1) "vendido" else "vendidos"}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F766E), // Teal escuro destacado
                        fontSize = 12.sp
                    )

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted.copy(alpha = 0.5f)
                    )

                    // Valor total gerado
                    Text(
                        text = viewModel.formatCurrency(item.totalRevenue),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryBlueMain,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Indicador de desempenho / Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(performancePillBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = performancePillText,
                    color = performancePillColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Tela/Dialog completo de "Ranking de Produtos".
 * Permite filtrar por período: Hoje, Esta semana, Este mês, Este ano e Todo o período.
 */
@Composable
fun ProductRankingDialog(
    products: List<Product>,
    debts: List<Debt>,
    onDismissRequest: () -> Unit,
    onProductClick: (Product) -> Unit = {},
    viewModel: FiadoViewModel
) {
    var selectedPeriod by remember { mutableStateOf("Todo o período") }

    val ranking = remember(products, debts, selectedPeriod) {
        viewModel.getBestSellingProducts(products, debts, selectedPeriod)
    }

    val totalUnitsSold = remember(ranking) {
        ranking.sumOf { it.totalQuantitySold }
    }

    val totalRevenue = remember(ranking) {
        ranking.sumOf { it.totalRevenue }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("product_ranking_dialog"),
            color = BackgroundLight
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header Bar
                Surface(
                    color = PureWhite,
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = onDismissRequest,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Fechar",
                                        tint = TextDarkPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Ranking de Produtos",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = PrimaryBlueDark,
                                        fontSize = 18.sp
                                    )
                                    Text(
                                        text = "Ordenado por quantidade total vendida",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                        fontSize = 11.5.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Chips de Filtro por Período
                        val periods = listOf("Hoje", "Esta semana", "Este mês", "Este ano", "Todo o período")
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(periods) { period ->
                                val isSelected = selectedPeriod == period
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedPeriod = period },
                                    label = {
                                        Text(
                                            text = period,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryBlueMain,
                                        selectedLabelColor = PureWhite,
                                        containerColor = SurfaceSubtle,
                                        labelColor = TextDarkSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = if (isSelected) PrimaryBlueMain else BorderSubtle
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.testTag("ranking_filter_${period.lowercase().replace(" ", "_")}")
                                )
                            }
                        }
                    }
                }

                // Resumo do período
                if (ranking.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = PrimaryBlueSubtleCompat(),
                        border = BorderStroke(1.dp, BorderSoftBlue)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Total Vendido no Período",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = PrimaryBlueMain
                                )
                                Text(
                                    text = "$totalUnitsSold unidades",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PrimaryBlueDark
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Faturamento Gerado",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = PrimaryBlueMain
                                )
                                Text(
                                    text = viewModel.formatCurrency(totalRevenue),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PrimaryBlueDark
                                )
                            }
                        }
                    }
                }

                // Lista de produtos no ranking
                if (ranking.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inbox,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = TextMuted.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "Nenhuma venda registrada para este período.",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextDarkSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Text(
                                text = "Selecione 'Todo o período' ou registre novas vendas para visualizar o ranking.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(ranking) { item ->
                            BestSellingProductCard(
                                item = item,
                                onClick = {
                                    onProductClick(item.product)
                                    onDismissRequest()
                                },
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrimaryBlueSubtleCompat(): Color = Color(0xFFEFF6FF)
