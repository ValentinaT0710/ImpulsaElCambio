package com.impulsaElCambio.Servicios;

import com.impulsaElCambio.Model.Foro_post;
import com.impulsaElCambio.Repository.ForoPostRepository;
import com.impulsaElCambio.Model.UsuarioModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import org.springframework.data.domain.Pageable;


/**
 * Servicio que maneja las operaciones relacionadas con el foro.
 * Esta clase proporciona métodos para crear, editar, borrar y recuperar posts del foro.
 */
@Service
public class ForoService {
    private static final Logger logger = LoggerFactory.getLogger(ForoService.class);

    private final ForoPostRepository foroPostRepository;
    private final UsuarioService usuarioService;

    public ForoService(ForoPostRepository foroPostRepository, UsuarioService usuarioService) {
        this.foroPostRepository = foroPostRepository;
        this.usuarioService = usuarioService;
    }
    
    /**
     * Recupera solo los comentarios principales del foro.
     */
    public List<Foro_post> getAllComentarios() {
        // Solo traer comentarios principales (no respuestas)
        List<Foro_post> comentarios = foroPostRepository.findMainComments(Pageable.unpaged());
        for (Foro_post comentario : comentarios) {
            if (comentario.getUsuario() != null) {
                comentario.setInsigniasUsuario(comentario.getUsuario().getInsignias());
            }
            // Forzar la carga de respuestas para evitar LazyLoading
            comentario.getRespuestas().size();
        }
        return comentarios;
    }

    /**
     * Crea un nuevo comentario en el foro.
     * @param contenido El contenido del comentario.
     * @param usuarioId El ID del usuario que crea el comentario.
     * @return El comentario creado.
     * @throws RuntimeException si ocurre un error durante la creación.
     */
    @Transactional
    public Foro_post crearComentario(String contenido, Long usuarioId) {
        logger.info("Creando comentario con contenido: '" + contenido + "'");
        try {
            UsuarioModel usuario = usuarioService.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            Foro_post nuevoComentario = new Foro_post();
            nuevoComentario.setContenido(contenido);
            nuevoComentario.setUsuario(usuario);
            nuevoComentario.setFechaCreacion(LocalDateTime.now());
            nuevoComentario.setInsigniasUsuario(usuario.getInsignias());  // Añade esta línea
            return foroPostRepository.save(nuevoComentario);
        } catch (Exception e) {
            logger.error("Error al crear comentario: ", e);
            throw new RuntimeException("Error al crear comentario", e);
        }
    }

    /**
     * Borra un comentario del foro.
     * @param id El ID del comentario a borrar.
     * @param usuarioId El ID del usuario que intenta borrar el comentario.
     * @return Un mensaje indicando el resultado de la operación.
     */
    @Transactional
    public String borrarComentario(Long id, Long usuarioId) {
        Optional<Foro_post> comentarioOpt = foroPostRepository.findById(id);
        Optional<UsuarioModel> usuarioOpt = usuarioService.findById(usuarioId);
        
        if (comentarioOpt.isEmpty()) {
            return "Comentario no encontrado";
        }
        if (usuarioOpt.isEmpty()) {
            return "Usuario no encontrado";
        }
        
        Foro_post comentario = comentarioOpt.get();
        UsuarioModel usuario = usuarioOpt.get();
        
        // Permitir borrar si es el autor del comentario o si es un administrador
        if (comentario.getUsuario().getId().equals(usuarioId) || usuario.getRol() == UsuarioModel.Rol.ADMIN) {
            foroPostRepository.deleteById(id);
            return "Comentario eliminado exitosamente";
        } else {
            return "No tienes permiso para eliminar este comentario";
        }
    }

    /**
     * Edita un comentario existente en el foro.
     * @param id El ID del comentario a editar.
     * @param nuevoContenido El nuevo contenido del comentario.
     * @param usuarioId El ID del usuario que intenta editar el comentario.
     * @return El comentario editado, o null si la operación no fue exitosa.
     */
    @Transactional
    public Foro_post editarComentario(Long id, String nuevoContenido, Long usuarioId) {
        return foroPostRepository.findById(id)
            .flatMap(comentario -> usuarioService.findById(usuarioId)
                .map(usuario -> {
                    if (comentario.getUsuario().getId().equals(usuarioId) || usuario.getRol() == UsuarioModel.Rol.ADMIN) {
                        comentario.setContenido(nuevoContenido);
                        return foroPostRepository.save(comentario);
                    }
                    return null;
                }))
            .orElse(null);
    }

    /**
     * Cuenta el número total de posts en el foro.
     * @return El número total de posts.
     */
    public int countAllPosts() {
        return (int) foroPostRepository.count();
    }

