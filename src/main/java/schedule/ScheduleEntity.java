package schedule;

import java.time.LocalDateTime;

public class ScheduleEntity {
    private final long id;
    private Long userId;
    private String text;
    private LocalDateTime dateTime;
    private boolean done;

    public ScheduleEntity(long id) { this.id = id; }

    // getters / setters
    public long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public LocalDateTime getDateTime() { return dateTime; }
    public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime; }
    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }
}
