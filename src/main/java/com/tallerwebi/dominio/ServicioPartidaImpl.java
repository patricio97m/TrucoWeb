package com.tallerwebi.dominio;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.ModelMap;
import com.tallerwebi.dominio.ServicioIA;
import com.tallerwebi.dominio.excepcion.JugadaInvalidaException;
import com.tallerwebi.enums.Jugador;
import com.tallerwebi.enums.TipoJugada;
import com.tallerwebi.infraestructura.RepositorioPartida;

import java.util.ArrayList;
import java.util.Random;

import javax.transaction.Transactional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Service("servicioPartida")
@Transactional
public class ServicioPartidaImpl implements ServicioPartida{

    private ServicioIA servicioIa;
    private RepositorioPartida repositorioPartida;
    private static final Logger logger = LogManager.getLogger(ServicioPartidaImpl.class);

    @Autowired
    public ServicioPartidaImpl(RepositorioPartida repositorioPartida, ServicioIA servicioIa){
        this.repositorioPartida = repositorioPartida;
        this.servicioIa = servicioIa;
    }

    //Sistema de la Partida

    @Override
    public boolean partidaExiste(Long idPartida){
        return !(repositorioPartida.buscarPartidaPorId(idPartida) == null);
    }

    @Override
    public Long iniciarPartida(){
        Partida partida = new Partida();
        nuevaRonda(partida);
        return repositorioPartida.guardarNuevaPartida(partida);
    }

    @Override
    public void nuevaRonda(Partida partida){
        reiniciarRonda(partida);
        repartirCartas(partida);
        if((partida.getPuntosJugador() + partida.getPuntosIa()) != 0){
            partida.cambiarQuienEsMano();
        }
    }

    @Override
    public void reiniciarRonda(Partida partida) {
        Ronda ronda = new Ronda();
        repositorioPartida.guardarNuevaRonda(ronda);
        partida.setRonda(ronda);
    }

    @Override
    public void repartirCartas(Partida partida){
        ArrayList<Long> numerosDeCartas = generarNumerosDeCartas(6);
        ArrayList<Carta> cartasDelJugador = new ArrayList<Carta>();
        ArrayList<Carta> cartasDeLaIa = new ArrayList<Carta>();
        int index = 0;
        for (Long id : numerosDeCartas) {
            Carta carta = repositorioPartida.buscarCartaPorId(id);
            if(index < 3){
                cartasDelJugador.add(carta);
            }
            else{
                cartasDeLaIa.add(carta);
            }
            index++;
        }

        Mano cartasJugadasJugador = new Mano();
        Mano cartasJugadasIa = new Mano();
        repositorioPartida.guardarNuevaMano(cartasJugadasJugador);
        repositorioPartida.guardarNuevaMano(cartasJugadasIa);

        Mano manoDelJugador = new Mano(cartasDelJugador);
        Mano manoDeLaIa = new Mano(cartasDeLaIa);
        repositorioPartida.guardarNuevaMano(manoDelJugador);
        repositorioPartida.guardarNuevaMano(manoDeLaIa);

        partida.setManoDelJugador(manoDelJugador);
        partida.setManoDeLaIa(manoDeLaIa);
        partida.setCartasJugadasJugador(cartasJugadasJugador);
        partida.setCartasJugadasIa(cartasJugadasIa);
    }

