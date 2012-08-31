/**
 * TÓPICOS ESPECIAIS EM DESENVOLVIMENTO WEB
 * WEBSERVER COM OS 8 REQUISITOS SOLICITADOS EM AULA
 * 2012.2
 * RAFAEL PERAZZO BARBOSA MOTA
 * 5060192
 * Prof. Gerosa
 */
package appwebserver;

import java.io.*;
import java.net.*;
import java.util.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;

public final class WebServer {
    
    public static void main(String argv[]) throws Exception {
        
        int porta = 8080; // Porta que o servidor ouvirá
        String diretorioBase = "/home/rafaelperazzo/www"; // diretório onde estarão os arquivos
        
        System.out.println("Servidor Web iniciado.");
        ServerSocket socket = new ServerSocket(porta); // Cria um socket

        while (true) { // Loop infinito aguardando conexões

            Socket conexaoSocket = socket.accept(); // Escuta o socket
            //4) Transformando o servidor em multithread
            Requisicao request = new Requisicao(conexaoSocket,diretorioBase);
            Thread thread = new Thread(request);
            thread.start();
            
        }

    }
}

final class Requisicao extends Thread {
    final static String CRLF = "\r\n";
    String diretorioBase;
    Socket socket;

    public Requisicao(Socket socket, String diretorioBase) throws Exception {
        this.socket = socket;
        this.diretorioBase = diretorioBase;
    }

