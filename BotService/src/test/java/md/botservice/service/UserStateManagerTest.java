package md.botservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserStateManagerTest {

    private UserStateManager stateManager;

    @BeforeEach
    void setUp() {
        stateManager = new UserStateManager();
    }

    @Test
    void testInitialStateIsNormal() {
        assertEquals(UserStateManager.State.NORMAL, stateManager.getState(1L));
        assertFalse(stateManager.isAwaitingInput(1L));
    }

    @Test
    void testSetAndGetState() {
        stateManager.setState(1L, UserStateManager.State.AWAITING_INTERESTS);

        assertEquals(UserStateManager.State.AWAITING_INTERESTS, stateManager.getState(1L));
        assertTrue(stateManager.isAwaitingInput(1L));

        assertEquals(UserStateManager.State.NORMAL, stateManager.getState(2L));
    }

    @Test
    void testClearState() {
        stateManager.setState(1L, UserStateManager.State.AWAITING_SOURCE_URL);
        stateManager.setData(1L, "Some Data");

        stateManager.clearState(1L);

        assertEquals(UserStateManager.State.NORMAL, stateManager.getState(1L));
        assertNull(stateManager.getData(1L));
    }

    @Test
    void testSetAndGetData() {
        stateManager.setData(1L, "https://t.me/test");
        assertEquals("https://t.me/test", stateManager.getData(1L));
        assertNull(stateManager.getData(2L));
    }

}