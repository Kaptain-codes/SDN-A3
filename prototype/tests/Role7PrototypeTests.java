import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static java.lang.System.out;

/** Covers the independent prototype/src codec and outbox used with the Java server. */
public final class Role7PrototypeTests {
    static TestHarness.TestCase tc(String id,String title,String req,String component,String steps,String expected,String kind,TestHarness.Check fn){return new TestHarness.TestCase(id,title,req,component,steps,expected,kind,fn);}
    static List<TestHarness.TestCase> cases(){return List.of(
        tc("P01","Prototype line codec preserves plan text","FR3, FR5","ServerClient.encode and decode","Round-trip punctuation and line breaks in synthetic plan fields.","The decoded fields are unchanged.","Codec unit",()->{Map<String,String> p=Map.of("title","Water & bottles = \"safe\"","safety","Line 1\nLine 2");String encoded=ServerClient.encode(p);TestHarness.equal(p,ServerClient.decode(encoded),"Prototype codec");TestHarness.observe(encoded);}),
        tc("P02","Prototype outbox writes and reloads exact lines","FR5, QR1","Outbox.save and load","Save two synthetic outbox lines; construct a new Outbox and load.","The two lines are unchanged.","Real file storage integration",()->{List<String> lines=List.of("id=one&title=Water%26Light","id=two&action=SAVE_PLAN");new Outbox().save(lines);TestHarness.equal(lines,new Outbox().load(),"Outbox file round trip");TestHarness.observe("two lines persisted and restored");}),
        tc("P03","Prototype outbox survives a real JVM restart","FR5, QR1","Outbox","A writer JVM saves and exits; a separate reader JVM loads the same isolated file.","The reader verifies the exact queued item.","Real separate-process restart integration",()->{for(String mode:List.of("write","read")){Process p=TestHarness.javaProcess(RestartProbe.class.getName(),Path.of(System.getProperty("user.home")),mode).redirectErrorStream(true).start();String result=new String(p.getInputStream().readAllBytes(),StandardCharsets.UTF_8);TestHarness.require(p.waitFor(10,TimeUnit.SECONDS),"Prototype restart timeout");TestHarness.observe(mode+": "+result.trim());TestHarness.equal(0,p.exitValue(),"Prototype "+mode+" process");}}),
        tc("P04","Outbox write failure reaches its caller","FR5, QR1","Outbox.save","Create a regular file where the .stemmate directory must be, then save.","The caller receives a failure signal and can avoid claiming a durable save.","Real filesystem failure injection",()->{Files.writeString(Path.of(System.getProperty("user.home"),".stemmate"),"blocking regular file");boolean failed=false;try{new Outbox().save(List.of("id=one"));}catch(RuntimeException e){failed=true;}TestHarness.observe("save returned to caller without throwing="+!failed);TestHarness.require(failed,"Outbox.save only prints the write failure and returns normally; caller receives no failure signal");})
    );}
    public static void main(String[] args)throws Exception{TestHarness.run(Role7PrototypeTests.class.getName(),args,cases());}
    public static final class RestartProbe {public static void main(String[] args){List<String> expected=List.of("id=restart-one&action=SAVE_PLAN&title=Synthetic");if(args[0].equals("write")){new Outbox().save(expected);out.println("wrote queued item");}else{TestHarness.equal(expected,new Outbox().load(),"Prototype queue survives JVM exit");out.println("verified exact item in a new JVM");}}}
}
