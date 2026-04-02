package ch.ebu.peachcollector.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ch.ebu.peachcollector.Event
import ch.ebu.peachcollector.EventStatus

/**
 * Room Data Access Object for Event and EventStatus tables.
 * All functions are suspend for coroutine-based access.
 */
@Dao
interface EventDao {

    // region Event operations

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: Event): Long

    @Query("SELECT * FROM Event")
    suspend fun getAllEvents(): List<Event>

    @Query("SELECT * FROM Event WHERE id = :eventRowId")
    suspend fun getEvent(eventRowId: Int): Event?

    @Delete
    suspend fun deleteEvent(event: Event)

    @Query("DELETE FROM Event")
    suspend fun deleteAllEvents()

    @Query("DELETE FROM EventStatus")
    suspend fun deleteAllStatuses()

    @Query("DELETE FROM Event WHERE id IN (SELECT id FROM Event ORDER BY id ASC LIMIT :limit OFFSET :offset)")
    suspend fun deleteEvents(offset: Int, limit: Int)

    @Query("DELETE FROM Event WHERE creationDate < :dateLimit")
    suspend fun deleteEventsBefore(dateLimit: Long)

    // endregion

    // region EventStatus operations

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatus(status: EventStatus)

    @Update
    suspend fun updateStatus(status: EventStatus)

    @Query("UPDATE EventStatus SET status = :status WHERE event_id = :eventId AND publisher_name = :publisherName")
    suspend fun updateStatus(eventId: Int, publisherName: String, status: Int)

    @Query("SELECT * FROM EventStatus")
    suspend fun getAllEventStatuses(): List<EventStatus>

    @Query("SELECT * FROM EventStatus WHERE event_id = :eventId AND publisher_name = :publisherName")
    suspend fun getEventStatus(eventId: Int, publisherName: String): EventStatus?

    @Query("SELECT * FROM EventStatus WHERE publisher_name = :publisherName")
    suspend fun getStatuses(publisherName: String): List<EventStatus>

    @Query("SELECT * FROM EventStatus WHERE publisher_name = :publisherName AND status = :status")
    suspend fun getStatusesForPublisher(publisherName: String, status: Int): List<EventStatus>

    @Query("SELECT * FROM EventStatus WHERE event_id = :eventId")
    suspend fun getStatusesForEvent(eventId: Int): List<EventStatus>

    @Query("SELECT * FROM EventStatus WHERE event_id = :eventId AND status < 2")
    suspend fun getPendingEventStatuses(eventId: Int): List<EventStatus>

    @Query("SELECT * FROM EventStatus WHERE publisher_name = :publisherName AND status < 2")
    suspend fun getPendingStatuses(publisherName: String): List<EventStatus>

    // endregion
}
