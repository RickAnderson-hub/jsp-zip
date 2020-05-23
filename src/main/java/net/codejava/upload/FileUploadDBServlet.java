package net.codejava.upload;

import org.apache.commons.io.IOUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.*;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@WebServlet("/uploadServlet")
@MultipartConfig(maxFileSize = 16177215)    // upload file's size up to 16MB
public class FileUploadDBServlet extends HttpServlet {

    // database connection settings
    private String dbURL = "jdbc:mysql://localhost:3306/AppDB";
    private String dbUser = "root";
    private String dbPass = "password";

    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws ServletException, IOException {
        // gets values of text fields
        String firstName = request.getParameter("firstName");
        String lastName = request.getParameter("lastName");

        InputStream inputStream = null;    // input stream of the upload file

        // obtains the upload file part in this multipart request
        Part filePart = request.getPart("photo");
        if (filePart != null) {
            // prints out some information for debugging
            System.out.println(filePart.getName());
            System.out.println(filePart.getSize());
            System.out.println(filePart.getContentType());

            // obtains input stream of the upload file
            inputStream = filePart.getInputStream();
            saveInputStreamIntoCache(inputStream);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(out, Charset.forName("UTF-8"));
        try {
            zos.setLevel(Deflater.NO_COMPRESSION);
            zos.putNextEntry(new ZipEntry("archived0"));
            IOUtils.copy(inputStream, zos); // org.apache.commons.io.IOUtils;
            zos.closeEntry();
        /* use more Entries to add more files
        and use closeEntry() to close each file entry */
        } finally {
            zos.close();
        }

        Connection conn = null;    // connection to the database
        String message = null;    // message will be sent back to client

        try {
            // connects to the database
            DriverManager.registerDriver(new com.mysql.jdbc.Driver());
            conn = DriverManager.getConnection(dbURL, dbUser, dbPass);

            // constructs SQL statement
            String sql = "INSERT INTO contacts (first_name, last_name, photo) values (?, ?, ?)";
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, firstName);
            statement.setString(2, lastName);

            // fetches input stream of the upload file for the blob column
            InputStream theFile = new ByteArrayInputStream(out.toByteArray());
            writeToFile(theFile, "zip.zip");
            statement.setBlob(3, theFile);
            out.close();
            theFile.close();
            // sends the statement to the databasy
            // e server
            int row = statement.executeUpdate();
            if (row > 0) {
                message = "File uploaded and saved into database";
            }
        } catch (SQLException ex) {
            message = "ERROR: " + ex.getMessage();
            ex.printStackTrace();
        } finally {
            if (conn != null) {
                // closes the database connection
                try {
                    conn.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            // sets the message in request scope
            request.setAttribute("Message", message);

            // forwards to the message page
            getServletContext().getRequestDispatcher("/Message.jsp").forward(request, response);
        }
    }

    private void saveInputStreamIntoCache(InputStream inputStream) {

    }

    private void writeToFile(InputStream theFile, String filename) throws IOException {
//        byte[] buffer = new byte[theFile.available()];
//
//        File targetFile = new File("/home/rick/deleteme/" + filename);
//        OutputStream outStream = new FileOutputStream(targetFile);
//        outStream.write(buffer);
//        outStream.close();

        byte[] buffer = new byte[theFile.available()];
        theFile.read(buffer);

        File targetFile = new File("/home/rick/deleteme/" + filename);
        OutputStream outStream = new FileOutputStream(targetFile);
        outStream.write(buffer);

    }
}