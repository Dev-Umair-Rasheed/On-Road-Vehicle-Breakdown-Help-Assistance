package com.example.helpassistance.activities;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.example.helpassistance.R;

public class HelperDashboard extends AppCompatActivity {

    Button checkCustomerLoc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_helper_dashboard);

        checkCustomerLoc = findViewById(R.id.btn_check_customer_loc);
        checkCustomerLoc.setOnClickListener(v -> {
            Dialog dialogCheck = new Dialog(this);
            dialogCheck.setContentView(R.layout.check_loc_dialog);
            dialogCheck.setCancelable(true);
            dialogCheck.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            Button btncheck = dialogCheck.findViewById(R.id.btnCheck);
            EditText edtEmail = dialogCheck.findViewById(R.id.helperEmail);
            btncheck.setOnClickListener(v1 -> {
                if (edtEmail.getText().toString().isEmpty()) {
                    edtEmail.setError("PLease enter email!");
                } else {
                    Intent i = new Intent(HelperDashboard.this, CustomerLocation.class);
                    i.putExtra("cEmail", edtEmail.getText().toString());
                    startActivity(i);
                }
            });
            dialogCheck.show();
        });

    }
}