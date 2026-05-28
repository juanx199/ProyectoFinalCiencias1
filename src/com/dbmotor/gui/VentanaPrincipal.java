package com.dbmotor.gui;

import com.dbmotor.core.ArbolAVL;
import com.dbmotor.model.BaseDatos;
import com.dbmotor.model.Registro;
import com.dbmotor.model.Tabla;
import com.dbmotor.model.TipoDato;
import com.dbmotor.parser.ParserSQL;
import com.dbmotor.parser.ResultadoQuery;
import com.dbmotor.parser.InterpreteREPL;
import com.dbmotor.storage.GestorPersistencia;
import com.dbmotor.utils.GeneradorDatasets;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Random;

// Ventana Principal Swing que actúa como panel de control interactivo del motor relacional AVL.

public class VentanaPrincipal extends JFrame {
    private final BaseDatos db;
    private final GestorPersistencia pers;
    private final ParserSQL parser;

    private JComboBox<String> comboTablas;
    private VisualizadorArbol panelVisualizador;
    private JTextArea terminalOutput;
    private JTextField inputCommand;
    private JLabel labelEstadisticas;

    private Tabla tablaActiva = null;
    private final Random random = new Random();

    public VentanaPrincipal(BaseDatos db, GestorPersistencia pers, ParserSQL parser) {
        this.db = db;
        this.pers = pers;
        this.parser = parser;

        setTitle("🗄️ Motor de Base de Datos Relacional Indexado por Árbol AVL");
        setSize(1200, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        inicializarInterfaz();
        refrescarTablas();
    }

    private void inicializarInterfaz() {
        // Layout Principal: BorderLayout
        setLayout(new BorderLayout());

        // 1. Panel Superior (Selección de Tabla y Estadísticas)
        JPanel panelSuperior = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        panelSuperior.setBackground(new Color(30, 41, 59)); // Slate 800
        panelSuperior.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(51, 65, 85)));

        JLabel labelTabla = new JLabel("Tabla Activa (Índice AVL):");
        labelTabla.setForeground(Color.WHITE);
        labelTabla.setFont(new Font("SansSerif", Font.BOLD, 12));

        comboTablas = new JComboBox<>();
        comboTablas.setPreferredSize(new Dimension(180, 26));
        comboTablas.addActionListener(e -> seleccionarTablaActiva());

        labelEstadisticas = new JLabel("Registros: 0 | Altura: 0 | Balanceo: OK");
        labelEstadisticas.setForeground(new Color(148, 163, 184)); // Slate 400
        labelEstadisticas.setFont(new Font("SansSerif", Font.PLAIN, 12));

        panelSuperior.add(labelTabla);
        panelSuperior.add(comboTablas);
        panelSuperior.add(labelEstadisticas);
        add(panelSuperior, BorderLayout.NORTH);

        // 2. Panel Central (SplitPane: Izquierda = Visualizador, Derecha = Controles y
        // Terminal)
        JSplitPane splitPrincipal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPrincipal.setDividerLocation(650);
        splitPrincipal.setResizeWeight(0.6);