    @Override
    public void actualizarCambiosDePartida(Long idPartida, Jugada jugada, Jugador jugador) throws JugadaInvalidaException {
        TipoJugada tipoJugada = jugada.getTipoJugada();
        Integer index = jugada.getIndex();
        Partida partida = repositorioPartida.buscarPartidaPorId(idPartida);

        partida.setUltimaJugada(jugada);

        if(tipoJugada == TipoJugada.ENVIDO){
            try {
				calcularCambiosEnvido(idPartida, index, jugador);
                if(jugador == Jugador.IA){
                    partida.setTurnoIA(false);
                }
                else{
                    partida.setTurnoIA(true);
                }
			} catch (JugadaInvalidaException e) {
				e.printStackTrace();
			}
        }
        else if(tipoJugada == TipoJugada.TRUCO){
            try {
				calcularCambiosTruco(idPartida, jugador);
                if(jugador == Jugador.IA){
                    partida.setTurnoIA(false);
                }
                else{
                    partida.setTurnoIA(true);
                }
			} catch (JugadaInvalidaException e) {
				e.printStackTrace();
			}
        }
        else if(tipoJugada == TipoJugada.RESPUESTA){
            if(jugador == Jugador.IA){
                partida.setTurnoIA(false);
            }
            else{
                partida.setTurnoIA(true);
            }
            calcularCambiosRespuesta(idPartida, index, jugador); 
        }
        else if(tipoJugada == TipoJugada.CARTA){
            try{
                calcularCambiosCarta(idPartida, index, jugador);
            }
            catch(JugadaInvalidaException e){
				e.printStackTrace();
            }
        }
        else if(tipoJugada == TipoJugada.MAZO){
            calcularCambiosMazo(idPartida, jugador);
        }
        else{
            throw new JugadaInvalidaException("El tipo de jugada realizada no existe");
        }

        partida.chequearSiHayUnGanador();
    }

    @Override
    public void calcularJugadaIA(Long idPartida) {
        Jugada jugadaIa = servicioIa.calcularJugada(idPartida);
        try {
            actualizarCambiosDePartida(idPartida, jugadaIa, Jugador.IA);
        } catch (JugadaInvalidaException e) {
            e.printStackTrace();
        }

    }









    //Getters y métodos auxiliares

    private void calcularCambiosMazo(Long idPartida, Jugador jugador) {
        Partida partida = repositorioPartida.buscarPartidaPorId(idPartida);

        if(partida.getEstadoEnvido() == 0 && noSeJugoNingunaCarta(partida) && partida.getCantoTruco()){
            if(jugador == Jugador.IA){
                partida.setPuntosJugador(partida.getPuntosJugador() + 1);
            }
            else if(jugador == Jugador.J1){
                partida.setPuntosIa(partida.getPuntosIa() + 1);
            }
        }
        else if(partida.getEstadoEnvido() != -1){
            if(jugador == Jugador.IA){
                partida.setPuntosJugador(partida.getPuntosJugador() + partida.getEstadoEnvido());
            }
            else if(jugador == Jugador.J1){
                partida.setPuntosIa(partida.getPuntosIa() + partida.getEstadoEnvido());
            }
        }
        
        if(jugador == Jugador.IA){
            partida.setPuntosJugador(partida.getPuntosJugador() + partida.getEstadoTruco());
        }
        else if(jugador == Jugador.J1){
            partida.setPuntosIa(partida.getPuntosIa() + partida.getEstadoTruco());
        }

        nuevaRonda(partida);
    }

