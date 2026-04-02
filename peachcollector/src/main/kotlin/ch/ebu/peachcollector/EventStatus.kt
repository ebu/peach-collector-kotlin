package ch.ebu.peachcollector

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity tracking the publishing status of each event per publisher.
 * One Event can have multiple EventStatus records (one per publisher).
 */
@Entity(
    tableName = "EventStatus",
    foreignKeys = [
        ForeignKey(
            entity = Event::class,
            parentColumns = ["id"],
            childColumns = ["event_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["event_id"])]
)
data class EventStatus(
    @ColumnInfo(name = "event_id") val eventID: Int,
    @ColumnInfo(name = "publisher_name") val publisherName: String,
    @ColumnInfo(name = "status") var status: Int = Status.QUEUED,
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}
