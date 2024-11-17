package com.impulsaElCambio.Repository;

import com.impulsaElCambio.Model.UsuarioModel;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;


/**
 * Repositorio para la entidad usuarioModel.
 * Esta interfaz proporciona métodos para acceder y manipular datos de usuarios en la base de datos.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<UsuarioModel, Long> {
       
    /**
     * Busca un usuario por su correo electrónico.
     * @param correo El correo electrónico del usuario a buscar.
     * @return Un Optional que contiene el usuario si se encuentra, o vacío si no.
     */
    Optional<UsuarioModel> findByCorreo(String correo);
    
    /**
     * Cuenta el número de usuarios con un rol específico.
     * @param rol El rol de los usuarios a contar.
     * @return El número de usuarios con el rol especificado.
     */
    int countByRol(UsuarioModel.Rol rol);
    

    @Query("SELECT DISTINCT u.empresa FROM UsuarioModel u WHERE u.empresa IS NOT NULL")
    List<String> findDistinctEmpresas();
    
    @Query(value = "WITH ranked_empresas AS (" +
           "SELECT empresa, COUNT(*) as total, " +
           "RANK() OVER (ORDER BY COUNT(*) DESC) as rnk " +
           "FROM usuarios " +
           "GROUP BY empresa) " +
           "SELECT empresa, total FROM ranked_empresas WHERE rnk = 1", 
           nativeQuery = true)
    List<Object[]> findEmpresaConMasUsuarios();

    
    @Query("SELECT u.empresa, COUNT(u), " +
           "(SELECT COUNT(DISTINCT p) FROM proyectoModel p JOIN p.participantes part WHERE part.empresa = u.empresa), " +
           "CAST(COUNT(u) AS float) / (SELECT COUNT(*) FROM UsuarioModel) * 100 " +
           "FROM UsuarioModel u GROUP BY u.empresa")
    List<Object[]> getDesgloseParticipacionEmpresarial();
    
    @Query("SELECT u.empresa, COUNT(DISTINCT u) FROM UsuarioModel u " +
           "JOIN u.proyectos p " +
           "WHERE u.rol = com.impulsaElCambio.Model.UsuarioModel$Rol.VOLUNTARIO " +
           "GROUP BY u.empresa ORDER BY COUNT(DISTINCT u) DESC")
    List<Object[]> contarVoluntariosEnProyectosPorEmpresa(Pageable topN);

    List<UsuarioModel> findAllByOrderByPuntosDesc(Pageable pageable);

    Optional<UsuarioModel> findByResetPasswordToken(String token);
}
