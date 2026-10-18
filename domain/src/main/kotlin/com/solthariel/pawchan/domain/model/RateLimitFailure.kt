package com.solthariel.pawchan.domain.model

/** Classification of an existing transport-shaped DomainFailure; not a site's posting policy. */
val DomainFailure.isRateLimited: Boolean
    get() = this is DomainFailure.HttpStatus && status == RATE_LIMITED_STATUS

private const val RATE_LIMITED_STATUS = 429
