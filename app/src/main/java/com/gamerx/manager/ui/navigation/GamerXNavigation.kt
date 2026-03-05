package com.gamerx.manager.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.gamerx.manager.feature.home.HomeScreen
import com.gamerx.manager.feature.performance.PerformanceScreen
import com.gamerx.manager.feature.tweaks.TweaksScreen
import com.gamerx.manager.feature.spoofer.SpooferScreen
import com.gamerx.manager.feature.linux.LinuxScreen

@Composable
fun SetupNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(route = Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(route = Screen.Tools.route) {
            com.gamerx.manager.feature.tools.ToolsScreen(navController = navController)
        }
        composable(route = Screen.Customize.route) {
            com.gamerx.manager.feature.customize.CustomizeScreen(navController = navController)
        }
        composable(route = Screen.Settings.route) {
            com.gamerx.manager.feature.settings.SettingsScreen(navController = navController)
        }
        composable(route = Screen.Performance.route) {
            PerformanceScreen(navController = navController)
        }
        composable(route = Screen.Tweaks.route) {
            TweaksScreen(navController = navController)
        }
        composable(route = Screen.Spoofer.route) {
            SpooferScreen(navController = navController)
        }
        composable(route = Screen.Linux.route) {
            LinuxScreen(navController = navController)
        }
        composable(route = Screen.DeviceInfo.route) {
            com.gamerx.manager.feature.deviceinfo.DeviceInfoScreen(navController = navController)
        }
        composable(route = Screen.IconPack.route) {
            com.gamerx.manager.feature.iconpack.IconPackScreen(navController = navController)
        }
        composable(route = Screen.BootAnim.route) {
            com.gamerx.manager.feature.bootanim.BootAnimationScreen(navController = navController)
        }
        composable(route = Screen.Shell.route) {
            com.gamerx.manager.feature.shell.ShellScreen(navController = navController)
        }
        composable(
            route = Screen.IconPackDetail.route,
            arguments = listOf(androidx.navigation.navArgument("packName") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val packName = backStackEntry.arguments?.getString("packName") ?: return@composable
            com.gamerx.manager.feature.iconpack.IconPackDetailScreen(navController = navController, packName = packName)
        }
        composable(
            route = Screen.BootAnimDetail.route,
            arguments = listOf(androidx.navigation.navArgument("animName") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val animName = backStackEntry.arguments?.getString("animName") ?: return@composable
            com.gamerx.manager.feature.bootanim.BootAnimDetailScreen(navController = navController, animName = animName)
        }
    }
}
