package com.example.fitto.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FittoDatabaseMigrationPolicyTest {

    @Test
    fun explicitMigrations_coverEverySupportedVersionFromSixToCurrent() {
        val transitions = FittoDatabaseMigrations.ALL
            .map { migration -> migration.startVersion to migration.endVersion }

        assertEquals(
            listOf(6 to 7, 7 to 8, 8 to 9),
            transitions
        )
    }

    @Test
    fun destructiveFallback_isRestrictedToPreMigrationSchemas() {
        val fallbackVersions = FittoDatabaseMigrations.LEGACY_DESTRUCTIVE_START_VERSIONS.toList()
        val explicitVersions = FittoDatabaseMigrations.ALL
            .flatMap { migration -> listOf(migration.startVersion, migration.endVersion) }
            .toSet()

        assertEquals(listOf(1, 2, 3, 4, 5), fallbackVersions)
        assertTrue(fallbackVersions.none(explicitVersions::contains))
    }
}
