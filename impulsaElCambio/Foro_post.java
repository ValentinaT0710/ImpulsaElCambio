package com.impulsaElCambio.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Entity
@Table(name = "foro_post")
public class Foro_post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String contenido;
    private LocalDateTime fechaCreacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "posts"})
    private UsuarioModel usuario;

    private String insigniasUsuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comentario_padre_id")
    private Foro_post comentarioPadre;

    @OneToMany(mappedBy = "comentarioPadre", cascade = CascadeType.ALL)
    private List<Foro_post> respuestas = new ArrayList<>();

    @ManyToMany
    @JoinTable(
        name = "me_encanta_comentarios",
        joinColumns = @JoinColumn(name = "comentario_id"),
        inverseJoinColumns = @JoinColumn(name = "usuario_id")
    )
    private Set<UsuarioModel> meEncanta = new HashSet<>();

    private int contadorMeEncanta = 0;



    // Constructor por defecto (necesario para JPA)
    public Foro_post() {
    }

    // Constructor con contenido y fechaCreacion
    public Foro_post(String contenido, LocalDateTime fechaCreacion) {
        this.contenido = contenido;
        this.fechaCreacion = fechaCreacion;
    }

    // Constructor solo con contenido (establece fechaCreacion como el tiempo actual)
    public Foro_post(String contenido) {
        this.contenido = contenido;
        this.fechaCreacion = LocalDateTime.now();
    }

    // Getters y setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public UsuarioModel getUsuario() {
        return usuario;
    }

    public void setUsuario(UsuarioModel usuario) {
        this.usuario = usuario;
    }

    // Add this method
    public Long getUsuarioId() {
        return usuario != null ? usuario.getId() : null;
    }

    // Add this method
    public String getUsuarioNombre() {
        return usuario != null ? usuario.getNombre() : null;
    }

    public void setInsigniasUsuario(String insignias) {
        this.insigniasUsuario = insignias;
    }

    public String getInsigniasUsuario() {
        return this.insigniasUsuario;
    }

    public Foro_post getComentarioPadre() {
        return comentarioPadre;
    }

    public void setComentarioPadre(Foro_post comentarioPadre) {
        this.comentarioPadre = comentarioPadre;
    }

    public List<Foro_post> getRespuestas() {
        return respuestas;
    }

    public Set<UsuarioModel> getMeEncanta() {
        return meEncanta;
    }

    public void setMeEncanta(Set<UsuarioModel> meEncanta) {
        this.meEncanta = meEncanta;
    }

    public int getContadorMeEncanta() {
        return contadorMeEncanta;
    }

    public void setContadorMeEncanta(int contadorMeEncanta) {
        this.contadorMeEncanta = contadorMeEncanta;
    }
}
