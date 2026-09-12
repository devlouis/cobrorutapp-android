package com.pe.cobrorutapp.feature.collection

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pe.cobrorutapp.core.designsystem.EmptyState

@Composable
fun CollectionScreen(modifier: Modifier = Modifier) {
    EmptyState(
        title = stringResource(R.string.collection_empty_title),
        description = stringResource(R.string.collection_empty_description),
        modifier = modifier,
    )
}
