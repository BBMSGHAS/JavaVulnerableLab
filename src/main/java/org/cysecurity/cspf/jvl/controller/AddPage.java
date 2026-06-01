package org.cysecurity.cspf.jvl.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AddPage extends HttpServlet {

    private static final int MAX_CONTENT_LENGTH = 10_000;
    private static final String SAFE_FILENAME_REGEX = "^[a-zA-Z0-9._-]+\\.txt$";

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");

        String fileName = request.getParameter("filename");
        String content = request.getParameter("content");

        if (fileName == null || content == null || fileName.isBlank()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "filename or content parameter is missing");
            return;
        }

        if (!fileName.matches(SAFE_FILENAME_REGEX)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid filename. Only .txt files are allowed.");
            return;
        }

        if (content.length() > MAX_CONTENT_LENGTH) {
            response.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Content too large.");
            return;
        }

        try {
            String pagesRealPath = getServletContext().getRealPath("/pages");
            if (pagesRealPath == null) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Pages directory unavailable.");
                return;
            }

            Path pagesDir = Paths.get(pagesRealPath).toAbsolutePath().normalize();
            Files.createDirectories(pagesDir);

            Path targetFile = pagesDir.resolve(fileName).normalize();
            if (!targetFile.startsWith(pagesDir)) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file path.");
                return;
            }

            if (Files.exists(targetFile)) {
                response.sendError(HttpServletResponse.SC_CONFLICT, "File already exists.");
                return;
            }

            Files.writeString(targetFile, content, StandardCharsets.UTF_8);

            String safeFileName = escapeHtml(fileName);

            response.setStatus(HttpServletResponse.SC_CREATED);
            try (PrintWriter out = response.getWriter()) {
                out.print("Successfully created the file: <a href='../pages/" 
                        + safeFileName + "'>" + safeFileName + "</a>");
            }

        } catch (InvalidPathException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid path.");
        } catch (IOException e) {
            log("Failed to create page", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to create file.");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "GET not supported.");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    public String getServletInfo() {
        return "Creates text pages safely";
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}
