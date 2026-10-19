package com.convoy.itemdb;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import net.sqlcipher.database.SQLiteDatabase;

public class AuthActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        SQLiteDatabase.loadLibs(this);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(48, 80, 48, 48); root.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView title = new TextView(this); title.setText("Item DB\nSecure login"); title.setTextSize(26); title.setTextColor(Color.BLACK); root.addView(title);
        EditText username = new EditText(this); username.setHint("Username"); username.setText("user"); root.addView(username);
        EditText password = new EditText(this); password.setHint("Password"); password.setInputType(0x81); root.addView(password);
        TextView error = new TextView(this); error.setTextColor(Color.RED); root.addView(error);
        Button login = new Button(this); login.setText("Sign in"); root.addView(login);
        login.setOnClickListener(view -> {
            if (AuthSession.verify(this, username.getText().toString().trim(), password.getText().toString())) {
                startActivity(new android.content.Intent(this, MainActivity.class)); finish();
            } else error.setText("Invalid username or password");
        });
        setContentView(root);
    }
}
