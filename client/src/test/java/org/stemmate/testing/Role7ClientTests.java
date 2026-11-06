package org.stemmate.testing;

import org.stemmate.connectivity.ConnectivityMonitor;
import org.stemmate.model.*;
import org.stemmate.repository.*;
import org.stemmate.repository.exception.*;
import org.stemmate.service.*;
import org.stemmate.store.*;
import org.stemmate.sync.SyncManager;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import javax.net.ssl.*;
import static org.stemmate.testing.TestHarness.*;

/** Tests the repository client classes. Test doubles are confined to the remote HTTP/network boundary. */
public final class Role7ClientTests {
    static final UUID ACT=UUID.fromString("10000000-0000-0000-0000-000000000001");
    static final UUID PLAN=UUID.fromString("20000000-0000-0000-0000-000000000001");
    static final Instant NOW=Instant.parse("2026-11-06T19:00:00Z");
    static final Clock CLOCK=Clock.fixed(NOW,ZoneOffset.UTC);
    static Activity activity(){return new Activity(ACT,"Synthetic water activity","Synthetic lesson","Grade 7",30,"Water",
            List.of(new PlanStep("Build filter",30,"Compare layers")),List.of(new Material("Bottle","1")),List.of(new SafetyNote("Do not drink the water")));}
    static SessionPlan plan(UUID id){return new SessionPlan(id,ACT,"Synthetic water plan","Grade 7",activity().steps(),List.of(30),activity().materials(),activity().safetyNotes(),List.of(new InclusionPrompt("Picture instructions")),SyncStatus.WAITING_TO_SYNC,NOW,NOW);}
    static SessionPlan changed(SessionPlan p,String title,List<PlanStep> steps,List<Integer> timing,List<Material> materials,List<SafetyNote> safety,List<InclusionPrompt> inclusion){return new SessionPlan(p.planId(),p.activityId(),title,p.targetGroup(),steps,timing,materials,safety,inclusion,p.syncStatus(),p.createdAt(),p.updatedAt());}
    static List<TestCase> cases(){List<TestCase> tests=new ArrayList<>();
        tests.add(tc("J01","Combined catalogue filters","FR1","LocalActivityStore.findByCriteria","Filter by level, topic, duration and case-insensitive material name.","Only the matching cached activity is returned.","Repository integration",()->{
            LocalActivityStore store=new LocalActivityStore(List.of(activity()));
            List<Activity> found=store.findByCriteria(new ActivityCriteria("grade 7","water",30,List.of("BOTTLE")));
            equal(1,found.size(),"Combined filters");equal(0,store.findByCriteria(new ActivityCriteria(null,null,29,List.of())).size(),"Duration boundary");observe("matching="+found.size()+"; 29-minute limit excludes 30-minute activity");}));
        tests.add(tc("J02","Offline activity selection persists","FR2","LocalActivityStore","Save the same activity twice; reconstruct the repository with the cached catalogue.","The saved-offline mark survives reload without duplicate entries.","Real file storage integration",()->{
            LocalActivityStore first=new LocalActivityStore(List.of(activity()));first.saveForOffline(activity());first.saveForOffline(activity());
            LocalActivityStore second=new LocalActivityStore(List.of(activity()));require(second.isAvailableOffline(ACT),"Offline selection lost");equal(1,second.findByCriteria(new ActivityCriteria(null,null,null,List.of())).size(),"No duplicate catalogue row");observe("offline mark restored from activity-offline.ser");}));
        tests.add(tc("J03","Draft is created from the selected activity","FR3, FR4","SessionPlanService.createDraftFromActivity","Create a new draft from a cached activity.","Activity ID, steps, timing, materials and safety are copied; editable title starts blank.","Unit",()->{
            SessionPlan draft=new SessionPlanService(new InMemoryDraftStore()).createDraftFromActivity(PLAN,activity(),NOW);
            equal(ACT,draft.activityId(),"Activity link");equal(activity().steps(),draft.steps(),"Steps");equal(List.of(30),draft.timingMinutes(),"Timing");equal(activity().materials(),draft.materials(),"Materials");equal("",draft.title(),"Editable draft title");observe("draft activity="+draft.activityId()+"; steps="+draft.steps().size());}));
        tests.add(tc("J04","A valid plan is saved and queued","FR3, FR4, FR5","SessionPlanService and LocalSessionPlanStore","Save a complete synthetic plan through the service.","One complete record and one WAITING_TO_SYNC outbox entry are stored.","Service and real file integration",()->{
            LocalSessionPlanStore store=new LocalSessionPlanStore();new SessionPlanService(store).savePlan(plan(PLAN));
            equal(plan(PLAN),store.findById(PLAN),"Complete plan round trip");equal(1,store.outboxRepository().findPending().size(),"Outbox entry");equal(SyncStatus.WAITING_TO_SYNC,store.findById(PLAN).syncStatus(),"Visible status value");observe("plan and outbox record saved; state=WAITING_TO_SYNC");}));
        tests.add(tc("J05","Blank title is rejected","FR3, FR4","SessionPlanService.validate","Replace the plan title with whitespace and try to save.","ValidationException is raised and no plan is stored.","Validation unit",()->{
            LocalSessionPlanStore store=new LocalSessionPlanStore();SessionPlan p=plan(PLAN);
            expect(ValidationException.class,()->new SessionPlanService(store).savePlan(changed(p,"  ",p.steps(),p.timingMinutes(),p.materials(),p.safetyNotes(),p.inclusionPrompts())));equal(0,store.findAll().size(),"Invalid plan not stored");observe("blank title rejected before persistence");}));
        tests.add(tc("J06","Missing required plan elements are rejected","FR4","SessionPlanService.validate","Individually empty steps, timing, materials, safety and inclusion.","Each incomplete plan is rejected.","Validation unit",()->{
            SessionPlan p=plan(PLAN);SessionPlanService s=new SessionPlanService(new InMemoryDraftStore());
            for(SessionPlan bad:List.of(changed(p,p.title(),List.of(),List.of(),p.materials(),p.safetyNotes(),p.inclusionPrompts()),changed(p,p.title(),p.steps(),List.of(),p.materials(),p.safetyNotes(),p.inclusionPrompts()),changed(p,p.title(),p.steps(),p.timingMinutes(),List.of(),p.safetyNotes(),p.inclusionPrompts()),changed(p,p.title(),p.steps(),p.timingMinutes(),p.materials(),List.of(),p.inclusionPrompts()),changed(p,p.title(),p.steps(),p.timingMinutes(),p.materials(),p.safetyNotes(),List.of())))expect(ValidationException.class,()->s.validate(bad));observe("five missing-element variants rejected");}));
        tests.add(tc("J07","Negative and mismatched timing are rejected","FR4","SessionPlanService.validate","Provide negative timing, then two timings for one step.","Both invalid timing variants are rejected.","Validation unit",()->{
            SessionPlan p=plan(PLAN);SessionPlanService s=new SessionPlanService(new InMemoryDraftStore());
            for(List<Integer> timing:List.of(List.of(-1),List.of(10,20)))expect(ValidationException.class,()->s.validate(changed(p,p.title(),p.steps(),timing,p.materials(),p.safetyNotes(),p.inclusionPrompts())));observe("negative and mismatched timings rejected");}));
        tests.add(tc("J08","Incomplete draft can be autosaved and restored","FR6","SessionPlanService.autosaveDraft and LocalSessionPlanStore","Autosave a draft before its title and target group are completed; reconstruct storage.","The incomplete draft is restored unchanged.","Real file storage integration",()->{
            SessionPlanService service=new SessionPlanService(new LocalSessionPlanStore());SessionPlan draft=service.createDraftFromActivity(PLAN,activity(),NOW);service.autosaveDraft(draft);
            SessionPlan restored=new SessionPlanService(new LocalSessionPlanStore()).restoreLatestDraft();equal(draft,restored,"Restored draft fields");observe("empty-title draft restored from drafts.ser");}));
        tests.add(tc("J09","Plan draft and outbox survive a real JVM restart","FR2, FR3, FR5, FR6, QR1","LocalSessionPlanStore, LocalOutboxRepository and LocalActivityStore","A writer JVM saves data and exits. A new reader JVM uses the same isolated user.home.","The reader verifies the complete plan, draft, offline mark and pending outbox.","Real separate-process restart integration",()->{
            Path home=Path.of(System.getProperty("user.home"));for(String mode:List.of("write","read")){
                ProcessBuilder pb=javaProcess(RestartProbe.class.getName(),home,mode);pb.redirectErrorStream(true);Process p=pb.start();String output=new String(p.getInputStream().readAllBytes(),StandardCharsets.UTF_8);require(p.waitFor(10,TimeUnit.SECONDS),"Restart probe timed out");observe(mode+" process: "+output.trim());equal(0,p.exitValue(),"Separate "+mode+" JVM exit");}}));
        tests.add(tc("J10","Saving the same plan ID is idempotent locally","FR3, FR5","LocalSessionPlanStore.save","Save the same plan twice.","There is one plan and one pending outbox record.","Real file storage integration",()->{
            LocalSessionPlanStore s=new LocalSessionPlanStore();s.save(plan(PLAN));s.save(plan(PLAN));equal(1,s.findAll().size(),"Plan count");equal(1,s.outboxRepository().findAll().size(),"Outbox count");observe("plans=1; outbox=1");}));
        tests.add(tc("J11","Offline state prevents remote calls","FR5, QR1","SyncManager and ConnectivityMonitor","Queue a plan while offline and call syncPending.","The remote is not called; the plan remains pending.","Sync integration with remote double",()->{
            LocalSessionPlanStore s=new LocalSessionPlanStore();s.save(plan(PLAN));int[] calls={0};SyncManager m=new SyncManager(s,s.outboxRepository(),p->{calls[0]++;return Set.of(PLAN);},new ConnectivityMonitor(false));m.syncPending();equal(0,calls[0],"Remote calls");equal(1,s.findPending().size(),"Plan retained");observe("remote calls=0; pending=1");}));
        tests.add(tc("J12","Reconnect synchronises a queued plan","FR5","ConnectivityMonitor and SyncManager","Queue offline, register sync manager, then change connectivity to online.","Exactly one remote upload occurs; the confirmed plan becomes SYNCED.","Sync integration with remote double",()->{
            LocalSessionPlanStore s=new LocalSessionPlanStore();s.save(plan(PLAN));ConnectivityMonitor c=new ConnectivityMonitor(false);int[] calls={0};new SyncManager(s,s.outboxRepository(),p->{calls[0]++;return Set.of(PLAN);},c);c.setOnline(true);equal(1,calls[0],"Reconnect upload");equal(SyncStatus.SYNCED,s.findById(PLAN).syncStatus(),"Confirmed state");equal(0,s.findPending().size(),"Pending count");observe("reconnect calls=1; state=SYNCED");}));
        tests.add(tc("J13","Failure retains data logs the error and permits retry","FR5, QR1, SUS1","SyncManager and LocalSyncLogStore","Inject NetworkUnavailableException, inspect stored state/log, then retry successfully.","Failure is logged and retained; retry confirms without a duplicate.","Failure injection and real file integration",()->{
            LocalSessionPlanStore s=new LocalSessionPlanStore();s.save(plan(PLAN));LocalSyncLogStore log=new LocalSyncLogStore();boolean[] fail={true};RemoteSessionPlanStore remote=p->{if(fail[0])throw new NetworkUnavailableException("Injected loss");return Set.of(PLAN);};SyncManager m=new SyncManager(s,s.outboxRepository(),remote,new ConnectivityMonitor(true),CLOCK,log);
            m.syncPending();equal(SyncStatus.FAILED_RETRY,s.findById(PLAN).syncStatus(),"Retry state");equal(1,new LocalSyncLogStore().findAll().size(),"Persistent failure log");fail[0]=false;m.syncPending();equal(SyncStatus.SYNCED,s.findById(PLAN).syncStatus(),"Retry success");equal(1,s.findAll().size(),"No duplicate");observe("FAILED_RETRY persisted; one failure log; successful retry; plans=1");}));
        tests.add(tc("J14","Only acknowledged plans are confirmed","FR5, QR1","SyncManager","Queue two plans; remote double confirms only the first.","First is SYNCED; second is FAILED_RETRY and remains available.","Sync integration with partial acknowledgement double",()->{
            LocalSessionPlanStore s=new LocalSessionPlanStore();UUID other=UUID.randomUUID();s.save(plan(PLAN));s.save(plan(other));new SyncManager(s,s.outboxRepository(),p->Set.of(PLAN),new ConnectivityMonitor(true)).syncPending();equal(SyncStatus.SYNCED,s.findById(PLAN).syncStatus(),"Acknowledged plan");equal(SyncStatus.FAILED_RETRY,s.findById(other).syncStatus(),"Unconfirmed plan");observe("acknowledged=SYNCED; unconfirmed=FAILED_RETRY");}));
        tests.add(tc("J15","Sync batches are bounded at twenty","QR2, FR5","SyncManager.MAX_BATCH_SIZE","Queue 45 plans and record each remote-double batch size.","Batch sizes are 20, 20 and 5; all 45 records remain stored.","Batch integration with remote double",()->{
            LocalSessionPlanStore s=new LocalSessionPlanStore();for(int i=0;i<45;i++)s.save(plan(UUID.randomUUID()));List<Integer> sizes=new ArrayList<>();new SyncManager(s,s.outboxRepository(),p->{sizes.add(p.size());Set<UUID> ids=new HashSet<>();p.forEach(x->ids.add(x.planId()));return ids;},new ConnectivityMonitor(true)).syncPending();equal(List.of(20,20,5),sizes,"Batch sizes");equal(45,s.findAll().size(),"Records retained");observe("batch sizes="+sizes+"; saved plans=45");}));
        tests.add(tc("J16","HTTP payload quotes scalar JSON strings","FR4, FR5","HttpRemoteSessionPlanStore.planJson","Upload through a capturing HttpClient double and inspect the actual request body.","UUID and title fields are quoted JSON strings.","HTTP adapter contract with transport double",()->{
            CapturingClient c=new CapturingClient(200,"{\"accepted\":[{\"planId\":\""+PLAN+"\"}]}");http(c).uploadBatch(List.of(plan(PLAN)));observe("request body="+c.body);require(c.body.contains("\"planId\":\""+PLAN+"\""),"planId is not a quoted JSON string");require(c.body.contains("\"title\":\"Synthetic water plan\""),"title is not a quoted JSON string");}));
        tests.add(tc("J17","Valid acknowledgement IDs are parsed","FR5","HttpRemoteSessionPlanStore.parseConfirmation","Return a successful response with one accepted plan ID.","The adapter returns that ID.","HTTP adapter with transport double",()->{
            CapturingClient c=new CapturingClient(200,"{\"accepted\":[{\"planId\":\""+PLAN+"\"}]}");equal(Set.of(PLAN),http(c).uploadBatch(List.of(plan(PLAN))),"Confirmation IDs");observe("accepted ID returned; real HTTP not used");}));
        tests.add(tc("J18","Rejected acknowledgement IDs must not be accepted","FR5, QR1","HttpRemoteSessionPlanStore.parseConfirmation","Return accepted=[] and put the sent ID only in rejected.","The rejected plan ID is never returned as confirmed.","HTTP adapter negative contract with transport double",()->{
            CapturingClient c=new CapturingClient(200,"{\"accepted\":[],\"rejected\":[{\"planId\":\""+PLAN+"\"}]}");Set<UUID> result;try{result=http(c).uploadBatch(List.of(plan(PLAN)));}catch(NetworkUnavailableException e){observe("Rejected-only response refused");return;}observe("confirmed="+result);require(!result.contains(PLAN),"A rejected plan was returned as confirmed");}));
        tests.add(tc("J19","HTTP failure becomes a retryable network error","FR5, QR1","HttpRemoteSessionPlanStore.uploadBatch","Return HTTP 503 from the transport double.","NetworkUnavailableException is raised.","HTTP adapter failure injection",()->{CapturingClient c=new CapturingClient(503,"down");expect(NetworkUnavailableException.class,()->http(c).uploadBatch(List.of(plan(PLAN))));observe("HTTP 503 rejected as a network failure");}));
        tests.add(tc("J20","Oversized HTTP batch is rejected before send","QR2","HttpRemoteSessionPlanStore.uploadBatch","Try to upload 21 plans in one adapter call.","IllegalArgumentException is raised and send is not called.","Boundary unit",()->{CapturingClient c=new CapturingClient(200,"{}");expect(IllegalArgumentException.class,()->http(c).uploadBatch(Collections.nCopies(21,plan(PLAN))));equal(0,c.calls,"No transport call");observe("batch of 21 rejected; transport calls=0");}));
        tests.add(tc("J21","Failed plan write must not leave an orphan outbox","FR3, QR1","LocalSessionPlanStore.save","Make plans.ser a directory so the plan write fails, then reload the outbox.","The failed save does not leave a durable queue entry without a durable plan.","Real filesystem failure injection",()->{
            Path folder=Path.of(System.getProperty("user.home"),".stemmate");Files.createDirectories(folder.resolve("plans.ser"));LocalSessionPlanStore s=new LocalSessionPlanStore();expect(IllegalStateException.class,()->s.save(plan(PLAN)));LocalOutboxRepository outbox=new LocalOutboxRepository();observe("durable pending outbox="+outbox.findPending().size()+"; durable plan file is a directory");equal(0,outbox.findPending().size(),"Orphan durable outbox after failed save");}));
        tests.add(tc("J22","Wrong stored object type is handled on load","FR6, QR1","FileStoreSupport.read","Write a serialised String where plans.ser expects a map, then open the repository.","No uncaught ClassCastException escapes; a diagnosable recovery path is available.","Real stored-data failure injection",()->{
            Path p=Path.of(System.getProperty("user.home"),".stemmate","plans.ser");Files.createDirectories(p.getParent());try(ObjectOutputStream out=new ObjectOutputStream(Files.newOutputStream(p))){out.writeObject("wrong type");}try{new LocalSessionPlanStore();observe("load returned without ClassCastException; warning still needs review");}catch(ClassCastException e){observe(e.toString());throw new AssertionError("Unexpected stored object type crashes startup",e);}}));
        tests.add(tc("J23","Whitespace safety content is rejected","FR4","SessionPlanService.validate","Provide a nonempty safety-note list whose text is only spaces.","ValidationException is raised for empty safety content.","Validation negative unit",()->{
            SessionPlan p=plan(PLAN);SessionPlan bad=changed(p,p.title(),p.steps(),p.timingMinutes(),p.materials(),List.of(new SafetyNote("   ")),p.inclusionPrompts());expect(ValidationException.class,()->new SessionPlanService(new InMemoryDraftStore()).validate(bad));observe("blank safety text rejected");}));
        tests.add(tc("J24","Interrupted SYNCING record remains retryable after reload","FR5, QR1","LocalOutboxRepository.findPending","Persist SYNCING to represent interruption after upload begins; reload the outbox.","The interrupted record is eligible for retry rather than stuck outside the pending queue.","Persistent interrupted-state recovery",()->{
            LocalSessionPlanStore s=new LocalSessionPlanStore();s.save(plan(PLAN));s.outboxRepository().updateStatus(PLAN,SyncStatus.SYNCING,NOW,null);LocalOutboxRepository reloaded=new LocalOutboxRepository();observe("stored state="+reloaded.findByPlanId(PLAN).state()+"; retry queue size="+reloaded.findPending().size());equal(1,reloaded.findPending().size(),"Interrupted item must be retryable");}));
        tests.add(tc("J25","Swing form labels have explicit label bindings","AC1","DesktopShellFrame form source","Inspect the production form helpers for JLabel.setLabelFor bindings.","Form labels are programmatically associated with their controls; manual assistive-technology checks remain separate.","Static source accessibility audit",()->{
            String code=Files.readString(repository.resolve("client/src/main/java/org/stemmate/ui/DesktopShellFrame.java"));long bindings=code.lines().filter(x->x.contains(".setLabelFor(")).count();observe("setLabelFor bindings="+bindings+"; named sync/list controls exist but do not bind planner field labels");require(bindings>0,"No setLabelFor association is present in DesktopShellFrame");}));
        return tests;
    }
    static TestCase tc(String id,String title,String req,String component,String steps,String expected,String kind,Check fn){return new TestCase(id,title,req,component,steps,expected,kind,fn);}
    static HttpRemoteSessionPlanStore http(CapturingClient c){return new HttpRemoteSessionPlanStore(c,"http://localhost:9090",Duration.ofSeconds(2));}
    public static void main(String[] args)throws Exception{TestHarness.run(Role7ClientTests.class.getName(),args,cases());}

