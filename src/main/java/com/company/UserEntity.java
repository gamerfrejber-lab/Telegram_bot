package com.company;

import java.io.Serializable;

public class UserEntity implements Serializable {

    private Long id;

    private String fullName;
    private int age;
    private String phone;

    private UserSteps userSteps;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public UserSteps getUserSteps() {
        return userSteps;
    }

    public void setUserSteps(UserSteps userSteps) {
        this.userSteps = userSteps;
    }
}
