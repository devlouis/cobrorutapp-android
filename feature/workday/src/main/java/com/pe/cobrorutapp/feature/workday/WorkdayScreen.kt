package com.pe.cobrorutapp.feature.workday

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pe.cobrorutapp.core.designsystem.EmptyState

@Composable
fun WorkdayScreen(modifier: Modifier = Modifier) {
    EmptyState(
        title = stringResource(R.string.workday_empty_title),
        description = stringResource(R.string.workday_empty_description),
        modifier = modifier,
    )
}
