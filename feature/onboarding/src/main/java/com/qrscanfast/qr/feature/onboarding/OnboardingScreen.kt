package com.qrscanfast.qr.feature.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qrscanfast.qr.feature.onboarding.model.OnboardingPage
import kotlinx.coroutines.launch

import androidx.compose.ui.tooling.preview.Preview
import com.qrscanfast.qr.core.ui.theme.FastQrScanTheme

/**
 * Main Onboarding Screen featuring a 3-page introduction.
 *
 * Uses [HorizontalPager] for swipe transitions and displays a progress indicator,
 * a "Skip" button, and dynamic navigation buttons ("Next" or "Get Started").
 *
 * @param viewModel Hilt-provided ViewModel for onboarding logic.
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
            iconRes = 0 // Using icons for now
        ),
        OnboardingPage(
            titleRes = R.string.onboarding_title_2,
            descriptionRes = R.string.onboarding_description_2,
            iconRes = 0
        ),
        OnboardingPage(
            titleRes = R.string.onboarding_title_3,
            descriptionRes = R.string.onboarding_description_3,
            iconRes = 0
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                if (pagerState.currentPage < pages.size - 1) {
                    TextButton(onClick = onComplete) {
                        Text(text = stringResource(id = R.string.onboarding_skip))
                    }
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page Indicator
                Row(
                    Modifier
                        .height(50.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pages.size) { iteration ->
                        val color = if (pagerState.currentPage == iteration) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        }
                        Surface(
                            modifier = Modifier
                                .padding(2.dp)
                                .size(if (pagerState.currentPage == iteration) 10.dp else 8.dp),
                            shape = MaterialTheme.shapes.extraLarge,
                            color = color
                        ) {}
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (pagerState.currentPage < pages.size - 1) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onComplete()
                        }
                    }
                ) {
                    Text(
                        text = if (pagerState.currentPage < pages.size - 1) {
                            stringResource(id = R.string.onboarding_next)
                        } else {
                            stringResource(id = R.string.onboarding_get_started)
                        }
                    )
                }
            }
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
                icon = when (pageIndex) {
                    0 -> Icons.Default.Home
                    1 -> Icons.Default.Add
                    else -> Icons.Default.Star
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SimplePreview() {
    Text("Hello Onboarding")
}

@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreview() {
    FastQrScanTheme {
        OnboardingScreenContent(onComplete = {})
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    icon: ImageVector
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(160.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = stringResource(id = page.titleRes),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(id = page.descriptionRes),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
