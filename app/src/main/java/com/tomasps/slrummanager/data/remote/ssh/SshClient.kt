package com.tomasps.slrummanager.data.remote.ssh

import android.util.Log
import com.tomasps.slrummanager.domain.model.AuthMethod
import com.tomasps.slrummanager.domain.model.Server
import net.schmizz.sshj.DefaultConfig
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.keyprovider.OpenSSHKeyFile
import java.io.StringReader
import java.net.Inet4Address
import java.net.InetAddress
import java.util.concurrent.TimeUnit

class SshClient {

    companion object {
        private const val TAG = "SshClient"
    }

    private fun buildConfig(): DefaultConfig {
        val config = DefaultConfig()
        Log.d(TAG, "kex algorithms: ${config.keyExchangeFactories.map { it.name }}")
        return config
    }

    fun executeCommands(
        server: Server,
        password: String?,
        privateKeyPem: String?,
        vararg commands: String
    ): List<String> {
        Log.d(TAG, "executeCommands: host=${server.hostname} port=${server.port} auth=${server.authMethod} commands=${commands.toList()}")

        val config = buildConfig()
        val client = SSHClient(config)
        client.addHostKeyVerifier(PromiscuousVerifier())
        client.connectTimeout = 30_000
        client.timeout = 60_000

        try {
            // DNS resolution — log all addresses, prefer IPv4
            val allAddresses = try {
                InetAddress.getAllByName(server.hostname).toList()
            } catch (e: Exception) {
                Log.e(TAG, "DNS resolution failed for '${server.hostname}': ${e.message}", e)
                throw e
            }
            Log.d(TAG, "DNS resolved '${server.hostname}' to: ${allAddresses.map { "${it.hostAddress} (${it::class.simpleName})" }}")

            val address = allAddresses.firstOrNull { it is Inet4Address } ?: allAddresses.first()
            Log.d(TAG, "Connecting to selected address: ${address.hostAddress}:${server.port}")

            try {
                client.connect(address, server.port)
                Log.d(TAG, "TCP connected to ${address.hostAddress}:${server.port}")
            } catch (e: Exception) {
                Log.e(TAG, "TCP connect failed to ${address.hostAddress}:${server.port}: ${e.message}", e)
                throw e
            }

            when (server.authMethod) {
                AuthMethod.PASSWORD -> {
                    Log.d(TAG, "Authenticating with password for user '${server.username}'")
                    try {
                        client.authPassword(server.username, password ?: error("No password provided"))
                        Log.d(TAG, "Password auth succeeded")
                    } catch (e: Exception) {
                        Log.e(TAG, "Password auth failed: ${e.message}", e)
                        throw e
                    }
                }
                AuthMethod.SSH_KEY -> {
                    Log.d(TAG, "Authenticating with SSH key for user '${server.username}'")
                    try {
                        val keyFile = OpenSSHKeyFile()
                        keyFile.init(StringReader(privateKeyPem ?: error("No private key provided")))
                        client.authPublickey(server.username, keyFile)
                        Log.d(TAG, "SSH key auth succeeded")
                    } catch (e: Exception) {
                        Log.e(TAG, "SSH key auth failed: ${e.message}", e)
                        throw e
                    }
                }
            }

            return commands.map { cmd ->
                Log.d(TAG, "Executing command: $cmd")
                client.startSession().use { session ->
                    session.exec(cmd).use { command ->
                        command.join(60, TimeUnit.SECONDS)
                        val output = command.inputStream.bufferedReader().readText()
                        val exitStatus = command.exitStatus
                        Log.d(TAG, "Command exit=$exitStatus output=${output.take(200)}")
                        output
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "executeCommands failed: ${e::class.simpleName}: ${e.message}", e)
            throw e
        } finally {
            runCatching { client.disconnect() }
                .onFailure { Log.w(TAG, "disconnect error: ${it.message}") }
        }
    }

    fun testConnection(server: Server, password: String?, privateKeyPem: String?): Long {
        Log.d(TAG, "testConnection: host=${server.hostname}")
        val start = System.currentTimeMillis()
        executeCommands(server, password, privateKeyPem, "echo ok")
        val latency = System.currentTimeMillis() - start
        Log.d(TAG, "testConnection latency=${latency}ms")
        return latency
    }
}
