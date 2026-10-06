package com.lexora.service

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Menu
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lexora.service.core.model.Organization
import com.lexora.service.core.model.ServiceUser
import com.lexora.service.core.navigation.Routes

data class AppMenuDestination(val route: String, @StringRes val titleRes: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LexoraTopBar(title: String, canNavigateBack: Boolean, onBack: () -> Unit, onOpenRoute: (String) -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack, enabled = canNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
            }
        },
        actions = {
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
fun LexoraBottomBar(currentRoute: String, onNavigate: (String) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == Routes.Home,
            onClick = { onNavigate(Routes.Home) },
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_title_home)) },
        )
        NavigationBarItem(
            selected = currentRoute == Routes.Menu,
            onClick = { onNavigate(Routes.Menu) },
            icon = { Icon(Icons.AutoMirrored.Filled.Menu, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_title_menu)) },
        )
    }
}

@Composable
fun LexoraMenuScreen(destinations: List<AppMenuDestination>, onOpenProfileSettings: () -> Unit, onOpenDestination: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
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
        items(destinations) { destination ->
            OutlinedButton(
                onClick = { onOpenDestination(destination.route) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(destination.titleRes))
            }
        }
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
