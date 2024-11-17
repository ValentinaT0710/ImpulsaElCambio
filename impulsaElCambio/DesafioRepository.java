package com.impulsaElCambio.Repository;

import com.impulsaElCambio.Model.DesafioModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DesafioRepository extends JpaRepository<DesafioModel, Long> {
    
    @Query("SELECT d FROM DesafioModel d WHERE d.fechaFin >= :now AND d.activo = true")
    List<DesafioModel> findActiveDesafios(@Param("now") LocalDateTime now);
    
    @Query("SELECT d FROM DesafioModel d WHERE d.fechaFin < :ahora")
    List<DesafioModel> findExpiredDesafios(@Param("ahora") LocalDateTime ahora);
    
    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END FROM DesafioModel d " +
           "JOIN d.usuariosCompletados u " +
           "WHERE d.id = :desafioId AND u.id = :usuarioId")
    boolean hasUserCompletedDesafio(@Param("desafioId") Long desafioId, @Param("usuarioId") Long usuarioId);
    
    @Query("SELECT d FROM DesafioModel d JOIN d.usuariosCompletados u WHERE u.id = :usuarioId")
    List<DesafioModel> findCompletedDesafiosByUsuario(@Param("usuarioId") Long usuarioId);
} 