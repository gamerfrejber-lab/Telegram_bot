package com.company;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserRepository {


    public void saveUser(com.company.UserEntity entity) {
        List<com.company.UserEntity> list = getList();

        list.add(entity);
        saveList(list);

    }






    private void saveList(List<com.company.UserEntity> list) {

        try {
            FileOutputStream fileOutputStream = new FileOutputStream("src/main/java/com/company/user_data.txt");
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(list);
            objectOutputStream.close();
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

    }


    List<com.company.UserEntity> getList() {
        try {
            FileInputStream fileInputStream = new FileInputStream("src/main/java/com/company/user_data.txt");
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            List<com.company.UserEntity> list = (List<com.company.UserEntity>) objectInputStream.readObject();
            System.out.println(list);
            objectInputStream.close();
            return list;
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (ClassNotFoundException e) {
            System.out.println(e.getMessage());
        }
        return Collections.emptyList();
    }



    {
        List<com.company.UserEntity> list1 = getList();
        if (list1.isEmpty()) {
            List<com.company.UserEntity> list = new ArrayList<>();
            com.company.UserEntity entity = new com.company.UserEntity();
            entity.setId( 123L );
            entity.setFullName("Suxrob_Anvarovich");
            list.add( entity );
            saveList(list);
        }
    }


    public List<com.company.UserEntity> getAllUsers() {
        return getList();
    }
}
