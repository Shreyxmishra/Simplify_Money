package in.simplifymoney.ledgersync.store;
import in.simplifymoney.ledgersync.model.*; import java.math.BigDecimal; import java.time.YearMonth; import java.util.*;
public final class InMemoryDocumentStore implements DocumentStore { private final Map<String,NormalizedTxn> tx=new LinkedHashMap<>(); private static String k(NormalizedTxn t){return t.accountLast4()+"|"+t.occurredAt()+"|"+t.direction()+"|"+t.amount();}
 public void save(NormalizedTxn t){tx.putIfAbsent(k(t),t);} public List<NormalizedTxn> forAccountMonth(String a,YearMonth m){return tx.values().stream().filter(t->t.accountLast4().equals(a)&&YearMonth.from(t.occurredAt()).equals(m)).sorted(Comparator.comparing(NormalizedTxn::occurredAt).reversed()).toList();}
 public Map<Category,BigDecimal> categoryTotals(String a){Map<Category,BigDecimal>r=new EnumMap<>(Category.class);for(Category c:Category.values())r.put(c,BigDecimal.ZERO.setScale(2));for(NormalizedTxn t:tx.values())if(t.accountLast4().equals(a))r.put(t.category(),r.get(t.category()).add(t.amount()));return r;}
 public Optional<NormalizedTxn> byMessageId(String id){return tx.values().stream().filter(t->t.sourceMessageIds().contains(id)).findFirst();}
}
