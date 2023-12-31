package com.tallerwebi.dominio;

import com.tallerwebi.dominio.excepcion.JugadaInvalidaException;
import com.tallerwebi.enums.Jugador;
import com.tallerwebi.enums.TipoJugada;
import com.tallerwebi.infraestructura.RepositorioPartida;

import java.util.ArrayList;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("servicioIA")
@Transactional
public class ServicioIAImpl implements ServicioIA {

    private RepositorioPartida repositorioPartida;

    @Autowired
    public ServicioIAImpl(RepositorioPartida repositorioPartida){
        this.repositorioPartida = repositorioPartida;
    }

    @Override
    public Jugada calcularJugada(Long idPartida) {
        Partida partida = repositorioPartida.buscarPartidaPorId(idPartida);
        if (partida.getCantoTruco() || partida.getCantoEnvido()) {
            return respuestaAleatoria();
        } else {
            int numero = (int) (Math.random() * 2);

            if (numero == 0) {
                short indice = calcularEnvido(idPartida);
                if (indice != 0) {
                    try {
                        return cantarEnvido(idPartida, indice);
                    } catch (JugadaInvalidaException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (numero == 1) {
                if (verificarTruco(idPartida)) {
                    return cantarTruco();
                }
            }
        }
        return tirarCartaAleatoria(idPartida);
    }

    @Override
    public Jugada respuestaAleatoria(){
        int numero = (int) (Math.random() * 2);

        if(numero==0){
            return new Jugada(TipoJugada.RESPUESTA,0);
        }else{
            return new Jugada(TipoJugada.RESPUESTA,1);
        }
    }

    @Override
    public Jugada tirarCartaAleatoria(Long idPartida) {
        Mano manoIA = repositorioPartida.buscarCartasDeLaIa(idPartida);

        int numero;

        while(true){
            numero = (int) (Math.random() * 3 + 1);
            if (manoIA.getCarta(numero)!=null) {
                return new Jugada(TipoJugada.CARTA, numero);
            }
        }
    }


    @Override
    public Jugada tirarMejorCarta(Long idPartida) {
        Mano manoIA = repositorioPartida.buscarCartasDeLaIa(idPartida);
        if (manoIA == null || manoIA.getCartas().isEmpty()) {
            return null;
        }

        Carta mejorCarta;
        Integer posicionCarta = 0;

        if (manoIA.getCarta(1).getValorTruco() >= manoIA.getCarta(2).getValorTruco()) {
            if (manoIA.getCarta(1).getValorTruco() >= manoIA.getCarta(3).getValorTruco()) {
                mejorCarta = manoIA.getCarta(1);
                posicionCarta = 1;
            } else {
                mejorCarta = manoIA.getCarta(3);
                posicionCarta = 3;
            }
        } else {
            if (manoIA.getCarta(2).getValorTruco() >= manoIA.getCarta(3).getValorTruco()) {
                mejorCarta = manoIA.getCarta(2);
                posicionCarta = 2;
            } else {
                mejorCarta = manoIA.getCarta(3);
                posicionCarta = 3;
            }
        }


        return new Jugada(TipoJugada.CARTA, posicionCarta);
    }

    @Override
    public Jugada tirarPeorCarta(Long idPartida) {
        Mano manoIA = repositorioPartida.buscarCartasDeLaIa(idPartida);
        if (manoIA == null || manoIA.getCartas().isEmpty()) {
            return null;
        }
        Carta mejorCarta;
        Integer posicionCarta = 0;

        if (manoIA.getCarta(1).getValorTruco() <= manoIA.getCarta(2).getValorTruco()) {
            if (manoIA.getCarta(1).getValorTruco() <= manoIA.getCarta(3).getValorTruco()) {
                mejorCarta = manoIA.getCarta(1);
                posicionCarta = 1;
            } else {
                mejorCarta = manoIA.getCarta(3);
                posicionCarta = 3;
            }
        } else {
            if (manoIA.getCarta(2).getValorTruco() <= manoIA.getCarta(3).getValorTruco()) {
                mejorCarta = manoIA.getCarta(2);
                posicionCarta = 2;
            } else {
                mejorCarta = manoIA.getCarta(3);
                posicionCarta = 3;
            }
        }

        return new Jugada(TipoJugada.CARTA, posicionCarta);
    }

    @Override
    public boolean verificarTruco(Long idPartida){
        Partida partida = repositorioPartida.buscarPartidaPorId(idPartida);

        int estadoTruco = partida.getEstadoTruco();
        int trucoAQuerer = partida.getTrucoAQuerer();
        boolean puedeCantarTruco = partida.puedeCantarTruco(Jugador.IA);

        if(estadoTruco+trucoAQuerer < 4 && puedeCantarTruco){
            return true;
        } else{
            return false;
        }
    }
    @Override
    public Jugada cantarTruco() {
        return new Jugada(TipoJugada.TRUCO);
    }
    @Override
    public Jugada cantarEnvido(Long idPartida,short indice) throws JugadaInvalidaException{

        if(indice == 2){
            return new Jugada(TipoJugada.ENVIDO,2);
        }
        if(indice == 3){
            return new Jugada(TipoJugada.ENVIDO,3);
        }
        if(indice == 100){
            return new Jugada(TipoJugada.ENVIDO,100);
        }


        throw new JugadaInvalidaException("El indice para cantar envido es invalido");
    }

    private boolean siPuedeCantarFaltaEnvido(Long idPartida) {
        Partida partida = repositorioPartida.buscarPartidaPorId(idPartida);

        return !partida.getCantoFaltaEnvido();
    }

    private boolean siPuedeCantarRealEnvido(Long idPartida) {
        Partida partida = repositorioPartida.buscarPartidaPorId(idPartida);

        int valorEnvido = partida.getEstadoEnvido();

        if(valorEnvido==0 || valorEnvido==2){
            return true;
        } else{
            return false;
        }
    }

    private boolean siPuedeCantarEnvido(Long idPartida) {
        Partida partida = repositorioPartida.buscarPartidaPorId(idPartida);

        int valorEnvido = partida.getEstadoEnvido();


        if(valorEnvido==0){
            return true;
        } else{
            return false;
        }

    }

    @Override
    public short calcularEnvido(Long idPartida) {

        Mano manoIA = repositorioPartida.buscarCartasDeLaIa(idPartida);
        if (manoIA == null || manoIA.getCartas().isEmpty()) {
            return 0;
        }

        short envido = manoIA.getValorEnvido();

        if (envido >= 17 && envido <= 25 && siPuedeCantarEnvido(idPartida)) {
            return 2;
        }
        if (envido >= 26 && envido <= 30 && siPuedeCantarRealEnvido(idPartida)) {
            return 3;
        }
        if (envido >= 31 && envido <= 33 && siPuedeCantarFaltaEnvido(idPartida)) {
            return 100;
        }

        return 0;
    }

    @Override
    public Jugada aceptarTruco(Long idPartida) {
        return new Jugada(TipoJugada.RESPUESTA,1);
    }

    @Override
    public Jugada rechazarTruco(Long idPartida) {
        return new Jugada(TipoJugada.RESPUESTA,2);
    }

    @Override
    public Jugada aceptarEnvido(Long idPartida) {
        return new Jugada(TipoJugada.RESPUESTA,1);

    }


    @Override
    public Jugada rechazarEnvido(Long idPartida) {
        return new Jugada(TipoJugada.RESPUESTA,2);
    }


}
