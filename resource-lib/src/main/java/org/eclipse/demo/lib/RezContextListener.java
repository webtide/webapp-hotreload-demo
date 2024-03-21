package org.eclipse.demo.lib;

import java.io.IOException;
import java.io.InputStream;

import com.github.fge.msgsimple.bundle.MessageBundle;
import com.github.fge.msgsimple.bundle.PropertiesBundle;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class RezContextListener implements ServletContextListener
{
    private String rez;
    private static final MessageBundle BUNDLE
        = PropertiesBundle.forPath("/com/github/fge/jackson/jsonNodeReader");

    @Override
    public void contextInitialized(ServletContextEvent sce)
    {
        System.err.println("### " + this.getClass().getName() + ".contextInitialized()");
        try
        {
            InputStream in = sce.getServletContext().getResourceAsStream("/WEB-INF/resources/foo.txt");
            StringBuilder str = new StringBuilder();
            boolean done = false;
            int c;
            while (!done)
            {
                c = in.read();

                // Only read the first line, leaving the InputStream not at EOF
                if (c == -1 || c == '\n' || c == '\r')
                    done = true;
                else
                    str.append(Character.toChars(c));
            }
            rez = str.toString();
            sce.getServletContext().setAttribute("Rez", rez);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce)
    {
        ServletContextListener.super.contextDestroyed(sce);
    }
}
