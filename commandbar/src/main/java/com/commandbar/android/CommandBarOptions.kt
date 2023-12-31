package com.commandbar.android


data class CommandBarOptions(
    var orgId: String,
    var userId: String? = null,
    var spinnerColor: String? = "#3662F1",
    var launchCode: String? = "prod"
) {
    constructor(dictionary: Map<String, Any>) : this(
        orgId = dictionary["orgId"] as String,
        userId = dictionary["userId"] as? String,
        spinnerColor = dictionary["spinnerColor"] as? String ?: "#3662F1",
        launchCode = dictionary["launchCode"] as? String ?: "prod"
    )
}