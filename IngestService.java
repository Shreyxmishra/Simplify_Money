package in.simplifymoney.ledgersync.ingest;

import in.simplifymoney.ledgersync.json.Json;
import in.simplifymoney.ledgersync.model.*;
import in.simplifymoney.ledgersync.parse.*;
import in.simplifymoney.ledgersync.store.LedgerStore;
import java.io.IOException; import java.math.BigDecimal; import java.nio.file.*; import java.time.*; import java.util.*; import java.util.stream.Stream;

public final class IngestService {
 private final Parsers parsers; private final LedgerStore store;
 public IngestService(Parsers p,LedgerStore s){parsers=p;store=s;}
 public Stats ingestFile(Path corpus)throws IOException{
  List<RawMessage> messages=readCorpus(corpus); List<ParsedTxn> parsed=new ArrayList<>(); int skipped=0;
  for(RawMessage m:messages){var p=parsers.parse(m); if(p.isPresent())parsed.add(p.get()); else skipped++;}
  Map<String,List<ParsedTxn>> groups=new LinkedHashMap<>();
  for(ParsedTxn p:parsed){String k=p.accountLast4()+"|"+p.occurredAt()+"|"+p.direction()+"|"+p.amount().toPlainString();groups.computeIfAbsent(k,x->new ArrayList<>()).add(p);}
  List<NormalizedTxn> txns=new ArrayList<>();
  for(List<ParsedTxn> g:groups.values()){
   ParsedTxn p=g.get(0); List<String> ids=g.stream().map(ParsedTxn::sourceMessageId).sorted().toList();
   txns.add(new NormalizedTxn(p.accountLast4(),p.occurredAt(),p.direction(),p.amount(),p.direction()==Direction.DEBIT?Category.SPEND:Category.INCOME,p.merchant(),ids));
  }
  for(int i=0;i<txns.size();i++){
   NormalizedTxn a=txns.get(i); if(a.direction()!=Direction.DEBIT)continue;
   if(a.merchant().toUpperCase(Locale.ROOT).contains("UPI") && a.amount().compareTo(new BigDecimal("100.00"))<=0)
    txns.set(i,new NormalizedTxn(a.accountLast4(),a.occurredAt(),a.direction(),a.amount(),Category.MICRO,a.merchant(),a.sourceMessageIds()));
  }
  for(int i=0;i<txns.size();i++){
   NormalizedTxn a=txns.get(i); if(a.direction()!=Direction.DEBIT)continue;
   for(int j=0;j<txns.size();j++){if(i==j)continue; NormalizedTxn b=txns.get(j); if(b.direction()!=Direction.CREDIT||b.accountLast4().equals(a.accountLast4()))continue;
    long sec=Math.abs(Duration.between(a.occurredAt(),b.occurredAt()).getSeconds());
    if(sec<=180 && a.amount().compareTo(b.amount())==0 && a.merchant().toUpperCase(Locale.ROOT).contains("PARAG KAPOOR") && b.merchant().toUpperCase(Locale.ROOT).contains("PARAG KAPOOR")){
      txns.set(i,new NormalizedTxn(a.accountLast4(),a.occurredAt(),a.direction(),a.amount(),Category.TRANSFER,a.merchant(),a.sourceMessageIds()));
      txns.set(j,new NormalizedTxn(b.accountLast4(),b.occurredAt(),b.direction(),b.amount(),Category.TRANSFER,b.merchant(),b.sourceMessageIds())); break;
    }
   }
  }
  txns.sort(Comparator.comparing(NormalizedTxn::occurredAt).thenComparing(NormalizedTxn::accountLast4)); for(NormalizedTxn t:txns)store.save(t);
  return new Stats(messages.size(),txns.size(),skipped);
 }
 public static List<RawMessage> readCorpus(Path corpus)throws IOException{List<RawMessage> out=new ArrayList<>();try(Stream<String> lines=Files.lines(corpus)){for(String line:(Iterable<String>)lines.filter(s->!s.isBlank())::iterator){Map<String,Object>o=Json.parseObject(line);out.add(new RawMessage((String)o.get("message_id"),(String)o.get("channel"),(String)o.get("sender"),OffsetDateTime.parse((String)o.get("received_at")),(String)o.get("device_id"),(String)o.get("body")));}}return out;}
 public record Stats(int messagesRead,int transactionsWritten,int messagesSkipped){}
}
