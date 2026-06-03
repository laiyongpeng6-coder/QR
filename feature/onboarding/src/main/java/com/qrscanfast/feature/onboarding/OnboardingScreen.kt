package com.qrscanfast.feature.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qrscanfast.core.ui.theme.FastQrScanTheme
import com.qrscanfast.feature.onboarding.model.OnboardingPage
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview

/**
 * Onboarding 主页面。
 *
 * ## AI 交接
 * - 职责：展示三页引导内容，并在完成后写入持久化状态。
 * - 当前状态：已改成品牌化卡片布局，适合继续迭代为首启产品页。
 * - 依赖：`OnboardingViewModel`、`OnboardingPage`、`core/ui` 按钮组件。
 * - 安全修改范围：页面结构、视觉层次、引导文案、页内装饰。
 * - 风险 / TODO：如果以后增加页数或首启策略，需要同步底部文案和进度展示。
 */
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    OnboardingScreenContent(
        onComplete = { viewModel.completeOnboarding() }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OnboardingScreenContent(
    onComplete: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            titleRes = R.string.onboarding_title_1,
            descriptionRes = R.string.onboarding_description_1,
            icon = Icons.Filled.QrCodeScanner,
            accentColor = Color(0xFF0D6B5D),
            accentColorSecondary = Color(0xFF7AF8E2)
        ),
        OnboardingPage(
            titleRes = R.string.onboarding_title_2,
            descriptionRes = R.string.onboarding_description_2,
            icon = Icons.Filled.Share,
            accentColor = Color(0xFF2257A6),
            accentColorSecondary = Color(0xFFB9D7FF)
        ),
        OnboardingPage(
            titleRes = R.string.onboarding_title_3,
            descriptionRes = R.string.onboarding_description_3,
            icon = Icons.Filled.AutoFixHigh,
            accentColor = Color(0xFF6A2DD8),
            accentColorSecondary = Color(0xFFE2C8FF)
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()
    val currentPage = pagerState.currentPage
    val currentPageData = pages[currentPage]

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            OnboardingTopBar(
                pageNumber = currentPage + 1,
                totalPages = pages.size,
                accentColor = currentPageData.accentColor,
                onSkip = onComplete
            )
        },
        bottomBar = {
            OnboardingBottomBar(
                pagerState = pagerState,
                totalPages = pages.size,
                accentColor = currentPageData.accentColor,
                onNext = {
                    if (pagerState.currentPage < pages.size - 1) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onComplete()
                    }
                }
            )
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { pageIndex ->
            OnboardingPageContent(
                page = pages[pageIndex],
                pageIndex = pageIndex,
                totalPages = pages.size
            )
        }
    }
}

@Composable
private fun OnboardingTopBar(
    pageNumber: Int,
    totalPages: Int,
    accentColor: Color,
    onSkip: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = accentColor.copy(alpha = 0.12f)
        ) {
            Text(
                text = stringResource(R.string.onboarding_brand),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = accentColor,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "$pageNumber/$totalPages",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.weight(1f))

        TextButton(onClick = onSkip) {
            Text(text = stringResource(id = R.string.onboarding_skip))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OnboardingBottomBar(
    pagerState: androidx.compose.foundation.pager.PagerState,
    totalPages: Int,
    accentColor: Color,
    onNext: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shadowElevation = 14.dp,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(totalPages) { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (selected) 12.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) accentColor else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.onboarding_swipe_hint),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (pagerState.currentPage < totalPages - 1) {
                        stringResource(id = R.string.onboarding_next)
                    } else {
                        stringResource(id = R.string.onboarding_get_started)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    pageIndex: Int,
    totalPages: Int
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        page.accentColor.copy(alpha = 0.18f),
                        MaterialTheme.colorScheme.background,
                        page.accentColorSecondary.copy(alpha = 0.14f)
                    )
                )
            )
    ) {
        DecorativeOrb(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 24.dp, end = 20.dp),
            color = page.accentColorSecondary
        )

        DecorativeOrb(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = 100.dp)
                .size(180.dp),
            color = page.accentColor
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = page.accentColor.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = page.accentColor.copy(alpha = 0.18f)
                )
            ) {
                Text(
                    text = "${pageIndex + 1}/$totalPages",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = page.accentColor,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            HeroArtwork(page = page)

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(id = page.titleRes),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(id = page.descriptionRes),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    FeatureChipRow(pageIndex = pageIndex)
                }
            }
        }
    }
}

@Composable
private fun HeroArtwork(page: OnboardingPage) {
    Box(
        modifier = Modifier.size(270.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.size(270.dp),
            shape = CircleShape,
            color = page.accentColor.copy(alpha = 0.14f)
        ) {}

        Surface(
            modifier = Modifier
                .size(220.dp)
                .offset(y = 6.dp),
            shape = CircleShape,
            color = page.accentColorSecondary.copy(alpha = 0.18f)
        ) {}

        Surface(
            modifier = Modifier
                .size(150.dp)
                .border(
                    width = 1.dp,
                    color = page.accentColor.copy(alpha = 0.10f),
                    shape = CircleShape
                ),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "QR",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = page.accentColor.copy(alpha = 0.22f)
                )
                androidx.compose.material3.Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = page.accentColor
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-10).dp, y = 18.dp),
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            shadowElevation = 4.dp
        ) {
            Text(
                text = "FAST",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = page.accentColor
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 14.dp, y = (-18).dp),
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            shadowElevation = 4.dp
        ) {
            Text(
                text = "3 STEPS",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = page.accentColor
            )
        }
    }
}

@Composable
private fun FeatureChipRow(pageIndex: Int) {
    val featureLabels = when (pageIndex) {
        0 -> listOf(
            R.string.onboarding_feature_fast,
            R.string.onboarding_feature_private,
            R.string.onboarding_feature_secure
        )
        1 -> listOf(
            R.string.onboarding_feature_multi,
            R.string.onboarding_feature_share,
            R.string.onboarding_feature_contacts
        )
        else -> listOf(
            R.string.onboarding_feature_style,
            R.string.onboarding_feature_logo,
            R.string.onboarding_feature_export
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        featureLabels.forEach { labelRes ->
            AssistChip(
                onClick = {},
                label = { Text(text = stringResource(id = labelRes)) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun DecorativeOrb(
    modifier: Modifier,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = color.copy(alpha = 0.20f)
    ) {}
}

@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreview() {
    FastQrScanTheme {
        OnboardingScreenContent(onComplete = {})
    }
}
