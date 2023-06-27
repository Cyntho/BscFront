package org.cyntho.bscfront.data

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class StatusMessage(
    @SerializedName("id")       var id: Int? = -1,
    @SerializedName("status")   var status : MessageType? = MessageType.STATUS,
    @SerializedName("time")     var time: Long = 0,
    @SerializedName("sps")      var sps: Int? = -1,
    @SerializedName("group")    var group: Int? = -1,
    @SerializedName("device")   var device: String? = "00",
    @SerializedName("part")     var part: Int? = -1,
    @SerializedName("message")  var message: String? = ""
) {
    override fun toString(): String {
        return "[$id] [$status] [$time]: [$sps$group$device$part] $message"
    }
}