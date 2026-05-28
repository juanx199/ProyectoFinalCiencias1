package com.dbmotor;

import com.dbmotor.model.BaseDatos;
import com.dbmotor.parser.InterpreteREPL;
import com.dbmotor.parser.ParserSQL;
import com.dbmotor.storage.GestorPersistencia;
import com.dbmotor.gui.VentanaPrincipal;

import javax.swing.*;
import java.io.File;

/**
 * Clase principal (Entry Point) del Motor de Base de Datos AVL.
 * Arranca la base de datos, carga la persistencia física en disco y
 * lanza la interfaz de usuario correspondiente (GUI Swing o CLI REPL).
 */
public class Main {

    public static void main(String[] args) {
        // 1. Definir directorio de persistencia física (dentro del espacio de trabajo)
        String workspaceRoot = System.getProperty("user.home") 
                + File.separator + ".gemini" + File.separator + "antigravity" 
                + File.separator + "scratch" + File.separator + "db-motor-avl";
        String directorioDatos = workspaceRoot + File.separator + "data";

        System.out.println("🗄️ Inicializando Motor de Base de Datos AVL...");
        System.out.println("📂 Ruta física de almacenamiento: " + directorioDatos);

        // 2. Instanciar núcleo del motor, persistencia y parser
        BaseDatos db = new BaseDatos();
        GestorPersistencia persistencia = new GestorPersistencia(directorioDatos);
        ParserSQL parser = new ParserSQL(db, persistencia);

        // 3. Cargar y reconstruir bases de datos existentes desde archivos CSV
        try {
            System.out.println("⏳ Cargando y reconstruyendo índices AVL desde disco...");
            persistencia.cargarBaseDatos(db);
            System.out.println("✔️ Carga inicial de datos finalizada.");
        } catch (Exception e) {
            System.err.println("⚠️ Error al cargar archivos de persistencia: " + e.getMessage());
        }

        // 4. Analizar argumentos para decidir el modo de ejecución
        boolean modoConsola = false;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("--console") || arg.equalsIgnoreCase("-c")) {
                modoConsola = true;
                break;
            }
        }

        if (modoConsola) {
            // Iniciar en modo Consola REPL
            InterpreteREPL repl = new InterpreteREPL(parser);
            repl.iniciar();
        } else {
            // Iniciar en modo Interfaz Gráfica (Swing GUI)
            System.out.println("🖥️ Iniciando interfaz de usuario Swing...");
            try {
                // Configurar Look & Feel del Sistema para un estilo nativo y premium
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                // Caída silenciosa a Look & Feel por defecto
            }

            // Ejecutar la creación de Swing en el Event Dispatch Thread (Thread-Safe)
            SwingUtilities.invokeLater(() -> {
                try {
                    VentanaPrincipal ventana = new VentanaPrincipal(db, persistencia, parser);
                    ventana.setVisible(true);
                    System.out.println("✔️ Interfaz Gráfica lista.");
                } catch (Exception e) {
                    System.err.println("❌ Error crítico al lanzar la interfaz gráfica: " + e.getMessage());
                    e.printStackTrace();
                    System.out.println("\n🔄 Intentando iniciar consola REPL de respaldo...");
                    new InterpreteREPL(parser).iniciar();
                }
            });
        }
    }
}
