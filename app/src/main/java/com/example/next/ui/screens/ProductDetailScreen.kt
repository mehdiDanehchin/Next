package com.example.next.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.next.R
import com.example.next.di.AppContainer
import com.example.next.ui.theme.*
import com.example.next.viewmodel.ProductDetailEvent
import com.example.next.viewmodel.ProductDetailViewModel

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProductDetailScreen(
    container: AppContainer,
    onBack: () -> Unit,
    fromFeatured: Boolean = false,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedContentScope
) {
    val viewModel: ProductDetailViewModel = viewModel(factory = container.productDetailViewModelFactory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val customColors = LocalCustomColors.current
    val colorScheme = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()

    // One-shot events (toasts) from the ViewModel.
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ProductDetailEvent.Toast -> {
                    val message = if (event.formatArg != null) {
                        context.getString(event.messageRes, event.formatArg)
                    } else {
                        context.getString(event.messageRes)
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = context.getString(R.string.loading_product),
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }
        return
    }

    if (uiState.errorRes != null || uiState.product == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_image_placeholder),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = customColors.textHint
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = uiState.errorRes?.let { context.getString(it) }
                        ?: context.getString(R.string.product_not_found),
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onBack,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text(
                        text = context.getString(R.string.go_back),
                        color = customColors.textOnPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }
        return
    }

    val p = uiState.product!!
    val isFavorite = uiState.isFavorite

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(customColors.surfaceWhite)
                .padding(start = 8.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_down),
                    contentDescription = context.getString(R.string.cd_back),
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified
                )
            }
            Text(
                text = context.getString(R.string.app_name),
                color = colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { viewModel.toggleFavorite() }, modifier = Modifier.size(40.dp)) {
                Icon(
                    painter = painterResource(
                        if (isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart
                    ),
                    contentDescription = context.getString(R.string.cd_favorite),
                    modifier = Modifier.size(28.dp),
                    tint = Color.Unspecified
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            ProductImage(
                imageName = p.imageUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(customColors.surfaceWhite)
                    // Same per-section key as the home thumbnail that was tapped:
                    // the image morphs between the two screens instead of fading.
                    // (Keys must be unique within the SharedTransitionScope, so
                    // the featured row and the grid use different prefixes.)
                    .let { base ->
                        with(sharedTransitionScope) {
                            base.sharedElement(
                                rememberSharedContentState(
                                    key = if (fromFeatured) {
                                        "featured_product_image_${p.id}"
                                    } else {
                                        "grid_product_image_${p.id}"
                                    }
                                ),
                                animatedVisibilityScope
                            )
                        }
                    },
                contentScale = ContentScale.Fit
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-16).dp)
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = customColors.surfaceWhite)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = p.name,
                        color = colorScheme.onSurface,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = p.formattedPrice,
                        color = Primary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = p.category,
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = context.getString(R.string.description),
                        color = colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = p.description.ifEmpty { context.getString(R.string.no_description) },
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = context.getString(R.string.specifications),
                        color = colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        colors = CardDefaults.cardColors(containerColor = colorScheme.background)
                    ) {
                        Text(
                            text = p.specifications.ifEmpty { context.getString(R.string.no_specifications) },
                            color = colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 19.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 8.dp,
            color = customColors.surfaceWhite
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { viewModel.toggleFavorite() },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = customColors.primaryLight)
                ) {
                    Text(
                        text = if (isFavorite) context.getString(R.string.in_wishlist) else context.getString(R.string.add_to_wishlist),
                        color = customColors.textOnPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = { viewModel.addToCart() },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text(
                        text = context.getString(R.string.add_to_cart),
                        color = customColors.textOnPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }
    }
}