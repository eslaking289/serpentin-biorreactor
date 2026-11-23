package com.thermabio.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.AssetFileDescriptor;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.RelativeSizeSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.HorizontalScrollView;
import android.widget.SeekBar;
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
    private final Map<String, Double> schematicResults = new LinkedHashMap<>();
    private final Map<String, String> resultValues = new LinkedHashMap<>();
    private final Map<String, String> resultUnits = new LinkedHashMap<>();
    private SharedPreferences preferences;
    private LinearLayout resultRows;
    private LinearLayout summaryGrid;
    private LinearLayout calculatorSummaryGrid;
    private TextView flowWarningText;
    private final Button[] navigationButtons = new Button[4];
    private final int[] navigationAccents = {TEAL, YELLOW, MAGENTA, TEAL};
    private TextView statusText;
    private BioreactorModelView modelView;
    private FrameLayout modelPreviewFrame;
    private FrameLayout modelFullFrame;
    private TextView modelInfoText;
    private TextView modelTemperatureText;
    private boolean modelLabels = true;
    private boolean modelSpinning;
    private ScrollView scrollView;
    private FrameLayout root;
    private LinearLayout calculatorScreen;
    private LinearLayout resultsScreen;
    private LinearLayout methodologyScreen;
    private LinearLayout modelScreen;
    private int currentScreen = 0;
    private Typeface headlineTypeface;
    private Typeface bodyTypeface;
    private Typeface monoTypeface;
    private boolean initializing = true;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(PAPER);
        getWindow().setNavigationBarColor(PAPER);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.view.WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                int appearance = android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                controller.setSystemBarsAppearance(appearance, appearance);
            }
        } else {
            @SuppressWarnings("deprecation")
            int flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            getWindow().getDecorView().setSystemUiVisibility(flags);
        }
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
        add("qMet", "Calor metabólico específico", "kW/m³", "Calor metabólico y biomasa", 38);
        add("volume", "Volumen de caldo", "m³", "Calor metabólico y biomasa", 1.2);
        add("agitation", "Potencia disipada por agitación", "kW", "Calor metabólico y biomasa", 2.9);
        add("tBroth", "Temperatura uniforme del caldo", "°C", "Temperaturas del proceso", 37);
        add("tIn", "Entrada del refrigerante", "°C", "Temperaturas del proceso", 10);
        add("tOut", "Salida objetivo del refrigerante", "°C", "Temperaturas del proceso", 15);
        add("coolantFlow", "Caudal disponible", "kg/s", "Temperaturas del proceso", 2.5);
        add("rho", "Densidad del refrigerante", "kg/m³", "Propiedades del refrigerante", 998);
        add("cp", "Calor específico", "kJ/(kg·K)", "Propiedades del refrigerante", 4.18);
        add("mu", "Viscosidad dinámica", "mPa·s", "Propiedades del refrigerante", 1.0);
        add("kf", "Conductividad térmica", "W/(m·K)", "Propiedades del refrigerante", 0.6);
        add("hBroth", "Coeficiente lado caldo, hₒ", "W/(m²·K)", "Propiedades del refrigerante", 1150);
        add("wallK", "Conductividad de pared", "W/(m·K)", "Propiedades del refrigerante", 16.3);
        add("fouling", "Resistencia de ensuciamiento", "m²·K/W", "Transferencia y geometría", 0.0002);
        add("tubeD", "Diámetro exterior del tubo", "mm", "Geometría helicoidal", 25.4);
        add("wallT", "Espesor de pared", "mm", "Geometría helicoidal", 1.65);
        add("coilD", "Diámetro de hélice (línea central)", "mm", "Geometría helicoidal", 550);
        add("tankD", "Diámetro interno del biorreactor", "mm", "Geometría helicoidal", 1100);
        add("clearance", "Holgura radial mínima", "mm", "Geometría helicoidal", 160);
        add("pitch", "Paso entre vueltas", "mm", "Geometría helicoidal", 50);
        add("tankHeight", "Altura útil disponible", "mm", "Geometría helicoidal", 1400);
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
        modelScreen = newScreen();
        modelScreen.setPadding(0, 0, 0, 0);
        screenStack.addView(calculatorScreen);
        screenStack.addView(resultsScreen);
        screenStack.addView(methodologyScreen);
        screenStack.addView(modelScreen);
        resultsScreen.setVisibility(View.GONE);
        methodologyScreen.setVisibility(View.GONE);
        modelScreen.setVisibility(View.GONE);
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
        brandCopy.addView(text("DISEÑO TÉRMICO  /  CÁLCULO LOCAL", 10, MUTED, true));
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
        LinearLayout fieldRow = null;
        int fieldInGroup = 0;
        for (Spec spec : specs) {
            if (!spec.group.equals(currentGroup)) {
                currentGroup = spec.group;
                fieldRow = null;
                fieldInGroup = 0;
                TextView heading = text(currentGroup.toUpperCase(Locale.ROOT), 13, INK, true);
                heading.setLetterSpacing(0.04f);
                int groupAccent = currentGroup.equals("Calor metabólico y biomasa") ? YELLOW : currentGroup.equals("Temperaturas del proceso") ? MAGENTA : currentGroup.equals("Propiedades del refrigerante") ? TEAL : MAGENTA;
                heading.setPadding(dp(10), dp(8), dp(10), dp(8));
                heading.setBackground(rounded(groupAccent, 3, INK));
                formCard.addView(heading, margins(0, 14, 0, 4));
                TextView note = text(groupNote(currentGroup).toUpperCase(Locale.ROOT), 10, MUTED, true);
                formCard.addView(note, margins(0, 0, 0, 8));
            }
            if (fieldInGroup % 2 == 0) {
                fieldRow = new LinearLayout(this);
                fieldRow.setOrientation(LinearLayout.HORIZONTAL);
                fieldRow.setGravity(Gravity.TOP);
                formCard.addView(fieldRow, margins(0, 0, 0, 8));
            }
            addEditor(fieldRow, spec);
            fieldInGroup++;
            if (fieldInGroup % 2 == 1 && specs.indexOf(spec) == specs.size() - 1) {
                View spacer = new View(this);
                fieldRow.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1));
            }
        }
        addMetabolicBalanceCard(calculatorScreen);

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

        addScreenHeader(resultsScreen, "RESULTADOS TÉRMICOS Y MECÁNICOS", "SIMULACIÓN LOCAL  ·  ACTUALIZACIÓN EN VIVO");
        addBioreactorResultsCard(resultsScreen);
        LinearLayout output = card();
        output.setPadding(dp(14), dp(4), dp(14), dp(14));
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
        addMethodBlock(method, "01 / BALANCE TÉRMICO TOTAL", YELLOW,
                equationText("Q_met = q_met · V_caldo"),
                equationText("Q_diseño = Q_met + P_agitación − Q_pérdidas"),
                equationText("Modelo actual: Q_pérdidas = 0"));
        addMethodBlock(method, "02 / DIFERENCIA MEDIA LOGARÍTMICA", TEAL,
                equationText("ΔT₁ = T_caldo − T_salida"),
                equationText("ΔT₂ = T_caldo − T_entrada"),
                equationLine(equationText("ΔT_lm ="), fraction("ΔT₂ − ΔT₁", "ln(ΔT₂ / ΔT₁)")));
        addMethodBlock(method, "03 / COEFICIENTE GLOBAL (U) HELICOIDAL", MAGENTA,
                equationLine(equationText("ṁ_req ="), fraction("Q_diseño", "C_p · (T_s − T_e)")),
                equationLine(equationText("v ="), fraction("4ṁ", "ρπD_i²")),
                equationLine(equationText("Re ="), fraction("4ṁ", "πD_iμ")),
                equationLine(equationText("Pr ="), fraction("μ · C_p · 1000", "k_f")),
                equationText("Nu = 3.66, para Re < 2300"),
                equationText("2300 ≤ Re < 3000: interpolación lineal hasta Nu_Gn(3000, Pr)"),
                equationLine(equationText("w ="), fraction("Re − 2300", "700"), equationText("limitado entre 0 y 1")),
                equationText("Nu_trans = (1 − w) · 3.66 + w · Nu_Gn(3000, Pr)"),
                equationText("Nu_Gn ="),
                equationLine(fraction("(f_D/8)(Re − 1000)Pr", "1 + 12.7√(f_D/8)[Pr^{2/3} − 1]")),
                equationLine(equationText("De = Re√(D_i/D_hélice)")),
                equationLine(equationText("C_h = 1 + w · 3.5 ·"), fraction("D_i", "D_hélice")),
                equationLine(equationText("h_i ="), fraction("Nu · k_f", "D_i"), equationText("· C_h")));
        addMethodBlock(method, "04 / RESISTENCIAS Y ÁREA EXTERIOR", TEAL,
                equationLine(equationText("1/U_o ="), fraction("1", "h_o"), equationText("+ R_f +"), fraction("D_o ln(D_o/D_i)", "2 k_p")),
                equationLine(equationText("+"), fraction("D_o", "D_i · h_i")),
                equationLine(equationText("A_o ="), fraction("1000 · Q_diseño", "U_o · ΔT_lm")),
                equationLine(equationText("L ="), fraction("A_o", "π · D_o")));
        addMethodBlock(method, "05 / GEOMETRÍA Y CAÍDA DE PRESIÓN", Color.BLACK,
                equationText("D_i = D_o − 2 t_pared"),
                equationText("D_hélice + D_o + 2 holgura ≤ D_tanque"),
                equationText("L_vuelta = √[(π · D_hélice)² + paso²]"),
                equationLine(equationText("N ="), fraction("L", "L_vuelta"), equationText("· H = N · paso")),
                equationText("Ajuste vertical: H + D_o ≤ H_tanque"),
                equationLine(equationText("ΔP ≈ f_D ·"), fraction("L", "D_i"), equationText("·"), fraction("ρ · v²", "2")),
                equationLine(equationText("f_D ="), fraction("64", "Re"), equationText("si Re ≤ 2300")),
                equationText("2300 < Re < 4000: interpolación lineal de f_D"),
                equationText("f_D = [0.79 ln(Re) − 1.64]^{−2}, si Re ≥ 4000"));
        LinearLayout assumptions = card();
        TextView assumptionsTitle = text("SUPUESTOS Y LÍMITES", 16, INK, true);
        assumptionsTitle.setBackground(rounded(MAGENTA, 0, INK));
        assumptionsTitle.setPadding(dp(8), dp(6), dp(8), dp(6));
        assumptions.addView(assumptionsTitle, margins(0, 0, 0, 8));
        addAssumptionRow(assumptions, "Caldo perfectamente mezclado a temperatura uniforme; sin gradientes térmicos.", TEAL);
        addAssumptionRow(assumptions, "q_met se ingresa desde datos o balance, en kW/m³; no lo predice esta app.", YELLOW);
        addAssumptionRow(assumptions, "Pérdidas de calor al ambiente omitidas: Q_pérdidas = 0.", MAGENTA);
        addAssumptionRow(assumptions, "Gnielinski orientativa para 3,000 ≤ Re ≤ 5×10⁶ y 0.5 ≤ Pr ≤ 2,000.", TEAL);
        addAssumptionRow(assumptions, "Nu laminar = 3.66 para tubo recto desarrollado y pared a temperatura constante; no corrige Dean.", MAGENTA);
        addAssumptionRow(assumptions, "La mejora helicoidal de Nu es una aproximación, no una validación experimental.", TEAL);
        addAssumptionRow(assumptions, "μ se convierte de mPa·s a Pa·s; Cp de kJ/(kg·K) a J/(kg·K) para Pr.", YELLOW);
        addAssumptionRow(assumptions, "ΔP de tubo recto equivalente; no incluye curvatura ni pérdidas locales.", YELLOW);
        addAssumptionRow(assumptions, "Estimación preliminar: verificar propiedades, bomba y diseño mecánico por separado.", MAGENTA);
        methodologyScreen.addView(assumptions, margins(0, 0, 0, 14));
        Button toCalculator = smallButton("IR A LA CALCULADORA  →");
        toCalculator.setBackground(projected(TEAL));
        toCalculator.setOnClickListener(v -> showScreen(0));
        methodologyScreen.addView(toCalculator, margins(0, 0, 0, 14));

        TextView disclaimer = text("ESTIMACIÓN ACADÉMICA PRELIMINAR. VERIFICA PROPIEDADES Y CORRELACIONES ANTES DE FABRICAR U OPERAR UN EQUIPO.", 11, MUTED, true);
        disclaimer.setLineSpacing(dp(3), 1f);
        methodologyScreen.addView(disclaimer, margins(2, 0, 2, 12));
        FrameLayout.LayoutParams scrollParams = new FrameLayout.LayoutParams(-1, -1);
        scrollParams.bottomMargin = dp(68);
        addFullIndexScreen(modelScreen);
        root.addView(scrollView, scrollParams);

        LinearLayout navigation = new LinearLayout(this);
        navigation.setOrientation(LinearLayout.HORIZONTAL);
        navigation.setGravity(Gravity.CENTER);
        navigation.setPadding(dp(10), dp(7), dp(10), dp(7));
        navigation.setBackground(projected(PAPER));
        addNavigationItem(navigation, "☷\nCALCULADORA", TEAL, 0);
        addNavigationItem(navigation, "▤\nRESULTADOS", YELLOW, 1);
        addNavigationItem(navigation, "▧\nMETODOLOGÍA", MAGENTA, 2);
        addNavigationItem(navigation, "◉\nMODELO 3D", TEAL, 3);
        FrameLayout.LayoutParams navParams = new FrameLayout.LayoutParams(-1, dp(64), Gravity.BOTTOM);
        root.addView(navigation, navParams);
        setContentView(root);
    }

    private void addMethodBlock(LinearLayout parent, String title, int accent, View... equations) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setPadding(dp(10), dp(8), dp(10), dp(8));
        block.setBackground(projected(PAPER));
        TextView heading = text(title, 11, accent == Color.BLACK ? Color.WHITE : INK, true);
        heading.setBackground(rounded(accent, 0, INK));
        heading.setPadding(dp(8), dp(6), dp(8), dp(6));
        block.addView(heading);
        LinearLayout equationPanel = new LinearLayout(this);
        equationPanel.setOrientation(LinearLayout.VERTICAL);
        equationPanel.setPadding(dp(5), dp(6), dp(5), dp(6));
        equationPanel.setBackground(rounded(BG, 0, INK));
        for (View equation : equations) equationPanel.addView(equation, new LinearLayout.LayoutParams(-1, -2));
        block.addView(equationPanel, margins(0, 5, 0, 0));
        parent.addView(block, margins(0, 0, 0, 8));
    }

    private TextView equationText(String source) {
        TextView view = text("", 13, INK, false);
        view.setText(mathText(source));
        view.setTypeface(Typeface.create("serif", Typeface.NORMAL));
        view.setGravity(Gravity.CENTER);
        view.setIncludeFontPadding(true);
        view.setPadding(dp(2), dp(4), dp(2), dp(4));
        return view;
    }

    private LinearLayout equationLine(View... parts) {
        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER);
        line.setPadding(0, dp(2), 0, dp(2));
        for (View part : parts) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
            params.setMargins(dp(2), 0, dp(2), 0);
            line.addView(part, params);
        }
        return line;
    }

    private LinearLayout fraction(String numerator, String denominator) {
        LinearLayout fraction = new LinearLayout(this);
        fraction.setOrientation(LinearLayout.VERTICAL);
        fraction.setGravity(Gravity.CENTER);
        TextView top = equationText(numerator);
        TextView bottom = equationText(denominator);
        top.setTextSize(12);
        bottom.setTextSize(12);
        fraction.addView(top, new LinearLayout.LayoutParams(-2, -2));
        View bar = new View(this);
        bar.setBackgroundColor(INK);
        top.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        bottom.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int fractionWidth = Math.max(dp(30), Math.max(top.getMeasuredWidth(), bottom.getMeasuredWidth()));
        fraction.addView(bar, new LinearLayout.LayoutParams(dp(fractionWidth), dp(1)));
        fraction.addView(bottom, new LinearLayout.LayoutParams(-2, -2));
        return fraction;
    }

    private CharSequence mathText(String source) {
        SpannableStringBuilder result = new SpannableStringBuilder();
        for (int i = 0; i < source.length();) {
            char current = source.charAt(i);
            if (current == '_' || current == '^') {
                boolean subscript = current == '_';
                int start = ++i;
                int end;
                if (i < source.length() && source.charAt(i) == '{') {
                    start = ++i;
                    end = source.indexOf('}', i);
                    if (end < 0) end = source.length();
                    i = Math.min(source.length(), end + 1);
                } else {
                    while (i < source.length() && (Character.isLetterOrDigit(source.charAt(i)) || source.charAt(i) == '−')) i++;
                    end = i;
                }
                if (end <= start) {
                    result.append(current);
                    continue;
                }
                int spanStart = result.length();
                result.append(source, start, end);
                result.setSpan(subscript ? new SubscriptSpan() : new SuperscriptSpan(), spanStart, result.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                result.setSpan(new RelativeSizeSpan(0.72f), spanStart, result.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                result.append(current);
                i++;
            }
        }
        return result;
    }

    private void addAssumptionRow(LinearLayout parent, String copy, int accent) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(7), dp(7), dp(7), dp(7));
        row.setBackground(rounded(BG, 0, INK));
        TextView mark = text("✓", 13, INK, true);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(rounded(accent, 0, INK));
        row.addView(mark, new LinearLayout.LayoutParams(dp(27), dp(27)));
        TextView label = text("", 11, INK, false);
        label.setText(mathText(copy));
        label.setPadding(dp(8), 0, 0, 0);
        row.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
        parent.addView(row, margins(0, 0, 0, 6));
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

    private void addBioreactorResultsCard(LinearLayout parent) {
        LinearLayout module = card();
        module.setPadding(dp(10), dp(10), dp(10), dp(4));
        TextView tag = text("MODELO PARAMÉTRICO  /  COTAS EN VIVO", 10, INK, true);
        tag.setBackground(rounded(TEAL, 0, INK));
        tag.setPadding(dp(8), dp(6), dp(8), dp(6));
        module.addView(tag, margins(0, 0, 0, 8));
        module.addView(coilSchematic());
        Button explore = smallButton("EXPLORAR MODELO 3D  →");
        explore.setBackground(projected(TEAL));
        explore.setOnClickListener(v -> showScreen(3));
        module.addView(explore, margins(0, 8, 0, 0));
        parent.addView(module, margins(0, 0, 0, 4));
    }

    private void addMetabolicBalanceCard(LinearLayout parent) {
        LinearLayout module = card();
        TextView tag = text("CALCULADORA  /  BALANCE METABÓLICO", 10, INK, true);
        tag.setBackground(rounded(YELLOW, 0, INK));
        tag.setPadding(dp(8), dp(6), dp(8), dp(6));
        module.addView(tag, margins(0, 0, 0, 8));
        module.addView(text("Q̇_total = q̇_met · V_caldo + P_agitación", 16, INK, true), margins(0, 0, 0, 5));
        TextView note = text("Usa «Despejar este dato» en calor metabólico, volumen o potencia para calcular esa entrada. La carga total se actualiza con esos valores.", 12, MUTED, false);
        note.setLineSpacing(dp(2), 1f);
        module.addView(note);
        parent.addView(module, margins(0, 4, 0, 16));
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
        copy.addView(text("● SIN RED  ·  CÁLCULO EN EL DISPOSITIVO", 9, INK, true));
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
        if (screenIndex < 0 || screenIndex > 3) return;
        LinearLayout target = screenFor(screenIndex);
        LinearLayout current = screenFor(currentScreen);
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
        if (screenIndex == 1) showModelIn(modelPreviewFrame, true);
        if (screenIndex == 3) showModelIn(modelFullFrame, false);
        for (int i = 0; i < navigationButtons.length; i++) {
            if (navigationButtons[i] != null) {
                navigationButtons[i].setBackground(projected(i == screenIndex ? navigationAccents[i] : Color.WHITE));
            }
        }
    }

    private LinearLayout screenFor(int index) {
        if (index == 0) return calculatorScreen;
        if (index == 1) return resultsScreen;
        if (index == 2) return methodologyScreen;
        return modelScreen;
    }

    private void addFullIndexScreen(LinearLayout parent) {
        addScreenHeader(parent, "MODELO 3D DEL BIORREACTOR", "COMPONENTES  ·  PARÁMETROS  ·  CÁLCULOS");
        LinearLayout modes = new LinearLayout(this);
        modes.setOrientation(LinearLayout.HORIZONTAL);
        String[] modeNames = {"CORTE", "TRANSPARENTE", "CERRADO"};
        int[] modeColors = {TEAL, YELLOW, MAGENTA};
        for (int i = 0; i < modeNames.length; i++) {
            final int mode = i;
            Button button = smallButton(modeNames[i]);
            button.setBackground(projected(modeColors[i]));
            button.setOnClickListener(v -> { ensureModelView(); modelView.setViewMode(mode); });
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, dp(42), 1f);
            bp.setMargins(dp(3), 0, dp(3), dp(8));
            modes.addView(button, bp);
        }
        parent.addView(modes);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button labels = smallButton("ETIQUETAS  ✓");
        labels.setOnClickListener(v -> {
            modelLabels = !modelLabels;
            labels.setText(modelLabels ? "ETIQUETAS  ✓" : "ETIQUETAS  —");
            ensureModelView(); modelView.setLabels(modelLabels);
        });
        actions.addView(labels, new LinearLayout.LayoutParams(0, dp(40), 1f));
        Button spin = smallButton("⟳  GIRAR");
        spin.setOnClickListener(v -> {
            modelSpinning = !modelSpinning;
            spin.setText(modelSpinning ? "Ⅱ  PAUSAR" : "⟳  GIRAR");
            ensureModelView(); modelView.setSpinning(modelSpinning);
        });
        LinearLayout.LayoutParams spinParams = new LinearLayout.LayoutParams(0, dp(40), 1f);
        spinParams.setMargins(dp(8), 0, 0, 0);
        actions.addView(spin, spinParams);
        parent.addView(actions, margins(0, 0, 0, 8));

        LinearLayout tempRow = new LinearLayout(this);
        tempRow.setGravity(Gravity.CENTER_VERTICAL);
        modelTemperatureText = text("TEMPERATURA VISUALIZADA  ·  37 °C", 10, INK, true);
        tempRow.addView(modelTemperatureText, new LinearLayout.LayoutParams(dp(190), -2));
        SeekBar temperature = new SeekBar(this);
        temperature.setMax(850);
        temperature.setProgress(420);
        tempRow.addView(temperature, new LinearLayout.LayoutParams(0, dp(38), 1f));
        temperature.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float c = -5f + progress / 10f;
                modelTemperatureText.setText(String.format(Locale.getDefault(), "TEMPERATURA VISUALIZADA  ·  %.1f °C", c));
                ensureModelView(); modelView.setDisplayTemperature(c);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        parent.addView(tempRow, margins(0, 0, 0, 4));

        modelFullFrame = new FrameLayout(this);
        modelFullFrame.setBackground(rounded(Color.WHITE, 0, INK));
        float density = getResources().getDisplayMetrics().density;
        int viewportHeight = Math.round(getResources().getDisplayMetrics().heightPixels / density) - 220;
        parent.addView(modelFullFrame, new LinearLayout.LayoutParams(-1, dp(Math.max(410, Math.min(650, viewportHeight)))));

        HorizontalScrollView partsScroll = new HorizontalScrollView(this);
        partsScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout parts = new LinearLayout(this);
        parts.setOrientation(LinearLayout.HORIZONTAL);
        String[] partNames = {"Tanque", "Tapa y motor", "Serpentín", "Impulsores", "Deflectores", "Sensores"};
        for (String part : partNames) {
            Button chip = smallButton(part.toUpperCase(Locale.ROOT));
            chip.setOnClickListener(v -> { ensureModelView(); modelView.selectPart(part); });
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-2, dp(38));
            cp.setMargins(0, 0, dp(7), 0);
            parts.addView(chip, cp);
        }
        partsScroll.addView(parts);
        parent.addView(partsScroll, margins(0, 10, 0, 8));
        modelInfoText = text("ARRASTRA EL MODELO PARA GIRARLO O SELECCIONA UN COMPONENTE.", 12, INK, true);
        modelInfoText.setPadding(dp(12), dp(11), dp(12), dp(11));
        modelInfoText.setBackground(rounded(Color.WHITE, 0, INK));
        parent.addView(modelInfoText, margins(0, 0, 0, 14));
    }

    private void ensureModelView() {
        if (modelView != null) return;
        modelView = new BioreactorModelView(this);
        modelView.setLabels(modelLabels);
        modelView.setSpinning(modelSpinning);
        modelView.setPartListener(this::updateModelInfo);
        updateIntegratedModel();
    }

    private void showModelIn(FrameLayout target, boolean preview) {
        if (target == null) return;
        ensureModelView();
        android.view.ViewParent oldParent = modelView.getParent();
        if (oldParent instanceof android.view.ViewGroup) ((android.view.ViewGroup) oldParent).removeView(modelView);
        target.removeAllViews();
        target.addView(modelView, new FrameLayout.LayoutParams(-1, -1));
        modelView.setLabels(preview ? false : modelLabels);
    }

    private View coilSchematic() {
        modelPreviewFrame = new FrameLayout(this);
        modelPreviewFrame.setBackground(rounded(Color.WHITE, 0, INK));
        float density = getResources().getDisplayMetrics().density;
        int screenWidthDp = Math.round(getResources().getDisplayMetrics().widthPixels / density);
        int diagramHeight = Math.min(420, Math.round((screenWidthDp - 52) * 470f / 620f + 58));
        modelPreviewFrame.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(diagramHeight)));
        return modelPreviewFrame;
    }

    private void updateIntegratedModel() {
        if (schematicResults.isEmpty()) return;
        if (modelView != null) {
            modelView.setDesign(get(schematicResults, "tankDiameter"), get(schematicResults, "tankHeight"),
                    get(schematicResults, "liquidHeight"), get(schematicResults, "coilDiameter"),
                    get(schematicResults, "tubeDiameter"), get(schematicResults, "turns"),
                    get(schematicResults, "pitch"), get(schematicResults, "brothTemperature"),
                    get(schematicResults, "inletTemperature"), get(schematicResults, "outletTemperature"));
        }
    }

    private void updateModelInfo(String part) {
        if (modelInfoText == null) return;
        if (schematicResults.isEmpty()) {
            modelInfoText.setText("CALCULA EL DIMENSIONAMIENTO PARA VINCULAR LAS MEDIDAS DEL MODELO.");
            return;
        }
        String detail;
        if (part.contains("Serpentín")) detail = "SERPENTÍN · " + f(get(schematicResults, "turns")) + " vueltas · " + f(get(schematicResults, "tubeLength")) + " m · Ø exterior " + f(get(schematicResults, "tubeDiameter") * 1000) + " mm.";
        else if (part.contains("Impulsor")) detail = "IMPULSORES · Mezclan el caldo y ayudan a transferir calor hacia el serpentín. Potencia de agitación: " + f(get(values, "agitation")) + " kW.";
        else if (part.contains("Tapa") || part.contains("motor")) detail = "TAPA Y MOTOR · Cierre superior, soporte del eje, sensores y puertos del biorreactor.";
        else if (part.contains("Deflectores")) detail = "DEFLECTORES · Reducen el giro del caldo y promueven mezcla radial.";
        else if (part.contains("Sensor")) detail = "SENSORES · Referencia visual para medición de temperatura y condiciones del proceso.";
        else detail = "TANQUE · Ø " + f(get(schematicResults, "tankDiameter") * 100) + " cm · nivel calculado " + f(get(schematicResults, "liquidHeight") * 100) + " cm · volumen " + f(get(values, "volume")) + " m³.";
        modelInfoText.setText(detail.toUpperCase(Locale.ROOT));
    }

    private String groupNote(String group) {
        if (group.equals("Calor metabólico y biomasa")) return "Calor volumétrico y potencia disipada";
        if (group.equals("Temperaturas del proceso")) return "Fuerza impulsora y capacidad de refrigeración";
        if (group.equals("Propiedades del refrigerante")) return "Propiedades del agua y coeficiente exterior";
        return "Propiedades, coeficientes y dimensiones del serpentín";
    }

    private void addEditor(LinearLayout parent, Spec spec) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        TextView label = text(spec.label.toUpperCase(Locale.ROOT), 10, INK, true);
        label.setMinHeight(dp(30));
        cell.addView(label, margins(0, 4, 0, 4));
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(rounded(Color.WHITE, 3, INK));
        EditText edit = new EditText(this);
        edit.setSingleLine(true);
        edit.setTextSize(15);
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
        TextView unit = text(spec.unit, 10, MUTED, false);
        unit.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        int unitAccent = spec.group.equals("Temperaturas del proceso") ? MAGENTA :
                spec.group.equals("Propiedades del refrigerante") ? TEAL :
                spec.group.equals("Geometría helicoidal") ? MAGENTA : YELLOW;
        unit.setTextColor(INK);
        unit.setBackground(rounded(unitAccent, 3, INK));
        unit.setPadding(dp(8), dp(5), dp(8), dp(5));
        LinearLayout.LayoutParams unitParams = new LinearLayout.LayoutParams(-2, -2);
        unitParams.setMargins(0, dp(5), dp(5), dp(5));
        row.addView(unit, unitParams);
        cell.addView(row);
        if ("qMet".equals(spec.key) || "volume".equals(spec.key) || "agitation".equals(spec.key)) {
            Button solve = smallButton("DESPEJAR ESTE DATO");
            solve.setTextSize(9);
            solve.setMinHeight(dp(36));
            solve.setBackground(projected(YELLOW));
            solve.setOnClickListener(v -> showMetabolicSolveDialog(spec.key));
            cell.addView(solve, margins(0, 4, 0, 2));
        }
        LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(0, -2, 1f);
        cellParams.setMargins(dp(3), 0, dp(5), dp(6));
        parent.addView(cell, cellParams);
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

    private void showMetabolicSolveDialog(String target) {
        String[] knownKeys = "qMet".equals(target) ? new String[]{"qTotal", "volume", "agitation"} :
                "volume".equals(target) ? new String[]{"qTotal", "qMet", "agitation"} :
                        new String[]{"qTotal", "qMet", "volume"};
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("qTotal", "Carga térmica total conocida (kW)");
        labels.put("qMet", "Calor metabólico específico (kW/m³)");
        labels.put("volume", "Volumen de caldo (m³)");
        labels.put("agitation", "Potencia disipada por agitación (kW)");
        String resultUnit = "qMet".equals(target) ? "kW/m³" : "volume".equals(target) ? "m³" : "kW";

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(4), dp(20), dp(8));
        TextView equation = text("Q̇_total = q̇_met · V_caldo + P_agitación\nIngresa una carga térmica total conocida. Los otros dos datos se precargan desde la calculadora y puedes editarlos aquí.", 12, MUTED, false);
        equation.setLineSpacing(dp(3), 1f);
        content.addView(equation, margins(0, 0, 0, 12));

        Map<String, EditText> knownInputs = new LinkedHashMap<>();
        for (String key : knownKeys) {
            TextView label = text(labels.get(key), 11, INK, true);
            content.addView(label, margins(0, 5, 0, 2));
            EditText input = new EditText(this);
            input.setSingleLine(true);
            input.setTextSize(16);
            input.setTypeface(monoTypeface);
            input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
            input.setHint("Carga térmica conocida");
            input.setSelectAllOnFocus(true);
            input.setPadding(dp(10), dp(7), dp(10), dp(7));
            if (!"qTotal".equals(key)) {
                EditText current = editors.get(key);
                if (current != null) input.setText(current.getText().toString());
            }
            knownInputs.put(key, input);
            content.addView(input, margins(0, 0, 0, 5));
        }
        TextView result = text("Resultado: —", 16, INK, true);
        result.setBackground(rounded(TEAL, 2, INK));
        result.setPadding(dp(10), dp(9), dp(10), dp(9));
        content.addView(result, margins(0, 12, 0, 0));

        String title = "qMet".equals(target) ? "Calcular calor metabólico específico" :
                "volume".equals(target) ? "Calcular volumen de caldo" : "Calcular potencia de agitación";
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(content)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Aplicar a la calculadora", null)
                .create();
        final double[] solved = {Double.NaN};
        dialog.setOnShowListener(ignored -> {
            Button apply = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Runnable update = () -> {
                try {
                    double qTotal = dialogNumber(knownInputs.get("qTotal"), "La carga térmica total");
                    double qMet = "qMet".equals(target) ? Double.NaN : dialogNumber(knownInputs.get("qMet"), "El calor metabólico específico");
                    double volume = "volume".equals(target) ? Double.NaN : dialogNumber(knownInputs.get("volume"), "El volumen de caldo");
                    double agitation = "agitation".equals(target) ? Double.NaN : dialogNumber(knownInputs.get("agitation"), "La potencia de agitación");
                    solved[0] = MetabolicBalance.solve(target, qTotal, qMet, volume, agitation);
                    result.setText("Resultado: " + f(solved[0]) + " " + resultUnit);
                    result.setTextColor(INK);
                    apply.setEnabled(true);
                } catch (IllegalArgumentException error) {
                    solved[0] = Double.NaN;
                    result.setText("Revisa los datos: " + error.getMessage());
                    result.setTextColor(MAGENTA);
                    apply.setEnabled(false);
                }
            };
            for (EditText input : knownInputs.values()) input.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { update.run(); }
                @Override public void afterTextChanged(Editable s) {}
            });
            apply.setEnabled(false);
            apply.setOnClickListener(v -> {
                if (!Double.isFinite(solved[0])) return;
                EditText targetInput = editors.get(target);
                if (targetInput != null) targetInput.setText(String.format(Locale.US, "%.10g", solved[0]));
                dialog.dismiss();
                Toast.makeText(this, "Valor aplicado y resultados actualizados.", Toast.LENGTH_SHORT).show();
            });
            update.run();
        });
        dialog.show();
    }

    private double dialogNumber(EditText input, String label) {
        String value = input == null ? "" : input.getText().toString().trim().replace(',', '.');
        if (value.isEmpty()) throw new IllegalArgumentException("ingresa " + label.toLowerCase(Locale.ROOT) + ".");
        try {
            double number = Double.parseDouble(value);
            if (!Double.isFinite(number)) throw new NumberFormatException();
            return number;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(label + " debe ser un número finito.");
        }
    }

    private void calculate(boolean notify) {
        values.clear();
        schematicResults.clear();
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
            updateIntegratedModel();
            if (notify) Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private Map<String, String> calculateModel(Map<String, Double> v) {
        List<String> errors = new ArrayList<>();
        double doM = get(v, "tubeD") / 1000, wallM = get(v, "wallT") / 1000;
        double di = doM - 2 * wallM;
        double coilD = get(v, "coilD") / 1000;
        double tankD = get(v, "tankD") / 1000;
        double tankH = get(v, "tankHeight") / 1000;
        double clearance = get(v, "clearance") / 1000;
        if (!(get(v, "tBroth") > get(v, "tOut") && get(v, "tBroth") > get(v, "tIn"))) errors.add("El caldo debe estar a mayor temperatura que ambas corrientes de refrigerante.");
        if (!(get(v, "tOut") > get(v, "tIn"))) errors.add("La salida del refrigerante debe ser mayor que su entrada.");
        if (get(v, "qMet") < 0 || get(v, "volume") <= 0 || get(v, "agitation") < 0) errors.add("q_met y agitación no pueden ser negativos; el volumen debe ser positivo.");
        for (String key : new String[]{"cp", "rho", "mu", "kf", "tubeD", "wallT", "wallK", "pitch", "tankD", "tankHeight", "coilD", "hBroth"}) if (get(v, key) <= 0) errors.add("Las propiedades, dimensiones y coeficiente externo deben ser positivos.");
        if (get(v, "coolantFlow") < 0 || get(v, "clearance") < 0 || get(v, "fouling") < 0) errors.add("Caudal, holgura y ensuciamiento no pueden ser negativos.");
        if (di <= 0) errors.add("El espesor de pared deja un diámetro interior nulo o negativo.");
        if (get(v, "pitch") / 1000 < doM) errors.add("El paso entre vueltas no puede ser menor que el diámetro exterior del tubo.");
        if (coilD <= doM) errors.add("El diámetro de hélice debe superar el diámetro exterior del tubo.");
        if (coilD + doM + 2 * clearance > tankD) errors.add("La hélice, el tubo y las holguras no caben dentro del diámetro del biorreactor.");
        double tankCapacity = Math.PI * tankD * tankD / 4 * tankH;
        if (get(v, "volume") > tankCapacity) errors.add("El volumen de caldo supera la capacidad cilíndrica aproximada del tanque.");
        if (!errors.isEmpty()) throw new IllegalArgumentException(errors.get(0));

        double qReaction = get(v, "qMet") * get(v, "volume");
        double qTotal = qReaction + get(v, "agitation"); // Q_pérdidas al ambiente = 0 según el supuesto del modelo
        if (!(qTotal > 0)) throw new IllegalArgumentException("La carga térmica total debe ser mayor que cero.");
        double lm = lmtd(get(v, "tBroth") - get(v, "tOut"), get(v, "tBroth") - get(v, "tIn"));
        double mass = qTotal / (get(v, "cp") * (get(v, "tOut") - get(v, "tIn"))); // kW / [kJ/(kg K) K] = kg/s
        double mu = get(v, "mu") / 1000; // mPa·s -> Pa·s
        double cpJ = get(v, "cp") * 1000;
        double velocity = mass / (get(v, "rho") * Math.PI * di * di / 4);
        double re = 4 * mass / (Math.PI * di * mu);
        double pr = mu * cpJ / get(v, "kf");
        double nu = nusselt(re, pr);
        double curvatureWeight = Math.max(0, Math.min(1, (re - 2300) / 700));
        double curvatureFactor = 1 + curvatureWeight * 3.5 * di / coilD;
        double hi = nu * get(v, "kf") / di * curvatureFactor;
        double u = 1 / (doM / (di * hi) + doM * Math.log(doM / di) / (2 * get(v, "wallK")) + 1 / get(v, "hBroth") + get(v, "fouling"));
        double area = qTotal * 1000 / (u * lm);
        double length = area / (Math.PI * doM);
        double pitch = get(v, "pitch") / 1000;
        double turnLength = Math.sqrt(Math.pow(Math.PI * coilD, 2) + pitch * pitch);
        double turns = length / turnLength;
        double height = turns * pitch;
        double darcy = darcyFactor(re);
        double pressure = darcy * (length / di) * get(v, "rho") * velocity * velocity / 2;
        double flowRatio = get(v, "coolantFlow") / mass;
        for (double result : new double[]{lm, mass, velocity, re, pr, nu, hi, u, area, length, turns, height, darcy, pressure, flowRatio}) {
            if (!Double.isFinite(result) || result < 0) throw new IllegalArgumentException("El resultado no es finito o físicamente válido; revisa unidades y dimensiones.");
        }
        boolean gnielinskiRange = re >= 3000 && re <= 5e6 && pr >= 0.5 && pr <= 2000;
        boolean radialFit = coilD + doM + 2 * clearance <= tankD;
        double liquidHeight = get(v, "volume") / (Math.PI * tankD * tankD / 4);
        double assumedAxialClearance = doM;
        boolean heightFit = height + doM + assumedAxialClearance <= liquidHeight;
        schematicResults.clear();
        schematicResults.put("coilDiameter", coilD);
        schematicResults.put("tubeDiameter", doM);
        schematicResults.put("tankDiameter", tankD);
        schematicResults.put("tankHeight", tankH);
        schematicResults.put("liquidHeight", liquidHeight);
        schematicResults.put("pitch", pitch);
        schematicResults.put("turns", turns);
        schematicResults.put("coilHeight", height);
        schematicResults.put("tubeLength", length);
        schematicResults.put("brothTemperature", get(v, "tBroth"));
        schematicResults.put("inletTemperature", get(v, "tIn"));
        schematicResults.put("outletTemperature", get(v, "tOut"));
        schematicResults.put("flowLpm", mass / get(v, "rho") * 60000);
        schematicResults.put("overallU", u);
        Map<String, String> out = new LinkedHashMap<>();
        out.put("Carga metabólica", f(qReaction) + " kW");
        out.put("Carga térmica total", f(qTotal) + " kW");
        out.put("LMTD", f(lm) + " K");
        out.put("Caudal másico requerido", f(mass) + " kg/s");
        out.put("Caudal volumétrico requerido", f(mass / get(v, "rho") * 3600) + " m³/h");
        out.put("Caudal másico disponible", f(get(v, "coolantFlow")) + " kg/s");
        out.put("Velocidad interna", f(velocity) + " m/s");
        out.put("Número de Reynolds", f0(re) + " · " + regime(re));
        out.put("Número de Prandtl", f(pr));
        out.put("Número de Nusselt", f(nu));
        out.put("Número de Dean", f(re * Math.sqrt(di / coilD)));
        out.put("Coeficiente interno estimado", f0(hi) + " W/(m²·K)");
        out.put("Coeficiente global exterior", f0(u) + " W/(m²·K)");
        out.put("Área exterior requerida", f(area) + " m²");
        out.put("Longitud de tubo", f(length) + " m");
        out.put("Vueltas teóricas", f(turns));
        out.put("Altura centro a centro", f(height) + " m");
        out.put("Nivel de caldo (cilindro ideal)", f(liquidHeight) + " m");
        out.put("Capacidad cilíndrica aproximada", f(tankCapacity) + " m³");
        out.put("Holgura axial asumida", f(assumedAxialClearance * 1000) + " mm");
        out.put("Ajuste radial", radialFit ? "Cabe según cotas ingresadas" : "No cabe");
        out.put("Ajuste vertical", heightFit ? "Sumergido bajo el nivel estimado" : "No queda totalmente sumergido");
        out.put("Caída de presión (tubo recto equivalente)", f(pressure / 1000) + " kPa");
        out.put("Estado del caudal", flowRatio >= 1 ? "Suficiente para el objetivo" : "Insuficiente para el objetivo");
        out.put("Rango de Gnielinski", gnielinskiRange ? "Dentro del intervalo orientativo" : "Fuera del intervalo; hᵢ más incierto");
        List<String> warnings = new ArrayList<>();
        if (re < 2300) warnings.add("Laminar: Nu = 3.66 es de tubo recto; no corrige el efecto Dean en la hélice.");
        if (re >= 2300 && re < 4000) warnings.add("Transición: el régimen y las interpolaciones dependen de la geometría helicoidal.");
        if ((re >= 2300 && re < 3000) || !gnielinskiRange) warnings.add("La correlación de Gnielinski está fuera de su intervalo orientativo o se está interpolando.");
        warnings.add("Nu helicoidal usa un multiplicador simplificado; no valida el serpentín real.");
        warnings.add("ΔP usa tubo recto y subestima potencialmente la pérdida de la hélice; no dimensiona una bomba.");
        if (velocity > 2.4) warnings.add("Alerta de cribado (> 2.4 m/s, no es un límite universal): verificar pérdida real, erosión, cavitación y criterio del proceso.");
        if (!heightFit) warnings.add("La hélice supera el nivel de caldo estimado con el volumen y diámetro del tanque.");
        out.put("Advertencias del modelo", String.join(" ", warnings));
        return out;
    }

    private double get(Map<String, Double> v, String key) { return v.get(key); }
    private String regime(double re) { return re >= 4000 ? "turbulento" : re >= 2300 ? "transición" : "laminar"; }
    private double darcyFactor(double re) {
        if (re <= 2300) return 64 / re;
        double turbulentAt4000 = Math.pow(0.79 * Math.log(4000) - 1.64, -2);
        if (re >= 4000) return Math.pow(0.79 * Math.log(re) - 1.64, -2);
        double weight = (re - 2300) / (4000 - 2300);
        return (64 / 2300) * (1 - weight) + turbulentAt4000 * weight;
    }
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
        if (!(d1 > 0) || !(d2 > 0) || !Double.isFinite(d1) || !Double.isFinite(d2)) {
            throw new IllegalArgumentException("Las diferencias de temperatura para ΔT_lm deben ser positivas y finitas.");
        }
        double rel = (d2 - d1) / d1;
        if (Math.abs(rel) < 1e-8) return rel == 0 ? d1 : d1 * rel / Math.log1p(rel);
        return (d2 - d1) / (Math.log(d2) - Math.log(d1));
    }
    private String f(double value) { return String.format(Locale.getDefault(), "%,.3f", value); }
    private String f0(double value) { return String.format(Locale.getDefault(), "%,.0f", value); }

    private void renderResults(Map<String, String> result) {
        resultValues.clear(); resultValues.putAll(result);
        updateIntegratedModel();
        statusText.setText("Actualizado en vivo · cálculo ejecutado en este teléfono");
        statusText.setTextColor(INK);
        summaryGrid.removeAllViews(); resultRows.removeAllViews(); calculatorSummaryGrid.removeAllViews();
        populateSummaryGrid(summaryGrid, result);
        populateSummaryGrid(calculatorSummaryGrid, result);
        boolean flowInsufficient = result.get("Estado del caudal").startsWith("Insuficiente");
        double velocity = Double.parseDouble(result.get("Velocidad interna").replace(" m/s", "").replace(',', '.'));
        boolean heightDoesNotFit = !result.get("Ajuste vertical").startsWith("Sumergido");
        boolean caution = flowInsufficient || velocity > 2.4 || heightDoesNotFit;
        String message = flowInsufficient
                ? "CAUDAL INSUFICIENTE · Aumenta el caudal disponible o revisa las temperaturas objetivo."
                : heightDoesNotFit
                    ? "LA HÉLICE NO QUEDA COMPLETAMENTE BAJO EL NIVEL DE CALDO ESTIMADO."
                    : velocity > 2.4
                        ? "VELOCIDAD INTERNA ALTA · Verifica presión real, erosión y cavitación; ΔP mostrado es de tubo recto."
                        : "EL CAUDAL DISPONIBLE CUBRE EL BALANCE DE ENERGÍA · Revisa las advertencias del modelo.";
        flowWarningText.setText(message);
        flowWarningText.setBackground(rounded(caution ? YELLOW : TEAL, 0, INK));
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

    @SuppressWarnings("deprecation")
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
