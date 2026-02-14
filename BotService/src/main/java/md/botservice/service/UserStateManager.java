package md.botservice.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserStateManager {

    public enum State {
        NORMAL,
        AWAITING_INTERESTS,
        AWAITING_SOURCE_URL,
        AWAITING_SOURCE_REMOVAL
    }

    private final Map<Long, State> userStates = new ConcurrentHashMap<>();
    private final Map<Long, Object> userData = new ConcurrentHashMap<>();

    public void setState(Long userId, State state) {
        userStates.put(userId, state);
    }

    public State getState(Long userId) {
        return userStates.getOrDefault(userId, State.NORMAL);
    }

    public void clearState(Long userId) {
        userStates.remove(userId);
        userData.remove(userId);
    }

    public void setData(Long userId, Object data) {
        userData.put(userId, data);
    }

    public Object getData(Long userId) {
        return userData.get(userId);
    }

    public boolean isAwaitingInput(Long userId) {
        State state = getState(userId);
        return state != State.NORMAL;
    }
}
