package org.cyntho.bscfront.data

import com.google.gson.annotations.SerializedName

/**
 * Wrapper for any status message being transmitted
 */
data class StatusMessage(

    @SerializedName("location") var location: Int? = 0,
    @SerializedName("id")       var id: String? = "INVALID ID",
    @SerializedName("status")   var status : MessageType? = MessageType.WARNING,
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

    override fun equals(other: Any?): Boolean {
        // We only really care about the id, since it's derived from python's uuid4 class
        if (other is StatusMessage){
            return id.equals(other.id) && !id.equals("INVALID ID")
        }
        return false
    }

    override fun hashCode(): Int {
        var result = location ?: 0
        result = 31 * result + (id?.hashCode() ?: 0)
        result = 31 * result + (status?.hashCode() ?: 0)
        result = 31 * result + time.hashCode()
        result = 31 * result + (sps ?: 0)
        result = 31 * result + (group ?: 0)
        result = 31 * result + (device?.hashCode() ?: 0)
        result = 31 * result + (part ?: 0)
        result = 31 * result + (message?.hashCode() ?: 0)
        return result
    }
}
