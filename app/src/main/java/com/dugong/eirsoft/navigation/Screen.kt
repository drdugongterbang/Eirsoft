package com.dugong.eirsoft.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object AdminDashboard : Screen("admin_dashboard")
    object MemberDashboard : Screen("member_dashboard")
    object ManageProperties : Screen("manage_properties")
    object AddEditProperty : Screen("add_edit_property/{propertyId}") {
        fun createRoute(propertyId: String = "new") = "add_edit_property/$propertyId"
    }
    object ApproveLoans : Screen("approve_loans")
    object PropertyDetail : Screen("property_detail/{propertyId}") {
        fun createRoute(propertyId: String) = "property_detail/$propertyId"
    }
    object RequestLoan : Screen("request_loan/{propertyId}") {
        fun createRoute(propertyId: String) = "request_loan/$propertyId"
    }
    object LoanHistory : Screen("loan_history")
    object Profile : Screen("profile")
    object MemberDetail : Screen("member_detail/{userId}") {
        fun createRoute(userId: String) = "member_detail/$userId"
    }
}