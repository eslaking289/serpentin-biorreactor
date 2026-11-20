package com.thermabio.app;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int CREATE_CSV = 41;
    private static final int INK = Color.BLACK;
    private static final int MUTED = Color.rgb(60, 73, 74);
    private static final int TEAL = Color.rgb(0, 194, 200);
    private static final int MAGENTA = Color.rgb(255, 0, 255);
    private static final int YELLOW = Color.rgb(255, 215, 0);
    private static final int PAPER = Color.rgb(245, 243, 220);
    private static final int BG = Color.rgb(252, 250, 227);
    private static final int LINE = Color.BLACK;

    private static final class Spec {
        final String key, label, unit, group;
        final double value;
        Spec(String key, String label, String unit, String group, double value) {
            this.key = key; this.label = label; this.unit = unit; this.group = group; this.value = value;
        }
    }

    private final List<Spec> specs = new ArrayList<>();
    private final Map<String, EditText> editors = new LinkedHashMap<>();
    private final Map<String, Double> values = new LinkedHashMap<>();
    private final Map<String, String> resultValues = new LinkedHashMap<>();
    private final Map<String, String> resultUnits = new LinkedHashMap<>();
    private SharedPreferences preferences;
    private LinearLayout resultRows;
    private LinearLayout summaryGrid;
    private LinearLayout calculatorSummaryGrid;
    private TextView flowWarningText;
    private final Button[] navigationButtons = new Button[3];
    private final int[] navigationAccents = {TEAL, YELLOW, MAGENTA};
    private TextView statusText;
    private ScrollView scrollView;
    private FrameLayout root;
    private LinearLayout calculatorScreen;
    private LinearLayout resultsScreen;
    private LinearLayout methodologyScreen;
    private int currentScreen = 0;
    private Typeface headlineTypeface;
    private Typeface bodyTypeface;
    private Typeface monoTypeface;
    private boolean initializing = true;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(PAPER);
        getWindow().setNavigationBarColor(PAPER);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        headlineTypeface = loadFont("fonts/SpaceGrotesk.ttf", 800, Typeface.create("sans-serif-black", Typeface.NORMAL));
        bodyTypeface = loadFont("fonts/Chivo.ttf", 400, Typeface.create("sans-serif", Typeface.NORMAL));
        monoTypeface = loadFont("fonts/JetBrainsMono.ttf", 600, Typeface.create("monospace", Typeface.NORMAL));
        preferences = getSharedPreferences("thermabio-local", MODE_PRIVATE);
        defineInputs();
        buildInterface();
        initializing = false;
        calculate(false);
    }

    private void defineInputs() {
        add("dh", "Calor liberado, |ΔHᵣ|", "kJ/mol", "Generación de calor", 460);
        add("substrateRate", "Consumo molar de referencia (OUR)", "mol/h", "Generación de calor", 50);
        add("heatFactor", "Factor de calor metabólico al caldo", "fracción", "Generación de calor", 1);
        add("agitation", "Potencia de agitación disipada", "kW", "Generación de calor", 1.2);
        add("tBroth", "Temperatura uniforme del caldo", "°C", "Temperaturas del proceso", 30);
        add("tIn", "Entrada del refrigerante", "°C", "Temperaturas del proceso", 10);
        add("tOut", "Salida objetivo del refrigerante", "°C", "Temperaturas del proceso", 18);
        add("coolantFlow", "Caudal másico disponible", "kg/s", "Temperaturas del proceso", 0.25);
        add("hBroth", "Coeficiente externo del caldo, hₒ", "W/(m²·K)", "Transferencia y geometría", 1200);
        add("cp", "Calor específico del refrigerante", "J/(kg·K)", "Transferencia y geometría", 4185);
        add("rho", "Densidad del refrigerante", "kg/m³", "Transferencia y geometría", 999);
        add("mu", "Viscosidad dinámica del refrigerante", "mPa·s", "Transferencia y geometría", 1.17);
        add("kf", "Conductividad del refrigerante", "W/(m·K)", "Transferencia y geometría", 0.589);
        add("wallK", "Conductividad de pared", "W/(m·K)", "Transferencia y geometría", 16);
        add("wallT", "Espesor de pared", "mm", "Transferencia y geometría", 2);
        add("fouling", "Resistencia de ensuciamiento", "m²·K/W", "Transferencia y geometría", 0.0002);
        add("tubeD", "Diámetro exterior del tubo", "mm", "Transferencia y geometría", 25);
        add("tankD", "Diámetro interno del biorreactor", "m", "Transferencia y geometría", 1.2);
        add("clearance", "Holgura radial a la pared", "mm", "Transferencia y geometría", 100);
        add("pitch", "Separación vertical entre vueltas", "mm", "Transferencia y geometría", 75);
    }

    private void add(String key, String label, String unit, String group, double value) {
        specs.add(new Spec(key, label, unit, group, value));
    }

    private void buildInterface() {
        root = new FrameLayout(this);
        root.setBackgroundColor(BG);
        root.setFitsSystemWindows(true);
        scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(BG);
        LinearLayout screenStack = new LinearLayout(this);
        screenStack.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(screenStack);
        calculatorScreen = newScreen();
        resultsScreen = newScreen();
        methodologyScreen = newScreen();
        screenStack.addView(calculatorScreen);
        screenStack.addView(resultsScreen);
        screenStack.addView(methodologyScreen);
        resultsScreen.setVisibility(View.GONE);
        methodologyScreen.setVisibility(View.GONE);
        LinearLayout page = calculatorScreen;

        LinearLayout brand = new LinearLayout(this);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        TextView mark = text("TB", 17, INK, true);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(rounded(TEAL, 4, INK));
        brand.addView(mark, new LinearLayout.LayoutParams(dp(48), dp(48)));
        LinearLayout brandCopy = new LinearLayout(this);
        brandCopy.setOrientation(LinearLayout.VERTICAL);
        brandCopy.setPadding(dp(12), 0, 0, 0);
        brandCopy.addView(text("THERMABIO", 21, INK, true));
        brandCopy.addView(text("DISEÑO TÉRMICO  /  100% OFFLINE", 10, MUTED, true));
        brand.addView(brandCopy, new LinearLayout.LayoutParams(0, -2, 1));
        Button reset = smallButton("Restablecer");
        reset.setOnClickListener(v -> resetInputs());
        brand.addView(reset);
        page.addView(brand, margins(0, 4, 0, 22));

        TextView title = text("SERPENTÍN DE\nENFRIAMIENTO", 29, INK, true);
        page.addView(title, margins(0, 0, 0, 7));
        TextView intro = text("BALANCE TÉRMICO Y DIMENSIONAMIENTO PRELIMINAR PARA BIORREACTORES.", 13, MUTED, true);
        intro.setLineSpacing(dp(3), 1f);
        page.addView(intro, margins(0, 0, 0, 18));

        calculatorSummaryGrid = new LinearLayout(this);
        calculatorSummaryGrid.setOrientation(LinearLayout.VERTICAL);
        page.addView(calculatorSummaryGrid, margins(0, 0, 0, 9));

        LinearLayout formCard = card();
        page.addView(formCard, margins(0, 0, 0, 16));
        String currentGroup = "";
        for (Spec spec : specs) {
            if (!spec.group.equals(currentGroup)) {
                currentGroup = spec.group;
                TextView heading = text(currentGroup.toUpperCase(Locale.ROOT), 16, INK, true);
                int groupAccent = currentGroup.equals("Generación de calor") ? TEAL : currentGroup.equals("Temperaturas del proceso") ? MAGENTA : YELLOW;
                heading.setPadding(dp(10), dp(8), dp(10), dp(8));
                heading.setBackground(rounded(groupAccent, 3, INK));
                formCard.addView(heading, margins(0, 14, 0, 4));
                TextView note = text(groupNote(currentGroup).toUpperCase(Locale.ROOT), 10, MUTED, true);
                formCard.addView(note, margins(0, 0, 0, 8));
            }
            addEditor(formCard, spec);
        }

        Button calculate = new Button(this);
        calculate.setText("⚡  CALCULAR DIMENSIONAMIENTO");
        calculate.setTextColor(INK);
        calculate.setTextSize(16);
        calculate.setAllCaps(false);
        calculate.setTypeface(headlineTypeface);
        calculate.setBackground(projected(MAGENTA));
        calculate.setPadding(dp(16), dp(10), dp(16), dp(10));
        calculate.setOnClickListener(v -> {
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(calculate.getWindowToken(), 0);
            calculate(true);
            showScreen(1);
        });
        page.addView(calculate, margins(0, 0, 0, 16));

        addScreenHeader(resultsScreen, "RESULTADOS TÉRMICOS Y MECÁNICOS", "SIMULACIÓN LOCAL  ·  100% OFFLINE");
        LinearLayout output = card();
        resultsScreen.addView(output, margins(0, 0, 0, 12));
        TextView resultsTitle = text("RESULTADOS DEL DISEÑO", 20, INK, true);
        output.addView(resultsTitle, margins(0, 0, 0, 4));
        statusText = text("Completa los parámetros para calcular.", 13, MUTED, false);
        output.addView(statusText, margins(0, 0, 0, 14));
        flowWarningText = text("", 12, INK, true);
        flowWarningText.setPadding(dp(10), dp(9), dp(10), dp(9));
        flowWarningText.setBackground(rounded(YELLOW, 0, INK));
        flowWarningText.setVisibility(View.GONE);
        output.addView(flowWarningText, margins(0, 0, 0, 12));
        output.addView(coilSchematic(), margins(0, 0, 0, 12));
        summaryGrid = new LinearLayout(this);
        summaryGrid.setOrientation(LinearLayout.VERTICAL);
        output.addView(summaryGrid);
        resultRows = new LinearLayout(this);
        resultRows.setOrientation(LinearLayout.VERTICAL);
        output.addView(resultRows, margins(0, 12, 0, 0));
        Button export = smallButton("Guardar informe CSV");
        export.setOnClickListener(v -> {
            if (resultValues.isEmpty()) {
                Toast.makeText(this, "Primero completa datos válidos.", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_TITLE, "informe-thermabio.csv");
            startActivityForResult(intent, CREATE_CSV);
        });
        output.addView(export, margins(0, 18, 0, 0));
        Button editAgain = smallButton("Editar parámetros");
        editAgain.setOnClickListener(v -> showScreen(0));
        output.addView(editAgain, margins(0, 10, 0, 0));

        addScreenHeader(methodologyScreen, "METODOLOGÍA Y ECUACIONES", "BASES Y CRITERIOS DE DIMENSIONAMIENTO");
        LinearLayout method = card();
        methodologyScreen.addView(method, margins(0, 0, 0, 14));
        TextView methodTitle = text("METODOLOGÍA", 20, INK, true);
        method.addView(methodTitle, margins(0, 0, 0, 5));
        method.addView(text("FUNDAMENTO TÉRMICO · CÁLCULO LOCAL", 10, MUTED, true), margins(0, 0, 0, 12));
        addMethodBlock(method, "01 / BALANCE DE CALOR", "Q_rxn = |ΔHᵣ| · OUR · f_calor / 3600\nQ_total = Q_rxn + P_agitación", TEAL);
        addMethodBlock(method, "02 / CAPACIDAD Y FUERZA IMPULSORA", "ṁ_req = 1000 Q_total / [Cp (T_s − T_e)]\nΔTlm = (ΔT₁ − ΔT₂) / ln(ΔT₁ / ΔT₂)", YELLOW);
        addMethodBlock(method, "03 / FLUJO INTERNO", "Re = 4ṁ / (π Di μ)    ·    Pr = μ Cp / k_f\nNu: 3.66 (laminar) · interpolación · Gnielinski", TEAL);
        addMethodBlock(method, "04 / RESISTENCIAS TÉRMICAS", "1/Uₒ = 1/hₒ + Rf + Do ln(Do/Di)/(2 k_p) + Do/(Di hᵢ)\nA = 1000 Q_total / (Uₒ ΔTlm)    ·    L = A / (π Do)", MAGENTA);
        addMethodBlock(method, "05 / GEOMETRÍA Y PRESIÓN", "D_hélice = D_tanque − 2 holgura − Do\nL_vuelta = √[(π D_hélice)² + paso²]    ·    ΔP ≈ f_D (L/Di) ρv²/2", YELLOW);
        TextView assumptions = text("SUPUESTOS Y LÍMITES\nCaldo bien mezclado y a temperatura uniforme; propiedades constantes; hₒ ingresado; correlaciones y pérdidas en hélice aproximadas. Gnielinski es orientativa en 3,000 ≤ Re ≤ 5×10⁶ y 0.5 ≤ Pr ≤ 2,000. La caída de presión equivale a tubo recto y omite accesorios y pérdidas locales. Verifica unidades, propiedades y diseño mecánico antes de fabricar u operar.", 12, MUTED, false);
        assumptions.setLineSpacing(dp(3), 1f);
        assumptions.setPadding(dp(10), dp(10), dp(10), dp(10));
        assumptions.setBackground(rounded(Color.WHITE, 0, INK));
        method.addView(assumptions, margins(0, 4, 0, 8));

        TextView disclaimer = text("ESTIMACIÓN ACADÉMICA PRELIMINAR. VERIFICA PROPIEDADES Y CORRELACIONES ANTES DE FABRICAR U OPERAR UN EQUIPO.", 11, MUTED, true);
        disclaimer.setLineSpacing(dp(3), 1f);
        methodologyScreen.addView(disclaimer, margins(2, 0, 2, 12));
        FrameLayout.LayoutParams scrollParams = new FrameLayout.LayoutParams(-1, -1);
        scrollParams.bottomMargin = dp(68);
        root.addView(scrollView, scrollParams);

        LinearLayout navigation = new LinearLayout(this);
        navigation.setOrientation(LinearLayout.HORIZONTAL);
        navigation.setGravity(Gravity.CENTER);
        navigation.setPadding(dp(10), dp(7), dp(10), dp(7));
        navigation.setBackground(projected(PAPER));
        addNavigationItem(navigation, "01  CALCULAR", TEAL, 0);
        addNavigationItem(navigation, "02  RESULTADOS", YELLOW, 1);
        addNavigationItem(navigation, "03  MÉTODO", MAGENTA, 2);
        FrameLayout.LayoutParams navParams = new FrameLayout.LayoutParams(-1, dp(64), Gravity.BOTTOM);
        root.addView(navigation, navParams);
        setContentView(root);
    }

    private void addMethodBlock(LinearLayout parent, String title, String equation, int accent) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setPadding(dp(10), dp(8), dp(10), dp(8));
        block.setBackground(projected(PAPER));
        TextView heading = text(title, 11, INK, true);
        heading.setBackground(rounded(accent, 0, INK));
        heading.setPadding(dp(8), dp(6), dp(8), dp(6));
        block.addView(heading);
        TextView formula = text(equation, 12, INK, true);
        formula.setTypeface(monoTypeface);
        formula.setLineSpacing(dp(3), 1f);
        block.addView(formula, margins(0, 5, 0, 0));
        parent.addView(block, margins(0, 0, 0, 8));
    }

    private void addNavigationItem(LinearLayout parent, String title, int accent, int destination) {
        Button button = new Button(this);
        button.setText(title);
        button.setTextSize(10);
        button.setTextColor(INK);
        button.setAllCaps(false);
        button.setMinHeight(dp(48));
        button.setTypeface(headlineTypeface);
        button.setPadding(dp(3), 0, dp(3), 0);
        button.setBackground(projected(destination == 0 ? accent : Color.WHITE));
        navigationButtons[destination] = button;
        button.setOnClickListener(v -> showScreen(destination));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1f);
        params.setMargins(dp(3), 0, dp(3), 0);
        parent.addView(button, params);
    }

    private LinearLayout newScreen() {
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setPadding(dp(16), dp(12), dp(16), dp(34));
        return screen;
    }

    private void addScreenHeader(LinearLayout screen, String title, String subtitle) {
        LinearLayout brand = new LinearLayout(this);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        TextView mark = text("TB", 17, INK, true);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(rounded(TEAL, 0, INK));
        brand.addView(mark, new LinearLayout.LayoutParams(dp(44), dp(44)));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(10), 0, 0, 0);
        copy.addView(text("THERMABIO", 19, INK, true));
        copy.addView(text("● OFFLINE  ·  CÁLCULO EN EL DISPOSITIVO", 9, INK, true));
        brand.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        screen.addView(brand, margins(0, 4, 0, 18));
        TextView heading = text(title, 25, INK, true);
        heading.setLineSpacing(dp(1), 1f);
        screen.addView(heading, margins(0, 0, 0, 5));
        TextView caption = text(subtitle, 10, MUTED, true);
        caption.setLetterSpacing(0.08f);
        screen.addView(caption, margins(0, 0, 0, 14));
    }

    private void showScreen(int screenIndex) {
        if (screenIndex < 0 || screenIndex > 2) return;
        LinearLayout target = screenIndex == 0 ? calculatorScreen : screenIndex == 1 ? resultsScreen : methodologyScreen;
        LinearLayout current = currentScreen == 0 ? calculatorScreen : currentScreen == 1 ? resultsScreen : methodologyScreen;
        if (current == target) {
            scrollView.smoothScrollTo(0, 0);
            return;
        }
        current.animate().alpha(0f).setDuration(70).withEndAction(() -> {
            current.setVisibility(View.GONE);
            target.setAlpha(0f);
            target.setVisibility(View.VISIBLE);
            scrollView.scrollTo(0, 0);
            target.animate().alpha(1f).setDuration(150).start();
        }).start();
        currentScreen = screenIndex;
        for (int i = 0; i < navigationButtons.length; i++) {
            if (navigationButtons[i] != null) {
                navigationButtons[i].setBackground(projected(i == screenIndex ? navigationAccents[i] : Color.WHITE));
            }
        }
    }

    private View coilSchematic() {
        FrameLayout diagram = new FrameLayout(this);
        diagram.setBackground(rounded(Color.WHITE, 0, INK));
        View drawing = new View(this) {
            private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                float w = getWidth(), h = getHeight();
                line.setStyle(Paint.Style.STROKE);
                line.setStrokeWidth(dp(2));
                line.setColor(TEAL);
                Path coil = new Path();
                float left = dp(20), right = w - dp(20), center = h * 0.47f, amplitude = h * 0.23f;
                coil.moveTo(left, center);
                int steps = 180;
                for (int i = 1; i <= steps; i++) {
                    float x = left + (right - left) * i / steps;
                    float y = center + (float) Math.sin(i * 4f * (float) Math.PI / steps) * amplitude;
                    coil.lineTo(x, y);
                }
                canvas.drawPath(coil, line);
                line.setColor(INK);
                line.setStrokeWidth(dp(1));
                canvas.drawLine(left, dp(12), left, h - dp(12), line);
                canvas.drawLine(right, dp(12), right, h - dp(12), line);
                canvas.drawLine(left, dp(12), right, dp(12), line);
                canvas.drawLine(left, h - dp(12), right, h - dp(12), line);
            }
        };
        diagram.addView(drawing, new FrameLayout.LayoutParams(-1, dp(118)));
        TextView caption = text("ESQUEMA CONCEPTUAL DEL SERPENTÍN  ·  NO A ESCALA", 9, MUTED, true);
        caption.setGravity(Gravity.CENTER);
        caption.setPadding(dp(5), dp(4), dp(5), dp(4));
        FrameLayout.LayoutParams captionParams = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        diagram.addView(caption, captionParams);
        return diagram;
    }

    private String groupNote(String group) {
        if (group.equals("Generación de calor")) return "Balance de energía de fermentación";
        if (group.equals("Temperaturas del proceso")) return "Fuerza impulsora y capacidad de refrigeración";
        return "Propiedades, coeficientes y dimensiones del serpentín";
    }

    private void addEditor(LinearLayout parent, Spec spec) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        TextView label = text(spec.label, 14, INK, true);
        cell.addView(label, margins(0, 6, 0, 6));
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(rounded(Color.WHITE, 3, INK));
        EditText edit = new EditText(this);
        edit.setSingleLine(true);
        edit.setTextSize(16);
        edit.setTypeface(monoTypeface);
        edit.setTextColor(INK);
        edit.setSelectAllOnFocus(true);
        edit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        edit.setPadding(dp(13), dp(9), dp(5), dp(9));
        edit.setBackgroundColor(Color.TRANSPARENT);
        String saved = preferences.getString(spec.key, Double.toString(spec.value));
        edit.setText(saved);
        editors.put(spec.key, edit);
        row.addView(edit, new LinearLayout.LayoutParams(0, -2, 1));
        TextView unit = text(spec.unit, 12, MUTED, false);
        unit.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        int unitAccent = spec.group.equals("Temperaturas del proceso") ? MAGENTA : spec.group.equals("Transferencia y geometría") ? YELLOW : TEAL;
        unit.setTextColor(INK);
        unit.setBackground(rounded(unitAccent, 3, INK));
        unit.setPadding(dp(8), dp(5), dp(8), dp(5));
        LinearLayout.LayoutParams unitParams = new LinearLayout.LayoutParams(-2, -2);
        unitParams.setMargins(0, dp(5), dp(5), dp(5));
        row.addView(unit, unitParams);
        cell.addView(row);
        parent.addView(cell, margins(0, 0, 0, 6));
        edit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (initializing) return;
                preferences.edit().putString(spec.key, s.toString()).apply();
                calculate(false);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void calculate(boolean notify) {
        values.clear();
        try {
            for (Spec spec : specs) {
                String raw = editors.get(spec.key).getText().toString().trim().replace(',', '.');
                if (raw.isEmpty()) throw new IllegalArgumentException("Completa todos los parámetros.");
                double value = Double.parseDouble(raw);
                if (!Double.isFinite(value)) throw new IllegalArgumentException("Usa únicamente valores numéricos finitos.");
                values.put(spec.key, value);
            }
            Map<String, String> result = calculateModel(values);
            renderResults(result);
        } catch (IllegalArgumentException ex) {
            resultValues.clear();
            statusText.setText(ex.getMessage());
        statusText.setTextColor(MAGENTA);
            summaryGrid.removeAllViews();
            resultRows.removeAllViews();
            if (calculatorSummaryGrid != null) calculatorSummaryGrid.removeAllViews();
            if (flowWarningText != null) flowWarningText.setVisibility(View.GONE);
            if (notify) Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private Map<String, String> calculateModel(Map<String, Double> v) {
        List<String> errors = new ArrayList<>();
        double doM = get(v, "tubeD") / 1000, wallM = get(v, "wallT") / 1000;
        double di = doM - 2 * wallM;
        double coilD = get(v, "tankD") - 2 * get(v, "clearance") / 1000 - doM;
        if (!(get(v, "tBroth") > get(v, "tOut") && get(v, "tBroth") > get(v, "tIn"))) errors.add("El caldo debe estar a mayor temperatura que ambas corrientes de refrigerante.");
        if (!(get(v, "tOut") > get(v, "tIn"))) errors.add("La salida del refrigerante debe ser mayor que su entrada.");
        if (get(v, "dh") <= 0 || get(v, "substrateRate") < 0 || get(v, "heatFactor") < 0 || get(v, "heatFactor") > 1 || get(v, "agitation") < 0) errors.add("Revisa entalpía, consumo, factor de calor y potencia de agitación.");
        for (String key : new String[]{"cp", "rho", "mu", "kf", "tubeD", "wallT", "wallK", "pitch", "tankD", "hBroth"}) if (get(v, key) <= 0) errors.add("Las propiedades, dimensiones y coeficiente externo deben ser positivos.");
        if (get(v, "coolantFlow") < 0 || get(v, "clearance") < 0 || get(v, "fouling") < 0) errors.add("Caudal, holgura y ensuciamiento no pueden ser negativos.");
        if (di <= 0) errors.add("El espesor de pared deja un diámetro interior nulo o negativo.");
        if (get(v, "pitch") / 1000 < doM) errors.add("El paso entre vueltas no puede ser menor que el diámetro exterior del tubo.");
        if (coilD <= doM) errors.add("El diámetro de hélice debe superar el diámetro exterior del tubo.");
        if (!errors.isEmpty()) throw new IllegalArgumentException(errors.get(0));

        double qReaction = get(v, "dh") * get(v, "substrateRate") * get(v, "heatFactor") / 3600;
        double qTotal = qReaction + get(v, "agitation");
        if (!(qTotal > 0)) throw new IllegalArgumentException("La carga térmica total debe ser mayor que cero.");
        double lm = lmtd(get(v, "tBroth") - get(v, "tOut"), get(v, "tBroth") - get(v, "tIn"));
        double mass = qTotal * 1000 / (get(v, "cp") * (get(v, "tOut") - get(v, "tIn")));
        double mu = get(v, "mu") / 1000;
        double velocity = mass / (get(v, "rho") * Math.PI * di * di / 4);
        double re = 4 * mass / (Math.PI * di * mu);
        double pr = mu * get(v, "cp") / get(v, "kf");
        double hStraight = nusselt(re, pr) * get(v, "kf") / di;
        double hi = re >= 2300 ? hStraight * (1 + 3.5 * di / coilD) : hStraight;
        double u = 1 / (doM / (di * hi) + doM * Math.log(doM / di) / (2 * get(v, "wallK")) + 1 / get(v, "hBroth") + get(v, "fouling"));
        double area = qTotal * 1000 / (u * lm);
        double length = area / (Math.PI * doM);
        double pitch = get(v, "pitch") / 1000;
        double turnLength = Math.sqrt(Math.pow(Math.PI * coilD, 2) + pitch * pitch);
        double turns = length / turnLength;
        double height = turns * pitch;
        double darcy = re < 2300 ? 64 / re : Math.pow(0.79 * Math.log(re) - 1.64, -2);
        double pressure = darcy * (length / di) * get(v, "rho") * velocity * velocity / 2;
        double flowRatio = get(v, "coolantFlow") / mass;
        Map<String, String> out = new LinkedHashMap<>();
        out.put("Carga metabólica", f(qReaction) + " kW");
        out.put("Carga térmica total", f(qTotal) + " kW");
        out.put("LMTD", f(lm) + " K");
        out.put("Caudal másico requerido", f(mass) + " kg/s");
        out.put("Caudal másico disponible", f(get(v, "coolantFlow")) + " kg/s");
        out.put("Velocidad interna", f(velocity) + " m/s");
        out.put("Número de Reynolds", f0(re) + " · " + regime(re));
        out.put("Número de Prandtl", f(pr));
        out.put("Coeficiente interno estimado", f0(hi) + " W/(m²·K)");
        out.put("Coeficiente global exterior", f0(u) + " W/(m²·K)");
        out.put("Área exterior requerida", f(area) + " m²");
        out.put("Longitud de tubo", f(length) + " m");
        out.put("Vueltas teóricas", f(turns));
        out.put("Altura centro a centro", f(height) + " m");
        out.put("Caída de presión (tubo recto equivalente)", f(pressure / 1000) + " kPa");
        out.put("Estado del caudal", flowRatio >= 1 ? "Suficiente para el objetivo" : "Insuficiente para el objetivo");
        out.put("Rango de Gnielinski", re >= 3000 && re <= 5e6 && pr >= 0.5 && pr <= 2000 ? "Dentro del rango orientativo" : "Fuera del rango; hᵢ orientativo");
        return out;
    }

    private double get(Map<String, Double> v, String key) { return v.get(key); }
    private String regime(double re) { return re >= 4000 ? "turbulento" : re >= 2300 ? "transición" : "laminar"; }
    private double nusselt(double re, double pr) {
        if (re < 2300) return 3.66;
        if (re >= 3000) return gnielinski(re, pr);
        double weight = (re - 2300) / 700;
        return 3.66 * (1 - weight) + gnielinski(3000, pr) * weight;
    }
    private double gnielinski(double re, double pr) {
        double f = Math.pow(0.79 * Math.log(re) - 1.64, -2);
        return (f / 8) * (re - 1000) * pr / (1 + 12.7 * Math.sqrt(f / 8) * (Math.pow(pr, 2.0 / 3.0) - 1));
    }
    private double lmtd(double d1, double d2) {
        if (Math.abs(d1 - d2) < 1e-9 * Math.max(d1, d2)) return (d1 + d2) / 2;
        return (d2 - d1) / Math.log(d2 / d1);
    }
    private String f(double value) { return String.format(Locale.getDefault(), "%,.3f", value); }
    private String f0(double value) { return String.format(Locale.getDefault(), "%,.0f", value); }

    private void renderResults(Map<String, String> result) {
        resultValues.clear(); resultValues.putAll(result);
        statusText.setText("Actualizado en vivo · cálculos realizados en este teléfono");
        statusText.setTextColor(INK);
        summaryGrid.removeAllViews(); resultRows.removeAllViews(); calculatorSummaryGrid.removeAllViews();
        populateSummaryGrid(summaryGrid, result);
        populateSummaryGrid(calculatorSummaryGrid, result);
        boolean flowInsufficient = result.get("Estado del caudal").startsWith("Insuficiente");
        flowWarningText.setText(flowInsufficient
                ? "ALERTA DE INGENIERÍA · EL CAUDAL DISPONIBLE NO CUBRE EL DEBER TÉRMICO. Aumenta el flujo disponible o revisa las temperaturas objetivo."
                : "CAUDAL DISPONIBLE SUFICIENTE PARA EL DEBER TÉRMICO CALCULADO.");
        flowWarningText.setBackground(rounded(flowInsufficient ? YELLOW : TEAL, 0, INK));
        flowWarningText.setVisibility(View.VISIBLE);
        resultRows.removeAllViews();
        for (Map.Entry<String, String> entry : result.entrySet()) {
            LinearLayout line = new LinearLayout(this);
            line.setGravity(Gravity.CENTER_VERTICAL);
            TextView label = text(entry.getKey(), 13, MUTED, false);
            TextView value = text(entry.getValue(), 13, INK, true);
            value.setMaxWidth(dp(150));
            value.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            line.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
            line.addView(value, new LinearLayout.LayoutParams(-2, -2));
            resultRows.addView(line, margins(0, 0, 0, 8));
            View divider = new View(this); divider.setBackgroundColor(LINE);
            resultRows.addView(divider, new LinearLayout.LayoutParams(-1, dp(2)));
            View space = new View(this); resultRows.addView(space, new LinearLayout.LayoutParams(-1, dp(7)));
        }
    }

    private void populateSummaryGrid(LinearLayout parent, Map<String, String> result) {
        String[][] keyRows = {{"Carga térmica total", "Área exterior requerida"}, {"Longitud de tubo", "Caudal másico requerido"}};
        String[] shortLabels = {"CARGA TÉRMICA (Q)", "ÁREA ESTIMADA (A)", "LONGITUD DE TUBO (L)", "CAUDAL REQUERIDO (ṁ)"};
        int tileIndex = 0;
        for (String[] keys : keyRows) {
            LinearLayout row = new LinearLayout(this);
            for (String key : keys) {
                LinearLayout tile = new LinearLayout(this);
                tile.setOrientation(LinearLayout.VERTICAL);
                tile.setPadding(dp(12), dp(12), dp(10), dp(12));
                int accent = navigationAccents[tileIndex % navigationAccents.length];
                tile.setBackground(projected(Color.WHITE));
                TextView label = text(shortLabels[tileIndex], 10, INK, true);
                label.setBackground(rounded(accent, 0, INK));
                label.setPadding(dp(6), dp(4), dp(6), dp(4));
                TextView value = text(result.get(key), 17, INK, true);
                value.setTypeface(monoTypeface);
                tile.addView(label); tile.addView(value, margins(0, 6, 0, 0));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
                if (keys[0].equals(key)) lp.setMargins(0, 0, dp(8), dp(8)); else lp.setMargins(dp(4), 0, 0, dp(8));
                row.addView(tile, lp);
                tileIndex++;
            }
            parent.addView(row);
        }
    }

    private void resetInputs() {
        initializing = true;
        preferences.edit().clear().apply();
        for (Spec spec : specs) editors.get(spec.key).setText(Double.toString(spec.value));
        initializing = false;
        calculate(false);
        Toast.makeText(this, "Valores iniciales restaurados.", Toast.LENGTH_SHORT).show();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != CREATE_CSV || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try (OutputStreamWriter writer = new OutputStreamWriter(getContentResolver().openOutputStream(uri), StandardCharsets.UTF_8)) {
            writer.write("Parámetro,Valor,Unidad\n");
            for (Spec spec : specs) writer.write(csv("Entrada: " + spec.label) + "," + csv(editors.get(spec.key).getText().toString()) + "," + csv(spec.unit) + "\n");
            for (Map.Entry<String, String> entry : resultValues.entrySet()) writer.write(csv("Resultado: " + entry.getKey()) + "," + csv(entry.getValue()) + ",\n");
            writer.write(csv("Advertencia") + "," + csv("Estimación académica preliminar; verificar correlaciones y seguridad antes de construir.") + ",\n");
            Toast.makeText(this, "Informe guardado.", Toast.LENGTH_SHORT).show();
        } catch (Exception ex) {
            Toast.makeText(this, "No se pudo guardar el informe.", Toast.LENGTH_LONG).show();
        }
    }

    private String csv(String input) { return "\"" + input.replace("\"", "\"\"") + "\""; }
    private LinearLayout card() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(14), dp(14), dp(14), dp(14));
        layout.setBackground(projected(PAPER));
        return layout;
    }
    private TextView text(String value, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value); view.setTextSize(size); view.setTextColor(color);
        view.setTypeface(bold ? headlineTypeface : bodyTypeface);
        return view;
    }
    private Typeface loadFont(String assetPath, int weight, Typeface fallback) {
        if (Build.VERSION.SDK_INT < 26) return fallback;
        try (AssetFileDescriptor descriptor = getAssets().openFd(assetPath)) {
            return new Typeface.Builder(descriptor.getFileDescriptor()).setWeight(weight).build();
        } catch (Exception ignored) {
            return fallback;
        }
    }
    private Button smallButton(String title) {
        Button button = new Button(this);
        button.setText(title); button.setTextSize(13); button.setTextColor(INK); button.setAllCaps(false);
        button.setTypeface(headlineTypeface);
        button.setBackground(rounded(title.toLowerCase(Locale.ROOT).contains("restablecer") ? YELLOW : MAGENTA, 3, INK));
        return button;
    }
    private GradientDrawable rounded(int color, int radius, Integer stroke) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(0);
        if (stroke != null) d.setStroke(dp(3), stroke);
        return d;
    }
    private Drawable projected(int color) {
        LayerDrawable projection = new LayerDrawable(new Drawable[]{
                new ColorDrawable(INK), rounded(color, 0, INK)
        });
        projection.setLayerInset(0, dp(4), dp(4), dp(-4), dp(-4));
        return projection;
    }
    private LinearLayout.LayoutParams margins(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(dp(left), dp(top), dp(right), dp(bottom)); return lp;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
