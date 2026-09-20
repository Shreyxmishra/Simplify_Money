package in.simplifymoney.ledgersync.store;
import in.simplifymoney.ledgersync.model.NormalizedTxn; import java.util.*;
public final class InMemoryLedgerStore implements LedgerStore { private final Map<String,NormalizedTxn> rows=new LinkedHashMap<>();
 private static String key(NormalizedTxn t){return t.accountLast4()+"|"+t.occurredAt()+"|"+t.direction()+"|"+t.amount()+"|"+String.join(",",t.sourceMessageIds());}
 @Override public void save(NormalizedTxn t){rows.putIfAbsent(key(t),t);} @Override public List<NormalizedTxn> all(){return Collections.unmodifiableList(new ArrayList<>(rows.values()));} @Override public long count(){return rows.size();}}
