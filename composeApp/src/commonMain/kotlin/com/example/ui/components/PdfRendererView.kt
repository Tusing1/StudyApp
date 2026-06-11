package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun PdfRendererView(url: String, modifier: Modifier = Modifier)
