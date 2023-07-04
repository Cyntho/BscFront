package org.cyntho.bscfront.data

import com.google.gson.annotations.SerializedName

enum class MessageType {

    @SerializedName("WARNING")
    WARNING,

    @SerializedName("ERROR")
    ERROR
}