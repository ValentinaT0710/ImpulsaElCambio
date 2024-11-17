package com.impulsaElCambio.Servicios;

import com.impulsaElCambio.Model.DesafioModel;
import com.impulsaElCambio.Model.UsuarioModel;
import com.impulsaElCambio.Repository.DesafioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DesafioService {
    
    private static final Logger logger = LoggerFactory.getLogger(DesafioService.class);
    
    @Autowired
    private DesafioRepository desafioRepository;
    
    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ForoService foroService;
    
    @Autowired
    private ProyectoService proyectoService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public DesafioModel crearDesafio(DesafioModel desafio) {
        desafio.setActivo(true);
        DesafioModel desafioGuardado = desafioRepository.save(desafio);
        
        // Notificar a los clientes sobre el nuevo desafío
        Map<String, Object> mensaje = new HashMap<>();
        mensaje.put("tipo", "NUEVO_DESAFIO");
        mensaje.put("desafio", convertirDesafioAMap(desafioGuardado));
        messagingTemplate.convertAndSend("/topic/desafios", mensaje);
        
        return desafioGuardado;
    }

    private Map<String, Object> convertirDesafioAMap(DesafioModel desafio) {
        Map<String, Object> desafioMap = new HashMap<>();
        desafioMap.put("id", desafio.getId());
        desafioMap.put("tipoDesafio", desafio.getTipoDesafio());
        desafioMap.put("puntosRecompensa", desafio.getPuntosRecompensa());
        desafioMap.put("fechaInicio", desafio.getFechaInicio().toString());
        desafioMap.put("fechaFin", desafio.getFechaFin().toString());
        desafioMap.put("proyectoId", desafio.getProyectoId());
        desafioMap.put("activo", desafio.isActivo());
        return desafioMap;
    }

    public List<DesafioModel> obtenerDesafiosActivos() {
        return desafioRepository.findActiveDesafios(LocalDateTime.now());
    }

    @Transactional
    public String participarEnDesafio(Long desafioId, Long usuarioId) {
        DesafioModel desafio = desafioRepository.findById(desafioId)
            .orElseThrow(() -> new RuntimeException("Desafío no encontrado"));
            
        UsuarioModel usuario = usuarioService.findById(usuarioId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!desafio.estaActivo()) {
            return "El desafío no está activo";
        }

        if (desafio.getUsuariosCompletados().contains(usuario)) {
            return "Ya has completado este desafío";
        }

        if (desafio.getParticipantes().contains(usuario)) {
            return "Ya estás participando en este desafío";
        }

        desafio.getParticipantes().add(usuario);
        desafioRepository.save(desafio);
        return "Te has unido al desafío exitosamente";
    }

    @Transactional
    public String completarDesafio(Long desafioId, Long usuarioId) {
        DesafioModel desafio = desafioRepository.findById(desafioId)
            .orElseThrow(() -> new RuntimeException("Desafío no encontrado"));
            
        UsuarioModel usuario = usuarioService.findById(usuarioId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Verificar si ya completó el desafío
        if (desafio.getUsuariosCompletados().contains(usuario)) {
            return "Ya has completado este desafío";
        }

        // Validaciones básicas
        if (!desafio.estaActivo()) {
            return "El desafío ya no está activo";
        }

        if (!desafio.getParticipantes().contains(usuario)) {
            return "No estás participando en este desafío";
        }

        // Validación específica según el tipo de desafío
        boolean tareaCompletada = false;
        String mensajeError = "";

        switch (desafio.getTipoDesafio()) {
            case PUBLICAR_FORO:
                // Verifica si el usuario ha publicado en el foro después de unirse al desafío
                boolean haPublicado = foroService.hasUserPostedSince(usuarioId, desafio.getFechaInicio());
                if (!haPublicado) {
                    mensajeError = "Debes publicar al menos un post en el foro para completar este desafío";
                } else {
                    tareaCompletada = true;
                }
                break;
                
            case UNIRSE_PROYECTO:
                // Verifica si el usuario realmente se unió al proyecto específico
                if (desafio.getProyectoId() == null) {
                    mensajeError = "Error en la configuración del desafío: no hay proyecto asociado";
                } else {
                    boolean estaEnProyecto = proyectoService.isUserParticipatingInProject(usuarioId, desafio.getProyectoId());
                    if (!estaEnProyecto) {
                        mensajeError = "Debes unirte al proyecto específico para completar este desafío";
                    } else {
                        tareaCompletada = true;
                    }
                }
                break;
        }

        // Solo completa el desafío si la tarea fue realmente realizada
        if (tareaCompletada) {
            try {
                // Remover de participantes
                desafio.getParticipantes().remove(usuario);
                
                // Agregar a completados
                usuario.addDesafioCompletado(desafio);
                
                // Agregar puntos
                usuarioService.agregarPuntos(usuarioId, desafio.getPuntosRecompensa());
                
                // Guardar cambios
                desafioRepository.save(desafio);
                usuarioService.saveUsuario(usuario);
                
                return "¡Felicitaciones! Has completado el desafío";
            } catch (Exception e) {
                throw new RuntimeException("Error al completar el desafío: " + e.getMessage());
            }
        } else {
            return mensajeError;
        }
    }

    @Transactional
    public void verificarCompletitudDesafio(Long usuarioId) {
        List<DesafioModel> desafiosActivos = obtenerDesafiosActivos();
        
        for (DesafioModel desafio : desafiosActivos) {
            // Verifica si el usuario está participando y no ha completado el desafío
            if (desafio.getParticipantes().stream()
                    .anyMatch(u -> u.getId().equals(usuarioId)) &&
                !desafio.getUsuariosCompletados().stream()
                    .anyMatch(u -> u.getId().equals(usuarioId))) {
                
                boolean completado = false;
                
                switch (desafio.getTipoDesafio()) {
                    case PUBLICAR_FORO:
                        // Verifica que el usuario haya publicado después de unirse al desafío
                        completado = foroService.hasUserPostedSince(usuarioId, desafio.getFechaInicio());
                        break;
                        
                    case UNIRSE_PROYECTO:
                        // Verifica que el usuario esté activamente participando en el proyecto específico
                        if (desafio.getProyectoId() != null) {
                            completado = proyectoService.isUserParticipatingInProject(usuarioId, desafio.getProyectoId()) &&
                                       proyectoService.hasUserCompletedMinimumParticipation(usuarioId, desafio.getProyectoId());
                        }
                        break;
                }
                
                if (completado) {
                    completarDesafio(desafio.getId(), usuarioId);
                }
            }
        }
    }

    @Transactional
    public void eliminarDesafio(Long id) {
        try {
            desafioRepository.deleteById(id);
            
            // Notificar eliminación por WebSocket
            Map<String, Object> mensaje = new HashMap<>();
            mensaje.put("tipo", "ELIMINAR_DESAFIO");
            mensaje.put("desafioId", id);
            
            logger.info("Enviando notificación de eliminación de desafío: {}", id);
            messagingTemplate.convertAndSend("/topic/desafios", mensaje);
            
        } catch (Exception e) {
            logger.error("Error al eliminar el desafío: {}", id, e);
            throw new RuntimeException("Error al eliminar el desafío", e);
        }
    }

    public List<Map<String, Object>> obtenerDesafiosActivosUsuario(Long usuarioId) {
        try {
            logger.info("Obteniendo desafíos activos para usuario: {}", usuarioId);
            List<DesafioModel> desafiosActivos = obtenerDesafiosActivos();
            logger.info("Desafíos activos encontrados: {}", desafiosActivos.size());
            return desafiosActivos.stream()
                .filter(desafio -> desafio.getParticipantes().stream()
                    .anyMatch(u -> u.getId().equals(usuarioId)))
                .map(desafio -> {
                    Map<String, Object> desafioMap = new HashMap<>();
                    desafioMap.put("id", desafio.getId());
                    desafioMap.put("nombre", desafio.getNombre());
                    desafioMap.put("descripcion", desafio.getDescripcion());
                    desafioMap.put("tipoDesafio", desafio.getTipoDesafio());
                    
                    // Calcular progreso
                    int progreso = calcularProgresoDesafio(desafio, usuarioId);
                    desafioMap.put("progreso", progreso);
                    
                    // Calcular días restantes
                    long diasRestantes = ChronoUnit.DAYS.between(LocalDateTime.now(), desafio.getFechaFin());
                    desafioMap.put("diasRestantes", diasRestantes);
                    
                    return desafioMap;
                })
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error al obtener desafíos activos: ", e);
            return new ArrayList<>();
        }
    }

    private int calcularProgresoDesafio(DesafioModel desafio, Long usuarioId) {
        switch (desafio.getTipoDesafio()) {
            case PUBLICAR_FORO:
                // Verificar publicaciones en el foro desde que se unió al desafío
                boolean haPublicado = foroService.hasUserPostedSince(usuarioId, desafio.getFechaInicio());
                return haPublicado ? 100 : 0;
                
            case UNIRSE_PROYECTO:
                // Verificar participación en proyecto
                if (desafio.getProyectoId() != null) {
                    boolean estaEnProyecto = proyectoService.isUserParticipatingInProject(usuarioId, desafio.getProyectoId());
                    boolean cumpleParticipacion = proyectoService.hasUserCompletedMinimumParticipation(usuarioId, desafio.getProyectoId());
                    
                    if (cumpleParticipacion) return 100;
                    if (estaEnProyecto) return 50;
                }
                return 0;
                
            default:
                return 0;
        }
    }
} 