    /**
     * Cuenta el número de posts creados en los últimos días especificados.
     * @param days El número de días hacia atrás para contar los posts.
     * @return El número de posts creados en el período especificado.
     */
    public long countRecentPosts(int days) {
        try {
            return foroPostRepository.countByFechaCreacionAfter(LocalDateTime.now().minusDays(days));
        } catch (Exception e) {
            logger.error("Error al contar posts recientes: ", e);
            return 0;
        }
    }
    /**
     * Cuenta el número de posts realizados por un usuario específico.
     * @param userId El ID del usuario del cual se quieren contar los posts.
     * @return El número total de posts realizados por el usuario.
     */
    public int countPostsByUser(Long userId) {
        return foroPostRepository.countByUsuario_Id(userId);
    }

    public List<Foro_post> getUltimasCincoPublicaciones() {
        return foroPostRepository.findTop5ByOrderByFechaCreacionDesc();
    }

    @Transactional
    public void eliminarPostsDeUsuario(UsuarioModel usuario) {
        foroPostRepository.deleteByUsuario(usuario);
    }

    public boolean hasUserPostedSince(Long usuarioId, LocalDateTime since) {
        return foroPostRepository.existsByUsuario_IdAndFechaCreacionAfter(usuarioId, since);
    }

    @Transactional
    public Map<String, Object> toggleMeEncanta(Long comentarioId, Long usuarioId) {
        Map<String, Object> resultado = new HashMap<>();
        
        try {
            Foro_post comentario = foroPostRepository.findById(comentarioId)
                .orElseThrow(() -> new RuntimeException("Comentario no encontrado"));
            
            UsuarioModel usuario = usuarioService.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            boolean meEncantaActivo = false;
            
            // Verificar si el usuario ya dio me encanta
            if (comentario.getMeEncanta().contains(usuario)) {
                // Si ya existe, lo quitamos
                comentario.getMeEncanta().remove(usuario);
                comentario.setContadorMeEncanta(comentario.getContadorMeEncanta() - 1);
            } else {
                // Si no existe, lo agregamos
                comentario.getMeEncanta().add(usuario);
                comentario.setContadorMeEncanta(comentario.getContadorMeEncanta() + 1);
                meEncantaActivo = true;
            }
            
            foroPostRepository.save(comentario);
            
            resultado.put("contadorMeEncanta", comentario.getContadorMeEncanta());
            resultado.put("meEncantaActivo", meEncantaActivo);
            
        } catch (Exception e) {
            logger.error("Error al procesar me encanta: ", e);
            resultado.put("error", "Error al procesar la acción de me encanta");
        }
        
        return resultado;
    }

    @Transactional
    public Foro_post crearRespuesta(String contenido, Long usuarioId, Long comentarioPadreId) {
        try {
            Foro_post comentarioPadre = foroPostRepository.findById(comentarioPadreId)
                .orElseThrow(() -> new RuntimeException("Comentario padre no encontrado"));
            
            UsuarioModel usuario = usuarioService.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            Foro_post respuesta = new Foro_post();
            respuesta.setContenido(contenido);
            respuesta.setUsuario(usuario);
            respuesta.setComentarioPadre(comentarioPadre);
            respuesta.setFechaCreacion(LocalDateTime.now());
            respuesta.setInsigniasUsuario(usuario.getInsignias());
            
            return foroPostRepository.save(respuesta);
        } catch (Exception e) {
            logger.error("Error al crear respuesta: ", e);
            throw new RuntimeException("Error al crear la respuesta: " + e.getMessage());
        }
    }

    @Transactional
    public boolean eliminarComentario(Long comentarioId, Long usuarioId) {
        try {
            Foro_post comentario = foroPostRepository.findById(comentarioId)
                .orElseThrow(() -> new RuntimeException("Comentario no encontrado"));
            
            // Verificar si el usuario es el autor del comentario
            if (!comentario.getUsuario().getId().equals(usuarioId)) {
                return false;
            }
            
            // Si es una respuesta, actualizar el contador del comentario padre
            if (comentario.getComentarioPadre() != null) {
                Foro_post comentarioPadre = comentario.getComentarioPadre();
                comentarioPadre.getRespuestas().remove(comentario);
                foroPostRepository.save(comentarioPadre);
            }
            
            foroPostRepository.delete(comentario);
            return true;
        } catch (Exception e) {
            logger.error("Error al eliminar comentario: ", e);
            throw new RuntimeException("Error al eliminar el comentario", e);
        }
    }

    public List<Foro_post> findMainComments(Pageable pageable) {
        return foroPostRepository.findByComentarioPadreIsNullOrderByFechaCreacionDesc(pageable);
    }

    @Transactional
    public void eliminarMeEncantaDeUsuario(Long usuarioId) {
        List<Foro_post> posts = foroPostRepository.findAll();
        for (Foro_post post : posts) {
            post.getMeEncanta().removeIf(usuario -> usuario.getId().equals(usuarioId));
            foroPostRepository.save(post);
        }
    }
}
