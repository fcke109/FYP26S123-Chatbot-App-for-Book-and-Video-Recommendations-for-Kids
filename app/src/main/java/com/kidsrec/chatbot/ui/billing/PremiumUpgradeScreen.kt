package com.kidsrec.chatbot.ui.billing

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kidsrec.chatbot.data.repository.PurchaseState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumUpgradeScreen(
    onBack: () -> Unit
) {
    val viewModel: BillingViewModel = hiltViewModel()
    val products by viewModel.products.collectAsState()
    val purchaseState by viewModel.purchaseState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(purchaseState) {
        if (purchaseState is PurchaseState.Success) {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upgrade to Premium") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Premium Plan",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Covers your account and all linked children",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Benefits list
            val benefits = listOf(
                "Unlimited book recommendations" to Icons.Default.MenuBook,
                "Extended screen time options" to Icons.Default.Timer,
                "Priority content approval" to Icons.Default.CheckCircle,
                "Advanced parental controls" to Icons.Default.Shield,
                "Ad-free experience" to Icons.Default.Block
            )

            benefits.forEach { (text, icon) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = text, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Price
            val product = products.firstOrNull()
            val priceText = product?.subscriptionOfferDetails?.firstOrNull()
                ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice

            if (priceText != null) {
                Text(
                    text = priceText,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "per month",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Premium subscription",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Subscription not yet available in your region.\nPlease check back later.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val activity = context as? Activity
                    if (activity != null) {
                        viewModel.purchasePremium(activity)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = product != null && purchaseState !is PurchaseState.Processing
            ) {
                if (purchaseState is PurchaseState.Processing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        if (product != null) "Upgrade Now" else "Not Available Yet",
                        fontSize = 16.sp
                    )
                }
            }

            if (purchaseState is PurchaseState.Error) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = (purchaseState as PurchaseState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
