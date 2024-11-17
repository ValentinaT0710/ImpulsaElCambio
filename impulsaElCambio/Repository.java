package com.impulsaElCambio.Repository;

import com.impulsaElCambio.Model.proyectoModel;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.lang.NonNull;
import org.springframework.data.domain.Pageable;



/**
 * Repositorio para la entidad proyectoModel.
 * Esta interfaz proporciona métodos para acceder y manipular datos de proyectos en la base de datos.
 */
public interface Repository extends JpaRepository<proyectoModel, Long> {

    /**
     * Cuenta el número de proyectos con un estado específico.
     * @param estado El estado de los proyectos a contar.
     * @return El número de proyectos con el estado especificado.
     */
    long countByEstado(String estado);
    /**
     * Cuenta el número de participaciones en proyectos por un usuario.
     * @param userId El ID del usuario.
     * @return El número de participaciones en proyectos del usuario especificado.
     */
    @Query("SELECT COUNT(p) FROM proyectoModel p JOIN p.participantes u WHERE u.id = :userId")
    int countProjectParticipationByUserId(@Param("userId") Long userId);
    /**
     * Elimina un proyecto por su ID.
     * @param id El ID del proyecto a eliminar.
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM proyectoModel p WHERE p.id = :id")
    void deleteById(@Param("id") @NonNull Long id);

    @Query("SELECT DISTINCT p.id FROM proyectoModel p JOIN p.participantes u WHERE u.id = :usuarioId")
    Set<Long> findProyectoIdsByParticipanteId(@Param("usuarioId") Long usuarioId);
    
    boolean existsByParticipantesIdAndFechaCreacionAfter(Long usuarioId, LocalDateTime fecha);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM proyectoModel p " +
           "JOIN p.participantes u " +
           "WHERE p.id = :proyectoId AND u.id = :usuarioId")
    boolean existsByIdAndParticipantesId(@Param("proyectoId") Long proyectoId, 
                                        @Param("usuarioId") Long usuarioId);

    @Query("SELECT p FROM proyectoModel p WHERE p.estado = 'activo' AND " +
           "(p.fechaExpiracion IS NULL OR p.fechaExpiracion > CURRENT_TIMESTAMP)")
    List<proyectoModel> findActiveProjects();

    @Query("SELECT p FROM proyectoModel p ORDER BY SIZE(p.participantes) DESC")
    List<proyectoModel> findAllByOrderByParticipantesDesc(Pageable pageable);
}

