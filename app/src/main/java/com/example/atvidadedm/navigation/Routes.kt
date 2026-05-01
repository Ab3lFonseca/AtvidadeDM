package com.example.atvidadedm.navigation

object AppRoutes {
	const val LOGIN = "login"
	const val REGISTER = "register"
	const val FORGOT_PASSWORD = "forgot_password"
	const val MENU = "menu"
}

object MenuRoutes {
	const val HOME = "menu_home"
	const val NEW_TRIP = "new_trip"
	const val MY_TRIPS = "my_trips"
	const val ABOUT = "about"
	const val EDIT_TRIP_PATTERN = "edit_trip/{tripId}"

	fun editTrip(tripId: Long): String = "edit_trip/$tripId"
}

