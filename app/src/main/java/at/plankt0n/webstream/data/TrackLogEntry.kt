package at.plankt0n.webstream.data

data class TrackLogEntry(
    val timestamp: Long,
    val rawTitle: String,
    val streamName: String = ""
)

