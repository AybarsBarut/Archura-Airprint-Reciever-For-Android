package com.archura.airprint.infrastructure.airprint.mdns

import java.io.ByteArrayOutputStream
import java.net.Inet4Address
import java.net.InetAddress
import java.nio.ByteBuffer
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AirPrintMdnsPacketTest {
    private val packet = AirPrintMdnsPacket(
        serviceName = "Archura AirPrint Receiver",
        hostName = "archura-airprint-test",
        port = 9100,
        localAddress = InetAddress.getByName("192.168.1.50") as Inet4Address,
        txtRecords = AirPrintTxtRecords.build(
            serviceName = "Archura AirPrint Receiver",
            uuid = "00000000-0000-0000-0000-000000000000",
            localAddress = "192.168.1.50",
            port = 9100,
        ),
    )

    @Test
    fun testBuildResponseForUniversalSubtypeQuery() {
        val response = packet.buildResponseIfRelevant(
            queryFor("_universal._sub._ipp._tcp.local."),
        )

        assertNotNull(response)
        assertTrue(response!!.toString(Charsets.ISO_8859_1).contains("_universal"))
    }

    @Test
    fun testIgnoreUnrelatedQuery() {
        val response = packet.buildResponseIfRelevant(
            queryFor("_http._tcp.local."),
        )

        assertNull(response)
    }

    private fun queryFor(name: String): ByteArray {
        return ByteArrayOutputStream().apply {
            writeShort(0)
            writeShort(0)
            writeShort(1)
            writeShort(0)
            writeShort(0)
            writeShort(0)
            writeDnsName(name)
            writeShort(TYPE_PTR)
            writeShort(CLASS_IN)
        }.toByteArray()
    }

    private fun ByteArrayOutputStream.writeDnsName(name: String) {
        name.trimEnd('.')
            .split(".")
            .forEach { label ->
                val bytes = label.toByteArray(Charsets.UTF_8)
                write(bytes.size)
                write(bytes)
            }
        write(0)
    }

    private fun ByteArrayOutputStream.writeShort(value: Int) {
        write(ByteBuffer.allocate(Short.SIZE_BYTES).putShort(value.toShort()).array())
    }

    private companion object {
        const val CLASS_IN = 0x0001
        const val TYPE_PTR = 0x000C
    }
}
