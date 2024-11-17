package com.impulsaElCambio.Controller;

import com.impulsaElCambio.Model.UsuarioModel;
import com.impulsaElCambio.Servicios.RankingService;




import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;


@Controller
public class RankingController {
    @Autowired
    private RankingService rankingService;

    @GetMapping("/ranking-list")
    public String mostrarRanking(Model model) {
        Map<String, Object> estadisticas = rankingService.obtenerEstadisticasRanking();
        
        @SuppressWarnings("unchecked")
        List<UsuarioModel> topUsuarios = (List<UsuarioModel>) estadisticas.getOrDefault("topUsuarios", new ArrayList<>());
        List<Map<String, Object>> rankingData = topUsuarios.stream().map(usuario -> {
            Map<String, Object> userData = new HashMap<>();
            userData.put("nombre", usuario.getNombre());
            userData.put("puntos", usuario.getPuntos());
            userData.put("insignias", usuario.getInsignias());
            userData.put("proyectosCompletados", rankingService.obtenerProyectosCompletados(usuario));
            return userData;
        }).collect(Collectors.toList());
        
        model.addAttribute("topUsuarios", rankingData);
        return "ranking";
    }
}
