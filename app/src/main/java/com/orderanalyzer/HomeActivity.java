package com.orderanalyzer;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

/**
 * HomeActivity — the app's landing screen.
 *
 * <p>Displays two section cards:
 * <ul>
 *   <li>Amul Milk Orders — navigates to {@link AmulMilkOrdersActivity}</li>
 *   <li>Daily Product Orders — navigates to {@link DailyProductOrdersActivity}</li>
 * </ul>
 */
public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Card 1: Amul Milk Orders
        CardView cardAmulMilk = findViewById(R.id.card_amul_milk);
        cardAmulMilk.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AmulMilkOrdersActivity.class);
            startActivity(intent);
        });

        // Card 2: Daily Product Orders
        CardView cardDailyProduct = findViewById(R.id.card_daily_product);
        cardDailyProduct.setOnClickListener(v -> {
            if (com.orderanalyzer.firebase.FirebaseShopAuthManager.getInstance(this).isLoggedIn()) {
                Intent intent = new Intent(HomeActivity.this, DailyProductOrdersActivity.class);
                startActivity(intent);
            } else {
                Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
    }
}
