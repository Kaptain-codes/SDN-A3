import javax.tools.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Run from repository root with: java scripts/RunRole7Tests.java. Requires a JDK 21+, no Maven downloads. */
public class RunRole7Tests {
    public static void main(String[] args)throws Exception {
        Path root=Path.of(args.length>0?args[0]:".").toAbsolutePath().normalize();
        Path evidence=Path.of(args.length>1?args[1]:root.resolve("docs/tests/role7/evidence").toString()).toAbsolutePath();
        Files.createDirectories(evidence);Files.createDirectories(root.resolve("target"));
        Path classes=Files.createTempDirectory(root.resolve("target"),"role7-test-classes-");
        List<Path> sources=new ArrayList<>();
        for(String folder:List.of("model","repository","service","store","sync","connectivity"))
            collect(root.resolve("client/src/main/java/org/stemmate/"+folder),sources);
        for(String folder:List.of("client/src/test/java","server/src/main/java","server/src/test/java","prototype/tests"))collect(root.resolve(folder),sources);
        for(String file:List.of("Outbox.java","ServerClient.java"))sources.add(root.resolve("prototype/src/"+file));
        sources.sort(Comparator.comparing(Path::toString));
        JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();
        if(compiler==null)throw new IllegalStateException("A full JDK 21 or later is required, not a runtime-only Java installation.");
        StringWriter diagnostics=new StringWriter();boolean compiled;
        try(StandardJavaFileManager manager=compiler.getStandardFileManager(null,null,StandardCharsets.UTF_8)){
            compiled=compiler.getTask(diagnostics,manager,null,List.of("-source","21","-target","21","-encoding","UTF-8","-d",classes.toString()),null,manager.getJavaFileObjectsFromPaths(sources)).call();
        }
        Files.writeString(evidence.resolve("compile.txt"),"Compiler: "+System.getProperty("java.version")+"\nCompiled selected production core and tests: "+compiled+"\n"+diagnostics);
        System.out.println("Compiled "+sources.size()+" actual core and test source files: "+compiled);
        if(!compiled){System.out.println(diagnostics);System.exit(2);}
        Map<String,String> suites=new LinkedHashMap<>();suites.put("client","org.stemmate.testing.Role7ClientTests");suites.put("server","stemmate.server.Role7ServerTests");suites.put("prototype","Role7PrototypeTests");
        int exit=0;List<String> results=new ArrayList<>();
        for(var entry:suites.entrySet()){
            Path folder=evidence.resolve(entry.getKey());Files.createDirectories(folder);
            String java=Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").startsWith("Windows")?"java.exe":"java").toString();
            ProcessBuilder pb=new ProcessBuilder(java,"-Djava.awt.headless=true","-cp",classes.toString(),entry.getValue(),root.toString(),folder.toString());
            pb.directory(root.toFile()).redirectErrorStream(true).redirectOutput(folder.resolve("console.txt").toFile());
            pb.environment().remove("STEMMATE_OUTBOX_FILE");
            Process process=pb.start();boolean ended=process.waitFor(55,TimeUnit.SECONDS);
            if(!ended){process.destroyForcibly();throw new IllegalStateException("Suite exceeded its 55 second limit: "+entry.getKey());}
            System.out.println(Files.readString(folder.resolve("console.txt")));
            if(process.exitValue()!=0)exit=1;
            Path result=folder.resolve("results.json");if(Files.exists(result))results.add(Files.readString(result));else exit=2;
        }
        Files.writeString(evidence.resolve("all_results.json"),"{\"executed_utc\":\""+Instant.now()+"\",\"suites\":["+String.join(",",results)+"]}\n");
        StringBuilder hashes=new StringBuilder("SHA256  Repository-relative source path\n");
        for(Path source:sources)hashes.append(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(source)))).append("  ").append(root.relativize(source).toString().replace('\\','/')).append('\n');
        Files.writeString(evidence.resolve("source_hashes.txt"),hashes.toString());
        System.out.println("Evidence: "+evidence+"; exit="+exit+" (1 means documented test failures)");
        System.exit(exit);
    }
    static void collect(Path directory,List<Path> sources)throws IOException{try(var files=Files.walk(directory)){files.filter(p->p.toString().endsWith(".java")).forEach(sources::add);}}
}
