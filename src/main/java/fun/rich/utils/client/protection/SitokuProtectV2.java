/* package fun.rich.utils.client.protection;
// \\ CODED BY SITOKY // \\
import fun.rich.features.impl.misc.SelfDestruct;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
// CODED BY SITOKY
public final class SitokuProtectV2 {
    // \\ CODED BY SITOKY // \\
    private static final String[] HTTP_DEBUGGER_PROCESSES = {
            "fiddler", "Fiddler", "charles", "Charles", "httpdebugger", "HTTPDebugger",
            "wireshark", "Wireshark", "mitmproxy", "burpsuite", "Burp", "proxifier", "Proxifier"
    };
    // \\ CODED BY SITOKY // \\
    private static final String[] DEBUGGER_PROCESSES = {
            "x64dbg", "x32dbg", "ollydbg", "ida", "ida64", "ghidra", "cheatengine", "CheatEngine",
            "processhacker", "Procmon", "dnspy", "dnSpy", "ilspy", "ILSpy", "jadx", "frida", "frida-server",
            "xposed", "Xposed", "lsposed", "recaf", "Recaf"
    };
    // \\ CODED BY SITOKY // \\
    private static final String[] SUSPICIOUS_JVM_ARGS = {
            "-agentlib:jdwp", "-Xrunjdwp", "-agentpath", "-javaagent:", "-Xdebug",
            "transport=dt_socket", "suspend=y", "address=*:", "frida", "xposed"
    };
    // \\ CODED BY SITOKY // \\
    private static final int[] PROXY_PORTS = {8888, 8889, 8081, 3128};
    private static final int[] JDWP_PORTS = {5005, 5006, 8000};
    // \\ CODED BY SITOKY // \\
    private SitokuProtectV2() {}
    // CODED BY SITOKY
    public static boolean run() {
        return !checkDebuggerAttached() && !checkSuspiciousJvmArgs() && !checkProxyEnvironment()
                && !checkHttpDebuggerProcesses() && !checkDebuggerProcesses()
                && !checkProxyPortsListening() && !checkJdwpPorts();
    }
    // \\ CODED BY SITOKY // \\
    private static boolean checkDebuggerAttached() {
        try {
            List<String> args = ManagementFactory.getRuntimeMXBean().getInputArguments();
            if (args != null) {
                for (String arg : args) {
                    String lower = arg.toLowerCase();
                    if (lower.contains("jdwp") || lower.contains("-xdebug") ||
                            lower.contains("transport=dt_socket") || lower.contains("suspend=y")) {
                        return true;
                    }
                }
            }
        } catch (Throwable t) { return true; }
        return false;
    }
    // CODED BY SITOKY
    private static boolean checkSuspiciousJvmArgs() {
        try {
            List<String> args = ManagementFactory.getRuntimeMXBean().getInputArguments();
            if (args == null) return false;
            String all = String.join(" ", args).toLowerCase();
            for (String s : SUSPICIOUS_JVM_ARGS) {
                if (all.contains(s.toLowerCase())) return true;
            }
        } catch (Throwable t) { return true; }
        return false;
    }
    // CODED BY SITOKY
    private static boolean checkProxyEnvironment() {
        try {
            for (String var : new String[]{"HTTP_PROXY", "HTTPS_PROXY", "http_proxy", "https_proxy", "JAVA_TOOL_OPTIONS"}) {
                String val = System.getenv(var);
                if (val != null && !val.isEmpty()) {
                    String lower = val.toLowerCase();
                    if (lower.contains("fiddler") || lower.contains("charles") || lower.contains("127.0.0.1:8888") ||
                            lower.contains(":8889") || lower.contains("frida")) return true;
                }
            }
            if (Boolean.getBoolean("java.net.useSystemProxies")) return true;
        } catch (Throwable t) { return true; }
        return false;
    }

    // \\ CODED BY SITOKY // \\
    private static boolean checkHttpDebuggerProcesses() {
        return checkProcessList(HTTP_DEBUGGER_PROCESSES);
    }
    // \\ CODED BY SITOKY // \\
    private static boolean checkDebuggerProcesses() {
        return checkProcessList(DEBUGGER_PROCESSES);
    }
    // \\ CODED BY SITOKY // \\
    private static boolean checkProcessList(String[] processNames) {
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            List<String> command = new ArrayList<>();
            if (os.contains("win")) {
                command.add("tasklist");
                command.add("/FO");
                command.add("CSV");
                command.add("/NH");
            } else if (os.contains("linux") || os.contains("mac")) {
                command.add("ps");
                command.add("-e");
            } else return false;
            // \\ CODED BY SITOKY // \\
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) output.append(line).append("\n");
            }
            p.waitFor();
            String procList = output.toString().toLowerCase();
            // \\ CODED BY SITOKY // \\
            for (String proc : processNames) {
                if (procList.contains(proc.toLowerCase())) return true;
            }
        } catch (Throwable t) {}
        return false;
    }
    // \\ CODED BY SITOKY // \\
    private static boolean checkProxyPortsListening() {
        try {
            for (int port : PROXY_PORTS) {
                try (Socket s = new Socket()) {
                    s.connect(new java.net.InetSocketAddress("127.0.0.1", port), 50);
                    return true;
                } catch (Exception e) {}
            }
        } catch (Throwable t) { return true; }
        return false;
    }
    // \\ CODED BY SITOKY // \\
    private static boolean checkJdwpPorts() {
        try {
            for (int port : JDWP_PORTS) {
                try (Socket s = new Socket()) {
                    s.connect(new java.net.InetSocketAddress("127.0.0.1", port), 30);
                    return true;
                } catch (Exception e) {}
            }
        } catch (Throwable t) { return true; }
        return false;
    }
}
// \\ CODED BY SITOKY // \\
 */