    @SuppressWarnings("empty-statement")
    public void processa() throws Exception {
        InputStream inputStream = socket.getInputStream();
        DataOutputStream output = new DataOutputStream(socket.getOutputStream());
        BufferedReader input = new BufferedReader(new InputStreamReader(inputStream));
        String requestLine = input.readLine();
        PrintStream out = new PrintStream(output);
        String header = requestLine;
        // Extract the filename from the request line.
        StringTokenizer tokens = new StringTokenizer(requestLine);
        
        /*
         * PROCESSAMENTO DA ENTRADA - INICIO
         * 
         */
        //2) Código para servir arquivos
        tokens.nextToken();
        String arquivo = tokens.nextToken();
        arquivo = diretorioBase + arquivo;
        if (arquivo.contains("favicon.ico")) {
            if (new File(arquivo).exists()) {
                //Enviando a statusLine
                output.writeBytes("HTTP/1.1 200 OK" + CRLF);
                //Enviando Content-Line
                output.writeBytes(null);
                //Enviando a linha em branco
                output.writeBytes(CRLF);
                FileInputStream arquivoRequisitado = null;
                try {
                    arquivoRequisitado = new FileInputStream(arquivo);
                } catch (FileNotFoundException e) {
                    System.err.println(e.getMessage());
                }
                sendBytes(arquivoRequisitado, output);
                arquivoRequisitado.close();
            }
            else {
                //Enviando a statusLine
                output.writeBytes("HTTP/1.1 404 Not Found" + CRLF);
                //Enviando Content-Line
                output.writeBytes("text/plain" + CRLF);
                //Enviando a linha em branco
                output.writeBytes(CRLF);
                //Enviando entityBody
                output.writeBytes("Erro 404 - Arquivo inexistente: " + arquivo);
            }
        }
        else {
            /*8) Implementação de cookies para colocar o numero de visitas na página
            * Procurando a existencia de Cookies e autorização
            */
            String novaString;
            String cookieLine = "";
            String autorizationLine = "";
            boolean autorizado = false;
            while ( (novaString = input.readLine()).length()!=0 ) {
                header = header + novaString + "\n";
                if (novaString.contains("Cookie:")) {
                    cookieLine = novaString;
                }
                if(novaString.contains("Authorization")) {
                    autorizationLine = novaString;
                    autorizado=true;
                }
            }
            int contador = 0;
            StringTokenizer listaCookies = new StringTokenizer(cookieLine);
            if (listaCookies.countTokens()>0) { //Existe o cookie contador
                listaCookies.nextToken();
                String cont = listaCookies.nextToken();
                int posicaoIgual = cont.indexOf("=");
                if (posicaoIgual>0) {
                    contador = Integer.parseInt(cont.substring(posicaoIgual+1));
                    contador++;
                }
            }
            else {
                contador++;
            }
            //Obtendo dados de autenticação
            String login = "";
            String senha = "";
            if (!autorizationLine.isEmpty()) {
                StringTokenizer autorizacao = new StringTokenizer(autorizationLine);
                autorizacao.nextToken();
                autorizacao.nextToken();
                String dados = autorizacao.nextToken();
                String credenciais = new String(Base64.decodeBase64(dados));
                login = credenciais.substring(0, credenciais.indexOf(":"));
                senha = credenciais.substring(credenciais.indexOf(":")+1);
            }
            // Abrir o arquivo requisitado.
            FileInputStream arquivoRequisitado = null;
            boolean fileExists = true;
            try {
                arquivoRequisitado = new FileInputStream(arquivo);
            } catch (FileNotFoundException e) {
                fileExists = false;   
            }
        
            // Construindo a mensagem de resposta
            String statusLine;
            
            //3) Escrever o cabeçalho Server na resposta
            String serverLine = "Server: RP Server/GNULinux" + CRLF;
            //6) Implementar acesso a recursos mediante autorização (as 
            //credenciais são autenticadas com o algoritmo Base64)
            String autenticationLine = "WWW-Authenticate: Basic realm=\"Arquivo protegido\"" + CRLF;
                        
            if ((fileExists)) { //Se o arquivo existe
            
                //Definindo a linha de status
                if (!autorizado) {
                    statusLine = "HTTP/1.1 401 Authorization Required" + CRLF;
                    output.writeBytes(statusLine);
                    //Enviando a autenticação
                    output.writeBytes(autenticationLine);
                    output.writeBytes(CRLF);
                }
                else {
                    //Conferindo credenciais
                    if ((login.equals("rafael")) && (senha.equals("12345"))) { 
                        statusLine = "HTTP/1.1 200 OK" + CRLF;
                        //Definindo o tipo de conteúdo do arquivo
                        String contentTypeLine = "Content-type: " + contentType(arquivo) +  CRLF;
                        //8) Implementação de cookies para colocar o numero de visitas na página
                        String cookie = "Set-Cookie: contador=" + contador + CRLF;
                        String html = FileUtils.readFileToString(new File(arquivo));
                        //7) Interpretando um script PHP
                        String retorno =  "";
                        while (html.contains("<?")) {
                            String script = html.substring(html.indexOf("<?"), html.indexOf("?>")+2);
                            File f = new File(diretorioBase + "/tmp.php");
                            if (f.exists()) {
                                f.delete();
                            }
                            f.createNewFile();
                            FileUtils.writeStringToFile(f, script);
                            retorno = executarComandoExterno("php " + diretorioBase+"/tmp.php");                                             
                            html = html.replace(script, retorno);
                            f.delete();
                        }
                        
                        if (html.indexOf("Visitas ao servidor")==-1) { //Primeira visita
                            html = html.replace("</body>", "<br><br>Visitas ao servidor: " + String.valueOf(contador) + "</body>");            
                            
                        }
                        else { //Demais visitas
                            String str = html.substring(html.indexOf("<br><br>Visitas ao servidor: "), html.indexOf("</html>") + 7);
                            html = html.replace(str," ");
                            html = html.concat("\n</body>\n</html>");
                            html = html.replace("</body>", "<br><br>Visitas ao servidor: " + String.valueOf(contador) + "</body>");            
                        }
                        //FileUtils.writeStringToFile(new File(arquivo), html);
                        output.writeBytes(statusLine);
                        //Enviando Content-Line
                        output.writeBytes(contentTypeLine);
                        //Enviando a serverLine
                        output.writeBytes(serverLine);
                        //Enviando cookies
                        output.writeBytes(cookie);
                        //Enviando a linha em branco
                        output.writeBytes(CRLF);
                        //Enviando o conteudo
                        output.writeBytes(html + CRLF);     
                        
               
                    }       
                    else { // Se as credenciais estiverem erradas
                        statusLine = "HTTP/1.1 401 Unauthorized" + CRLF;
                        String contentTypeLine = "Content-type: " + "text/plain" + CRLF;
                        //Definindo o conteúdo
                        String entityBody = "Erro 401 - Não autorizado!" + CRLF;
                        output.writeBytes(statusLine);
                        //Enviando Content-Line
                        output.writeBytes(contentTypeLine);
                        //Enviando a serverLine
                        output.writeBytes(serverLine);
                        //Enviando a linha em branco
                        output.writeBytes(CRLF);
                        //Enviando dados
                        output.writeBytes(entityBody);
                    }
                }
                arquivoRequisitado.close();
            }
            else { // Se o arquivo não existe
            
                //5) Tratar os erros de requisições malformadas ou de recursonão encontrado
                //Definindo a linha de status
                statusLine = "HTTP/1.1 404 Not Found" + CRLF;
                String contentTypeLine = "Content-type: " + "text/plain" + CRLF;
                //Definindo o conteúdo
                String entityBody = "Erro 404 - Arquivo inexistente!" + CRLF;
                
                output.writeBytes(statusLine);
                //Enviando Content-Line
                output.writeBytes(contentTypeLine);
                //Enviando a serverLine
                output.writeBytes(serverLine);
                //Enviando a linha em branco
                output.writeBytes(CRLF);
                //Enviando dados
                output.writeBytes(entityBody);
            }
                       
        /*
         *  PROCESSAMENTO DA ENTRADA - FIM
         */
        }
        
        try {
            output.close();
            input.close();
            socket.close();
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
        }
        
    }

    private static void sendBytes(FileInputStream fis, OutputStream os) throws Exception {
        byte[] buffer = new byte[1024];
        int bytes;
        while ((bytes = fis.read(buffer)) != -1) {
            os.write(buffer, 0, bytes);
        }
    }

    private static String contentType(String fileName) {
        if (fileName.endsWith(".htm") || fileName.endsWith(".html")) {
            return "text/html";
        }
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (fileName.endsWith(".gif")) {
            return "image/gif";
        }
        if (fileName.endsWith(".txt")) {
            return "text/plain";
        }
        if (fileName.endsWith(".pdf")) {
            return "application/pdf";
        }
        return "application/octet-stream";
    }
    
    //4) Servidor multithread
    @Override
    public void run() {
        try {
            this.processa();
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
        
    }
    
    private String executarComandoExterno(String cmd) {
        int pid = -1;
        String retorno = "";
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            BufferedInputStream buffer = new BufferedInputStream( process.getInputStream() );
            BufferedReader commandOutput= new BufferedReader( new InputStreamReader( buffer ) );
            String line = "";     
            while ( ( line = commandOutput.readLine() ) != null ) {
                retorno = retorno + line;
             }
             commandOutput.close(); 
        }
        catch(IOException e) {
            System.err.println(e.getMessage());
        }
        return retorno;
    }
    
}
