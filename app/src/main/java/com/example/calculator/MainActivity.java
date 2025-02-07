package com.example.calculator;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    static final String PREF_NAME = "VaultPrefs";
    static final String KEY_PIN_SET = "pin_set";
    static final String KEY_PIN = "pin";

    private SharedPreferences prefs;
    private EditText calculatorInput;
    private Button equalsButton;
    private boolean isLongPressed = false;

    TextView tvsec, tvMain;
    Button bac, bc, bbrac1, bbrac2, bsin, bcos, btan, blog, bln, bfact, bsquare, bsqrt, binv, b0, b9, b8, b7, b6, b5, b4, b3, b2, b1, bpi, bmul, bminus, bplus, bequal, bdot, bdiv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean pinSet = prefs.getBoolean(KEY_PIN_SET, false);

        if (!pinSet) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

//        ImageButton settingsButton = findViewById(R.id.settings);
//        settingsButton.setOnClickListener(v -> {
//            Intent intent = new Intent(this, SettingsActivity.class);
//            startActivity(intent);
//        });

        tvsec = findViewById(R.id.idTVSecondary);
        tvMain = findViewById(R.id.idTVprimary);
        bac = findViewById(R.id.bac);
        bc = findViewById(R.id.bc);
        bbrac1 = findViewById(R.id.bbrac1);
        bbrac2 = findViewById(R.id.bbrac2);
        bsin = findViewById(R.id.bsin);
        bcos = findViewById(R.id.bcos);
        btan = findViewById(R.id.btan);
        blog = findViewById(R.id.blog);
        bln = findViewById(R.id.bln);
        bfact = findViewById(R.id.bfact);
        bsquare = findViewById(R.id.bsquare);
        bsqrt = findViewById(R.id.bsqrt);
        binv = findViewById(R.id.binv);
        b0 = findViewById(R.id.b0);
        b9 = findViewById(R.id.b9);
        b8 = findViewById(R.id.b8);
        b7 = findViewById(R.id.b7);
        b6 = findViewById(R.id.b6);
        b5 = findViewById(R.id.b5);
        b4 = findViewById(R.id.b4);
        b3 = findViewById(R.id.b3);
        b2 = findViewById(R.id.b2);
        b1 = findViewById(R.id.b1);
        bpi = findViewById(R.id.bpi);
        bmul = findViewById(R.id.bmul);
        bminus = findViewById(R.id.bminus);
        bplus = findViewById(R.id.bplus);
        bequal = findViewById(R.id.bequal);
        bdot = findViewById(R.id.bdot);
        bdiv = findViewById(R.id.bdiv);


        b1.setOnClickListener(v -> appendText("1"));
        b2.setOnClickListener(v -> appendText("2"));
        b3.setOnClickListener(v -> appendText("3"));
        b4.setOnClickListener(v -> appendText("4"));
        b5.setOnClickListener(v -> appendText("5"));
        b6.setOnClickListener(v -> appendText("6"));
        b7.setOnClickListener(v -> appendText("7"));
        b8.setOnClickListener(v -> appendText("8"));
        b9.setOnClickListener(v -> appendText("9"));
        b0.setOnClickListener(v -> appendText("0"));
        bdot.setOnClickListener(v -> appendText("."));
        bplus.setOnClickListener(v -> appendOperator("+"));
        bminus.setOnClickListener(v -> appendOperator("-"));
        bmul.setOnClickListener(v -> appendOperator("*"));
        bdiv.setOnClickListener(v -> appendOperator("/"));
        bbrac1.setOnClickListener(v -> appendText("("));
        bbrac2.setOnClickListener(v -> appendText(")"));
        bpi.setOnClickListener(v -> {
            appendText(String.valueOf(Math.PI));
            tvsec.setText(bpi.getText().toString());
        });
        bsin.setOnClickListener(v -> appendText("sin("));
        bcos.setOnClickListener(v -> appendText("cos("));
        btan.setOnClickListener(v -> appendText("tan("));
        binv.setOnClickListener(v -> appendText("^(-1)"));
        bln.setOnClickListener(v -> appendText("ln("));
        blog.setOnClickListener(v -> appendText("log("));

        bsqrt.setOnClickListener(v -> calculateUnary("sqrt"));
        bsquare.setOnClickListener(v -> calculateUnary("sq"));
        bfact.setOnClickListener(v -> calculateUnary("fact"));

        bequal.setOnClickListener(v -> {
            // If it's a long press, trigger the vault login
            if (isLongPressed) {
                // Reset the long press flag after triggering the vault login
                isLongPressed = false;

                boolean pin = prefs.getBoolean(KEY_PIN_SET, false);
                if (pin) {
                    // If PIN is set, proceed with vault login
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                } else {
                    // Show message if PIN isn't set
                    Toast.makeText(MainActivity.this, "Please set a password first in settings.", Toast.LENGTH_SHORT).show();
                }
                return; // Return to prevent calculator evaluation on long press
            }

            // Regular calculator evaluation (if not a long press)
            String str = tvMain.getText().toString();
            try {
                double result = evaluate(str);
                tvMain.setText(String.format("%.8f", result)); // Display result
                tvsec.setText(str); // Display equation
            } catch (ArithmeticException e) {
                showError("Arithmetic Error: " + e.getMessage());
            } catch (NumberFormatException e) {
                showError("Invalid Number Format");
            } catch (RuntimeException e) {
                showError("Evaluation Error: " + e.getMessage());
            }
        });


        bequal.setOnLongClickListener(v -> {
            isLongPressed = true; // Set flag indicating a long press
            return true; // Return true to handle long press
        });

        bac.setOnClickListener(v -> {
            tvMain.setText("");
            tvsec.setText("");
        });

        bc.setOnClickListener(v -> {
            String str = tvMain.getText().toString();
            if (!str.isEmpty()) {
                tvMain.setText(str.substring(0, str.length() - 1));
            }
        });
    }

    private void appendText(String text) {
        tvMain.setText(tvMain.getText().toString() + text);
    }

    private void appendOperator(String operator) {
        String str = tvMain.getText().toString();
        if (!str.isEmpty() && !isOperator(str.charAt(str.length() - 1))) {
            tvMain.setText(str + operator);
        }
    }

    private boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/';
    }

    private void calculateUnary(String operation) {
        String str = tvMain.getText().toString();
        if (str.isEmpty()) {
            showError("Please enter a number");
            return;
        }

        try {
            double value = Double.parseDouble(str);
            double result = 0;

            switch (operation) {
                case "sqrt":
                    result = Math.sqrt(value);
                    break;
                case "sq":
                    result = value * value;
                    break;
                case "fact":
                    if (value < 0 || value % 1 != 0) {
                        showError("Invalid input for factorial");
                        return;
                    }
                    result = factorial((int) value);
                    break;
            }
            tvMain.setText(String.valueOf(result));
            tvsec.setText(String.format("%s(%s)", operation, str));
        } catch (NumberFormatException e) {
            showError("Invalid number");
        } catch (ArithmeticException e) {
            showError("Error in calculation");
        }
    }

    private void showError(String message) {
        tvMain.setText(message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public int factorial(int n) {
        if (n == 0) return 1;
        int fact = 1;
        for (int i = 1; i <= n; i++) {
            fact *= i;
        }
        return fact;
    }

    // Function to evaluate expressions
    public double evaluate(String str) {
        return new Object() {
            int pos = -1;
            int ch = 0;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char) ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                while (true) {
                    if (eat('+')) x += parseTerm();
                    else if (eat('-')) x -= parseTerm();
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                while (true) {
                    if (eat('*')) x *= parseFactor();
                    else if (eat('/')) {
                        double divisor = parseFactor();
                        if (divisor == 0) {
                            throw new ArithmeticException("Division by zero");
                        }
                        x /= divisor;
                    } else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor();
                if (eat('-')) return -parseFactor();

                double x;
                int startPos = pos;
                if (eat('(')) {
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, pos));
                } else if (ch >= 'a' && ch <= 'z') {
                    while (ch >= 'a' && ch <= 'z') nextChar();
                    String func = str.substring(startPos, pos);
                    x = parseFactor();
                    if (func.equals("sqrt")) x = Math.sqrt(x);
                    else if (func.equals("sin")) x = Math.sin(x);
                    else if (func.equals("cos")) x = Math.cos(x);
                    else if (func.equals("tan")) x = Math.tan(x);
                    else if (func.equals("log")) x = Math.log10(x);
                    else if (func.equals("ln")) x = Math.log(x);
                    else throw new RuntimeException("Unknown function: " + func);
                } else if (ch == 'π' || ch == 'π'){ //Added pi
                    x = Math.PI;
                    nextChar();
                }
                else {
                    throw new RuntimeException("Unexpected: " + (char) ch);
                }

                if (eat('^')) x = Math.pow(x, parseFactor());

                return x;
            }
        }.parse();
    }
}
