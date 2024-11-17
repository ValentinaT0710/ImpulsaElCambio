package com.impulsaElCambio.Servicios;

import com.impulsaElCambio.Model.UsuarioModel;
import com.impulsaElCambio.Repository.UsuarioRepository;
import com.impulsaElCambio.Repository.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;


import java.util.*;
import java.util.stream.Collectors;

@Service
public class RankingService {

    @Autowired
    private UsuarioRepository usuarioRepo;
    
    @Autowired
    private Repository proyectoRepo;

    /**
     * Obtiene el ranking general de usuarios basado en puntos
     * @param limit Número máximo de usuarios a retornar
     * @return Lista de usuarios ordenada por puntos
     */
    public List<Map<String, Object>> obtenerRankingGeneral(int limit) {
        List<UsuarioModel> topUsuarios = usuarioRepo.findAllByOrderByPuntosDesc(PageRequest.of(0, limit));
        
        return topUsuarios.stream().map(usuario -> {
            Map<String, Object> rankingData = new HashMap<>();
            rankingData.put("id", usuario.getId());
            rankingData.put("nombre", usuario.getNombre());
            rankingData.put("puntos", usuario.getPuntos());
            rankingData.put("insignias", usuario.getInsigniasList());
            rankingData.put("proyectosCompletados", obtenerProyectosCompletados(usuario));
            rankingData.put("empresa", usuario.getEmpresa());
            return rankingData;
        }).collect(Collectors.toList());
    }

    /**
     * Obtiene el ranking por empresa
     * @param empresa Nombre de la empresa
     * @param limit Número máximo de usuarios a retornar
     * @return Lista de usuarios de la empresa ordenada por puntos
     */
    public List<Map<String, Object>> obtenerRankingPorEmpresa(String empresa, int limit) {
        // Implementar lógica específica para ranking por empresa si es necesario
        return obtenerRankingGeneral(limit).stream()
            .filter(userData -> empresa.equals(userData.get("empresa")))
            .collect(Collectors.toList());
    }

    /**
     * Calcula el número de proyectos completados por un usuario
     */
    public int obtenerProyectosCompletados(UsuarioModel usuario) {
        return (int) usuario.getProyectos().stream()
                .filter(proyecto -> "completado".equals(proyecto.getEstado()))
                .count();
    }

    /**
     * Obtiene estadísticas generales del ranking
     */
    public Map<String, Object> obtenerEstadisticasRanking() {
        Map<String, Object> estadisticas = new HashMap<>();
        
        // Total de voluntarios activos
        estadisticas.put("totalVoluntarios", usuarioRepo.countByRol(UsuarioModel.Rol.VOLUNTARIO));
        
        // Total de proyectos activos
        estadisticas.put("proyectosActivos", proyectoRepo.countByEstado("activo"));
        
        // Obtener top usuarios
        List<UsuarioModel> topUsuarios = usuarioRepo.findAllByOrderByPuntosDesc(PageRequest.of(0, 10));
        estadisticas.put("topUsuarios", topUsuarios);
        
        // Top empresas por participación
        List<Object[]> topEmpresas = usuarioRepo.contarVoluntariosEnProyectosPorEmpresa(PageRequest.of(0, 5));
        List<Map<String, Object>> empresasRanking = topEmpresas.stream()
            .map(empresa -> {
                Map<String, Object> empresaData = new HashMap<>();
                empresaData.put("nombre", empresa[0]);
                empresaData.put("voluntarios", empresa[1]);
                return empresaData;
            })
            .collect(Collectors.toList());
        
        estadisticas.put("topEmpresas", empresasRanking);
        
        return estadisticas;
    }
}
