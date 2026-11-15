package com.soumyadeep.voicekeyboard;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.inputmethodservice.InputMethodService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** A voice-first keyboard: listens, shows the words as you speak, then fixes unclear speech. */
public class VoiceIme extends InputMethodService implements RecognitionListener {

    private static final int RED = Color.rgb(216, 67, 46);
    private static final int DARK = Color.rgb(28, 35, 48);
    private static final int DECK = Color.rgb(207, 213, 222);
    private static final int KEY = Color.rgb(248, 249, 251);

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final StringBuilder session = new StringBuilder();
    private String partial = "";
    private SpeechRecognizer recognizer;
    private boolean listening;
    private boolean fixing;
    private int inputToken;
    private TextView status;
    private Button mic;

    private SharedPreferences prefs() {
        return getSharedPreferences(SettingsActivity.PREFS, Context.MODE_PRIVATE);
    }

    private int dp(float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()));
    }

    private GradientDrawable box(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(10));
        return g;
    }

    private Button key(String label, float weight, View.OnClickListener onClick) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(DARK);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        b.setBackground(box(KEY));
        b.setPadding(0, 0, 0, 0);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(48), weight);
        lp.setMargins(dp(3), dp(3), dp(3), dp(3));
        b.setLayoutParams(lp);
        b.setOnClickListener(onClick);
        return b;
    }

    @Override
    public View onCreateInputView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(DECK);
        root.setPadding(dp(6), dp(6), dp(6), dp(8));

        status = new TextView(this);
        status.setTextColor(DARK);
        status.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        status.setGravity(Gravity.CENTER);
        status.setPadding(dp(8), dp(4), dp(8), dp(4));
        status.setText("Tap the red key and speak");
        root.addView(status, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        mic = new Button(this);
        mic.setAllCaps(false);
        mic.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(72));
        mlp.setMargins(dp(3), dp(3), dp(3), dp(3));
        root.addView(mic, mlp);
        mic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listening) {
                    stopListening(true);
                } else {
                    startListening();
                }
            }
        });
        paintMic();

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(key("Fix", 1.4f, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fixBeforeCursor();
            }
        }));
        row.addView(key(",", 1f, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                type(",");
            }
        }));
        row.addView(key(".", 1f, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                type(".");
            }
        }));
        row.addView(key("?", 1f, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                type("?");
            }
        }));
        row.addView(key("Space", 2f, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                type(" ");
            }
        }));
        row.addView(key("⌫", 1.3f, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endComposing();
                sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
            }
        }));
        row.addView(key("Enter", 1.5f, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endComposing();
                sendKeyChar('\n');
            }
        }));
        root.addView(row, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.addView(key("Other keyboard", 1f, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showInputMethodPicker();
                }
            }
        }));
        row2.addView(key("Settings", 1f, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSettings();
            }
        }));
        root.addView(row2, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return root;
    }

    private void openSettings() {
        Intent i = new Intent(this, SettingsActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    private void paintMic() {
        if (mic == null) {
            return;
        }
        mic.setText(listening ? "Stop listening" : "Start listening");
        mic.setTextColor(Color.WHITE);
        mic.setBackground(box(listening ? DARK : RED));
    }

    private void say(String text) {
        if (status != null) {
            status.setText(text);
        }
    }

    private String full() {
        if (partial.isEmpty()) {
            return session.toString();
        }
        return session.length() > 0 ? session + " " + partial : partial;
    }

    private void showComposing() {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.setComposingText(full(), 1);
        }
    }

    /** Keep what was dictated so far as ordinary text, so a typed key does not replace it. */
    private void endComposing() {
        if (session.length() > 0 || !partial.isEmpty()) {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.finishComposingText();
            }
            session.setLength(0);
            partial = "";
        }
    }

    private void type(String s) {
        endComposing();
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.commitText(s, 1);
        }
    }

    // ---------- listening ----------

    private void startListening() {
        if (fixing) {
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            say("Allow the microphone in the Voice Keyboard app first");
            openSettings();
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            say("Speech recognition is missing. Install or enable the Google app.");
            return;
        }
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this);
            recognizer.setRecognitionListener(this);
        }
        session.setLength(0);
        partial = "";
        listening = true;
        paintMic();
        say("Listening. Speak now, take your time.");
        listenOnce();
    }

    private void listenOnce() {
        if (!listening || recognizer == null) {
            return;
        }
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        i.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        String lang = prefs().getString(SettingsActivity.K_SPEAK, "");
        if (lang != null && !lang.isEmpty()) {
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang);
        }
        try {
            recognizer.startListening(i);
        } catch (RuntimeException e) {
            stopListening(false);
            say("Listening could not start. Tap the red key again.");
        }
    }

    private void restartSoon(long delayMs) {
        main.postDelayed(new Runnable() {
            @Override
            public void run() {
                listenOnce();
            }
        }, delayMs);
    }

    private void stopListening(boolean thenFix) {
        listening = false;
        paintMic();
        if (recognizer != null) {
            recognizer.cancel();
        }
        String text = full().trim();
        session.setLength(0);
        partial = "";
        if (text.isEmpty()) {
            say("Tap the red key and speak");
            return;
        }
        boolean auto = prefs().getBoolean(SettingsActivity.K_AUTO, true);
        if (thenFix && auto) {
            fixComposing(text);
        } else {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.commitText(text + " ", 1);
            }
            say("Typed. Tap the red key to speak again.");
        }
    }

    private static String first(Bundle b) {
        if (b == null) {
            return "";
        }
        ArrayList<String> list = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (list == null || list.isEmpty() || list.get(0) == null) {
            return "";
        }
        return list.get(0).trim();
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        if (!listening) {
            return;
        }
        partial = first(partialResults);
        showComposing();
    }

    @Override
    public void onResults(Bundle results) {
        if (!listening) {
            return;
        }
        String text = first(results);
        partial = "";
        if (!text.isEmpty()) {
            if (session.length() > 0) {
                session.append(' ');
            }
            session.append(text);
        }
        showComposing();
        restartSoon(50);
    }

    @Override
    public void onError(int error) {
        if (!listening) {
            return;
        }
        if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
            restartSoon(100);
        } else if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_CLIENT) {
            restartSoon(400);
        } else if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
            stopListening(false);
            say("Allow the microphone in the Voice Keyboard app first");
        } else if (error == SpeechRecognizer.ERROR_NETWORK || error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT
                || error == SpeechRecognizer.ERROR_SERVER) {
            stopListening(false);
            say("Listening needs an internet connection for this language");
        } else {
            stopListening(false);
            say("Listening stopped (error " + error + "). Tap the red key again.");
        }
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
    }

    @Override
    public void onBeginningOfSpeech() {
    }

    @Override
    public void onRmsChanged(float rmsdB) {
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
    }

    @Override
    public void onEndOfSpeech() {
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
    }

    // ---------- fixing ----------

    private interface Done {
        void run(String fixed, boolean smart);
    }

    private void fixInBackground(final String raw, final Done done) {
        final String key = prefs().getString(SettingsActivity.K_KEY, "");
        final String model = prefs().getString(SettingsActivity.K_MODEL, Claude.DEFAULT_MODEL);
        final String target = prefs().getString(SettingsActivity.K_WRITE, "");
        fixing = true;
        say("Fixing your words…");
        io.execute(new Runnable() {
            @Override
            public void run() {
                String out;
                boolean smart = false;
                if (key == null || key.trim().isEmpty()) {
                    out = Cleaner.basic(raw);
                } else {
                    try {
                        out = Claude.fix(key.trim(), model, raw, target);
                        smart = true;
                    } catch (Exception e) {
                        out = Cleaner.basic(raw);
                    }
                }
                final String result = (out == null || out.trim().isEmpty()) ? raw : out.trim();
                final boolean wasSmart = smart;
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        fixing = false;
                        done.run(result, wasSmart);
                    }
                });
            }
        });
    }

    private String doneMessage(boolean smart) {
        String key = prefs().getString(SettingsActivity.K_KEY, "");
        if (smart) {
            return "Fixed. Tap the red key to speak again.";
        }
        if (key == null || key.trim().isEmpty()) {
            return "Basic cleanup done. Add an API key in Settings for full fixing.";
        }
        return "Could not reach the fixer, so a basic cleanup was done.";
    }

    /** Replace the dictated (still underlined) text with its fixed version. */
    private void fixComposing(final String raw) {
        final int token = inputToken;
        fixInBackground(raw, new Done() {
            @Override
            public void run(String fixed, boolean smart) {
                if (token != inputToken) {
                    return;
                }
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) {
                    ic.commitText(fixed + " ", 1);
                }
                say(doneMessage(smart));
            }
        });
    }

    /** The Fix key: repair the text already in the box, just before the cursor. */
    private void fixBeforeCursor() {
        if (fixing) {
            return;
        }
        if (listening) {
            stopListening(true);
            return;
        }
        final InputConnection ic0 = getCurrentInputConnection();
        if (ic0 == null) {
            return;
        }
        CharSequence before = ic0.getTextBeforeCursor(2000, 0);
        if (before == null || before.toString().trim().isEmpty()) {
            say("Nothing to fix yet");
            return;
        }
        final String original = before.toString();
        final int token = inputToken;
        fixInBackground(original.trim(), new Done() {
            @Override
            public void run(String fixed, boolean smart) {
                if (token != inputToken) {
                    return;
                }
                InputConnection ic = getCurrentInputConnection();
                if (ic == null) {
                    return;
                }
                CharSequence now = ic.getTextBeforeCursor(2000, 0);
                if (now == null || !original.contentEquals(now)) {
                    say("The text changed, so it was left as it is");
                    return;
                }
                ic.beginBatchEdit();
                ic.deleteSurroundingText(original.length(), 0);
                ic.commitText(fixed, 1);
                ic.endBatchEdit();
                say(doneMessage(smart));
            }
        });
    }

    // ---------- lifecycle ----------

    private void reset() {
        inputToken++;
        if (listening) {
            listening = false;
            if (recognizer != null) {
                recognizer.cancel();
            }
        }
        session.setLength(0);
        partial = "";
        paintMic();
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        if (!restarting) {
            reset();
        }
    }

    @Override
    public void onFinishInputView(boolean finishingInput) {
        super.onFinishInputView(finishingInput);
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.finishComposingText();
        }
        reset();
    }

    @Override
    public void onDestroy() {
        if (recognizer != null) {
            recognizer.destroy();
            recognizer = null;
        }
        io.shutdownNow();
        super.onDestroy();
    }
}