    private void calcularCambiosCarta(Long idPartida, Integer index, Jugador jugador) throws JugadaInvalidaException {
        logger.info("Se jugó una carta");
        Partida partida = repositorioPartida.buscarPartidaPorId(idPartida);
        if(partida.getCantoEnvido() || partida.getCantoTruco()){
            throw new JugadaInvalidaException("No se puede jugar una carta si se esta cantando envido primero");
        }
        int tiradaActual = partida.getTiradaActual();
        Mano manoDelJugador;
        Mano cartasJugadasDelJugador;
        Mano cartasJugadasDelRival;
        if(jugador == Jugador.IA){
            manoDelJugador = partida.getManoDeLaIa();
            cartasJugadasDelJugador = partida.getCartasJugadasIa();
            cartasJugadasDelRival = partida.getCartasJugadasJugador();
        }
        else{
            manoDelJugador = partida.getManoDelJugador();
            cartasJugadasDelJugador = partida.getCartasJugadasJugador();
            cartasJugadasDelRival = partida.getCartasJugadasIa();
        }

        cartasJugadasDelJugador.setCarta(tiradaActual, manoDelJugador.getCarta(index));
        manoDelJugador.setCarta(index, null);
        
        if(jugador == Jugador.IA){
            partida.setTurnoIA(false);
        }
        else{
            partida.setTurnoIA(true);
        }

        if(cartasJugadasDelRival.getCarta(tiradaActual) != null){
            logger.info("El rival ya jugó una carta en esta Tirada");
            Jugador ganadorTirada = partida.calcularGanadorTirada(tiradaActual);
            if(ganadorTirada == jugador){
                partida.setTurnoIA(!partida.isTurnoIA());
            }
            Jugador ganadorRonda = partida.hayGanadorDeLaRonda();

            if(ganadorRonda == Jugador.NA){
                logger.info("Nadie gano la ronda");
                partida.setTiradaActual(tiradaActual + 1);
            }
            else if(ganadorRonda == Jugador.IA){
                logger.info("La IA gano la ronda");
                partida.setPuntosIa(partida.getPuntosIa() + partida.getEstadoTruco());
                nuevaRonda(partida);
                    
            }
            else if(ganadorRonda == Jugador.J1){
                logger.info("El jugador gano la ronda");
                partida.setPuntosJugador(partida.getPuntosJugador() + partida.getEstadoTruco());
                nuevaRonda(partida);
            }    
        }
    }

    private void calcularCambiosRespuesta(Long idPartida, Integer index, Jugador jugador) {
        Partida partida = repositorioPartida.buscarPartidaPorId(idPartida);

        if(index == 0){
            if(partida.getCantoEnvido()){
                if(partida.getEstadoEnvido() == 0){
                    if(jugador == Jugador.IA){
                        partida.setPuntosJugador(partida.getPuntosJugador() + 1);
                    }
                    else if(jugador == Jugador.J1){
                        partida.setPuntosIa(partida.getPuntosIa() + 1);
                    }
                }else{
                    if(jugador == Jugador.IA){
                        partida.setPuntosJugador(partida.getPuntosJugador() + partida.getEstadoEnvido());
                    }
                    else if(jugador == Jugador.J1){
                        partida.setPuntosIa(partida.getPuntosIa() + partida.getEstadoEnvido());
                    }
                }
                
                partida.setCantoEnvido(false);
                partida.setEstadoEnvido(-1);
            }
            else if(partida.getCantoTruco()){
                if(jugador == Jugador.IA){
                    partida.setPuntosJugador(partida.getPuntosJugador() + partida.getEstadoTruco());
                }
                else if(jugador == Jugador.J1){
                    partida.setPuntosIa(partida.getPuntosIa() + partida.getEstadoTruco());
                }
                nuevaRonda(partida);
            }
        }
        else{
            if(partida.getCantoEnvido()){
                Jugador ganadorEnvido = partida.getGanadorEnvido();

                if(partida.getCantoFaltaEnvido()){
                    partida.setEstadoEnvido(partida.getEnvidoAQuerer());
                }
                else{
                    partida.setEstadoEnvido(partida.getEstadoEnvido() + partida.getEnvidoAQuerer());
                }
                
                if(ganadorEnvido == Jugador.IA){
                    partida.setPuntosJugador(partida.getPuntosJugador() + partida.getEstadoEnvido());
                }
                else if(ganadorEnvido == Jugador.J1){
                    partida.setPuntosIa(partida.getPuntosIa() + partida.getEstadoEnvido());
                }

                partida.setEnvidoAQuerer(0);
                partida.setCantoEnvido(false);
                partida.setEstadoEnvido(-1);
                
            }
            else if(partida.getCantoTruco()){
                partida.setEstadoTruco(partida.getEstadoTruco() + partida.getTrucoAQuerer());
                partida.setTrucoAQuerer(0);
                partida.setCantoTruco(false);
                partida.setEstadoEnvido(-1);
            }
               
        }
    }

