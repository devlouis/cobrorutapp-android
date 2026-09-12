package com.pe.cobrorutapp.feature.income

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pe.cobrorutapp.core.designsystem.EmptyState

@Composable
fun IncomeScreen(modifier: Modifier = Modifier) {
    EmptyState(
        title = stringResource(R.string.income_empty_title),
        description = stringResource(R.string.income_empty_description),
        modifier = modifier,
    )
}
