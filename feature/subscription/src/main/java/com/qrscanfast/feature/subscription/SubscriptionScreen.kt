package com.qrscanfast.feature.subscription

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * 订阅页面 — 模仿 PRO vs BASIC 对比表风格。
 * 仅展示 3 天试用 + $6.99/周 方案（单一 CTA，提升转化率）。
 */
@Composable
fun SubscriptionScreen(
    onDismiss: () -> Unit,
    onPurchaseSuccess: () -> Unit,
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val purchaseState by viewModel.purchaseState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // 处理购买状态反馈
    LaunchedEffect(purchaseState) {
        when (purchaseState) {
            is PurchaseUiState.Success, is PurchaseUiState.RestoreSuccess -> {
                onPurchaseSuccess()
                viewModel.resetPurchaseState()
            }
            is PurchaseUiState.Error -> {
                snackbarHostState.showSnackbar(
                    (purchaseState as PurchaseUiState.Error).message
                )
                viewModel.resetPurchaseState()
            }
            is PurchaseUiState.RestoreEmpty -> {
                snackbarHostState.showSnackbar(
                    context.getString(R.string.sub_no_purchase_found)
                )
                viewModel.resetPurchaseState()
            }
            is PurchaseUiState.Cancelled -> {
                viewModel.resetPurchaseState()
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE8F5F2),
                        Color(0xFFFFFFFF)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // 关闭按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        tint = Color(0xFF333333)
                    )
                }
            }

            // 可滚动内容
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // 顶部图标
                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    color = Color(0xFF2DB89A)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 标题
                Text(
                    text = stringResource(R.string.sub_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1A1A1A),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.sub_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF888888),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // PRO / BASIC 表头
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    // PRO 标签
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF2DB89A)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.sub_pro),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Text(
                        text = stringResource(R.string.sub_basic),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF888888)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE))

                // 功能对比列表
                FeatureComparisonRow(stringResource(R.string.sub_feature_scan), proEnabled = true, basicEnabled = true)
                FeatureComparisonRow(stringResource(R.string.sub_feature_create), proEnabled = true, basicEnabled = true)
                FeatureComparisonRow(stringResource(R.string.sub_feature_ai), proEnabled = true, basicEnabled = false)
                FeatureComparisonRow(stringResource(R.string.sub_feature_batch), proEnabled = true, basicEnabled = false)
                FeatureComparisonRow(stringResource(R.string.sub_feature_wifi), proEnabled = true, basicEnabled = false)
                FeatureComparisonRow(stringResource(R.string.sub_feature_product), proEnabled = true, basicEnabled = false)
                FeatureComparisonRow(stringResource(R.string.sub_feature_history), proEnabled = true, basicEnabled = false)
                FeatureComparisonRow(stringResource(R.string.sub_feature_no_ads), proEnabled = true, basicEnabled = false)

                Spacer(modifier = Modifier.height(24.dp))
            }

            // 底部固定区域
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 价格说明
                Text(
                    text = stringResource(R.string.sub_pricing_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF2DB89A),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // CTA 按钮
                Button(
                    onClick = {
                        viewModel.selectPlan(com.qrscanfast.core.domain.model.SubscriptionPlan.TRIAL)
                        (context as? Activity)?.let { activity ->
                            viewModel.confirmPurchase(activity)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    enabled = purchaseState !is PurchaseUiState.Loading,
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2DB89A),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF2DB89A).copy(alpha = 0.5f)
                    )
                ) {
                    if (purchaseState is PurchaseUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = stringResource(R.string.sub_cta_button),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("›", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 条款（紧凑单段）
                Text(
                    text = stringResource(R.string.sub_terms_text),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFAAAAAA),
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp,
                    fontSize = 10.sp
                )
            }
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

/**
 * 功能对比行：功能名称 + PRO 勾/叉 + BASIC 勾/叉
 */
@Composable
private fun FeatureComparisonRow(
    featureName: String,
    proEnabled: Boolean,
    basicEnabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 功能名称
        Text(
            text = featureName,
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF333333),
            modifier = Modifier.weight(1f)
        )

        // PRO 列
        FeatureIcon(enabled = proEnabled)

        Spacer(modifier = Modifier.width(32.dp))

        // BASIC 列
        FeatureIcon(enabled = basicEnabled)

        Spacer(modifier = Modifier.width(8.dp))
    }

    HorizontalDivider(color = Color(0xFFF5F5F5))
}

@Composable
private fun FeatureIcon(enabled: Boolean) {
    if (enabled) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = Color(0xFF2DB89A)
        )
    } else {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = Color(0xFFFF4444)
        )
    }
}
