package com.hossam.carenote.core.network

import com.hossam.carenote.core.model.CareNote
import com.hossam.carenote.core.model.Resident
import com.hossam.carenote.core.model.SyncStatus
import com.hossam.carenote.core.model.Vitals
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.UUID

class RetrofitRemoteDataSourceTest {
    private val server = MockWebServer()
    private lateinit var remoteDataSource: RetrofitRemoteDataSource

    private val note = CareNote(
        id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
        residentId = "res-1",
        transcript = "Pulse 72",
        vitals = Vitals(pulse = 72),
        createdAt = Instant.parse("2026-11-01T08:30:00Z"),
    )

    @Before
    fun setUp() {
        server.start()
        remoteDataSource = RetrofitRemoteDataSource(careNoteApi(server.url("/"), OkHttpClient()))
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `given a note, when sending, then its id is the idempotency key`() = runTest {
        // Given
        server.enqueue(response(code = 201))

        // When
        remoteDataSource.sendNote(note)

        // Then
        val request = server.takeRequest()
        assertEquals("/notes", request.url.encodedPath)
        assertEquals(note.id.toString(), request.headers[IDEMPOTENCY_KEY_HEADER])
    }

    @Test
    fun `given the server stores the note, when sending, then it is accepted`() = runTest {
        // Given
        server.enqueue(response(code = 201))

        // When
        val result = remoteDataSource.sendNote(note)

        // Then
        assertEquals(SendResult.Accepted, result)
    }

    @Test
    fun `given an earlier attempt already stored the note, when sending again, then it is accepted`() = runTest {
        // Given
        server.enqueue(response(code = 409))

        // When
        val result = remoteDataSource.sendNote(note)

        // Then
        assertEquals(SendResult.Accepted, result)
    }

    @Test
    fun `given the server is down or busy, when sending, then it is retried later`() = runTest {
        // Given
        server.enqueue(response(code = 503))
        server.enqueue(response(code = 429))

        // When
        val results = listOf(remoteDataSource.sendNote(note), remoteDataSource.sendNote(note))

        // Then
        assertEquals(listOf(SendResult.RetryLater, SendResult.RetryLater), results)
    }

    @Test
    fun `given the request timed out, when sending, then it is retried later`() = runTest {
        // Given
        server.enqueue(response(code = 408))

        // When
        val result = remoteDataSource.sendNote(note)

        // Then
        assertEquals(SendResult.RetryLater, result)
    }

    @Test
    fun `given the server refuses the note, when sending, then it is rejected`() = runTest {
        // Given
        server.enqueue(response(code = 400))

        // When
        val result = remoteDataSource.sendNote(note)

        // Then
        assertEquals(SendResult.Rejected(400), result)
    }

    @Test
    fun `given no connection, when sending, then it is retried later`() = runTest {
        // Given
        server.close()

        // When
        val result = remoteDataSource.sendNote(note)

        // Then
        assertEquals(SendResult.RetryLater, result)
    }

    @Test
    fun `given residents on the server, when fetching, then they are returned`() = runTest {
        // Given
        val body = """[{ "id": "res-1", "firstName": "Anna", "lastName": "Becker", "room": "101" }]"""
        server.enqueue(response(code = 200, body = body))

        // When
        val result = remoteDataSource.fetchResidents()

        // Then
        assertEquals(
            FetchResult.Success(listOf(Resident(id = "res-1", firstName = "Anna", lastName = "Becker", room = "101"))),
            result,
        )
    }

    @Test
    fun `given the server is down, when fetching residents, then they are unavailable`() = runTest {
        // Given
        server.enqueue(response(code = 500))

        // When
        val result = remoteDataSource.fetchResidents()

        // Then
        assertEquals(FetchResult.Unavailable, result)
    }

    @Test
    fun `given an unreadable body, when fetching residents, then it failed`() = runTest {
        // Given
        server.enqueue(response(code = 200, body = """{ "not": "a list" }"""))

        // When
        val result = remoteDataSource.fetchResidents()

        // Then
        assertEquals(FetchResult.Failed, result)
    }

    @Test
    fun `given notes on the server, when fetching the first page, then they come back synced`() = runTest {
        // Given
        val body = """
            {
              "notes": [{
                "id": "00000000-0000-0000-0000-000000000002",
                "residentId": "res-1",
                "transcript": "Pulse 76",
                "vitals": { "pulse": 76 },
                "createdAt": "2026-11-01T09:00:00Z",
                "revisionOf": "00000000-0000-0000-0000-000000000001"
              }],
              "cursor": "c-1",
              "hasMore": true
            }
        """
        server.enqueue(response(code = 200, body = body))

        // When
        val result = remoteDataSource.fetchNotes(after = null)

        // Then
        val edit = CareNote(
            id = UUID.fromString("00000000-0000-0000-0000-000000000002"),
            residentId = "res-1",
            transcript = "Pulse 76",
            vitals = Vitals(pulse = 76),
            createdAt = Instant.parse("2026-11-01T09:00:00Z"),
            revisionOf = note.id,
            syncStatus = SyncStatus.SYNCED,
        )
        assertEquals(FetchResult.Success(NotePage(notes = listOf(edit), cursor = "c-1", hasMore = true)), result)
        val request = server.takeRequest()
        assertEquals("/notes", request.url.encodedPath)
        assertNull(request.url.queryParameter("after"))
        assertEquals("100", request.url.queryParameter("limit"))
    }

    @Test
    fun `given a cursor, when fetching the next page, then the cursor is sent`() = runTest {
        // Given
        server.enqueue(response(code = 200, body = """{ "notes": [], "cursor": "c-2", "hasMore": false }"""))

        // When
        val result = remoteDataSource.fetchNotes(after = "c-1")

        // Then
        assertEquals(FetchResult.Success(NotePage(notes = emptyList(), cursor = "c-2", hasMore = false)), result)
        assertEquals("c-1", server.takeRequest().url.queryParameter("after"))
    }

    @Test
    fun `given the server is down, when fetching notes, then they are unavailable`() = runTest {
        // Given
        server.enqueue(response(code = 503))

        // When
        val result = remoteDataSource.fetchNotes(after = null)

        // Then
        assertEquals(FetchResult.Unavailable, result)
    }

    @Test
    fun `given a note whose id can't be read, when fetching notes, then the page failed`() = runTest {
        // Given: skipping the note would move the cursor past it for good
        val badNote = """
            { "id": "not-a-uuid", "residentId": "res-1", "transcript": "", "vitals": {},
              "createdAt": "2026-11-01T09:00:00Z" }
        """
        server.enqueue(response(code = 200, body = """{ "notes": [$badNote], "cursor": "c-1", "hasMore": false }"""))

        // When
        val result = remoteDataSource.fetchNotes(after = null)

        // Then
        assertEquals(FetchResult.Failed, result)
    }

    private fun response(code: Int, body: String = "") = MockResponse.Builder().code(code).body(body).build()
}
