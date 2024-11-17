package com.impulsaElCambio.Repository;

import com.impulsaElCambio.Model.Foro_post;
import com.impulsaElCambio.Model.UsuarioModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio para la entidad foro_post.
 * Esta interfaz proporciona métodos para acceder y manipular datos de posts del foro en la base de datos.
 */
@Repository
public interface ForoPostRepository extends JpaRepository<Foro_post, Long> {
    
    /**
     * Encuentra los N posts más recientes del foro, ordenados por fecha de creación descendente.
     * @param pageable Objeto Pageable que define el número de resultados a devolver.
     * @return Una lista de los N posts más recientes.
     */
    List<Foro_post> findTopNByOrderByFechaCreacionDesc(Pageable pageable);
    
    /**
     * Cuenta el número de posts creados después de una fecha específica.
     * @param fecha La fecha a partir de la cual se cuentan los posts.
     * @return El número de posts creados después de la fecha especificada.
     */
    long countByFechaCreacionAfter(LocalDateTime fecha);
    
    /**
     * Cuenta el número de posts creados por un usuario específico.
     * @param userId El ID del usuario para el cual se cuentan los posts.
     * @return El número de posts creados por el usuario especificado.
     */
    int countByUsuario_Id(Long userId);

    /**
     * Encuentra los 5 posts más recientes del foro, ordenados por fecha de creación descendente.
     * @return Una lista de los 5 posts más recientes.
     */
    List<Foro_post> findTop5ByOrderByFechaCreacionDesc();

    /**
     * Elimina todos los posts de un usuario específico.
     * @param usuario El usuario cuyos posts se eliminarán.
     */
    void deleteByUsuario(UsuarioModel usuario);

    /**
     * Checks if any posts exist for a given user ID that were created after a specific date/time.
     * @param usuarioId The ID of the user to check.
     * @param fechaCreacion The date/time to check against.
     * @return true if posts exist for the given user ID and creation date/time, false otherwise.
     */
    boolean existsByUsuario_IdAndFechaCreacionAfter(Long usuarioId, LocalDateTime fechaCreacion);

    /**
     * Encuentra solo los comentarios principales (no respuestas) ordenados por fecha descendente
     * @return Lista de comentarios principales ordenados por fecha de creación descendente
     */
    @Query("SELECT f FROM Foro_post f WHERE f.comentarioPadre IS NULL ORDER BY f.fechaCreacion DESC")
    List<Foro_post> findMainComments(Pageable pageable);
    
    List<Foro_post> findByComentarioPadreIsNullOrderByFechaCreacionDesc(Pageable pageable);

    @Query("UPDATE Foro_post f SET f.meEncanta = null WHERE f.id IN (SELECT fp.id FROM Foro_post fp JOIN fp.meEncanta u WHERE u.id = :usuarioId)")
    @Modifying
    void deleteMeEncantaByUsuarioId(@Param("usuarioId") Long usuarioId);
}
