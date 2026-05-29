package com.dbmotor.gui;

import com.dbmotor.core.ArbolAVL;
import com.dbmotor.core.NodoAVL;
import com.dbmotor.model.Registro;

import javax.swing.*;
import java.awt.*;

// Componente gráfico personalizado (JPanel) que dibuja interactivamente la topología del árbol AVL.

public class VisualizadorArbol extends JPanel {
    private ArbolAVL<Registro> arbol;

    public VisualizadorArbol() {
        this.arbol = null;
        // Fondo Charcoal/Oscuro premium
        setBackground(new Color(15, 23, 42)); // Slate 900
    }

    // Setea el árbol AVL actual y refresca la visualización.

    public void setArbol(ArbolAVL<Registro> arbol) {
        this.arbol = arbol;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        // Anti-Aliasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (arbol == null || arbol.getRoot() == null) {
            g2d.setColor(new Color(148, 163, 184)); // Slate 400
            g2d.setFont(new Font("SansSerif", Font.ITALIC, 16));
            String msg = "Árbol AVL Vacío o Sin Índice Activo";
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
            return;
        }

        // Dibuja la topología del árbol a partir de la raíz
        int xInicial = getWidth() / 2;
        int yInicial = 50;
        int offsetInicial = Math.max(getWidth() / 4, 80); // Ajustar dinámicamente según el ancho

        dibujarArbol(g2d, arbol.getRoot(), xInicial, yInicial, offsetInicial);
    }

    /**
     * Dibuja recursivamente el nodo AVL actual, conectando las líneas a los hijos
     * primero.
     */
    private void dibujarArbol(Graphics2D g2d, NodoAVL<Registro> nodo, int x, int y, int xOffset) {
        if (nodo == null)
            return;

        // 1. Dibujar conexiones con los hijos
        int yHijo = y + 70;

        if (nodo.left != null) {
            g2d.setColor(new Color(71, 85, 105)); // Slate 600
            g2d.setStroke(new BasicStroke(2.0f));
            g2d.drawLine(x, y, x - xOffset, yHijo);
            dibujarArbol(g2d, nodo.left, x - xOffset, yHijo, Math.max(xOffset / 2, 15));
        }

        if (nodo.right != null) {
            g2d.setColor(new Color(71, 85, 105)); // Slate 600
            g2d.setStroke(new BasicStroke(2.0f));
            g2d.drawLine(x, y, x + xOffset, yHijo);
            dibujarArbol(g2d, nodo.right, x + xOffset, yHijo, Math.max(xOffset / 2, 15));
        }

        // 2. Dibujar el nodo actual
        int radio = 20;

        // Gradiente pa que se vea bonito
        GradientPaint gradiente = new GradientPaint(
                x - radio, y - radio, new Color(14, 165, 233), // Sky 500
                x + radio, y + radio, new Color(3, 105, 161) // Sky 700
        );
        g2d.setPaint(gradiente);
        g2d.fillOval(x - radio, y - radio, radio * 2, radio * 2);

        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawOval(x - radio, y - radiusToDiameterOffset(radio), radio * 2, radio * 2);

        // Clave Primaria (Integer) dentro del nodo
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
        String llaveStr = String.valueOf(nodo.key);
        FontMetrics fm = g2d.getFontMetrics();
        int tx = x - fm.stringWidth(llaveStr) / 2;
        int ty = y + fm.getAscent() / 2 - 2;
        g2d.drawString(llaveStr, tx, ty);

        // 3. Dibujar Factor de Balanceo BF y Altura H arriba del nodo
        int hIzq = (nodo.left != null) ? nodo.left.height : 0;
        int hDer = (nodo.right != null) ? nodo.right.height : 0;
        int bf = hIzq - hDer;

        // Color según balanceo: Verde=Perfecto, Amarillo=Ligeramente desbalanceado,
        // Rojo=Crítico (nunca en AVL)
        Color colorBf = new Color(74, 222, 128); // Green 400
        if (Math.abs(bf) == 1) {
            colorBf = new Color(250, 204, 21); // Yellow 400
        } else if (Math.abs(bf) > 1) {
            colorBf = new Color(248, 113, 113); // Red 400
        }

        g2d.setColor(colorBf);
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 10));
        String metadataStr = "bf:" + bf + " h:" + nodo.height;
        int mx = x - g2d.getFontMetrics().stringWidth(metadataStr) / 2;
        g2d.drawString(metadataStr, mx, y - radio - 5);
    }

    private int radiusToDiameterOffset(int radio) {
        return radio;
    }
}
