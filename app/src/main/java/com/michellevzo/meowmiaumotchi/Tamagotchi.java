package com.michellevzo.meowmiaumotchi;

public class Tamagotchi {

    public enum Estado { FELIZ, NORMAL, TRISTE, MUERTO }
    public enum Accion { NINGUNA, COMIENDO, JUGANDO, DURMIENDO }

    private int hambre;
    private int felicidad;
    private int energia;
    private boolean vivo;
    private int dias;
    private Accion accionActual;
    private boolean durmiendo;

    public Tamagotchi() {
        reiniciar();
    }

    public void reiniciar() {
        hambre       = 20;
        felicidad    = 80;
        energia      = 80;
        vivo         = true;
        dias         = 1;
        accionActual = Accion.NINGUNA;
        durmiendo    = false;
    }

    public void pasarTiempo() {
        if (!vivo) return;
        if (durmiendo) {
            // Si está durmiendo solo sube energía, no bajan stats
            energia = Math.min(100, energia + 2);
            return;
        }
        hambre    = Math.min(100, hambre + 3);
        felicidad = Math.max(0,   felicidad - 4);
        energia   = Math.max(0,   energia - 3);
        accionActual = Accion.NINGUNA;

        boolean sinComida    = hambre >= 100;
        boolean sinFelicidad = felicidad <= 0;
        boolean sinEnergia   = energia <= 0;

        if (sinComida && sinFelicidad && sinEnergia) {
            vivo = false;
        }
    }

    // Aplica ticks offline según segundos transcurridos
    public void aplicarTiempoOffline(long segundos) {
        if (!vivo) return;
        // Un tick cada 8 segundos
        int ticks = (int)(segundos / 6);
        for (int i = 0; i < ticks; i++) {
            pasarTiempo();
            if (!vivo) break;
        }
    }

    public void sumarDia() {
        if (vivo) dias++;
    }

    public String comer() {
        if (!vivo) return "Tu gatito ya no está... 💔";
        if (durmiendo) return "😴 Tu gatito está dormido...";
        accionActual = Accion.COMIENDO;
        hambre    = Math.max(0,   hambre - 35);
        felicidad = Math.min(100, felicidad + 5);
        energia   = Math.min(100, energia + 5);
        return "🍡 ¡Ñam ñam! Tu gatito comió feliz~";
    }

    public String jugar() {
        if (!vivo) return "Tu gatito ya no está... 💔";
        if (durmiendo) return "😴 Tu gatito está dormido...";
        if (energia < 15) return "😴 Tu gatito está muy cansado para jugar...";
        accionActual = Accion.JUGANDO;
        felicidad = Math.min(100, felicidad + 25);
        hambre    = Math.min(100, hambre + 12);
        energia   = Math.max(0,   energia - 20);
        return "🎀 ¡Yay! ¡Tu gatito jugó contigo!";
    }

    public String dormir() {
        if (!vivo) return "Tu gatito ya no está... 💔";
        if (durmiendo) return "😴 Ya está durmiendo~";
        accionActual = Accion.DURMIENDO;
        durmiendo = true;
        return "🌙 Tu gatito se fue a dormir~";
    }

    public String despertar() {
        if (!vivo) return "Tu gatito ya no está... 💔";
        if (!durmiendo) return "😊 Tu gatito ya está despierto~";
        durmiendo = false;
        accionActual = Accion.NINGUNA;
        return "☀️ ¡Buenos días! Tu gatito despertó~";
    }

    public Estado getEstado() {
        if (!vivo) return Estado.MUERTO;
        int score = (100 - hambre) + felicidad + energia;
        if (score > 210) return Estado.FELIZ;
        if (score > 130) return Estado.NORMAL;
        return Estado.TRISTE;
    }

    public Accion getAccionActual()  { return accionActual; }
    public int getHambrePorc()       { return 100 - hambre; }
    public int getFelicidad()        { return felicidad; }
    public int getEnergia()          { return energia; }
    public boolean isVivo()          { return vivo; }
    public boolean isDurmiendo()     { return durmiendo; }
    public int getDias()             { return dias; }

    // Getters/Setters para guardar estado
    public int getHambreRaw()        { return hambre; }
    public void setHambre(int h)     { hambre = h; }
    public void setFelicidad(int f)  { felicidad = f; }
    public void setEnergia(int e)    { energia = e; }
    public void setVivo(boolean v)   { vivo = v; }
    public void setDias(int d)       { dias = d; }
    public void setDurmiendo(boolean d) { durmiendo = d; }
}