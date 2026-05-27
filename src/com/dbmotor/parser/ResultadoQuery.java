package com.dbmotor.parser;

import com.dbmotor.model.Registro;
import com.dbmotor.model.TipoDato;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Representa el resultado unificado de la ejecución de una consulta.
 * Puede contener datos tabulares, mensajes de éxito o métricas de rendimiento del árbol AVL.
 */
public class ResultadoQuery {
    private final LinkedHashMap<String, TipoDato> esquema;
    private final List<Registro> filas;
    private final String mensaje;
    private String metricaRendimiento;

    public ResultadoQuery(LinkedHashMap<String, TipoDato> esquema, List<Registro> filas, String mensaje) {
        this.esquema = esquema != null ? esquema : new LinkedHashMap<>();
        this.filas = filas != null ? filas : new ArrayList<>();
        this.mensaje = mensaje;
        this.metricaRendimiento = "N/A";
    }

    public static ResultadoQuery exito(String mensaje) {
        return new ResultadoQuery(null, null, mensaje);
    }

    public static ResultadoQuery vacio(String mensaje) {
        return new ResultadoQuery(null, null, mensaje);
    }

    public LinkedHashMap<String, TipoDato> getEsquema() {
        return esquema;
    }

    public List<Registro> getFilas() {
        return filas;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getMetricaRendimiento() {
        return metricaRendimiento;
    }

    public void setMetricaRendimiento(String metricaRendimiento) {
        this.metricaRendimiento = metricaRendimiento;
    }

    public boolean tieneFilas() {
        return filas != null && !filas.isEmpty();
    }
}
