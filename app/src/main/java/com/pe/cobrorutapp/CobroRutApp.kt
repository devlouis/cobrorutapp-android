package com.pe.cobrorutapp

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.pe.cobrorutapp.core.domain.importing.ImportedCustomerRow
import com.pe.cobrorutapp.core.domain.matching.CustomerReference
import com.pe.cobrorutapp.core.domain.matching.MatchableCustomer
import com.pe.cobrorutapp.core.domain.money.Money
import com.pe.cobrorutapp.core.domain.preferences.ThemeMode
import com.pe.cobrorutapp.feature.collection.CollectionScreen
import com.pe.cobrorutapp.feature.collection.clients.ImportedClientDetailScreen
import com.pe.cobrorutapp.feature.collection.clients.ImportedClientUi
import com.pe.cobrorutapp.feature.collection.clients.ImportedClientsScreen
import com.pe.cobrorutapp.feature.collection.clients.ImportedClientsUiState
import com.pe.cobrorutapp.feature.collection.clients.ImportedCreditUi
import com.pe.cobrorutapp.feature.collection.manual.ManualAssistantRoute
import com.pe.cobrorutapp.feature.collection.manual.ManualAssistantViewModel
import com.pe.cobrorutapp.feature.collection.manual.ManualDirectoryCustomer
import com.pe.cobrorutapp.feature.collection.manual.ManualDraftQueueRoute
import com.pe.cobrorutapp.feature.collection.manual.ManualDraftReviewRoute
import com.pe.cobrorutapp.feature.collection.manual.SyntheticDebt
import com.pe.cobrorutapp.feature.income.IncomeScreen
import com.pe.cobrorutapp.feature.workday.ImportExcelScreen
import com.pe.cobrorutapp.feature.workday.WorkdayScreen
import com.pe.cobrorutapp.probe.NotificationProbeEntry
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable private data object WorkdayDestination : NavKey
@Serializable private data object CollectionDestination : NavKey
@Serializable private data object IncomeDestination : NavKey
@Serializable private data object ImportExcelDestination : NavKey
@Serializable private data object CustomersDestination : NavKey
@Serializable private data object SettingsDestination : NavKey
@Serializable private data object ManualAssistantDestination : NavKey
@Serializable private data object ManualQueueDestination : NavKey
@Serializable private data class ManualDraftReviewDestination(val draftId: Long) : NavKey
@Serializable private data class ClientDetailDestination(val clientId: String) : NavKey

private enum class RootDestination(@param:StringRes val label: Int, val symbol: String) {
    WORKDAY(R.string.destination_workday, "≡"),
    COLLECTION(R.string.destination_collection, "S/"),
    INCOME(R.string.destination_income, "↓"),
}

private data class DrawerDestination(val route: NavKey, @param:StringRes val label: Int)

