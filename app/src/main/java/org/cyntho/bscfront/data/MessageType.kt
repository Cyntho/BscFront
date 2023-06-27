package org.cyntho.bscfront.data

import com.google.gson.annotations.SerializedName

enum class MessageType {
    @SerializedName("STATUS")
    STATUS,

    @SerializedName("INFO")
    INFO,

    @SerializedName("WARNING")
    WARNING,

    @SerializedName("ERROR")
    ERROR
}