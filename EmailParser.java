package in.simplifymoney.ledgersync.parse;

import in.simplifymoney.ledgersync.model.Direction;
import in.simplifymoney.ledgersync.model.RawMessage;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EmailParser implements MessageParser {
    private static final Pattern TX = Pattern.compile("Your account ending (?<acct>\\d{4}) has been (?<dir>credited|debited) with (?:INR|Rs\\.?)\\s*(?<amt>[0-9,]+(?:\\.[0-9]{1,2})?)\\.", Pattern.CASE_INSENSITIVE);
    private static final Pattern MERCHANT = Pattern.compile("Merchant / Remarks:\\s*(?<merchant>.+?)\\s*(?:\\n|$)", Pattern.CASE_INSENSITIVE);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss xx", Locale.ENGLISH);
    @Override public boolean supports(RawMessage m){ return "email".equalsIgnoreCase(m.channel()); }
    @Override public Optional<ParsedTxn> parse(RawMessage m){
        Matcher x=TX.matcher(m.body()); if(!x.find()) return Optional.empty();
        BigDecimal amt=new BigDecimal(x.group("amt").replace(",","")).setScale(2);
        OffsetDateTime at;
        Matcher dm=Pattern.compile("Date:\\s*(.+?)(?:\\n|$)",Pattern.CASE_INSENSITIVE).matcher(m.body());
        try { at=dm.find()?ZonedDateTime.parse(dm.group(1).trim(), DATE).withZoneSameInstant(Dates.IST).toOffsetDateTime():m.receivedAt(); }
        catch(Exception e){ at=m.receivedAt(); }
        Matcher mm=MERCHANT.matcher(m.body()); String merchant=mm.find()?mm.group("merchant").trim():"";
        Direction d="debited".equalsIgnoreCase(x.group("dir"))?Direction.DEBIT:Direction.CREDIT;
        return Optional.of(new ParsedTxn(x.group("acct"),at,d,amt,merchant,null,m.messageId()));
    }
}
