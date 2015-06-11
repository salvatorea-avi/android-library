package com.urbanairship.location;

import android.app.PendingIntent;
import android.content.Intent;

import com.urbanairship.BaseTestCase;
import com.urbanairship.TestApplication;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Matchers;

import java.util.UUID;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UALocationProviderTest extends BaseTestCase {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    UALocationProvider provider;

    PendingIntent pendingIntent;

    LocationAdapter mockAdapterOne;
    LocationAdapter mockAdapterTwo;
    LocationRequestOptions options;

    @Before
    public void setUp() {
        mockAdapterOne = mock(LocationAdapter.class);
        mockAdapterTwo = mock(LocationAdapter.class);

        provider = new UALocationProvider(mockAdapterOne, mockAdapterTwo);

        pendingIntent = PendingIntent.getService(TestApplication.getApplication(), 1, new Intent().addCategory(UUID.randomUUID().toString()), 0);
        options = LocationRequestOptions.createDefaultOptions();
    }

    /**
     * Test connection to the provider.
     */
    @Test
    public void testConnect() {
        when(mockAdapterOne.connect()).thenReturn(false);
        when(mockAdapterTwo.connect()).thenReturn(true);

        provider.connect();

        // Verify we tried both adapters
        verify(mockAdapterOne, times(1)).connect();
        verify(mockAdapterTwo, times(1)).connect();


        // Connect again
        provider.connect();

        // Verify we did not try to connect again
        verify(mockAdapterOne, times(1)).connect();
        verify(mockAdapterTwo, times(1)).connect();
    }

    /**
     * Test disconnecting from the provider.
     */
    @Test
    public void testDisconnect() {
        when(mockAdapterOne.connect()).thenReturn(false);
        when(mockAdapterTwo.connect()).thenReturn(true);

        provider.connect();
        provider.disconnect();

        // Verify we only disconnected from the connected provider (mockAdapterTwo)
        verify(mockAdapterOne, times(0)).disconnect();
        verify(mockAdapterTwo, times(1)).disconnect();

        // Disconnect again
        provider.disconnect();

        // Verify we did not try to disconnect again
        verify(mockAdapterOne, times(0)).disconnect();
        verify(mockAdapterTwo, times(1)).disconnect();
    }

    /**
     * Test canceling location updates tries to cancel updates
     * on all of the adapters.
     */
    @Test
    public void testCancelRequests() {
        when(mockAdapterOne.connect()).thenReturn(false);
        when(mockAdapterTwo.connect()).thenReturn(true);

        provider.cancelRequests(pendingIntent);

        // Verify we attempted to connect to both adapters
        verify(mockAdapterOne, times(1)).connect();
        verify(mockAdapterTwo, times(1)).connect();

        // Verify we only canceled requests on the adapter that was connected
        verify(mockAdapterOne, times(0)).cancelLocationUpdates(Matchers.refEq(pendingIntent));
        verify(mockAdapterTwo, times(1)).cancelLocationUpdates(Matchers.refEq(pendingIntent));
    }

    /**
     * Test requesting location updates only requests from the connected
     * adapter.
     */
    @Test
    public void testRequestLocationUpdates() {
        when(mockAdapterTwo.connect()).thenReturn(true);
        provider.connect();

        provider.requestLocationUpdates(options, pendingIntent);

        verify(mockAdapterTwo).requestLocationUpdates(eq(options), Matchers.refEq(pendingIntent));
        verify(mockAdapterOne, times(0)).requestLocationUpdates(eq(options), Matchers.refEq(pendingIntent));
    }

    /**
     * Test requesting location updates when the provider is not connected throws
     * an illegal state exception.
     */
    @Test
    public void testRequestLocationUpdatesNotConnected() {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Provider must be connected before making requests.");
        provider.requestLocationUpdates(options, pendingIntent);
    }

    /**
     * Test single location updates only requests from the connected adapter.
     */
    @Test
    public void testSingleLocationRequest() {
        PendingLocationResult request = new PendingLocationResult() {
            @Override
            protected void onCancel() {
            }
        };

        when(mockAdapterOne.connect()).thenReturn(true);
        when(mockAdapterOne.requestSingleLocation(options)).thenReturn(request);

        provider.connect();
        assertEquals("Should return the adapters pending result.", provider.requestSingleLocation(options), request);
    }

    /**
     * Test requesting a single location update when the provider is not connected throws
     * an illegal state exception.
     */
    @Test
    public void testSingleLocationRequestNotConnected() {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Provider must be connected before making requests.");
        provider.requestSingleLocation(options);
    }

    /**
     * Test single request returns null if none of the adapters were connectible.
     */
    @Test
    public void testSingleLocationRequestNoAdapter() {
        PendingLocationResult request = new PendingLocationResult() {
            @Override
            protected void onCancel() {
            }
        };

        when(mockAdapterOne.connect()).thenReturn(false);
        when(mockAdapterOne.requestSingleLocation(options)).thenReturn(request);
        when(mockAdapterTwo.requestSingleLocation(options)).thenReturn(request);

        provider.connect();
        assertNull("Should return null if no connected adapter", provider.requestSingleLocation(options));
    }
}