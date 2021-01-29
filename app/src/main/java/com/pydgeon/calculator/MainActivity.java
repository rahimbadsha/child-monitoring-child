package com.pydgeon.calculator;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class MainActivity extends AppCompatActivity{

    private TextView btnCancel, btnPlusMinus, btnMod, btnDivide, btn7, btn8, btn9, btnMult, btn4, btn5, btn6, btnMinus
            , btn1, btn2, btn3, btnPlus, btn0, btnDot, btnEqual, txtBackStack, txtResult;

    private ImageView btnClear;

    private TextView[] textViews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent serviceIntent = new Intent(this, ForegroundService.class);
        ContextCompat.startForegroundService(this, serviceIntent);

        initViews();

        setOnTouchListner();
    }

    private void initViews() {
        btnCancel = findViewById(R.id.btnClear);
        btnPlusMinus = findViewById(R.id.btnPlusMinus);
        btnMod = findViewById(R.id.btnModulus);
        btnDivide = findViewById(R.id.btnDivide);

        btn7 = findViewById(R.id.btnSeven);
        btn8 = findViewById(R.id.btnEight);
        btn9 = findViewById(R.id.btnNine);
        btnMult = findViewById(R.id.btnMul);

        btn4 = findViewById(R.id.btnFour);
        btn5 = findViewById(R.id.btnFive);
        btn6 = findViewById(R.id.btnSix);
        btnMinus = findViewById(R.id.btnMinus);

        btn1 = findViewById(R.id.btnOne);
        btn2 = findViewById(R.id.btnTwo);
        btn3 = findViewById(R.id.btnThree);
        btnPlus = findViewById(R.id.btnPlus);

        btn0 = findViewById(R.id.btnZero);
        btnDot = findViewById(R.id.btnDot);
        btnEqual = findViewById(R.id.btnEqual);

        txtBackStack = findViewById(R.id.txtBackStack);
        txtResult = findViewById(R.id.txtResult);
        btnClear = findViewById(R.id.btnBackSpace);

        textViews = new TextView[]{btnPlusMinus, btnMod, btnDivide, btn7, btn8, btn9, btnMult, btn4, btn5, btn6, btnMinus
                , btn1, btn2, btn3, btnPlus, btn0, btnDot};

    }

    @Override
    protected void onStart() {
        super.onStart();

        btnClear.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if(txtBackStack.length() > 0)
                    txtBackStack.setText(txtBackStack.getText().toString().substring(0, txtBackStack.length() - 1));

                calculate();
            }
        });

        btnClear.setOnLongClickListener(new View.OnLongClickListener(){

            @Override
            public boolean onLongClick(View v) {
                txtBackStack.setText("");
                return true;
            }
        });

        btnEqual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculate();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtBackStack.setText("");
                txtResult.setText("");
            }
        });
    }

    private void calculate() {
        Calculator calculator = new Calculator();
        double result  = calculator.compute(txtBackStack.getText().toString());
        String total = result+"";
        if(total.length() > 10){
            txtResult.setText("");
            NumberFormat numberFormat = new DecimalFormat("0.########E0");
            txtResult.setText(numberFormat.format(result));
        }else {
            NumberFormat numberFormat = new DecimalFormat("0");
            txtResult.setText("");
            txtResult.setText(numberFormat.format(result));
        }
    }



    private void setOnTouchListner() {
        for(final TextView textView : textViews){
            final ColorDrawable color = (ColorDrawable) textView.getBackground();
            textView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if(event.getAction() == MotionEvent.ACTION_DOWN){
                        textView.setBackground(new ColorDrawable(getResources().getColor(R.color.colorLightGray)));
                        return true;
                    }else if(event.getAction() == MotionEvent.ACTION_UP){
                        if(textView.getId() == R.id.btnModulus){
                            Log.d("Touch", "Mod");
                            txtBackStack.setText(txtBackStack.getText().toString()+"%");
                        }else if(textView.getId() == R.id.btnDivide){
                            Log.d("Touch", "Div");
                            txtBackStack.setText(txtBackStack.getText().toString()+"/");
                        }else if(textView.getId() == R.id.btnMul){
                            Log.d("Touch", "Mul");
                            txtBackStack.setText(txtBackStack.getText().toString()+"*");
                        }else if(textView.getId() == R.id.btnMinus){
                            Log.d("Touch", "Min");
                            txtBackStack.setText(txtBackStack.getText().toString()+"-");
                        }else if(textView.getId() == R.id.btnPlus){
                            Log.d("Touch", "Add");
                            txtBackStack.setText(txtBackStack.getText().toString()+"+");
                        }else {
                            txtBackStack.setText(txtBackStack.getText().toString()+""+textView.getText().toString().trim());
                        }
                        textView.setBackground(color);
                        return true;
                    }
                    return false;
                }
            });
        }
    }
}
