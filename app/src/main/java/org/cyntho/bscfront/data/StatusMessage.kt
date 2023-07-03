package org.cyntho.bscfront.data

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class StatusMessage(

    @SerializedName("location") var location: Int? = 0,
    @SerializedName("id")       var id: String? = "INVALID ID",
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


/*
    def __init__(self, location: int,
                 error_id: str,
                 error_code: int,
                 error_type: int,
                 sps_id: int,
                 timestamp):
        self.location = location
        self.error_id = error_id
        self.error_code = error_code
        self.error_type = error_type
        self.sps_id = sps_id
        self.timestamp = timestamp
 */