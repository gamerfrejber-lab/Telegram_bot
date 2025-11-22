package users;

import java.time.LocalDateTime;

public class UserEntity {
    private Long id;
    private String fullName;
    private Integer age;
    private String phone;
    private LocalDateTime registeredAt;
    private UserSteps step; // for registration steps

    public UserEntity() {}

    public UserEntity(Long id) { this.id = id; }

    // getters / setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
    public UserSteps getStep() { return step; }
    public void setStep(UserSteps step) { this.step = step; }
}
