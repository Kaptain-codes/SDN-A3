

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Small dependency-free test runner. Every case runs in a separate JVM and private data folder. */
public final class TestHarness {
    @FunctionalInterface public interface Check { void run() throws Exception; }
    public record TestCase(String id, String title, String requirement, String component,
                           String steps, String expected, String kind, Check check) { }
    private TestHarness() { }
    public static Path repository;
    public static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void equal(Object expected, Object actual, String message) {
        require(Objects.equals(expected, actual), message + "; expected=" + expected + "; actual=" + actual);
    }
    public static <T extends Throwable> T expect(Class<T> kind, Check action) throws Exception {
        try { action.run(); } catch (Throwable e) {
            if (kind.isInstance(e)) return kind.cast(e);
            throw new AssertionError("Expected " + kind.getSimpleName() + " but got " + e, e);
        }
        throw new AssertionError("Expected " + kind.getSimpleName() + " but no exception was raised");
    }
    public static void observe(String text) { System.out.println("Observed: " + text); }
    public static void run(String suite, String[] args, List<TestCase> cases) throws Exception {
        if (args.length > 0 && args[0].equals("--case")) {
            repository = Path.of(args[2]).toAbsolutePath();
            TestCase test = cases.stream().filter(t -> t.id.equals(args[1])).findFirst().orElseThrow();
            try { test.check.run(); System.out.println("PASS " + test.id); }
            catch (Throwable failure) { failure.printStackTrace(System.out); System.exit(1); }
            return;
        }
        repository = Path.of(args.length > 0 ? args[0] : ".").toAbsolutePath().normalize();
        Path output = Path.of(args.length > 1 ? args[1] : "target/role7-test-evidence").toAbsolutePath();
        Files.createDirectories(output);
        Path dataRoot = Files.createTempDirectory(repository.resolve("target"), "role7-cases-");
        String timestamp = Instant.now().toString();
        List<String> records = new ArrayList<>(); StringBuilder xml = new StringBuilder();
        int failures=0;
        for (TestCase test : cases) {
            Path home=dataRoot.resolve(test.id); Files.createDirectories(home);
            Path log=output.resolve(test.id+".txt");
            ProcessBuilder pb = javaProcess(suite, home, "--case", test.id, repository.toString());
            pb.directory(repository.toFile()).redirectErrorStream(true).redirectOutput(log.toFile());
            pb.environment().remove("STEMMATE_OUTBOX_FILE");
            long start=System.nanoTime(); Process process=pb.start();
            boolean finished=process.waitFor(35,TimeUnit.SECONDS);
            if(!finished) { process.destroyForcibly(); process.waitFor(5,TimeUnit.SECONDS); }
            boolean pass=finished && process.exitValue()==0;
            if(!pass)failures++;
            String actual=Files.readString(log); double seconds=(System.nanoTime()-start)/1e9;
            String status=pass?"PASS":"FAIL";
            System.out.println(status+" "+test.id+" "+test.title);
            records.add("{\"id\":"+json(test.id)+",\"title\":"+json(test.title)
                    +",\"requirement\":"+json(test.requirement)+",\"component\":"+json(test.component)
                    +",\"steps\":"+json(test.steps)+",\"expected\":"+json(test.expected)
                    +",\"kind\":"+json(test.kind)+",\"status\":"+json(status)
                    +",\"actual\":"+json(actual)+",\"seconds\":"+seconds+"}");
            xml.append("<testcase classname=\"").append(escape(suite)).append("\" name=\"")
                    .append(escape(test.id+" "+test.title)).append("\" time=\"").append(seconds).append("\">");
            if(!pass)xml.append("<failure message=\"Assertion or execution failed\">").append(escape(actual)).append("</failure>");
            xml.append("<system-out>").append(escape(actual)).append("</system-out></testcase>\n");
        }
        String summary=String.format("%d passed; %d failed; %d executed",cases.size()-failures,failures,cases.size());
        System.out.println(summary);
        Files.writeString(output.resolve("results.json"),"{\"suite\":"+json(suite)+",\"started_utc\":"+json(timestamp)
                +",\"java\":"+json(System.getProperty("java.version"))+",\"execution\":\"Real compiled Java; isolated child JVM per case; see each kind for doubles and static checks\""
                +",\"total\":"+cases.size()+",\"passed\":"+(cases.size()-failures)+",\"failed\":"+failures
                +",\"results\":["+String.join(",\n",records)+"]}\n");
        Files.writeString(output.resolve("junit.xml"),"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<testsuite name=\""+escape(suite)+"\" tests=\""+cases.size()+"\" failures=\""+failures+"\" timestamp=\""+timestamp+"\">\n"+xml+"</testsuite>\n");
        Files.writeString(output.resolve("summary.txt"),timestamp+"\n"+summary+"\n");
        if(failures>0)System.exit(1);
    }
    public static ProcessBuilder javaProcess(String main, Path home, String... args) {
        String executable=Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").startsWith("Windows")?"java.exe":"java").toString();
        String cp=Arrays.stream(System.getProperty("java.class.path").split(java.io.File.pathSeparator))
                .map(x->Path.of(x).toAbsolutePath().toString()).collect(java.util.stream.Collectors.joining(java.io.File.pathSeparator));
        List<String> command=new ArrayList<>(List.of(executable,"-Djava.awt.headless=true","-Duser.home="+home.toAbsolutePath(),"-cp",cp,main));
        command.addAll(List.of(args)); return new ProcessBuilder(command);
    }
    public static String json(String s) {
        if(s==null)return "null";StringBuilder b=new StringBuilder("\"");
        for(char c:s.toCharArray())switch(c){case '\\'->b.append("\\\\");case '"'->b.append("\\\"");case '\n'->b.append("\\n");case '\r'->b.append("\\r");case '\t'->b.append("\\t");default->{if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}
        return b.append('"').toString();
    }
    private static String escape(String s){return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;");}
}
