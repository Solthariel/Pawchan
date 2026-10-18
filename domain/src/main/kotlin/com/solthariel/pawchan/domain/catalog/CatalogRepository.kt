package com.solthariel.pawchan.domain.catalog

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.CatalogSnapshot
import com.solthariel.pawchan.domain.model.DomainOutcome

fun interface CatalogRepository {
    suspend fun loadCatalog(board: BoardAddress): DomainOutcome<CatalogSnapshot>
}
