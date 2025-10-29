package com.triviaapp.servlets;

import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;

public class QuizCompleteServlet extends HttpServlet
{
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
    {
        resp.setContentType("text/html;charset=UTF-8");
        String score = req.getParameter("scorePct");
        if (score == null) score = "0";

        try (PrintWriter w = resp.getWriter())
        {
            w.println("<!DOCTYPE html><html><head><meta charset='utf-8'><title>Quiz Complete</title>");
            w.println("<script src='https://cdn.jsdelivr.net/npm/@tailwindcss/browser@4'></script></head>");
            w.println("<body class='bg-blue-900 text-yellow-300 font-mono flex flex-col items-center justify-center h-screen'>");
            w.println("<div class='text-4xl font-bold mb-4'>Quiz Completed!</div>");
            w.println("<div class='text-2xl mb-6'>Your Score: " + score + "%</div>");
            w.println("<a href='/' class='bg-yellow-400 text-black px-6 py-2 rounded-lg'>Back to Categories</a>");
            w.println("</body></html>");
        }
    }
}
