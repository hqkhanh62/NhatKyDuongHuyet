package com.example.nhatkyduonghuyet.ui.navigation.animation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavBackStackEntry

fun AnimatedContentTransitionScope<NavBackStackEntry>.defaultEnterTransition() =
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = tween(300)
    ) + fadeIn(animationSpec = tween(300))

fun AnimatedContentTransitionScope<NavBackStackEntry>.defaultExitTransition() =
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = tween(300)
    ) + fadeOut(animationSpec = tween(300))

fun AnimatedContentTransitionScope<NavBackStackEntry>.defaultPopEnterTransition() =
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = tween(300)
    ) + fadeIn(animationSpec = tween(300))

fun AnimatedContentTransitionScope<NavBackStackEntry>.defaultPopExitTransition() =
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = tween(300)
    ) + fadeOut(animationSpec = tween(300))
