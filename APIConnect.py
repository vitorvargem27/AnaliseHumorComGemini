import os
import time
from typing import Optional

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from google import genai
from google.genai.errors import APIError

GEMINI_API_KEY = "AIzaSyDhXi3Un2gz-Dgk9szye98H4AV0pLnHbLY"

try:
    client = genai.Client(api_key=GEMINI_API_KEY) # O cliente busca a chave automaticamente da variável de ambiente
except Exception as e:
    # Se a chave não estiver configurada no ambiente, o cliente falhará.
    print(f"Erro ao inicializar o cliente Gemini. Certifique-se de que GEMINI_API_KEY está configurada: {e}")
    # Você pode definir um fallback ou sair, dependendo do seu caso.

app = FastAPI(
    title="Gemini Video Analysis API",
    description="API para analisar vídeos usando o Google Gemini",
    version="1.0.0"
)

# Modelo Pydantic para a requisição da análise
class AnaliseRequest(BaseModel):
    caminho_video: str
    prompt_analise: str
    # Opcional: para maior flexibilidade
    model_name: str = 'gemini-2.5-flash'


def analisar_video_gemini_core(client: genai.Client, caminho_video: str, prompt_analise: str, model_name: str) -> str:
    """
    Lógica central de upload, análise e exclusão do Gemini Files API.
    Retorna o texto da análise.
    """
    if not os.path.exists(caminho_video):
        raise FileNotFoundError(f"O arquivo de vídeo não foi encontrado em '{caminho_video}'")

    video_file = None
    try:
        # 1. Upload do Arquivo
        video_file = client.files.upload(file=caminho_video)

        # Aguarda o processamento
        while video_file.state == 'PROCESSING':
            time.sleep(5)
            video_file = client.files.get(name=video_file.name)

        if video_file.state != 'ACTIVE':
            raise Exception(f"Erro no processamento do vídeo. Estado final: {video_file.state}")

        # 2. Geração de Conteúdo (Análise)
        response = client.models.generate_content(
            model=model_name,
            contents=[prompt_analise, video_file]
        )

        return response.text

    except APIError as e:
        raise HTTPException(status_code=500, detail=f"Erro na API do Gemini: {e}")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Erro interno no servidor: {e}")
    finally:
        # 3. Limpeza (Deletar o arquivo)
        if video_file:
            client.files.delete(name=video_file.name)


@app.post("/api/v1/analisar-video/")
def analisar_video_endpoint(request: AnaliseRequest):
    """
    Endpoint da API para analisar um vídeo usando o Gemini.
    """
    try:
        resultado = analisar_video_gemini_core(
            client=client,
            caminho_video=request.caminho_video,
            prompt_analise=request.prompt_analise,
            model_name=request.model_name
        )
        # Retorna o resultado como um objeto JSON
        return {"status": "sucesso", "analise": resultado}
    except HTTPException as http_ex:
        raise http_ex
    except Exception as e:
        # Se ocorrer um erro que não foi tratado dentro do core
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    import uvicorn
    # Execute a API localmente
    uvicorn.run(app, host="0.0.0.0", port=8000)