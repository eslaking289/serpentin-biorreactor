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
import android.annotation.SuppressLint;
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
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;
import org.json.JSONObject;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int CREATE_CSV = 41;
    private static final int INK = Color.rgb(227, 226, 230);
    private static final int MUTED = Color.rgb(187, 201, 203);
    private static final int TEAL = Color.rgb(32, 201, 216);
    private static final int ON_TEAL = Color.rgb(0, 54, 59);
    private static final int MAGENTA = Color.rgb(207, 206, 255);
    private static final int ERROR = Color.rgb(255, 180, 171);
    private static final int YELLOW = Color.rgb(245, 158, 11);
    private static final int PAPER = Color.rgb(31, 31, 35);
    private static final int BG = Color.rgb(18, 19, 22);
    private static final int LINE = Color.rgb(60, 73, 75);
    private static final int TEAL_SOFT = Color.rgb(0, 54, 59);
    private static final int CORAL_SOFT = Color.rgb(50, 37, 42);
    private static final int AMBER_SOFT = Color.rgb(48, 39, 22);

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
    private final Map<String, String> bioprocessResults = new LinkedHashMap<>();
    private SharedPreferences preferences;
    private LinearLayout resultRows;
    private LinearLayout summaryGrid;
    private LinearLayout calculatorSummaryGrid;
    private TextView flowWarningText;
    private final Button[] navigationButtons = new Button[4];
    private final Button[] modelSectionButtons = new Button[4];
    private final int[] navigationAccents = {TEAL, YELLOW, MAGENTA, TEAL};
    private TextView statusText;
    private WebView modelWebView;
    private WebView bioprocessWebView;
    private boolean bioprocessWebViewReady;
    private boolean modelWebViewLoaded;
    private boolean modelWebViewReady;
    private boolean modelPreviewMode;
    private int selectedModelSection;
    private FrameLayout modelPreviewFrame;
    private FrameLayout modelFullFrame;
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
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        View decor = getWindow().getDecorView();
        decor.setSystemUiVisibility(0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.view.WindowInsetsController controller = decor.getWindowInsetsController();
            if (controller != null) {
                int appearance = android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                controller.setSystemBarsAppearance(0, appearance);
            }
        }
        headlineTypeface = loadFont("fonts/Chivo.ttf", 700, Typeface.DEFAULT_BOLD);
        bodyTypeface = loadFont("fonts/Chivo.ttf", 400, Typeface.DEFAULT);
        monoTypeface = loadFont("fonts/JetBrainsMono.ttf", 500, Typeface.MONOSPACE);
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
        add("phiD", "Fracción de P_agit convertida en calor", "0–1", "Calor metabólico y biomasa", 1);
        add("qOther", "Otras cargas térmicas", "kW", "Calor metabólico y biomasa", 0);
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
        add("foulingInside", "Ensuciamiento interno Rf,i", "m²·K/W", "Transferencia y geometría", 0);
        add("tubeD", "Diámetro exterior del tubo", "mm", "Geometría helicoidal", 25.4);
        add("wallT", "Espesor de pared", "mm", "Geometría helicoidal", 1.65);
        add("coilD", "Diámetro de hélice (línea central)", "mm", "Geometría helicoidal", 550);
        add("tankD", "Diámetro interno del biorreactor", "mm", "Geometría helicoidal", 1100);
        add("clearance", "Holgura radial mínima", "mm", "Geometría helicoidal", 160);
        add("verticalClearance", "Holgura vertical mínima", "mm", "Geometría helicoidal", 0);
        add("pitch", "Paso entre vueltas", "mm", "Geometría helicoidal", 50);
        add("tankHeight", "Altura útil disponible", "mm", "Geometría helicoidal", 1400);
        add("fDarcyCoil", "Factor Darcy hélice (0 = tubo recto)", "—", "Hidráulica y bombeo", 0);
        add("localK", "Suma de pérdidas locales ΣK", "—", "Hidráulica y bombeo", 0);
        add("elevation", "Elevación neta del circuito", "m", "Hidráulica y bombeo", 0);
        add("pumpEfficiency", "Eficiencia bomba η (0 = no calcular)", "0–1", "Hidráulica y bombeo", 0);
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
        brand.setPadding(dp(10), dp(8), dp(8), dp(8));
        brand.setBackground(rounded(PAPER, 2, LINE));
        TextView mark = text("TB", 17, INK, true);
        mark.setGravity(Gravity.CENTER);
        mark.setTextColor(ON_TEAL);
        mark.setBackground(rounded(TEAL, 14, null));
        brand.addView(mark, new LinearLayout.LayoutParams(dp(44), dp(44)));
        LinearLayout brandCopy = new LinearLayout(this);
        brandCopy.setOrientation(LinearLayout.VERTICAL);
        brandCopy.setPadding(dp(12), 0, 0, 0);
        brandCopy.addView(text("THERMABIO", 19, INK, true));
        brandCopy.addView(text("BIOPROCESS ENGINEERING  ●  LOCAL", 9, TEAL, true));
        brand.addView(brandCopy, new LinearLayout.LayoutParams(0, -2, 1));
        Button reset = smallButton("Restablecer");
        reset.setOnClickListener(v -> resetInputs());
        brand.addView(reset);
        page.addView(brand, margins(0, 4, 0, 16));

        LinearLayout hero = card();
        hero.setPadding(dp(20), dp(18), dp(20), dp(20));
        hero.setBackground(rounded(PAPER, 2, LINE));
        TextView eyebrow = text("DISEÑO DE PROCESO  ·  CÁLCULO LOCAL", 10, TEAL, true);
        eyebrow.setLetterSpacing(0.08f);
        hero.addView(eyebrow, margins(0, 0, 0, 8));
        TextView title = text("Serpentín de\nenfriamiento", 26, INK, true);
        title.setLineSpacing(dp(1), 0.98f);
        hero.addView(title, margins(0, 0, 0, 7));
        TextView intro = text("Balance térmico y dimensionamiento preliminar para biorreactores.", 13, MUTED, false);
        intro.setLineSpacing(dp(3), 1f);
        hero.addView(intro);
        page.addView(hero, margins(0, 0, 0, 16));

        calculatorSummaryGrid = new LinearLayout(this);
        calculatorSummaryGrid.setOrientation(LinearLayout.VERTICAL);
        page.addView(calculatorSummaryGrid, margins(0, 0, 0, 9));

        String currentGroup = "";
        LinearLayout groupCard = null;
        for (Spec spec : specs) {
            if (!spec.group.equals(currentGroup)) {
                currentGroup = spec.group;
                groupCard = card();
                groupCard.setPadding(dp(12), dp(12), dp(12), dp(8));
                page.addView(groupCard, margins(0, 0, 0, 10));
                TextView heading = text(currentGroup.toUpperCase(Locale.ROOT), 13, INK, true);
                heading.setLetterSpacing(0.02f);
                int groupAccent = currentGroup.equals("Calor metabólico y biomasa") ? AMBER_SOFT : currentGroup.equals("Temperaturas del proceso") ? TEAL_SOFT : currentGroup.equals("Propiedades del refrigerante") ? PAPER : CORAL_SOFT;
                heading.setPadding(dp(9), dp(9), dp(9), dp(9));
                heading.setTextColor(currentGroup.equals("Transferencia y geometría") || currentGroup.equals("Geometría helicoidal") ? MAGENTA : TEAL);
                heading.setBackground(rounded(groupAccent, 1, LINE));
                groupCard.addView(heading, margins(0, 0, 0, 4));
                TextView note = text(groupNote(currentGroup), 10, MUTED, false);
                note.setLineSpacing(dp(2), 1f);
                groupCard.addView(note, margins(0, 0, 0, 8));
            }
            addEditor(groupCard, spec);
        }
        addMetabolicBalanceCard(calculatorScreen);

        Button calculate = new Button(this);
        calculate.setText("CALCULAR DIMENSIONAMIENTO");
        calculate.setTextColor(INK);
        calculate.setTextSize(16);
        calculate.setAllCaps(false);
        calculate.setTypeface(headlineTypeface);
        calculate.setTextColor(ON_TEAL);
        calculate.setBackground(rounded(TEAL, 2, LINE));
        calculate.setElevation(dp(2));
        calculate.setPadding(dp(18), dp(13), dp(18), dp(13));
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
        flowWarningText.setBackground(rounded(AMBER_SOFT, 12, LINE));
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
                equationText("Q_met = q_met · V; Q_agit = φd · P_agit"),
                equationText("Q_total = q_met·V + φd·P_agit + Q_otros"),
                equationText("Q_rxn = −ΔH_rxn · consumo molar / 3600 [kW, mol/h y kJ/mol]"),
                equationText("M · Cp,b · dTb/dt = Q_generado − Q_serpentín − Q_ambiente + Q_alimentación"));
        addMethodBlock(method, "02 / DIFERENCIA MEDIA LOGARÍTMICA", TEAL,
                equationText("ΔT₁ = T_caldo − T_salida"),
                equationText("ΔT₂ = T_caldo − T_entrada"),
                equationLine(equationText("ΔT_lm ="), fraction("ΔT₂ − ΔT₁", "ln(ΔT₂ / ΔT₁)")),
                equationLine(equationText("ṁ_refrigerante ="), fraction("Q_diseño", "Cp · ΔT_refrigerante")),
                equationText("Q_refrigerante = ṁ · Cp · (T_salida−T_entrada)"),
                equationText("T_salida = T_entrada + Q/(ṁ · Cp)"));
        addMethodBlock(method, "03 / TRANSFERENCIA EN EL TUBO", MAGENTA,
                equationLine(equationText("ṁ_req ="), fraction("Q_diseño", "C_p · (T_s − T_e)")),
                equationLine(equationText("v ="), fraction("4ṁ", "ρπD_i²")),
                equationLine(equationText("Re ="), fraction("4ṁ", "πD_iμ")),
                equationLine(equationText("Pr ="), fraction("μ · C_p · 1000", "k_f")),
                equationText("Nu laminar usado: 3.66; 4.36 corresponde a flujo de pared constante"),
                equationText("Nu transición 2300≤Re<3000: w=(Re−2300)/700; Nu=(1−w)·3.66+w·Nu_G(3000,Pr)"),
                equationText("Nu_G=(f/8)(Re−1000)Pr / [1+12.7√(f/8)(Pr^(2/3)−1)]"),
                equationLine(equationText("h_i ="), fraction("Nu · k_f", "D_i")),
                equationText("De = Re√(D_i/D_hélice); se reporta sin corrección helicoidal"));
        addMethodBlock(method, "04 / RESISTENCIAS Y ÁREA EXTERIOR", TEAL,
                equationText("1/Uo=Do/(Di·hi)+Do·ln(Do/Di)/(2kp)+1/ho+Rf,o+(Do/Di)·Rf,i"),
                equationLine(equationText("A_o ="), fraction("1000 · Q_diseño", "U_o · ΔT_lm")),
                equationText("L_requerida=A_o/(π·Do); A_instalada=π·Do·L_instalada"),
                equationLine(equationText("Q_capacidad = U_o · A_instalada · ΔT_lm")),
                equationText("Suficiencia térmica = Q_capacidad / Q_diseño"));
        addMethodBlock(method, "05 / GEOMETRÍA Y CAÍDA DE PRESIÓN", INK,
                equationText("D_i = D_o − 2 t_pared"),
                equationText("D_hélice + D_o + 2 holgura ≤ D_tanque"),
                equationText("L_vuelta = √[(π · D_hélice)² + paso²]"),
                equationText("N_teóricas = L_req/L_vuelta; N_reales = ceil(N_teóricas)"),
                equationText("L_instalada=N_reales·L_vuelta; Htotal=(Nreales−1)·paso+Do"),
                equationText("fD=64/Re (Re≤2300); w=(Re−2300)/1700; fD interpola a fP(4000); luego fP=[0.79ln(Re)−1.64]⁻²"),
                equationText("ΔP_total=f_D(L/D_i)ρv²/2 + ΣKρv²/2 + ρgΔz; P_bomba=ΔP·Q/η"));
        addMethodBlock(method, "06 / BIOPROCESO Y CALOR ACOPLADO", YELLOW,
                equationText("μ=μmax·S/(Ks+S); μO₂=μ·CL/(KO₂+CL)"),
                equationText("dX/dt=(μ−kd)X; qS=μ/Yx/s+ms; rS=qS·X"),
                equationText("OUR=qO₂·X; OTR=kLa(C*−CL); dCL/dt=OTR−OUR"),
                equationText("Qmet,O₂=YQ/O₂·OUR·V·1000/3600 [kW]"),
                equationText("Qmet,rxn=−ΔHrxn·consumo molar/3600 [kW]"),
                equationText("Reimp=ρ·N·D²/μ; Pagit=Np·ρ·N³·D⁵ [W]"),
                equationText("Qagit=φd·Pagit/1000 [kW]"),
                equationText("Nu caldo=0.9·Re^0.62·Pr^0.33·(μ/μw)^0.14 [correlación Doran]"),
                equationText("hcaldo=Nu·kcaldo/Dtanque"),
                equationText("Qgenerado=Qmet+Qagit+Qfeed+Qotros; OUR y ΔH son métodos alternativos"),
                equationText("M·Cp·dT/dt=Qgenerado−Qremovido−Qambiente+Qalimentación"),
                equationText("Qcapacidad=Uo·Ainstalada·ΔTlm; suficiencia=Qcapacidad/Qdiseño"));
        LinearLayout assumptions = card();
        TextView assumptionsTitle = text("SUPUESTOS Y LÍMITES", 16, INK, true);
        assumptionsTitle.setBackground(rounded(Color.rgb(41, 42, 45), 6, LINE));
        assumptionsTitle.setPadding(dp(8), dp(6), dp(8), dp(6));
        assumptions.addView(assumptionsTitle, margins(0, 0, 0, 8));
        addAssumptionRow(assumptions, "Caldo perfectamente mezclado a temperatura uniforme; sin gradientes térmicos.", TEAL);
        addAssumptionRow(assumptions, "Los parámetros biológicos se ingresan con unidades y origen declarados; no hay valores por especie.", YELLOW);
        addAssumptionRow(assumptions, "OUR y entalpía son métodos alternativos para el calor; no se suman entre sí.", MAGENTA);
        addAssumptionRow(assumptions, "Gnielinski orientativa para 3,000 ≤ Re ≤ 5×10⁶ y 0.5 ≤ Pr ≤ 2,000.", TEAL);
        addAssumptionRow(assumptions, "Nu laminar y Gnielinski son correlaciones de tubo recto; Dean se informa sin factor añadido.", MAGENTA);
        addAssumptionRow(assumptions, "Sin fD de serpentín, la presión usa Darcy recto como aproximación y se advierte en resultados.", TEAL);
        addAssumptionRow(assumptions, "μ se convierte de mPa·s a Pa·s; Cp de kJ/(kg·K) a J/(kg·K) para Pr.", YELLOW);
        addAssumptionRow(assumptions, "ΔP de tubo recto equivalente; no incluye curvatura ni pérdidas locales.", YELLOW);
        addAssumptionRow(assumptions, "Estimación preliminar: verificar propiedades, bomba y diseño mecánico por separado.", MAGENTA);
        methodologyScreen.addView(assumptions, margins(0, 0, 0, 14));
        Button toCalculator = smallButton("Ir a la calculadora  →");
        toCalculator.setBackground(projected(TEAL));
        toCalculator.setTextColor(ON_TEAL);
        toCalculator.setOnClickListener(v -> showScreen(0));
        methodologyScreen.addView(toCalculator, margins(0, 0, 0, 14));

        TextView disclaimer = text("ESTIMACIÓN ACADÉMICA PRELIMINAR. VERIFICA PROPIEDADES Y CORRELACIONES ANTES DE FABRICAR U OPERAR UN EQUIPO.", 11, MUTED, true);
        disclaimer.setLineSpacing(dp(3), 1f);
        methodologyScreen.addView(disclaimer, margins(2, 0, 2, 12));
        FrameLayout.LayoutParams scrollParams = new FrameLayout.LayoutParams(-1, -1);
        scrollParams.bottomMargin = dp(92);
        addFullIndexScreen(modelScreen);
        root.addView(scrollView, scrollParams);

        LinearLayout navigation = new LinearLayout(this);
        navigation.setOrientation(LinearLayout.HORIZONTAL);
        navigation.setGravity(Gravity.CENTER);
        navigation.setPadding(dp(7), dp(6), dp(7), dp(6));
        navigation.setBackground(rounded(PAPER, 1, LINE));
        navigation.setElevation(dp(5));
        addNavigationItem(navigation, "Módulos", TEAL, 0);
        addNavigationItem(navigation, "Simulación", YELLOW, 1);
        addNavigationItem(navigation, "Fórmulas", MAGENTA, 2);
        addNavigationItem(navigation, "Serpentín 3D", TEAL, 3);
        FrameLayout.LayoutParams navParams = new FrameLayout.LayoutParams(-1, dp(70), Gravity.BOTTOM);
        navParams.setMargins(dp(12), 0, dp(12), dp(8));
        root.addView(navigation, navParams);
        setContentView(root);
    }

    private void addMethodBlock(LinearLayout parent, String title, int accent, View... equations) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setPadding(dp(10), dp(8), dp(10), dp(8));
        block.setBackground(rounded(PAPER, 8, LINE));
        TextView heading = text(title, 11, accent == INK ? INK : accent, true);
        heading.setBackground(rounded(softAccent(accent), 4, null));
        heading.setPadding(dp(10), dp(8), dp(10), dp(8));
        block.addView(heading);
        LinearLayout equationPanel = new LinearLayout(this);
        equationPanel.setOrientation(LinearLayout.VERTICAL);
        equationPanel.setPadding(dp(5), dp(6), dp(5), dp(6));
        equationPanel.setBackground(rounded(Color.rgb(13, 14, 17), 6, LINE));
        for (View equation : equations) equationPanel.addView(equation, new LinearLayout.LayoutParams(-1, -2));
        block.addView(equationPanel, margins(0, 5, 0, 0));
        parent.addView(block, margins(0, 0, 0, 8));
    }

    private TextView equationText(String source) {
        TextView view = text("", 13, INK, false);
        view.setText(mathText(source));
        view.setTypeface(monoTypeface);
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
        row.setBackground(rounded(Color.rgb(41, 42, 45), 4, LINE));
        TextView mark = text("✓", 13, accent, true);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(rounded(softAccent(accent), 9, null));
        row.addView(mark, new LinearLayout.LayoutParams(dp(27), dp(27)));
        TextView label = text("", 11, INK, false);
        label.setText(mathText(copy));
        label.setPadding(dp(8), 0, 0, 0);
        row.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
        parent.addView(row, margins(0, 0, 0, 6));
    }

    private int softAccent(int accent) {
        if (accent == TEAL) return TEAL_SOFT;
        if (accent == MAGENTA) return CORAL_SOFT;
        if (accent == YELLOW) return AMBER_SOFT;
        return Color.rgb(41, 42, 45);
    }

    private void addNavigationItem(LinearLayout parent, String title, int accent, int destination) {
        Button button = new Button(this);
        String[] icons = {"▦", "⌁", "Σ", "◉"};
        button.setText(icons[destination] + "\n" + title);
        button.setTextSize(9.5f);
        button.setTextColor(MUTED);
        button.setAllCaps(false);
        button.setMinHeight(dp(54));
        button.setTypeface(headlineTypeface);
        button.setPadding(dp(2), 0, dp(2), 0);
        button.setBackground(rounded(Color.TRANSPARENT, 8, null));
        button.setGravity(Gravity.CENTER);
        navigationButtons[destination] = button;
        styleNavigationButton(button, destination == currentScreen);
        button.setOnClickListener(v -> showScreen(destination));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1f);
        params.setMargins(dp(3), 0, dp(3), 0);
        parent.addView(button, params);
    }

    private void styleNavigationButton(Button button, boolean selected) {
        button.setTextColor(selected ? TEAL : MUTED);
        button.setTypeface(headlineTypeface, selected ? Typeface.BOLD : Typeface.NORMAL);
        button.setBackground(rounded(selected ? TEAL_SOFT : Color.TRANSPARENT, 8, selected ? TEAL : null));
    }

    private void addBioreactorResultsCard(LinearLayout parent) {
        LinearLayout module = card();
        module.setPadding(dp(10), dp(10), dp(10), dp(4));
        TextView tag = text("MODELO PARAMÉTRICO  /  COTAS EN VIVO", 10, INK, true);
        tag.setTextColor(TEAL);
        tag.setBackground(rounded(TEAL_SOFT, 9, null));
        tag.setPadding(dp(8), dp(6), dp(8), dp(6));
        module.addView(tag, margins(0, 0, 0, 8));
        module.addView(coilSchematic());
        Button explore = smallButton("Explorar modelo 3D  →");
        explore.setBackground(projected(TEAL));
        explore.setTextColor(ON_TEAL);
        explore.setOnClickListener(v -> showScreen(3));
        module.addView(explore, margins(0, 8, 0, 0));
        parent.addView(module, margins(0, 0, 0, 4));
    }

    private void addMetabolicBalanceCard(LinearLayout parent) {
        LinearLayout module = card();
        module.setPadding(dp(8), dp(8), dp(8), dp(8));
        TextView tag = text("06  /  BIOPROCESO Y BALANCE DE CALOR", 13, INK, true);
        tag.setPadding(dp(9), dp(9), dp(9), dp(9));
        tag.setBackground(rounded(AMBER_SOFT, 1, LINE));
        module.addView(tag, margins(0, 0, 0, 6));
        TextView note = text("ENLACE EN VIVO: los valores compartidos se actualizan entre este análisis y el dimensionamiento térmico.", 11, MUTED, false);
        note.setLineSpacing(dp(2), 1f);
        module.addView(note, margins(2, 0, 2, 8));

        bioprocessWebView = new WebView(this);
        bioprocessWebView.setBackgroundColor(BG);
        bioprocessWebView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        bioprocessWebView.setVerticalScrollBarEnabled(false);
        bioprocessWebView.setHorizontalScrollBarEnabled(false);
        bioprocessWebView.setNestedScrollingEnabled(false);
        bioprocessWebView.setOnTouchListener((view, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_MOVE && view.getParent() != null) {
                view.getParent().requestDisallowInterceptTouchEvent(false);
            }
            return false;
        });
        bioprocessWebView.getSettings().setJavaScriptEnabled(true);
        bioprocessWebView.getSettings().setAllowFileAccess(true);
        bioprocessWebView.addJavascriptInterface(new BioprocessBridge(), "ThermaBioNative");
        bioprocessWebView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                bioprocessWebViewReady = true;
                syncNativeInputsToBioprocess();
            }
        });
        module.addView(bioprocessWebView, new LinearLayout.LayoutParams(-1, dp(320)));
        bioprocessWebView.loadUrl("file:///android_asset/bioprocess.html");
        parent.addView(module, margins(0, 0, 0, 14));
    }

    private final class BioprocessBridge {
        @JavascriptInterface public void setContentHeight(int cssHeight) {
            runOnUiThread(() -> {
                if (bioprocessWebView == null) return;
                int height = Math.max(dp(300), Math.min(dp(4800), dp(cssHeight)));
                View view = bioprocessWebView;
                android.view.ViewGroup.LayoutParams params = view.getLayoutParams();
                if (params != null && params.height != height) {
                    params.height = height;
                    view.setLayoutParams(params);
                }
            });
        }

        @JavascriptInterface public void updateLinkedInputs(String json) {
            runOnUiThread(() -> {
                try {
                    JSONObject data = new JSONObject(json);
                    bioprocessResults.clear();
                    JSONObject summary = data.optJSONObject("summary");
                    if (summary != null) {
                        java.util.Iterator<String> keys = summary.keys();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            bioprocessResults.put(key, summary.optString(key, "—"));
                        }
                    }
                    initializing = true;
                    double volume = data.optDouble("volumeM3", Double.NaN);
                    if (Double.isFinite(volume) && volume > 0) setEditorValue("volume", volume);
                    setEditorValue("qMet", volume > 0 && !data.isNull("metabolicKW")
                            ? data.getDouble("metabolicKW") / volume : 0);
                    setEditorValue("agitation", data.isNull("agitationPowerKW") ? 0 : data.getDouble("agitationPowerKW"));
                    setEditorValue("phiD", data.isNull("agitationHeatFraction") ? 1 : data.getDouble("agitationHeatFraction"));
                    setEditorValue("qOther", data.isNull("otherKW") ? 0 : data.getDouble("otherKW"));
                    setIfPresent(data, "brothTemperatureC", "tBroth");
                    setIfPresent(data, "coolantInletTempC", "tIn");
                    setIfPresent(data, "coolantMassFlowKgS", "coolantFlow");
                    setIfPresent(data, "coolantCpKJPerKgK", "cp");
                    setIfPresent(data, "coolantDensityKgM3", "rho");
                    double inlet = editors.containsKey("tIn") ? readEditorValue("tIn") : Double.NaN;
                    double delta = data.optDouble("coolantDeltaTK", Double.NaN);
                    if (Double.isFinite(inlet) && Double.isFinite(delta) && delta > 0) setEditorValue("tOut", inlet + delta);
                    initializing = false;
                    calculate(false);
                } catch (Exception error) {
                    initializing = false;
                    Toast.makeText(MainActivity.this, "No se pudieron sincronizar los valores: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }

        @JavascriptInterface public void updateSummary(String json) {
            runOnUiThread(() -> {
                try {
                    JSONObject summary = new JSONObject(json);
                    bioprocessResults.clear();
                    java.util.Iterator<String> keys = summary.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        bioprocessResults.put(key, summary.optString(key, "—"));
                    }
                    calculate(false);
                } catch (Exception ignored) { }
            });
        }
    }

    private void setIfPresent(JSONObject data, String sourceKey, String targetKey) throws Exception {
        if (!data.isNull(sourceKey)) setEditorValue(targetKey, data.getDouble(sourceKey));
    }

    private double readEditorValue(String key) {
        if (!editors.containsKey(key)) return Double.NaN;
        try { return Double.parseDouble(editors.get(key).getText().toString().trim().replace(',', '.')); }
        catch (Exception ignored) { return Double.NaN; }
    }

    private void syncNativeInputsToBioprocess() {
        if (bioprocessWebView == null || !bioprocessWebViewReady) return;
        try {
            double volume = readEditorValue("volume");
            double qMet = readEditorValue("qMet");
            double qOther = readEditorValue("qOther");
            double coolantIn = readEditorValue("tIn");
            JSONObject linked = new JSONObject();
            putIfFinite(linked, "volumeM3", volume);
            putIfFinite(linked, "brothTemperatureC", readEditorValue("tBroth"));
            putIfFinite(linked, "manualMetabolicKW", qMet * volume);
            putIfFinite(linked, "agitationKW", readEditorValue("agitation"));
            putIfFinite(linked, "agitationHeatFraction", readEditorValue("phiD"));
            putIfFinite(linked, "otherHeatTotalKW", qOther);
            putIfFinite(linked, "coolantInletTempC", coolantIn);
            putIfFinite(linked, "coolantDeltaTK", readEditorValue("tOut") - coolantIn);
            putIfFinite(linked, "coolantMassFlowKgS", readEditorValue("coolantFlow"));
            putIfFinite(linked, "coolantCpKJPerKgK", readEditorValue("cp"));
            putIfFinite(linked, "coolantDensityKgM3", readEditorValue("rho"));
            bioprocessWebView.evaluateJavascript("window.ThermaBioBioprocessUi&&window.ThermaBioBioprocessUi.setNativeValues(" + linked + ")", null);
        } catch (Exception ignored) { }
    }

    private void putIfFinite(JSONObject object, String key, double value) throws Exception {
        if (Double.isFinite(value)) object.put(key, value);
    }

    private void setEditorValue(String key, double value) {
        if (!Double.isFinite(value) || !editors.containsKey(key)) return;
        String text = Double.toString(value);
        editors.get(key).setText(text);
        preferences.edit().putString(key, text).apply();
    }

    private LinearLayout newScreen() {
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setPadding(dp(18), dp(14), dp(18), dp(28));
        return screen;
    }

    private void addScreenHeader(LinearLayout screen, String title, String subtitle) {
        LinearLayout brand = new LinearLayout(this);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        brand.setPadding(dp(10), dp(8), dp(8), dp(8));
        brand.setBackground(rounded(PAPER, 1, LINE));
        TextView mark = text("TB", 17, INK, true);
        mark.setGravity(Gravity.CENTER);
        mark.setTextColor(ON_TEAL);
        mark.setBackground(rounded(TEAL, 14, null));
        brand.addView(mark, new LinearLayout.LayoutParams(dp(42), dp(42)));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(10), 0, 0, 0);
        copy.addView(text("THERMABIO", 18, INK, true));
        copy.addView(text("● SIN RED  ·  CÁLCULO LOCAL", 9, MUTED, true));
        brand.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        screen.addView(brand, margins(0, 4, 0, 14));
        TextView heading = text(title, 23, INK, true);
        heading.setLineSpacing(dp(1), 1f);
        heading.setPadding(dp(10), dp(9), dp(10), dp(9));
        heading.setBackground(rounded(PAPER, 1, LINE));
        screen.addView(heading, margins(0, 0, 0, 5));
        TextView caption = text(subtitle, 10, MUTED, true);
        caption.setTextColor(MUTED);
        caption.setLetterSpacing(0.04f);
        screen.addView(caption, margins(0, 0, 0, 16));
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
        if (screenIndex == 3) {
            showModelIn(modelFullFrame, false);
            selectModelSection(0);
        }
        for (int i = 0; i < navigationButtons.length; i++) {
            if (navigationButtons[i] != null) {
                styleNavigationButton(navigationButtons[i], i == screenIndex);
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
        LinearLayout sections = new LinearLayout(this);
        sections.setOrientation(LinearLayout.HORIZONTAL);
        sections.setGravity(Gravity.CENTER);
        sections.setPadding(dp(4), dp(4), dp(4), dp(4));
        sections.setBackground(rounded(PAPER, 1, LINE));
        String[] sectionNames = {"Modelo", "Componentes", "Parámetros", "Cálculos"};
        for (int i = 0; i < sectionNames.length; i++) {
            final int section = i;
            Button button = new Button(this);
            button.setText(sectionNames[i]);
            button.setTextSize(9.5f);
            button.setAllCaps(false);
            button.setMinHeight(dp(42));
            button.setPadding(dp(2), 0, dp(2), 0);
            button.setTypeface(headlineTypeface);
            button.setBackground(rounded(Color.TRANSPARENT, 12, null));
            modelSectionButtons[i] = button;
            styleModelSectionButton(button, i == selectedModelSection);
            button.setOnClickListener(v -> selectModelSection(section));
            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(0, -1, 1f);
            buttonParams.setMargins(dp(2), 0, dp(2), 0);
            sections.addView(button, buttonParams);
        }
        parent.addView(sections, margins(0, 0, 0, 10));
        modelFullFrame = new FrameLayout(this);
        modelFullFrame.setBackground(rounded(PAPER, 1, LINE));
        float density = getResources().getDisplayMetrics().density;
        int viewportHeight = Math.round(getResources().getDisplayMetrics().heightPixels / density) - 220;
        parent.addView(modelFullFrame, new LinearLayout.LayoutParams(-1, dp(Math.max(640, Math.min(900, viewportHeight)))));
    }

    private void selectModelSection(int section) {
        selectedModelSection = section;
        for (int i = 0; i < modelSectionButtons.length; i++) {
            if (modelSectionButtons[i] != null) styleModelSectionButton(modelSectionButtons[i], i == section);
        }
        if (modelWebView == null || !modelWebViewReady) return;
        if (section == 0) {
            modelWebView.evaluateJavascript("window.scrollTo({top:0,behavior:'smooth'})", null);
            return;
        }
        String tab = section == 1 ? "comp" : section == 2 ? "par" : "calc";
        String js = "(function(){var b=document.querySelector('.tab[data-tab=\"" + tab + "\"]');if(b)b.click();"
                + "var p=document.querySelector('.side');if(p)p.scrollIntoView({behavior:'smooth',block:'start'})})()";
        modelWebView.evaluateJavascript(js, null);
    }

    private void styleModelSectionButton(Button button, boolean selected) {
        button.setTextColor(selected ? TEAL : MUTED);
        button.setTypeface(headlineTypeface, selected ? Typeface.BOLD : Typeface.NORMAL);
        button.setBackground(rounded(selected ? TEAL_SOFT : Color.TRANSPARENT, 12, null));
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void ensureModelWebView() {
        if (modelWebView != null) return;
        modelWebView = new WebView(this);
        modelWebView.setBackgroundColor(BG);
        modelWebView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        modelWebView.setVerticalScrollBarEnabled(false);
        modelWebView.setHorizontalScrollBarEnabled(false);
        modelWebView.setNestedScrollingEnabled(false);
        android.webkit.WebSettings settings = modelWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(false);
        settings.setBlockNetworkLoads(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        modelWebView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                modelWebViewReady = true;
                applyModelContainerStyle();
                updateIntegratedModel();
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest request) {
                return !"file:///android_asset/index.html".equals(request.getUrl().toString());
            }
            @SuppressWarnings("deprecation")
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return !"file:///android_asset/index.html".equals(url);
            }
        });
    }

    private void showModelIn(FrameLayout target, boolean preview) {
        if (target == null) return;
        ensureModelWebView();
        modelPreviewMode = preview;
        android.view.ViewParent oldParent = modelWebView.getParent();
        if (oldParent instanceof android.view.ViewGroup) ((android.view.ViewGroup) oldParent).removeView(modelWebView);
        target.removeAllViews();
        target.addView(modelWebView, new FrameLayout.LayoutParams(-1, -1));
        if (modelWebViewReady) applyModelContainerStyle();
        if (!modelWebViewLoaded) {
            modelWebViewLoaded = true;
            modelWebView.loadUrl("file:///android_asset/index.html");
        }
    }

    private void applyModelContainerStyle() {
        if (modelWebView == null || !modelWebViewReady) return;
        String js = "(function(){if(!document.body)return;document.body.dataset.theme='light';"
                + "var s=document.getElementById('thermabioNativeStyle');if(!s){s=document.createElement('style');"
                + "s.id='thermabioNativeStyle';document.head.appendChild(s);}"
                + "s.textContent='html,body{margin:0!important;background:#121316!important;color:#e3e2e6!important;font-family:Chivo,Arial,sans-serif!important}"
                + "@font-face{font-family:Chivo;src:url(file:///android_asset/fonts/Chivo.ttf)}@font-face{font-family:JetBrainsMono;src:url(file:///android_asset/fonts/JetBrainsMono.ttf)}"
                + ".iso[data-theme=light]{--anatomy-page:#121316;--anatomy-card:#1f1f23;--anatomy-paper:#1f1f23;--anatomy-face:#292a2d;--anatomy-top:#343538;--anatomy-text:#bbc9cb;--anatomy-strong:#e3e2e6;--anatomy-muted:#859395;--anatomy-hi:#e3e2e6;--anatomy-mid:#bbc9cb;--anatomy-lo:#3c494b;--anatomy-faint:#3f4147;--anatomy-lit:#20c9d8;--anatomy-halo:#3c494b;--anatomy-dot:#20c9d8;--anatomy-red:#ffb4ab;--anatomy-green:#51dea2;--anatomy-blue:#aeb0ff}"
                + ".app{max-width:none!important;padding:10px 8px 18px!important}"
                + ".layout{display:block!important}.stage{position:static!important}"
                + ".top{display:flex!important;justify-content:flex-end!important;padding:0 4px 8px!important}.top>div{display:none!important}"
                + ".btn{border-radius:6px!important;border:1px solid #3f4147!important;background:#292a2d!important;color:#e3e2e6!important;box-shadow:none!important;font-family:JetBrainsMono,monospace!important}"
                + ".iso-plate,.side{border-radius:8px!important;border:1px solid #3f4147!important;box-shadow:none!important}"
                + ".stagecard{position:relative!important;overflow:hidden!important}.tb-zoom{position:absolute;z-index:20;right:10px;bottom:10px;display:flex;align-items:center;gap:5px;padding:5px;border:1px solid #3f4147;border-radius:6px;background:#1f1f23}.tb-zoom button{min-width:38px;height:36px;border:1px solid #3f4147;border-radius:4px;background:#20c9d8;color:#00363b;font-size:16px;font-weight:700}.tb-zoom button.tb-zoom-reset{min-width:52px;font-size:12px}"
                + ".native-preview .app{height:100%!important;padding:0!important}"
                + ".native-preview .top,.native-preview .bar,.native-preview .tshow,.native-preview .side,.native-preview .iso-legend{display:none!important}"
                + ".native-preview .layout,.native-preview .stage,.native-preview .iso-figure,.native-preview .stagecard,.native-preview #fig{height:100%!important;margin:0!important}"
                + ".native-preview .stagecard{padding:4px!important;border:0!important;box-shadow:none!important}"
                + ".native-preview #svg{height:100%!important;max-height:100%!important;width:100%!important}';"
                + "document.body.classList.toggle('native-preview'," + modelPreviewMode + ");"
                + "var stage=document.querySelector('.stagecard');if(stage){var zoom=document.getElementById('tbModelZoom');if(!zoom){zoom=document.createElement('div');zoom.id='tbModelZoom';zoom.className='tb-zoom';[['−','Alejar modelo',-0.25],['100%','Restablecer zoom',0],['+','Acercar modelo',0.25]].forEach(function(item,index){var b=document.createElement('button');b.type='button';b.textContent=item[0];b.setAttribute('aria-label',item[1]);if(index===1)b.className='tb-zoom-reset';zoom.appendChild(b)});stage.appendChild(zoom)}}"
                + "var svg=document.getElementById('svg');if(svg&&!svg.dataset.tbZoomReady){svg.dataset.tbZoomReady='1';window.__tbModelZoom=1;var applyZoom=function(value){window.__tbModelZoom=Math.max(0.75,Math.min(2.5,value));svg.style.transform='scale('+window.__tbModelZoom+')';svg.style.transformOrigin='50% 50%';svg.style.transition='transform 120ms ease';var reset=document.querySelector('.tb-zoom-reset');if(reset)reset.textContent=Math.round(window.__tbModelZoom*100)+'%'};document.querySelectorAll('#tbModelZoom button').forEach(function(b,index){b.addEventListener('click',function(){applyZoom(index===1?1:window.__tbModelZoom+(index===0?-0.25:0.25))})});var pinchBase=0,pinchZoom=1,distance=function(t){var x=t[0].clientX-t[1].clientX,y=t[0].clientY-t[1].clientY;return Math.sqrt(x*x+y*y)};svg.addEventListener('touchstart',function(e){if(e.touches.length===2){pinchBase=distance(e.touches);pinchZoom=window.__tbModelZoom;e.preventDefault()}},{passive:false});svg.addEventListener('touchmove',function(e){if(e.touches.length===2&&pinchBase){e.preventDefault();applyZoom(pinchZoom*distance(e.touches)/pinchBase)}},{passive:false});svg.addEventListener('touchend',function(e){if(e.touches.length<2)pinchBase=0})}"
                + "var p=document.getElementById('btnPause');if(" + modelPreviewMode + "&&p&&p.getAttribute('aria-pressed')!=='true')p.click();"
                + "window.scrollTo(0,0);})()";
        modelWebView.evaluateJavascript(js, null);
    }

    private View coilSchematic() {
        modelPreviewFrame = new FrameLayout(this);
        modelPreviewFrame.setBackground(rounded(PAPER, 1, LINE));
        float density = getResources().getDisplayMetrics().density;
        int screenWidthDp = Math.round(getResources().getDisplayMetrics().widthPixels / density);
        int diagramHeight = Math.min(420, Math.round((screenWidthDp - 52) * 470f / 620f + 58));
        modelPreviewFrame.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(diagramHeight)));
        return modelPreviewFrame;
    }

    private void updateIntegratedModel() {
        if (schematicResults.isEmpty()) return;
        if (modelWebView != null && modelWebViewReady) {
            double broth = schematicResults.get("brothTemperature");
            double safeTarget = broth - Math.max(1.0, Math.abs(broth) * 0.01);
            String params = "{"
                    + "Vw:" + jsNumber(get(schematicResults, "liquidHeight") * Math.PI * Math.pow(get(schematicResults, "tankDiameter"), 2) / 4 * 1000) + ","
                    + "Di:" + jsNumber(get(schematicResults, "tankDiameter") * 100) + ","
                    + "Ht:" + jsNumber(get(schematicResults, "tankHeight") * 100) + ","
                    + "De:" + jsNumber(get(schematicResults, "tubeDiameter") * 1000) + ","
                    + "Dc:" + jsNumber(get(schematicResults, "coilDiameter") * 100) + ","
                    + "N:" + jsNumber(get(schematicResults, "turns")) + ","
                    + "L:" + jsNumber(get(schematicResults, "tubeLength")) + ","
                    + "p:" + jsNumber(get(schematicResults, "pitch") * 100) + ","
                    + "T0:" + jsNumber(broth) + ",Tobj:" + jsNumber(safeTarget) + ","
                    + "Tin:" + jsNumber(get(schematicResults, "inletTemperature")) + ","
                    + "Tout:" + jsNumber(get(schematicResults, "outletTemperature")) + ","
                    + "caudal:" + jsNumber(get(schematicResults, "flowLpm")) + ","
                    + "U:" + jsNumber(get(schematicResults, "overallU")) + "}";
            modelWebView.evaluateJavascript("window.BiorreactorApp&&window.BiorreactorApp.setParametros(" + params + ")", null);
        }
    }

    private String jsNumber(double value) {
        return Double.isFinite(value) ? String.format(Locale.US, "%.8f", value) : "0";
    }

    private String groupNote(String group) {
        if (group.equals("Calor metabólico y biomasa")) return "Calor volumétrico y potencia disipada";
        if (group.equals("Temperaturas del proceso")) return "Fuerza impulsora y capacidad de refrigeración";
        if (group.equals("Propiedades del refrigerante")) return "Propiedades del agua y coeficiente exterior";
        if (group.equals("Hidráulica y bombeo")) return "Introduce fD,coil y pérdidas conocidas; 0 usa la aproximación recta";
        return "Propiedades, coeficientes y dimensiones del serpentín";
    }

    private void addEditor(LinearLayout parent, Spec spec) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        TextView label = text(spec.label, 11.5f, INK, true);
        label.setMinHeight(dp(34));
        label.setLineSpacing(dp(1), 1f);
        cell.addView(label, margins(0, 2, 0, 4));
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(rounded(PAPER, 1, LINE));
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
        int unitAccent = spec.group.equals("Temperaturas del proceso") ? TEAL_SOFT :
                spec.group.equals("Propiedades del refrigerante") ? Color.rgb(41, 42, 45) :
                spec.group.equals("Geometría helicoidal") ? CORAL_SOFT : AMBER_SOFT;
        unit.setTextColor(MUTED);
        unit.setBackground(rounded(unitAccent, 1, LINE));
        unit.setPadding(dp(8), dp(5), dp(8), dp(5));
        LinearLayout.LayoutParams unitParams = new LinearLayout.LayoutParams(-2, -2);
        unitParams.setMargins(0, dp(5), dp(5), dp(5));
        row.addView(unit, unitParams);
        cell.addView(row);
        if ("qMet".equals(spec.key) || "volume".equals(spec.key) || "agitation".equals(spec.key)) {
            Button solve = smallButton("Despejar este dato");
            solve.setTextSize(10);
            solve.setMinHeight(dp(36));
            solve.setTextColor(TEAL);
            solve.setBackground(rounded(AMBER_SOFT, 10, null));
            solve.setOnClickListener(v -> showMetabolicSolveDialog(spec.key));
            cell.addView(solve, margins(0, 4, 0, 2));
        }
        LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(-1, -2);
        cellParams.setMargins(dp(2), 0, dp(2), dp(12));
        parent.addView(cell, cellParams);
        edit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (initializing) return;
                preferences.edit().putString(spec.key, s.toString()).apply();
                calculate(false);
                syncNativeInputsToBioprocess();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void showMetabolicSolveDialog(String target) {
        String[] knownKeys = "qMet".equals(target) ? new String[]{"qTotal", "volume", "agitation", "phiD", "qOther"} :
                "volume".equals(target) ? new String[]{"qTotal", "qMet", "agitation", "phiD", "qOther"} :
                        new String[]{"qTotal", "qMet", "volume", "phiD", "qOther"};
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("qTotal", "Carga térmica total conocida (kW)");
        labels.put("qMet", "Calor metabólico específico (kW/m³)");
        labels.put("volume", "Volumen de caldo (m³)");
        labels.put("agitation", "Potencia disipada por agitación (kW)");
        labels.put("phiD", "Fracción de agitación como calor φd (0–1)");
        labels.put("qOther", "Otras cargas térmicas (kW)");
        String resultUnit = "qMet".equals(target) ? "kW/m³" : "volume".equals(target) ? "m³" : "kW";

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(4), dp(20), dp(8));
        TextView equation = text("Q̇_total = q̇_met · V + φd · P_agit + Q̇_otros\nIngresa la carga total conocida; los valores restantes se precargan y puedes editarlos.", 12, MUTED, false);
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
        result.setBackground(rounded(TEAL_SOFT, 12, null));
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
                    double phiD = dialogNumber(knownInputs.get("phiD"), "La fracción de calor de agitación");
                    double qOther = dialogNumber(knownInputs.get("qOther"), "Otras cargas térmicas");
                    solved[0] = MetabolicBalance.solve(target, qTotal, qMet, volume, agitation, phiD, qOther);
                    result.setText("Resultado: " + f(solved[0]) + " " + resultUnit);
                    result.setTextColor(INK);
                    apply.setEnabled(true);
                } catch (IllegalArgumentException error) {
                    solved[0] = Double.NaN;
                    result.setText("Revisa los datos: " + error.getMessage());
                    result.setTextColor(ERROR);
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
            for (Map.Entry<String, String> entry : bioprocessResults.entrySet()) {
                result.put("Bioproceso · " + entry.getKey(), entry.getValue());
            }
            renderResults(result);
        } catch (IllegalArgumentException ex) {
            resultValues.clear();
            statusText.setText(ex.getMessage());
        statusText.setTextColor(ERROR);
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
        if (get(v, "volume") <= 0 || get(v, "agitation") < 0 || get(v, "qOther") + get(v, "qMet") * get(v, "volume") + get(v, "phiD") * get(v, "agitation") <= 0) errors.add("El volumen debe ser positivo y la carga térmica neta mayor que cero.");
        if (get(v, "phiD") < 0 || get(v, "phiD") > 1) errors.add("La fracción φd debe estar entre 0 y 1.");
        for (String key : new String[]{"cp", "rho", "mu", "kf", "tubeD", "wallT", "wallK", "pitch", "tankD", "tankHeight", "coilD", "hBroth"}) if (get(v, key) <= 0) errors.add("Las propiedades, dimensiones y coeficiente externo deben ser positivos.");
        if (get(v, "coolantFlow") < 0 || get(v, "clearance") < 0 || get(v, "verticalClearance") < 0 || get(v, "fouling") < 0 || get(v, "foulingInside") < 0 || get(v, "localK") < 0 || get(v, "fDarcyCoil") < 0 || get(v, "pumpEfficiency") < 0 || get(v, "pumpEfficiency") > 1) errors.add("Caudal, holguras, pérdidas y eficiencias deben tener valores válidos.");
        if (di <= 0) errors.add("El espesor de pared deja un diámetro interior nulo o negativo.");
        if (get(v, "pitch") / 1000 < doM + get(v, "verticalClearance") / 1000) errors.add("El paso debe cubrir el diámetro del tubo más la holgura vertical indicada.");
        if (coilD <= doM) errors.add("El diámetro de hélice debe superar el diámetro exterior del tubo.");
        if (coilD + doM + 2 * clearance > tankD) errors.add("La hélice, el tubo y las holguras no caben dentro del diámetro del biorreactor.");
        double tankCapacity = Math.PI * tankD * tankD / 4 * tankH;
        if (get(v, "volume") > tankCapacity) errors.add("El volumen de caldo supera la capacidad cilíndrica aproximada del tanque.");
        if (!errors.isEmpty()) throw new IllegalArgumentException(errors.get(0));

        double qReaction = get(v, "qMet") * get(v, "volume");
        double qAgitation = get(v, "phiD") * get(v, "agitation");
        double qTotal = qReaction + qAgitation + get(v, "qOther");
        if (!(qTotal > 0)) throw new IllegalArgumentException("La carga térmica total debe ser mayor que cero.");
        double lm = lmtd(get(v, "tBroth") - get(v, "tOut"), get(v, "tBroth") - get(v, "tIn"));
        double mass = qTotal / (get(v, "cp") * (get(v, "tOut") - get(v, "tIn"))); // kW / [kJ/(kg K) K] = kg/s
        double mu = get(v, "mu") / 1000; // mPa·s -> Pa·s
        double cpJ = get(v, "cp") * 1000;
        double velocity = mass / (get(v, "rho") * Math.PI * di * di / 4);
        double re = 4 * mass / (Math.PI * di * mu);
        double pr = mu * cpJ / get(v, "kf");
        double nu = nusselt(re, pr);
        double hi = nu * get(v, "kf") / di;
        double u = 1 / (doM / (di * hi) + doM * Math.log(doM / di) / (2 * get(v, "wallK")) + 1 / get(v, "hBroth") + get(v, "fouling") + (doM / di) * get(v, "foulingInside"));
        double area = qTotal * 1000 / (u * lm);
        double length = area / (Math.PI * doM);
        double pitch = get(v, "pitch") / 1000;
        double turnLength = Math.sqrt(Math.pow(Math.PI * coilD, 2) + pitch * pitch);
        double turns = length / turnLength;
        int actualTurns = (int) Math.ceil(turns);
        double actualLength = actualTurns * turnLength;
        double heightCenters = (actualTurns - 1) * pitch;
        double height = heightCenters + doM;
        double installedArea = Math.PI * doM * actualLength;
        double thermalCapacity = u * installedArea * lm / 1000;
        double thermalSufficiency = thermalCapacity / qTotal;
        double tubeVolume = Math.PI * di * di / 4 * actualLength;
        double darcyStraight = darcyFactor(re);
        double darcy = get(v, "fDarcyCoil") > 0 ? get(v, "fDarcyCoil") : darcyStraight;
        double dynamicPressure = get(v, "rho") * velocity * velocity / 2;
        double frictionPressure = darcy * (actualLength / di) * dynamicPressure;
        double localPressure = get(v, "localK") * dynamicPressure;
        double elevationPressure = get(v, "rho") * 9.80665 * get(v, "elevation");
        double pressure = frictionPressure + localPressure + elevationPressure;
        double pumpPower = get(v, "pumpEfficiency") > 0
                ? Math.max(0, pressure * mass / get(v, "rho") / get(v, "pumpEfficiency") / 1000)
                : Double.NaN;
        double availableOutlet = get(v, "coolantFlow") > 0
                ? get(v, "tIn") + qTotal / (get(v, "coolantFlow") * get(v, "cp"))
                : Double.NaN;
        double availableCooling = get(v, "coolantFlow") * get(v, "cp") * (get(v, "tOut") - get(v, "tIn"));
        double flowRatio = get(v, "coolantFlow") / mass;
        for (double result : new double[]{lm, mass, velocity, re, pr, nu, hi, u, area, length, turns, height,
                actualLength, installedArea, thermalCapacity, thermalSufficiency, tubeVolume,
                darcy, frictionPressure, localPressure, flowRatio, availableCooling}) {
            if (!Double.isFinite(result) || result < 0) throw new IllegalArgumentException("El resultado no es finito o físicamente válido; revisa unidades y dimensiones.");
        }
        boolean gnielinskiRange = re >= 3000 && re <= 5e6 && pr >= 0.5 && pr <= 2000;
        boolean radialFit = coilD + doM + 2 * clearance <= tankD;
        double liquidHeight = get(v, "volume") / (Math.PI * tankD * tankD / 4);
        double assumedAxialClearance = doM;
        boolean heightFit = height + 2 * assumedAxialClearance <= liquidHeight;
        schematicResults.clear();
        schematicResults.put("coilDiameter", coilD);
        schematicResults.put("tubeDiameter", doM);
        schematicResults.put("tankDiameter", tankD);
        schematicResults.put("tankHeight", tankH);
        schematicResults.put("liquidHeight", liquidHeight);
        schematicResults.put("pitch", pitch);
        schematicResults.put("turns", (double) actualTurns);
        schematicResults.put("coilHeight", height);
        schematicResults.put("tubeLength", actualLength);
        schematicResults.put("brothTemperature", get(v, "tBroth"));
        schematicResults.put("inletTemperature", get(v, "tIn"));
        schematicResults.put("outletTemperature", get(v, "tOut"));
        schematicResults.put("flowLpm", mass / get(v, "rho") * 60000);
        schematicResults.put("overallU", u);
        Map<String, String> out = new LinkedHashMap<>();
        out.put("Carga metabólica", f(qReaction) + " kW");
        out.put("Calor de agitación φd·P", f(qAgitation) + " kW");
        out.put("Otras cargas térmicas", f(get(v, "qOther")) + " kW");
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
        out.put("Longitud requerida / instalada", f(length) + " / " + f(actualLength) + " m");
        out.put("Vueltas teóricas / reales", f(turns) + " / " + actualTurns);
        out.put("Altura entre centros / total", f(heightCenters) + " / " + f(height) + " m");
        out.put("Área instalada / capacidad térmica", f(installedArea) + " m² / " + f(thermalCapacity) + " kW");
        out.put("Suficiencia térmica instalada", f(thermalSufficiency) + " ×");
        out.put("Volumen interno del serpentín", f(tubeVolume) + " m³");
        out.put("Nivel de caldo (cilindro ideal)", f(liquidHeight) + " m");
        out.put("Capacidad cilíndrica aproximada", f(tankCapacity) + " m³");
        out.put("Holgura axial asumida", f(assumedAxialClearance * 1000) + " mm");
        out.put("Ajuste radial", radialFit ? "Cabe según cotas ingresadas" : "No cabe");
        out.put("Ajuste vertical", heightFit ? "Sumergido bajo el nivel estimado" : "No queda totalmente sumergido");
        out.put("ΔP fricción / total", f(frictionPressure / 1000) + " / " + f(pressure / 1000) + " kPa");
        out.put("ΔP local / elevación", f(localPressure / 1000) + " / " + f(elevationPressure / 1000) + " kPa");
        out.put("Factor Darcy usado", f(darcy) + (get(v, "fDarcyCoil") > 0 ? " ingresado para hélice" : " de tubo recto equivalente"));
        out.put("Potencia de bomba", Double.isNaN(pumpPower) ? "No calculada: falta η" : f(pumpPower) + " kW");
        out.put("Capacidad refrigerante disponible", f(availableCooling) + " kW");
        out.put("Salida refrigerante con caudal disponible", Double.isNaN(availableOutlet) ? "No calculada: caudal cero" : f(availableOutlet) + " °C");
        out.put("Estado del caudal", flowRatio >= 1 ? "Suficiente para el objetivo" : "Insuficiente para el objetivo");
        out.put("Rango de Gnielinski", gnielinskiRange ? "Dentro del intervalo orientativo" : "Fuera del intervalo; hᵢ más incierto");
        List<String> warnings = new ArrayList<>();
        if (re < 2300) warnings.add("Laminar: Nu = 3.66 es de tubo recto; no corrige el efecto Dean en la hélice.");
        if (re >= 2300 && re < 4000) warnings.add("Transición: el régimen y las interpolaciones dependen de la geometría helicoidal.");
        if ((re >= 2300 && re < 3000) || !gnielinskiRange) warnings.add("La correlación de Gnielinski está fuera de su intervalo orientativo o se está interpolando.");
        warnings.add("Nuᵢ usa correlaciones de tubo recto; Dean se informa, pero no corrige la transferencia helicoidal.");
        if (get(v, "fDarcyCoil") == 0) warnings.add("No se ingresó fD,coil: fricción estimada con factor de tubo recto; pérdidas locales y elevación dependen de ΣK y Δz.");
        warnings.add("El resultado hidráulico es preliminar y no sustituye una curva de sistema ni la selección de bomba.");
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
        flowWarningText.setBackground(rounded(caution ? AMBER_SOFT : TEAL_SOFT, 12, LINE));
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
            resultRows.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
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
                tile.setBackground(rounded(PAPER, 1, LINE));
                TextView label = text(shortLabels[tileIndex], 9, MUTED, true);
                label.setLetterSpacing(0.04f);
                TextView value = text(result.get(key), 18, INK, true);
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
        bioprocessResults.clear();
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
        layout.setPadding(dp(16), dp(16), dp(16), dp(16));
        layout.setBackground(rounded(PAPER, 8, LINE));
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
        button.setText(title); button.setTextSize(12); button.setTextColor(INK); button.setAllCaps(false);
        button.setTypeface(headlineTypeface);
        int surface = title.toLowerCase(Locale.ROOT).contains("restablecer") ? AMBER_SOFT : PAPER;
        button.setBackground(rounded(surface, 12, LINE));
        button.setMinHeight(dp(42));
        return button;
    }
    private GradientDrawable rounded(int color, int radius, Integer stroke) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(Math.min(radius, 12)));
        if (stroke != null) d.setStroke(dp(1), stroke);
        return d;
    }
    private Drawable projected(int color) {
        LayerDrawable projection = new LayerDrawable(new Drawable[]{
                new ColorDrawable(Color.rgb(41, 42, 45)), rounded(color, 2, LINE)
        });
        projection.setLayerInset(0, 0, dp(2), 0, 0);
        return projection;
    }
    private LinearLayout.LayoutParams margins(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(dp(left), dp(top), dp(right), dp(bottom)); return lp;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
