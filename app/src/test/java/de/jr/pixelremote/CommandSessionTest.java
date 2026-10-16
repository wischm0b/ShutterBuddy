package de.jr.pixelremote;

import org.junit.Test;
import static org.junit.Assert.*;

public class CommandSessionTest {
    @Test public void cancellationBeforeDelayedCommandPreventsPhoto() {
        CommandSession session = new CommandSession("watch", "session", 0);
        session.issueFast("fast", 0); session.cancelSequence(1);
        assertEquals(CommandSession.Decision.INVALID, session.beginFast(1, "fast", 100));
        assertEquals(Protocol.CANCELLED, session.cached(1));
    }
    @Test public void prefetchedTicketExecutesWithoutPrepareAndCannotReplay() {
        CommandSession session = new CommandSession("watch", "session", 0);
        session.issueFast("fast", 100);
        assertEquals(CommandSession.Decision.EXECUTE, session.beginFast(1, "fast", 200));
        assertEquals(CommandSession.Decision.BUSY, session.beginFast(1, "fast", 201));
        session.finish(Protocol.OK);
        assertEquals(CommandSession.Decision.DUPLICATE, session.beginFast(1, "fast", 202));
        assertEquals(CommandSession.Decision.INVALID, session.beginFast(2, "fast", 203));
    }
    @Test public void refreshDoesNotInvalidateAlreadyInFlightTicket() {
        CommandSession session = new CommandSession("watch", "session", 0);
        session.issueFast("first", 100); session.issueFast("second", 1000);
        assertEquals(CommandSession.Decision.EXECUTE, session.beginFast(1, "first", 1100));
    }
    @Test public void expiredPrefetchedTicketAndOldSequenceCannotFire() {
        CommandSession session = new CommandSession("watch", "session", 0);
        session.issueFast("expired", 0);
        assertEquals(CommandSession.Decision.INVALID, session.beginFast(1, "expired", 5001));
        session.issueFast("new", 6000);
        assertEquals(CommandSession.Decision.EXECUTE, session.beginFast(2, "new", 6001));
        session.finish(Protocol.OK);
        assertEquals(CommandSession.Decision.INVALID, session.beginFast(1, "new", 6002));
    }
    @Test public void delayedShutterCannotExecute() {
        CommandSession session = new CommandSession("watch", "session", 0);
        assertEquals("ticket", session.arm(1, "ticket", 10));
        assertEquals(CommandSession.Decision.INVALID, session.begin(1, "ticket", 3011));
    }
    @Test public void duplicateMessageCannotExecuteAgain() {
        CommandSession session = new CommandSession("watch", "session", 0);
        session.arm(1, "ticket", 10);
        assertEquals(CommandSession.Decision.EXECUTE, session.begin(1, "ticket", 20));
        assertEquals(CommandSession.Decision.BUSY, session.begin(1, "ticket", 21));
        session.finish(Protocol.OK);
        assertEquals(CommandSession.Decision.DUPLICATE, session.begin(1, "ticket", 22));
        assertEquals(Protocol.OK, session.cached(1));
    }
    @Test public void repeatedPrepareDoesNotExtendTicketLifetime() {
        CommandSession session = new CommandSession("watch", "session", 0);
        session.arm(1, "ticket", 10);
        assertEquals("ticket", session.arm(1, "other-ticket", 1000));
        assertNull(session.arm(1, "new-ticket", 3011));
    }
    @Test public void oldSessionAndWrongTicketsAreRejected() {
        CommandSession session = new CommandSession("watch", "session", 0);
        assertFalse(session.touch("old-session", 1));
        session.arm(1, "ticket", 10);
        assertEquals(CommandSession.Decision.INVALID, session.begin(1, "wrong", 20));
        assertEquals(CommandSession.Decision.INVALID, session.begin(2, "ticket", 20));
        assertFalse(session.touch("session", 25001));
    }
    @Test public void expiredSessionCannotBeArmedAndOlderSequencesCannotReplay() {
        CommandSession session = new CommandSession("watch", "session", 0);
        session.arm(2, "ticket", 10);
        session.begin(2, "ticket", 20); session.finish(Protocol.OK);
        assertNull(session.arm(1, "old", 30));
        assertNull(session.arm(3, "late", 25001));
    }
}
