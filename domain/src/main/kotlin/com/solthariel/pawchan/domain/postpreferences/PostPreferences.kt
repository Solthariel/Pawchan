package com.solthariel.pawchan.domain.postpreferences

import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress

data class PostPreferences(
    val own: Set<Long> = emptySet(),
    val hidden: Set<Long> = emptySet(),
    val replyTrees: Set<Long> = emptySet(),
    val names: Set<String> = emptySet(),
    val similar: Set<String> = emptySet(),
    val shown: Set<Long> = emptySet(),
)

interface PostPreferencesRepository {
    suspend fun loadPostPreferences(address: ThreadAddress): DomainOutcome<PostPreferences>

    suspend fun savePostPreferences(
        address: ThreadAddress,
        value: PostPreferences,
    ): DomainOutcome<Unit>
}
