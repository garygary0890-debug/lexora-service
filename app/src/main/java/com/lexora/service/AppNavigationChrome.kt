package com.lexora.service

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lexora.service.core.model.Organization
import com.lexora.service.core.model.ServiceUser
import com.lexora.service.core.navigation.Routes

data class AppMenuDestination(val route: String, @StringRes val titleRes: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LexoraTopBar(
    title: String,
    canNavigateBack: Boolean,
    onBack: () -> Unit,
    onOpenRoute: (String) -> Unit,
    onOpenSearch: (() -> Unit)? = null,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack, enabled = canNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
            }
        },
        actions = {
            if (onOpenSearch != null) {
                IconButton(onClick = onOpenSearch) {
                    Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.nav_title_search))
                }
            }
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.nav_more_actions))
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                listOf(
                    Routes.Menu to R.string.nav_action_menu,
                    Routes.ProfileSettings to R.string.nav_action_profile,
                    Routes.Settings to R.string.nav_action_settings,
                    Routes.Notifications to R.string.nav_action_notifications,
                ).forEach { (route, labelRes) ->
                    DropdownMenuItem(
                        text = { Text(stringResource(labelRes)) },
                        onClick = {
                            menuExpanded = false
                            onOpenRoute(route)
                        },
                    )
                }
            }
        },
    )
}

@Composable
fun LexoraBottomBar(
    currentRoute: String,
    selectedDestinations: List<AppMenuDestination>,
    onNavigate: (String) -> Unit,
) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == Routes.Home,
            onClick = { onNavigate(Routes.Home) },
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_title_home)) },
        )
        selectedDestinations.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = { onNavigate(destination.route) },
                icon = { Icon(Icons.Filled.Apps, contentDescription = null) },
                label = { Text(stringResource(destination.titleRes), maxLines = 1) },
            )
        }
        NavigationBarItem(
            selected = currentRoute == Routes.Menu,
            onClick = { onNavigate(Routes.Menu) },
            icon = { Icon(Icons.Filled.Menu, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_title_menu)) },
        )
    }
}

@Composable
fun LexoraMenuScreen(
    destinations: List<AppMenuDestination>,
    selectedRoutes: List<String>,
    onSelectedRoutesChange: (List<String>) -> Unit,
    onOpenProfileSettings: () -> Unit,
    onOpenDestination: (String) -> Unit,
) {
    var editorOpen by remember { mutableStateOf(false) }
    var draftRoutes by remember { mutableStateOf(selectedRoutes) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            OutlinedButton(
                onClick = onOpenProfileSettings,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.nav_title_profile_settings))
                    Text(stringResource(R.string.nav_profile_panel_description))
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            OutlinedButton(
                onClick = {
                    draftRoutes = selectedRoutes
                    editorOpen = true
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.nav_edit_bottom_menu))
            }
        }
        items(destinations, key = { it.route }) { destination ->
            Card(
                onClick = { onOpenDestination(destination.route) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Filled.Apps, contentDescription = null)
                    Text(stringResource(destination.titleRes))
                }
            }
        }
    }

    if (editorOpen) {
        AlertDialog(
            onDismissRequest = { editorOpen = false },
            title = { Text(stringResource(R.string.nav_edit_bottom_menu)) },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(stringResource(R.string.nav_bottom_menu_limit))
                    destinations.forEach { destination ->
                        val selectedIndex = draftRoutes.indexOf(destination.route)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = selectedIndex >= 0,
                                enabled = selectedIndex >= 0 || draftRoutes.size < 3,
                                onCheckedChange = { checked ->
                                    draftRoutes = if (checked) {
                                        (draftRoutes + destination.route).take(3)
                                    } else {
                                        draftRoutes.filterNot { it == destination.route }
                                    }
                                },
                            )
                            Text(
                                text = stringResource(destination.titleRes),
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                            )
                            if (selectedIndex >= 0) {
                                IconButton(
                                    enabled = selectedIndex > 0,
                                    onClick = {
                                        val updated = draftRoutes.toMutableList()
                                        val moved = updated.removeAt(selectedIndex)
                                        updated.add(selectedIndex - 1, moved)
                                        draftRoutes = updated
                                    },
                                ) {
                                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.nav_move_up))
                                }
                                IconButton(
                                    enabled = selectedIndex < draftRoutes.lastIndex,
                                    onClick = {
                                        val updated = draftRoutes.toMutableList()
                                        val moved = updated.removeAt(selectedIndex)
                                        updated.add(selectedIndex + 1, moved)
                                        draftRoutes = updated
                                    },
                                ) {
                                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.nav_move_down))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSelectedRoutesChange(draftRoutes)
                        editorOpen = false
                    },
                ) { Text(stringResource(R.string.nav_save)) }
            },
            dismissButton = {
                TextButton(onClick = { editorOpen = false }) {
                    Text(stringResource(R.string.nav_cancel))
                }
            },
        )
    }
}

@Composable
fun ProfileSettingsScreen(user: ServiceUser, activeOrganization: Organization, onManageOrganizations: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.nav_active_organization))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(activeOrganization.name)
                Text(stringResource(R.string.nav_active_organization_description))
            }
        }
        OutlinedButton(onClick = onManageOrganizations, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.nav_manage_organizations))
        }
        Text(stringResource(R.string.nav_profile_heading))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(user.displayName)
                Text(stringResource(R.string.nav_roles, user.roles.joinToString()))
            }
        }
    }
}
