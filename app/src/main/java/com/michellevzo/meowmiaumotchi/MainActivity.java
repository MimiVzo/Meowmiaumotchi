package com.michellevzo.meowmiaumotchi;

import android.animation.ObjectAnimator;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS           = "meowprefs";
    private static final String CHANNEL_ID      = "meow_channel";
    private static final int    NOTIF_HAMBRE    = 1;
    private static final int    NOTIF_FELICIDAD = 2;
    private static final int    NOTIF_ENERGIA   = 3;

    // Vistas principales
    private ImageView   ivGato;
    private TextView    tvEstado, tvDias, tvMensaje;
    private TextView    tvHambreNum, tvFelicidadNum, tvEnergiaNum;
    private ProgressBar pbHambre, pbFelicidad, pbEnergia;
    private View        layoutPrincipal;

    // Bienvenida
    private LinearLayout layoutBienvenida;
    private EditText     etNombreGato;

    // Botón dormir/despertar
    private LinearLayout btnDormir;
    private TextView     tvBtnDormir, emojiBtnDormir;

    // Overlay acción
    private FrameLayout overlayAccion;
    private ImageView   ivAccionGato;
    private TextView    tvAccionTitulo, tvAccionDesc;

    // Pantalla muerte
    private LinearLayout layoutMuerto;
    private TextView     tvDiasMuerto;

    // Lógica
    private Tamagotchi tamagotchi;
    private Handler    handler         = new Handler();
    private Random     random          = new Random();
    private int        tickCount       = 0;
    private boolean    mostrandoAccion = false;
    private String     nombreGato      = "Gatito";

    // Imágenes estado
    private final int[] imgFeliz  = {R.drawable.cat_happy1, R.drawable.cat_happy2, R.drawable.cat_happy3};
    private final int[] imgNormal = {R.drawable.cat_normal1, R.drawable.cat_normal2, R.drawable.cat_normal3};
    private final int[] imgTriste = {R.drawable.cat_sad1, R.drawable.cat_sad2, R.drawable.cat_sad3};

    // Imágenes acciones
    private final int[] imgDormido  = {R.drawable.cat_sleep1, R.drawable.cat_sleep2};
    private final int[] imgComiendo = {R.drawable.cat_eat1,   R.drawable.cat_eat2};
    private final int[] imgJugando  = {R.drawable.cat_play1,  R.drawable.cat_play2};

    // Frases
    private final String[] frasesFelix  = {
            "😸 ¡%s está súper feliz~!", "✨ ¡Nyaa! ¡Todo está genial!", "🌸 %s te ama mucho~"
    };
    private final String[] frasesNormal = {
            "😊 %s está bien~", "🐱 Miau... todo tranquilo", "☁️ %s descansa plácido"
    };
    private final String[] frasesTriste = {
            "😿 %s necesita atención...", "💧 Miau... tiene hambre...", "🌧️ %s está tristón..."
    };

    // Game loop cada 8 segundos
    private final Runnable gameLoop = new Runnable() {
        @Override
        public void run() {
            tamagotchi.pasarTiempo();
            tickCount++;
            if (tickCount % 10 == 0) tamagotchi.sumarDia();
            verificarNotificaciones();
            if (!mostrandoAccion) actualizarUI(null);
            if (tamagotchi.isVivo()) {
                handler.postDelayed(this, 8000);
            } else {
                mostrarPantallaMuerte();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        crearCanalNotificaciones();
        pedirPermisoNotificaciones();

        // Vincular vistas
        ivGato           = findViewById(R.id.ivGato);
        tvEstado         = findViewById(R.id.tvEstado);
        tvDias           = findViewById(R.id.tvDias);
        tvMensaje        = findViewById(R.id.tvMensaje);
        tvHambreNum      = findViewById(R.id.tvHambreNum);
        tvFelicidadNum   = findViewById(R.id.tvFelicidadNum);
        tvEnergiaNum     = findViewById(R.id.tvEnergiaNum);
        pbHambre         = findViewById(R.id.pbHambre);
        pbFelicidad      = findViewById(R.id.pbFelicidad);
        pbEnergia        = findViewById(R.id.pbEnergia);
        layoutPrincipal  = findViewById(R.id.layoutPrincipal);
        layoutBienvenida = findViewById(R.id.layoutBienvenida);
        etNombreGato     = findViewById(R.id.etNombreGato);
        btnDormir        = findViewById(R.id.btnDormir);
        tvBtnDormir      = findViewById(R.id.tvBtnDormir);
        emojiBtnDormir   = findViewById(R.id.emojiBtnDormir);
        overlayAccion    = findViewById(R.id.overlayAccion);
        ivAccionGato     = findViewById(R.id.ivAccionGato);
        tvAccionTitulo   = findViewById(R.id.tvAccionTitulo);
        tvAccionDesc     = findViewById(R.id.tvAccionDesc);
        layoutMuerto     = findViewById(R.id.layoutMuerto);
        tvDiasMuerto     = findViewById(R.id.tvDiasMuerto);

        limpiarSiReinstalacion();

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (!prefs.contains("timestamp")) {
            mostrarBienvenida();
        } else {
            nombreGato = prefs.getString("nombre", "Gatito");
            cargarEstado();
            iniciarJuego();
        }
    }

    private void limpiarSiReinstalacion() {
        try {
            long installTime = getPackageManager()
                    .getPackageInfo(getPackageName(), 0)
                    .firstInstallTime;

            SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
            long saveTimestamp = prefs.getLong("timestamp", -1);

            // Si hay un save guardado pero es ANTERIOR a la instalación actual,
            // significa que la app fue reinstalada → limpiar el save
            if (saveTimestamp != -1 && saveTimestamp < installTime) {
                prefs.edit().clear().apply();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void mostrarBienvenida() {
        layoutBienvenida.setVisibility(View.VISIBLE);
        layoutPrincipal.setVisibility(View.GONE);
        layoutMuerto.setVisibility(View.GONE);
        overlayAccion.setVisibility(View.GONE);

        findViewById(R.id.btnAdoptar).setOnClickListener(v -> {
            String nombre = etNombreGato.getText().toString().trim();
            if (nombre.isEmpty()) nombre = "Gatito";
            nombreGato = nombre;
            getSharedPreferences(PREFS, MODE_PRIVATE)
                    .edit()
                    .putString("nombre", nombreGato)
                    .apply();
            layoutBienvenida.setVisibility(View.GONE);
            tamagotchi = new Tamagotchi();
            iniciarJuego();
        });
    }

    private void crearCanalNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    CHANNEL_ID, "Meowmaumotchi",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            canal.setDescription("Notificaciones de tu gatito virtual");
            getSystemService(NotificationManager.class).createNotificationChannel(canal);
        }
    }

    private void pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }
    }

    private void reproducirSonido(int recurso) {
        try {
            MediaPlayer mp = MediaPlayer.create(this, recurso);
            if (mp != null) {
                mp.setOnCompletionListener(MediaPlayer::release);
                mp.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void enviarNotificacion(int id, String titulo, String mensaje) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        NotificationManager manager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            manager.notify(id, builder.build());
        }
    }

    private void verificarNotificaciones() {
        if (!tamagotchi.isVivo()) return;
        if (tamagotchi.getHambrePorc() < 30)
            enviarNotificacion(NOTIF_HAMBRE,
                    "🍡 ¡" + nombreGato + " tiene hambre!",
                    "Su pancita está casi vacía~ ¡Dale de comer!");
        if (tamagotchi.getFelicidad() < 30)
            enviarNotificacion(NOTIF_FELICIDAD,
                    "💖 ¡" + nombreGato + " está triste!",
                    "Necesita que juegues con él~ 🎀");
        if (tamagotchi.getEnergia() < 30)
            enviarNotificacion(NOTIF_ENERGIA,
                    "⚡ ¡" + nombreGato + " está agotado!",
                    "Ponlo a dormir para que recupere energía~ 🌙");
    }

    private void iniciarJuego() {
        mostrandoAccion = false;
        tickCount       = 0;

        layoutMuerto.setVisibility(View.GONE);
        overlayAccion.setVisibility(View.GONE);
        layoutBienvenida.setVisibility(View.GONE);
        layoutPrincipal.setVisibility(View.VISIBLE);

        findViewById(R.id.btnComer).setOnClickListener(v -> {
            animarBoton(v);
            reproducirSonido(R.raw.sonido_comer);
            String msg = tamagotchi.comer();
            if (msg.contains("dormido")) mostrarMensaje(msg);
            else mostrarAnimacionAccion(Tamagotchi.Accion.COMIENDO, msg);
        });

        findViewById(R.id.btnJugar).setOnClickListener(v -> {
            animarBoton(v);
            reproducirSonido(R.raw.sonido_jugar);
            String msg = tamagotchi.jugar();
            if (msg.contains("dormido") || msg.contains("cansado")) mostrarMensaje(msg);
            else mostrarAnimacionAccion(Tamagotchi.Accion.JUGANDO, msg);
        });

        btnDormir.setOnClickListener(v -> {
            animarBoton(v);
            if (tamagotchi.isDurmiendo()) {
                reproducirSonido(R.raw.sonido_despertar);
                String msg = tamagotchi.despertar();
                actualizarBtnDormir();
                mostrarAnimacionAccion(Tamagotchi.Accion.NINGUNA, msg);
            } else {
                reproducirSonido(R.raw.sonido_dormir);
                String msg = tamagotchi.dormir();
                actualizarBtnDormir();
                mostrarAnimacionAccion(Tamagotchi.Accion.DURMIENDO, msg);
            }
        });

        findViewById(R.id.btnReiniciar).setOnClickListener(v -> {
            handler.removeCallbacks(gameLoop);
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().clear().apply();
            tamagotchi = null;
            mostrarBienvenida();
        });

        if (tamagotchi == null) tamagotchi = new Tamagotchi();

        if (!tamagotchi.isVivo()) {
            mostrarPantallaMuerte();
            return;
        }

        handler.removeCallbacks(gameLoop);
        handler.postDelayed(gameLoop, 8000);
        actualizarUI(null);
        actualizarBtnDormir();
    }

    private void actualizarBtnDormir() {
        if (tamagotchi.isDurmiendo()) {
            emojiBtnDormir.setText("☀️");
            tvBtnDormir.setText("Despertar");
        } else {
            emojiBtnDormir.setText("🌙");
            tvBtnDormir.setText("Dormir");
        }
    }

    private void guardarEstado() {
        SharedPreferences.Editor ed = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
        ed.putInt("hambre",        tamagotchi.getHambreRaw());
        ed.putInt("felicidad",     tamagotchi.getFelicidad());
        ed.putInt("energia",       tamagotchi.getEnergia());
        ed.putInt("dias",          tamagotchi.getDias());
        ed.putBoolean("vivo",      tamagotchi.isVivo());
        ed.putBoolean("durmiendo", tamagotchi.isDurmiendo());
        ed.putString("nombre",     nombreGato);
        ed.putLong("timestamp",    System.currentTimeMillis());
        ed.apply();
    }

    private void cargarEstado() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        tamagotchi = new Tamagotchi();

        if (!prefs.contains("timestamp")) {
            tamagotchi.reiniciar();
            return;
        }

        tamagotchi.setHambre(prefs.getInt("hambre", 20));
        tamagotchi.setFelicidad(prefs.getInt("felicidad", 80));
        tamagotchi.setEnergia(prefs.getInt("energia", 80));
        tamagotchi.setDias(prefs.getInt("dias", 1));
        tamagotchi.setVivo(prefs.getBoolean("vivo", true));
        nombreGato = prefs.getString("nombre", "Gatito");

        if (!tamagotchi.isVivo()) return;

        long ahora    = System.currentTimeMillis();
        long antes    = prefs.getLong("timestamp", ahora);
        long segundos = (ahora - antes) / 1000;

        boolean estabaDurmiendo = prefs.getBoolean("durmiendo", false);
        tamagotchi.setDurmiendo(segundos < 5 ? false : estabaDurmiendo);

        if (segundos > 5) tamagotchi.aplicarTiempoOffline(segundos);

        if (!tamagotchi.isVivo() && tamagotchi.getHambrePorc() > 10)
            tamagotchi.reiniciar();
    }

    private void mostrarAnimacionAccion(Tamagotchi.Accion accion, String mensaje) {
        if (accion == Tamagotchi.Accion.NINGUNA) {
            mostrarMensaje(mensaje);
            actualizarUI(null);
            return;
        }
        mostrandoAccion = true;
        overlayAccion.setVisibility(View.VISIBLE);

        switch (accion) {
            case COMIENDO:
                ivAccionGato.setImageResource(imgComiendo[random.nextInt(2)]);
                tvAccionTitulo.setText("¡Comiendo! 🍡");
                break;
            case JUGANDO:
                ivAccionGato.setImageResource(imgJugando[random.nextInt(2)]);
                tvAccionTitulo.setText("¡Jugando! 🎀");
                break;
            case DURMIENDO:
                ivAccionGato.setImageResource(imgDormido[random.nextInt(2)]);
                tvAccionTitulo.setText("Durmiendo... 🌙");
                break;
        }
        tvAccionDesc.setText(mensaje);

        Animation bounce = AnimationUtils.loadAnimation(this, R.anim.bounce);
        ivAccionGato.startAnimation(bounce);

        handler.postDelayed(() -> {
            overlayAccion.setVisibility(View.GONE);
            mostrandoAccion = false;
            actualizarUI(null);
        }, 2500);
    }

    private void mostrarMensaje(String msg) {
        tvMensaje.setText(msg);
        handler.postDelayed(() -> tvMensaje.setText(""), 3000);
    }

    private void mostrarPantallaMuerte() {
        layoutMuerto.setVisibility(View.VISIBLE);
        layoutPrincipal.setVisibility(View.GONE);
        tvDiasMuerto.setText(nombreGato + " vivió " + tamagotchi.getDias() + " día(s) de aventuras 🌈");
        Animation bounce = AnimationUtils.loadAnimation(this, R.anim.bounce);
        findViewById(R.id.ivGatoMuerto).startAnimation(bounce);
    }

    private void actualizarUI(String mensaje) {
        animarBarra(pbHambre,    tamagotchi.getHambrePorc());
        animarBarra(pbFelicidad, tamagotchi.getFelicidad());
        animarBarra(pbEnergia,   tamagotchi.getEnergia());

        tvHambreNum.setText(tamagotchi.getHambrePorc() + "%");
        tvFelicidadNum.setText(tamagotchi.getFelicidad() + "%");
        tvEnergiaNum.setText(tamagotchi.getEnergia() + "%");
        tvDias.setText("⭐ " + nombreGato + " — Día " + tamagotchi.getDias());

        if (tamagotchi.isDurmiendo()) {
            ivGato.setImageResource(imgDormido[random.nextInt(2)]);
            tvEstado.setText("😴 " + nombreGato + " está durmiendo... Zzz");
            return;
        }

        Tamagotchi.Estado estado = tamagotchi.getEstado();
        String[] frases;
        switch (estado) {
            case FELIZ:
                ivGato.setImageResource(imgFeliz[random.nextInt(3)]);
                frases = frasesFelix;
                break;
            case NORMAL:
                ivGato.setImageResource(imgNormal[random.nextInt(3)]);
                frases = frasesNormal;
                break;
            case TRISTE:
                ivGato.setImageResource(imgTriste[random.nextInt(3)]);
                frases = frasesTriste;
                break;
            default:
                ivGato.setImageResource(R.drawable.cat_dead);
                tvEstado.setText("💀 " + nombreGato + " se fue al cielo gatuno...");
                return;
        }
        tvEstado.setText(String.format(frases[random.nextInt(frases.length)], nombreGato));

        if (mensaje != null) mostrarMensaje(mensaje);

        Animation bounce = AnimationUtils.loadAnimation(this, R.anim.bounce);
        ivGato.startAnimation(bounce);
    }

    private void animarBarra(ProgressBar pb, int valor) {
        ObjectAnimator.ofInt(pb, "progress", pb.getProgress(), valor)
                .setDuration(500).start();
    }

    private void animarBoton(View v) {
        Animation bounce = AnimationUtils.loadAnimation(this, R.anim.bounce);
        v.startAnimation(bounce);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (tamagotchi != null) guardarEstado();
        handler.removeCallbacks(gameLoop);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tamagotchi != null && tamagotchi.isVivo()) {
            handler.removeCallbacks(gameLoop);
            handler.postDelayed(gameLoop, 8000);
            actualizarUI(null);
            actualizarBtnDormir();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(gameLoop);
    }
}