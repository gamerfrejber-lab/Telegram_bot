package com.company;

import javax.ws.rs.core.Request;
import java.util.HashMap;
import java.util.Map;

public class DataBase {


    public static final Map<Long, UserEntity> users = new HashMap<>();
    public static final Map<Long, Integer> randomNumbers = new HashMap<>();
    public static final Map<Long, TicTacToeGame> ticTacToe = new HashMap<>();
    public static final Map<Integer, Request> requests = new HashMap<>();

    public static Integer lastAdminMsgId = null;
    public static Long broadcastPendingAdmin = null;


        public static final Long ADMIN_ID = 8050814208L;


}