private val drawerDestinations = listOf(
    DrawerDestination(ImportExcelDestination, R.string.drawer_import_excel),
    DrawerDestination(CustomersDestination, R.string.drawer_customers),
    DrawerDestination(SettingsDestination, R.string.drawer_settings),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CobroRutApp(
    state: AppUiState,
    fieldFlowState: FieldFlowUiState,
    manualAssistantViewModel: ManualAssistantViewModel,
    onWorkbookSelected: (String) -> Unit,
    onConfirmImport: () -> Unit,
    onCustomerQueryChanged: (String) -> Unit,
    onCycleTheme: () -> Unit,
) {
    val workdayBackStack = rememberNavBackStack(WorkdayDestination)
    val collectionBackStack = rememberNavBackStack(CollectionDestination)
    val incomeBackStack = rememberNavBackStack(IncomeDestination)
    var selectedRootIndex by rememberSaveable { mutableIntStateOf(0) }
    val currentBackStack = when (RootDestination.entries[selectedRootIndex]) {
        RootDestination.WORKDAY -> workdayBackStack
        RootDestination.COLLECTION -> collectionBackStack
        RootDestination.INCOME -> incomeBackStack
    }
    val currentRoute = currentBackStack.last()
    val isRootDestination = currentBackStack.size == 1
    val usesShellTopBar = isRootDestination ||
        currentRoute == ImportExcelDestination || currentRoute == SettingsDestination
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val manualAssistantState by manualAssistantViewModel.uiState.collectAsStateWithLifecycle()
    val importedClients = remember(fieldFlowState.activeRows) {
        val nameCounts = fieldFlowState.activeRows
            .groupingBy { it.name.trim().lowercase(Locale.ROOT) }
            .eachCount()
        fieldFlowState.activeRows.map { row ->
            row.toImportedClientUi(
                sameNameRowCount = nameCounts.getValue(row.name.trim().lowercase(Locale.ROOT)),
            )
        }
    }
    val visibleClients = remember(importedClients, fieldFlowState.customerQuery) {
        importedClients.filterBy(fieldFlowState.customerQuery, fieldFlowState.activeRows)
    }
    val workbookPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.toString()?.let(onWorkbookSelected)
    }

    LaunchedEffect(fieldFlowState.activeRows) {
        if (fieldFlowState.activeRows.isNotEmpty()) {
            manualAssistantViewModel.replaceDirectory(
                fieldFlowState.activeRows.map(ImportedCustomerRow::toManualDirectoryCustomer),
            )
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = isRootDestination,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                )
                HorizontalDivider()
                drawerDestinations.forEach { destination ->
                    NavigationDrawerItem(
                        label = { Text(stringResource(destination.label)) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch {
                                drawerState.close()
                                if (currentBackStack.last() != destination.route) {
                                    currentBackStack.add(destination.route)
                                }
                            }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
            }
        },
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                if (usesShellTopBar) {
                    TopAppBar(
                        title = {
                            Text(
                                if (isRootDestination) stringResource(R.string.app_name)
                                else stringResource(currentRoute.titleResource()),
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    if (isRootDestination) coroutineScope.launch { drawerState.open() }
                                    else currentBackStack.removeLastOrNull()
                                },
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (isRootDestination) R.drawable.ic_menu else R.drawable.ic_arrow_back,
                                    ),
                                    contentDescription = stringResource(
                                        if (isRootDestination) R.string.open_navigation_menu
                                        else R.string.navigate_back,
                                    ),
                                )
                            }
                        },
                    )
                }
            },
            bottomBar = {
                if (isRootDestination) {
                    NavigationBar {
                        RootDestination.entries.forEachIndexed { index, destination ->
                            NavigationBarItem(
                                selected = selectedRootIndex == index,
                                onClick = { selectedRootIndex = index },
                                icon = { Text(destination.symbol) },
                                label = { Text(stringResource(destination.label)) },
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (isRootDestination) {
                    FloatingActionButton(onClick = { currentBackStack.add(ManualAssistantDestination) }) {
                        BadgedBox(
                            badge = {
                                if (manualAssistantState.pendingCount > 0) {
                                    Badge { Text(manualAssistantState.pendingCount.toString()) }
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_chat),
                                contentDescription = stringResource(R.string.open_manual_assistant),
                            )
                        }
                    }
                }
            },
        ) { padding ->
            Column(Modifier.padding(padding)) {
                if (state.preferencesError) {
                    Text(
                        text = stringResource(R.string.preferences_error),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                NavDisplay(
                    backStack = currentBackStack,
                    onBack = { currentBackStack.removeLastOrNull() },
                    entryProvider = { key ->
                        NavEntry(key) {
                            when (key) {
                                WorkdayDestination -> WorkdayScreen()
                                CollectionDestination -> CollectionScreen()
                                IncomeDestination -> Column {
                                    NotificationProbeEntry()
                                    IncomeScreen(Modifier.weight(1f))
                                }
                                ImportExcelDestination -> ImportExcelScreen(
                                    state = fieldFlowState.excelImport,
                                    showTitle = false,
                                    onChooseFile = {
                                        workbookPicker.launch(
                                            arrayOf(
                                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                                "application/vnd.ms-excel",
                                            ),
                                        )
                                    },
                                    onConfirmImport = onConfirmImport,
                                    onOpenDirectory = { currentBackStack.add(CustomersDestination) },
                                )
                                CustomersDestination -> ImportedClientsScreen(
                                    state = ImportedClientsUiState(
                                        query = fieldFlowState.customerQuery,
                                        clients = visibleClients,
                                        importedCount = importedClients.size,
                                        excludedWithoutDniCount = fieldFlowState.excludedWithoutDni,
                                    ),
                                    onQueryChanged = onCustomerQueryChanged,
                                    onClientSelected = { currentBackStack.add(ClientDetailDestination(it)) },
                                    onBack = { currentBackStack.removeLastOrNull() },
                                )
                                is ClientDetailDestination -> ImportedClientDetailScreen(
                                    client = importedClients.firstOrNull { it.id == key.clientId },
                                    onBack = { currentBackStack.removeLastOrNull() },
                                )
                                SettingsDestination -> SettingsScreen(state, onCycleTheme)
                                ManualAssistantDestination -> ManualAssistantRoute(
                                    viewModel = manualAssistantViewModel,
                                    onBack = { currentBackStack.removeLastOrNull() },
                                    onOpenQueue = { currentBackStack.add(ManualQueueDestination) },
                                )
                                ManualQueueDestination -> ManualDraftQueueRoute(
                                    viewModel = manualAssistantViewModel,
                                    onBack = { currentBackStack.removeLastOrNull() },
                                    onReviewDraft = { currentBackStack.add(ManualDraftReviewDestination(it)) },
                                )
                                is ManualDraftReviewDestination -> ManualDraftReviewRoute(
                                    viewModel = manualAssistantViewModel,
                                    draftId = key.draftId,
                                    onBack = { currentBackStack.removeLastOrNull() },
                                )
                                else -> error("Unknown navigation key: $key")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@StringRes
private fun NavKey.titleResource(): Int = when (this) {
    ImportExcelDestination -> R.string.drawer_import_excel
    SettingsDestination -> R.string.drawer_settings
    else -> R.string.app_name
}

@Composable
private fun SettingsScreen(state: AppUiState, onCycleTheme: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.settings_theme_description), style = MaterialTheme.typography.bodyLarge)
        Button(
            onClick = onCycleTheme,
            enabled = state.preferencesReady && !state.isSaving,
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Text(
                stringResource(
                    when (state.themeMode) {
                        ThemeMode.SYSTEM -> R.string.theme_system
                        ThemeMode.LIGHT -> R.string.theme_light
                        ThemeMode.DARK -> R.string.theme_dark
                    },
                ),
            )
        }
    }
}

private fun ImportedCustomerRow.toImportedClientUi(sameNameRowCount: Int): ImportedClientUi = ImportedClientUi(
    id = "import-row-$sourceRowNumber",
    name = name,
    maskedDni = dni.maskDni(),
    address = address,
    sameNameRowCount = sameNameRowCount,
    credits = listOf(
        ImportedCreditUi(
            id = "import-credit-$sourceRowNumber",
            date = dateRaw.orDash(),
            sale = sale.money.displayOrRaw(sale.rawValue),
            service = serviceRaw,
            total = total.money.displayOrRaw(total.rawValue),
            installmentValue = installmentValue.money.displayOrRaw(installmentValue.rawValue),
            balance = balance.money.displayOrRaw(balance.rawValue),
            importedPayment = dailyPayment.money.displayOrRaw(dailyPayment.rawValue),
            installmentNumber = installmentNumberRaw.orDash(),
        ),
    ),
)

private fun ImportedCustomerRow.toManualDirectoryCustomer(): ManualDirectoryCustomer =
    ManualDirectoryCustomer(
        customer = MatchableCustomer(
            reference = CustomerReference("import-row-$sourceRowNumber"),
            displayName = name,
        ),
        debts = listOf(
            SyntheticDebt(
                reference = "import-credit-$sourceRowNumber",
                label = "Cuota ${installmentNumberRaw.orDash()} · " +
                    "saldo ${balance.money.displayOrRaw(balance.rawValue)} · " +
                    "cuota diaria ${installmentValue.money.displayOrRaw(installmentValue.rawValue)}",
            ),
        ),
    )

private fun List<ImportedClientUi>.filterBy(
    query: String,
    sourceRows: List<ImportedCustomerRow>,
): List<ImportedClientUi> {
    val normalized = query.trim().lowercase(Locale.ROOT)
    if (normalized.isEmpty()) return this
    val matchingIds = sourceRows.asSequence()
        .filter { row ->
            row.name.lowercase(Locale.ROOT).contains(normalized) ||
                row.dni.lowercase(Locale.ROOT).contains(normalized) ||
                row.address.orEmpty().lowercase(Locale.ROOT).contains(normalized)
        }
        .map { "import-row-${it.sourceRowNumber}" }
        .toSet()
    return filter { it.id in matchingIds }
}

private fun String.maskDni(): String = when {
    length <= 2 -> "•".repeat(length)
    else -> "•".repeat(length - 2) + takeLast(2)
}

private fun String?.orDash(): String = this?.takeIf(String::isNotBlank) ?: "—"

private fun Money?.displayOrRaw(rawValue: String?): String {
    if (this == null) return rawValue.orDash()
    val whole = minorUnits / 100
    val fraction = (minorUnits % 100).toString().padStart(2, '0')
    return "S/ $whole.$fraction"
}
