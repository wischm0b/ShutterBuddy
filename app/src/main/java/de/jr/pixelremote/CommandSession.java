package de.jr.pixelremote;

/** A shutter needs a fresh, single-use ticket; delayed and duplicate messages never replay a photo. */
final class CommandSession {
    enum Decision { EXECUTE, DUPLICATE, BUSY, INVALID }
    final String nonce, token;
    private long expires;
    private int sequence;
    private String ticket;
    private long ticketExpires;
    private boolean pending;
    private int result = -1;
    private final java.util.LinkedHashMap<String, Long> fastTickets = new java.util.LinkedHashMap<>();
    static final int FAST_TTL = 5000;
    CommandSession(String nonce, String token, long now) { this.nonce = nonce; this.token = token; expires = now + 25000; }
    boolean touch(String candidate, long now) {
        if (!token.equals(candidate) || now > expires) return false;
        expires = now + 25000;
        return true;
    }
    String arm(int seq, String newTicket, long now) {
        if (now > expires || seq <= 0 || seq < sequence || pending) return null;
        if (seq == sequence) return result < 0 && now <= ticketExpires ? ticket : null;
        sequence = seq; ticket = newTicket; ticketExpires = now + 3000; result = -1;
        return ticket;
    }
    Decision begin(int seq, String candidate, long now) {
        if (seq != sequence || seq <= 0 || !java.util.Objects.equals(ticket, candidate) || candidate == null) return Decision.INVALID;
        if (pending) return Decision.BUSY;
        if (result >= 0) return Decision.DUPLICATE;
        if (now > expires || now > ticketExpires) return Decision.INVALID;
        pending = true;
        return Decision.EXECUTE;
    }
    void finish(int code) { pending = false; result = code; }
    int cached(int seq) { return seq == sequence && !pending ? result : -1; }
    void cancelSequence(int seq) {
        if (seq < sequence || pending || (seq == sequence && result >= 0)) return;
        sequence = seq; result = Protocol.CANCELLED;
    }
    String issueFast(String value, long now) {
        fastTickets.entrySet().removeIf(entry -> entry.getValue() < now);
        while (fastTickets.size() >= 8) fastTickets.remove(fastTickets.keySet().iterator().next());
        fastTickets.put(value, now + FAST_TTL);
        return value;
    }
    Decision beginFast(int seq, String candidate, long now) {
        if (seq == sequence && seq > 0 && java.util.Objects.equals(ticket, candidate) && candidate != null) {
            if (pending) return Decision.BUSY;
            if (result >= 0) return Decision.DUPLICATE;
        }
        Long validUntil = fastTickets.get(candidate);
        if (seq <= sequence || candidate == null || validUntil == null || now > validUntil || now > expires) return Decision.INVALID;
        if (pending) return Decision.BUSY;
        fastTickets.remove(candidate);
        sequence = seq; ticket = candidate; pending = true; result = -1;
        return Decision.EXECUTE;
    }
}
