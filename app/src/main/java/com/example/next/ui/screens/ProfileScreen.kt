package com.example.next.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsSuggest
import com.example.next.R
import com.example.next.models.ThemeMode
import com.example.next.ui.theme.*

@Composable
fun ProfileScreen(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeChanged: (ThemeMode) -> Unit = {},
    onMyOrders: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val customColors = LocalCustomColors.current
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        // Header
        Text(
            text = context.getString(R.string.nav_profile),
            color = colorScheme.onSurface,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier
                .fillMaxWidth()
                .background(customColors.surfaceWhite)
                .padding(16.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Profile Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(containerColor = customColors.surfaceWhite)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(customColors.primaryLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = context.getString(R.string.profile_avatar_letter),
                            color = Primary,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = context.getString(R.string.guest_user),
                        color = colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = context.getString(R.string.guest_email),
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Theme Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(containerColor = customColors.surfaceWhite)
            ) {
                Column(modifier = Modifier.padding(4.dp)) {
                    // Section header
                    Text(
                        text = context.getString(R.string.theme_settings),
                        color = colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
                    )

                    // Light Mode
                    ThemeOptionRow(
                        icon = { Icon(Icons.Filled.LightMode, contentDescription = null, modifier = Modifier.size(24.dp)) },
                        label = context.getString(R.string.theme_light),
                        isSelected = themeMode == ThemeMode.LIGHT,
                        onClick = { onThemeChanged(ThemeMode.LIGHT) },
                        customColors = customColors,
                        colorScheme = colorScheme
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        thickness = 0.5.dp,
                        color = customColors.divider
                    )

                    // Dark Mode
                    ThemeOptionRow(
                        icon = { Icon(Icons.Filled.DarkMode, contentDescription = null, modifier = Modifier.size(24.dp)) },
                        label = context.getString(R.string.theme_dark),
                        isSelected = themeMode == ThemeMode.DARK,
                        onClick = { onThemeChanged(ThemeMode.DARK) },
                        customColors = customColors,
                        colorScheme = colorScheme
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        thickness = 0.5.dp,
                        color = customColors.divider
                    )

                    // Follow System
                    ThemeOptionRow(
                        icon = { Icon(Icons.Filled.SettingsSuggest, contentDescription = null, modifier = Modifier.size(24.dp)) },
                        label = context.getString(R.string.theme_system),
                        isSelected = themeMode == ThemeMode.SYSTEM,
                        onClick = { onThemeChanged(ThemeMode.SYSTEM) },
                        customColors = customColors,
                        colorScheme = colorScheme
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Menu Items Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(containerColor = customColors.surfaceWhite)
            ) {
                Column(modifier = Modifier.padding(4.dp)) {
                    // My Orders
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onMyOrders)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_cart),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = context.getString(R.string.orders_title),
                            color = colorScheme.onSurface,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_down),
                            contentDescription = null,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer { rotationZ = -90f },
                            tint = customColors.textHint
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        thickness = 0.5.dp,
                        color = customColors.divider
                    )

                    // App Version
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_home),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = context.getString(R.string.app_version),
                            color = colorScheme.onSurface,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        thickness = 0.5.dp,
                        color = customColors.divider
                    )

                    // About
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_image_placeholder),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = context.getString(R.string.about_app),
                            color = colorScheme.onSurface,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_down),
                            contentDescription = null,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer { rotationZ = -90f },
                            tint = customColors.textHint
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        thickness = 0.5.dp,
                        color = customColors.divider
                    )

                    // Settings
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_up),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = context.getString(R.string.settings),
                            color = colorScheme.onSurface,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_down),
                            contentDescription = null,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer { rotationZ = -90f },
                            tint = customColors.textHint
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // About Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(containerColor = customColors.surfaceWhite)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = context.getString(R.string.about_app),
                        color = colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = context.getString(R.string.about_description),
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // Developer credit: small label + name, subtle but elegant.
                    Text(
                        text = context.getString(R.string.about_developer),
                        color = customColors.textHint,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = context.getString(R.string.about_developer_name),
                        color = Primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionRow(
    icon: @Composable () -> Unit,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    customColors: CustomColors,
    colorScheme: androidx.compose.material3.ColorScheme
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (isSelected) Primary else customColors.iconInactive
        ) {
            icon()
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            color = if (isSelected) Primary else colorScheme.onSurface,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(
                painter = painterResource(R.drawable.ic_heart_filled),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Primary
            )
        }
    }
}
