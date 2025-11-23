import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Main {

    // Configurações da API Python
    private static final String API_URL = "http://localhost:8000/api/v1/analisar-video/";

    // Cliente HTTP (Thread-safe, pode ser reutilizado)
    private static final HttpClient httpClient = HttpClient.newBuilder().build();

    // Mapper para JSON (Thread-safe)
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {

        // Caminho do vídeo na MÁQUINA onde está rodando a API Python
        String videoPath = "C:\\Users\\Administrador\\Downloads\\teste_reacoes.mp4";

        // Prompt que será enviado para o Gemini fazer análise
        String prompt = "Você é um especialista em análise de expressões faciais. Retorne o humor correto para cada segundo do vídeo.";

        try {
            String resultadoAnalise = analisarVideo(videoPath, prompt);

            System.out.println("--- Resultado da Análise do Gemini (Via Java) ---");
            System.out.println(resultadoAnalise);
            System.out.println("-------------------------------------------------");

        } catch (Exception e) {
            System.err.println("Erro ao consumir a API: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Envia a requisição POST para a API Python.
     */
    public static String analisarVideo(String caminhoVideo, String prompt)
            throws IOException, InterruptedException {

        // 1. Criar o payload JSON
        String jsonPayload = objectMapper.writeValueAsString(
                Map.of("caminho_video", caminhoVideo, "prompt_analise", prompt)
        );

        // 2. Construir a requisição HTTP
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        // 3. Enviar a requisição
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // 4. Verificar se deu erro
        if (response.statusCode() != 200) {
            throw new RuntimeException("Falha na API. Código: "
                    + response.statusCode()
                    + " Detalhes: "
                    + response.body());
        }

        // 5. Interpretar o JSON de resposta
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);

        return responseBody.get("analise").toString();
    }
}
