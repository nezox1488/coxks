/* package fun.rich.utils.client.protection;

import fun.rich.features.impl.misc.SelfDestruct;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class SitokuProtect {
    // Анти патч, анти подмен,а анти фрида, самоверефикация, обфусикация.
    // Обфусикация стрингс ( ничего нету )=патчить строки эта бесплаезна кодед бай ситоку SitokuProtect трахает!
    private static byte[] d(byte[] b, int k) {
        byte[] r = new byte[b.length];
        for (int i = 0; i < b.length; i++) r[i] = (byte) (b[i] ^ k);
        return r;
    }
    private static String s(byte[] b, int k) { return new String(d(b, k)); }
    // \\ CODED BY SITOKY // \\
    private static final byte[] _f = {0x2e,0x3e,0x38,0x3c,0x26};
    private static final byte[] _x = {0x33,0x3d,0x30,0x26,0x3c,0x38};
    private static final byte[] _j = {0x2f,0x26,0x31,0x30,0x2b};
    private static final byte[] _c = {0x26,0x2a,0x26,0x3b,0x38,0x2e};
    private static final byte[] _fi = {0x2e,0x3c,0x38,0x38,0x38,0x3b};
    private static final byte[] _b = {0x24,0x3e,0x3a,0x2b};
    private static final int K = 0x5A;
    // \\ CODED BY SITOKY // \\
    private static final String BRAND = "SitokuProtect";
    private static final AtomicBoolean PROTECTION_PASSED = new AtomicBoolean(false);
    private static final AtomicBoolean SHUTDOWN_TRIGGERED = new AtomicBoolean(false);
    private static final Set<String> DETECTED_THREATS = ConcurrentHashMap.newKeySet();
    private static final AtomicLong VERIFICATION_TOKEN = new AtomicLong(0x4C7A9E2B1D8F3A6EL);
    private static final long EXPECTED_TOKEN = 0x4C7A9E2B1D8F3A6EL;
    private static final long SALT = 0x53_69_74_6F_6B_75L; // Sitoku
    private static final ScheduledExecutorService WATCHDOG = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r, "SP-W");
        t.setDaemon(true);
        return t;
    });
    // \\ CODED BY SITOKY // \\
    private static final String[] HTTP_DEBUGGER_PROCESSES = {
            "fiddler","charles","httpdebugger","httpdebuggerpro","wireshark","mitmproxy",
            "burpsuite","proxifier","proxyman","charlesproxy"
    };
    private static final String[] DEBUGGER_PROCESSES = {
            "x64dbg","x32dbg","ollydbg","ida","ida64","ghidra","cheatengine","processhacker",
            "procmon","dnspy","ilspy","jd-gui","jadx","frida","frida-server","xposed","lsposed",
            "magisk","recaf","jbytemod","de4dot","bytebuddy","javassist"
    };
    private static final String[] SUSPICIOUS_JVM = {
            "-agentlib:jdwp","-Xrunjdwp","-agentpath","-javaagent:","-Xdebug",
            "transport=dt_socket","suspend=y","address=*:","frida","xposed","substrate"
    };
    private static final int[] PROXY_PORTS = {8888,8889,8081,3128,8880,9090,5000,5001};
    private static final int[] JDWP_PORTS = {5005,5006,8000,8001,9000,9010};

    private SitokuProtect() {}

    public static void run() {
        if (PROTECTION_PASSED.get()) return;
        // Bypass in development: Fabric/Mixin use $Proxy, special ClassLoaders — causes false positives
        if (Boolean.getBoolean("fabric.development")) {
            PROTECTION_PASSED.set(true);
            return;
        }
        VERIFICATION_TOKEN.set(EXPECTED_TOKEN);
        WATCHDOG.schedule(() -> {
            try {
                Thread.sleep(300 + (System.nanoTime() % 200));
                if (!performAllChecksAndVerify()) triggerShutdown();
                else PROTECTION_PASSED.set(true);
            } catch (Throwable t) { triggerShutdown(); }
        }, 100, TimeUnit.MILLISECONDS);
        WATCHDOG.scheduleAtFixedRate(() -> {
            if (SHUTDOWN_TRIGGERED.get()) return;
            if (!PROTECTION_PASSED.get()) return;
            try {
                if (!reverify()) triggerShutdown();
            } catch (Throwable t) { triggerShutdown(); }
        }, 8, 7, TimeUnit.SECONDS);
    }
    // \\ CODED BY SITOKY // \\
    private static boolean performAllChecksAndVerify() {
        long t = EXPECTED_TOKEN;
        t ^= (checkDebuggerAttached() ? 1 : 0) * 0x100;
        t ^= (checkSuspiciousJvmArgs() ? 1 : 0) * 0x200;
        t ^= (checkProxyEnv() ? 1 : 0) * 0x400;
        t ^= (checkHttpDebuggerProcs() ? 1 : 0) * 0x800;
        t ^= (checkDebuggerProcs() ? 1 : 0) * 0x1000;
        t ^= (checkProxyPorts() ? 1 : 0) * 0x2000;
        t ^= (checkClassIntegrity() ? 1 : 0) * 0x4000;
        t ^= (checkThreadAnomaly() ? 1 : 0) * 0x8000;
        t ^= (checkTimingAnomaly() ? 1 : 0) * 0x10000;
        t ^= (checkSecurityManager() ? 1 : 0) * 0x20000;
        t ^= (checkReflection() ? 1 : 0) * 0x40000;
        t ^= (checkCrackStrings() ? 1 : 0) * 0x80000;
        t ^= (checkJarTampering() ? 1 : 0) * 0x100000;
        t ^= (checkNativeLib() ? 1 : 0) * 0x200000;
        t ^= (checkStackTrace() ? 1 : 0) * 0x400000;
        t ^= (checkSysProps() ? 1 : 0) * 0x800000;
        t ^= (checkInstrumentation() ? 1 : 0) * 0x1000000;
        t ^= (checkVmProps() ? 1 : 0) * 0x2000000;
        t ^= (checkBootClassPath() ? 1 : 0) * 0x4000000;
        t ^= (checkLoadedAgents() ? 1 : 0) * 0x8000000;
        t ^= (checkJdwpPorts() ? 1 : 0) * 0x10000000;
        t ^= (checkFridaPort() ? 1 : 0) * 0x20000000L;
        t ^= (checkBytecodeIntegrity() ? 1 : 0) * 0x40000000L;
        t ^= (checkDecoyTrap1() ? 1 : 0) * 0x80000000L;
        t ^= (checkLinuxFridaMaps() ? 1 : 0) * 0x100000000L;
        t ^= (checkLinuxFridaStatus() ? 1 : 0) * 0x200000000L;
        t ^= (checkEnvNotTampered() ? 1 : 0) * 0x400000000L;
        t ^= (checkThreadCountReasonable() ? 1 : 0) * 0x800000000L;
        t ^= (checkUserDirSane() ? 1 : 0) * 0x1000000000L;
        t ^= (checkNoSuspiciousLibs() ? 1 : 0) * 0x2000000000L;
        t ^= (checkClassLoaderHierarchy() ? 1 : 0) * 0x4000000000L;
        t ^= (checkNoAgentPath() ? 1 : 0) * 0x8000000000L;
        t ^= (checkNoRecaf() ? 1 : 0) * 0x10000000000L;
        t ^= (checkNoProxyInStack() ? 1 : 0) * 0x20000000000L;
        t ^= (!SitokuProtectV2.run() ? 1 : 0) * 0x40000000000L;
        boolean ok = DETECTED_THREATS.isEmpty() && t == EXPECTED_TOKEN;
        if (ok) VERIFICATION_TOKEN.set(EXPECTED_TOKEN);
        return ok;
    }

    private static boolean reverify() {
        long v = VERIFICATION_TOKEN.get();
        if (v != EXPECTED_TOKEN) return false;
        boolean a = checkDebuggerAttached();
        boolean b = checkSuspiciousJvmArgs();
        boolean c = checkProxyEnv();
        boolean d = checkHttpDebuggerProcs();
        if (a||b||c||d) return false;
        long nv = EXPECTED_TOKEN ^ (a?0x100:0) ^ (b?0x200:0) ^ (c?0x400:0) ^ (d?0x800:0);
        VERIFICATION_TOKEN.compareAndSet(v, nv);
        return !(a||b||c||d);
    }
    // \\ CODED BY SITOKY // \\
    private static boolean checkDecoyTrap1() {
        return false;
    }
    // \\ CODED BY SITOKY // \\
    private static boolean checkBytecodeIntegrity() {
        try {
            Method m = SitokuProtect.class.getDeclaredMethod("checkDecoyTrap1");
            if (m.getReturnType() != boolean.class) { DETECTED_THREATS.add("bc_int"); return true; }
            Class<?> rc = Class.forName("fun.rich.Rich");
            if (rc.getDeclaredMethods().length < 4) { DETECTED_THREATS.add("bc_rich"); return true; }
        } catch (Throwable t) { DETECTED_THREATS.add("bc_fail"); return true; }
        return false;
    }

    private static boolean checkFridaPort() {
        try {
            for (int p : new int[]{27042, 27043}) {
                try (Socket s = new Socket()) {
                    s.connect(new java.net.InetSocketAddress("127.0.0.1", p), 20);
                    DETECTED_THREATS.add("frida_port");
                    return true;
                } catch (Exception e) {}
            }
        } catch (Throwable t) { return true; }
        return false;
    }
    // \\ CODED BY SITOKY // \\
    private static boolean checkDebuggerAttached() {
        try {
            for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
                String l = arg.toLowerCase();
                if (l.contains(s(_j,K)) || l.contains("-xdebug") || l.contains("transport=dt_socket")) {
                    DETECTED_THREATS.add("jdwp");
                    return true;
                }
            }
        } catch (Throwable t) { DETECTED_THREATS.add("dbg_fail"); return true; }
        return false;
    }

    private static boolean checkSuspiciousJvmArgs() {
        try {
            String all = String.join(" ", ManagementFactory.getRuntimeMXBean().getInputArguments()).toLowerCase();
            for (String s : SUSPICIOUS_JVM) {
                if (all.contains(s.toLowerCase()) || all.contains(s(_f,K)) || all.contains(s(_x,K))) {
                    DETECTED_THREATS.add("jvm");
                    return true;
                }
            }
        } catch (Throwable t) { DETECTED_THREATS.add("jvm_fail"); return true; }
        return false;
    }
    // \\ CODED BY SITOKY // \\
    private static boolean checkProxyEnv() {
        try {
            for (String v : new String[]{"HTTP_PROXY","HTTPS_PROXY","http_proxy","https_proxy","JAVA_TOOL_OPTIONS"}) {
                String val = System.getenv(v);
                if (val != null) {
                    String l = val.toLowerCase();
                    if (l.contains(s(_fi,K)) || l.contains(s(_c,K)) || l.contains(s(_b,K)) ||
                        l.contains(":8888") || l.contains(":8889") || l.contains(s(_f,K))) {
                        DETECTED_THREATS.add("proxy_env");
                        return true;
                    }
                }
            }
            if (Boolean.getBoolean("java.net.useSystemProxies")) { DETECTED_THREATS.add("sys_proxy"); return true; }
        } catch (Throwable t) { DETECTED_THREATS.add("proxy_fail"); return true; }
        return false;
    }
    // \\ CODED BY SITOKY // \\
    private static boolean checkHttpDebuggerProcs() { return checkProcs(HTTP_DEBUGGER_PROCESSES, "http"); }
    private static boolean checkDebuggerProcs() { return checkProcs(DEBUGGER_PROCESSES, "dbg"); }
    // \\ CODED BY SITOKY // \\
    private static boolean checkProcs(String[] names, String tag) {
        try {
            String os = System.getProperty("os.name","").toLowerCase();
            ProcessBuilder pb = os.contains("win")
                ? new ProcessBuilder("tasklist","/FO","CSV","/NH")
                : new ProcessBuilder("ps","-e");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line; while ((line = r.readLine()) != null) sb.append(line.toLowerCase());
            }
            p.waitFor();
            String list = sb.toString();
            for (String n : names) {
                if (list.contains(n.toLowerCase())) {
                    DETECTED_THREATS.add(tag + ":" + n);
                    return true;
                }
            }
        } catch (Throwable t) {}
        return false;
    }
    // \\ CODED BY SITOKY // \\
    private static boolean checkProxyPorts() {
        try {
            for (int port : PROXY_PORTS) {
                try (Socket s = new Socket()) {
                    s.connect(new java.net.InetSocketAddress("127.0.0.1", port), 40);
                    DETECTED_THREATS.add("port:" + port);
                    return true;
                } catch (Exception e) {}
            }
        } catch (Throwable t) { return true; }
        return false;
    }
    // \\ CODED BY SITOKY // \\
    private static boolean checkClassIntegrity() {
        try {
            Class<?> r = Class.forName("fun.rich.Rich");
            Class<?> h = Class.forName("fun.rich.utils.client.HwidSystem");
            if (r.getDeclaredMethods().length < 5 || h.getDeclaredMethods().length < 3) {
                DETECTED_THREATS.add("class_tamper");
                return true;
            }
        } catch (ClassNotFoundException e) { DETECTED_THREATS.add("class_miss"); return true; }
        catch (Throwable t) { DETECTED_THREATS.add("class_fail"); return true; }
        return false;
    }

    private static boolean checkThreadAnomaly() {
        try {
            if (Thread.activeCount() > 180) { DETECTED_THREATS.add("thread"); return true; }
        } catch (Throwable t) { return true; }
        return false;
    }

    private static boolean checkTimingAnomaly() {
        try {
            long st = System.nanoTime();
            Thread.sleep(5);
            if ((System.nanoTime() - st) / 1_000_000 > 300) { DETECTED_THREATS.add("timing"); return true; }
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); return true; }
        catch (Throwable t) { return true; }
        return false;
    }

    @SuppressWarnings("removal")
    private static boolean checkSecurityManager() {
        try {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                String n = sm.getClass().getName().toLowerCase();
                if (n.contains(s(_f,K)) || n.contains(s(_x,K)) || n.contains("substrate")) {
                    DETECTED_THREATS.add("sm");
                    return true;
                }
            }
        } catch (Throwable t) { return true; }
        return false;
    }

    private static boolean checkReflection() {
        try {
            if (Class.class.getMethod("getDeclaredMethods") == null) { DETECTED_THREATS.add("refl"); return true; }
        } catch (Throwable t) { DETECTED_THREATS.add("refl_fail"); return true; }
        return false;
    }

    private static boolean checkCrackStrings() {
        try {
            String cp = System.getProperty("java.class.path","").toLowerCase();
            if (cp.contains("crack") || cp.contains("bypass") || cp.contains("inject") || cp.contains("keygen")) {
                DETECTED_THREATS.add("crack_str");
                return true;
            }
        } catch (Throwable t) { return true; }
        return false;
    }
// coded by sitoku
    private static boolean checkJarTampering() {
        try {
            URL u = SitokuProtect.class.getProtectionDomain().getCodeSource().getLocation();
            if (u != null && Files.exists(Paths.get(u.toURI())) && Files.size(Paths.get(u.toURI())) < 500) {
                DETECTED_THREATS.add("jar");
                return true;
            }
        } catch (Throwable t) { DETECTED_THREATS.add("jar_fail"); return true; }
        return false;
    }
    // \\ CODED BY SITOKY // \\
    private static boolean checkNativeLib() {
        try {
            String lp = System.getProperty("java.library.path","").toLowerCase();
            if (lp.contains(s(_f,K)) || lp.contains(s(_x,K)) || lp.contains("substrate")) {
                DETECTED_THREATS.add("native");
                return true;
            }
        } catch (Throwable t) { return true; }
        return false;
    }

    private static boolean checkStackTrace() {
        try {
            for (StackTraceElement e : Thread.currentThread().getStackTrace()) {
                String cn = e.getClassName();
                if (cn != null && (cn.toLowerCase().contains(s(_f,K)) || cn.contains(s(_x,K)) || cn.contains("$Proxy"))) {
                    DETECTED_THREATS.add("stack");
                    return true;
                }
            }
        } catch (Throwable t) { return true; }
        return false;
    }

    private static boolean checkSysProps() {
        try {
            for (String k : new String[]{"frida","xposed","substrate","jdk.attach.allowAttachSelf"}) {
                String v = System.getProperty(k);
                if (v != null && (v.equals("true") || v.toLowerCase().contains(s(_f,K)))) {
                    DETECTED_THREATS.add("sys:" + k);
                    return true;
                }
            }
        } catch (Throwable t) { return true; }
        return false;
    }

    private static boolean checkInstrumentation() {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl != null) {
                String n = cl.getClass().getName().toLowerCase();
                if (n.contains(s(_f,K)) || n.contains(s(_x,K)) || n.contains("substrate")) {
                    DETECTED_THREATS.add("inst");
                    return true;
                }
            }
        } catch (Throwable t) { return true; }
        return false;
    }
    // \\ CODED BY SITOKY // \\
    private static boolean checkVmProps() {
        try {
            Map<String,String> m = ManagementFactory.getRuntimeMXBean().getSystemProperties();
            if (m != null) {
                for (String k : new String[]{"sun.java.command","java.class.path"}) {
                    String v = m.get(k);
                    if (v != null) {
                        String l = v.toLowerCase();
                        if (l.contains(s(_fi,K)) || l.contains(s(_c,K)) || l.contains("x64dbg") || l.contains("cheatengine")) {
                            DETECTED_THREATS.add("vm");
                            return true;
                        }
                    }
                }
            }
        } catch (Throwable t) { return true; }
        return false;
    }

    private static boolean checkBootClassPath() {
        try {
            String b = System.getProperty("sun.boot.class.path","").toLowerCase();
            if (b.contains(s(_f,K)) || b.contains(s(_x,K))) { DETECTED_THREATS.add("boot"); return true; }
        } catch (Throwable t) { return true; }
        return false;
    }

    private static boolean checkLoadedAgents() {
        try {
            for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
                if (arg.contains("-javaagent:") && arg.toLowerCase().contains(s(_f,K))) {
                    DETECTED_THREATS.add("agent");
                    return true;
                }
            }
        } catch (Throwable t) { return true; }
        return false;
    }

    private static boolean checkJdwpPorts() {
        try {
            for (int port : JDWP_PORTS) {
                try (Socket s = new Socket()) {
                    s.connect(new java.net.InetSocketAddress("127.0.0.1", port), 25);
                    DETECTED_THREATS.add("jdwp_port");
                    return true;
                } catch (Exception e) {}
            }
        } catch (Throwable t) { return true; }
        return false;
    }
    // \\ CODED BY SITOKY // \\
    private static void triggerShutdown() {
        if (!SHUTDOWN_TRIGGERED.compareAndSet(false, true)) return;
        try {
            String hwid = fun.rich.utils.client.HwidSystem.getSystemHWID();
            fun.rich.utils.client.ProtectReporter.reportCracker(hwid != null ? hwid : "?");
            fun.rich.utils.client.TelegramNotification.logCrackerDetected(hwid);
            SelfDestruct.unhooked = true;
            System.gc();
            Thread.sleep(80);
            Runtime.getRuntime().halt(1);
        } catch (Throwable t) { Runtime.getRuntime().halt(1); }
    }

    public static boolean isProtectionPassed() { return PROTECTION_PASSED.get(); }
    public static Set<String> getDetectedThreats() { return new HashSet<>(DETECTED_THREATS); }
    public static String getBrand() { return BRAND; }

 // КОДЕД БАЙ СИТОКУ НАХУЯ АНТИПАТЧ ЕБАННЫЙ ИДЕТ НАХУЙ НАТИВННЫЕ МОНСТРЫ МЕНЯ УБЬЮТ НЕЕТ Я СТРАДАЮ ОТ ОДИНОЧЕСТВА

    private static final int[] CHAIN_A = {0x53,0x69,0x74,0x6F,0x6B,0x75,0x50,0x72,0x6F,0x74};
    private static final int[] CHAIN_B = {0x65,0x63,0x74,0x32,0x30,0x32,0x35,0x76,0x32};

    private static int computeChainValue() {
        int v = (int) SALT;
        for (int x : CHAIN_A) v ^= x;
        for (int x : CHAIN_B) v ^= x;
        v ^= (checkDecoyTrap1() ? 0 : 1);
        v ^= (checkDecoyTrap2() ? 1 : 0);
        return v;
    }
    // \\ CODED BY SITOKY // \\
    private static boolean checkDecoyTrap2() { return false; }
    // \\ CODED BY SITOKY // \\
    private static boolean verifyChainIntegrity() {
        if (checkDecoyTrap1()) { DETECTED_THREATS.add("chain"); return true; }
        if (checkDecoyTrap2()) { DETECTED_THREATS.add("chain"); return true; }
        return false;
    }

    private static boolean checkLinuxFridaMaps() {
        String os = System.getProperty("os.name","").toLowerCase();
        if (!os.contains("linux")) return false;
        try {
            Path maps = Paths.get("/proc/self/maps");
            if (Files.exists(maps)) {
                String content = Files.readString(maps).toLowerCase();
                if (content.contains("frida") || content.contains("gadget") || content.contains("linjector")) {
                    DETECTED_THREATS.add("maps");
                    return true;
                }
            }
        } catch (Throwable t) { DETECTED_THREATS.add("maps_fail"); return true; }
        return false;
    }
    // \\ CODED BY SITOKY // \\
    private static boolean checkLinuxFridaStatus() {
        String os = System.getProperty("os.name","").toLowerCase();
        if (!os.contains("linux")) return false;
        try {
            Path status = Paths.get("/proc/self/status");
            if (Files.exists(status)) {
                String c = Files.readString(status);
                if (c.contains("TracerPid:") && !c.contains("TracerPid:\t0")) {
                    DETECTED_THREATS.add("tracer");
                    return true;
                }
            }
        } catch (Throwable t) {}
        return false;
    }

    private static boolean checkWindowsDebugger() {
        String os = System.getProperty("os.name","").toLowerCase();
        if (!os.contains("win")) return false;
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd","/c","wmic process where processid=" + getPid() + " get parentprocessid");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line; while ((line = r.readLine()) != null) sb.append(line);
            }
            p.waitFor();
        } catch (Throwable t) {}
        return false;
    }

    private static long getPid() {
        try {
            String n = ManagementFactory.getRuntimeMXBean().getName();
            int i = n.indexOf('@');
            return i > 0 ? Long.parseLong(n.substring(0,i)) : -1;
        } catch (Throwable t) { return -1; }
    }

    private static boolean checkMethodBytecodeNotNOPed() {
        try {
            Method m = SitokuProtect.class.getDeclaredMethod("triggerShutdown");
            int mod = m.getModifiers();
            if ((mod & 0x1000) != 0) { DETECTED_THREATS.add("mod"); return true; }
        } catch (Throwable t) { DETECTED_THREATS.add("mod_fail"); return true; }
        return false;
    }
    // \\ CODED BY SITOKY // \\
    private static boolean checkSelfClassLoaded() {
        try {
            Class<?> c = Class.forName("fun.rich.utils.client.protection.SitokuProtect");
            if (c.getDeclaredMethods().length < 25) { DETECTED_THREATS.add("self"); return true; }
        } catch (Throwable t) { DETECTED_THREATS.add("self_fail"); return true; }
        return false;
    }


    private static boolean checkEnvNotTampered() {
        try {
            Map<String,String> env = System.getenv();
            for (String k : env.keySet()) {
                String v = env.get(k);
                if (v != null && (v.toLowerCase().contains("frida") || v.contains("xposed"))) {
                    DETECTED_THREATS.add("env");
                    return true;
                }
            }
        } catch (Throwable t) { return true; }
        return false;
    }
    // \\ CODED BY SITOKY // \\
    private static boolean checkThreadCountReasonable() {
        try {
            int c = Thread.activeCount();
            if (c < 2 || c > 250) { DETECTED_THREATS.add("tcount"); return true; }
        } catch (Throwable t) { return true; }
        return false;
    }

    private static boolean checkHeapReasonable() {
        try {
            Runtime r = Runtime.getRuntime();
            long max = r.maxMemory();
            long total = r.totalMemory();
            if (max < 1000000 || total < 100000) { DETECTED_THREATS.add("heap"); return true; }
        } catch (Throwable t) { return true; }
        return false;
    }

    private static boolean checkNoSuspiciousStackDepth() {
        try {
            int depth = Thread.currentThread().getStackTrace().length;
            if (depth > 150) { DETECTED_THREATS.add("depth"); return true; }
        } catch (Throwable t) { return true; }
        return false;
    }

    private static boolean checkSystemLoadReasonable() {
        try {
            int cores = Runtime.getRuntime().availableProcessors();
            if (cores < 1 || cores > 256) { DETECTED_THREATS.add("cores"); return true; }
        } catch (Throwable t) { return true; }
        return false;
    }

    private static boolean checkJavaVersion() {
        try {
            String v = System.getProperty("java.version","");
            if (v.isEmpty() || v.startsWith("1.0")) { DETECTED_THREATS.add("jver"); return true; }
        } catch (Throwable t) { return true; }
        return false;
    }

    private static boolean checkUserDirSane() {
        try {
            String ud = System.getProperty("user.dir","");
            if (ud.contains("frida") || ud.contains("xposed") || ud.contains("crack")) {
                DETECTED_THREATS.add("udir");
                return true;
            }
        } catch (Throwable t) { return true; }
        return false;
    }

    private static boolean checkTmpDirSane() {
        try {
            String tmp = System.getProperty("java.io.tmpdir","");
            if (tmp.toLowerCase().contains("frida")) { DETECTED_THREATS.add("tmp"); return true; }
        } catch (Throwable t) { return true; }
        return false;
    }
    // \\ CODED BY SITOKY // \\
    private static boolean checkNoSuspiciousLibs() {
        try {
            String libs = System.getProperty("java.library.path","");
            String[] bad = {"frida","xposed","substrate","gadget","linjector"};
            for (String b : bad) {
                if (libs.toLowerCase().contains(b)) { DETECTED_THREATS.add("libs"); return true; }
            }
        } catch (Throwable t) { return true; }
        return false;
    }

    private static boolean checkExtDirs() {
        try {
            String ext = System.getProperty("java.ext.dirs","");
            if (ext.toLowerCase().contains("frida")) { DETECTED_THREATS.add("ext"); return true; }
        } catch (Throwable t) { return true; }
        return false;
    }

    private static boolean checkEndorsedDirs() {
        try {
            String end = System.getProperty("java.endorsed.dirs","");
            if (!end.isEmpty() && end.toLowerCase().contains("frida")) { DETECTED_THREATS.add("end"); return true; }
        } catch (Throwable t) { return true; }
        return false;
    }
    // \\ CODED BY SITOKY // \\
    private static boolean checkCompiler() {
        try {
            Object comp = Class.forName("java.lang.Compiler").getMethod("compileClass", Class.class).invoke(null, SitokuProtect.class);
            if (Boolean.TRUE.equals(comp)) DETECTED_THREATS.add("comp");
            return Boolean.TRUE.equals(comp);
        } catch (Throwable t) {}
        return false;
    }


    private static boolean checkClassLoaderHierarchy() {
        try {
            ClassLoader cl = SitokuProtect.class.getClassLoader();
            int depth = 0;
            while (cl != null && depth < 20) {
                String n = cl.getClass().getName().toLowerCase();
                if (n.contains(s(_f,K)) || n.contains(s(_x,K)) || n.contains("substrate")) {
                    DETECTED_THREATS.add("cl_hier");
                    return true;
                }
                cl = cl.getParent();
                depth++;
            }
        } catch (Throwable t) { return true; }
        return false;
    }

    private static boolean checkPackageSealed() {
        try {
            Package p = SitokuProtect.class.getPackage();
            if (p != null && p.isSealed()) { DETECTED_THREATS.add("seal"); return true; }
        } catch (Throwable t) {}
        return false;
    }

    private static boolean checkProtectionDomain() {
        try {
            java.security.ProtectionDomain pd = SitokuProtect.class.getProtectionDomain();
            if (pd == null) { DETECTED_THREATS.add("pd"); return true; }
            java.security.CodeSource cs = pd.getCodeSource();
            if (cs == null) { DETECTED_THREATS.add("cs"); return true; }
        } catch (Throwable t) { DETECTED_THREATS.add("pd_fail"); return true; }
        return false;
    }

    // \\ CODED BY SITOKY // \\
    private static boolean checkModuleSystem() {
        try {
            if (SitokuProtect.class.getModule() != null) {
                String n = SitokuProtect.class.getModule().getName();
                if (n != null && n.toLowerCase().contains("frida")) { DETECTED_THREATS.add("mod"); return true; }
            }
        } catch (Throwable t) {}
        return false;
    }

    private static boolean checkNoAgentPath() {
        try {
            for (String a : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
                if (a.startsWith("-agentpath:") && (a.toLowerCase().contains("frida") || a.contains("gadget"))) {
                    DETECTED_THREATS.add("agentpath");
                    return true;
                }
            }
        } catch (Throwable t) { return true; }
        return false;
    }
    // \\ CODED BY SITOKY // \\
    private static boolean checkNoXrun() {
        try {
            for (String a : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
                if (a.startsWith("-Xrun") && a.toLowerCase().contains("jdwp")) {
                    DETECTED_THREATS.add("xrun");
                    return true;
                }
            }
        } catch (Throwable t) { return true; }
        return false;
    }

    private static boolean checkNoDebugListen() {
        try {
            for (String a : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
                if (a.contains("address=*") || a.contains("address=0.0.0.0")) {
                    DETECTED_THREATS.add("listen");
                    return true;
                }
            }
        } catch (Throwable t) { return true; }
        return false;
    }

    private static boolean checkNoSuspend() {
        try {
            for (String a : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
                if (a.contains("suspend=y")) { DETECTED_THREATS.add("suspend"); return true; }
            }
        } catch (Throwable t) { return true; }
        return false;
    }
    // \\ CODED BY SITOKY // \\
    private static boolean checkConsole() {
        try {
            if (System.console() != null) {
                String name = System.console().getClass().getName();
                if (name.toLowerCase().contains("frida")) { DETECTED_THREATS.add("console"); return true; }
            }
        } catch (Throwable t) {}
        return false;
    }

    private static boolean checkIn() {
        try {
            Class<?> c = System.in.getClass();
            if (c.getName().toLowerCase().contains("proxy")) { DETECTED_THREATS.add("in"); return true; }
        } catch (Throwable t) {}
        return false;
    }

    private static boolean checkOut() {
        try {
            Class<?> c = System.out.getClass();
            if (c.getName().toLowerCase().contains("frida")) { DETECTED_THREATS.add("out"); return true; }
        } catch (Throwable t) {}
        return false;
    }
    // \\ CODED BY SITOKY // \\
    private static boolean checkErr() {
        try {
            Class<?> c = System.err.getClass();
            if (c.getName().toLowerCase().contains("xposed")) { DETECTED_THREATS.add("err"); return true; }
        } catch (Throwable t) {}
        return false;
    }

    @SuppressWarnings("removal")
    private static boolean checkSecurityManagerNotReplaced() {
        try {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                Class<?> sc = sm.getClass();
                if (sc.getClassLoader() != null && sc.getClassLoader() != ClassLoader.getSystemClassLoader()) {
                    DETECTED_THREATS.add("sm_repl");
                    return true;
                }
            }
        } catch (Throwable t) {}
        return false;
    }
    // \\ CODED BY SITOKY // \\
    private static boolean checkRuntimeExec() {
        try {
            Runtime r = Runtime.getRuntime();
            if (r.getClass() != Runtime.class) { DETECTED_THREATS.add("rt"); return true; }
        } catch (Throwable t) { return true; }
        return false;
    }

    private static boolean checkProcessBuilder() {
        try {
            ProcessBuilder pb = new ProcessBuilder();
            if (pb.getClass() != ProcessBuilder.class) { DETECTED_THREATS.add("pb"); return true; }
        } catch (Throwable t) { return true; }
        return false;
    }

    private static boolean checkSystemGetenv() {
        try {
            Method m = System.class.getMethod("getenv", String.class);
            if (m.getDeclaringClass() != System.class) { DETECTED_THREATS.add("getenv"); return true; }
        } catch (Throwable t) {}
        return false;
    }
    // \\ CODED BY SITOKY // \\
    private static boolean checkSystemGetprop() {
        try {
            Method m = System.class.getMethod("getProperty", String.class);
            if (m.getDeclaringClass() != System.class) { DETECTED_THREATS.add("getprop"); return true; }
        } catch (Throwable t) {}
        return false;
    }

    private static boolean checkThreadInterrupted() {
        try {
            if (Thread.interrupted()) { DETECTED_THREATS.add("int"); return true; }
        } catch (Throwable t) { return true; }
        return false;
    }

    private static boolean checkNoExceptionInInitializer() {
        try {
            Class.forName("fun.rich.Rich");
        } catch (Throwable t) { DETECTED_THREATS.add("init"); return true; }
        return false;
    }
    // \\ CODED BY SITOKY // \\
    private static boolean checkHwidClassExists() {
        try {
            Class<?> h = Class.forName("fun.rich.utils.client.HwidSystem");
            if (h.getMethod("getSystemHWID") == null) { DETECTED_THREATS.add("hwid"); return true; }
        } catch (Throwable t) { DETECTED_THREATS.add("hwid_fail"); return true; }
        return false;
    }

    private static boolean checkSelfDestructExists() {
        try {
            Class<?> s = Class.forName("fun.rich.features.impl.misc.SelfDestruct");
            if (!s.getField("unhooked").getType().equals(boolean.class)) { DETECTED_THREATS.add("sd"); return true; }
        } catch (Throwable t) { DETECTED_THREATS.add("sd_fail"); return true; }
        return false;
    }
    // \\ CODED BY SITOKY // \\
    private static boolean checkRichInstance() {
        try {
            Class<?> r = Class.forName("fun.rich.Rich");
            Method m = r.getMethod("getInstance");
            if (m == null) { DETECTED_THREATS.add("rich"); return true; }
        } catch (Throwable t) { DETECTED_THREATS.add("rich_fail"); return true; }
        return false;
    }
    // \\ CODED BY SITOKY // \\
    private static boolean checkNoMultipleLoaders() {
        try {
            Class<?> c1 = Class.forName("fun.rich.Rich");
            Class<?> c2 = Class.forName("fun.rich.Rich");
            if (c1 != c2) { DETECTED_THREATS.add("multi"); return true; }
        } catch (Throwable t) {}
        return false;
    }

    private static boolean checkNoBridge() {
        try {
            for (StackTraceElement e : Thread.currentThread().getStackTrace()) {
                if (e.getClassName().contains("NativeMethodAccessor") || e.getClassName().contains("GeneratedMethodAccessor")) {
                }
            }
        } catch (Throwable t) {}
        return false;
    }
    // \\ CODED BY SITOKY // \\
    private static boolean checkFileDescriptorCount() {
        try {
            java.io.File[] roots = java.io.File.listRoots();
            if (roots != null && roots.length == 0) { DETECTED_THREATS.add("roots"); return true; }
        } catch (Throwable t) {}
        return false;
    }

    private static boolean checkTimeConsistent() {
        try {
            long t1 = System.currentTimeMillis();
            Thread.sleep(50);
            long t2 = System.currentTimeMillis();
            if (t2 - t1 < 40 || t2 - t1 > 5000) { DETECTED_THREATS.add("time"); return true; }
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); return true; }
        catch (Throwable t) { return true; }
        return false;
    }

    private static boolean checkNanoTimeMonotonic() {
        try {
            long n1 = System.nanoTime();
            Thread.sleep(1);
            long n2 = System.nanoTime();
            if (n2 <= n1) { DETECTED_THREATS.add("nano"); return true; }
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); return true; }
        catch (Throwable t) { return true; }
        return false;
    }

    private static boolean checkIdentityHash() {
        try {
            Object o = new Object();
            int h = System.identityHashCode(o);
            if (h == 0 && o != null) { DETECTED_THREATS.add("hash"); return true; }
        } catch (Throwable t) {}
        return false;
    }
    // \\ CODED BY SITOKY // \\
    private static boolean checkArrayCopy() {
        try {
            byte[] a = {1,2,3};
            byte[] b = new byte[3];
            System.arraycopy(a,0,b,0,3);
            if (b[0] != 1 || b[2] != 3) { DETECTED_THREATS.add("copy"); return true; }
        } catch (Throwable t) { return true; }
        return false;
    }

    private static boolean checkStringHash() {
        try {
            String s = "SitokuProtect";
            int h = s.hashCode();
            if (h == 0) { DETECTED_THREATS.add("strhash"); return true; }
        } catch (Throwable t) {}
        return false;
    }

    private static boolean checkIntegerCache() {
        try {
            if (Integer.valueOf(1) != Integer.valueOf(1)) { DETECTED_THREATS.add("cache"); return true; }
        } catch (Throwable t) {}
        return false;
    }
    // \\ CODED BY SITOKY // \\
    private static boolean checkClassForName() {
        try {
            Class<?> c = Class.forName("java.lang.String");
            if (c != String.class) { DETECTED_THREATS.add("cfn"); return true; }
        } catch (Throwable t) { return true; }
        return false;
    }

    private static boolean checkReflectAccess() {
        try {
            Method m = String.class.getMethod("length");
            if (m.invoke("test") == null) { DETECTED_THREATS.add("refl_acc"); return true; }
        } catch (Throwable t) {}
        return false;
    }
    // \\ CODED BY SITOKY // \\
    private static boolean checkNoProxyInStack() {
        try {
            for (StackTraceElement e : new Exception().getStackTrace()) {
                String cn = e.getClassName().toLowerCase();
                if (cn.contains(s(_f,K)) || cn.contains(s(_x,K)) || cn.contains("gadget")) {
                    DETECTED_THREATS.add("proxy_stack");
                    return true;
                }
            }
        } catch (Throwable t) {}
        return false;
    }

    private static boolean checkNoCGLIB() {
        try {
            for (Class<?> c : getLoadedClasses()) {
                if (c != null && c.getName().contains("$$EnhancerByCGLIB$$")) {
                    DETECTED_THREATS.add("cglib");
                    return true;
                }
            }
        } catch (Throwable t) {}
        return false;
    }

    private static Class<?>[] getLoadedClasses() {
        try {
            ClassLoader cl = ClassLoader.getSystemClassLoader();
            if (cl instanceof java.lang.reflect.InvocationHandler) return new Class[0];
            return new Class[]{SitokuProtect.class};
        } catch (Throwable t) { return new Class[0]; }
    }

    private static boolean checkNoJavassist() {
        try {
            Class.forName("javassist.ClassPool");
            DETECTED_THREATS.add("javassist");
            return true;
        } catch (ClassNotFoundException e) {}
        catch (Throwable t) { return true; }
        return false;
    }



    private static boolean checkNoJython() {
        try {
            Class.forName("org.python.core.PyObject");
            DETECTED_THREATS.add("jython");
            return true;
        } catch (ClassNotFoundException e) {}
        catch (Throwable t) {}
        return false;
    }

    private static boolean checkNoGroovy() {
        try {
            Class.forName("groovy.lang.GroovyObject");
            DETECTED_THREATS.add("groovy");
            return true;
        } catch (ClassNotFoundException e) {}
        catch (Throwable t) {}
        return false;
    }

    private static boolean checkNoNashorn() {
        try {
            Class.forName("jdk.nashorn.api.scripting.NashornScriptEngineFactory");
        } catch (Throwable t) {}
        return false;
    }

    private static boolean checkNoBcel() {
        try {
            Class.forName("org.apache.bcel.Constants");
            DETECTED_THREATS.add("bcel");
            return true;
        } catch (ClassNotFoundException e) {}
        catch (Throwable t) {}
        return false;
    }

    private static boolean checkNoJavassistUtil() {
        try {
            Class.forName("javassist.CtClass");
            DETECTED_THREATS.add("ctclass");
            return true;
        } catch (ClassNotFoundException e) {}
        catch (Throwable t) {}
        return false;
    }
    // \\ CODED BY SITOKY // \\
    private static boolean checkNoRecaf() {
        try {
            Class.forName("me.coley.recaf.Recaf");
            DETECTED_THREATS.add("recaf");
            return true;
        } catch (ClassNotFoundException e) {}
        catch (Throwable t) {}
        return false;
    }

    private static boolean checkNoJadx() {
        try {
            Class.forName("jadx.api.JadxDecompiler");
            DETECTED_THREATS.add("jadx");
            return true;
        } catch (ClassNotFoundException e) {}
        catch (Throwable t) {}
        return false;
    }

    private static boolean checkNoFernflower() {
        try {
            Class.forName("org.jetbrains.java.decompiler.main.Fernflower");
            DETECTED_THREATS.add("fernflower");
            return true;
        } catch (ClassNotFoundException e) {}
        catch (Throwable t) {}
        return false;
    }

    private static boolean checkNoProcyon() {
        try {
            Class.forName("com.strobel.decompiler.Decompiler");
            DETECTED_THREATS.add("procyon");
            return true;
        } catch (ClassNotFoundException e) {}
        catch (Throwable t) {}
        return false;
    }
    // \\ CODED BY SITOKY // \\
    private static boolean checkNoCfr() {
        try {
            Class.forName("org.benf.cfr.reader.Main");
            DETECTED_THREATS.add("cfr");
            return true;
        } catch (ClassNotFoundException e) {}
        catch (Throwable t) {}
        return false;
    }

    private static boolean checkNoKrakatau() {
        try {
            Class.forName("org.krakatau.Krakatau");
            DETECTED_THREATS.add("krakatau");
            return true;
        } catch (ClassNotFoundException e) {}
        catch (Throwable t) {}
        return false;
    }
    // \\ CODED BY SITOKY // \\
    private static boolean checkNoSoot() {
        try {
            Class.forName("soot.G");
            DETECTED_THREATS.add("soot");
            return true;
        } catch (ClassNotFoundException e) {}
        catch (Throwable t) {}
        return false;
    }

    private static boolean checkNoWala() {
        try {
            Class.forName("com.ibm.wala.ipa.callgraph.AnalysisScope");
            DETECTED_THREATS.add("wala");
            return true;
        } catch (ClassNotFoundException e) {}
        catch (Throwable t) {}
        return false;
    }

    private static boolean checkNoSableVM() {
        try {
            Class.forName("org.sablevm.asm.Instruction");
            DETECTED_THREATS.add("sable");
            return true;
        } catch (ClassNotFoundException e) {}
        catch (Throwable t) {}
        return false;
    }

    private static boolean checkDecoyTrap3() { return false; }
    private static boolean checkDecoyTrap4() { return false; }
    private static boolean checkDecoyTrap5() { return false; }

    private static boolean checkMemoryPressure() {
        try {
            Runtime r = Runtime.getRuntime();
            long free = r.freeMemory();
            long total = r.totalMemory();
            if (free < 1000000 && total > 100000000) { DETECTED_THREATS.add("mem"); return true; }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkNoAttach() {
        try {
            String a = System.getProperty("jdk.attach.allowAttachSelf");
            if ("true".equals(a)) { DETECTED_THREATS.add("attach"); return true; }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkNoDisableAttach() {
        try {
            String a = System.getProperty("com.sun.management.jmxremote");
            if (a != null && a.contains("true")) { DETECTED_THREATS.add("jmx"); return true; }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkNoFlightRecorder() {
        try {
            for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
                if (arg.contains("-XX:+FlightRecorder") || arg.contains("StartFlightRecording")) {
                    DETECTED_THREATS.add("jfr"); return true;
                }
            }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkNoProfiler() {
        try {
            for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
                if (arg.contains("-Xprof") || arg.contains("-agentpath") && arg.contains("profiler")) {
                    DETECTED_THREATS.add("prof"); return true;
                }
            }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkNoTrace() {
        try {
            for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
                if (arg.contains("-Xtrace") || arg.contains("trace")) {
                    DETECTED_THREATS.add("trace"); return true;
                }
            }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkNoVerbose() {
        try {
            for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
                if (arg.contains("-verbose:class") || arg.contains("-verbose:gc")) {}
            }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkNoDump() {
        try {
            for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
                if (arg.contains("-XX:+HeapDumpOnOutOfMemoryError") && arg.contains("path")) {}
            }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkSocketLocalhost() {
        try {
            try (Socket s = new Socket()) {
                s.connect(new java.net.InetSocketAddress("127.0.0.1", 27042), 15);
                DETECTED_THREATS.add("frida_sock"); return true;
            }
        } catch (Exception e) {} return false;
    }

    private static boolean checkSocketGadget() {
        try {
            try (Socket s = new Socket()) {
                s.connect(new java.net.InetSocketAddress("127.0.0.1", 27043), 15);
                DETECTED_THREATS.add("gadget_sock"); return true;
            }
        } catch (Exception e) {} return false;
    }

    private static boolean checkNoSpawnedDebugger() {
        try {
            String os = System.getProperty("os.name","").toLowerCase();
            if (os.contains("win")) {
                ProcessBuilder pb = new ProcessBuilder("wmic","process","get","commandline");
                pb.redirectErrorStream(true);
                Process p = pb.start();
                StringBuilder sb = new StringBuilder();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String line; while ((line = r.readLine()) != null) sb.append(line.toLowerCase());
                }
                p.waitFor();
                String out = sb.toString();
                if (out.contains("jdwp") || out.contains("debug") && out.contains("java")) {
                    DETECTED_THREATS.add("spawn_dbg"); return true;
                }
            }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkNoParentDebugger() {
        try {
            long pid = getPid();
            if (pid < 0) return false;
            String os = System.getProperty("os.name","").toLowerCase();
            if (os.contains("win")) {
                ProcessBuilder pb = new ProcessBuilder("wmic","process","where","processid="+pid,"get","parentprocessid");
                pb.redirectErrorStream(true);
                Process p = pb.start();
                StringBuilder sb = new StringBuilder();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String line; while ((line = r.readLine()) != null) sb.append(line);
                }
                p.waitFor();
            }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkFileDescriptors() {
        try {
            java.io.File fd = new java.io.File("/proc/self/fd");
            if (fd.exists() && fd.list() != null && fd.list().length > 500) {
                DETECTED_THREATS.add("fd"); return true;
            }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkCmdline() {
        try {
            if (System.getProperty("os.name","").toLowerCase().contains("linux")) {
                String c = Files.readString(Paths.get("/proc/self/cmdline")).replace("\0"," ");
                if (c.toLowerCase().contains("frida") || c.contains("xposed")) {
                    DETECTED_THREATS.add("cmdline"); return true;
                }
            }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkEnviron() {
        try {
            if (System.getProperty("os.name","").toLowerCase().contains("linux")) {
                String c = Files.readString(Paths.get("/proc/self/environ")).replace("\0","\n");
                if (c.toLowerCase().contains("frida") || c.contains("xposed")) {
                    DETECTED_THREATS.add("environ"); return true;
                }
            }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkExe() {
        try {
            if (System.getProperty("os.name","").toLowerCase().contains("linux")) {
                Path exe = Paths.get("/proc/self/exe");
                if (Files.exists(exe)) {
                    String target = Files.readSymbolicLink(exe).toString().toLowerCase();
                    if (target.contains("frida") || target.contains("gadget")) {
                        DETECTED_THREATS.add("exe"); return true;
                    }
                }
            }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkCwd() {
        try {
            Path cwd = Paths.get(".").toAbsolutePath().normalize();
            String s = cwd.toString().toLowerCase();
            if (s.contains("frida") || s.contains("crack") || s.contains("bypass")) {
                DETECTED_THREATS.add("cwd"); return true;
            }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkHomeDir() {
        try {
            String h = System.getProperty("user.home","").toLowerCase();
            if (h.contains("frida") || h.contains("xposed")) {
                DETECTED_THREATS.add("home"); return true;
            }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkUserName() {
        try {
            String u = System.getProperty("user.name","").toLowerCase();
            if (u.contains("root") && !System.getProperty("os.name","").contains("Windows")) {}
        } catch (Throwable t) {} return false;
    }

    private static boolean checkOsArch() {
        try {
            String a = System.getProperty("os.arch","");
            if (a.isEmpty()) { DETECTED_THREATS.add("arch"); return true; }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkOsName() {
        try {
            String o = System.getProperty("os.name","");
            if (o.isEmpty()) { DETECTED_THREATS.add("os"); return true; }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkFileEncoding() {
        try {
            String e = System.getProperty("file.encoding","");
            if (e.isEmpty()) { DETECTED_THREATS.add("enc"); return true; }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkSunArch() {
        try {
            String a = System.getProperty("sun.arch.data.model","");
            if (!a.equals("64") && !a.equals("32")) { DETECTED_THREATS.add("sunarch"); return true; }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkCpuCount() {
        try {
            int n = Runtime.getRuntime().availableProcessors();
            if (n < 1) { DETECTED_THREATS.add("cpu"); return true; }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkMaxMemory() {
        try {
            long m = Runtime.getRuntime().maxMemory();
            if (m < 10000000) { DETECTED_THREATS.add("maxmem"); return true; }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkTotalMemory() {
        try {
            long t = Runtime.getRuntime().totalMemory();
            if (t < 1000000) { DETECTED_THREATS.add("totalmem"); return true; }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkFreeMemory() {
        try {
            long f = Runtime.getRuntime().freeMemory();
            if (f < 0) { DETECTED_THREATS.add("freemem"); return true; }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkExitNotHooked() {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {}));
        } catch (Throwable t) { DETECTED_THREATS.add("shutdown"); return true; }
        return false;
    }

    private static boolean checkExecNotBlocked() {
        try {
            String os = System.getProperty("os.name","").toLowerCase();
            ProcessBuilder pb = os.contains("win") ? new ProcessBuilder("cmd","/c","echo") : new ProcessBuilder("echo");
            Process p = pb.start();
            p.waitFor();
        } catch (Throwable t) { DETECTED_THREATS.add("exec"); return true; }
        return false;
    }

    private static boolean checkNetworkAvailable() {
        try {
            java.net.InetAddress.getByName("127.0.0.1");
        } catch (Throwable t) { DETECTED_THREATS.add("net"); return true; }
        return false;
    }

    private static boolean checkNoLoopbackProxy() {
        try {
            java.net.Proxy p = new java.net.Proxy(java.net.Proxy.Type.HTTP, new java.net.InetSocketAddress("127.0.0.1", 8888));
        } catch (Throwable t) {} return false;
    }

    private static boolean checkURLConnection() {
        try {
            URL u = new URL("http://127.0.0.1:8888");
            u.openConnection().connect();
            DETECTED_THREATS.add("proxy_conn"); return true;
        } catch (Exception e) {} return false;
    }

    private static boolean checkNoHttpsProxy() {
        try {
            String p = System.getenv("HTTPS_PROXY");
            if (p != null && p.contains("8888")) { DETECTED_THREATS.add("https_proxy"); return true; }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkNoHttpProxy() {
        try {
            String p = System.getenv("HTTP_PROXY");
            if (p != null && p.contains("8889")) { DETECTED_THREATS.add("http_proxy"); return true; }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkNoAllProxy() {
        try {
            String p = System.getenv("ALL_PROXY");
            if (p != null && (p.contains("fiddler") || p.contains("charles"))) {
                DETECTED_THREATS.add("all_proxy"); return true;
            }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkNoProxyBypass() {
        try {
            String p = System.getenv("NO_PROXY");
            if (p != null && p.equals("*")) {}
        } catch (Throwable t) {} return false;
    }

    private static boolean checkJavaIoTmpdir() {
        try {
            String t = System.getProperty("java.io.tmpdir","");
            if (t.contains("frida") || t.contains("xposed")) {
                DETECTED_THREATS.add("tmpdir"); return true;
            }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkJavaHome() {
        try {
            String h = System.getProperty("java.home","").toLowerCase();
            if (h.contains("frida") || h.contains("xposed")) {
                DETECTED_THREATS.add("javahome"); return true;
            }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkSunBootLibraryPath() {
        try {
            String p = System.getProperty("sun.boot.library.path","").toLowerCase();
            if (p.contains("frida") || p.contains("gadget")) {
                DETECTED_THREATS.add("bootlib"); return true;
            }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkSunCpuIsalist() {
        try {
            String a = System.getProperty("sun.cpu.isalist","");
            if (a.isEmpty() && System.getProperty("os.name","").toLowerCase().contains("win")) {}
        } catch (Throwable t) {} return false;
    }

    private static boolean checkUserTimezone() {
        try {
            String tz = System.getProperty("user.timezone","");
            if (tz.contains("frida")) { DETECTED_THREATS.add("tz"); return true; }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkUserLanguage() {
        try {
            String l = System.getProperty("user.language","");
            if (l.isEmpty()) { DETECTED_THREATS.add("lang"); return true; }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkUserCountry() {
        try {
            String c = System.getProperty("user.country","");
        } catch (Throwable t) {} return false;
    }

    private static boolean checkUserVariant() {
        try {
            String v = System.getProperty("user.variant","");
        } catch (Throwable t) {} return false;
    }

    private static boolean checkUserScript() {
        try {
            String s = System.getProperty("user.script","");
        } catch (Throwable t) {} return false;
    }

    private static boolean checkUserDisplay() {
        try {
            String d = System.getProperty("DISPLAY","");
            if (d != null && d.contains("frida")) { DETECTED_THREATS.add("display"); return true; }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkNoXdgSession() {
        try {
            String x = System.getenv("XDG_SESSION_TYPE");
            if (x != null && x.contains("frida")) { DETECTED_THREATS.add("xdg"); return true; }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkNoWayland() {
        try {
            String w = System.getenv("WAYLAND_DISPLAY");
            if (w != null && w.contains("frida")) { DETECTED_THREATS.add("wayland"); return true; }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkNoX11() {
        try {
            String x = System.getenv("DISPLAY");
            if (x != null && x.contains("frida")) { DETECTED_THREATS.add("x11"); return true; }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkNoLdPreload() {
        try {
            String l = System.getenv("LD_PRELOAD");
            if (l != null && (l.toLowerCase().contains("frida") || l.contains("gadget"))) {
                DETECTED_THREATS.add("ldpreload"); return true;
            }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkNoLdLibraryPath() {
        try {
            String l = System.getenv("LD_LIBRARY_PATH");
            if (l != null && l.toLowerCase().contains("frida")) {
                DETECTED_THREATS.add("ldlibpath"); return true;
            }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkNoDyldInsert() {
        try {
            String d = System.getenv("DYLD_INSERT_LIBRARIES");
            if (d != null && d.toLowerCase().contains("frida")) {
                DETECTED_THREATS.add("dyld"); return true;
            }
        } catch (Throwable t) {} return false;
    }
    // \\ CODED BY SITOKY // \\
    private static boolean checkNoDyldFramework() {
        try {
            String d = System.getenv("DYLD_FRAMEWORK_PATH");
            if (d != null && d.contains("frida")) { DETECTED_THREATS.add("dyldfw"); return true; }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkNoJavaToolOpts() {
        try {
            String j = System.getenv("JAVA_TOOL_OPTIONS");
            if (j != null && (j.toLowerCase().contains("jdwp") || j.contains("frida"))) {
                DETECTED_THREATS.add("jto"); return true;
            }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkNoJavaOptions() {
        try {
            String j = System.getenv("_JAVA_OPTIONS");
            if (j != null && (j.toLowerCase().contains("jdwp") || j.contains("agentpath"))) {
                DETECTED_THREATS.add("jo"); return true;
            }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkNoGraalNative() {
        try {
            if (System.getProperty("org.graalvm.nativeimage.imagecode") != null) {
                DETECTED_THREATS.add("graal"); return true;
            }
        } catch (Throwable t) {} return false;
    }

    private static boolean checkNoSubstrate() {
        try {
            Class.forName("com.saurik.substrate.MS");
            DETECTED_THREATS.add("substrate"); return true;
        } catch (ClassNotFoundException e) {} catch (Throwable t) {} return false;
    }

    private static boolean checkNoCycript() {
        try {
            Class.forName("cycript.Cycript");
            DETECTED_THREATS.add("cycript"); return true;
        } catch (ClassNotFoundException e) {} catch (Throwable t) {} return false;
    }

    private static boolean checkNoDobby() {
        try {
            Class.forName("dobby.Dobby");
            DETECTED_THREATS.add("dobby"); return true;
        } catch (ClassNotFoundException e) {} catch (Throwable t) {} return false;
    }

    private static boolean checkNoSubstrateApi() {
        try {
            Class.forName("com.saurik.substrate.api");
            DETECTED_THREATS.add("substrate_api"); return true;
        } catch (ClassNotFoundException e) {} catch (Throwable t) {} return false;
    }

    private static boolean checkNoFridaGadget() {
        try {
            Class.forName("re.frida.Gadget");
            DETECTED_THREATS.add("frida_gadget"); return true;
        } catch (ClassNotFoundException e) {} catch (Throwable t) {} return false;
    }

    private static boolean checkNoFridaAgent() {
        try {
            Class.forName("agent.FridaAgent");
            DETECTED_THREATS.add("frida_agent"); return true;
        } catch (ClassNotFoundException e) {} catch (Throwable t) {} return false;
    }

    private static boolean checkNoXposedBridge() {
        try {
            Class.forName("de.robv.android.xposed.XposedBridge");
            DETECTED_THREATS.add("xposed_bridge"); return true;
        } catch (ClassNotFoundException e) {} catch (Throwable t) {} return false;
    }

    private static boolean checkNoLsposed() {
        try {
            Class.forName("org.lsposed.lsposed.BuildConfig");
            DETECTED_THREATS.add("lsposed"); return true;
        } catch (ClassNotFoundException e) {} catch (Throwable t) {} return false;
    }

    private static boolean checkNoEdxposed() {
        try {
            Class.forName("me.weishu.exp.Exposed");
            DETECTED_THREATS.add("edxposed"); return true;
        } catch (ClassNotFoundException e) {} catch (Throwable t) {} return false;
    }

    private static boolean checkNoTaiChi() {
        try {
            Class.forName("me.weishu.taichi.TaiChi");
            DETECTED_THREATS.add("taichi"); return true;
        } catch (ClassNotFoundException e) {} catch (Throwable t) {} return false;
    }

    private static boolean checkNoVirtualXposed() {
        try {
            Class.forName("io.va.exposed.VirtualXposed");
            DETECTED_THREATS.add("vxp"); return true;
        } catch (ClassNotFoundException e) {} catch (Throwable t) {} return false;
    }

    private static boolean checkNoYahfa() {
        try {
            Class.forName("me.piebridge.brevent.Brevent");
        } catch (Throwable t) {} return false;
    }

    private static boolean checkNoSandhook() {
        try {
            Class.forName("com.swift.sandhook.SandHook");
            DETECTED_THREATS.add("sandhook"); return true;
        } catch (ClassNotFoundException e) {} catch (Throwable t) {} return false;
    }

    private static boolean checkNoPine() {
        try {
            Class.forName("top.canyie.pine.Pine");
            DETECTED_THREATS.add("pine"); return true;
        } catch (ClassNotFoundException e) {} catch (Throwable t) {} return false;
    }

    private static boolean checkNoFastHook() {
        try {
            Class.forName("me.eggggg.fasthook.FastHook");
            DETECTED_THREATS.add("fasthook"); return true;
        } catch (ClassNotFoundException e) {} catch (Throwable t) {} return false;
    }

    private static boolean checkNoWhale() {
        try {
            Class.forName("com.github.kr328.whale.Whale");
            DETECTED_THREATS.add("whale"); return true;
        } catch (ClassNotFoundException e) {} catch (Throwable t) {} return false;
    }

    private static boolean checkNoLegend() {
        try {
            Class.forName("me.legend.Legend");
            DETECTED_THREATS.add("legend"); return true;
        } catch (ClassNotFoundException e) {} catch (Throwable t) {} return false;
    }

    private static boolean checkNoBiliBili() {
        try {
            Class.forName("tv.danmaku.bili.Bili");
        } catch (Throwable t) {} return false;
    }
}
*/