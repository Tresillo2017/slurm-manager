package com.tomasps.slrummanager.data.remote.ssh

import com.tomasps.slrummanager.domain.model.AuthMethod
import com.tomasps.slrummanager.domain.model.Server
import net.schmizz.sshj.DefaultConfig
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.keyprovider.OpenSSHKeyFile
import java.io.StringReader
import java.net.InetAddress
import java.util.concurrent.TimeUnit

class SshClient {

    private fun buildConfig(): DefaultConfig {
        val config = DefaultConfig()
        // Remove X25519/X448 kex algorithms — Android BouncyCastle doesn't support them
        val supported = config.keyExchangeFactories.filter { factory ->
            val name = factory.name.lowercase()
            !name.contains("x25519") && !name.contains("x448")
        }
        config.keyExchangeFactories = supported
        return config
    }

    fun executeCommands(
        server: Server,
        password: String?,
        privateKeyPem: String?,
        vararg commands: String
    ): List<String> {
        val client = SSHClient(buildConfig())
        client.addHostKeyVerifier(PromiscuousVerifier())
        client.connectTimeout = 30_000
        client.timeout = 60_000
        try {
            // Resolve hostname preferring IPv4 to avoid ::1 on loopback/VPN hosts
            val address = InetAddress.getAllByName(server.hostname)
                .firstOrNull { it is java.net.Inet4Address }
                ?: InetAddress.getByName(server.hostname)
            client.connect(address, server.port)
            when (server.authMethod) {
                AuthMethod.PASSWORD ->
                    client.authPassword(server.username, password ?: error("No password provided"))
                AuthMethod.SSH_KEY -> {
                    val keyFile = OpenSSHKeyFile()
                    keyFile.init(StringReader(privateKeyPem ?: error("No private key provided")))
                    client.authPublickey(server.username, keyFile)
                }
            }
            return commands.map { cmd ->
                client.startSession().use { session ->
                    session.exec(cmd).use { command ->
                        command.join(60, TimeUnit.SECONDS)
                        command.inputStream.bufferedReader().readText()
                    }
                }
            }
        } finally {
            runCatching { client.disconnect() }
        }
    }

    fun testConnection(server: Server, password: String?, privateKeyPem: String?): Long {
        val start = System.currentTimeMillis()
        executeCommands(server, password, privateKeyPem, "echo ok")
        return System.currentTimeMillis() - start
    }
}
