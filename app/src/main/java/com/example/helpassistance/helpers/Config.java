package com.example.helpassistance.helpers;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Config {
    public static boolean isAdmin = false;

    public static String preference = "help assistance main";
    public static String remember_me = "remember";
    public static String isHelper = "isHelper";

    public static DatabaseReference customer_db = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers");
    public static DatabaseReference helper_db = FirebaseDatabase.getInstance().getReference().child("Users").child("Helper");

}