    private void calcularCambiosEnvido(Long idPartida, Integer index, Jugador jugador) throws JugadaInvalidaException {
        Partida partida = repositorioPartida.buscarPartidaPorId(idPartida);
        if(partida.getEstadoEnvido() >= 0){
            if(partida.getCantoEnvido()){
            partida.setEstadoEnvido(partida.getEstadoEnvido() + partida.getEnvidoAQuerer());
            }
            else{
                partida.setCantoEnvido(true);
            }

            if(index < 4){
                //Envido o Real Envido
                partida.setEnvidoAQuerer(index.intValue());
            }
            else{
                //Falta Envido
                partida.setCantoFaltaEnvido(true);
                partida.setEnvidoAQuerer(partida.getLimitePuntos() - partida.getPuntosGanador());
            }
        }
        else{
            throw new JugadaInvalidaException("El jugador " + jugador + " intento cantar envido cuando no era posible");
        }
    }

    private void calcularCambiosTruco(Long idPartida, Jugador jugador) throws JugadaInvalidaException {
        Partida partida = repositorioPartida.buscarPartidaPorId(idPartida);
        if(partida.puedeCantarTruco(jugador)){
            if(partida.getCantoTruco()){
            partida.setEstadoTruco(partida.getEstadoTruco() + partida.getTrucoAQuerer());
            }
            partida.setTrucoAQuerer(1);
            partida.setCantoTruco(true);
            partida.setQuienCantoTruco(jugador);
        }
        else throw new JugadaInvalidaException("El jugador " + jugador + " intento cantar truco sin tener el quiero");
        
    }

    private boolean noSeJugoNingunaCarta(Partida partida) {
        if(partida.getCartasJugadasIa().getCarta(1) == null && partida.getCartasJugadasJugador().getCarta(1) == null){
            return true;
        }
        else{
            return false;
        } 
    }

    private ArrayList<Long> generarNumerosDeCartas(int count){
            ArrayList<Long> randomNumbers = new ArrayList<>();
            Random random = new Random();

            while (randomNumbers.size() < count) {
                Long randomNumber = (long) random.nextInt(40);
                if (!randomNumbers.contains(randomNumber)) {
                    randomNumbers.add(randomNumber);
                }
            }

            return randomNumbers;
        }

    @Override
    public ArrayList<String> getManoDelJugador(Long idPartida) {
        ArrayList<Carta> cartasDelJugador = repositorioPartida.buscarCartasDelJugador(idPartida).getCartas();
        ArrayList<String> nombreCartasDelJugador = new ArrayList<String>();
        for (Carta carta : cartasDelJugador) {
            if(carta != null){
                String nombre = carta.getPalo() + carta.getNumero();
                nombreCartasDelJugador.add(nombre);
            }
            else{
                nombreCartasDelJugador.add("NULL");
            }
            
        }
        return nombreCartasDelJugador;
    }

    @Override
    public ArrayList<String> getCartasJugadasJugador(Long idPartida) {
        ArrayList<Carta> cartasJugadasJugador = repositorioPartida.buscarCartasJugadasJugador(idPartida).getCartas();
        ArrayList<String> nombreCartasJugadasJugador = new ArrayList<String>();
        for (Carta carta : cartasJugadasJugador){
            if(carta != null){
                String nombre = carta.getPalo() + carta.getNumero();
                nombreCartasJugadasJugador.add(nombre);
            }
            
        }
        return nombreCartasJugadasJugador;
    }

    @Override
    public ArrayList<String> getCartasJugadasIa(Long idPartida) {
        ArrayList<Carta> cartasJugadasIa = repositorioPartida.buscarCartasJugadasIa(idPartida).getCartas();
        ArrayList<String> nombreCartasJugadasIa = new ArrayList<String>();
        for (Carta carta : cartasJugadasIa){
            if(carta != null){
                String nombre = carta.getPalo() + carta.getNumero();
                nombreCartasJugadasIa.add(nombre);
            }
            
        }
        return nombreCartasJugadasIa;
    }

