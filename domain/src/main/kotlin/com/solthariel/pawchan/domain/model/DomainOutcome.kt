package com.solthariel.pawchan.domain.model

sealed interface DomainFailure {
    data object UnsupportedSite : DomainFailure

    data object NetworkUnavailable : DomainFailure

    data object NetworkTimeout : DomainFailure

    data object SecureConnectionFailed : DomainFailure

    data object InvalidResponse : DomainFailure

    data object NotFound : DomainFailure

    data object StorageUnavailable : DomainFailure

    data class HttpStatus(
        val status: Int,
        val retryAfterMillis: Long? = null,
    ) : DomainFailure

    data class ProtectionChallenge(
        val provider: String,
        val verificationUrl: RemoteUrl,
    ) : DomainFailure
}

sealed interface DomainOutcome<out T> {
    data class Success<T>(
        val value: T,
    ) : DomainOutcome<T>

    data class Failure(
        val reason: DomainFailure,
    ) : DomainOutcome<Nothing>
}