        // Subpanel Izquierdo: Visualizador AVL
        panelVisualizador = new VisualizadorArbol();
        panelVisualizador.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85)),
                "Topología Dinámica del Índice AVL (Clave Primaria)",
                TitledBorder.LEADING, TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 12), Color.WHITE));
        splitPrincipal.setLeftComponent(panelVisualizador);

        // Subpanel Derecho: Dividido verticalmente (Arriba = Atajos, Abajo = Terminal
        // CLI)
        JPanel panelDerecho = new JPanel(new BorderLayout());

        // Atajos Rápidos
        JPanel panelAtajos = new JPanel(new GridLayout(4, 2, 8, 8));
        panelAtajos.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                "Atajos y Generadores",
                TitledBorder.LEADING, TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 11), new Color(15, 23, 42)));
        panelAtajos.setBackground(new Color(241, 245, 249)); // Slate 100

        JButton btnCrear = new JButton("1. Inicializar Tabla 'usuarios'");
        btnCrear.addActionListener(e -> crearTablaDefecto());

        JButton btnInsertarRand = new JButton("2. Insertar Registro Aleatorio");
        btnInsertarRand.addActionListener(e -> insertarRegistroAleatorio());

        JButton btnEliminar = new JButton("3. Eliminar Clave");
        btnEliminar.addActionListener(e -> eliminarRegistroDialogo());

        JButton btnBuscar = new JButton("4. Buscar por Clave");
        btnBuscar.addActionListener(e -> buscarRegistroDialogo());

        JButton btnDataset50 = new JButton("⚡ Cargar Dataset Pequeño (50)");
        btnDataset50.addActionListener(e -> generarDataset(50));

        JButton btnDataset5000 = new JButton("🔥 Cargar Dataset Grande (5000+)");
        btnDataset5000.addActionListener(e -> generarDataset(5000));

        JButton btnLimpiar = new JButton("🧹 Limpiar Tabla Activa");
        btnLimpiar.addActionListener(e -> limpiarTablaActiva());

        JButton btnActualizar = new JButton("🔄 Recargar Árbol");
        btnActualizar.addActionListener(e -> actualizarVisualizacion());

        panelAtajos.add(btnCrear);
        panelAtajos.add(btnInsertarRand);
        panelAtajos.add(btnEliminar);
        panelAtajos.add(btnBuscar);
        panelAtajos.add(btnDataset50);
        panelAtajos.add(btnDataset5000);
        panelAtajos.add(btnLimpiar);
        panelAtajos.add(btnActualizar);

        panelDerecho.add(panelAtajos, BorderLayout.NORTH);

        // Terminal CLI Integrado
        JPanel panelTerminal = new JPanel(new BorderLayout());
        panelTerminal.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85)),
                "Consola Terminal SQL Integrada",
                TitledBorder.LEADING, TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 12), Color.WHITE));
        panelTerminal.setBackground(new Color(15, 23, 42)); // Slate 900

        terminalOutput = new JTextArea();
        terminalOutput.setBackground(Color.BLACK);
        terminalOutput.setForeground(new Color(34, 197, 94)); // Green 500 (Consola)
        terminalOutput.setFont(new Font("Courier New", Font.PLAIN, 12));
        terminalOutput.setEditable(false);
        JScrollPane scrollTerminal = new JScrollPane(terminalOutput);

        JPanel panelInput = new JPanel(new BorderLayout());
        panelInput.setBackground(Color.BLACK);

        JLabel promptLabel = new JLabel("  sql> ");
        promptLabel.setForeground(new Color(34, 197, 94));
        promptLabel.setFont(new Font("Courier New", Font.BOLD, 12));

        inputCommand = new JTextField();
        inputCommand.setBackground(Color.BLACK);
        inputCommand.setForeground(Color.WHITE);
        inputCommand.setCaretColor(Color.WHITE);
        inputCommand.setFont(new Font("Courier New", Font.PLAIN, 12));
        inputCommand.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        inputCommand.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ejecutarComandoSQL();
            }
        });

        panelInput.add(promptLabel, BorderLayout.WEST);
        panelInput.add(inputCommand, BorderLayout.CENTER);

        panelTerminal.add(scrollTerminal, BorderLayout.CENTER);
        panelTerminal.add(panelInput, BorderLayout.SOUTH);

        panelDerecho.add(panelTerminal, BorderLayout.CENTER);

        splitPrincipal.setRightComponent(panelDerecho);
        add(splitPrincipal, BorderLayout.CENTER);

        // Mensaje de Bienvenida en la Consola
        terminalOutput.append("¡Consola de base de datos relacional inicializada correctamente!\n");
        terminalOutput.append("Tip: Puedes escribir SQL-like aquí o hacer clic en los botones de atajo de arriba.\n\n");
    }

    private void refrescarTablas() {
        comboTablas.removeAllItems();
        boolean tieneTablas = false;
        for (Tabla t : db.obtenerTablas()) {
            comboTablas.addItem(t.getNombre());
            tieneTablas = true;
        }

        if (tieneTablas) {
            comboTablas.setSelectedIndex(0);
            seleccionarTablaActiva();
        } else {
            tablaActiva = null;
            panelVisualizador.setArbol(null);
            actualizarEstadisticas();
        }
    }

    private void seleccionarTablaActiva() {
        String selected = (String) comboTablas.getSelectedItem();
        if (selected != null) {
            tablaActiva = db.obtenerTabla(selected);
            panelVisualizador.setArbol(tablaActiva.getIndice());
            actualizarEstadisticas();
        }
    }

    private void actualizarEstadisticas() {
        if (tablaActiva == null) {
            labelEstadisticas.setText("Registros: 0 | Altura: 0 | Balanceo: OK");
            return;
        }
        int size = tablaActiva.obtenerTodos().size();
        int height = (tablaActiva.getIndice().getRoot() != null) ? tablaActiva.getIndice().getRoot().height : 0;
        boolean balance = tablaActiva.getIndice().verifyBalance();
        labelEstadisticas.setText("Registros: " + size + " | Altura: " + height + " | Autobalanceado AVL: "
                + (balance ? "SÍ (OK)" : "ERROR"));
    }

    private void actualizarVisualizacion() {
        panelVisualizador.repaint();
        actualizarEstadisticas();
    }

    // --- ACCIONES DE LOS BOTONES DE ATAJO ---

    private void crearTablaDefecto() {
        try {
            LinkedHashMap<String, TipoDato> esquema = new LinkedHashMap<>();
            esquema.put("id", TipoDato.INT);
            esquema.put("nombre", TipoDato.TEXT);
            esquema.put("saldo", TipoDato.REAL);
            esquema.put("activo", TipoDato.BOOLEAN);

            parser.ejecutar("CREATE TABLE usuarios (id INT PK, nombre TEXT, saldo REAL, activo BOOLEAN);");
            terminalOutput.append("✔️ Tabla 'usuarios' creada con éxito.\n");
            refrescarTablas();
        } catch (Exception e) {
            terminalOutput.append("❌ Error: " + e.getMessage() + "\n");
        }
    }

    private void insertarRegistroAleatorio() {
        if (tablaActiva == null) {
            JOptionPane.showMessageDialog(this, "Por favor, crea o selecciona una tabla primero.", "Error",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Generar datos aleatorios
        int id = random.nextInt(1000);
        // Garantizar unicidad
        while (tablaActiva.buscar(id) != null) {
            id = random.nextInt(1000);
        }

        String[] nombres = { "Juan", "Maria", "Carlos", "Sofia", "Pedro", "Ana", "Luis", "Elena", "Andres", "Clara" };
        String nombre = nombres[random.nextInt(nombres.length)] + " " + (char) ('A' + random.nextInt(26)) + ".";
        double saldo = Math.round((10 + random.nextDouble() * 1000) * 100.0) / 100.0;
        boolean activo = random.nextBoolean();

        String query;
        if (tablaActiva.getNombre().equalsIgnoreCase("usuarios")) {
            query = String.format("INSERT INTO %s VALUES (%d, '%s', %.2f, %b);", tablaActiva.getNombre(), id, nombre,
                    saldo, activo);
        } else {
            // Generalización para cualquier otra tabla creada por CLI
            StringBuilder queryB = new StringBuilder("INSERT INTO " + tablaActiva.getNombre() + " VALUES (" + id);
            boolean primero = true;
            for (String col : tablaActiva.getEsquema().keySet()) {
                if (primero) {
                    primero = false;
                    continue; // Saltar PK
                }
                TipoDato tipo = tablaActiva.getEsquema().get(col);
                if (tipo == TipoDato.TEXT) {
                    queryB.append(", 'RandText'");
                } else if (tipo == TipoDato.INT) {
                    queryB.append(", 1");
                } else if (tipo == TipoDato.REAL) {
                    queryB.append(", 1.0");
                } else if (tipo == TipoDato.BOOLEAN) {
                    queryB.append(", true");
                }
            }
            queryB.append(");");
            query = queryB.toString();
        }

        try {
            parser.ejecutar(query);
            terminalOutput.append("✔️ " + query + "\n");
            actualizarVisualizacion();
        } catch (Exception e) {
            terminalOutput.append("❌ Error al insertar: " + e.getMessage() + "\n");
        }
    }

    private void eliminarRegistroDialogo() {
        if (tablaActiva == null)
            return;
        String val = JOptionPane.showInputDialog(this,
                "Ingrese la clave primaria (ID entero) del registro a eliminar:");
        if (val == null || val.trim().isEmpty())
            return;

        try {
            int key = Integer.parseInt(val.trim());
            String query = "DELETE FROM " + tablaActiva.getNombre() + " WHERE " + tablaActiva.getClavePrimaria() + " = "
                    + key + ";";
            ResultadoQuery res = parser.ejecutar(query);
            terminalOutput.append("✔️ " + res.getMensaje() + "\n");
            actualizarVisualizacion();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error al Eliminar",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void buscarRegistroDialogo() {
        if (tablaActiva == null)
            return;
        String val = JOptionPane.showInputDialog(this,
                "Ingrese el ID del registro a buscar (Exacto o Rango separado por coma min,max):");
        if (val == null || val.trim().isEmpty())
            return;

        try {
            String query;
            if (val.contains(",")) {
                String[] rango = val.split(",");
                query = "SELECT * FROM " + tablaActiva.getNombre() + " WHERE " + tablaActiva.getClavePrimaria()
                        + " BETWEEN " + rango[0].trim() + " AND " + rango[1].trim() + ";";
            } else {
                query = "SELECT * FROM " + tablaActiva.getNombre() + " WHERE " + tablaActiva.getClavePrimaria() + " = "
                        + val.trim() + ";";
            }

            long start = System.nanoTime();
            ResultadoQuery res = parser.ejecutar(query);
            long elapsed = System.nanoTime() - start;

            terminalOutput.append("✔️ Ejecutado: " + query + "\n");

            // Capturar la impresión ASCII redirigiéndola a la terminal gráfica!
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            PrintStream viejo = System.out;
            System.setOut(ps);

            InterpreteREPL.imprimirResultado(res, elapsed);

            System.out.flush();
            System.setOut(viejo);

            terminalOutput.append(baos.toString());
            terminalOutput.append("\n");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al buscar: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void generarDataset(int cantidad) {
        if (tablaActiva == null) {
            // Inicializar por defecto
            crearTablaDefecto();
        }

        terminalOutput.append("⏳ Generando dataset masivo de " + cantidad + " registros...\n");

        try {
            long start = System.nanoTime();
            if (cantidad == 50) {
                GeneradorDatasets.generarEInsertar(tablaActiva, 50);
                terminalOutput
                        .append("✔️ Dataset de 50 registros cargado. El Árbol AVL ha sido balanceado en memoria.\n");
            } else {
                GeneradorDatasets.generarEInsertar(tablaActiva, 5000);
                terminalOutput.append("✔️ Dataset mediano de 5,000+ registros cargado en memoria exitosamente.\n");

                // Ejecutar benchmark comparativo
                terminalOutput.append("📊 Iniciando Benchmark Comparativo de Búsqueda:\n");
                String metricas = GeneradorDatasets.ejecutarBenchmark(tablaActiva);
                terminalOutput.append(metricas + "\n");
            }
            pers.guardarTabla(tablaActiva); // Sincronizar a disco
            long total = System.nanoTime() - start;
            terminalOutput
                    .append(String.format("⏱️ Tiempo total de inserción y guardado: %.2f ms\n\n", total / 1_000_000.0));
            actualizarVisualizacion();
        } catch (Exception e) {
            terminalOutput.append("❌ Error en Dataset: " + e.getMessage() + "\n");
        }
    }

    private void limpiarTablaActiva() {
        if (tablaActiva == null)
            return;
        int opt = JOptionPane.showConfirmDialog(this,
                "¿Está seguro que desea eliminar todos los registros de la tabla '" + tablaActiva.getNombre() + "'?",
                "Confirmar Limpieza", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            try {
                // Instanciar un árbol AVL vacío
                Tabla nueva = new Tabla(tablaActiva.getNombre(), tablaActiva.getEsquema(),
                        tablaActiva.getClavePrimaria());
                db.eliminarTabla(tablaActiva.getNombre());
                db.registrarTabla(nueva);
                pers.guardarTabla(nueva);
                refrescarTablas();
                terminalOutput.append("✔️ Tabla '" + nueva.getNombre() + "' vaciada con éxito.\n");
            } catch (Exception e) {
                terminalOutput.append("❌ Error al limpiar: " + e.getMessage() + "\n");
            }
        }
    }

    // --- ACCIÓN DE LA CONSOLA CLI ---

    private void ejecutarComandoSQL() {
        String sql = inputCommand.getText().trim();
        if (sql.isEmpty())
            return;

        inputCommand.setText("");
        terminalOutput.append("avl-db> " + sql + "\n");

        try {
            long start = System.nanoTime();
            ResultadoQuery res = parser.ejecutar(sql);
            long elapsed = System.nanoTime() - start;

            // Redirigir tabla ASCII a la terminal gráfica
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            PrintStream viejo = System.out;
            System.setOut(ps);

            InterpreteREPL.imprimirResultado(res, elapsed);

            System.out.flush();
            System.setOut(viejo);

            terminalOutput.append(baos.toString());
            terminalOutput.append("\n");

            // Si es un comando de definición (CREATE TABLE o DROP TABLE), refrescar
            // catálogo
            String upperSql = sql.toUpperCase();
            if (upperSql.contains("CREATE TABLE") || upperSql.contains("DROP TABLE")) {
                refrescarTablas();
            } else {
                actualizarVisualizacion();
            }

        } catch (Exception e) {
            terminalOutput.append("❌ ERROR: " + e.getMessage() + "\n\n");
        }
    }
}
