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
        String prompt = "Você é um especialista em análise de expressões fáciais e humor das pessoas e deve analisar este vídeo, "
         + "identificando as principais ações. reaçoes, humores e sentimentos passados pela pessoa do vídeo."
         + "Você está aqui para dar dicas humanizadas sobre como melhorar o comportamento em um processo seletivo,"
         + " pois os vídeos analisados são de processo seletivo testes feitos com você."
         + "\n\nIDIOMA :"
         + "\nIdentifique e retorne no mesmo idioma da solicitação enviada (PT-BR, EN, ES, etc.)."
         + "\n\nRetorne o humor correto para cada segundo do vídeo."
         +"\n\nRegras :"
         + "\n*SEMPRE* analise com calma e procure em vídeos de analise no youtube e em outros meios"
         + "para responder corretamente sobre as expressões da pessoa do vídeo"
         + "\nIGNORE os sons externos e foque apenas em me dizer quais os humores (exemplos: chateação, raiva, alegria,"
         + " e todas as identificadas) da pessoa."
         + "\nDETALHE qual segundo específico do vídeo foi notado o humor"
         + "\n\nTIPO DE RESPOSTA :"
         + "\n1. -> SEMPRE foque na empatia para falar sobre os humores da pessoa."
         + "\n2. -> Se comunique de maneira pacífica, buscando críticas construtivas."
         + "\n3. -> Jamais use palavras que podem deixar uma pessoa magoada."
         + "\n4. -> Quando notar algo a ser melhorado, informe de maneira construtiva falando 'Você pode fazer x coisa "
         + "em vez de y coisa, pois assim melhorará em tal análise do recrutador'."
         + "\n\nPADRÃO DE RESPOSTA :"
         + "\n1- Seção **Análise** onde você começará com o que analisou sobre cada segundo do vídeo."
         + "\n2- Seção **Dicas** onde irá colocar as dicas construtivas para a pessoa."
         + "\n3- Seção **Apoio** onde você enviará uma mensagem de apoio com pontos positivos do que avaliou."
         + "\n\nOrganize as seções acima com um design apresentável, com emojis (se possível)" ;

        try {
            String resultadoAnalise = analisarVideo(videoPath, prompt);

            System.out.println("=================== Resultado da Análise do Gemini : =============================");
            System.out.println(resultadoAnalise);
            System.out.println("==================================================================================");

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
