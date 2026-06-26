package com.example.atvidadedm.navigation

object AppRoutes {
	const val LOGIN = "login"
	const val REGISTER = "register"
	const val FORGOT_PASSWORD = "forgot_password"
	const val MENU = "menu"
	const val ROUTEIRO_PATTERN = "roteiro_screen/{tripId}"
	const val TRIP_PHOTOS_PATTERN = "trip_photos_screen/{tripId}"

	fun roteiro(tripId: Long): String = "roteiro_screen/$tripId"
	fun tripPhotos(tripId: Long): String = "trip_photos_screen/$tripId"
}

object MenuRoutes {
	const val HOME = "menu_home"
	const val ROUTEIRO_PATTERN = "roteiro/{tripId}"
	const val TRIP_PHOTOS_PATTERN = "trip_photos/{tripId}"
	const val NEW_TRIP = "new_trip"
	const val MY_TRIPS = "my_trips"
	const val ABOUT = "about"
	const val EDIT_TRIP_PATTERN = "edit_trip/{tripId}"

	fun roteiro(tripId: Long): String = "roteiro/$tripId"
	fun tripPhotos(tripId: Long): String = "trip_photos/$tripId"

	fun editTrip(tripId: Long): String = "edit_trip/$tripId"
}
