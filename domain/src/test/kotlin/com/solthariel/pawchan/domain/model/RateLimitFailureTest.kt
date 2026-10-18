package com.solthariel.pawchan.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RateLimitFailureTest {
    @Test fun onlyHttp429IsRateLimited() {
        assertTrue(DomainFailure.HttpStatus(429).isRateLimited)
        for (code in listOf(200, 403, 404, 428, 430, 500, 503)) {
            assertFalse(DomainFailure.HttpStatus(code).isRateLimited)
        }
    }

    @Test fun networkAndStorageFailuresDoNotBecomeRateLimits() {
        val failures =
            listOf(
                DomainFailure.NetworkUnavailable,
                DomainFailure.NetworkTimeout,
                DomainFailure.SecureConnectionFailed,
                DomainFailure.StorageUnavailable,
                DomainFailure.InvalidResponse,
                DomainFailure.NotFound,
            )
        failures.forEach { assertFalse(it.isRateLimited) }
    }
}
