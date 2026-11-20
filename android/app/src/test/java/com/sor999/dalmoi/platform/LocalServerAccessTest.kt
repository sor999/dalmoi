package com.sor999.dalmoi.platform

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalServerAccessTest {
    @Test
    fun permissionGateOnlyMatchesLocalAddressesWithoutResolvingPublicHosts() {
        listOf(
            "http://10.0.2.2:8080/api/v1/",
            "http://127.0.0.1:8080/",
            "http://localhost/",
            "http://192.168.1.3/",
            "https://172.16.0.2/",
            "http://[::1]/",
        ).forEach { assertTrue(isLocalServer(it)) }
        listOf("https://api.example.com/", "https://172.32.0.1/", "https://192.0.2.1/", "", "invalid")
            .forEach { assertFalse(isLocalServer(it)) }
    }
}
