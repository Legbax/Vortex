package com.vortex

import android.content.Context
import com.topjohnwu.superuser.Shell
import com.vortex.utils.CryptoUtils
import java.io.File
import java.io.FileOutputStream

object ProxyManager {
    private const val PORT_TCP = 12345
    private const val PORT_UDP = 12346
    private const val CHAIN = "VORTEX_PROXY"

    fun startProxy(context: Context): Boolean {
        val config = CryptoUtils.getProxyConfig(context) ?: return false
        val binFile = extractRedsocks(context)
        val confFile = writeConfig(context, config)

        cleanupPrevious(context)

        val result = Shell.su("${binFile.absolutePath} -c ${confFile.absolutePath}").exec()
        if (!result.isSuccess) return false

        setupIptables(context, config)
        return true
    }

    fun stopProxy(context: Context) {
        cleanupPrevious(context)
    }

    private fun cleanupPrevious(context: Context) {
        Shell.su("pkill redsocks").exec()

        // Limpieza de IPv4 e IPv6 Global
        Shell.su(
            "iptables -t nat -D OUTPUT -j $CHAIN 2>/dev/null",
            "ip6tables -D OUTPUT -j REJECT 2>/dev/null"
        ).exec()

        // Limpieza de reglas Per-App
        val uids = getTargetUids(context, CryptoUtils.getProxyConfig(context))
        uids.forEach { uid ->
            Shell.su(
                "iptables -t nat -D OUTPUT -m owner --uid-owner $uid -j $CHAIN 2>/dev/null",
                "ip6tables -D OUTPUT -m owner --uid-owner $uid -j REJECT 2>/dev/null"
            ).exec()
        }

        // Destruir cadena NAT
        Shell.su(
            "iptables -t nat -F $CHAIN 2>/dev/null || true",
            "iptables -t nat -X $CHAIN 2>/dev/null || true"
        ).exec()
    }

    private fun setupIptables(context: Context, config: CryptoUtils.ProxyConfig) {
        val uids = getTargetUids(context, config)
        val cmds = mutableListOf<String>(
            "iptables -t nat -N $CHAIN 2>/dev/null || true",
            "iptables -t nat -F $CHAIN",
            // Bypass OpSec (Loops, LAN, Multicast)
            "iptables -t nat -A $CHAIN -d 0.0.0.0/8 -j RETURN",
            "iptables -t nat -A $CHAIN -d 127.0.0.0/8 -j RETURN",
            "iptables -t nat -A $CHAIN -d 10.0.0.0/8 -j RETURN",
            "iptables -t nat -A $CHAIN -d 172.16.0.0/12 -j RETURN",
            "iptables -t nat -A $CHAIN -d 192.168.0.0/16 -j RETURN",
            "iptables -t nat -A $CHAIN -d 224.0.0.0/4 -j RETURN",
            "iptables -t nat -A $CHAIN -d 240.0.0.0/4 -j RETURN",
            "iptables -t nat -A $CHAIN -d ${config.host} -j RETURN",
            // Redirección a Redsocks (TCP y UDP mitigando DNS Leaks)
            "iptables -t nat -A $CHAIN -p tcp -j REDIRECT --to-ports $PORT_TCP",
            "iptables -t nat -A $CHAIN -p udp -j REDIRECT --to-ports $PORT_UDP"
        )

        if (config.isGlobal) {
            cmds.add("iptables -t nat -I OUTPUT -j $CHAIN")
            cmds.add("ip6tables -I OUTPUT -j REJECT") // Kill-Switch IPv6 Global
        } else {
            uids.forEach { uid ->
                cmds.add("iptables -t nat -I OUTPUT -m owner --uid-owner $uid -j $CHAIN")
                cmds.add("ip6tables -I OUTPUT -m owner --uid-owner $uid -j REJECT") // Kill-Switch IPv6 Per-App
            }
        }
        Shell.su(*cmds.toTypedArray()).exec()
    }

    private fun getTargetUids(context: Context, config: CryptoUtils.ProxyConfig?): List<Int> {
        val pm = context.packageManager
        val uids = mutableListOf<Int>()

        DeviceData.TARGET_APPS.forEach { pkg ->
            try { uids.add(pm.getApplicationInfo(pkg, 0).uid) } catch (e: Exception) {}
        }
        if (config?.includeWebView == true) {
            DeviceData.WEBVIEW_PACKAGES.forEach { pkg ->
                try { uids.add(pm.getApplicationInfo(pkg, 0).uid) } catch (e: Exception) {}
            }
        }
        return uids.distinct()
    }

    private fun extractRedsocks(context: Context): File {
        val binFile = File(context.filesDir, "redsocks")
        if (!binFile.exists()) {
            context.assets.open("redsocks").use { input ->
                FileOutputStream(binFile).use { output -> input.copyTo(output) }
            }
            Shell.su("chmod 700 ${binFile.absolutePath}").exec() // Aislamiento SELinux
        }
        return binFile
    }

    private fun writeConfig(context: Context, config: CryptoUtils.ProxyConfig): File {
        val confFile = File(context.filesDir, "redsocks.conf")
        val auth = if (config.user.isNotEmpty()) "login = \"${config.user}\"; password = \"${config.pass}\";" else ""
        val conf = """
            base { log_debug = off; log_info = off; daemon = on; }
            redsocks { local_ip = 127.0.0.1; local_port = $PORT_TCP; ip = ${config.host}; port = ${config.port}; type = socks5; $auth }
            redudp { local_ip = 127.0.0.1; local_port = $PORT_UDP; ip = ${config.host}; port = ${config.port}; type = socks5; $auth }
        """.trimIndent()
        confFile.writeText(conf)
        return confFile
    }
}
