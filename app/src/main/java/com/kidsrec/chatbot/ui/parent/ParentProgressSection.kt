package com.kidsrec.chatbot.ui.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ParentProgressSection(
    viewModel: ParentProgressViewModel,
    childUserId: String
) {
    LaunchedEffect(childUserId) {
        viewModel.observeChildProgress(childUserId)
    }

    val report by viewModel.weeklyReport.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Learning Progress Tracker",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "A simple weekly snapshot of reading, videos, topics, and growth.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PastelSummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Books Read",
                    value = report.booksRead.toString(),
                    icon = Icons.Default.Book,
                    containerColor = Color(0xFFE8F0FE),
                    contentColor = Color(0xFF1A73E8)
                )

                PastelSummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Videos Watched",
                    value = report.videosWatched.toString(),
                    icon = Icons.Default.PlayCircle,
                    containerColor = Color(0xFFFFF1E6),
                    contentColor = Color(0xFFEF6C00)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PastelSummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Topics",
                    value = report.topicsExplored.size.toString(),
                    icon = Icons.Default.Topic,
                    containerColor = Color(0xFFEAF7EE),
                    contentColor = Color(0xFF2E7D32)
                )

                PastelSummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Growth",
                    value = report.readingLevelGrowth,
                    icon = Icons.Default.TrendingUp,
                    containerColor = Color(0xFFF3E8FD),
                    contentColor = Color(0xFF8E24AA)
                )
            }
        }

        item {
            DashboardCard(
                title = "Weekly Report",
                icon = Icons.Default.BarChart
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF8F5FF),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Top Topic",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = report.topTopic.ifBlank { "No topic explored yet" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                SimpleWeeklyBarChart(
                    booksRead = report.booksRead,
                    videosWatched = report.videosWatched,
                    topicsCount = report.topicsExplored.size
                )
            }
        }

        item {
            DashboardCard(
                title = "Reading Level Growth",
                icon = Icons.Default.AutoGraph
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SoftInfoCard(
                        modifier = Modifier.weight(1f),
                        label = "Average Score",
                        value = if (report.averageReadingLevelScore == 0.0) "0.0" else String.format("%.1f", report.averageReadingLevelScore)
                    )
                    SoftInfoCard(
                        modifier = Modifier.weight(1f),
                        label = "Trend",
                        value = report.readingLevelGrowth
                    )
                }
            }
        }

        item {
            DashboardCard(
                title = "Topics Explored",
                icon = Icons.Default.Topic
            ) {
                if (report.topicsExplored.isEmpty()) {
                    EmptyStateText("No topics explored yet.")
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(report.topicsExplored) { topic ->
                            AssistChip(
                                onClick = { },
                                label = { Text(topic) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = Color(0xFFF3E8FD),
                                    labelColor = Color(0xFF5E35B1)
                                )
                            )
                        }
                    }
                }
            }
        }

        item {
            DashboardCard(
                title = "Recent Activity",
                icon = Icons.Default.BarChart
            ) {
                if (report.recentEvents.isEmpty()) {
                    EmptyStateText("No recent activity yet.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        report.recentEvents.forEachIndexed { index, event ->
                            ActivityRow(
                                title = event.title.ifBlank { event.topic },
                                subtitle = event.type.replace("_", " "),
                                contentType = event.contentType
                            )

                            if (index != report.recentEvents.lastIndex) {
                                Divider(
                                    modifier = Modifier.padding(top = 2.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
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
private fun DashboardCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFFBFF)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEDE7F6)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFF5E35B1),
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun PastelSummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.75f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
private fun SoftInfoCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF7F3FF)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SimpleWeeklyBarChart(
    booksRead: Int,
    videosWatched: Int,
    topicsCount: Int
) {
    val maxValue = maxOf(booksRead, videosWatched, topicsCount, 1)

    Column {
        Text(
            text = "This Week",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            ChartBar(
                label = "Books",
                value = booksRead,
                maxValue = maxValue,
                barColor = Color(0xFF90CAF9)
            )
            ChartBar(
                label = "Videos",
                value = videosWatched,
                maxValue = maxValue,
                barColor = Color(0xFFFFCC80)
            )
            ChartBar(
                label = "Topics",
                value = topicsCount,
                maxValue = maxValue,
                barColor = Color(0xFFA5D6A7)
            )
        }
    }
}

@Composable
private fun ChartBar(
    label: String,
    value: Int,
    maxValue: Int,
    barColor: Color
) {
    val barHeight = if (maxValue == 0) 0f else (value.toFloat() / maxValue.toFloat()) * 110f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .width(38.dp)
                .height(110.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF1F3F4))
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(barColor)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ActivityRow(
    title: String,
    subtitle: String,
    contentType: String
) {
    val icon = when (contentType) {
        "BOOK" -> Icons.Default.Book
        "VIDEO" -> Icons.Default.PlayCircle
        else -> Icons.Default.Topic
    }

    val iconBg = when (contentType) {
        "BOOK" -> Color(0xFFE8F0FE)
        "VIDEO" -> Color(0xFFFFF1E6)
        else -> Color(0xFFEAF7EE)
    }

    val iconTint = when (contentType) {
        "BOOK" -> Color(0xFF1A73E8)
        "VIDEO" -> Color(0xFFEF6C00)
        else -> Color(0xFF2E7D32)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            color = iconBg,
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(10.dp),
                tint = iconTint
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyStateText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}