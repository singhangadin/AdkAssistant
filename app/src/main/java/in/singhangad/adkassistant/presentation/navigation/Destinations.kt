package `in`.singhangad.adkassistant.presentation.navigation

/** Type-safe route constants for the Navigation Component. */
sealed class Destination(val route: String) {
    data object Chat : Destination("chat")
    data object About : Destination("about")
}
