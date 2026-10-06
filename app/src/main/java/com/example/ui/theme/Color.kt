package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Premium Financial Palette - Exclusively Light Theme
val PrimaryBlueDark = Color(0xFF0C326F)       // Deep Royal Navy for high prominence
val PrimaryBlueMain = Color(0xFF104896)       // Primary Royal Blue
val PrimaryBlueLight = Color(0xFF1D71B8)      // Bright Sapphire Blue
val BlueCardButton = Color(0xFF1B4E8C)        // Button container on top of blue card
val BlueCardButtonBorder = Color(0x33FFFFFF)  // Subtle white border for card buttons

// Surface & Background Colors
val PureWhite = Color(0xFFFFFFFF)
val BackgroundLight = Color(0xFFF8FAFC)       // Crisp pure light with subtle cool undertone
val SurfaceWhite = Color(0xFFFFFFFF)
val SurfaceSubtle = Color(0xFFF1F5F9)         // Light Slate Container
val SurfaceSecondary = Color(0xFFE8F1FC)      // Very soft light blue container
val BorderSubtle = Color(0xFFE2E8F0)          // Crisp subtle border
val BorderSoftBlue = Color(0xFFCCE0F5)        // Soft blue border

// Typography Colors
val TextDarkPrimary = Color(0xFF0F172A)       // High contrast dark slate text
val TextDarkSecondary = Color(0xFF475569)     // Slate secondary text
val TextMuted = Color(0xFF64748B)             // Subtle helper text
val TextWhite = Color(0xFFFFFFFF)
val TextWhiteTranslucent = Color(0xCCFFFFFF)  // 80% opacity white text

// Financial Status Accents
val SuccessGreen = Color(0xFF16A34A)          // Crisp Emerald Green for payments & growth
val SuccessGreenLight = Color(0xFFDCFCE7)     // Soft green badge background
val WarningAmber = Color(0xFFD97706)          // Amber for partial / 14-day delay
val WarningAmberLight = Color(0xFFFEF3C7)     // Soft amber badge background
val DangerRed = Color(0xFFDC2626)             // Red for long overdue (>30 days)
val DangerRedLight = Color(0xFFFEE2E2)        // Soft red badge background

// Material Light Colors compatibility
val PrimaryLight = PrimaryBlueMain
val SecondaryLight = Color(0xFF3B82F6)
val TertiaryLight = Color(0xFF0284C7)
val SurfaceLight = PureWhite
val OnPrimaryLight = PureWhite
val OnSecondaryLight = PureWhite
val ErrorLight = DangerRed

// Dark Palette compatibility (Light-mode only app)
val PrimaryDark = PrimaryBlueMain
val OnPrimaryDark = PureWhite
val SecondaryDark = Color(0xFF3B82F6)
val OnSecondaryDark = PureWhite
val TertiaryDark = Color(0xFF0284C7)
val BackgroundDark = BackgroundLight
val SurfaceDark = PureWhite
val ErrorDark = DangerRed