    public static final class RestartProbe {
        public static void main(String[] args) {
            LocalActivityStore a=new LocalActivityStore(List.of(activity()));LocalSessionPlanStore s=new LocalSessionPlanStore();SessionPlanService service=new SessionPlanService(s);
            if(args[0].equals("write")){a.saveForOffline(activity());service.savePlan(plan(PLAN));service.autosaveDraft(service.createDraftFromActivity(UUID.fromString("30000000-0000-0000-0000-000000000001"),activity(),NOW));System.out.println("wrote complete plan, draft, outbox and offline mark");}
            else{equal(plan(PLAN),s.findById(PLAN),"Full plan survives process restart");equal(1,s.outboxRepository().findPending().size(),"Outbox survives");require(service.restoreLatestDraft()!=null,"Draft survives");require(a.isAvailableOffline(ACT),"Offline mark survives");System.out.println("verified exact complete plan, draft, pending outbox and offline mark in a new JVM");}
        }
    }
    static final class CapturingClient extends HttpClient {
        final int status;final String response;String body="";int calls;HttpRequest request;
        CapturingClient(int status,String response){this.status=status;this.response=response;}
        @Override public <T> HttpResponse<T> send(HttpRequest req,HttpResponse.BodyHandler<T> handler)throws IOException,InterruptedException{
            request=req;calls++;ByteArrayOutputStream bytes=new ByteArrayOutputStream();CompletableFuture<Void> complete=new CompletableFuture<>();
            req.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>(){
                public void onSubscribe(Flow.Subscription s){s.request(Long.MAX_VALUE);}public void onNext(ByteBuffer b){byte[] x=new byte[b.remaining()];b.get(x);bytes.writeBytes(x);}public void onError(Throwable x){complete.completeExceptionally(x);}public void onComplete(){complete.complete(null);}});
            try{complete.get(2,TimeUnit.SECONDS);}catch(ExecutionException|TimeoutException e){throw new IOException(e);}body=bytes.toString(StandardCharsets.UTF_8);
            return new HttpResponse<T>(){
                public int statusCode(){return status;}public HttpRequest request(){return req;}public Optional<HttpResponse<T>> previousResponse(){return Optional.empty();}
                public HttpHeaders headers(){return HttpHeaders.of(Map.of(),(a,b)->true);}@SuppressWarnings("unchecked")public T body(){return (T)response;}
                public Optional<SSLSession> sslSession(){return Optional.empty();}public URI uri(){return req.uri();}public Version version(){return Version.HTTP_1_1;}};
        }
        @Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest r,HttpResponse.BodyHandler<T> h){throw new UnsupportedOperationException();}
        @Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest r,HttpResponse.BodyHandler<T> h,HttpResponse.PushPromiseHandler<T> p){throw new UnsupportedOperationException();}
        public Optional<CookieHandler> cookieHandler(){return Optional.empty();}public Optional<Duration> connectTimeout(){return Optional.of(Duration.ofSeconds(2));}
        public Redirect followRedirects(){return Redirect.NEVER;}public Optional<ProxySelector> proxy(){return Optional.empty();}
        public SSLContext sslContext(){try{return SSLContext.getDefault();}catch(Exception e){throw new IllegalStateException(e);}}public SSLParameters sslParameters(){return new SSLParameters();}
        public Optional<Authenticator> authenticator(){return Optional.empty();}public Version version(){return Version.HTTP_1_1;}public Optional<Executor> executor(){return Optional.empty();}
    }
}