    @Override
    public ModelMap getDetallesPartida(Long idPartida) {
        Partida partida = repositorioPartida.buscarPartidaPorId(idPartida);
        ModelMap model = new ModelMap();
        model.put("Ultima Jugada", partida.getUltimaJugada());
        model.put("turnoIA", partida.isTurnoIA());
        model.put("manoDelJugador", getManoDelJugador(idPartida));
        model.put("cartasRestantesIa", 3 - (getCartasJugadasIa(idPartida).size()));
        model.put("cartasJugadasIa", getCartasJugadasIa(idPartida));
        model.put("cartasJugadasJugador", getCartasJugadasJugador(idPartida));
        model.put("puntosJugador", partida.getPuntosJugador());
        model.put("puntosIa", partida.getPuntosIa());
        model.put("truco", partida.getEstadoTruco());
        model.put("trucoAQuerer", partida.getTrucoAQuerer());
        model.put("cantoTruco", partida.getCantoTruco());
        model.put("puedeCantarTruco", partida.puedeCantarTruco(Jugador.J1));
        model.put("envido", partida.getEstadoEnvido());
        model.put("envidoAQuerer", partida.getEnvidoAQuerer());
        model.put("cantoEnvido", partida.getCantoEnvido());
        model.put("cantoFaltaEnvido", partida.getCantoFaltaEnvido());
        model.put("ganador", partida.getGanador());
        return model;


    }

    @Override
    public String getDetallesPartidaJSON(Long idPartida) {
        Partida partida = repositorioPartida.buscarPartidaPorId(idPartida);

        // Construye manualmente una cadena JSON
        String json = "{"
                + "\"ultimaJugada\":\"" + partida.getUltimaJugada() + "\","
                + "\"turnoIA\":" + partida.isTurnoIA() + ","
                + "\"manoDelJugador\":" + convertirArrayListAJSON(getManoDelJugador(idPartida)) + ","
                + "\"cartasRestantesIa\":" + (3 - getCartasJugadasIa(idPartida).size()) + ","
                + "\"cartasJugadasIa\":" + convertirArrayListAJSON(getCartasJugadasIa(idPartida)) + ","
                + "\"cartasJugadasJugador\":" + convertirArrayListAJSON(getCartasJugadasJugador(idPartida)) + ","
                + "\"puntosJugador\":" + partida.getPuntosJugador() + ","
                + "\"puntosIa\":" + partida.getPuntosIa() + ","
                + "\"truco\":" + partida.getEstadoTruco() + ","
                + "\"trucoAQuerer\":" + partida.getTrucoAQuerer() + ","
                + "\"cantoTruco\":" + partida.getCantoTruco() + ","
                + "\"puedeCantarTruco\":" + partida.puedeCantarTruco(Jugador.J1) + ","
                + "\"quienCantoTruco\":\"" + partida.getQuienCantoTruco() + "\","
                + "\"envido\":" + partida.getEstadoEnvido() + ","
                + "\"envidoAQuerer\":" + partida.getEnvidoAQuerer() + ","
                + "\"cantoEnvido\":" + partida.getCantoEnvido() + ","
                + "\"cantoFaltaEnvido\":" + partida.getCantoFaltaEnvido() + ","
                + "\"puntosEnvidoIA\":" + partida.getManoDeLaIa().getValorEnvido() + ","
                + "\"puntosEnvidoJugador\":" + partida.getManoDelJugador().getValorEnvido() + ","
                + "\"tiradaActual\":" + partida.getTiradaActual() + ","
                + "\"quienEsMano\":\"" + partida.getQuienEsMano() + "\","
                + "\"ganador\":\"" + partida.getGanador() + "\""
                + "}";

        return json;
    }

    private String convertirArrayListAJSON(ArrayList<String> arrayList) {
        StringBuilder json = new StringBuilder("[");
        for (String item : arrayList) {
            json.append("\"").append(item).append("\",");
        }
        if (arrayList.size() > 0) {
            json.deleteCharAt(json.length() - 1); // Elimina la última coma
        }
        json.append("]");
        return json.toString();
    }
        
}


