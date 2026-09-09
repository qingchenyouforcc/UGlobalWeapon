import com.sun.tools.attach.VirtualMachine;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;

/** One-shot graceful stop for the workspace Paper JVM whose stdin was closed. */
public final class StopPaper {
    public static void main(String[] args) throws Exception {
        VirtualMachine vm = VirtualMachine.attach(args[0]);
        try {
            String directory = vm.getSystemProperties().getProperty("user.dir");
            if (!Path.of(directory).toRealPath().equals(Path.of(args[2]).toRealPath())) {
                throw new IllegalStateException("Target JVM is not this workspace server");
            }
            vm.loadAgent(Path.of(args[1]).toAbsolutePath().toString());
            System.out.println("Requested normal Bukkit shutdown.");
        } finally {
            vm.detach();
        }
    }

    public static void agentmain(String args, Instrumentation instrumentation) throws Exception {
        for (Class<?> type : instrumentation.getAllLoadedClasses()) {
            if (type.getName().equals("org.bukkit.Bukkit")) {
                type.getMethod("shutdown").invoke(null);
                return;
            }
        }
        throw new IllegalStateException("Bukkit was not found in the target JVM");
    }
}
