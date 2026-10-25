package com.solthariel.pawchan.data.database

import org.junit.runner.RunWith
import org.junit.runners.Suite

/** Run every database device test explicitly; an empty discovery result is not a successful run. */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    DatabaseMigrationTest::class,
    FilterStatisticsRepositoryTest::class,
    ThreadLibraryBackupRuntimeTest::class,
)
class DatabaseDeviceSuite
