package com.tallerwebi.dominio;

import java.util.ArrayList;

import org.springframework.ui.ModelMap;

import com.tallerwebi.dominio.excepcion.JugadaInvalidaException;
import com.tallerwebi.enums.Jugador;

public interface ServicioPartida {

    boolean partidaExiste(Long idPartida);
    Long iniciarPartida();
    void nuevaRonda(Partida partida);
    void reiniciarRonda(Partida partida);
    void repartirCartas(Partida partida);
    void actualizarCambiosDePartida(Long idPartida, Jugada jugada, Jugador jugador, Usuario usuario) throws JugadaInvalidaException;
    void calcularJugadaIA(Long idPartida);
    Partida buscarPartida(Long idPartida);
}
