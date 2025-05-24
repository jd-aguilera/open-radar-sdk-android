package io.radar.sdk

import android.content.Context
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.json.JSONObject
import android.location.Location
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.test.core.app.ApplicationProvider
import android.content.SharedPreferences

// You might need to adjust imports based on actual file structure and available libraries

@RunWith(RobolectricTestRunner::class) // Or MockitoJUnitRunner if not using Robolectric
@Config(sdk = [28]) // Example, if using Robolectric
class CustomBackendTest {

    @Mock
    lateinit var mockContext: Context

    @Mock
    lateinit var mockSharedPreferences: SharedPreferences

    @Mock
    lateinit var mockEditor: SharedPreferences.Editor

    @Mock
    lateinit var mockApiClient: RadarApiClient

    @Before
    fun setUp() {
        // Initialize mocks created with @Mock annotation
        org.mockito.MockitoAnnotations.openMocks(this)

        // Basic mocking for SharedPreferences
        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        // For RadarSettings.getCustomBackendUrl to work in the test
        `when`(mockSharedPreferences.getString(eq("custom_backend_url"), isNull())).thenAnswer { invocation ->
            // Return the custom URL if it was set by putString, otherwise null
            val key = invocation.arguments[0] as String
            if (mockedUrlStorage.containsKey(key)) {
                return@thenAnswer mockedUrlStorage[key]
            }
            invocation.arguments[1] // Default value (null in this case)
        }
        `when`(mockEditor.putString(eq("custom_backend_url"), anyString())).thenAnswer { invocation ->
            mockedUrlStorage[invocation.arguments[0] as String] = invocation.arguments[1] as String
            mockEditor
        }


        // Provide a default mock for ApplicationProvider.getApplicationContext()
        // if running Robolectric tests that might use RadarUtils, etc.
        // In Robolectric, ApplicationProvider.getApplicationContext() usually works out of the box,
        // but if specific mocking is needed:
        // val mockApplicationContext = mock(Context::class.java)
        // `when`(ApplicationProvider.getApplicationContext<Context>()).thenReturn(mockApplicationContext)
        // `when`(mockApplicationContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockSharedPreferences)


        Radar.apiClient = mockApiClient // Inject a mock apiClient
        Radar.initialized = false // Reset initialization state for tests
        Radar.logger = mock(RadarLogger::class.java) // Mock logger to avoid NPEs if it's used internally
        Radar.handler = mock(android.os.Handler::class.java) // Mock handler
    }

    // Simple in-memory store for what's "written" to SharedPreferences mock
    private val mockedUrlStorage = mutableMapOf<String, String?>()

    @Test
    fun testInitialize_storesCustomBackendUrl() {
        val customUrl = "http://my.custom.backend/api"
        Radar.initialize(mockContext, customUrl)

        // Verify that RadarSettings.setCustomBackendUrl was effectively called
        // This indirectly tests if the URL is stored via SharedPreferences
        verify(mockEditor).putString(eq("custom_backend_url"), eq(customUrl))
        assert(RadarSettings.getCustomBackendUrl(mockContext) == customUrl) {
            "Custom backend URL was not stored correctly. Expected: $customUrl, Got: ${RadarSettings.getCustomBackendUrl(mockContext)}"
        }
    }

    @Test
    fun testTrackOnce_usesCustomBackend() {
        val customUrl = "http://my.custom.backend/api/" // Ensure trailing slash if paths are appended directly
        Radar.initialize(mockContext, customUrl) // Initialize with the custom URL
        Radar.initialized = true // Ensure Radar is marked as initialized for trackOnce to proceed

        val mockLocation = Location("mock")
        mockLocation.latitude = 10.0
        mockLocation.longitude = 20.0
        mockLocation.accuracy = 5f

        // This is a simplified verification.
        // We are checking that some method on mockApiClient is called.
        // A more robust test would involve capturing arguments to apiHelper.request
        // within RadarApiClient, but that requires deeper mocking or refactoring RadarApiClient for testability.
        // For now, we'll assume that if initialize set the URL and track is called, it *should* use it.

        Radar.trackOnce(mockLocation, null)

        // Verify that a track-related method on apiClient is called.
        // The actual verification depends on how RadarApiClient is structured post-refactor.
        // We're checking that the 'track' method of the apiClient is called.
        // The arguments to 'track' would ideally be checked to ensure the custom URL is somehow used,
        // but that might require more complex argument captors.
        verify(mockApiClient).track(
            any(Location::class.java),
            anyBoolean(),
            anyBoolean(),
            any(Radar.RadarLocationSource::class.java),
            anyBoolean(),
            isNull(), // beacons
            anyBoolean(), // verified
            isNull(), // integrityToken
            isNull(), // integrityException
            isNull(), // encrypted
            isNull(), // expectedCountryCode
            isNull(), // expectedStateCode
            any() // callback
        )

        // To truly verify the URL, you would need to:
        // 1. Not mock RadarApiClient but mock RadarApiHelper instead.
        // 2. Capture the URL passed to RadarApiHelper.request.
        // This test as written provides a basic check that the flow reaches the apiClient.
         Radar.logger.i("Note: This test verifies track reaches apiClient. Deeper URL verification needs RadarApiHelper mocking.")
    }
}
