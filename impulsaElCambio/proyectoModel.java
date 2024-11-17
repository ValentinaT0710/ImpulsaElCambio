package com.impulsaElCambio.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static jakarta.persistence.GenerationType.*;

/**
 * Representa un proyecto en el sistema.
 * Esta clase es una entidad JPA que se mapea a la tabla "proyectos" en la base de datos.
 */
@Entity
@Table(name = "proyectos")
public class proyectoModel {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    private String nombre;
    private String descripcion;
    private String imagenUrl;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaExpiracion;

    @Column(nullable = false)
    private String estado = "activo";

    /**
     * Relación muchos a muchos con la entidad usuarioModel.
     * Representa los participantes del proyecto.
     */
   @ManyToMany
    @JoinTable( 
        name = "proyecto_participantes",
        joinColumns = @JoinColumn(name = "proyecto_id"),
        inverseJoinColumns = @JoinColumn(name = "usuario_id")
    )
    private Set<UsuarioModel> participantes = new HashSet<>();

    /**
     * Relación uno a muchos con la entidad desafioModel.
     * Representa los desafíos del proyecto.
     */
    @OneToMany(mappedBy = "proyecto", fetch = FetchType.LAZY)
    private Set<DesafioModel> desafios = new HashSet<>();

    /**
     * Constructor por defecto requerido por JPA.
     */
    public proyectoModel() {
    }

    /**
     * Constructor con todos los campos.
     * @param id Identificador único del proyecto.
     * @param nombre Nombre del proyecto.
     * @param descripcion Descripción del proyecto.
     * @param imagenUrl URL de la imagen asociada al proyecto.
     * @param fechaCreacion Fecha y hora de creación del proyecto.
     * @param participantes Lista de usuarios participantes en el proyecto.
     */
    public proyectoModel(Long id, String nombre, String descripcion, String imagenUrl, LocalDateTime fechaCreacion, Set<UsuarioModel> participantes) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.imagenUrl = imagenUrl;
        this.fechaCreacion = fechaCreacion;
        this.participantes = participantes;
    }

    // Getters y setters

    /**
     * Obtiene el ID del proyecto.
     * @return El ID del proyecto.
     */
    public Long getId() {
        return id;
    }

    /**
     * Establece el ID del proyecto.
     * @param id El ID a establecer.
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Obtiene el nombre del proyecto.
     * @return El nombre del proyecto.
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Establece el nombre del proyecto.
     * @param nombre El nombre a establecer.
     */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /**
     * Obtiene la descripción del proyecto.
     * @return La descripción del proyecto.
     */
    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Establece la descripción del proyecto.
     * @param descripcion La descripción a establecer.
     */
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    /**
     * Obtiene la URL de la imagen del proyecto.
     * @return La URL de la imagen del proyecto.
     */
    public String getImagenUrl() {
        return imagenUrl;
    }

    /**
     * Establece la URL de la imagen del proyecto.
     * @param imagenUrl La URL de la imagen a establecer.
     */
    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    /**
     * Obtiene la fecha de creación del proyecto.
     * @return La fecha de creación del proyecto.
     */
    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    /**
     * Establece la fecha de creación del proyecto.
     * @param fechaCreacion La fecha de creación a establecer.
     */
    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    /**
     * Obtiene la fecha de expiración del proyecto.
     * @return La fecha de expiración del proyecto.
     */
    public LocalDateTime getFechaExpiracion() {
        return fechaExpiracion;
    }

    /**
     * Establece la fecha de expiración del proyecto.
     * @param fechaExpiracion La fecha de expiración a establecer.
     */
    public void setFechaExpiracion(LocalDateTime fechaExpiracion) {
        this.fechaExpiracion = fechaExpiracion;
    }

    /**
     * Obtiene el estado del proyecto.
     * @return El estado del proyecto.
     */
    public String getEstado() {
        return estado;
    }

    /**
     * Establece el estado del proyecto.
     * @param estado El estado a establecer.
     */
    public void setEstado(String estado) {
        this.estado = estado;
    }

    /**
     * Obtiene la lista de participantes del proyecto.
     * @return La lista de participantes del proyecto.
     */
    public Set<UsuarioModel> getParticipantes() {
        return participantes;
    }

    /**
     * Establece la lista de participantes del proyecto.
     * @param participantes La lista de participantes a establecer.
     */
    public void setParticipantes(Set<UsuarioModel> participantes) {
        this.participantes = participantes;
    }

    /**
     * Añade un participante al proyecto.
     * @param participante El usuario a añadir como participante.
     */
    public void addParticipante(UsuarioModel participante) {
        this.participantes.add(participante);
    }

    /**
     * Obtiene el número de participantes del proyecto.
     * @return El número de participantes del proyecto.
     */
    public int getNumeroParticipantes() {
        return this.participantes.size();
    }

    /**
     * Verifica si el proyecto ha expirado.
     * @return Verdadero si el proyecto ha expirado, falso en caso contrario.
     */
    public boolean isExpirado() {
        return this.fechaExpiracion != null && LocalDateTime.now().isAfter(this.fechaExpiracion);
    }

    public boolean isActivo(LocalDateTime referenceTime) {
        return fechaExpiracion == null || fechaExpiracion.isAfter(referenceTime);
    }

    /**
     * Obtiene la lista de desafíos del proyecto.
     * @return La lista de desafíos del proyecto.
     */
    public Set<DesafioModel> getDesafios() {
        return desafios;
    }
}
