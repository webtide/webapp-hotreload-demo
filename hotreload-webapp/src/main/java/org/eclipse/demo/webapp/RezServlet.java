package org.eclipse.demo.webapp;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class RezServlet extends HttpServlet
{
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        resp.setStatus(200);
        resp.setCharacterEncoding("utf-8");
        resp.setContentType("text/plain");
        PrintWriter out = resp.getWriter();
        out.printf("servletContext.attr[Rez]=%s%n", getServletContext().getAttribute("Rez"));
    }
}
