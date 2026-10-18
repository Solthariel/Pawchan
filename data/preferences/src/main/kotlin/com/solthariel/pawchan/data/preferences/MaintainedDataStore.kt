package com.solthariel.pawchan.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.solthariel.pawchan.data.maintenance.MaintenanceGate

internal class MaintainedDataStore(
    private val delegate: DataStore<Preferences>,
    private val gate: MaintenanceGate,
) : DataStore<Preferences> {
    override val data = delegate.data

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        gate.access { delegate.updateData(transform) }
}
