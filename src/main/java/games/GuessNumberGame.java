package games;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class GuessNumberGame {
    private final Map<Long,Integer> numbers = new HashMap<>();
    private final Random r = new Random();

    public void start(Long chatId) {
        numbers.put(chatId, r.nextInt(10) + 1);
    }

    public Integer guess(Long chatId, int value) {
        Integer secret = numbers.get(chatId);
        if (secret == null) return null;
        if (value == secret) {
            numbers.remove(chatId);
            return 1; // success
        }
        return 0; // wrong
    }
}
