package org.cyntho.bscfront.data

import com.google.gson.annotations.SerializedName

/**
 * Wrapper for locations being transmitted in the 'res/settings' topic
 */
data class LocationWrapper(

    @SerializedName("id")       var id: Int? = 0,
    @SerializedName("name")     var name: String? = "",
    @SerializedName("SPS")      var sps: List<Int>? = mutableListOf(),
    @SerializedName("RBG")      var rbg: List<Int>? = mutableListOf()
)
