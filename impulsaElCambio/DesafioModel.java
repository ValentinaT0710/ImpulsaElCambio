package com.impulsaElCambio.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonManagedReference;

@Entity
@Table(name = "desafios")
public class DesafioModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int puntosRecompensa;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private boolean activo;
    private String nombre;

    @ManyToMany
    @JoinTable(
        name = "desafio_participantes",
        joinColumns = @JoinColumn(name = "desafio_id"),
        inverseJoinColumns = @JoinColumn(name = "usuario_id")
    )
    @JsonManagedReference("desafio-participantes")
    private Set<UsuarioModel> participantes = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "desafio_completados",
        joinColumns = @JoinColumn(name = "desafio_id"),
        inverseJoinColumns = @JoinColumn(name = "usuario_id")
    )
    @JsonManagedReference("desafio-completados")
    private Set<UsuarioModel> usuariosCompletados = new HashSet<>();

    public enum TipoDesafio {
        PUBLICAR_FORO("Publicar en el Foro"),
        UNIRSE_PROYECTO("Unirse a un Proyecto");

        private final String descripcion;

        TipoDesafio(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    @Enumerated(EnumType.STRING)
    private TipoDesafio tipoDesafio;

    private Long proyectoId;

    @ManyToOne
    @JoinColumn(name = "proyecto_id")
    private proyectoModel proyecto;

    // Constructor
    public DesafioModel() {
    }

    public DesafioModel(int puntosRecompensa, LocalDateTime fechaInicio, LocalDateTime fechaFin, boolean activo) {
        this.puntosRecompensa = puntosRecompensa;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.activo = activo;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public int getPuntosRecompensa() {
        return puntosRecompensa;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public boolean isActivo() {
        return activo;
    }

    public Set<UsuarioModel> getParticipantes() {
        return participantes;
    }

    public Set<UsuarioModel> getUsuariosCompletados() {
        return usuariosCompletados;
    }

    public TipoDesafio getTipoDesafio() {
        return tipoDesafio;
    }

    public Long getProyectoId() {
        return proyectoId;
    }

    public proyectoModel getProyecto() {
        return proyecto;
    }

    public String getNombre() {
        return nombre;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setPuntosRecompensa(int puntosRecompensa) {
        this.puntosRecompensa = puntosRecompensa;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public void setFechaFin(LocalDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public void setParticipantes(Set<UsuarioModel> participantes) {
        this.participantes = participantes;
    }

    public void setUsuariosCompletados(Set<UsuarioModel> usuariosCompletados) {
        this.usuariosCompletados = usuariosCompletados;
    }

    public void setTipoDesafio(TipoDesafio tipoDesafio) {
        this.tipoDesafio = tipoDesafio;
    }

    public void setProyectoId(Long proyectoId) {
        this.proyectoId = proyectoId;
    }

    public void setProyecto(proyectoModel proyecto) {
        this.proyecto = proyecto;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public boolean estaActivo() {
        LocalDateTime ahora = LocalDateTime.now();
        return activo && ahora.isAfter(fechaInicio) && ahora.isBefore(fechaFin);
    }

    public boolean puedeParticipar(UsuarioModel usuario) {
        return estaActivo() && 
               !participantes.contains(usuario) && 
               !usuariosCompletados.contains(usuario);
    }

    public String getDescripcion() {
        return tipoDesafio != null ? tipoDesafio.getDescripcion() : "";
    }
}
    