package com.soumyadeep.voicekeyboard;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

/** Setup steps and options for the keyboard. */
public class SettingsActivity extends Activity {

    static final String PREFS = "vk";
    static final String K_SPEAK = "speak";
    static final String K_WRITE = "write";
    static final String K_AUTO = "auto";
    static final String K_KEY = "key";
    static final String K_MODEL = "model";

    private static final String[] SPEAK_CODES = {"", "en-IN", "en-US", "en-GB", "bn-IN", "hi-IN", "ur-IN", "ta-IN",
            "te-IN", "mr-IN", "gu-IN", "kn-IN", "ml-IN", "pa-IN", "es-ES", "fr-FR", "de-DE", "pt-BR", "ar-SA",
            "zh-CN", "ja-JP", "ko-KR", "ru-RU", "id-ID"};
    private static final String[] SPEAK_LABELS = {"My phone's language", "English (India)", "English (US)",
            "English (UK)", "Bengali", "Hindi", "Urdu", "Tamil", "Telugu", "Marathi", "Gujarati", "Kannada",
            "Malayalam", "Punjabi", "Spanish", "French", "German", "Portuguese", "Arabic", "Chinese", "Japanese",
            "Korean", "Russian", "Indonesian"};
    private static final String[] WRITE_VALUES = {"", "English", "Bengali", "Hindi", "Urdu", "Tamil", "Telugu",
            "Marathi", "Gujarati", "Punjabi", "Spanish", "French", "German", "Portuguese", "Arabic", "Chinese",
            "Japanese", "Korean", "Russian", "Indonesian"};
    private static final String[] WRITE_LABELS = {"The same language I speak", "English", "Bengali", "Hindi", "Urdu",
            "Tamil", "Telugu", "Marathi", "Gujarati", "Punjabi", "Spanish", "French", "German", "Portuguese",
            "Arabic", "Chinese", "Japanese", "Korean", "Russian", "Indonesian"};

    private Spinner speak;
    private Spinner write;
    private CheckBox auto;
    private EditText key;
    private EditText model;
    private TextView micState;

    private int dp(float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()));
    }

    private TextView text(LinearLayout parent, String s, float sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        if (bold) {
            t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(12);
        parent.addView(t, lp);
        return t;
    }

    private Button button(LinearLayout parent, String s, View.OnClickListener onClick) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setOnClickListener(onClick);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(6);
        parent.addView(b, lp);
        return b;
    }

    private Spinner spinner(LinearLayout parent, String[] labels) {
        Spinner s = new Spinner(this);
        ArrayAdapter<String> a = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, labels);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(a);
        parent.addView(s, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return s;
    }

    private static int indexOf(String[] values, String v) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(v)) {
                return i;
            }
        }
        return 0;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Voice Keyboard");
        SharedPreferences p = getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(12), dp(18), dp(24));
        scroll.addView(root);

        text(root, "Set up (do these once)", 18, true);
        button(root, "1. Allow the microphone", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
            }
        });
        micState = text(root, "", 13, false);
        button(root, "2. Turn on Voice Keyboard in phone settings", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
            }
        });
        button(root, "3. Switch to Voice Keyboard", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showInputMethodPicker();
                }
            }
        });

        text(root, "Try it here", 18, true);
        EditText tryBox = new EditText(this);
        tryBox.setHint("Tap here, then tap the red key and speak");
        tryBox.setMinLines(3);
        tryBox.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        root.addView(tryBox, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        text(root, "Options", 18, true);
        text(root, "I speak", 14, false);
        speak = spinner(root, SPEAK_LABELS);
        speak.setSelection(indexOf(SPEAK_CODES, p.getString(K_SPEAK, "")));
        text(root, "Write in", 14, false);
        write = spinner(root, WRITE_LABELS);
        write.setSelection(indexOf(WRITE_VALUES, p.getString(K_WRITE, "")));
        auto = new CheckBox(this);
        auto.setText("Fix my words when I stop speaking");
        auto.setChecked(p.getBoolean(K_AUTO, true));
        root.addView(auto);

        text(root, "Full fixing (optional)", 18, true);
        text(root, "Without a key, fixing only removes fillers like \"um\" and repeated words. With an Anthropic API "
                + "key it rewrites unclear speech properly in any language, and can translate into the language "
                + "chosen under Write in. The key is stored only on this phone and sent only to api.anthropic.com.",
                13, false);
        text(root, "Anthropic API key", 14, false);
        key = new EditText(this);
        key.setHint("sk-ant-…");
        key.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        key.setText(p.getString(K_KEY, ""));
        root.addView(key, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        text(root, "Model", 14, false);
        model = new EditText(this);
        model.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        model.setText(p.getString(K_MODEL, Claude.DEFAULT_MODEL));
        root.addView(model, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        text(root, "Options are saved when you leave this screen.", 13, false);
        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean ok = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        micState.setText(ok ? "Microphone is allowed." : "Microphone is not allowed yet.");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean ok = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        micState.setText(ok ? "Microphone is allowed."
                : "Microphone was refused. Allow it in phone settings, under Apps, Voice Keyboard, Permissions.");
    }

    @Override
    protected void onPause() {
        super.onPause();
        String m = model.getText().toString().trim();
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(K_SPEAK, SPEAK_CODES[speak.getSelectedItemPosition()])
                .putString(K_WRITE, WRITE_VALUES[write.getSelectedItemPosition()])
                .putBoolean(K_AUTO, auto.isChecked())
                .putString(K_KEY, key.getText().toString().trim())
                .putString(K_MODEL, m.isEmpty() ? Claude.DEFAULT_MODEL : m)
                .apply();
    }
}
