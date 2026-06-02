package com.tomasps.slrummanager.data.remote.ssh

import com.tomasps.slrummanager.domain.model.AuthMethod
import com.tomasps.slrummanager.domain.model.Server
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import java.io.StringReader
import java.util.concurrent.TimeUnit

class SshClient {

    fun executeCommands(
        server: Server,
        password: String?,
        privateKeyPem: String?,
        vararg commands: String
    ): List<String> {
        val client = SSHClient()
        client.addHostKeyVerifier(PromiscuousVerifier())
        client.connectTimeout = 30_000
        client.timeout = 60_000
        try {
            client.connect(server.hostname, server.port)
            when (server.authMethod) {
                AuthMethod.PASSWORD ->
                    client.authPassword(server.username, password ?: error("No password provided"))
                AuthMethod.SSH_KEY -> {
                    val keyProvider = client.loadKeys(
                        StringReader(privateKeyPem ?: error("No private key provided")), null, null
                    )
                    client.authPublickey(server.username, keyProvider)
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
